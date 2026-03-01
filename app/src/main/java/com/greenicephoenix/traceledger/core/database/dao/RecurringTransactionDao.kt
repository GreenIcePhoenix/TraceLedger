package com.greenicephoenix.traceledger.core.database.dao

import androidx.room.*
import com.greenicephoenix.traceledger.core.database.entity.RecurringTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recurring: RecurringTransactionEntity)

    @Update
    suspend fun update(recurring: RecurringTransactionEntity)

    @Delete
    suspend fun delete(recurring: RecurringTransactionEntity)

    @Query("SELECT * FROM recurring_transactions ORDER BY startDate ASC")
    fun getAll(): Flow<List<RecurringTransactionEntity>>

    @Query(
        """
        SELECT * FROM recurring_transactions
        WHERE endDate IS NULL OR endDate >= :today
        """
    )
    suspend fun getActiveRecurring(today: String): List<RecurringTransactionEntity>

    @Query("SELECT * FROM recurring_transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): RecurringTransactionEntity?

}