package com.greenicephoenix.traceledger.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One SMS that parsed successfully and is waiting for user review.
 * ID types match the rest of the app — String UUIDs, not Longs.
 * Amount stored as Double (sufficient precision for SMS-parsed values;
 * converted to BigDecimal when writing the final TransactionEntity).
 */
@Entity(tableName = "sms_pending_transactions")
data class SmsPendingTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    /** Original system SMS ID — used only for deduplication on inbox scan */
    val smsId: Long = -1L,

    /** Full original SMS text shown to user during review */
    val smsBody: String,

    val sender: String,

    /** Unix ms when SMS was received — used as fallback transaction date */
    val receivedAt: Long,

    // ── Parsed fields ────────────────────────────────────────────────────────

    val parsedAmount: Double,
    val parsedDescription: String,

    /** "EXPENSE" or "INCOME" — matches TransactionType.name */
    val parsedType: String,

    /** Unix ms parsed from SMS date text; falls back to receivedAt */
    val parsedDate: Long,

    // ── Suggestions (String UUIDs matching the rest of the app) ─────────────

    /** Suggested category id from AutoCategorizer — null if no match */
    val suggestedCategoryId: String? = null,

    /** Suggested account id — null (no account number stored in AccountEntity) */
    val suggestedAccountId: String? = null,

    /** Last 4 digits extracted from SMS — informational only for display */
    val accountLastFour: String? = null,

    val isProcessed: Boolean = false,
    val isAccepted: Boolean = false,

    /** MD5 of (sender + body) — prevents duplicate queue entries */
    val contentHash: String = "",

    val createdAt: Long = System.currentTimeMillis()
)