package com.greenicephoenix.traceledger.feature.sms.model

/**
 * The output of a successful SMS parse.
 *
 * This is a pure data class with no Room dependency.
 * The SmsRuleEngine produces it; the repository converts it
 * to a SmsPendingTransactionEntity before saving to DB.
 */
data class ParsedSmsTransaction(
    val amount: Double,
    val description: String,
    /** EXPENSE or INCOME — we represent transfers as EXPENSE from sender's perspective */
    val type: SmsTransactionType,
    /** Parsed from SMS date text; falls back to SMS received-at timestamp */
    val transactionDate: Long,
    val accountLastFour: String?,
    val detectedBankName: String?,
    val suggestedCategoryName: String? = null,
)

enum class SmsTransactionType { EXPENSE, INCOME }