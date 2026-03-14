package com.greenicephoenix.traceledger.core.currency

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {

    fun format(amount: String, currency: Currency): String {
        if (amount.isBlank()) return ""
        val number = amount.toBigDecimalOrNull() ?: return amount

        // FIX: Handle negative amounts by separating sign from formatting.
        // Previously: ₹-560  (symbol + negative number)
        // Now:        -₹560  (sign + symbol + absolute number)
        val isNegative = number.signum() < 0
        val absolute   = number.abs()

        val locale = when (currency) {
            Currency.INR -> Locale("en", "IN")
            Currency.EUR -> Locale.GERMANY
            Currency.GBP -> Locale.UK
            Currency.JPY -> Locale.JAPAN
            else         -> Locale.US
        }

        val formatter = NumberFormat.getNumberInstance(locale).apply {
            isGroupingUsed        = true
            minimumFractionDigits = if (amount.contains(".")) 2 else 0
            maximumFractionDigits = if (currency == Currency.JPY) 0 else 2
        }

        val formatted = "${currency.symbol}${formatter.format(absolute)}"
        return if (isNegative) "-$formatted" else formatted
    }
}