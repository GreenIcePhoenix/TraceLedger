package com.greenicephoenix.traceledger.feature.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.feature.statistics.components.BackHeader
import com.greenicephoenix.traceledger.feature.statistics.components.ExpenseVelocityChart

@Composable
fun ExpenseVelocityScreen(viewModel: StatisticsViewModel, onBack: () -> Unit) {
    val points by viewModel.expenseVelocity.collectAsState()
    LazyColumn(
        modifier            = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding      = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { BackHeader(title = "Expense Velocity", onBack = onBack) }
        item { Text("Cumulative spend: this month vs last month vs average", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(0.5f)) }
        // Legend
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendItem("This month", com.greenicephoenix.traceledger.core.ui.theme.NothingRed, LineStyle.SOLID)
                LegendItem("Last month", com.greenicephoenix.traceledger.core.ui.theme.NothingRed.copy(alpha = 0.45f), LineStyle.DASHED)
                LegendItem("Average", MaterialTheme.colorScheme.onSurface.copy(0.5f), LineStyle.DOTTED)
            }
        }
        item {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
                ExpenseVelocityChart(points = points, modifier = Modifier.fillMaxWidth().padding(16.dp))
            }
        }
        item { Spacer(Modifier.height(48.dp)) }
    }
}

private enum class LineStyle { SOLID, DASHED, DOTTED }

@Composable
private fun LegendItem(label: String, color: androidx.compose.ui.graphics.Color, style: LineStyle) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(20.dp, 2.dp)) {
            drawLine(color, androidx.compose.ui.geometry.Offset(0f, size.height / 2f), androidx.compose.ui.geometry.Offset(size.width, size.height / 2f), 2f)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground.copy(0.6f))
    }
}