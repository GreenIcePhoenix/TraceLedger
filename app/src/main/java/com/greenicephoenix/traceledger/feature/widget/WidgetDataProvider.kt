package com.greenicephoenix.traceledger.feature.widget

import android.content.Context
import com.greenicephoenix.traceledger.TraceLedgerApp
import com.greenicephoenix.traceledger.core.currency.CurrencyFormatter
import com.greenicephoenix.traceledger.core.currency.CurrencyManager
import com.greenicephoenix.traceledger.domain.model.TransactionType
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Holds all the pre-formatted strings the widget composable needs to render.
 * Everything is a String because Glance Text composables only take strings.
 */
data class WidgetData(
    val totalBalance      : String,   // e.g. "₹1,23,456"
    val monthlyIncome     : String,   // e.g. "₹45,000"
    val monthlyExpense    : String,   // e.g. "₹32,000"
    val monthlyNet        : String,   // e.g. "+₹13,000" or "-₹5,000"
    val isNetPositive     : Boolean,  // drives green vs red colour
    val monthLabel        : String,   // e.g. "May 2026"
    val isBalancePositive : Boolean   // drives white vs red colour on balance
)

object WidgetDataProvider {

    /**
     * Loads live data from the database and formats it for the widget.
     *
     * Why we use .first() on each Flow:
     *   Flow.first() is a suspend function that waits for one emission and returns it.
     *   We don't need to observe continuously here — we just need a snapshot of
     *   current data. This is called from provideGlance() which already runs in
     *   a coroutine, so suspending is perfectly safe.
     *
     * Why we init CurrencyManager here:
     *   CurrencyManager.init() is normally called from MainActivity, but the widget
     *   can be updated by the OS (every 30 min) without MainActivity ever opening.
     *   init() is idempotent (it checks if already initialised and returns early),
     *   so calling it here is always safe.
     */
    suspend fun load(context: Context): WidgetData {

        // Ensure currency preference is loaded from DataStore.
        // Safe to call even if MainActivity already called it — no side effects.
        CurrencyManager.init(context)

        // ── Get AppContainer (our manual DI) ──────────────────────────────────
        val container = (context.applicationContext as TraceLedgerApp).container
        val currency  = CurrencyManager.currency.value

        // ── Total balance across all accounts ─────────────────────────────────
        // observeAccounts() returns Flow<List<AccountUiModel>>.
        // We filter by includeInTotal — some accounts (e.g. a credit card the user
        // uses as a liability) may be excluded from the total by user preference.
        val accounts = container.accountRepository.observeAccounts().first()
        val totalBalance: BigDecimal = accounts
            .filter { it.includeInTotal }
            .fold(BigDecimal.ZERO) { acc, account -> acc + account.balance }

        // ── Monthly income and expense ────────────────────────────────────────
        // TransactionRepository has no pre-built aggregate functions for income/expense.
        // We load all transactions for the current month and calculate ourselves.
        // This mirrors how DashboardViewModel computes its totals.
        val currentMonth = YearMonth.now()
        val transactions = container.transactionRepository
            .observeTransactionsForMonth(currentMonth)
            .first()

        // Sum all INCOME transaction amounts for this month
        val monthlyIncome: BigDecimal = transactions
            .filter { it.type == TransactionType.INCOME }
            .fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }

        // Sum all EXPENSE transaction amounts for this month
        val monthlyExpense: BigDecimal = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }

        val net: BigDecimal = monthlyIncome - monthlyExpense

        // ── Format amounts as strings ─────────────────────────────────────────
        // CurrencyFormatter.format() expects a String, not BigDecimal.
        // toPlainString() gives us "123456.00" with no scientific notation.
        // The formatter handles grouping (Indian/International) and the symbol.
        val formattedBalance = CurrencyFormatter.format(totalBalance.toPlainString(), currency)
        val formattedIncome  = CurrencyFormatter.format(monthlyIncome.toPlainString(), currency)
        val formattedExpense = CurrencyFormatter.format(monthlyExpense.toPlainString(), currency)

        // Net: prefix with "+" when positive (CurrencyFormatter already adds "-" for negatives)
        val formattedNet = if (net >= BigDecimal.ZERO) {
            "+" + CurrencyFormatter.format(net.toPlainString(), currency)
        } else {
            // net is negative — toPlainString() gives "-5000.00"
            // CurrencyFormatter sees the minus sign and formats as "-₹5,000"
            CurrencyFormatter.format(net.toPlainString(), currency)
        }

        // ── Month label ───────────────────────────────────────────────────────
        // e.g. "May 2026"
        val monthLabel = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}"

        return WidgetData(
            totalBalance      = formattedBalance,
            monthlyIncome     = formattedIncome,
            monthlyExpense    = formattedExpense,
            monthlyNet        = formattedNet,
            isNetPositive     = net >= BigDecimal.ZERO,
            monthLabel        = monthLabel,
            isBalancePositive = totalBalance >= BigDecimal.ZERO
        )
    }

    /**
     * Safe fallback used when load() throws (e.g. very first launch before the
     * database has been created, or an unexpected Room error).
     * The widget will display dashes rather than crash.
     */
    fun fallback(): WidgetData {
        val now = YearMonth.now()
        return WidgetData(
            totalBalance      = "—",
            monthlyIncome     = "—",
            monthlyExpense    = "—",
            monthlyNet        = "—",
            isNetPositive     = true,
            monthLabel        = "${now.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${now.year}",
            isBalancePositive = true
        )
    }
}