package com.greenicephoenix.traceledger.feature.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.greenicephoenix.traceledger.core.repository.TransactionRepository
import com.greenicephoenix.traceledger.feature.budgets.data.BudgetRepository

class StatisticsViewModelFactory(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository:      BudgetRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatisticsViewModel::class.java)) {
            return StatisticsViewModel(transactionRepository, budgetRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}