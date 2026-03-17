package com.greenicephoenix.traceledger.feature.support

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
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

/**
 * Supported PayPal currencies with their ISO code and display symbol.
 * PayPal.me accepts amount+code appended to the URL: /100EUR, /50GBP etc.
 */
private data class PayPalCurrency(
    val code: String,    // ISO 4217 code appended to the URL
    val symbol: String,  // Display symbol shown in the UI
    val label: String    // Full label shown in the selector
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    // ── INR / UPI state ───────────────────────────────────────────────────────
    val inrPresets = listOf(20, 50, 100, 200)
    var selectedInr by remember { mutableStateOf<Int?>(50) }
    var customInr   by remember { mutableStateOf("") }

    // ── PayPal state ──────────────────────────────────────────────────────────
    val usdPresets = listOf(1, 2, 5, 10)
    var selectedUsd      by remember { mutableStateOf<Int?>(2) }
    var customUsd        by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf(PAYPAL_CURRENCIES[0]) }  // default USD
    var showCurrencySheet by remember { mutableStateOf(false) }

    // ── Dialog state ──────────────────────────────────────────────────────────
    var showNoUpiDialog     by remember { mutableStateOf(false) }
    var showNoBrowserDialog by remember { mutableStateOf(false) }

    // Effective amounts — preset takes priority; custom used when preset is null
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

        Icon(
            imageVector        = Icons.Default.Favorite,
            contentDescription = null,
            tint               = NothingRed,
            modifier           = Modifier.size(56.dp).align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text      = "TraceLedger is free and always will be.",
            style     = MaterialTheme.typography.titleMedium,
            color     = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier  = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text      = "If it saves you time or helps you manage your money better, consider buying me a coffee. It means a lot.",
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier  = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
        )

        Spacer(Modifier.height(32.dp))

        // ── INDIA — UPI ───────────────────────────────────────────────────────
        SectionLabel("INDIA — PAY WITH UPI")

        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape    = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier            = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // INR preset chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    inrPresets.forEach { amount ->
                        AmountChip(
                            label      = "₹$amount",
                            isSelected = selectedInr == amount,
                            modifier   = Modifier.weight(1f),
                            onClick    = { selectedInr = amount; customInr = "" }
                        )
                    }
                }

                // Custom INR
                OutlinedTextField(
                    value         = customInr,
                    onValueChange = { v ->
                        customInr   = v.filter { it.isDigit() }
                        selectedInr = null
                    },
                    label           = { Text("Custom amount") },
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
                    colors   = ButtonDefaults.buttonColors(containerColor = NothingRed),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text  = if (effectiveInr.isNotBlank()) "Pay ₹$effectiveInr with UPI"
                        else "Select or enter an amount",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Text(
                    text      = "Opens GPay, PhonePe, Paytm, BHIM, or any UPI app",
                    style     = MaterialTheme.typography.labelSmall,
                    color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── INTERNATIONAL — PayPal ────────────────────────────────────────────
        SectionLabel("INTERNATIONAL — PAY WITH PAYPAL")

        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape    = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier            = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // ── Currency selector row ─────────────────────────────────────
                // Tapping opens a bottom sheet with all supported PayPal currencies.
                // The selected currency controls the symbol shown in chips/prefix
                // and the code appended to the PayPal URL.
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
                        .padding(horizontal = 16.dp, vertical = 14.dp),
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
                    Text(
                        text  = "Change",
                        style = MaterialTheme.typography.labelSmall,
                        color = NothingRed
                    )
                }

                // Preset amount chips — symbol updates with selected currency
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    usdPresets.forEach { amount ->
                        AmountChip(
                            label      = "${selectedCurrency.symbol}$amount",
                            isSelected = selectedUsd == amount,
                            modifier   = Modifier.weight(1f),
                            onClick    = { selectedUsd = amount; customUsd = "" }
                        )
                    }
                }

                // Custom amount — prefix symbol updates with selected currency
                OutlinedTextField(
                    value         = customUsd,
                    onValueChange = { v ->
                        // Allow digits + one decimal point
                        val filtered = v.filter { it.isDigit() || it == '.' }
                        val dotIdx   = filtered.indexOf('.')
                        customUsd   = if (dotIdx == -1) filtered
                        else filtered.substring(0, dotIdx + 1) +
                                filtered.substring(dotIdx + 1).filter { it.isDigit() }
                        selectedUsd = null
                    },
                    label           = { Text("Custom amount") },
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

                // PayPal button
                // URL format: https://paypal.me/USERNAME/AMOUNT_CURRENCYCODE
                // e.g.        https://paypal.me/GreenIcePhoenix/100EUR
                OutlinedButton(
                    onClick  = {
                        if (effectiveUsd.isBlank() || effectiveUsd == "0") return@OutlinedButton
                        val url = "${AppLinks.PAYPAL_ME}/${effectiveUsd}${selectedCurrency.code}"
                        if (!launchUrl(context, url)) showNoBrowserDialog = true
                    },
                    enabled  = effectiveUsd.isNotBlank() && effectiveUsd != "0",
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape    = RoundedCornerShape(12.dp),
                    border   = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
                ) {
                    Text(
                        text  = if (effectiveUsd.isNotBlank())
                            "Pay ${selectedCurrency.symbol}$effectiveUsd (${selectedCurrency.code}) with PayPal"
                        else "Select or enter an amount",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text      = "Opens PayPal in your browser · Charged in ${selectedCurrency.code}",
                    style     = MaterialTheme.typography.labelSmall,
                    color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Disclaimers ───────────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape    = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier            = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DisclaimerLine("Payments are voluntary and non-refundable.")
                DisclaimerLine("This does not unlock any features — TraceLedger is already fully free.")
                DisclaimerLine("No payment data is processed or stored by this app.")
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
                                if (isSelected) NothingRed.copy(alpha = 0.10f)
                                else Color.Transparent
                            )
                            .clickable {
                                selectedCurrency = currency
                                // Reset presets when currency changes so amounts feel fresh
                                selectedUsd = usdPresets[1]
                                customUsd   = ""
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
                            // Symbol badge
                            Box(
                                modifier         = Modifier
                                    .size(36.dp)
                                    .background(
                                        MaterialTheme.colorScheme.background,
                                        RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text  = currency.symbol,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isSelected) NothingRed
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text  = currency.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) NothingRed
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (isSelected) {
                            Text("✓", color = NothingRed, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }

    // ── Error dialogs ─────────────────────────────────────────────────────────

    if (showNoUpiDialog) {
        AlertDialog(
            onDismissRequest = { showNoUpiDialog = false },
            shape            = RoundedCornerShape(20.dp),
            containerColor   = MaterialTheme.colorScheme.surface,
            title  = { Text("No UPI App Found", style = MaterialTheme.typography.titleMedium) },
            text   = { Text("Please install Google Pay, PhonePe, Paytm, or BHIM and try again.\n\nAlternatively use the PayPal option.") },
            confirmButton = {
                TextButton(onClick = { showNoUpiDialog = false }) { Text("OK", color = NothingRed) }
            }
        )
    }

    if (showNoBrowserDialog) {
        AlertDialog(
            onDismissRequest = { showNoBrowserDialog = false },
            shape            = RoundedCornerShape(20.dp),
            containerColor   = MaterialTheme.colorScheme.surface,
            title  = { Text("Cannot Open PayPal", style = MaterialTheme.typography.titleMedium) },
            text   = { Text("Could not open a browser. Please install Chrome or Firefox and try again.") },
            confirmButton = {
                TextButton(onClick = { showNoBrowserDialog = false }) { Text("OK", color = NothingRed) }
            }
        )
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
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DisclaimerLine(text: String) {
    Text(
        text  = "• $text",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// URL launching
// ─────────────────────────────────────────────────────────────────────────────

private fun launchUrl(context: Context, url: String): Boolean {
    if (url.isBlank()) return false
    val intent   = Intent(Intent.ACTION_VIEW, Uri.parse(url))
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