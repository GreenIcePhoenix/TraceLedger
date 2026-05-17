package com.greenicephoenix.traceledger.feature.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.domain.model.CategoryUiModel
import com.greenicephoenix.traceledger.feature.statistics.components.BackHeader
import com.greenicephoenix.traceledger.feature.statistics.components.CategoryLegend
import com.greenicephoenix.traceledger.feature.statistics.components.DonutChart

@Composable
fun IncomeBreakdownScreen(
    viewModel:   StatisticsViewModel,
    categoryMap: Map<String, CategoryUiModel>,
    onBack:      () -> Unit,
    onDrillDown: (categoryId: String) -> Unit
) {
    val slices by viewModel.incomeCategorySlices.collectAsState()
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier            = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding      = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item { BackHeader(title = "Income Breakdown", onBack = onBack) }

        item {
            if (slices.isEmpty()) {
                Box(
                    modifier         = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = "No income data for this month",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            } else {
                DonutChart(
                    slices       = slices,
                    categoryMap  = categoryMap,
                    centerLabel  = "INCOME",
                    modifier     = Modifier.fillMaxWidth(),
                    onSegmentTap = { categoryId ->
                        if (selectedCategoryId == categoryId) onDrillDown(categoryId)
                        else selectedCategoryId = categoryId
                    }
                )
            }
        }

        item {
            CategoryLegend(
                slices             = slices,
                categoryMap        = categoryMap,
                selectedCategoryId = selectedCategoryId,
                onItemClick        = { categoryId ->
                    if (selectedCategoryId == categoryId) onDrillDown(categoryId)
                    else selectedCategoryId = categoryId
                }
            )
        }

        item { Spacer(Modifier.height(48.dp)) }
    }
}