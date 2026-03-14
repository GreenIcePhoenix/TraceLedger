package com.greenicephoenix.traceledger.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.greenicephoenix.traceledger.core.repository.TransactionRepository
import com.greenicephoenix.traceledger.domain.model.TransactionType
import com.greenicephoenix.traceledger.domain.model.TransactionUiModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.math.BigDecimal
import java.time.YearMonth

// ─────────────────────────────────────────────────────────────────────────────
// DashboardViewModel
//
// NEW in Phase 2.
//
// Previously the Dashboard borrowed StatisticsViewModel for monthly figures.
// That was an architecture violation — StatisticsViewModel owns the Statistics
// screen's selected month, and if the user changed months in Statistics and
// came back to Dashboard, the Dashboard would show the wrong month's numbers.
//
// DashboardViewModel always shows the CURRENT month and owns its own
// transaction stream independently of Statistics.
//
// Responsibilities:
//   - Current month income / expense / net
//   - Last 5 transactions for the "Recent" section on Dashboard
//   - Nothing else — keep this ViewModel minimal
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    // Always the current calendar month — Dashboard never navigates months
    private val currentMonth = YearMonth.now()

    // Month-filtered stream — only loads current month's rows from DB
    private val currentMonthTransactions: Flow<List<TransactionUiModel>> =
        transactionRepository.observeTransactionsForMonth(currentMonth)

    // ── Monthly aggregates ────────────────────────────────────────────────────

    val monthlyIncome: StateFlow<BigDecimal> =
        currentMonthTransactions
            .map { txs ->
                txs.filter { it.type == TransactionType.INCOME }
                    .fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BigDecimal.ZERO)

    val monthlyExpense: StateFlow<BigDecimal> =
        currentMonthTransactions
            .map { txs ->
                txs.filter { it.type == TransactionType.EXPENSE }
                    .fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BigDecimal.ZERO)

    val monthlyNet: StateFlow<BigDecimal> =
        combine(monthlyIncome, monthlyExpense) { income, expense ->
            income - expense
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BigDecimal.ZERO)

    // ── Recent transactions (last 5 across all time, not just this month) ─────
    // We use observeTransactions() here (not month-filtered) because "recent"
    // means the 5 most recent regardless of month — e.g. if today is the 1st,
    // the most recent transactions are from last month.
    val recentTransactions: StateFlow<List<TransactionUiModel>> =
        transactionRepository.observeTransactions()
            .map { txs ->
                txs.sortedByDescending { it.date }
                    .take(5)
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

// ─────────────────────────────────────────────────────────────────────────────
// DashboardViewModelFactory
// ─────────────────────────────────────────────────────────────────────────────
class DashboardViewModelFactory(
    private val transactionRepository: TransactionRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == DashboardViewModel::class.java)
        return DashboardViewModel(transactionRepository) as T
    }
}