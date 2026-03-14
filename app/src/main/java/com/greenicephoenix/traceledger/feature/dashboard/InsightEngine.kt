package com.greenicephoenix.traceledger.feature.dashboard

import java.math.BigDecimal
import java.math.RoundingMode

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
}