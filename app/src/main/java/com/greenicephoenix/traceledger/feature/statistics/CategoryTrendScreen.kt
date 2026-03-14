package com.greenicephoenix.traceledger.feature.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.domain.model.CategoryUiModel
import com.greenicephoenix.traceledger.feature.statistics.components.BackHeader
import com.greenicephoenix.traceledger.feature.statistics.components.CategoryTrendLineChart

@Composable
fun CategoryTrendScreen(
    viewModel: StatisticsViewModel,
    categoryMap: Map<String, CategoryUiModel>,
    onBack: () -> Unit
) {
    val allTrends   by viewModel.categoryExpenseTrends.collectAsState()

    // The top categories with the most months of data
    val topCategoryIds = remember(allTrends) {
        allTrends
            .groupBy { it.categoryId }
            .entries
            .sortedByDescending { (_, entries) ->
                entries.sumOf { it.total.toDouble() }
            }
            .take(5)
            .map { it.key }
    }

    // Default to the category with most total spend
    var selectedCategoryId by remember(topCategoryIds) {
        mutableStateOf(topCategoryIds.firstOrNull() ?: "")
    }

    LazyColumn(
        modifier            = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding      = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            BackHeader(title = "Spending Trends", onBack = onBack)
        }

        if (allTrends.isEmpty() || topCategoryIds.isEmpty()) {
            item {
                Box(
                    modifier         = Modifier.fillMaxWidth().padding(vertical = 64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = "Not enough data for trends yet.\nAdd transactions across multiple months.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            return@LazyColumn
        }

        // Category selector chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(topCategoryIds) { categoryId ->
                    val category   = categoryMap[categoryId]
                    val isSelected = categoryId == selectedCategoryId

                    FilterChip(
                        selected = isSelected,
                        onClick  = { selectedCategoryId = categoryId },
                        label    = {
                            Text(
                                text  = category?.name ?: categoryId.take(8),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        leadingIcon = if (isSelected && category != null) {
                            {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(category.color), RoundedCornerShape(4.dp))
                                )
                            }
                        } else null
                    )
                }
            }
        }

        // Chart
        item {
            Card(
                shape  = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val selectedCategory = categoryMap[selectedCategoryId]
                    if (selectedCategory != null) {
                        Text(
                            text  = selectedCategory.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text  = "Monthly spend — last 6 months",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.height(16.dp))
                    }

                    if (selectedCategoryId.isNotEmpty()) {
                        CategoryTrendLineChart(
                            allTrends          = allTrends,
                            selectedCategoryId = selectedCategoryId,
                            topCategoryIds     = topCategoryIds,
                            modifier           = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(48.dp)) }
    }
}