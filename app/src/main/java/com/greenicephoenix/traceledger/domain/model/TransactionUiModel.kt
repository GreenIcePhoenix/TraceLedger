package com.greenicephoenix.traceledger.domain.model

import java.time.Instant
import java.time.LocalDate
import java.math.BigDecimal

/**
 * UI / domain representation of a transaction.
 *
 * Rules:
 * - amount is always positive
 * - account direction is defined by [type]
 * - category applies only to EXPENSE and INCOME
 */
data class TransactionUiModel(
    val id: String,

    // Core
    val type: TransactionType,
    val amount: BigDecimal,
    val date: LocalDate,

    // Accounts
    val fromAccountId: String?,
    val toAccountId: String?,

    // Category
    val categoryId: String?,

    // Optional metadata
    val note: String? = null,

    // System
    val createdAt: Instant,

    // 🔁 NEW — null for manual transactions
    val recurringId: String? = null
)