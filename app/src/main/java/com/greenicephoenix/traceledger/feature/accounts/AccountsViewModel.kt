package com.greenicephoenix.traceledger.feature.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenicephoenix.traceledger.core.repository.AccountRepository
import com.greenicephoenix.traceledger.domain.model.AccountUiModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// AccountsViewModel
//
// FIX: Previously used AndroidViewModel and pulled repositories directly from
// the Application context inside the ViewModel:
//
//   class AccountsViewModel(application: Application) : AndroidViewModel(application) {
//       private val app = getApplication<TraceLedgerApp>()
//       private val accountRepository = app.container.accountRepository  // ← WRONG
//   }
//
// This violated the architecture rules: ViewModels must receive dependencies
// via constructor injection, not pull them from global state. The old approach:
//   - Is untestable (can't inject a mock repository)
//   - Couples the ViewModel to the Application class
//   - Is inconsistent with every other ViewModel in the project
//
// The fix: plain ViewModel + factory, same as AddTransactionViewModel,
// BudgetsViewModel, StatisticsViewModel, etc.
// ─────────────────────────────────────────────────────────────────────────────
class AccountsViewModel(
    private val accountRepository: AccountRepository
) : ViewModel() {

    // Reactive stream of all accounts. Updates automatically when DB changes.
    val accounts: StateFlow<List<AccountUiModel>> =
        accountRepository.observeAccounts()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    /** Create or update an account. Uses upsert so the same call handles both. */
    fun saveAccount(account: AccountUiModel) {
        viewModelScope.launch {
            accountRepository.upsert(account)
        }
    }

    /** Delete an account by ID. Room's RESTRICT foreign key will prevent
     *  deletion if transactions reference this account. Handle the error
     *  in the UI if deletion fails. */
    fun deleteAccount(accountId: String) {
        viewModelScope.launch {
            accountRepository.delete(accountId)
        }
    }
}