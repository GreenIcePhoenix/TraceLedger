package com.greenicephoenix.traceledger.feature.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.core.ui.theme.NothingRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text  = "PRIVACY POLICY",
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
            contentPadding      = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                PolicySection(
                    title = "Last updated: March 2026",
                    body  = "TraceLedger is a privacy-first application. This policy explains how your data is handled — the short answer is: it isn't, because we never collect it."
                )
            }

            item {
                PolicySection(
                    title = "1. Data Collection",
                    body  = "TraceLedger does not collect, transmit, or store any personal data on external servers. All information you enter — accounts, transactions, categories, budgets — is stored exclusively in a local SQLite database on your device.\n\nWe collect nothing. There are no analytics, no crash reporting, no usage tracking, and no telemetry of any kind."
                )
            }

            item {
                PolicySection(
                    title = "2. Internet Access",
                    body  = "TraceLedger does not request the INTERNET permission. The app is entirely offline. No network requests are made at any time during normal use of the app.\n\nLinks to external pages (Discord, Website, GitHub) open in your device's browser — outside of the app — using Android's standard intent system."
                )
            }

            item {
                PolicySection(
                    title = "3. Data Storage",
                    body  = "All data is stored locally on your device using Room (SQLite). The database is stored in the app's private storage area and is not accessible to other apps.\n\nIf you uninstall TraceLedger, all data is permanently deleted from your device."
                )
            }

            item {
                PolicySection(
                    title = "4. Backups",
                    body  = "TraceLedger supports manual JSON and CSV export. These files are created only when you explicitly request an export and are saved to a location of your choosing on your device.\n\nAndroid's Auto Backup may include app data in your Google account backup depending on your device settings. You can disable this in your Android backup settings."
                )
            }

            item {
                PolicySection(
                    title = "5. Third-Party Services",
                    body  = "TraceLedger does not integrate with any third-party services, SDKs, or APIs. There are no advertising networks, analytics platforms, or social media integrations within the app."
                )
            }

            item {
                PolicySection(
                    title = "6. Children",
                    body  = "TraceLedger does not knowingly collect any information from anyone, including children under 13. Since we collect no data at all, this app complies with COPPA by design."
                )
            }

            item {
                PolicySection(
                    title = "7. Changes to This Policy",
                    body  = "If this policy changes, the updated version will be included in the app update and noted in the changelog. The effective date at the top of this document will be updated accordingly."
                )
            }

            item {
                PolicySection(
                    title = "8. Contact",
                    body  = "If you have questions about this privacy policy, open an issue on GitHub or reach out on our Discord server."
                )
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun PolicySection(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text  = title,
            style = MaterialTheme.typography.titleSmall,
            color = NothingRed
        )
        Text(
            text  = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            lineHeight = MaterialTheme.typography.bodyMedium.fontSize * 1.7
        )
    }
}