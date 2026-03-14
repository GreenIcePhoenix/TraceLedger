package com.greenicephoenix.traceledger.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.greenicephoenix.traceledger.core.repository.RecurringTransactionRepository
import com.greenicephoenix.traceledger.core.repository.TransactionRepository
import com.greenicephoenix.traceledger.domain.model.TransactionType
import com.greenicephoenix.traceledger.domain.model.TransactionUiModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.math.BigDecimal
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val transactionRepository: TransactionRepository,
    private val recurringRepository: RecurringTransactionRepository
) : ViewModel() {

    private val currentMonth  = YearMonth.now()
    private val previousMonth = currentMonth.minusMonths(1)

    private val currentMonthTransactions: Flow<List<TransactionUiModel>> =
        transactionRepository.observeTransactionsForMonth(currentMonth)

    private val previousMonthTransactions: Flow<List<TransactionUiModel>> =
        transactionRepository.observeTransactionsForMonth(previousMonth)

    // ── Current month aggregates ──────────────────────────────────────────────

    val monthlyIncome: StateFlow<BigDecimal> =
        currentMonthTransactions.map { txs ->
            txs.filter { it.type == TransactionType.INCOME }
                .fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BigDecimal.ZERO)

    val monthlyExpense: StateFlow<BigDecimal> =
        currentMonthTransactions.map { txs ->
            txs.filter { it.type == TransactionType.EXPENSE }
                .fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BigDecimal.ZERO)

    val monthlyNet: StateFlow<BigDecimal> =
        combine(monthlyIncome, monthlyExpense) { income, expense ->
            income - expense
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BigDecimal.ZERO)

    // ── Previous month expense (for comparison insight) ───────────────────────

    private val lastMonthExpense: StateFlow<BigDecimal> =
        previousMonthTransactions.map { txs ->
            txs.filter { it.type == TransactionType.EXPENSE }
                .fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BigDecimal.ZERO)

    // ── Recent transactions (last 5, not month-filtered) ─────────────────────

    val recentTransactions: StateFlow<List<TransactionUiModel>> =
        transactionRepository.observeTransactions()
            .map { txs -> txs.sortedByDescending { it.date }.take(5) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Recurring monthly cost ────────────────────────────────────────────────

    val recurringMonthlyCost: StateFlow<BigDecimal?> =
        recurringRepository.getAllRecurring()
            .map { rules ->
                val items = rules
                    .filter { it.isActive && it.type == TransactionType.EXPENSE.name }
                    .map { InsightEngine.RecurringCostItem(it.amount, it.frequency) }
                InsightEngine.recurringMonthlyCost(items)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // ── Derived insight strings ───────────────────────────────────────────────

    val spendingChangeInsight: StateFlow<String?> =
        combine(monthlyExpense, lastMonthExpense) { current, last ->
            InsightEngine.spendingChangeInsight(current, last)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val savingsSummary: StateFlow<String?> =
        combine(monthlyIncome, monthlyExpense) { income, expense ->
            InsightEngine.savingsSummary(income, expense)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val netWorthTrend: StateFlow<String?> =
        monthlyNet.map { net ->
            InsightEngine.netWorthTrend(net)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

class DashboardViewModelFactory(
    private val transactionRepository: TransactionRepository,
    private val recurringRepository: RecurringTransactionRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == DashboardViewModel::class.java)
        return DashboardViewModel(transactionRepository, recurringRepository) as T
    }
}