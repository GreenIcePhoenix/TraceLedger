package com.greenicephoenix.traceledger.core.di

import android.content.Context
import com.greenicephoenix.traceledger.core.database.TraceLedgerDatabase
import com.greenicephoenix.traceledger.core.datastore.SettingsDataStore
import com.greenicephoenix.traceledger.core.export.ExportService
import com.greenicephoenix.traceledger.core.importer.ImportService
import com.greenicephoenix.traceledger.core.recurring.RecurringTransactionGenerator
import com.greenicephoenix.traceledger.core.repository.AccountRepository
import com.greenicephoenix.traceledger.core.repository.CategoryRepository
import com.greenicephoenix.traceledger.core.repository.RecurringTransactionRepository
import com.greenicephoenix.traceledger.core.repository.TransactionRepository
import com.greenicephoenix.traceledger.feature.accounts.AccountsViewModelFactory
import com.greenicephoenix.traceledger.feature.budgets.data.BudgetRepository
import com.greenicephoenix.traceledger.feature.categories.CategoriesViewModelFactory
import com.greenicephoenix.traceledger.feature.dashboard.DashboardViewModelFactory
import com.greenicephoenix.traceledger.feature.recurring.AddEditRecurringViewModelFactory
import com.greenicephoenix.traceledger.feature.recurring.RecurringTransactionsViewModelFactory
import com.greenicephoenix.traceledger.feature.statistics.StatisticsViewModelFactory
import com.greenicephoenix.traceledger.feature.templates.data.TemplateRepository
import com.greenicephoenix.traceledger.feature.templates.TemplatesViewModelFactory

class AppContainer(context: Context) {

    private val database = TraceLedgerDatabase.getInstance(context)

    val settingsDataStore: SettingsDataStore = SettingsDataStore(context)

    val accountRepository: AccountRepository =
        AccountRepository(database.accountDao())

    val transactionRepository: TransactionRepository =
        TransactionRepository(
            database       = database,
            transactionDao = database.transactionDao(),
            accountDao     = database.accountDao()
        )

    val categoryRepository: CategoryRepository =
        CategoryRepository(database.categoryDao())

    val budgetRepository: BudgetRepository by lazy {
        BudgetRepository(database.budgetDao())
    }

    val recurringTransactionRepository: RecurringTransactionRepository by lazy {
        RecurringTransactionRepository(
            recurringDao   = database.recurringTransactionDao(),
            transactionDao = database.transactionDao()
        )
    }

    val templateRepository: TemplateRepository by lazy {
        TemplateRepository(database.transactionTemplateDao())
    }

    val exportService by lazy {
        ExportService(database = database, contentResolver = context.contentResolver)
    }

    val importService by lazy {
        ImportService(database = database, contentResolver = context.contentResolver)
    }

    val recurringGenerator: RecurringTransactionGenerator by lazy {
        RecurringTransactionGenerator(
            recurringRepository   = recurringTransactionRepository,
            transactionRepository = transactionRepository
        )
    }

    val accountsViewModelFactory   = AccountsViewModelFactory(accountRepository)
    val categoriesViewModelFactory = CategoriesViewModelFactory(categoryRepository)
    val statisticsViewModelFactory = StatisticsViewModelFactory(transactionRepository)

    val dashboardViewModelFactory = DashboardViewModelFactory(
        transactionRepository = transactionRepository,
        recurringRepository   = recurringTransactionRepository
    )

    val recurringViewModelFactory  = RecurringTransactionsViewModelFactory(recurringTransactionRepository)
    val addEditRecurringFactory    = AddEditRecurringViewModelFactory(recurringTransactionRepository)

    val templatesViewModelFactory by lazy {
        TemplatesViewModelFactory(templateRepository)
    }
}