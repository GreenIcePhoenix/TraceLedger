package com.greenicephoenix.traceledger.feature.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.core.navigation.Routes
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import com.greenicephoenix.traceledger.core.currency.Currency
import com.greenicephoenix.traceledger.core.currency.CurrencyManager
import com.greenicephoenix.traceledger.core.export.ExportFormat
import com.greenicephoenix.traceledger.core.importer.ImportPreview
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.greenicephoenix.traceledger.core.ui.theme.ThemeManager
import com.greenicephoenix.traceledger.core.ui.theme.ThemeMode


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
    val coroutineScope = rememberCoroutineScope()

    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var importPreview by remember { mutableStateOf<ImportPreview?>(null) }
    var showImportPreview by remember { mutableStateOf(false) }
    var importProgress by remember { mutableStateOf<Int?>(null) }

    val currentCurrency by CurrencyManager.currency.collectAsState()
    var showCurrencySheet by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var showImportSheet by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val currentTheme by ThemeManager
        .themeModeFlow(context)
        .collectAsState(initial = ThemeMode.DARK)

    var pendingExportFormat by remember { mutableStateOf<ExportFormat?>(null) }
    var pendingImportType by remember { mutableStateOf<ImportType?>(null) }

    val exportLauncher =
        rememberLauncherForActivityResult(
            contract = CreateDocument("*/*")
        ) { uri ->
            // User may cancel → uri == null
            if (uri != null && pendingExportFormat != null) {
                onExportUriReady(pendingExportFormat!!, uri)
            }
            pendingExportFormat = null
        }

    val importLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null && pendingImportType != null) {

                val type = pendingImportType ?: return@rememberLauncherForActivityResult

                when (type) {

                    ImportType.JSON -> {
                        pendingImportUri = uri
                        coroutineScope.launch {
                            try {
                                importPreview = onImportPreviewRequested(uri)
                                showImportPreview = true
                            } catch (e: Exception) {
                                onImportError(e.message ?: "Invalid backup file")
                            }
                        }
                    }

                    ImportType.CSV -> {
                        coroutineScope.launch {
                            try {
                                importPreview = onImportPreviewRequested(uri)
                                pendingImportUri = uri
                                showImportPreview = true
                            } catch (e: Exception) {
                                onImportError(e.message ?: "Invalid CSV file")
                            }
                        }
                    }

                }

                pendingImportType = null
            }
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            text = "SETTINGS",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ───────── Currency ─────────
        SettingsItem(
            title = "Currency",
            subtitle = "${currentCurrency.code} (${currentCurrency.symbol})",
            onClick = { showCurrencySheet = true }
        )

        // ───────── Theme ─────────
        SettingsItem(
            title = "Theme",
            subtitle = "Dark / Light",
            onClick = { showThemeSheet = true }
        )

        // ───────── Categories (existing) ─────────
        SettingsItem(
            title = "Categories",
            subtitle = "Manage income and expense categories",
            onClick = { onNavigate(Routes.CATEGORIES) }
        )

        // ───────── Budgets ─────────
        SettingsItem(
            title = "Budgets",
            subtitle = "Monthly spending limits",
            onClick = onBudgetsClick
        )

        // ───────── Recurring Transactions ─────────
        SettingsItem(
            title = "Recurring Transactions",
            subtitle = "Manage automatic repeating transactions",
            onClick = { onNavigate(Routes.RECURRING) }
        )

        // ───────── Export ─────────
        SettingsItem(
            title = "Export data",
            subtitle = "Backup your data as JSON or CSV",
            onClick = { showExportSheet = true }
        )

        // ───────── Import ─────────
        SettingsItem(
            title = "Import data",
            subtitle = "Restore data from a backup file",
            onClick = { showImportSheet = true }
        )

        // ───────── About ─────────
        SettingsItem(
            title = "About",
            subtitle = "Version, changelog, and app info",
            onClick = { onNavigate(Routes.ABOUT) }
        )


    }

    if (showCurrencySheet) {
        CurrencyPickerBottomSheet(
            selected = currentCurrency,
            onSelect = {
                CurrencyManager.setCurrency(it)
                showCurrencySheet = false
            },
            onDismiss = { showCurrencySheet = false }
        )
    }

    if (showExportSheet) {
        ModalBottomSheet(
            onDismissRequest = { showExportSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                Text(
                    text = "Export data",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Choose an export format",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(8.dp))

                ExportOption(
                    title = "JSON (recommended)",
                    description = "Full backup with all data and relationships",
                    onClick = {
                        showExportSheet = false
                        pendingExportFormat = ExportFormat.JSON
                        exportLauncher.launch(
                            exportDefaultFileName(ExportFormat.JSON)
                        )
                    }
                )

                ExportOption(
                    title = "CSV",
                    description = "Transactions only, for spreadsheets",
                    onClick = {
                        showExportSheet = false
                        pendingExportFormat = ExportFormat.CSV
                        exportLauncher.launch(
                            exportDefaultFileName(ExportFormat.CSV)
                        )
                    }
                )
            }
        }
    }

    if (showImportSheet) {
        ModalBottomSheet(
            onDismissRequest = { showImportSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                Text(
                    text = "Import data",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                ImportOption(
                    title = "JSON (full restore)",
                    description = "Restores all accounts, categories, budgets, and transactions",
                    onClick = {
                        pendingImportType = ImportType.JSON
                        showImportSheet = false
                        importLauncher.launch(arrayOf("application/json"))
                    }
                )

                ImportOption(
                    title = "CSV (transactions only)",
                    description = "Imports transactions into existing accounts and categories",
                    onClick = {
                        pendingImportType = ImportType.CSV
                        showImportSheet = false
                        importLauncher.launch(arrayOf("text/csv"))
                    }
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "CSV import will skip rows with unknown accounts or categories.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }

    if (showImportPreview && importPreview != null) {
        ModalBottomSheet(
            onDismissRequest = {
                showImportPreview = false
                importPreview = null
                pendingImportUri = null
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                val canImport = importPreview?.let {
                    // JSON always allowed
                    it.totalRows == 0 || it.validRows > 0
                } ?: false

                Text(
                    text = "Import preview",
                    style = MaterialTheme.typography.titleMedium
                )

                importPreview?.let { preview ->
                    Text("Accounts: ${preview.accounts}")
                    Text("Categories: ${preview.categories}")
                    Text("Budgets: ${preview.budgets}")
                    Text("Transactions: ${preview.transactions}")
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "This will replace all existing data.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(Modifier.height(8.dp))

                if (!canImport) {
                    Text(
                        text = "No valid rows found. Import is disabled.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {

                    TextButton(
                        onClick = {
                            showImportPreview = false
                            importPreview = null
                            pendingImportUri = null
                        }
                    ) {
                        Text("Cancel")
                    }

                    Spacer(Modifier.width(8.dp))

                    TextButton(
                        enabled = canImport,
                        onClick = {
                            val uri = pendingImportUri ?: return@TextButton
                            showImportPreview = false
                            onImportConfirmed(uri) { progress ->
                                importProgress = progress
                            }
                        }
                    ) {
                        Text(
                            text = "Import",
                            color = if (canImport)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }

    if (showThemeSheet) {
        ThemePickerBottomSheet(
            selected = currentTheme,
            onSelect = { mode ->
                showThemeSheet = false
                coroutineScope.launch {
                    ThemeManager.setThemeMode(context, mode)
                }
            },
            onDismiss = { showThemeSheet = false }
        )
    }

    importProgress?.let { progress ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    MaterialTheme.colorScheme.background.copy(alpha = 0.85f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(24.dp)
            ) {

                Text(
                    text = "Importing data…",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium
                )

                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = ProgressIndicatorDefaults.linearColor,
                    trackColor = ProgressIndicatorDefaults.linearTrackColor,
                    strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                )

                Text(
                    text = "$progress%",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    LaunchedEffect(importProgress) {
        if (importProgress != null && importProgress!! >= 100) {
            // Small delay so user sees completion
            kotlinx.coroutines.delay(400)

            importProgress = null
            pendingImportUri = null
        }
    }

}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyPickerBottomSheet(
    selected: Currency,
    onSelect: (Currency) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Currency.entries.forEach { currency ->
                val isSelected = currency == selected

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isSelected)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            else Color.Transparent,
                            shape = MaterialTheme.shapes.medium
                        )
                        .clickable { onSelect(currency) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${currency.code} (${currency.symbol})",
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemePickerBottomSheet(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeMode.entries.forEach { mode ->
                val isSelected = mode == selected

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isSelected)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            else Color.Transparent,
                            shape = MaterialTheme.shapes.medium
                        )
                        .clickable { onSelect(mode) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (mode) {
                            ThemeMode.DARK -> "Dark"
                            ThemeMode.LIGHT -> "Light"
                        },
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}


@Composable
private fun ExportOption(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

private fun exportMimeType(format: ExportFormat): String =
    when (format) {
        ExportFormat.JSON -> "application/json"
        ExportFormat.CSV -> "text/csv"
    }

private fun exportDefaultFileName(format: ExportFormat): String =
    when (format) {
        ExportFormat.JSON -> "TraceLedger-backup.json"
        ExportFormat.CSV -> "TraceLedger-transactions.csv"
    }

@Composable
private fun ImportOption(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}