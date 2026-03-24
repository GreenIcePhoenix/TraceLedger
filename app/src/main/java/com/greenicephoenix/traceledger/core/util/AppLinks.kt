package com.greenicephoenix.traceledger.core.util

/**
 * Single source of truth for all external URLs and payment identifiers.
 *
 * INFRASTRUCTURE NOTE (v1.1.0 → v1.2.0):
 *   The update checker previously called api.github.com/repos/.../releases/latest.
 *   That API is unavailable for private repositories.
 *   We now serve a static release.json from Cloudflare Pages instead.
 *   The file lives at: https://traceledger.pages.dev/release.json
 *
 *   release.json schema:
 *   {
 *     "version":      "1.1.0",
 *     "releaseNotes": "...",
 *     "apkUrl":       "https://traceledger.pages.dev/TraceLedger-1.1.0.apk",
 *     "apkSizeBytes": 12345678
 *   }
 *
 *   To publish a new release:
 *     1. Build the signed APK.
 *     2. Update release.json with the new version, notes, filename, and size.
 *     3. Upload both the APK and release.json via Cloudflare Pages → Direct Upload.
 *     Done. No GitHub API involved.
 */
object AppLinks {
    const val WEBSITE        = "https://traceledger.pages.dev"
    const val DISCORD        = "https://discord.gg/MYFfxqvKCQ"
    const val GITHUB         = "https://github.com/NRoy9/TraceLedger"
    const val PRIVACY_POLICY = "https://traceledger.pages.dev/privacy"
    const val TERMS          = "https://traceledger.pages.dev/terms"

    // ── Update checker — Cloudflare static JSON ───────────────────────────────
    // This replaced the GitHub releases API when the repo went private.
    // Update this file on Cloudflare whenever a new version is released.
    const val UPDATE_API = "https://traceledger.pages.dev/release.json"

    // ── Tip Jar ───────────────────────────────────────────────────────────────
    /** Your UPI VPA (Virtual Payment Address). e.g. "yourname@upi" */
    const val UPI_ID    = "greenicephoenix@axisb"

    /** Your PayPal.me profile URL. */
    const val PAYPAL_ME = "https://paypal.me/GreenIcePhoenix"
}