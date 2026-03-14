package com.greenicephoenix.traceledger.feature.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenicephoenix.traceledger.core.repository.TransactionRepository
import com.greenicephoenix.traceledger.domain.model.TransactionType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth

class StatisticsViewModel(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    data class CategorySlice(
        val categoryId: String,
        val amount: BigDecimal,
        val percentage: Float
    )

    data class CashflowEntry(
        val day: Int,
        val income: BigDecimal,
        val expense: BigDecimal
    )

    data class CategoryMonthlyTrend(
        val categoryId: String,
        val month: YearMonth,
        val total: BigDecimal
    )

    // ── Selected month ────────────────────────────────────────────────────────

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    fun previousMonth() { _selectedMonth.value = _selectedMonth.value.minusMonths(1) }
    fun nextMonth()     { _selectedMonth.value = _selectedMonth.value.plusMonths(1)  }
    fun selectMonth(month: YearMonth) { _selectedMonth.value = month }

    // ── Month streams ─────────────────────────────────────────────────────────

    @OptIn(ExperimentalCoroutinesApi::class)
    private val monthlyTransactions = _selectedMonth.flatMapLatest { month ->
        transactionRepository.observeTransactionsForMonth(month)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val previousMonthTransactions = _selectedMonth.flatMapLatest { month ->
        transactionRepository.observeTransactionsForMonth(month.minusMonths(1))
    }

    private val analyticsTransactions = monthlyTransactions.map { list ->
        list.filter { it.type == TransactionType.EXPENSE || it.type == TransactionType.INCOME }
    }

    private val previousAnalyticsTransactions = previousMonthTransactions.map { list ->
        list.filter { it.type == TransactionType.EXPENSE || it.type == TransactionType.INCOME }
    }

    // ── Current month aggregates ──────────────────────────────────────────────

    val totalIncome: StateFlow<BigDecimal> =
        analyticsTransactions.map { list ->
            list.filter { it.type == TransactionType.INCOME }
                .fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BigDecimal.ZERO)

    val totalExpense: StateFlow<BigDecimal> =
        analyticsTransactions.map { list ->
            list.filter { it.type == TransactionType.EXPENSE }
                .fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BigDecimal.ZERO)

    val netAmount: StateFlow<BigDecimal> =
        combine(totalIncome, totalExpense) { income, expense ->
            income.subtract(expense)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BigDecimal.ZERO)

    // ── Previous month aggregates (for comparison arrows) ────────────────────

    val prevMonthIncome: StateFlow<BigDecimal> =
        previousAnalyticsTransactions.map { list ->
            list.filter { it.type == TransactionType.INCOME }
                .fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BigDecimal.ZERO)

    val prevMonthExpense: StateFlow<BigDecimal> =
        previousAnalyticsTransactions.map { list ->
            list.filter { it.type == TransactionType.EXPENSE }
                .fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BigDecimal.ZERO)

    // ── Category breakdowns ───────────────────────────────────────────────────

    val expenseByCategory: StateFlow<Map<String, BigDecimal>> =
        analyticsTransactions.map { txs ->
            txs.filter { it.type == TransactionType.EXPENSE && it.categoryId != null }
                .groupBy { it.categoryId!! }
                .mapValues { (_, list) -> list.fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount } }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val incomeByCategory: StateFlow<Map<String, BigDecimal>> =
        analyticsTransactions.map { txs ->
            txs.filter { it.type == TransactionType.INCOME && it.categoryId != null }
                .groupBy { it.categoryId!! }
                .mapValues { (_, list) -> list.fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount } }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val expenseCategorySlices: StateFlow<List<CategorySlice>> =
        expenseByCategory.map { buildCategorySlices(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val incomeCategorySlices: StateFlow<List<CategorySlice>> =
        incomeByCategory.map { buildCategorySlices(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun buildCategorySlices(totals: Map<String, BigDecimal>): List<CategorySlice> {
        if (totals.isEmpty()) return emptyList()
        val totalAmount = totals.values.fold(BigDecimal.ZERO) { acc, v -> acc + v }
        if (totalAmount == BigDecimal.ZERO) return emptyList()
        return totals.map { (categoryId, amount) ->
            CategorySlice(
                categoryId = categoryId,
                amount     = amount,
                percentage = amount.multiply(BigDecimal(100))
                    .divide(totalAmount, 4, RoundingMode.HALF_UP)
                    .toFloat()
            )
        }.sortedByDescending { it.amount }
    }

    // ── Cashflow by day ───────────────────────────────────────────────────────

    val cashflowByDay: StateFlow<List<CashflowEntry>> =
        analyticsTransactions.map { transactions ->
            transactions.groupBy { it.date.dayOfMonth }
                .map { (day, dayTxs) ->
                    CashflowEntry(
                        day     = day,
                        income  = dayTxs.filter { it.type == TransactionType.INCOME }.fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount },
                        expense = dayTxs.filter { it.type == TransactionType.EXPENSE }.fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }
                    )
                }.sortedBy { it.day }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Category expense trends across all months ─────────────────────────────

    val categoryExpenseTrends: StateFlow<List<CategoryMonthlyTrend>> =
        transactionRepository.observeTransactions().map { transactions ->
            transactions.asSequence()
                .filter { it.type == TransactionType.EXPENSE && it.categoryId != null }
                .groupBy { Pair(it.categoryId!!, YearMonth.from(it.date)) }
                .map { (key, list) ->
                    CategoryMonthlyTrend(
                        categoryId = key.first,
                        month      = key.second,
                        total      = list.fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }
                    )
                }
                .sortedWith(compareBy<CategoryMonthlyTrend> { it.categoryId }.thenBy { it.month })
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}