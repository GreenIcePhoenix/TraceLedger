package com.greenicephoenix.traceledger.feature.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.core.currency.CurrencyFormatter
import com.greenicephoenix.traceledger.core.currency.CurrencyManager
import com.greenicephoenix.traceledger.core.navigation.Routes
import com.greenicephoenix.traceledger.core.ui.components.MonthSelector
import com.greenicephoenix.traceledger.core.ui.theme.NothingRed
import com.greenicephoenix.traceledger.core.ui.theme.SuccessGreen
import com.greenicephoenix.traceledger.domain.model.CategoryUiModel
import java.math.BigDecimal

@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel,
    categoryMap: Map<String, CategoryUiModel>,
    onNavigate: (String) -> Unit
) {
    val currency       by CurrencyManager.currency.collectAsState()
    val selectedMonth  by viewModel.selectedMonth.collectAsState()
    val totalIncome    by viewModel.totalIncome.collectAsState()
    val totalExpense   by viewModel.totalExpense.collectAsState()
    val netAmount      by viewModel.netAmount.collectAsState()
    val prevIncome     by viewModel.prevMonthIncome.collectAsState()
    val prevExpense    by viewModel.prevMonthExpense.collectAsState()

    LazyColumn(
        modifier       = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        item {
            Text(
                text  = "STATISTICS",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            MonthSelector(
                month      = selectedMonth,
                onPrevious = viewModel::previousMonth,
                onNext     = viewModel::nextMonth
            )
        }

        // ── Summary row: Income / Expense / Net ───────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    label    = "INCOME",
                    value    = CurrencyFormatter.format(totalIncome.toPlainString(), currency),
                    valueColor = SuccessGreen,
                    previous   = prevIncome,
                    current    = totalIncome
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    label    = "EXPENSE",
                    value    = CurrencyFormatter.format(totalExpense.toPlainString(), currency),
                    valueColor = NothingRed,
                    previous   = prevExpense,
                    current    = totalExpense,
                    invertDelta = true   // higher expense = worse, so arrow is inverted
                )
            }
        }

        item {
            Card(
                shape  = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text  = "NET",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text  = CurrencyFormatter.format(netAmount.toPlainString(), currency),
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (netAmount.signum() >= 0) SuccessGreen else NothingRed
                    )
                    if (totalIncome > BigDecimal.ZERO) {
                        val savingsRate = netAmount
                            .multiply(BigDecimal(100))
                            .divide(totalIncome, 0, java.math.RoundingMode.HALF_UP)
                            .toInt()
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text  = if (savingsRate >= 0) "Savings rate: $savingsRate%"
                            else "Overspent by ${-savingsRate}% of income",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }

        // ── Navigation cards ──────────────────────────────────────────────────
        item {
            Text(
                text  = "BREAKDOWNS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }

        item {
            NavCard(
                title    = "Expense Breakdown",
                subtitle = "Where your money goes this month",
                onClick  = { onNavigate(Routes.STATISTICS_BREAKDOWN) }
            )
        }

        item {
            NavCard(
                title    = "Income Breakdown",
                subtitle = "Your income sources this month",
                onClick  = { onNavigate(Routes.STATISTICS_INCOME) }
            )
        }

        item {
            NavCard(
                title    = "Cashflow",
                subtitle = "Daily income vs expense",
                onClick  = { onNavigate(Routes.STATISTICS_CASHFLOW) }
            )
        }

        item { Spacer(Modifier.height(48.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// StatCard — metric card with vs-last-month comparison arrow
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color,
    previous: BigDecimal,
    current: BigDecimal,
    invertDelta: Boolean = false
) {
    val delta      = current.subtract(previous)
    val isUp       = delta > BigDecimal.ZERO
    val isFlat     = delta.compareTo(BigDecimal.ZERO) == 0 || previous == BigDecimal.ZERO
    val positiveUp = if (invertDelta) !isUp else isUp

    val arrowIcon: ImageVector = when {
        isFlat -> Icons.Default.Remove
        isUp   -> Icons.Default.ArrowDropUp
        else   -> Icons.Default.ArrowDropDown
    }
    val arrowColor = when {
        isFlat    -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        positiveUp -> SuccessGreen
        else       -> NothingRed
    }

    Card(
        modifier = modifier,
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Text(text = value, style = MaterialTheme.typography.titleMedium, color = valueColor)
            if (!isFlat) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(arrowIcon, null, tint = arrowColor, modifier = Modifier.size(16.dp))
                    Text(
                        text  = "vs last month",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// NavCard — tappable section navigation card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun NavCard(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
            Icon(
                imageVector        = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier           = Modifier.size(24.dp)
            )
        }
    }
}