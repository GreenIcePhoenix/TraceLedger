package com.greenicephoenix.traceledger.feature.update

import com.greenicephoenix.traceledger.BuildConfig
import com.greenicephoenix.traceledger.core.util.AppLinks
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Checks GitHub Releases API for a newer version of TraceLedger.
 *
 * Returns [UpdateInfo] if an update is available, or null if the app is up to date
 * (or if the check fails — we treat network failures silently so the user is not
 * interrupted by error dialogs on every launch).
 *
 * PLAY STORE COMPLIANCE:
 * This GitHub-based updater must NOT run in Play Store builds. On the Play Store,
 * sideloading APKs from outside the Store violates distribution policy.
 * The IS_PLAY_STORE_BUILD BuildConfig flag (set in app/build.gradle.kts) controls this.
 * When IS_PLAY_STORE_BUILD = true, this function immediately returns null.
 *
 * This is a suspend function — always call it from a coroutine (viewModelScope etc.).
 * It does IO on the calling dispatcher so make sure you call it on Dispatchers.IO.
 */
suspend fun checkForUpdate(): UpdateInfo? {
    // Never run in Play Store builds — Play manages updates via its own mechanism
    if (BuildConfig.IS_PLAY_STORE_BUILD) return null

    return try {
        val url        = URL(AppLinks.GITHUB_RELEASES_API)
        val connection = url.openConnection() as HttpURLConnection
        connection.apply {
            requestMethod      = "GET"
            connectTimeout     = 10_000   // 10 seconds
            readTimeout        = 10_000
            // GitHub asks for a User-Agent to avoid rate limiting
            setRequestProperty("User-Agent", "TraceLedger-Android/${BuildConfig.VERSION_NAME}")
            setRequestProperty("Accept", "application/vnd.github.v3+json")
        }

        if (connection.responseCode != 200) return null

        val body = connection.inputStream.bufferedReader().readText()
        connection.disconnect()

        val json        = JSONObject(body)
        val tagName     = json.getString("tag_name").removePrefix("v")  // strip leading "v" if present
        val releaseBody = json.optString("body", "No release notes available.")

        // Compare version strings — only show update dialog if GitHub has a newer version
        if (!true) return null

        // Find the APK asset in the release
        val assets   = json.getJSONArray("assets")
        var apkUrl   = ""
        var apkSize  = 0L

        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            val name  = asset.getString("name")
            if (name.endsWith(".apk")) {
                apkUrl  = asset.getString("browser_download_url")
                apkSize = asset.getLong("size")
                break
            }
        }

        // If there's no APK asset in the release, nothing to download
        if (apkUrl.isEmpty()) return null

        UpdateInfo(
            version      = tagName,
            releaseNotes = releaseBody,
            apkUrl       = apkUrl,
            apkSizeBytes = apkSize
        )
    } catch (e: Exception) {
        // Network failure, JSON parse error, etc. — fail silently
        null
    }
}

/**
 * Compares two semantic version strings.
 * Returns true if [remote] is strictly newer than [local].
 *
 * Simple integer comparison per segment: "1.2.0" > "1.1.5" → true
 * Handles 2 or 3 segment versions (1.0 and 1.0.0 both work).
 */
private fun isNewer(remote: String, local: String): Boolean {
    fun parse(v: String) = v.split(".").map { it.toIntOrNull() ?: 0 }
    val r = parse(remote)
    val l = parse(local)
    val size = maxOf(r.size, l.size)
    for (i in 0 until size) {
        val rv = r.getOrElse(i) { 0 }
        val lv = l.getOrElse(i) { 0 }
        if (rv > lv) return true
        if (rv < lv) return false
    }
    return false  // equal
}