package com.greenicephoenix.traceledger.core.util

/**
 * ChangelogData — Single source of truth for TraceLedger release notes.
 *
 *
 * HOW TO ADD A NEW RELEASE:
 *   1. Bump versionName in app/build.gradle.kts (e.g. "1.4.0")
 *   2. Copy the template at the bottom of this file.
 *   3. Paste it at the TOP of `entries` — newest entry must always be first.
 *   4. That's it. ChangelogScreen updates automatically.
 *
 * CHANGE TYPES:
 *   NEW      — New feature, screen, or capability
 *   IMPROVED — Enhancement or redesign of an existing feature
 *   FIXED    — Bug fix or correctness improvement
 *   SECURITY — Privacy or security change
 */

// ─────────────────────────────────────────────────────────────────────────────
// Data model
// ─────────────────────────────────────────────────────────────────────────────

data class VersionEntry(
    val version     : String,   // Must match versionName in build.gradle.kts
    val releaseDate : String,   // e.g. "10 May 2026"
    val tagline     : String,   // Short flavour line shown under the version number
    val changes     : List<ChangeItem>
)

data class ChangeItem(
    val type        : ChangeType,
    val description : String
)

enum class ChangeType(val label: String) {
    NEW     ("New"),
    IMPROVED("Improved"),
    FIXED   ("Fixed"),
    SECURITY("Security")
}

// ─────────────────────────────────────────────────────────────────────────────
// Changelog — newest first
// ─────────────────────────────────────────────────────────────────────────────

object ChangelogData {

    val entries: List<VersionEntry> = listOf(

        // ── v1.5.0 ────────────────────────────────────────────────────────────────
        VersionEntry(
            version     = "1.5.0",
            releaseDate = "06 Jun 2026",
            tagline     = "Smarter imports, last-4 matching, and a consistent design language.",
            changes     = listOf(
                ChangeItem(ChangeType.NEW,      "Statement import learning store — category assignments remembered across sessions"),
                ChangeItem(ChangeType.NEW,      "Apply to similar — assign a category to all matching transactions in one tap"),
                ChangeItem(ChangeType.NEW,      "Bulk assign — one category to all uncategorized rows at once"),
                ChangeItem(ChangeType.NEW,      "Account last 4 digits — optional field shown on cards and in account selectors"),
                ChangeItem(ChangeType.IMPROVED, "Statement import works with any bank — preamble rows, blank columns, multi-sheet, merged headers"),
                ChangeItem(ChangeType.IMPROVED, "CSV encoding and delimiter auto-detected — UTF-16, Windows-1252, tab, semicolon all supported"),
                ChangeItem(ChangeType.IMPROVED, "Added Federal Bank, Canara, PNB, Bank of Baroda, IDFC First Bank CSV support"),
                ChangeItem(ChangeType.IMPROVED, "Import review — auto-suggested categories marked with ~ prefix; description always visible"),
                ChangeItem(ChangeType.IMPROVED, "Category picker in import matches AddTransaction style with icons and colors"),
                ChangeItem(ChangeType.IMPROVED, "SMS account matching now uses last 4 digits for higher accuracy"),
                ChangeItem(ChangeType.IMPROVED, "Categories can be renamed even when transactions are linked"),
                ChangeItem(ChangeType.IMPROVED, "Delete category shows warning instead of hiding the button"),
                ChangeItem(ChangeType.IMPROVED, "Design system — shared color tokens, layout tokens, and Compose primitives across all screens"),
                ChangeItem(ChangeType.IMPROVED, "AddEditRecurring, AddEditBudget, and Templates screens redesigned"),
                ChangeItem(ChangeType.FIXED,    "Auto backup now includes currency and number format settings"),
                ChangeItem(ChangeType.FIXED,    "Category rename no longer crashes when transactions are linked"),
                ChangeItem(ChangeType.FIXED,    "ICICI and other banks with preamble rows now fully imported"),
                ChangeItem(ChangeType.FIXED,    "Credit transactions in XLSX now correctly identified"),
            )
        ),

        // ── v1.4.0 ────────────────────────────────────────────────────────────
        VersionEntry(
            version     = "1.4.0",
            releaseDate = "26 May 2026",
            tagline     = "Investments arrive, analytics go deep, and the entire finance experience evolves.",
            changes     = listOf(

                // ── Investment ────────────────────────────────────────────────
                ChangeItem(ChangeType.NEW,      "Investment transaction type — track stocks, mutual funds, FDs, crypto, gold, PPF, NPS, and real estate separately from expenses"),
                ChangeItem(ChangeType.NEW,      "Investment categories seeded by default with distinct colors and icon support"),
                ChangeItem(ChangeType.NEW,      "Investment Breakdown screen with donut chart and category drill-down"),
                ChangeItem(ChangeType.NEW,      "Investment Trend, Invest vs Earn vs Spend, and Portfolio Allocation analytics screens"),
                ChangeItem(ChangeType.NEW,      "Dashboard expanded to 4 summary cards: Income, Expense, Invested, and Net"),
                ChangeItem(ChangeType.NEW,      "Net now calculated as Income − Expense − Investment for a more accurate financial overview"),
                ChangeItem(ChangeType.NEW,      "Add Transaction fully redesigned — adaptive amount field colors, quick amount chips (+100/+500/+1000/+5000), compact notes section, and full-width SAVE action"),
                ChangeItem(ChangeType.NEW,      "Date picker shortcuts added: Today, Yesterday, and named recent days for faster entry"),
                ChangeItem(ChangeType.NEW,      "Statistics system completely rebuilt with 23 analytics screens across 4 phases"),
                ChangeItem(ChangeType.NEW,      "New analytics include Spending Heatmap, Day-of-Week Analysis, Cashflow Waterfall, Treemap, Sankey Flow, Financial Health Gauge, Savings Rate Trend, Expense Velocity, Income Stability, Forecasting, Recurring Analytics, and more"),
                ChangeItem(ChangeType.NEW,      "SMS Review redesigned with Card, Table, and Wizard modes for reviewing 1 to 100+ transactions efficiently"),
                ChangeItem(ChangeType.NEW,      "Categories expanded with richer seeded defaults for Expense, Income, and Investment tracking"),
                ChangeItem(ChangeType.NEW,      "Category system upgraded with 50+ adaptive icons and expanded color palettes"),
                ChangeItem(ChangeType.NEW,      "JSON backup now includes recurring rules, templates and budgets — full device migration in one file"),
                ChangeItem(ChangeType.NEW,      "Unified CSV import with transaction-centric format — accounts and categories derived automatically from your data"),
                ChangeItem(ChangeType.NEW,      "CSV import mapping screen — map accounts and categories to existing ones or create new before confirming"),
                ChangeItem(ChangeType.NEW,      "CSV import template download — fill in manually and import without any prior export"),
                ChangeItem(ChangeType.NEW,      "Auto Backup — periodic JSON backup to a user-chosen folder (Daily, Weekly, Bi-weekly, Monthly, Quarterly)"),
                ChangeItem(ChangeType.IMPROVED, "Dedicated Import / Export screen replacing scattered settings rows"),
                ChangeItem(ChangeType.IMPROVED, "JSON import now merges by name when IDs differ — safe to restore after reinstall without duplicating accounts"),
                ChangeItem(ChangeType.IMPROVED, "CSV import resolves account and category matches by name — no longer fails after reinstall"),
                ChangeItem(ChangeType.IMPROVED, "Import preview shows counts for all data types including recurring rules and templates"),
                ChangeItem(ChangeType.IMPROVED, "CSV file picker now accepts all file manager MIME types — .csv files visible in every app"),
                ChangeItem(ChangeType.IMPROVED, "Edit Transaction actions streamlined with Update and Delete buttons aligned in a single row"),
                ChangeItem(ChangeType.IMPROVED, "Charts upgraded to ComposeCharts 0.2.5 with smoother animations, gestures, and improved drill-down interactions"),
                ChangeItem(ChangeType.IMPROVED, "Add/Edit Category screen redesigned with responsive icon and color grids"),
                ChangeItem(ChangeType.IMPROVED, "Theme handling improved across onboarding, categories, charts, and analytics"),

                // ── Bug fixes ─────────────────────────────────────────────────
                ChangeItem(ChangeType.FIXED,    "SMS Review table headers and rows now scroll perfectly in sync"),
                ChangeItem(ChangeType.FIXED,    "Transactions can no longer be saved from SMS Review without assigning a category"),
                ChangeItem(ChangeType.FIXED,    "Budget rings, legends, and chart tooltips now consistently display proper category names"),
                ChangeItem(ChangeType.FIXED,    "Savings Rate Trend now shows proper empty states when income data is unavailable"),
            )
        ),

        // ── v1.3.0 ────────────────────────────────────────────────────────────
        VersionEntry(
            version     = "1.3.0",
            releaseDate = "25 Apr 2026",
            tagline     = "Private SMS intelligence, now smarter, cleaner, and fully customizable.",
            changes     = listOf(
                ChangeItem(ChangeType.NEW,      "SMS Transaction Detection — auto-detects financial SMS from 30+ Indian banks and wallets"),
                ChangeItem(ChangeType.NEW,      "Real-time detection — new SMS instantly queued for review (opt-in, RECEIVE_SMS permission)"),
                ChangeItem(ChangeType.NEW,      "Inbox scan — import historical transactions with preset or custom date range"),
                ChangeItem(ChangeType.NEW,      "SMS Review screen — accept, edit, or reject each transaction before it is saved"),
                ChangeItem(ChangeType.NEW,      "Smart account matching — resolves the correct account even for UPI credit notifications"),
                ChangeItem(ChangeType.NEW,      "Auto-categorisation — 40+ keyword patterns for Zomato, Amazon, Airtel, Apollo, and more"),
                ChangeItem(ChangeType.NEW,      "Learning engine — silently learns from your corrections and improves over time"),
                ChangeItem(ChangeType.NEW,      "Custom SMS Rules — define rules for unsupported SMS formats with optional advanced regex mode"),
                ChangeItem(ChangeType.NEW,      "Rule tester — preview extraction results with live regex validation and named group references"),
                ChangeItem(ChangeType.NEW,      "Always-exclude rules — permanently silence specific senders"),
                ChangeItem(ChangeType.NEW,      "Help & FAQ — searchable answers to common questions, accessible from Settings"),
                ChangeItem(ChangeType.NEW,      "What's New screen — full release history with collapsible version cards"),
                ChangeItem(ChangeType.IMPROVED, "Settings redesigned — new SYSTEM section with What's New, Check for Updates, Website, Privacy Policy, and Terms"),
                ChangeItem(ChangeType.IMPROVED, "Dashboard and Settings now show live pending SMS review counts"),
                ChangeItem(ChangeType.IMPROVED, "About screen simplified — identity, privacy promises, and legal links only"),
                ChangeItem(ChangeType.IMPROVED, "Discord community link moved to Settings → App for easier access"),
                ChangeItem(ChangeType.IMPROVED, "SMS merchant extraction improved for 'used at', 'payment to', 'beneficiary', standalone VPA, and parenthetical UPI formats"),
                ChangeItem(ChangeType.IMPROVED, "Fallback merchant extraction now prioritises VPA and 'to MERCHANT' patterns before raw SMS text"),
                ChangeItem(ChangeType.FIXED,    "Category cards invisible in light mode — gradients now use surface-to-surfaceVariant with a subtle border"),
            )
        ),

        // ── v1.2.0 ────────────────────────────────────────────────────────────
        VersionEntry(
            version     = "1.2.0",
            releaseDate = "15 Mar 2026",
            tagline     = "A new look, a new icon, and your balance on your home screen.",
            changes     = listOf(
                ChangeItem(ChangeType.NEW,      "Extra Dark Mode — OLED-optimised Extra Dark theme joins System, Light, and Dark in Settings → Appearance"),
                ChangeItem(ChangeType.NEW,      "Home Screen Widget — shows total balance and this month's income, expense, and net at a glance"),
                ChangeItem(ChangeType.NEW,      "Spending Forecast — Dashboard now projects your month-end spend based on your daily average so far"),
                ChangeItem(ChangeType.NEW,      "Transaction Templates — save any transaction as a template and replay it in one tap from the Add Transaction screen"),
                ChangeItem(ChangeType.IMPROVED, "GIP Theme Rebrand — TraceLedger now uses the GreenIcePhoenix Sovereign Violet accent across all interactive elements"),
                ChangeItem(ChangeType.IMPROVED, "New App Icon — Vines Green on Void Deep, aligned with the GreenIcePhoenix ecosystem and built for adaptive icon launchers"),
                ChangeItem(ChangeType.IMPROVED, "Settings Redesign — reorganised into clear sections with inline current values for currency, theme, and format"),
            )
        ),

        // ── v1.1.0 ────────────────────────────────────────────────────────────
        VersionEntry(
            version     = "1.1.0",
            releaseDate = "20 Jan 2026",
            tagline     = "Daily reminders, auto updates, and number formatting done right.",
            changes     = listOf(
                ChangeItem(ChangeType.NEW,      "Daily Reminder — set a daily notification to remind you to log your transactions; choose any time from Settings → Notifications"),
                ChangeItem(ChangeType.NEW,      "Auto Update Checker — TraceLedger now checks for new releases on launch and lets you download and install updates directly"),
                ChangeItem(ChangeType.NEW,      "Support the Developer — tip jar added to Settings; TraceLedger is free and always will be"),
                ChangeItem(ChangeType.IMPROVED, "Number Format Applied — Indian and International number grouping now applied everywhere in the app, not just as a display label"),
                ChangeItem(ChangeType.FIXED,    "CSV Import Feedback — importing a CSV now shows exactly how many transactions were added, how many rows were skipped, and why"),
            )
        ),

        // ── v1.0.0 ────────────────────────────────────────────────────────────
        VersionEntry(
            version     = "1.0.0",
            releaseDate = "01 Dec 2025",
            tagline     = "Recurring schedules, financial insights, and a full transaction history.",
            changes     = listOf(
                ChangeItem(ChangeType.NEW,      "Recurring Transactions — schedule automatic expenses, income, and transfers: daily, weekly, monthly, quarterly, or yearly"),
                ChangeItem(ChangeType.NEW,      "Financial Insights — spending vs last month, savings rate, recurring cost summary, and net worth direction on the Dashboard"),
                ChangeItem(ChangeType.NEW,      "Spending Trends — category spend tracked across months with a line chart in Statistics"),
                ChangeItem(ChangeType.NEW,      "Transaction History — transactions now grouped by date with Today / Yesterday headers and an amount range filter"),
                ChangeItem(ChangeType.NEW,      "Transaction Detail — tap any transaction to view full details, edit, or delete from the detail sheet"),
                ChangeItem(ChangeType.NEW,      "Number Format — choose between Indian (1,00,000) and International (100,000) formatting in Settings"),
                ChangeItem(ChangeType.NEW,      "Onboarding — first-launch walkthrough introducing the app to new users"),
                ChangeItem(ChangeType.IMPROVED, "Budget Warnings — alerts now trigger at 75% (warning) and 90% (critical), not just when exceeded"),
            )
        ),

        // ── v0.3.0 ────────────────────────────────────────────────────────────
        VersionEntry(
            version     = "0.3.0",
            releaseDate = "15 Nov 2025",
            tagline     = "Full light mode, cleaner charts, and a smoother feel throughout.",
            changes     = listOf(
                ChangeItem(ChangeType.IMPROVED, "UI Upgrade — full light mode support with improved contrast and readability throughout the app"),
                ChangeItem(ChangeType.IMPROVED, "Dashboard — cleaner financial summaries and more consistent Material 3 surfaces"),
                ChangeItem(ChangeType.IMPROVED, "Charts — refined charts, legends, and improved visual hierarchy across statistics screens"),
                ChangeItem(ChangeType.IMPROVED, "Navigation — improved bottom navigation clarity and better typography spacing"),
            )
        ),

        // ── v0.2.0 ────────────────────────────────────────────────────────────
        VersionEntry(
            version     = "0.2.0",
            releaseDate = "01 Nov 2025",
            tagline     = "The first ledger entry.",
            changes     = listOf(
                ChangeItem(ChangeType.NEW, "Dashboard — total balance, income, and expense summaries"),
                ChangeItem(ChangeType.NEW, "Accounts — add and manage multiple accounts with balance tracking"),
                ChangeItem(ChangeType.NEW, "Transactions — log expenses, income, and transfers between accounts"),
                ChangeItem(ChangeType.NEW, "Budgets — monthly budget limits per category"),
                ChangeItem(ChangeType.NEW, "Statistics — expense and income breakdown charts"),
                ChangeItem(ChangeType.NEW, "Import / Export — JSON and CSV data backup and restore"),
            )
        ),

        // ── TEMPLATE FOR NEXT RELEASE — copy to top when shipping ─────────────
        //
        // VersionEntry(
        //     version     = "1.X.0",
        //     releaseDate = "DD MMM YYYY",
        //     tagline     = "Short punchy line describing this release",
        //     changes     = listOf(
        //         ChangeItem(ChangeType.NEW,      "Something brand new"),
        //         ChangeItem(ChangeType.IMPROVED, "Something made better"),
        //         ChangeItem(ChangeType.FIXED,    "Something that was broken"),
        //         ChangeItem(ChangeType.SECURITY, "Security or privacy improvement"),
        //     )
        // ),
    )

    /** Version string of the latest entry — used for update comparison. */
    val latestVersion: String get() = entries.firstOrNull()?.version ?: ""

    /** Look up a specific version's notes by version string. */
    fun forVersion(version: String): VersionEntry? = entries.find { it.version == version }
}