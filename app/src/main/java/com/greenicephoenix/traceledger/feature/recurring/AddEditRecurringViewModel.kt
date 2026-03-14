package com.greenicephoenix.traceledger.feature.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenicephoenix.traceledger.core.database.entity.RecurringTransactionEntity
import com.greenicephoenix.traceledger.core.repository.RecurringTransactionRepository
import com.greenicephoenix.traceledger.domain.model.RecurringFrequency
import com.greenicephoenix.traceledger.domain.model.TransactionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class AddEditRecurringViewModel(
    private val repository: RecurringTransactionRepository
) : ViewModel() {

    // ── Editing state ──────────────────────────────────────────────────────────

    private var editingRecurringId: String? = null

    // FIX: currentRecurring was a plain `var` — not observable.
    // The NavGraph composable could not react when load() finished fetching
    // from the DB, so `existing` was always null when AddEditRecurringScreen
    // rendered. Changed to StateFlow so the composable recomposes correctly
    // once the data arrives from the suspend call.
    private val _currentRecurring = MutableStateFlow<RecurringTransactionEntity?>(null)
    val currentRecurring: StateFlow<RecurringTransactionEntity?> = _currentRecurring.asStateFlow()

    // ── Load existing rule for editing ────────────────────────────────────────

    /**
     * Load a recurring rule by ID.
     * Call this from a LaunchedEffect in the NavGraph when opening edit mode.
     * The result is emitted to [currentRecurring] — observe that in the UI.
     *
     * The hasLoaded guard prevents re-fetching on recomposition.
     */
    private var hasLoaded = false

    fun load(id: String) {
        if (hasLoaded) return
        hasLoaded = true
        editingRecurringId = id

        viewModelScope.launch {
            val entity = repository.getById(id) ?: return@launch
            _currentRecurring.value = entity
        }
    }

    // ── Save (insert or update) ───────────────────────────────────────────────

    fun saveRecurring(
        type: TransactionType,
        amount: BigDecimal,
        fromAccountId: String?,
        toAccountId: String?,
        categoryId: String?,
        frequency: RecurringFrequency,
        startDate: LocalDate,
        endDate: LocalDate?,
        note: String?
    ) {
        viewModelScope.launch {

            // When updating, preserve lastGeneratedDate and isActive from the
            // original rule — we should not reset generation history on edit.
            val existing = _currentRecurring.value

            val entity = RecurringTransactionEntity(
                id                = editingRecurringId ?: UUID.randomUUID().toString(),
                type              = type.name,
                amount            = amount,
                fromAccountId     = fromAccountId,
                toAccountId       = toAccountId,
                categoryId        = categoryId,
                note              = note,
                startDate         = startDate,
                endDate           = endDate,
                frequency         = frequency.name,
                // Preserve existing generation state when editing.
                // If creating new, lastGeneratedDate starts null.
                lastGeneratedDate = existing?.lastGeneratedDate,
                isActive          = existing?.isActive ?: true
            )

            if (editingRecurringId == null) {
                repository.insert(entity)
            } else {
                repository.update(entity)
            }
        }
    }
}