package com.greenicephoenix.traceledger.feature.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.greenicephoenix.traceledger.core.repository.RecurringTransactionRepository

class AddEditRecurringViewModelFactory(
    private val repository: RecurringTransactionRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AddEditRecurringViewModel(repository) as T
    }
}