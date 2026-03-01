package com.greenicephoenix.traceledger.feature.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenicephoenix.traceledger.core.database.entity.RecurringTransactionEntity
import com.greenicephoenix.traceledger.core.repository.RecurringTransactionRepository
import com.greenicephoenix.traceledger.domain.model.RecurringFrequency
import com.greenicephoenix.traceledger.domain.model.TransactionType
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class AddEditRecurringViewModel(
    private val repository: RecurringTransactionRepository
) : ViewModel() {
    var editingRecurringId: String? = null
        private set

    var currentRecurring: RecurringTransactionEntity? = null
        private set

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

            val entity = RecurringTransactionEntity(
                id = editingRecurringId ?: UUID.randomUUID().toString(),
                type = type.name,
                amount = amount,
                fromAccountId = fromAccountId,
                toAccountId = toAccountId,
                categoryId = categoryId,
                note = note,
                startDate = startDate,
                endDate = endDate,
                frequency = frequency.name,
                lastGeneratedDate = null
            )

            if (editingRecurringId == null) {
                repository.insert(entity)
            } else {
                repository.update(entity)
            }
        }
    }
    fun load(id: String) {
        viewModelScope.launch {
            val entity = repository.getById(id) ?: return@launch
            editingRecurringId = id
            currentRecurring = entity
        }
    }


}