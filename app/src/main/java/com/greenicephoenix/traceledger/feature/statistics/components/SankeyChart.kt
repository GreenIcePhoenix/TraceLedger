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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import com.greenicephoenix.traceledger.domain.model.CategoryUiModel
import com.greenicephoenix.traceledger.feature.statistics.StatisticsViewModel.SankeyLink
import com.greenicephoenix.traceledger.feature.statistics.StatisticsViewModel.SankeyNode
import com.greenicephoenix.traceledger.feature.statistics.StatisticsViewModel.SankeyNodeType

/**
 * Sankey flow diagram — income sources (left) → expense categories (right).
 * Flows are bezier curves whose width represents the proportional amount.
 * Max 5 nodes per side to keep the chart readable.
 */
@Composable
fun SankeyChart(
    nodes:       List<SankeyNode>,
    links:       List<SankeyLink>,
    categoryMap: Map<String, CategoryUiModel>,
    modifier:    Modifier = Modifier
) {
    if (nodes.isEmpty() || links.isEmpty()) return

    val context = LocalContext.current
    val outfitTypeface = remember {
        runCatching { ResourcesCompat.getFont(context, R.font.outfit_regular) }.getOrNull()
            ?: Typeface.DEFAULT
    }

    val animProgress = remember(nodes) { Animatable(0f) }
    LaunchedEffect(nodes) {
        animProgress.snapTo(0f)
        val dur = animDuration(context, 800)
        if (dur == 0) animProgress.snapTo(1f)
        else animProgress.animateTo(1f, tween(dur, easing = FastOutSlowInEasing))
    }

    val incomeNodes  = nodes.filter { it.type == SankeyNodeType.INCOME  }.take(5)
    val expenseNodes = nodes.filter { it.type == SankeyNodeType.EXPENSE }.take(5)
    val fallback     = MaterialTheme.colorScheme.surfaceVariant
    val onSurface    = MaterialTheme.colorScheme.onSurface

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        val nodeW    = 14.dp.toPx()
        val padding  = 24.dp.toPx()
        val chartH   = size.height - padding * 2f
        val leftX    = padding
        val rightX   = size.width - padding - nodeW
        val prog     = animProgress.value

        // Layout income nodes (left column)
        val incomeGap  = if (incomeNodes.size > 1) chartH / incomeNodes.size else chartH / 2f
        val incomeRects = incomeNodes.mapIndexed { i, node ->
            val nodeH = chartH / incomeNodes.size.coerceAtLeast(1) * 0.7f
            val top   = padding + i * incomeGap + (incomeGap - nodeH) / 2f
            node.id to Pair(top, top + nodeH)
        }.toMap()

        // Layout expense nodes (right column)
        val expenseGap  = if (expenseNodes.size > 1) chartH / expenseNodes.size else chartH / 2f
        val expenseRects = expenseNodes.mapIndexed { i, node ->
            val nodeH = chartH / expenseNodes.size.coerceAtLeast(1) * 0.7f
            val top   = padding + i * expenseGap + (expenseGap - nodeH) / 2f
            node.id to Pair(top, top + nodeH)
        }.toMap()

        // Draw flow links (bezier curves)
        links.filter { link ->
            incomeRects.containsKey(link.sourceId) && expenseRects.containsKey(link.targetId)
        }.forEach { link ->
            val (srcTop, srcBot) = incomeRects[link.sourceId] ?: return@forEach
            val (dstTop, dstBot) = expenseRects[link.targetId] ?: return@forEach

            val srcMid   = (srcTop + srcBot) / 2f
            val dstMid   = (dstTop + dstBot) / 2f
            val linkH    = ((srcBot - srcTop) * link.fraction * prog).coerceAtLeast(2.dp.toPx())
            val srcColor = categoryMap[link.sourceId]?.color?.let { Color(it) } ?: SuccessGreen

            val path = Path().apply {
                val startY = srcMid - linkH / 2f
                val endY   = dstMid - linkH / 2f
                moveTo(leftX + nodeW, startY)
                cubicTo(
                    (leftX + nodeW + (rightX - leftX - nodeW) * 0.4f), startY,
                    (leftX + nodeW + (rightX - leftX - nodeW) * 0.6f), endY,
                    rightX, endY
                )
                lineTo(rightX, endY + linkH)
                cubicTo(
                    (leftX + nodeW + (rightX - leftX - nodeW) * 0.6f), startY + linkH,
                    (leftX + nodeW + (rightX - leftX - nodeW) * 0.4f), startY + linkH,
                    leftX + nodeW, startY + linkH
                )
                close()
            }
            drawPath(path, srcColor.copy(alpha = 0.25f * prog))
        }

        // Draw income nodes (left)
        incomeNodes.forEachIndexed { i, node ->
            val (top, bot) = incomeRects[node.id] ?: return@forEachIndexed
            val color      = categoryMap[node.id]?.color?.let { Color(it) } ?: SuccessGreen
            drawRect(color.copy(alpha = prog), Offset(leftX, top), Size(nodeW, bot - top))
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    typeface    = outfitTypeface
                    this.color  = onSurface.copy(alpha = 0.7f).toArgb()
                    textSize    = 9.sp.toPx()
                    textAlign   = android.graphics.Paint.Align.RIGHT
                    isAntiAlias = true
                }
                val name = categoryMap[node.id]?.name ?: node.label.take(10)
                canvas.nativeCanvas.drawText(name, leftX - 4.dp.toPx(), (top + bot) / 2f + 3.dp.toPx(), paint)
            }
        }

        // Draw expense nodes (right)
        expenseNodes.forEachIndexed { i, node ->
            val (top, bot) = expenseRects[node.id] ?: return@forEachIndexed
            val color      = categoryMap[node.id]?.color?.let { Color(it) } ?: NothingRed
            drawRect(color.copy(alpha = prog), Offset(rightX, top), Size(nodeW, bot - top))
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    typeface    = outfitTypeface
                    this.color  = onSurface.copy(alpha = 0.7f).toArgb()
                    textSize    = 9.sp.toPx()
                    textAlign   = android.graphics.Paint.Align.LEFT
                    isAntiAlias = true
                }
                val name = categoryMap[node.id]?.name ?: node.label.take(10)
                canvas.nativeCanvas.drawText(name, rightX + nodeW + 4.dp.toPx(), (top + bot) / 2f + 3.dp.toPx(), paint)
            }
        }
    }
}