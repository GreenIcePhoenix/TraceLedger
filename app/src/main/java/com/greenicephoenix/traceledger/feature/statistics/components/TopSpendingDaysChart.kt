package com.greenicephoenix.traceledger.feature.statistics.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.feature.statistics.StatisticsViewModel.TopSpendDay
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
import java.time.format.DateTimeFormatter

@Composable
fun TopSpendingDaysChart(
    days:     List<TopSpendDay>,
    modifier: Modifier = Modifier
) {
    if (days.isEmpty()) return

    val primaryColor = MaterialTheme.colorScheme.primary
    val labelColor   = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    val gridColor    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val axisColor    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    val formatter    = DateTimeFormatter.ofPattern("d MMM yy")

    val bars = remember(days) {
        days.map { day ->
            val alpha = (1f - (day.rank - 1) * 0.06f).coerceAtLeast(0.4f)
            Bars(
                label  = day.date.format(formatter),
                values = listOf(
                    Bars.Data(
                        value = day.total,
                        color = SolidColor(primaryColor.copy(alpha = alpha))
                    )
                )
            )
        }
    }

    RowChart(
        modifier            = modifier.fillMaxWidth().height((days.size * 42 + 20).dp),
        data                = bars,
        barProperties       = BarProperties(
            thickness    = 18.dp,
            cornerRadius = Bars.Data.Radius.Rectangle(topRight = 6.dp, bottomRight = 6.dp),
            style        = DrawStyle.Fill
        ),
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