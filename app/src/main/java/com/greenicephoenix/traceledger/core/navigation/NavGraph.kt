package com.greenicephoenix.traceledger.core.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.*
import androidx.navigation.compose.*
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
import com.greenicephoenix.traceledger.feature.dashboard.DashboardViewModel
import com.greenicephoenix.traceledger.feature.recurring.AddEditRecurringScreen
import com.greenicephoenix.traceledger.feature.recurring.AddEditRecurringViewModel
import com.greenicephoenix.traceledger.feature.recurring.RecurringTransactionsScreen
import com.greenicephoenix.traceledger.feature.settings.SettingsScreen
import com.greenicephoenix.traceledger.feature.statistics.CashflowScreen
import com.greenicephoenix.traceledger.feature.statistics.CategoryTrendScreen
import com.greenicephoenix.traceledger.feature.statistics.ExpenseBreakdownScreen
import com.greenicephoenix.traceledger.feature.statistics.IncomeBreakdownScreen
import com.greenicephoenix.traceledger.feature.statistics.StatisticsScreen
import com.greenicephoenix.traceledger.feature.statistics.StatisticsViewModel
import com.greenicephoenix.traceledger.feature.transactions.HistoryScreen
import com.greenicephoenix.traceledger.feature.transactions.TransactionsViewModel
import com.greenicephoenix.traceledger.feature.transactions.TransactionsViewModelFactory
import kotlinx.coroutines.launch
import com.greenicephoenix.traceledger.feature.support.SupportScreen

// REMOVED imports — no longer needed:
// import com.greenicephoenix.traceledger.feature.about.PrivacyPolicyScreen
// import com.greenicephoenix.traceledger.feature.about.TermsScreen
// Both screens have been deleted. Privacy Policy and Terms of Use are now
// served from the website only. AboutScreen opens them via LocalUriHandler.

@Composable
fun TraceLedgerNavGraph(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    isLightTheme: Boolean
) {
    val context = LocalContext.current
    val app     = context.applicationContext as TraceLedgerApp

    // ── Shared ViewModels (graph-scoped) ──────────────────────────────────────
    val categoriesViewModel: CategoriesViewModel =
        viewModel(factory = app.container.categoriesViewModelFactory)

    val budgetsViewModel: BudgetsViewModel = viewModel(
        factory = BudgetsViewModelFactory(
            budgetRepository      = app.container.budgetRepository,
            transactionRepository = app.container.transactionRepository
        )
    )

    val accountsViewModel: AccountsViewModel =
        viewModel(factory = app.container.accountsViewModelFactory)

    val categories by categoriesViewModel.categories.collectAsState()
    val accounts   by accountsViewModel.accounts.collectAsState()

    NavHost(navController = navController, startDestination = Routes.DASHBOARD) {

        /* ── DASHBOARD ─────────────────────────────────────────────────────── */
        composable(Routes.DASHBOARD) {
            val dashboardViewModel: DashboardViewModel =
                viewModel(factory = app.container.dashboardViewModelFactory)

            val warningBudgetsCount  by budgetsViewModel.warningBudgetsCount.collectAsState()
            val hasExceededBudgets   by budgetsViewModel.hasExceededBudgets.collectAsState()
            val exceededBudgetsCount by budgetsViewModel.exceededBudgetsCount.collectAsState()

            DashboardScreen(
                accounts           = accounts,
                dashboardViewModel = dashboardViewModel,
                budgetsViewModel   = budgetsViewModel,
                categories         = categories,
                onNavigate         = { route -> navController.navigate(route) },
                onAddAccount       = { navController.navigate(Routes.ADD_ACCOUNT) },
                onAccountClick     = { account ->
                    navController.navigate(Routes.EDIT_ACCOUNT.replace("{accountId}", account.id))
                },
                onTransactionClick = { transactionId ->
                    navController.navigate(Routes.EDIT_TRANSACTION.replace("{transactionId}", transactionId))
                }
            )
        }

        /* ── ACCOUNTS ──────────────────────────────────────────────────────── */
        composable(Routes.ACCOUNTS) {
            AccountsScreen(
                accounts       = accounts,
                viewModel      = accountsViewModel,
                onBack         = { navController.popBackStack() },
                onAddAccount   = { navController.navigate(Routes.ADD_ACCOUNT) },
                onAccountClick = { account ->
                    navController.navigate(Routes.EDIT_ACCOUNT.replace("{accountId}", account.id))
                }
            )
        }

        composable(Routes.ADD_ACCOUNT) {
            AddEditAccountScreen(
                existingAccount = null,
                onCancel = { navController.popBackStack() },
                onSave   = { newAccount ->
                    accountsViewModel.saveAccount(newAccount)
                    navController.popBackStack()
                }
            )
        }

        composable(
            route     = Routes.EDIT_ACCOUNT,
            arguments = listOf(navArgument("accountId") { type = NavType.StringType })
        ) { backStackEntry ->
            val accountId     = backStackEntry.arguments?.getString("accountId")
            val accountToEdit = accounts.firstOrNull { it.id == accountId }
            AddEditAccountScreen(
                existingAccount = accountToEdit,
                onCancel = { navController.popBackStack() },
                onSave   = { updatedAccount ->
                    accountsViewModel.saveAccount(updatedAccount)
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
                viewModel         = transactionsViewModel,
                accounts          = accounts,
                categories        = categories,
                onBack            = { navController.popBackStack() },
                onEditTransaction = { transactionId ->
                    navController.navigate(Routes.EDIT_TRANSACTION.replace("{transactionId}", transactionId))
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
                state      = state,
                accounts   = accounts,
                categories = categories,
                isEditMode = false,
                onEvent    = addTransactionViewModel::onEvent,
                onCancel   = { navController.popBackStack() }
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
            route     = Routes.EDIT_TRANSACTION,
            arguments = listOf(navArgument("transactionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getString("transactionId") ?: return@composable
            val scope = rememberCoroutineScope()
            val viewModel: AddTransactionViewModel = viewModel(
                factory = AddTransactionViewModelFactory(
                    transactionRepository = app.container.transactionRepository
                )
            )
            LaunchedEffect(transactionId) { viewModel.initEdit(transactionId) }
            val state by viewModel.state.collectAsState()
            AddTransactionScreen(
                state      = state,
                accounts   = accounts,
                categories = categories,
                isEditMode = true,
                onEvent    = viewModel::onEvent,
                onCancel   = { navController.popBackStack() }
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
        navigation(route = Routes.STATISTICS, startDestination = Routes.STATISTICS_OVERVIEW) {
            composable(Routes.STATISTICS_OVERVIEW) { backStackEntry ->
                val parentEntry = remember(backStackEntry) { navController.getBackStackEntry(Routes.STATISTICS) }
                val statisticsViewModel = viewModel<StatisticsViewModel>(parentEntry, factory = app.container.statisticsViewModelFactory)
                StatisticsScreen(
                    viewModel   = statisticsViewModel,
                    categoryMap = categories.associateBy { it.id },
                    onNavigate  = { route -> navController.navigate(route) }
                )
            }
            composable(Routes.STATISTICS_BREAKDOWN) { backStackEntry ->
                val parentEntry = remember(backStackEntry) { navController.getBackStackEntry(Routes.STATISTICS) }
                val statisticsViewModel = viewModel<StatisticsViewModel>(parentEntry, factory = app.container.statisticsViewModelFactory)
                ExpenseBreakdownScreen(
                    viewModel   = statisticsViewModel,
                    categoryMap = categories.associateBy { it.id },
                    onBack      = { navController.popBackStack() }
                )
            }
            composable(Routes.STATISTICS_INCOME) { backStackEntry ->
                val parentEntry = remember(backStackEntry) { navController.getBackStackEntry(Routes.STATISTICS) }
                val statisticsViewModel = viewModel<StatisticsViewModel>(parentEntry, factory = app.container.statisticsViewModelFactory)
                IncomeBreakdownScreen(
                    viewModel   = statisticsViewModel,
                    categoryMap = categories.associateBy { it.id },
                    onBack      = { navController.popBackStack() }
                )
            }
            composable(Routes.STATISTICS_TRENDS) { backStackEntry ->
                val parentEntry = remember(backStackEntry) { navController.getBackStackEntry(Routes.STATISTICS) }
                val statisticsViewModel = viewModel<StatisticsViewModel>(parentEntry, factory = app.container.statisticsViewModelFactory)
                CategoryTrendScreen(
                    viewModel   = statisticsViewModel,
                    categoryMap = categories.associateBy { it.id },
                    onBack      = { navController.popBackStack() }
                )
            }
        }

        composable(Routes.STATISTICS_CASHFLOW) { backStackEntry ->
            val parentEntry = remember(backStackEntry) { navController.getBackStackEntry(Routes.STATISTICS) }
            val statisticsViewModel = viewModel<StatisticsViewModel>(parentEntry, factory = app.container.statisticsViewModelFactory)
            CashflowScreen(viewModel = statisticsViewModel, onBack = { navController.popBackStack() })
        }

        /* ── SETTINGS ──────────────────────────────────────────────────────── */
        composable(Routes.SETTINGS) {
            val scope = rememberCoroutineScope()
            SettingsScreen(
                onBudgetsClick   = { navController.navigate(Routes.BUDGETS) },
                onNavigate       = { route -> navController.navigate(route) },
                onExportSelected = { },
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
                onImportContinue         = { },
                onImportUriReady         = { uri ->
                    scope.launch {
                        try {
                            app.container.importService.importJson(uri = uri, onProgress = { })
                            snackbarHostState.showSnackbar("Import completed")
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar(e.message ?: "Import failed")
                        }
                    }
                },
                onImportPreviewRequested = { uri -> app.container.importService.previewCsv(uri) },
                onImportConfirmed        = { uri, onProgress ->
                    scope.launch {
                        val result = app.container.importService.importCsvTransactions(
                            uri = uri, onProgress = onProgress
                        )
                        val msg = buildString {
                            append("${result.imported} transaction(s) imported")
                            if (result.skipped > 0) append(", ${result.skipped} row(s) skipped")
                        }
                        snackbarHostState.showSnackbar(msg)
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
                categories      = categories,
                isLightTheme    = isLightTheme,
                viewModel       = categoriesViewModel,
                onBack          = { navController.popBackStack() },
                onAddCategory   = { navController.navigate(Routes.ADD_CATEGORY) },
                onCategoryClick = { category ->
                    navController.navigate(Routes.EDIT_CATEGORY.replace("{categoryId}", category.id))
                }
            )
        }

        composable(Routes.ADD_CATEGORY) {
            AddEditCategoryScreen(
                existingCategory = null,
                onCancel = { navController.popBackStack() },
                onSave   = { newCategory ->
                    categoriesViewModel.addCategory(newCategory)
                    navController.popBackStack()
                }
            )
        }

        composable(
            route     = Routes.EDIT_CATEGORY,
            arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
        ) { backStackEntry ->
            val categoryId     = backStackEntry.arguments?.getString("categoryId")
            val categoryToEdit = categories.firstOrNull { it.id == categoryId }
            AddEditCategoryScreen(
                existingCategory = categoryToEdit,
                onCancel = { navController.popBackStack() },
                onSave   = { updatedCategory ->
                    categoriesViewModel.updateCategory(updatedCategory)
                    navController.popBackStack()
                }
            )
        }

        /* ── BUDGETS ───────────────────────────────────────────────────────── */
        composable(Routes.BUDGETS) {
            BudgetsScreen(
                viewModel    = budgetsViewModel,
                categories   = categories,
                onAddBudget  = { navController.navigate(Routes.ADD_EDIT_BUDGET) },
                onEditBudget = { budgetId -> navController.navigate("${Routes.ADD_EDIT_BUDGET}/$budgetId") },
                onBack       = { navController.popBackStack() }
            )
        }

        composable(Routes.ADD_EDIT_BUDGET) {
            val selectedMonth by budgetsViewModel.selectedMonth.collectAsState()
            AddEditBudgetScreen(
                viewModel  = budgetsViewModel,
                categories = categories,
                budgetId   = null,
                month      = selectedMonth,
                onBack     = { navController.popBackStack() }
            )
        }

        composable(
            route     = "${Routes.ADD_EDIT_BUDGET}/{budgetId}",
            arguments = listOf(navArgument("budgetId") { type = NavType.StringType; nullable = true; defaultValue = null })
        ) { backStackEntry ->
            val budgetId      = backStackEntry.arguments?.getString("budgetId")
            val selectedMonth by budgetsViewModel.selectedMonth.collectAsState()
            AddEditBudgetScreen(
                viewModel  = budgetsViewModel,
                categories = categories,
                budgetId   = budgetId,
                month      = selectedMonth,
                onBack     = { navController.popBackStack() }
            )
        }

        /* ── ABOUT ─────────────────────────────────────────────────────────── */
        // onPrivacyPolicy and onTerms callbacks removed — AboutScreen now opens
        // website URLs directly via LocalUriHandler. No internal routes needed.
        composable(Routes.ABOUT) {
            AboutScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // REMOVED: Routes.PRIVACY_POLICY composable (PrivacyPolicyScreen deleted)
        // REMOVED: Routes.TERMS composable (TermsScreen deleted)

        /* ── RECURRING ─────────────────────────────────────────────────────── */
        composable(Routes.RECURRING) {
            RecurringTransactionsScreen(
                viewModelFactory = app.container.recurringViewModelFactory,
                onAddClick       = { navController.navigate(Routes.ADD_RECURRING) },
                onEditClick      = { recurring -> navController.navigate("edit_recurring/${recurring.id}") },
                onBack           = { navController.popBackStack() }
            )
        }

        composable(Routes.ADD_RECURRING) {
            val recurringViewModel: AddEditRecurringViewModel =
                viewModel(factory = app.container.addEditRecurringFactory)
            AddEditRecurringScreen(
                accounts   = accounts,
                categories = categories,
                existing   = null,
                onSave     = { type, amount, fromId, toId, categoryId, frequency, start, end, note ->
                    recurringViewModel.saveRecurring(type, amount, fromId, toId, categoryId, frequency, start, end, note)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route     = Routes.EDIT_RECURRING,
            arguments = listOf(navArgument("recurringId") { type = NavType.StringType })
        ) { backStackEntry ->
            val recurringId = backStackEntry.arguments?.getString("recurringId") ?: return@composable
            val recurringViewModel: AddEditRecurringViewModel =
                viewModel(factory = app.container.addEditRecurringFactory)
            LaunchedEffect(recurringId) { recurringViewModel.load(recurringId) }
            val existing by recurringViewModel.currentRecurring.collectAsState()
            AddEditRecurringScreen(
                accounts   = accounts,
                categories = categories,
                existing   = existing,
                onSave     = { type, amount, fromId, toId, categoryId, frequency, start, end, note ->
                    recurringViewModel.saveRecurring(type, amount, fromId, toId, categoryId, frequency, start, end, note)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }

        /* ── SUPPORT ──────────────────────────────────────────────────────── */
        composable(Routes.SUPPORT) {
            SupportScreen(onBack = { navController.popBackStack() })
        }
    }
}