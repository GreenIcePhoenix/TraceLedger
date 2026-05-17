package com.greenicephoenix.traceledger.feature.statistics.model

import java.time.LocalDate

/**
 * Shared data models for the statistics chart system.
 * These are pure data — no rendering logic, no Compose imports.
 * ViewModel produces these; composables consume them.
 */

/** Generic (x, y) point for line/area/sparkline charts. x = epoch day, y = amount. */
data class ChartPoint(
    val x: Long,
    val y: Double
)

/** One day in a spending heatmap calendar grid. */
data class CalendarDay(
    val date: LocalDate,
    val totalExpense: Double,
    /** 0.0–1.0 normalised against the month's max-spend day. */
    val intensity: Float
)

/** Avg spend per day-of-week, used for WeekdayPattern chart. */
data class WeekdayPattern(
    val dayOfWeek: Int,   // 1 = Monday … 7 = Sunday
    val hourOfDay: Int,   // 0–23 (reserved for future time-of-day chart)
    val totalAmount: Double,
    val count: Int
)