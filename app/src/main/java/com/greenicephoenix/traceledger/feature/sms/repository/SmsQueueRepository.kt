package com.greenicephoenix.traceledger.feature.sms.repository

import android.content.ContentResolver
import android.content.Context
import android.provider.Telephony
import com.greenicephoenix.traceledger.core.database.dao.SmsPendingTransactionDao
import com.greenicephoenix.traceledger.core.database.dao.SmsCustomRuleDao
import com.greenicephoenix.traceledger.core.database.dao.TransactionDao
import com.greenicephoenix.traceledger.core.database.dao.AccountDao
import com.greenicephoenix.traceledger.core.database.dao.CategoryDao
import com.greenicephoenix.traceledger.core.database.entity.SmsCustomRuleEntity
import com.greenicephoenix.traceledger.core.database.entity.SmsPendingTransactionEntity
import com.greenicephoenix.traceledger.core.database.entity.TransactionEntity
import com.greenicephoenix.traceledger.domain.model.AccountType
import com.greenicephoenix.traceledger.feature.sms.model.SmsParseResult
import com.greenicephoenix.traceledger.feature.sms.parser.SmsRuleEngine
import com.greenicephoenix.traceledger.feature.sms.store.SmsLearningStore
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

class SmsQueueRepository(
    private val smsPendingDao: SmsPendingTransactionDao,
    private val smsCustomRuleDao: SmsCustomRuleDao,
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val context: Context,
    private val learningStore: SmsLearningStore,
) {
    private val engine = SmsRuleEngine()

    // ── Observe ───────────────────────────────────────────────────────────────

    fun observePendingCount(): Flow<Int> = smsPendingDao.observePendingCount()
    fun observePending(): Flow<List<SmsPendingTransactionEntity>> = smsPendingDao.observePending()

    // ── Process incoming SMS ──────────────────────────────────────────────────

    suspend fun processIncomingSms(
        sender: String,
        body: String,
        timestamp: Long,
        smsId: Long
    ): Boolean {
        val hash = contentHash(sender, body)
        if (smsPendingDao.countByHash(hash) > 0) return false

        val customRules = smsCustomRuleDao.getEnabledRulesSorted()
        val parseResult = engine.parse(sender, body, timestamp, customRules)
        if (parseResult !is SmsParseResult.Success) return false

        val parsed = parseResult.transaction

        // ── Issue 3: Find which custom rule matched (if any) ──────────────────
        // Custom rule defaults take the HIGHEST priority for category and account.
        // The user explicitly configured these — we must respect them.
        val matchedCustomRule: SmsCustomRuleEntity? = customRules.firstOrNull { rule ->
            !rule.isExclusionRule &&
                    sender.lowercase().contains(rule.senderPattern.lowercase())
        }

        // ── Category suggestion — priority order: ─────────────────────────────
        // 1. Custom rule default (user explicitly configured)
        // 2. Learning store (user correction history)
        // 3. Auto-categorizer by keyword match
        val suggestedCategoryId: String? =
            matchedCustomRule?.defaultCategoryId?.takeIf { it.isNotBlank() }
                ?: learningStore.getLearnedCategoryForDescription(parsed.description)
                ?: parsed.suggestedCategoryName?.let { name -> categoryDao.findByName(name)?.id }

        // ── Account suggestion — priority order: ──────────────────────────────
        // 1. Custom rule default (user explicitly configured)
        // 2. Learning store
        // 3. Body-based bank name detection (handles UPI credit notifications)
        // 4. Sender-based detection
        val suggestedAccountId: String? =
            matchedCustomRule?.defaultAccountId?.takeIf { it.isNotBlank() }
                ?: resolveAccountSuggestion(sender, body)

        smsPendingDao.insert(
            SmsPendingTransactionEntity(
                smsId               = smsId,
                smsBody             = body,
                sender              = sender,
                receivedAt          = timestamp,
                parsedAmount        = parsed.amount,
                parsedDescription   = parsed.description,
                parsedType          = parsed.type.name,
                parsedDate          = parsed.transactionDate,
                suggestedCategoryId = suggestedCategoryId,
                suggestedAccountId  = suggestedAccountId,
                accountLastFour     = parsed.accountLastFour,
                contentHash         = hash,
            )
        )
        return true
    }

    // ── Account resolution ────────────────────────────────────────────────────

    /**
     * Resolves which TraceLedger account this SMS most likely belongs to.
     *
     * Priority:
     *  1. Learning store (user-trained — highest confidence)
     *  2. Body-based bank detection when body has bank name + account number.
     *     This handles UPI credit notifications (e.g. PhonePe sender but
     *     "ICICI bank" appears in the body alongside "xx160").
     *  3. Sender-based detection (standard path for direct bank SMSes)
     *  4. null → user picks manually on review screen
     */
    private suspend fun resolveAccountSuggestion(
        sender: String,
        body: String,
    ): String? {
        // 1. Learning store
        learningStore.getLearnedAccountForSender(sender)?.let { return it }

        // 2. Body-based detection — only used when body explicitly names a bank
        //    AND contains an account number (strong signal this is a bank txn,
        //    not just a UPI payment confirmation to a wallet)
        val bodyBankInfo = engine.detectBankInfoFromBody(body)
        if (bodyBankInfo != null && engine.hasAccountInBody(body)) {
            val fragment  = extractSearchFragment(bodyBankInfo.bankName)
            val typeHint  = if (isCreditCardSms(body) || bodyBankInfo.isCreditCard)
                AccountType.CREDIT_CARD.name else AccountType.BANK.name
            val byTypeAndName = accountDao.findByNameContainingAndType(fragment, typeHint)?.id
            if (byTypeAndName != null) return byTypeAndName
            val byNameOnly = accountDao.findByNameContaining(fragment)?.id
            if (byNameOnly != null) return byNameOnly
        }

        // 3. Sender-based detection (original logic)
        val bankInfo = engine.detectBankInfo(sender) ?: return null
        val fragment = extractSearchFragment(bankInfo.bankName)

        return when {
            isCreditCardSms(body) || bankInfo.isCreditCard ->
                accountDao.findByNameContainingAndType(fragment, AccountType.CREDIT_CARD.name)?.id
                    ?: accountDao.findByNameContaining(fragment)?.id

            bankInfo.isWallet ->
                accountDao.findByNameContainingAndType(fragment, AccountType.WALLET.name)?.id
                    ?: accountDao.findByNameContaining(fragment)?.id

            else ->
                accountDao.findByNameContainingAndType(fragment, AccountType.BANK.name)?.id
                    ?: accountDao.findByNameContaining(fragment)?.id
        }
    }

    // ── Inbox scan ────────────────────────────────────────────────────────────

    /**
     * Scans the SMS inbox between [startMs] and [endMs] (both inclusive, epoch-ms).
     *
     * Issue 2: now accepts explicit timestamps instead of a relative daysBack Int,
     * so the UI can pass either a preset range or a custom date picker selection.
     *
     * Default endMs = now so callers can omit it for "up to today" scans.
     */
    suspend fun scanInbox(
        startMs: Long,
        endMs: Long = System.currentTimeMillis(),
        progressCallback: ((current: Int, total: Int) -> Unit)? = null
    ): Int {
        val contentResolver: ContentResolver = context.contentResolver
        var queued = 0

        val cursor = contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
            // Filter both ends of the range
            "${Telephony.Sms.DATE} >= ? AND ${Telephony.Sms.DATE} <= ?",
            arrayOf(startMs.toString(), endMs.toString()),
            "${Telephony.Sms.DATE} DESC"
        ) ?: return 0

        val total   = cursor.count
        var current = 0

        cursor.use { c ->
            val idCol      = c.getColumnIndexOrThrow(Telephony.Sms._ID)
            val addressCol = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyCol    = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateCol    = c.getColumnIndexOrThrow(Telephony.Sms.DATE)

            while (c.moveToNext()) {
                current++
                progressCallback?.invoke(current, total)
                val smsId     = c.getLong(idCol)
                val sender    = c.getString(addressCol) ?: continue
                val body      = c.getString(bodyCol)    ?: continue
                val timestamp = c.getLong(dateCol)
                if (processIncomingSms(sender, body, timestamp, smsId)) queued++
            }
        }
        return queued
    }

    // ── Accept / Reject ───────────────────────────────────────────────────────

    suspend fun acceptTransaction(
        pendingId: Long,
        accountId: String,
        categoryId: String?,
        amount: Double,
        description: String,
        dateMsEpoch: Long,
        type: String,
    ) {
        val localDate = Instant.ofEpochMilli(dateMsEpoch)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        transactionDao.insertTransaction(
            TransactionEntity(
                id            = UUID.randomUUID().toString(),
                type          = type,
                amount        = BigDecimal.valueOf(amount),
                date          = localDate,
                fromAccountId = if (type == "EXPENSE") accountId else null,
                toAccountId   = if (type == "INCOME")  accountId else null,
                categoryId    = categoryId,
                note          = description,
                createdAt     = Instant.now(),
            )
        )
        smsPendingDao.markProcessed(pendingId, accepted = true)
    }

    suspend fun rejectTransaction(pendingId: Long) {
        smsPendingDao.markProcessed(pendingId, accepted = false)
    }

    suspend fun rejectAll() {
        smsPendingDao.rejectAll()
    }

    suspend fun cleanupOldProcessed() {
        val cutoff = System.currentTimeMillis() - java.util.concurrent.TimeUnit.DAYS.toMillis(30)
        smsPendingDao.deleteOldProcessed(cutoff)
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun extractSearchFragment(bankName: String): String =
        bankName.lowercase()
            .replace(" credit card", "")
            .replace(" bank", "")
            .replace(" card", "")
            .trim()
            .split(" ")
            .firstOrNull { it.isNotBlank() && it.length > 1 }
            ?: bankName.lowercase().take(4)

    private fun isCreditCardSms(body: String): Boolean {
        val lower = body.lowercase()
        return lower.contains("credit card") ||
                lower.contains("outstanding") ||
                lower.contains("min due") ||
                lower.contains("payment due") ||
                lower.contains("statement balance") ||
                lower.contains("minimum payment") ||
                lower.contains("available credit") ||
                lower.contains("card xx")
    }

    private fun contentHash(sender: String, body: String): String {
        val input = "$sender::$body"
        return MessageDigest.getInstance("MD5")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}