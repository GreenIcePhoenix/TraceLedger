package com.greenicephoenix.traceledger.feature.about

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.core.ui.theme.NothingRed
import com.greenicephoenix.traceledger.core.util.ChangelogEntry
import com.greenicephoenix.traceledger.core.util.ChangelogIconMapper
import com.greenicephoenix.traceledger.core.util.VersionChangelog

/**
 * The "What's New" bottom sheet shown on first launch after an update.
 *
 * Extracted from MainActivity to keep that file lean.
 * Call this from MainActivity when showChangelogSheet is true.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewSheet(
    changelog: VersionChangelog,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest  = onDismiss,
        containerColor    = MaterialTheme.colorScheme.surface,
        shape             = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            AnimatedVisibility(
                visible = true,
                enter   = fadeIn() + slideInVertically { it / 3 }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {

                    // ── Header ────────────────────────────────────────────────
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text  = "WHAT'S NEW",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = NothingRed.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text     = "v${changelog.version}",
                                style    = MaterialTheme.typography.labelSmall,
                                color    = NothingRed,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text  = "Here's what's included in this release.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )

                    Spacer(Modifier.height(24.dp))

                    // ── Feature entries ───────────────────────────────────────
                    if (changelog.entries.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            changelog.entries.forEachIndexed { index, entry ->
                                WhatsNewEntryRow(entry = entry)
                                if (index < changelog.entries.lastIndex) {
                                    HorizontalDivider(
                                        modifier  = Modifier.padding(start = 52.dp),
                                        thickness = 0.5.dp,
                                        color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                                    )
                                }
                            }
                        }
                    } else {
                        // Fallback for plain-text changelog entries
                        Text(
                            text  = changelog.rawText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    Spacer(Modifier.height(28.dp))

                    // ── CTA ───────────────────────────────────────────────────
                    Button(
                        onClick  = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = NothingRed
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text  = "Got it",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// WhatsNewEntryRow — one feature line in the sheet
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun WhatsNewEntryRow(entry: ChangelogEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(
                    NothingRed.copy(alpha = 0.10f),
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

        // Text
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text  = entry.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text  = entry.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}