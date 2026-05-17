package com.greenicephoenix.traceledger.feature.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.core.ui.theme.NothingRed
import com.greenicephoenix.traceledger.core.ui.theme.TraceLedgerThemeExtras

// ─────────────────────────────────────────────────────────────────────────────
// BudgetWarningBanner
//
// Phase 2 update: previously this only showed when budgets were exceeded (100%).
// Now it also shows a softer warning at 75% (WARNING state), using different
// colours and messaging for each threshold so users get early notice.
//
// Thresholds (defined in BudgetsViewModel):
//   SAFE      → no banner shown
//   WARNING   → 75–89% used  → amber/orange tone, "approaching limit"
//   EXCEEDED  → 90%+ used    → red tone, "exceeded"
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun BudgetWarningBanner(
    exceededCount: Int,
    warningCount: Int = 0,
    onClick: () -> Unit
) {
    // Determine the most severe state to display
    // If both exist, show the exceeded banner (more urgent)
    val isExceeded = exceededCount > 0
    val isWarning  = warningCount  > 0 && !isExceeded

    if (!isExceeded && !isWarning) return

//    val bannerColor = if (isExceeded)
//        NothingRed.copy(alpha = 0.12f)
//    else
//        androidx.compose.ui.graphics.Color(0xFFFFF3E0)

    val bannerColor = if (isExceeded)
        TraceLedgerThemeExtras.errorBanner
    else
        TraceLedgerThemeExtras.warningBanner

    val iconTint = if (isExceeded)
        NothingRed
    else
        androidx.compose.ui.graphics.Color(0xFFF57C00)  // Amber icon

    val message = when {
        isExceeded && exceededCount == 1 -> "You've exceeded 1 budget this month"
        isExceeded                        -> "You've exceeded $exceededCount budgets this month"
        isWarning  && warningCount  == 1  -> "1 budget is approaching its limit"
        else                              -> "$warningCount budgets are approaching their limits"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bannerColor, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector        = Icons.Default.Warning,
            contentDescription = null,
            tint               = iconTint,
            modifier           = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text  = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text  = "Tap to review",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}