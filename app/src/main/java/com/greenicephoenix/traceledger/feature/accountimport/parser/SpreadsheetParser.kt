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
            val sheet = workbook.getSheetAt(0)

            // Extract all rows as List<List<String>>
            val rows = (0..sheet.lastRowNum).mapNotNull { rowIdx ->
                val row = sheet.getRow(rowIdx) ?: return@mapNotNull null
                val maxCol = row.lastCellNum.toInt().coerceAtLeast(1)
                val cells = (0 until maxCol).map { col ->
                    cellToString(row.getCell(col))
                }
                if (cells.all { it.isBlank() }) null else cells
            }

            workbook.close()

            if (rows.isEmpty()) {
                return SpreadsheetParseResult.Error("No data found in this spreadsheet.")
            }

            // Find the header row — scan first 5 rows for one that matches column keywords
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
     * Scan the first few rows to find the header row and build a column mapping.
     * Returns (headerRowIndex, ColumnMapping) or null if not found.
     */
    private fun findHeaderAndMapping(rows: List<List<String>>): Pair<Int, ColumnMapping>? {
        for (i in 0 until minOf(rows.size, 8)) {
            val mapping = FuzzyColumnMapper.detectColumns(rows[i])
            if (mapping != null) return i to mapping.copy(skipRows = 1)
        }
        return null
    }

    // ── Cell → String ─────────────────────────────────────────────────────────

    /**
     * Convert any POI cell to a clean string representation.
     * Handles date cells specially — returns "DD/MM/YYYY" format.
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
                    // Strip ".0" from whole numbers: 15000.0 → "15000"
                    if (num == floor(num) && !num.isInfinite()) num.toLong().toString()
                    else num.toString()
                }
            }

            CellType.FORMULA -> {
                // Evaluate formula result
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

        // Skip footer/header text rows
        val lower = description.lowercase()
        if ("opening balance" in lower || "closing balance" in lower ||
            lower.trim() == "total"    || "balance b/f" in lower    ||
            lower.trim() in setOf("narration", "particulars", "description", "details")) {
            return null
        }

        // Amount + direction
        val (amount, isCredit) = if (mapping.amountCol >= 0) {
            val rawAmount = cols.getOrElse(mapping.amountCol)    { "" }
            val direction = cols.getOrElse(mapping.directionCol) { "Dr" }.trim().lowercase()
            val parsed    = parseAmount(rawAmount) ?: return null
            parsed to direction.startsWith("cr")
        } else {
            val debit  = parseAmount(cols.getOrElse(mapping.debitCol)  { "" })
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

    // ── Shared helpers ────────────────────────────────────────────────────────

    private fun parseAmount(raw: String): BigDecimal? {
        val cleaned = raw.replace(Regex("[₹$€£¥,\\s]"), "").trim()
        if (cleaned.isEmpty() || cleaned == "-") return null
        return try { BigDecimal(cleaned).abs() } catch (e: NumberFormatException) { null }
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
        return "EncryptedDocument"   in name  ||
                "InvalidPassword"     in name  ||
                "org.apache.poi.poifs.crypt" in name ||
                "password"            in msg.lowercase() ||
                "encrypted"           in msg.lowercase() ||
                "decrypt"             in msg.lowercase()
    }
}