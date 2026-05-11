package com.greenicephoenix.traceledger.feature.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.BuildConfig
import com.greenicephoenix.traceledger.R
import com.greenicephoenix.traceledger.core.ui.theme.SuccessGreen

/**
 * AboutScreen — simplified.
 *
 * Contains:
 *   - App identity card (logo, version, tagline)
 *   - Privacy promises card
 *   - Legal links (Privacy Policy, Terms of Use)
 *
 * What moved out:
 *   - Connect section (Discord, Website) → Settings → APP
 *   - Check for Updates → Settings → SYSTEM
 *   - What's New / Changelog → Settings → SYSTEM → ChangelogScreen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current

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
            contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── App identity ──────────────────────────────────────────────────
            item { AppIdentityCard() }

            // ── Privacy promises ──────────────────────────────────────────────
            item { SectionLabel("PRIVACY") }
            item { PrivacyCard() }

            // ── Legal links ───────────────────────────────────────────────────
            item { SectionLabel("LEGAL") }
            item {
                Card(
                    shape    = RoundedCornerShape(20.dp),
                    colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        LegalLinkRow(
                            icon     = Icons.Default.PrivacyTip,
                            title    = "Privacy Policy",
                            subtitle = "How we handle your data",
                            onClick  = { uriHandler.openUri("https://traceledger.pages.dev/privacy.html") }
                        )
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            modifier  = Modifier.padding(horizontal = 16.dp)
                        )
                        LegalLinkRow(
                            icon     = Icons.Default.Gavel,
                            title    = "Terms of Use",
                            subtitle = "Usage terms and conditions",
                            onClick  = { uriHandler.openUri("https://traceledger.pages.dev/terms.html") }
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AppIdentityCard
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AppIdentityCard() {
    Card(
        shape    = RoundedCornerShape(20.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // App icon
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter            = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = "TraceLedger icon",
                    modifier           = Modifier.size(56.dp)
                )
            }

            // App name + version
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text      = "TraceLedger",
                    style     = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color     = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text      = "v${BuildConfig.VERSION_NAME}",
                    style     = MaterialTheme.typography.bodySmall,
                    color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    textAlign = TextAlign.Center
                )
            }

            // Tagline
            Text(
                text      = "Privacy-first personal finance. No cloud. No ads. Always yours.",
                style     = MaterialTheme.typography.bodySmall,
                color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PrivacyCard — 2×2 badge grid
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
                Triple(Icons.Default.WifiOff,       "Offline only",  "No internet required"),
                Triple(Icons.Default.VisibilityOff, "No tracking",   "Zero analytics"),
                Triple(Icons.Default.Block,          "No ads",        "Ever"),
                Triple(Icons.Default.Cloud,          "No cloud sync", "Local storage only")
            )

            // 2-column grid
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
                                    .background(
                                        SuccessGreen.copy(alpha = 0.06f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(12.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector        = icon,
                                    contentDescription = null,
                                    tint               = MaterialTheme.colorScheme.primary,
                                    modifier           = Modifier.size(16.dp)
                                )
                                Column {
                                    Text(title,    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                                    Text(subtitle, style = MaterialTheme.typography.labelSmall,  color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
                                }
                            }
                        }
                        // Pad to fill row if odd number
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LegalLinkRow — opens browser on tap
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LegalLinkRow(
    icon     : ImageVector,
    title    : String,
    subtitle : String,
    onClick  : () -> Unit
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.primary,
            modifier           = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title,    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.labelSmall,  color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
        }
        Icon(
            imageVector        = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
            modifier           = Modifier.size(15.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SectionLabel
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