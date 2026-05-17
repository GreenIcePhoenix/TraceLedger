package com.greenicephoenix.traceledger.feature.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.core.currency.CurrencyFormatter
import com.greenicephoenix.traceledger.core.currency.CurrencyManager
import com.greenicephoenix.traceledger.core.ui.theme.NothingRed
import com.greenicephoenix.traceledger.core.ui.theme.SuccessGreen
import com.greenicephoenix.traceledger.core.ui.theme.WarningAmber
import com.greenicephoenix.traceledger.feature.statistics.StatisticsViewModel.RollingWindowData
import com.greenicephoenix.traceledger.feature.statistics.components.BackHeader

@Composable
fun RollingWindowScreen(viewModel: StatisticsViewModel, onBack: () -> Unit) {
    val windows  by viewModel.rollingWindows.collectAsState()
    val currency by CurrencyManager.currency.collectAsState()

    LazyColumn(
        modifier            = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding      = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { BackHeader(title = "30/60/90 Day Summary", onBack = onBack) }
        item { Text("Rolling expense windows ending today", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(0.5f)) }

        items(windows.size) { i ->
            RollingWindowCard(window = windows[i], currency = currency)
        }

        item { Spacer(Modifier.height(48.dp)) }
    }
}

@Composable
private fun RollingWindowCard(
    window:   RollingWindowData,
    currency: com.greenicephoenix.traceledger.core.currency.Currency
) {
    val trendColor = when {
        window.trend > 0.1f  -> NothingRed
        window.trend < -0.1f -> SuccessGreen
        else                 -> WarningAmber
    }
    val trendLabel = when {
        window.trend > 0.1f  -> "+${(window.trend * 100).toInt()}% vs prior period"
        window.trend < -0.1f -> "${(window.trend * 100).toInt()}% vs prior period"
        else                 -> "Stable vs prior period"
    }

    Card(
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(window.label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(trendLabel, style = MaterialTheme.typography.labelSmall, color = trendColor)
            }
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                StatItem(Modifier.weight(1f), "Spent",   CurrencyFormatter.format(window.totalExpense.toString(), currency), NothingRed)
                StatItem(Modifier.weight(1f), "Earned",  CurrencyFormatter.format(window.totalIncome.toString(),  currency), SuccessGreen)
                StatItem(Modifier.weight(1f), "Daily avg", CurrencyFormatter.format(window.dailyAverage.toString(), currency), MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun StatItem(modifier: Modifier, label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(modifier, Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
        Text(value, style = MaterialTheme.typography.titleSmall, color = color)
    }
}