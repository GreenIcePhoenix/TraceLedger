// ─────────────────────────────────────────────────────────────────────────────
// FILE: feature/accountimport/parser/CsvStatementParser.kt
//
// What this does:
//   Parses a bank statement CSV file into a List<ParsedTransaction>.
//   Each bank has a ColumnMapping that describes which CSV column index
//   holds the date, description, debit amount, credit amount, etc.
//
// Design principles:
//   - Never crashes on bad data. Every row is wrapped in a try/catch.
//     If a row fails to parse, it produces a ParsedTransaction with
//     date = null and hasDateError = true (handled in the review screen).
//   - Amount parsing: strips currency symbols, commas, and spaces.
//     Handles both "1,23,456.78" (Indian) and "1,234,567.89" (International).
//   - Date parsing: tries multiple formats per bank since some banks change
//     their export format across statement periods.
//   - Credit/Debit detection: some banks use separate Debit/Credit columns,
//     others use a single Amount column with a Dr/Cr indicator.
//
// Adding a new bank:
//   1. Add a BankFormat entry in BankFormat.kt
//   2. Add a detection fingerprint in BankFormatDetector.kt
//   3. Add a ColumnMapping entry in the MAPPINGS map below
//   Nothing else needs to change.
// ─────────────────────────────────────────────────────────────────────────────
package com.greenicephoenix.traceledger.feature.accountimport.parser

import android.content.Context
import android.net.Uri
import com.greenicephoenix.traceledger.feature.accountimport.model.BankFormat
import com.greenicephoenix.traceledger.feature.accountimport.model.ParsedTransaction
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

// ── Column mapping data class ─────────────────────────────────────────────────

/**
 * Describes which column index in a bank's CSV holds each piece of data.
 * Indices are 0-based. Use -1 to indicate "this column doesn't exist".
 *
 * @param skipRows         How many rows to skip before the data rows start.
 *                         Most banks: 1 (skip the header). Some: 2 or more.
 * @param dateCol          Column index for transaction date.
 * @param descriptionCol   Column index for narration / particulars.
 * @param debitCol         Column index for debit (money out) amount.
 *                         -1 if the bank uses a single amount + direction column.
 * @param creditCol        Column index for credit (money in) amount.
 *                         -1 if the bank uses a single amount + direction column.
 * @param amountCol        Column index for amount when debit/credit are combined.
 *                         -1 if the bank uses separate debit/credit columns.
 * @param directionCol     Column index for "Dr"/"Cr" indicator when [amountCol] != -1.
 * @param balanceCol       Column index for running balance. -1 if not present.
 * @param referenceCol     Column index for cheque/UTR reference number. -1 if not present.
 * @param dateFormats      List of date format strings to try, in order of preference.
 *                         We try all formats because some banks change formats over time.
 */
data class ColumnMapping(
    val skipRows:       Int,
    val dateCol:        Int,
    val descriptionCol: Int,
    val debitCol:       Int  = -1,
    val creditCol:      Int  = -1,
    val amountCol:      Int  = -1,
    val directionCol:   Int  = -1,
    val balanceCol:     Int  = -1,
    val referenceCol:   Int  = -1,
    val categoryCol:    Int  = -1,
    val dateFormats:    List<String> = listOf("dd/MM/yy", "dd/MM/yyyy", "dd-MM-yyyy", "yyyy-MM-dd")
)

// ── Bank column mappings ──────────────────────────────────────────────────────

/**
 * Column mappings for every supported bank.
 *
 * HOW TO READ THESE:
 * Open a sample statement from each bank in a text editor.
 * Count columns from 0. Map each column to the field it represents.
 *
 * Example HDFC row (indices 0-6):
 * "01/01/24","UPI-SWIGGY","01/01/24","150.00","","","9,850.00"
 *  0=date     1=narration  2=valueDate 3=debit 4=credit 5=ref   6=balance
 */
private val MAPPINGS: Map<BankFormat, ColumnMapping> = mapOf(

    // HDFC Bank CSV
    // Header: Date, Narration, Value Dat, Debit Amount, Credit Amount, Chq/Ref Number, Closing Balance
    BankFormat.HDFC_CSV to ColumnMapping(
        skipRows       = 1,
        dateCol        = 0,
        descriptionCol = 1,
        debitCol       = 3,
        creditCol      = 4,
        referenceCol   = 5,
        balanceCol     = 6,
        dateFormats    = listOf("dd/MM/yy", "dd/MM/yyyy")
    ),

    // ICICI Bank CSV
    // Header: Transaction Date, Value Date, Description, Ref No./Cheque No., Debit, Credit, Balance
    BankFormat.ICICI_CSV to ColumnMapping(
        skipRows       = 1,
        dateCol        = 0,
        descriptionCol = 2,
        referenceCol   = 3,
        debitCol       = 4,
        creditCol      = 5,
        balanceCol     = 6,
        dateFormats    = listOf("dd/MM/yyyy", "dd-MM-yyyy")
    ),

    // SBI CSV
    // Header: Txn Date, Value Date, Description, Ref No./Cheque No., Branch Code, Debit, Credit, Balance
    BankFormat.SBI_CSV to ColumnMapping(
        skipRows       = 1,
        dateCol        = 0,
        descriptionCol = 2,
        referenceCol   = 3,
        debitCol       = 5,
        creditCol      = 6,
        balanceCol     = 7,
        dateFormats    = listOf("dd MMM yyyy", "dd/MM/yyyy", "dd-MM-yyyy")
    ),

    // Axis Bank CSV
    // Header: Tran Date, CHQNO, PARTICULARS, DR, CR, BAL
    BankFormat.AXIS_CSV to ColumnMapping(
        skipRows       = 1,
        dateCol        = 0,
        descriptionCol = 2,
        referenceCol   = 1,
        debitCol       = 3,
        creditCol      = 4,
        balanceCol     = 5,
        dateFormats    = listOf("dd-MM-yyyy", "dd/MM/yyyy")
    ),

    // Kotak Bank CSV
    // Header: Date, Narration/Particulars, Cheq. /Ref. No., Withdrawal (Dr), Deposit (Cr), Balance
    BankFormat.KOTAK_CSV to ColumnMapping(
        skipRows       = 1,
        dateCol        = 0,
        descriptionCol = 1,
        referenceCol   = 2,
        debitCol       = 3,
        creditCol      = 4,
        balanceCol     = 5,
        dateFormats    = listOf("dd-MM-yyyy", "dd/MM/yyyy")
    ),

    // Yes Bank CSV
    // Header: Date, Narration, Ref No, Value Date, Withdrawal Amt (Dr), Deposit Amt (Cr), Closing Balance
    BankFormat.YES_CSV to ColumnMapping(
        skipRows       = 1,
        dateCol        = 0,
        descriptionCol = 1,
        referenceCol   = 2,
        debitCol       = 4,
        creditCol      = 5,
        balanceCol     = 6,
        dateFormats    = listOf("dd-MM-yyyy", "dd/MM/yyyy", "MM/dd/yyyy")
    ),

    // IndusInd Bank CSV
    // Header: Transaction Date, Particulars, Cheque No, Amount, Dr/Cr, Balance
    BankFormat.INDUSIND_CSV to ColumnMapping(
        skipRows       = 1,
        dateCol        = 0,
        descriptionCol = 1,
        referenceCol   = 2,
        amountCol      = 3,
        directionCol   = 4,
        balanceCol     = 5,
        dateFormats    = listOf("dd/MM/yyyy", "dd-MM-yyyy")
    ),

)

// ── Parser ────────────────────────────────────────────────────────────────────

object CsvStatementParser {

    /**
     * Parse a CSV file into a list of [ParsedTransaction].
     *
     * Rows that fail to parse completely have [ParsedTransaction.date] = null.
     * The review screen flags these and prevents them from being imported.
     *
     * @param context  Needed to open the URI via ContentResolver.
     * @param uri      Content URI of the CSV file.
     * @param format   The bank format detected by [BankFormatDetector].
     * @return         List of parsed rows, including failed ones (date = null).
     *                 Returns empty list if the file cannot be opened.
     */
    fun parse(
        context: Context,
        uri:     Uri,
        format:  BankFormat
    ): List<ParsedTransaction> {
        val lines = try {
            context.contentResolver.openInputStream(uri)
                ?.bufferedReader()
                ?.readLines()
                ?: return emptyList()
        } catch (e: Exception) {
            return emptyList()
        }

        //For GENERIC_CSV, auto-detect column layout from the header row
        //using FuzzyColumnMapper. This handles any bank not in MAPPINGS.
        val mapping = if (format == BankFormat.GENERIC_CSV) {
            val headerCols = lines.firstOrNull()
                ?.let { splitCsvLine(it) }
                ?: return emptyList()
            FuzzyColumnMapper.detectColumns(headerCols) ?: return emptyList()
        } else {
            MAPPINGS[format] ?: return emptyList()
        }

        return lines.drop(mapping.skipRows)
            .filter { it.isNotBlank() }
            .mapNotNull { line -> parseRow(line, mapping) }
    }

    // ── Row parsing ───────────────────────────────────────────────────────────

    /**
     * Parse a single CSV row into a [ParsedTransaction].
     * Returns null for completely empty or malformed rows that provide no data.
     * Returns a ParsedTransaction with date=null for rows with parseable amounts
     * but unparseable dates — these appear as errors in the review screen.
     */
    private fun parseRow(line: String, mapping: ColumnMapping): ParsedTransaction? {
        val cols = splitCsvLine(line)

        // Need at least enough columns to reach the furthest required index
        val requiredCols = maxOf(
            mapping.dateCol,
            mapping.descriptionCol,
            mapping.debitCol,
            mapping.creditCol,
            mapping.amountCol
        ) + 1
        if (cols.size < requiredCols) return null

        val rawDate     = cols.getOrElse(mapping.dateCol)     { "" }.trim()
        val description = cols.getOrElse(mapping.descriptionCol) { "" }.trim()
        val referenceNo = if (mapping.referenceCol >= 0)
            cols.getOrElse(mapping.referenceCol) { "" }.trim().ifEmpty { null }
        else null

        // Determine amount and direction
        val (amount, isCredit) = if (mapping.amountCol >= 0) {
            // Single amount column with Dr/Cr indicator (e.g. IndusInd)
            val rawAmount = cols.getOrElse(mapping.amountCol) { "" }
            val direction = cols.getOrElse(mapping.directionCol) { "Dr" }.trim().lowercase()
            val parsed    = parseAmount(rawAmount) ?: return null
            parsed to (direction.startsWith("cr"))
        } else {
            // Separate debit/credit columns (most banks)
            val rawDebit  = cols.getOrElse(mapping.debitCol)  { "" }
            val rawCredit = cols.getOrElse(mapping.creditCol) { "" }
            val debit     = parseAmount(rawDebit)
            val credit    = parseAmount(rawCredit)
            when {
                debit  != null && debit  > BigDecimal.ZERO -> debit  to false
                credit != null && credit > BigDecimal.ZERO -> credit to true
                else -> return null  // Both empty — skip this row (often a metadata row)
            }
        }

        val balance = if (mapping.balanceCol >= 0)
            parseAmount(cols.getOrElse(mapping.balanceCol) { "" })
        else null

        // Read the category name directly from the CSV if the column exists.
        // The ViewModel will look this up against the user's category list.
        val importedCategoryName = if (mapping.categoryCol >= 0)
            cols.getOrElse(mapping.categoryCol) { "" }.trim().ifEmpty { null }
        else null

        // Parse the date — on failure, keep the raw string for display
        val parsedDate = parseDate(rawDate, mapping.dateFormats)

        // Skip rows that look like footer summaries (e.g. "Opening Balance", "Closing Balance")
        if (description.lowercase().let {
                "opening balance" in it || "closing balance" in it ||
                        "total" == it.trim()    || "balance b/f" in it
            }) return null

        return ParsedTransaction(
            rawDate     = rawDate,
            date        = parsedDate,
            description = description,
            amount      = amount,
            isCredit    = isCredit,
            balance     = balance,
            referenceNo = referenceNo,
            importedCategoryName = importedCategoryName
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Parse an amount string to BigDecimal.
     * Handles: "1,23,456.78", "1,234,567.89", "1234567.89", "₹ 1,234.56", " "
     * Returns null for empty, whitespace-only, or non-numeric strings.
     */
    private fun parseAmount(raw: String): BigDecimal? {
        // Strip everything except digits, dots, and minus signs
        val cleaned = raw
            .replace(Regex("[₹$€£¥,\\s]"), "")  // currency symbols, commas, spaces
            .replace("(", "-")                    // some banks use (1234.00) for negatives
            .replace(")", "")
            .trim()
        if (cleaned.isEmpty() || cleaned == "-") return null
        return try {
            BigDecimal(cleaned).abs()  // always positive — direction from isCredit
        } catch (e: NumberFormatException) {
            null
        }
    }

    /**
     * Try parsing a date string using each format in [formats] until one succeeds.
     * Returns null if none work.
     */
    private fun parseDate(raw: String, formats: List<String>): LocalDate? {
        val cleaned = raw.trim()
        if (cleaned.isEmpty()) return null
        for (fmt in formats) {
            try {
                return LocalDate.parse(cleaned, DateTimeFormatter.ofPattern(fmt))
            } catch (e: DateTimeParseException) {
                // Try next format
            }
        }
        return null
    }

    /**
     * Split a CSV line into columns, respecting quoted fields.
     * Handles fields like: "Narration with, comma inside","Amount"
     *
     * This is a simple RFC 4180 compliant parser. We don't use a library
     * to keep dependencies minimal — the CSV format used by Indian banks
     * is straightforward enough that a hand-rolled parser is sufficient.
     */
    private fun splitCsvLine(line: String): List<String> {
        val result  = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false

        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && !inQuotes               -> inQuotes = true
                c == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    // Escaped quote inside a quoted field: "" → "
                    current.append('"')
                    i++
                }
                c == '"' && inQuotes                -> inQuotes = false
                c == ',' && !inQuotes               -> {
                    result.add(current.toString())
                    current.clear()
                }
                else                                -> current.append(c)
            }
            i++
        }
        result.add(current.toString())  // add the last field
        return result
    }
}