package com.greenicephoenix.traceledger.feature.update

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.greenicephoenix.traceledger.core.ui.theme.NothingRed
import androidx.core.net.toUri

/**
 * Modal dialog shown when [UpdateInfo] is non-null (a newer version exists on GitHub).
 *
 * Shows:
 *   - New version number
 *   - Download size
 *   - Release notes (scrollable)
 *   - "Download & Install" button — uses DownloadManager (system service)
 *   - "Later" button — dismisses without downloading
 *
 * Download flow:
 *   1. DownloadManager queues the APK download in the background
 *   2. System notification shows download progress
 *   3. When complete, the install intent is triggered via a BroadcastReceiver
 *      registered in MainActivity (see Batch 3 wiring)
 *
 * PLAY STORE NOTE: This dialog should never appear in Play Store builds because
 * UpdateChecker returns null when IS_PLAY_STORE_BUILD = true.
 */
@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        shape            = RoundedCornerShape(20.dp),
        containerColor   = MaterialTheme.colorScheme.surface,
        title = {
            Column {
                Text(
                    text  = "UPDATE AVAILABLE",
                    style = MaterialTheme.typography.headlineMedium,
                    color = NothingRed
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = "v${updateInfo.version}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text  = "Download size: ${formatBytes(updateInfo.apkSizeBytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
        },
        text = {
            Column {
                Text(
                    text  = "WHAT'S NEW",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                )
                Spacer(Modifier.height(8.dp))
                // Release notes in a scrollable box — they can be long
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .background(
                            MaterialTheme.colorScheme.background,
                            RoundedCornerShape(10.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text     = updateInfo.releaseNotes,
                        style    = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace   // matches the app's Nothing aesthetic
                        ),
                        color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    startDownload(context, updateInfo)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = NothingRed),
                shape  = RoundedCornerShape(12.dp)
            ) {
                Text("Download & Install", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }
    )
}

/**
 * Enqueues the APK download via Android's DownloadManager.
 *
 * Why DownloadManager?
 * - No custom download code needed
 * - Shows a system notification with progress automatically
 * - Handles retries on network failure
 * - Saves to the public Downloads folder so FileProvider can serve it for install
 */
private fun startDownload(context: Context, updateInfo: UpdateInfo) {
    val fileName = "TraceLedger-${updateInfo.version}.apk"

    val request = DownloadManager.Request(updateInfo.apkUrl.toUri())
        .setTitle("TraceLedger Update")
        .setDescription("Downloading v${updateInfo.version}")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        .setAllowedOverMetered(true)
        .setAllowedOverRoaming(false)

    val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    manager.enqueue(request)
    // The install prompt is triggered from MainActivity when download completes
    // (see Batch 3 — MainActivity.kt wiring)
}

/** Formats byte count as human-readable string: 7,340,032 → "7.0 MB" */
private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1_048_576 -> String.format("%.1f MB", bytes / 1_048_576.0)
        bytes >= 1_024     -> String.format("%.0f KB", bytes / 1_024.0)
        else               -> "$bytes B"
    }
}