package com.greenicephoenix.traceledger.feature.addtransaction

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.greenicephoenix.traceledger.domain.model.TransactionType
import java.math.BigDecimal
import com.greenicephoenix.traceledger.domain.model.TransactionUiModel
import java.time.Instant
import java.util.UUID  // FIX: import UUID instead of relying on System.currentTimeMillis
import com.greenicephoenix.traceledger.core.repository.TransactionRepository
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class AddTransactionViewModel(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AddTransactionState())
    val state: StateFlow<AddTransactionState> = _state.asStateFlow()

    fun onEvent(event: AddTransactionEvent) {
        when (event) {

            is AddTransactionEvent.ChangeType -> {
                handleTypeChange(event.type)
            }

            is AddTransactionEvent.ChangeAmount -> {
                update { it.copy(amount = event.amount) }
            }

            is AddTransactionEvent.ChangeDate -> {
                update { it.copy(date = event.date) }
            }

            is AddTransactionEvent.ChangeNotes -> {
                update { it.copy(notes = event.notes) }
            }

            is AddTransactionEvent.SelectFromAccount -> {
                update { it.copy(fromAccountId = event.accountId) }
            }

            is AddTransactionEvent.SelectToAccount -> {
                update { it.copy(toAccountId = event.accountId) }
            }

            is AddTransactionEvent.SelectCategory -> {
                update { it.copy(categoryId = event.categoryId) }
            }

            AddTransactionEvent.Save -> {
                validateAndFinalize()
            }

            AddTransactionEvent.Delete -> {
                deleteTransactionIfEditing()
            }
        }

        recomputeCanSave()
    }

    // ─────────────────────────────────────────────
    // TYPE SWITCH LOGIC
    // When the user switches type, clear fields that
    // don't apply to the new type to avoid invalid state.
    // ─────────────────────────────────────────────

    private fun handleTypeChange(type: TransactionType) {
        update {
            when (type) {
                TransactionType.EXPENSE -> it.copy(
                    type = type,
                    toAccountId = null   // Expense has no destination account
                )
                TransactionType.INCOME -> it.copy(
                    type = type,
                    fromAccountId = null  // Income has no source account
                )
                TransactionType.TRANSFER -> it.copy(
                    type = type,
                    categoryId = null    // Transfer has no category
                )
            }
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
                // New transaction — insert with balance adjustment
                transactionRepository.insertTransactionWithBalance(transaction)
            } else {
                // Editing existing — reverse old balance, apply new balance
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
            TransactionType.EXPENSE -> when {
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
                state.fromAccountId == null                    -> TransactionValidationError.MissingFromAccount
                state.toAccountId == null                      -> TransactionValidationError.MissingToAccount
                state.fromAccountId == state.toAccountId       -> TransactionValidationError.SameAccountTransfer
                else                                           -> null
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
    // Recomputed after every event so the Save button
    // state stays in sync without explicit triggers.
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

    /**
     * Build a TransactionUiModel from validated state.
     *
     * FIX: ID now uses UUID.randomUUID() instead of System.currentTimeMillis().
     * The old approach caused silent data loss when two transactions were saved
     * in the same millisecond (possible when recurring generator runs multiple
     * entries at once). UUID guarantees global uniqueness with no collision risk.
     */
    private fun buildTransaction(state: AddTransactionState): TransactionUiModel {
        return TransactionUiModel(
            id = UUID.randomUUID().toString(), // FIX: was System.currentTimeMillis().toString()
            type = state.type,
            amount = state.amount.toBigDecimal(), // Safe — already validated above
            date = state.date,
            fromAccountId = state.fromAccountId,
            toAccountId = state.toAccountId,
            categoryId = state.categoryId,
            note = state.notes.takeIf { it.isNotBlank() },
            createdAt = Instant.now()
            // recurringId intentionally omitted — manual transactions are never recurring
        )
    }

    // ─────────────────────────────────────────────
    // EDIT MODE INITIALIZATION
    // Called from NavGraph when opening an existing transaction.
    // The hasLoadedEditData guard prevents double-loading on
    // recomposition (e.g. screen rotation).
    // ─────────────────────────────────────────────

    private var editingTransactionId: String? = null
    private var hasLoadedEditData = false

    fun initEdit(transactionId: String) {
        if (hasLoadedEditData) return  // Guard: only load once per ViewModel instance
        hasLoadedEditData = true
        editingTransactionId = transactionId

        viewModelScope.launch {
            val tx = transactionRepository.getTransactionById(transactionId)
                ?: return@launch  // Transaction not found — nothing to load

            _state.value = AddTransactionState(
                type = tx.type,
                amount = tx.amount.toPlainString(),
                date = tx.date,
                notes = tx.note.orEmpty(),
                fromAccountId = tx.fromAccountId,
                toAccountId = tx.toAccountId,
                categoryId = tx.categoryId,
                saveCompleted = false,
                isEditMode = true
            )

            recomputeCanSave()
        }
    }
}