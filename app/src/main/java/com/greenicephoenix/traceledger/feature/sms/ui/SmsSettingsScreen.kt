package com.greenicephoenix.traceledger.feature.sms.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.greenicephoenix.traceledger.core.navigation.Routes
import com.greenicephoenix.traceledger.feature.sms.viewmodel.InboxScanState
import com.greenicephoenix.traceledger.feature.sms.viewmodel.SmsSettingsViewModel

@Composable
fun SmsSettingsScreen(
    viewModel: SmsSettingsViewModel,
    onNavigate: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToReview: () -> Unit,
) {
    val state   by viewModel.state.collectAsState()
    val context = LocalContext.current

    val receiveSmsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) viewModel.setRealtimeEnabled(true) }

    var showScanDialog by remember { mutableStateOf(false) }
    val readSmsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) showScanDialog = true }

    if (showScanDialog) {
        InboxScanRangeDialog(
            onConfirm = { startMs, endMs ->
                showScanDialog = false
                viewModel.startInboxScan(startMs, endMs)
            },
            onDismiss = { showScanDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        // ── HEADER — matches ImportHubScreen exactly ──────────────────────────
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint               = MaterialTheme.colorScheme.onBackground
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = "SMS DETECTION",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text  = "Auto-detect bank & wallet transactions",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }

        // Matches ImportHubScreen's divider after header
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

        // ── CONTENT ───────────────────────────────────────────────────────────
        LazyColumn(
            contentPadding      = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // ── Privacy notice — matches ImportHubScreen warning card style ───
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector        = Icons.Default.Lock,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.secondary,
                        modifier           = Modifier.size(20.dp).padding(top = 1.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text       = "100% Private",
                            style      = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text  = "Your SMS messages are read and parsed entirely on your device. No data is sent to any server.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // ── Accuracy disclaimer — same card style, error tint ─────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.error.copy(alpha = 0.25f),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector        = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.error,
                        modifier           = Modifier.size(20.dp).padding(top = 1.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text       = "Detection Accuracy Notice",
                            style      = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text =
                                "Automatic detection may not capture every transaction or interpret all messages correctly. " +
                                        "The app learns from your corrections and improves over time — but may never reach 100% accuracy. " +
                                        "Always review transactions before saving.",
                            style      = MaterialTheme.typography.bodySmall,
                            color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // ── Pending badge ─────────────────────────────────────────────────
            if (state.pendingCount > 0) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                            .clickable { onNavigateToReview() }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "${state.pendingCount} transactions pending review",
                                style      = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color      = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Tap to review",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        FilledTonalButton(
                            onClick = onNavigateToReview,
                            shape   = RoundedCornerShape(10.dp)
                        ) { Text("REVIEW", letterSpacing = 0.5.sp) }
                    }
                }
            }

            // ── Real-time detection ───────────────────────────────────────────
            item {
                SmsSection(title = "REAL-TIME DETECTION") {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Detect incoming SMS", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                if (state.isRealtimeEnabled) "Active" else "Inactive",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (state.isRealtimeEnabled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.isRealtimeEnabled,
                            onCheckedChange = { wantEnabled ->
                                if (wantEnabled) {
                                    val perm = Manifest.permission.RECEIVE_SMS
                                    when (ContextCompat.checkSelfPermission(context, perm)) {
                                        PackageManager.PERMISSION_GRANTED -> viewModel.setRealtimeEnabled(true)
                                        else -> receiveSmsLauncher.launch(perm)
                                    }
                                } else {
                                    viewModel.setRealtimeEnabled(false)
                                }
                            }
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Automatically detects financial SMS as they arrive. " +
                                "Transactions are queued for your review — nothing is saved without your approval.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        lineHeight = 18.sp
                    )
                }
            }

            // ── Import past transactions ──────────────────────────────────────
            item {
                SmsSection(title = "IMPORT PAST TRANSACTIONS") {
                    when (val scanState = state.inboxScanState) {
                        is InboxScanState.Idle -> {
                            Button(
                                onClick = {
                                    val perm = Manifest.permission.READ_SMS
                                    when (ContextCompat.checkSelfPermission(context, perm)) {
                                        PackageManager.PERMISSION_GRANTED -> showScanDialog = true
                                        else -> readSmsLauncher.launch(perm)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape    = RoundedCornerShape(12.dp)
                            ) { Text("SCAN SMS INBOX", letterSpacing = 0.5.sp) }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Scans your inbox for past bank and wallet SMSes. All processing is on-device — nothing leaves your phone.",
                                style      = MaterialTheme.typography.bodySmall,
                                color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                lineHeight = 18.sp
                            )
                        }
                        is InboxScanState.Scanning -> {
                            val progress = if (scanState.total > 0) scanState.current.toFloat() / scanState.total else 0f
                            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(6.dp))
                            Text(
                                if (scanState.total > 0) "Scanning ${scanState.current} / ${scanState.total} messages…"
                                else "Starting scan…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        is InboxScanState.Done -> {
                            Text(
                                if (scanState.newItemsFound > 0) "Found ${scanState.newItemsFound} new transactions"
                                else "No new transactions found",
                                style      = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (scanState.newItemsFound > 0) {
                                    FilledTonalButton(
                                        onClick = onNavigateToReview,
                                        shape   = RoundedCornerShape(12.dp)
                                    ) { Text("REVIEW") }
                                }
                                OutlinedButton(
                                    onClick = { viewModel.resetScanState() },
                                    shape   = RoundedCornerShape(12.dp)
                                ) { Text("SCAN AGAIN") }
                            }
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
            }

            // ── Custom rules ──────────────────────────────────────────────────
            item {
                SmsSection(title = "CUSTOM RULES") {
                    Text(
                        "Define rules for SMS formats the built-in engine misses, or permanently exclude senders you never want tracked.",
                        style      = MaterialTheme.typography.bodySmall,
                        color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick  = { onNavigate(Routes.SMS_CUSTOM_RULES) },
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(12.dp)
                    ) { Text("MANAGE CUSTOM RULES", letterSpacing = 0.5.sp) }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ── Section wrapper — flat label + content card ───────────────────────────────
// Kept from original: groups related controls with a subtle label above

@Composable
private fun SmsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style         = MaterialTheme.typography.labelMedium,
            color         = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            fontWeight    = FontWeight.Medium,
            letterSpacing = 1.sp
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                .padding(16.dp)
        ) { content() }
    }
}

// ── Inbox scan dialog ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InboxScanRangeDialog(
    onConfirm: (startMs: Long, endMs: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedPreset   by remember { mutableIntStateOf(90) }
    var showDatePicker   by remember { mutableStateOf(false) }

    val presets = listOf(
        30  to "Last 30 days",
        90  to "Last 90 days",
        180 to "Last 6 months",
        365 to "Last 1 year",
    )

    // ── Custom date range picker ──────────────────────────────────────────────
    if (showDatePicker) {
        val now = System.currentTimeMillis()
        val pickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = now - java.util.concurrent.TimeUnit.DAYS.toMillis(90),
            initialSelectedEndDateMillis   = now
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val start = pickerState.selectedStartDateMillis
                        val end   = pickerState.selectedEndDateMillis ?: now
                        if (start != null) {
                            onConfirm(start, end)
                        }
                        showDatePicker = false
                    },
                    enabled = pickerState.selectedStartDateMillis != null
                ) { Text("Scan") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DateRangePicker(
                state    = pickerState,
                title    = { Text("Select scan range", modifier = Modifier.padding(start = 24.dp, top = 16.dp)) },
                headline = { Text("From – To", modifier = Modifier.padding(start = 24.dp, bottom = 8.dp)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
            )
        }
        return
    }

    // ── Preset picker ─────────────────────────────────────────────────────────
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("How far back to scan?") },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                presets.forEach { (days, label) ->
                    Row(
                        modifier          = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected  = selectedPreset == days,
                            onClick   = { selectedPreset = days }
                        )
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                HorizontalDivider(
                    modifier  = Modifier.padding(vertical = 4.dp),
                    color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )

                // Custom date range entry point
                TextButton(
                    onClick  = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Custom date range →",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val endMs   = System.currentTimeMillis()
                    val startMs = endMs - java.util.concurrent.TimeUnit.DAYS.toMillis(selectedPreset.toLong())
                    onConfirm(startMs, endMs)
                }
            ) { Text("Start Scan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}