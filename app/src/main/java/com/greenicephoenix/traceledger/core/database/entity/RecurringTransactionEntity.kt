package com.greenicephoenix.traceledger.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.LocalDate

@Entity(
    tableName = "recurring_transactions",
    indices = [
        Index("frequency"),
        Index("startDate"),
        Index("endDate")
    ]
)
data class RecurringTransactionEntity(

    @PrimaryKey
    val id: String,

    val type: String, // Expense / Income / Transfer

    val amount: BigDecimal,

    val fromAccountId: String?,
    val toAccountId: String?, // only for transfer

    val categoryId: String?, // null for transfer

    val note: String?,

    val startDate: LocalDate,

    val endDate: LocalDate?, // nullable for infinite

    val frequency: String, // DAILY, WEEKLY, MONTHLY, QUARTERLY, HALF_YEARLY, YEARLY

    val lastGeneratedDate: LocalDate?
)
