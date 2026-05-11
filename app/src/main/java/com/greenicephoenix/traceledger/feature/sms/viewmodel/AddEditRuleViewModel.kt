package com.greenicephoenix.traceledger.feature.sms.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenicephoenix.traceledger.core.database.entity.SmsCustomRuleEntity
import com.greenicephoenix.traceledger.feature.sms.model.SmsParseResult
import com.greenicephoenix.traceledger.feature.sms.parser.SmsRuleEngine
import com.greenicephoenix.traceledger.feature.sms.repository.SmsRuleRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// ── Form state ────────────────────────────────────────────────────────────────

data class RuleFormState(
    val id: Long = 0,
    val name: String = "",
    val senderPattern: String = "",
    val isExclusionRule: Boolean = false,
    val amountPrefix: String = "",
    val debitKeywords: String = "",
    val creditKeywords: String = "",
    val merchantKeyword: String = "",
    val defaultCategoryId: String? = null,
    val defaultAccountId: String? = null,
    val isEnabled: Boolean = true,
    val priority: Int = 10,
    // Validation errors
    val nameError: String? = null,
    val senderError: String? = null,
    // Navigation trigger
    val isSaved: Boolean = false,
)

// ── Tester state ──────────────────────────────────────────────────────────────

sealed class RuleTesterState {
    object Idle : RuleTesterState()
    data class Detected(
        val amount: Double,
        val type: String,
        val description: String,
    ) : RuleTesterState()
    object WouldBeExcluded : RuleTesterState()
    data class NoMatch(val reason: String) : RuleTesterState()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class AddEditRuleViewModel(
    private val repository: SmsRuleRepository,
) : ViewModel() {

    private val engine = SmsRuleEngine()

    private val _form = MutableStateFlow(RuleFormState())
    val form: StateFlow<RuleFormState> = _form.asStateFlow()

    private val _tester = MutableStateFlow<RuleTesterState>(RuleTesterState.Idle)
    val tester: StateFlow<RuleTesterState> = _tester.asStateFlow()

    // ── Load (edit mode) ──────────────────────────────────────────────────────

    fun loadRule(rule: SmsCustomRuleEntity) {
        _form.value = RuleFormState(
            id              = rule.id,
            name            = rule.name,
            senderPattern   = rule.senderPattern,
            isExclusionRule = rule.isExclusionRule,
            amountPrefix    = rule.amountPrefix,
            debitKeywords   = rule.debitKeywords,
            creditKeywords  = rule.creditKeywords,
            merchantKeyword = rule.merchantRegex,
            defaultCategoryId = rule.defaultCategoryId,
            defaultAccountId  = rule.defaultAccountId,
            isEnabled       = rule.isEnabled,
            priority        = rule.priority,
        )
    }

    // ── Field updates ─────────────────────────────────────────────────────────

    fun updateName(v: String)             = _form.update { it.copy(name = v, nameError = null) }
    fun updateSenderPattern(v: String)    = _form.update { it.copy(senderPattern = v, senderError = null) }
    fun updateIsExclusion(v: Boolean)     = _form.update { it.copy(isExclusionRule = v) }
    fun updateAmountPrefix(v: String)     = _form.update { it.copy(amountPrefix = v) }
    fun updateDebitKeywords(v: String)    = _form.update { it.copy(debitKeywords = v) }
    fun updateCreditKeywords(v: String)   = _form.update { it.copy(creditKeywords = v) }
    fun updateMerchantKeyword(v: String)  = _form.update { it.copy(merchantKeyword = v) }
    fun updateDefaultCategory(id: String?) = _form.update { it.copy(defaultCategoryId = id) }
    fun updateDefaultAccount(id: String?)  = _form.update { it.copy(defaultAccountId = id) }
    fun updateEnabled(v: Boolean)         = _form.update { it.copy(isEnabled = v) }
    fun updatePriority(v: Int)            = _form.update { it.copy(priority = v.coerceIn(1, 20)) }

    // ── Rule tester ───────────────────────────────────────────────────────────

    /**
     * Tests the CURRENT form state against a pasted SMS body.
     * Bypasses the sender-check gate — we only test parsing logic.
     */
    fun testRule(smsBody: String) {
        val state = _form.value
        if (state.senderPattern.isBlank()) {
            _tester.value = RuleTesterState.NoMatch("Enter a sender pattern first")
            return
        }
        if (state.isExclusionRule) {
            _tester.value = RuleTesterState.WouldBeExcluded
            return
        }
        val tempRule = state.toEntity()
        val result   = engine.testCustomRule(smsBody, tempRule, System.currentTimeMillis())
        _tester.value = when (result) {
            is SmsParseResult.Success     -> RuleTesterState.Detected(
                amount      = result.transaction.amount,
                type        = result.transaction.type.name,
                description = result.transaction.description,
            )
            is SmsParseResult.NotFinancial -> RuleTesterState.NoMatch("No amount or direction detected")
            is SmsParseResult.ParseError   -> RuleTesterState.NoMatch(result.reason)
        }
    }

    fun resetTester() { _tester.value = RuleTesterState.Idle }

    // ── Save ──────────────────────────────────────────────────────────────────

    fun save() {
        val state = _form.value
        var hasError = false

        if (state.name.isBlank()) {
            _form.update { it.copy(nameError = "Name is required") }
            hasError = true
        }
        if (state.senderPattern.isBlank()) {
            _form.update { it.copy(senderError = "Sender pattern is required") }
            hasError = true
        }
        if (hasError) return

        viewModelScope.launch {
            repository.addOrUpdateRule(state.toEntity())
            _form.update { it.copy(isSaved = true) }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun RuleFormState.toEntity() = SmsCustomRuleEntity(
        id                = id,
        name              = name.trim(),
        senderPattern     = senderPattern.trim(),
        amountPrefix      = amountPrefix.trim(),
        debitKeywords     = debitKeywords.trim(),
        creditKeywords    = creditKeywords.trim(),
        merchantRegex     = merchantKeyword.trim(),
        defaultCategoryId = defaultCategoryId,
        defaultAccountId  = defaultAccountId,
        isEnabled         = isEnabled,
        priority          = priority,
        isAdvancedMode    = false,
        rawRegex          = "",
        isExclusionRule   = isExclusionRule,
        createdAt         = System.currentTimeMillis()
    )
}