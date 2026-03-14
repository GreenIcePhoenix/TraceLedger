package com.greenicephoenix.traceledger.feature.about

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.greenicephoenix.traceledger.BuildConfig
import com.greenicephoenix.traceledger.core.ui.theme.NothingRed
import com.greenicephoenix.traceledger.core.ui.theme.SuccessGreen
import com.greenicephoenix.traceledger.core.util.ChangelogIconMapper
import com.greenicephoenix.traceledger.core.util.ChangelogParser
import com.greenicephoenix.traceledger.core.util.VersionChangelog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context    = LocalContext.current
    val versions   = remember { ChangelogParser.loadVersioned(context) }
    val current    = versions.firstOrNull { it.version == BuildConfig.VERSION_NAME }
    val previous   = versions.filter { it.version != BuildConfig.VERSION_NAME }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text  = "ABOUT",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
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
            contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── APP IDENTITY ──────────────────────────────────────────────────
            item {
                AppIdentityCard()
            }

            // ── CURRENT VERSION WHATS NEW ─────────────────────────────────────
            if (current != null) {
                item {
                    SectionLabel("WHAT'S NEW")
                }
                item {
                    VersionCard(
                        changelog  = current,
                        isCurrent  = true,
                        expandedByDefault = true
                    )
                }
            }

            // ── PREVIOUS VERSIONS ─────────────────────────────────────────────
            if (previous.isNotEmpty()) {
                item {
                    SectionLabel("PREVIOUS VERSIONS")
                }
                items(previous) { changelog ->
                    VersionCard(
                        changelog         = changelog,
                        isCurrent         = false,
                        expandedByDefault = false
                    )
                }
            }

            // ── PRIVACY PROMISE ───────────────────────────────────────────────
            item {
                SectionLabel("PRIVACY")
            }
            item {
                PrivacyCard()
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AppIdentityCard
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AppIdentityCard() {
    Card(
        shape    = RoundedCornerShape(24.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        // FIX: Added modifier = Modifier.fillMaxWidth() to the Column.
        // Without it the Column was only as wide as its widest child,
        // so horizontalAlignment = CenterHorizontally had nothing to center against.
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(NothingRed.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = "TL",
                    style = MaterialTheme.typography.headlineMedium,
                    color = NothingRed
                )
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text      = "TRACELEDGER",
                style     = MaterialTheme.typography.headlineMedium,
                color     = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Text(
                text      = "Version ${BuildConfig.VERSION_NAME}  ·  Build ${BuildConfig.VERSION_CODE}",
                style     = MaterialTheme.typography.labelSmall,
                color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text      = "Private finance tracking, offline-first.",
                style     = MaterialTheme.typography.bodyMedium,
                color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// VersionCard — collapsible card showing one version's changelog entries
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun VersionCard(
    changelog: VersionChangelog,
    isCurrent: Boolean,
    expandedByDefault: Boolean
) {
    var expanded by remember { mutableStateOf(expandedByDefault) }

    Card(
        shape  = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Header row — version + expand toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text  = "v${changelog.version}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isCurrent) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = NothingRed.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text     = "Current",
                                style    = MaterialTheme.typography.labelSmall,
                                color    = NothingRed,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Feature entries — animated expand/collapse
            AnimatedVisibility(
                visible = expanded,
                enter   = expandVertically(),
                exit    = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    )

                    if (changelog.entries.isNotEmpty()) {
                        changelog.entries.forEach { entry ->
                            ChangelogEntryRow(entry = entry)
                        }
                    } else {
                        // Fallback: plain text for old-format entries
                        Text(
                            text     = changelog.rawText,
                            style    = MaterialTheme.typography.bodySmall,
                            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ChangelogEntryRow — one feature line with icon, title, description
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ChangelogEntryRow(
    entry: com.greenicephoenix.traceledger.core.util.ChangelogEntry
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Icon in subtle tinted box
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    NothingRed.copy(alpha = 0.08f),
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = ChangelogIconMapper.get(entry.iconName),
                contentDescription = null,
                tint               = NothingRed,
                modifier           = Modifier.size(18.dp)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text  = entry.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text  = entry.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PrivacyCard — four promise badges in a 2x2 grid
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PrivacyCard() {
    Card(
        shape  = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier            = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text  = "Your data stays on your device. Always.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // 2x2 grid of privacy badges
            val badges = listOf(
                Triple(Icons.Default.WifiOff,      "Offline only",   "No internet required"),
                Triple(Icons.Default.VisibilityOff, "No tracking",    "Zero analytics"),
                Triple(Icons.Default.Block,         "No ads",         "Ever"),
                Triple(Icons.Default.Cloud,         "No cloud sync",  "Local storage only")
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                badges.chunked(2).forEach { row ->
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEach { (icon, title, subtitle) ->
                            PrivacyBadge(
                                icon     = icon,
                                title    = title,
                                subtitle = subtitle,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // If odd number, fill remaining space
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacyBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier              = modifier
            .background(
                SuccessGreen.copy(alpha = 0.06f),
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = SuccessGreen,
            modifier           = Modifier.size(18.dp)
        )
        Column {
            Text(
                text  = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text  = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SectionLabel — small uppercase section divider
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SectionLabel(text: String) {
    Text(
        text     = text,
        style    = MaterialTheme.typography.labelSmall,
        color    = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}