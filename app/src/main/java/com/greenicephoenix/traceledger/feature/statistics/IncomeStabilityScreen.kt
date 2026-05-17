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
import com.greenicephoenix.traceledger.feature.statistics.components.IncomeStabilityChart

@Composable
fun IncomeStabilityScreen(viewModel: StatisticsViewModel, onBack: () -> Unit) {
    val stability by viewModel.incomeStability.collectAsState()
    LazyColumn(
        modifier            = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding      = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { BackHeader(title = "Income Stability", onBack = onBack) }
        item {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
                IncomeStabilityChart(data = stability, modifier = Modifier.fillMaxWidth().padding(16.dp))
            }
        }
        item { Spacer(Modifier.height(48.dp)) }
    }
}