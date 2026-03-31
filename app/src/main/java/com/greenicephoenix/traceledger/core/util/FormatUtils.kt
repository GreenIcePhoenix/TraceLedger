package com.greenicephoenix.traceledger.core.util

import java.math.BigDecimal

/**
 * Formats a float value into a compact human-readable magnitude string.
 * Used by chart axes to avoid long numbers cluttering the display.
 *
 * Examples:
 *   999      → "999"
 *   1500     → "1.5K"
 *   1000000  → "1.0M"
 */
fun formatCompactMagnitude(value: Float): String {
    return when {
        value >= 1_000_000f -> "%.1fM".format(value / 1_000_000f)
        value >= 1_000f     -> "%.1fK".format(value / 1_000f)
        else                -> "%.0f".format(value)
    }
}

/**
 * Overload for BigDecimal — converts to Float and delegates.
 */
fun formatCompactMagnitude(value: BigDecimal): String =
    formatCompactMagnitude(value.toFloat())