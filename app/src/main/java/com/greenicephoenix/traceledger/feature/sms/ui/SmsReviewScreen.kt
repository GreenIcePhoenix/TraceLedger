package com.greenicephoenix.traceledger.feature.sms.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.core.database.entity.SmsPendingTransactionEntity
import com.greenicephoenix.traceledger.domain.model.AccountUiModel
import com.greenicephoenix.traceledger.domain.model.CategoryUiModel
import com.greenicephoenix.traceledger.feature.sms.viewmodel.SmsReviewViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsReviewScreen(
    viewModel: SmsReviewViewModel,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show snackbar when a transaction is saved
    LaunchedEffect(state.lastSavedDescription) {
        state.lastSavedDescription?.let { desc ->
            snackbarHostState.showSnackbar("Saved: $desc")
            viewModel.clearSavedMessage()
        }
    }

    // State for the inline edit sheet
    var editingItem by remember { mutableStateOf<SmsPendingTransactionEntity?>(null) }
    var showRejectAllDialog by remember { mutableStateOf(false) }

    if (showRejectAllDialog) {
        AlertDialog(
            onDismissRequest = { showRejectAllDialog = false },
            title = { Text("Dismiss all?") },
            text = { Text("This will mark all ${state.items.size} pending transactions as rejected. You can't undo this.") },
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

    // Edit bottom sheet
    editingItem?.let { item ->
        SmsEditSheet(
            item = item,
            accounts = state.accounts,
            categories = state.categories,
            onAccept = { accountId, categoryId ->
                viewModel.acceptTransaction(item, accountId, categoryId)
                editingItem = null
            },
            onDismiss = { editingItem = null }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Review Transactions")
                        if (state.items.isNotEmpty()) {
                            Text(
                                "${state.items.size} pending",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (state.items.isNotEmpty()) {
                        TextButton(onClick = { showRejectAllDialog = true }) {
                            Text("Dismiss All", color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (state.items.isEmpty()) {
            EmptyReviewState(onNavigateBack = onNavigateBack)
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = state.items,
                key = { it.id }
            ) { item ->
                SmsReviewRow(
                    item = item,
                    accounts = state.accounts,
                    categories = state.categories,
                    onAccept = { accountId, categoryId ->
                        viewModel.acceptTransaction(item, accountId, categoryId)
                    },
                    onReject = { viewModel.rejectTransaction(item) },
                    onEdit = { editingItem = item }
                )
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun SmsReviewRow(
    item: SmsPendingTransactionEntity,
    accounts: List<AccountUiModel>,
    categories: List<CategoryUiModel>,
    onAccept: (accountId: Long, categoryId: Long?) -> Unit,
    onReject: () -> Unit,
    onEdit: () -> Unit,
) {
    val isExpense = item.parsedType == "EXPENSE"
    val accentColor = if (isExpense) MaterialTheme.colorScheme.error
    else Color(0xFF27AE60)

    var expanded by remember { mutableStateOf(false) }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            // --- Colored top border ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(accentColor)
            )

            Column(modifier = Modifier.padding(12.dp)) {
                // --- Header row: description + amount ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            item.parsedDescription,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            formatDate(item.parsedDate),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "${if (isExpense) "−" else "+"}${formatAmount(item.parsedAmount)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // --- Tags row: account last4, category ---
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item.accountLastFour?.let { last4 ->
                        InfoChip(text = "···$last4")
                    }
                    val category = categories.find { it.id == item.suggestedCategoryId }
                    category?.let { InfoChip(text = it.name) }
                    val account = accounts.find { it.id == item.suggestedAccountId }
                    account?.let { InfoChip(text = it.name) }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // --- Expandable SMS body ---
                Text(
                    if (expanded) item.smsBody
                    else item.smsBody.take(80) + if (item.smsBody.length > 80) "…" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { expanded = !expanded }
                )
                if (item.smsBody.length > 80) {
                    Text(
                        if (expanded) "Show less" else "Show original",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { expanded = !expanded }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // --- Action buttons ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Save button — quick save with suggested account/category
                    val suggestedAccountId = item.suggestedAccountId
                        ?: accounts.firstOrNull()?.id

                    Button(
                        onClick = {
                            if (suggestedAccountId != null) {
                                onAccept(suggestedAccountId, item.suggestedCategoryId)
                            } else {
                                onEdit() // No account suggestion → force user to pick
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor
                        )
                    ) {
                        Text(
                            if (suggestedAccountId != null) "Save" else "Select Account",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    // Edit button
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Edit", style = MaterialTheme.typography.labelMedium)
                    }

                    // Reject icon
                    IconButton(
                        onClick = onReject,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(Icons.Default.Close, "Reject")
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoChip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SmsEditSheet(
    item: SmsPendingTransactionEntity,
    accounts: List<AccountUiModel>,
    categories: List<CategoryUiModel>,
    onAccept: (accountId: Long, categoryId: Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedAccountId by remember {
        mutableStateOf(item.suggestedAccountId ?: accounts.firstOrNull()?.id ?: 0L)
    }
    var selectedCategoryId by remember { mutableStateOf(item.suggestedCategoryId) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Edit before saving",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(item.parsedDescription, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${if (item.parsedType == "EXPENSE") "−" else "+"}${formatAmount(item.parsedAmount)}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (item.parsedType == "EXPENSE") MaterialTheme.colorScheme.error
                else Color(0xFF27AE60)
            )

            // Account picker
            Text("Save to account", style = MaterialTheme.typography.labelMedium)
            accounts.forEach { account ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedAccountId = account.id }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedAccountId == account.id,
                        onClick = { selectedAccountId = account.id }
                    )
                    Text(account.name, style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Category picker
            Text("Category", style = MaterialTheme.typography.labelMedium)
            val filtered = categories.filter { it.type == item.parsedType }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                filtered.take(6).forEach { cat ->
                    FilterChip(
                        selected = selectedCategoryId == cat.id,
                        onClick = { selectedCategoryId = if (selectedCategoryId == cat.id) null else cat.id },
                        label = { Text(cat.name, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            Button(
                onClick = { onAccept(selectedAccountId, selectedCategoryId) },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedAccountId != 0L
            ) {
                Text("Save Transaction")
            }
        }
    }
}

@Composable
private fun EmptyReviewState(onNavigateBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("✓", style = MaterialTheme.typography.displayLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "All caught up!",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "No transactions pending review.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(onClick = onNavigateBack) { Text("Back") }
    }
}

// ---- Formatting helpers ----
private fun formatAmount(amount: Double): String =
    NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(amount)

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))