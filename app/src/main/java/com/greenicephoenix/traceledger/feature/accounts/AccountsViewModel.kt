package com.greenicephoenix.traceledger.feature.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenicephoenix.traceledger.core.repository.AccountRepository
import com.greenicephoenix.traceledger.domain.model.AccountUiModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AccountsViewModel(
    private val accountRepository: AccountRepository
) : ViewModel() {

    val accounts: StateFlow<List<AccountUiModel>> =
        accountRepository.observeAccounts()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Null = no error. Non-null = message to show in an error dialog.
    private val _deleteError = MutableStateFlow<String?>(null)
    val deleteError: StateFlow<String?> = _deleteError.asStateFlow()

    fun clearDeleteError() { _deleteError.value = null }

    fun saveAccount(account: AccountUiModel) {
        viewModelScope.launch { accountRepository.upsert(account) }
    }

    fun deleteAccount(accountId: String) {
        viewModelScope.launch {
            accountRepository.delete(accountId)
                .onFailure { e -> _deleteError.value = e.message }
        }
    }
}