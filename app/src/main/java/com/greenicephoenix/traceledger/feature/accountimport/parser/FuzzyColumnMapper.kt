// ─────────────────────────────────────────────────────────────────────────────
// FILE: feature/accountimport/parser/FuzzyColumnMapper.kt
//
// What this does:
//   Takes the header row of ANY bank statement (CSV or XLSX) and returns a
//   ColumnMapping by matching each header against known patterns.
//
// This is what makes TraceLedger bank-agnostic. Instead of maintaining a
// hardcoded mapping per bank ("HDFC debit is column 3"), we look at the actual
// header text and figure it out on the spot.
//
// Covers: Federal, Canara, PNB, Bank of Baroda, IDFC, AU Small Finance,
//         RBL, HSBC, Standard Chartered, DBS, and any future bank.
//
// Confidence scoring:
//   Each column gets a score from 0–3 based on how well it matches each role.
//   Highest scoring column per role wins. Ties broken by column order (earlier wins).
//
// Minimum requirements for a valid mapping:
//   - Date column found
//   - Description column found
//   - At least one of: debit column, credit column, or amount column found
//   If minimum not met → returns null → caller falls back to UNKNOWN.
// ─────────────────────────────────────────────────────────────────────────────
package com.greenicephoenix.traceledger.feature.accountimport.parser

object FuzzyColumnMapper {

    // ── Pattern lists ─────────────────────────────────────────────────────────
    // Each list is checked in order — first match scores 3, second 2, etc.
    // Multiple patterns per role handle different bank naming conventions.

    private val DATE_PATTERNS = listOf(
        Regex("(txn|trans|value|posting|tran).?date",   RegexOption.IGNORE_CASE),
        Regex("^(date|dt)$",                             RegexOption.IGNORE_CASE),
        Regex("date",                                    RegexOption.IGNORE_CASE)
    )

    private val DESC_PATTERNS = listOf(
        Regex("narration|particulars",                          RegexOption.IGNORE_CASE),
        Regex("description|remarks?|details?|memo|comments?",  RegexOption.IGNORE_CASE),
        Regex("transaction.?(detail|info|remark)",              RegexOption.IGNORE_CASE)
    )

    private val DEBIT_PATTERNS = listOf(
        Regex("withdrawal.?(amt|amount)?|\\bdebit.?(amt|amount)?\\b", RegexOption.IGNORE_CASE),
        Regex("\\bdr\\.?(amt|amount)?\\b",               RegexOption.IGNORE_CASE),
        Regex("withdraw|paid.?out|money.?out",            RegexOption.IGNORE_CASE)
    )

    private val CREDIT_PATTERNS = listOf(
        Regex("deposit.?(amt|amount)?|\\bcredit.?(amt|amount)?\\b",   RegexOption.IGNORE_CASE),
        Regex("\\bcr\\.?(amt|amount)?\\b",               RegexOption.IGNORE_CASE),
        Regex("paid.?in|money.?in|receipts?",             RegexOption.IGNORE_CASE)
    )

    private val BALANCE_PATTERNS = listOf(
        Regex("closing.?balance|available.?balance",     RegexOption.IGNORE_CASE),
        Regex("\\bbalance\\b|\\bbal\\b",                 RegexOption.IGNORE_CASE),
        Regex("running.?balance|current.?balance",       RegexOption.IGNORE_CASE)
    )

    private val REFERENCE_PATTERNS = listOf(
        Regex("chq.?(no|number)?|cheque.?(no|number)?",  RegexOption.IGNORE_CASE),
        Regex("ref.?(no|number)?|utr|transaction.?id",   RegexOption.IGNORE_CASE),
        Regex("instrument|serial|voucher",               RegexOption.IGNORE_CASE)
    )

    private val AMOUNT_PATTERNS = listOf(
        Regex("^(amount|amt)$",                          RegexOption.IGNORE_CASE),
        Regex("\\b(amount|amt)\\b",                      RegexOption.IGNORE_CASE)
    )

    private val DIRECTION_PATTERNS = listOf(
        Regex("dr.?/?.?cr|cr.?/?.?dr",                  RegexOption.IGNORE_CASE),
        Regex("\\btype\\b|debit.?credit",                RegexOption.IGNORE_CASE)
    )

    private val CATEGORY_PATTERNS = listOf(
        Regex("^(category|cat)$",         RegexOption.IGNORE_CASE),
        Regex("\\b(category|cat)\\.?\\b", RegexOption.IGNORE_CASE)
    )

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Detect column layout from a header row.
     *
     * @param headers  List of header cell values from the first row.
     * @return         A [ColumnMapping] if the minimum columns are found, null otherwise.
     */
    fun detectColumns(headers: List<String>): ColumnMapping? {
        if (headers.isEmpty()) return null

        // Score each column against each role, pick best match per role
        val dateCol      = bestMatch(headers, DATE_PATTERNS)
        val descCol      = bestMatch(headers, DESC_PATTERNS)
        val debitCol     = bestMatch(headers, DEBIT_PATTERNS)
        val creditCol    = bestMatch(headers, CREDIT_PATTERNS)
        val balanceCol   = bestMatch(headers, BALANCE_PATTERNS)
        val refCol       = bestMatch(headers, REFERENCE_PATTERNS)
        val amountCol    = bestMatch(headers, AMOUNT_PATTERNS)
        val directionCol = bestMatch(headers, DIRECTION_PATTERNS)
        val categoryCol  = bestMatch(headers, CATEGORY_PATTERNS)

        // Minimum: date + description + at least one amount column
        if (dateCol < 0 || descCol < 0) return null
        val hasAmounts = debitCol >= 0 || creditCol >= 0 || amountCol >= 0
        if (!hasAmounts) return null

        return ColumnMapping(
            skipRows       = 1,
            dateCol        = dateCol,
            descriptionCol = descCol,
            debitCol       = debitCol,
            creditCol      = creditCol,
            amountCol      = amountCol,
            directionCol   = directionCol,
            balanceCol     = balanceCol,
            referenceCol   = refCol,
            categoryCol    = categoryCol,
            // Try all common Indian bank date formats — order matters (most specific first)
            dateFormats    = listOf(
                "dd/MM/yyyy", "dd-MM-yyyy", "dd/MM/yy",  "dd-MM-yy",
                "dd MMM yyyy", "dd MMM yy", "yyyy-MM-dd", "MM/dd/yyyy",
                "d/M/yyyy",    "d-M-yyyy",   "d MMM yyyy"
            )
        )
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Score each header against the patterns and return the index of the
     * best-matching column. Returns -1 if no column matches any pattern.
     *
     * Scoring:
     *   Pattern[0] match → 3 points (most specific)
     *   Pattern[1] match → 2 points
     *   Pattern[2] match → 1 point
     */
    private fun bestMatch(headers: List<String>, patterns: List<Regex>): Int {
        var bestIndex = -1
        var bestScore = 0

        headers.forEachIndexed { index, header ->
            val h = header.trim()
            if (h.isEmpty()) return@forEachIndexed
            patterns.forEachIndexed { patternIndex, pattern ->
                if (pattern.containsMatchIn(h)) {
                    // Higher pattern index = lower score (patterns ordered specific → broad)
                    val score = patterns.size - patternIndex
                    if (score > bestScore) {
                        bestScore = score
                        bestIndex = index
                    }
                    return@forEachIndexed // first match for this pattern level is enough
                }
            }
        }
        return bestIndex
    }
}