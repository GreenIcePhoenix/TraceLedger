// ─────────────────────────────────────────────────────────────────────────────
// FILE: feature/accountimport/model/ParsedTransaction.kt
//
// What this is:
//   The raw output of the parser layer (CsvStatementParser / PdfStatementParser).
//   This is NOT a database entity. It is a plain data class that carries
//   everything we extracted from one row of a bank statement.
//
// Why separate from ImportReviewItem:
//   ParsedTransaction is immutable parser output.
//   ImportReviewItem is the mutable UI state the user can edit on the review
//   screen (change category, exclude row, etc.).
//   Keeping them separate means the parser never needs to know about UI state,
//   and the UI layer never needs to re-parse the file.
// ─────────────────────────────────────────────────────────────────────────────
package com.greenicephoenix.traceledger.feature.accountimport.model

import java.math.BigDecimal
import java.time.LocalDate

/**
 * One transaction row as parsed directly from a bank statement.
 * Immutable. Produced by parsers. Consumed by ImportReviewViewModel.
 *
 * @param rawDate       The date string exactly as it appeared in the file.
 *                      Kept for display in error cases where parsing failed.
 * @param date          Parsed LocalDate. Null if the date string could not
 *                      be parsed — this row will be flagged with an error in
 *                      the review screen.
 * @param description   Raw narration/particulars text from the statement.
 * @param amount        Always positive. Use [isCredit] to determine direction.
 * @param isCredit      true  → money IN  → maps to TransactionType.INCOME
 *                      false → money OUT → maps to TransactionType.EXPENSE
 * @param balance       Running balance from the statement, if available.
 *                      Used on the review screen as a reference only — we do
 *                      not use it to set account balance automatically.
 * @param referenceNo   Cheque number / UTR / reference — stored in note field.
 */
data class ParsedTransaction(
    val rawDate: String,
    val date: LocalDate?,
    val description: String,
    val amount: BigDecimal,
    val isCredit: Boolean,
    val balance: BigDecimal? = null,
    val referenceNo: String? = null,
    val importedCategoryName: String? = null   // set when CSV has a category column
)
