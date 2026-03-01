package com.greenicephoenix.traceledger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.greenicephoenix.traceledger.core.navigation.Routes
import com.greenicephoenix.traceledger.core.navigation.TraceLedgerNavGraph
import com.greenicephoenix.traceledger.core.ui.components.BottomBar
import com.greenicephoenix.traceledger.core.ui.theme.TraceLedgerTheme
import com.greenicephoenix.traceledger.core.currency.CurrencyManager
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.greenicephoenix.traceledger.core.ui.theme.ThemeManager
import com.greenicephoenix.traceledger.core.ui.theme.ThemeMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.greenicephoenix.traceledger.core.util.ChangelogParser
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔒 We handle system insets manually via Compose
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            LaunchedEffect(Unit) {
                CurrencyManager.init(applicationContext)

                val app = applicationContext as TraceLedgerApp
                app.container.recurringGenerator.generateIfNeeded()
            }

            val context = LocalContext.current

            val app = context.applicationContext as TraceLedgerApp
            val settingsDataStore = app.container.settingsDataStore
            val recurringGenerator = app.container.recurringGenerator

            val lastSeenVersion by settingsDataStore
                .lastSeenVersion
                .collectAsState(initial = null)

            var hasCheckedVersion by remember { mutableStateOf(false) }

            var showChangelogSheet by remember { mutableStateOf(false) }

            val themeMode by ThemeManager
                .themeModeFlow(context)
                .collectAsState(initial = ThemeMode.DARK)

            val view = LocalView.current

            LaunchedEffect(themeMode) {
                val window = this@MainActivity.window
                val controller = WindowInsetsControllerCompat(window, view)

                // Light theme → dark icons
                // Dark theme → light icons
                controller.isAppearanceLightStatusBars =
                    themeMode == ThemeMode.LIGHT
            }

            TraceLedgerTheme(
                themeMode = themeMode
            ) {
                LaunchedEffect(lastSeenVersion) {
                    val currentVersion = BuildConfig.VERSION_NAME

                    if (!hasCheckedVersion) {
                        hasCheckedVersion = true
                        return@LaunchedEffect
                    }

                    when (lastSeenVersion) {
                        null -> showChangelogSheet = true
                        currentVersion -> { /* do nothing */ }
                        else -> showChangelogSheet = true
                    }
                }

                LaunchedEffect(Unit) {
                    recurringGenerator.generateIfNeeded()
                }

                val navController = rememberNavController()
                // DO NOT read back stack before NavHost is attached
                var currentRoute by remember { mutableStateOf<String?>(null) }


                // Screens that show top settings bar
                val showTopBar = currentRoute in listOf(
                    Routes.DASHBOARD,
                    Routes.TRANSACTIONS,
                    Routes.STATISTICS,
                    Routes.SETTINGS
                )

                // Screens that show bottom nav bar
                val showBottomBar =
                    currentRoute?.let { route ->
                        route == Routes.DASHBOARD ||
                                route == Routes.TRANSACTIONS ||
                                route.startsWith(Routes.STATISTICS) ||
                                route == Routes.SETTINGS
                    } ?: false

                val snackbarHostState = remember { SnackbarHostState() }

                if (showChangelogSheet) {

                    ModalBottomSheet(
                        onDismissRequest = {
                            showChangelogSheet = false

                            // Persist version immediately
                            lifecycleScope.launch {
                                settingsDataStore.setLastSeenVersion(
                                    BuildConfig.VERSION_NAME
                                )
                                android.util.Log.d(
                                    "CHANGELOG_DEBUG",
                                    "Saved version ${BuildConfig.VERSION_NAME}"
                                )
                            }
                        }
                    ) {

                        val allChangelogs = ChangelogParser.load(context)
                        val currentVersion = BuildConfig.VERSION_NAME
                        val changelogText =
                            allChangelogs[currentVersion]
                                ?: "No changelog available."

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
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

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = changelogText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                    )

                                    Spacer(modifier = Modifier.height(24.dp))

                                    Button(
                                        onClick = {
                                            showChangelogSheet = false
                                            lifecycleScope.launch {
                                                settingsDataStore.setLastSeenVersion(
                                                    BuildConfig.VERSION_NAME
                                                )
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



                Scaffold(
                    snackbarHost = {
                        SnackbarHost(hostState = snackbarHostState)
                    },
                    bottomBar = {
                        if (showBottomBar) {
                            BottomBar(
                                currentRoute = currentRoute,
                                onNavigate = { route ->
                                    navController.navigate(route) {
                                        launchSingleTop = true
                                        restoreState = true
                                        popUpTo(Routes.DASHBOARD) {
                                            saveState = true
                                        }
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
                    Box(
                        modifier = Modifier.padding(paddingValues)
                    ) {
                        TraceLedgerNavGraph(
                            navController = navController,
                            snackbarHostState = snackbarHostState,
                            isLightTheme = themeMode == ThemeMode.LIGHT
                        )
                        // SAFELY observe route AFTER graph is attached
                        val backStackEntry by navController.currentBackStackEntryAsState()
                        currentRoute = backStackEntry?.destination?.route
                    }
                }
            }
        }
    }
}
