// ─────────────────────────────────────────────────────────────────────────────
// FILE: feature/accountimport/categorizer/AutoCategorizer.kt
//
// What this does:
//   Looks at a transaction description from a bank statement and returns the
//   best matching category ID from the user's existing category list.
//
// How it works:
//   1. We maintain a list of KEYWORD_RULES — each rule has a set of keywords
//      and the standard category name it maps to.
//   2. For each parsed transaction, we check the description against all rules.
//   3. If a rule matches, we look up the category by name in the user's
//      existing categories (from Room DB, passed in as a list).
//   4. If the category exists, we return its ID. If not, we return null
//      (user will see "No category" in the review screen and can pick one).
//
// Design decisions:
//   - We match against the USER's existing categories by name, not hardcoded IDs.
//     This means it works regardless of what category IDs Room assigned.
//   - Matching is case-insensitive and uses "contains" — not exact match.
//     This handles UPI prefixes like "UPI-SWIGGY" matching "swiggy".
//   - The user's custom categories are also checked (by name substring).
//   - If multiple rules match, the FIRST match wins. Rules are ordered by
//     specificity — more specific rules come before broad ones like "Other".
// ─────────────────────────────────────────────────────────────────────────────
package com.greenicephoenix.traceledger.feature.accountimport.categorizer

import com.greenicephoenix.traceledger.domain.model.CategoryType
import com.greenicephoenix.traceledger.domain.model.CategoryUiModel
import com.greenicephoenix.traceledger.domain.model.TransactionType

// ── Keyword rule ──────────────────────────────────────────────────────────────

/**
 * Maps a set of keyword substrings to a standard category name.
 *
 * @param keywords        Any of these substrings (case-insensitive) in the
 *                        transaction description will trigger this rule.
 * @param categoryName    The category name to look up in the user's category list.
 *                        Must match the name exactly (case-insensitive) as it
 *                        appears in default categories seeded at app install.
 * @param forType         Which transaction type this rule applies to.
 *                        EXPENSE keywords should not match income transactions
 *                        (e.g. "SALARY" → Income, not Expense).
 */
data class KeywordRule(
    val keywords:     List<String>,
    val categoryName: String,
    val forType:      TransactionType
)

// ── Rules ─────────────────────────────────────────────────────────────────────
// Order matters: more specific rules first, broader ones last.
// EXPENSE rules check isCredit = false transactions.
// INCOME rules check isCredit = true transactions.

private val KEYWORD_RULES = listOf(

    // ── INCOME ────────────────────────────────────────────────────────────────
    KeywordRule(
        keywords     = listOf("salary", "stipend", "payroll", "pay credit", "wages"),
        categoryName = "Salary",
        forType      = TransactionType.INCOME
    ),
    KeywordRule(
        keywords     = listOf("interest credit", "interest earned", "fd interest", "rd interest"),
        categoryName = "Investment",
        forType      = TransactionType.INCOME
    ),
    KeywordRule(
        keywords     = listOf("dividend", "mutual fund", "redemption"),
        categoryName = "Investment",
        forType      = TransactionType.INCOME
    ),
    KeywordRule(
        keywords     = listOf("refund", "cashback", "reversal"),
        categoryName = "Other",
        forType      = TransactionType.INCOME
    ),
    KeywordRule(
        keywords     = listOf("rental income", "rent received"),
        categoryName = "Other",
        forType      = TransactionType.INCOME
    ),

    // ── EXPENSE: Food & Dining ────────────────────────────────────────────────
    KeywordRule(
        keywords     = listOf("zomato", "swiggy", "dunzo", "blinkit", "zepto",
            "dominos", "mcdonald", "kfc", "pizza", "burger",
            "starbucks", "cafe", "restaurant", "hotel", "biryani",
            "eatsure", "faasos"),
        categoryName = "Food",
        forType      = TransactionType.EXPENSE
    ),

    // ── EXPENSE: Groceries ───────────────────────────────────────────────────
    KeywordRule(
        keywords     = listOf("grofers", "bigbasket", "dmart", "reliance fresh",
            "more supermarket", "spencer", "nature basket",
            "jiomart", "milkbasket"),
        categoryName = "Food",
        forType      = TransactionType.EXPENSE
    ),

    // ── EXPENSE: Shopping ────────────────────────────────────────────────────
    KeywordRule(
        keywords     = listOf("amazon", "flipkart", "myntra", "nykaa", "ajio",
            "meesho", "snapdeal", "shopsy", "tatacliq",
            "firstcry", "purplle"),
        categoryName = "Shopping",
        forType      = TransactionType.EXPENSE
    ),

    // ── EXPENSE: Transport ───────────────────────────────────────────────────
    KeywordRule(
        keywords     = listOf("uber", "ola", "rapido", "blu smart", "meru",
            "irctc", "indian railway", "redbus", "abhibus",
            "metro", "dmrc", "bmtc", "best bus",
            "makemytrip", "yatra", "goibibo", "indigo", "airindia",
            "spicejet", "vistara", "akasa"),
        categoryName = "Transport",
        forType      = TransactionType.EXPENSE
    ),
    KeywordRule(
        keywords     = listOf("fastag", "toll", "fuel", "petrol", "diesel", "hp petrol",
            "iocl", "bpcl", "hindustan petroleum"),
        categoryName = "Transport",
        forType      = TransactionType.EXPENSE
    ),

    // ── EXPENSE: Entertainment ────────────────────────────────────────────────
    KeywordRule(
        keywords     = listOf("netflix", "amazon prime", "prime video", "hotstar",
            "disney", "sony liv", "zee5", "jiocinema",
            "spotify", "gaana", "jiosaavn", "apple music",
            "youtube premium", "bookmyshow", "pvr", "inox"),
        categoryName = "Entertainment",
        forType      = TransactionType.EXPENSE
    ),

    // ── EXPENSE: Health ───────────────────────────────────────────────────────
    KeywordRule(
        keywords     = listOf("apollo", "fortis", "max hospital", "medanta",
            "pharmacy", "medical", "hospital", "clinic",
            "1mg", "pharmeasy", "netmeds", "healthkart",
            "doctor", "lab test", "diagnostics", "thyrocare"),
        categoryName = "Health",
        forType      = TransactionType.EXPENSE
    ),

    // ── EXPENSE: Utilities ────────────────────────────────────────────────────
    KeywordRule(
        keywords     = listOf("electricity", "bescom", "tata power", "adani electricity",
            "mahanagar gas", "igl", "mgl", "piped gas",
            "water bill", "bwssb", "jal board",
            "broadband", "jio fiber", "airtel fiber", "act broadband",
            "tata sky", "dish tv", "sun direct", "d2h"),
        categoryName = "Utilities",
        forType      = TransactionType.EXPENSE
    ),
    KeywordRule(
        keywords     = listOf("airtel", "jio", "vodafone", "vi ", "bsnl", "recharge",
            "mobile bill", "postpaid"),
        categoryName = "Utilities",
        forType      = TransactionType.EXPENSE
    ),

    // ── EXPENSE: Housing / Rent ───────────────────────────────────────────────
    KeywordRule(
        keywords     = listOf("rent", "maintenance", "society", "nobroker",
            "housing.com", "magicbricks", "99acres"),
        categoryName = "Housing",
        forType      = TransactionType.EXPENSE
    ),

    // ── EXPENSE: Education ────────────────────────────────────────────────────
    KeywordRule(
        keywords     = listOf("fees", "tuition", "school", "college", "university",
            "byju", "unacademy", "vedantu", "coursera", "udemy",
            "upgrad", "simplilearn"),
        categoryName = "Education",
        forType      = TransactionType.EXPENSE
    ),

    // ── EXPENSE: Insurance ────────────────────────────────────────────────────
    KeywordRule(
        keywords     = listOf("insurance", "lic", "icici pru", "hdfc life",
            "max life", "bajaj allianz", "star health",
            "niva bupa", "religare", "policy premium"),
        categoryName = "Insurance",
        forType      = TransactionType.EXPENSE
    ),

    // ── EXPENSE: ATM / Cash ───────────────────────────────────────────────────
    KeywordRule(
        keywords     = listOf("atm cash", "cash withdrawal", "atm withdrawal"),
        categoryName = "Cash",
        forType      = TransactionType.EXPENSE
    )
)

// ── AutoCategorizer ───────────────────────────────────────────────────────────

object AutoCategorizer {

    /**
     * Suggest a category ID for a transaction based on its description.
     *
     * @param description   The narration/particulars text from the bank statement.
     * @param isCredit      true if this is a credit (income), false if debit (expense).
     * @param userCategories The user's full category list from the DB.
     *                       Both default and custom categories are checked.
     * @return              The ID of the best matching category, or null if no
     *                      match was found. Null means the review screen shows
     *                      "No category" and the user picks manually.
     */
    fun suggest(
        description:    String,
        isCredit:       Boolean,
        userCategories: List<CategoryUiModel>
    ): String? {
        val targetType = if (isCredit) TransactionType.INCOME else TransactionType.EXPENSE
        val normalised = description.lowercase()

        // Check keyword rules in order — first match wins
        for (rule in KEYWORD_RULES) {
            if (rule.forType != targetType) continue
            if (rule.keywords.any { keyword -> keyword in normalised }) {
                // Look up by name in user's categories (case-insensitive)
                val match = userCategories.firstOrNull { category ->
                    category.type.name == targetType.name &&
                            category.name.equals(rule.categoryName, ignoreCase = true)
                }
                if (match != null) return match.id
            }
        }

        // No keyword match — try matching the description directly against
        // the user's custom category names (they might have "Petrol", "Gym", etc.)
        val directMatch = userCategories.firstOrNull { category ->
            category.type.name == targetType.name &&
                    normalised.contains(category.name.lowercase())
        }
        if (directMatch != null) return directMatch.id

        return null  // No match — user will assign manually in review screen
    }
}