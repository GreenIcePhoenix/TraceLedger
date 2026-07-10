// ─────────────────────────────────────────────────────────────────────────────
// FILE: feature/accountimport/parser/CsvStatementParser.kt
//
// Tier 1 improvements (v1.5.0):
//   - Encoding auto-detection: UTF-16 LE/BE, UTF-8 BOM, Windows-1252 fallback
//   - Delimiter auto-detection: comma, tab, semicolon, pipe
//   - Header row scan: up to 50 rows (handles preamble-heavy bank CSVs)
//   - Known bank header scan: finds actual header row for hardcoded-mapping banks
//   - New Indian bank mappings: Federal, Canara, PNB, Bank of Baroda, IDFC First
//
// Design principles (unchanged):
//   - Never crashes on bad data — every row is wrapped in try/catch
//   - Amount parsing strips currency symbols, commas, spaces
//   - Date parsing tries multiple formats
// ─────────────────────────────────────────────────────────────────────────────
package com.greenicephoenix.traceledger.feature.accountimport.parser

import android.content.Context
import android.net.Uri
import com.greenicephoenix.traceledger.feature.accountimport.model.BankFormat
import com.greenicephoenix.traceledger.feature.accountimport.model.ParsedTransaction
import java.math.BigDecimal
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

// ── Column mapping data class ─────────────────────────────────────────────────

/**
 * Describes which column index in a bank's CSV holds each piece of data.
 * Indices are 0-based. Use -1 to indicate "this column doesn't exist".
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
    val dateFormats:    List<String> = listOf(
        "dd/MM/yyyy", "dd-MM-yyyy", "dd/MM/yy", "dd-MM-yy",
        "dd MMM yyyy", "dd MMM yy", "yyyy-MM-dd", "MM/dd/yyyy",
        "d/M/yyyy", "d-M-yyyy", "d MMM yyyy"
    )
)

// ── Bank column mappings ──────────────────────────────────────────────────────

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

    // Federal Bank CSV
    // Header: Transaction Date, Particulars, Cheque No, Debit, Credit, Balance
    BankFormat.FEDERAL_CSV to ColumnMapping(
        skipRows       = 1,
        dateCol        = 0,
        descriptionCol = 1,
        referenceCol   = 2,
        debitCol       = 3,
        creditCol      = 4,
        balanceCol     = 5,
        dateFormats    = listOf("dd/MM/yyyy", "dd-MM-yyyy", "dd MMM yyyy")
    ),

    // Canara Bank CSV
    // Header: Sl No, Transaction Date, Value Date, Description, Ref No, Debit, Credit, Balance
    BankFormat.CANARA_CSV to ColumnMapping(
        skipRows       = 1,
        dateCol        = 1,
        descriptionCol = 3,
        referenceCol   = 4,
        debitCol       = 5,
        creditCol      = 6,
        balanceCol     = 7,
        dateFormats    = listOf("dd/MM/yyyy", "dd-MM-yyyy", "dd MMM yyyy")
    ),

    // PNB (Punjab National Bank) CSV
    // Header: Date, Particulars, Debit, Credit, Balance
    BankFormat.PNB_CSV to ColumnMapping(
        skipRows       = 1,
        dateCol        = 0,
        descriptionCol = 1,
        debitCol       = 2,
        creditCol      = 3,
        balanceCol     = 4,
        dateFormats    = listOf("dd/MM/yyyy", "dd-MM-yyyy", "dd MMM yyyy")
    ),

    // Bank of Baroda CSV
    // Header: Txn Date, Narration, Ref Num, Value Date, Withdrawal Amt, Deposit Amt, Closing Balance
    BankFormat.BOB_CSV to ColumnMapping(
        skipRows       = 1,
        dateCol        = 0,
        descriptionCol = 1,
        referenceCol   = 2,
        debitCol       = 4,
        creditCol      = 5,
        balanceCol     = 6,
        dateFormats    = listOf("dd/MM/yyyy", "dd-MM-yyyy", "dd MMM yyyy")
    ),

    // IDFC First Bank CSV
    // Header: Date, Transaction Details, Ref No, Debit, Credit, Balance
    BankFormat.IDFC_CSV to ColumnMapping(
        skipRows       = 1,
        dateCol        = 0,
        descriptionCol = 1,
        referenceCol   = 2,
        debitCol       = 3,
        creditCol      = 4,
        balanceCol     = 5,
        dateFormats    = listOf("dd/MM/yyyy", "dd-MM-yyyy", "dd MMM yyyy")
    ),
)

// ── Parser ────────────────────────────────────────────────────────────────────

object CsvStatementParser {

    /**
     * Parse a CSV/TSV file into a list of [ParsedTransaction].
     *
     * Tier 1 improvements:
     *  - Auto-detects file encoding (UTF-16, UTF-8 BOM, Windows-1252 fallback)
     *  - Auto-detects delimiter (comma, tab, semicolon, pipe)
     *  - Scans up to 50 rows for the actual header (handles preamble blocks)
     */
    fun parse(
        context: Context,
        uri:     Uri,
        format:  BankFormat
    ): List<ParsedTransaction> {

        // ── Step 1: Read all bytes and detect encoding ────────────────────────
        val bytes = try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return emptyList()
        } catch (e: Exception) {
            return emptyList()
        }

        val charset  = detectEncoding(bytes)
        val rawText  = bytes.toString(charset)
        val allLines = rawText.lines().filter { it.isNotBlank() }

        if (allLines.isEmpty()) return emptyList()

        // ── Step 2: Detect delimiter ──────────────────────────────────────────
        val delimiter = detectDelimiter(allLines.take(10))

        // ── Step 3: Get column mapping ────────────────────────────────────────
        val mapping = if (format == BankFormat.GENERIC_CSV) {
            // Unknown bank — use FuzzyColumnMapper to auto-detect columns
            findHeaderRowGeneric(allLines, delimiter)?.let { (idx, m) ->
                m.copy(skipRows = idx + 1)
            } ?: return emptyList()
        } else {
            // Known bank — use hardcoded mapping but find actual header row
            // to handle preamble rows that appear before the header
            val knownMapping    = MAPPINGS[format] ?: return emptyList()
            val actualHeaderIdx = findKnownFormatHeaderRow(allLines, format, delimiter)
            knownMapping.copy(skipRows = (actualHeaderIdx ?: 0) + 1)
        }

        // ── Step 4: Parse data rows ───────────────────────────────────────────
        return allLines.drop(mapping.skipRows)
            .filter { it.isNotBlank() }
            .mapNotNull { line -> parseRow(line, mapping, delimiter) }
    }

    // ── Encoding detection ────────────────────────────────────────────────────

    /**
     * Detect file encoding from BOM bytes.
     * Falls back to UTF-8 then Windows-1252 if no BOM found.
     */
    private fun detectEncoding(bytes: ByteArray): Charset {
        if (bytes.size >= 2) {
            // UTF-16 Little Endian BOM: FF FE
            if (bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte())
                return Charsets.UTF_16LE
            // UTF-16 Big Endian BOM: FE FF
            if (bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte())
                return Charsets.UTF_16BE
        }
        if (bytes.size >= 3) {
            // UTF-8 BOM: EF BB BF
            if (bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte())
                return Charsets.UTF_8
        }
        // No BOM — try UTF-8; if it produces replacement characters fall back to Windows-1252
        val utf8Text = bytes.toString(Charsets.UTF_8)
        return if (utf8Text.contains('\uFFFD')) {
            // Replacement character found — likely Windows-1252 (common in Indian bank exports)
            charset("windows-1252")
        } else {
            Charsets.UTF_8
        }
    }

    // ── Delimiter detection ───────────────────────────────────────────────────

    /**
     * Detect the delimiter used in the file by counting candidate characters
     * across the first few lines. The delimiter with the most consistent
     * non-zero count wins.
     */
    private fun detectDelimiter(sampleLines: List<String>): Char {
        val candidates = listOf(',', '\t', ';', '|')
        val scores = candidates.associateWith { delim ->
            val counts = sampleLines.map { line -> line.count { it == delim } }
            val nonZero = counts.filter { it > 0 }
            // Score = consistency (low variance) × presence (non-zero count)
            if (nonZero.isEmpty()) 0.0
            else nonZero.average() * nonZero.size.toDouble() / sampleLines.size
        }
        return scores.maxByOrNull { it.value }?.key ?: ','
    }

    // ── Header row detection ──────────────────────────────────────────────────

    /**
     * For GENERIC_CSV: scan up to 50 rows and use FuzzyColumnMapper to find
     * the first row that looks like a column header.
     */
    private fun findHeaderRowGeneric(
        lines:     List<String>,
        delimiter: Char
    ): Pair<Int, ColumnMapping>? {
        for (i in 0 until minOf(lines.size, 50)) {
            val cols    = splitLine(lines[i], delimiter)
            // Strip leading blank columns (same as SpreadsheetParser)
            val leading = cols.indexOfFirst { it.isNotBlank() }.coerceAtLeast(0)
            val trimmed = cols.drop(leading)
            val mapping = FuzzyColumnMapper.detectColumns(trimmed) ?: continue
            // Offset column indices back
            val offset  = mapping.copy(
                dateCol        = if (mapping.dateCol        >= 0) mapping.dateCol        + leading else -1,
                descriptionCol = if (mapping.descriptionCol >= 0) mapping.descriptionCol + leading else -1,
                debitCol       = if (mapping.debitCol       >= 0) mapping.debitCol       + leading else -1,
                creditCol      = if (mapping.creditCol      >= 0) mapping.creditCol      + leading else -1,
                amountCol      = if (mapping.amountCol      >= 0) mapping.amountCol      + leading else -1,
                directionCol   = if (mapping.directionCol   >= 0) mapping.directionCol   + leading else -1,
                balanceCol     = if (mapping.balanceCol     >= 0) mapping.balanceCol     + leading else -1,
                referenceCol   = if (mapping.referenceCol   >= 0) mapping.referenceCol   + leading else -1,
                categoryCol    = if (mapping.categoryCol    >= 0) mapping.categoryCol    + leading else -1
            )
            return i to offset
        }
        return null
    }

    /**
     * For known bank formats: find the actual header row by looking for the
     * bank-specific fingerprint. Handles preamble rows before the header.
     * Returns the row index of the header, or null if not found (use row 0).
     */
    private fun findKnownFormatHeaderRow(
        lines:     List<String>,
        format:    BankFormat,
        delimiter: Char
    ): Int? {
        val fingerprint = when (format) {
            BankFormat.HDFC_CSV     -> "narration"
            BankFormat.ICICI_CSV    -> "transaction date"
            BankFormat.SBI_CSV      -> "txn date"
            BankFormat.AXIS_CSV     -> "particulars"
            BankFormat.KOTAK_CSV    -> "withdrawal"
            BankFormat.YES_CSV      -> "withdrawal amt"
            BankFormat.INDUSIND_CSV -> "dr/cr"
            BankFormat.FEDERAL_CSV  -> "particulars"
            BankFormat.CANARA_CSV   -> "description"
            BankFormat.PNB_CSV      -> "particulars"
            BankFormat.BOB_CSV      -> "narration"
            BankFormat.IDFC_CSV     -> "transaction details"
            else                    -> return null
        }
        for (i in 0 until minOf(lines.size, 50)) {
            if (lines[i].lowercase().contains(fingerprint)) return i
        }
        return null
    }

    // ── Row parsing ───────────────────────────────────────────────────────────

    private fun parseRow(
        line:      String,
        mapping:   ColumnMapping,
        delimiter: Char
    ): ParsedTransaction? {
        val cols = splitLine(line, delimiter)

        val requiredCols = maxOf(
            mapping.dateCol,
            mapping.descriptionCol,
            mapping.debitCol.coerceAtLeast(0),
            mapping.creditCol.coerceAtLeast(0),
            mapping.amountCol.coerceAtLeast(0)
        ) + 1
        if (cols.size < requiredCols) return null

        val rawDate     = cols.getOrElse(mapping.dateCol)        { "" }.trim()
        val description = cols.getOrElse(mapping.descriptionCol) { "" }.trim()
        val referenceNo = if (mapping.referenceCol >= 0)
            cols.getOrElse(mapping.referenceCol) { "" }.trim().ifEmpty { null }
        else null

        // Skip summary/footer rows
        val lower = description.lowercase()
        if ("opening balance" in lower || "closing balance" in lower ||
            lower.trim() == "total"    || "balance b/f" in lower    ||
            lower.trim() in setOf("narration", "particulars", "description", "details")) {
            return null
        }

        // Amount + direction
        val (amount, isCredit) = if (mapping.amountCol >= 0 && mapping.directionCol >= 0) {
            // Single amount column with explicit DR/CR direction column (e.g. IndusInd)
            val rawAmount = cols.getOrElse(mapping.amountCol)    { "" }
            val direction = cols.getOrElse(mapping.directionCol) { "Dr" }.trim().lowercase()
            val parsed    = parseAmount(rawAmount) ?: return null
            parsed to direction.startsWith("cr")
        } else {
            // Separate debit/credit columns (most banks including ICICI)
            val debit  = parseAmount(cols.getOrElse(if (mapping.debitCol  >= 0) mapping.debitCol  else mapping.amountCol) { "" })
            val credit = parseAmount(cols.getOrElse(mapping.creditCol) { "" })
            when {
                debit  != null && debit  > BigDecimal.ZERO -> debit  to false
                credit != null && credit > BigDecimal.ZERO -> credit to true
                else -> return null
            }
        }

        val balance = if (mapping.balanceCol >= 0)
            parseAmount(cols.getOrElse(mapping.balanceCol) { "" })
        else null

        val importedCategoryName = if (mapping.categoryCol >= 0)
            cols.getOrElse(mapping.categoryCol) { "" }.trim().ifEmpty { null }
        else null

        return ParsedTransaction(
            rawDate              = rawDate,
            date                 = parseDate(rawDate, mapping.dateFormats),
            description          = description,
            amount               = amount,
            isCredit             = isCredit,
            balance              = balance,
            referenceNo          = referenceNo,
            importedCategoryName = importedCategoryName
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun parseAmount(raw: String): BigDecimal? {
        val cleaned = raw.replace(Regex("[₹$€£¥,\\s]"), "").trim()
        if (cleaned.isEmpty() || cleaned == "-" || cleaned == "0.00" || cleaned == "0") return null
        return try {
            val value = BigDecimal(cleaned).abs()
            if (value.compareTo(BigDecimal.ZERO) == 0) null else value
        } catch (e: NumberFormatException) { null }
    }

    private fun parseDate(raw: String, formats: List<String>): LocalDate? {
        val cleaned = raw.trim()
        if (cleaned.isEmpty()) return null
        for (fmt in formats) {
            try { return LocalDate.parse(cleaned, DateTimeFormatter.ofPattern(fmt)) }
            catch (e: DateTimeParseException) { /* try next */ }
        }
        return null
    }

    /**
     * Split a delimited line into columns, respecting quoted fields.
     * Handles: comma, tab, semicolon, pipe delimiters.
     * RFC 4180 compliant for quoted fields containing the delimiter.
     */
    private fun splitLine(line: String, delimiter: Char): List<String> {
        val result   = mutableListOf<String>()
        val current  = StringBuilder()
        var inQuotes = false
        var i        = 0

        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && !inQuotes -> inQuotes = true
                c == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"'); i++   // escaped quote "" → "
                }
                c == '"' && inQuotes  -> inQuotes = false
                c == delimiter && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        result.add(current.toString())
        return result
    }
}