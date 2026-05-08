package com.greenicephoenix.traceledger.feature.accountimport.parser

import android.content.Context
import android.net.Uri
import com.greenicephoenix.traceledger.feature.accountimport.model.BankFormat
import com.greenicephoenix.traceledger.feature.accountimport.model.ParsedTransaction

sealed class ParseResult {
    data class Success(
        val transactions: List<ParsedTransaction>,
        val format:       BankFormat,
        val totalLines:   Int
    ) : ParseResult()

    data class Failure(val message: String, val canRetry: Boolean = true) : ParseResult()

    /**
     * File is password-protected. Show password dialog and retry.
     * [fileUri] is passed through so the ViewModel can retry without the
     * caller needing to re-pass it.
     */
    data class NeedsPassword(val fileUri: Uri) : ParseResult()
}

object StatementParserFactory {

    /**
     * Detect format, route to the correct parser, return a typed result.
     *
     * @param context   For ContentResolver access.
     * @param uri       Content URI from SAF file picker.
     * @param password  Null on first call. Pass user-entered value on retry
     *                  after receiving [ParseResult.NeedsPassword].
     */
    suspend fun parse(context: Context, uri: Uri, password: String? = null): ParseResult {

        val format = try {
            BankFormatDetector.detect(context, uri)
        } catch (e: Exception) {
            return ParseResult.Failure("Could not read file: ${e.message}")
        }

        return when {

            // ── Unknown — show informative error ──────────────────────────────
            format == BankFormat.UNKNOWN -> ParseResult.Failure(
                "Unsupported file format.\n\n" +
                        "Supported formats:\n" +
                        "• Excel (.xlsx / .xls) — any bank\n" +
                        "• PDF — any bank (text-based; password-protected supported)\n" +
                        "• CSV — HDFC, ICICI, SBI, Axis, Kotak, Yes Bank, IndusInd\n\n" +
                        "Tip: Download the Excel statement from your bank's netbanking portal."
            )

            // ── Spreadsheet (XLS / XLSX) → SpreadsheetParser (Apache POI) ────
            // Handles: .xls, .xlsx, password-protected versions of both.
            // Uses FuzzyColumnMapper — works for any bank worldwide.
            format.isSpreadsheet -> {
                when (val result = SpreadsheetParser.parse(context, uri, password)) {
                    is SpreadsheetParseResult.NeedsPassword -> ParseResult.NeedsPassword(uri)
                    is SpreadsheetParseResult.Error -> ParseResult.Failure(
                        "Could not read Excel file: ${result.message}\n\n" +
                                "Make sure this is a transaction statement downloaded from netbanking."
                    )
                    is SpreadsheetParseResult.Success -> {
                        if (result.transactions.isEmpty()) {
                            ParseResult.Failure(
                                "No transactions found in this Excel file.\n\n" +
                                        "The file may be a summary rather than a full statement. " +
                                        "Download the full transaction history from netbanking."
                            )
                        } else {
                            ParseResult.Success(result.transactions, format, result.transactions.size)
                        }
                    }
                }
            }

            // ── PDF → PdfStatementParser (PDFBox + UniversalPdfParser) ────────
            // Handles: any bank worldwide, text-based PDFs, password-protected.
            // NOTE: Scanned (image-only) PDFs are not supported without OCR.
            format.isPdf -> {
                when (val result = PdfStatementParser.parse(context, uri, password)) {
                    is PdfParseResult.NeedsPassword -> ParseResult.NeedsPassword(uri)
                    is PdfParseResult.Error -> ParseResult.Failure(result.message)
                    is PdfParseResult.Success -> {
                        if (result.transactions.isEmpty()) {
                            ParseResult.Failure(
                                "No transactions found in this PDF.\n\n" +
                                        "The PDF may be image-based (scanned). Try downloading an Excel statement."
                            )
                        } else {
                            ParseResult.Success(result.transactions, result.format, result.transactions.size)
                        }
                    }
                }
            }

            // ── CSV → CsvStatementParser (bank-specific column mappings) ──────
            // Bank fingerprinting already done in BankFormatDetector.
            // GENERIC_CSV uses FuzzyColumnMapper as a fallback.
            format.isCsv -> {
                val rows = try {
                    CsvStatementParser.parse(context, uri, format)
                } catch (e: Exception) {
                    return ParseResult.Failure("Error reading CSV: ${e.message}")
                }
                if (rows.isEmpty()) {
                    ParseResult.Failure(
                        "No transactions found in this CSV file.\n\n" +
                                "Make sure the file is a full transaction statement, not a monthly summary."
                    )
                } else {
                    ParseResult.Success(rows, format, rows.size)
                }
            }

            else -> ParseResult.Failure("Unexpected format: ${format.name}")
        }
    }
}