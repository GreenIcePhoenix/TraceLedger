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
//
// FIX: Removed duplicate methods that previously existed:
//   - insert()          was a duplicate of insertTransaction()
//   - deleteAll()       was a duplicate of deleteAllTransactions()
// These duplicates created confusion about which to call, and insert() was
// missing OnConflictStrategy which caused crashes on duplicate primary keys.
// ─────────────────────────────────────────────────────────────────────────────
@Dao
interface TransactionDao {

    // ── OBSERVE (reactive, for UI) ────────────────────────────────────────────

    /**
     * Observe ALL transactions ordered newest-first.
     * Used by StatisticsViewModel and BudgetsViewModel which need full history.
     * For month-filtered use cases, prefer observeTransactionsForMonth().
     */
    @Query("SELECT * FROM transactions ORDER BY date DESC, createdAt DESC")
    fun observeAllTransactions(): Flow<List<TransactionEntity>>

    /**
     * Observe transactions filtered to a specific date range.
     * Use this in HistoryScreen / TransactionsViewModel to avoid loading
     * the full transaction table into memory.
     *
     * Example: pass startDate = 2026-03-01, endDate = 2026-03-31
     */
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

    /**
     * Used by RecurringTransactionGenerator to check if a transaction
     * for a specific recurring rule + date already exists.
     * Prevents duplicate generation when the app is opened multiple times.
     */
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

    /**
     * Fetch all transactions once (non-reactive).
     * Used by ExportService to snapshot data for export.
     */
    @Query("SELECT * FROM transactions ORDER BY date ASC")
    suspend fun getAllOnce(): List<TransactionEntity>

    // ── WRITES ────────────────────────────────────────────────────────────────

    /**
     * Insert a transaction.
     * REPLACE strategy: if a transaction with the same ID somehow exists,
     * it will be replaced. This is safe because IDs are now UUIDs.
     *
     * IMPORTANT: Call TransactionRepository.insertTransactionWithBalance()
     * instead of this directly — the repository also updates account balances.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    /**
     * Update an existing transaction in place.
     *
     * IMPORTANT: Call TransactionRepository.updateTransactionWithBalance()
     * instead of this directly — the repository handles balance reversal + re-apply.
     */
    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    /**
     * Delete a single transaction by ID.
     *
     * IMPORTANT: Call TransactionRepository.deleteTransactionWithBalance()
     * instead of this directly — the repository reverses the balance impact first.
     */
    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteTransaction(transactionId: String)

    /**
     * Delete ALL transactions.
     * Only used by ImportService when replacing all data during import.
     * Never call this from UI.
     */
    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
}