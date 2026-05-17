package com.greenicephoenix.traceledger.feature.statistics.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.feature.statistics.StatisticsViewModel
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
fun CashflowBarChart(
    entries:       List<StatisticsViewModel.CashflowEntry>,
    selectedDay:   Int?,
    onDaySelected: (StatisticsViewModel.CashflowEntry) -> Unit,
    modifier:      Modifier = Modifier,
    onScrub:       ((StatisticsViewModel.CashflowEntry?) -> Unit)? = null
) {
    if (entries.isEmpty()) {
        Text(
            text  = "No cashflow data",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        return
    }

    val incomeColor  = MaterialTheme.colorScheme.primary
    val expenseColor = MaterialTheme.colorScheme.error
    val labelColor   = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    val gridColor    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val axisColor    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)

    val bars = remember(entries) {
        entries.map { entry ->
            Bars(
                label  = "${entry.day}",
                values = listOf(
                    Bars.Data(
                        label = "In",
                        value = entry.income.toDouble(),
                        color = SolidColor(incomeColor)
                    ),
                    Bars.Data(
                        label  = "Out",
                        value  = entry.expense.toDouble(),
                        color  = SolidColor(expenseColor)
                    )
                )
            )
        }
    }

    ColumnChart(
        modifier          = modifier.fillMaxWidth().height(300.dp),
        data              = bars,
        barProperties     = BarProperties(
            thickness     = 5.dp,
            spacing       = 2.dp,
            cornerRadius  = Bars.Data.Radius.Rectangle(topRight = 3.dp, topLeft = 3.dp),
            style         = DrawStyle.Fill
        ),
        indicatorProperties = HorizontalIndicatorProperties(
            textStyle     = MaterialTheme.typography.labelSmall.copy(color = labelColor),
            padding       = 8.dp
        ),
        labelProperties   = LabelProperties(
            enabled       = true,
            textStyle     = MaterialTheme.typography.labelSmall.copy(color = labelColor),
            labels        = entries.map { if (it.day % 5 == 0) "${it.day}" else "" }
        ),
        gridProperties    = GridProperties(
            xAxisProperties = GridProperties.AxisProperties(
                color     = SolidColor(gridColor),
                thickness = 0.5.dp
            ),
            yAxisProperties = GridProperties.AxisProperties(
                color     = SolidColor(gridColor),
                thickness = 0.5.dp
            )
        ),
        dividerProperties = DividerProperties(
            xAxisProperties = LineProperties(
                color     = SolidColor(axisColor),
                thickness = 1.dp
            ),
            yAxisProperties = LineProperties(enabled = false)
        ),
        popupProperties   = PopupProperties(
            containerColor = MaterialTheme.colorScheme.inverseSurface,
            textStyle      = MaterialTheme.typography.labelSmall.copy(
                color      = MaterialTheme.colorScheme.inverseOnSurface
            ),
            contentBuilder = { popup -> popup.value.toBigDecimal().toPlainString() }
        ),
        animationMode     = AnimationMode.Together(),
        animationSpec     = spring(
            dampingRatio  = Spring.DampingRatioMediumBouncy,
            stiffness     = Spring.StiffnessLow
        )
    )
}