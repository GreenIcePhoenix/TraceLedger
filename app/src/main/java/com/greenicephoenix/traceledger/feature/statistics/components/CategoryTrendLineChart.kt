package com.greenicephoenix.traceledger.feature.statistics.components

import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import com.greenicephoenix.traceledger.R
import com.greenicephoenix.traceledger.core.util.formatCompactMagnitude
import com.greenicephoenix.traceledger.feature.statistics.StatisticsViewModel.CategoryMonthlyTrend
import java.math.BigDecimal

/**
 * Multi-category line/area chart for spending or income trends across months.
 *
 * @param showAreaFill   If true, fills area under the selected category's line
 * @param lineColor      Primary line color — caller passes theme color (primary or secondary)
 * @param onScrub        Called during drag: (monthLabel, value) pair, null on drag end
 */
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
    val context = LocalContext.current
    val outfitTypeface = remember {
        runCatching { ResourcesCompat.getFont(context, R.font.outfit_regular) }.getOrNull()
            ?: Typeface.DEFAULT
    }

    val trendsByCategory = allTrends.groupBy { it.categoryId }
    val selectedEntries  = trendsByCategory[selectedCategoryId]?.sortedBy { it.month } ?: emptyList()
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

    val maxValue = (selectedEntries + comparisonEntries.flatten()).maxOf { it.total }

    val axisColor  = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
    val gridColor  = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    // Fallback to MaterialTheme.colorScheme.primary if lineColor not specified
    val resolvedColor = if (lineColor == Color.Unspecified)
        MaterialTheme.colorScheme.primary else lineColor

    var scrubFraction by remember { mutableStateOf<Float?>(null) }

    val monthFormatter = java.time.format.DateTimeFormatter.ofPattern("MMM")

    key(selectedCategoryId) {
        Canvas(
            modifier = modifier
                .fillMaxWidth()
                .height(220.dp)
                .pointerInput(selectedEntries) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val leftPx = 52.dp.toPx()
                            if (offset.x >= leftPx && selectedEntries.isNotEmpty()) {
                                scrubFraction = ((offset.x - leftPx) / (size.width - leftPx)).coerceIn(0f, 1f)
                                val idx = (scrubFraction!! * (selectedEntries.size - 1)).toInt()
                                    .coerceIn(0, selectedEntries.lastIndex)
                                val entry = selectedEntries[idx]
                                onScrub?.invoke(
                                    entry.month.format(monthFormatter),
                                    formatCompactMagnitude(entry.total)
                                )
                            }
                        },
                        onDrag = { change, _ ->
                            val leftPx = 52.dp.toPx()
                            scrubFraction = ((change.position.x - leftPx) / (size.width - leftPx)).coerceIn(0f, 1f)
                            val idx = (scrubFraction!! * (selectedEntries.size - 1)).toInt()
                                .coerceIn(0, selectedEntries.lastIndex)
                            val entry = selectedEntries[idx]
                            onScrub?.invoke(
                                entry.month.format(monthFormatter),
                                formatCompactMagnitude(entry.total)
                            )
                        },
                        onDragEnd    = { scrubFraction = null; onScrub?.invoke("", "") },
                        onDragCancel = { scrubFraction = null; onScrub?.invoke("", "") }
                    )
                }
        ) {
            val leftPad      = 52.dp.toPx()
            val bottomPad    = 24.dp.toPx()
            val chartWidth   = size.width - leftPad
            val chartHeight  = size.height - bottomPad
            val monthCount   = (allMonths.size - 1).coerceAtLeast(1)

            fun xFor(monthIndex: Int) = leftPad + chartWidth * (monthIndex.toFloat() / monthCount)
            fun yFor(total: BigDecimal) =
                if (maxValue == BigDecimal.ZERO) chartHeight
                else chartHeight - (total.toFloat() / maxValue.toFloat()) * chartHeight

            // Axes
            drawLine(axisColor, Offset(leftPad, 0f),          Offset(leftPad, chartHeight),    1.dp.toPx())
            drawLine(axisColor, Offset(leftPad, chartHeight),  Offset(size.width, chartHeight), 1.dp.toPx())

            // Grid + Y labels
            repeat(3) { i ->
                val fraction = (i + 1) / 3f
                val y        = chartHeight - chartHeight * fraction
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
                        formatCompactMagnitude(maxValue.multiply(BigDecimal(fraction.toDouble()))),
                        leftPad - 6.dp.toPx(), y + 4.dp.toPx(), paint
                    )
                }
            }

            // Comparison lines (faint)
            comparisonEntries.forEach { entries ->
                if (entries.size < 2) return@forEach
                val path = Path()
                entries.forEachIndexed { i, entry ->
                    val x = xFor(allMonths.indexOf(entry.month))
                    val y = yFor(entry.total)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, axisColor.copy(alpha = 0.20f), style = Stroke(1.dp.toPx()))
            }

            // Area fill under selected line
            if (showAreaFill && selectedEntries.size >= 2) {
                val areaPath = Path()
                selectedEntries.forEachIndexed { i, entry ->
                    val x = xFor(allMonths.indexOf(entry.month))
                    val y = yFor(entry.total)
                    if (i == 0) areaPath.moveTo(x, y) else areaPath.lineTo(x, y)
                }
                areaPath.lineTo(xFor(allMonths.indexOf(selectedEntries.last().month)), chartHeight)
                areaPath.lineTo(xFor(allMonths.indexOf(selectedEntries.first().month)), chartHeight)
                areaPath.close()
                drawPath(
                    areaPath,
                    Brush.verticalGradient(
                        listOf(resolvedColor.copy(alpha = 0.30f), resolvedColor.copy(alpha = 0f)),
                        startY = 0f, endY = chartHeight
                    )
                )
            }

            // Selected line + dots
            if (selectedEntries.size == 1) {
                val x = leftPad + chartWidth / 2f
                val y = yFor(selectedEntries.first().total)
                drawCircle(resolvedColor, 6.dp.toPx(), Offset(x, y))
            } else {
                val linePath = Path()
                selectedEntries.forEachIndexed { i, entry ->
                    val x = xFor(allMonths.indexOf(entry.month))
                    val y = yFor(entry.total)
                    if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
                    drawCircle(resolvedColor, 4.dp.toPx(), Offset(x, y))
                }
                drawPath(linePath, resolvedColor, style = Stroke(2.dp.toPx()))

                // X-axis month labels
                selectedEntries.forEach { entry ->
                    val x = xFor(allMonths.indexOf(entry.month))
                    drawIntoCanvas { canvas ->
                        val paint = android.graphics.Paint().apply {
                            typeface    = outfitTypeface
                            color       = axisColor.toArgb()
                            textSize    = 10.sp.toPx()
                            textAlign   = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                        }
                        canvas.nativeCanvas.drawText(
                            entry.month.format(monthFormatter),
                            x, chartHeight + 16.dp.toPx(), paint
                        )
                    }
                }
            }

            // Scrub line
            scrubFraction?.let { frac ->
                val scrubX = leftPad + frac * chartWidth
                drawLine(
                    color       = axisColor.copy(alpha = 0.4f),
                    start       = Offset(scrubX, 0f),
                    end         = Offset(scrubX, chartHeight),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }
    }
}