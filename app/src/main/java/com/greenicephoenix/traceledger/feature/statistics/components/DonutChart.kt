package com.greenicephoenix.traceledger.feature.statistics.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
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
import ir.ehsannarmani.compose_charts.PieChart
import ir.ehsannarmani.compose_charts.models.Pie
import java.math.BigDecimal

@Composable
fun DonutChart(
    slices:       List<StatisticsViewModel.CategorySlice>,
    categoryMap:  Map<String, CategoryUiModel>,
    centerLabel:  String,
    modifier:     Modifier = Modifier,
    onSegmentTap: ((categoryId: String) -> Unit)? = null
) {
    if (slices.isEmpty()) {
        Box(modifier.size(220.dp), Alignment.Center) {
            Text(
                text  = "No data",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
        return
    }

    val currency by CurrencyManager.currency.collectAsState()
    val fallback  = MaterialTheme.colorScheme.surfaceVariant

    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    val totalAmount = slices.fold(BigDecimal.ZERO) { acc, s -> acc + s.amount }

    // Pie.Style.Stroke renders as donut ring; width controls ring thickness
    val pieData = remember(slices, selectedCategoryId) {
        slices.map { slice ->
            val color = categoryMap[slice.categoryId]?.color?.let { Color(it) } ?: fallback
            Pie(
                label         = slice.categoryId, // use id as key, not name (names can duplicate)
                data          = slice.amount.toDouble(),
                color         = color,
                selectedColor = color,
                selected      = slice.categoryId == selectedCategoryId,
                style         = Pie.Style.Stroke(width = 42.dp)
            )
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        PieChart(
            modifier              = Modifier.size(220.dp),
            data                  = pieData,
            selectedScale         = 1.05f,
            selectedPaddingDegree = 2f,
            spaceDegree           = 2f,
            scaleAnimEnterSpec    = tween(200),
            scaleAnimExitSpec     = tween(200),
            colorAnimEnterSpec    = tween(200),
            colorAnimExitSpec     = tween(200),
            onPieClick            = { pie ->
                val tappedId = pie.label // label holds categoryId
                if (tappedId != null) {
                    if (selectedCategoryId == tappedId) {
                        onSegmentTap?.invoke(tappedId)
                        selectedCategoryId = null
                    } else {
                        selectedCategoryId = tappedId
                    }
                }
            }
        )

        // Animated center label
        val selectedSlice = slices.firstOrNull { it.categoryId == selectedCategoryId }

        AnimatedContent(
            targetState    = selectedCategoryId,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
            label          = "donut-center"
        ) { currentSelectedId ->
            val currentSlice  = slices.firstOrNull { it.categoryId == currentSelectedId }
            val currentLabel  = currentSlice?.let { categoryMap[it.categoryId]?.name } ?: centerLabel
            val currentAmount = currentSlice?.amount ?: totalAmount

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text  = currentLabel.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    text  = CurrencyFormatter.format(currentAmount.toPlainString(), currency),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}