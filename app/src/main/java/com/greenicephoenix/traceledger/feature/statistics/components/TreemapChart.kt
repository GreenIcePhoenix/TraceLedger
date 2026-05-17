package com.greenicephoenix.traceledger.feature.statistics.components

import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import com.greenicephoenix.traceledger.R
import com.greenicephoenix.traceledger.core.util.animDuration
import com.greenicephoenix.traceledger.domain.model.CategoryUiModel
import com.greenicephoenix.traceledger.feature.statistics.StatisticsViewModel.TreemapNode

/**
 * Squarified treemap — category spending as proportional rectangles.
 * Implements the squarify algorithm for optimal aspect ratios.
 *
 * @param onNodeTap  Called with categoryId when a rectangle is tapped
 */
@Composable
fun TreemapChart(
    nodes:       List<TreemapNode>,
    categoryMap: Map<String, CategoryUiModel>,
    modifier:    Modifier = Modifier,
    onNodeTap:   ((categoryId: String) -> Unit)? = null
) {
    if (nodes.isEmpty()) return

    val context = LocalContext.current
    val outfitTypeface = remember {
        runCatching { ResourcesCompat.getFont(context, R.font.outfit_regular) }.getOrNull()
            ?: Typeface.DEFAULT
    }

    val animProgress = remember(nodes) { Animatable(0f) }
    LaunchedEffect(nodes) {
        animProgress.snapTo(0f)
        val dur = animDuration(context, 600)
        if (dur == 0) animProgress.snapTo(1f)
        else animProgress.animateTo(1f, tween(dur, easing = FastOutSlowInEasing))
    }

    var selectedId by remember { mutableStateOf<String?>(null) }
    // Store computed rects for hit testing — updated each draw
    val rectMap = remember { mutableMapOf<String, Rect>() }

    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val fallbackColor  = MaterialTheme.colorScheme.surfaceVariant

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(320.dp)
            .pointerInput(nodes) {
                detectTapGestures { offset ->
                    val tapped = rectMap.entries.firstOrNull { (_, rect) ->
                        rect.contains(offset)
                    }?.key
                    if (tapped != null) {
                        selectedId = if (selectedId == tapped) null else tapped
                        onNodeTap?.invoke(tapped)
                    }
                }
            }
    ) {
        val totalW = size.width
        val totalH = size.height
        val gap    = 3.dp.toPx()
        val prog   = animProgress.value

        // Squarify layout
        val rects = squarify(nodes, Rect(0f, 0f, totalW, totalH), gap)

        rects.forEach { (node, rect) ->
            val category  = categoryMap[node.categoryId]
            val baseColor = category?.color?.let { Color(it) } ?: fallbackColor
            val isSelected = node.categoryId == selectedId
            val alpha      = if (isSelected) 1f else (0.75f + 0.25f * prog)
            var color      = baseColor.copy(alpha = alpha)

            rectMap[node.categoryId] = rect

            // Cell background
            drawRoundRect(
                color        = color,
                topLeft      = Offset(rect.left, rect.top),
                size         = Size(rect.width, rect.height),
                cornerRadius = CornerRadius(8.dp.toPx())
            )

            // Selected overlay
            if (isSelected) {
                drawRoundRect(
                    color        = onSurfaceColor.copy(alpha = 0.15f),
                    topLeft      = Offset(rect.left, rect.top),
                    size         = Size(rect.width, rect.height),
                    cornerRadius = CornerRadius(8.dp.toPx())
                )
            }

            // Labels — only if cell is large enough
            if (rect.width > 60.dp.toPx() && rect.height > 40.dp.toPx()) {
                val textColor = onSurfaceColor.copy(alpha = if (isSelected) 1f else 0.9f)
                val name      = category?.name ?: node.categoryId.take(10)

                drawIntoCanvas { canvas ->
                    // Category name
                    val namePaint = android.graphics.Paint().apply {
                        typeface    = outfitTypeface
                        color       = textColor
                        textSize    = 11.sp.toPx()
                        textAlign   = android.graphics.Paint.Align.LEFT
                        isAntiAlias = true
                    }
                    canvas.nativeCanvas.drawText(
                        name,
                        rect.left + 8.dp.toPx(),
                        rect.top + 20.dp.toPx(),
                        namePaint
                    )

                    // Percentage
                    if (rect.height > 56.dp.toPx()) {
                        val pctPaint = android.graphics.Paint().apply {
                            typeface    = outfitTypeface
                            color       = textColor.copy(alpha = 0.75f)
                            textSize    = 10.sp.toPx()
                            textAlign   = android.graphics.Paint.Align.LEFT
                            isAntiAlias = true
                        }
                        canvas.nativeCanvas.drawText(
                            String.format("%.1f%%", node.fraction * 100f),
                            rect.left + 8.dp.toPx(),
                            rect.top + 34.dp.toPx(),
                            pctPaint
                        )
                    }
                }
            }
        }
    }
}

// ── Squarify algorithm ────────────────────────────────────────────────────────
// Lays out nodes into a rectangle with good aspect ratios.

private data class LayoutItem(val node: TreemapNode, val rect: Rect)

private fun squarify(
    nodes: List<TreemapNode>,
    bounds: Rect,
    gap: Float
): List<LayoutItem> {
    if (nodes.isEmpty()) return emptyList()
    val total  = nodes.sumOf { it.amount }.takeIf { it > 0.0 } ?: return emptyList()
    val result = mutableListOf<LayoutItem>()
    layoutRow(nodes, bounds, total, gap, result)
    return result
}

private fun layoutRow(
    nodes:   List<TreemapNode>,
    bounds:  Rect,
    total:   Double,
    gap:     Float,
    result:  MutableList<LayoutItem>
) {
    if (nodes.isEmpty() || bounds.width <= 0f || bounds.height <= 0f) return

    val isWide  = bounds.width >= bounds.height
    val rowSize = if (isWide) bounds.height else bounds.width

    // Find optimal row using squarify's worst-ratio heuristic
    val rowNodes  = mutableListOf(nodes[0])
    var remaining = nodes.drop(1)

    for (node in remaining.toList()) {
        val candidate = rowNodes + node
        if (worstRatio(candidate, rowSize, total, bounds) <=
            worstRatio(rowNodes,  rowSize, total, bounds)) {
            rowNodes.add(node)
            remaining = remaining.drop(1)
        } else break
    }

    // Lay out rowNodes along the short axis
    val rowSum      = rowNodes.sumOf { it.amount }
    val rowFraction = (rowSum / total).toFloat()
    val rowExtent   = if (isWide) bounds.width * rowFraction else bounds.height * rowFraction

    var offset = if (isWide) bounds.top else bounds.left

    rowNodes.forEach { node ->
        val nodeFraction = (node.amount / rowSum).toFloat()
        val nodeSize     = (if (isWide) bounds.height else bounds.width) * nodeFraction

        val rect = if (isWide) {
            Rect(bounds.left, offset, bounds.left + rowExtent - gap, offset + nodeSize - gap)
        } else {
            Rect(offset, bounds.top, offset + nodeSize - gap, bounds.top + rowExtent - gap)
        }

        result.add(LayoutItem(node, rect))
        offset += nodeSize
    }

    // Recurse on remaining nodes in the leftover bounds
    val newBounds = if (isWide)
        Rect(bounds.left + rowExtent, bounds.top, bounds.right, bounds.bottom)
    else
        Rect(bounds.left, bounds.top + rowExtent, bounds.right, bounds.bottom)

    if (remaining.isNotEmpty()) {
        layoutRow(remaining, newBounds, total - rowSum, gap, result)
    }
}

private fun worstRatio(
    nodes:    List<TreemapNode>,
    rowSize:  Float,
    total:    Double,
    bounds:   Rect
): Double {
    if (nodes.isEmpty()) return Double.MAX_VALUE
    val rowSum  = nodes.sumOf { it.amount }
    val rowW    = if (bounds.width >= bounds.height)
        bounds.width * (rowSum / total) else rowSize.toDouble()
    val rowH    = if (bounds.width >= bounds.height)
        rowSize.toDouble() else bounds.height * (rowSum / total)
    var worst   = 0.0
    nodes.forEach { node ->
        val frac = node.amount / rowSum
        val w    = if (bounds.width >= bounds.height) rowW else rowW * frac
        val h    = if (bounds.width >= bounds.height) rowH * frac else rowH
        if (w > 0 && h > 0) {
            val ratio = maxOf(w / h, h / w)
            if (ratio > worst) worst = ratio
        }
    }
    return worst
}