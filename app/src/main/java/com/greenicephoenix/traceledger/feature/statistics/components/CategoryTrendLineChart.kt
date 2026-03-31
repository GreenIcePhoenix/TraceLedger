package com.greenicephoenix.traceledger.feature.statistics.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenicephoenix.traceledger.feature.statistics.StatisticsViewModel.CategoryMonthlyTrend
import com.greenicephoenix.traceledger.core.util.formatCompactMagnitude
import java.math.BigDecimal

@Composable
fun CategoryTrendLineChart(
    allTrends: List<CategoryMonthlyTrend>,
    selectedCategoryId: String,
    topCategoryIds: List<String>,
    modifier: Modifier = Modifier
) {

    val trendsByCategory =
        allTrends.groupBy { it.categoryId }

    val selectedEntries =
        trendsByCategory[selectedCategoryId]
            ?.sortedBy { it.month }
            ?: emptyList()

    val comparisonEntries =
        topCategoryIds
            .filter { it != selectedCategoryId }
            .mapNotNull { trendsByCategory[it]?.sortedBy { e -> e.month } }

    val allMonths =
        (selectedEntries + comparisonEntries.flatten())
            .map { it.month }
            .distinct()
            .sorted()

    if (selectedEntries.isEmpty()) {
        Text(
            text = "No trend data",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        return
    }

    val maxValue =
        (selectedEntries + comparisonEntries.flatten())
            .maxOf { it.total }

    val axisColor =
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    val gridColor =
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)

    val trendColor = MaterialTheme.colorScheme.primary

    androidx.compose.runtime.key(selectedCategoryId) {
        Canvas(
            modifier = modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {

            val leftPadding = 52.dp.toPx()
            val bottomPadding = 24.dp.toPx()

            val chartWidth = size.width - leftPadding
            val chartHeight = size.height - bottomPadding

            // Axes
            drawLine(
                color = axisColor,
                start = Offset(leftPadding, 0f),
                end = Offset(leftPadding, chartHeight),
                strokeWidth = 1.dp.toPx()
            )

            drawLine(
                color = axisColor,
                start = Offset(leftPadding, chartHeight),
                end = Offset(size.width, chartHeight),
                strokeWidth = 1.dp.toPx()
            )

            // Horizontal grid lines (3)
            repeat(3) { i ->
                val fraction = (i + 1) / 3f
                val y = chartHeight - chartHeight * fraction

                drawLine(
                    color = gridColor,
                    start = Offset(leftPadding, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )

                // Y-axis label (magnitude only)
                val value =
                    maxValue.multiply(BigDecimal(fraction.toDouble()))

                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply {
                        color = axisColor.toArgb()
                        textSize = 10.sp.toPx()
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

            // ── Comparison lines (Top-N categories, faint) ──
            comparisonEntries.forEach { entries ->

                if (entries.size < 2) return@forEach

                val comparisonPath = Path()

                entries.forEachIndexed { index, entry ->
                    val monthIndex = allMonths.indexOf(entry.month)

                    val x =
                        leftPadding +
                                chartWidth * (monthIndex.toFloat() / (allMonths.size - 1).coerceAtLeast(
                            1
                        ))


                    val y =
                        if (maxValue == BigDecimal.ZERO) chartHeight
                        else chartHeight -
                                (entry.total.toFloat() / maxValue.toFloat()) * chartHeight

                    if (index == 0) {
                        comparisonPath.moveTo(x, y)
                    } else {
                        comparisonPath.lineTo(x, y)
                    }
                }

                drawPath(
                    path = comparisonPath,
                    color = axisColor.copy(alpha = 0.25f),
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // Line path
            val path = Path()

            if (selectedEntries.size == 1) {

                // Single data point — draw centered dot
                val entry = selectedEntries.first()

                val x = leftPadding + chartWidth / 2f
                val y =
                    if (maxValue == BigDecimal.ZERO) chartHeight
                    else chartHeight -
                            (entry.total.toFloat() / maxValue.toFloat()) * chartHeight

                drawCircle(
                    color = trendColor,
                    radius = 6.dp.toPx(),
                    center = Offset(x, y)
                )

            } else {

                selectedEntries.forEachIndexed { index, entry ->
                    val monthIndex = allMonths.indexOf(entry.month)

                    val x =
                        leftPadding +
                                chartWidth * (monthIndex.toFloat() / (allMonths.size - 1).coerceAtLeast(
                            1
                        ))


                    val y =
                        if (maxValue == BigDecimal.ZERO) chartHeight
                        else chartHeight -
                                (entry.total.toFloat() / maxValue.toFloat()) * chartHeight

                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }

                    drawCircle(
                        color = trendColor,
                        radius = 4.dp.toPx(),
                        center = Offset(x, y)
                    )
                }

                drawPath(
                    path = path,
                    color = trendColor,
                    style = Stroke(width = 2.dp.toPx())
                )

                // ── X-axis month labels ──
                val monthFormatter =
                    java.time.format.DateTimeFormatter.ofPattern("MMM")

                selectedEntries.forEachIndexed { index, entry ->

                    val monthIndex = allMonths.indexOf(entry.month)

                    val x =
                        leftPadding +
                                chartWidth * (monthIndex.toFloat() / (allMonths.size - 1).coerceAtLeast(
                            1
                        ))


                    drawIntoCanvas { canvas ->
                        val paint = android.graphics.Paint().apply {
                            color = axisColor.toArgb()
                            textSize = 10.sp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                        }

                        canvas.nativeCanvas.drawText(
                            entry.month.format(monthFormatter),
                            x,
                            chartHeight + 16.dp.toPx(),
                            paint
                        )
                    }
                }

            }
        }
    }
}