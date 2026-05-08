package com.greenicephoenix.traceledger.feature.accountimport.model

// ─────────────────────────────────────────────────────────────────────────────
// FILE: feature/accountimport/model/ImportReviewItem.kt
//
// What this is:
//   The mutable UI state for one transaction row on the ImportReviewScreen.
//   Created from ParsedTransaction by the ViewModel.
//
// Fields the user can change on the review screen:
//   - isIncluded    → toggle to exclude a row from import
//   - categoryId    → tap to reassign via CategoryPickerSheet
//   - note          → tapping description opens an edit field
//
// Fields that cannot be changed in this version:
//   - date, amount, type — treat these as locked to the statement values.
//     Users can always edit a transaction normally after import.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Mutable review state for one parsed transaction row.
 * Lives in ImportReviewViewModel.uiState.reviewItems (a SnapshotStateList).
 *
 * @param id            Stable unique key for Compose LazyColumn — just the
 *                      list index converted to string. Not a DB id.
 * @param parsed        The original immutable parsed data. Never mutated.
 * @param suggestedCategoryId  Auto-categoriser's best guess. Null if no match.
 * @param categoryId    Current selection (starts equal to suggestedCategoryId).
 *                      User can change this via CategoryPickerSheet.
 * @param note          Editable note field. Pre-filled with description.
 * @param isIncluded    Whether this row will be inserted into the DB.
 *                      Starts true. User can untick to skip.
 * @param isDuplicate   True if a transaction with the same account + date(±1d)
 *                      + amount already exists in the DB.
 *                      Shown as a yellow warning badge — does NOT auto-exclude.
 * @param hasDateError  True if the date could not be parsed. Row is always
 *                      excluded if this is true and cannot be re-included.
 */
data class ImportReviewItem(
    val id: String,
    val parsed: ParsedTransaction,
    val suggestedCategoryId: String?,
    val categoryId: String?,
    val note: String,
    val isIncluded: Boolean = true,
    val isDuplicate: Boolean = false,
    val hasDateError: Boolean = false
)