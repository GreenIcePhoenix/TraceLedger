package com.greenicephoenix.traceledger.feature.statistics.components

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.core.ui.theme.NothingRed
import com.greenicephoenix.traceledger.core.ui.theme.SuccessGreen
import com.greenicephoenix.traceledger.feature.statistics.StatisticsViewModel.SavingsRatePoint
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.models.AnimationMode
import ir.ehsannarmani.compose_charts.models.DividerProperties
import ir.ehsannarmani.compose_charts.models.DotProperties
import ir.ehsannarmani.compose_charts.models.DrawStyle
import ir.ehsannarmani.compose_charts.models.GridProperties
import ir.ehsannarmani.compose_charts.models.HorizontalIndicatorProperties
import ir.ehsannarmani.compose_charts.models.IndicatorCount
import ir.ehsannarmani.compose_charts.models.LabelProperties
import ir.ehsannarmani.compose_charts.models.Line
import ir.ehsannarmani.compose_charts.models.LineProperties
import ir.ehsannarmani.compose_charts.models.PopupProperties

@Composable
fun SavingsRateTrendChart(
    points:   List<SavingsRatePoint>,
    modifier: Modifier = Modifier
) {
    if (points.size < 2) return

    val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    val gridColor  = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val axisColor  = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)

    // Positive savings: clamp negatives to 0
    val posLine = Line(
        label                   = "Savings",
        values                  = points.map { it.rate.coerceAtLeast(0f).toDouble() * 100.0 },
        color                   = SolidColor(SuccessGreen),
        firstGradientFillColor  = SuccessGreen.copy(alpha = 0.28f),
        secondGradientFillColor = Color.Unspecified,
        drawStyle               = DrawStyle.Stroke(width = 2.dp),
        strokeAnimationSpec     = tween(700),
        gradientAnimationSpec   = tween(700),
        dotProperties           = DotProperties(enabled = false),
        curvedEdges             = true
    )

    // Overspend: clamp positives to 0, negate for display
    val negLine = Line(
        label                   = "Overspend",
        values                  = points.map { (-it.rate.coerceAtMost(0f)).toDouble() * 100.0 },
        color                   = SolidColor(NothingRed),
        firstGradientFillColor  = NothingRed.copy(alpha = 0.22f),
        secondGradientFillColor = Color.Unspecified,
        drawStyle               = DrawStyle.Stroke(width = 2.dp),
        strokeAnimationSpec     = tween(700),
        gradientAnimationSpec   = tween(700),
        dotProperties           = DotProperties(enabled = false),
        curvedEdges             = true
    )

    LineChart(
        modifier            = modifier.fillMaxWidth().height(160.dp),
        data                = listOf(posLine, negLine),
        indicatorProperties = HorizontalIndicatorProperties(
            textStyle      = MaterialTheme.typography.labelSmall.copy(color = labelColor),
            padding        = 8.dp,
            count          = IndicatorCount.CountBased(count = 4),
            contentBuilder = { value -> "${value.toInt()}%" }
        ),
        labelProperties     = LabelProperties(
            enabled   = true,
            textStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor),
            labels    = points.mapIndexed { i, pt -> if (i % 2 == 0) pt.monthLabel else "" }
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
            ),
            contentBuilder = { popup -> "${popup.value.toInt()}%" }
        ),
        animationMode       = AnimationMode.Together()
    )
}