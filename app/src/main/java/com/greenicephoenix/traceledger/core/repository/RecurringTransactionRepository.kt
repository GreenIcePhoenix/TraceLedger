package com.greenicephoenix.traceledger.core.repository

import com.greenicephoenix.traceledger.core.database.dao.RecurringTransactionDao
import com.greenicephoenix.traceledger.core.database.dao.TransactionDao
import com.greenicephoenix.traceledger.core.database.entity.RecurringTransactionEntity
import kotlinx.coroutines.flow.Flow

class RecurringTransactionRepository(
    private val recurringDao: RecurringTransactionDao,
    private val transactionDao: TransactionDao
) {

    fun getAllRecurring(): Flow<List<RecurringTransactionEntity>> =
        recurringDao.getAll()

    suspend fun insert(recurring: RecurringTransactionEntity) =
        recurringDao.insert(recurring)

    suspend fun update(recurring: RecurringTransactionEntity) =
        recurringDao.update(recurring)

    suspend fun delete(recurring: RecurringTransactionEntity) =
        recurringDao.delete(recurring)

    suspend fun getActiveRecurring(today: String): List<RecurringTransactionEntity> =
        recurringDao.getActiveRecurring(today)

    suspend fun getById(id: String): RecurringTransactionEntity? =
        recurringDao.getById(id)

}