package com.greenicephoenix.traceledger.feature.statistics.components

import androidx.compose.runtime.Composable
import com.greenicephoenix.traceledger.domain.model.CategoryUiModel
import com.greenicephoenix.traceledger.feature.statistics.StatisticsViewModel

@Composable
fun ExpenseLegend(
    slices: List<StatisticsViewModel.CategorySlice>,
    categoryMap: Map<String, CategoryUiModel>
) {
    CategoryLegend(slices = slices, categoryMap = categoryMap)
}