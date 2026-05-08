package com.greenicephoenix.traceledger.feature.sms.model

/**
 * Three possible outcomes when the rule engine processes an SMS:
 *
 *  Success  — we found a financial transaction; ParsedSmsTransaction is ready.
 *  NotFinancial — the SMS is not a transaction (OTP, promo, news alert, etc.). Drop it.
 *  ParseError   — it looks financial but we couldn't extract a valid amount. Log it.
 */
sealed class SmsParseResult {
    data class Success(val transaction: ParsedSmsTransaction) : SmsParseResult()
    object NotFinancial : SmsParseResult()
    data class ParseError(val reason: String) : SmsParseResult()
}