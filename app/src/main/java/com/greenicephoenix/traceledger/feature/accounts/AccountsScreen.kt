package com.greenicephoenix.traceledger.feature.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.core.currency.CurrencyFormatter
import com.greenicephoenix.traceledger.core.currency.CurrencyManager
import com.greenicephoenix.traceledger.domain.model.AccountUiModel

@Composable
fun AccountsScreen(
    accounts: List<AccountUiModel>,
    viewModel: AccountsViewModel,
    onBack: () -> Unit,
    onAddAccount: () -> Unit,
    onAccountClick: (AccountUiModel) -> Unit,
    // v1.3.0: navigates to the Import Hub from the banner at top of accounts list.
    // Wired in NavGraph: onNavigateToImport = { navController.navigate(Routes.IMPORT_HUB) }
    onNavigateToImport: () -> Unit = {}
) {
    var accountPendingDeletion by remember { mutableStateOf<AccountUiModel?>(null) }

    // Observe delete errors from ViewModel. When Room's FK constraint fires,
    // the error message flows here automatically.
    val deleteError by viewModel.deleteError.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── HEADER ────────────────────────────────────────────────────────────
        AccountsHeader(
            vaultCount   = accounts.size,
            onBack       = onBack,
            onAddAccount = onAddAccount
        )

        // ── EMPTY STATE ───────────────────────────────────────────────────────
        if (accounts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text  = "No accounts yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    TextButton(onClick = onAddAccount) {
                        Text("Add your first account", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        } else {
            // ── ACCOUNT LIST ──────────────────────────────────────────────────
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // Import nudge banner — soft prompt, not a blocking UI element.
                // Tapping it navigates to Settings → Import Hub.
                // Shown only when there are accounts (user is ready to import).
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            // Slight inner padding reduction so it sits flush
                            // with the account cards below it.
                            .padding(bottom = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.07f))
                            .clickable { onNavigateToImport() }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.FileDownload,
                            contentDescription = null,
                            tint     = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text  = "Have a bank statement? Import it to auto-fill transactions.",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                items(accounts, key = { it.id }) { account ->
                    AccountRowCard(
                        account     = account,
                        onClick     = { onAccountClick(account) },
                        onLongPress = { accountPendingDeletion = account }
                    )
                }
            }
        }
    }

    // ── DELETE CONFIRMATION DIALOG ────────────────────────────────────────────
    accountPendingDeletion?.let { account ->
        AlertDialog(
            onDismissRequest = { accountPendingDeletion = null },
            title = { Text("Delete account?") },
            text  = {
                Text(
                    "This will permanently delete \"${account.name}\". " +
                            "Accounts with existing transactions cannot be deleted."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAccount(account.id)
                        accountPendingDeletion = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { accountPendingDeletion = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ── DELETE BLOCKED ERROR DIALOG ───────────────────────────────────────────
    deleteError?.let { message ->
        AlertDialog(
            onDismissRequest = { viewModel.clearDeleteError() },
            title = { Text("Cannot delete account") },
            text  = { Text(message) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearDeleteError() }) {
                    Text("OK")
                }
            }
        )
    }
}

// ── Private composables ────────────────────────────────────────────────────────

@Composable
private fun AccountsHeader(
    vaultCount: Int,
    onBack: () -> Unit,
    onAddAccount: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint               = MaterialTheme.colorScheme.onBackground
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = "ACCOUNTS",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text  = "$vaultCount account${if (vaultCount == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
            IconButton(onClick = onAddAccount) {
                Icon(
                    imageVector        = Icons.Default.Add,
                    contentDescription = "Add Account",
                    tint               = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun AccountRowCard(
    account: AccountUiModel,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val currency by CurrencyManager.currency.collectAsState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap       = { onClick() },
                    onLongPress = { onLongPress() }
                )
            },
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Colour dot
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(account.color), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = account.name.uppercase(),
                    style      = MaterialTheme.typography.bodyMedium,
                    color      = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text  = account.type.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text       = CurrencyFormatter.format(account.balance.toPlainString(), currency),
                    style      = MaterialTheme.typography.bodyMedium,
                    color      = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text  = "Long press to delete",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }
    }
}