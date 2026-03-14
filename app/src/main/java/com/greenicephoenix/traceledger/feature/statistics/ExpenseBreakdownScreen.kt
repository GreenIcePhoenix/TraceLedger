package com.greenicephoenix.traceledger.feature.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.domain.model.CategoryUiModel
import com.greenicephoenix.traceledger.feature.statistics.components.BackHeader
import com.greenicephoenix.traceledger.feature.statistics.components.ExpenseDonutChart
import com.greenicephoenix.traceledger.feature.statistics.components.ExpenseLegend

@Composable
fun ExpenseBreakdownScreen(
    viewModel: StatisticsViewModel,
    categoryMap: Map<String, CategoryUiModel>,
    onBack: () -> Unit
) {
    val slices by viewModel.expenseCategorySlices.collectAsState()

    LazyColumn(
        modifier            = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding      = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            BackHeader(title = "Expense Breakdown", onBack = onBack)
        }

        item {
            if (slices.isEmpty()) {
                Box(
                    modifier            = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    contentAlignment    = androidx.compose.ui.Alignment.Center
                ) {
                    Text(
                        text  = "No expense data for this month",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            } else {
                ExpenseDonutChart(
                    slices      = slices,
                    categoryMap = categoryMap,
                    modifier    = Modifier.fillMaxWidth()
                )
            }
        }

        item {
            ExpenseLegend(slices = slices, categoryMap = categoryMap)
        }

        item { Spacer(Modifier.height(48.dp)) }
    }
}