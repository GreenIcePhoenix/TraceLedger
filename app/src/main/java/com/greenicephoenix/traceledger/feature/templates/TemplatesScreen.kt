package com.greenicephoenix.traceledger.feature.templates

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.core.currency.CurrencyFormatter
import com.greenicephoenix.traceledger.core.currency.CurrencyManager
import com.greenicephoenix.traceledger.core.ui.theme.NothingRed
import com.greenicephoenix.traceledger.core.ui.theme.SuccessGreen
import com.greenicephoenix.traceledger.domain.model.AccountUiModel
import com.greenicephoenix.traceledger.domain.model.CategoryUiModel
import com.greenicephoenix.traceledger.domain.model.TransactionType
import com.greenicephoenix.traceledger.feature.templates.domain.TransactionTemplateUiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(
    templates: List<TransactionTemplateUiModel>,
    accounts: List<AccountUiModel>,
    categories: List<CategoryUiModel>,
    onAddTemplate: () -> Unit,
    onEditTemplate: (String) -> Unit,
    onDelete: (String) -> Unit,
    onBack: () -> Unit
) {
    var deleteTarget by remember { mutableStateOf<TransactionTemplateUiModel?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text  = "TEMPLATES",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text  = "${templates.size} saved",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null,
                            tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick           = onAddTemplate,
                containerColor    = MaterialTheme.colorScheme.primary,
                contentColor      = Color.White,
                shape             = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Template")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        if (templates.isEmpty()) {
            // Empty state
            Box(
                modifier          = Modifier.fillMaxSize().padding(padding),
                contentAlignment  = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                        modifier           = Modifier.size(48.dp)
                    )
                    Text(
                        text  = "No templates yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                    Text(
                        text  = "Add a template or save one from the\nAdd Transaction screen",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier       = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(templates, key = { it.id }) { template ->
                    TemplateCard(
                        template   = template,
                        accounts   = accounts,
                        categories = categories,
                        onEdit     = { onEditTemplate(template.id) },
                        onDelete   = { deleteTarget = template }
                    )
                }
                // Bottom padding so FAB doesn't cover last item
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }

    // Delete confirmation dialog
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = {
                Text("Delete template?", color = MaterialTheme.colorScheme.onSurface)
            },
            text = {
                Text(
                    text  = "\"${target.name}\" will be permanently removed.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(target.id)
                    deleteTarget = null
                }) {
                    Text("Delete", color = NothingRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TemplateCard — single template list item
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun TemplateCard(
    template: TransactionTemplateUiModel,
    accounts: List<AccountUiModel>,
    categories: List<CategoryUiModel>,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val currency by CurrencyManager.currency.collectAsState()

    // Resolve human-readable subtitle (category or account names)
    val subtitle = buildString {
        when (template.type) {
            TransactionType.EXPENSE, TransactionType.INCOME -> {
                categories.firstOrNull { it.id == template.categoryId }?.name?.let { append(it) }
                accounts.firstOrNull { it.id == (template.fromAccountId ?: template.toAccountId) }
                    ?.name?.let {
                        if (isNotEmpty()) append(" · ")
                        append(it)
                    }
            }
            TransactionType.TRANSFER -> {
                val from = accounts.firstOrNull { it.id == template.fromAccountId }?.name ?: "?"
                val to   = accounts.firstOrNull { it.id == template.toAccountId   }?.name ?: "?"
                append("$from → $to")
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type indicator dot
            val typeColor = when (template.type) {
                TransactionType.EXPENSE  -> NothingRed
                TransactionType.INCOME   -> SuccessGreen
                TransactionType.TRANSFER -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            }
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(typeColor, RoundedCornerShape(50))
            )
            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text     = template.name,
                    style    = MaterialTheme.typography.titleSmall,
                    color    = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text  = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Amount (if preset)
            template.amount?.let {
                Text(
                    text  = CurrencyFormatter.format(it.toPlainString(), currency),
                    style = MaterialTheme.typography.bodyMedium,
                    color = typeColor
                )
                Spacer(Modifier.width(8.dp))
            }

            // Delete button
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector        = Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier           = Modifier.size(18.dp)
                )
            }
        }
    }
}