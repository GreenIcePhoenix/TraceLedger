package com.greenicephoenix.traceledger.feature.about

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenicephoenix.traceledger.core.util.ChangeItem
import com.greenicephoenix.traceledger.core.util.ChangeType
import com.greenicephoenix.traceledger.core.util.ChangelogData
import com.greenicephoenix.traceledger.core.util.VersionEntry

/**
 * ChangelogScreen — full TraceLedger release history.
 *
 * BEHAVIOUR:
 *   - Latest entry (index 0) is always expanded on first load.
 *   - All older entries start collapsed; tapping their header toggles them.
 *   - Content is sourced from ChangelogData.kt — add new entries at the top of that file.
 *
 * Entry point: Settings → SYSTEM → What's New
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogScreen(onBack: () -> Unit) {
    // Tracks which older entries are manually expanded.
    // Latest (index 0) is always expanded — no state needed for it.
    val expandedVersions = remember { mutableStateListOf<String>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text  = "WHAT'S NEW",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint               = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier            = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(ChangelogData.entries) { index, entry ->
                val isLatest   = index == 0
                val isExpanded = isLatest || expandedVersions.contains(entry.version)

                VersionCard(
                    entry      = entry,
                    isLatest   = isLatest,
                    isExpanded = isExpanded,
                    onToggle   = {
                        // Latest is pinned open — only allow toggle on older entries
                        if (!isLatest) {
                            if (expandedVersions.contains(entry.version))
                                expandedVersions.remove(entry.version)
                            else
                                expandedVersions.add(entry.version)
                        }
                    }
                )
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// VersionCard — one card per release
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun VersionCard(
    entry      : VersionEntry,
    isLatest   : Boolean,
    isExpanded : Boolean,
    onToggle   : () -> Unit
) {
    Card(
        shape    = RoundedCornerShape(20.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {

            // ── Header row — always visible ───────────────────────────────────
            // Latest entry has no click handler (cannot be collapsed).
            // Older entries are fully clickable and show a chevron.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (!isLatest) Modifier.clickable(onClick = onToggle) else Modifier)
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {

                    // Version string, LATEST badge, release date
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text  = "v${entry.version}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight    = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isLatest) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text     = "LATEST",
                                        style    = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight    = FontWeight.Bold,
                                            fontSize      = 9.sp,
                                            letterSpacing = 1.sp
                                        ),
                                        color    = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text  = entry.releaseDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }

                    Spacer(Modifier.height(3.dp))

                    // Tagline — always visible, even when the card is collapsed
                    Text(
                        text      = entry.tagline,
                        style     = MaterialTheme.typography.bodySmall,
                        color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontStyle = FontStyle.Italic
                    )
                }

                // Chevron — only rendered on collapsible (non-latest) entries
                if (!isLatest) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector        = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                        modifier           = Modifier.size(20.dp)
                    )
                }
            }

            // ── Change list — animated expand / collapse ───────────────────
            // 200ms expand, 160ms collapse — snappy without feeling rushed.
            AnimatedVisibility(
                visible = isExpanded,
                enter   = expandVertically(tween(200)),
                exit    = shrinkVertically(tween(160))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
                ) {
                    HorizontalDivider(
                        color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        thickness = 0.5.dp
                    )
                    Spacer(Modifier.height(12.dp))
                    entry.changes.forEach { change ->
                        ChangeItemRow(item = change)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ChangeItemRow — coloured dot + type label + description
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChangeItemRow(item: ChangeItem) {
    Row(
        modifier              = Modifier.padding(vertical = 4.dp),
        verticalAlignment     = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Coloured dot — type indicator
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(6.dp)
                .background(color = changeTypeColor(item.type), shape = CircleShape)
        )
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            // Type label (e.g. "NEW", "IMPROVED")
            Text(
                text  = item.type.label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 0.8.sp,
                    fontSize      = 9.sp,
                    fontWeight    = FontWeight.Bold
                ),
                color = changeTypeColor(item.type).copy(alpha = 0.85f)
            )
            // Description
            Text(
                text       = item.description,
                style      = MaterialTheme.typography.bodySmall,
                color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                lineHeight = 18.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// changeTypeColor — dot and label colour per ChangeType
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun changeTypeColor(type: ChangeType): Color = when (type) {
    ChangeType.NEW      -> MaterialTheme.colorScheme.primary  // Sovereign Violet
    ChangeType.IMPROVED -> Color(0xFF00BCD4)                  // Teal
    ChangeType.FIXED    -> Color(0xFF4CAF50)                  // Green
    ChangeType.SECURITY -> Color(0xFFFFAB00)                  // Amber
}