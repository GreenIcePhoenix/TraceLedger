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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.BuildConfig
import com.greenicephoenix.traceledger.core.ui.theme.NothingRed
import com.greenicephoenix.traceledger.core.ui.theme.SuccessGreen
import com.greenicephoenix.traceledger.core.util.AppLinks
import com.greenicephoenix.traceledger.core.util.ChangelogIconMapper
import com.greenicephoenix.traceledger.core.util.ChangelogParser
import com.greenicephoenix.traceledger.core.util.VersionChangelog
import com.greenicephoenix.traceledger.feature.update.UpdateDialog
import com.greenicephoenix.traceledger.feature.update.UpdateInfo
import com.greenicephoenix.traceledger.feature.update.checkForUpdate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * About screen.
 *
 * CHANGE: onPrivacyPolicy and onTerms callback parameters have been removed.
 * Privacy Policy and Terms of Use are now served from the website only
 * (traceledger.pages.dev/privacy and traceledger.pages.dev/terms).
 * Tapping either row opens the URL in the system browser via LocalUriHandler.
 * This eliminates the duplicate in-app copies and ensures the website version
 * is always the canonical, up-to-date source.
 *
 * CHANGE: Legal section icon tints are now both NothingRed (consistent).
 * Previously Privacy Policy was SuccessGreen and Terms was grey — inconsistent
 * with each other and with the rest of the screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit
    // onPrivacyPolicy and onTerms removed — URLs opened directly via uriHandler
) {
    val context        = LocalContext.current
    val uriHandler     = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()
    val versions       = remember { ChangelogParser.loadVersioned(context) }
    val current        = versions.firstOrNull { it.version == BuildConfig.VERSION_NAME }
    val previous       = versions.filter { it.version != BuildConfig.VERSION_NAME }

    // ── Update check state ────────────────────────────────────────────────────
    var checkingUpdate by remember { mutableStateOf(false) }
    var pendingUpdate  by remember { mutableStateOf<UpdateInfo?>(null) }
    var showUpToDate   by remember { mutableStateOf(false) }

    pendingUpdate?.let { update ->
        UpdateDialog(
            updateInfo = update,
            onDismiss  = { pendingUpdate = null }
        )
    }

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
            item { AppIdentityCard() }

            // ── CONNECT ───────────────────────────────────────────────────────
            item { SectionLabel("CONNECT") }

            item {
                ConnectCard {
                    LinkRow(
                        icon       = Icons.Default.Forum,
                        iconTint   = NothingRed,
                        title      = "Discord",
                        subtitle   = "Join the community, share feedback",
                        isExternal = true,
                        onClick    = { uriHandler.openUri(AppLinks.DISCORD) }
                    )
                    RowDivider()
                    LinkRow(
                        icon       = Icons.Default.Language,
                        iconTint   = MaterialTheme.colorScheme.primary,
                        title      = "Website",
                        subtitle   = "traceledger.pages.dev",
                        isExternal = true,
                        onClick    = { uriHandler.openUri(AppLinks.WEBSITE) }
                    )
                    RowDivider()
                    // ── Check for updates row ─────────────────────────────────
                    CheckForUpdatesRow(
                        isChecking = checkingUpdate,
                        isUpToDate = showUpToDate,
                        onClick    = {
                            if (checkingUpdate) return@CheckForUpdatesRow
                            checkingUpdate = true
                            showUpToDate   = false
                            coroutineScope.launch {
                                val result = withContext(Dispatchers.IO) { checkForUpdate() }
                                checkingUpdate = false
                                if (result != null) pendingUpdate = result
                                else showUpToDate = true
                            }
                        }
                    )
                }
            }

            // ── WHAT'S NEW ────────────────────────────────────────────────────
            if (current != null) {
                item { SectionLabel("WHAT'S NEW") }
                item {
                    VersionCard(
                        changelog         = current,
                        isCurrent         = true,
                        expandedByDefault = true
                    )
                }
            }

            // ── PREVIOUS VERSIONS ─────────────────────────────────────────────
            if (previous.isNotEmpty()) {
                item { SectionLabel("PREVIOUS VERSIONS") }
                items(previous) { changelog ->
                    VersionCard(
                        changelog         = changelog,
                        isCurrent         = false,
                        expandedByDefault = false
                    )
                }
            }

            // ── PRIVACY PROMISE ───────────────────────────────────────────────
            item { SectionLabel("PRIVACY") }
            item { PrivacyCard() }

            // ── LEGAL ─────────────────────────────────────────────────────────
            // Both rows open the canonical website pages in the system browser.
            // There are no longer separate in-app screens for these documents —
            // the website versions are always current and avoid duplication.
            item { SectionLabel("LEGAL") }

            item {
                ConnectCard {
                    LinkRow(
                        icon       = Icons.Default.PrivacyTip,
                        iconTint   = NothingRed,           // was SuccessGreen — now consistent
                        title      = "Privacy Policy",
                        subtitle   = "How your data is handled",
                        isExternal = true,                 // opens browser, not internal screen
                        onClick    = { uriHandler.openUri(AppLinks.PRIVACY_POLICY) }
                    )
                    RowDivider()
                    LinkRow(
                        icon       = Icons.Default.Gavel,
                        iconTint   = NothingRed,           // was grey — now consistent with Privacy row
                        title      = "Terms of Use",
                        subtitle   = "Rules for using TraceLedger",
                        isExternal = true,                 // opens browser, not internal screen
                        onClick    = { uriHandler.openUri(AppLinks.TERMS) }
                    )
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CheckForUpdatesRow
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CheckForUpdatesRow(
    isChecking: Boolean,
    isUpToDate: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isChecking) { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(NothingRed.copy(alpha = 0.10f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Default.SystemUpdate,
                contentDescription = null,
                tint               = NothingRed,
                modifier           = Modifier.size(18.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = "Check for updates",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text  = when {
                    isChecking -> "Checking\u2026"
                    isUpToDate -> "You\u2019re up to date  \u2713"
                    else       -> "v${BuildConfig.VERSION_NAME} installed"
                },
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    isUpToDate -> SuccessGreen
                    else       -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                }
            )
        }

        if (isChecking) {
            CircularProgressIndicator(
                modifier    = Modifier.size(16.dp),
                color       = NothingRed,
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector        = Icons.Default.ChevronRight,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                modifier           = Modifier.size(16.dp)
            )
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
        Column(
            modifier            = Modifier.fillMaxWidth().padding(24.dp),
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
                text      = "Version ${BuildConfig.VERSION_NAME}",
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
// ConnectCard
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ConnectCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape    = RoundedCornerShape(20.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(content = content)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LinkRow
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun LinkRow(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    isExternal: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(iconTint.copy(alpha = 0.10f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = iconTint,
                modifier           = Modifier.size(18.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text  = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            )
        }

        // External link icon for browser-opening rows, chevron for internal navigation
        Icon(
            imageVector        = if (isExternal) Icons.AutoMirrored.Filled.OpenInNew
            else Icons.Default.ChevronRight,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
            modifier           = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier  = Modifier.padding(start = 68.dp),
        thickness = 0.5.dp,
        color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// VersionCard
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun VersionCard(
    changelog: VersionChangelog,
    isCurrent: Boolean,
    expandedByDefault: Boolean
) {
    var expanded by remember { mutableStateOf(expandedByDefault) }

    Card(
        shape    = RoundedCornerShape(20.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
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
                    imageVector        = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier           = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    )
                    if (changelog.entries.isNotEmpty()) {
                        changelog.entries.forEach { entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                                verticalAlignment     = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(NothingRed.copy(alpha = 0.08f), RoundedCornerShape(10.dp)),
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
                                    Text(entry.title,       style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                    Text(entry.description, style = MaterialTheme.typography.bodySmall,  color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                                }
                            }
                        }
                    } else {
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
// PrivacyCard
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun PrivacyCard() {
    Card(
        shape    = RoundedCornerShape(20.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
            val badges = listOf(
                Triple(Icons.Default.WifiOff,        "Offline only",  "No internet required"),
                Triple(Icons.Default.VisibilityOff,  "No tracking",   "Zero analytics"),
                Triple(Icons.Default.Block,           "No ads",        "Ever"),
                Triple(Icons.Default.Cloud,           "No cloud sync", "Local storage only")
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                badges.chunked(2).forEach { row ->
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEach { (icon, title, subtitle) ->
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(SuccessGreen.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
                                    .padding(12.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(icon, null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                                Column {
                                    Text(title,    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                                    Text(subtitle, style = MaterialTheme.typography.labelSmall,  color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
                                }
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text     = text,
        style    = MaterialTheme.typography.labelSmall,
        color    = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}