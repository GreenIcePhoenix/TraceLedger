package com.greenicephoenix.traceledger.feature.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.BuildConfig
import com.greenicephoenix.traceledger.R
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.greenicephoenix.traceledger.core.util.ChangelogParser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit
) {

    val context = LocalContext.current

    val versionName = BuildConfig.VERSION_NAME
    var showPrevious by remember { mutableStateOf(false) }

    val allChangelogs = remember {
        ChangelogParser.load(context)
    }

    val sortedVersions = allChangelogs.keys.sortedDescending()

    val changelogText = allChangelogs[versionName]
        ?: "No changelog available for this version."

    val previousVersions = sortedVersions.filter { it != versionName }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About", color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Card(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 4.dp
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "TraceLedger",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Text(
                        text = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )

                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
                    )

                    Text(
                        text = "What's New",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Text(
                        text = changelogText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )

                    if (previousVersions.isNotEmpty()) {

                        TextButton(
                            onClick = { showPrevious = !showPrevious }
                        ) {
                            Text(
                                text = if (showPrevious) "Hide Previous Versions"
                                else "View Previous Versions"
                            )
                        }

                        if (showPrevious) {
                            previousVersions.forEach { version ->

                                HorizontalDivider(
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
                                )

                                Text(
                                    text = "Version $version",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onBackground
                                )

                                Text(
                                    text = allChangelogs[version] ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
                    )

                    Text(
                        text = "Privacy",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Text(
                        text = "• Offline-only\n• No analytics\n• No ads\n• No cloud sync",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

private fun loadChangelog(context: android.content.Context): Map<String, String> {
    return try {
        val fullText = context.resources
            .openRawResource(R.raw.changelog)
            .bufferedReader()
            .use { it.readText() }

        val sections = fullText.split("# ")
            .filter { it.isNotBlank() }

        sections.associate { section ->
            val lines = section.lines()
            val version = lines.first().trim()
            val content = lines.drop(1).joinToString("\n").trim()
            version to content
        }

    } catch (e: Exception) {
        emptyMap()
    }
}
