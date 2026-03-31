package com.greenicephoenix.traceledger.feature.statistics.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenicephoenix.traceledger.feature.statistics.StatisticsViewModel
import com.greenicephoenix.traceledger.core.util.formatCompactMagnitude
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun CashflowBarChart(
    entries: List<StatisticsViewModel.CashflowEntry>,
    selectedDay: Int?,
    onDaySelected: (StatisticsViewModel.CashflowEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    if (entries.isEmpty()) {
        Text(
            text  = "No cashflow data",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        return
    }

    val maxValue = entries.fold(BigDecimal.ZERO) { acc, e -> maxOf(acc, e.income, e.expense) }

    val animatedProgress by animateFloatAsState(
        targetValue   = 1f,
        animationSpec = tween(durationMillis = 500),
        label         = "cashflow-animation"
    )

    val axisColor  = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    val gridColor  = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)

    val highlightAlpha by animateFloatAsState(
        targetValue   = if (selectedDay != null) 0.10f else 0f,
        animationSpec = tween(durationMillis = 180),
        label         = "cashflow-highlight"
    )

    val incomeColor  = MaterialTheme.colorScheme.primary
    val expenseColor = MaterialTheme.colorScheme.error

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .pointerInput(entries) {
                detectTapGestures { offset ->
                    val leftPaddingPx = 44.dp.toPx()
                    val usableWidth   = size.width - leftPaddingPx
                    if (offset.x < leftPaddingPx || entries.isEmpty()) return@detectTapGestures
                    val dayGroupWidth = usableWidth / entries.size
                    val index         = ((offset.x - leftPaddingPx) / dayGroupWidth).toInt()
                    if (index in entries.indices) onDaySelected(entries[index])
                }
            }
    ) {
        val leftPadding   = 44.dp.toPx()
        val chartHeight   = size.height * 0.75f
        val chartBottom   = chartHeight
        val labelY        = chartBottom + 22.dp.toPx()
        val dayGroupWidth = (size.width - leftPadding) / entries.size
        val barWidth      = dayGroupWidth * 0.25f
        val intraBarGap   = barWidth * 0.3f

        // Axes
        drawLine(axisColor, Offset(leftPadding, 0f), Offset(leftPadding, chartBottom), 1.dp.toPx())
        drawLine(axisColor, Offset(leftPadding, chartBottom), Offset(size.width, chartBottom), 1.dp.toPx())

        // Grid lines + Y labels
        val gridSteps = 4
        repeat(gridSteps) { step ->
            val fraction = (step + 1) / gridSteps.toFloat()
            val y        = chartBottom - (chartBottom * fraction)
            // FIX: Use setScale before dividing to avoid high-precision BigDecimal strings
            val value    = maxValue.multiply(BigDecimal(fraction.toDouble()))
                .setScale(2, RoundingMode.HALF_UP)

            drawLine(gridColor, Offset(leftPadding, y), Offset(size.width, y), 1.dp.toPx())

            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    color     = axisColor.toArgb()
                    textSize  = 11.sp.toPx()
                    textAlign = android.graphics.Paint.Align.RIGHT
                    isAntiAlias = true
                }
                canvas.nativeCanvas.drawText(
                    formatCompactMagnitude(value),
                    leftPadding - 6.dp.toPx(),
                    y + 4.dp.toPx(),
                    paint
                )
            }
        }

        // Bars + X labels
        entries.forEachIndexed { index, entry ->
            val isSelected = selectedDay == entry.day

            // Column highlight behind the bars
            if (isSelected && highlightAlpha > 0f) {
                drawRect(
                    color   = axisColor.copy(alpha = highlightAlpha),
                    topLeft = Offset(leftPadding + index * dayGroupWidth, 0f),
                    size    = Size(dayGroupWidth, chartBottom)
                )
            }

            val rawIncomeHeight =
                if (maxValue == BigDecimal.ZERO) 0f
                else (entry.income.toFloat() / maxValue.toFloat()) * chartHeight * animatedProgress

            val rawExpenseHeight =
                if (maxValue == BigDecimal.ZERO) 0f
                else (entry.expense.toFloat() / maxValue.toFloat()) * chartHeight * animatedProgress

            // FIX: Previously the bar was scaled by 1.08x on selection but the
            // topLeft.y stayed fixed, so the extra height pushed the bar BELOW
            // chartBottom (below the axis line), causing it to "expand outside".
            //
            // Correct approach: keep the bar BOTTOM anchored at chartBottom and
            // grow the bar UPWARD on selection. This means topLeft.y decreases
            // by the extra scaled amount, keeping everything within bounds.
            val scaleFactor      = if (isSelected) 1.08f else 1f
            val incomeHeight     = (rawIncomeHeight  * scaleFactor).coerceAtMost(chartHeight)
            val expenseHeight    = (rawExpenseHeight * scaleFactor).coerceAtMost(chartHeight)

            val groupCenterX = leftPadding + index * dayGroupWidth + (dayGroupWidth / 2f)
            val incomeX      = groupCenterX - barWidth - (intraBarGap / 2f)
            val expenseX     = groupCenterX + (intraBarGap / 2f)

            // topLeft.y = chartBottom - height  (bar grows upward from the axis)
            // size.height = height              (never extends below chartBottom)
            drawRect(
                color   = incomeColor,
                topLeft = Offset(incomeX,  chartBottom - incomeHeight),
                size    = Size(barWidth, incomeHeight)
            )

            drawRect(
                color   = expenseColor,
                topLeft = Offset(expenseX, chartBottom - expenseHeight),
                size    = Size(barWidth, expenseHeight)
            )

            // Day label
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    color     = axisColor.toArgb()
                    textSize  = 11.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.nativeCanvas.drawText(entry.day.toString(), groupCenterX, labelY, paint)
            }
        }
    }
}