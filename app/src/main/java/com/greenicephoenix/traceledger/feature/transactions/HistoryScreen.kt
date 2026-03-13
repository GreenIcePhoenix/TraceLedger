package com.greenicephoenix.traceledger.feature.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.core.currency.CurrencyFormatter
import com.greenicephoenix.traceledger.core.currency.CurrencyManager
import com.greenicephoenix.traceledger.core.ui.theme.NothingRed
import com.greenicephoenix.traceledger.core.ui.theme.SuccessGreen  // FIX: use named constant
import com.greenicephoenix.traceledger.domain.model.AccountUiModel
import com.greenicephoenix.traceledger.domain.model.CategoryUiModel
import com.greenicephoenix.traceledger.domain.model.TransactionType
import com.greenicephoenix.traceledger.domain.model.TransactionUiModel
import com.greenicephoenix.traceledger.feature.categories.CategoryIcons
import com.greenicephoenix.traceledger.feature.transactions.components.TransactionDetailSheet
import com.greenicephoenix.traceledger.feature.transactions.components.TransactionRow

@Composable
fun HistoryScreen(
    viewModel: TransactionsViewModel,
    categories: List<CategoryUiModel>,
    accounts: List<AccountUiModel>,
    onBack: () -> Unit,
    onEditTransaction: (String) -> Unit
) {
    // FIX: selectedTransaction now actually gets set when a row is tapped.
    // Previously it was never set so the detail sheet was dead code.
    var selectedTransaction by remember { mutableStateOf<TransactionUiModel?>(null) }

    val currency by CurrencyManager.currency.collectAsState()
    val month by viewModel.selectedMonth.collectAsState()
    val transactions by viewModel.visibleTransactions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val typeFilter by viewModel.typeFilter.collectAsState()

    // FIX: These were collected but never displayed. Now shown in the summary row.
    val totalIn by viewModel.totalIn.collectAsState()
    val totalOut by viewModel.totalOut.collectAsState()

    LaunchedEffect(accounts) { viewModel.setAccounts(accounts) }
    LaunchedEffect(categories) { viewModel.setCategories(categories) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {

        // ── HEADER ────────────────────────────────────────────────────────────
        Text(
            text = "TRANSACTIONS",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(12.dp))

        // ── SEARCH ────────────────────────────────────────────────────────────
        OutlinedTextField(
            value = searchQuery,
            onValueChange = viewModel::updateSearch,
            placeholder = {
                Text(
                    "Search by amount, note, category…",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )

        Spacer(Modifier.height(12.dp))

        // ── TYPE FILTER CHIPS ─────────────────────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                null to "ALL",
                TransactionType.EXPENSE to "EXPENSE",
                TransactionType.INCOME to "INCOME",
                TransactionType.TRANSFER to "TRANSFER"
            ).forEach { (type, label) ->
                val selected = typeFilter == type
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.updateTypeFilter(type) },
                    label = {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── MONTH SELECTOR ────────────────────────────────────────────────────
        com.greenicephoenix.traceledger.core.ui.components.MonthSelector(
            month = month,
            onPrevious = { viewModel.goToPreviousMonth() },
            onNext = { viewModel.goToNextMonth() }
        )

        Spacer(Modifier.height(8.dp))

        // ── MONTHLY SUMMARY ROW ───────────────────────────────────────────────
        // FIX: totalIn and totalOut were computed but never displayed.
        // Now shown as a compact summary row below the month selector.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "In  ${CurrencyFormatter.format(totalIn.toPlainString(), currency)}",
                style = MaterialTheme.typography.bodySmall,
                color = SuccessGreen  // FIX: was Color(0xFF4CAF50) hardcoded
            )
            Text(
                text = "Out  ${CurrencyFormatter.format(totalOut.toPlainString(), currency)}",
                style = MaterialTheme.typography.bodySmall,
                color = NothingRed
            )
        }

        Spacer(Modifier.height(12.dp))

        // ── TRANSACTION LIST ──────────────────────────────────────────────────
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // FIX: Added key = { it.id } for stable Compose recomposition.
            // Without a key, Compose treats all items as potentially changed on
            // every list update, causing all visible rows to redraw unnecessarily.
            // With key, only actually changed/added/removed items recompose.
            // Also switched from index-based items(size) to items(list) which
            // is the correct modern Compose pattern.
            items(
                items = transactions,
                key = { tx -> tx.id }  // FIX: stable key for efficient recomposition
            ) { tx ->

                val category = categories.firstOrNull { it.id == tx.categoryId }
                val account = when (tx.type) {
                    TransactionType.EXPENSE,
                    TransactionType.TRANSFER -> accounts.firstOrNull { it.id == tx.fromAccountId }
                    TransactionType.INCOME   -> accounts.firstOrNull { it.id == tx.toAccountId }
                }

                val displayTitle = if (tx.type == TransactionType.TRANSFER) {
                    val toAccount = accounts.firstOrNull { it.id == tx.toAccountId }?.name ?: "Account"
                    "Transfer to $toAccount"
                } else {
                    category?.name ?: "Category"
                }

                val categoryIcon = if (tx.type == TransactionType.TRANSFER) {
                    Icons.Default.SyncAlt
                } else {
                    CategoryIcons.all[category?.icon] ?: CategoryIcons.all["default"]!!
                }

                val iconColor = if (tx.type == TransactionType.TRANSFER) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                } else {
                    Color(category?.color ?: 0xFF9E9E9E)
                }

                TransactionRow(
                    transaction = tx,
                    categoryName = displayTitle,
                    categoryIcon = categoryIcon,
                    categoryColor = iconColor,
                    accountName = account?.name ?: "Account",
                    amountText = CurrencyFormatter.format(tx.amount.toPlainString(), currency),
                    onClick = {
                        // FIX: Previously this called onEditTransaction() directly,
                        // making the detail sheet unreachable (selectedTransaction was
                        // never set). Now tapping a row opens the detail sheet.
                        // From the detail sheet, user can choose to Edit (navigates
                        // to edit screen) or Delete.
                        selectedTransaction = tx
                    }
                )
            }
        }
    }

    // ── TRANSACTION DETAIL SHEET ──────────────────────────────────────────────
    // Rendered outside the Column so it overlays the full screen correctly.
    // FIX: This was previously unreachable because selectedTransaction was never set.
    selectedTransaction?.let { tx ->

        val categoryName = if (tx.type == TransactionType.TRANSFER) {
            val toAccount = accounts.firstOrNull { it.id == tx.toAccountId }?.name ?: "Account"
            "Transfer to $toAccount"
        } else {
            categories.firstOrNull { it.id == tx.categoryId }?.name ?: "Category"
        }

        val accountName = when (tx.type) {
            TransactionType.EXPENSE  -> accounts.firstOrNull { it.id == tx.fromAccountId }?.name
            TransactionType.INCOME   -> accounts.firstOrNull { it.id == tx.toAccountId }?.name
            TransactionType.TRANSFER -> accounts.firstOrNull { it.id == tx.fromAccountId }?.name
        } ?: "Account"

        TransactionDetailSheet(
            transaction = tx,
            categoryName = categoryName,
            accountName = accountName,
            onDismiss = { selectedTransaction = null },
            onEdit = {
                selectedTransaction = null       // Close sheet first
                onEditTransaction(tx.id)         // Then navigate to edit screen
            }
        )
    }
}