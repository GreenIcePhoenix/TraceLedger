// ─────────────────────────────────────────────────────────────────────────────
// FILE: feature/accountimport/parser/SpreadsheetParser.kt
//
// Handles ALL spreadsheet formats:
//   .xlsx        — unencrypted Excel (OpenXML)
//   .xls         — legacy Excel 97-2003 binary format
//   .xlsx (pw)   — password-protected Excel
//   .xls  (pw)   — password-protected legacy Excel
//
// Uses Apache POI's WorkbookFactory which auto-detects the file type.
// Column detection uses FuzzyColumnMapper — no hardcoded bank layouts.
//
// v1.5.0 improvements:
//   - Multi-sheet scan: picks the sheet that contains a valid transaction header
//   - Leading blank column offset: handles banks (ICICI) with blank column A
//   - 2-row merged header fallback: for banks with merged cell headers
//   - Header validation: minimum 4 non-blank columns + amount cols required
//     to reject preamble rows (e.g. "Transaction Date from | 01/03/2026")
//     that partially match FuzzyColumnMapper but are NOT real headers
//
// DEPENDENCIES (already in libs.versions.toml and build.gradle.kts):
//   implementation(libs.poi.core)
//   implementation(libs.poi.ooxml)
// ─────────────────────────────────────────────────────────────────────────────
package com.greenicephoenix.traceledger.feature.accountimport.parser

import android.content.Context
import android.net.Uri
import com.greenicephoenix.traceledger.feature.accountimport.model.ParsedTransaction
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.math.floor

// ── Result type ───────────────────────────────────────────────────────────────

sealed class SpreadsheetParseResult {
    data class Success(val transactions: List<ParsedTransaction>) : SpreadsheetParseResult()
    data class Error(val message: String) : SpreadsheetParseResult()
    object NeedsPassword : SpreadsheetParseResult()
}

// ── Parser ────────────────────────────────────────────────────────────────────

object SpreadsheetParser {

    /**
     * Parse a spreadsheet file (.xls or .xlsx, encrypted or not).
     *
     * @param context   For ContentResolver URI access.
     * @param uri       Content URI of the file.
     * @param password  Optional. Pass null first. If result is NeedsPassword,
     *                  prompt user and retry with the entered password.
     */
    fun parse(
        context:  Context,
        uri:      Uri,
        password: String? = null
    ): SpreadsheetParseResult {

        val inputStream = context.contentResolver.openInputStream(uri)
            ?: return SpreadsheetParseResult.Error("Cannot open file.")

        // WorkbookFactory auto-detects XLS vs XLSX and handles encryption.
        val workbook = try {
            if (!password.isNullOrEmpty()) {
                WorkbookFactory.create(inputStream, password)
            } else {
                WorkbookFactory.create(inputStream)
            }
        } catch (e: Exception) {
            return when {
                isEncryptionError(e) -> SpreadsheetParseResult.NeedsPassword
                else                 -> SpreadsheetParseResult.Error(
                    "Cannot read spreadsheet: ${e.message ?: "unknown error"}.\n\n" +
                            "Make sure the file is a valid Excel file downloaded from netbanking."
                )
            }
        }

        return try {
            // ── Multi-sheet scan ─────────────────────────────────────────────
            // Some banks put transactions on sheet 2+. Scan all sheets and
            // pick the first one that contains a valid transaction header.
            // Falls back to sheet 0 if no sheet passes the header check.
            val sheet = run {
                var bestSheet = workbook.getSheetAt(0)
                for (i in 0 until workbook.numberOfSheets) {
                    val candidate = workbook.getSheetAt(i)
                    if (candidate.lastRowNum <= 0) continue

                    // Extract first 20 rows and check if any row is a valid header
                    val sampleRows = (0..minOf(candidate.lastRowNum, 20)).mapNotNull { rowIdx ->
                        val row    = candidate.getRow(rowIdx) ?: return@mapNotNull null
                        val maxCol = row.lastCellNum.toInt().coerceAtLeast(1)
                        (0 until maxCol).map { col -> cellToString(row.getCell(col)) }
                            .takeIf { cells -> cells.any { it.isNotBlank() } }
                    }

                    // Use the same header validation as findHeaderAndMapping
                    val hasValidHeader = sampleRows.any { row ->
                        val leading = row.indexOfFirst { it.isNotBlank() }.coerceAtLeast(0)
                        val trimmed = row.drop(leading)
                        val m = FuzzyColumnMapper.detectColumns(trimmed) ?: return@any false
                        isStrongHeaderMapping(trimmed, m)
                    }

                    if (hasValidHeader) {
                        bestSheet = candidate
                        break
                    }
                }
                bestSheet
            }

            // ── Extract all rows as List<List<String>> ───────────────────────
            val rows = (0..sheet.lastRowNum).mapNotNull { rowIdx ->
                val row    = sheet.getRow(rowIdx) ?: return@mapNotNull null
                val maxCol = row.lastCellNum.toInt().coerceAtLeast(1)
                val cells  = (0 until maxCol).map { col -> cellToString(row.getCell(col)) }
                if (cells.all { it.isBlank() }) null else cells
            }

            workbook.close()

            if (rows.isEmpty()) {
                return SpreadsheetParseResult.Error("No data found in this spreadsheet.")
            }

            // ── Find header + column mapping ─────────────────────────────────
            val (headerRowIdx, mapping) = findHeaderAndMapping(rows)
                ?: return SpreadsheetParseResult.Error(
                    "Could not identify the column structure.\n\n" +
                            "Make sure this is a transaction statement (not an account summary) " +
                            "downloaded from your bank's netbanking portal.\n\n" +
                            "First row found: ${rows.firstOrNull()?.take(5)?.joinToString(", ")}"
                )

            val transactions = rows.drop(headerRowIdx + 1)
                .filter { row -> row.any { it.isNotBlank() } }
                .mapNotNull { row -> parseRow(row, mapping) }

            SpreadsheetParseResult.Success(transactions)

        } catch (e: Exception) {
            try { workbook.close() } catch (_: Exception) {}
            SpreadsheetParseResult.Error("Error parsing spreadsheet: ${e.message}")
        }
    }

    // ── Header detection ──────────────────────────────────────────────────────

    /**
     * Validate that a FuzzyColumnMapper result is a REAL transaction header
     * and not a preamble key-value row that partially matches.
     *
     * Requirements:
     *   1. At least 4 non-blank columns in the trimmed row
     *   2. Must have BOTH debit + credit columns, OR a single amount column
     *
     * This rejects preamble rows like:
     *   ["Transaction Date from", "01/03/2026", "to", "01/06/2026"]
     * which have a date match and description match but no amount columns.
     */
    private fun isStrongHeaderMapping(trimmedRow: List<String>, mapping: ColumnMapping): Boolean {
        val nonBlankCount = trimmedRow.count { it.isNotBlank() }
        val hasAmountCols = (mapping.debitCol >= 0 && mapping.creditCol >= 0) ||
                mapping.amountCol >= 0
        return nonBlankCount >= 4 && hasAmountCols
    }

    /**
     * Scan up to 20 rows to find the transaction header and build a ColumnMapping.
     *
     * Pass 1 — single-row headers (most banks)
     * Pass 2 — 2-row merged headers (some banks use merged cells spanning 2 rows)
     *
     * For each candidate row:
     *   - Strip leading blank columns (handles ICICI blank column A)
     *   - Run FuzzyColumnMapper
     *   - Validate with isStrongHeaderMapping (rejects preamble rows)
     *   - Offset all column indices back by leadingBlanks
     */
    private fun findHeaderAndMapping(rows: List<List<String>>): Pair<Int, ColumnMapping>? {

        // ── Pass 1: single-row header ─────────────────────────────────────────
        for (i in 0 until minOf(rows.size, 20)) {
            val row          = rows[i]
            val leadingBlanks = row.indexOfFirst { it.isNotBlank() }.coerceAtLeast(0)
            val trimmedRow   = row.drop(leadingBlanks)

            val mapping = FuzzyColumnMapper.detectColumns(trimmedRow) ?: continue

            // Reject preamble rows that partially match (e.g. "Transaction Date from | 01/03/2026")
            if (!isStrongHeaderMapping(trimmedRow, mapping)) continue

            val offsetMapping = applyOffset(mapping, leadingBlanks)
            return i to offsetMapping
        }

        // ── Pass 2: 2-row merged header ───────────────────────────────────────
        // Some banks use merged cells; POI reads merged cells as blank in the
        // child rows. Combining adjacent rows reconstructs the full header.
        for (i in 0 until minOf(rows.size - 1, 20)) {
            val combined = rows[i].zip(rows[i + 1]) { a, b ->
                listOf(a, b).filter { it.isNotBlank() }.joinToString(" ")
            }
            val leading  = combined.indexOfFirst { it.isNotBlank() }.coerceAtLeast(0)
            val trimmed  = combined.drop(leading)
            val mapping  = FuzzyColumnMapper.detectColumns(trimmed) ?: continue

            if (!isStrongHeaderMapping(trimmed, mapping)) continue

            val offsetMapping = applyOffset(mapping, leading)
            // +1 because we consumed 2 rows for the header — data starts after row i+1
            return (i + 1) to offsetMapping
        }

        return null
    }

    /**
     * Offset all column indices in a ColumnMapping by [offset].
     * This corrects for leading blank columns that were stripped before
     * FuzzyColumnMapper ran.
     */
    private fun applyOffset(mapping: ColumnMapping, offset: Int): ColumnMapping {
        if (offset == 0) return mapping
        fun shift(col: Int) = if (col >= 0) col + offset else -1
        return mapping.copy(
            skipRows       = 1,
            dateCol        = shift(mapping.dateCol),
            descriptionCol = shift(mapping.descriptionCol),
            debitCol       = shift(mapping.debitCol),
            creditCol      = shift(mapping.creditCol),
            amountCol      = shift(mapping.amountCol),
            directionCol   = shift(mapping.directionCol),
            balanceCol     = shift(mapping.balanceCol),
            referenceCol   = shift(mapping.referenceCol),
            categoryCol    = shift(mapping.categoryCol)
        )
    }

    // ── Cell → String ─────────────────────────────────────────────────────────

    /**
     * Convert any POI cell to a clean string representation.
     * Handles date cells specially — returns "DD/MM/YYYY" format.
     * Preserves decimal values for amount cells (e.g. "0.00", "30.00").
     */
    private fun cellToString(cell: org.apache.poi.ss.usermodel.Cell?): String {
        if (cell == null) return ""
        return when (cell.cellType) {
            CellType.STRING  -> cell.stringCellValue.trim()

            CellType.NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    // Excel stores dates as numbers — convert to LocalDate
                    try {
                        val date = cell.localDateTimeCellValue
                            ?.toLocalDate()
                            ?: cell.dateCellValue.toInstant()
                                .atZone(ZoneId.systemDefault()).toLocalDate()
                        date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    } catch (e: Exception) {
                        cell.numericCellValue.toLong().toString()
                    }
                } else {
                    val num = cell.numericCellValue
                    // Preserve decimals for amount cells — "0.00" must stay "0.00"
                    // so parseAmount can correctly identify zero-value cells.
                    // Only strip ".0" for truly whole numbers that are clearly not amounts.
                    if (num == floor(num) && !num.isInfinite() && num >= 1000) {
                        // Large whole numbers (serial numbers, account numbers) — strip .0
                        num.toLong().toString()
                    } else {
                        // Small numbers or decimals — preserve as-is for amount parsing
                        num.toBigDecimal().stripTrailingZeros().toPlainString()
                            .let { if (it == "0") "0.00" else it }
                    }
                }
            }

            CellType.FORMULA -> {
                try {
                    val num = cell.numericCellValue
                    if (num == floor(num) && !num.isInfinite()) num.toLong().toString()
                    else num.toString()
                } catch (e: Exception) {
                    try { cell.stringCellValue.trim() }
                    catch (e2: Exception) { "" }
                }
            }

            CellType.BOOLEAN -> cell.booleanCellValue.toString()
            CellType.BLANK   -> ""
            else             -> cell.toString().trim()
        }
    }

    // ── Row → ParsedTransaction ───────────────────────────────────────────────

    private fun parseRow(cols: List<String>, mapping: ColumnMapping): ParsedTransaction? {
        val requiredCols = listOf(
            mapping.dateCol,
            mapping.descriptionCol,
            mapping.debitCol.coerceAtLeast(0),
            mapping.creditCol.coerceAtLeast(0),
            mapping.amountCol.coerceAtLeast(0)
        ).max() + 1
        if (cols.size < requiredCols) return null

        val rawDate     = cols.getOrElse(mapping.dateCol)        { "" }.trim()
        val description = cols.getOrElse(mapping.descriptionCol) { "" }.trim()
        val referenceNo = if (mapping.referenceCol >= 0)
            cols.getOrElse(mapping.referenceCol) { "" }.trim().ifEmpty { null }
        else null

        // Skip footer/summary rows
        val lower = description.lowercase()
        if ("opening balance" in lower || "closing balance" in lower ||
            lower.trim() == "total"    || "balance b/f" in lower    ||
            lower.trim() in setOf("narration", "particulars", "description", "details")) {
            return null
        }

        // Skip rows with no meaningful description (S No. rows, blank rows)
        if (description.isBlank() || description.matches(Regex("\\d+"))) return null

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

        return ParsedTransaction(
            rawDate     = rawDate,
            date        = parseDate(rawDate, mapping.dateFormats),
            description = description,
            amount      = amount,
            isCredit    = isCredit,
            balance     = balance,
            referenceNo = referenceNo
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Parse an amount string, returning null for zero or unparseable values.
     * Handles currency symbols (₹, $, €, £, ¥), commas, spaces.
     * Treats "0", "0.00", "-" as null (no transaction).
     */
    private fun parseAmount(raw: String): BigDecimal? {
        val cleaned = raw
            .replace(Regex("[₹$€£¥,\\s]"), "")
            .replace("(", "-")
            .replace(")", "")
            .trim()
        if (cleaned.isEmpty() || cleaned == "-") return null
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
     * Check if an exception indicates an encrypted/password-protected file.
     * Apache POI uses several different exception types across versions.
     */
    private fun isEncryptionError(e: Exception): Boolean {
        val name = e.javaClass.name + " " + (e.javaClass.simpleName)
        val msg  = e.message ?: ""
        return "EncryptedDocument"          in name ||
                "InvalidPassword"            in name ||
                "org.apache.poi.poifs.crypt" in name ||
                "password"                   in msg.lowercase() ||
                "encrypted"                  in msg.lowercase() ||
                "decrypt"                    in msg.lowercase()
    }
}