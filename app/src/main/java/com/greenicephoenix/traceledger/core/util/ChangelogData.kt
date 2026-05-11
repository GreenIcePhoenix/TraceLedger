package com.greenicephoenix.traceledger.core.util

/**
 * ChangelogData — Single source of truth for TraceLedger release notes.
 *
 * Previously stored in res/raw/changelog.txt — that file is now dead code and can be deleted.
 * ChangelogParser.kt and ChangelogIconMapper.kt are also no longer used and can be deleted.
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

        // ── v1.3.0 ────────────────────────────────────────────────────────────
        VersionEntry(
            version     = "1.3.0",
            releaseDate = "25 May 2026",
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
                ChangeItem(ChangeType.IMPROVED, "GIP Theme Rebrand — TraceLedger now uses the GreenIcePhoenix Sovereign Violet accent across all interactive elements"),
                ChangeItem(ChangeType.IMPROVED, "New App Icon — Vines Green on Void Deep, aligned with the GreenIcePhoenix ecosystem and built for adaptive icon launchers"),
                ChangeItem(ChangeType.NEW,      "Extra Dark Mode — OLED-optimised Extra Dark theme joins System, Light, and Dark in Settings → Appearance"),
                ChangeItem(ChangeType.NEW,      "Home Screen Widget — shows total balance and this month's income, expense, and net at a glance"),
                ChangeItem(ChangeType.NEW,      "Spending Forecast — Dashboard now projects your month-end spend based on your daily average so far"),
                ChangeItem(ChangeType.NEW,      "Transaction Templates — save any transaction as a template and replay it in one tap from the Add Transaction screen"),
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
                ChangeItem(ChangeType.IMPROVED, "Budget Warnings — alerts now trigger at 75% (warning) and 90% (critical), not just when exceeded"),
                ChangeItem(ChangeType.NEW,      "Number Format — choose between Indian (1,00,000) and International (100,000) formatting in Settings"),
                ChangeItem(ChangeType.NEW,      "Onboarding — first-launch walkthrough introducing the app to new users"),
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