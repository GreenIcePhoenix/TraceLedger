package com.greenicephoenix.traceledger.feature.addtransaction

import com.greenicephoenix.traceledger.domain.model.CategoryUiModel
import com.greenicephoenix.traceledger.domain.model.TransactionType
import com.greenicephoenix.traceledger.feature.templates.domain.TransactionTemplateUiModel
import java.time.LocalDate

data class AddTransactionState(
    val type: TransactionType = TransactionType.EXPENSE,
    val amount: String = "",
    val date: LocalDate = LocalDate.now(),
    val notes: String = "",
    val fromAccountId: String? = null,
    val toAccountId: String? = null,
    val categoryId: String? = null,
    val validationError: TransactionValidationError? = null,
    val canSave: Boolean = false,
    val selectedCategory: CategoryUiModel? = null,
    val saveCompleted: Boolean = false,
    val isEditMode: Boolean = false,
    // Template feedback — set true briefly after "Save as Template" succeeds
    val templateSaved: Boolean = false
)

sealed interface AddTransactionEvent {

    data class ChangeType(val type: TransactionType) : AddTransactionEvent
    data class ChangeAmount(val amount: String) : AddTransactionEvent
    data class ChangeDate(val date: LocalDate) : AddTransactionEvent
    data class ChangeNotes(val notes: String) : AddTransactionEvent

    data class SelectFromAccount(val accountId: String) : AddTransactionEvent
    data class SelectToAccount(val accountId: String) : AddTransactionEvent
    data class SelectCategory(val categoryId: String) : AddTransactionEvent

    /**
     * Apply a template to the form. Pre-fills type, amount (if set),
     * accounts, category, and notes. Date is always kept as today.
     */
    data class ApplyTemplate(val template: TransactionTemplateUiModel) : AddTransactionEvent

    /**
     * Save current form fields as a new template with the given name.
     * Amount, accounts, category, and notes are all saved as-is.
     * Validation is NOT required — partial templates are allowed.
     */
    data class SaveAsTemplate(val name: String) : AddTransactionEvent

    /** Reset templateSaved flag after the NavGraph has shown the snackbar. */
    object ConsumeTemplateSaved : AddTransactionEvent

    object Save : AddTransactionEvent
    object Delete : AddTransactionEvent
}

sealed interface TransactionValidationError {
    object MissingAmount : TransactionValidationError
    object InvalidAmount : TransactionValidationError
    object MissingFromAccount : TransactionValidationError
    object MissingToAccount : TransactionValidationError
    object MissingCategory : TransactionValidationError
    object SameAccountTransfer : TransactionValidationError
}