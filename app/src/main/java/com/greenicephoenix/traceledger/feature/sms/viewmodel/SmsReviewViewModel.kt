package com.greenicephoenix.traceledger.feature.sms.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenicephoenix.traceledger.core.database.entity.SmsPendingTransactionEntity
import com.greenicephoenix.traceledger.feature.sms.repository.SmsQueueRepository
import com.greenicephoenix.traceledger.feature.sms.store.SmsLearningStore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SmsReviewViewModel(
    private val repository: SmsQueueRepository,
    private val learningStore: SmsLearningStore,
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val pendingItems: StateFlow<List<SmsPendingTransactionEntity>> =
        repository.observePending()
            .onEach { _isLoading.value = false }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _lastSavedDescription = MutableStateFlow<String?>(null)
    val lastSavedDescription: StateFlow<String?> = _lastSavedDescription.asStateFlow()

    // ── Actions ───────────────────────────────────────────────────────────────

    fun acceptTransaction(
        item: SmsPendingTransactionEntity,
        accountId: String,
        categoryId: String?,
    ) {

        if (accountId.isBlank()) return
        if (categoryId == null) return

        viewModelScope.launch {
            // ── Issue 5: App learns from user's correction ────────────────────
            // If user chose a DIFFERENT account than suggested → remember for this sender
            if (accountId != item.suggestedAccountId) {
                learningStore.learnAccountForSender(item.sender, accountId)
            }
            // If user chose a DIFFERENT category than suggested → remember for this description
            categoryId?.let { catId ->
                if (catId != item.suggestedCategoryId) {
                    learningStore.learnCategoryForDescription(item.parsedDescription, catId)
                }
            }

            repository.acceptTransaction(
                pendingId   = item.id,
                accountId   = accountId,
                categoryId  = categoryId,
                amount      = item.parsedAmount,
                description = item.parsedDescription,
                dateMsEpoch = item.parsedDate,
                type        = item.parsedType,
            )
            _lastSavedDescription.value = "Transaction saved"
        }
    }

    fun rejectTransaction(item: SmsPendingTransactionEntity) {
        viewModelScope.launch { repository.rejectTransaction(item.id) }
    }

    fun rejectAll() {
        viewModelScope.launch { repository.rejectAll() }
    }

    fun clearSavedMessage() {
        _lastSavedDescription.value = null
    }
}