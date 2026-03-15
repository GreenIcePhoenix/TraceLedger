package com.greenicephoenix.traceledger.core.currency

import com.greenicephoenix.traceledger.core.datastore.NumberFormat
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat as JavaNumberFormat
import java.util.Locale

/**
 * Formats a numeric string with the correct currency symbol and grouping style.
 *
 * Two concerns are now kept separate:
 *   1. Currency symbol and minor-unit digits → driven by [Currency]
 *   2. Thousands-grouping style               → driven by [NumberFormatManager]
 *
 * Sign handling: always produces "-₹560" not "₹-560".
 *
 * This object is stateless. NumberFormatManager.format is a StateFlow so reading
 * .value here is always in-sync with the user's last saved preference.
 */
object CurrencyFormatter {

    fun format(amount: String, currency: Currency): String {
        if (amount.isBlank()) return ""
        val number = amount.toBigDecimalOrNull() ?: return amount

        // Separate sign so we always prefix it before the symbol
        val isNegative = number.signum() < 0
        val absolute   = number.abs()

        // Decide grouping locale:
        //   • INDIAN format  → use the en_IN locale which gives 1,00,000 grouping
        //   • INTERNATIONAL  → use Locale.US which gives 100,000 grouping for all currencies
        val userFormat = NumberFormatManager.format.value
        val groupingLocale = when (userFormat) {
            NumberFormat.INTERNATIONAL -> Locale.US
            NumberFormat.INDIAN        -> when (currency) {
                // For non-INR currencies the user has chosen Indian grouping, which is
                // a bit unusual, but we respect the preference and use en_IN.
                else -> Locale("en", "IN")
            }
        }

        val javaFormatter = JavaNumberFormat.getNumberInstance(groupingLocale).apply {
            isGroupingUsed        = true
            minimumFractionDigits = if (amount.contains(".")) 2 else 0
            // JPY has no sub-unit; all others use 2 decimal places
            maximumFractionDigits = if (currency == Currency.JPY) 0 else 2
        }

        val formatted = "${currency.symbol}${javaFormatter.format(absolute)}"
        return if (isNegative) "-$formatted" else formatted
    }

    /**
     * Compact format for chart axis labels: abbreviates large numbers.
     * e.g. 1,50,000 → "₹1.5L"   |   25,000 → "₹25K"
     * Does NOT apply number-format grouping — these are always short labels.
     */
    fun formatCompact(amount: Double, currency: Currency): String {
        val symbol = currency.symbol
        return when {
            amount >= 10_000_000 -> "${symbol}${String.format("%.1f", amount / 10_000_000)}Cr"
            amount >= 100_000    -> "${symbol}${String.format("%.1f", amount / 100_000)}L"
            amount >= 1_000      -> "${symbol}${String.format("%.0f", amount / 1_000)}K"
            else                 -> "$symbol${amount.toLong()}"
        }
    }
}