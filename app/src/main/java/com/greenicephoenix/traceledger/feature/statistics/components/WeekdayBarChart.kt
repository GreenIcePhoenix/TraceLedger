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
import com.greenicephoenix.traceledger.core.util.animDuration
import com.greenicephoenix.traceledger.core.util.formatCompactMagnitude
import com.greenicephoenix.traceledger.feature.statistics.model.WeekdayPattern
import java.math.BigDecimal

/**
 * 7-bar chart showing total or average spend per day of week (Mon–Sun).
 * The highest bar is highlighted with the primary color; others use surfaceVariant.
 */
@Composable
fun WeekdayBarChart(
    patterns: List<WeekdayPattern>,
    modifier: Modifier = Modifier
) {
    if (patterns.isEmpty()) return

    val context = LocalContext.current
    val outfitTypeface = remember {
        runCatching { ResourcesCompat.getFont(context, R.font.outfit_regular) }.getOrNull()
            ?: Typeface.DEFAULT
    }

    val animProgress by animateFloatAsState(
        targetValue   = 1f,
        animationSpec = tween(animDuration(context, 600)),
        label         = "weekday-bars"
    )

    val maxTotal     = patterns.maxOf { it.totalAmount }
    val primaryColor = MaterialTheme.colorScheme.primary
    val dimColor     = MaterialTheme.colorScheme.surfaceVariant
    val axisColor    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    val labelColor   = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)

    val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    // Build a complete 7-entry list (fill missing days with 0)
    val fullPatterns = (1..7).map { dow ->
        patterns.firstOrNull { it.dayOfWeek == dow }
            ?: WeekdayPattern(dow, 0, 0.0, 0)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        val barGroupW  = size.width / 7f
        val barWidth   = barGroupW * 0.55f
        val chartH     = size.height - 36.dp.toPx()
        val chartBottom = chartH

        // Axis
        drawLine(axisColor, Offset(0f, chartBottom), Offset(size.width, chartBottom), 1.dp.toPx())

        fullPatterns.forEachIndexed { index, pattern ->
            val isMax     = pattern.totalAmount == maxTotal && maxTotal > 0.0
            val barColor  = if (isMax) primaryColor else dimColor
            val fraction  = if (maxTotal > 0.0) (pattern.totalAmount / maxTotal).toFloat() else 0f
            val barH      = fraction * chartH * animProgress
            val centerX   = index * barGroupW + barGroupW / 2f
            val left      = centerX - barWidth / 2f

            drawRect(
                color   = barColor,
                topLeft = Offset(left, chartBottom - barH),
                size    = Size(barWidth, barH)
            )

            // Value label above bar
            if (pattern.totalAmount > 0.0) {
                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply {
                        typeface    = outfitTypeface
                        color       = (if (isMax) primaryColor else labelColor).toArgb()
                        textSize    = 9.sp.toPx()
                        textAlign   = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    canvas.nativeCanvas.drawText(
                        formatCompactMagnitude(BigDecimal(pattern.totalAmount)),
                        centerX,
                        chartBottom - barH - 4.dp.toPx(),
                        paint
                    )
                }
            }

            // Day label below axis
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    typeface    = outfitTypeface
                    color       = labelColor.toArgb()
                    textSize    = 10.sp.toPx()
                    textAlign   = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.nativeCanvas.drawText(
                    dayLabels[index],
                    centerX,
                    chartBottom + 18.dp.toPx(),
                    paint
                )
            }
        }
    }
}