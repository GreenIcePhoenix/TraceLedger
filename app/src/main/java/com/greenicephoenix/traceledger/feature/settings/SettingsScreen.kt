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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.BuildConfig
import com.greenicephoenix.traceledger.core.currency.Currency
import com.greenicephoenix.traceledger.core.currency.CurrencyManager
import com.greenicephoenix.traceledger.core.currency.NumberFormatManager
import com.greenicephoenix.traceledger.core.datastore.NumberFormat
import com.greenicephoenix.traceledger.core.datastore.SettingsDataStore
import com.greenicephoenix.traceledger.core.export.ExportFormat
import com.greenicephoenix.traceledger.core.importer.ImportPreview
import com.greenicephoenix.traceledger.core.navigation.Routes
import com.greenicephoenix.traceledger.core.notifications.ReminderScheduler
import com.greenicephoenix.traceledger.core.ui.theme.ThemeManager
import com.greenicephoenix.traceledger.core.ui.theme.ThemeMode
import kotlinx.coroutines.launch

enum class ImportType { JSON, CSV }

// ── Icon accent colours ───────────────────────────────────────────────────────
// These are decorative only — not part of the Material3 theme.
// Each colour family groups related settings visually:
//   Green  → brand/money (Theme, Currency, Categories, Import)
//   Blue   → data/format (Number Format, Export)
//   Amber  → time/limits (Budgets, Reminder)
//   Purple → utility    (Templates, About)
private val IconGreen  = Color(0xFF2ECC71)
private val IconBlue   = Color(0xFF638FD4)
private val IconAmber  = Color(0xFFF59E0B)
private val IconPurple = Color(0xFF9575CD)

private val BgGreen  = Color(0xFF2ECC71).copy(alpha = 0.12f)
private val BgBlue   = Color(0xFF638FD4).copy(alpha = 0.12f)
private val BgAmber  = Color(0xFFF59E0B).copy(alpha = 0.12f)
private val BgPurple = Color(0xFF9575CD).copy(alpha = 0.12f)

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

    // ── Sheet / dialog visibility ─────────────────────────────────────────────
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
    // Default changed from DARK → SYSTEM to match the new ThemeManager default
    val currentTheme     by ThemeManager.themeModeFlow(context).collectAsState(initial = ThemeMode.SYSTEM)
    val currentNumFormat by settingsStore.numberFormat.collectAsState(initial = null)

    val reminderEnabled by settingsStore.reminderEnabled.collectAsState(initial = false)
    val reminderHour    by settingsStore.reminderHour.collectAsState(initial = 22)
    val reminderMinute  by settingsStore.reminderMinute.collectAsState(initial = 0)

    // ── Derived display strings ───────────────────────────────────────────────

    // Human-readable label for the currently selected theme mode
    val currentThemeLabel = when (currentTheme) {
        ThemeMode.SYSTEM     -> "System"
        ThemeMode.LIGHT      -> "Light"
        ThemeMode.DARK       -> "Dark"
        ThemeMode.ULTRA_DARK -> "Extra Dark"
    }

    val numFormatLabel = when (currentNumFormat) {
        NumberFormat.INDIAN.name        -> "Indian"
        NumberFormat.INTERNATIONAL.name -> "International"
        else                            -> "Indian"
    }

    // e.g. "10:00 PM"
    val reminderTimeLabel = remember(reminderHour, reminderMinute) {
        val amPm   = if (reminderHour < 12) "AM" else "PM"
        val hour12 = when {
            reminderHour == 0 -> 12
            reminderHour > 12 -> reminderHour - 12
            else              -> reminderHour
        }
        "$hour12:${reminderMinute.toString().padStart(2, '0')} $amPm"
    }

    // ── Notification permission launcher (Android 13+) ────────────────────────
    val notificationPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            coroutineScope.launch {
                settingsStore.setReminderEnabled(true)
                ReminderScheduler.schedule(context, reminderHour, reminderMinute)
            }
        }
    }

    // ── File launchers ────────────────────────────────────────────────────────
    val exportLauncher = rememberLauncherForActivityResult(CreateDocument("*/*")) { uri ->
        if (uri != null && pendingExportFormat != null) {
            onExportUriReady(pendingExportFormat!!, uri)
        }
        pendingExportFormat = null
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null && pendingImportType != null) {
            val type = pendingImportType ?: return@rememberLauncherForActivityResult
            when (type) {
                ImportType.JSON, ImportType.CSV -> {
                    coroutineScope.launch {
                        try {
                            importPreview     = onImportPreviewRequested(uri)
                            pendingImportUri  = uri
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
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {

        // Screen title — uses Cinzel via headlineMedium in TraceLedgerTypography
        Text(
            text     = "SETTINGS",
            style    = MaterialTheme.typography.headlineMedium,
            color    = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // ── APPEARANCE ────────────────────────────────────────────────────────
        SettingsSectionLabel("Appearance")

        SettingsRow(
            icon       = Icons.Outlined.Palette,
            iconTint   = IconGreen,
            iconBg     = BgGreen,
            title      = "Theme",
            value      = currentThemeLabel,
            onClick    = { showThemeSheet = true }
        )
        SettingsRow(
            icon       = Icons.Outlined.AttachMoney,
            iconTint   = IconGreen,
            iconBg     = BgGreen,
            title      = "Currency",
            value      = "${currentCurrency.code} ${currentCurrency.symbol}",
            onClick    = { showCurrencySheet = true }
        )
        SettingsRow(
            icon       = Icons.Outlined.Tag,
            iconTint   = IconBlue,
            iconBg     = BgBlue,
            title      = "Number Format",
            value      = numFormatLabel,
            onClick    = { showNumberFormatSheet = true }
        )

        Spacer(Modifier.height(20.dp))

        // ── FINANCE ───────────────────────────────────────────────────────────
        SettingsSectionLabel("Finance")

        SettingsRow(
            icon    = Icons.Outlined.Category,
            iconTint = IconGreen,
            iconBg  = BgGreen,
            title   = "Categories",
            subtitle = "Expense & income",
            onClick = { onNavigate(Routes.CATEGORIES) }
        )
        SettingsRow(
            icon    = Icons.Outlined.PieChart,
            iconTint = IconAmber,
            iconBg  = BgAmber,
            title   = "Budgets",
            subtitle = "Monthly limits",
            onClick = onBudgetsClick
        )
        SettingsRow(
            icon    = Icons.Outlined.Repeat,
            iconTint = IconBlue,
            iconBg  = BgBlue,
            title   = "Recurring",
            subtitle = "Auto transactions",
            onClick = { onNavigate(Routes.RECURRING) }
        )
        SettingsRow(
            icon    = Icons.Outlined.BookmarkBorder,
            iconTint = IconPurple,
            iconBg  = BgPurple,
            title   = "Templates",
            subtitle = "Saved transactions",
            onClick = { onNavigate(Routes.TEMPLATES) }
        )

        Spacer(Modifier.height(20.dp))

        // ── NOTIFICATIONS ─────────────────────────────────────────────────────
        SettingsSectionLabel("Notifications")

        SettingsRowToggle(
            icon     = Icons.Outlined.Notifications,
            iconTint = IconAmber,
            iconBg   = BgAmber,
            title    = "Daily Reminder",
            // Show time only when enabled — subtitle doubles as the status hint
            subtitle = if (reminderEnabled) reminderTimeLabel
            else "Remind you to log daily",
            checked  = reminderEnabled,
            // Tapping the row while ON opens the time picker
            onClick  = { if (reminderEnabled) showTimePicker = true },
            onCheckedChange = { enabled ->
                if (enabled) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        coroutineScope.launch {
                            settingsStore.setReminderEnabled(true)
                            ReminderScheduler.schedule(context, reminderHour, reminderMinute)
                        }
                    }
                } else {
                    coroutineScope.launch {
                        settingsStore.setReminderEnabled(false)
                        ReminderScheduler.cancel(context)
                    }
                }
            }
        )

        Spacer(Modifier.height(20.dp))

        // ── DATA ──────────────────────────────────────────────────────────────
        SettingsSectionLabel("Data")

        SettingsRow(
            icon    = Icons.Outlined.FileUpload,
            iconTint = IconBlue,
            iconBg  = BgBlue,
            title   = "Export Data",
            subtitle = "JSON · CSV",
            onClick = { showExportSheet = true }
        )
        SettingsRow(
            icon    = Icons.Outlined.FileDownload,
            iconTint = IconGreen,
            iconBg  = BgGreen,
            title   = "Import Data",
            subtitle = "Restore backup",
            onClick = { showImportSheet = true }
        )

        Spacer(Modifier.height(20.dp))

        // ── APP ───────────────────────────────────────────────────────────────
        SettingsSectionLabel("App")

        // Support row — special brand-accented treatment to make it stand out
        // without being obnoxious. Green tint border, glowing dot, own background.
        SupportRow(onClick = { onNavigate(Routes.SUPPORT) })

        SettingsRow(
            icon    = Icons.Outlined.Info,
            iconTint = IconPurple,
            iconBg  = BgPurple,
            title   = "About",
            // Show version inline so users don't have to open the page to check
            subtitle = "v${BuildConfig.VERSION_NAME} · Changelog",
            onClick = { onNavigate(Routes.ABOUT) }
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Dialogs and Bottom Sheets
    // Nothing below this line changes layout — all logic preserved as-is.
    // ─────────────────────────────────────────────────────────────────────────

    // ── Time Picker ───────────────────────────────────────────────────────────
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
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    showTimePicker = false
                    coroutineScope.launch {
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
                    // FIXED: was NothingRed — now uses theme primary (VinesGreen)
                    Text("Set", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(
                        "Cancel",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        )
    }

    // ── Currency Sheet ────────────────────────────────────────────────────────
    if (showCurrencySheet) {
        PickerBottomSheet(title = "Currency", onDismiss = { showCurrencySheet = false }) {
            Currency.entries.forEach { currency ->
                PickerRow(
                    label      = "${currency.code}  ${currency.symbol}",
                    isSelected = currency == currentCurrency,
                    onClick    = {
                        CurrencyManager.setCurrency(currency)
                        showCurrencySheet = false
                    }
                )
            }
        }
    }

    // ── Theme Sheet ───────────────────────────────────────────────────────────
    if (showThemeSheet) {
        PickerBottomSheet(title = "Theme", onDismiss = { showThemeSheet = false }) {
            // Maps each ThemeMode enum value to its display label.
            // ULTRA_DARK is shown as "Extra Dark" — more user-friendly than the enum name.
            ThemeMode.entries.forEach { mode ->
                val label = when (mode) {
                    ThemeMode.SYSTEM     -> "System (follow device)"
                    ThemeMode.LIGHT      -> "Light"
                    ThemeMode.DARK       -> "Dark"
                    ThemeMode.ULTRA_DARK -> "Extra Dark  (OLED)"
                }
                PickerRow(
                    label      = label,
                    isSelected = mode == currentTheme,
                    onClick    = {
                        showThemeSheet = false
                        coroutineScope.launch { ThemeManager.setThemeMode(context, mode) }
                    }
                )
            }
        }
    }

    // ── Number Format Sheet ───────────────────────────────────────────────────
    if (showNumberFormatSheet) {
        PickerBottomSheet(
            title     = "Number Format",
            onDismiss = { showNumberFormatSheet = false }
        ) {
            NumberFormat.entries.forEach { format ->
                PickerRow(
                    label      = "${format.label}  e.g. ${format.example}",
                    isSelected = (currentNumFormat ?: NumberFormat.INDIAN.name) == format.name,
                    onClick    = {
                        showNumberFormatSheet = false
                        NumberFormatManager.setFormat(format)
                    }
                )
            }
        }
    }

    // ── Export Sheet ──────────────────────────────────────────────────────────
    if (showExportSheet) {
        PickerBottomSheet(title = "Export Data", onDismiss = { showExportSheet = false }) {
            Text(
                text     = "Choose a format",
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
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

    // ── Import Sheet ──────────────────────────────────────────────────────────
    if (showImportSheet) {
        PickerBottomSheet(title = "Import Data", onDismiss = { showImportSheet = false }) {
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

    // ── Import Preview Sheet ──────────────────────────────────────────────────
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
                Text("Import Preview", style = MaterialTheme.typography.titleMedium)

                if (preview.accounts > 0)     Text("Accounts: ${preview.accounts}")
                if (preview.categories > 0)   Text("Categories: ${preview.categories}")
                if (preview.budgets > 0)      Text("Budgets: ${preview.budgets}")
                if (preview.transactions > 0) Text("Transactions: ${preview.transactions}")
                if (preview.validRows > 0)    Text("Valid rows: ${preview.validRows}")
                if (preview.skippedRows > 0) {
                    Text(
                        "Skipped rows: ${preview.skippedRows}",
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

    // ── Import progress overlay ───────────────────────────────────────────────
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
// Private composables
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Section label — small uppercase text above a group of rows.
 * Uses onBackground at reduced opacity so it recedes behind the rows.
 */
@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text     = text.uppercase(),
        style    = MaterialTheme.typography.labelSmall,
        color    = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
    )
}

/**
 * Standard settings row.
 *
 * Layout: [Icon] [Title + optional subtitle]  [optional value] [›]
 *
 * The icon sits in a small rounded square with a tinted background.
 * The value (if provided) is shown in primary green before the chevron —
 * so users can see what's currently set without opening the sheet.
 */
@Composable
private fun SettingsRow(
    icon     : ImageVector,
    iconTint : Color,
    iconBg   : Color,
    title    : String,
    subtitle : String?  = null,
    value    : String?  = null,
    onClick  : () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Coloured icon container
        Box(
            modifier        = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = iconTint,
                modifier           = Modifier.size(18.dp)
            )
        }

        // Title + subtitle
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text  = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }

        // Current value — green, aligned right before the chevron
        if (value != null) {
            Text(
                text  = value,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Chevron
        Icon(
            imageVector        = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
            modifier           = Modifier.size(18.dp)
        )
    }
}

/**
 * Settings row with a trailing Switch.
 * Two separate touch targets:
 *   - Row body  → onClick  (e.g. open time picker)
 *   - Switch    → onCheckedChange (toggle on/off)
 */
@Composable
private fun SettingsRowToggle(
    icon            : ImageVector,
    iconTint        : Color,
    iconBg          : Color,
    title           : String,
    subtitle        : String?  = null,
    checked         : Boolean,
    onClick         : () -> Unit,
    onCheckedChange : (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier         = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = iconTint,
                modifier           = Modifier.size(18.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text  = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }

        // FIXED: checkedTrackColor was NothingRed — now uses theme primary (VinesGreen)
        Switch(
            checked         = checked,
            onCheckedChange = onCheckedChange,
            colors          = SwitchDefaults.colors(
                checkedThumbColor   = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor   = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                uncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )
        )
    }
}

/**
 * Special "Support" row — stands apart from regular rows with a
 * faint green tint background and branded accent border.
 * Signals importance without being aggressive.
 */
@Composable
private fun SupportRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.07f))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Glowing dot — a small brand signal without full icon weight
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.primary)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = "Support TraceLedger",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text  = "UPI · PayPal — buy me a coffee",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        }

        Icon(
            imageVector        = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
            modifier           = Modifier.size(18.dp)
        )
    }

    Spacer(Modifier.height(4.dp))
}

/**
 * Reusable bottom sheet wrapper used by all picker sheets.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PickerBottomSheet(
    title     : String,
    onDismiss : () -> Unit,
    content   : @Composable ColumnScope.() -> Unit
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

/**
 * Single selectable row inside a picker bottom sheet.
 * Selected state: green tint background + green text + checkmark.
 */
@Composable
private fun PickerRow(
    label      : String,
    isSelected : Boolean,
    onClick    : () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                else Color.Transparent
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
            Icon(
                imageVector        = Icons.Outlined.Check,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Used inside Export and Import sheets to show a two-line option card.
 */
@Composable
private fun ExportOption(
    title       : String,
    description : String,
    onClick     : () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
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