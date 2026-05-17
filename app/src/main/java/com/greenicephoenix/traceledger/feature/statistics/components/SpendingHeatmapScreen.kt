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
import com.greenicephoenix.traceledger.core.ui.components.MonthSelector
import com.greenicephoenix.traceledger.core.ui.theme.NothingRed
import com.greenicephoenix.traceledger.feature.statistics.components.BackHeader
import com.greenicephoenix.traceledger.feature.statistics.components.SpendingHeatmap
import com.greenicephoenix.traceledger.feature.statistics.model.CalendarDay
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpendingHeatmapScreen(
    viewModel: StatisticsViewModel,
    onBack:    () -> Unit
) {
    val currency      by CurrencyManager.currency.collectAsState()
    val heatmap       by viewModel.calendarHeatmap.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()

    var tappedDay by remember { mutableStateOf<CalendarDay?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LazyColumn(
        modifier            = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding      = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { BackHeader(title = "Spending Heatmap", onBack = onBack) }

        item {
            MonthSelector(
                month      = selectedMonth,
                onPrevious = viewModel::previousMonth,
                onNext     = viewModel::nextMonth
            )
        }

        // Legend row
        item {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    "Less",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
                listOf(0.0f, 0.25f, 0.5f, 0.75f, 1.0f).forEach { intensity ->
                    val color = androidx.compose.ui.graphics.lerp(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.primary,
                        intensity * 0.85f
                    )
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(color, RoundedCornerShape(3.dp))
                    )
                }
                Text(
                    "More",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }

        item {
            if (heatmap.isEmpty()) {
                Box(
                    modifier         = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = "No spending data this month",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            } else {
                SpendingHeatmap(
                    days      = heatmap,
                    modifier  = Modifier.fillMaxWidth(),
                    onDayTap  = { day -> if (day.totalExpense > 0.0) tappedDay = day }
                )
            }
        }

        item { Spacer(Modifier.height(48.dp)) }
    }

    // Day detail sheet
    tappedDay?.let { day ->
        ModalBottomSheet(
            onDismissRequest = { tappedDay = null },
            sheetState       = sheetState
        ) {
            Column(
                modifier            = Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text  = day.date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        "Total spent",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        CurrencyFormatter.format(day.totalExpense.toString(), currency),
                        style = MaterialTheme.typography.titleMedium,
                        color = NothingRed
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}