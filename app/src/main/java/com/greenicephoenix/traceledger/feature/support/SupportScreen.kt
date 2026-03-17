package com.greenicephoenix.traceledger.feature.support

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.core.ui.theme.NothingRed
import com.greenicephoenix.traceledger.core.util.AppLinks
import androidx.core.net.toUri

private data class PayPalCurrency(
    val code: String,
    val symbol: String,
    val label: String
)

private val PAYPAL_CURRENCIES = listOf(
    PayPalCurrency("USD", "$",  "USD — US Dollar"),
    PayPalCurrency("EUR", "€",  "EUR — Euro"),
    PayPalCurrency("GBP", "£",  "GBP — British Pound"),
    PayPalCurrency("CAD", "C$", "CAD — Canadian Dollar"),
    PayPalCurrency("AUD", "A$", "AUD — Australian Dollar"),
    PayPalCurrency("SGD", "S$", "SGD — Singapore Dollar"),
    PayPalCurrency("JPY", "¥",  "JPY — Japanese Yen"),
)

// Which accordion section is open. Only one at a time.
private enum class OpenSection { NONE, UPI, PAYPAL }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    // ── Accordion state ───────────────────────────────────────────────────────
    var openSection by remember { mutableStateOf(OpenSection.NONE) }

    // ── INR / UPI state ───────────────────────────────────────────────────────
    // null = "Custom" chip is selected; Int = a preset is selected
    var selectedInr by remember { mutableStateOf<Int?>(50) }
    // Prefilled to "25" whenever the Custom chip is tapped
    var customInr   by remember { mutableStateOf("") }

    // ── PayPal state ──────────────────────────────────────────────────────────
    var selectedUsd      by remember { mutableStateOf<Int?>(2) }
    var customUsd        by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf(PAYPAL_CURRENCIES[0]) }
    var showCurrencySheet by remember { mutableStateOf(false) }

    // ── Dialog state ──────────────────────────────────────────────────────────
    var showNoUpiDialog     by remember { mutableStateOf(false) }
    var showNoBrowserDialog by remember { mutableStateOf(false) }

    // Effective amounts — null selectedInr/Usd means Custom chip is active
    val effectiveInr = selectedInr?.toString() ?: customInr.trim()
    val effectiveUsd = selectedUsd?.toString() ?: customUsd.trim()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {

        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                text  = "SUPPORT TRACELEDGER",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.height(24.dp))

        // ── Heart ─────────────────────────────────────────────────────────────
        Icon(
            imageVector        = Icons.Default.Favorite,
            contentDescription = null,
            tint               = NothingRed,
            modifier           = Modifier
                .size(52.dp)
                .align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(20.dp))

        // ── Detailed developer message ────────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape  = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier            = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text  = "Built by one person, for everyone.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text  = "TraceLedger is a labour of love — every screen, every feature, and every bug fix is crafted by a solo developer. " +
                            "The core features — transactions, accounts, budgets, statistics, and recurring entries — will always be free, no exceptions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text  = "Your support helps fund the time it takes to build new features, fix issues quickly, and keep the app updated for every new version of Android. " +
                            "If TraceLedger has been useful to you, a small tip goes a long way.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                // Quick privacy assurance line
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                )
                Text(
                    text  = "Payments are voluntary • Core features always free • No payment data stored by this app",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f),
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── UPI ACCORDION ─────────────────────────────────────────────────────
        SectionLabel("INDIA")

        AccordionCard(
            title     = "Pay with UPI",
            subtitle  = "GPay · PhonePe · Paytm · BHIM · any UPI app",
            isOpen    = openSection == OpenSection.UPI,
            onToggle  = {
                openSection = if (openSection == OpenSection.UPI) OpenSection.NONE else OpenSection.UPI
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // INR preset chips + Custom chip
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(20, 50, 100, 200).forEach { amount ->
                        AmountChip(
                            label      = "₹$amount",
                            isSelected = selectedInr == amount,
                            modifier   = Modifier.weight(1f),
                            onClick    = {
                                selectedInr = amount
                                customInr   = ""
                            }
                        )
                    }
                    // Custom chip — tapping prefills the field with 25
                    AmountChip(
                        label      = "Custom",
                        isSelected = selectedInr == null,
                        modifier   = Modifier.weight(1f),
                        onClick    = {
                            selectedInr = null
                            if (customInr.isBlank()) customInr = "25"
                        }
                    )
                }

                // Custom INR text field — only visible when Custom chip is selected
                AnimatedVisibility(visible = selectedInr == null) {
                    OutlinedTextField(
                        value         = customInr,
                        onValueChange = { v -> customInr = v.filter { it.isDigit() } },
                        label           = { Text("Enter amount") },
                        prefix          = { Text("₹") },
                        singleLine      = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier        = Modifier.fillMaxWidth(),
                        colors          = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = NothingRed,
                            focusedLabelColor    = NothingRed,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // UPI pay button — NothingRed filled
                Button(
                    onClick  = {
                        if (effectiveInr.isBlank() || effectiveInr == "0") return@Button
                        val url = "upi://pay" +
                                "?pa=${AppLinks.UPI_ID}" +
                                "&pn=TraceLedger" +
                                "&am=$effectiveInr" +
                                "&cu=INR" +
                                "&tn=TraceLedger+Tip"
                        if (!launchUrl(context, url)) showNoUpiDialog = true
                    },
                    enabled  = effectiveInr.isNotBlank() && effectiveInr != "0",
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = NothingRed,
                        disabledContainerColor = NothingRed.copy(alpha = 0.4f)
                    ),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text  = if (effectiveInr.isNotBlank() && effectiveInr != "0")
                            "Pay ₹$effectiveInr with UPI"
                        else "Select or enter an amount",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── PAYPAL ACCORDION ──────────────────────────────────────────────────
        SectionLabel("INTERNATIONAL")

        AccordionCard(
            title    = "Pay with PayPal",
            subtitle = "Opens in your browser · Pay in your currency",
            isOpen   = openSection == OpenSection.PAYPAL,
            onToggle = {
                openSection = if (openSection == OpenSection.PAYPAL) OpenSection.NONE else OpenSection.PAYPAL
            }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // Currency selector row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { showCurrencySheet = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text  = "Currency",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            text  = "${selectedCurrency.symbol}  ${selectedCurrency.label}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text("Change", style = MaterialTheme.typography.labelSmall, color = NothingRed)
                }

                // PayPal preset chips + Custom chip
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 2, 5, 10).forEach { amount ->
                        AmountChip(
                            label      = "${selectedCurrency.symbol}$amount",
                            isSelected = selectedUsd == amount,
                            modifier   = Modifier.weight(1f),
                            onClick    = {
                                selectedUsd = amount
                                customUsd   = ""
                            }
                        )
                    }
                    // Custom chip — tapping prefills with 25
                    AmountChip(
                        label      = "Custom",
                        isSelected = selectedUsd == null,
                        modifier   = Modifier.weight(1f),
                        onClick    = {
                            selectedUsd = null
                            if (customUsd.isBlank()) customUsd = "25"
                        }
                    )
                }

                // Custom PayPal field — only visible when Custom chip is selected
                AnimatedVisibility(visible = selectedUsd == null) {
                    OutlinedTextField(
                        value         = customUsd,
                        onValueChange = { v ->
                            val filtered = v.filter { it.isDigit() || it == '.' }
                            val dotIdx   = filtered.indexOf('.')
                            customUsd = if (dotIdx == -1) filtered
                            else filtered.substring(0, dotIdx + 1) +
                                    filtered.substring(dotIdx + 1).filter { it.isDigit() }
                            selectedUsd = null
                        },
                        label           = { Text("Enter amount") },
                        prefix          = { Text(selectedCurrency.symbol) },
                        singleLine      = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier        = Modifier.fillMaxWidth(),
                        colors          = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = NothingRed,
                            focusedLabelColor    = NothingRed,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // PayPal button — same NothingRed filled style as UPI button
                // URL format: https://paypal.me/USERNAME/AMOUNTCURRENCYCODE
                // e.g.        https://paypal.me/GreenIcePhoenix/100EUR
                Button(
                    onClick  = {
                        if (effectiveUsd.isBlank() || effectiveUsd == "0") return@Button
                        val url = "${AppLinks.PAYPAL_ME}/${effectiveUsd}${selectedCurrency.code}"
                        if (!launchUrl(context, url)) showNoBrowserDialog = true
                    },
                    enabled  = effectiveUsd.isNotBlank() && effectiveUsd != "0",
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = NothingRed,
                        disabledContainerColor = NothingRed.copy(alpha = 0.4f)
                    ),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text  = if (effectiveUsd.isNotBlank() && effectiveUsd != "0")
                            "Pay ${selectedCurrency.symbol}$effectiveUsd (${selectedCurrency.code}) with PayPal"
                        else "Select or enter an amount",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Text(
                    text      = "Charged in ${selectedCurrency.code} · Opens in browser",
                    style     = MaterialTheme.typography.labelSmall,
                    color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(40.dp))
    }

    // ── Currency bottom sheet ─────────────────────────────────────────────────
    if (showCurrencySheet) {
        ModalBottomSheet(onDismissRequest = { showCurrencySheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text     = "Select Currency",
                    style    = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                PAYPAL_CURRENCIES.forEach { currency ->
                    val isSelected = currency.code == selectedCurrency.code
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) NothingRed.copy(alpha = 0.10f) else Color.Transparent
                            )
                            .clickable {
                                selectedCurrency = currency
                                selectedUsd      = listOf(1, 2, 5, 10)[1]  // reset to $2 preset
                                customUsd        = ""
                                showCurrencySheet = false
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text  = currency.symbol,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isSelected) NothingRed else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text  = currency.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) NothingRed else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (isSelected) Text("✓", color = NothingRed, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }

    // ── Error dialogs ─────────────────────────────────────────────────────────

    if (showNoUpiDialog) {
        AlertDialog(
            onDismissRequest = { showNoUpiDialog = false },
            shape          = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title          = { Text("No UPI App Found", style = MaterialTheme.typography.titleMedium) },
            text           = { Text("Please install Google Pay, PhonePe, Paytm, or BHIM and try again.\n\nAlternatively use the PayPal option.") },
            confirmButton  = { TextButton(onClick = { showNoUpiDialog = false }) { Text("OK", color = NothingRed) } }
        )
    }

    if (showNoBrowserDialog) {
        AlertDialog(
            onDismissRequest = { showNoBrowserDialog = false },
            shape          = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title          = { Text("Cannot Open PayPal", style = MaterialTheme.typography.titleMedium) },
            text           = { Text("Could not open a browser. Please install Chrome or Firefox and try again.") },
            confirmButton  = { TextButton(onClick = { showNoBrowserDialog = false }) { Text("OK", color = NothingRed) } }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AccordionCard — tappable header + animated expandable body
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AccordionCard(
    title: String,
    subtitle: String,
    isOpen: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape  = RoundedCornerShape(20.dp)
    ) {
        Column {
            // ── Tappable header ───────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text  = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text  = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                Icon(
                    imageVector        = if (isOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint               = NothingRed,
                    modifier           = Modifier.size(20.dp)
                )
            }

            // ── Animated expandable body ──────────────────────────────────────
            AnimatedVisibility(
                visible = isOpen,
                enter   = expandVertically(),
                exit    = shrinkVertically()
            ) {
                Column {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    )
                    Column(
                        modifier            = Modifier.padding(16.dp),
                        content             = content
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text     = text,
        style    = MaterialTheme.typography.labelSmall,
        color    = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
        modifier = Modifier.padding(start = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun AmountChip(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) NothingRed else MaterialTheme.colorScheme.background)
            .border(
                1.dp,
                if (isSelected) NothingRed else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f),
                RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// URL launching
// ─────────────────────────────────────────────────────────────────────────────

private fun launchUrl(context: Context, url: String): Boolean {
    if (url.isBlank()) return false
    val intent   = Intent(Intent.ACTION_VIEW, url.toUri())
    val activity = context.findActivity()
    return try {
        if (activity != null) activity.startActivity(intent)
        else { intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(intent) }
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}

private fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}