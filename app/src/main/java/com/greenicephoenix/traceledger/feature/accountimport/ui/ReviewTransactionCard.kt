// ─────────────────────────────────────────────────────────────────────────────
// FILE: feature/accountimport/ui/ReviewTransactionCard.kt
//
// One transaction row in the ImportReviewScreen's LazyColumn.
// Shows date, description, amount, type, category, duplicate badge,
// and an include/exclude checkbox.
// ─────────────────────────────────────────────────────────────────────────────
package com.greenicephoenix.traceledger.feature.accountimport.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenicephoenix.traceledger.core.currency.CurrencyFormatter
import com.greenicephoenix.traceledger.core.currency.CurrencyManager
import com.greenicephoenix.traceledger.domain.model.CategoryUiModel
import com.greenicephoenix.traceledger.feature.accountimport.model.ImportReviewItem
import java.time.format.DateTimeFormatter

// Amber colour for duplicate warning — intentionally outside Material theme
// so it remains visible on both dark and light surfaces.
private val DuplicateAmber = Color(0xFFF59E0B)
private val DuplicateAmberBg = Color(0xFFF59E0B).copy(alpha = 0.12f)

// Date formatter shown on the card
private val CARD_DATE_FMT = DateTimeFormatter.ofPattern("dd MMM")

/**
 * Single transaction card in the import review list.
 *
 * @param item           The review item to display.
 * @param categoryMap    Map of categoryId → CategoryUiModel for name lookup.
 * @param onToggle       Called when the user taps the include/exclude checkbox.
 * @param onCategoryTap  Called when the user taps the category chip.
 */
@Composable
fun ReviewTransactionCard(
    item:          ImportReviewItem,
    categoryMap:   Map<String, CategoryUiModel>,
    onToggle:      () -> Unit,
    onCategoryTap: () -> Unit
) {
    val currency by CurrencyManager.currency.collectAsState()

    // Fade out excluded rows so the list communicates intent clearly
    val cardAlpha = if (item.isIncluded && !item.hasDateError) 1f else 0.45f

    // Animate border colour: amber for duplicates, primary for normal
    val borderColor by animateColorAsState(
        targetValue = when {
            item.hasDateError -> MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
            item.isDuplicate  -> DuplicateAmber.copy(alpha = 0.6f)
            else              -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        },
        label = "borderColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(cardAlpha)
            .border(
                width  = if (item.isDuplicate || item.hasDateError) 1.dp else 0.dp,
                color  = borderColor,
                shape  = RoundedCornerShape(14.dp)
            ),
        shape  = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isDuplicate && item.isIncluded)
                DuplicateAmberBg
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // ── Date column ───────────────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(42.dp)
            ) {
                if (item.hasDateError) {
                    // Date couldn't be parsed — show error icon
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = "Date error",
                        tint     = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text  = "ERR",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 9.sp
                    )
                } else {
                    val date = item.parsed.date
                    Text(
                        text  = date?.format(DateTimeFormatter.ofPattern("dd")) ?: "--",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text  = date?.format(DateTimeFormatter.ofPattern("MMM")) ?: "--",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            // ── Description + category ────────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text     = item.note,
                    style    = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color    = MaterialTheme.colorScheme.onSurface,
                    // Strike through excluded rows
                    textDecoration = if (!item.isIncluded && !item.hasDateError)
                        TextDecoration.LineThrough else TextDecoration.None
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Duplicate warning badge
                    if (item.isDuplicate) {
                        DuplicateBadge()
                    }

                    // Date error badge
                    if (item.hasDateError) {
                        ErrorBadge("Cannot parse date: ${item.parsed.rawDate}")
                    }

                    // Category chip — tappable to change
                    val category = item.categoryId?.let { categoryMap[it] }
                    CategoryChip(
                        categoryName = category?.name ?: "No category",
                        hasCategory  = category != null,
                        onClick      = { if (!item.hasDateError) onCategoryTap() }
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            // ── Amount + toggle ───────────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Amount coloured by direction
                Text(
                    text  = (if (item.parsed.isCredit) "+" else "-") +
                            CurrencyFormatter.format(item.parsed.amount.toPlainString(), currency),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (item.parsed.isCredit)
                        Color(0xFF27AE60)     // SuccessGreen — income
                    else
                        MaterialTheme.colorScheme.error   // ErrorRed — expense
                )

                // Include/exclude checkbox
                // hasDateError rows are locked excluded (checkbox disabled)
                Checkbox(
                    checked         = item.isIncluded && !item.hasDateError,
                    onCheckedChange = { if (!item.hasDateError) onToggle() },
                    enabled         = !item.hasDateError,
                    colors          = CheckboxDefaults.colors(
                        checkedColor   = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ── Private badge composables ─────────────────────────────────────────────────

/** Yellow badge shown when a transaction is a potential duplicate. */
@Composable
private fun DuplicateBadge() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(DuplicateAmberBg)
            .padding(horizontal = 5.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            Icons.Default.Warning,
            contentDescription = "Possible duplicate",
            tint     = DuplicateAmber,
            modifier = Modifier.size(10.dp)
        )
        Text(
            text  = "Duplicate?",
            style = MaterialTheme.typography.labelSmall,
            color = DuplicateAmber,
            fontSize = 9.sp
        )
    }
}

/** Red badge shown when a date could not be parsed. */
@Composable
private fun ErrorBadge(message: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 5.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text  = message,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            fontSize = 9.sp
        )
    }
}

/** Tappable category chip. Different style when no category is assigned. */
@Composable
private fun CategoryChip(
    categoryName: String,
    hasCategory:  Boolean,
    onClick:      () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(
                if (hasCategory)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text  = categoryName,
            style = MaterialTheme.typography.labelSmall,
            color = if (hasCategory)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            fontSize = 10.sp
        )
        Icon(
            Icons.Default.Edit,
            contentDescription = "Change category",
            tint     = if (hasCategory)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            else
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            modifier = Modifier.size(9.dp)
        )
    }
}