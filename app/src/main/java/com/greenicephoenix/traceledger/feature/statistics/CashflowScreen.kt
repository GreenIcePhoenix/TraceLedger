package com.greenicephoenix.traceledger.feature.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.core.currency.CurrencyFormatter
import com.greenicephoenix.traceledger.core.currency.CurrencyManager
import com.greenicephoenix.traceledger.core.ui.theme.NothingRed
import com.greenicephoenix.traceledger.core.ui.theme.SuccessGreen
import com.greenicephoenix.traceledger.feature.statistics.components.BackHeader
import com.greenicephoenix.traceledger.feature.statistics.components.CashflowBarChart
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val rowDateFormatter = DateTimeFormatter.ofPattern("dd MMM")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashflowScreen(
    viewModel: StatisticsViewModel,
    onBack: () -> Unit
) {
    val currency      by CurrencyManager.currency.collectAsState()
    val cashflow      by viewModel.cashflowByDay.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()

    var selectedEntry by remember { mutableStateOf<StatisticsViewModel.CashflowEntry?>(null) }
    val sheetState    = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var scrubEntry by remember { mutableStateOf<StatisticsViewModel.CashflowEntry?>(null) }

    LazyColumn(
        modifier            = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding      = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            BackHeader(title = "CASHFLOW", onBack = onBack)
        }

        item {
            Column {
                // Scrub value display — shown above chart during drag
                val displayEntry = scrubEntry ?: selectedEntry
                if (displayEntry != null) {
                    val net = displayEntry.income.subtract(displayEntry.expense)
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Day ${displayEntry.day}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                        Text(
                            CurrencyFormatter.format(displayEntry.income.toPlainString(), currency),
                            style = MaterialTheme.typography.labelSmall,
                            color = SuccessGreen
                        )
                        Text(
                            CurrencyFormatter.format(displayEntry.expense.toPlainString(), currency),
                            style = MaterialTheme.typography.labelSmall,
                            color = NothingRed
                        )
                    }
                }

                CashflowBarChart(
                    entries       = cashflow,
                    selectedDay   = selectedEntry?.day,
                    onDaySelected = { selectedEntry = it },
                    modifier      = Modifier.fillMaxWidth(),
                    onScrub       = { entry -> scrubEntry = entry }
                )
            }
        }

        if (cashflow.isEmpty()) {
            item {
                Box(
                    modifier         = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = "No cashflow data for this month",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                }
            }
        } else {
            // FIX: Replaced cramped single-column layout with proper 3-column row.
            // Previously: date on left, income+expense+net stacked in tiny labelSmall
            //             on the right — unreadable at a glance.
            // Now: date | income | expense | net  as 4 evenly-spaced columns.
            item {
                // Column headers
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text     = "Date",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                        modifier = Modifier.weight(1.4f)
                    )
                    listOf("In", "Out", "Net").forEachIndexed { i, label ->
                        Text(
                            text     = label,
                            style    = MaterialTheme.typography.labelSmall,
                            color    = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )
                    }
                }

                HorizontalDivider(
                    modifier  = Modifier.padding(top = 6.dp),
                    thickness = 0.5.dp,
                    color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )
            }

            items(cashflow.size) { index ->
                val entry = cashflow[index]
                val date  = LocalDate.of(selectedMonth.year, selectedMonth.month, entry.day)
                val net   = entry.income.subtract(entry.expense)

                CashflowRow(
                    date    = date.format(rowDateFormatter),
                    income  = CurrencyFormatter.format(entry.income.toPlainString(), currency),
                    expense = CurrencyFormatter.format(entry.expense.toPlainString(), currency),
                    net     = CurrencyFormatter.format(net.toPlainString(), currency),
                    netPositive = net.signum() >= 0
                )
            }
        }

        item { Spacer(Modifier.height(48.dp)) }
    }

    // Day detail bottom sheet
    selectedEntry?.let { entry ->
        val date = LocalDate.of(selectedMonth.year, selectedMonth.month, entry.day)
        val net  = entry.income.subtract(entry.expense)

        ModalBottomSheet(
            onDismissRequest = { selectedEntry = null },
            sheetState       = sheetState
        ) {
            Column(
                modifier            = Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text  = date.format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SheetStatColumn(Modifier.weight(1f), "INCOME",  CurrencyFormatter.format(entry.income.toPlainString(), currency), SuccessGreen)
                    SheetStatColumn(Modifier.weight(1f), "EXPENSE", CurrencyFormatter.format(entry.expense.toPlainString(), currency), NothingRed)
                    SheetStatColumn(
                        modifier = Modifier.weight(1f),
                        label    = "NET",
                        value    = CurrencyFormatter.format(net.toPlainString(), currency),
                        color    = if (net.signum() >= 0) SuccessGreen else NothingRed
                    )
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CashflowRow — 4-column row: date | income | expense | net
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CashflowRow(
    date: String,
    income: String,
    expense: String,
    net: String,
    netPositive: Boolean
) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text     = date,
            style    = MaterialTheme.typography.bodyMedium,
            color    = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1.4f)
        )
        Text(
            text      = income,
            style     = MaterialTheme.typography.bodyMedium,
            color     = SuccessGreen,
            modifier  = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
        Text(
            text      = expense,
            style     = MaterialTheme.typography.bodyMedium,
            color     = NothingRed,
            modifier  = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
        Text(
            text      = net,
            style     = MaterialTheme.typography.bodyMedium,
            color     = if (netPositive) SuccessGreen.copy(alpha = 0.8f) else NothingRed.copy(alpha = 0.8f),
            modifier  = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

@Composable
private fun SheetStatColumn(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}