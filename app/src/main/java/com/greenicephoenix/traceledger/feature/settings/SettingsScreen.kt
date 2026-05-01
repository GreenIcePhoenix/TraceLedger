package com.greenicephoenix.traceledger.feature.settings

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.core.currency.Currency
import com.greenicephoenix.traceledger.core.currency.CurrencyManager
import com.greenicephoenix.traceledger.core.currency.NumberFormatManager
import com.greenicephoenix.traceledger.core.datastore.NumberFormat
import com.greenicephoenix.traceledger.core.datastore.SettingsDataStore
import com.greenicephoenix.traceledger.core.export.ExportFormat
import com.greenicephoenix.traceledger.core.importer.ImportPreview
import com.greenicephoenix.traceledger.core.navigation.Routes
import com.greenicephoenix.traceledger.core.notifications.ReminderScheduler
import com.greenicephoenix.traceledger.core.ui.theme.NothingRed
import com.greenicephoenix.traceledger.core.ui.theme.ThemeManager
import com.greenicephoenix.traceledger.core.ui.theme.ThemeMode
import kotlinx.coroutines.launch

enum class ImportType { JSON, CSV }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBudgetsClick: () -> Unit,
    onNavigate: (String) -> Unit,
    onExportSelected: (ExportFormat) -> Unit,
    onExportUriReady: (ExportFormat, Uri) -> Unit,
    onImportContinue: () -> Unit,
    onImportUriReady: (Uri) -> Unit,
    onImportPreviewRequested: suspend (Uri) -> ImportPreview,
    onImportConfirmed: (Uri, (Int?) -> Unit) -> Unit,
    onImportError: (String) -> Unit
) {
    val context        = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val settingsStore  = remember { SettingsDataStore(context) }

    // ── Sheet / dialog visibility flags ──────────────────────────────────────
    var pendingImportUri    by remember { mutableStateOf<Uri?>(null) }
    var importPreview       by remember { mutableStateOf<ImportPreview?>(null) }
    var showImportPreview   by remember { mutableStateOf(false) }
    var importProgress      by remember { mutableStateOf<Int?>(null) }
    var pendingExportFormat by remember { mutableStateOf<ExportFormat?>(null) }
    var pendingImportType   by remember { mutableStateOf<ImportType?>(null) }

    var showCurrencySheet     by remember { mutableStateOf(false) }
    var showExportSheet       by remember { mutableStateOf(false) }
    var showImportSheet       by remember { mutableStateOf(false) }
    var showThemeSheet        by remember { mutableStateOf(false) }
    var showNumberFormatSheet by remember { mutableStateOf(false) }
    var showTimePicker        by remember { mutableStateOf(false) }

    // ── Observed state ────────────────────────────────────────────────────────
    val currentCurrency  by CurrencyManager.currency.collectAsState()
    val currentTheme     by ThemeManager.themeModeFlow(context).collectAsState(initial = ThemeMode.DARK)
    val currentNumFormat by settingsStore.numberFormat.collectAsState(initial = null)

    // Daily reminder state — read from DataStore
    val reminderEnabled by settingsStore.reminderEnabled.collectAsState(initial = false)
    val reminderHour    by settingsStore.reminderHour.collectAsState(initial = 22)
    val reminderMinute  by settingsStore.reminderMinute.collectAsState(initial = 0)

    val numFormatLabel = when (currentNumFormat) {
        NumberFormat.INDIAN.name        -> NumberFormat.INDIAN.label
        NumberFormat.INTERNATIONAL.name -> NumberFormat.INTERNATIONAL.label
        else                            -> "Indian (1,00,000)"
    }

    // Format reminder time as "10:00 PM" for the subtitle
    val reminderTimeLabel = remember(reminderHour, reminderMinute) {
        val amPm  = if (reminderHour < 12) "AM" else "PM"
        val hour12 = when {
            reminderHour == 0  -> 12
            reminderHour > 12  -> reminderHour - 12
            else               -> reminderHour
        }
        "$hour12:${reminderMinute.toString().padStart(2, '0')} $amPm"
    }

    // ── POST_NOTIFICATIONS permission launcher (Android 13+) ─────────────────
    // When the user turns on the reminder toggle, we request notification permission
    // on Android 13+. On older versions the permission is granted at install time.
    val notificationPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted — actually enable the reminder
            coroutineScope.launch {
                settingsStore.setReminderEnabled(true)
                ReminderScheduler.schedule(context, reminderHour, reminderMinute)
            }
        }
        // If denied: do nothing — the toggle stays off (we never wrote enabled=true)
    }

    // ── File launchers ────────────────────────────────────────────────────────
    val exportLauncher = rememberLauncherForActivityResult(CreateDocument("*/*")) { uri ->
        if (uri != null && pendingExportFormat != null) {
            onExportUriReady(pendingExportFormat!!, uri)
        }
        pendingExportFormat = null
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && pendingImportType != null) {
            val type = pendingImportType ?: return@rememberLauncherForActivityResult
            when (type) {
                ImportType.JSON, ImportType.CSV -> {
                    coroutineScope.launch {
                        try {
                            importPreview    = onImportPreviewRequested(uri)
                            pendingImportUri = uri
                            showImportPreview = true
                        } catch (e: Exception) {
                            onImportError(e.message ?: "Invalid file")
                        }
                    }
                }
            }
            pendingImportType = null
        }
    }

    // ── Main scrollable column ────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text  = "SETTINGS",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(4.dp))

        // ── APPEARANCE ────────────────────────────────────────────────────────
        SettingsSectionLabel("APPEARANCE")

        SettingsItem(
            title    = "Theme",
            subtitle = if (currentTheme == ThemeMode.DARK) "Dark" else "Light",
            onClick  = { showThemeSheet = true }
        )

        SettingsItem(
            title    = "Currency",
            subtitle = "${currentCurrency.code} (${currentCurrency.symbol})",
            onClick  = { showCurrencySheet = true }
        )

        SettingsItem(
            title    = "Number Format",
            subtitle = numFormatLabel,
            onClick  = { showNumberFormatSheet = true }
        )

        Spacer(Modifier.height(4.dp))

        // ── FINANCE ───────────────────────────────────────────────────────────
        SettingsSectionLabel("FINANCE")

        SettingsItem(
            title    = "Categories",
            subtitle = "Manage income and expense categories",
            onClick  = { onNavigate(Routes.CATEGORIES) }
        )

        SettingsItem(
            title    = "Budgets",
            subtitle = "Monthly spending limits",
            onClick  = onBudgetsClick
        )

        SettingsItem(
            title    = "Recurring Transactions",
            subtitle = "Manage automatic repeating transactions",
            onClick  = { onNavigate(Routes.RECURRING) }
        )

        SettingsItem(
            title    = "Transaction Templates",
            subtitle = "Save and reuse frequent transactions",
            onClick  = { onNavigate(Routes.TEMPLATES) }
        )

        Spacer(Modifier.height(4.dp))

        // ── NOTIFICATIONS ─────────────────────────────────────────────────────
        SettingsSectionLabel("NOTIFICATIONS")

        // Daily Reminder toggle row.
        // Tapping the card when the reminder is ON opens the time picker.
        // The Switch itself toggles the reminder on/off.
        SettingsItemWithToggle(
            title    = "Daily Reminder",
            subtitle = if (reminderEnabled) "Enabled · $reminderTimeLabel"
            else "Remind you to log transactions each day",
            checked  = reminderEnabled,
            onClick  = {
                // Tapping the card while enabled → open time picker to change time
                if (reminderEnabled) showTimePicker = true
            },
            onCheckedChange = { enabled ->
                if (enabled) {
                    // Turning ON: request notification permission on Android 13+
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        // Android 12 and below — permission is automatic
                        coroutineScope.launch {
                            settingsStore.setReminderEnabled(true)
                            ReminderScheduler.schedule(context, reminderHour, reminderMinute)
                        }
                    }
                } else {
                    // Turning OFF: cancel the alarm and clear the stored flag
                    coroutineScope.launch {
                        settingsStore.setReminderEnabled(false)
                        ReminderScheduler.cancel(context)
                    }
                }
            }
        )

        Spacer(Modifier.height(4.dp))

        // ── DATA ──────────────────────────────────────────────────────────────
        SettingsSectionLabel("DATA")

        SettingsItem(
            title    = "Export data",
            subtitle = "Backup your data as JSON or CSV",
            onClick  = { showExportSheet = true }
        )

        SettingsItem(
            title    = "Import data",
            subtitle = "Restore data from a backup file",
            onClick  = { showImportSheet = true }
        )

        Spacer(Modifier.height(4.dp))

        // ── APP ───────────────────────────────────────────────────────────────
        SettingsSectionLabel("APP")

        SettingsItem(
            title    = "Support the Developer",
            subtitle = "Paypal/UPI — tip jar",
            onClick  = { onNavigate(Routes.SUPPORT) }
        )

        SettingsItem(
            title    = "About",
            subtitle = "Version, changelog, and app info",
            onClick  = { onNavigate(Routes.ABOUT) }
        )
    }

    // ── Time Picker Dialog ────────────────────────────────────────────────────
    // Shown when the user taps the reminder row while it's already enabled.
    // Material 3 TimePicker is used here — it gives a polished clock-face UI.
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour   = reminderHour,
            initialMinute = reminderMinute,
            is24Hour      = false
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            shape            = RoundedCornerShape(20.dp),
            containerColor   = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text  = "REMINDER TIME",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                // TimePicker renders the clock face
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(onClick = {
                    showTimePicker = false
                    coroutineScope.launch {
                        // Save the new time and re-schedule the alarm
                        settingsStore.setReminderTime(
                            hour   = timePickerState.hour,
                            minute = timePickerState.minute
                        )
                        ReminderScheduler.schedule(
                            context = context,
                            hour    = timePickerState.hour,
                            minute  = timePickerState.minute
                        )
                    }
                }) {
                    Text("Set", color = NothingRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        )
    }

    // ── BOTTOM SHEETS ─────────────────────────────────────────────────────────

    if (showCurrencySheet) {
        PickerBottomSheet(title = "Currency", onDismiss = { showCurrencySheet = false }) {
            Currency.entries.forEach { currency ->
                val isSelected = currency == currentCurrency
                PickerRow(
                    label      = "${currency.code}  ${currency.symbol}  (${currency.code})",
                    isSelected = isSelected,
                    onClick    = {
                        CurrencyManager.setCurrency(currency)
                        showCurrencySheet = false
                    }
                )
            }
        }
    }

    if (showThemeSheet) {
        PickerBottomSheet(title = "Theme", onDismiss = { showThemeSheet = false }) {
            ThemeMode.entries.forEach { mode ->
                val isSelected = mode == currentTheme
                PickerRow(
                    label      = if (mode == ThemeMode.DARK) "Dark" else "Light",
                    isSelected = isSelected,
                    onClick    = {
                        showThemeSheet = false
                        coroutineScope.launch { ThemeManager.setThemeMode(context, mode) }
                    }
                )
            }
        }
    }

    if (showNumberFormatSheet) {
        PickerBottomSheet(title = "Number Format", onDismiss = { showNumberFormatSheet = false }) {
            NumberFormat.entries.forEach { format ->
                val isSelected = (currentNumFormat ?: NumberFormat.INDIAN.name) == format.name
                PickerRow(
                    label      = "${format.label}  e.g. ${format.example}",
                    isSelected = isSelected,
                    onClick    = {
                        showNumberFormatSheet = false
                        // Use NumberFormatManager so CurrencyFormatter sees the change
                        // immediately — no app restart required.
                        NumberFormatManager.setFormat(format)
                    }
                )
            }
        }
    }

    if (showExportSheet) {
        PickerBottomSheet(title = "Export data", onDismiss = { showExportSheet = false }) {
            Text(
                text     = "Choose a format",
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(8.dp))
            ExportOption(
                title       = "JSON (recommended)",
                description = "Full backup — accounts, categories, budgets, transactions",
                onClick     = {
                    showExportSheet     = false
                    pendingExportFormat = ExportFormat.JSON
                    exportLauncher.launch("TraceLedger-backup.json")
                }
            )
            ExportOption(
                title       = "CSV",
                description = "Transactions only — for spreadsheets",
                onClick     = {
                    showExportSheet     = false
                    pendingExportFormat = ExportFormat.CSV
                    exportLauncher.launch("TraceLedger-transactions.csv")
                }
            )
        }
    }

    if (showImportSheet) {
        PickerBottomSheet(title = "Import data", onDismiss = { showImportSheet = false }) {
            ExportOption(
                title       = "JSON (full restore)",
                description = "Replaces all data with backup contents",
                onClick     = {
                    pendingImportType = ImportType.JSON
                    showImportSheet   = false
                    importLauncher.launch(arrayOf("application/json"))
                }
            )
            ExportOption(
                title       = "CSV (transactions only)",
                description = "Adds transactions to existing accounts and categories",
                onClick     = {
                    pendingImportType = ImportType.CSV
                    showImportSheet   = false
                    importLauncher.launch(arrayOf("text/csv"))
                }
            )
            Text(
                text     = "CSV rows with unknown accounts or categories will be skipped.",
                style    = MaterialTheme.typography.labelSmall,
                color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }

    if (showImportPreview && importPreview != null) {
        ModalBottomSheet(onDismissRequest = {
            showImportPreview = false
            importPreview     = null
            pendingImportUri  = null
        }) {
            val preview   = importPreview!!
            val canImport = preview.totalRows == 0 || preview.validRows > 0

            Column(
                modifier            = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Import preview", style = MaterialTheme.typography.titleMedium)

                if (preview.accounts > 0)     Text("Accounts: ${preview.accounts}")
                if (preview.categories > 0)   Text("Categories: ${preview.categories}")
                if (preview.budgets > 0)      Text("Budgets: ${preview.budgets}")
                if (preview.transactions > 0) Text("Transactions: ${preview.transactions}")
                if (preview.validRows > 0)    Text("Valid rows: ${preview.validRows}")
                if (preview.skippedRows > 0) {
                    Text(
                        text  = "Skipped rows: ${preview.skippedRows}",
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Text(
                    text  = "JSON import will replace ALL existing data.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )

                if (!canImport) {
                    Text(
                        text  = "No valid rows found — import is disabled.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        showImportPreview = false
                        importPreview     = null
                        pendingImportUri  = null
                    }) { Text("Cancel") }

                    Spacer(Modifier.width(8.dp))

                    TextButton(
                        enabled = canImport,
                        onClick = {
                            val uri = pendingImportUri ?: return@TextButton
                            showImportPreview = false
                            onImportConfirmed(uri) { progress -> importProgress = progress }
                        }
                    ) {
                        Text(
                            text  = "Import",
                            color = if (canImport) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }

    // Import progress overlay — covers the screen while data is being written
    importProgress?.let { progress ->
        Box(
            modifier         = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier            = Modifier.padding(24.dp)
            ) {
                Text("Importing data…", style = MaterialTheme.typography.titleMedium)
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text  = "$progress%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }

    LaunchedEffect(importProgress) {
        if (importProgress != null && importProgress!! >= 100) {
            kotlinx.coroutines.delay(400)
            importProgress   = null
            pendingImportUri = null
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text     = text,
        style    = MaterialTheme.typography.labelSmall,
        color    = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

/** Standard settings row — tapping opens a sheet or navigates. */
@Composable
private fun SettingsItem(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape    = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text  = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text  = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Settings row with a Switch on the trailing edge.
 * Tapping the card fires [onClick] (e.g. open time picker).
 * Tapping the Switch fires [onCheckedChange].
 * These are deliberately kept separate so the two touch targets behave differently.
 */
@Composable
private fun SettingsItemWithToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onClick: () -> Unit,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape    = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text  = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Switch(
                checked         = checked,
                onCheckedChange = onCheckedChange,
                colors          = SwitchDefaults.colors(
                    checkedThumbColor  = MaterialTheme.colorScheme.surface,
                    checkedTrackColor  = NothingRed,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickerBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text     = title,
                style    = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            content()
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PickerRow(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else Color.Transparent,
                MaterialTheme.shapes.medium
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text     = label,
            color    = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
            style    = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Text("✓", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ExportOption(title: String, description: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text  = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text  = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}