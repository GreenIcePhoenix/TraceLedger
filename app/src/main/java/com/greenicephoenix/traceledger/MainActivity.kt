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
import com.greenicephoenix.traceledger.feature.onboarding.OnboardingScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Read both theme and onboarding state synchronously before first frame
        // to avoid any flash of wrong content.
        val app           = applicationContext as TraceLedgerApp
        val settingsStore = app.container.settingsDataStore

        val initialTheme = runBlocking {
            ThemeManager.themeModeFlow(applicationContext).first()
        }
        val initialOnboardingDone = runBlocking {
            settingsStore.onboardingComplete.first() ?: false
        }

        // One-time startup work — runs once per Activity lifecycle
        lifecycleScope.launch {
            CurrencyManager.init(applicationContext)
            app.container.recurringGenerator.generateIfNeeded()
        }

        setContent {
            val context = LocalContext.current
            val view    = LocalView.current

            val themeMode by ThemeManager
                .themeModeFlow(context)
                .collectAsState(initial = initialTheme)

            // Track whether onboarding is complete reactively so it updates
            // immediately when completeOnboarding() writes to DataStore.
            val onboardingComplete by settingsStore.onboardingComplete
                .collectAsState(initial = initialOnboardingDone)

            // Status bar icon colour synced to theme
            LaunchedEffect(themeMode) {
                val window     = this@MainActivity.window
                val controller = WindowInsetsControllerCompat(window, view)
                controller.isAppearanceLightStatusBars = (themeMode == ThemeMode.LIGHT)
            }

            // Changelog sheet state
            val lastSeenVersion by settingsStore.lastSeenVersion.collectAsState(initial = null)
            var hasCheckedVersion  by remember { mutableStateOf(false) }
            var showChangelogSheet by remember { mutableStateOf(false) }

            LaunchedEffect(lastSeenVersion) {
                if (!hasCheckedVersion) { hasCheckedVersion = true; return@LaunchedEffect }
                val currentVersion = BuildConfig.VERSION_NAME
                when (lastSeenVersion) {
                    null           -> showChangelogSheet = true
                    currentVersion -> {}
                    else           -> showChangelogSheet = true
                }
            }

            TraceLedgerTheme(themeMode = themeMode) {

                // ── ONBOARDING ────────────────────────────────────────────────
                // Show onboarding fullscreen if not yet completed.
                // Once completed, it will never be shown again.
                if (onboardingComplete != true) {
                    OnboardingScreen(
                        onComplete = {
                            lifecycleScope.launch {
                                settingsStore.completeOnboarding()
                            }
                        }
                    )
                    return@TraceLedgerTheme
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

                // Changelog sheet
                if (showChangelogSheet) {
                    ModalBottomSheet(
                        onDismissRequest = {
                            showChangelogSheet = false
                            lifecycleScope.launch {
                                settingsStore.setLastSeenVersion(BuildConfig.VERSION_NAME)
                            }
                        }
                    ) {
                        val allChangelogs  = ChangelogParser.load(context)
                        val currentVersion = BuildConfig.VERSION_NAME
                        val changelogText  = allChangelogs[currentVersion] ?: "No changelog available."

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                                .padding(24.dp)
                        ) {
                            AnimatedVisibility(
                                visible = true,
                                enter   = fadeIn() + slideInVertically { it / 4 }
                            ) {
                                Column {
                                    Text(
                                        text  = "What's New in $currentVersion",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        text  = changelogText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )
                                    Spacer(Modifier.height(24.dp))
                                    Button(
                                        onClick = {
                                            showChangelogSheet = false
                                            lifecycleScope.launch {
                                                settingsStore.setLastSeenVersion(BuildConfig.VERSION_NAME)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("Got it") }
                                }
                            }
                        }
                    }
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    bottomBar    = {
                        if (showBottomBar) {
                            BottomBar(
                                currentRoute = currentRoute,
                                onNavigate   = { route ->
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