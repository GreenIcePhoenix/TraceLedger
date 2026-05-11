package com.greenicephoenix.traceledger.feature.sms.store

import android.content.Context

/**
 * Persists user corrections to SMS parsing so the app improves over time.
 *
 * TWO LEARNING MAPS (both backed by SharedPreferences — no DB migration needed):
 *
 *  1. Sender → AccountId
 *     Stored when user picks a DIFFERENT account than what was auto-suggested.
 *     Next time an SMS from the same sender arrives, we skip bank-name matching
 *     and use the stored account directly.
 *     Key: normalised sender ("VK-HDFCBK" → "VKHDFCBK")
 *
 *  2. Description → CategoryId
 *     Stored when user picks a DIFFERENT category than what was auto-suggested.
 *     Next time a description with the same leading words arrives, we use the
 *     stored category.
 *     Key: first 4 significant words of description (lowercase, alphanumeric)
 *
 * Both maps are checked FIRST in SmsQueueRepository — learned preferences
 * always override built-in rules.
 */
class SmsLearningStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Account learning ──────────────────────────────────────────────────────

    /** Called by SmsReviewViewModel when user picks a different account than suggested */
    fun learnAccountForSender(sender: String, accountId: String) {
        prefs.edit()
            .putString("$KEY_ACCT${normalizeSender(sender)}", accountId)
            .apply()
    }

    /** Called by SmsQueueRepository — highest priority for account suggestion */
    fun getLearnedAccountForSender(sender: String): String? =
        prefs.getString("$KEY_ACCT${normalizeSender(sender)}", null)

    // ── Category learning ─────────────────────────────────────────────────────

    /** Called by SmsReviewViewModel when user picks a different category than suggested */
    fun learnCategoryForDescription(description: String, categoryId: String) {
        prefs.edit()
            .putString("$KEY_CAT${normalizeDescription(description)}", categoryId)
            .apply()
    }

    /** Called by SmsQueueRepository — highest priority for category suggestion */
    fun getLearnedCategoryForDescription(description: String): String? =
        prefs.getString("$KEY_CAT${normalizeDescription(description)}", null)

    // ── Normalisation ─────────────────────────────────────────────────────────

    /**
     * Strips formatting from sender IDs so "VK-HDFCBK" and "BP-HDFCBK"
     * both map to the same learned account.
     */
    private fun normalizeSender(sender: String): String =
        sender.uppercase().replace(Regex("[^A-Z0-9]"), "")

    /**
     * Takes the first 4 meaningful words from a description.
     * "HDFC Bank · Zomato Order" → "hdfc_bank_zomato_order"
     * "Amazon Fresh Delivery" → "amazon_fresh_delivery"
     * Short words (≤2 chars) and the "·" separator are skipped.
     */
    private fun normalizeDescription(description: String): String =
        description.lowercase()
            .replace(Regex("[^a-z0-9 ]"), " ")
            .split(" ")
            .filter { it.length > 2 }
            .take(4)
            .joinToString("_")

    companion object {
        private const val PREFS_NAME = "sms_learning"
        private const val KEY_ACCT   = "acct_"
        private const val KEY_CAT    = "cat_"
    }
}