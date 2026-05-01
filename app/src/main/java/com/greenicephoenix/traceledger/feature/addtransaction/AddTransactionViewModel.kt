package com.greenicephoenix.traceledger.feature.addtransaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.greenicephoenix.traceledger.core.repository.TransactionRepository
import com.greenicephoenix.traceledger.domain.model.TransactionType
import com.greenicephoenix.traceledger.domain.model.TransactionUiModel
import com.greenicephoenix.traceledger.feature.templates.data.TemplateRepository
import com.greenicephoenix.traceledger.feature.templates.domain.TransactionTemplateUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class AddTransactionViewModel(
    private val transactionRepository: TransactionRepository,
    private val templateRepository: TemplateRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AddTransactionState())
    val state: StateFlow<AddTransactionState> = _state.asStateFlow()

    /**
     * All saved templates — exposed to the screen for the picker sheet.
     * Collected reactively so new templates appear immediately.
     */
    val templates: StateFlow<List<TransactionTemplateUiModel>> =
        templateRepository.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onEvent(event: AddTransactionEvent) {
        when (event) {
            is AddTransactionEvent.ChangeType       -> handleTypeChange(event.type)
            is AddTransactionEvent.ChangeAmount     -> update { it.copy(amount = event.amount) }
            is AddTransactionEvent.ChangeDate       -> update { it.copy(date = event.date) }
            is AddTransactionEvent.ChangeNotes      -> update { it.copy(notes = event.notes) }
            is AddTransactionEvent.SelectFromAccount -> update { it.copy(fromAccountId = event.accountId) }
            is AddTransactionEvent.SelectToAccount   -> update { it.copy(toAccountId = event.accountId) }
            is AddTransactionEvent.SelectCategory    -> update { it.copy(categoryId = event.categoryId) }
            is AddTransactionEvent.ApplyTemplate     -> handleApplyTemplate(event.template)
            is AddTransactionEvent.SaveAsTemplate    -> handleSaveAsTemplate(event.name)
            AddTransactionEvent.ConsumeTemplateSaved -> update { it.copy(templateSaved = false) }
            AddTransactionEvent.Save                 -> validateAndFinalize()
            AddTransactionEvent.Delete               -> deleteTransactionIfEditing()
        }
        // Recompute canSave after every event except template events
        // (template events don't change validation-relevant state)
        if (event !is AddTransactionEvent.SaveAsTemplate &&
            event !is AddTransactionEvent.ConsumeTemplateSaved) {
            recomputeCanSave()
        }
    }

    // ─────────────────────────────────────────────
    // TYPE SWITCH LOGIC
    // ─────────────────────────────────────────────

    private fun handleTypeChange(type: TransactionType) {
        update {
            when (type) {
                TransactionType.EXPENSE  -> it.copy(type = type, toAccountId = null)
                TransactionType.INCOME   -> it.copy(type = type, fromAccountId = null)
                TransactionType.TRANSFER -> it.copy(type = type, categoryId = null)
            }
        }
    }

    // ─────────────────────────────────────────────
    // TEMPLATE: APPLY
    // Pre-fills the form from a saved template.
    // Date is always kept as today — never pre-filled from template.
    // ─────────────────────────────────────────────

    private fun handleApplyTemplate(template: TransactionTemplateUiModel) {
        update { current ->
            current.copy(
                type          = template.type,
                amount        = template.amount?.toPlainString() ?: "",
                fromAccountId = template.fromAccountId,
                toAccountId   = template.toAccountId,
                categoryId    = template.categoryId,
                notes         = template.notes.orEmpty(),
                // Date stays as today — do NOT restore from template
                validationError = null
            )
        }
    }

    // ─────────────────────────────────────────────
    // TEMPLATE: SAVE AS
    // Saves current form fields as a new template.
    // Does NOT require the form to be valid.
    // Partial templates (e.g. only category + account) are allowed.
    // ─────────────────────────────────────────────

    private fun handleSaveAsTemplate(name: String) {
        if (name.isBlank()) return
        val current = _state.value
        val template = TransactionTemplateUiModel(
            id            = UUID.randomUUID().toString(),
            name          = name.trim(),
            type          = current.type,
            amount        = current.amount.toBigDecimalOrNull(),
            fromAccountId = current.fromAccountId,
            toAccountId   = current.toAccountId,
            categoryId    = current.categoryId,
            notes         = current.notes.takeIf { it.isNotBlank() }
        )
        viewModelScope.launch {
            templateRepository.save(template)
            update { it.copy(templateSaved = true) }
        }
    }

    // ─────────────────────────────────────────────
    // VALIDATION + SAVE
    // ─────────────────────────────────────────────

    private fun validateAndFinalize() {
        val current = _state.value
        val error = validate(current)

        if (error != null) {
            update { it.copy(validationError = error, saveCompleted = false) }
            return
        }

        val transaction = buildTransaction(current)

        viewModelScope.launch {
            if (editingTransactionId == null) {
                transactionRepository.insertTransactionWithBalance(transaction)
            } else {
                val updatedTx = transaction.copy(id = editingTransactionId!!)
                transactionRepository.updateTransactionWithBalance(updatedTx)
            }
            update { it.copy(validationError = null, saveCompleted = true) }
        }
    }

    private fun validate(state: AddTransactionState): TransactionValidationError? {
        val amountValue = state.amount.toBigDecimalOrNull()
        if (state.amount.isBlank()) return TransactionValidationError.MissingAmount
        if (amountValue == null || amountValue <= BigDecimal.ZERO) return TransactionValidationError.InvalidAmount
        return when (state.type) {
            TransactionType.EXPENSE  -> when {
                state.fromAccountId == null -> TransactionValidationError.MissingFromAccount
                state.categoryId == null    -> TransactionValidationError.MissingCategory
                else                        -> null
            }
            TransactionType.INCOME -> when {
                state.toAccountId == null -> TransactionValidationError.MissingToAccount
                state.categoryId == null  -> TransactionValidationError.MissingCategory
                else                      -> null
            }
            TransactionType.TRANSFER -> when {
                state.fromAccountId == null                -> TransactionValidationError.MissingFromAccount
                state.toAccountId == null                  -> TransactionValidationError.MissingToAccount
                state.fromAccountId == state.toAccountId   -> TransactionValidationError.SameAccountTransfer
                else                                       -> null
            }
        }
    }

    // ─────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────

    private fun deleteTransactionIfEditing() {
        val transactionId = editingTransactionId ?: return
        viewModelScope.launch {
            transactionRepository.deleteTransactionWithBalance(transactionId)
            update { it.copy(saveCompleted = true, validationError = null) }
        }
    }

    // ─────────────────────────────────────────────
    // CAN SAVE DERIVATION
    // ─────────────────────────────────────────────

    private fun recomputeCanSave() {
        val error = validate(_state.value)
        update { it.copy(canSave = error == null, validationError = error) }
    }

    // ─────────────────────────────────────────────
    // STATE HELPERS
    // ─────────────────────────────────────────────

    private inline fun update(block: (AddTransactionState) -> AddTransactionState) {
        _state.value = block(_state.value)
    }

    fun consumeSaveCompleted() {
        update { it.copy(saveCompleted = false) }
    }

    private fun buildTransaction(state: AddTransactionState): TransactionUiModel {
        return TransactionUiModel(
            id            = UUID.randomUUID().toString(),
            type          = state.type,
            amount        = state.amount.toBigDecimal(),
            date          = state.date,
            fromAccountId = state.fromAccountId,
            toAccountId   = state.toAccountId,
            categoryId    = state.categoryId,
            note          = state.notes.takeIf { it.isNotBlank() },
            createdAt     = Instant.now()
        )
    }

    // ─────────────────────────────────────────────
    // EDIT MODE INITIALIZATION
    // ─────────────────────────────────────────────

    private var editingTransactionId: String? = null
    private var hasLoadedEditData = false

    fun initEdit(transactionId: String) {
        if (hasLoadedEditData) return
        hasLoadedEditData = true
        editingTransactionId = transactionId

        viewModelScope.launch {
            val tx = transactionRepository.getTransactionById(transactionId)
                ?: return@launch

            _state.value = AddTransactionState(
                type          = tx.type,
                amount        = tx.amount.toPlainString(),
                date          = tx.date,
                notes         = tx.note.orEmpty(),
                fromAccountId = tx.fromAccountId,
                toAccountId   = tx.toAccountId,
                categoryId    = tx.categoryId,
                saveCompleted = false,
                isEditMode    = true
            )
            recomputeCanSave()
        }
    }
}

class AddTransactionViewModelFactory(
    private val transactionRepository: TransactionRepository,
    private val templateRepository: TemplateRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == AddTransactionViewModel::class.java)
        return AddTransactionViewModel(transactionRepository, templateRepository) as T
    }
}