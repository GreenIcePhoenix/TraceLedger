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
    const val HELP                 = "help"
    const val CHANGELOG            = "changelog"
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
    const val SMS_SETTINGS = "sms_settings"
    const val SMS_REVIEW   = "sms_review"
    const val SMS_CUSTOM_RULES = "sms_custom_rules"
    const val SMS_ADD_RULE     = "sms_add_rule"
    const val SMS_EDIT_RULE    = "sms_edit_rule/{ruleId}"

    // ADD inside the Routes object, after STATISTICS_TRENDS:
    const val STATISTICS_HEATMAP  = "statistics/heatmap"
    const val STATISTICS_WEEKDAY  = "statistics/weekday"
    const val STATISTICS_AREA     = "statistics/area"
    const val STATISTICS_WATERFALL = "statistics/waterfall"

    // Phase 3
    const val STATISTICS_TREEMAP     = "statistics/treemap"
    const val STATISTICS_SANKEY      = "statistics/sankey"
    const val STATISTICS_HEALTH      = "statistics/health"
    const val STATISTICS_SAVINGS_RATE = "statistics/savings_rate"
    const val STATISTICS_VELOCITY    = "statistics/velocity"
    const val STATISTICS_CAT_COMPARE = "statistics/cat_compare"
    const val STATISTICS_INCOME_STABILITY = "statistics/income_stability"
    const val STATISTICS_TOP_DAYS    = "statistics/top_days"
    const val STATISTICS_ROLLING     = "statistics/rolling"

    fun smsEditRuleFor(ruleId: Long) = "sms_edit_rule/$ruleId"

    /** Build a concrete IMPORT_REVIEW route for a given accountId. */
    fun importReviewFor(accountId: String) = "import_review/$accountId"

    /**
     * Build a concrete IMPORT_RESULT route with the three counts.
     * Called by NavGraph after a successful import to navigate to Step 3.
     */
    fun importResultRoute(imported: Int, skipped: Int, duplicates: Int) =
        "import_result/$imported/$skipped/$duplicates"
}