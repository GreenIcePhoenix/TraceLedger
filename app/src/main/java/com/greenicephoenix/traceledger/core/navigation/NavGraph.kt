package com.greenicephoenix.traceledger.core.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import com.greenicephoenix.traceledger.TraceLedgerApp
import com.greenicephoenix.traceledger.feature.about.AboutScreen
import com.greenicephoenix.traceledger.feature.accounts.AccountsScreen
import com.greenicephoenix.traceledger.feature.accounts.AccountsViewModel
import com.greenicephoenix.traceledger.feature.accounts.AddEditAccountScreen
import com.greenicephoenix.traceledger.feature.addtransaction.AddTransactionScreen
import com.greenicephoenix.traceledger.feature.addtransaction.AddTransactionViewModel
import com.greenicephoenix.traceledger.feature.addtransaction.AddTransactionViewModelFactory
import com.greenicephoenix.traceledger.feature.budgets.AddEditBudgetScreen
import com.greenicephoenix.traceledger.feature.budgets.BudgetsScreen
import com.greenicephoenix.traceledger.feature.budgets.BudgetsViewModel
import com.greenicephoenix.traceledger.feature.budgets.BudgetsViewModelFactory
import com.greenicephoenix.traceledger.feature.categories.AddEditCategoryScreen
import com.greenicephoenix.traceledger.feature.categories.CategoriesScreen
import com.greenicephoenix.traceledger.feature.categories.CategoriesViewModel
import com.greenicephoenix.traceledger.feature.dashboard.DashboardScreen
import com.greenicephoenix.traceledger.feature.recurring.AddEditRecurringScreen
import com.greenicephoenix.traceledger.feature.recurring.AddEditRecurringViewModel
import com.greenicephoenix.traceledger.feature.recurring.RecurringTransactionsScreen
import com.greenicephoenix.traceledger.feature.settings.SettingsScreen
import com.greenicephoenix.traceledger.feature.statistics.CashflowScreen
import com.greenicephoenix.traceledger.feature.statistics.ExpenseBreakdownScreen
import com.greenicephoenix.traceledger.feature.statistics.IncomeBreakdownScreen
import com.greenicephoenix.traceledger.feature.statistics.StatisticsScreen
import com.greenicephoenix.traceledger.feature.statistics.StatisticsViewModel
import com.greenicephoenix.traceledger.feature.transactions.HistoryScreen
import com.greenicephoenix.traceledger.feature.transactions.TransactionsViewModel
import com.greenicephoenix.traceledger.feature.transactions.TransactionsViewModelFactory
import kotlinx.coroutines.launch

/**
 * Central navigation graph for TraceLedger.
 *
 * RULES:
 * - All routes must be defined in Routes.kt
 * - No feature may create its own NavHost
 * - All ViewModels must use factories from AppContainer
 *
 * FIX: All AccountsViewModel usages now use app.container.accountsViewModelFactory
 * instead of the bare viewModel() call (which relied on AndroidViewModel and
 * pulled repos directly from Application — an architecture violation).
 */
@Composable
fun TraceLedgerNavGraph(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    isLightTheme: Boolean
) {
    val context = LocalContext.current
    val app = context.applicationContext as TraceLedgerApp

    // ── Shared ViewModels (alive for the full nav graph lifetime) ─────────────
    // These are scoped to the NavGraph composable, so they survive screen navigation
    // within the graph but are cleared when the graph itself leaves composition.

    val categoriesViewModel: CategoriesViewModel =
        viewModel(factory = app.container.categoriesViewModelFactory)

    val budgetsViewModel: BudgetsViewModel = viewModel(
        factory = BudgetsViewModelFactory(
            budgetRepository = app.container.budgetRepository,
            transactionRepository = app.container.transactionRepository
        )
    )

    // FIX: AccountsViewModel now uses factory from AppContainer.
    // Previously created with viewModel() (no factory), which forced the ViewModel
    // to use AndroidViewModel and pull repos from the Application object directly.
    // Scoped here at the graph level so the same instance is shared across all
    // screens that need accounts, avoiding redundant DB observers.
    val accountsViewModel: AccountsViewModel =
        viewModel(factory = app.container.accountsViewModelFactory)

    val categories by categoriesViewModel.categories.collectAsState()
    val accounts by accountsViewModel.accounts.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Routes.DASHBOARD
    ) {

        /* ── DASHBOARD ─────────────────────────────────────────────────────── */
        composable(Routes.DASHBOARD) {

            val statisticsViewModel = viewModel<StatisticsViewModel>(
                factory = app.container.statisticsViewModelFactory
            )

            DashboardScreen(
                accounts = accounts,
                statisticsViewModel = statisticsViewModel,
                budgetsViewModel = budgetsViewModel,
                onNavigate = { route -> navController.navigate(route) },
                onAddAccount = { navController.navigate(Routes.ADD_ACCOUNT) },
                onAccountClick = { account ->
                    navController.navigate(
                        Routes.EDIT_ACCOUNT.replace("{accountId}", account.id)
                    )
                }
            )
        }

        /* ── ACCOUNTS ──────────────────────────────────────────────────────── */
        composable(Routes.ACCOUNTS) {
            AccountsScreen(
                accounts = accounts,
                onBack = { navController.popBackStack() },
                onAddAccount = { navController.navigate(Routes.ADD_ACCOUNT) },
                onAccountClick = { account ->
                    navController.navigate(
                        Routes.EDIT_ACCOUNT.replace("{accountId}", account.id)
                    )
                },
                onAccountLongPress = { account ->
                    accountsViewModel.deleteAccount(account.id)
                    true
                }
            )
        }

        /* ── ADD ACCOUNT ───────────────────────────────────────────────────── */
        composable(Routes.ADD_ACCOUNT) {
            AddEditAccountScreen(
                existingAccount = null,
                onCancel = { navController.popBackStack() },
                onSave = { newAccount ->
                    accountsViewModel.saveAccount(newAccount)  // FIX: was addAccount()
                    navController.popBackStack()
                }
            )
        }

        /* ── EDIT ACCOUNT ──────────────────────────────────────────────────── */
        composable(
            route = Routes.EDIT_ACCOUNT,
            arguments = listOf(navArgument("accountId") { type = NavType.StringType })
        ) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getString("accountId")
            val accountToEdit = accounts.firstOrNull { it.id == accountId }

            AddEditAccountScreen(
                existingAccount = accountToEdit,
                onCancel = { navController.popBackStack() },
                onSave = { updatedAccount ->
                    accountsViewModel.saveAccount(updatedAccount)  // FIX: was updateAccount()
                    navController.popBackStack()
                }
            )
        }

        /* ── TRANSACTIONS LIST ─────────────────────────────────────────────── */
        composable(Routes.TRANSACTIONS) {
            val transactionsViewModel: TransactionsViewModel = viewModel(
                factory = TransactionsViewModelFactory(
                    transactionRepository = app.container.transactionRepository
                )
            )

            HistoryScreen(
                viewModel = transactionsViewModel,
                accounts = accounts,
                categories = categories,
                onBack = { navController.popBackStack() },
                onEditTransaction = { transactionId ->
                    navController.navigate(
                        Routes.EDIT_TRANSACTION.replace("{transactionId}", transactionId)
                    )
                }
            )
        }

        /* ── ADD TRANSACTION ───────────────────────────────────────────────── */
        composable(Routes.ADD_TRANSACTION) {
            val scope = rememberCoroutineScope()

            val addTransactionViewModel: AddTransactionViewModel = viewModel(
                factory = AddTransactionViewModelFactory(
                    transactionRepository = app.container.transactionRepository
                )
            )

            val state by addTransactionViewModel.state.collectAsState()

            AddTransactionScreen(
                state = state,
                accounts = accounts,
                categories = categories,
                isEditMode = false,
                onEvent = addTransactionViewModel::onEvent,
                onCancel = { navController.popBackStack() }
            )

            LaunchedEffect(state.saveCompleted) {
                if (state.saveCompleted) {
                    navController.popBackStack()
                    scope.launch { snackbarHostState.showSnackbar("Transaction added") }
                    addTransactionViewModel.consumeSaveCompleted()
                }
            }
        }

        /* ── EDIT TRANSACTION ──────────────────────────────────────────────── */
        composable(
            route = Routes.EDIT_TRANSACTION,
            arguments = listOf(navArgument("transactionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getString("transactionId")
                ?: return@composable

            val scope = rememberCoroutineScope()

            val viewModel: AddTransactionViewModel = viewModel(
                factory = AddTransactionViewModelFactory(
                    transactionRepository = app.container.transactionRepository
                )
            )

            LaunchedEffect(transactionId) {
                viewModel.initEdit(transactionId)
            }

            val state by viewModel.state.collectAsState()

            AddTransactionScreen(
                state = state,
                accounts = accounts,
                categories = categories,
                isEditMode = true,
                onEvent = viewModel::onEvent,
                onCancel = { navController.popBackStack() }
            )

            LaunchedEffect(state.saveCompleted) {
                if (state.saveCompleted) {
                    navController.popBackStack()
                    scope.launch { snackbarHostState.showSnackbar("Transaction updated") }
                    viewModel.consumeSaveCompleted()
                }
            }
        }

        /* ── STATISTICS (nested graph) ─────────────────────────────────────── */
        // Statistics screens share a single StatisticsViewModel scoped to the
        // nested graph, so month selection persists as you navigate between
        // Overview → Breakdown → Income screens.
        navigation(
            route = Routes.STATISTICS,
            startDestination = Routes.STATISTICS_OVERVIEW
        ) {
            composable(Routes.STATISTICS_OVERVIEW) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Routes.STATISTICS)
                }
                val statisticsViewModel = viewModel<StatisticsViewModel>(
                    parentEntry,
                    factory = app.container.statisticsViewModelFactory
                )
                StatisticsScreen(
                    viewModel = statisticsViewModel,
                    categoryMap = categories.associateBy { it.id },
                    onNavigate = { route -> navController.navigate(route) }
                )
            }

            composable(Routes.STATISTICS_BREAKDOWN) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Routes.STATISTICS)
                }
                val statisticsViewModel = viewModel<StatisticsViewModel>(
                    parentEntry,
                    factory = app.container.statisticsViewModelFactory
                )
                ExpenseBreakdownScreen(
                    viewModel = statisticsViewModel,
                    categoryMap = categories.associateBy { it.id },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.STATISTICS_INCOME) { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Routes.STATISTICS)
                }
                val statisticsViewModel = viewModel<StatisticsViewModel>(
                    parentEntry,
                    factory = app.container.statisticsViewModelFactory
                )
                IncomeBreakdownScreen(
                    viewModel = statisticsViewModel,
                    categoryMap = categories.associateBy { it.id },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(Routes.STATISTICS_CASHFLOW) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Routes.STATISTICS)
            }
            val statisticsViewModel = viewModel<StatisticsViewModel>(
                parentEntry,
                factory = app.container.statisticsViewModelFactory
            )
            CashflowScreen(
                viewModel = statisticsViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        /* ── SETTINGS ──────────────────────────────────────────────────────── */
        composable(Routes.SETTINGS) {
            val scope = rememberCoroutineScope()

            SettingsScreen(
                onBudgetsClick = { navController.navigate(Routes.BUDGETS) },
                onNavigate = { route -> navController.navigate(route) },
                onExportSelected = { /* SAF flow triggered inside SettingsScreen */ },
                onExportUriReady = { format, uri ->
                    scope.launch {
                        try {
                            app.container.exportService.export(format, uri)
                            snackbarHostState.showSnackbar("Export completed")
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Export failed: ${e.message}")
                        }
                    }
                },
                onImportContinue = { /* handled by SAF launcher inside SettingsScreen */ },
                onImportUriReady = { uri ->
                    scope.launch {
                        try {
                            app.container.importService.importJson(
                                uri = uri,
                                onProgress = { /* no-op */ }
                            )
                            snackbarHostState.showSnackbar("Import completed")
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar(e.message ?: "Import failed")
                        }
                    }
                },
                onImportPreviewRequested = { uri ->
                    app.container.importService.previewCsv(uri)
                },
                onImportConfirmed = { uri, onProgress ->
                    scope.launch {
                        app.container.importService.importCsvTransactions(
                            uri = uri,
                            onProgress = onProgress
                        )
                        snackbarHostState.showSnackbar("CSV import completed")
                    }
                },
                onImportError = { message ->
                    scope.launch { snackbarHostState.showSnackbar(message) }
                }
            )
        }

        /* ── CATEGORIES ────────────────────────────────────────────────────── */
        composable(Routes.CATEGORIES) {
            CategoriesScreen(
                categories = categories,
                isLightTheme = isLightTheme,
                onBack = { navController.popBackStack() },
                onAddCategory = { navController.navigate(Routes.ADD_CATEGORY) },
                onCategoryClick = { category ->
                    navController.navigate(
                        Routes.EDIT_CATEGORY.replace("{categoryId}", category.id)
                    )
                }
            )
        }

        composable(Routes.ADD_CATEGORY) {
            AddEditCategoryScreen(
                existingCategory = null,
                onCancel = { navController.popBackStack() },
                onSave = { newCategory ->
                    categoriesViewModel.addCategory(newCategory)
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Routes.EDIT_CATEGORY,
            arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId")
            val categoryToEdit = categories.firstOrNull { it.id == categoryId }

            AddEditCategoryScreen(
                existingCategory = categoryToEdit,
                onCancel = { navController.popBackStack() },
                onSave = { updatedCategory ->
                    categoriesViewModel.updateCategory(updatedCategory)
                    navController.popBackStack()
                }
            )
        }

        /* ── BUDGETS ───────────────────────────────────────────────────────── */
        composable(Routes.BUDGETS) {
            BudgetsScreen(
                viewModel = budgetsViewModel,
                categories = categories,
                onAddBudget = { navController.navigate(Routes.ADD_EDIT_BUDGET) },
                onEditBudget = { budgetId ->
                    navController.navigate("${Routes.ADD_EDIT_BUDGET}/$budgetId")
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ADD_EDIT_BUDGET) {
            val selectedMonth by budgetsViewModel.selectedMonth.collectAsState()
            AddEditBudgetScreen(
                viewModel = budgetsViewModel,
                categories = categories,
                budgetId = null,
                month = selectedMonth,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "${Routes.ADD_EDIT_BUDGET}/{budgetId}",
            arguments = listOf(
                navArgument("budgetId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val budgetId = backStackEntry.arguments?.getString("budgetId")
            val selectedMonth by budgetsViewModel.selectedMonth.collectAsState()
            AddEditBudgetScreen(
                viewModel = budgetsViewModel,
                categories = categories,
                budgetId = budgetId,
                month = selectedMonth,
                onBack = { navController.popBackStack() }
            )
        }

        /* ── ABOUT ─────────────────────────────────────────────────────────── */
        composable(Routes.ABOUT) {
            AboutScreen(onBack = { navController.popBackStack() })
        }

        /* ── RECURRING ─────────────────────────────────────────────────────── */
        composable(Routes.RECURRING) {
            RecurringTransactionsScreen(
                viewModelFactory = app.container.recurringViewModelFactory,
                onAddClick = { navController.navigate(Routes.ADD_RECURRING) },
                onEditClick = { recurring ->
                    navController.navigate("edit_recurring/${recurring.id}")
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ADD_RECURRING) {
            val viewModel: AddEditRecurringViewModel =
                viewModel(factory = app.container.addEditRecurringFactory)

            AddEditRecurringScreen(
                accounts = accounts,
                categories = categories,
                existing = null,
                onSave = { type, amount, fromId, toId, categoryId, frequency, start, end, note ->
                    viewModel.saveRecurring(
                        type = type,
                        amount = amount,
                        fromAccountId = fromId,
                        toAccountId = toId,
                        categoryId = categoryId,
                        frequency = frequency,
                        startDate = start,
                        endDate = end,
                        note = note
                    )
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }

        /* ── EDIT RECURRING ────────────────────────────────────────────────── */
        composable(
            route = Routes.EDIT_RECURRING,
            arguments = listOf(navArgument("recurringId") { type = NavType.StringType })
        ) { backStackEntry ->
            val recurringId = backStackEntry.arguments?.getString("recurringId")
                ?: return@composable

            val viewModel: AddEditRecurringViewModel =
                viewModel(factory = app.container.addEditRecurringFactory)

            // FIX: Call load() so the ViewModel fetches the rule from the DB.
            // LaunchedEffect(recurringId) runs once when this screen enters
            // composition. The guard inside load() prevents double-loading.
            LaunchedEffect(recurringId) {
                viewModel.load(recurringId)
            }

            // FIX: Observe currentRecurring as a StateFlow so this composable
            // recomposes when the data arrives from the suspend load() call.
            // Previously currentRecurring was a plain var — not observable —
            // so the screen always rendered with existing = null (blank form).
            val existing by viewModel.currentRecurring.collectAsState()

            AddEditRecurringScreen(
                accounts   = accounts,
                categories = categories,
                existing   = existing,   // FIX: was hardcoded null
                onSave     = { type, amount, fromId, toId, categoryId, frequency, start, end, note ->
                    viewModel.saveRecurring(
                        type          = type,
                        amount        = amount,
                        fromAccountId = fromId,
                        toAccountId   = toId,
                        categoryId    = categoryId,
                        frequency     = frequency,
                        startDate     = start,
                        endDate       = end,
                        note          = note
                    )
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}