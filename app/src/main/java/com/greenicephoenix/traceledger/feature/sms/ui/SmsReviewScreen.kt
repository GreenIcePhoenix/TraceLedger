package com.greenicephoenix.traceledger.feature.sms.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenicephoenix.traceledger.core.database.entity.SmsPendingTransactionEntity
import com.greenicephoenix.traceledger.domain.model.AccountUiModel
import com.greenicephoenix.traceledger.domain.model.CategoryUiModel
import com.greenicephoenix.traceledger.feature.sms.viewmodel.SmsReviewViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SmsReviewScreen(
    viewModel: SmsReviewViewModel,
    accounts: List<AccountUiModel>,
    categories: List<CategoryUiModel>,
    onNavigateBack: () -> Unit,
) {
    val isLoading            by viewModel.isLoading.collectAsState()
    val items                by viewModel.pendingItems.collectAsState()
    val lastSavedDescription by viewModel.lastSavedDescription.collectAsState()

    val snackbarHostState    = remember { SnackbarHostState() }
    LaunchedEffect(lastSavedDescription) {
        lastSavedDescription?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSavedMessage()
        }
    }

    var editingItem          by remember { mutableStateOf<SmsPendingTransactionEntity?>(null) }
    var showRejectAllDialog  by remember { mutableStateOf(false) }

    // ── Dialogs ───────────────────────────────────────────────────────────────
    if (showRejectAllDialog) {
        AlertDialog(
            onDismissRequest = { showRejectAllDialog = false },
            title  = { Text("Dismiss all?") },
            text   = { Text("Mark all ${items.size} pending transactions as rejected.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.rejectAll()
                    showRejectAllDialog = false
                    onNavigateBack()
                }) { Text("Dismiss All", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showRejectAllDialog = false }) { Text("Cancel") }
            }
        )
    }

    editingItem?.let { item ->
        SmsEditSheet(
            item       = item,
            accounts   = accounts,
            categories = categories,
            onAccept   = { accountId, categoryId ->
                viewModel.acceptTransaction(item, accountId, categoryId)
                editingItem = null
            },
            onDismiss = { editingItem = null }
        )
    }

    // ── Root layout — matches CategoriesScreen pattern exactly ────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header (no TopAppBar / no Scaffold gap) ───────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint               = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                    ) {
                        Text(
                            text  = "REVIEW TRANSACTIONS",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (items.isNotEmpty()) {
                            Text(
                                text          = "${items.size} PENDING",
                                style         = MaterialTheme.typography.labelSmall,
                                color         = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                    if (items.isNotEmpty()) {
                        TextButton(onClick = { showRejectAllDialog = true }) {
                            Text(
                                "DISMISS ALL",
                                color         = MaterialTheme.colorScheme.error,
                                style         = MaterialTheme.typography.labelMedium,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }

            // ── Content ───────────────────────────────────────────────────────
            when {
                isLoading -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }

                items.isEmpty() -> EmptyReviewState(onNavigateBack = onNavigateBack)

                else -> LazyColumn(
                    modifier            = Modifier.fillMaxSize(),
                    contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items = items, key = { it.id }) { item ->
                        SmsReviewRow(
                            item       = item,
                            accounts   = accounts,
                            categories = categories,
                            onAccept   = { accountId, categoryId ->
                                viewModel.acceptTransaction(item, accountId, categoryId)
                            },
                            onReject   = { viewModel.rejectTransaction(item) },
                            onEdit     = { editingItem = item }
                        )
                    }
                    // Bottom spacer so last card isn't hidden behind nav bar
                    item { Spacer(Modifier.height(96.dp)) }
                }
            }
        }

        // ── Snackbar overlay (no Scaffold, so we place it manually) ──────────
        SnackbarHost(
            hostState = snackbarHostState,
            modifier  = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}

// ── Transaction row ───────────────────────────────────────────────────────────

@Composable
private fun SmsReviewRow(
    item: SmsPendingTransactionEntity,
    accounts: List<AccountUiModel>,
    categories: List<CategoryUiModel>,
    onAccept: (accountId: String, categoryId: String?) -> Unit,
    onReject: () -> Unit,
    onEdit: () -> Unit,
) {
    val isExpense   = item.parsedType == "EXPENSE"
    val accentColor = if (isExpense) MaterialTheme.colorScheme.error else Color(0xFF27AE60)

    val suggestedAccount  = accounts.find   { it.id == item.suggestedAccountId }
    val suggestedCategory = categories.find { it.id == item.suggestedCategoryId }
    var expanded          by remember { mutableStateOf(false) }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {

            // Left accent border — debit=red, credit=green
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accentColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                // Description + amount
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text  = formatDate(item.parsedDate),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text       = "${if (isExpense) "−" else "+"}${formatAmount(item.parsedAmount)}",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = accentColor
                    )
                }

                // Info chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    item.accountLastFour?.let  { InfoChip("···$it") }
                    suggestedCategory?.let     { InfoChip(it.name) }
                    suggestedAccount?.let      { InfoChip(it.name) }
                }

                // Expandable original SMS body
                Text(
                    text     = if (expanded) item.smsBody
                    else item.smsBody.take(72) + if (item.smsBody.length > 72) "…" else "",
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.clickable { expanded = !expanded }
                )
                if (item.smsBody.length > 72) {
                    Text(
                        text     = if (expanded) "Show less" else "Show original",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { expanded = !expanded }
                    )
                }

                // Action buttons — Save uses primary color (not debit/credit accent)
                val quickSaveAccountId = suggestedAccount?.id ?: accounts.firstOrNull()?.id

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier              = Modifier.fillMaxWidth()
                ) {
                    val canQuickSave = quickSaveAccountId != null && item.suggestedCategoryId != null

                    Button(
                        onClick  = {
                            if (canQuickSave) onAccept(quickSaveAccountId!!, item.suggestedCategoryId)
                            else onEdit()
                        },
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(10.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            when {
                                quickSaveAccountId == null -> "SELECT ACCOUNT"
                                item.suggestedCategoryId == null -> "SELECT CATEGORY"
                                else -> "SAVE"
                            },
                            style         = MaterialTheme.typography.labelMedium,
                            letterSpacing = 0.5.sp
                        )
                    }
                    OutlinedButton(
                        onClick  = onEdit,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(10.dp)
                    ) {
                        Text("EDIT", style = MaterialTheme.typography.labelMedium, letterSpacing = 0.5.sp)
                    }
                    IconButton(
                        onClick = onReject,
                        colors  = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                        )
                    ) {
                        Icon(Icons.Default.Close, "Reject", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

// ── Edit sheet ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SmsEditSheet(
    item: SmsPendingTransactionEntity,
    accounts: List<AccountUiModel>,
    categories: List<CategoryUiModel>,
    onAccept: (accountId: String, categoryId: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState         = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedAccountId  by remember {
        mutableStateOf(item.suggestedAccountId ?: accounts.firstOrNull()?.id ?: "")
    }
    var selectedCategoryId by remember { mutableStateOf<String?>(item.suggestedCategoryId) }

    val isExpense           = item.parsedType == "EXPENSE"
    val amountColor         = if (isExpense) MaterialTheme.colorScheme.error else Color(0xFF27AE60)
    val filteredCategories  = categories.filter { it.type.name == item.parsedType }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                "EDIT TRANSACTION",
                style         = MaterialTheme.typography.titleMedium,
                letterSpacing = 1.sp,
                fontWeight    = FontWeight.SemiBold
            )

            Text(
                "${if (isExpense) "−" else "+"}${formatAmount(item.parsedAmount)}",
                style      = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color      = amountColor
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            // Account section — all accounts, scrollable
            Text(
                "ACCOUNT",
                style         = MaterialTheme.typography.labelMedium,
                color         = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            accounts.forEach { account ->
                val isSelected = selectedAccountId == account.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            else Color.Transparent
                        )
                        .clickable { selectedAccountId = account.id }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick  = { selectedAccountId = account.id },
                        colors   = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                    )
                    Column {
                        Text(account.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(
                            account.type.name.replace("_", " "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            // Category section — all categories in FlowRow
            Text(
                "CATEGORY",
                style         = MaterialTheme.typography.labelMedium,
                color         = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement   = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategoryId == null,
                    onClick  = { selectedCategoryId = null },
                    label    = { Text("None", style = MaterialTheme.typography.labelMedium) }
                )
                filteredCategories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategoryId == cat.id,
                        onClick  = {
                            selectedCategoryId = if (selectedCategoryId == cat.id) null else cat.id
                        },
                        label = { Text(cat.name, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            Button(
                onClick  = { onAccept(selectedAccountId, selectedCategoryId) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled  = selectedAccountId.isNotBlank(),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("SAVE TRANSACTION", style = MaterialTheme.typography.labelLarge, letterSpacing = 1.sp)
            }
        }
    }
}

// ── Empty state ────────────────────────────────────────────────────────────────

@Composable
private fun EmptyReviewState(onNavigateBack: () -> Unit) {
    Column(
        modifier                = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement     = Arrangement.Center,
        horizontalAlignment     = Alignment.CenterHorizontally
    ) {
        Text("✓", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("ALL CAUGHT UP", style = MaterialTheme.typography.titleLarge, letterSpacing = 1.sp)
        Spacer(Modifier.height(8.dp))
        Text("No transactions pending review.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(28.dp))
        OutlinedButton(onClick = onNavigateBack, shape = RoundedCornerShape(12.dp)) {
            Text("BACK", letterSpacing = 1.sp)
        }
    }
}

// ── Shared helpers ─────────────────────────────────────────────────────────────

@Composable
private fun InfoChip(text: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(6.dp)) {
        Text(
            text     = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatAmount(amount: Double): String =
    NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(amount)

private fun formatDate(timestampMs: Long): String =
    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestampMs))