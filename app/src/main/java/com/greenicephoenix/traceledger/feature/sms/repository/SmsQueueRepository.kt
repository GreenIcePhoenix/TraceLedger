package com.greenicephoenix.traceledger.feature.sms.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.provider.Telephony
import com.greenicephoenix.traceledger.core.database.dao.SmsPendingTransactionDao
import com.greenicephoenix.traceledger.core.database.dao.SmsCustomRuleDao
import com.greenicephoenix.traceledger.core.database.dao.TransactionDao
import com.greenicephoenix.traceledger.core.database.dao.AccountDao
import com.greenicephoenix.traceledger.core.database.dao.CategoryDao
import com.greenicephoenix.traceledger.core.database.entity.SmsPendingTransactionEntity
import com.greenicephoenix.traceledger.core.database.entity.TransactionEntity
import com.greenicephoenix.traceledger.feature.sms.model.SmsParseResult
import com.greenicephoenix.traceledger.feature.sms.model.SmsTransactionType
import com.greenicephoenix.traceledger.feature.sms.parser.SmsRuleEngine
import kotlinx.coroutines.flow.Flow
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Handles all SMS-to-queue operations:
 *  - processIncomingSms()   — parse a single SMS and save to queue if valid
 *  - scanInbox()            — bulk-scan SMS inbox for historical transactions
 *  - acceptTransaction()    — convert a queued item into a real Transaction
 *  - rejectTransaction()    — mark a queued item as rejected
 *  - observePending()       — live Flow of pending items for the UI
 */
class SmsQueueRepository(
    private val smsPendingDao: SmsPendingTransactionDao,
    private val smsCustomRuleDao: SmsCustomRuleDao,
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val context: Context,
) {
    private val engine = SmsRuleEngine()

    // =========================================================================
    //  OBSERVE
    // =========================================================================

    fun observePendingCount(): Flow<Int> = smsPendingDao.observePendingCount()
    fun observePending(): Flow<List<SmsPendingTransactionEntity>> = smsPendingDao.observePending()

    // =========================================================================
    //  PROCESS INCOMING SMS (real-time)
    // =========================================================================

    /**
     * Called by SmsTransactionReceiver for each incoming SMS.
     * Returns true if the SMS was queued, false if it was ignored.
     */
    suspend fun processIncomingSms(
        sender: String,
        body: String,
        timestamp: Long,
        smsId: Long
    ): Boolean {
        // 1. Deduplication check using content hash
        val hash = contentHash(sender, body)
        if (smsPendingDao.countByHash(hash) > 0) return false

        // 2. Get custom rules to pass to the engine
        val customRules = smsCustomRuleDao.getEnabledRulesSorted()

        // 3. Parse the SMS
        val parseResult = engine.parse(sender, body, timestamp, customRules)
        if (parseResult !is SmsParseResult.Success) return false

        val parsed = parseResult.transaction

        // 4. Find matching account by last-4 digits
        val suggestedAccountId = parsed.accountLastFour?.let { last4 ->
            accountDao.findByLastFour(last4)?.id
        }

        // 5. Find matching category by suggested name
        val suggestedCategoryId = parsed.suggestedCategoryName?.let { name ->
            categoryDao.findByName(name)?.id
        }

        // 6. Save to queue
        smsPendingDao.insert(
            SmsPendingTransactionEntity(
                smsId = smsId,
                smsBody = body,
                sender = sender,
                receivedAt = timestamp,
                parsedAmount = parsed.amount,
                parsedDescription = parsed.description,
                parsedType = parsed.type.name,
                parsedDate = parsed.transactionDate,
                suggestedCategoryId = suggestedCategoryId,
                suggestedAccountId = suggestedAccountId,
                accountLastFour = parsed.accountLastFour,
                contentHash = hash,
                createdAt = System.currentTimeMillis()
            )
        )
        return true
    }

    // =========================================================================
    //  INBOX SCAN (historical)
    // =========================================================================

    /**
     * Scans the SMS inbox for past financial messages.
     *
     * @param daysBack How many days back to scan (default: 90 days)
     * @param progressCallback Called after each SMS is processed with (current, total)
     * @return Number of new items added to the queue
     */
    suspend fun scanInbox(
        daysBack: Int = 90,
        progressCallback: ((current: Int, total: Int) -> Unit)? = null
    ): Int {
        val cutoffMs = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(daysBack.toLong())
        val contentResolver: ContentResolver = context.contentResolver
        var queued = 0

        val cursor = contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,   // sender
                Telephony.Sms.BODY,
                Telephony.Sms.DATE
            ),
            "${Telephony.Sms.DATE} > ?",
            arrayOf(cutoffMs.toString()),
            "${Telephony.Sms.DATE} DESC"
        ) ?: return 0

        val total = cursor.count
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

                val wasQueued = processIncomingSms(sender, body, timestamp, smsId)
                if (wasQueued) queued++
            }
        }

        return queued
    }

    // =========================================================================
    //  ACCEPT / REJECT
    // =========================================================================

    /**
     * Converts a pending SMS item into a real Transaction in the main ledger.
     *
     * @param pendingId     The ID from sms_pending_transactions
     * @param accountId     The TraceLedger account to save to
     * @param categoryId    The category (may be null for uncategorised)
     * @param amount        Possibly edited by user on review screen
     * @param description   Possibly edited by user
     * @param date          Unix ms timestamp
     * @param type          EXPENSE or INCOME
     */
    suspend fun acceptTransaction(
        pendingId: Long,
        accountId: Long,
        categoryId: Long?,
        amount: Double,
        description: String,
        date: Long,
        type: String,
        note: String = ""
    ) {
        // Save as a real transaction
        transactionDao.insert(
            TransactionEntity(
                accountId = accountId,
                categoryId = categoryId,
                amount = amount,
                type = type,
                date = date,
                note = if (note.isNotBlank()) note else description,
                title = description,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )

        // Mark the pending item as accepted
        smsPendingDao.markProcessed(pendingId, accepted = true)
    }

    suspend fun rejectTransaction(pendingId: Long) {
        smsPendingDao.markProcessed(pendingId, accepted = false)
    }

    suspend fun rejectAll() {
        smsPendingDao.rejectAll()
    }

    /** Update a pending item's parsed fields (user edited on review screen) */
    suspend fun updateParsedFields(
        id: Long,
        amount: Double,
        description: String,
        type: String,
        categoryId: Long?,
        accountId: Long?,
        date: Long
    ) {
        smsPendingDao.updateParsedFields(id, amount, description, type, categoryId, accountId, date)
    }

    // =========================================================================
    //  MAINTENANCE
    // =========================================================================

    suspend fun cleanupOldProcessed() {
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
        smsPendingDao.deleteOldProcessed(cutoff)
    }

    // =========================================================================
    //  HELPERS
    // =========================================================================

    private fun contentHash(sender: String, body: String): String {
        val input = "$sender::$body"
        return MessageDigest.getInstance("MD5")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}