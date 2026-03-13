package com.greenicephoenix.traceledger.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.greenicephoenix.traceledger.R

// ─────────────────────────────────────────────────────────────────────────────
// Font setup
//
// DotMatrixFont — inspired by Nothing OS dot-matrix aesthetic.
// This font is used for screen headers, key financial figures, and display text.
// The font file lives at: res/font/dot_matrix.ttf
//
// Licensing note: ensure dot_matrix.ttf in res/font/ is either:
//   - OFL (SIL Open Font License) licensed, or
//   - A custom font you own outright
// before publishing to the Play Store.
// ─────────────────────────────────────────────────────────────────────────────
val DotMatrixFont = FontFamily(
    Font(R.font.dot_matrix, FontWeight.Normal)
)

// ─────────────────────────────────────────────────────────────────────────────
// TraceLedger Typography
//
// Usage rules:
//   displayLarge / displayMedium / displaySmall  → Big financial numbers (balance, totals)
//   headlineLarge / headlineMedium               → Screen titles (OVERVIEW, TRANSACTIONS)
//   titleLarge / titleMedium                     → Section titles, card headers
//   bodyLarge / bodyMedium / bodySmall           → Regular content text
//   labelLarge / labelMedium / labelSmall        → Chips, badges, captions
//
// DotMatrixFont is ONLY used for display* and headline* styles.
// All body and label text uses system SansSerif to stay readable.
// ─────────────────────────────────────────────────────────────────────────────
val TraceLedgerTypography = Typography(

    // ── Display (large financial numbers) ────────────────────────────────────

    // Used for: nothing currently — reserved for full-screen balance views
    displayLarge = TextStyle(
        fontFamily = DotMatrixFont,
        fontWeight = FontWeight.Normal,
        fontSize = 48.sp,
        lineHeight = 56.sp
    ),

    // Used for: nothing currently — reserved for prominent balance displays
    displayMedium = TextStyle(
        fontFamily = DotMatrixFont,
        fontWeight = FontWeight.Normal,
        fontSize = 40.sp,
        lineHeight = 48.sp
    ),

    // FIX: Was missing — DashboardScreen uses displaySmall for total balance.
    // Without this definition it fell back to Material3 default (Roboto),
    // breaking the Nothing-brand dot-matrix aesthetic on the most important number.
    displaySmall = TextStyle(
        fontFamily = DotMatrixFont,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 40.sp
    ),

    // ── Headlines (screen titles) ─────────────────────────────────────────────

    // Used for: main screen titles like "OVERVIEW", "TRANSACTIONS"
    headlineLarge = TextStyle(
        fontFamily = DotMatrixFont,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 36.sp
    ),

    // Used for: sub-screen titles
    headlineMedium = TextStyle(
        fontFamily = DotMatrixFont,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),

    headlineSmall = TextStyle(
        fontFamily = DotMatrixFont,
        fontWeight = FontWeight.Normal,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),

    // ── Titles (card headers, section labels) ─────────────────────────────────

    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 26.sp
    ),

    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),

    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),

    // ── Body (regular content) ────────────────────────────────────────────────

    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),

    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),

    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),

    // ── Labels (chips, badges, captions) ─────────────────────────────────────

    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),

    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),

    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp
    )
)