package com.greenicephoenix.traceledger.feature.statistics.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.core.ui.theme.ErrorRed
import com.greenicephoenix.traceledger.core.ui.theme.SuccessGreen
import com.greenicephoenix.traceledger.core.ui.theme.WarningAmber
import com.greenicephoenix.traceledger.feature.statistics.StatisticsViewModel.IncomeStabilityData

/**
 * Income stability display — uses SparklineChart for the monthly bar trend
 * + a stability score gauge row.
 * Reuses existing SparklineChart for the visual.
 */
@Composable
fun IncomeStabilityChart(
    data:     IncomeStabilityData,
    modifier: Modifier = Modifier
) {
    val scoreColor = when {
        data.stabilityScore >= 75 -> SuccessGreen
        data.stabilityScore >= 50 -> WarningAmber
        else                      -> ErrorRed
    }

    val stabilityLabel = when {
        data.stabilityScore >= 75 -> "Stable"
        data.stabilityScore >= 50 -> "Moderate"
        else                      -> "Variable"
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Sparkline of monthly income amounts
        if (data.monthlyAmounts.isNotEmpty()) {
            SparklineChart(
                points   = data.monthlyAmounts.map { it.toFloat() },
                color    = SuccessGreen,
                modifier = Modifier.fillMaxWidth().height(72.dp),
                showArea = true
            )
        }

        // Score row
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Stability Score",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    "${data.stabilityScore}/100",
                    style = MaterialTheme.typography.titleLarge,
                    color = scoreColor
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    stabilityLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = scoreColor
                )
                Text(
                    "CV: ${String.format("%.1f%%", data.cv * 100f)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                )
            }
        }

        // Variance insight
        if (data.mean > 0.0) {
            Text(
                text  = when {
                    data.stabilityScore >= 75 ->
                        "Your income is consistent. Variance is within normal range."
                    data.stabilityScore >= 50 ->
                        "Your income has moderate swings. Consider building a buffer."
                    else ->
                        "High income variability detected. A 3-month expense buffer is recommended."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
        }
    }
}