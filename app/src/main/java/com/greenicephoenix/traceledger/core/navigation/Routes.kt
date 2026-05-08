package com.greenicephoenix.traceledger.core.navigation

object Routes {
    const val ONBOARDING           = "onboarding"
    const val DASHBOARD            = "dashboard"
    const val ACCOUNTS             = "accounts"
    const val ADD_ACCOUNT          = "add_account"
    const val EDIT_ACCOUNT         = "add_account/{accountId}"
    const val TRANSACTIONS         = "transactions"
    const val ADD_TRANSACTION      = "add_transaction"
    const val EDIT_TRANSACTION     = "edit_transaction/{transactionId}"
    const val STATISTICS           = "statistics"
    const val STATISTICS_OVERVIEW  = "statistics/overview"
    const val STATISTICS_BREAKDOWN = "statistics/breakdown"
    const val STATISTICS_INCOME    = "statistics/income"
    const val STATISTICS_CASHFLOW  = "statistics/cashflow"
    const val STATISTICS_TRENDS    = "statistics/trends"
    const val SETTINGS             = "settings"
    const val CATEGORIES           = "categories"
    const val ADD_CATEGORY         = "add_category"
    const val EDIT_CATEGORY        = "edit_category/{categoryId}"
    const val BUDGETS              = "budgets"
    const val ADD_EDIT_BUDGET      = "add-edit-budget"
    const val ABOUT                = "about"
    const val RECURRING            = "recurring"
    const val ADD_RECURRING        = "add_recurring"
    const val EDIT_RECURRING       = "edit_recurring/{recurringId}"
    const val SUPPORT              = "support"
    const val TEMPLATES            = "templates"
    const val ADD_TEMPLATE         = "add_template"
    const val EDIT_TEMPLATE        = "edit_template/{templateId}"

    // ── v1.3.0: Statement Import ───────────────────────────────────────────────
    //
    // IMPORT_HUB    — Step 1: pick account + file
    // IMPORT_REVIEW — Step 2: review parsed transactions before confirming
    //                 accountId passed as route arg; fileUri via SavedStateHandle
    // IMPORT_RESULT — Step 3: success summary with imported/skipped counts
    //                 Three int args passed directly in the route path
    const val IMPORT_HUB    = "import_hub"
    const val IMPORT_REVIEW = "import_review/{accountId}"
    const val IMPORT_RESULT = "import_result/{imported}/{skipped}/{duplicates}"

    /** Build a concrete IMPORT_REVIEW route for a given accountId. */
    fun importReviewFor(accountId: String) = "import_review/$accountId"

    /**
     * Build a concrete IMPORT_RESULT route with the three counts.
     * Called by NavGraph after a successful import to navigate to Step 3.
     */
    fun importResultRoute(imported: Int, skipped: Int, duplicates: Int) =
        "import_result/$imported/$skipped/$duplicates"
}