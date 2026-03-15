package com.greenicephoenix.traceledger.core.util

/**
 * Single source of truth for all external URLs and payment identifiers.
 *
 * ⚠️  Before publishing v1.1.0:
 *   - Replace UPI_ID with your real UPI address (e.g. yourname@upi)
 *   - Replace PAYPAL_ME with your PayPal.me URL
 */
object AppLinks {
    const val WEBSITE        = "https://traceledger.pages.dev"
    const val DISCORD        = "https://discord.gg/MYFfxqvKCQ"
    const val GITHUB         = "https://github.com/NRoy9/TraceLedger"
    const val PRIVACY_POLICY = "https://traceledger.pages.dev/privacy"
    const val TERMS          = "https://traceledger.pages.dev/terms"

    // ── GitHub release API — used by UpdateChecker ────────────────────────────
    const val GITHUB_RELEASES_API =
        "https://api.github.com/repos/NRoy9/TraceLedger/releases/latest"

    // ── Tip Jar — replace with your real IDs before publishing ───────────────
    /** Your UPI VPA (Virtual Payment Address). e.g. "yourname@upi" */
    const val UPI_ID  = "greenicephoenix@axisb"

    /** Your PayPal.me profile URL. e.g. "https://paypal.me/yourprofile" */
    const val PAYPAL_ME = "paypal.me/GreenIcePhoenix"
}