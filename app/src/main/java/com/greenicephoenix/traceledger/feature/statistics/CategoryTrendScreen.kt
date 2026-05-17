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
import com.greenicephoenix.traceledger.core.ui.theme.NothingRed
import com.greenicephoenix.traceledger.core.ui.theme.SuccessGreen
import com.greenicephoenix.traceledger.domain.model.CategoryUiModel
import com.greenicephoenix.traceledger.feature.statistics.components.BackHeader
import com.greenicephoenix.traceledger.feature.statistics.components.CategoryTrendLineChart

private enum class TrendMode { EXPENSE, INCOME }

@Composable
fun CategoryTrendScreen(
    viewModel:   StatisticsViewModel,
    categoryMap: Map<String, CategoryUiModel>,
    onBack:      () -> Unit
) {
    val expenseTrends by viewModel.categoryExpenseTrends.collectAsState()
    val incomeTrends  by viewModel.categoryIncomeTrends.collectAsState()

    var mode by remember { mutableStateOf(TrendMode.EXPENSE) }

    val allTrends = if (mode == TrendMode.EXPENSE) expenseTrends else incomeTrends
    val lineColor  = if (mode == TrendMode.EXPENSE) NothingRed else SuccessGreen

    val topCategoryIds = remember(allTrends) {
        allTrends.groupBy { it.categoryId }.entries
            .sortedByDescending { (_, entries) -> entries.sumOf { it.total.toDouble() } }
            .take(5).map { it.key }
    }

    var selectedCategoryId by remember(topCategoryIds) {
        mutableStateOf(topCategoryIds.firstOrNull() ?: "")
    }

    // Scrub state — shown inline above the chart
    var scrubLabel by remember { mutableStateOf("") }
    var scrubValue by remember { mutableStateOf("") }

    LazyColumn(
        modifier            = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding      = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { BackHeader(title = "Spending Trends", onBack = onBack) }

        // Expense / Income toggle
        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                TrendMode.entries.forEachIndexed { index, trendMode ->
                    SegmentedButton(
                        selected = mode == trendMode,
                        onClick  = { mode = trendMode; selectedCategoryId = "" },
                        shape    = SegmentedButtonDefaults.itemShape(index, TrendMode.entries.size),
                        label    = { Text(trendMode.name) }
                    )
                }
            }
        }

        if (allTrends.isEmpty() || topCategoryIds.isEmpty()) {
            item {
                Box(
                    modifier         = Modifier.fillMaxWidth().padding(vertical = 64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text      = "Not enough data for trends yet.\nAdd transactions across multiple months.",
                        style     = MaterialTheme.typography.bodyMedium,
                        color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            return@LazyColumn
        }

        // Category selector chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            { Box(Modifier.size(8.dp).background(Color(category.color), RoundedCornerShape(4.dp))) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor     = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }

        // Chart card
        item {
            Card(
                shape    = RoundedCornerShape(20.dp),
                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val selectedCategory = categoryMap[selectedCategoryId]
                    if (selectedCategory != null) {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text  = selectedCategory.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text  = "Monthly ${mode.name.lowercase()} — last 12 months",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                            // Scrub tooltip inline in header
                            if (scrubLabel.isNotBlank()) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text  = scrubValue,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = lineColor
                                    )
                                    Text(
                                        text  = scrubLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    if (selectedCategoryId.isNotEmpty()) {
                        CategoryTrendLineChart(
                            allTrends          = allTrends,
                            selectedCategoryId = selectedCategoryId,
                            topCategoryIds     = topCategoryIds,
                            modifier           = Modifier.fillMaxWidth(),
                            showAreaFill       = true,
                            lineColor          = lineColor,
                            onScrub            = { label, value ->
                                scrubLabel = label
                                scrubValue = value
                            }
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(48.dp)) }
    }
}