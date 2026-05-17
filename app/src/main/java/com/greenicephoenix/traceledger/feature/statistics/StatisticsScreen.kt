package com.greenicephoenix.traceledger.feature.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.greenicephoenix.traceledger.feature.statistics.components.BudgetRing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ChevronRight
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
import com.greenicephoenix.traceledger.feature.statistics.components.SparklineChart
import com.greenicephoenix.traceledger.feature.statistics.components.SpendingStreakCard
import java.math.BigDecimal

@Composable
fun StatisticsScreen(
    viewModel:   StatisticsViewModel,
    categoryMap: Map<String, CategoryUiModel>,
    onNavigate:  (String) -> Unit
) {
    val currency           by CurrencyManager.currency.collectAsState()
    val selectedMonth      by viewModel.selectedMonth.collectAsState()
    val totalIncome        by viewModel.totalIncome.collectAsState()
    val totalExpense       by viewModel.totalExpense.collectAsState()
    val netAmount          by viewModel.netAmount.collectAsState()
    val prevIncome         by viewModel.prevMonthIncome.collectAsState()
    val prevExpense        by viewModel.prevMonthExpense.collectAsState()
    val dailyExpensePoints by viewModel.dailyExpensePoints.collectAsState()
    val dailyIncomePoints  by viewModel.dailyIncomePoints.collectAsState()
    val budgetRings by viewModel.budgetRings.collectAsState()
    val spendingStreak by viewModel.spendingStreak.collectAsState()

    LazyColumn(
        modifier            = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding      = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "STATISTICS",
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

        // Income + Expense stat cards with sparklines
        item {
            Row(
                modifier              = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SparkStatCard(
                    modifier     = Modifier.weight(1f).fillMaxHeight(),
                    label        = "INCOME",
                    value        = CurrencyFormatter.format(totalIncome.toPlainString(), currency),
                    valueColor   = SuccessGreen,
                    previous     = prevIncome,
                    current      = totalIncome,
                    invertDelta  = false,
                    sparkPoints  = dailyIncomePoints,
                    sparkColor   = SuccessGreen
                )
                SparkStatCard(
                    modifier     = Modifier.weight(1f).fillMaxHeight(),
                    label        = "EXPENSE",
                    value        = CurrencyFormatter.format(totalExpense.toPlainString(), currency),
                    valueColor   = NothingRed,
                    previous     = prevExpense,
                    current      = totalExpense,
                    invertDelta  = true,
                    sparkPoints  = dailyExpensePoints,
                    sparkColor   = NothingRed
                )
            }
        }

        // Net card with savings rate
        item {
            Card(
                shape    = RoundedCornerShape(16.dp),
                colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "NET",
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

        item {
            if (budgetRings.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "BUDGETS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding        = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(budgetRings) { ring ->
                            BudgetRing(data = ring)
                        }
                    }
                }
            }
        }

        item {
            Text(
                "BREAKDOWNS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }

        item {
            SpendingStreakCard(
                streak   = spendingStreak,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item { NavCard("Expense Breakdown",  "Where your money goes this month") { onNavigate(Routes.STATISTICS_BREAKDOWN) } }
        item { NavCard("Income Breakdown",   "Your income sources this month")   { onNavigate(Routes.STATISTICS_INCOME)    } }
        item { NavCard("Cashflow",           "Daily income vs expense")          { onNavigate(Routes.STATISTICS_CASHFLOW)  } }
        item { NavCard("Spending Trends",    "Category spend across months")     { onNavigate(Routes.STATISTICS_TRENDS)    } }
        item { NavCard("Income vs Expense", "12-month area chart")          { onNavigate(Routes.STATISTICS_AREA)      } }
        item { NavCard("Cashflow Waterfall", "Monthly flow breakdown")      { onNavigate(Routes.STATISTICS_WATERFALL) } }
        item { NavCard("Spending Heatmap",  "Calendar view of daily spend") { onNavigate(Routes.STATISTICS_HEATMAP)   } }
        item { NavCard("Day of Week",       "When do you spend most?")      { onNavigate(Routes.STATISTICS_WEEKDAY)   } }

        item {
            Text("ANALYSIS", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
        }
        item { NavCard("Spending Map",         "Proportional category treemap")        { onNavigate(Routes.STATISTICS_TREEMAP)          } }
        item { NavCard("Money Flow",           "Income → expense Sankey diagram")      { onNavigate(Routes.STATISTICS_SANKEY)           } }
        item { NavCard("Financial Health",     "Composite health score")               { onNavigate(Routes.STATISTICS_HEALTH)           } }
        item { NavCard("Savings Rate Trend",   "Are you saving more each month?")      { onNavigate(Routes.STATISTICS_SAVINGS_RATE)     } }
        item { NavCard("Expense Velocity",     "Are you spending faster than usual?")  { onNavigate(Routes.STATISTICS_VELOCITY)         } }
        item { NavCard("Month vs Last Month",  "Top 5 category comparison")            { onNavigate(Routes.STATISTICS_CAT_COMPARE)      } }
        item { NavCard("Income Stability",     "How consistent is your income?")       { onNavigate(Routes.STATISTICS_INCOME_STABILITY) } }
        item { NavCard("Top Spending Days",    "Your 10 biggest spend days ever")      { onNavigate(Routes.STATISTICS_TOP_DAYS)         } }
        item { NavCard("30/60/90 Summary",     "Rolling expense windows")              { onNavigate(Routes.STATISTICS_ROLLING)          } }

        item { Spacer(Modifier.height(48.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SparkStatCard — stat card with a mini sparkline
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SparkStatCard(
    modifier:    Modifier = Modifier,
    label:       String,
    value:       String,
    valueColor:  androidx.compose.ui.graphics.Color,
    previous:    BigDecimal,
    current:     BigDecimal,
    invertDelta: Boolean,
    sparkPoints: List<Float>,
    sparkColor:  androidx.compose.ui.graphics.Color
) {
    val delta       = current.subtract(previous)
    val isFlat      = delta.compareTo(BigDecimal.ZERO) == 0 || previous == BigDecimal.ZERO
    val isUp        = delta > BigDecimal.ZERO
    val positiveDir = if (invertDelta) !isUp else isUp

    val arrowIcon: ImageVector = when {
        isFlat -> Icons.Default.Remove
        isUp   -> Icons.Default.ArrowDropUp
        else   -> Icons.AutoMirrored.Filled.KeyboardArrowRight
    }
    val arrowColor = when {
        isFlat      -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        positiveDir -> SuccessGreen
        else        -> NothingRed
    }

    Card(
        modifier = modifier,
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall,  color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Text(value, style = MaterialTheme.typography.titleMedium, color = valueColor)

            // Sparkline — only render when there is meaningful data
            if (sparkPoints.any { it > 0f }) {
                SparklineChart(
                    points   = sparkPoints,
                    color    = sparkColor,
                    modifier = Modifier.fillMaxWidth().height(36.dp).padding(top = 4.dp),
                    showArea = true
                )
            }

            if (!isFlat) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(arrowIcon, null, tint = arrowColor, modifier = Modifier.size(16.dp))
                    Text(
                        "vs last month",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// NavCard — unchanged from original
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun NavCard(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title,    style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,   color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
        }
    }
}