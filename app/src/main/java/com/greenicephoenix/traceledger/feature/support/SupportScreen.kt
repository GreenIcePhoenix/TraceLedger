package com.greenicephoenix.traceledger.feature.support

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.net.Uri
import com.greenicephoenix.traceledger.core.ui.theme.NothingRed
import com.greenicephoenix.traceledger.core.util.AppLinks

/**
 * Tip jar screen. No SDK or billing library required.
 *
 * UPI deep link format:  upi://pay?pa=ID&pn=NAME&am=AMOUNT&cu=INR&tn=NOTE
 * PayPal link format:    https://paypal.me/PROFILE/AMOUNT
 *
 * The UriHandler opens whichever UPI app the user has installed (GPay, PhonePe, Paytm, etc.)
 * Android shows a chooser if multiple UPI apps are installed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(onBack: () -> Unit) {
    val context    = LocalContext.current
    val uriHandler = LocalUriHandler.current

    // Available tip amounts in INR
    val tipAmounts = listOf(20, 50, 100, 200)
    var selectedAmount by remember { mutableIntStateOf(50) }  // default to ₹50

    // Shown when the user taps UPI but no UPI app is installed on the device
    var showNoUpiDialog by remember { mutableStateOf(false) }

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
                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint               = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text  = "SUPPORT TRACELEDGER",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.height(24.dp))

        // ── Heart icon ────────────────────────────────────────────────────────
        Icon(
            imageVector        = Icons.Default.Favorite,
            contentDescription = null,
            tint               = NothingRed,
            modifier           = Modifier
                .size(56.dp)
                .align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(20.dp))

        // ── Tagline ───────────────────────────────────────────────────────────
        Text(
            text      = "TraceLedger is free and always will be.",
            style     = MaterialTheme.typography.titleMedium,
            color     = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier  = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text      = "If it saves you time or helps you manage your money better, consider buying me a coffee. It means a lot.",
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier  = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        )

        Spacer(Modifier.height(32.dp))

        // ── Amount selector ───────────────────────────────────────────────────
        Text(
            text     = "SELECT AMOUNT",
            style    = MaterialTheme.typography.labelSmall,
            color    = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
            modifier = Modifier.padding(start = 24.dp, bottom = 12.dp)
        )

        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            tipAmounts.forEach { amount ->
                val isSelected = amount == selectedAmount
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) NothingRed
                            else MaterialTheme.colorScheme.surface
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) NothingRed
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedAmount = amount }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = "₹$amount",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) Color.White
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        // ── UPI Button ────────────────────────────────────────────────────────
        Button(
            onClick = {
                // Build the UPI deep link
                // pa = payee UPI VPA, pn = display name, am = amount, cu = currency, tn = note
                val upiUri = "upi://pay" +
                        "?pa=${AppLinks.UPI_ID}" +
                        "&pn=TraceLedger" +
                        "&am=$selectedAmount" +
                        "&cu=INR" +
                        "&tn=TraceLedger+Tip"

                // Check whether any UPI app is installed BEFORE trying to open the URI.
                // LocalUriHandler.openUri() throws IllegalArgumentException if no app
                // handles the scheme — that crash is what we're preventing here.
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(upiUri))
                val canHandle = intent.resolveActivity(context.packageManager) != null

                if (canHandle) {
                    try {
                        uriHandler.openUri(upiUri)
                    } catch (e: Exception) {
                        // Defensive catch — resolveActivity said it could handle it
                        // but launch failed for some other reason. Show fallback.
                        showNoUpiDialog = true
                    }
                } else {
                    // No UPI app on this device — show a friendly explanation
                    showNoUpiDialog = true
                }
            },
            modifier       = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 20.dp),
            colors         = ButtonDefaults.buttonColors(containerColor = NothingRed),
            shape          = RoundedCornerShape(14.dp)
        ) {
            Text(
                text  = "Pay ₹$selectedAmount with UPI",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }

        Spacer(Modifier.height(12.dp))

        // ── PayPal Button ─────────────────────────────────────────────────────
        OutlinedButton(
            onClick = {
                // PayPal.me opens in the browser — can't crash the same way UPI does,
                // but we wrap it anyway for robustness.
                try {
                    uriHandler.openUri("${AppLinks.PAYPAL_ME}/$selectedAmount")
                } catch (e: Exception) {
                    // Browser not available — extremely unlikely but handled gracefully
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 20.dp),
            shape    = RoundedCornerShape(14.dp),
            border   = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        ) {
            Text(
                text  = "Pay with PayPal",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.height(28.dp))

        // ── Disclaimers ───────────────────────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier            = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DisclaimerLine("Payments are voluntary and non-refundable.")
                DisclaimerLine("This does not unlock any features — TraceLedger is already fully free.")
                DisclaimerLine("No payment data is processed or stored by this app. Payments go directly to the developer.")
            }
        }

        Spacer(Modifier.height(40.dp))
    }

    // ── No UPI App Dialog ─────────────────────────────────────────────────────
    // Shown when the user taps "Pay with UPI" but no UPI app (GPay, PhonePe,
    // Paytm, BHIM, etc.) is installed on the device.
    if (showNoUpiDialog) {
        AlertDialog(
            onDismissRequest = { showNoUpiDialog = false },
            shape            = RoundedCornerShape(20.dp),
            containerColor   = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text  = "No UPI App Found",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text  = "Please install a UPI app such as Google Pay, PhonePe, Paytm, or BHIM, then try again.\n\nAlternatively, use the PayPal option below.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                TextButton(onClick = { showNoUpiDialog = false }) {
                    Text("OK", color = NothingRed)
                }
            }
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