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
import com.greenicephoenix.traceledger.feature.statistics.StatisticsViewModel.TopSpendDay
import java.math.BigDecimal
import java.time.format.DateTimeFormatter

/**
 * Horizontal bar chart of top 10 all-time highest-spend days.
 * Rank number on the left, date + amount on the right.
 */
@Composable
fun TopSpendingDaysChart(
    days:     List<TopSpendDay>,
    modifier: Modifier = Modifier
) {
    if (days.isEmpty()) return

    val context = LocalContext.current
    val outfitTypeface = remember {
        runCatching { ResourcesCompat.getFont(context, R.font.outfit_regular) }.getOrNull()
            ?: Typeface.DEFAULT
    }

    val animProgress by animateFloatAsState(
        targetValue   = 1f,
        animationSpec = tween(animDuration(context, 700)),
        label         = "top-days"
    )

    val maxVal      = days.maxOf { it.total }.takeIf { it > 0.0 } ?: 1.0
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurface   = MaterialTheme.colorScheme.onSurface
    val formatter   = DateTimeFormatter.ofPattern("d MMM yy")
    val rowH        = 36.dp

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(rowH * days.size)
    ) {
        val rankW  = 24.dp.toPx()
        val dateW  = 60.dp.toPx()
        val amtW   = 56.dp.toPx()
        val barArea = size.width - rankW - dateW - amtW - 16.dp.toPx()
        val rH      = size.height / days.size

        days.forEachIndexed { i, day ->
            val y       = i * rH
            val centerY = y + rH / 2f
            val barH    = rH * 0.40f
            val barY    = centerY - barH / 2f
            val fraction = (day.total / maxVal * animProgress).toFloat()
            val alpha    = 1f - (i * 0.07f).coerceAtMost(0.5f)

            // Bar
            drawRect(
                color   = primaryColor.copy(alpha = alpha),
                topLeft = Offset(rankW + dateW + 8.dp.toPx(), barY),
                size    = Size(fraction * barArea, barH)
            )

            drawIntoCanvas { canvas ->
                // Rank
                val rankPaint = android.graphics.Paint().apply {
                    typeface    = outfitTypeface
                    color       = onSurface.copy(alpha = 0.4f + 0.6f * alpha).toArgb()
                    textSize    = 10.sp.toPx()
                    textAlign   = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.nativeCanvas.drawText("#${day.rank}", rankW / 2f, centerY + 3.dp.toPx(), rankPaint)

                // Date
                val datePaint = android.graphics.Paint().apply {
                    typeface    = outfitTypeface
                    color       = onSurface.copy(alpha = 0.65f).toArgb()
                    textSize    = 10.sp.toPx()
                    textAlign   = android.graphics.Paint.Align.LEFT
                    isAntiAlias = true
                }
                canvas.nativeCanvas.drawText(day.date.format(formatter), rankW + 2.dp.toPx(), centerY + 3.dp.toPx(), datePaint)

                // Amount
                val amtPaint = android.graphics.Paint().apply {
                    typeface    = outfitTypeface
                    color       = primaryColor.copy(alpha = alpha).toArgb()
                    textSize    = 10.sp.toPx()
                    textAlign   = android.graphics.Paint.Align.RIGHT
                    isAntiAlias = true
                }
                canvas.nativeCanvas.drawText(
                    formatCompactMagnitude(BigDecimal(day.total)),
                    size.width,
                    centerY + 3.dp.toPx(),
                    amtPaint
                )
            }
        }
    }
}