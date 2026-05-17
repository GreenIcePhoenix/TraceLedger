package com.greenicephoenix.traceledger.feature.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.core.currency.CurrencyFormatter
import com.greenicephoenix.traceledger.core.currency.CurrencyManager
import com.greenicephoenix.traceledger.core.ui.components.MonthSelector
import com.greenicephoenix.traceledger.core.ui.theme.NothingRed
import com.greenicephoenix.traceledger.core.ui.theme.SuccessGreen
import com.greenicephoenix.traceledger.feature.statistics.components.BackHeader
import com.greenicephoenix.traceledger.feature.statistics.components.WaterfallChart

@Composable
fun WaterfallScreen(
    viewModel: StatisticsViewModel,
    onBack:    () -> Unit
) {
    val bars          by viewModel.waterfallBars.collectAsState()
    val currency      by CurrencyManager.currency.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val totalIncome   by viewModel.totalIncome.collectAsState()
    val totalExpense  by viewModel.totalExpense.collectAsState()
    val netAmount     by viewModel.netAmount.collectAsState()

    LazyColumn(
        modifier            = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding      = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { BackHeader(title = "Cashflow Waterfall", onBack = onBack) }

        item {
            MonthSelector(
                month      = selectedMonth,
                onPrevious = viewModel::previousMonth,
                onNext     = viewModel::nextMonth
            )
        }

        item {
            Card(
                shape    = RoundedCornerShape(20.dp),
                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Monthly cashflow breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "How income and expenses combine this month",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(16.dp))
                    WaterfallChart(
                        bars     = bars,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Summary row
        item {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WaterfallStat(Modifier.weight(1f), "Income",  CurrencyFormatter.format(totalIncome.toPlainString(), currency),  SuccessGreen)
                WaterfallStat(Modifier.weight(1f), "Expense", CurrencyFormatter.format(totalExpense.toPlainString(), currency), NothingRed)
                WaterfallStat(
                    modifier = Modifier.weight(1f),
                    label    = "Net",
                    value    = CurrencyFormatter.format(netAmount.toPlainString(), currency),
                    color    = if (netAmount.signum() >= 0) SuccessGreen else NothingRed
                )
            }
        }

        item { Spacer(Modifier.height(48.dp)) }
    }
}

@Composable
private fun WaterfallStat(
    modifier: Modifier,
    label:    String,
    value:    String,
    color:    androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = modifier,
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Text(value, style = MaterialTheme.typography.titleSmall, color = color)
        }
    }
}