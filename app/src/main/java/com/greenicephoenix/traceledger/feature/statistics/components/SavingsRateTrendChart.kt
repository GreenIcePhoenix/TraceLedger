package com.greenicephoenix.traceledger.feature.statistics.components

import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
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
import com.greenicephoenix.traceledger.core.ui.theme.SuccessGreen
import com.greenicephoenix.traceledger.feature.statistics.StatisticsViewModel.SavingsRatePoint

/**
 * Line chart of monthly savings rate (%) — last 12 months.
 * Zero line drawn at midpoint. Positive = green, negative = red fill.
 */
@Composable
fun SavingsRateTrendChart(
    points:   List<SavingsRatePoint>,
    modifier: Modifier = Modifier
) {
    if (points.size < 2) return

    val context = LocalContext.current
    val outfitTypeface = remember {
        runCatching { ResourcesCompat.getFont(context, R.font.outfit_regular) }.getOrNull()
            ?: Typeface.DEFAULT
    }

    val axisColor  = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
    val maxAbs     = points.maxOf { kotlin.math.abs(it.rate) }.coerceAtLeast(0.01f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
    ) {
        val leftPad  = 36.dp.toPx()
        val botPad   = 20.dp.toPx()
        val chartW   = size.width - leftPad
        val chartH   = size.height - botPad
        val midY     = chartH / 2f
        val step     = chartW / (points.size - 1).coerceAtLeast(1).toFloat()

        fun xAt(i: Int)   = leftPad + i * step
        fun yAt(r: Float) = midY - (r / maxAbs) * midY * 0.85f

        // Zero line
        drawLine(axisColor, Offset(leftPad, midY), Offset(size.width, midY), 1.dp.toPx())

        // Positive area (green)
        val posPath = Path()
        var started = false
        points.forEachIndexed { i, pt ->
            val x = xAt(i); val y = yAt(pt.rate.coerceAtLeast(0f))
            if (!started) { posPath.moveTo(x, midY); started = true }
            posPath.lineTo(x, y)
        }
        posPath.lineTo(xAt(points.lastIndex), midY); posPath.close()
        drawPath(posPath, Brush.verticalGradient(
            listOf(SuccessGreen.copy(0.35f), SuccessGreen.copy(0f)),
            startY = 0f, endY = midY
        ))

        // Negative area (red)
        val negPath = Path()
        started = false
        points.forEachIndexed { i, pt ->
            val x = xAt(i); val y = yAt(pt.rate.coerceAtMost(0f))
            if (!started) { negPath.moveTo(x, midY); started = true }
            negPath.lineTo(x, y)
        }
        negPath.lineTo(xAt(points.lastIndex), midY); negPath.close()
        drawPath(negPath, Brush.verticalGradient(
            listOf(NothingRed.copy(0f), NothingRed.copy(0.30f)),
            startY = midY, endY = chartH
        ))

        // Main line
        val linePath = Path()
        points.forEachIndexed { i, pt ->
            val x = xAt(i); val y = yAt(pt.rate)
            if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
        }
        drawPath(linePath, SuccessGreen, style = Stroke(2.dp.toPx()))

        // X labels
        points.forEachIndexed { i, pt ->
            if (i % 2 == 0) {
                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply {
                        typeface    = outfitTypeface
                        color       = axisColor.toArgb()
                        textSize    = 9.sp.toPx()
                        textAlign   = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    canvas.nativeCanvas.drawText(pt.monthLabel, xAt(i), chartH + 14.dp.toPx(), paint)
                }
            }
        }

        // Y label (0%)
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint().apply {
                typeface    = outfitTypeface
                color       = axisColor.toArgb()
                textSize    = 9.sp.toPx()
                textAlign   = android.graphics.Paint.Align.RIGHT
                isAntiAlias = true
            }
            canvas.nativeCanvas.drawText("0%", leftPad - 4.dp.toPx(), midY + 3.dp.toPx(), paint)
        }
    }
}