package com.greenicephoenix.traceledger.feature.sms.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.core.database.entity.SmsCustomRuleEntity
import com.greenicephoenix.traceledger.feature.sms.viewmodel.CustomRulesViewModel

@Composable
fun CustomRulesScreen(
    viewModel: CustomRulesViewModel,
    onNavigateBack: () -> Unit,
    onAddRule: () -> Unit,
    onEditRule: (SmsCustomRuleEntity) -> Unit,
) {
    val rules by viewModel.rules.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick        = onAddRule,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Rule")
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {

            // ── HEADER — matches RecurringTransactionsScreen exactly ──────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint               = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text  = "CUSTOM RULES",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // ── LIST — same structure and padding as RecurringTransactionsScreen ─
            LazyColumn(
                modifier            = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding      = PaddingValues(top = 8.dp, bottom = 100.dp)
            ) {

                // Empty state — identical pattern to RecurringTransactionsScreen
                if (rules.isEmpty()) {
                    item {
                        Column(
                            modifier                = Modifier
                                .fillMaxWidth()
                                .padding(top = 120.dp),
                            horizontalAlignment     = Alignment.CenterHorizontally,
                            verticalArrangement     = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text      = "No custom rules yet.",
                                style     = MaterialTheme.typography.bodyMedium,
                                color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                            TextButton(onClick = onAddRule) {
                                Text("Create one", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                items(items = rules, key = { it.id }) { rule ->
                    CustomRuleCard(
                        rule            = rule,
                        onToggleEnabled = { viewModel.toggleEnabled(rule) },
                        onEdit          = { onEditRule(rule) },
                        onDelete        = { viewModel.deleteRule(rule) }
                    )
                }
            }
        }
    }
}

// ── Rule card — mirrors RecurringItemCard structure ────────────────────────────

@Composable
private fun CustomRuleCard(
    rule: SmsCustomRuleEntity,
    onToggleEnabled: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title  = { Text("Delete rule?", style = MaterialTheme.typography.titleMedium) },
            text   = {
                Text(
                    "\"${rule.name}\" will be permanently removed.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape  = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {

            // ── Top row: rule name + active/paused status pill ────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text  = rule.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (rule.isEnabled)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                ) {
                    Text(
                        text     = if (rule.isEnabled) "Active" else "Paused",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = if (rule.isEnabled)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Sender pattern
            Text(
                text  = "Sender contains: ${rule.senderPattern}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            // Type + priority
            Text(
                text  = if (rule.isExclusionRule) "Always exclude"
                else "Detect transaction • Priority ${rule.priority}",
                style = MaterialTheme.typography.bodySmall,
                color = if (rule.isExclusionRule)
                    MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            Spacer(Modifier.height(4.dp))
            HorizontalDivider(
                thickness = 0.5.dp,
                color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )
            Spacer(Modifier.height(4.dp))

            // ── Action buttons — same pattern as RecurringItemCard ────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                TextButton(onClick = onToggleEnabled) {
                    Text(
                        text  = if (rule.isEnabled) "Disable" else "Enable",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { showDeleteDialog = true }) {
                    Text(
                        text  = "Delete",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}