package com.greenicephoenix.traceledger.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.greenicephoenix.traceledger.core.database.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

// ─────────────────────────────────────────────────────────────────────────────
// TransactionDao
//
// RULES (non-negotiable):
// - This interface contains ONLY SQL queries.
// - Business logic (balance updates, validation) belongs in TransactionRepository.
// - Never call these methods directly from UI or ViewModel.
// ─────────────────────────────────────────────────────────────────────────────
@Dao
interface TransactionDao {

    // ── OBSERVE (reactive, for UI) ────────────────────────────────────────────

    @Query("SELECT * FROM transactions ORDER BY date DESC, createdAt DESC")
    fun observeAllTransactions(): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions
        WHERE date >= :startDate AND date <= :endDate
        ORDER BY date DESC, createdAt DESC
    """)
    fun observeTransactionsForMonth(
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<TransactionEntity>>

    // ── SINGLE READS (suspend, for one-off lookups) ───────────────────────────

    @Query("SELECT * FROM transactions WHERE id = :transactionId LIMIT 1")
    suspend fun getTransactionById(transactionId: String): TransactionEntity?

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

    @Query("SELECT * FROM transactions ORDER BY date ASC")
    suspend fun getAllOnce(): List<TransactionEntity>

    // ── v1.3.0: Statement Import — duplicate detection ─────────────────────────
    //
    // Fetches all transactions that touch a specific account within a date range.
    // Used by StatementImportRepository to build a lookup table for duplicate
    // detection — we check each imported row against this list before marking
    // it as a potential duplicate.
    //
    // We query by BOTH fromAccountId and toAccountId because:
    //   - EXPENSE transactions only have fromAccountId set
    //   - INCOME transactions only have toAccountId set
    //   - TRANSFER transactions have both
    // A statement for "ICICI account" should check rows where ICICI is
    // on either side of the transaction.
    @Query("""
        SELECT * FROM transactions
        WHERE (fromAccountId = :accountId OR toAccountId = :accountId)
        AND date >= :startDate AND date <= :endDate
        ORDER BY date ASC
    """)
    suspend fun getTransactionsForAccountInRange(
        accountId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<TransactionEntity>

    // ── WRITES ────────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    // v1.3.0: Bulk insert for statement import — REPLACE strategy means
    // re-importing the same statement won't create duplicates by ID
    // (though we show duplicate warnings by date+amount in the review screen).
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteTransaction(transactionId: String)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
}