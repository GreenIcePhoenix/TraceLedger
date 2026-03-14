package com.greenicephoenix.traceledger.feature.transactions.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.core.ui.theme.NothingRed
import com.greenicephoenix.traceledger.core.ui.theme.SuccessGreen
import com.greenicephoenix.traceledger.domain.model.TransactionType
import com.greenicephoenix.traceledger.domain.model.TransactionUiModel

@Composable
fun TransactionRow(
    modifier: Modifier = Modifier,
    transaction: TransactionUiModel,
    categoryName: String,
    categoryIcon: ImageVector,
    categoryColor: Color,
    accountName: String,
    amountText: String,
    onClick: () -> Unit
) {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f

    val gradientColors = if (isLight)
        listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface)
    else
        listOf(Color(0xFF1A1A1A), Color(0xFF0F0F0F))

    // FIX: Income was coloured with MaterialTheme.colorScheme.primary which is NothingRed.
    // Income should be green. Only expense is red.
    val amountColor = when (transaction.type) {
        TransactionType.EXPENSE  -> NothingRed
        TransactionType.INCOME   -> SuccessGreen
        TransactionType.TRANSFER -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    }

    Card(
        modifier  = modifier.fillMaxWidth().clickable { onClick() },
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(gradientColors), RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category icon
                    Box(modifier = Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector        = categoryIcon,
                            contentDescription = null,
                            tint               = categoryColor,
                            modifier           = Modifier.size(20.dp)
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(
                        modifier            = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text  = buildString {
                                    append(categoryName)
                                    transaction.note?.let { append(" · $it") }
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (transaction.recurringId != null) {
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    imageVector        = Icons.Default.Sync,
                                    contentDescription = "Recurring",
                                    modifier           = Modifier.size(14.dp),
                                    tint               = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Text(
                            text  = "$accountName · ${transaction.date}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    Text(
                        text  = amountText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = amountColor
                    )
                }

                // Accent line
                Box(
                    modifier = Modifier
                        .padding(start = 52.dp)
                        .fillMaxWidth(0.7f)
                        .height(1.dp)
                        .background(categoryColor.copy(alpha = 0.4f), RoundedCornerShape(1.dp))
                )
            }
        }
    }
}