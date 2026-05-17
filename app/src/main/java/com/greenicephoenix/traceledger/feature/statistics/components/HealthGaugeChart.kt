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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.core.ui.theme.ErrorRed
import com.greenicephoenix.traceledger.core.ui.theme.SuccessGreen
import com.greenicephoenix.traceledger.core.ui.theme.WarningAmber
import com.greenicephoenix.traceledger.core.util.animDuration
import com.greenicephoenix.traceledger.feature.statistics.StatisticsViewModel.HealthScore
import kotlin.math.cos
import kotlin.math.sin

/**
 * Semi-circle gauge showing financial health score 0–100.
 * Arc zones: 0–50 = red, 50–70 = amber, 70–85 = primary, 85–100 = green.
 * Needle animates to current score position.
 */
@Composable
fun HealthGaugeChart(
    data:     HealthScore,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val animProgress = remember(data.score) { Animatable(0f) }
    LaunchedEffect(data.score) {
        animProgress.snapTo(0f)
        val dur = animDuration(context, 800)
        if (dur == 0) animProgress.snapTo(data.score / 100f)
        else animProgress.animateTo(
            data.score / 100f,
            tween(dur, easing = FastOutSlowInEasing)
        )
    }

    val primaryColor  = MaterialTheme.colorScheme.primary
    val surfaceVar    = MaterialTheme.colorScheme.surfaceVariant
    val onSurface     = MaterialTheme.colorScheme.onSurface

    val scoreColor = when {
        data.score >= 85 -> SuccessGreen
        data.score >= 70 -> primaryColor
        data.score >= 50 -> WarningAmber
        else             -> ErrorRed
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
            val cx       = size.width / 2f
            val cy       = size.height * 0.85f
            val radius   = size.width * 0.38f
            val strokeW  = 20.dp.toPx()
            val topLeft  = Offset(cx - radius, cy - radius)
            val arcSize  = Size(radius * 2f, radius * 2f)

            // Background track (180° semi-circle)
            drawArc(
                color      = surfaceVar,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter  = false,
                topLeft    = topLeft,
                size       = arcSize,
                style      = Stroke(strokeW, cap = StrokeCap.Round)
            )

            // Zone arcs
            data class Zone(val start: Float, val sweep: Float, val color: Color)
            val zones = listOf(
                Zone(180f, 90f,  ErrorRed),       // 0–50%   → red
                Zone(270f, 36f,  WarningAmber),   // 50–70%  → amber
                Zone(306f, 27f,  primaryColor),   // 70–85%  → violet
                Zone(333f, 27f,  SuccessGreen)    // 85–100% → green
            )
            zones.forEach { zone ->
                drawArc(
                    color      = zone.color.copy(alpha = 0.35f),
                    startAngle = zone.start,
                    sweepAngle = zone.sweep,
                    useCenter  = false,
                    topLeft    = topLeft,
                    size       = arcSize,
                    style      = Stroke(strokeW, cap = StrokeCap.Butt)
                )
            }

            // Filled arc up to current score
            val sweepAngle = 180f * animProgress.value
            drawArc(
                color      = scoreColor,
                startAngle = 180f,
                sweepAngle = sweepAngle,
                useCenter  = false,
                topLeft    = topLeft,
                size       = arcSize,
                style      = Stroke(strokeW, cap = StrokeCap.Round)
            )

            // Needle
            val angleRad = Math.toRadians((180f + sweepAngle).toDouble())
            val needleLen = radius - strokeW / 2f - 4.dp.toPx()
            val needleX  = cx + cos(angleRad).toFloat() * needleLen
            val needleY  = cy + sin(angleRad).toFloat() * needleLen
            drawLine(
                color       = onSurface,
                start       = Offset(cx, cy),
                end         = Offset(needleX, needleY),
                strokeWidth = 3.dp.toPx(),
                cap         = StrokeCap.Round
            )
            drawCircle(onSurface, 5.dp.toPx(), Offset(cx, cy))
        }

        // Score text centered below gauge
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.padding(top = 96.dp)
        ) {
            Text(
                text       = data.grade,
                style      = MaterialTheme.typography.displaySmall,
                color      = scoreColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text  = "${data.score}/100",
                style = MaterialTheme.typography.bodySmall,
                color = onSurface.copy(alpha = 0.5f)
            )
        }
    }
}