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
import androidx.compose.ui.graphics.Color
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
import com.greenicephoenix.traceledger.domain.model.CategoryUiModel
import com.greenicephoenix.traceledger.feature.statistics.StatisticsViewModel.CategoryComparison
import java.math.BigDecimal

/**
 * Grouped bar chart — top 5 categories, this month vs last month side by side.
 * Category name on Y-axis (horizontal bars for better label readability).
 */
@Composable
fun CategoryComparisonChart(
    comparisons: List<CategoryComparison>,
    categoryMap: Map<String, CategoryUiModel>,
    modifier:    Modifier = Modifier
) {
    if (comparisons.isEmpty()) return

    val context = LocalContext.current
    val outfitTypeface = remember {
        runCatching { ResourcesCompat.getFont(context, R.font.outfit_regular) }.getOrNull()
            ?: Typeface.DEFAULT
    }

    val animProgress by animateFloatAsState(
        targetValue   = 1f,
        animationSpec = tween(animDuration(context, 600)),
        label         = "cat-compare"
    )

    val maxVal     = comparisons.maxOf { maxOf(it.thisMonth, it.lastMonth) }
        .takeIf { it > 0.0 } ?: 1.0
    val onSurface  = MaterialTheme.colorScheme.onSurface
    val dimColor   = MaterialTheme.colorScheme.surfaceVariant
    val fallback   = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height((comparisons.size * 52 + 20).dp)
    ) {
        val labelW    = 80.dp.toPx()
        val chartW    = size.width - labelW - 12.dp.toPx()
        val rowH      = size.height / comparisons.size
        val barH      = rowH * 0.28f
        val barGap    = rowH * 0.06f

        comparisons.forEachIndexed { i, comp ->
            val category  = categoryMap[comp.categoryId]
            val color     = category?.color?.let { Color(it) } ?: fallback
            val centerY   = i * rowH + rowH / 2f
            val thisBarY  = centerY - barH - barGap / 2f
            val lastBarY  = centerY + barGap / 2f

            // Category label
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    typeface    = outfitTypeface
                    this.color  = onSurface.copy(alpha = 0.7f).toArgb()
                    textSize    = 10.sp.toPx()
                    textAlign   = android.graphics.Paint.Align.RIGHT
                    isAntiAlias = true
                }
                canvas.nativeCanvas.drawText(
                    category?.name ?: comp.categoryId.take(8),
                    labelW - 8.dp.toPx(),
                    centerY + 3.dp.toPx(),
                    paint
                )
            }

            // This month bar
            val thisW = (comp.thisMonth / maxVal * chartW * animProgress).toFloat()
            drawRect(color, Offset(labelW, thisBarY), Size(thisW, barH))

            // Last month bar (dimmed)
            val lastW = (comp.lastMonth / maxVal * chartW * animProgress).toFloat()
            drawRect(color.copy(alpha = 0.3f), Offset(labelW, lastBarY), Size(lastW, barH))

            // Value label on this month bar
            if (thisW > 32.dp.toPx()) {
                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply {
                        typeface    = outfitTypeface
                        this.color  = onSurface.copy(alpha = 0.8f).toArgb()
                        textSize    = 9.sp.toPx()
                        textAlign   = android.graphics.Paint.Align.LEFT
                        isAntiAlias = true
                    }
                    canvas.nativeCanvas.drawText(
                        formatCompactMagnitude(BigDecimal(comp.thisMonth)),
                        labelW + thisW + 4.dp.toPx(),
                        thisBarY + barH - 2.dp.toPx(),
                        paint
                    )
                }
            }

            // Change % badge
            val changeText = if (comp.changePercent >= 0) "+${comp.changePercent.toInt()}%"
            else "${comp.changePercent.toInt()}%"
            val badgeColor = if (comp.changePercent > 0)
                com.greenicephoenix.traceledger.core.ui.theme.NothingRed
            else com.greenicephoenix.traceledger.core.ui.theme.SuccessGreen

            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    typeface    = outfitTypeface
                    this.color  = badgeColor.toArgb()
                    textSize    = 9.sp.toPx()
                    textAlign   = android.graphics.Paint.Align.RIGHT
                    isAntiAlias = true
                }
                canvas.nativeCanvas.drawText(
                    changeText,
                    size.width,
                    thisBarY + barH - 2.dp.toPx(),
                    paint
                )
            }
        }
    }
}