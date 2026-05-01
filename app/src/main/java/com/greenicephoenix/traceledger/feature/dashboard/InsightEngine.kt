package com.greenicephoenix.traceledger.feature.dashboard

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

/**
 * Stateless engine that computes human-readable financial insights
 * from raw aggregated data. No Android dependencies, no coroutines.
 * Every function is a pure transformation of its inputs.
 */
object InsightEngine {

    /**
     * Describes how spending changed versus the previous month.
     * Returns null when there is no previous month data to compare.
     *
     * Examples:
     *   "You spent 23% more than last month"
     *   "You spent 15% less than last month"
     *   "Spending unchanged from last month"
     */
    fun spendingChangeInsight(
        thisMonthExpense: BigDecimal,
        lastMonthExpense: BigDecimal
    ): String? {
        if (lastMonthExpense == BigDecimal.ZERO) return null
        if (thisMonthExpense == BigDecimal.ZERO && lastMonthExpense == BigDecimal.ZERO) return null

        val change = thisMonthExpense.subtract(lastMonthExpense)
        val pct = change
            .multiply(BigDecimal(100))
            .divide(lastMonthExpense, 0, RoundingMode.HALF_UP)
            .toInt()

        return when {
            pct > 0  -> "You spent $pct% more than last month"
            pct < 0  -> "You spent ${-pct}% less than last month"
            else     -> "Spending unchanged from last month"
        }
    }

    /**
     * Monthly savings summary line.
     * Returns null when income is zero (nothing to summarise).
     *
     * Examples:
     *   "You saved ₹15,500 — 31% of income"
     *   "You overspent ₹3,200 this month"
     */
    fun savingsSummary(
        income: BigDecimal,
        expense: BigDecimal
    ): String? {
        if (income == BigDecimal.ZERO) return null

        val net = income.subtract(expense)

        return if (net >= BigDecimal.ZERO) {
            val savingsRate = net
                .multiply(BigDecimal(100))
                .divide(income, 0, RoundingMode.HALF_UP)
                .toInt()
            "Saved $savingsRate% of income this month"
        } else {
            "Overspent this month"
        }
    }

    /**
     * Describes the direction of net worth based on total balance
     * compared to last month's balance estimate.
     *
     * lastMonthNet = this month's net (income − expense used as a proxy for
     * balance movement, since we don't store historical balances).
     */
    fun netWorthTrend(monthlyNet: BigDecimal): String? {
        return when {
            monthlyNet > BigDecimal.ZERO -> "Balance trending up this month"
            monthlyNet < BigDecimal.ZERO -> "Balance trending down this month"
            else                         -> null
        }
    }

    /**
     * Estimates the monthly cost of active recurring expenses.
     * DAILY and WEEKLY frequencies are normalised to a monthly equivalent.
     * Returns null when there are no active recurring expenses.
     */
    fun recurringMonthlyCost(
        activeRecurringExpenses: List<RecurringCostItem>
    ): BigDecimal? {
        if (activeRecurringExpenses.isEmpty()) return null

        val total = activeRecurringExpenses.fold(BigDecimal.ZERO) { acc, item ->
            acc + item.monthlyEquivalent()
        }

        return if (total > BigDecimal.ZERO) total else null
    }

    data class RecurringCostItem(
        val amount: BigDecimal,
        val frequency: String
    ) {
        fun monthlyEquivalent(): BigDecimal = when (frequency) {
            "DAILY"       -> amount.multiply(BigDecimal("30.44"))
            "WEEKLY"      -> amount.multiply(BigDecimal("4.33"))
            "MONTHLY"     -> amount
            "QUARTERLY"   -> amount.divide(BigDecimal("3"), 2, RoundingMode.HALF_UP)
            "HALF_YEARLY" -> amount.divide(BigDecimal("6"), 2, RoundingMode.HALF_UP)
            "YEARLY"      -> amount.divide(BigDecimal("12"), 2, RoundingMode.HALF_UP)
            else          -> amount
        }
    }

    // ─── Spending Forecast ────────────────────────────────────────────────────────

    /**
     * Holds the result of a spending forecast calculation.
     *
     * @param forecastTotal      Projected total spend for the full month (BigDecimal)
     * @param dailyAverage       Average spend per day so far this month
     * @param remainingForecast  Projected spend for the remaining days of the month
     * @param isHighSpend        True if forecast exceeds last month's total spend
     */
    data class SpendingForecast(
        val forecastTotal: BigDecimal,
        val dailyAverage: BigDecimal,
        val remainingForecast: BigDecimal,
        val isHighSpend: Boolean
    )

    /**
     * Computes a spending forecast for the current month.
     *
     * Only returns a result between day 3 and day 25 — outside this window
     * the forecast is either too early to be meaningful or too close to month
     * end to be useful.
     *
     * @param totalSpentSoFar   Sum of all expenses recorded so far this month
     * @param lastMonthTotal    Total expenses from the previous month (used for isHighSpend)
     * @param today             The current date (injected so this stays unit-testable)
     */
    fun spendingForecast(
        totalSpentSoFar: BigDecimal,
        lastMonthTotal: BigDecimal,
        today: LocalDate = LocalDate.now()
    ): SpendingForecast? {
        val dayOfMonth = today.dayOfMonth
        val daysInMonth = today.lengthOfMonth()

        // Only show forecast between day 3 and day 25
        if (dayOfMonth !in 3..25) return null
        // Avoid division by zero
        if (totalSpentSoFar <= BigDecimal.ZERO) return null

        val dailyAverage = totalSpentSoFar.divide(
            BigDecimal(dayOfMonth),
            2,
            RoundingMode.HALF_UP
        )
        val remainingDays = BigDecimal(daysInMonth - dayOfMonth)
        val remainingForecast = dailyAverage.multiply(remainingDays)
        val forecastTotal = totalSpentSoFar.add(remainingForecast)

        // Flag as high spend if forecast exceeds last month's total
        val isHighSpend = lastMonthTotal > BigDecimal.ZERO &&
                forecastTotal > lastMonthTotal

        return SpendingForecast(
            forecastTotal = forecastTotal,
            dailyAverage = dailyAverage,
            remainingForecast = remainingForecast,
            isHighSpend = isHighSpend
        )
    }
}