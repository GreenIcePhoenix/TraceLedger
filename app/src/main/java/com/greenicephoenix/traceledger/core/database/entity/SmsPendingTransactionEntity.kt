package com.greenicephoenix.traceledger.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a single SMS that has been parsed and is waiting for the user
 * to review it before it becomes a real Transaction.
 *
 * KEY FIELDS:
 *  - contentHash: MD5 of (sender + body). Used to prevent the same SMS from being
 *    queued twice if the broadcast fires more than once (can happen on some OEMs).
 *  - isProcessed: true once the user has accepted/rejected. Rows are kept for 30
 *    days then cleaned up, so the user can see history.
 *  - isAccepted: only meaningful when isProcessed = true.
 *  - accountLastFour: the 4-digit account suffix extracted from the SMS. We use this
 *    to suggest which TraceLedger account this transaction belongs to.
 */
@Entity(tableName = "sms_pending_transactions")
data class SmsPendingTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    /** The original system SMS ID — used for deduplication */
    val smsId: Long = -1L,

    /** Full original SMS text — shown to user during review */
    val smsBody: String,

    /** Sender ID, e.g. "VK-HDFCBK" */
    val sender: String,

    /** Unix timestamp (ms) when the SMS was received */
    val receivedAt: Long,

    // --- Parsed fields ---
    val parsedAmount: Double,
    val parsedDescription: String,
    /** "EXPENSE" or "INCOME" */
    val parsedType: String,
    /** Unix timestamp (ms) parsed from SMS date, or receivedAt as fallback */
    val parsedDate: Long,

    /** Suggested category from AutoCategorizer — null if no match */
    val suggestedCategoryId: Long? = null,

    /** Suggested account from last-4 matching — null if no match */
    val suggestedAccountId: Long? = null,

    /** Last 4 digits of account/card extracted from SMS */
    val accountLastFour: String? = null,

    /** True once user has reviewed (accepted or rejected) */
    val isProcessed: Boolean = false,

    /** True if user accepted this as a transaction */
    val isAccepted: Boolean = false,

    /**
     * Hash of (sender + smsBody) — prevents duplicate entries.
     * Using a simple concatenation hash is sufficient here.
     */
    val contentHash: String = "",

    val createdAt: Long = System.currentTimeMillis()
)