package com.greenicephoenix.traceledger.core.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Maps the icon name string from changelog.txt entries to a Material ImageVector.
 * Fallback is Info if the name isn't recognised.
 *
 * To add a new icon: add an entry here and use the same name in changelog.txt.
 */
object ChangelogIconMapper {
    fun get(name: String): ImageVector = when (name) {
        "AccountBalance"  -> Icons.Default.AccountBalance
        "TrendingUp"      -> Icons.AutoMirrored.Filled.TrendingUp
        "PieChart"        -> Icons.Default.PieChart
        "BarChart"        -> Icons.Default.BarChart
        "Savings"         -> Icons.Default.Savings
        "Repeat"          -> Icons.Default.Repeat
        "Lightbulb"       -> Icons.Default.Lightbulb
        "ImportExport"    -> Icons.Default.ImportExport
        "Lock"            -> Icons.Default.Lock
        "Category"        -> Icons.Default.Category
        "Receipt"         -> Icons.Default.Receipt
        "CreditCard"      -> Icons.Default.CreditCard
        "BugReport"       -> Icons.Default.BugReport
        "Speed"           -> Icons.Default.Speed
        "Palette"         -> Icons.Default.Palette
        "Star"            -> Icons.Default.Star
        else              -> Icons.Default.Info
    }
}