package com.greenicephoenix.traceledger.core.util

import android.content.Context
import android.provider.Settings

/**
 * Returns true when the user has enabled "Remove animations" in
 * Developer Options (ANIMATOR_DURATION_SCALE == 0).
 * Charts call this to snapTo(1f) instead of animateTo(1f).
 */
fun isReducedMotion(context: Context): Boolean {
    val scale = Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f
    )
    return scale == 0f
}

/**
 * Returns `full` duration in ms normally, or 0 if reduced motion is enabled.
 * Usage: tween(animDuration(context, 700))
 */
fun animDuration(context: Context, full: Int): Int =
    if (isReducedMotion(context)) 0 else full