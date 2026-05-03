package com.greenicephoenix.traceledger.feature.recurring

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.greenicephoenix.traceledger.core.currency.CurrencyFormatter
import com.greenicephoenix.traceledger.core.currency.CurrencyManager
import com.greenicephoenix.traceledger.core.database.entity.RecurringTransactionEntity
import com.greenicephoenix.traceledger.core.recurring.RecurringDateCalculator
import com.greenicephoenix.traceledger.core.ui.theme.NothingRed
import com.greenicephoenix.traceledger.core.ui.theme.SuccessGreen
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringTransactionsScreen(
    viewModelFactory: RecurringTransactionsViewModelFactory,
    onAddClick: () -> Unit,
    onEditClick: (RecurringTransactionEntity) -> Unit,
    onBack: () -> Unit
) {
    val viewModel: RecurringTransactionsViewModel = viewModel(factory = viewModelFactory)
    val recurringList by viewModel.recurringTransactions.collectAsState()
    val currency by CurrencyManager.currency.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Recurring")
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {

            // ── HEADER ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text  = "RECURRING",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // ── LIST ──────────────────────────────────────────────────────────
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
            ) {

                if (recurringList.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 120.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text      = "No recurring transactions yet.",
                                style     = MaterialTheme.typography.bodyMedium,
                                color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                            TextButton(onClick = onAddClick) {
                                Text("Create one", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                items(
                    items = recurringList,
                    key   = { it.id }
                ) { recurring ->
                    RecurringItemCard(
                        recurring = recurring,
                        currency  = currency,
                        onClick   = { onEditClick(recurring) },
                        onDelete  = { viewModel.delete(it) },
                        onToggle  = { viewModel.toggleActive(it) }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// RecurringItemCard
//
// Displays one recurring rule with:
//   - Type + frequency label
//   - Currency-formatted amount
//   - Human-readable "Next run" date
//   - Active/Paused status
//   - Pause/Resume and Delete actions
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun RecurringItemCard(
    recurring: RecurringTransactionEntity,
    currency: com.greenicephoenix.traceledger.core.currency.Currency,
    onClick: () -> Unit,
    onDelete: (RecurringTransactionEntity) -> Unit,
    onToggle: (RecurringTransactionEntity) -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // FIX #15: nextRun was displayed as raw LocalDate.toString() ("2026-04-01").
    // Now formatted as "1 Apr 2026" which is human readable.
    val nextRun = RecurringDateCalculator.nextExecutionDate(
        startDate         = recurring.startDate,
        lastGeneratedDate = recurring.lastGeneratedDate,
        frequency         = recurring.frequency
    )
    val nextRunLabel = formatNextRun(nextRun)

    // Frequency label — convert enum string to readable form
    val frequencyLabel = when (recurring.frequency) {
        "DAILY"       -> "Daily"
        "WEEKLY"      -> "Weekly"
        "MONTHLY"     -> "Monthly"
        "QUARTERLY"   -> "Quarterly"
        "HALF_YEARLY" -> "Every 6 months"
        "YEARLY"      -> "Yearly"
        else          -> recurring.frequency
    }

    // Type label
    val typeLabel = when (recurring.type) {
        "EXPENSE"  -> "Expense"
        "INCOME"   -> "Income"
        "TRANSFER" -> "Transfer"
        else       -> recurring.type
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete recurring rule?", style = MaterialTheme.typography.titleMedium) },
            text  = {
                Text(
                    "Future transactions will no longer be generated. Already-created transactions are not affected.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete(recurring)
                }) {
                    Text("Delete", color = NothingRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape  = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {

            // ── TOP ROW: type + frequency + status dot ────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text  = "$typeLabel • $frequencyLabel",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                // Active/Paused pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (recurring.isActive)
                        SuccessGreen.copy(alpha = 0.12f)
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                ) {
                    Text(
                        text  = if (recurring.isActive) "Active" else "Paused",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (recurring.isActive)
                            SuccessGreen
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // ── AMOUNT ────────────────────────────────────────────────────────
            // FIX: was recurring.amount.toPlainString() — no currency symbol.
            // Now uses CurrencyFormatter to match the rest of the app.
            Text(
                text  = CurrencyFormatter.format(recurring.amount.toPlainString(), currency),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            // ── NOTE (optional) ───────────────────────────────────────────────
            recurring.note?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text  = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            // ── NEXT RUN ──────────────────────────────────────────────────────
            Text(
                text  = "Next: $nextRunLabel",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            Spacer(Modifier.height(4.dp))
            HorizontalDivider(
                thickness = 0.5.dp,
                color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )
            Spacer(Modifier.height(4.dp))

            // ── ACTIONS ROW ───────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pause / Resume
                TextButton(onClick = { onToggle(recurring) }) {
                    Icon(
                        imageVector = if (recurring.isActive) Icons.Default.Pause
                        else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text  = if (recurring.isActive) "Pause" else "Resume",
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Delete
                TextButton(onClick = { showDeleteConfirm = true }) {
                    Text(
                        text  = "Delete",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// formatNextRun — converts LocalDate to a human-readable string
// "Today", "Tomorrow", or "1 Apr 2026"
// ─────────────────────────────────────────────────────────────────────────────
private fun formatNextRun(date: LocalDate): String {
    val today    = LocalDate.now()
    val tomorrow = today.plusDays(1)
    return when (date) {
        today    -> "Today"
        tomorrow -> "Tomorrow"
        else     -> date.format(
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
        )
    }
}