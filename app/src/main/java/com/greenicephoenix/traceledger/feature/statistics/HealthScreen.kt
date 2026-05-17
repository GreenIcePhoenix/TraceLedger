package com.greenicephoenix.traceledger.feature.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.core.ui.theme.ErrorRed
import com.greenicephoenix.traceledger.core.ui.theme.SuccessGreen
import com.greenicephoenix.traceledger.core.ui.theme.WarningAmber
import com.greenicephoenix.traceledger.feature.statistics.components.BackHeader
import com.greenicephoenix.traceledger.feature.statistics.components.HealthGaugeChart

@Composable
fun HealthScreen(
    viewModel: StatisticsViewModel,
    onBack:    () -> Unit
) {
    val health by viewModel.healthScore.collectAsState()

    LazyColumn(
        modifier            = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding      = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { BackHeader(title = "Financial Health", onBack = onBack) }
        item {
            HealthGaugeChart(data = health, modifier = Modifier.fillMaxWidth())
        }
        // Breakdown cards
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HealthFactorCard(Modifier.weight(1f), "Savings Rate",
                    "${(health.savingsRate * 100).toInt()}%",
                    if (health.savingsRate >= 0.2f) SuccessGreen
                    else if (health.savingsRate >= 0f) WarningAmber else ErrorRed)
                HealthFactorCard(Modifier.weight(1f), "Budget",
                    "${(health.budgetAdherence * 100).toInt()}%",
                    if (health.budgetAdherence >= 0.8f) SuccessGreen
                    else if (health.budgetAdherence >= 0.6f) WarningAmber else ErrorRed)
                HealthFactorCard(Modifier.weight(1f), "Consistency",
                    "${(health.consistency * 100).toInt()}%",
                    if (health.consistency >= 0.6f) SuccessGreen
                    else if (health.consistency >= 0.4f) WarningAmber else ErrorRed)
            }
        }
        item { Spacer(Modifier.height(48.dp)) }
    }
}

@Composable
private fun HealthFactorCard(modifier: Modifier, label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Card(modifier, RoundedCornerShape(14.dp), CardDefaults.cardColors(MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(12.dp), Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
            Text(value, style = MaterialTheme.typography.titleMedium, color = color)
        }
    }
}