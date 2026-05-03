package com.greenicephoenix.traceledger.feature.recurring

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.domain.model.RecurringFrequency
import java.math.BigDecimal
import java.time.LocalDate
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewmodel.compose.viewModel
import com.greenicephoenix.traceledger.core.currency.CurrencyFormatter
import com.greenicephoenix.traceledger.core.currency.CurrencyManager
import com.greenicephoenix.traceledger.core.database.entity.RecurringTransactionEntity
import com.greenicephoenix.traceledger.domain.model.TransactionType
import com.greenicephoenix.traceledger.domain.model.AccountUiModel
import com.greenicephoenix.traceledger.domain.model.CategoryUiModel
import com.greenicephoenix.traceledger.feature.addtransaction.AccountSelector
import com.greenicephoenix.traceledger.feature.addtransaction.CategorySelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRecurringScreen(
    accounts: List<AccountUiModel>,
    categories: List<CategoryUiModel>,
    existing: RecurringTransactionEntity? = null,
    onSave: (
        type: TransactionType,
        amount: BigDecimal,
        fromAccountId: String?,
        toAccountId: String?,
        categoryId: String?,
        frequency: RecurringFrequency,
        startDate: LocalDate,
        endDate: LocalDate?,
        note: String?
    ) -> Unit,
    onBack: () -> Unit
) {
    var amountText by remember(existing) {
        mutableStateOf(existing?.amount?.toPlainString() ?: "")
    }

    var selectedFrequency by remember(existing) {
        mutableStateOf(
            existing?.let { RecurringFrequency.valueOf(it.frequency) }
                ?: RecurringFrequency.MONTHLY
        )
    }

    var selectedType by remember(existing) {
        mutableStateOf(
            existing?.let { TransactionType.valueOf(it.type) }
                ?: TransactionType.EXPENSE
        )
    }

    var fromAccountId by remember(existing) {
        mutableStateOf(existing?.fromAccountId)
    }

    var toAccountId by remember(existing) {
        mutableStateOf(existing?.toAccountId)
    }

    var categoryId by remember(existing) {
        mutableStateOf(existing?.categoryId)
    }

    var note by remember(existing) {
        mutableStateOf(existing?.note ?: "")
    }

    var startDate by remember(existing) {
        mutableStateOf(existing?.startDate ?: LocalDate.now())
    }

    var endDate by remember(existing) {
        mutableStateOf(existing?.endDate)
    }

    val scrollState = rememberScrollState()
    val canSave = remember(
        selectedType,
        amountText,
        fromAccountId,
        toAccountId,
        categoryId
    ) {
        val amount = amountText.toBigDecimalOrNull()

        if (amount == null || amount <= BigDecimal.ZERO) return@remember false

        when (selectedType) {

            TransactionType.EXPENSE ->
                fromAccountId != null && categoryId != null

            TransactionType.INCOME ->
                toAccountId != null && categoryId != null

            TransactionType.TRANSFER ->
                fromAccountId != null &&
                        toAccountId != null &&
                        fromAccountId != toAccountId
        }
    }

    var showFrequencySheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        /* ---------- HEADER ---------- */
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = if (existing == null) "Add Recurring" else "Edit Recurring",
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium
            )

            IconButton(
                enabled = canSave,
                onClick = {
                    val amount = amountText.toBigDecimal()

                    onSave(
                        selectedType,
                        amount,
                        fromAccountId,
                        toAccountId,
                        categoryId,
                        selectedFrequency,
                        startDate,
                        endDate,
                        note.ifBlank { null }
                    )
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Save",
                    tint = if (canSave)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }

        /* ---------- FORM CONTAINER ---------- */
        Card(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {

            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                AmountInput(
                    rawValue = amountText,
                    onChange = { amountText = it }
                )

                RecurringTransactionTypeSelector(
                    selected = selectedType,
                    onSelected = { selectedType = it }
                )

                when (selectedType) {

                    TransactionType.EXPENSE -> {

                        AccountSelector(
                            label = "From Account",
                            accounts = accounts,
                            selectedAccountId = fromAccountId,
                            onSelect = { fromAccountId = it }
                        )

                        CategorySelector(
                            categories = categories,
                            type = com.greenicephoenix.traceledger.domain.model.CategoryType.EXPENSE,
                            selectedCategoryId = categoryId,
                            onSelect = { categoryId = it }
                        )
                    }

                    TransactionType.INCOME -> {

                        AccountSelector(
                            label = "To Account",
                            accounts = accounts,
                            selectedAccountId = toAccountId,
                            onSelect = { toAccountId = it }
                        )

                        CategorySelector(
                            categories = categories,
                            type = com.greenicephoenix.traceledger.domain.model.CategoryType.INCOME,
                            selectedCategoryId = categoryId,
                            onSelect = { categoryId = it }
                        )
                    }

                    TransactionType.TRANSFER -> {

                        FormSection {
                            AccountSelector(
                                label = "Source Account",
                                accounts = accounts,
                                selectedAccountId = fromAccountId,
                                onSelect = { fromAccountId = it }
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "↓",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        FormSection {
                            AccountSelector(
                                label = "Destination Account",
                                accounts = accounts,
                                selectedAccountId = toAccountId,
                                onSelect = { toAccountId = it }
                            )
                        }
                    }
                }

                FrequencySelector(
                    selected = selectedFrequency,
                    onSelect = { selectedFrequency = it }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    DateSelectorField(
                        label = "Start Date",
                        date = startDate,
                        onSelect = { startDate = it },
                        modifier = Modifier.weight(1f)
                    )

                    DateSelectorField(
                        label = "End Date",
                        date = endDate,
                        onSelect = { endDate = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (showFrequencySheet) {
            ModalBottomSheet(
                onDismissRequest = { showFrequencySheet = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    Text(
                        text = "Select Frequency",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    RecurringFrequency.entries.forEach { frequency ->

                        val isSelected = frequency == selectedFrequency

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isSelected)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    else
                                        MaterialTheme.colorScheme.surface,
                                    shape = MaterialTheme.shapes.medium
                                )
                                .clickable {
                                    selectedFrequency = frequency
                                    showFrequencySheet = false
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Text(
                                text = frequency.name.replace("_", " "),
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FrequencySelector(
    selected: RecurringFrequency,
    onSelect: (RecurringFrequency) -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }

    val interactionSource = remember { MutableInteractionSource() }

    val arrowRotation by animateFloatAsState(
        targetValue = if (showSheet) 180f else 0f,
        animationSpec = tween(
            durationMillis = 180,
            easing = FastOutSlowInEasing
        ),
        label = "frequencyArrowRotation"
    )

    OutlinedTextField(
        value = selected.name.replace("_", " "),
        onValueChange = {},
        label = { Text("Frequency") },
        readOnly = true,
        interactionSource = interactionSource,
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.rotate(arrowRotation),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    )

    // EXACT SAME LOGIC AS CATEGORY SELECTOR
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect {
            if (it is PressInteraction.Release) {
                showSheet = true
            }
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false }
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RecurringFrequency.entries.forEach { frequency ->

                    val isSelected = frequency == selected

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isSelected)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                onSelect(frequency)
                                showSheet = false
                            }
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Text(
                            text = frequency.name.replace("_", " "),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurringDateSelector(
    label: String,
    date: LocalDate?,
    allowClear: Boolean,
    onDateSelected: (LocalDate) -> Unit,
    onClear: () -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }

    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = date?.toEpochDay()?.times(86_400_000)
    )

    val interactionSource = remember { MutableInteractionSource() }

    OutlinedTextField(
        value = date?.toString() ?: "No end date",
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        interactionSource = interactionSource,
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            if (allowClear && date != null) {
                TextButton(onClick = onClear) {
                    Text("Clear")
                }
            }
        }
    )

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect {
            if (it is PressInteraction.Release) {
                showPicker = true
            }
        }
    }

    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            onDateSelected(
                                LocalDate.ofEpochDay(millis / 86_400_000)
                            )
                        }
                        showPicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun RecurringTransactionTypeSelector(
    selected: TransactionType,
    onSelected: (TransactionType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(22.dp)
            )
            .padding(4.dp)
    ) {

        TransactionType.entries.forEach { type ->

            val isSelected = type == selected

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        if (isSelected)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else
                            Color.Transparent,
                        RoundedCornerShape(18.dp)
                    )
                    .clickable { onSelected(type) },
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = type.name,
                    color =
                        if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun AmountInput(
    rawValue: String,
    onChange: (String) -> Unit
) {
    val currency by CurrencyManager.currency.collectAsState()

    var textFieldValue by remember(rawValue) {
        mutableStateOf(
            androidx.compose.ui.text.input.TextFieldValue(
                text = rawValue,
                selection = androidx.compose.ui.text.TextRange(rawValue.length)
            )
        )
    }

    BasicTextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            val newText = newValue.text

            if (
                newText.isEmpty() ||
                newText.matches(Regex("""\d+(\.\d{0,2})?"""))
            ) {
                textFieldValue = newValue.copy(
                    selection = androidx.compose.ui.text.TextRange(newText.length)
                )
                onChange(newText)
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number
        ),
        textStyle = TextStyle.Default.copy(color = Color.Transparent),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = CurrencyFormatter.format(
                        textFieldValue.text,
                        currency
                    ),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .width(64.dp)
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                )
            }
        }
    )
}

@Composable
private fun FormSection(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateSelectorField(
    label: String,
    date: LocalDate?,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }

    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis =
            date?.toEpochDay()?.times(86_400_000)
    )

    val interactionSource = remember { MutableInteractionSource() }

    OutlinedTextField(
        value = date?.toString() ?: "No end date",
        onValueChange = {},
        label = { Text(label) },
        readOnly = true,
        interactionSource = interactionSource,
        modifier = modifier,
        trailingIcon = {
            Icon(Icons.Default.DateRange, contentDescription = null)
        }
    )

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect {
            if (it is PressInteraction.Release) {
                showPicker = true
            }
        }
    }

    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            onSelect(
                                LocalDate.ofEpochDay(millis / 86_400_000)
                            )
                        }
                        showPicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}