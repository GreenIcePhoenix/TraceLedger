package com.greenicephoenix.traceledger.feature.templates

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.core.ui.theme.NothingRed
import com.greenicephoenix.traceledger.domain.model.AccountUiModel
import com.greenicephoenix.traceledger.domain.model.CategoryUiModel
import com.greenicephoenix.traceledger.domain.model.TransactionType
import com.greenicephoenix.traceledger.feature.addtransaction.AccountSelector
import com.greenicephoenix.traceledger.feature.addtransaction.CategorySelector
import com.greenicephoenix.traceledger.feature.templates.domain.TransactionTemplateUiModel
import java.util.UUID

/**
 * Form screen for creating or editing a transaction template.
 *
 * Differences from AddTransactionScreen:
 *  - Has a "Template Name" field at the top (required)
 *  - Amount is optional — leave blank to not preset it
 *  - No date field — date is always set fresh when the template is applied
 *  - No delete button — deletion is handled from TemplatesScreen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTemplateScreen(
    existingTemplate: TransactionTemplateUiModel?,
    accounts: List<AccountUiModel>,
    categories: List<CategoryUiModel>,
    onSave: (TransactionTemplateUiModel) -> Unit,
    onCancel: () -> Unit
) {
    val isEdit = existingTemplate != null

    // ── Local form state ──────────────────────────────────────────────────────
    var name          by remember { mutableStateOf(existingTemplate?.name ?: "") }
    var type          by remember { mutableStateOf(existingTemplate?.type ?: TransactionType.EXPENSE) }
    var amount        by remember { mutableStateOf(existingTemplate?.amount?.toPlainString() ?: "") }
    var fromAccountId by remember { mutableStateOf(existingTemplate?.fromAccountId) }
    var toAccountId   by remember { mutableStateOf(existingTemplate?.toAccountId) }
    var categoryId    by remember { mutableStateOf(existingTemplate?.categoryId) }
    var notes         by remember { mutableStateOf(existingTemplate?.notes ?: "") }

    var nameError by remember { mutableStateOf(false) }

    val canSave = name.isNotBlank()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header
            Row(
                modifier          = Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onBackground)
                }
                Text(
                    text      = if (isEdit) "Edit Template" else "New Template",
                    modifier  = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color     = MaterialTheme.colorScheme.onBackground,
                    style     = MaterialTheme.typography.titleMedium
                )
                IconButton(
                    onClick  = {
                        if (name.isBlank()) { nameError = true; return@IconButton }
                        onSave(
                            TransactionTemplateUiModel(
                                id            = existingTemplate?.id ?: UUID.randomUUID().toString(),
                                name          = name.trim(),
                                type          = type,
                                amount        = amount.toBigDecimalOrNull(),
                                fromAccountId = fromAccountId,
                                toAccountId   = toAccountId,
                                categoryId    = categoryId,
                                notes         = notes.takeIf { it.isNotBlank() }
                            )
                        )
                    },
                    enabled  = canSave
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Save",
                        tint = if (canSave) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))

            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                    .padding(16.dp)
            ) {
                LazyColumn(
                    modifier            = Modifier.fillMaxSize(),
                    contentPadding      = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Template name (required)
                    item {
                        OutlinedTextField(
                            value         = name,
                            onValueChange = { name = it; nameError = false },
                            label         = { Text("Template Name *") },
                            placeholder   = { Text("e.g. Monthly Rent") },
                            isError       = nameError,
                            supportingText = if (nameError) {{ Text("Name is required") }} else null,
                            modifier      = Modifier.fillMaxWidth(),
                            singleLine    = true
                        )
                    }

                    // Transaction type selector
                    item {
                        // Reuse the same 3-segment type selector as AddTransactionScreen
                        com.greenicephoenix.traceledger.feature.addtransaction.TransactionTypeSelector(
                            selected   = type,
                            onSelected = { newType ->
                                type = newType
                                // Clear fields that don't apply to the new type
                                when (newType) {
                                    TransactionType.EXPENSE  -> toAccountId = null
                                    TransactionType.INCOME   -> fromAccountId = null
                                    TransactionType.TRANSFER -> categoryId = null
                                }
                            }
                        )
                    }

                    // Amount (optional)
                    item {
                        OutlinedTextField(
                            value         = amount,
                            onValueChange = {
                                if (it.isEmpty() || it.matches(Regex("""\d+(\.\d{0,2})?""")))
                                    amount = it
                            },
                            label         = { Text("Amount (optional)") },
                            placeholder   = { Text("Leave blank to enter each time") },
                            modifier      = Modifier.fillMaxWidth(),
                            singleLine    = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            )
                        )
                    }

                    // Category (EXPENSE / INCOME only)
                    if (type != TransactionType.TRANSFER) {
                        item {
                            CategorySelector(
                                categories         = categories,
                                type               = if (type == TransactionType.EXPENSE)
                                    com.greenicephoenix.traceledger.domain.model.CategoryType.EXPENSE
                                else
                                    com.greenicephoenix.traceledger.domain.model.CategoryType.INCOME,
                                selectedCategoryId = categoryId,
                                onSelect           = { categoryId = it }
                            )
                        }
                    }

                    // From account
                    if (type == TransactionType.EXPENSE || type == TransactionType.TRANSFER) {
                        item {
                            AccountSelector(
                                label             = if (type == TransactionType.TRANSFER) "From Account" else "Account",
                                accounts          = accounts,
                                selectedAccountId = fromAccountId,
                                onSelect          = { fromAccountId = it }
                            )
                        }
                    }

                    // To account
                    if (type == TransactionType.INCOME || type == TransactionType.TRANSFER) {
                        item {
                            AccountSelector(
                                label             = if (type == TransactionType.TRANSFER) "To Account" else "Account",
                                accounts          = accounts,
                                selectedAccountId = toAccountId,
                                onSelect          = { toAccountId = it }
                            )
                        }
                    }

                    // Notes
                    item {
                        OutlinedTextField(
                            value         = notes,
                            onValueChange = { notes = it },
                            label         = { Text("Notes (optional)") },
                            modifier      = Modifier.fillMaxWidth(),
                            minLines      = 2,
                            maxLines      = 4
                        )
                    }
                }
            }
        }
    }
}