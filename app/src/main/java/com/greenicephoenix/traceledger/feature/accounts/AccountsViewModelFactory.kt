package com.greenicephoenix.traceledger.feature.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.greenicephoenix.traceledger.core.repository.AccountRepository

// ─────────────────────────────────────────────────────────────────────────────
// AccountsViewModelFactory
//
// NEW FILE — required because AccountsViewModel now takes constructor parameters.
//
// All ViewModels in TraceLedger that need repositories must use a factory.
// This is consistent with:
//   - AddTransactionViewModelFactory
//   - BudgetsViewModelFactory
//   - StatisticsViewModelFactory
//   - RecurringTransactionsViewModelFactory
//   - CategoriesViewModelFactory
//
// The factory is registered in AppContainer and injected via NavGraph.
// ─────────────────────────────────────────────────────────────────────────────
class AccountsViewModelFactory(
    private val accountRepository: AccountRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == AccountsViewModel::class.java) {
            "AccountsViewModelFactory can only create AccountsViewModel"
        }
        return AccountsViewModel(accountRepository) as T
    }
}