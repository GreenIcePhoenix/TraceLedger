package com.greenicephoenix.traceledger.feature.transactions.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.domain.model.TransactionType
import com.greenicephoenix.traceledger.domain.model.TransactionUiModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncAlt

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

    val gradientColors =
        if (isLight) {
            listOf(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.surface
            )
        } else {
            listOf(
                Color(0xFF1A1A1A),
                Color(0xFF0F0F0F)
            )
        }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(gradientColors),
                    shape = RoundedCornerShape(18.dp)
                )
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // ICON
                    Box(
                        modifier = Modifier.size(46.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = categoryIcon,
                            contentDescription = null,
                            tint = categoryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Text(
                                text = buildString {
                                    append(categoryName)
                                    transaction.note?.let {
                                        append(" · ")
                                        append(it)
                                    }
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (transaction.recurringId != null) {

                                Spacer(modifier = Modifier.width(6.dp))

                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Recurring",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Text(
                            text = "$accountName · ${transaction.date}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = MaterialTheme.typography.bodySmall.fontSize * 1.1
                            ),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    Text(
                        text = amountText,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = MaterialTheme.typography.bodyLarge.fontSize * 1.15
                        ),
                        color =
                            when (transaction.type) {
                                TransactionType.EXPENSE ->
                                    MaterialTheme.colorScheme.error

                                TransactionType.INCOME ->
                                    MaterialTheme.colorScheme.primary

                                TransactionType.TRANSFER ->
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            }
                    )
                }

                // Accent line
                Box(
                    modifier = Modifier
                        .padding(start = 52.dp)
                        .fillMaxWidth(0.7f)
                        .height(1.dp)
                        .background(
                            color = categoryColor.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(1.dp)
                        )
                )
            }
        }
    }
}
