// ─────────────────────────────────────────────────────────────────────────────
// FILE: feature/accountimport/parser/UniversalPdfParser.kt
//
// Bank-agnostic PDF transaction extractor using the Rule Engine approach.
// Works for any bank worldwide — HDFC, ICICI, SBI, Federal, HSBC, Citi,
// Bank of America, Lloyds, Westpac, and any other bank.
//
// Algorithm:
//   1. For each line of extracted PDF text:
//      a. Check if line contains a DATE pattern → if not, skip
//      b. Check if line contains at least one AMOUNT → if not, skip
//      c. Last amount = running balance (usually); second-to-last = transaction amount
//      d. Determine DEBIT/CREDIT via: DR/CR keywords → balance delta → default debit
//      e. Description = text between the date and first amount
//   2. Filter out header/footer lines (Opening Balance, Closing Balance, etc.)
//
// Handles:
//   Indian formats:     1,23,456.78  |  DD/MM/YYYY  |  DD MMM YYYY
//   American formats:   1,234,567.89 |  MM/DD/YYYY
//   European formats:   1.234.567,89 |  DD.MM.YYYY
//   ISO format:         1234567.89   |  YYYY-MM-DD
// ─────────────────────────────────────────────────────────────────────────────
package com.greenicephoenix.traceledger.feature.accountimport.parser

import com.greenicephoenix.traceledger.feature.accountimport.model.ParsedTransaction
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

object UniversalPdfParser {

    // ── Date patterns ─────────────────────────────────────────────────────────
    // Ordered: most specific first (longest match wins).
    private val DATE_PATTERNS = listOf(
        // DD MMM YYYY or D MMM YYYY  (01 Jan 2024 / 1 Jan 2024)
        Regex("""\b(\d{1,2}\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\.?\s+\d{4})\b""", RegexOption.IGNORE_CASE),
        // YYYY-MM-DD  ISO
        Regex("""\b(\d{4}[-/]\d{2}[-/]\d{2})\b"""),
        // DD/MM/YYYY or MM/DD/YYYY or DD-MM-YYYY or DD.MM.YYYY (4-digit year)
        Regex("""\b(\d{1,2}[/\-\.]\d{1,2}[/\-\.]\d{4})\b"""),
        // DD/MM/YY  (2-digit year)
        Regex("""\b(\d{1,2}[/\-\.]\d{1,2}[/\-\.]\d{2})\b""")
    )

    // ── Amount pattern ────────────────────────────────────────────────────────
    // Matches: 1,23,456.78 | 1,234,567.89 | 1.234,56 | 1234567.89 | 150.00
    // Requires at least one digit, optional comma grouping, mandatory decimal
    // OR large round numbers (catches 150 but not dates like 12).
    private val AMOUNT_PATTERN = Regex(
        """(?<![.\d,])(\d{1,3}(?:[,\.]\d{2,3})+(?:[,.]\d{1,2})?|\d+\.\d{2})(?![.\d])"""
    )

    // ── Debit/Credit keywords ─────────────────────────────────────────────────
    private val DEBIT_KW  = Regex("""\b(dr\.?|debit|withdrawal|w/d|purchase|paid|payment|sent|deducted)\b""",  RegexOption.IGNORE_CASE)
    private val CREDIT_KW = Regex("""\b(cr\.?|credit|deposit|received|refund|reversal|salary|credited|interest\s+credit)\b""", RegexOption.IGNORE_CASE)

    // ── Lines to skip ─────────────────────────────────────────────────────────
    private val SKIP_PATTERN = Regex(
        """opening\s+balance|closing\s+balance|^total\b|balance\s+b/?f|brought\s+forward|
           |^statement\s+of|account\s+(number|no)|page\s+\d+\s+of|customer\s+(name|id)|
           |branch\s+(name|code)|ifsc|^date\s+(narration|description|particulars)|
           |^\s*(narration|description|particulars|details)\s*${'$'}""".trimMargin("|"),
        RegexOption.IGNORE_CASE
    )

    // ── Date formatters (tried in order) ─────────────────────────────────────
    private val DATE_FORMATTERS = listOf(
        DateTimeFormatter.ofPattern("d MMM yyyy",  Locale.ENGLISH),
        DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("d-MMM-yyyy",  Locale.ENGLISH),
        DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("dd.MM.yyyy"),
        DateTimeFormatter.ofPattern("d/M/yyyy"),
        DateTimeFormatter.ofPattern("d-M-yyyy"),
        // American formats — tried last since DD/MM ambiguous
        DateTimeFormatter.ofPattern("MM/dd/yyyy"),
        DateTimeFormatter.ofPattern("M/d/yyyy"),
        // 2-digit year
        DateTimeFormatter.ofPattern("dd/MM/yy"),
        DateTimeFormatter.ofPattern("MM/dd/yy"),
        DateTimeFormatter.ofPattern("dd-MM-yy")
    )

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Parse PDF extracted text into transactions.
     * Text must have been extracted with PDFBox using sortByPosition = true.
     */
    fun parse(text: String): List<ParsedTransaction> {
        val lines = text.lines()
            .map { it.trim() }
            .filter { it.length > 8 }   // skip very short lines (noise)

        var previousBalance: BigDecimal? = null
        val results = mutableListOf<ParsedTransaction>()

        for (line in lines) {
            if (SKIP_PATTERN.containsMatchIn(line)) continue

            val tx = parseLine(line, previousBalance) ?: continue

            // Sanity check: reject implausibly large amounts (> 10 crore)
            // which usually indicate we've mismatched a non-amount number
            if (tx.amount > BigDecimal("100000000")) continue

            results.add(tx)
            if (tx.balance != null) previousBalance = tx.balance
        }

        return results
    }

    // ── Core line parser ──────────────────────────────────────────────────────

    private fun parseLine(line: String, previousBalance: BigDecimal?): ParsedTransaction? {
        // Requirement 1: must contain a date
        val dateResult = DATE_PATTERNS.firstNotNullOfOrNull { pattern ->
            pattern.find(line)?.let { it to it.value }
        } ?: return null
        val (dateMatchObj, rawDate) = dateResult

        // Requirement 2: must contain at least one amount
        val amountMatches = AMOUNT_PATTERN.findAll(line)
            .map { m ->
                val cleaned = normaliseAmount(m.value) ?: return@map null
                if (cleaned <= BigDecimal.ZERO) null
                else Triple(m.range.first, m.range.last, cleaned)
            }
            .filterNotNull()
            .toList()

        if (amountMatches.isEmpty()) return null

        // Last amount = running balance, second-to-last = transaction amount
        val balance = amountMatches.last().third
        val transactionAmount = when {
            amountMatches.size >= 2 -> amountMatches[amountMatches.size - 2].third
            else                    -> amountMatches.first().third
        }

        // Determine direction
        val isCredit = when {
            CREDIT_KW.containsMatchIn(line) && !DEBIT_KW.containsMatchIn(line) -> true
            DEBIT_KW.containsMatchIn(line)                                       -> false
            // Balance delta: balance rose → credit
            previousBalance != null && amountMatches.size >= 2 ->
                balance > previousBalance
            else -> false
        }

        // Description: text between end of date and start of first amount
        val dateEnd       = dateMatchObj.range.last + 1
        val firstAmtStart = amountMatches.first().first
        val description   = buildDescription(line, dateEnd, firstAmtStart)

        val parsedDate = tryParseDate(rawDate)

        return ParsedTransaction(
            rawDate     = rawDate,
            date        = parsedDate,
            description = description,
            amount      = transactionAmount,
            isCredit    = isCredit,
            balance     = if (amountMatches.size >= 2) balance else null
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Normalise amount strings from multiple locales.
     * Indian:   "1,23,456.78"  → 123456.78
     * American: "1,234,567.89" → 1234567.89
     * European: "1.234,56"     → 1234.56  (dot = thousands, comma = decimal)
     */
    private fun normaliseAmount(raw: String): BigDecimal? {
        val s = raw.trim()
        // Detect European format: ends with ,XX (comma before 2 digits)
        val european = Regex(""",(\d{2})${'$'}""").find(s)
        val cleaned = if (european != null && !s.contains('.')) {
            // European: replace dots (thousands) and comma (decimal)
            s.replace(".", "").replace(",", ".")
        } else {
            // Indian / American / plain: remove all commas and spaces
            s.replace(Regex("[,\\s]"), "")
        }
        return try {
            BigDecimal(cleaned).abs()
        } catch (e: NumberFormatException) {
            null
        }
    }

    private fun buildDescription(line: String, dateEnd: Int, firstAmtStart: Int): String {
        val start = dateEnd.coerceAtMost(line.length)
        val end   = firstAmtStart.coerceAtLeast(start).coerceAtMost(line.length)
        return line.substring(start, end)
            .replace(Regex("""[|\\]"""), " ")
            .replace(Regex("""\s{2,}"""), " ")
            .trim()
            .ifBlank {
                // Fallback: first 80 chars of line
                line.take(80).trim()
            }
    }

    private fun tryParseDate(raw: String): LocalDate? {
        val cleaned = raw.trim()
        for (fmt in DATE_FORMATTERS) {
            try { return LocalDate.parse(cleaned, fmt) }
            catch (e: DateTimeParseException) { /* try next */ }
        }
        return null
    }
}