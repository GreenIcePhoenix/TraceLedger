package com.greenicephoenix.traceledger.feature.statistics.components

import android.graphics.Typeface
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import com.greenicephoenix.traceledger.R
import com.greenicephoenix.traceledger.core.util.animDuration
import com.greenicephoenix.traceledger.core.util.formatCompactMagnitude
import com.greenicephoenix.traceledger.feature.statistics.StatisticsViewModel
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Daily cashflow bar chart — income (primary) vs expense (error) side-by-side bars.
 *
 * Interactions:
 * - Tap: selects a day, triggers onDaySelected (bottom sheet)
 * - Drag: scrubs across days, triggers onScrub with current entry
 *
 * @param onScrub  Called during drag with the entry under the finger, null on drag end
 */
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

    val context  = LocalContext.current
    // Load Outfit font for axis labels — falls back to system default if unavailable
    val outfitTypeface = remember {
        runCatching { ResourcesCompat.getFont(context, R.font.outfit_regular) }.getOrNull()
            ?: Typeface.DEFAULT
    }

    val maxValue = entries.fold(BigDecimal.ZERO) { acc, e -> maxOf(acc, e.income, e.expense) }

    val animProgress by animateFloatAsState(
        targetValue   = 1f,
        animationSpec = tween(animDuration(context, 500)),
        label         = "cashflow-bars"
    )

    val highlightAlpha by animateFloatAsState(
        targetValue   = if (selectedDay != null) 0.10f else 0f,
        animationSpec = tween(180),
        label         = "cashflow-highlight"
    )

    val axisColor    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
    val gridColor    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val incomeColor  = MaterialTheme.colorScheme.primary
    val expenseColor = MaterialTheme.colorScheme.error

    // Scrub position as fraction of usable width
    var scrubFraction by remember { mutableStateOf<Float?>(null) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            // Tap — select a day (triggers bottom sheet)
            .pointerInput(entries) {
                detectTapGestures { offset ->
                    val leftPx = 44.dp.toPx()
                    if (offset.x < leftPx || entries.isEmpty()) return@detectTapGestures
                    val index = ((offset.x - leftPx) / ((size.width - leftPx) / entries.size)).toInt()
                    if (index in entries.indices) onDaySelected(entries[index])
                }
            }
            // Drag — scrub (shows inline tooltip, NOT bottom sheet)
            .pointerInput(entries) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val leftPx = 44.dp.toPx()
                        if (offset.x >= leftPx && entries.isNotEmpty()) {
                            scrubFraction = ((offset.x - leftPx) / (size.width - leftPx)).coerceIn(0f, 1f)
                            val index = (scrubFraction!! * entries.size).toInt().coerceIn(0, entries.lastIndex)
                            onScrub?.invoke(entries[index])
                        }
                    },
                    onDrag = { change, _ ->
                        val leftPx = 44.dp.toPx()
                        scrubFraction = ((change.position.x - leftPx) / (size.width - leftPx)).coerceIn(0f, 1f)
                        val index = (scrubFraction!! * entries.size).toInt().coerceIn(0, entries.lastIndex)
                        onScrub?.invoke(entries[index])
                    },
                    onDragEnd   = { scrubFraction = null; onScrub?.invoke(null) },
                    onDragCancel = { scrubFraction = null; onScrub?.invoke(null) }
                )
            }
    ) {
        val leftPad       = 44.dp.toPx()
        val chartHeight   = size.height * 0.75f
        val chartBottom   = chartHeight
        val labelY        = chartBottom + 22.dp.toPx()
        val dayGroupWidth = (size.width - leftPad) / entries.size
        val barWidth      = dayGroupWidth * 0.25f
        val intraBarGap   = barWidth * 0.3f

        // Axes
        drawLine(axisColor, Offset(leftPad, 0f),         Offset(leftPad, chartBottom),    1.dp.toPx())
        drawLine(axisColor, Offset(leftPad, chartBottom), Offset(size.width, chartBottom), 1.dp.toPx())

        // Grid + Y labels
        repeat(4) { step ->
            val fraction = (step + 1) / 4f
            val y        = chartBottom - chartBottom * fraction
            val value    = maxValue.multiply(BigDecimal(fraction.toDouble()))
                .setScale(2, RoundingMode.HALF_UP)

            drawLine(gridColor, Offset(leftPad, y), Offset(size.width, y), 1.dp.toPx())

            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    typeface    = outfitTypeface
                    color       = axisColor.toArgb()
                    textSize    = 10.sp.toPx()
                    textAlign   = android.graphics.Paint.Align.RIGHT
                    isAntiAlias = true
                }
                canvas.nativeCanvas.drawText(
                    formatCompactMagnitude(value),
                    leftPad - 6.dp.toPx(),
                    y + 4.dp.toPx(),
                    paint
                )
            }
        }

        // Scrub line
        scrubFraction?.let { frac ->
            val scrubX = leftPad + frac * (size.width - leftPad)
            drawLine(
                color       = axisColor.copy(alpha = 0.4f),
                start       = Offset(scrubX, 0f),
                end         = Offset(scrubX, chartBottom),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Bars + X labels
        entries.forEachIndexed { index, entry ->
            val isSelected = selectedDay == entry.day
            val scaleFactor = if (isSelected) 1.08f else 1f

            // Column highlight
            if (isSelected && highlightAlpha > 0f) {
                drawRect(
                    color   = axisColor.copy(alpha = highlightAlpha),
                    topLeft = Offset(leftPad + index * dayGroupWidth, 0f),
                    size    = Size(dayGroupWidth, chartBottom)
                )
            }

            val rawInH  = if (maxValue == BigDecimal.ZERO) 0f
            else (entry.income.toFloat() / maxValue.toFloat()) * chartHeight * animProgress
            val rawExpH = if (maxValue == BigDecimal.ZERO) 0f
            else (entry.expense.toFloat() / maxValue.toFloat()) * chartHeight * animProgress

            val incH  = (rawInH  * scaleFactor).coerceAtMost(chartHeight)
            val expH  = (rawExpH * scaleFactor).coerceAtMost(chartHeight)

            val groupCenterX = leftPad + index * dayGroupWidth + dayGroupWidth / 2f
            val incomeX      = groupCenterX - barWidth - intraBarGap / 2f
            val expenseX     = groupCenterX + intraBarGap / 2f

            drawRect(incomeColor,  Offset(incomeX,  chartBottom - incH),  Size(barWidth, incH))
            drawRect(expenseColor, Offset(expenseX, chartBottom - expH),  Size(barWidth, expH))

            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    typeface    = outfitTypeface
                    color       = axisColor.toArgb()
                    textSize    = 10.sp.toPx()
                    textAlign   = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.nativeCanvas.drawText(entry.day.toString(), groupCenterX, labelY, paint)
            }
        }
    }
}