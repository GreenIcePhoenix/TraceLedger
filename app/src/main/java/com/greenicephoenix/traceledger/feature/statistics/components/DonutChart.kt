package com.greenicephoenix.traceledger.feature.statistics.components

import android.graphics.BlurMaskFilter
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.core.currency.CurrencyFormatter
import com.greenicephoenix.traceledger.core.currency.CurrencyManager
import com.greenicephoenix.traceledger.core.util.animDuration
import com.greenicephoenix.traceledger.domain.model.CategoryUiModel
import com.greenicephoenix.traceledger.feature.statistics.StatisticsViewModel
import java.math.BigDecimal
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Unified donut chart for both expense and income breakdowns.
 *
 * Features:
 * - Animated sweep reveal on first composition and data change
 * - Tap a segment to select it (arc widens + glow via BlurMaskFilter)
 * - Animated center label transitions between total and selected segment
 * - Reduced motion: skips animation, snaps directly to final state
 * - Drill-down: onSegmentTap callback for navigation
 *
 * @param centerLabel  String shown in the donut hole when nothing is selected ("EXPENSE"/"INCOME")
 * @param onSegmentTap Optional callback with categoryId — use for drill-down navigation
 */
@Composable
fun DonutChart(
    slices:        List<StatisticsViewModel.CategorySlice>,
    categoryMap:   Map<String, CategoryUiModel>,
    centerLabel:   String,
    modifier:      Modifier = Modifier,
    onSegmentTap:  ((categoryId: String) -> Unit)? = null
) {
    if (slices.isEmpty()) {
        Box(
            modifier         = modifier.size(220.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text  = "No data",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
        return
    }

    val context  = LocalContext.current
    val currency by CurrencyManager.currency.collectAsState()
    val fallback = MaterialTheme.colorScheme.surfaceVariant

    // Animated reveal progress 0→1
    val animProgress = remember(slices) { Animatable(0f) }
    LaunchedEffect(slices) {
        animProgress.snapTo(0f)
        val dur = animDuration(context, 700)
        if (dur == 0) animProgress.snapTo(1f)
        else animProgress.animateTo(1f, tween(dur, easing = FastOutSlowInEasing))
    }

    // Selected segment state
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }

    // Animate stroke width for selected arc (32dp normal → 42dp selected)
    val baseStroke     = 32.dp
    val selectedStroke = 42.dp

    val totalAmount = slices.fold(BigDecimal.ZERO) { acc, s -> acc + s.amount }
    val gapDegrees  = 2f

    // Pre-compute start angles for hit testing
    val segmentAngles = remember(slices) {
        val result = mutableListOf<Triple<String, Float, Float>>() // id, start, end
        var start = -90f
        slices.forEach { slice ->
            val sweep = slice.percentage * 3.6f
            result.add(Triple(slice.categoryId, start, start + sweep))
            start += sweep
        }
        result
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .size(220.dp)
                .pointerInput(slices) {
                    detectTapGestures { offset ->
                        // Convert tap offset to angle relative to center
                        val cx     = size.width  / 2f
                        val cy     = size.height / 2f
                        val dx     = offset.x - cx
                        val dy     = offset.y - cy
                        val radius = sqrt(dx * dx + dy * dy)

                        // Only register taps within the donut ring
                        val strokePx = baseStroke.toPx()
                        val outerR   = (min(size.width, size.height) - strokePx) / 2f + strokePx
                        val innerR   = outerR - strokePx * 1.5f
                        if (radius < innerR || radius > outerR) return@detectTapGestures

                        // atan2 returns angle in radians; convert to degrees
                        // offset by -90 to match our startAngle = -90
                        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                        // normalise to 0..360
                        angle = ((angle + 360f) % 360f)

                        val tapped = segmentAngles.firstOrNull { (_, start, end) ->
                            // Normalise segment angles to 0..360
                            val s = ((start + 360f) % 360f)
                            val e = ((end   + 360f) % 360f)
                            if (s <= e) angle in s..e else angle >= s || angle <= e
                        }?.first

                        if (tapped != null) {
                            selectedCategoryId =
                                if (selectedCategoryId == tapped) null else tapped
                            if (tapped != null) onSegmentTap?.invoke(tapped)
                        }
                    }
                }
        ) {
            val progress    = animProgress.value
            val strokePx    = baseStroke.toPx()
            val selStrokePx = selectedStroke.toPx()
            val diameter    = min(size.width, size.height) - strokePx
            val topLeft     = androidx.compose.ui.geometry.Offset(strokePx / 2f, strokePx / 2f)
            val arcSize     = Size(diameter, diameter)

            var startAngle = -90f

            slices.forEach { slice ->
                val isSelected  = slice.categoryId == selectedCategoryId
                val fullSweep   = slice.percentage * 3.6f
                val sweepAngle  = fullSweep * progress
                val color       = categoryMap[slice.categoryId]?.color?.let { Color(it) } ?: fallback
                val strokeWidth = if (isSelected) selStrokePx else strokePx

                // Selected segment: glow via BlurMaskFilter
                if (isSelected) {
                    drawIntoCanvas { canvas ->
                        val glowPaint = Paint().apply {
                            asFrameworkPaint().apply {
                                isAntiAlias = true
                                this.color  = android.graphics.Color.TRANSPARENT
                                setShadowLayer(16f, 0f, 0f, color.copy(alpha = 0.6f).toArgb())
                                maskFilter  = BlurMaskFilter(12f, BlurMaskFilter.Blur.NORMAL)
                            }
                        }
                        // Draw glow as a slightly larger arc
                        canvas.drawArc(
                            left        = topLeft.x - 4f,
                            top         = topLeft.y - 4f,
                            right       = topLeft.x + arcSize.width + 4f,
                            bottom      = topLeft.y + arcSize.height + 4f,
                            startAngle  = startAngle + gapDegrees / 2f,
                            sweepAngle  = (sweepAngle - gapDegrees).coerceAtLeast(0f),
                            useCenter   = false,
                            paint       = glowPaint
                        )
                    }
                }

                drawArc(
                    color      = color,
                    startAngle = startAngle + gapDegrees / 2f,
                    sweepAngle = (sweepAngle - gapDegrees).coerceAtLeast(0f),
                    useCenter  = false,
                    topLeft    = topLeft,
                    size       = arcSize,
                    style      = Stroke(width = strokeWidth)
                )

                startAngle += fullSweep
            }
        }

        // Center label — animates between total and selected segment amount
        val selectedSlice = slices.firstOrNull { it.categoryId == selectedCategoryId }
        val selectedName  = selectedSlice?.let { categoryMap[it.categoryId]?.name } ?: centerLabel
        val displayAmount = selectedSlice?.amount ?: totalAmount

        AnimatedContent(
            targetState = selectedCategoryId,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
            label = "donut-center"
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text  = selectedName.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    text  = CurrencyFormatter.format(displayAmount.toPlainString(), currency),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}