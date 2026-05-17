package com.greenicephoenix.traceledger.feature.statistics.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.feature.statistics.model.WeekdayPattern
import ir.ehsannarmani.compose_charts.ColumnChart
import ir.ehsannarmani.compose_charts.models.AnimationMode
import ir.ehsannarmani.compose_charts.models.BarProperties
import ir.ehsannarmani.compose_charts.models.Bars
import ir.ehsannarmani.compose_charts.models.DividerProperties
import ir.ehsannarmani.compose_charts.models.DrawStyle
import ir.ehsannarmani.compose_charts.models.GridProperties
import ir.ehsannarmani.compose_charts.models.HorizontalIndicatorProperties
import ir.ehsannarmani.compose_charts.models.LabelProperties
import ir.ehsannarmani.compose_charts.models.LineProperties
import ir.ehsannarmani.compose_charts.models.PopupProperties

@Composable
fun WeekdayBarChart(
    patterns: List<WeekdayPattern>,
    modifier: Modifier = Modifier
) {
    if (patterns.isEmpty()) return

    val primaryColor = MaterialTheme.colorScheme.primary
    val dimColor     = MaterialTheme.colorScheme.surfaceVariant
    val labelColor   = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    val gridColor    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val axisColor    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    val dayLabels    = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val maxTotal     = patterns.maxOfOrNull { it.totalAmount } ?: 0.0

    val fullPatterns = (1..7).map { dow ->
        patterns.firstOrNull { it.dayOfWeek == dow }
            ?: WeekdayPattern(dow, 0, 0.0, 0)
    }

    val bars = remember(patterns) {
        fullPatterns.mapIndexed { index, pattern ->
            val isMax = pattern.totalAmount == maxTotal && maxTotal > 0.0
            Bars(
                label  = dayLabels[index],
                values = listOf(
                    Bars.Data(
                        value = pattern.totalAmount,
                        color = SolidColor(if (isMax) primaryColor else dimColor)
                    )
                )
            )
        }
    }

    ColumnChart(
        modifier            = modifier.fillMaxWidth().height(200.dp),
        data                = bars,
        barProperties       = BarProperties(
            thickness    = 26.dp,
            cornerRadius = Bars.Data.Radius.Rectangle(topRight = 6.dp, topLeft = 6.dp),
            style        = DrawStyle.Fill
        ),
        indicatorProperties = HorizontalIndicatorProperties(
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
            xAxisProperties = LineProperties(color = SolidColor(axisColor), thickness = 1.dp),
            yAxisProperties = LineProperties(enabled = false)
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