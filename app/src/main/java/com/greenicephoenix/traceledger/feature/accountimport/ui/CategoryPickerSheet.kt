package com.greenicephoenix.traceledger.feature.accountimport.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.domain.model.CategoryUiModel
import com.greenicephoenix.traceledger.domain.model.TransactionType
import com.greenicephoenix.traceledger.feature.categories.CategoryIcons

/**
 * Bottom sheet for selecting a category on the import review screen.
 * Matches the AddTransaction CategorySelector style — icons + color circles.
 *
 * @param isCredit   true = INCOME categories, false = EXPENSE categories
 * @param categories Full list — filtered internally by type
 * @param currentId  Currently selected category ID
 * @param onSelect   Called with chosen ID, or null for "No category"
 * @param onDismiss  Called on dismiss without selection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportCategoryPickerSheet(
    isCredit:   Boolean,
    categories: List<CategoryUiModel>,
    currentId:  String?,
    onSelect:   (String?) -> Unit,
    onDismiss:  () -> Unit
) {
    val targetTypeName = if (isCredit) TransactionType.INCOME.name else TransactionType.EXPENSE.name
    val filtered   = categories.filter { it.type.name == targetTypeName }
    val typeLabel  = if (isCredit) "Income" else "Expense"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text       = "$typeLabel Categories",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.weight(1f)
                )
                Text(
                    text  = "Tap to assign",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {

                // "No category" option
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (currentId == null)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                else Color.Transparent
                            )
                            .clickable { onSelect(null) }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Icon placeholder — same size as category icons
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Cancel,
                                contentDescription = null,
                                tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text       = "No category",
                            style      = MaterialTheme.typography.bodyMedium,
                            color      = if (currentId == null) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontWeight = if (currentId == null) FontWeight.Medium else FontWeight.Normal,
                            modifier   = Modifier.weight(1f)
                        )
                        if (currentId == null) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                tint     = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                items(filtered, key = { it.id }) { category ->
                    val isSelected = category.id == currentId
                    val catColor   = Color(category.color)
                    val icon       = CategoryIcons.iconFor(category.icon)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                else Color.Transparent
                            )
                            .clickable { onSelect(category.id) }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Icon circle — matches AddTransaction CategorySelector style
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(catColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector        = icon,
                                contentDescription = null,
                                tint               = catColor,
                                modifier           = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text       = category.name,
                            style      = MaterialTheme.typography.bodyMedium,
                            color      = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                            modifier   = Modifier.weight(1f)
                        )

                        if (isSelected) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                tint     = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}