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

class TransactionRepository(
    private val database: TraceLedgerDatabase,
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao
) {
    /** Observe all transactions as UI models */
    fun observeTransactions(): Flow<List<TransactionUiModel>> {
        return transactionDao.observeAllTransactions()
            .map { entities ->
                entities.map { it.toUiModel() }
            }
    }

    /**
     * Atomically insert a transaction and apply its balance impact.
     */
    suspend fun insertTransactionWithBalance(transaction: TransactionUiModel) {
        database.withTransaction {
            transactionDao.insertTransaction(transaction.toEntity())
            applyBalanceOnAdd(transaction)
        }
    }


    /**
     * Atomically delete a transaction and reverse its balance impact.
     */
    suspend fun deleteTransactionWithBalance(transactionId: String) {
        database.withTransaction {
            val tx = transactionDao.getTransactionById(transactionId)
                ?: return@withTransaction

            applyBalanceOnDelete(tx.toUiModel())
            transactionDao.deleteTransaction(transactionId)
        }
    }

    /**
     * Atomically update a transaction and adjust balances.
     */
    suspend fun updateTransactionWithBalance(updated: TransactionUiModel) {
        database.withTransaction {
            val oldTx = transactionDao.getTransactionById(updated.id)
                ?: return@withTransaction

            applyBalanceOnDelete(oldTx.toUiModel())
            transactionDao.updateTransaction(updated.toEntity())
            applyBalanceOnAdd(updated)
        }
    }

    /** Insert transaction */
    suspend fun insert(transaction: TransactionUiModel) {
        transactionDao.insertTransaction(transaction.toEntity())
    }

    /** Delete transaction */
    suspend fun delete(transactionId: String) {
        transactionDao.deleteTransaction(transactionId)
    }

    /** Get a single transaction by id */
    suspend fun getTransactionById(transactionId: String): TransactionUiModel? {
        return transactionDao
            .getTransactionById(transactionId)
            ?.toUiModel()
    }

    /** Update an existing transaction */
    suspend fun update(transaction: TransactionUiModel) {
        transactionDao.updateTransaction(transaction.toEntity())
    }

    /**
     * Apply balance changes for a newly added transaction.
     * This MUST be called only on ADD (not update, not delete).
     */
    suspend fun applyBalanceOnAdd(transaction: TransactionUiModel) {
        when (transaction.type) {

            TransactionType.EXPENSE -> {
                transaction.fromAccountId?.let { accountId ->
                    accountDao.updateBalanceByDelta(
                        accountId = accountId,
                        delta = transaction.amount.negate()
                    )
                }
            }

            TransactionType.INCOME -> {
                transaction.toAccountId?.let { accountId ->
                    accountDao.updateBalanceByDelta(
                        accountId = accountId,
                        delta = transaction.amount
                    )
                }
            }

            TransactionType.TRANSFER -> {
                transaction.fromAccountId?.let { fromId ->
                    accountDao.updateBalanceByDelta(
                        accountId = fromId,
                        delta = transaction.amount.negate()
                    )
                }
                transaction.toAccountId?.let { toId ->
                    accountDao.updateBalanceByDelta(
                        accountId = toId,
                        delta = transaction.amount
                    )
                }
            }
        }
    }

    /**
     * Reverse balance changes for a deleted transaction.
     * This MUST be called only on DELETE.
     */
    suspend fun applyBalanceOnDelete(transaction: TransactionUiModel) {
        when (transaction.type) {

            TransactionType.EXPENSE -> {
                transaction.fromAccountId?.let { accountId ->
                    accountDao.updateBalanceByDelta(
                        accountId = accountId,
                        delta = transaction.amount
                    )
                }
            }

            TransactionType.INCOME -> {
                transaction.toAccountId?.let { accountId ->
                    accountDao.updateBalanceByDelta(
                        accountId = accountId,
                        delta = transaction.amount.negate()
                    )
                }
            }

            TransactionType.TRANSFER -> {
                transaction.fromAccountId?.let { fromId ->
                    accountDao.updateBalanceByDelta(
                        accountId = fromId,
                        delta = transaction.amount
                    )
                }
                transaction.toAccountId?.let { toId ->
                    accountDao.updateBalanceByDelta(
                        accountId = toId,
                        delta = transaction.amount.negate()
                    )
                }
            }
        }
    }


    // ---------------- MAPPERS ----------------

    private fun TransactionEntity.toUiModel(): TransactionUiModel {
        return TransactionUiModel(
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
    }

    private fun TransactionUiModel.toEntity(): TransactionEntity {
        return TransactionEntity(
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

    suspend fun getByRecurringAndDate(
        recurringId: String,
        date: LocalDate
    ): TransactionUiModel? {
        return transactionDao
            .getByRecurringAndDate(recurringId, date)
            ?.toUiModel()
    }

    suspend fun runInTransaction(block: suspend () -> Unit) {
        database.withTransaction {
            block()
        }
    }

}