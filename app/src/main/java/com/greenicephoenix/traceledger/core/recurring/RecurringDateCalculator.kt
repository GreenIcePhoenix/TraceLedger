package com.greenicephoenix.traceledger.core.recurring

import java.time.LocalDate

object RecurringDateCalculator {

    fun calculateNextDate(
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

    fun nextExecutionDate(
        startDate: LocalDate,
        lastGeneratedDate: LocalDate?,
        frequency: String
    ): LocalDate {

        return if (lastGeneratedDate == null) {
            startDate
        } else {
            calculateNextDate(lastGeneratedDate, frequency)
        }
    }
}