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
import com.greenicephoenix.traceledger.core.ui.theme.NothingRed
import com.greenicephoenix.traceledger.core.ui.theme.SuccessGreen
import com.greenicephoenix.traceledger.feature.statistics.components.AreaChart
import com.greenicephoenix.traceledger.feature.statistics.components.BackHeader

@Composable
fun AreaChartScreen(
    viewModel: StatisticsViewModel,
    onBack:    () -> Unit
) {
    val points   by viewModel.monthlyAreaPoints.collectAsState()
    val currency by CurrencyManager.currency.collectAsState()

    // Totals for the most recent month in the area data
    val latest   = points.lastOrNull()
    val totalIncome  by viewModel.totalIncome.collectAsState()
    val totalExpense by viewModel.totalExpense.collectAsState()

    LazyColumn(
        modifier            = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding      = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { BackHeader(title = "Income vs Expense", onBack = onBack) }

        item {
            Text(
                "12-month overview",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }

        // Legend
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                LegendDot(color = SuccessGreen, label = "Income")
                LegendDot(color = NothingRed,   label = "Expense")
            }
        }

        item {
            if (points.isEmpty()) {
                Box(
                    modifier         = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = "Not enough data yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            } else {
                Card(
                    shape    = RoundedCornerShape(20.dp),
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AreaChart(
                        points   = points,
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    )
                }
            }
        }

        // Current month summary
        item {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    label    = "This month income",
                    value    = CurrencyFormatter.format(totalIncome.toPlainString(), currency),
                    color    = SuccessGreen
                )
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    label    = "This month expense",
                    value    = CurrencyFormatter.format(totalExpense.toPlainString(), currency),
                    color    = NothingRed
                )
            }
        }

        item { Spacer(Modifier.height(48.dp)) }
    }
}

@Composable
private fun LegendDot(
    color: androidx.compose.ui.graphics.Color,
    label: String
) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, androidx.compose.foundation.shape.CircleShape)
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
    }
}

@Composable
private fun SummaryCard(
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
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Text(value, style = MaterialTheme.typography.titleSmall, color = color)
        }
    }
}