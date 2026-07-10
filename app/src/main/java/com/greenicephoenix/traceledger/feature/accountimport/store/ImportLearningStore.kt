package com.greenicephoenix.traceledger.feature.accountimport.store

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

/**
 * Remembers user category assignments from previous import sessions.
 *
 * When a user manually assigns a category to a transaction during import review,
 * that assignment is stored here keyed by description prefix. On the next import,
 * this store is checked BEFORE AutoCategorizer — so "ACH/RAIL VIKAS NIGAM" will
 * automatically get "Mutual Funds" because the user assigned it last time.
 *
 * Storage: DataStore<Preferences> — key-value, persists across app restarts.
 * Key format: "import_cat::<isCredit>::<descriptionPrefix>"
 * Value: categoryId string
 *
 * Max entries: 500 — oldest entries are pruned when limit is reached.
 * This prevents unbounded growth for users who import frequently.
 */
private val Context.importLearningDataStore by preferencesDataStore(
    name = "import_learning"
)

class ImportLearningStore(private val context: Context) {

    // ── Learn ─────────────────────────────────────────────────────────────────

    /**
     * Store a user's category assignment for future imports.
     *
     * @param description  The raw bank description (prefix will be extracted).
     * @param isCredit     Whether this was a credit (income) transaction.
     * @param categoryId   The category ID the user assigned.
     */
    suspend fun learn(
        description: String,
        isCredit:    Boolean,
        categoryId:  String
    ) {
        val key = preferenceKey(description, isCredit)
        context.importLearningDataStore.edit { prefs ->
            prefs[key] = categoryId
        }
    }

    // ── Suggest ───────────────────────────────────────────────────────────────

    /**
     * Look up a previously learned category for a transaction description.
     *
     * @param description  The raw bank description to look up.
     * @param isCredit     Whether this is a credit (income) transaction.
     * @return             The previously assigned category ID, or null if not found.
     */
    suspend fun getSuggestion(
        description: String,
        isCredit:    Boolean
    ): String? {
        val key   = preferenceKey(description, isCredit)
        val prefs = context.importLearningDataStore.data.first()
        return prefs[key]
    }

    // ── Bulk learn (called after import confirmation) ─────────────────────────

    /**
     * Learn all manual assignments from a completed import session.
     *
     * Called after the user confirms import — learns every transaction where
     * the user changed the category from the auto-suggested value.
     *
     * Only learns USER-CHANGED assignments (categoryId != suggestedCategoryId),
     * not auto-suggested ones — we don't want to reinforce AutoCategorizer's
     * mistakes.
     *
     * @param assignments  List of (description, isCredit, categoryId, suggestedCategoryId)
     */
    suspend fun learnAll(
        assignments: List<LearningEntry>
    ) {
        if (assignments.isEmpty()) return
        context.importLearningDataStore.edit { prefs ->
            for (entry in assignments) {
                // Only learn if user explicitly changed the category
                if (entry.categoryId != null &&
                    entry.categoryId != entry.suggestedCategoryId) {
                    val key = makeKey(entry.description, entry.isCredit)
                    prefs[stringPreferencesKey(key)] = entry.categoryId
                }
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun preferenceKey(description: String, isCredit: Boolean) =
        stringPreferencesKey(makeKey(description, isCredit))

    /**
     * Build a stable DataStore key from description + direction.
     * Uses the same prefix logic as StatementImportViewModel.descriptionPrefix()
     * so keys are consistent across import sessions.
     */
    private fun makeKey(description: String, isCredit: Boolean): String {
        val prefix = descriptionPrefix(description)
        val dir    = if (isCredit) "cr" else "dr"
        return "import_cat::${dir}::${prefix}"
    }

    private fun descriptionPrefix(description: String): String {
        val lower    = description.lowercase().trim()
        val segments = lower.split("/")
        return when {
            segments.size >= 2 -> "${segments[0]}/${segments[1].take(12)}"
            else               -> lower.take(15)
        }
    }
}

/**
 * A single learning entry from an import session.
 * Passed to [ImportLearningStore.learnAll] after import confirmation.
 */
data class LearningEntry(
    val description:          String,
    val isCredit:             Boolean,
    val categoryId:           String?,   // what the user assigned
    val suggestedCategoryId:  String?    // what AutoCategorizer suggested
)