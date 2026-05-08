package com.greenicephoenix.traceledger.feature.accountimport.model

/**
 * File formats recognised by BankFormatDetector.
 *
 * XLSX / XLS → SpreadsheetParser (Apache POI, any bank via FuzzyColumnMapper)
 * PDF        → PdfStatementParser (PDFBox + UniversalPdfParser, any bank)
 * CSV_*      → CsvStatementParser (bank-specific column mappings for reliability)
 * GENERIC_CSV→ CsvStatementParser (FuzzyColumnMapper fallback)
 * UNKNOWN    → show error, ask user to try different file
 */
enum class BankFormat(val displayName: String) {

    // ── Spreadsheet (handled by SpreadsheetParser + FuzzyColumnMapper) ─────────
    // No per-bank variants needed — FuzzyColumnMapper detects any bank's columns.
    XLSX     ("Excel (.xlsx)"),
    XLS      ("Excel (.xls)"),

    // ── PDF (handled by PdfStatementParser + UniversalPdfParser) ──────────────
    // No per-bank variants needed — UniversalPdfParser uses Rule Engine for any bank.
    PDF      ("PDF Statement"),

    // ── CSV (bank-specific for reliability, then generic fallback) ─────────────
    // CSV has no magic bytes, so we fingerprint headers per bank.
    HDFC_CSV     ("HDFC Bank"),
    ICICI_CSV    ("ICICI Bank"),
    SBI_CSV      ("State Bank of India"),
    AXIS_CSV     ("Axis Bank"),
    KOTAK_CSV    ("Kotak Bank"),
    YES_CSV      ("Yes Bank"),
    INDUSIND_CSV ("IndusInd Bank"),
    GENERIC_CSV  ("Unknown Bank"),

    // ── Unrecognised ───────────────────────────────────────────────────────────
    UNKNOWN  ("Unknown");

    /** True for spreadsheet formats (XLS/XLSX) — routed to SpreadsheetParser. */
    val isSpreadsheet: Boolean get() = this == XLSX || this == XLS

    /** True for PDF format — routed to PdfStatementParser. */
    val isPdf: Boolean get() = this == PDF

    /** True for CSV formats — routed to CsvStatementParser. */
    val isCsv: Boolean get() = this != XLSX && this != XLS &&
            this != PDF  && this != UNKNOWN
}