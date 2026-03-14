package com.greenicephoenix.traceledger.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.core.currency.CurrencyFormatter
import com.greenicephoenix.traceledger.core.currency.CurrencyManager
import com.greenicephoenix.traceledger.core.navigation.Routes
import com.greenicephoenix.traceledger.core.ui.theme.NothingRed
import com.greenicephoenix.traceledger.core.ui.theme.SuccessGreen
import com.greenicephoenix.traceledger.domain.model.*
import com.greenicephoenix.traceledger.feature.budgets.domain.BudgetState
import com.greenicephoenix.traceledger.feature.budgets.ui.BudgetColors
import com.greenicephoenix.traceledger.feature.dashboard.components.BudgetWarningBanner
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.min

@Composable
fun DashboardScreen(
    accounts: List<AccountUiModel>,
    dashboardViewModel: DashboardViewModel,
    budgetsViewModel: com.greenicephoenix.traceledger.feature.budgets.BudgetsViewModel,
    categories: List<CategoryUiModel>,
    onNavigate: (String) -> Unit,
    onAddAccount: () -> Unit,
    onAccountClick: (AccountUiModel) -> Unit,
    onTransactionClick: (String) -> Unit
) {
    val currency             by CurrencyManager.currency.collectAsState()
    val monthlyIncome        by dashboardViewModel.monthlyIncome.collectAsState()
    val monthlyExpense       by dashboardViewModel.monthlyExpense.collectAsState()
    val monthlyNet           by dashboardViewModel.monthlyNet.collectAsState()
    val recentTxs            by dashboardViewModel.recentTransactions.collectAsState()
    val recurringCost        by dashboardViewModel.recurringMonthlyCost.collectAsState()
    val spendingInsight      by dashboardViewModel.spendingChangeInsight.collectAsState()
    val savingsSummary       by dashboardViewModel.savingsSummary.collectAsState()
    val netWorthTrend        by dashboardViewModel.netWorthTrend.collectAsState()

    val budgetStatuses          by budgetsViewModel.budgetStatuses.collectAsState()
    val hasExceededBudgets      by budgetsViewModel.hasExceededBudgets.collectAsState()
    val exceededBudgetsCount    by budgetsViewModel.exceededBudgetsCount.collectAsState()
    val warningBudgetsCount     by budgetsViewModel.warningBudgetsCount.collectAsState()

    val totalLimit = budgetStatuses.fold(BigDecimal.ZERO) { acc, b -> acc + b.limit }
    val totalUsed  = budgetStatuses.fold(BigDecimal.ZERO) { acc, b -> acc + b.used  }

    val budgetProgress = if (totalLimit > BigDecimal.ZERO)
        totalUsed.divide(totalLimit, 4, RoundingMode.HALF_UP).toFloat()
    else 0f

    val budgetState = when {
        budgetProgress >= 0.90f -> BudgetState.EXCEEDED
        budgetProgress >= 0.75f -> BudgetState.WARNING
        else                    -> BudgetState.SAFE
    }

    val totalBalance = accounts
        .filter { it.includeInTotal }
        .fold(BigDecimal.ZERO) { acc, a -> acc + a.balance }

    val dashboardAccounts = accounts.take(4)

    // Collect all available insights into a list so we can render them in a loop
    val insights = buildList {
        spendingInsight?.let {
            // Green if spending went down, red if up, neutral if unchanged
            val color = when {
                it.contains("less")      -> SuccessGreen
                it.contains("more")      -> NothingRed
                else                     -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            }
            add(InsightItem(Icons.AutoMirrored.Filled.TrendingUp, it, color))
        }
        savingsSummary?.let {
            val color = if (it.contains("Overspent")) NothingRed else SuccessGreen
            add(InsightItem(Icons.Default.Savings, it, color))
        }
        netWorthTrend?.let {
            val color = if (it.contains("up")) SuccessGreen else NothingRed
            add(InsightItem(Icons.Default.AccountBalance, it, color))
        }
        recurringCost?.let {
            // Recurring cost is neutral-informational — use a muted white/grey
            add(InsightItem(
                icon      = Icons.Default.Repeat,
                text      = "Recurring expenses: ${CurrencyFormatter.format(it.toPlainString(), currency)}/mo",
                iconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            ))
        }
    }

    LazyVerticalGrid(
        columns            = GridCells.Fixed(2),
        modifier           = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement   = Arrangement.spacedBy(16.dp),
        contentPadding        = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {

        // ── HEADER ────────────────────────────────────────────────────────────
        item(span = { GridItemSpan(2) }) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text  = "OVERVIEW",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text  = "This month",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }

        // ── BUDGET BANNER ─────────────────────────────────────────────────────
        if (hasExceededBudgets || warningBudgetsCount > 0) {
            item(span = { GridItemSpan(2) }) {
                BudgetWarningBanner(
                    exceededCount = exceededBudgetsCount,
                    warningCount  = warningBudgetsCount,
                    onClick       = { onNavigate(Routes.BUDGETS) }
                )
            }
        }

        // ── TOTAL BALANCE ─────────────────────────────────────────────────────
        item(span = { GridItemSpan(2) }) {
            Card(
                shape  = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text  = "TOTAL BALANCE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                    Text(
                        text  = CurrencyFormatter.format(totalBalance.toPlainString(), currency),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text  = "In  ${CurrencyFormatter.format(monthlyIncome.toPlainString(), currency)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SuccessGreen
                        )
                        Text(
                            text  = "Out  ${CurrencyFormatter.format(monthlyExpense.toPlainString(), currency)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = NothingRed
                        )
                    }
                }
            }
        }

        // ── NET THIS MONTH ────────────────────────────────────────────────────
        item(span = { GridItemSpan(2) }) {
            Card(
                shape  = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text  = "NET THIS MONTH",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text  = CurrencyFormatter.format(monthlyNet.toPlainString(), currency),
                        style = MaterialTheme.typography.titleLarge,
                        color = if (monthlyNet >= BigDecimal.ZERO) SuccessGreen else NothingRed
                    )
                }
            }
        }

        // ── BUDGET CARD ───────────────────────────────────────────────────────
        item(span = { GridItemSpan(2) }) {
            if (budgetStatuses.isNotEmpty()) {
                MonthlyBudgetCard(
                    used    = totalUsed.toDouble(),
                    limit   = totalLimit.toDouble(),
                    state   = budgetState,
                    onClick = { onNavigate(Routes.BUDGETS) }
                )
            } else {
                Card(
                    shape  = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().clickable { onNavigate(Routes.BUDGETS) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector        = Icons.Default.AddCircleOutline,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier           = Modifier.size(20.dp)
                        )
                        Text(
                            text  = "Set a monthly budget",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }

        // ── INSIGHTS SECTION ──────────────────────────────────────────────────
        // Only shown when at least one insight is available
        if (insights.isNotEmpty()) {
            item(span = { GridItemSpan(2) }) {
                Text(
                    text  = "INSIGHTS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }

            item(span = { GridItemSpan(2) }) {
                Card(
                    shape  = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        insights.forEachIndexed { index, insight ->
                            InsightRow(icon = insight.icon, text = insight.text, iconColor = insight.iconColor)
                            if (index < insights.lastIndex) {
                                HorizontalDivider(
                                    modifier  = Modifier.padding(horizontal = 16.dp),
                                    thickness = 0.5.dp,
                                    color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── ACCOUNTS SECTION ──────────────────────────────────────────────────
        item(span = { GridItemSpan(2) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text  = "ACCOUNTS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
                if (accounts.size > 4) {
                    Text(
                        text  = "See all",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onNavigate(Routes.ACCOUNTS) }
                    )
                }
            }
        }

        item { AddAccountCard(onClick = onAddAccount) }

        items(dashboardAccounts) { account ->
            DashboardAccountCard(account = account, onClick = { onAccountClick(account) })
        }

        // ── RECENT TRANSACTIONS ───────────────────────────────────────────────
        if (recentTxs.isNotEmpty()) {
            item(span = { GridItemSpan(2) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text  = "RECENT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                    Text(
                        text  = "See all",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onNavigate(Routes.TRANSACTIONS) }
                    )
                }
            }

            item(span = { GridItemSpan(2) }) {
                Card(
                    shape  = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        recentTxs.forEachIndexed { index, tx ->
                            val category = categories.firstOrNull { it.id == tx.categoryId }
                            val account  = when (tx.type) {
                                TransactionType.EXPENSE,
                                TransactionType.TRANSFER -> accounts.firstOrNull { it.id == tx.fromAccountId }
                                TransactionType.INCOME   -> accounts.firstOrNull { it.id == tx.toAccountId }
                            }
                            val title = when (tx.type) {
                                TransactionType.TRANSFER -> "Transfer → ${accounts.firstOrNull { it.id == tx.toAccountId }?.name ?: "Account"}"
                                else                     -> category?.name ?: "Transaction"
                            }
                            val amountColor = when (tx.type) {
                                TransactionType.INCOME   -> SuccessGreen
                                TransactionType.EXPENSE  -> NothingRed
                                TransactionType.TRANSFER -> MaterialTheme.colorScheme.onSurface
                            }
                            val prefix = when (tx.type) {
                                TransactionType.INCOME   -> "+"
                                TransactionType.EXPENSE  -> "-"
                                TransactionType.TRANSFER -> ""
                            }

                            RecentTransactionRow(
                                title         = title,
                                subtitle      = account?.name ?: "",
                                date          = tx.date.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())),
                                amountText    = "$prefix${CurrencyFormatter.format(tx.amount.toPlainString(), currency)}",
                                amountColor   = amountColor,
                                categoryColor = Color(category?.color ?: 0xFF9E9E9E),
                                onClick       = { onTransactionClick(tx.id) }
                            )

                            if (index < recentTxs.lastIndex) {
                                HorizontalDivider(
                                    modifier  = Modifier.padding(horizontal = 16.dp),
                                    thickness = 0.5.dp,
                                    color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// InsightItem — data holder for a single insight row
// ─────────────────────────────────────────────────────────────────────────────
private data class InsightItem(
    val icon: ImageVector,
    val text: String,
    val iconColor: Color
)

// ─────────────────────────────────────────────────────────────────────────────
// InsightRow — renders one insight inside the insights card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun InsightRow(icon: ImageVector, text: String, iconColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = iconColor,
            modifier           = Modifier.size(18.dp)
        )
        Text(
            text     = text,
            style    = MaterialTheme.typography.bodyMedium,
            color    = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// RecentTransactionRow
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun RecentTransactionRow(
    title: String,
    subtitle: String,
    date: String,
    amountText: String,
    amountColor: Color,
    categoryColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.size(8.dp).background(categoryColor, CircleShape))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = title,
                style    = MaterialTheme.typography.bodyMedium,
                color    = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text  = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(text = amountText, style = MaterialTheme.typography.bodyMedium, color = amountColor)
            Text(text = date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AddAccountCard
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AddAccountCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(92.dp).clickable { onClick() },
        shape    = RoundedCornerShape(18.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Add, null, tint = NothingRed, modifier = Modifier.size(28.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DashboardAccountCard
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun DashboardAccountCard(account: AccountUiModel, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(92.dp).clickable { onClick() },
        shape    = RoundedCornerShape(18.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(account.type.icon(), null, tint = Color(account.color), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text     = account.name,
                    color    = MaterialTheme.colorScheme.onBackground,
                    style    = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IncludedBadge(isIncluded = account.includeInTotal)
            }

            val currency by CurrencyManager.currency.collectAsState()
            Text(
                text  = CurrencyFormatter.format(account.balance.toPlainString(), currency),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.bodyLarge
            )

            Box(modifier = Modifier.fillMaxWidth(0.8f).height(2.dp).background(Color(account.color), RoundedCornerShape(2.dp)))
        }
    }
}

@Composable
private fun IncludedBadge(isIncluded: Boolean) {
    val color = if (isIncluded) SuccessGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    val icon  = if (isIncluded) Icons.Default.Check else Icons.Default.VisibilityOff
    Box(
        modifier = Modifier.size(18.dp).background(color.copy(alpha = 0.15f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(12.dp))
    }
}

@Composable
fun MonthlyBudgetCard(used: Double, limit: Double, state: BudgetState, onClick: () -> Unit) {
    val currency        by CurrencyManager.currency.collectAsState()
    val progress         = if (limit > 0.0) (used / limit).toFloat() else 0f
    val clampedProgress  = min(progress, 1f)
    val accentColor      = BudgetColors.accent(state)

    Card(
        shape    = RoundedCornerShape(20.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("MONTHLY BUDGET", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            Text(
                text  = "${CurrencyFormatter.format(used.toBigDecimal().toPlainString(), currency)} / ${CurrencyFormatter.format(limit.toBigDecimal().toPlainString(), currency)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text  = "USED: ${(clampedProgress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = accentColor
            )
            Box(
                modifier = Modifier.fillMaxWidth().height(4.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(2.dp))
            ) {
                Box(modifier = Modifier.fillMaxWidth(clampedProgress).height(4.dp).background(accentColor, RoundedCornerShape(2.dp)))
            }
        }
    }
}