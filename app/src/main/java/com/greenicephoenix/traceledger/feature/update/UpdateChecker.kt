package com.greenicephoenix.traceledger.feature.update

import com.greenicephoenix.traceledger.BuildConfig
import com.greenicephoenix.traceledger.core.util.AppLinks
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Checks for a newer version of TraceLedger by fetching a static JSON file
 * hosted on Cloudflare Pages.
 *
 * ── Why Cloudflare instead of GitHub API? ────────────────────────────────────
 * The GitHub Releases API returns 404 for private repositories. Since the
 * TraceLedger repo is private, we host a hand-maintained release.json on
 * Cloudflare Pages instead. We control the file entirely — no API tokens,
 * no rate limits, no private repo restrictions.
 *
 * ── release.json schema (at AppLinks.UPDATE_API) ─────────────────────────────
 * {
 *   "version":      "1.1.0",          ← semantic version, no "v" prefix
 *   "releaseNotes": "What changed.",  ← plain text or light markdown
 *   "apkUrl":       "https://traceledger.pages.dev/TraceLedger-1.1.0.apk",
 *   "apkSizeBytes": 12345678          ← exact byte count of the APK file
 * }
 *
 * ── Return values ─────────────────────────────────────────────────────────────
 * Returns [UpdateInfo] when a newer version is available.
 * Returns null when:
 *   - IS_PLAY_STORE_BUILD is true (Play Store manages its own updates)
 *   - The app is already up to date
 *   - Any network or parse error (fails silently — we never show error dialogs)
 *
 * ── Threading ─────────────────────────────────────────────────────────────────
 * This is a suspend function. Call it on Dispatchers.IO (e.g. withContext).
 */
fun checkForUpdate(): UpdateInfo? {
    // Never run in Play Store builds — Play manages updates via its own mechanism
    if (BuildConfig.IS_PLAY_STORE_BUILD) return null

    return try {
        val url        = URL(AppLinks.UPDATE_API)
        val connection = url.openConnection() as HttpURLConnection
        connection.apply {
            requestMethod  = "GET"
            connectTimeout = 10_000   // 10 seconds
            readTimeout    = 10_000
            // Identify ourselves so Cloudflare CDN can log the user agent
            setRequestProperty("User-Agent", "TraceLedger-Android/${BuildConfig.VERSION_NAME}")
            setRequestProperty("Accept",     "application/json")
        }

        if (connection.responseCode != 200) return null

        val body = connection.inputStream.bufferedReader().readText()
        connection.disconnect()

        // ── Parse the flat Cloudflare JSON ────────────────────────────────────
        // The schema is intentionally simple — four fields, no nesting.
        val json        = JSONObject(body)
        val version     = json.getString("version")        // e.g. "1.1.0"
        val releaseBody = json.optString(
            "releaseNotes",
            "No release notes available."
        )
        val apkUrl      = json.optString("apkUrl",  "")
        val apkSize     = json.optLong("apkSizeBytes", 0L)

        // Only show the update dialog if the remote version is strictly newer
        if (!isNewer(remote = version, local = BuildConfig.VERSION_NAME)) return null

        // Need a valid APK URL to be useful
        if (apkUrl.isBlank()) return null

        UpdateInfo(
            version      = version,
            releaseNotes = releaseBody,
            apkUrl       = apkUrl,
            apkSizeBytes = apkSize
        )

    } catch (e: Exception) {
        // Network failure, JSON parse error, malformed URL, etc. — fail silently
        null
    }
}

/**
 * Compares two semantic version strings (no "v" prefix).
 * Returns true if [remote] is strictly newer than [local].
 *
 * Examples:
 *   isNewer("1.2.0", "1.1.0") → true
 *   isNewer("1.1.0", "1.1.0") → false
 *   isNewer("1.0",   "1.0.0") → false
 *
 * Handles 2- or 3-segment versions. Non-numeric segments treated as 0.
 */
private fun isNewer(remote: String, local: String): Boolean {
    fun parse(v: String) = v.trim().split(".").map { it.toIntOrNull() ?: 0 }

    val r    = parse(remote)
    val l    = parse(local)
    val size = maxOf(r.size, l.size)

    for (i in 0 until size) {
        val rv = r.getOrElse(i) { 0 }
        val lv = l.getOrElse(i) { 0 }
        if (rv > lv) return true
        if (rv < lv) return false
    }
    return false  // versions are equal
}