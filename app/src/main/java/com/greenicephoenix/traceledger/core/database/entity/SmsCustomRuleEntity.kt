package com.greenicephoenix.traceledger.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_custom_rules")
data class SmsCustomRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    /** Human-readable label e.g. "My office canteen wallet" */
    val name: String,

    /** The sender ID must CONTAIN this string (case-insensitive) */
    val senderPattern: String,

    /** Keyword just before the amount, e.g. "Rs." or "INR" */
    val amountPrefix: String = "",

    /** Comma-separated debit indicators, e.g. "debited,spent" */
    val debitKeywords: String = "",

    /** Comma-separated credit indicators, e.g. "credited,received" */
    val creditKeywords: String = "",

    /** Keyword after which the merchant name follows, e.g. "Info:" */
    val merchantRegex: String = "",

    /** String UUID — pre-selects a category on the review screen */
    val defaultCategoryId: String? = null,   // ← String UUID, not Long

    /** String UUID — pre-selects an account on the review screen */
    val defaultAccountId: String? = null,    // ← String UUID, not Long

    val isEnabled: Boolean = true,

    /** Higher = checked first. Custom rules default to 10, beating all built-in rules. */
    val priority: Int = 10,

    /** If true, ignore parsing fields — use rawRegex (Phase 2) */
    val isAdvancedMode: Boolean = false,

    val rawRegex: String = "",

    /**
     * If true, any SMS from the matching sender is immediately discarded.
     * Overrides all parsing fields. Used to silence senders that only send
     * statements, spam, or alerts you never want in the ledger.
     */
    val isExclusionRule: Boolean = false,

    val createdAt: Long = System.currentTimeMillis()
)