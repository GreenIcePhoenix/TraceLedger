package com.greenicephoenix.traceledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.greenicephoenix.traceledger.core.currency.CurrencyManager
import com.greenicephoenix.traceledger.core.navigation.Routes
import com.greenicephoenix.traceledger.core.navigation.TraceLedgerNavGraph
import com.greenicephoenix.traceledger.core.ui.components.BottomBar
import com.greenicephoenix.traceledger.core.ui.theme.ThemeManager
import com.greenicephoenix.traceledger.core.ui.theme.ThemeMode
import com.greenicephoenix.traceledger.core.ui.theme.TraceLedgerTheme
import com.greenicephoenix.traceledger.core.util.ChangelogParser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ── System bar insets ─────────────────────────────────────────────────
        // We control padding ourselves via Compose insets, so tell the system
        // not to apply its own window fitting.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // ── One-time startup work (NOT in Compose) ────────────────────────────
        // These run once when the Activity is created, in the Activity's
        // coroutine scope — not inside the Compose tree.
        //
        // FIX: Previously these were in LaunchedEffect(Unit) inside setContent.
        // LaunchedEffect(Unit) re-triggers when the Compose tree is recreated
        // (e.g. theme changes). Moving them here guarantees they run exactly once
        // per Activity lifecycle, regardless of recompositions.
        lifecycleScope.launch {
            // Initialize currency from DataStore (loads user's saved preference)
            CurrencyManager.init(applicationContext)

            // Run recurring transaction generator.
            // This checks all active recurring rules and creates any transactions
            // that are due today and haven't been generated yet.
            // Safe to run multiple times — duplicate guard is inside the generator.
            val app = applicationContext as TraceLedgerApp
            app.container.recurringGenerator.generateIfNeeded()
        }

        // ── FIX: Read theme preference before setContent ──────────────────────
        // Previously the theme was loaded asynchronously inside Compose using
        // collectAsState(initial = ThemeMode.DARK). This caused a one-frame flash
        // of the wrong theme every time the app launched (dark flash for light
        // theme users, or vice versa). Reading it synchronously here eliminates
        // that flash. runBlocking is acceptable here because:
        //   1. It only runs once at app start
        //   2. DataStore reads from an in-memory cache after first access
        //   3. The alternative (showing the wrong theme) is worse UX
        val initialTheme = runBlocking {
            ThemeManager.themeModeFlow(applicationContext).first()
        }

        setContent {
            val context = LocalContext.current
            val app = context.applicationContext as TraceLedgerApp
            val settingsDataStore = app.container.settingsDataStore

            // ── Theme state ───────────────────────────────────────────────────
            // Start with the pre-loaded value (no flash), then stay reactive
            // so in-app theme changes take effect immediately.
            val themeMode by ThemeManager
                .themeModeFlow(context)
                .collectAsState(initial = initialTheme)  // FIX: use pre-loaded value

            // ── Status bar icon color ─────────────────────────────────────────
            val view = LocalView.current
            LaunchedEffect(themeMode) {
                val window = this@MainActivity.window
                val controller = WindowInsetsControllerCompat(window, view)
                // Light theme → dark icons on status bar (readable on light bg)
                // Dark theme → light icons on status bar (readable on dark bg)
                controller.isAppearanceLightStatusBars = (themeMode == ThemeMode.LIGHT)
            }

            // ── Changelog sheet state ─────────────────────────────────────────
            val lastSeenVersion by settingsDataStore.lastSeenVersion
                .collectAsState(initial = null)

            var hasCheckedVersion by remember { mutableStateOf(false) }
            var showChangelogSheet by remember { mutableStateOf(false) }

            LaunchedEffect(lastSeenVersion) {
                if (!hasCheckedVersion) {
                    hasCheckedVersion = true
                    return@LaunchedEffect
                }
                val currentVersion = BuildConfig.VERSION_NAME
                when (lastSeenVersion) {
                    null           -> showChangelogSheet = true   // First install
                    currentVersion -> { /* Already seen this version */ }
                    else           -> showChangelogSheet = true   // New version
                }
            }

            TraceLedgerTheme(themeMode = themeMode) {

                val navController = rememberNavController()
                var currentRoute by remember { mutableStateOf<String?>(null) }

                val showBottomBar = currentRoute?.let { route ->
                    route == Routes.DASHBOARD ||
                            route == Routes.TRANSACTIONS ||
                            route.startsWith(Routes.STATISTICS) ||
                            route == Routes.SETTINGS
                } ?: false

                val snackbarHostState = remember { SnackbarHostState() }

                // ── Changelog sheet ───────────────────────────────────────────
                if (showChangelogSheet) {
                    ModalBottomSheet(
                        onDismissRequest = {
                            showChangelogSheet = false
                            lifecycleScope.launch {
                                settingsDataStore.setLastSeenVersion(BuildConfig.VERSION_NAME)
                            }
                        }
                    ) {
                        val allChangelogs = ChangelogParser.load(context)
                        val currentVersion = BuildConfig.VERSION_NAME
                        val changelogText = allChangelogs[currentVersion] ?: "No changelog available."

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                                .padding(24.dp)
                        ) {
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn() + slideInVertically { it / 4 }
                            ) {
                                Column {
                                    Text(
                                        text = "What's New in $currentVersion",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        text = changelogText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                    Spacer(Modifier.height(24.dp))
                                    Button(
                                        onClick = {
                                            showChangelogSheet = false
                                            lifecycleScope.launch {
                                                settingsDataStore.setLastSeenVersion(BuildConfig.VERSION_NAME)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Got it")
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Main scaffold ─────────────────────────────────────────────
                Scaffold(
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    bottomBar = {
                        if (showBottomBar) {
                            BottomBar(
                                currentRoute = currentRoute,
                                onNavigate = { route ->
                                    navController.navigate(route) {
                                        launchSingleTop = true
                                        restoreState = true
                                        popUpTo(Routes.DASHBOARD) { saveState = true }
                                    }
                                },
                                onAddTransaction = {
                                    navController.navigate(Routes.ADD_TRANSACTION)
                                }
                            )
                        }
                    },
                    contentWindowInsets = WindowInsets.systemBars
                ) { paddingValues ->
                    Box(modifier = Modifier.padding(paddingValues)) {
                        TraceLedgerNavGraph(
                            navController = navController,
                            snackbarHostState = snackbarHostState,
                            isLightTheme = themeMode == ThemeMode.LIGHT
                        )
                        // Read current route AFTER NavGraph is attached to avoid
                        // reading from an uninitialized back stack.
                        val backStackEntry by navController.currentBackStackEntryAsState()
                        currentRoute = backStackEntry?.destination?.route
                    }
                }
            }
        }
    }
}