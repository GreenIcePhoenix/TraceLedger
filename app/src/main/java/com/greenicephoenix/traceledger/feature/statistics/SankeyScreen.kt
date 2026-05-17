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
import com.greenicephoenix.traceledger.core.ui.components.MonthSelector
import com.greenicephoenix.traceledger.domain.model.CategoryUiModel
import com.greenicephoenix.traceledger.feature.statistics.components.BackHeader
import com.greenicephoenix.traceledger.feature.statistics.components.SankeyChart

@Composable
fun SankeyScreen(
    viewModel:   StatisticsViewModel,
    categoryMap: Map<String, CategoryUiModel>,
    onBack:      () -> Unit
) {
    val sankeyData by viewModel.sankeyData.collectAsState()
    val nodes = sankeyData.first
    val links = sankeyData.second
    val selectedMonth  by viewModel.selectedMonth.collectAsState()

    LazyColumn(
        modifier            = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding      = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { BackHeader(title = "Money Flow", onBack = onBack) }
        item { MonthSelector(selectedMonth, viewModel::previousMonth, viewModel::nextMonth) }
        item {
            Text(
                "Income sources → Expense categories",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
        item {
            if (nodes.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), Alignment.Center) {
                    Text("Need both income and expense data",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            } else {
                Card(
                    shape  = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SankeyChart(
                        nodes       = nodes,
                        links       = links,
                        categoryMap = categoryMap,
                        modifier    = Modifier.fillMaxWidth().padding(16.dp)
                    )
                }
            }
        }
        item { Spacer(Modifier.height(48.dp)) }
    }
}