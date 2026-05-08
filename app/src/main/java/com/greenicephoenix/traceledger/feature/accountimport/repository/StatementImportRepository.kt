// ─────────────────────────────────────────────────────────────────────────────
// FILE: feature/accountimport/repository/StatementImportRepository.kt
//
// What this does:
//   Handles all database operations for the statement import feature.
//   Kept separate from TransactionRepository intentionally — import has
//   fundamentally different write semantics (bulk, bypassing balance logic
//   in some strategies) that don't belong in the core repository.
//
// Three write strategies matching BalanceStrategy.kt:
//   KEEP_EXISTING       → bulkInsertRecordsOnly()   (no balance change)
//   SET_TO_STATEMENT    → bulkInsertRecordsOnly() + adjustBalance()
//   RECALCULATE_FROM_ALL → bulkInsertWithBalanceUpdates()
// ─────────────────────────────────────────────────────────────────────────────
package com.greenicephoenix.traceledger.feature.accountimport.repository

import androidx.room.withTransaction
import com.greenicephoenix.traceledger.core.database.TraceLedgerDatabase
import com.greenicephoenix.traceledger.core.database.dao.AccountDao
import com.greenicephoenix.traceledger.core.database.dao.TransactionDao
import com.greenicephoenix.traceledger.core.database.entity.TransactionEntity
import com.greenicephoenix.traceledger.domain.model.TransactionType
import com.greenicephoenix.traceledger.domain.model.TransactionUiModel
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Instant

class StatementImportRepository(
    private val database: TraceLedgerDatabase,
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao
) {

    // ── Duplicate detection ───────────────────────────────────────────────────

    /**
     * Fetch all transactions touching [accountId] within the date range
     * [startDate] to [endDate] (inclusive).
     *
     * The caller (StatementImportViewModel) uses this to build an in-memory
     * lookup table and checks each import row against it.
     *
     * Why not check per-row in the DB? Batch-fetching once and checking in
     * memory is far faster than N individual queries for N imported rows.
     */
    suspend fun getExistingTransactionsInRange(
        accountId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<TransactionEntity> {
        return transactionDao.getTransactionsForAccountInRange(accountId, startDate, endDate)
    }

    /**
     * Count how many transactions already exist for this account.
     * Used by the ViewModel to pre-select the balance strategy:
     *   0 transactions → pre-select RECALCULATE
     *   > 0            → pre-select KEEP_EXISTING
     */
    suspend fun getExistingTransactionCount(accountId: String): Int {
        // We query a wide date range to count ALL transactions for the account.
        // This is intentionally a full scan — it only runs once on the import hub.
        val all = transactionDao.getTransactionsForAccountInRange(
            accountId  = accountId,
            startDate  = LocalDate.of(2000, 1, 1),
            endDate    = LocalDate.of(2100, 12, 31)
        )
        return all.size
    }

    // ── Write strategy 1: KEEP_EXISTING ──────────────────────────────────────

    /**
     * Insert transactions as records only — no balance updates.
     *
     * Use this for KEEP_EXISTING strategy: the account balance is already
     * correct; we're adding historical records for analytics/reference only.
     *
     * All inserts run inside a single Room transaction — either all succeed
     * or none do. Returns the number of rows inserted.
     *
     * @param transactions  List of entities ready to insert.
     * @param onProgress    Called with 0..100 to update UI progress indicator.
     */
    suspend fun bulkInsertRecordsOnly(
        transactions: List<TransactionEntity>,
        onProgress: (Int) -> Unit = {}
    ): Int {
        if (transactions.isEmpty()) return 0
        database.withTransaction {
            // Batch in chunks of 50 so progress updates feel smooth
            transactions.chunked(50).forEachIndexed { chunkIndex, chunk ->
                transactionDao.insertTransactions(chunk)
                val progress = ((chunkIndex + 1) * 50.coerceAtMost(transactions.size))
                    .times(100)
                    .div(transactions.size)
                onProgress(progress)
            }
        }
        onProgress(100)
        return transactions.size
    }

    // ── Write strategy 2: SET_TO_STATEMENT ───────────────────────────────────

    /**
     * Insert records without balance delta, then adjust the account balance
     * to match the user-provided [targetBalance] from the statement.
     *
     * @param transactions    Entities to insert.
     * @param accountId       Account whose balance we'll update.
     * @param currentBalance  The account's current balance in TraceLedger.
     * @param targetBalance   The closing balance from the last page of the statement.
     * @param onProgress      Progress callback 0..100.
     */
    suspend fun bulkInsertAndSetBalance(
        transactions:   List<TransactionEntity>,
        accountId:      String,
        currentBalance: BigDecimal,
        targetBalance:  BigDecimal,
        onProgress:     (Int) -> Unit = {}
    ): Int {
        if (transactions.isEmpty()) return 0
        database.withTransaction {
            transactions.chunked(50).forEachIndexed { chunkIndex, chunk ->
                transactionDao.insertTransactions(chunk)
                val progress = ((chunkIndex + 1) * 50.coerceAtMost(transactions.size))
                    .times(100)
                    .div(transactions.size)
                    .coerceAtMost(90)   // reserve last 10% for balance update
                onProgress(progress)
            }
            // Adjust balance: delta = targetBalance - currentBalance
            // updateBalanceByDelta adds the delta to the existing balance.
            val delta = targetBalance.subtract(currentBalance)
            accountDao.updateBalanceByDelta(accountId, delta)
        }
        onProgress(100)
        return transactions.size
    }

    // ── Write strategy 3: RECALCULATE_FROM_ALL ───────────────────────────────

    /**
     * Insert transactions WITH balance updates applied atomically.
     * Use only for fresh accounts (zero existing transactions).
     *
     * Each transaction updates the account balance just like a normal add —
     * EXPENSE subtracts, INCOME adds, TRANSFER moves between accounts.
     *
     * @param transactions  Full UiModels (needed for balance direction logic).
     * @param onProgress    Progress callback 0..100.
     */
    suspend fun bulkInsertWithBalanceUpdates(
        transactions: List<TransactionUiModel>,
        onProgress:   (Int) -> Unit = {}
    ): Int {
        if (transactions.isEmpty()) return 0
        database.withTransaction {
            transactions.forEachIndexed { index, tx ->
                transactionDao.insertTransaction(tx.toEntity())
                applyBalanceDelta(tx)
                val progress = ((index + 1) * 100) / transactions.size
                onProgress(progress)
            }
        }
        return transactions.size
    }

    // ── Balance delta (mirrors TransactionRepository logic) ──────────────────

    private suspend fun applyBalanceDelta(transaction: TransactionUiModel) {
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

    // ── Entity builder (used by ViewModel) ───────────────────────────────────

    /**
     * Build a [TransactionEntity] from import review data.
     * Called by the ViewModel for each included [ ImportReviewItem ].
     *
     * @param accountId   The account this statement belongs to.
     * @param item        The reviewed transaction data.
     * @param isCredit    true = INCOME (money in), false = EXPENSE (money out).
     */
    fun buildEntity(
        accountId: String,
        amount:    BigDecimal,
        date:      LocalDate,
        note:      String,
        isCredit:  Boolean,
        categoryId: String?
    ): TransactionEntity {
        return TransactionEntity(
            id            = java.util.UUID.randomUUID().toString(),
            type          = if (isCredit) TransactionType.INCOME.name else TransactionType.EXPENSE.name,
            amount        = amount,
            date          = date,
            // INCOME: money arrives INTO the account → toAccountId
            // EXPENSE: money leaves FROM the account → fromAccountId
            fromAccountId = if (!isCredit) accountId else null,
            toAccountId   = if (isCredit)  accountId else null,
            categoryId    = categoryId,
            note          = note,
            createdAt     = Instant.now(),
            recurringId   = null   // imported transactions are never recurring
        )
    }

    // ── Mapper used by bulkInsertWithBalanceUpdates ───────────────────────────

    private fun TransactionUiModel.toEntity() = TransactionEntity(
        id            = id,
        type          = type.name,
        amount        = amount,
        date          = date,
        fromAccountId = fromAccountId,
        toAccountId   = toAccountId,
        categoryId    = categoryId,
        note          = note,
        createdAt     = createdAt,
        recurringId   = recurringId
    )
}