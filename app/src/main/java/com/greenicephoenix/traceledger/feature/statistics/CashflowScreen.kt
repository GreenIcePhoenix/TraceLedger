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

    LazyColumn(
        modifier            = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding      = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            BackHeader(title = "Cashflow", onBack = onBack)
        }

        item {
            CashflowBarChart(
                entries       = cashflow,
                selectedDay   = selectedEntry?.day,
                onDaySelected = { selectedEntry = it },
                modifier      = Modifier.fillMaxWidth()
            )
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
            items(cashflow.size) { index ->
                val entry = cashflow[index]
                val date  = LocalDate.of(selectedMonth.year, selectedMonth.month, entry.day)

                CashflowRow(
                    date    = date.format(rowDateFormatter),
                    income  = CurrencyFormatter.format(entry.income.toPlainString(), currency),
                    expense = CurrencyFormatter.format(entry.expense.toPlainString(), currency),
                    net     = CurrencyFormatter.format(
                        entry.income.subtract(entry.expense).toPlainString(), currency
                    ),
                    netPositive = entry.income >= entry.expense
                )
            }
        }

        item { Spacer(Modifier.height(48.dp)) }
    }

    // Day detail sheet
    selectedEntry?.let { entry ->
        val date = LocalDate.of(selectedMonth.year, selectedMonth.month, entry.day)

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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SheetStatColumn(
                        modifier = Modifier.weight(1f),
                        label    = "INCOME",
                        value    = CurrencyFormatter.format(entry.income.toPlainString(), currency),
                        color    = SuccessGreen
                    )
                    SheetStatColumn(
                        modifier = Modifier.weight(1f),
                        label    = "EXPENSE",
                        value    = CurrencyFormatter.format(entry.expense.toPlainString(), currency),
                        color    = NothingRed
                    )
                    SheetStatColumn(
                        modifier = Modifier.weight(1f),
                        label    = "NET",
                        value    = CurrencyFormatter.format(
                            entry.income.subtract(entry.expense).toPlainString(), currency
                        ),
                        color    = if (entry.income >= entry.expense) SuccessGreen else NothingRed
                    )
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun CashflowRow(
    date: String,
    income: String,
    expense: String,
    net: String,
    netPositive: Boolean
) {
    Row(
        modifier          = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text  = date,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(text = "+ $income",  style = MaterialTheme.typography.bodySmall, color = SuccessGreen)
            Text(text = "− $expense", style = MaterialTheme.typography.bodySmall, color = NothingRed)
            Text(
                text  = net,
                style = MaterialTheme.typography.labelSmall,
                color = if (netPositive) SuccessGreen.copy(alpha = 0.7f) else NothingRed.copy(alpha = 0.7f)
            )
        }
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
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}