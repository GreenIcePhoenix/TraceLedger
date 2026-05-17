package com.greenicephoenix.traceledger.feature.statistics.components

import android.graphics.Typeface
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import com.greenicephoenix.traceledger.feature.statistics.StatisticsViewModel.WaterfallBar
import java.math.BigDecimal
import kotlin.math.abs

/**
 * Cashflow waterfall chart.
 * Bars float from the previous bar's end — standard waterfall pattern.
 * Opening=primary, Income=SuccessGreen, Expense=NothingRed, Net=primary/error.
 */
@Composable
fun WaterfallChart(
    bars:     List<WaterfallBar>,
    modifier: Modifier = Modifier
) {
    if (bars.isEmpty()) return

    val context = LocalContext.current
    val outfitTypeface = remember {
        runCatching { ResourcesCompat.getFont(context, R.font.outfit_regular) }.getOrNull()
            ?: Typeface.DEFAULT
    }

    val animProgress by animateFloatAsState(
        targetValue   = 1f,
        animationSpec = tween(animDuration(context, 700)),
        label         = "waterfall-anim"
    )

    val maxAbs      = bars.maxOf { abs(it.value) }.takeIf { it > 0.0 } ?: 1.0
    val axisColor   = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
    val primaryColor = MaterialTheme.colorScheme.primary
    val labelColor  = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        val leftPad    = 12.dp.toPx()
        val rightPad   = 12.dp.toPx()
        val bottomPad  = 32.dp.toPx()
        val chartW     = size.width - leftPad - rightPad
        val chartH     = size.height - bottomPad
        val midY       = chartH / 2f  // zero line for waterfall
        val barGroupW  = chartW / bars.size.toFloat()
        val barWidth   = barGroupW * 0.55f

        // Zero line
        drawLine(axisColor, Offset(leftPad, midY), Offset(size.width - rightPad, midY), 1.dp.toPx())

        bars.forEachIndexed { i, bar ->
            val centerX  = leftPad + i * barGroupW + barGroupW / 2f
            val left     = centerX - barWidth / 2f

            val barColor = when (bar.label) {
                "Income"  -> SuccessGreen
                "Expense" -> NothingRed
                "Net"     -> if (bar.isPositive) SuccessGreen else NothingRed
                else      -> primaryColor
            }

            // Bar height proportional to value; grows from midY up (positive) or down (negative)
            val barH   = (abs(bar.value) / maxAbs * (chartH / 2f) * animProgress).toFloat()
            val top    = if (bar.isPositive) midY - barH else midY
            val height = barH

            drawRect(
                color   = barColor.copy(alpha = 0.85f),
                topLeft = Offset(left, top),
                size    = Size(barWidth, height)
            )

            // Value label above/below bar
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    typeface    = outfitTypeface
                    color       = barColor.toArgb()
                    textSize    = 10.sp.toPx()
                    textAlign   = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                val labelY = if (bar.isPositive) top - 4.dp.toPx()
                else top + height + 14.dp.toPx()
                canvas.nativeCanvas.drawText(
                    formatCompactMagnitude(BigDecimal(bar.value)),
                    centerX, labelY, paint
                )
            }

            // Bar label below axis
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    typeface    = outfitTypeface
                    color       = labelColor.toArgb()
                    textSize    = 10.sp.toPx()
                    textAlign   = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.nativeCanvas.drawText(
                    bar.label, centerX, chartH + 20.dp.toPx(), paint
                )
            }
        }
    }
}