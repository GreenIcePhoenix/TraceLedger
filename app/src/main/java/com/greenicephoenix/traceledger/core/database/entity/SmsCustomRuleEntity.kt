package com.greenicephoenix.traceledger.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user-defined SMS parsing rule.
 *
 * TWO MODES:
 *  1. Simple (isAdvancedMode = false):
 *     Uses senderPattern, amountPrefix, debitKeywords, creditKeywords, merchantRegex.
 *     The rule engine builds a regex from these parts.
 *     Example: sender="HDFCBK", amountPrefix="Rs.", debitKeyword="debited"
 *
 *  2. Advanced (isAdvancedMode = true):
 *     Uses rawRegex — a full Java/Kotlin regex with named groups:
 *     (?P<amount>...) (?P<merchant>...) (?P<type>...)
 *     This mode is for power users.
 *
 * PRIORITY:
 *  Built-in rules have priority 0–9. Custom rules default to priority 10
 *  so they always win. User can increase priority further.
 */
@Entity(tableName = "sms_custom_rules")
data class SmsCustomRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    /** Human-readable name, e.g. "My office canteen wallet" */
    val name: String,

    /** The sender ID must CONTAIN this string (case-insensitive) */
    val senderPattern: String,

    /** Simple mode: keyword that appears just before the amount, e.g. "Rs." or "INR" */
    val amountPrefix: String = "",

    /** Comma-separated keywords that indicate a debit/expense, e.g. "debited,spent,paid" */
    val debitKeywords: String = "",

    /** Comma-separated keywords that indicate a credit/income, e.g. "credited,received" */
    val creditKeywords: String = "",

    /** Regex/keyword to extract the merchant name after */
    val merchantRegex: String = "",

    /** Pre-set category for all transactions matching this rule */
    val defaultCategoryId: Long? = null,

    /** Pre-set account for all transactions matching this rule */
    val defaultAccountId: Long? = null,

    val isEnabled: Boolean = true,

    /** Higher number = checked first. Defaults to 10 (beats all built-in rules at 0–9) */
    val priority: Int = 10,

    /** If true, use rawRegex instead of the simple fields */
    val isAdvancedMode: Boolean = false,

    /** Full regex pattern with named groups for advanced mode */
    val rawRegex: String = "",

    val createdAt: Long = System.currentTimeMillis()
)