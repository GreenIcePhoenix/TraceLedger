package com.greenicephoenix.traceledger.feature.statistics.components

import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import com.greenicephoenix.traceledger.R
import com.greenicephoenix.traceledger.feature.statistics.model.CalendarDay
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/**
 * Calendar-grid spending heatmap.
 * 7 columns (Mon–Sun), rows = weeks in the month.
 * Cell color interpolates from surface (no spend) → primary (max spend day).
 *
 * @param onDayTap  Called with the tapped CalendarDay — parent shows detail sheet
 */
@Composable
fun SpendingHeatmap(
    days:      List<CalendarDay>,
    modifier:  Modifier = Modifier,
    onDayTap:  (CalendarDay) -> Unit = {}
) {
    if (days.isEmpty()) return

    val context = LocalContext.current
    val outfitTypeface = remember {
        runCatching { ResourcesCompat.getFont(context, R.font.outfit_regular) }.getOrNull()
            ?: Typeface.DEFAULT
    }

    val primaryColor  = MaterialTheme.colorScheme.primary
    val surfaceColor  = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val todayColor    = MaterialTheme.colorScheme.secondary

    // Build grid: week rows × 7 day columns
    // First day of month — shift to correct column (Mon=0)
    val firstDow = days.first().date.dayOfWeek.value - 1  // Mon=0..Sun=6
    val totalCells = firstDow + days.size
    val numRows = (totalCells + 6) / 7

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height((numRows * 44 + 28).dp)
            .pointerInput(days) {
                detectTapGestures { offset ->
                    val headerH  = 24.dp.toPx()
                    val cellSize = (size.width / 7f)
                    val col      = (offset.x / cellSize).toInt().coerceIn(0, 6)
                    val row      = ((offset.y - headerH) / cellSize).toInt()
                    if (row < 0) return@detectTapGestures
                    val cellIndex = row * 7 + col
                    val dayIndex  = cellIndex - firstDow
                    if (dayIndex in days.indices) onDayTap(days[dayIndex])
                }
            }
    ) {
        val headerH  = 24.dp.toPx()
        val cellSize = size.width / 7f
        val padding  = 3.dp.toPx()

        // Day-of-week headers
        DayOfWeek.values().forEachIndexed { col, dow ->
            val label = dow.getDisplayName(TextStyle.NARROW, Locale.getDefault())
            val x     = col * cellSize + cellSize / 2f
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    typeface    = outfitTypeface
                    color       = onSurfaceColor.copy(alpha = 0.4f).toArgb()
                    textSize    = 10.sp.toPx()
                    textAlign   = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.nativeCanvas.drawText(label, x, headerH - 4.dp.toPx(), paint)
            }
        }

        // Cells
        repeat(numRows) { row ->
            repeat(7) { col ->
                val cellIndex = row * 7 + col
                val dayIndex  = cellIndex - firstDow
                val left      = col * cellSize + padding
                val top       = headerH + row * cellSize + padding
                val cellW     = cellSize - padding * 2f
                val cellH     = cellSize - padding * 2f

                if (dayIndex < 0 || dayIndex >= days.size) {
                    // Empty cell — draw faint placeholder
                    drawRoundRect(
                        color       = surfaceColor.copy(alpha = 0.3f),
                        topLeft     = Offset(left, top),
                        size        = Size(cellW, cellH),
                        cornerRadius = CornerRadius(6.dp.toPx())
                    )
                    return@repeat
                }

                val day   = days[dayIndex]
                // Interpolate: 0 intensity → surfaceVariant, 1 → primary
                var color = lerp(surfaceColor, primaryColor, day.intensity * 0.85f)

                drawRoundRect(
                    color        = color,
                    topLeft      = Offset(left, top),
                    size         = Size(cellW, cellH),
                    cornerRadius = CornerRadius(6.dp.toPx())
                )

                // Day number label
                val textColor = if (day.intensity > 0.5f)
                    onSurfaceColor.copy(alpha = 0.9f) else onSurfaceColor.copy(alpha = 0.5f)

                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply {
                        typeface    = outfitTypeface
                        color       = textColor
                        textSize    = 10.sp.toPx()
                        textAlign   = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    canvas.nativeCanvas.drawText(
                        day.date.dayOfMonth.toString(),
                        left + cellW / 2f,
                        top + cellH / 2f + 4.dp.toPx(),
                        paint
                    )
                }
            }
        }
    }
}