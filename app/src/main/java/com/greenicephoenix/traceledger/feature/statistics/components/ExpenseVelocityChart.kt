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
import com.greenicephoenix.traceledger.feature.statistics.StatisticsViewModel.VelocityPoint
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
import ir.ehsannarmani.compose_charts.models.StrokeStyle

@Composable
fun ExpenseVelocityChart(
    points:   List<VelocityPoint>,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    val labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    val gridColor  = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val axisColor  = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    val avgColor   = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.40f)

    val currentLine = Line(
        label                   = "This month",
        values                  = points.map { it.currentMonth },
        color                   = SolidColor(NothingRed),
        firstGradientFillColor  = NothingRed.copy(alpha = 0.15f),
        secondGradientFillColor = Color.Unspecified,
        drawStyle               = DrawStyle.Stroke(width = 2.dp),
        strokeAnimationSpec     = tween(600),
        dotProperties           = DotProperties(enabled = false),
        curvedEdges             = true
    )

    val previousLine = Line(
        label                   = "Last month",
        values                  = points.map { it.previousMonth },
        color                   = SolidColor(NothingRed.copy(alpha = 0.45f)),
        firstGradientFillColor  = Color.Unspecified,
        secondGradientFillColor = Color.Unspecified,
        // Dashed via StrokeStyle
        drawStyle               = DrawStyle.Stroke(
            width       = 1.5.dp,
            strokeStyle = StrokeStyle.Dashed(
                intervals = floatArrayOf(10f, 4f),
                phase     = 0f
            )
        ),
        strokeAnimationSpec     = tween(600),
        dotProperties           = DotProperties(enabled = false),
        curvedEdges             = true
    )

    val avgLine = Line(
        label                   = "Average",
        values                  = points.map { it.monthlyAverage },
        color                   = SolidColor(avgColor),
        firstGradientFillColor  = Color.Unspecified,
        secondGradientFillColor = Color.Unspecified,
        drawStyle               = DrawStyle.Stroke(
            width       = 1.5.dp,
            strokeStyle = StrokeStyle.Dashed(
                intervals = floatArrayOf(4f, 4f),
                phase     = 0f
            )
        ),
        strokeAnimationSpec     = tween(600),
        dotProperties           = DotProperties(enabled = false),
        curvedEdges             = true
    )

    LineChart(
        modifier            = modifier.fillMaxWidth().height(200.dp),
        data                = listOf(currentLine, previousLine, avgLine),
        indicatorProperties = HorizontalIndicatorProperties(
            textStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor),
            padding   = 8.dp
        ),
        labelProperties     = LabelProperties(
            enabled   = true,
            textStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor),
            labels    = points.mapIndexed { i, pt -> if (pt.day % 5 == 0) "${pt.day}" else "" }
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