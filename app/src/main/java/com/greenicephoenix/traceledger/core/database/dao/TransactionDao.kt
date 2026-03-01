package com.greenicephoenix.traceledger.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.greenicephoenix.traceledger.core.database.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import androidx.room.Update
import java.time.LocalDate

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY date DESC, createdAt DESC")
    fun observeAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :transactionId LIMIT 1")
    suspend fun getTransactionById(transactionId: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteTransaction(transactionId: String)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions ORDER BY date ASC")
    suspend fun getAllOnce(): List<TransactionEntity>

    @Insert
    suspend fun insert(entity: TransactionEntity)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()

    @Query("""
        SELECT * FROM transactions
        WHERE recurringId = :recurringId
        AND date = :date
        LIMIT 1
    """)
    suspend fun getByRecurringAndDate(
        recurringId: String,
        date: LocalDate
    ): TransactionEntity?

}