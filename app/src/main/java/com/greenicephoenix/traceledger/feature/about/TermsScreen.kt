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
fun TermsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text  = "TERMS OF USE",
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
                    body  = "By installing and using TraceLedger, you agree to these terms. Please read them — they're short and written in plain language."
                )
            }

            item {
                PolicySection(
                    title = "1. Acceptance",
                    body  = "These Terms of Use govern your use of the TraceLedger Android application. If you do not agree with any part of these terms, please uninstall the app."
                )
            }

            item {
                PolicySection(
                    title = "2. Intended Use",
                    body  = "TraceLedger is a personal finance management tool intended for personal, non-commercial use. You may use the app to track your own income, expenses, accounts, and budgets.\n\nYou may not use the app for illegal purposes or to store data belonging to others without their consent."
                )
            }

            item {
                PolicySection(
                    title = "3. Your Data",
                    body  = "All data you enter into TraceLedger is stored locally on your device. You are solely responsible for the accuracy of the data you enter and for maintaining your own backups.\n\nWe strongly recommend using the JSON export feature regularly to back up your data. We are not liable for data loss caused by uninstalling the app, device failure, or any other circumstance."
                )
            }

            item {
                PolicySection(
                    title = "4. Financial Disclaimer",
                    body  = "TraceLedger is a personal tracking tool only. It does not provide financial advice, investment recommendations, or tax guidance.\n\nThe figures shown in the app reflect only the data you have entered. Always verify your balances with your actual bank statements. TraceLedger is not a substitute for professional financial advice."
                )
            }

            item {
                PolicySection(
                    title = "5. Open Source",
                    body  = "TraceLedger is open-source software. The source code is available on GitHub. You may review, fork, and contribute to the codebase in accordance with the project's open-source licence.\n\nYou may not redistribute modified versions under the TraceLedger name without permission."
                )
            }

            item {
                PolicySection(
                    title = "6. No Warranty",
                    body  = "TraceLedger is provided \"as is\" without warranty of any kind, express or implied. We make no guarantees regarding uptime, accuracy, fitness for a particular purpose, or freedom from bugs.\n\nUse the app at your own discretion."
                )
            }

            item {
                PolicySection(
                    title = "7. Limitation of Liability",
                    body  = "To the maximum extent permitted by law, the developers of TraceLedger shall not be liable for any indirect, incidental, special, or consequential damages arising from your use of the app, including but not limited to data loss or financial decisions made based on app data."
                )
            }

            item {
                PolicySection(
                    title = "8. Updates to These Terms",
                    body  = "These terms may be updated with new app versions. The effective date at the top will reflect the latest revision. Continued use of the app after an update constitutes acceptance of the revised terms."
                )
            }

            item {
                PolicySection(
                    title = "9. Contact",
                    body  = "For questions about these terms, open an issue on GitHub or reach us on Discord."
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