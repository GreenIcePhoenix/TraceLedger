package com.greenicephoenix.traceledger.core.database.dao

import androidx.room.*
import com.greenicephoenix.traceledger.core.database.entity.SmsPendingTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsPendingTransactionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: SmsPendingTransactionEntity): Long

    /** Live list of items not yet reviewed — drives the review screen */
    @Query("SELECT * FROM sms_pending_transactions WHERE isProcessed = 0 ORDER BY receivedAt DESC")
    fun observePending(): Flow<List<SmsPendingTransactionEntity>>

    /** Live count — drives dashboard badge and settings row */
    @Query("SELECT COUNT(*) FROM sms_pending_transactions WHERE isProcessed = 0")
    fun observePendingCount(): Flow<Int>

    /** Dedup check before inserting */
    @Query("SELECT COUNT(*) FROM sms_pending_transactions WHERE contentHash = :hash")
    suspend fun countByHash(hash: String): Int

    @Query("UPDATE sms_pending_transactions SET isProcessed = 1, isAccepted = :accepted WHERE id = :id")
    suspend fun markProcessed(id: Long, accepted: Boolean)

    @Query("UPDATE sms_pending_transactions SET isProcessed = 1, isAccepted = 0 WHERE isProcessed = 0")
    suspend fun rejectAll()

    /**
     * Called when the user edits a row before saving.
     * suggestedCategoryId / suggestedAccountId are String? (UUID) matching the app's ID type.
     */
    @Query("""
        UPDATE sms_pending_transactions
        SET suggestedCategoryId = :categoryId,
            suggestedAccountId  = :accountId
        WHERE id = :id
    """)
    suspend fun updateSuggestions(id: Long, categoryId: String?, accountId: String?)

    @Query("DELETE FROM sms_pending_transactions WHERE isProcessed = 1 AND createdAt < :cutoffMs")
    suspend fun deleteOldProcessed(cutoffMs: Long)
}