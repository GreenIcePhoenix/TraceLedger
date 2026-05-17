package com.greenicephoenix.traceledger.feature.statistics.components

import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import com.greenicephoenix.traceledger.core.util.animDuration
import com.greenicephoenix.traceledger.core.util.formatCompactMagnitude
import com.greenicephoenix.traceledger.feature.statistics.StatisticsViewModel.MonthlyAreaPoint
import java.math.BigDecimal

/**
 * Overlapping area chart — income (green) and expense (red) over 12 months.
 * Both areas are semi-transparent so the overlap is visible.
 */
@Composable
fun AreaChart(
    points:   List<MonthlyAreaPoint>,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    val context = LocalContext.current
    val outfitTypeface = remember {
        runCatching { ResourcesCompat.getFont(context, R.font.outfit_regular) }.getOrNull()
            ?: Typeface.DEFAULT
    }

    val animProgress = remember(points) { Animatable(0f) }
    LaunchedEffect(points) {
        animProgress.snapTo(0f)
        val dur = animDuration(context, 700)
        if (dur == 0) animProgress.snapTo(1f)
        else animProgress.animateTo(1f, tween(dur, easing = FastOutSlowInEasing))
    }

    val maxVal   = points.maxOf { maxOf(it.income, it.expense) }.takeIf { it > 0.0 } ?: 1.0
    val axisColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
    ) {
        val leftPad   = 48.dp.toPx()
        val bottomPad = 24.dp.toPx()
        val chartW    = size.width - leftPad
        val chartH    = size.height - bottomPad
        val n         = points.size
        val step      = chartW / (n - 1).coerceAtLeast(1).toFloat()

        fun xAt(i: Int)   = leftPad + i * step
        fun yAt(v: Double) = (chartH - (v / maxVal * chartH * animProgress.value)).toFloat()

        // Axes
        drawLine(axisColor, Offset(leftPad, 0f),    Offset(leftPad, chartH),    1.dp.toPx())
        drawLine(axisColor, Offset(leftPad, chartH), Offset(size.width, chartH), 1.dp.toPx())

        // Grid + Y labels
        repeat(3) { i ->
            val fraction = (i + 1) / 3f
            val y        = chartH - chartH * fraction
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
                    formatCompactMagnitude(BigDecimal(maxVal * fraction)),
                    leftPad - 4.dp.toPx(), y + 4.dp.toPx(), paint
                )
            }
        }

        // Income area
        val incomePath = Path()
        points.forEachIndexed { i, pt ->
            val x = xAt(i); val y = yAt(pt.income)
            if (i == 0) incomePath.moveTo(x, y) else incomePath.lineTo(x, y)
        }
        val incomeArea = Path().apply {
            addPath(incomePath)
            lineTo(xAt(n - 1), chartH); lineTo(xAt(0), chartH); close()
        }
        drawPath(incomeArea, Brush.verticalGradient(
            listOf(SuccessGreen.copy(alpha = 0.35f), SuccessGreen.copy(alpha = 0f)),
            startY = 0f, endY = chartH
        ))
        drawPath(incomePath, SuccessGreen, style = Stroke(2.dp.toPx()))

        // Expense area
        val expensePath = Path()
        points.forEachIndexed { i, pt ->
            val x = xAt(i); val y = yAt(pt.expense)
            if (i == 0) expensePath.moveTo(x, y) else expensePath.lineTo(x, y)
        }
        val expenseArea = Path().apply {
            addPath(expensePath)
            lineTo(xAt(n - 1), chartH); lineTo(xAt(0), chartH); close()
        }
        drawPath(expenseArea, Brush.verticalGradient(
            listOf(NothingRed.copy(alpha = 0.30f), NothingRed.copy(alpha = 0f)),
            startY = 0f, endY = chartH
        ))
        drawPath(expensePath, NothingRed, style = Stroke(2.dp.toPx()))

        // X-axis month labels (every other month to avoid crowding)
        points.forEachIndexed { i, pt ->
            if (i % 2 == 0) {
                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply {
                        typeface    = outfitTypeface
                        color       = axisColor.toArgb()
                        textSize    = 10.sp.toPx()
                        textAlign   = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    canvas.nativeCanvas.drawText(
                        pt.monthLabel, xAt(i), chartH + 16.dp.toPx(), paint
                    )
                }
            }
        }
    }
}