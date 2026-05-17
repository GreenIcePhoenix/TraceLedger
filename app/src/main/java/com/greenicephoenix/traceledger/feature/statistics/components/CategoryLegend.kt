package com.greenicephoenix.traceledger.feature.statistics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.core.currency.CurrencyFormatter
import com.greenicephoenix.traceledger.core.currency.CurrencyManager
import com.greenicephoenix.traceledger.domain.model.CategoryUiModel
import com.greenicephoenix.traceledger.feature.statistics.StatisticsViewModel

/**
 * Shared legend for both expense and income breakdown screens.
 *
 * @param selectedCategoryId If set, highlights the matching row
 * @param onItemClick        If set, rows become tappable for drill-down
 */
@Composable
internal fun CategoryLegend(
    slices:             List<StatisticsViewModel.CategorySlice>,
    categoryMap:        Map<String, CategoryUiModel>,
    selectedCategoryId: String? = null,
    onItemClick:        ((categoryId: String) -> Unit)? = null
) {
    if (slices.isEmpty()) return

    val currency by CurrencyManager.currency.collectAsState()

    Column(
        modifier            = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        slices.forEach { slice ->
            val category   = categoryMap[slice.categoryId]
            val color      = category?.color?.let { Color(it) } ?: MaterialTheme.colorScheme.surfaceVariant
            val isSelected = slice.categoryId == selectedCategoryId

            val rowModifier = if (onItemClick != null)
                Modifier
                    .fillMaxWidth()
                    .background(
                        if (isSelected) color.copy(alpha = 0.08f) else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onItemClick(slice.categoryId) }
                    .padding(vertical = 6.dp, horizontal = 4.dp)
            else
                Modifier.fillMaxWidth()

            Row(
                modifier          = rowModifier,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(10.dp).background(color, CircleShape))
                Spacer(Modifier.width(12.dp))
                Text(
                    text     = category?.name ?: "Unknown",
                    style    = MaterialTheme.typography.bodyMedium,
                    color    = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .background(color.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text  = String.format("%.1f%%", slice.percentage),
                        style = MaterialTheme.typography.labelSmall,
                        color = color
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text  = CurrencyFormatter.format(slice.amount.toPlainString(), currency),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}