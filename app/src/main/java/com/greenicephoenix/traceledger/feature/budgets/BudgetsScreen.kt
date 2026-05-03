package com.greenicephoenix.traceledger.feature.budgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.core.currency.CurrencyFormatter
import com.greenicephoenix.traceledger.core.currency.CurrencyManager
import com.greenicephoenix.traceledger.domain.model.CategoryUiModel
import com.greenicephoenix.traceledger.feature.budgets.components.BudgetAccentProgress
import com.greenicephoenix.traceledger.feature.budgets.domain.BudgetStatus
import com.greenicephoenix.traceledger.feature.budgets.ui.BudgetColors
import com.greenicephoenix.traceledger.feature.categories.CategoryIcons
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    viewModel: BudgetsViewModel,
    categories: List<CategoryUiModel>,
    onAddBudget: () -> Unit,
    onEditBudget: (String) -> Unit,
    onBack: () -> Unit
) {
    val budgetStatuses by viewModel.budgetStatuses.collectAsState()
    val selectedMonth  by viewModel.selectedMonth.collectAsState()
    val isPastMonth     = selectedMonth.isBefore(YearMonth.now())

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick        = { if (!isPastMonth) onAddBudget() },
                containerColor = if (isPastMonth) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Budget")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            BudgetsHeader(viewModel = viewModel, onBack = onBack)

            LazyColumn(
                modifier            = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding      = PaddingValues(bottom = 96.dp)
            ) {
                if (budgetStatuses.isEmpty()) {
                    item {
                        Column(
                            modifier            = Modifier.fillMaxWidth().padding(top = 120.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text      = "No budgets for this month.\nTap + to create one.",
                                color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                style     = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(12.dp))
                            TextButton(onClick = onAddBudget) {
                                Text("Create a budget", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                } else {
                    items(budgetStatuses.distinctBy { it.categoryId }) { status ->
                        val category = categories.firstOrNull { it.id == status.categoryId }
                        if (category != null) {
                            BudgetItemCard(
                                status   = status,
                                category = category,
                                onClick  = { onEditBudget(status.budgetId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetsHeader(viewModel: BudgetsViewModel, onBack: () -> Unit) {
    val month by viewModel.selectedMonth.collectAsState()

    Column {
        Row(
            modifier          = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                text  = "BUDGETS",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.selectMonth(month.minusMonths(1)) }) {
                Icon(Icons.Default.ChevronLeft, null, tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                text  = "${month.month.name.take(3).uppercase()} ${month.year}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = { viewModel.selectMonth(month.plusMonths(1)) }) {
                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun BudgetItemCard(
    status: BudgetStatus,
    category: CategoryUiModel,
    onClick: () -> Unit
) {
    val currency by CurrencyManager.currency.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape    = RoundedCornerShape(20.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector        = CategoryIcons.all[category.icon] ?: CategoryIcons.all["default"]!!,
                    contentDescription = null,
                    tint               = Color(category.color),
                    modifier           = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text  = category.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text  = "${(status.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = BudgetColors.remainingText(status.state)
                )
            }

            // FIX: was "Used: ${status.used} of ${status.limit}" — raw BigDecimal string
            // Now properly formatted with currency symbol and locale-correct grouping
            Text(
                text  = "${CurrencyFormatter.format(status.used.toPlainString(), currency)} of ${CurrencyFormatter.format(status.limit.toPlainString(), currency)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            BudgetAccentProgress(progress = status.progress, state = status.state)

            Text(
                text  = "Remaining: ${CurrencyFormatter.format(status.remaining.toPlainString(), currency)}",
                style = MaterialTheme.typography.labelSmall,
                color = BudgetColors.remainingText(status.state)
            )
        }
    }
}