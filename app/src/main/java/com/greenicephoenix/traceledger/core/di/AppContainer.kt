package com.greenicephoenix.traceledger.core.di

import android.content.Context
import com.greenicephoenix.traceledger.core.database.TraceLedgerDatabase
import com.greenicephoenix.traceledger.core.export.ExportService
import com.greenicephoenix.traceledger.core.importer.ImportService
import com.greenicephoenix.traceledger.core.repository.AccountRepository
import com.greenicephoenix.traceledger.core.repository.TransactionRepository
import com.greenicephoenix.traceledger.core.repository.CategoryRepository
import com.greenicephoenix.traceledger.core.repository.RecurringTransactionRepository
import com.greenicephoenix.traceledger.core.datastore.SettingsDataStore
import com.greenicephoenix.traceledger.core.recurring.RecurringTransactionGenerator
import com.greenicephoenix.traceledger.feature.accounts.AccountsViewModelFactory       // FIX: new
import com.greenicephoenix.traceledger.feature.budgets.data.BudgetRepository
import com.greenicephoenix.traceledger.feature.categories.CategoriesViewModelFactory
import com.greenicephoenix.traceledger.feature.recurring.AddEditRecurringViewModelFactory
import com.greenicephoenix.traceledger.feature.recurring.RecurringTransactionsViewModelFactory
import com.greenicephoenix.traceledger.feature.statistics.StatisticsViewModelFactory

// ─────────────────────────────────────────────────────────────────────────────
// AppContainer — Manual dependency injection container
//
// All repositories and ViewModel factories are created here once
// and reused across the app. This avoids re-creating expensive objects
// like the database or repositories on every screen.
//
// Lazy initialization is used for services that aren't always needed
// (e.g. export/import only run when the user explicitly triggers them).
//
// This manual DI approach is intentional and appropriate for this app's size.
// Hilt would add complexity without meaningful benefit until the codebase
// grows significantly larger.
// ─────────────────────────────────────────────────────────────────────────────
class AppContainer(context: Context) {

    // ── Database (singleton) ──────────────────────────────────────────────────
    private val database = TraceLedgerDatabase.getInstance(context)

    // ── Settings ──────────────────────────────────────────────────────────────
    val settingsDataStore: SettingsDataStore = SettingsDataStore(context)

    // ── Repositories ──────────────────────────────────────────────────────────

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

    val budgetRepository: BudgetRepository by lazy {
        BudgetRepository(database.budgetDao())
    }

    val recurringTransactionRepository: RecurringTransactionRepository by lazy {
        RecurringTransactionRepository(
            recurringDao = database.recurringTransactionDao(),
            transactionDao = database.transactionDao()
        )
    }

    // ── Services (lazy — only created when first used) ────────────────────────

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

    // ── Recurring generator ───────────────────────────────────────────────────

    val recurringGenerator: RecurringTransactionGenerator by lazy {
        RecurringTransactionGenerator(
            recurringRepository = recurringTransactionRepository,
            transactionRepository = transactionRepository
        )
    }

    // ── ViewModel Factories ───────────────────────────────────────────────────
    // All ViewModels use factories for consistent, testable dependency injection.

    // FIX: AccountsViewModelFactory is now registered here.
    // Previously AccountsViewModel used AndroidViewModel and pulled its own
    // dependencies — this is now fixed to match the rest of the architecture.
    val accountsViewModelFactory =
        AccountsViewModelFactory(accountRepository)

    val categoriesViewModelFactory =
        CategoriesViewModelFactory(categoryRepository)

    val statisticsViewModelFactory =
        StatisticsViewModelFactory(transactionRepository)

    val recurringViewModelFactory =
        RecurringTransactionsViewModelFactory(recurringTransactionRepository)

    val addEditRecurringFactory =
        AddEditRecurringViewModelFactory(recurringTransactionRepository)
}