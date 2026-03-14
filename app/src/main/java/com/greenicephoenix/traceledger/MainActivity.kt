package com.greenicephoenix.traceledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
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
import com.greenicephoenix.traceledger.feature.about.WhatsNewSheet
import com.greenicephoenix.traceledger.feature.onboarding.OnboardingScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val app           = applicationContext as TraceLedgerApp
        val settingsStore = app.container.settingsDataStore

        // Read persisted state synchronously before first frame — prevents flash
        val initialTheme          = runBlocking { ThemeManager.themeModeFlow(applicationContext).first() }
        val initialOnboardingDone = runBlocking { settingsStore.onboardingComplete.first() ?: false }

        lifecycleScope.launch {
            CurrencyManager.init(applicationContext)
            app.container.recurringGenerator.generateIfNeeded()
        }

        setContent {
            val context = LocalContext.current
            val view    = LocalView.current

            val themeMode by ThemeManager.themeModeFlow(context)
                .collectAsState(initial = initialTheme)

            val onboardingComplete by settingsStore.onboardingComplete
                .collectAsState(initial = initialOnboardingDone)

            LaunchedEffect(themeMode) {
                WindowInsetsControllerCompat(window, view).isAppearanceLightStatusBars =
                    (themeMode == ThemeMode.LIGHT)
            }

            // Changelog / What's New sheet
            val lastSeenVersion by settingsStore.lastSeenVersion.collectAsState(initial = null)
            var hasCheckedVersion  by remember { mutableStateOf(false) }
            var showWhatsNew       by remember { mutableStateOf(false) }

            LaunchedEffect(lastSeenVersion) {
                if (!hasCheckedVersion) { hasCheckedVersion = true; return@LaunchedEffect }
                when (lastSeenVersion) {
                    null, BuildConfig.VERSION_NAME -> {
                        if (lastSeenVersion == null) showWhatsNew = true
                    }
                    else -> showWhatsNew = true
                }
            }

            TraceLedgerTheme(themeMode = themeMode) {

                // ── ONBOARDING ────────────────────────────────────────────────
                if (onboardingComplete != true) {
                    OnboardingScreen(
                        onComplete = {
                            lifecycleScope.launch { settingsStore.completeOnboarding() }
                        }
                    )
                    return@TraceLedgerTheme
                }

                // ── WHAT'S NEW SHEET ──────────────────────────────────────────
                if (showWhatsNew) {
                    val allVersions   = remember { ChangelogParser.loadVersioned(context) }
                    val currentEntry  = allVersions.firstOrNull {
                        it.version == BuildConfig.VERSION_NAME
                    }

                    if (currentEntry != null) {
                        WhatsNewSheet(
                            changelog = currentEntry,
                            onDismiss = {
                                showWhatsNew = false
                                lifecycleScope.launch {
                                    settingsStore.setLastSeenVersion(BuildConfig.VERSION_NAME)
                                }
                            }
                        )
                    }
                }

                // ── MAIN APP ──────────────────────────────────────────────────
                val navController = rememberNavController()
                var currentRoute  by remember { mutableStateOf<String?>(null) }

                val showBottomBar = currentRoute?.let { route ->
                    route == Routes.DASHBOARD ||
                            route == Routes.TRANSACTIONS ||
                            route.startsWith(Routes.STATISTICS) ||
                            route == Routes.SETTINGS
                } ?: false

                val snackbarHostState = remember { SnackbarHostState() }

                Scaffold(
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    bottomBar    = {
                        if (showBottomBar) {
                            BottomBar(
                                currentRoute     = currentRoute,
                                onNavigate       = { route ->
                                    navController.navigate(route) {
                                        launchSingleTop = true
                                        restoreState    = true
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
                            navController     = navController,
                            snackbarHostState = snackbarHostState,
                            isLightTheme      = themeMode == ThemeMode.LIGHT
                        )
                        val backStackEntry by navController.currentBackStackEntryAsState()
                        currentRoute = backStackEntry?.destination?.route
                    }
                }
            }
        }
    }
}