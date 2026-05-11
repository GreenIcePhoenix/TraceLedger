package com.greenicephoenix.traceledger.feature.sms.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenicephoenix.traceledger.core.database.entity.SmsCustomRuleEntity
import com.greenicephoenix.traceledger.feature.sms.repository.SmsRuleRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Drives the Custom Rules list screen.
 * Supports toggle-enabled and delete directly from the list.
 */
class CustomRulesViewModel(
    private val repository: SmsRuleRepository,
) : ViewModel() {

    val rules: StateFlow<List<SmsCustomRuleEntity>> =
        repository.observeRules()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleEnabled(rule: SmsCustomRuleEntity) {
        viewModelScope.launch {
            repository.updateRule(rule.copy(isEnabled = !rule.isEnabled))
        }
    }

    fun deleteRule(rule: SmsCustomRuleEntity) {
        viewModelScope.launch {
            repository.deleteRule(rule)
        }
    }
}