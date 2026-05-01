package com.greenicephoenix.traceledger.feature.templates.domain

import com.greenicephoenix.traceledger.domain.model.TransactionType
import java.math.BigDecimal

/**
 * UI-layer data class for a transaction template.
 * Used everywhere outside the data layer — ViewModels, screens, NavGraph.
 *
 * amount is nullable: null means the template doesn't preset an amount.
 * All account/category IDs are nullable: a template may preset only some fields.
 */
data class TransactionTemplateUiModel(
    val id: String,
    val name: String,
    val type: TransactionType,
    val amount: BigDecimal?,      // null = no preset amount
    val fromAccountId: String?,
    val toAccountId: String?,
    val categoryId: String?,
    val notes: String?
)