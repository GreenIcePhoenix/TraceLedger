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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.core.currency.CurrencyFormatter
import com.greenicephoenix.traceledger.core.currency.CurrencyManager
import com.greenicephoenix.traceledger.domain.model.CategoryUiModel
import com.greenicephoenix.traceledger.feature.statistics.StatisticsViewModel
import java.math.BigDecimal
import kotlin.math.min

@Composable
fun IncomeDonutChart(
    slices: List<StatisticsViewModel.CategorySlice>,
    categoryMap: Map<String, CategoryUiModel>,
    modifier: Modifier = Modifier
) {
    if (slices.isEmpty()) {
        Box(
            modifier = modifier.size(220.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text  = "No income data",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
        return
    }

    val animatedProgress = remember(slices) { Animatable(0f) }
    LaunchedEffect(slices) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue   = 1f,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
        )
    }

    val currency   by CurrencyManager.currency.collectAsState()
    val fallback    = MaterialTheme.colorScheme.surfaceVariant
    val totalAmount = slices.fold(BigDecimal.ZERO) { acc, s -> acc + s.amount }
    val gapDegrees  = 2f

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(220.dp)) {
            val strokeWidth = 32.dp.toPx()
            val diameter    = min(size.width, size.height) - strokeWidth
            val topLeft     = androidx.compose.ui.geometry.Offset(strokeWidth / 2f, strokeWidth / 2f)
            val arcSize     = Size(diameter, diameter)
            val progress    = animatedProgress.value

            var startAngle = -90f

            slices.forEach { slice ->
                val fullSweep  = slice.percentage * 3.6f
                val sweepAngle = fullSweep * progress
                val color      = categoryMap[slice.categoryId]?.color?.let { Color(it) } ?: fallback

                drawArc(
                    color      = color,
                    startAngle = startAngle + (gapDegrees / 2f),
                    sweepAngle = (sweepAngle - gapDegrees).coerceAtLeast(0f),
                    useCenter  = false,
                    topLeft    = topLeft,
                    size       = arcSize,
                    style      = Stroke(width = strokeWidth)
                )

                startAngle += fullSweep
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text  = "INCOME",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                text  = CurrencyFormatter.format(totalAmount.toPlainString(), currency),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}