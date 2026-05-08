package com.greenicephoenix.traceledger.feature.accountimport.ui

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.greenicephoenix.traceledger.core.currency.CurrencyFormatter
import com.greenicephoenix.traceledger.core.currency.CurrencyManager
import com.greenicephoenix.traceledger.domain.model.AccountUiModel
import com.greenicephoenix.traceledger.domain.model.CategoryUiModel
import com.greenicephoenix.traceledger.feature.accountimport.model.BalanceStrategy
import com.greenicephoenix.traceledger.feature.accountimport.model.ImportReviewItem
import com.greenicephoenix.traceledger.feature.accountimport.viewmodel.ImportReviewState
import com.greenicephoenix.traceledger.feature.accountimport.viewmodel.ReviewFilter
import com.greenicephoenix.traceledger.feature.accountimport.viewmodel.StatementImportViewModel
import com.greenicephoenix.traceledger.feature.accountimport.viewmodel.StatementImportViewModelFactory
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

private val AmberWarning = Color(0xFFF59E0B)
private val IncomeGreen  = Color(0xFF27AE60)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportReviewScreen(
    accountId:    String,
    fileUri:      Uri,
    accounts:     List<AccountUiModel>,
    categories:   List<CategoryUiModel>,
    vmFactory:    StatementImportViewModelFactory,
    onBack:       () -> Unit,
    onRetry:      () -> Unit,
    onImportDone: (imported: Int, skipped: Int, duplicates: Int) -> Unit
) {
    val viewModel: StatementImportViewModel = viewModel(factory = vmFactory)
    val state by viewModel.state.collectAsState()

    val account     = accounts.firstOrNull { it.id == accountId }
    val categoryMap = remember(categories) { categories.associateBy { it.id } }

    var editingItemId    by remember { mutableStateOf<String?>(null) }
    var showBalanceSheet by remember { mutableStateOf(false) }

    LaunchedEffect(fileUri, accountId) {
        if (account != null) viewModel.startParsing(fileUri, account, categories)
    }

    LaunchedEffect(state) {
        if (state is ImportReviewState.Completed) {
            val done = state as ImportReviewState.Completed
            onImportDone(done.imported, done.skipped, done.duplicates)
        }
    }

    // ── Screen layout — flat dark, matches Categories/Accounts screens ─────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header — identical pattern to AccountsScreen / CategoriesScreen
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
                    text  = "REVIEW IMPORT",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (state is ImportReviewState.Reviewing) {
                    val s = state as ImportReviewState.Reviewing
                    Text(
                        text  = "${s.format.displayName} · ${s.account.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

        // Content area
        Box(modifier = Modifier.weight(1f)) {
            when (val s = state) {
                is ImportReviewState.Idle,
                is ImportReviewState.Parsing    -> ParsingView()
                is ImportReviewState.ParseError -> ParseErrorView(s.message, onBack, onRetry)
                is ImportReviewState.NeedsPassword -> ParsingView() // dialog shown separately
                is ImportReviewState.Reviewing  -> ReviewingContent(
                    reviewing      = s,
                    categoryMap    = categoryMap,
                    onFilterChange = { viewModel.setFilter(it) },
                    onToggle       = { viewModel.toggleIncluded(it) },
                    onCategoryTap  = { editingItemId = it },
                    onExcludeDuplicates = { viewModel.excludeAllDuplicates() }
                )
                is ImportReviewState.Importing  -> ImportingView(s)
                is ImportReviewState.Completed  -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Bottom action bar — only in Reviewing state
        if (state is ImportReviewState.Reviewing) {
            val reviewing = state as ImportReviewState.Reviewing
            BottomActionBar(
                reviewing          = reviewing,
                onShowBalanceSheet = { showBalanceSheet = true },
                onConfirm          = { viewModel.confirmImport() }
            )
        }
    }

    // ── Password dialog ───────────────────────────────────────────────────────
    if (state is ImportReviewState.NeedsPassword) {
        val needs = state as ImportReviewState.NeedsPassword
        PasswordDialog(
            wasWrong  = needs.wasWrongPassword,
            onConfirm = { viewModel.retryWithPassword(it) },
            onDismiss = onRetry  // "Cancel" goes back to import hub
        )
    }

    // ── Category picker ───────────────────────────────────────────────────────
    val editingItem = (state as? ImportReviewState.Reviewing)
        ?.items?.firstOrNull { it.id == editingItemId }

    if (editingItemId != null && editingItem != null) {
        ImportCategoryPickerSheet(
            isCredit   = editingItem.parsed.isCredit,
            categories = categories,
            currentId  = editingItem.categoryId,
            onSelect   = { categoryId ->
                viewModel.updateCategory(editingItemId!!, categoryId)
                editingItemId = null
            },
            onDismiss = { editingItemId = null }
        )
    }

    // ── Balance strategy sheet ────────────────────────────────────────────────
    if (showBalanceSheet && state is ImportReviewState.Reviewing) {
        BalanceStrategySheet(
            reviewing              = state as ImportReviewState.Reviewing,
            onStrategyChange       = { viewModel.setBalanceStrategy(it) },
            onClosingBalanceChange = { viewModel.updateClosingBalanceInput(it) },
            onDismiss              = { showBalanceSheet = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// State views
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ParsingView() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Text(
                "Reading your statement…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun ParseErrorView(message: String, onBack: () -> Unit, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.ErrorOutline, null,
            tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(16.dp))
        Text("Could not read statement", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry, shape = RoundedCornerShape(12.dp)) {
            Text("Try a different file")
        }
    }
}

@Composable
private fun ImportingView(state: ImportReviewState.Importing) {
    val progress = if (state.total > 0) state.current.toFloat() / state.total else 0f
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp)) {
            CircularProgressIndicator(progress = { progress },
                color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
            Text("Importing ${state.current} of ${state.total}…",
                style = MaterialTheme.typography.bodyMedium)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Main reviewing content
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ReviewingContent(
    reviewing:          ImportReviewState.Reviewing,
    categoryMap:        Map<String, CategoryUiModel>,
    onFilterChange:     (ReviewFilter) -> Unit,
    onToggle:           (String) -> Unit,
    onCategoryTap:      (String) -> Unit,
    onExcludeDuplicates: () -> Unit
) {
    val currency by CurrencyManager.currency.collectAsState()

    LazyColumn(
        contentPadding     = PaddingValues(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Summary strip
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryCell("Selected", "${reviewing.includedItems.size} / ${reviewing.items.size}")
                SummaryCell("Bank", reviewing.format.displayName)
                val net = reviewing.totalAmount
                SummaryCell(
                    label      = "Net",
                    value      = CurrencyFormatter.format(net.abs().toPlainString(), currency),
                    valueColor = if (net >= BigDecimal.ZERO) IncomeGreen
                    else MaterialTheme.colorScheme.error
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        }

        // Filter tabs — pill style matching EXPENSE/INCOME selector
        item {
            FilterTabRow(
                current        = reviewing.filterMode,
                totalCount     = reviewing.items.size,
                includedCount  = reviewing.includedItems.size,
                duplicateCount = reviewing.duplicateCount,
                onSelect       = onFilterChange
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
        }

        // Hint — shown once to orient the user
        item {
            Text(
                text     = "Tap any category chip to change it  ·  Uncheck rows to exclude them",
                style    = MaterialTheme.typography.labelSmall,
                color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        // Warnings
        if (reviewing.duplicateCount > 0) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AmberWarning.copy(alpha = 0.08f))
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, null, tint = AmberWarning, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${reviewing.duplicateCount} possible duplicate${if (reviewing.duplicateCount > 1) "s" else ""}",
                        style = MaterialTheme.typography.labelMedium,
                        color = AmberWarning,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onExcludeDuplicates,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                        Text("Exclude all", style = MaterialTheme.typography.labelSmall, color = AmberWarning)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            }
        }

        if (reviewing.dateErrorCount > 0) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ErrorOutline, null,
                        tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${reviewing.dateErrorCount} row${if (reviewing.dateErrorCount > 1) "s" else ""} skipped — unreadable date",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            }
        }

        // Transaction rows
        if (reviewing.filteredItems.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Text("No transactions match this filter",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f))
                }
            }
        } else {
            items(reviewing.filteredItems, key = { it.id }) { item ->
                TransactionRow(
                    item          = item,
                    categoryMap   = categoryMap,
                    currency      = currency,
                    onToggle      = { onToggle(item.id) },
                    onCategoryTap = { onCategoryTap(item.id) }
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    modifier = Modifier.padding(start = 20.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Transaction row — flat, no card, left colour border
// ─────────────────────────────────────────────────────────────────────────────

private val DATE_FMT = DateTimeFormatter.ofPattern("dd MMM")

@Composable
private fun TransactionRow(
    item:          ImportReviewItem,
    categoryMap:   Map<String, CategoryUiModel>,
    currency:      com.greenicephoenix.traceledger.core.currency.Currency,
    onToggle:      () -> Unit,
    onCategoryTap: () -> Unit
) {
    val leftColor by animateColorAsState(
        targetValue = when {
            item.hasDateError -> MaterialTheme.colorScheme.error
            item.isDuplicate  -> AmberWarning
            item.parsed.isCredit -> IncomeGreen
            else -> MaterialTheme.colorScheme.error
        },
        label = "leftBorder"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (item.isIncluded && !item.hasDateError) 1f else 0.4f),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Coloured left border — 3dp, full row height
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(56.dp)
                .background(leftColor)
        )

        Row(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Date
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(36.dp)
            ) {
                if (item.hasDateError) {
                    Text("ERR", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error, fontSize = 9.sp)
                } else {
                    val date = item.parsed.date
                    Text(
                        date?.format(DateTimeFormatter.ofPattern("dd")) ?: "--",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        date?.format(DateTimeFormatter.ofPattern("MMM")) ?: "--",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontSize = 10.sp
                    )
                }
            }

            // Description + category
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text      = item.note,
                    style     = MaterialTheme.typography.bodyMedium,
                    maxLines  = 1,
                    overflow  = TextOverflow.Ellipsis,
                    color     = MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (!item.isIncluded && !item.hasDateError)
                        TextDecoration.LineThrough else TextDecoration.None
                )
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    if (item.isDuplicate) {
                        Text(
                            "⚠ Duplicate?",
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberWarning, fontSize = 9.sp
                        )
                    }
                    val category = item.categoryId?.let { categoryMap[it] }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (category != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )
                            .clickable(enabled = !item.hasDateError) { onCategoryTap() }
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text      = category?.name ?: "Set category",
                            style     = MaterialTheme.typography.labelSmall,
                            color     = if (category != null) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontSize  = 10.sp
                        )
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Change category",
                            tint     = if (category != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }

            // Amount + checkbox
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = (if (item.parsed.isCredit) "+" else "−") +
                            CurrencyFormatter.format(item.parsed.amount.toPlainString(), currency),
                    style      = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color      = if (item.parsed.isCredit) IncomeGreen
                    else MaterialTheme.colorScheme.error
                )
                Checkbox(
                    checked         = item.isIncluded && !item.hasDateError,
                    onCheckedChange = { if (!item.hasDateError) onToggle() },
                    enabled         = !item.hasDateError,
                    modifier        = Modifier.size(18.dp),
                    colors          = CheckboxDefaults.colors(
                        checkedColor   = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Filter tab row — pill style matching Categories screen's EXPENSE/INCOME toggle
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FilterTabRow(
    current:        ReviewFilter,
    totalCount:     Int,
    includedCount:  Int,
    duplicateCount: Int,
    onSelect:       (ReviewFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(50.dp))
            .background(MaterialTheme.colorScheme.surface),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        FilterTab("All ($totalCount)",          current == ReviewFilter.ALL,        Modifier.weight(1f)) { onSelect(ReviewFilter.ALL) }
        FilterTab("Included ($includedCount)",  current == ReviewFilter.INCLUDED,   Modifier.weight(1f)) { onSelect(ReviewFilter.INCLUDED) }
        FilterTab("Dupes ($duplicateCount)",    current == ReviewFilter.DUPLICATES, Modifier.weight(1f)) { onSelect(ReviewFilter.DUPLICATES) }
    }
}

@Composable
private fun FilterTab(label: String, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(50.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 8.dp)
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bottom action bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BottomActionBar(
    reviewing:          ImportReviewState.Reviewing,
    onShowBalanceSheet: () -> Unit,
    onConfirm:          () -> Unit
) {
    val currency      by CurrencyManager.currency.collectAsState()
    val includedCount  = reviewing.includedItems.size

    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Balance strategy row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable { onShowBalanceSheet() }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.AccountBalance, null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "Balance: ${balanceStrategyLabel(reviewing.balanceStrategy)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Default.Edit, null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
        }

        Button(
            onClick  = onConfirm,
            enabled  = includedCount > 0,
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                if (includedCount > 0) "Import $includedCount transaction${if (includedCount > 1) "s" else ""}"
                else "No transactions selected",
                style = MaterialTheme.typography.titleSmall
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Password dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PasswordDialog(
    wasWrong:  Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var password   by remember { mutableStateOf("") }
    var isVisible  by remember { mutableStateOf(false) }

    // Reset password field when the "wrong password" flag changes so user
    // sees a fresh field with the error message instead of the previous text.
    LaunchedEffect(wasWrong) { if (wasWrong) password = "" }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Lock, null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
        },
        title = { Text("Password Protected") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "This file is password-protected. Enter the password set by your bank.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                // Error message for wrong password
                if (wasWrong) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(8.dp)
                    ) {
                        Icon(Icons.Default.Error, null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Incorrect password. Try again.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
                OutlinedTextField(
                    value           = password,
                    onValueChange   = { password = it },
                    label           = { Text("File Password") },
                    singleLine      = true,
                    isError         = wasWrong,
                    visualTransformation = if (isVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isVisible = !isVisible }) {
                            Icon(
                                if (isVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isVisible) "Hide" else "Show"
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction    = ImeAction.Done
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (password.isNotBlank()) onConfirm(password) },
                enabled = password.isNotBlank()) {
                Text("Unlock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Balance strategy sheet
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BalanceStrategySheet(
    reviewing:              ImportReviewState.Reviewing,
    onStrategyChange:       (BalanceStrategy) -> Unit,
    onClosingBalanceChange: (String) -> Unit,
    onDismiss:              () -> Unit
) {
    val currency by CurrencyManager.currency.collectAsState()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Account Balance After Import",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Current: ${CurrencyFormatter.format(reviewing.account.balance.toPlainString(), currency)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Spacer(Modifier.height(4.dp))

            StrategyOption(
                isSelected  = reviewing.balanceStrategy is BalanceStrategy.KeepExisting,
                title       = "Keep balance as-is",
                description = "Balance is already correct. Import records only.",
                recommended = reviewing.existingTxCount > 0,
                onClick     = { onStrategyChange(BalanceStrategy.KeepExisting) }
            )
            StrategyOption(
                isSelected  = reviewing.balanceStrategy is BalanceStrategy.SetToStatement,
                title       = "Set to statement closing balance",
                description = "TraceLedger balance will match your bank statement.",
                recommended = false,
                onClick     = { onStrategyChange(BalanceStrategy.SetToStatement(BigDecimal.ZERO)) }
            )
            AnimatedVisibility(reviewing.balanceStrategy is BalanceStrategy.SetToStatement) {
                OutlinedTextField(
                    value           = reviewing.closingBalanceInput,
                    onValueChange   = { text ->
                        onClosingBalanceChange(text)
                        text.toBigDecimalOrNull()?.let { onStrategyChange(BalanceStrategy.SetToStatement(it)) }
                    },
                    label           = { Text("Closing balance from statement") },
                    prefix          = { Text(currency.symbol) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier        = Modifier.fillMaxWidth().padding(start = 16.dp)
                )
            }
            StrategyOption(
                isSelected  = reviewing.balanceStrategy is BalanceStrategy.RecalculateFromAll,
                title       = "Recalculate from transactions",
                description = "⚠ Only use for newly created accounts with no prior transactions.",
                recommended = reviewing.existingTxCount == 0,
                onClick     = { onStrategyChange(BalanceStrategy.RecalculateFromAll) }
            )
        }
    }
}

@Composable
private fun StrategyOption(
    isSelected:  Boolean,
    title:       String,
    description: String,
    recommended: Boolean,
    onClick:     () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.surface
            )
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        RadioButton(isSelected, onClick,
            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary))
        Spacer(Modifier.width(6.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title,
                    style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f))
                if (recommended) {
                    Text("Recommended",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 5.dp, vertical = 1.dp))
                }
            }
            Text(description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SummaryCell(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
            color = valueColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun balanceStrategyLabel(strategy: BalanceStrategy): String = when (strategy) {
    is BalanceStrategy.KeepExisting       -> "Keep existing"
    is BalanceStrategy.SetToStatement     -> "Set to statement"
    is BalanceStrategy.RecalculateFromAll -> "Recalculate"
}