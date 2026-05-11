package com.greenicephoenix.traceledger.core.navigation

import android.net.Uri
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
import com.greenicephoenix.traceledger.feature.accountimport.ui.ImportHubScreen
import com.greenicephoenix.traceledger.feature.accountimport.ui.ImportReviewScreen
import com.greenicephoenix.traceledger.feature.addtransaction.AddTransactionScreen
import com.greenicephoenix.traceledger.feature.addtransaction.AddTransactionViewModel
import com.greenicephoenix.traceledger.feature.addtransaction.AddTransactionEvent
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
import com.greenicephoenix.traceledger.feature.support.SupportScreen
import com.greenicephoenix.traceledger.feature.templates.AddEditTemplateScreen
import com.greenicephoenix.traceledger.feature.templates.TemplatesScreen
import com.greenicephoenix.traceledger.feature.templates.TemplatesViewModel
import com.greenicephoenix.traceledger.feature.transactions.HistoryScreen
import com.greenicephoenix.traceledger.feature.transactions.TransactionsViewModel
import com.greenicephoenix.traceledger.feature.transactions.TransactionsViewModelFactory
import com.greenicephoenix.traceledger.feature.accountimport.ui.ImportResultScreen
import com.greenicephoenix.traceledger.feature.sms.ui.AddEditRuleScreen
import com.greenicephoenix.traceledger.feature.sms.ui.CustomRulesScreen
import com.greenicephoenix.traceledger.feature.sms.ui.SmsReviewScreen
import com.greenicephoenix.traceledger.feature.sms.ui.SmsSettingsScreen
import com.greenicephoenix.traceledger.feature.sms.viewmodel.SmsReviewViewModel
import com.greenicephoenix.traceledger.feature.sms.viewmodel.SmsSettingsViewModel
import com.greenicephoenix.traceledger.feature.sms.viewmodel.CustomRulesViewModel
import com.greenicephoenix.traceledger.feature.sms.viewmodel.AddEditRuleViewModel
import com.greenicephoenix.traceledger.feature.help.HelpScreen
import com.greenicephoenix.traceledger.feature.about.ChangelogScreen
import kotlinx.coroutines.launch

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
                accounts           = accounts,
                viewModel          = accountsViewModel,
                onBack             = { navController.popBackStack() },
                onAddAccount       = { navController.navigate(Routes.ADD_ACCOUNT) },
                onAccountClick     = { account ->
                    navController.navigate(Routes.EDIT_ACCOUNT.replace("{accountId}", account.id))
                },
                onNavigateToImport = { navController.navigate(Routes.IMPORT_HUB) }
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
                    transactionRepository = app.container.transactionRepository,
                    templateRepository    = app.container.templateRepository
                )
            )
            val state     by addTransactionViewModel.state.collectAsState()
            val templates by addTransactionViewModel.templates.collectAsState()
            AddTransactionScreen(
                state      = state,
                accounts   = accounts,
                categories = categories,
                templates  = templates,
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
            LaunchedEffect(state.templateSaved) {
                if (state.templateSaved) {
                    scope.launch { snackbarHostState.showSnackbar("Template saved") }
                    addTransactionViewModel.onEvent(AddTransactionEvent.ConsumeTemplateSaved)
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
                    transactionRepository = app.container.transactionRepository,
                    templateRepository    = app.container.templateRepository
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

        /* ── STATISTICS ────────────────────────────────────────────────────── */
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
            // Collect the SMS pending count here so the Settings row stays live
            val smsPendingCount by app.container.smsQueueRepository
                .observePendingCount()
                .collectAsState(initial = 0)
            SettingsScreen(
                onBudgetsClick   = { navController.navigate(Routes.BUDGETS) },
                onNavigate       = { route -> navController.navigate(route) },
                smsPendingCount  = smsPendingCount,
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
            arguments = listOf(navArgument("budgetId") {
                type         = NavType.StringType
                nullable     = true
                defaultValue = null
            })
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
        composable(Routes.ABOUT) {
            AboutScreen(onBack = { navController.popBackStack() })
        }

        /* ── CHANGELOG ──────────────────────────────────────────────────────── */
        composable(Routes.CHANGELOG) {
            ChangelogScreen(onBack = { navController.popBackStack() })
        }

        /* ── HELP & FAQ ─────────────────────────────────────────────────────── */
        composable(Routes.HELP) {
            HelpScreen(onBack = { navController.popBackStack() })
        }

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

        /* ── SUPPORT ───────────────────────────────────────────────────────── */
        composable(Routes.SUPPORT) {
            SupportScreen(onBack = { navController.popBackStack() })
        }

        /* ── TEMPLATES ─────────────────────────────────────────────────────── */
        composable(Routes.TEMPLATES) {
            val vm: TemplatesViewModel = viewModel(factory = app.container.templatesViewModelFactory)
            val templates by vm.templates.collectAsState()
            TemplatesScreen(
                templates      = templates,
                accounts       = accounts,
                categories     = categories,
                onAddTemplate  = { navController.navigate(Routes.ADD_TEMPLATE) },
                onEditTemplate = { id ->
                    navController.navigate(Routes.EDIT_TEMPLATE.replace("{templateId}", id))
                },
                onDelete       = { id -> vm.deleteTemplate(id) },
                onBack         = { navController.popBackStack() }
            )
        }

        composable(Routes.ADD_TEMPLATE) {
            val vm: TemplatesViewModel = viewModel(factory = app.container.templatesViewModelFactory)
            AddEditTemplateScreen(
                existingTemplate = null,
                accounts         = accounts,
                categories       = categories,
                onSave           = { template -> vm.saveTemplate(template); navController.popBackStack() },
                onCancel         = { navController.popBackStack() }
            )
        }

        composable(
            route     = Routes.EDIT_TEMPLATE,
            arguments = listOf(navArgument("templateId") { type = NavType.StringType })
        ) { backStackEntry ->
            val templateId = backStackEntry.arguments?.getString("templateId") ?: return@composable
            val vm: TemplatesViewModel = viewModel(factory = app.container.templatesViewModelFactory)
            val templates by vm.templates.collectAsState()
            val existing = templates.firstOrNull { it.id == templateId }
            key(existing) {
                AddEditTemplateScreen(
                    existingTemplate = existing,
                    accounts         = accounts,
                    categories       = categories,
                    onSave           = { template -> vm.saveTemplate(template); navController.popBackStack() },
                    onCancel         = { navController.popBackStack() }
                )
            }
        }

        /* ── IMPORT HUB — Step 1 ───────────────────────────────────────────── */
        composable(Routes.IMPORT_HUB) {
            ImportHubScreen(
                accounts = accounts,
                onBack   = { navController.popBackStack() },
                onFileReady = { accountId, fileUri ->
                    // Store URI in SavedStateHandle — URIs contain characters that
                    // break URL encoding if passed as a route argument directly.
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set("pending_import_uri", fileUri.toString())
                    navController.navigate(Routes.importReviewFor(accountId))
                }
            )
        }

        /* ── IMPORT REVIEW — Step 2 ────────────────────────────────────────── */
        composable(
            route     = Routes.IMPORT_REVIEW,
            arguments = listOf(navArgument("accountId") { type = NavType.StringType })
        ) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getString("accountId") ?: return@composable

            // Retrieve URI stored by IMPORT_HUB before navigating here
            val uriString = navController
                .previousBackStackEntry
                ?.savedStateHandle
                ?.get<String>("pending_import_uri")
            val fileUri = uriString?.let { Uri.parse(it) }

            if (fileUri == null) {
                // URI lost (e.g. process death) — go back so user can re-pick file
                LaunchedEffect(Unit) { navController.popBackStack() }
                return@composable
            }

            ImportReviewScreen(
                accountId    = accountId,
                fileUri      = fileUri,
                accounts     = accounts,
                categories   = categories,
                vmFactory    = app.container.statementImportViewModelFactory,
                onBack       = { navController.popBackStack() },
                onRetry      = { navController.popBackStack(Routes.IMPORT_HUB, inclusive = false) },
                onImportDone = { imported, skipped, duplicates ->
                    // Pop IMPORT_REVIEW and IMPORT_HUB off the back stack,
                    // then go to result screen. User can't "back" into review
                    // after a successful import — the data is already written.
                    navController.navigate(
                        Routes.importResultRoute(imported, skipped, duplicates)
                    ) {
                        popUpTo(Routes.IMPORT_HUB) { inclusive = true }
                    }
                }
            )
        }

        /* ── IMPORT RESULT — Step 3 ────────────────────────────────────────────── */
        composable(
            route     = Routes.IMPORT_RESULT,
            arguments = listOf(
                navArgument("imported")   { type = NavType.IntType },
                navArgument("skipped")    { type = NavType.IntType },
                navArgument("duplicates") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val imported   = backStackEntry.arguments?.getInt("imported")   ?: 0
            val skipped    = backStackEntry.arguments?.getInt("skipped")    ?: 0
            val duplicates = backStackEntry.arguments?.getInt("duplicates") ?: 0

            ImportResultScreen(
                imported           = imported,
                skipped            = skipped,
                duplicates         = duplicates,
                onViewTransactions = {
                    // Navigate to Transactions tab, clearing the import back stack
                    // so the user can't "back" into the result screen.
                    navController.navigate(Routes.TRANSACTIONS) {
                        popUpTo(Routes.SETTINGS) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onDone = {
                    // Return to Settings — import flow is complete
                    navController.navigate(Routes.SETTINGS) {
                        popUpTo(Routes.SETTINGS) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }

        // ADD these two composable destinations inside your NavHost { ... } block:

        composable(Routes.SMS_SETTINGS) {
            val viewModel: SmsSettingsViewModel = viewModel(
                factory = app.container.smsSettingsViewModelFactory
            )
            SmsSettingsScreen(
                viewModel = viewModel,
                onNavigate = { route -> navController.navigate(route) },
                onNavigateBack = { navController.popBackStack() },
                onNavigateToReview = { navController.navigate(Routes.SMS_REVIEW) }
            )
        }

        composable(Routes.SMS_REVIEW) {
            val viewModel: SmsReviewViewModel = viewModel(
                factory = app.container.smsReviewViewModelFactory
            )
            SmsReviewScreen(
                viewModel      = viewModel,
                accounts       = accounts,    // already available — graph-scoped
                categories     = categories,  // already available — graph-scoped
                onNavigateBack = { navController.popBackStack() }
            )
        }

        /* ── SMS CUSTOM RULES LIST ─────────────────────────────────────────────── */
        composable(Routes.SMS_CUSTOM_RULES) {
            val vm: CustomRulesViewModel = viewModel(factory = app.container.customRulesViewModelFactory)
            CustomRulesScreen(
                viewModel      = vm,
                onNavigateBack = { navController.popBackStack() },
                onAddRule      = { navController.navigate(Routes.SMS_ADD_RULE) },
                onEditRule     = { rule -> navController.navigate(Routes.smsEditRuleFor(rule.id)) }
            )
        }

        /* ── ADD RULE ───────────────────────────────────────────────────────────── */
        composable(Routes.SMS_ADD_RULE) {
            val vm: AddEditRuleViewModel = viewModel(factory = app.container.addEditRuleViewModelFactory)
            AddEditRuleScreen(
                viewModel      = vm,
                accounts       = accounts,
                categories     = categories,
                isEditMode     = false,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        /* ── EDIT RULE ──────────────────────────────────────────────────────────── */
        composable(
            route     = Routes.SMS_EDIT_RULE,
            arguments = listOf(navArgument("ruleId") { type = NavType.LongType })
        ) { backStackEntry ->
            val ruleId = backStackEntry.arguments?.getLong("ruleId") ?: return@composable
            val vm: AddEditRuleViewModel = viewModel(factory = app.container.addEditRuleViewModelFactory)
            // Load the rule once
            val allRules by app.container.smsRuleRepository.observeRules().collectAsState(initial = emptyList())
            LaunchedEffect(ruleId, allRules) {
                allRules.firstOrNull { it.id == ruleId }?.let { vm.loadRule(it) }
            }
            AddEditRuleScreen(
                viewModel      = vm,
                accounts       = accounts,
                categories     = categories,
                isEditMode     = true,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}