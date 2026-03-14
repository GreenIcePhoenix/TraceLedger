package com.greenicephoenix.traceledger.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenicephoenix.traceledger.domain.model.AccountUiModel
import com.greenicephoenix.traceledger.domain.model.CategoryUiModel
import com.greenicephoenix.traceledger.domain.model.TransactionType
import com.greenicephoenix.traceledger.domain.model.TransactionUiModel
import com.greenicephoenix.traceledger.core.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

// ─────────────────────────────────────────────────────────────────────────────
// DayGroup
//
// Represents one date section in the transaction history list.
// The UI renders a date header followed by all transactions on that day.
//
// Example:
//   DayGroup(date = 2026-03-14, transactions = [Coffee, Lunch, Uber])
//   DayGroup(date = 2026-03-13, transactions = [Groceries, Netflix])
// ─────────────────────────────────────────────────────────────────────────────
data class DayGroup(
    val date: LocalDate,
    val transactions: List<TransactionUiModel>
)

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModel(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    // ── Selected month ────────────────────────────────────────────────────────
    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    // ── Search + filter ───────────────────────────────────────────────────────
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _typeFilter = MutableStateFlow<TransactionType?>(null) // null = ALL
    val typeFilter: StateFlow<TransactionType?> = _typeFilter.asStateFlow()

    // ── Reference data for search (account/category name lookup) ─────────────
    // These are set from the UI via setAccounts() / setCategories() because
    // the ViewModel doesn't own the account/category repositories directly.
    private val accountNameMap = MutableStateFlow<Map<String, String>>(emptyMap())
    private val categoryNameMap = MutableStateFlow<Map<String, String>>(emptyMap())

    private val referenceData = combine(
        accountNameMap,
        categoryNameMap
    ) { accounts, categories ->
        accounts to categories
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyMap<String, String>() to emptyMap()
    )

    // ── FIX #14: Month-filtered transaction stream ────────────────────────────
    // Previously: observeTransactions() fetched ALL transactions (full table),
    // then filtered in memory. This caused the entire history to load whenever
    // any transaction changed — O(n) work for every DB update.
    //
    // Now: flatMapLatest switches to a new flow each time the month changes,
    // fetching only that month's rows from the DB. This is far more efficient
    // for users with years of data.
    private val monthlyTransactions: Flow<List<TransactionUiModel>> =
        _selectedMonth.flatMapLatest { month ->
            transactionRepository.observeTransactionsForMonth(month)
        }

    // ── Filtered + sorted flat list (used for totals) ─────────────────────────
    private val filteredTransactions: StateFlow<List<TransactionUiModel>> =
        combine(
            monthlyTransactions,
            _typeFilter,
            _searchQuery,
            referenceData
        ) { transactions, typeFilter, query, ref ->
            val (accounts, categories) = ref
            val q = query.trim().lowercase()

            transactions
                .asSequence()
                .filter { typeFilter == null || it.type == typeFilter }
                .filter { tx ->
                    if (q.isBlank()) return@filter true

                    val accountMatch =
                        tx.fromAccountId?.let { accounts[it] }?.contains(q) == true ||
                                tx.toAccountId?.let { accounts[it] }?.contains(q) == true

                    val categoryMatch =
                        tx.categoryId?.let { categories[it] }?.contains(q) == true

                    val amountMatch  = tx.amount.toPlainString().contains(q)
                    val notesMatch   = tx.note?.lowercase()?.contains(q) == true
                    val dateMatch    = tx.date.toString().contains(q)
                    val typeMatch    = tx.type.name.lowercase().contains(q)

                    accountMatch || categoryMatch || amountMatch ||
                            notesMatch   || dateMatch     || typeMatch
                }
                .sortedByDescending { it.date }
                .toList()
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    // ── FIX #12: Grouped by date for the History UI ───────────────────────────
    // The UI observes this instead of the flat list.
    // Groups are sorted newest-date-first.
    // Within each group, transactions are sorted by createdAt descending
    // so the most recently entered one appears first if two share the same date.
    val groupedTransactions: StateFlow<List<DayGroup>> =
        filteredTransactions
            .map { transactions ->
                transactions
                    .groupBy { it.date }
                    .entries
                    .sortedByDescending { it.key }   // newest date first
                    .map { (date, txs) ->
                        DayGroup(
                            date = date,
                            transactions = txs.sortedByDescending { it.createdAt }
                        )
                    }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    // ── Monthly totals ────────────────────────────────────────────────────────
    val totalIn: StateFlow<BigDecimal> =
        filteredTransactions
            .map { list ->
                list.filter { it.type == TransactionType.INCOME }
                    .fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BigDecimal.ZERO)

    val totalOut: StateFlow<BigDecimal> =
        filteredTransactions
            .map { list ->
                list.filter { it.type == TransactionType.EXPENSE }
                    .fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BigDecimal.ZERO)

    // ── Actions ───────────────────────────────────────────────────────────────

    fun goToPreviousMonth() { _selectedMonth.value = _selectedMonth.value.minusMonths(1) }
    fun goToNextMonth()     { _selectedMonth.value = _selectedMonth.value.plusMonths(1)  }
    fun selectMonth(month: YearMonth) { _selectedMonth.value = month }

    fun updateSearch(query: String)          { _searchQuery.value = query }
    fun updateTypeFilter(type: TransactionType?) { _typeFilter.value = type }

    fun setAccounts(accounts: List<AccountUiModel>) {
        accountNameMap.value = accounts.associate { it.id to it.name.lowercase() }
    }

    fun setCategories(categories: List<CategoryUiModel>) {
        categoryNameMap.value = categories.associate { it.id to it.name.lowercase() }
    }
}