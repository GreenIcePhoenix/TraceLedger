package com.greenicephoenix.traceledger.feature.statistics.components

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.feature.statistics.StatisticsViewModel.CategoryMonthlyTrend
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.models.AnimationMode
import ir.ehsannarmani.compose_charts.models.DividerProperties
import ir.ehsannarmani.compose_charts.models.DotProperties
import ir.ehsannarmani.compose_charts.models.DrawStyle
import ir.ehsannarmani.compose_charts.models.GridProperties
import ir.ehsannarmani.compose_charts.models.HorizontalIndicatorProperties
import ir.ehsannarmani.compose_charts.models.LabelProperties
import ir.ehsannarmani.compose_charts.models.Line
import ir.ehsannarmani.compose_charts.models.LineProperties
import ir.ehsannarmani.compose_charts.models.PopupProperties
import java.time.format.DateTimeFormatter

@Composable
fun CategoryTrendLineChart(
    allTrends:          List<CategoryMonthlyTrend>,
    selectedCategoryId: String,
    topCategoryIds:     List<String>,
    modifier:           Modifier = Modifier,
    showAreaFill:       Boolean  = true,
    lineColor:          Color    = Color.Unspecified,
    onScrub:            ((String, String) -> Unit)? = null
) {
    val trendsByCategory  = allTrends.groupBy { it.categoryId }
    val selectedEntries   = trendsByCategory[selectedCategoryId]?.sortedBy { it.month } ?: emptyList()
    val comparisonEntries = topCategoryIds
        .filter { it != selectedCategoryId }
        .mapNotNull { trendsByCategory[it]?.sortedBy { e -> e.month } }
    val allMonths = (selectedEntries + comparisonEntries.flatten())
        .map { it.month }.distinct().sorted()

    if (selectedEntries.isEmpty()) {
        Text(
            text  = "No trend data",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        return
    }

    val resolvedColor  = if (lineColor == Color.Unspecified) MaterialTheme.colorScheme.primary else lineColor
    val dimColor       = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
    val labelColor     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    val gridColor      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val axisColor      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    val monthFormatter = DateTimeFormatter.ofPattern("MMM")

    val selectedLine = Line(
        label                  = selectedEntries.firstOrNull()?.categoryId ?: "",
        values                 = allMonths.map { month ->
            selectedEntries.firstOrNull { it.month == month }?.total?.toDouble() ?: 0.0
        },
        color                  = SolidColor(resolvedColor),
        firstGradientFillColor = if (showAreaFill) resolvedColor.copy(alpha = 0.25f) else Color.Unspecified,
        secondGradientFillColor = Color.Unspecified,
        drawStyle              = DrawStyle.Stroke(width = 2.dp),
        strokeAnimationSpec    = tween(700),
        gradientAnimationSpec  = tween(700),
        dotProperties          = DotProperties(
            enabled     = true,
            radius      = 4.dp,
            color       = SolidColor(resolvedColor),
            strokeColor = SolidColor(resolvedColor),
            strokeWidth = 2.dp
        ),
        curvedEdges = true
    )

    val compLines = comparisonEntries.map { entries ->
        Line(
            label  = entries.firstOrNull()?.categoryId ?: "",
            values = allMonths.map { month ->
                entries.firstOrNull { it.month == month }?.total?.toDouble() ?: 0.0
            },
            color               = SolidColor(dimColor),
            drawStyle           = DrawStyle.Stroke(width = 1.dp),
            strokeAnimationSpec = tween(700),
            curvedEdges         = true
        )
    }

    key(selectedCategoryId) {
        LineChart(
            modifier            = modifier.fillMaxWidth().height(220.dp),
            data                = listOf(selectedLine) + compLines,
            indicatorProperties = HorizontalIndicatorProperties(
                textStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor),
                padding   = 8.dp
            ),
            labelProperties     = LabelProperties(
                enabled   = true,
                textStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor),
                labels    = allMonths.map { it.format(monthFormatter) }
            ),
            gridProperties      = GridProperties(
                xAxisProperties = GridProperties.AxisProperties(color = SolidColor(gridColor), thickness = 0.5.dp),
                yAxisProperties = GridProperties.AxisProperties(color = SolidColor(gridColor), thickness = 0.5.dp)
            ),
            dividerProperties   = DividerProperties(
                xAxisProperties = LineProperties(color = SolidColor(axisColor), thickness = 1.dp),
                yAxisProperties = LineProperties(enabled = false)
            ),
            popupProperties     = PopupProperties(
                containerColor = MaterialTheme.colorScheme.inverseSurface,
                textStyle      = MaterialTheme.typography.labelSmall.copy(
                    color      = MaterialTheme.colorScheme.inverseOnSurface
                )
            ),
            animationMode       = AnimationMode.Together()
        )
    }
}