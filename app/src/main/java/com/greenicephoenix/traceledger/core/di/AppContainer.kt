package com.greenicephoenix.traceledger.core.di

import android.content.Context
import com.greenicephoenix.traceledger.core.database.TraceLedgerDatabase
import com.greenicephoenix.traceledger.core.export.ExportService
import com.greenicephoenix.traceledger.core.importer.ImportService
import com.greenicephoenix.traceledger.core.repository.AccountRepository
import com.greenicephoenix.traceledger.core.repository.TransactionRepository
import com.greenicephoenix.traceledger.feature.statistics.StatisticsViewModelFactory
import com.greenicephoenix.traceledger.feature.budgets.data.BudgetRepository
import com.greenicephoenix.traceledger.core.repository.CategoryRepository
import com.greenicephoenix.traceledger.feature.categories.CategoriesViewModelFactory
import com.greenicephoenix.traceledger.core.datastore.SettingsDataStore
import com.greenicephoenix.traceledger.core.repository.RecurringTransactionRepository
import com.greenicephoenix.traceledger.core.recurring.RecurringTransactionGenerator
import com.greenicephoenix.traceledger.feature.recurring.RecurringTransactionsViewModelFactory
import com.greenicephoenix.traceledger.feature.recurring.AddEditRecurringViewModelFactory

class AppContainer(context: Context) {

    private val database = TraceLedgerDatabase.getInstance(context)

    val settingsDataStore: SettingsDataStore =
        SettingsDataStore(context)

    val accountRepository: AccountRepository =
        AccountRepository(database.accountDao())

    val transactionRepository: TransactionRepository =
        TransactionRepository(
            database = database,
            transactionDao = database.transactionDao(),
            accountDao = database.accountDao()
        )

    val categoryRepository: CategoryRepository =
        CategoryRepository(database.categoryDao())

    val categoriesViewModelFactory =
        CategoriesViewModelFactory(categoryRepository)


    val budgetRepository: BudgetRepository by lazy {
        BudgetRepository(
            database.budgetDao()
        )
    }

    val statisticsViewModelFactory =
        StatisticsViewModelFactory(transactionRepository)

    val exportService by lazy {
        ExportService(
            database = database,
            contentResolver = context.contentResolver
        )
    }

    val importService by lazy {
        ImportService(
            database = database,
            contentResolver = context.contentResolver
        )
    }

    val recurringTransactionRepository: RecurringTransactionRepository by lazy {
        RecurringTransactionRepository(
            recurringDao = database.recurringTransactionDao(),
            transactionDao = database.transactionDao()
        )
    }

    val recurringGenerator: RecurringTransactionGenerator by lazy {
        RecurringTransactionGenerator(
            recurringRepository = recurringTransactionRepository,
            transactionRepository = transactionRepository
        )
    }

    val recurringViewModelFactory =
        RecurringTransactionsViewModelFactory(
            recurringTransactionRepository
        )

    val addEditRecurringFactory =
        AddEditRecurringViewModelFactory(
            recurringTransactionRepository
        )

}