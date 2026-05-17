package com.greenicephoenix.traceledger.feature.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.domain.model.CategoryUiModel
import com.greenicephoenix.traceledger.feature.statistics.components.BackHeader
import com.greenicephoenix.traceledger.feature.statistics.components.CategoryComparisonChart

@Composable
fun CategoryComparisonScreen(
    viewModel:   StatisticsViewModel,
    categoryMap: Map<String, CategoryUiModel>,
    onBack:      () -> Unit
) {
    val comparisons by viewModel.categoryComparison.collectAsState()
    LazyColumn(
        modifier            = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding      = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { BackHeader(title = "Month vs Last Month", onBack = onBack) }
        item { Text("Top 5 categories — solid bar = this month, faded = last month", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(0.5f)) }
        item {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
                CategoryComparisonChart(comparisons = comparisons, categoryMap = categoryMap, modifier = Modifier.fillMaxWidth().padding(16.dp))
            }
        }
        item { Spacer(Modifier.height(48.dp)) }
    }
}