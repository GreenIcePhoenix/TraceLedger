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
import com.greenicephoenix.traceledger.feature.statistics.components.SavingsRateTrendChart

@Composable
fun SavingsRateTrendScreen(viewModel: StatisticsViewModel, onBack: () -> Unit) {
    val points by viewModel.savingsRateTrend.collectAsState()
    LazyColumn(
        modifier            = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding      = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { BackHeader(title = "Savings Rate Trend", onBack = onBack) }
        item { Text("Monthly savings rate — last 12 months", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)) }
        item {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
                SavingsRateTrendChart(points = points, modifier = Modifier.fillMaxWidth().padding(16.dp))
            }
        }
        // Latest rate callout
        val latest = points.lastOrNull()
        if (latest != null) {
            item {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), Arrangement.spacedBy(4.dp)) {
                        Text("This month", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                        Text(
                            "${String.format("%.1f", latest.rate * 100f)}% savings rate",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (latest.rate >= 0) com.greenicephoenix.traceledger.core.ui.theme.SuccessGreen else com.greenicephoenix.traceledger.core.ui.theme.NothingRed
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(48.dp)) }
    }
}