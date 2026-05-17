package com.greenicephoenix.traceledger.feature.statistics.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.domain.model.CategoryUiModel
import com.greenicephoenix.traceledger.feature.statistics.StatisticsViewModel.CategoryComparison
import ir.ehsannarmani.compose_charts.RowChart
import ir.ehsannarmani.compose_charts.models.AnimationMode
import ir.ehsannarmani.compose_charts.models.BarProperties
import ir.ehsannarmani.compose_charts.models.Bars
import ir.ehsannarmani.compose_charts.models.DividerProperties
import ir.ehsannarmani.compose_charts.models.DrawStyle
import ir.ehsannarmani.compose_charts.models.GridProperties
import ir.ehsannarmani.compose_charts.models.LabelProperties
import ir.ehsannarmani.compose_charts.models.LineProperties
import ir.ehsannarmani.compose_charts.models.PopupProperties
import ir.ehsannarmani.compose_charts.models.VerticalIndicatorProperties

@Composable
fun CategoryComparisonChart(
    comparisons: List<CategoryComparison>,
    categoryMap: Map<String, CategoryUiModel>,
    modifier:    Modifier = Modifier
) {
    if (comparisons.isEmpty()) return

    val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    val gridColor  = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val axisColor  = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    val fallback   = MaterialTheme.colorScheme.primary

    val bars = remember(comparisons) {
        comparisons.map { comp ->
            val color = categoryMap[comp.categoryId]?.color?.let { Color(it) } ?: fallback
            val name  = categoryMap[comp.categoryId]?.name ?: comp.categoryId.take(10)
            Bars(
                label  = name,
                values = listOf(
                    Bars.Data(
                        label = "This",
                        value = comp.thisMonth,
                        color = SolidColor(color)
                    ),
                    Bars.Data(
                        label = "Last",
                        value = comp.lastMonth,
                        color = SolidColor(color.copy(alpha = 0.35f))
                    )
                )
            )
        }
    }

    RowChart(
        modifier            = modifier.fillMaxWidth().height((comparisons.size * 60 + 20).dp),
        data                = bars,
        barProperties       = BarProperties(
            thickness    = 10.dp,
            spacing      = 3.dp,
            cornerRadius = Bars.Data.Radius.Rectangle(topRight = 4.dp, bottomRight = 4.dp),
            style        = DrawStyle.Fill
        ),
        // RowChart uses VerticalIndicatorProperties (indicators on the vertical/Y axis)
        indicatorProperties = VerticalIndicatorProperties(
            textStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor),
            padding   = 8.dp
        ),
        labelProperties     = LabelProperties(
            enabled   = true,
            textStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)
        ),
        gridProperties      = GridProperties(
            xAxisProperties = GridProperties.AxisProperties(color = SolidColor(gridColor), thickness = 0.5.dp),
            yAxisProperties = GridProperties.AxisProperties(color = SolidColor(gridColor), thickness = 0.5.dp)
        ),
        dividerProperties   = DividerProperties(
            xAxisProperties = LineProperties(enabled = false),
            yAxisProperties = LineProperties(color = SolidColor(axisColor), thickness = 1.dp)
        ),
        popupProperties     = PopupProperties(
            containerColor = MaterialTheme.colorScheme.inverseSurface,
            textStyle      = MaterialTheme.typography.labelSmall.copy(
                color      = MaterialTheme.colorScheme.inverseOnSurface
            )
        ),
        animationMode       = AnimationMode.Together(),
        animationSpec       = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessLow
        )
    )
}