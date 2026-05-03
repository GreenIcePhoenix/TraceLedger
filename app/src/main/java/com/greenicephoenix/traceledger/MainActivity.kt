package com.greenicephoenix.traceledger

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.FileProvider
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
import com.greenicephoenix.traceledger.feature.update.UpdateDialog
import com.greenicephoenix.traceledger.feature.update.UpdateInfo
import com.greenicephoenix.traceledger.feature.update.checkForUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.greenicephoenix.traceledger.feature.widget.WidgetConstants
import com.greenicephoenix.traceledger.feature.widget.WidgetUpdateHelper
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {

    // Holds the pending download ID so we can match it in the download-complete receiver
    private var pendingDownloadId: Long = -1L

    // BroadcastReceiver that fires when DownloadManager finishes downloading the APK
    private val downloadCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (downloadId != pendingDownloadId) return  // not our download

            // Ask DownloadManager for the local file URI
            val manager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            val query   = DownloadManager.Query().setFilterById(downloadId)
            val cursor  = manager.query(query)

            if (cursor.moveToFirst()) {
                val statusCol = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val uriCol    = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                if (statusCol >= 0 && cursor.getInt(statusCol) == DownloadManager.STATUS_SUCCESSFUL) {
                    val localUri = cursor.getString(uriCol)
                    triggerInstall(localUri)
                }
            }
            cursor.close()
        }
    }

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

        // Register download-complete receiver so we can trigger the install prompt
        // RECEIVER_NOT_EXPORTED — we only care about our own downloads
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                downloadCompleteReceiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                RECEIVER_NOT_EXPORTED
            )
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(
                downloadCompleteReceiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            )
        }

        // Read the widget's "add transaction" flag BEFORE setContent.
        // We capture it here so it's stable — intent extras don't change
        // after the activity is created.
        val widgetNavigateToAdd = intent.getBooleanExtra(
            WidgetConstants.EXTRA_NAVIGATE_TO_ADD, false
        )
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

            // ── Changelog / What's New sheet ──────────────────────────────────
            val lastSeenVersion by settingsStore.lastSeenVersion.collectAsState(initial = null)
            var hasCheckedVersion by remember { mutableStateOf(false) }
            var showWhatsNew      by remember { mutableStateOf(false) }

            LaunchedEffect(lastSeenVersion) {
                if (!hasCheckedVersion) { hasCheckedVersion = true; return@LaunchedEffect }
                when (lastSeenVersion) {
                    null, BuildConfig.VERSION_NAME -> {
                        if (lastSeenVersion == null) showWhatsNew = true
                    }
                    else -> showWhatsNew = true
                }
            }

            // ── Update Checker ────────────────────────────────────────────────
            // Runs once on launch, on IO dispatcher, after onboarding is done.
            // Returns null silently on any error (network down, rate limit, etc.)
            var pendingUpdate by remember { mutableStateOf<UpdateInfo?>(null) }

            LaunchedEffect(onboardingComplete) {
                if (onboardingComplete == true) {
                    withContext(Dispatchers.IO) {
                        pendingUpdate = checkForUpdate()
                    }
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

                // ── UPDATE DIALOG ─────────────────────────────────────────────
                pendingUpdate?.let { update ->
                    UpdateDialog(
                        updateInfo = update,
                        onDismiss  = { pendingUpdate = null }
                    )
                }

                // ── WHAT'S NEW SHEET ──────────────────────────────────────────
                if (showWhatsNew) {
                    val allVersions  = remember { ChangelogParser.loadVersioned(context) }
                    val currentEntry = allVersions.firstOrNull {
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
                // If launched by the widget's "+" button, navigate to Add Transaction.
                // delay(300) waits for the NavGraph to be ready before navigating.
                LaunchedEffect(Unit) {
                    if (widgetNavigateToAdd) {
                        delay(300L)
                        navController.navigate(Routes.ADD_TRANSACTION)
                    }
                }
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

    override fun onResume() {
        super.onResume()
        // Refresh widget data every time the user returns from the app.
        // This ensures the widget shows current balance after adding a transaction.
        WidgetUpdateHelper.requestUpdate(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister the download receiver to avoid leaks
        try { unregisterReceiver(downloadCompleteReceiver) } catch (_: Exception) {}
    }

    /**
     * Triggers the system APK install prompt after download completes.
     *
     * Uses FileProvider (content:// URI) as required on Android 7+.
     * The FileProvider authority is declared in AndroidManifest.xml.
     *
     * Note: REQUEST_INSTALL_PACKAGES permission is required and prompted
     * at runtime — Android will show its own dialog if not yet granted.
     */
    private fun triggerInstall(localFileUri: String) {
        try {
            val file = File(Uri.parse(localFileUri).path ?: return)

            // FileProvider converts a File path to a content:// URI that other
            // apps (the package installer) can read safely
            val installUri = FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                file
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(installUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            startActivity(installIntent)
        } catch (e: Exception) {
            // Graceful degradation — if install fails, the APK is still in Downloads
            e.printStackTrace()
        }
    }
}