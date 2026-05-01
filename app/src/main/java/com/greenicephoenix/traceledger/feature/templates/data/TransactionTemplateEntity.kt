package com.greenicephoenix.traceledger.feature.templates.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Room entity for a saved transaction template.
 *
 * amount is String? (not BigDecimal) — null means "no preset amount".
 * The user can save a template that only presets type/category/account,
 * leaving the amount to be entered fresh each time.
 *
 * createdAt is stored as Long (epoch millis) — Room uses the existing
 * Instant converter in RoomConverters.kt.
 */
@Entity(tableName = "transaction_templates")
data class TransactionTemplateEntity(
    @PrimaryKey val id: String,
    val name: String,             // User-facing label, e.g. "Monthly Rent"
    val type: String,             // TransactionType.name(), e.g. "EXPENSE"
    val amount: String?,          // null = no preset. "1500.00" = preset amount.
    val fromAccountId: String?,
    val toAccountId: String?,
    val categoryId: String?,
    val notes: String?,
    val createdAt: Long           // Instant.toEpochMilli()
)