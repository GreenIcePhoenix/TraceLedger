package com.greenicephoenix.traceledger.feature.update

/**
 * Holds information about a newer release fetched from GitHub.
 *
 * [version]        — the tag name from GitHub e.g. "1.1.0"
 * [releaseNotes]   — the release body (markdown), truncated in the dialog
 * [apkUrl]         — direct download URL for the APK asset
 * [apkSizeBytes]   — download size, shown to the user before they confirm download
 */
data class UpdateInfo(
    val version: String,
    val releaseNotes: String,
    val apkUrl: String,
    val apkSizeBytes: Long
)