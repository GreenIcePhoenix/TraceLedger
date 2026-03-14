package com.greenicephoenix.traceledger.feature.transactions.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.core.currency.CurrencyFormatter
import com.greenicephoenix.traceledger.core.currency.CurrencyManager
import com.greenicephoenix.traceledger.core.ui.theme.NothingRed
import com.greenicephoenix.traceledger.core.ui.theme.SuccessGreen
import com.greenicephoenix.traceledger.domain.model.TransactionType
import com.greenicephoenix.traceledger.domain.model.TransactionUiModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailSheet(
    transaction: TransactionUiModel,
    categoryName: String,
    accountName: String,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    // Phase 2: separate onDelete callback so HistoryScreen can show a snackbar
    // after deletion completes, without navigating anywhere.
    onDelete: (TransactionUiModel) -> Unit
) {
    val currency by CurrencyManager.currency.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // ── Delete confirmation dialog ─────────────────────────────────────────────
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete transaction?", style = MaterialTheme.typography.titleMedium) },
            text  = {
                Text(
                    "This will reverse the balance change on your account. This cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDismiss()           // Close sheet first
                        onDelete(transaction) // Then trigger delete + snackbar in parent
                    }
                ) { Text("Delete", color = NothingRed) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = MaterialTheme.colorScheme.surface,
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {

            // ── TYPE LABEL ────────────────────────────────────────────────────
            Text(
                text  = transaction.type.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            Spacer(Modifier.height(4.dp))

            // ── CATEGORY / TITLE ──────────────────────────────────────────────
            Text(
                text  = categoryName,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(2.dp))

            // ── ACCOUNT ───────────────────────────────────────────────────────
            Text(
                text  = accountName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(Modifier.height(16.dp))

            // ── AMOUNT ────────────────────────────────────────────────────────
            Text(
                text  = CurrencyFormatter.format(transaction.amount.toPlainString(), currency),
                style = MaterialTheme.typography.displaySmall,
                color = when (transaction.type) {
                    TransactionType.INCOME   -> SuccessGreen
                    TransactionType.EXPENSE  -> NothingRed
                    TransactionType.TRANSFER -> MaterialTheme.colorScheme.onSurface
                }
            )

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            Spacer(Modifier.height(16.dp))

            // ── DATE ──────────────────────────────────────────────────────────
            DetailRow(
                label = "DATE",
                value = transaction.date.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
            )

            // ── NOTE ──────────────────────────────────────────────────────────
            transaction.note?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(12.dp))
                DetailRow(label = "NOTE", value = it)
            }

            // ── RECURRING BADGE ───────────────────────────────────────────────
            if (transaction.recurringId != null) {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SyncAlt,
                        contentDescription = null,
                        tint        = MaterialTheme.colorScheme.primary,
                        modifier    = Modifier.size(14.dp)
                    )
                    Text(
                        text  = "Auto-generated by recurring rule",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            Spacer(Modifier.height(16.dp))

            // ── ACTIONS ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick  = { showDeleteConfirm = true },
                    modifier = Modifier.weight(1f),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = NothingRed),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, NothingRed.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Delete", style = MaterialTheme.typography.labelLarge)
                }

                Button(
                    onClick  = { onDismiss(); onEdit() },
                    modifier = Modifier.weight(1f),
                    colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Edit", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Text(
            text  = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}