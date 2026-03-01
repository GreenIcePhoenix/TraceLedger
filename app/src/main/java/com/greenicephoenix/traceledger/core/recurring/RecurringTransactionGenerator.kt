package com.greenicephoenix.traceledger.core.recurring

import com.greenicephoenix.traceledger.core.database.entity.RecurringTransactionEntity
import com.greenicephoenix.traceledger.core.repository.RecurringTransactionRepository
import com.greenicephoenix.traceledger.core.repository.TransactionRepository
import com.greenicephoenix.traceledger.domain.model.TransactionType
import com.greenicephoenix.traceledger.domain.model.TransactionUiModel
import java.time.LocalDate
import java.time.Instant
import java.util.UUID

class RecurringTransactionGenerator(
    private val recurringRepository: RecurringTransactionRepository,
    private val transactionRepository: TransactionRepository
) {

    suspend fun generateIfNeeded() {

        val today = LocalDate.now()
        val activeRecurring =
            recurringRepository.getActiveRecurring(today.toString())

        for (recurring in activeRecurring) {

            transactionRepository.runInTransaction {

                val lastDate = recurring.lastGeneratedDate

                var nextDate = if (lastDate == null) {
                    recurring.startDate
                } else {
                    calculateNextDate(
                        baseDate = lastDate,
                        frequency = recurring.frequency
                    )
                }

                var latestGeneratedDate = lastDate

                while (!nextDate.isAfter(today)) {

                    if (recurring.endDate != null &&
                        nextDate.isAfter(recurring.endDate)
                    ) {
                        break
                    }

                    val existing = transactionRepository
                        .getByRecurringAndDate(recurring.id, nextDate)

                    if (existing == null) {

                        val uiModel = buildUiModel(
                            recurring = recurring,
                            date = nextDate
                        )

                        transactionRepository
                            .insertTransactionWithBalance(uiModel)
                    }

                    latestGeneratedDate = nextDate

                    nextDate = calculateNextDate(
                        baseDate = nextDate,
                        frequency = recurring.frequency
                    )
                }

                if (latestGeneratedDate != lastDate) {
                    recurringRepository.update(
                        recurring.copy(
                            lastGeneratedDate = latestGeneratedDate
                        )
                    )
                }
            }
        }
    }

    private fun calculateNextDate(
        baseDate: LocalDate,
        frequency: String
    ): LocalDate {

        return when (frequency) {
            "DAILY" -> baseDate.plusDays(1)
            "WEEKLY" -> baseDate.plusWeeks(1)
            "MONTHLY" -> safeAddMonths(baseDate, 1)
            "QUARTERLY" -> safeAddMonths(baseDate, 3)
            "HALF_YEARLY" -> safeAddMonths(baseDate, 6)
            "YEARLY" -> baseDate.plusYears(1)
            else -> baseDate
        }
    }

    private fun safeAddMonths(
        date: LocalDate,
        months: Long
    ): LocalDate {

        val target = date.plusMonths(months)

        val lastDayOfTargetMonth =
            target.withDayOfMonth(target.lengthOfMonth())

        return if (date.dayOfMonth == date.lengthOfMonth()) {
            lastDayOfTargetMonth
        } else {
            target
        }
    }


    private fun buildUiModel(
        recurring: RecurringTransactionEntity,
        date: LocalDate
    ): TransactionUiModel {

        return TransactionUiModel(
            id = UUID.randomUUID().toString(),
            type = TransactionType.valueOf(recurring.type),
            amount = recurring.amount,
            date = date,
            fromAccountId = recurring.fromAccountId,
            toAccountId = recurring.toAccountId,
            categoryId = recurring.categoryId,
            note = recurring.note,
            createdAt = Instant.now(),
            recurringId = recurring.id
        )
    }
}