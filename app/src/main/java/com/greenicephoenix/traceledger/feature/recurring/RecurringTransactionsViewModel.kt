package com.greenicephoenix.traceledger.feature.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenicephoenix.traceledger.core.database.entity.RecurringTransactionEntity
import com.greenicephoenix.traceledger.core.repository.RecurringTransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecurringTransactionsViewModel(
    private val repository: RecurringTransactionRepository
) : ViewModel() {

    val recurringTransactions =
        repository.getAllRecurring()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    fun delete(recurring: RecurringTransactionEntity) {
        viewModelScope.launch {
            repository.delete(recurring)
        }
    }
}