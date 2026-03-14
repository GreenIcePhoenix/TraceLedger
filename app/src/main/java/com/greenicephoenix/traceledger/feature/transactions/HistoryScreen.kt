package com.greenicephoenix.traceledger.feature.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.core.currency.CurrencyFormatter
import com.greenicephoenix.traceledger.core.currency.CurrencyManager
import com.greenicephoenix.traceledger.core.ui.theme.NothingRed
import com.greenicephoenix.traceledger.core.ui.theme.SuccessGreen
import com.greenicephoenix.traceledger.domain.model.AccountUiModel
import com.greenicephoenix.traceledger.domain.model.CategoryUiModel
import com.greenicephoenix.traceledger.domain.model.TransactionType
import com.greenicephoenix.traceledger.domain.model.TransactionUiModel
import com.greenicephoenix.traceledger.feature.categories.CategoryIcons
import com.greenicephoenix.traceledger.feature.transactions.components.TransactionDetailSheet
import com.greenicephoenix.traceledger.feature.transactions.components.TransactionRow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(
    viewModel: TransactionsViewModel,
    categories: List<CategoryUiModel>,
    accounts: List<AccountUiModel>,
    onBack: () -> Unit,
    onEditTransaction: (String) -> Unit
) {
    var selectedTransaction by remember { mutableStateOf<TransactionUiModel?>(null) }

    val currency by CurrencyManager.currency.collectAsState()
    val month by viewModel.selectedMonth.collectAsState()
    val groupedTransactions by viewModel.groupedTransactions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val typeFilter by viewModel.typeFilter.collectAsState()
    val totalIn  by viewModel.totalIn.collectAsState()
    val totalOut by viewModel.totalOut.collectAsState()

    // Snackbar for delete confirmation feedback
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(accounts)   { viewModel.setAccounts(accounts)    }
    LaunchedEffect(categories) { viewModel.setCategories(categories) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp)
        ) {

            // ── HEADER ────────────────────────────────────────────────────────
            Text(
                text  = "TRANSACTIONS",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(12.dp))

            // ── SEARCH ────────────────────────────────────────────────────────
            OutlinedTextField(
                value         = searchQuery,
                onValueChange = viewModel::updateSearch,
                placeholder   = {
                    Text(
                        "Search by amount, note, category…",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                },
                singleLine = true,
                modifier   = Modifier.fillMaxWidth(),
                colors     = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedTextColor     = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor   = MaterialTheme.colorScheme.onSurface,
                    cursorColor          = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(Modifier.height(12.dp))

            // ── TYPE FILTER CHIPS ─────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    null to "ALL",
                    TransactionType.EXPENSE  to "EXPENSE",
                    TransactionType.INCOME   to "INCOME",
                    TransactionType.TRANSFER to "TRANSFER"
                ).forEach { (type, label) ->
                    val selected = typeFilter == type
                    FilterChip(
                        selected = selected,
                        onClick  = { viewModel.updateTypeFilter(type) },
                        label    = {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── MONTH SELECTOR ────────────────────────────────────────────────
            com.greenicephoenix.traceledger.core.ui.components.MonthSelector(
                month      = month,
                onPrevious = { viewModel.goToPreviousMonth() },
                onNext     = { viewModel.goToNextMonth() }
            )

            Spacer(Modifier.height(8.dp))

            // ── MONTHLY TOTALS ────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text  = "In  ${CurrencyFormatter.format(totalIn.toPlainString(), currency)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SuccessGreen
                )
                Text(
                    text  = "Out  ${CurrencyFormatter.format(totalOut.toPlainString(), currency)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = NothingRed
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── GROUPED TRANSACTION LIST ──────────────────────────────────────
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(0.dp),
                contentPadding      = PaddingValues(bottom = 96.dp)
            ) {
                if (groupedTransactions.isEmpty()) {
                    item(key = "empty_state") {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text  = if (searchQuery.isBlank()) "No transactions this month"
                                else "No results for \"$searchQuery\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }
                }

                groupedTransactions.forEach { group ->
                    item(key = "header_${group.date}") {
                        DateSectionHeader(date = group.date)
                    }

                    items(group.transactions, key = { it.id }) { tx ->
                        val category = categories.firstOrNull { it.id == tx.categoryId }
                        val account  = when (tx.type) {
                            TransactionType.EXPENSE,
                            TransactionType.TRANSFER -> accounts.firstOrNull { it.id == tx.fromAccountId }
                            TransactionType.INCOME   -> accounts.firstOrNull { it.id == tx.toAccountId }
                        }
                        val displayTitle = if (tx.type == TransactionType.TRANSFER) {
                            "Transfer → ${accounts.firstOrNull { it.id == tx.toAccountId }?.name ?: "Account"}"
                        } else {
                            category?.name ?: "Category"
                        }
                        val categoryIcon  = if (tx.type == TransactionType.TRANSFER) Icons.Default.SyncAlt
                        else CategoryIcons.all[category?.icon] ?: CategoryIcons.all["default"]!!
                        val iconColor     = if (tx.type == TransactionType.TRANSFER) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        else Color(category?.color ?: 0xFF9E9E9E)

                        TransactionRow(
                            transaction   = tx,
                            categoryName  = displayTitle,
                            categoryIcon  = categoryIcon,
                            categoryColor = iconColor,
                            accountName   = account?.name ?: "Account",
                            amountText    = CurrencyFormatter.format(tx.amount.toPlainString(), currency),
                            onClick       = { selectedTransaction = tx }
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }

    // ── TRANSACTION DETAIL SHEET ──────────────────────────────────────────────
    selectedTransaction?.let { tx ->
        val categoryName = if (tx.type == TransactionType.TRANSFER) {
            "Transfer → ${accounts.firstOrNull { it.id == tx.toAccountId }?.name ?: "Account"}"
        } else {
            categories.firstOrNull { it.id == tx.categoryId }?.name ?: "Category"
        }
        val accountName = when (tx.type) {
            TransactionType.EXPENSE  -> accounts.firstOrNull { it.id == tx.fromAccountId }?.name
            TransactionType.INCOME   -> accounts.firstOrNull { it.id == tx.toAccountId }?.name
            TransactionType.TRANSFER -> accounts.firstOrNull { it.id == tx.fromAccountId }?.name
        } ?: "Account"

        TransactionDetailSheet(
            transaction  = tx,
            categoryName = categoryName,
            accountName  = accountName,
            onDismiss    = { selectedTransaction = null },
            onEdit       = {
                selectedTransaction = null
                onEditTransaction(tx.id)
            },
            // Phase 2: delete directly from the sheet and show a snackbar
            onDelete     = { deletedTx ->
                viewModel.deleteTransaction(deletedTx)
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message        = "Transaction deleted",
                        withDismissAction = true
                    )
                }
            }
        )
    }
}

@Composable
private fun DateSectionHeader(date: LocalDate) {
    val today     = LocalDate.now()
    val yesterday = today.minusDays(1)
    val label     = when (date) {
        today     -> "Today"
        yesterday -> "Yesterday"
        else      -> date.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()))
    }
    Box(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 6.dp)) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
        )
    }
}