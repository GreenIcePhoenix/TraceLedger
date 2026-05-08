// ─────────────────────────────────────────────────────────────────────────────
// FILE: feature/accountimport/ui/CategoryPickerSheet.kt
//
// Bottom sheet for picking/changing a category on the review screen.
// Shows EXPENSE and INCOME categories separately based on the transaction type.
// Includes a "No category" option to clear the assignment.
// ─────────────────────────────────────────────────────────────────────────────
package com.greenicephoenix.traceledger.feature.accountimport.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

/**
 * Bottom sheet for selecting a category on the review screen.
 *
 * @param isCredit       true = show INCOME categories, false = show EXPENSE categories.
 * @param categories     Full category list — filtered internally by type.
 * @param currentId      Currently selected category ID (shown with checkmark).
 * @param onSelect       Called with the chosen category ID. Null = "No category".
 * @param onDismiss      Called when the sheet is dismissed without selection.
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
    // Filter to the relevant type
    val targetTypeName = if (isCredit) TransactionType.INCOME.name else TransactionType.EXPENSE.name
    val filtered = categories.filter { it.type.name == targetTypeName }
    val typeLabel = if (isCredit) "Income" else "Expense"

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Sheet title
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

            HorizontalDivider()

            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                // "No category" option at the top
                item {
                    CategoryPickerRow(
                        name       = "No category",
                        colorHex   = null,
                        isSelected = currentId == null,
                        onClick    = { onSelect(null) },
                        isNone     = true
                    )
                }

                items(filtered, key = { it.id }) { category ->
                    CategoryPickerRow(
                        name       = category.name,
                        colorHex   = category.color.toString(),
                        isSelected = category.id == currentId,
                        onClick    = { onSelect(category.id) },
                        isNone     = false
                    )
                }

                // Padding at the bottom for system gesture bar
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun CategoryPickerRow(
    name:       String,
    colorHex:   String?,
    isSelected: Boolean,
    onClick:    () -> Unit,
    isNone:     Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                else Color.Transparent
            )
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Colour dot or "none" icon
        if (isNone) {
            Icon(
                Icons.Outlined.Cancel,
                contentDescription = null,
                tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        } else {
            // Parse the stored hex colour key
            val dotColor = parseColorKey(colorHex)
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(dotColor)
            )
        }

        Text(
            text     = name,
            style    = MaterialTheme.typography.bodyMedium,
            color    = if (isSelected) MaterialTheme.colorScheme.primary
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

/**
 * Parse the colorKey stored in CategoryEntity into a Compose Color.
 * ColorKey is stored as a hex string like "0xFF2ECC71" or "4281549393".
 * Falls back to a neutral grey if the format is unrecognised.
 */
private fun parseColorKey(colorKey: String?): Color {
    if (colorKey == null) return Color.Gray
    return try {
        // Handle both "0xFF..." format and plain long strings
        val long = if (colorKey.startsWith("0x", ignoreCase = true))
            java.lang.Long.decode(colorKey)
        else
            colorKey.toLong()
        Color(long)
    } catch (e: Exception) {
        Color.Gray
    }
}