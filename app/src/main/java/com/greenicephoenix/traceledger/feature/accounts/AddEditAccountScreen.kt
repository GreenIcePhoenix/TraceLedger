package com.greenicephoenix.traceledger.feature.accounts

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.core.ui.theme.AccountColors
import com.greenicephoenix.traceledger.core.ui.theme.NothingRed
import com.greenicephoenix.traceledger.domain.model.AccountType
import androidx.compose.foundation.layout.height
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.unit.sp
import com.greenicephoenix.traceledger.domain.model.AccountUiModel
import androidx.compose.ui.graphics.toArgb
import java.math.BigDecimal
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction

/**
 * Add / Edit Account Screen
 *
 * - Cancel (✕) and Save (✓) in TopAppBar
 * - Fully scrollable
 * - Credit card extra fields hidden behind expandable "Optional details"
 * - Credit card fields are informational only
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAccountScreen(
    existingAccount: AccountUiModel? = null,
    onCancel: () -> Unit,
    onSave: (AccountUiModel) -> Unit
) {
    // ---------------- STATE ----------------

    val isEditMode = existingAccount != null

    val editKey = existingAccount?.id

    var accountType by remember(editKey) {
        mutableStateOf(existingAccount?.type ?: AccountType.BANK)
    }

    var name by remember(editKey) {
        mutableStateOf(existingAccount?.name ?: "")
    }

    var balance by remember(editKey) {
        mutableStateOf(existingAccount?.balance?.toPlainString() ?: "")
    }

    var details by remember(editKey) {
        mutableStateOf(existingAccount?.details ?: "")
    }

    var includeInTotal by remember(editKey) {
        mutableStateOf(existingAccount?.includeInTotal ?: true)
    }

    var selectedColor by remember(editKey) {
        mutableStateOf(
            if (existingAccount != null) {
                Color(existingAccount.color.toInt())
            } else {
                AccountColors.first()
            }
        )
    }


    val isValidAccount = name.isNotBlank()

    // Credit card optional state
    var showCreditCardDetails by remember { mutableStateOf(false) }
    var creditLimit by remember { mutableStateOf("") }
    var billingDay by remember { mutableStateOf("") }
    var dueDay by remember { mutableStateOf("") }

    // ---------------- SCAFFOLD ----------------

    Scaffold { padding ->

        Column(
            modifier = Modifier
                //.fillMaxSize()
                //.padding(paddingValues)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {

            // =========================
            // COMPACT CUSTOM HEADER
            // =========================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                Text(
                    text = if (isEditMode) "Edit Account" else "Add Account",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )

                IconButton(
                    onClick = {
                        if (!isValidAccount) return@IconButton

                        val numericBalance =
                            balance.toBigDecimalOrNull() ?: BigDecimal.ZERO

                        val account = AccountUiModel(
                            id = existingAccount?.id ?: System.currentTimeMillis().toString(),
                            name = name.trim(),
                            balance = numericBalance,
                            type = accountType,
                            includeInTotal = includeInTotal,
                            details = details.takeIf { it.isNotBlank() },
                            color = selectedColor.toArgb().toLong()
                        )
                        Log.d(
                            "ACCOUNT_COLOR",
                            "Saving color = ${selectedColor.value.toString(16)}"
                        )

                        onSave(account)
                    },
                    enabled = isValidAccount
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Save",
                        tint = if (isValidAccount) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
            )

            // =========================
            // FORM CONTENT
            // =========================
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.TopCenter
            ) {
                Card(
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .padding(horizontal = 16.dp)
                        //.heightIn(max = 720.dp)
                        .fillMaxHeight()
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(5.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        item {
                            AddAccountFormContent(
                                name = name,
                                onNameChange = { name = it },
                                accountType = accountType,
                                onAccountTypeChange = { accountType = it },
                                balance = balance,
                                onBalanceChange = { balance = it },
                                selectedColor = selectedColor,
                                onColorSelect = { selectedColor = it },
                                includeInTotal = includeInTotal,
                                onIncludeInTotalChange = { includeInTotal = it },
                                details = details,
                                onDetailsChange = { details = it },
                                showCreditCardDetails = showCreditCardDetails,
                                onToggleCreditCardDetails = {
                                    showCreditCardDetails = !showCreditCardDetails
                                },
                                creditLimit = creditLimit,
                                onCreditLimitChange = { creditLimit = it },
                                billingDay = billingDay,
                                onBillingDayChange = { billingDay = it },
                                dueDay = dueDay,
                                onDueDayChange = { dueDay = it }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/* ------------------------ ACCOUNT TYPE SELECTOR ---------------------------- */
/* -------------------------------------------------------------------------- */

@Composable
private fun AccountTypeSelector(
    selected: AccountType,
    onSelected: (AccountType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                MaterialTheme.colorScheme.surfaceVariant
            )
            .padding(4.dp)
    ) {
        AccountType.entries.forEach { type ->
            val isSelected = type == selected

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                        else Color.Transparent
                    )
                    .clickable { onSelected(type) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = type.name.replace("_", " "),
                    color =
                        if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun AddAccountFormContent(
    name: String,
    onNameChange: (String) -> Unit,
    accountType: AccountType,
    onAccountTypeChange: (AccountType) -> Unit,
    balance: String,
    onBalanceChange: (String) -> Unit,
    selectedColor: Color?,
    onColorSelect: (Color) -> Unit,
    includeInTotal: Boolean,
    onIncludeInTotalChange: (Boolean) -> Unit,
    details: String,
    onDetailsChange: (String) -> Unit,
    showCreditCardDetails: Boolean,
    onToggleCreditCardDetails: () -> Unit,
    creditLimit: String,
    onCreditLimitChange: (String) -> Unit,
    billingDay: String,
    onBillingDayChange: (String) -> Unit,
    dueDay: String,
    onDueDayChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {

        AccountLabelField(
            name = name,
            onNameChange = onNameChange,
            label = "Account Name"
        )

        AccountTypeSelector(
            selected = accountType,
            onSelected = onAccountTypeChange
        )

        BalanceField(balance, onBalanceChange)

        NotesField(details, onDetailsChange)

        IncludeInTotalRow(includeInTotal, onIncludeInTotalChange)

        SectionLabel("ACCENT COLOR")
        ColorProtocolPicker(selectedColor, onColorSelect)

        if (accountType == AccountType.CREDIT_CARD) {
            CreditCardSection(
                expanded = showCreditCardDetails,
                onToggle = onToggleCreditCardDetails,
                creditLimit = creditLimit,
                onCreditLimitChange = onCreditLimitChange,
                billingDay = billingDay,
                onBillingDayChange = onBillingDayChange,
                dueDay = dueDay,
                onDueDayChange = onDueDayChange
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun AccountLabelField(
    name: String,
    onNameChange: (String) -> Unit,
    label: String
) {
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true
    )
}

@Composable
private fun TypeAndBalanceRow(
    accountType: AccountType,
    onAccountTypeChange: (AccountType) -> Unit,
    balance: String,
    onBalanceChange: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {

        Box(modifier = Modifier.weight(1f)) {
            AccountTypeSegmentedSelector(
                selected = accountType,
                onSelected = onAccountTypeChange
            )
        }

        OutlinedTextField(
            value = balance,
            onValueChange = onBalanceChange,
            label = { Text("BALANCE") },
            modifier = Modifier.weight(1f),
            singleLine = true
        )
    }
}


@Composable
private fun ColorProtocolPicker(
    selectedColor: Color?,
    onColorSelect: (Color) -> Unit
) {
    val colors = AccountColors.take(18)

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 44.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 220.dp), // ✅ CRITICAL FIX
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(colors) { color ->
            val selected = selectedColor == color

            Box(
                modifier = Modifier
                    .size(44.dp) // ✅ square grid cell / touch target
                    .clickable { onColorSelect(color) },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp) // ✅ visual circle
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (selected) 3.dp else 1.dp,
                            color =
                                if (selected)
                                    MaterialTheme.colorScheme.onSurface
                                else
                                    Color.Transparent,
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

@Composable
private fun IncludeInTotalRow(
    includeInTotal: Boolean,
    onIncludeInTotalChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Include in totals",
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyMedium
        )

        Switch(
            checked = includeInTotal,
            onCheckedChange = onIncludeInTotalChange
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        letterSpacing = 1.2.sp
    )
}

@Composable
private fun BalanceField(
    balance: String,
    onBalanceChange: (String) -> Unit
) {
    OutlinedTextField(
        value = balance,
        onValueChange = { newValue ->
            // Allow digits and at most one decimal point
            if (newValue.matches(Regex("^\\d*(\\.\\d*)?$"))) {
                onBalanceChange(newValue)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("BALANCE") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done
        )
    )
}

@Composable
private fun NotesField(
    details: String,
    onDetailsChange: (String) -> Unit
) {
    OutlinedTextField(
        value = details,
        onValueChange = onDetailsChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Notes") },
        singleLine = true
    )
}

@Composable
private fun CreditCardSection(
    expanded: Boolean,
    onToggle: () -> Unit,
    creditLimit: String,
    onCreditLimitChange: (String) -> Unit,
    billingDay: String,
    onBillingDayChange: (String) -> Unit,
    dueDay: String,
    onDueDayChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "OPTIONAL CREDIT CARD DETAILS",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.labelMedium
            )

            Icon(
                imageVector =
                    if (expanded) Icons.Default.ExpandLess
                    else Icons.Default.ExpandMore,
                contentDescription = null
            )
        }

        if (expanded) {
            OutlinedTextField(
                value = creditLimit,
                onValueChange = onCreditLimitChange,
                label = { Text("Credit Limit") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = billingDay,
                    onValueChange = onBillingDayChange,
                    label = { Text("Billing Day") },
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = dueDay,
                    onValueChange = onDueDayChange,
                    label = { Text("Due Day") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
