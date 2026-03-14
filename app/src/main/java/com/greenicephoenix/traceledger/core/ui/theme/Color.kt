package com.greenicephoenix.traceledger.core.ui.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// TraceLedger Color System — inspired by Nothing OS
//
// Rules:
//   - Pure blacks and deep greys for surfaces
//   - High contrast whites for text
//   - One red accent (NothingRed) for interactive elements
//   - SuccessGreen for income / positive values only
//   - No other accent colors at the theme level
//
// For category/account colors, see CategoryColors.kt and AccountColors.kt.
// Those are data-driven and not part of the theme system.
// ─────────────────────────────────────────────────────────────────────────────

// ── Base palette ──────────────────────────────────────────────────────────────
val BlackPure = Color(0xFF000000)
val WhitePure = Color(0xFFFFFFFF)

// ── Surface greys (dark theme) ────────────────────────────────────────────────
val Grey900 = Color(0xFF0D0D0D)
val Grey850 = Color(0xFF141414)
val Grey800 = Color(0xFF1A1A1A)
val Grey700 = Color(0xFF2A2A2A)
val Grey600 = Color(0xFF3A3A3A)

// ── Text (dark theme) ─────────────────────────────────────────────────────────
val TextPrimary = WhitePure
val TextSecondary = Color(0xFFB3B3B3)
val TextDisabled = Color(0xFF6F6F6F)

// ── Accent — Nothing Red ──────────────────────────────────────────────────────
// Used for: primary buttons, FAB, destructive actions, expense amounts
val NothingRed = Color(0xFFE53935)

// ── Status colors ─────────────────────────────────────────────────────────────
// FIX: SuccessGreen was defined here but many files were using Color(0xFF4CAF50)
// hardcoded instead. Import this constant — never hardcode the hex inline.
val SuccessGreen = Color(0xFF4CAF50)  // Income amounts, positive net balance
val ErrorRed = NothingRed             // Aliases NothingRed for semantic clarity

// ── Light theme surfaces ──────────────────────────────────────────────────────
val LightBackground = Color(0xFFF7F7F7)
val LightSurface    = Color(0xFFFFFFFF)
val LightSurfaceAlt = Color(0xFFEFEFEF)

// ── Text (light theme) ────────────────────────────────────────────────────────
val LightTextPrimary   = Color(0xFF0F0F0F)
val LightTextSecondary = Color(0xFF5F5F5F)
val LightTextDisabled  = Color(0xFF9E9E9E)