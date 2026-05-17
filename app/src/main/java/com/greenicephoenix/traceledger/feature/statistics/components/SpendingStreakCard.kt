package com.greenicephoenix.traceledger.feature.statistics.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.core.ui.theme.ErrorRed
import com.greenicephoenix.traceledger.core.ui.theme.SuccessGreen
import com.greenicephoenix.traceledger.core.ui.theme.WarningAmber
import com.greenicephoenix.traceledger.feature.statistics.StatisticsViewModel.SpendingStreak
import java.time.YearMonth

/**
 * Streak card showing consecutive days under budget this month.
 * Green = active streak, Amber = no budgets set, Red = streak broken.
 */
@Composable
fun SpendingStreakCard(
    streak:   SpendingStreak,
    modifier: Modifier = Modifier
) {
    val (accentColor, title, subtitle) = when {
        !streak.monthBudgetOk -> Triple(
            ErrorRed,
            "Budget Exceeded",
            "You've gone over budget this month"
        )
        streak.isActive && streak.days > 0 -> Triple(
            SuccessGreen,
            "${streak.days} day${if (streak.days != 1) "s" else ""} under budget",
            "Keep it up — you're on track this month"
        )
        else -> Triple(
            WarningAmber,
            "No active streak",
            "Set a budget to start tracking"
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(accentColor.copy(alpha = 0.10f))
            .padding(16.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Big streak number
        Box(
            modifier         = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accentColor.copy(alpha = 0.20f)),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState  = streak.days,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                label        = "streak-count"
            ) { days ->
                Text(
                    text  = if (days > 0) "$days" else "-",
                    style = MaterialTheme.typography.titleLarge,
                    color = accentColor
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text  = title,
                style = MaterialTheme.typography.titleSmall,
                color = accentColor
            )
            Text(
                text  = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
            Text(
                text  = YearMonth.now().month.name.lowercase()
                    .replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
            )
        }
    }
}