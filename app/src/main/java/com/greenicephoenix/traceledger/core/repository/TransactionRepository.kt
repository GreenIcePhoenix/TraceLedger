package com.greenicephoenix.traceledger.core.repository

import androidx.room.withTransaction
import com.greenicephoenix.traceledger.core.database.TraceLedgerDatabase
import com.greenicephoenix.traceledger.core.database.dao.AccountDao
import com.greenicephoenix.traceledger.core.database.dao.TransactionDao
import com.greenicephoenix.traceledger.core.database.entity.TransactionEntity
import com.greenicephoenix.traceledger.domain.model.TransactionType
import com.greenicephoenix.traceledger.domain.model.TransactionUiModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth

// ─────────────────────────────────────────────────────────────────────────────
// TransactionRepository
//
// RULES (non-negotiable):
// - All transaction writes MUST use the WithBalance variants.
//   These are atomic Room transactions — both the transaction insert
//   AND the account balance update happen together or not at all.
// - Raw insert/delete/update on the DAO must never be called from outside
//   this class. They are used only internally.
// - UI and ViewModels must never call DAO methods directly.
//
// FIX: Removed unsafe public insert() and delete() methods that bypassed
// balance logic. Any code that previously called those will now fail to
// compile, making the bug visible immediately.
// ─────────────────────────────────────────────────────────────────────────────
class TransactionRepository(
    private val database: TraceLedgerDatabase,
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao
) {

    // ── OBSERVE (reactive) ────────────────────────────────────────────────────

    /**
     * Observe ALL transactions — newest first.
     * Used by StatisticsViewModel and BudgetsViewModel which need complete history.
     *
     * For the History screen (month view), use observeTransactionsForMonth()
     * instead to avoid loading the entire table into memory.
     */
    fun observeTransactions(): Flow<List<TransactionUiModel>> {
        return transactionDao.observeAllTransactions()
            .map { entities -> entities.map { it.toUiModel() } }
    }

    /**
     * Observe transactions for a specific calendar month only.
     * This is far more efficient than observeTransactions() for the
     * History screen — avoids loading all-time data into memory.
     *
     * Use this in TransactionsViewModel.
     */
    fun observeTransactionsForMonth(month: YearMonth): Flow<List<TransactionUiModel>> {
        val startDate = month.atDay(1)               // e.g. 2026-03-01
        val endDate = month.atEndOfMonth()            // e.g. 2026-03-31
        return transactionDao.observeTransactionsForMonth(startDate, endDate)
            .map { entities -> entities.map { it.toUiModel() } }
    }

    // ── WRITES (atomic — always update balance together) ─────────────────────

    /**
     * Insert a new transaction AND update account balance(s) atomically.
     * Both operations succeed or both fail — no partial state possible.
     */
    suspend fun insertTransactionWithBalance(transaction: TransactionUiModel) {
        database.withTransaction {
            transactionDao.insertTransaction(transaction.toEntity())
            applyBalanceOnAdd(transaction)
        }
    }

    /**
     * Update an existing transaction AND re-apply balance changes atomically.
     * Sequence: reverse old balance → update transaction → apply new balance.
     */
    suspend fun updateTransactionWithBalance(updated: TransactionUiModel) {
        database.withTransaction {
            val oldTx = transactionDao.getTransactionById(updated.id)
                ?: return@withTransaction  // Transaction missing — nothing to update

            applyBalanceOnDelete(oldTx.toUiModel()) // Reverse old impact
            transactionDao.updateTransaction(updated.toEntity())
            applyBalanceOnAdd(updated)              // Apply new impact
        }
    }

    /**
     * Delete a transaction AND reverse its balance impact atomically.
     */
    suspend fun deleteTransactionWithBalance(transactionId: String) {
        database.withTransaction {
            val tx = transactionDao.getTransactionById(transactionId)
                ?: return@withTransaction  // Already deleted — safe to ignore

            applyBalanceOnDelete(tx.toUiModel()) // Reverse impact first
            transactionDao.deleteTransaction(transactionId)
        }
    }

    // ── READS (single, suspend) ───────────────────────────────────────────────

    /** Fetch a single transaction by ID. Returns null if not found. */
    suspend fun getTransactionById(transactionId: String): TransactionUiModel? {
        return transactionDao.getTransactionById(transactionId)?.toUiModel()
    }

    /**
     * Check if a transaction already exists for a given recurring rule + date.
     * Used by RecurringTransactionGenerator to prevent duplicate generation.
     */
    suspend fun getByRecurringAndDate(
        recurringId: String,
        date: LocalDate
    ): TransactionUiModel? {
        return transactionDao.getByRecurringAndDate(recurringId, date)?.toUiModel()
    }

    // ── UTILITY ───────────────────────────────────────────────────────────────

    /**
     * Run multiple operations inside a single Room transaction.
     * Used by RecurringTransactionGenerator to batch inserts atomically.
     */
    suspend fun runInTransaction(block: suspend () -> Unit) {
        database.withTransaction { block() }
    }

    // ── BALANCE LOGIC (private) ───────────────────────────────────────────────

    /**
     * Apply balance changes for a newly added transaction.
     *
     * EXPENSE  → fromAccount balance decreases
     * INCOME   → toAccount balance increases
     * TRANSFER → fromAccount decreases, toAccount increases
     *
     * Must ONLY be called on ADD, never on update or delete.
     */
    private suspend fun applyBalanceOnAdd(transaction: TransactionUiModel) {
        when (transaction.type) {
            TransactionType.EXPENSE -> {
                transaction.fromAccountId?.let {
                    accountDao.updateBalanceByDelta(it, transaction.amount.negate())
                }
            }
            TransactionType.INCOME -> {
                transaction.toAccountId?.let {
                    accountDao.updateBalanceByDelta(it, transaction.amount)
                }
            }
            TransactionType.TRANSFER -> {
                transaction.fromAccountId?.let {
                    accountDao.updateBalanceByDelta(it, transaction.amount.negate())
                }
                transaction.toAccountId?.let {
                    accountDao.updateBalanceByDelta(it, transaction.amount)
                }
            }
        }
    }

    /**
     * Reverse balance changes for a deleted (or replaced) transaction.
     * This is the exact inverse of applyBalanceOnAdd().
     *
     * Must ONLY be called on DELETE or before UPDATE.
     */
    private suspend fun applyBalanceOnDelete(transaction: TransactionUiModel) {
        when (transaction.type) {
            TransactionType.EXPENSE -> {
                transaction.fromAccountId?.let {
                    accountDao.updateBalanceByDelta(it, transaction.amount) // Reverse: add back
                }
            }
            TransactionType.INCOME -> {
                transaction.toAccountId?.let {
                    accountDao.updateBalanceByDelta(it, transaction.amount.negate()) // Reverse: subtract
                }
            }
            TransactionType.TRANSFER -> {
                transaction.fromAccountId?.let {
                    accountDao.updateBalanceByDelta(it, transaction.amount) // Reverse: add back to source
                }
                transaction.toAccountId?.let {
                    accountDao.updateBalanceByDelta(it, transaction.amount.negate()) // Reverse: remove from dest
                }
            }
        }
    }

    // ── MAPPERS (private) ─────────────────────────────────────────────────────

    private fun TransactionEntity.toUiModel() = TransactionUiModel(
        id = id,
        type = TransactionType.valueOf(type),
        amount = amount,
        date = date,
        fromAccountId = fromAccountId,
        toAccountId = toAccountId,
        categoryId = categoryId,
        note = note,
        createdAt = createdAt,
        recurringId = recurringId
    )

    private fun TransactionUiModel.toEntity() = TransactionEntity(
        id = id,
        type = type.name,
        amount = amount,
        date = date,
        fromAccountId = fromAccountId,
        toAccountId = toAccountId,
        categoryId = categoryId,
        note = note,
        createdAt = createdAt,
        recurringId = recurringId
    )
}