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
import com.greenicephoenix.traceledger.feature.statistics.components.TopSpendingDaysChart

@Composable
fun TopSpendingDaysScreen(viewModel: StatisticsViewModel, onBack: () -> Unit) {
    val days by viewModel.topSpendingDays.collectAsState()
    LazyColumn(
        modifier            = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding      = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { BackHeader(title = "Top Spending Days", onBack = onBack) }
        item { Text("Your 10 highest-spend days ever", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(0.5f)) }
        item {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
                TopSpendingDaysChart(days = days, modifier = Modifier.fillMaxWidth().padding(16.dp))
            }
        }
        item { Spacer(Modifier.height(48.dp)) }
    }
}