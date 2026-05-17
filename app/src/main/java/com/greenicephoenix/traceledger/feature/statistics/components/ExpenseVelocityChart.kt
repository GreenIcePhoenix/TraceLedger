package com.greenicephoenix.traceledger.feature.statistics.components

import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import com.greenicephoenix.traceledger.R
import com.greenicephoenix.traceledger.core.ui.theme.NothingRed
import com.greenicephoenix.traceledger.core.util.formatCompactMagnitude
import com.greenicephoenix.traceledger.feature.statistics.StatisticsViewModel.VelocityPoint
import java.math.BigDecimal

/**
 * Cumulative expense velocity — current month (solid) vs previous month (dashed)
 * vs monthly average (dotted). Shows if you're spending faster than usual.
 */
@Composable
fun ExpenseVelocityChart(
    points:   List<VelocityPoint>,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    val context = LocalContext.current
    val outfitTypeface = remember {
        runCatching { ResourcesCompat.getFont(context, R.font.outfit_regular) }.getOrNull()
            ?: Typeface.DEFAULT
    }

    val maxVal    = points.maxOf { maxOf(it.currentMonth, it.previousMonth, it.monthlyAverage) }
        .takeIf { it > 0.0 } ?: 1.0
    val axisColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        val leftPad = 44.dp.toPx()
        val botPad  = 20.dp.toPx()
        val chartW  = size.width - leftPad
        val chartH  = size.height - botPad
        val step    = chartW / (points.size - 1).coerceAtLeast(1).toFloat()

        fun xAt(i: Int)    = leftPad + i * step
        fun yAt(v: Double) = (chartH - (v / maxVal) * chartH).toFloat()

        // Axes
        drawLine(axisColor, Offset(leftPad, 0f),    Offset(leftPad, chartH),    1.dp.toPx())
        drawLine(axisColor, Offset(leftPad, chartH), Offset(size.width, chartH), 1.dp.toPx())

        // Grid
        repeat(3) { i ->
            val y = chartH - chartH * (i + 1) / 3f
            drawLine(gridColor, Offset(leftPad, y), Offset(size.width, y), 1.dp.toPx())
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    typeface    = outfitTypeface
                    color       = axisColor.toArgb()
                    textSize    = 9.sp.toPx()
                    textAlign   = android.graphics.Paint.Align.RIGHT
                    isAntiAlias = true
                }
                canvas.nativeCanvas.drawText(
                    formatCompactMagnitude(BigDecimal(maxVal * (i + 1) / 3.0)),
                    leftPad - 4.dp.toPx(), y + 3.dp.toPx(), paint
                )
            }
        }

        // Average line (dotted)
        val avgPath = Path()
        points.forEachIndexed { i, pt ->
            val x = xAt(i); val y = yAt(pt.monthlyAverage)
            if (i == 0) avgPath.moveTo(x, y) else avgPath.lineTo(x, y)
        }
        drawPath(avgPath, axisColor.copy(alpha = 0.5f), style = Stroke(
            1.5f.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
        ))

        // Previous month (dashed)
        val prevPath = Path()
        points.forEachIndexed { i, pt ->
            if (pt.previousMonth > 0.0) {
                val x = xAt(i); val y = yAt(pt.previousMonth)
                if (i == 0 || pt.previousMonth == 0.0) prevPath.moveTo(x, y)
                else prevPath.lineTo(x, y)
            }
        }
        drawPath(prevPath, NothingRed.copy(alpha = 0.45f), style = Stroke(
            1.5f.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 4f))
        ))

        // Current month (solid — primary color)
        val currPath = Path()
        val activePoints = points.filter { it.currentMonth > 0.0 }
        activePoints.forEachIndexed { i, pt ->
            val x = xAt(points.indexOf(pt)); val y = yAt(pt.currentMonth)
            if (i == 0) currPath.moveTo(x, y) else currPath.lineTo(x, y)
        }
        drawPath(currPath, NothingRed, style = Stroke(2.dp.toPx()))

        // X label — day numbers (every 5)
        points.forEachIndexed { i, pt ->
            if (pt.day % 5 == 0) {
                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply {
                        typeface    = outfitTypeface
                        color       = axisColor.toArgb()
                        textSize    = 9.sp.toPx()
                        textAlign   = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    canvas.nativeCanvas.drawText("${pt.day}", xAt(i), chartH + 14.dp.toPx(), paint)
                }
            }
        }
    }
}