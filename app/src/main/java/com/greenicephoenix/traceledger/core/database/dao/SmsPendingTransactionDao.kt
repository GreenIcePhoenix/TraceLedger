package com.greenicephoenix.traceledger.core.database.dao

import androidx.room.*
import com.greenicephoenix.traceledger.core.database.entity.SmsPendingTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsPendingTransactionDao {

    // --- Insert ---

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: SmsPendingTransactionEntity): Long

    // --- Queries ---

    /** All unprocessed items — shown on the review screen */
    @Query("SELECT * FROM sms_pending_transactions WHERE isProcessed = 0 ORDER BY receivedAt DESC")
    fun observePending(): Flow<List<SmsPendingTransactionEntity>>

    /** Count of pending (unprocessed) — used for the dashboard badge */
    @Query("SELECT COUNT(*) FROM sms_pending_transactions WHERE isProcessed = 0")
    fun observePendingCount(): Flow<Int>

    /** Check for duplicates before inserting — prevents the same SMS twice */
    @Query("SELECT COUNT(*) FROM sms_pending_transactions WHERE contentHash = :hash")
    suspend fun countByHash(hash: String): Int

    // --- Update ---

    /** Mark a single item as reviewed */
    @Query("""
        UPDATE sms_pending_transactions
        SET isProcessed = 1, isAccepted = :accepted
        WHERE id = :id
    """)
    suspend fun markProcessed(id: Long, accepted: Boolean)

    /** Mark ALL pending items as rejected (for "dismiss all" action) */
    @Query("UPDATE sms_pending_transactions SET isProcessed = 1, isAccepted = 0 WHERE isProcessed = 0")
    suspend fun rejectAll()

    /** Update parsed fields after user edits a row on the review screen */
    @Query("""
        UPDATE sms_pending_transactions
        SET parsedAmount = :amount,
            parsedDescription = :description,
            parsedType = :type,
            suggestedCategoryId = :categoryId,
            suggestedAccountId = :accountId,
            parsedDate = :date
        WHERE id = :id
    """)
    suspend fun updateParsedFields(
        id: Long,
        amount: Double,
        description: String,
        type: String,
        categoryId: Long?,
        accountId: Long?,
        date: Long
    )

    // --- Cleanup ---

    /** Remove processed entries older than 30 days */
    @Query("""
        DELETE FROM sms_pending_transactions
        WHERE isProcessed = 1 AND createdAt < :cutoffMs
    """)
    suspend fun deleteOldProcessed(cutoffMs: Long)
}