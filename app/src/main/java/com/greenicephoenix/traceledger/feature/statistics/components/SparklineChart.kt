package com.greenicephoenix.traceledger.feature.statistics.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Minimal inline trend chart — no axes, no labels, just the shape.
 * Used on StatisticsScreen hub cards to hint at the month's trend.
 *
 * @param points     Ordered list of float values (e.g. daily totals)
 * @param color      Line and area fill color (typically SuccessGreen or ErrorRed)
 * @param showArea   If true, fills area under line with gradient
 */
@Composable
fun SparklineChart(
    points:    List<Float>,
    color:     Color,
    modifier:  Modifier = Modifier,
    showArea:  Boolean  = true
) {
    if (points.size < 2) return

    val maxVal = remember(points) { points.max() }
    val minVal = remember(points) { points.min() }
    val range  = (maxVal - minVal).takeIf { it > 0f } ?: 1f

    Canvas(modifier = modifier) {
        val w     = size.width
        val h     = size.height
        val stepX = w / (points.size - 1).toFloat()

        fun xAt(i: Int)   = i * stepX
        fun yAt(v: Float) = h - ((v - minVal) / range) * h * 0.85f // 15% top padding

        // Build line path
        val linePath = Path()
        points.forEachIndexed { i, v ->
            val x = xAt(i); val y = yAt(v)
            if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
        }

        // Area fill
        if (showArea) {
            val areaPath = Path().apply {
                addPath(linePath)
                lineTo(xAt(points.lastIndex), h)
                lineTo(xAt(0), h)
                close()
            }
            drawPath(
                path  = areaPath,
                brush = Brush.verticalGradient(
                    colors    = listOf(color.copy(alpha = 0.35f), color.copy(alpha = 0f)),
                    startY    = 0f,
                    endY      = h
                )
            )
        }

        // Line stroke
        drawPath(
            path        = linePath,
            color       = color,
            style       = Stroke(width = 2f)
        )

        // End dot
        val lastX = xAt(points.lastIndex)
        val lastY = yAt(points.last())
        drawCircle(color = color, radius = 3.5f, center = Offset(lastX, lastY))
    }
}