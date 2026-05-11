package com.greenicephoenix.traceledger.feature.help

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.core.util.AppLinks

// ── Data models (private — only used in this file) ────────────────────────────

private data class FaqItem(val question: String, val answer: String)

private data class FaqSection(
    val title : String,
    val icon  : ImageVector,
    val items : List<FaqItem>
)

// ── FAQ content ───────────────────────────────────────────────────────────────
// Add new sections here freely — the screen renders them in order.
// To add a question: add a FaqItem to the relevant section's `items` list.
// To add a section: add a new FaqSection entry to FAQ_SECTIONS.

private val FAQ_SECTIONS = listOf(

    FaqSection(
        title = "Getting Started",
        icon  = Icons.Outlined.PlayArrow,
        items = listOf(
            FaqItem(
                question = "What is TraceLedger?",
                answer   = "TraceLedger is a privacy-first personal finance app. It tracks your income, expenses, and transfers entirely on your device — no cloud, no accounts, no internet required."
            ),
            FaqItem(
                question = "How do I add my first transaction?",
                answer   = "Tap the + button in the bottom navigation bar from any screen. Choose Expense, Income, or Transfer, fill in the details, and tap Save."
            ),
            FaqItem(
                question = "Does TraceLedger require internet access?",
                answer   = "No. The app works entirely offline. Internet is only used optionally to check for app updates, which you can trigger manually from Settings → About."
            ),
            FaqItem(
                question = "Is my data backed up to the cloud?",
                answer   = "No. All data is stored only on your device. Use Settings → Export Data to create local backups you can save to Google Drive or any storage you choose."
            )
        )
    ),

    FaqSection(
        title = "Transactions",
        icon  = Icons.Outlined.SwapHoriz,
        items = listOf(
            FaqItem(
                question = "What transaction types are available?",
                answer   = "Expense: money leaving an account. Income: money entering an account. Transfer: money moving between two of your own accounts (e.g. wallet to bank). Transfer does not require a category."
            ),
            FaqItem(
                question = "How do I edit or delete a transaction?",
                answer   = "Tap any transaction in the Transactions tab or on the Dashboard to open its detail sheet. From there you can tap Edit or Delete."
            ),
            FaqItem(
                question = "Can I add a note to a transaction?",
                answer   = "Yes. The Note field is optional and available on all transaction types. Notes are also searchable from the Transactions tab search/filter."
            ),
            FaqItem(
                question = "How does transaction search and filtering work?",
                answer   = "Tap the search icon in the Transactions tab. You can filter by category, date range, note text, transaction type, and amount. Multiple filters can be active at the same time."
            ),
            FaqItem(
                question = "What is a recurring transaction?",
                answer   = "A recurring transaction is a template that auto-creates a transaction on a set schedule (daily, weekly, monthly, etc.). Set them up in Settings → Recurring. They run in the background and are generated automatically on the due date."
            )
        )
    ),

    FaqSection(
        title = "Accounts",
        icon  = Icons.Outlined.AccountBalance,
        items = listOf(
            FaqItem(
                question = "What account types are supported?",
                answer   = "Bank, Wallet (e.g. Paytm, PhonePe), Cash, and Credit Card. Each type has relevant fields — for example, Credit Card has a credit limit field."
            ),
            FaqItem(
                question = "How do I set my opening balance?",
                answer   = "When adding or editing an account, enter the current real-world balance in the Balance field. TraceLedger will track changes from that point onward."
            ),
            FaqItem(
                question = "Can I assign a colour to an account?",
                answer   = "Yes. When adding or editing an account, you can pick from 20 colours. The colour appears on account cards and in charts to help you identify each account at a glance."
            ),
            FaqItem(
                question = "How do I delete an account?",
                answer   = "Open the account from the Accounts screen and tap Delete. Accounts with associated transactions cannot be deleted — reassign or delete those transactions first."
            )
        )
    ),

    FaqSection(
        title = "Categories",
        icon  = Icons.Outlined.Category,
        items = listOf(
            FaqItem(
                question = "Are expense and income categories separate?",
                answer   = "Yes. A category belongs to either Expense or Income. Some may share the same name (e.g. 'Other') but they are tracked independently and shown separately in Statistics."
            ),
            FaqItem(
                question = "Can I create custom categories?",
                answer   = "Yes. Go to Settings → Categories, tap the + button, enter a name, pick a colour, and save. Custom categories can be edited or deleted at any time."
            ),
            FaqItem(
                question = "Why can't I delete a category?",
                answer   = "A category cannot be deleted if any transaction has been recorded under it. To delete the category, first reassign those transactions to a different category or delete them."
            ),
            FaqItem(
                question = "Do Transfer transactions need a category?",
                answer   = "No. Transfers move money between your own accounts and do not require a category. Only Expense and Income transactions are categorised."
            )
        )
    ),

    FaqSection(
        title = "Budgets",
        icon  = Icons.Outlined.PieChart,
        items = listOf(
            FaqItem(
                question = "How do budgets work?",
                answer   = "A budget sets a monthly spending limit for a specific expense category. The Dashboard shows your progress. Budgets reset automatically at the start of each month."
            ),
            FaqItem(
                question = "What do the warning and exceeded colours mean?",
                answer   = "Warning (amber) appears when you have spent 80% or more of a budget for the month. Exceeded (red) means the limit has been passed. You can still add transactions — the budget is informational only."
            ),
            FaqItem(
                question = "How do I set or edit a budget?",
                answer   = "Go to Settings → Budgets, or tap the Budgets section on the Dashboard. Tap any category to set a limit or tap an existing budget to edit or delete it."
            )
        )
    ),

    FaqSection(
        title = "SMS Detection",
        // Filled Sms icon (Outlined variant not in the standard set)
        icon  = Icons.Default.Sms,
        items = listOf(
            FaqItem(
                question = "How does SMS detection work?",
                answer   = "When enabled, TraceLedger reads financial SMS messages from your bank or wallet and extracts the transaction amount, type, and merchant. All processing happens on-device — no data is sent anywhere."
            ),
            FaqItem(
                question = "Does the app read all my SMS?",
                answer   = "No. TraceLedger only processes messages that match known financial patterns (debit/credit keywords, amount patterns, known bank sender IDs). Personal messages are ignored."
            ),
            FaqItem(
                question = "What permissions are needed?",
                answer   = "RECEIVE_SMS for real-time detection of incoming messages, and READ_SMS for scanning your existing inbox. Both are optional and only requested when you explicitly enable the feature in Settings → SMS Detection."
            ),
            FaqItem(
                question = "Is anything saved automatically?",
                answer   = "No. Detected transactions go into a review queue first. Nothing is added to your ledger until you explicitly approve each transaction on the SMS Review screen."
            ),
            FaqItem(
                question = "What are custom SMS rules?",
                answer   = "If the built-in engine misses a specific bank or wallet SMS format, you can define a custom rule in Settings → SMS Detection → Manage Custom Rules. Set the sender pattern, keywords, and default account/category, then test it with a real SMS before saving."
            ),
            FaqItem(
                question = "What is the learning engine?",
                answer   = "When you correct an account or category suggestion on the SMS Review screen, TraceLedger silently learns from that correction and applies it to all future SMS from the same sender. No action required — it improves automatically."
            )
        )
    ),

    FaqSection(
        title = "Import & Export",
        icon  = Icons.Outlined.ImportExport,
        items = listOf(
            FaqItem(
                question = "How do I back up my data?",
                answer   = "Go to Settings → Export Data and choose JSON (full backup: accounts, categories, budgets, and transactions) or CSV (transactions only). Save the file to any location you choose."
            ),
            FaqItem(
                question = "How do I restore a backup?",
                answer   = "Go to Settings → Import Data and select your backup file. JSON import replaces all existing data. CSV import adds transactions to your existing accounts and categories."
            ),
            FaqItem(
                question = "How do I import a bank statement?",
                answer   = "Go to Settings → Import Transactions. Select an account, pick the statement file (PDF or CSV), review the detected transactions on the next screen, and tap Confirm. Support varies by bank and file format."
            ),
            FaqItem(
                question = "What file formats are supported?",
                answer   = "JSON and CSV for full data backups. PDF and CSV for bank statement import via the Import Transactions flow. The statement parser works best with standard CSV exports from most Indian banks."
            )
        )
    ),

    FaqSection(
        title = "Privacy & Security",
        icon  = Icons.Outlined.Lock,
        items = listOf(
            FaqItem(
                question = "What data does TraceLedger collect?",
                answer   = "None. TraceLedger has no analytics, no crash reporting, no advertising SDKs, and no server-side components. Your financial data never leaves your device."
            ),
            FaqItem(
                question = "Can I use the app without granting any permissions?",
                answer   = "Yes. The core app — adding and viewing transactions, budgets, statistics, and export — requires zero permissions. Optional features (SMS detection, notifications) only request permissions when you enable them."
            ),
            FaqItem(
                question = "Is the database encrypted?",
                answer   = "The database is stored in the app's private storage, protected by Android's app sandbox and inaccessible to other apps. Full database encryption is planned for a future release."
            )
        )
    )
)

// ── Screen ─────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onBack: () -> Unit) {
    val uriHandler  = LocalUriHandler.current
    var searchQuery by remember { mutableStateOf("") }

    // Filter FAQ sections/items based on search query (case-insensitive)
    val filteredSections = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            FAQ_SECTIONS
        } else {
            val q = searchQuery.trim().lowercase()
            FAQ_SECTIONS.mapNotNull { section ->
                val matching = section.items.filter { item ->
                    item.question.lowercase().contains(q) ||
                            item.answer.lowercase().contains(q) ||
                            section.title.lowercase().contains(q)
                }
                if (matching.isNotEmpty()) section.copy(items = matching) else null
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text  = "HELP & FAQ",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint               = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier            = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Search bar ────────────────────────────────────────────────────
            item {
                OutlinedTextField(
                    value         = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier      = Modifier.fillMaxWidth(),
                    placeholder   = {
                        Text(
                            text  = "Search questions...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector        = Icons.Outlined.Search,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier           = Modifier.size(20.dp)
                        )
                    },
                    // Show clear button only when there is text
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector        = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    modifier           = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else null,
                    singleLine      = true,
                    shape           = RoundedCornerShape(14.dp),
                    colors          = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        unfocusedBorderColor    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        focusedContainerColor   = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                )
            }

            // ── No-results empty state ────────────────────────────────────────
            if (filteredSections.isEmpty()) {
                item {
                    Box(
                        modifier         = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector        = Icons.Outlined.Search,
                                contentDescription = null,
                                tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                modifier           = Modifier.size(44.dp)
                            )
                            Text(
                                text  = "No results for \"$searchQuery\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                            Text(
                                text  = "Try a different word",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                            )
                        }
                    }
                }
            }

            // ── FAQ sections (one card per section) ───────────────────────────
            items(
                items = filteredSections,
                key   = { it.title }
            ) { section ->
                FaqSectionCard(section = section)
            }

            // ── "Still need help?" card (only shown when not searching) ───────
            if (searchQuery.isBlank()) {
                item {
                    HelpSectionLabel("STILL NEED HELP?")
                }
                item {
                    Card(
                        shape    = RoundedCornerShape(20.dp),
                        colors   = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            // Discord community link
                            CommunityLinkRow(
                                icon     = Icons.Default.Forum,
                                title    = "Discord Community",
                                subtitle = "Ask questions, share feedback",
                                onClick  = { uriHandler.openUri(AppLinks.DISCORD) }
                            )
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                modifier  = Modifier.padding(horizontal = 16.dp)
                            )
                            // Website link
                            CommunityLinkRow(
                                icon     = Icons.Default.Language,
                                title    = "Website",
                                subtitle = "traceledger.pages.dev",
                                onClick  = { uriHandler.openUri(AppLinks.WEBSITE) }
                            )
                        }
                    }
                }
            }

            // ── Bottom breathing room ─────────────────────────────────────────
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FaqSectionCard — groups all FaqItems for one section into a single card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun FaqSectionCard(section: FaqSection) {
    Card(
        shape    = RoundedCornerShape(20.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {

            // Section header row (non-clickable — just a label with icon)
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icon badge
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(9.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = section.icon,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.primary,
                        modifier           = Modifier.size(17.dp)
                    )
                }
                Text(
                    text  = section.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )

            // FAQ items — separated by a faint divider
            section.items.forEachIndexed { index, item ->
                FaqItemRow(item = item)
                // Divider between items (but not after the last one)
                if (index < section.items.lastIndex) {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                        modifier  = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FaqItemRow — a single collapsible question/answer pair
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun FaqItemRow(item: FaqItem) {
    // Each item manages its own expanded state independently
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        // Question row
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text     = item.question,
                style    = MaterialTheme.typography.bodyMedium,
                color    = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            // Expand/collapse chevron
            Icon(
                imageVector        = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier           = Modifier.size(18.dp)
            )
        }

        // Answer — animated expand/collapse
        AnimatedVisibility(
            visible = expanded,
            enter   = expandVertically(),
            exit    = shrinkVertically()
        ) {
            Text(
                text     = item.answer,
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                modifier = Modifier
                    .fillMaxWidth()
                    // Subtle background tint distinguishes answer from question
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.04f))
                    .padding(start = 20.dp, end = 20.dp, top = 0.dp, bottom = 16.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CommunityLinkRow — external link row used in the "Still need help?" card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CommunityLinkRow(
    icon     : ImageVector,
    title    : String,
    subtitle : String,
    onClick  : () -> Unit
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.primary,
            modifier           = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text  = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            )
        }
        // External link indicator
        Icon(
            imageVector        = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
            modifier           = Modifier.size(15.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HelpSectionLabel — section header label above cards
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun HelpSectionLabel(text: String) {
    Text(
        text     = text,
        style    = MaterialTheme.typography.labelSmall,
        color    = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}