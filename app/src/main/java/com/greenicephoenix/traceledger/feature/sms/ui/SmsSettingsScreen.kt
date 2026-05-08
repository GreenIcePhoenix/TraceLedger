package com.greenicephoenix.traceledger.feature.sms.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.greenicephoenix.traceledger.feature.sms.viewmodel.InboxScanState
import com.greenicephoenix.traceledger.feature.sms.viewmodel.SmsSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsSettingsScreen(
    viewModel: SmsSettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToReview: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    // ---- Permission launchers ----
    // RECEIVE_SMS: for real-time detection
    val receiveSmsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.setRealtimeEnabled(true)
    }

    // READ_SMS: for inbox scan
    var showScanDialog by remember { mutableStateOf(false) }
    val readSmsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) showScanDialog = true
    }

    // ---- Inbox scan range dialog ----
    if (showScanDialog) {
        InboxScanRangeDialog(
            onConfirm = { days ->
                showScanDialog = false
                viewModel.startInboxScan(days)
            },
            onDismiss = { showScanDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SMS Detection") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ---- Privacy note ----
            PrivacyNoticeCard()

            // ---- Pending badge ----
            AnimatedVisibility(visible = state.pendingCount > 0) {
                PendingCountCard(
                    count = state.pendingCount,
                    onClick = onNavigateToReview
                )
            }

            // ---- Real-time detection toggle ----
            SmsSection(title = "Real-Time Detection") {
                RealtimeToggleRow(
                    isEnabled = state.isRealtimeEnabled,
                    onToggle = { wantEnabled ->
                        if (wantEnabled) {
                            val permission = Manifest.permission.RECEIVE_SMS
                            when (ContextCompat.checkSelfPermission(context, permission)) {
                                PackageManager.PERMISSION_GRANTED -> viewModel.setRealtimeEnabled(true)
                                else -> receiveSmsLauncher.launch(permission)
                            }
                        } else {
                            viewModel.setRealtimeEnabled(false)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Automatically detects financial SMS as they arrive. " +
                            "Transactions are queued for your review — nothing is saved automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ---- Inbox scan ----
            SmsSection(title = "Import Past Transactions") {
                when (val scanState = state.inboxScanState) {
                    is InboxScanState.Idle -> {
                        FilledTonalButton(
                            onClick = {
                                val permission = Manifest.permission.READ_SMS
                                when (ContextCompat.checkSelfPermission(context, permission)) {
                                    PackageManager.PERMISSION_GRANTED -> showScanDialog = true
                                    else -> readSmsLauncher.launch(permission)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Scan SMS Inbox")
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Scans your inbox for past bank/wallet SMSes and adds them " +
                                    "to the review queue. Reads locally — nothing leaves your device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    is InboxScanState.Scanning -> {
                        ScanProgressRow(current = scanState.current, total = scanState.total)
                    }
                    is InboxScanState.Done -> {
                        ScanDoneRow(
                            count = scanState.newItemsFound,
                            onViewResults = onNavigateToReview,
                            onScanAgain = { viewModel.resetScanState() }
                        )
                    }
                    is InboxScanState.Error -> {
                        Text(
                            "Scan failed: ${scanState.message}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        TextButton(onClick = { viewModel.resetScanState() }) { Text("Retry") }
                    }
                }
            }

            // ---- Custom rules placeholder ----
            SmsSection(title = "Custom Rules") {
                Text(
                    "Define your own parsing rules for SMS formats not covered by the " +
                            "built-in engine. Coming soon.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ---- Sub-components ----

@Composable
private fun PrivacyNoticeCard() {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text("🔒", style = MaterialTheme.typography.titleLarge)
            Column {
                Text(
                    "100% Private",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    "Your SMS messages are read and parsed entirely on your device. " +
                            "No data is sent to any server.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun PendingCountCard(count: Int, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "$count transactions pending review",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Tap to review",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalButton(onClick = onClick) { Text("Review") }
        }
    }
}

@Composable
private fun SmsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) { content() }
        }
    }
}

@Composable
private fun RealtimeToggleRow(isEnabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Detect incoming SMS", style = MaterialTheme.typography.bodyMedium)
            Text(
                if (isEnabled) "Active" else "Inactive",
                style = MaterialTheme.typography.bodySmall,
                color = if (isEnabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = isEnabled, onCheckedChange = onToggle)
    }
}

@Composable
private fun ScanProgressRow(current: Int, total: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val progress = if (total > 0) current.toFloat() / total else 0f
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            if (total > 0) "Scanning $current / $total messages…"
            else "Starting scan…",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ScanDoneRow(count: Int, onViewResults: () -> Unit, onScanAgain: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            if (count > 0) "Found $count new transactions" else "No new transactions found",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (count > 0) {
                FilledTonalButton(onClick = onViewResults) { Text("Review") }
            }
            OutlinedButton(onClick = onScanAgain) { Text("Scan Again") }
        }
    }
}

@Composable
private fun InboxScanRangeDialog(
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedDays by remember { mutableIntStateOf(90) }
    val options = listOf(30 to "Last 30 days", 90 to "Last 90 days", 180 to "Last 6 months", 365 to "Last 1 year")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("How far back to scan?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEach { (days, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedDays == days,
                            onClick = { selectedDays = days }
                        )
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedDays) }) { Text("Start Scan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}