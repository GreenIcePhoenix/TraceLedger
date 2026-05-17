package com.greenicephoenix.traceledger.feature.statistics.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Floating tooltip shown while scrubbing a chart.
 * Caller positions this via Modifier.offset or Box alignment.
 *
 * @param value   Primary value string (e.g. "₹2,450")
 * @param label   Secondary label (e.g. "15 May" or "Food")
 * @param visible Drives AnimatedVisibility — fade in/out
 */
@Composable
fun ChartTooltip(
    value:   String,
    label:   String,
    visible: Boolean,
    modifier: Modifier = Modifier,
    minWidth: Dp = 80.dp
) {
    AnimatedVisibility(
        visible = visible,
        enter   = fadeIn(),
        exit    = fadeOut(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .defaultMinSize(minWidth = minWidth)
                .background(
                    color = MaterialTheme.colorScheme.inverseSurface,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text  = value,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.inverseOnSurface
            )
            if (label.isNotBlank()) {
                Text(
                    text  = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}