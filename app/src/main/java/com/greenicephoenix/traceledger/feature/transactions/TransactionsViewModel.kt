package com.greenicephoenix.traceledger.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenicephoenix.traceledger.core.repository.TransactionRepository
import com.greenicephoenix.traceledger.domain.model.AccountUiModel
import com.greenicephoenix.traceledger.domain.model.CategoryUiModel
import com.greenicephoenix.traceledger.domain.model.TransactionType
import com.greenicephoenix.traceledger.domain.model.TransactionUiModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

data class DayGroup(
    val date: LocalDate,
    val transactions: List<TransactionUiModel>
)

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModel(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _typeFilter = MutableStateFlow<TransactionType?>(null)
    val typeFilter: StateFlow<TransactionType?> = _typeFilter.asStateFlow()

    // Amount range filter — null means no limit applied
    private val _minAmount = MutableStateFlow<BigDecimal?>(null)
    val minAmount: StateFlow<BigDecimal?> = _minAmount.asStateFlow()

    private val _maxAmount = MutableStateFlow<BigDecimal?>(null)
    val maxAmount: StateFlow<BigDecimal?> = _maxAmount.asStateFlow()

    private val _categoryFilter = MutableStateFlow<String?>(null)
    val categoryFilter: StateFlow<String?> = _categoryFilter.asStateFlow()
    private val accountNameMap  = MutableStateFlow<Map<String, String>>(emptyMap())
    private val categoryNameMap = MutableStateFlow<Map<String, String>>(emptyMap())

    private val referenceData = combine(accountNameMap, categoryNameMap) { a, c -> a to c }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap<String, String>() to emptyMap())

    private val monthlyTransactions: Flow<List<TransactionUiModel>> =
        _selectedMonth.flatMapLatest { month ->
            transactionRepository.observeTransactionsForMonth(month)
        }

    private val filteredTransactions: StateFlow<List<TransactionUiModel>> =
        combine(
            monthlyTransactions,
            _typeFilter,
            _searchQuery,
            _minAmount,
            _maxAmount,
            referenceData,
            _categoryFilter          // ← NEW 7th stream
        ) { args ->
            @Suppress("UNCHECKED_CAST")
            val transactions    = args[0] as List<TransactionUiModel>
            val typeFilter      = args[1] as TransactionType?
            val query           = args[2] as String
            val minAmt          = args[3] as BigDecimal?
            val maxAmt          = args[4] as BigDecimal?
            val ref             = args[5] as Pair<Map<String, String>, Map<String, String>>
            val categoryFilter  = args[6] as String?           // ← NEW

            val (accounts, categories) = ref
            val q = query.trim().lowercase()

            transactions.asSequence()
                .filter { typeFilter == null || it.type == typeFilter }
                .filter { minAmt == null || it.amount >= minAmt }
                .filter { maxAmt == null || it.amount <= maxAmt }
                .filter { categoryFilter == null || it.categoryId == categoryFilter }  // ← NEW
                .filter { tx ->
                    if (q.isBlank()) return@filter true
                    val accountMatch  = tx.fromAccountId?.let { accounts[it] }?.contains(q) == true ||
                            tx.toAccountId?.let   { accounts[it] }?.contains(q) == true
                    val categoryMatch = tx.categoryId?.let { categories[it] }?.contains(q) == true
                    val amountMatch   = tx.amount.toPlainString().contains(q)
                    val notesMatch    = tx.note?.lowercase()?.contains(q) == true
                    val dateMatch     = tx.date.toString().contains(q)
                    val typeMatch     = tx.type.name.lowercase().contains(q)
                    accountMatch || categoryMatch || amountMatch || notesMatch || dateMatch || typeMatch
                }
                .sortedByDescending { it.date }
                .toList()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val groupedTransactions: StateFlow<List<DayGroup>> =
        filteredTransactions.map { transactions ->
            transactions.groupBy { it.date }
                .entries
                .sortedByDescending { it.key }
                .map { (date, txs) -> DayGroup(date, txs.sortedByDescending { it.createdAt }) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totalIn: StateFlow<BigDecimal> =
        filteredTransactions.map { list ->
            list.filter { it.type == TransactionType.INCOME }.fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BigDecimal.ZERO)

    val totalOut: StateFlow<BigDecimal> =
        filteredTransactions.map { list ->
            list.filter { it.type == TransactionType.EXPENSE }.fold(BigDecimal.ZERO) { acc, tx -> acc + tx.amount }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BigDecimal.ZERO)

    // Returns true when any non-text filter is active (used to show a filter badge)
    val hasActiveFilters: StateFlow<Boolean> =
        combine(_typeFilter, _minAmount, _maxAmount, _categoryFilter) { type, min, max, cat ->
            type != null || min != null || max != null || cat != null
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun deleteTransaction(transaction: TransactionUiModel) {
        viewModelScope.launch {
            transactionRepository.deleteTransactionWithBalance(transaction.id)
        }
    }

    fun goToPreviousMonth() { _selectedMonth.value = _selectedMonth.value.minusMonths(1) }
    fun goToNextMonth()     { _selectedMonth.value = _selectedMonth.value.plusMonths(1)  }
    fun selectMonth(month: YearMonth) { _selectedMonth.value = month }

    fun updateSearch(query: String)              { _searchQuery.value = query }
    fun updateTypeFilter(type: TransactionType?) { _typeFilter.value  = type  }
    fun updateMinAmount(amount: BigDecimal?)     { _minAmount.value   = amount }
    fun updateMaxAmount(amount: BigDecimal?)     { _maxAmount.value   = amount }
    fun clearAmountFilter()                      { _minAmount.value = null; _maxAmount.value = null }

    fun setAccounts(accounts: List<AccountUiModel>) {
        accountNameMap.value = accounts.associate { it.id to it.name.lowercase() }
    }
    fun setCategories(categories: List<CategoryUiModel>) {
        categoryNameMap.value = categories.associate { it.id to it.name.lowercase() }
    }

    fun setCategoryFilter(categoryId: String) { _categoryFilter.value = categoryId }
    fun clearCategoryFilter()                  { _categoryFilter.value = null }
}