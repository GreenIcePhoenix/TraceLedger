package com.greenicephoenix.traceledger.feature.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenicephoenix.traceledger.core.repository.TransactionRepository
import com.greenicephoenix.traceledger.domain.model.TransactionType
import com.greenicephoenix.traceledger.feature.budgets.data.BudgetRepository
import com.greenicephoenix.traceledger.feature.budgets.domain.BudgetSignal
import com.greenicephoenix.traceledger.feature.budgets.domain.BudgetState
import com.greenicephoenix.traceledger.feature.budgets.domain.BudgetStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetsViewModel(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    fun selectMonth(month: YearMonth) { _selectedMonth.value = month }

    private val budgetsForMonth = _selectedMonth.flatMapLatest { month ->
        budgetRepository.observeBudgetsForMonth(month)
    }

    private val expenseTransactionsForMonth = combine(
        transactionRepository.observeTransactions(),
        _selectedMonth
    ) { transactions, month ->
        transactions.filter {
            it.type == TransactionType.EXPENSE && YearMonth.from(it.date) == month
        }
    }

    val budgetStatuses: StateFlow<List<BudgetStatus>> =
        combine(budgetsForMonth, expenseTransactionsForMonth) { budgets, transactions ->
            budgets.filter { it.isActive }.map { budget ->
                val used = transactions
                    .filter { it.categoryId == budget.categoryId }
                    .fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }

                val remaining = budget.limitAmount.subtract(used)
                val progress  = if (budget.limitAmount > BigDecimal.ZERO)
                    used.divide(budget.limitAmount, 4, RoundingMode.HALF_UP).toFloat()
                else 0f

                // Phase 2: thresholds updated — 75% = WARNING, 90% = EXCEEDED
                val state = when {
                    progress >= 0.90f -> BudgetState.EXCEEDED
                    progress >= 0.75f -> BudgetState.WARNING
                    else              -> BudgetState.SAFE
                }

                BudgetStatus(
                    budgetId   = budget.id,
                    categoryId = budget.categoryId,
                    month      = budget.month,
                    limit      = budget.limitAmount,
                    used       = used,
                    remaining  = remaining,
                    progress   = progress,
                    state      = state
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val budgetSignals: StateFlow<List<BudgetSignal>> =
        budgetStatuses.map { statuses ->
            statuses.mapNotNull { status ->
                when (status.state) {
                    BudgetState.WARNING  -> BudgetSignal.ApproachingLimit(
                        budgetId   = status.budgetId,
                        categoryId = status.categoryId,
                        month      = status.month,
                        progress   = status.progress
                    )
                    BudgetState.EXCEEDED -> BudgetSignal.Exceeded(
                        budgetId       = status.budgetId,
                        categoryId     = status.categoryId,
                        month          = status.month,
                        overspentAmount = status.used - status.limit
                    )
                    else -> null
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Dashboard banner signals ───────────────────────────────────────────────

    val hasExceededBudgets: StateFlow<Boolean> =
        budgetSignals.map { it.any { s -> s is BudgetSignal.Exceeded } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val exceededBudgetsCount: StateFlow<Int> =
        budgetSignals.map { it.count { s -> s is BudgetSignal.Exceeded } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    // Phase 2: new — count of budgets in WARNING state (75–89%)
    val warningBudgetsCount: StateFlow<Int> =
        budgetSignals.map { it.count { s -> s is BudgetSignal.ApproachingLimit } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val hasWarningBudgets: StateFlow<Boolean> =
        warningBudgetsCount.map { it > 0 }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun deleteBudget(budgetId: String) {
        viewModelScope.launch { budgetRepository.deleteBudget(budgetId) }
    }
}