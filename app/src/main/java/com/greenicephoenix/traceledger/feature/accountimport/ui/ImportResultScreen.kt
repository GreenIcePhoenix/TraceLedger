// ─────────────────────────────────────────────────────────────────────────────
// FILE: feature/accountimport/ui/ImportResultScreen.kt
//
// Step 3 of the import flow — shown after all transactions are written to DB.
//
// What the user sees:
//   1. Animated checkmark that springs in on entry
//   2. Count-up animation for the imported number (satisfying feedback)
//   3. Summary rows: imported / skipped / duplicates detected
//   4. Two actions:
//      - "View Transactions" → goes to the Transactions screen
//      - "Done"             → returns to Settings
//
// Design: flat dark, no cards, matches rest of app.
// ─────────────────────────────────────────────────────────────────────────────
package com.greenicephoenix.traceledger.feature.accountimport.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val IncomeGreen  = Color(0xFF27AE60)
private val AmberWarning = Color(0xFFF59E0B)

/**
 * Import result screen — Step 3.
 *
 * @param imported    Number of transactions successfully written to DB.
 * @param skipped     Rows the user excluded + rows with date errors.
 * @param duplicates  Rows that were flagged as possible duplicates
 *                    (included in [imported] or [skipped] depending on user choice).
 * @param onViewTransactions  Navigate to the Transactions screen.
 * @param onDone              Pop back to Settings.
 */
@Composable
fun ImportResultScreen(
    imported:            Int,
    skipped:             Int,
    duplicates:          Int,
    onViewTransactions:  () -> Unit,
    onDone:              () -> Unit
) {
    // ── Animations ─────────────────────────────────────────────────────────────

    // Checkmark springs in after a short delay
    val checkScale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(150)
        checkScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness    = Spring.StiffnessMedium
            )
        )
    }

    // Count-up animation for the main imported number
    val importedAnim = remember { Animatable(0f) }
    LaunchedEffect(imported) {
        delay(400)  // start after checkmark settles
        importedAnim.animateTo(
            targetValue   = imported.toFloat(),
            animationSpec = tween(
                durationMillis = (imported * 20).coerceIn(600, 1200),
                easing         = FastOutSlowInEasing
            )
        )
    }

    // Stagger the detail rows appearing
    var showDetails by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(700)
        showDetails = true
    }

    // ── Layout ──────────────────────────────────────────────────────────────────

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp),
        verticalArrangement   = Arrangement.Center,
        horizontalAlignment   = Alignment.CenterHorizontally
    ) {

        // ── Animated checkmark circle ─────────────────────────────────────────
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(96.dp)
                .scale(checkScale.value)
                .clip(CircleShape)
                .background(IncomeGreen.copy(alpha = 0.15f))
        ) {
            Icon(
                imageVector        = Icons.Default.Check,
                contentDescription = "Import complete",
                tint               = IncomeGreen,
                modifier           = Modifier.size(48.dp)
            )
        }

        Spacer(Modifier.height(28.dp))

        // ── Count-up headline ─────────────────────────────────────────────────
        Text(
            text       = importedAnim.value.toInt().toString(),
            style      = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color      = IncomeGreen,
            fontSize   = 72.sp
        )
        Text(
            text  = if (imported == 1) "transaction imported" else "transactions imported",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(36.dp))

        // ── Detail rows ───────────────────────────────────────────────────────
        // Stagger their appearance slightly after the count-up
        if (showDetails) {
            Column(
                modifier            = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                ResultRow(
                    icon  = Icons.Default.FileDownload,
                    color = IncomeGreen,
                    label = "Transactions imported",
                    value = imported.toString()
                )
                HorizontalDivider(
                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                    modifier = Modifier.padding(start = 52.dp)
                )
                ResultRow(
                    icon  = Icons.Default.List,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    label = "Rows skipped",
                    value = skipped.toString()
                )
                if (duplicates > 0) {
                    HorizontalDivider(
                        color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                        modifier = Modifier.padding(start = 52.dp)
                    )
                    ResultRow(
                        icon  = Icons.Default.List,
                        color = AmberWarning,
                        label = "Possible duplicates detected",
                        value = duplicates.toString(),
                        note  = "Review in Transactions if needed"
                    )
                }
            }
        }

        Spacer(Modifier.height(44.dp))

        // ── Action buttons ────────────────────────────────────────────────────
        Button(
            onClick  = onViewTransactions,
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.List, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("View Transactions", style = MaterialTheme.typography.titleSmall)
        }

        Spacer(Modifier.height(10.dp))

        OutlinedButton(
            onClick  = onDone,
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(12.dp)
        ) {
            Text("Done", style = MaterialTheme.typography.titleSmall)
        }
    }
}

// ── Private composables ────────────────────────────────────────────────────────

@Composable
private fun ResultRow(
    icon:  ImageVector,
    color: Color,
    label: String,
    value: String,
    note:  String? = null
) {
    Row(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment   = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(color.copy(alpha = 0.12f))
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface)
            if (note != null) {
                Text(note, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            }
        }
        Text(
            text       = value,
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color      = color
        )
    }
}