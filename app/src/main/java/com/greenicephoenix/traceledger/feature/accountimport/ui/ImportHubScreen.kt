package com.greenicephoenix.traceledger.feature.accountimport.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenicephoenix.traceledger.domain.model.AccountType
import com.greenicephoenix.traceledger.domain.model.AccountUiModel

// Accepted MIME types for the system file picker.
// XLSX, CSV, and PDF are all accepted. We validate further after the user picks.
private val ACCEPTED_MIME_TYPES = arrayOf(
    // XLSX
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "application/vnd.ms-excel",
    // CSV
    "text/csv",
    "text/comma-separated-values",
    "text/plain",
    // PDF
    "application/pdf",
    // Generic fallback for file managers that return wrong MIME types
    "application/octet-stream",
    "*/*"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportHubScreen(
    accounts:    List<AccountUiModel>,
    onBack:      () -> Unit,
    onFileReady: (accountId: String, fileUri: Uri) -> Unit
) {
    var selectedAccount by remember { mutableStateOf<AccountUiModel?>(null) }
    var showAccountPicker by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val account = selectedAccount ?: return@rememberLauncherForActivityResult

        // Basic MIME validation — accept anything that could be a statement
        val mimeType = context.contentResolver.getType(uri) ?: ""
        val rejected = mimeType.isNotEmpty() &&
                mimeType !in listOf(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-excel",
            "text/csv", "text/comma-separated-values", "text/plain",
            "application/pdf", "application/octet-stream", "application/zip"
        ) &&
                !mimeType.startsWith("text/") &&
                !mimeType.contains("excel") &&
                !mimeType.contains("sheet")

        if (rejected) {
            errorMessage = "Unsupported file type. Please pick an Excel (.xlsx), CSV, or PDF file."
            return@rememberLauncherForActivityResult
        }

        errorMessage = null
        onFileReady(account.id, uri)
    }

    // ── Screen layout matches rest of app: dark background, no card wrappers ──
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header — same pattern as AccountsScreen, CategoriesScreen
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = "IMPORT TRANSACTIONS",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text  = "Bank & credit card statements",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {

            // ── Import warning ─────────────────────────────────────────────────────────
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
                        Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(top = 1.dp)
                    )

                    Spacer(Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Import Accuracy Notice",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text =
                                "Transaction import depends on the bank statement format and may not work correctly for all files. " +
                                        "Some statements may fail to detect or parse completely, certain transactions could be missed, " +
                                        "and calculated balances may not exactly match the original statement. " +
                                        "Please review imported transactions carefully before relying on the results.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // ── STEP 1 ────────────────────────────────────────────────────────
            item {
                StepSection(
                    number    = "1",
                    title     = "Select Account",
                    isComplete = selectedAccount != null
                ) {
                    Spacer(Modifier.height(12.dp))
                    if (selectedAccount != null) {
                        SelectedAccountRow(
                            account = selectedAccount!!,
                            onClick = { showAccountPicker = true }
                        )
                    } else {
                        OutlinedButton(
                            onClick = { showAccountPicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape    = RoundedCornerShape(12.dp),
                            border   = androidx.compose.foundation.BorderStroke(
                                1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                        ) {
                            Icon(
                                Icons.Default.AccountBalance,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Choose Account")
                        }
                    }
                }
            }

            // ── STEP 2 ────────────────────────────────────────────────────────
            item {
                StepSection(
                    number    = "2",
                    title     = "Pick Statement File",
                    isComplete = false
                ) {
                    Spacer(Modifier.height(8.dp))

                    // Supported formats info
                    SupportedFormatsInfo()

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick  = { filePickerLauncher.launch(ACCEPTED_MIME_TYPES) },
                        enabled  = selectedAccount != null,
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.FileOpen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (selectedAccount == null) "Select an account first"
                            else "Pick File (.xlsx / .csv / .pdf)"
                        )
                    }

                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter   = fadeIn(),
                        exit    = fadeOut()
                    ) {
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint     = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text  = errorMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // ── Privacy note ──────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.07f))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint     = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text  = "100% Private",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text  = "Your file is read on-device and never uploaded anywhere.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }

    // ── Account picker bottom sheet ───────────────────────────────────────────
    if (showAccountPicker) {
        AccountPickerSheet(
            accounts   = accounts,
            selectedId = selectedAccount?.id,
            onSelect   = { account ->
                selectedAccount   = account
                showAccountPicker = false
            },
            onDismiss = { showAccountPicker = false }
        )
    }
}

// ── Step section header ────────────────────────────────────────────────────────

@Composable
private fun StepSection(
    number:     String,
    title:      String,
    isComplete: Boolean,
    content:    @Composable ColumnScope.() -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Numbered circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        if (isComplete) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )
            ) {
                if (isComplete) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint     = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                } else {
                    Text(
                        text  = number,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text  = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold
            )
        }
        content()
    }
}

// ── Supported formats info ────────────────────────────────────────────────────

@Composable
private fun SupportedFormatsInfo() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // XLSX row
        FormatRow(
            icon        = "📊",
            format      = "Excel (.xlsx)",
            recommended = true
        )
        // CSV row
        FormatRow(
            icon   = "📄",
            format = "CSV"
        )
        // PDF row
        FormatRow(
            icon   = "📋",
            format = "PDF (text-based only)"
        )
    }
}

@Composable
private fun FormatRow(
    icon:        String,
    format:      String,
    recommended: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            .heightIn(min = 56.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = icon, fontSize = 16.sp, modifier = Modifier.padding(top = 1.dp))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text  = format,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                if (recommended) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text     = "Recommended",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

// ── Selected account row ──────────────────────────────────────────────────────

@Composable
private fun SelectedAccountRow(account: AccountUiModel, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(account.color).copy(alpha = 0.15f))
        ) {
            Icon(
                Icons.Default.AccountBalance,
                contentDescription = null,
                tint     = Color(account.color),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = account.name,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text  = account.type.displayName(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
        Icon(
            Icons.Default.SwapHoriz,
            contentDescription = "Change",
            tint     = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ── Account picker bottom sheet ───────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountPickerSheet(
    accounts:   List<AccountUiModel>,
    selectedId: String?,
    onSelect:   (AccountUiModel) -> Unit,
    onDismiss:  () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                text     = "Select Account",
                style    = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
            )
            HorizontalDivider()
            accounts.forEach { account ->
                val isSelected = account.id == selectedId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            else Color.Transparent
                        )
                        .clickable { onSelect(account) }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(account.color).copy(alpha = 0.15f))
                    ) {
                        Icon(
                            Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint     = Color(account.color),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text       = account.name,
                            style      = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis
                        )
                        Text(
                            text  = account.type.displayName(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    if (isSelected) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint     = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun AccountType.displayName() = when (this) {
    AccountType.BANK        -> "Bank Account"
    AccountType.WALLET      -> "Wallet"
    AccountType.CASH        -> "Cash"
    AccountType.CREDIT_CARD -> "Credit Card"
}