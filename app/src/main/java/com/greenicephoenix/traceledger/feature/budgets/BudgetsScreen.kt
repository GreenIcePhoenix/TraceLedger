package com.greenicephoenix.traceledger.feature.budgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.core.ui.theme.NothingRed
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.ui.text.style.TextAlign
import com.greenicephoenix.traceledger.domain.model.CategoryUiModel
import com.greenicephoenix.traceledger.feature.budgets.components.BudgetAccentProgress
import com.greenicephoenix.traceledger.feature.budgets.domain.BudgetStatus
import com.greenicephoenix.traceledger.feature.budgets.ui.BudgetColors
import com.greenicephoenix.traceledger.feature.categories.CategoryIcons
import java.time.YearMonth

private fun List<BudgetStatus>.uniqueByCategory(): List<BudgetStatus> =
    distinctBy { it.categoryId }

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
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val isPastMonth = selectedMonth.isBefore(YearMonth.now())


    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (!isPastMonth) onAddBudget()
                },
                containerColor = if (isPastMonth)
                    NothingRed.copy(alpha = 0.4f)
                else
                    NothingRed
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

            MonthSelector(
                viewModel = viewModel,
                onBack = onBack
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(budgetStatuses.uniqueByCategory()) { status ->
                    BudgetItemCard(
                        status = status,
                        category = categories.first { it.id == status.categoryId },
                        onClick = {
                            onEditBudget(status.budgetId)
                        }
                    )
                }

                if (budgetStatuses.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 120.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            Text(
                                text = "No budgets set for this month.\nTap + to create one.",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            TextButton(
                                onClick = onAddBudget
                            ) {
                                Text(
                                    text = "Create your first budget",
                                    color = NothingRed
                                )
                            }
                        }
                    }
                }
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            //containerColor = BudgetColors.cardBackground(status.state)
            containerColor = MaterialTheme.colorScheme.surface
        )

    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            /* ---------- CATEGORY ROW ---------- */
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector =
                        CategoryIcons.all[category.icon]
                            ?: CategoryIcons.all["default"]!!,
                    contentDescription = null,
                    tint = Color(category.color),
                    modifier = Modifier.size(22.dp)
                )


                Spacer(Modifier.width(12.dp))

                Text(
                    text = category.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            /* ---------- AMOUNT ---------- */
            Text(
                text = "Used: ${status.used} of ${status.limit}",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodySmall
            )

            /* ---------- PROGRESS ---------- */
            BudgetAccentProgress(
                progress = status.progress,
                state = status.state
            )

            /* ---------- REMAINING ---------- */
            Text(
                text = "Remaining ${status.remaining}",
                color = BudgetColors.remainingText(status.state),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun MonthSelector(
    viewModel: BudgetsViewModel,
    onBack: () -> Unit
) {
    val month by viewModel.selectedMonth.collectAsState()

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
            text = "Budgets",
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium
        )
    }

    /* ---------- MONTH SELECTOR ---------- */
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        IconButton(onClick = {
            // month change logic already in ViewModel
            viewModel.selectMonth(month.minusMonths(1))
        }) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "Previous Month",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        Text(
            text = month.month.name.uppercase() + " " + month.year,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium
        )

        IconButton(onClick = {
            viewModel.selectMonth(viewModel.selectedMonth.value.plusMonths(1))
        }) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Next Month",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }

}