package com.greenicephoenix.traceledger.feature.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
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
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: TransactionsViewModel,
    categories: List<CategoryUiModel>,
    accounts: List<AccountUiModel>,
    onBack: () -> Unit,
    onEditTransaction: (String) -> Unit
) {
    var selectedTransaction by remember { mutableStateOf<TransactionUiModel?>(null) }
    var showFilterSheet     by remember { mutableStateOf(false) }

    val currency            by CurrencyManager.currency.collectAsState()
    val month               by viewModel.selectedMonth.collectAsState()
    val groupedTransactions by viewModel.groupedTransactions.collectAsState()
    val searchQuery         by viewModel.searchQuery.collectAsState()
    val typeFilter          by viewModel.typeFilter.collectAsState()
    val totalIn             by viewModel.totalIn.collectAsState()
    val totalOut            by viewModel.totalOut.collectAsState()
    val hasActiveFilters    by viewModel.hasActiveFilters.collectAsState()
    val minAmount           by viewModel.minAmount.collectAsState()
    val maxAmount           by viewModel.maxAmount.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope             = rememberCoroutineScope()

    LaunchedEffect(accounts)   { viewModel.setAccounts(accounts)    }
    LaunchedEffect(categories) { viewModel.setCategories(categories) }

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding -> //HistoryScreen.kt: error here
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp)
        ) {

            // ── HEADER ────────────────────────────────────────────────────────
            Row(
                modifier          = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("TRANSACTIONS", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)

                // Filter button with badge when filters are active
                BadgedBox(
                    badge = {
                        if (hasActiveFilters) {
                            Badge(containerColor = NothingRed)
                        }
                    }
                ) {
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(
                            imageVector        = Icons.Default.FilterList,
                            contentDescription = "Filters",
                            tint               = if (hasActiveFilters) NothingRed
                            else MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── SEARCH ────────────────────────────────────────────────────────
            OutlinedTextField(
                value         = searchQuery,
                onValueChange = viewModel::updateSearch,
                placeholder   = {
                    Text("Search by amount, note, category…", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
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

            // ── TYPE CHIPS ────────────────────────────────────────────────────
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
                            Text(label, style = MaterialTheme.typography.labelMedium,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
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

            // ── TOTALS ────────────────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("In  ${CurrencyFormatter.format(totalIn.toPlainString(), currency)}", style = MaterialTheme.typography.bodySmall, color = SuccessGreen)
                Text("Out  ${CurrencyFormatter.format(totalOut.toPlainString(), currency)}", style = MaterialTheme.typography.bodySmall, color = NothingRed)
            }

            // Active amount filter indicator
            if (minAmount != null || maxAmount != null) {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text  = buildString {
                            append("Amount filter: ")
                            if (minAmount != null) append("≥ ${CurrencyFormatter.format(minAmount!!.toPlainString(), currency)}")
                            if (minAmount != null && maxAmount != null) append(" and ")
                            if (maxAmount != null) append("≤ ${CurrencyFormatter.format(maxAmount!!.toPlainString(), currency)}")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = NothingRed
                    )
                    TextButton(
                        onClick = { viewModel.clearAmountFilter() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text("Clear", style = MaterialTheme.typography.labelSmall, color = NothingRed)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── TRANSACTION LIST ──────────────────────────────────────────────
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(0.dp),
                contentPadding      = PaddingValues(bottom = 96.dp)
            ) {
                if (groupedTransactions.isEmpty()) {
                    item(key = "empty") {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text  = if (searchQuery.isBlank() && !hasActiveFilters) "No transactions this month"
                                else "No results matching current filters",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }
                }

                groupedTransactions.forEach { group ->
                    item(key = "header_${group.date}") { DateSectionHeader(group.date) }

                    items(group.transactions, key = { it.id }) { tx ->
                        val category = categories.firstOrNull { it.id == tx.categoryId }
                        val account  = when (tx.type) {
                            TransactionType.EXPENSE, TransactionType.TRANSFER -> accounts.firstOrNull { it.id == tx.fromAccountId }
                            TransactionType.INCOME -> accounts.firstOrNull { it.id == tx.toAccountId }
                        }
                        val displayTitle = if (tx.type == TransactionType.TRANSFER)
                            "Transfer → ${accounts.firstOrNull { it.id == tx.toAccountId }?.name ?: "Account"}"
                        else category?.name ?: "Category"

                        val categoryIcon = if (tx.type == TransactionType.TRANSFER) Icons.Default.SyncAlt
                        else CategoryIcons.all[category?.icon] ?: CategoryIcons.all["default"]!!
                        val iconColor    = if (tx.type == TransactionType.TRANSFER) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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

    // ── AMOUNT FILTER SHEET ───────────────────────────────────────────────────
    if (showFilterSheet) {
        AmountFilterSheet(
            currentMin = minAmount,
            currentMax = maxAmount,
            onApply    = { min, max ->
                viewModel.updateMinAmount(min)
                viewModel.updateMaxAmount(max)
                showFilterSheet = false
            },
            onClear    = {
                viewModel.clearAmountFilter()
                showFilterSheet = false
            },
            onDismiss  = { showFilterSheet = false }
        )
    }

    // ── DETAIL SHEET ──────────────────────────────────────────────────────────
    selectedTransaction?.let { tx ->
        val categoryName = if (tx.type == TransactionType.TRANSFER)
            "Transfer → ${accounts.firstOrNull { it.id == tx.toAccountId }?.name ?: "Account"}"
        else categories.firstOrNull { it.id == tx.categoryId }?.name ?: "Category"

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
            onEdit       = { selectedTransaction = null; onEditTransaction(tx.id) },
            onDelete     = { deletedTx ->
                viewModel.deleteTransaction(deletedTx)
                scope.launch { snackbarHostState.showSnackbar("Transaction deleted", withDismissAction = true) }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AmountFilterSheet
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AmountFilterSheet(
    currentMin: BigDecimal?,
    currentMax: BigDecimal?,
    onApply: (BigDecimal?, BigDecimal?) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    var minText by remember { mutableStateOf(currentMin?.toPlainString() ?: "") }
    var maxText by remember { mutableStateOf(currentMax?.toPlainString() ?: "") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Filter by Amount", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value         = minText,
                onValueChange = { if (it.isEmpty() || it.matches(Regex("""\d+(\.\d{0,2})?"""))) minText = it },
                label         = { Text("Minimum amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value         = maxText,
                onValueChange = { if (it.isEmpty() || it.matches(Regex("""\d+(\.\d{0,2})?"""))) maxText = it },
                label         = { Text("Maximum amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth()
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick  = onClear,
                    modifier = Modifier.weight(1f)
                ) { Text("Clear") }

                Button(
                    onClick  = {
                        val min = minText.toBigDecimalOrNull()
                        val max = maxText.toBigDecimalOrNull()
                        onApply(min, max)
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Apply") }
            }

            Spacer(Modifier.height(8.dp))
        }
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
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f))
    }
}