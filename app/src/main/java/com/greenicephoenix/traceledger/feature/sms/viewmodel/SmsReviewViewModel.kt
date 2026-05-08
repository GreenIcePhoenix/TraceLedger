package com.greenicephoenix.traceledger.feature.sms.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenicephoenix.traceledger.core.database.dao.AccountDao
import com.greenicephoenix.traceledger.core.database.dao.CategoryDao
import com.greenicephoenix.traceledger.core.database.entity.SmsPendingTransactionEntity
import com.greenicephoenix.traceledger.domain.model.AccountUiModel
import com.greenicephoenix.traceledger.domain.model.CategoryUiModel
import com.greenicephoenix.traceledger.feature.sms.repository.SmsQueueRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SmsReviewUiState(
    val items: List<SmsPendingTransactionEntity> = emptyList(),
    val accounts: List<AccountUiModel> = emptyList(),
    val categories: List<CategoryUiModel> = emptyList(),
    val isLoading: Boolean = true,
    /** Set when the user saves a transaction — shows a brief snackbar */
    val lastSavedDescription: String? = null,
)

class SmsReviewViewModel(
    private val repository: SmsQueueRepository,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
) : ViewModel() {

    private val _state = MutableStateFlow(SmsReviewUiState())
    val state: StateFlow<SmsReviewUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // Combine pending items + accounts + categories into one state
            combine(
                repository.observePending(),
                accountDao.getAllAccountsAsFlow(),
                categoryDao.getAllCategoriesAsFlow(),
            ) { items, accounts, categories ->
                _state.update { current ->
                    current.copy(
                        items = items,
                        accounts = accounts.map { it.toUiModel() },
                        categories = categories.map { it.toUiModel() },
                        isLoading = false
                    )
                }
            }.collect()
        }
    }

    fun acceptTransaction(
        item: SmsPendingTransactionEntity,
        accountId: Long,
        categoryId: Long?,
        note: String = ""
    ) {
        viewModelScope.launch {
            repository.acceptTransaction(
                pendingId = item.id,
                accountId = accountId,
                categoryId = categoryId,
                amount = item.parsedAmount,
                description = item.parsedDescription,
                date = item.parsedDate,
                type = item.parsedType,
                note = note
            )
            _state.update { it.copy(lastSavedDescription = item.parsedDescription) }
        }
    }

    fun rejectTransaction(item: SmsPendingTransactionEntity) {
        viewModelScope.launch { repository.rejectTransaction(item.id) }
    }

    fun rejectAll() {
        viewModelScope.launch { repository.rejectAll() }
    }

    fun clearSavedMessage() {
        _state.update { it.copy(lastSavedDescription = null) }
    }
}