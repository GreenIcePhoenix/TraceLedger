package com.greenicephoenix.traceledger.feature.sms.parser

import com.greenicephoenix.traceledger.core.database.entity.SmsCustomRuleEntity
import com.greenicephoenix.traceledger.feature.sms.model.ParsedSmsTransaction
import com.greenicephoenix.traceledger.feature.sms.model.SmsParseResult
import com.greenicephoenix.traceledger.feature.sms.model.SmsTransactionType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * The core SMS parsing engine.
 *
 * HOW IT WORKS (pipeline):
 *  1. isFinancialSms()  — quickly reject OTPs, promos, non-transactional SMS
 *  2. detectBankInfo()  — identify the bank/wallet for richer descriptions
 *  3. extractAmount()   — find and parse the transaction amount
 *  4. detectDirection() — debit (EXPENSE) or credit (INCOME)
 *  5. extractAccountLastFour() — link to a TraceLedger account
 *  6. extractMerchant() — build a clean description
 *  7. extractDate()     — parse transaction date from SMS text
 *  8. suggestCategory() — auto-categorise based on description
 *
 * CUSTOM RULES:
 *  Pass the list of user's enabled custom rules to parse().
 *  Custom rules are checked FIRST (they have higher priority).
 *  If a custom rule matches, it bypasses the built-in engine entirely.
 */
class SmsRuleEngine {

    /**
     * Main entry point. Parses a single SMS.
     *
     * @param sender    The sender ID (e.g. "VK-HDFCBK")
     * @param body      The full SMS body text
     * @param timestamp The Unix timestamp (ms) when the SMS was received
     * @param customRules User's custom rules from the database
     */
    fun parse(
        sender: String,
        body: String,
        timestamp: Long,
        customRules: List<SmsCustomRuleEntity> = emptyList()
    ): SmsParseResult {

        // --- Gate 1: Reject non-financial SMS ---
        if (!isFinancialSms(sender, body)) {
            return SmsParseResult.NotFinancial
        }

        // --- Gate 2: Try custom rules first (user rules override built-ins) ---
        val customResult = tryCustomRules(sender, body, timestamp, customRules)
        if (customResult != null) return customResult

        // --- Gate 3: Built-in generic parser ---
        return parseWithBuiltInRules(sender, body, timestamp)
    }

    // =========================================================================
    //  STEP 1: IS THIS A FINANCIAL SMS?
    // =========================================================================

    fun isFinancialSms(sender: String, body: String): Boolean {
        val senderUpper = sender.uppercase()
        val bodyLower = body.lowercase()

        // Reject OTP messages immediately
        if (BuiltInSmsRules.OTP_KEYWORDS.any { bodyLower.contains(it) }) return false

        // Reject promotional messages
        if (BuiltInSmsRules.PROMO_KEYWORDS.count { bodyLower.contains(it) } >= 2) return false

        // Accept if sender is a known financial institution
        val isKnownSender = BuiltInSmsRules.KNOWN_SENDERS.any { bankInfo ->
            bankInfo.senderContains.any { senderUpper.contains(it.uppercase()) }
        }
        if (isKnownSender) {
            // Even known senders send OTPs — re-check that we have an amount pattern
            return BuiltInSmsRules.AMOUNT_PATTERNS.any { it.containsMatchIn(body) }
        }

        // For unknown senders: must have both an amount AND a debit/credit keyword
        val hasAmount = BuiltInSmsRules.AMOUNT_PATTERNS.any { it.containsMatchIn(body) }
        val hasDirection = BuiltInSmsRules.DEBIT_KEYWORDS.any { bodyLower.contains(it) } ||
                BuiltInSmsRules.CREDIT_KEYWORDS.any { bodyLower.contains(it) }

        return hasAmount && hasDirection
    }

    // =========================================================================
    //  STEP 2: TRY CUSTOM RULES
    // =========================================================================

    private fun tryCustomRules(
        sender: String,
        body: String,
        timestamp: Long,
        customRules: List<SmsCustomRuleEntity>
    ): SmsParseResult? {
        val senderLower = sender.lowercase()

        for (rule in customRules) {
            if (!senderLower.contains(rule.senderPattern.lowercase())) continue

            return if (rule.isAdvancedMode && rule.rawRegex.isNotBlank()) {
                parseWithAdvancedCustomRule(body, timestamp, rule)
            } else {
                parseWithSimpleCustomRule(body, timestamp, rule)
            }
        }
        return null
    }

    private fun parseWithSimpleCustomRule(
        body: String,
        timestamp: Long,
        rule: SmsCustomRuleEntity
    ): SmsParseResult {
        val amount = extractAmount(body) ?: return SmsParseResult.ParseError("No amount in custom rule match")
        val type = detectDirectionWithKeywords(
            body,
            rule.debitKeywords.split(",").map { it.trim() },
            rule.creditKeywords.split(",").map { it.trim() }
        ) ?: detectDirection(body) ?: SmsTransactionType.EXPENSE

        val merchant = if (rule.merchantRegex.isNotBlank()) {
            try {
                Regex(rule.merchantRegex, RegexOption.IGNORE_CASE)
                    .find(body)?.groupValues?.getOrNull(1)?.trim() ?: extractMerchant(body)
            } catch (_: Exception) {
                extractMerchant(body)
            }
        } else {
            extractMerchant(body)
        }

        return SmsParseResult.Success(
            ParsedSmsTransaction(
                amount = amount,
                description = merchant,
                type = type,
                transactionDate = extractDate(body, timestamp),
                accountLastFour = extractAccountLastFour(body),
                detectedBankName = null,
                suggestedCategoryName = BuiltInSmsRules.suggestCategory(merchant)
            )
        )
    }

    private fun parseWithAdvancedCustomRule(
        body: String,
        timestamp: Long,
        rule: SmsCustomRuleEntity
    ): SmsParseResult {
        return try {
            val regex = Regex(rule.rawRegex, RegexOption.IGNORE_CASE)
            val match = regex.find(body) ?: return SmsParseResult.ParseError("Advanced regex no match")
            val amountStr = match.groups["amount"]?.value
                ?: return SmsParseResult.ParseError("Advanced regex: no 'amount' group")
            val amount = amountStr.replace(",", "").toDoubleOrNull()
                ?: return SmsParseResult.ParseError("Advanced regex: invalid amount")
            val merchant = match.groups["merchant"]?.value?.trim() ?: extractMerchant(body)
            val typeStr = match.groups["type"]?.value?.lowercase()
            val type = when {
                typeStr != null && CREDIT_TYPE_VALUES.any { typeStr.contains(it) } -> SmsTransactionType.INCOME
                typeStr != null && DEBIT_TYPE_VALUES.any { typeStr.contains(it) } -> SmsTransactionType.EXPENSE
                else -> detectDirection(body) ?: SmsTransactionType.EXPENSE
            }
            SmsParseResult.Success(
                ParsedSmsTransaction(
                    amount = amount,
                    description = merchant,
                    type = type,
                    transactionDate = extractDate(body, timestamp),
                    accountLastFour = extractAccountLastFour(body),
                    detectedBankName = null,
                    suggestedCategoryName = BuiltInSmsRules.suggestCategory(merchant)
                )
            )
        } catch (e: Exception) {
            SmsParseResult.ParseError("Advanced regex error: ${e.message}")
        }
    }

    // =========================================================================
    //  STEP 3: BUILT-IN GENERIC PARSER
    // =========================================================================

    private fun parseWithBuiltInRules(
        sender: String,
        body: String,
        timestamp: Long
    ): SmsParseResult {
        val amount = extractAmount(body)
            ?: return SmsParseResult.ParseError("No amount found")

        val direction = detectDirection(body)
            ?: return SmsParseResult.ParseError("Could not determine debit/credit direction")

        val bankInfo = detectBankInfo(sender)
        val merchant = extractMerchant(body)
        val accountLast4 = extractAccountLastFour(body)
        val date = extractDate(body, timestamp)

        // Build the description: "Zomato" or "HDFC Bank · Zomato"
        val description = if (bankInfo != null && merchant.isNotBlank() &&
            !merchant.contains(bankInfo.bankName, ignoreCase = true)
        ) {
            "${bankInfo.bankName} · $merchant"
        } else if (merchant.isNotBlank()) {
            merchant
        } else {
            bankInfo?.bankName ?: "Unknown"
        }

        val categoryName = BuiltInSmsRules.suggestCategory(merchant)

        return SmsParseResult.Success(
            ParsedSmsTransaction(
                amount = amount,
                description = description,
                type = direction,
                transactionDate = date,
                accountLastFour = accountLast4,
                detectedBankName = bankInfo?.bankName,
                suggestedCategoryName = categoryName
            )
        )
    }

    // =========================================================================
    //  EXTRACTION HELPERS
    // =========================================================================

    /** Tries all amount patterns and returns the first valid Double found */
    fun extractAmount(body: String): Double? {
        for (pattern in BuiltInSmsRules.AMOUNT_PATTERNS) {
            val match = pattern.find(body) ?: continue
            val rawAmount = match.groupValues[1].replace(",", "")
            return rawAmount.toDoubleOrNull()
        }
        return null
    }

    /** Returns EXPENSE (debit) or INCOME (credit) based on keyword search */
    fun detectDirection(body: String): SmsTransactionType? {
        val lower = body.lowercase()
        // Check debit keywords first — they tend to be more specific
        val isDebit = BuiltInSmsRules.DEBIT_KEYWORDS.any { lower.contains(it) }
        val isCredit = BuiltInSmsRules.CREDIT_KEYWORDS.any { lower.contains(it) }

        return when {
            isDebit && !isCredit -> SmsTransactionType.EXPENSE
            isCredit && !isDebit -> SmsTransactionType.INCOME
            // Both detected (unusual but possible) → check which appeared first
            isDebit && isCredit -> {
                val debitIdx = BuiltInSmsRules.DEBIT_KEYWORDS
                    .mapNotNull { lower.indexOf(it).takeIf { i -> i >= 0 } }.minOrNull() ?: Int.MAX_VALUE
                val creditIdx = BuiltInSmsRules.CREDIT_KEYWORDS
                    .mapNotNull { lower.indexOf(it).takeIf { i -> i >= 0 } }.minOrNull() ?: Int.MAX_VALUE
                if (debitIdx < creditIdx) SmsTransactionType.EXPENSE else SmsTransactionType.INCOME
            }
            else -> null
        }
    }

    private fun detectDirectionWithKeywords(
        body: String,
        debitKeywords: List<String>,
        creditKeywords: List<String>
    ): SmsTransactionType? {
        val lower = body.lowercase()
        val isDebit = debitKeywords.any { it.isNotBlank() && lower.contains(it.lowercase()) }
        val isCredit = creditKeywords.any { it.isNotBlank() && lower.contains(it.lowercase()) }
        return when {
            isDebit -> SmsTransactionType.EXPENSE
            isCredit -> SmsTransactionType.INCOME
            else -> null
        }
    }

    /** Extracts the last 4 digits of the account/card number */
    fun extractAccountLastFour(body: String): String? {
        for (pattern in BuiltInSmsRules.ACCOUNT_LAST4_PATTERNS) {
            val match = pattern.find(body) ?: continue
            return match.groupValues[1]
        }
        return null
    }

    /**
     * Extracts a clean merchant/description from the SMS body.
     *
     * Strategy:
     *  1. Look for a keyword that introduces the merchant name (Info:, at, to, etc.)
     *  2. Extract text after that keyword
     *  3. Cut off at any "end delimiter" (Available bal, on date, etc.)
     *  4. Clean up UPI VPA formatting (merchant@upi → merchant)
     *  5. Title-case the result
     */
    fun extractMerchant(body: String): String {
        val lower = body.lowercase()

        for (keyword in BuiltInSmsRules.MERCHANT_AFTER_KEYWORDS) {
            val idx = lower.indexOf(keyword)
            if (idx < 0) continue

            var merchant = body.substring(idx + keyword.length).trim()

            // Cut off at any end delimiter
            for (delimiter in BuiltInSmsRules.MERCHANT_END_DELIMITERS) {
                val endIdx = merchant.lowercase().indexOf(delimiter)
                if (endIdx > 0) {
                    merchant = merchant.substring(0, endIdx).trim()
                }
            }

            // Remove trailing punctuation
            merchant = merchant.trimEnd('.', ',', ';', ':', '-', ' ')

            // Clean UPI VPA: "AMAZON@UPI" → "Amazon", "9876543210@PAYTM" → drops if it's a phone number
            if (merchant.contains("@")) {
                val vpaPart = merchant.substringBefore("@").trim()
                // Skip if it's a phone number (10 digits)
                if (!vpaPart.matches(Regex("""[0-9]{10}"""))) {
                    merchant = vpaPart
                }
            }

            // Skip short/noisy extractions
            if (merchant.length >= 2 && !merchant.matches(Regex("""[0-9]+"""))) {
                return merchant.capitalizeWords()
            }
        }

        // Fallback: return a shortened version of the SMS body
        return body.take(60).trimEnd()
    }

    /**
     * Parses the transaction date from the SMS body.
     * Falls back to the SMS received-at timestamp if no date is found.
     */
    fun extractDate(body: String, fallbackTimestamp: Long): Long {
        // Try DD-MM-YYYY or DD/MM/YYYY
        val numericPattern = Regex("""(\d{1,2})[/-](\d{1,2})[/-](\d{2,4})""")
        numericPattern.find(body)?.let { match ->
            val day = match.groupValues[1].toIntOrNull() ?: return@let
            val month = match.groupValues[2].toIntOrNull() ?: return@let
            var year = match.groupValues[3].toIntOrNull() ?: return@let
            if (year < 100) year += 2000 // 24 → 2024
            if (month in 1..12 && day in 1..31) {
                val cal = Calendar.getInstance().apply {
                    set(year, month - 1, day, 12, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                return cal.timeInMillis
            }
        }

        // Try DD-Mon-YYYY (e.g. 01-Jan-24)
        val monthNames = mapOf(
            "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4,
            "may" to 5, "jun" to 6, "jul" to 7, "aug" to 8,
            "sep" to 9, "oct" to 10, "nov" to 11, "dec" to 12
        )
        val textDatePattern = Regex(
            """(\d{1,2})[- ](Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*[- ](\d{2,4})""",
            RegexOption.IGNORE_CASE
        )
        textDatePattern.find(body)?.let { match ->
            val day = match.groupValues[1].toIntOrNull() ?: return@let
            val month = monthNames[match.groupValues[2].take(3).lowercase()] ?: return@let
            var year = match.groupValues[3].toIntOrNull() ?: return@let
            if (year < 100) year += 2000
            val cal = Calendar.getInstance().apply {
                set(year, month - 1, day, 12, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis
        }

        return fallbackTimestamp
    }

    /** Identifies which bank sent this SMS */
    fun detectBankInfo(sender: String): BuiltInSmsRules.BankSenderInfo? {
        val senderUpper = sender.uppercase()
        return BuiltInSmsRules.KNOWN_SENDERS.firstOrNull { bankInfo ->
            bankInfo.senderContains.any { senderUpper.contains(it.uppercase()) }
        }
    }

    // =========================================================================
    //  PRIVATE HELPERS
    // =========================================================================

    private val CREDIT_TYPE_VALUES = listOf("credit", "cr", "received", "income")
    private val DEBIT_TYPE_VALUES  = listOf("debit", "dr", "spent", "expense", "paid")

    private fun String.capitalizeWords(): String =
        split(" ").joinToString(" ") { word ->
            if (word.length > 1) word[0].uppercaseChar() + word.substring(1).lowercase()
            else word.uppercase()
        }
}