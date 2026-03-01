package com.greenicephoenix.traceledger.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["fromAccountId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["toAccountId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("categoryId"),
        Index("fromAccountId"),
        Index("toAccountId"),
        Index("recurringId") // NEW
    ]
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val type: String,
    val amount: BigDecimal,
    val date: LocalDate,
    val fromAccountId: String?,
    val toAccountId: String?,
    val categoryId: String?,
    val note: String?,
    val createdAt: Instant,
    // NEW — nullable and defaulted
    val recurringId: String? = null
)
