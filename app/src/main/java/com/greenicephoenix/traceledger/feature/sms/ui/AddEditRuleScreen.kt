package com.greenicephoenix.traceledger.feature.sms.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenicephoenix.traceledger.domain.model.AccountUiModel
import com.greenicephoenix.traceledger.domain.model.CategoryUiModel
import com.greenicephoenix.traceledger.feature.sms.viewmodel.AddEditRuleViewModel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import com.greenicephoenix.traceledger.feature.sms.viewmodel.RuleTesterState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRuleScreen(
    viewModel: AddEditRuleViewModel,
    accounts: List<AccountUiModel>,
    categories: List<CategoryUiModel>,
    isEditMode: Boolean,
    onNavigateBack: () -> Unit,
) {
    val form   by viewModel.form.collectAsState()
    val tester by viewModel.tester.collectAsState()

    // Navigate back once save succeeds
    LaunchedEffect(form.isSaved) {
        if (form.isSaved) onNavigateBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        // ── Header ────────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint               = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text     = if (isEditMode) "EDIT RULE" else "ADD RULE",
                    style    = MaterialTheme.typography.headlineMedium,
                    color    = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        // ── Scrollable form ───────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Section 1: Basics ─────────────────────────────────────────────
            FormSection(title = "BASICS") {
                OutlinedTextField(
                    value         = form.name,
                    onValueChange = viewModel::updateName,
                    label         = { Text("Rule name") },
                    placeholder   = { Text("e.g. Canteen wallet, ICICI CC") },
                    isError       = form.nameError != null,
                    supportingText = form.nameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value         = form.senderPattern,
                    onValueChange = viewModel::updateSenderPattern,
                    label         = { Text("Sender ID contains") },
                    placeholder   = { Text("e.g. HDFCBK, AXISBK, PHONEPE") },
                    isError       = form.senderError != null,
                    supportingText = form.senderError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
                        ?: { Text("Case-insensitive match on the sender field", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(4.dp))

                // Rule type selector
                Text(
                    "RULE TYPE",
                    style         = MaterialTheme.typography.labelSmall,
                    color         = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    listOf(false to "Detect transaction", true to "Always exclude").forEach { (isExclusion, label) ->
                        val selected = form.isExclusionRule == isExclusion
                        Surface(
                            color    = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            shape    = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            TextButton(
                                onClick = { viewModel.updateIsExclusion(isExclusion) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    label,
                                    style  = MaterialTheme.typography.labelMedium,
                                    color  = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                if (form.isExclusionRule) {
                    Surface(
                        color  = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                        shape  = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Any SMS from this sender will be silently discarded. " +
                                    "No parsing fields needed.",
                            modifier = Modifier.padding(12.dp),
                            style    = MaterialTheme.typography.bodySmall,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Section 2: Parsing (only shown for non-exclusion rules) ────────
            if (!form.isExclusionRule) {
                FormSection(title = "PARSING") {

                    // ── Mode toggle ───────────────────────────────────────────
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Advanced mode",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "Use a custom regex pattern instead of keywords",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked         = form.isAdvancedMode,
                            onCheckedChange = viewModel::updateIsAdvancedMode
                        )
                    }

                    // ── Simple mode — keyword fields ──────────────────────────
                    AnimatedVisibility(
                        visible = !form.isAdvancedMode,
                        enter   = expandVertically(),
                        exit    = shrinkVertically()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value          = form.amountPrefix,
                                onValueChange  = viewModel::updateAmountPrefix,
                                label          = { Text("Amount keyword (optional)") },
                                placeholder    = { Text("e.g. Rs., INR, ₹") },
                                supportingText = { Text("The word just before the amount in the SMS", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                modifier       = Modifier.fillMaxWidth(),
                                shape          = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value          = form.debitKeywords,
                                onValueChange  = viewModel::updateDebitKeywords,
                                label          = { Text("Debit keywords (optional)") },
                                placeholder    = { Text("e.g. debited,spent,paid") },
                                supportingText = { Text("Comma-separated. Presence → EXPENSE", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                modifier       = Modifier.fillMaxWidth(),
                                shape          = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value          = form.creditKeywords,
                                onValueChange  = viewModel::updateCreditKeywords,
                                label          = { Text("Credit keywords (optional)") },
                                placeholder    = { Text("e.g. credited,received") },
                                supportingText = { Text("Comma-separated. Presence → INCOME", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                modifier       = Modifier.fillMaxWidth(),
                                shape          = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value          = form.merchantKeyword,
                                onValueChange  = viewModel::updateMerchantKeyword,
                                label          = { Text("Merchant keyword (optional)") },
                                placeholder    = { Text("e.g. Info:, Merchant:, at ") },
                                supportingText = { Text("Text after this keyword is used as the description", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                modifier       = Modifier.fillMaxWidth(),
                                shape          = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    // ── Advanced mode — raw regex field ───────────────────────
                    AnimatedVisibility(
                        visible = form.isAdvancedMode,
                        enter   = expandVertically(),
                        exit    = shrinkVertically()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                            // Regex input
                            OutlinedTextField(
                                value          = form.rawRegex,
                                onValueChange  = viewModel::updateRawRegex,
                                label          = { Text("Regex pattern") },
                                placeholder    = { Text("(?<amount>[\\d,]+).*(?<description>.+)") },
                                isError        = form.regexError != null,
                                supportingText = form.regexError?.let {
                                    { Text(it, color = MaterialTheme.colorScheme.error) }
                                } ?: { Text("Kotlin regex with named capture groups", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                minLines       = 3,
                                modifier       = Modifier.fillMaxWidth(),
                                shape          = RoundedCornerShape(12.dp),
                                textStyle      = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            )

                            // Named groups reference card
                            Surface(
                                color    = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                                shape    = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier            = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        "NAMED CAPTURE GROUPS",
                                        style         = MaterialTheme.typography.labelSmall,
                                        color         = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 0.8.sp
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    RegexGroupRow("(?<amount>...)",      "Transaction amount — digits, commas, dots")
                                    RegexGroupRow("(?<description>...)", "Merchant / description text")
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Direction (EXPENSE / INCOME) is still detected from debit / credit " +
                                                "keywords in the matched text. Use the tester below to verify.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Section 3: Defaults ───────────────────────────────────────
                FormSection(title = "DEFAULTS (OPTIONAL)") {
                    Text(
                        "Pre-select account and category on the review screen for SMS matching this rule.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))

                    // Account picker
                    AccountDropdown(
                        accounts          = accounts,
                        selectedAccountId = form.defaultAccountId,
                        onSelect          = viewModel::updateDefaultAccount
                    )

                    // Category picker
                    CategoryDropdown(
                        categories          = categories,
                        selectedCategoryId  = form.defaultCategoryId,
                        onSelect            = viewModel::updateDefaultCategory
                    )

                    // Priority
                    Row(
                        modifier          = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Priority", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.updatePriority(form.priority - 1) }) {
                            Text("−", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Text(
                            "${form.priority}",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color      = MaterialTheme.colorScheme.primary,
                            modifier   = Modifier.widthIn(min = 32.dp),
                            textAlign  = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        IconButton(onClick = { viewModel.updatePriority(form.priority + 1) }) {
                            Text("+", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Text(
                            "Higher = checked first",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Enabled toggle
                    Row(
                        modifier          = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Rule enabled", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = form.isEnabled, onCheckedChange = viewModel::updateEnabled)
                    }
                }

                // ── Section 4: Rule Tester ────────────────────────────────────
                RuleTesterSection(
                    testerState = tester,
                    onTest      = viewModel::testRule,
                    onReset     = viewModel::resetTester
                )
            }

            // ── Save button ───────────────────────────────────────────────────
            Button(
                onClick  = viewModel::save,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("SAVE RULE", style = MaterialTheme.typography.labelLarge, letterSpacing = 1.sp)
            }

            Spacer(Modifier.height(96.dp))
        }
    }
}

// ── Rule Tester ───────────────────────────────────────────────────────────────

@Composable
private fun RuleTesterSection(
    testerState: RuleTesterState,
    onTest: (String) -> Unit,
    onReset: () -> Unit,
) {
    var smsInput by remember { mutableStateOf("") }

    FormSection(title = "TEST YOUR RULE") {
        Text(
            "Paste a real SMS from the sender above to verify your rule extracts the right data.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value         = smsInput,
            onValueChange = { smsInput = it; onReset() },
            label         = { Text("Paste SMS here") },
            minLines      = 3,
            maxLines      = 6,
            modifier      = Modifier.fillMaxWidth(),
            shape         = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(8.dp))

        FilledTonalButton(
            onClick  = { onTest(smsInput) },
            enabled  = smsInput.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(12.dp)
        ) {
            Text("TEST RULE", letterSpacing = 0.5.sp)
        }

        // Result
        when (val state = testerState) {
            is RuleTesterState.Idle -> { /* nothing */ }

            is RuleTesterState.Detected -> {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color  = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape  = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("✓ RULE MATCHED", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, letterSpacing = 0.5.sp)
                        TesterRow("Amount",      "₹${state.amount}")
                        TesterRow("Type",        state.type)
                        TesterRow("Description", state.description)
                    }
                }
            }

            is RuleTesterState.WouldBeExcluded -> {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color  = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    shape  = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "✓ This SMS would be EXCLUDED (silently discarded).",
                        modifier = Modifier.padding(12.dp),
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.error
                    )
                }
            }

            is RuleTesterState.NoMatch -> {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color  = MaterialTheme.colorScheme.surfaceVariant,
                    shape  = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "✗ No match — ${state.reason}",
                        modifier = Modifier.padding(12.dp),
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TesterRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(80.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

// ── Pickers ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountDropdown(
    accounts: List<AccountUiModel>,
    selectedAccountId: String?,
    onSelect: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = accounts.find { it.id == selectedAccountId }?.name ?: "None"

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value         = selectedName,
            onValueChange = {},
            readOnly      = true,
            label         = { Text("Default account") },
            trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier      = Modifier.menuAnchor().fillMaxWidth(),
            shape         = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("None") }, onClick = { onSelect(null); expanded = false })
            accounts.forEach { account ->
                DropdownMenuItem(
                    text    = { Text(account.name) },
                    onClick = { onSelect(account.id); expanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    categories: List<CategoryUiModel>,
    selectedCategoryId: String?,
    onSelect: (String?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = categories.find { it.id == selectedCategoryId }?.name ?: "None"

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value         = selectedName,
            onValueChange = {},
            readOnly      = true,
            label         = { Text("Default category") },
            trailingIcon  = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier      = Modifier.menuAnchor().fillMaxWidth(),
            shape         = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("None") }, onClick = { onSelect(null); expanded = false })
            categories.forEach { category ->
                DropdownMenuItem(
                    text    = {
                        Column {
                            Text(category.name)
                            Text(category.type.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    onClick = { onSelect(category.id); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun RegexGroupRow(group: String, description: String) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.Top
    ) {
        Text(
            text       = group,
            style      = MaterialTheme.typography.labelSmall.copy(
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            ),
            color      = MaterialTheme.colorScheme.primary,
            modifier   = Modifier.widthIn(min = 160.dp)
        )
        Text(
            text  = description,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Form section wrapper ──────────────────────────────────────────────────────
@Composable
private fun FormSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            style         = MaterialTheme.typography.labelMedium,
            color         = MaterialTheme.colorScheme.primary,
            fontWeight    = FontWeight.SemiBold,
            letterSpacing = 1.sp
        )
        Surface(
            color    = MaterialTheme.colorScheme.surface,
            shape    = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier            = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) { content() }
        }
    }
}