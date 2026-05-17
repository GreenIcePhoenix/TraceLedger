package com.greenicephoenix.traceledger.feature.statistics.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.core.currency.CurrencyFormatter
import com.greenicephoenix.traceledger.core.currency.CurrencyManager
import com.greenicephoenix.traceledger.core.ui.theme.ErrorRed
import com.greenicephoenix.traceledger.core.ui.theme.WarningAmber
import com.greenicephoenix.traceledger.core.util.animDuration
import com.greenicephoenix.traceledger.feature.statistics.StatisticsViewModel

/**
 * Radial progress ring for a single budget.
 * Color thresholds: <80% = primary, 80–99% = WarningAmber, ≥100% = ErrorRed.
 */
@Composable
fun BudgetRing(
    data:     StatisticsViewModel.BudgetRingData,
    modifier: Modifier = Modifier,
    size:     Dp       = 80.dp
) {
    val context  = LocalContext.current
    val currency by CurrencyManager.currency.collectAsState()

    val animProgress = remember(data.utilization) { Animatable(0f) }
    LaunchedEffect(data.utilization) {
        animProgress.snapTo(0f)
        val dur = animDuration(context, 600)
        if (dur == 0) animProgress.snapTo(data.utilization.coerceAtMost(1f))
        else animProgress.animateTo(
            data.utilization.coerceAtMost(1f),
            tween(dur, easing = FastOutSlowInEasing)
        )
    }

    val ringColor = when {
        data.utilization >= 1.0f -> ErrorRed
        data.utilization >= 0.8f -> WarningAmber
        else                     -> MaterialTheme.colorScheme.primary
    }
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier            = modifier.width(size + 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
            Canvas(modifier = Modifier.size(size)) {
                val strokeWidth = (size.value * 0.12f).dp.toPx()
                val diameter    = this.size.width - strokeWidth
                val topLeft     = androidx.compose.ui.geometry.Offset(strokeWidth / 2f, strokeWidth / 2f)
                val arcSize     = Size(diameter, diameter)

                // Track (background ring)
                drawArc(
                    color      = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter  = false,
                    topLeft    = topLeft,
                    size       = arcSize,
                    style      = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Progress arc
                drawArc(
                    color      = ringColor,
                    startAngle = -90f,
                    sweepAngle = animProgress.value * 360f,
                    useCenter  = false,
                    topLeft    = topLeft,
                    size       = arcSize,
                    style      = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Percentage label in center
            Text(
                text  = "${(data.utilization * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = ringColor
            )
        }

        // Budget label
        Text(
            text     = data.label,
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Spent / limit
        Text(
            text  = "${CurrencyFormatter.format(data.spent.toString(), currency)} / ${CurrencyFormatter.format(data.limit.toString(), currency)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}