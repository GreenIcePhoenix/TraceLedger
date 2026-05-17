package com.greenicephoenix.traceledger.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * GreenIcePhoenix Ecosystem Color Palette — TraceLedger
 *
 * Design Philosophy:
 * - THREE distinct themes: Light, Dark (Void), Extra Dark (OLED)
 * - Primary accent: Sovereign Violet (#7C4DFF)
 * - No green as primary (avoids finance bias)
 * - Strong surface hierarchy (fixes dark vs ultra-dark issue)
 * - Material3-compatible token structure
 */

// ─────────────────────────────────────────────────────────────
// BASE COLORS
// ─────────────────────────────────────────────────────────────
val BlackPure = Color(0xFF000000)
val WhitePure = Color(0xFFFFFFFF)

// ─────────────────────────────────────────────────────────────
// PRIMARY ACCENT — SOVEREIGN VIOLET
// ─────────────────────────────────────────────────────────────
val SovereignViolet = Color(0xFF7C4DFF)        // Dark / Ultra Dark usage
val SovereignVioletDark = Color(0xFF5A35CC)    // Light mode (WCAG safe)

// Containers (Material-style usage)
val SovereignVioletContainer = Color(0xFFEDE7FF)
val SovereignVioletOnContainer = Color(0xFF1A1033)
val SovereignVioletDarkContainer = Color(0xFF2A1F55)

// ─────────────────────────────────────────────────────────────
// DARK MODE (Balanced — everyday usage)
// ─────────────────────────────────────────────────────────────
val DarkBackground       = Color(0xFF0F0F18)
val DarkSurface          = Color(0xFF16162A)
val DarkSurfaceVariant   = Color(0xFF1E1E32)

val DarkBorder           = Color(0xFF2C2C42)
val DarkBorderVariant    = Color(0xFF3A3A52)

// ─────────────────────────────────────────────────────────────
// EXTRA DARK MODE (True OLED)
// ─────────────────────────────────────────────────────────────
val UltraDarkBackground   = Color(0xFF000000)
val UltraDarkSurface      = Color(0xFF0A0A0F)
val UltraDarkSecondary    = Color(0xFF141420)

val UltraDarkBorder       = Color(0xFF1F1F2E)
val UltraDarkBorderVariant= Color(0xFF2A2A3A)

// ─────────────────────────────────────────────────────────────
// LIGHT MODE (Neutral, no tint)
// ─────────────────────────────────────────────────────────────
val LightBackground      = Color(0xFFFAFAFA)
val LightSurface         = Color(0xFFFFFFFF)
val LightSurfaceVariant  = Color(0xFFF1F1F5)

val LightBorder          = Color(0xFFE0E0E0)
val LightBorderVariant   = Color(0xFFD0D0D8)

// ─────────────────────────────────────────────────────────────
// TEXT COLORS
// ─────────────────────────────────────────────────────────────

// Dark / Ultra Dark
val TextPrimaryDark      = Color(0xFFFFFFFF)
val TextSecondaryDark    = Color(0xFFB0B0C8)
val TextTertiaryDark     = Color(0xFF8080A0)
val TextDisabledDark     = Color(0xFF5A5A78)

// Light
val LightTextPrimary     = Color(0xFF0F0F0F)
val LightTextSecondary   = Color(0xFF5F5F5F)
val LightTextTertiary    = Color(0xFF909090)
val LightTextDisabled    = Color(0xFFB0B0B0)

// ─────────────────────────────────────────────────────────────
// SEMANTIC COLORS (STRICT USAGE)
// ─────────────────────────────────────────────────────────────

// Income / success (allowed to remain green as semantic only)
val SuccessGreen = Color(0xFF27AE60)

// Errors & destructive actions ONLY
val ErrorRed = Color(0xFFE53935)

// Budget warning / caution
val WarningAmber = Color(0xFFF59E0B)

// ─────────────────────────────────────────────────────────────
// SPECIAL UI TOKENS
// ─────────────────────────────────────────────────────────────

// Glassmorphism background (matches GIP style)
val GlassMorphBg = Color(0x330F0F18)

// Subtle overlay (grain / texture simulation)
val DotMatrixOverlay = Color(0x0AFFFFFF)

// Tag / category accents (non-primary usage)
val TagBlue    = Color(0xFF0D2A40)
val TagPurple  = Color(0xFF2A1A40)
val TagOrange  = Color(0xFF5F3A1E)
val TagPink    = Color(0xFF5F1E3A)

// ─────────────────────────────────────────────────────────────
// BACKWARD COMPATIBILITY
// ─────────────────────────────────────────────────────────────

// Keep old references compiling if needed
val NothingRed = ErrorRed

// These tokens were renamed in the GIP theme update.
// They are kept so existing feature files compile without modification.
// DO NOT use these in new code — use the named tokens above instead.
//
//   Old name       →  New name
//   Grey850        →  DarkSurface
//   TextSecondary  →  TextSecondaryDark

val Grey850       = DarkSurface        // was Color(0xFF141414), now Color(0xFF16162A)
val TextSecondary = TextSecondaryDark  // was Color(0xFFB3B3B3), now Color(0xFFB0B0C8)

// ─────────────────────────────────────────────────────────────
// BANNER COLORS (WCAG-safe)
// ─────────────────────────────────────────────────────────────

// Warning banners
val WarningBannerLight = Color(0xFFFFF3E0)
val WarningBannerDark  = Color(0xFF4A3310)

// Error banners
val ErrorBannerLight   = Color(0xFFFFEBEE)
val ErrorBannerDark    = Color(0xFF4A1F1F)