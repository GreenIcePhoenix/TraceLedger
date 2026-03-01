package com.greenicephoenix.traceledger.feature.recurring

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import com.greenicephoenix.traceledger.core.database.entity.RecurringTransactionEntity
import com.greenicephoenix.traceledger.core.ui.theme.NothingRed
import androidx.compose.material.icons.Icons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringTransactionsScreen(
    viewModelFactory: RecurringTransactionsViewModelFactory,
    onAddClick: () -> Unit,
    onEditClick: (RecurringTransactionEntity) -> Unit,
    onBack: () -> Unit
) {

    val viewModel: RecurringTransactionsViewModel =
        viewModel(factory = viewModelFactory)

    val recurringList by viewModel.recurringTransactions.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = NothingRed
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Recurring")
            }
        }
    ) { padding ->

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
                    text = "Recurring",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            /* ---------- CONTENT ---------- */
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {

                if (recurringList.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 120.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            Text(
                                text = "No recurring transactions yet.\nTap + to create one.",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            TextButton(
                                onClick = onAddClick
                            ) {
                                Text(
                                    text = "Create your first recurring transaction",
                                    color = NothingRed
                                )
                            }
                        }
                    }
                }

                items(recurringList) { recurring ->
                    RecurringItemCard(
                        recurring = recurring,
                        onClick = { onEditClick(recurring) },
                        onDelete = { viewModel.delete(recurring) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecurringItemCard(
    recurring: RecurringTransactionEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Text(
                text = "${recurring.type} • ${recurring.frequency}",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = recurring.amount.toPlainString(),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(6.dp))

            TextButton(
                onClick = onDelete
            ) {
                Text(
                    text = "Delete",
                    color = NothingRed
                )
            }
        }
    }
}