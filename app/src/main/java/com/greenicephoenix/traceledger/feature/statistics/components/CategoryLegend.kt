package com.greenicephoenix.traceledger.feature.statistics.components

import androidx.compose.foundation.background
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
 * Shared legend implementation used by both ExpenseLegend and IncomeLegend.
 * Internal visibility — only accessible within this package.
 */
@Composable
internal fun CategoryLegend(
    slices: List<StatisticsViewModel.CategorySlice>,
    categoryMap: Map<String, CategoryUiModel>
) {
    if (slices.isEmpty()) return

    val currency by CurrencyManager.currency.collectAsState()

    Column(
        modifier            = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        slices.forEach { slice ->
            val category = categoryMap[slice.categoryId]
            val color    = category?.color?.let { Color(it) } ?: MaterialTheme.colorScheme.surfaceVariant

            LegendRow(
                color      = color,
                name       = category?.name ?: "Unknown",
                percentage = slice.percentage,
                amount     = CurrencyFormatter.format(slice.amount.toPlainString(), currency)
            )
        }
    }
}

@Composable
private fun LegendRow(
    color: Color,
    name: String,
    percentage: Float,
    amount: String
) {
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(10.dp).background(color, CircleShape))

        Spacer(Modifier.width(12.dp))

        Text(
            text     = name,
            style    = MaterialTheme.typography.bodyMedium,
            color    = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        // Coloured percentage pill
        Box(
            modifier = Modifier
                .background(color.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text  = String.format("%.1f%%", percentage),
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }

        Spacer(Modifier.width(12.dp))

        Text(
            text  = amount,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}