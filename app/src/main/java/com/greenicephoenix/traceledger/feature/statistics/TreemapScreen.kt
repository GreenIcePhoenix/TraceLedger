package com.greenicephoenix.traceledger.feature.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.core.ui.components.MonthSelector
import com.greenicephoenix.traceledger.domain.model.CategoryUiModel
import com.greenicephoenix.traceledger.feature.statistics.components.BackHeader
import com.greenicephoenix.traceledger.feature.statistics.components.TreemapChart

@Composable
fun TreemapScreen(
    viewModel:   StatisticsViewModel,
    categoryMap: Map<String, CategoryUiModel>,
    onBack:      () -> Unit,
    onDrillDown: (categoryId: String) -> Unit
) {
    val nodes         by viewModel.treemapNodes.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()

    LazyColumn(
        modifier            = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding      = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { BackHeader(title = "Spending Map", onBack = onBack) }
        item {
            MonthSelector(selectedMonth, viewModel::previousMonth, viewModel::nextMonth)
        }
        item {
            Text(
                "Area = proportion of total spending",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
        item {
            if (nodes.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), Alignment.Center) {
                    Text("No expense data this month",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            } else {
                TreemapChart(
                    nodes       = nodes,
                    categoryMap = categoryMap,
                    modifier    = Modifier.fillMaxWidth(),
                    onNodeTap   = onDrillDown
                )
            }
        }
        item { Spacer(Modifier.height(48.dp)) }
    }
}