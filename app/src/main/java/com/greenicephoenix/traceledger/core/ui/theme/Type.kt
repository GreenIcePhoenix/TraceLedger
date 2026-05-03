package com.greenicephoenix.traceledger.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.greenicephoenix.traceledger.R

// ─────────────────────────────────────────────────────────────────────────────
// Font families
//
// Three fonts, three distinct roles — no overlap:
//
//   DotMatrixFont  →  display* only (the big financial numbers: total balance,
//                     monthly totals). This is TraceLedger's signature. Kept
//                     from the original Nothing OS design — financial figures
//                     should feel distinct from all other text.
//
//   CinzelFont     →  headline* (screen-level titles: DASHBOARD, STATISTICS,
//                     ACCOUNTS). GIP brand font — sharp, elegant, forged-in-stone.
//                     License: SIL Open Font License (OFL) — Play Store safe.
//
//   OutfitFont     →  title*, body*, label* (all UI text, card headers, body
//                     copy, chips, captions). Modern geometric sans. Far more
//                     readable at small sizes than the old generic SansSerif.
//                     License: OFL — Play Store safe.
//
// Font files location: app/src/main/res/font/
// ─────────────────────────────────────────────────────────────────────────────

val DotMatrixFont = FontFamily(
    Font(R.font.dot_matrix, FontWeight.Normal)
)

val CinzelFont = FontFamily(
    Font(R.font.cinzel_regular, FontWeight.Normal),
    Font(R.font.cinzel_bold,    FontWeight.Bold),
    Font(R.font.cinzel_black,   FontWeight.Black)
)

val OutfitFont = FontFamily(
    Font(R.font.outfit_light,    FontWeight.Light),
    Font(R.font.outfit_regular,  FontWeight.Normal),
    Font(R.font.outfit_medium,   FontWeight.Medium),
    Font(R.font.outfit_semibold, FontWeight.SemiBold)
)

// ─────────────────────────────────────────────────────────────────────────────
// TraceLedger Typography
// ─────────────────────────────────────────────────────────────────────────────
val TraceLedgerTypography = Typography(

    // ── Display — DotMatrixFont ───────────────────────────────────────────────
    // Used for: total balance (displaySmall), large financial figures.
    // These are the app's signature numbers. DotMatrix makes them unmistakably
    // "financial data", visually separated from all other text.

    displayLarge = TextStyle(
        fontFamily = DotMatrixFont,
        fontWeight = FontWeight.Normal,
        fontSize   = 48.sp,
        lineHeight = 56.sp
    ),
    displayMedium = TextStyle(
        fontFamily = DotMatrixFont,
        fontWeight = FontWeight.Normal,
        fontSize   = 40.sp,
        lineHeight = 48.sp
    ),
    displaySmall = TextStyle(
        fontFamily = DotMatrixFont,
        fontWeight = FontWeight.Normal,
        fontSize   = 32.sp,
        lineHeight = 40.sp
    ),

    // ── Headlines — CinzelFont ────────────────────────────────────────────────
    // Used for: main screen titles (DASHBOARD, ACCOUNTS, STATISTICS, SETTINGS).
    // Cinzel's sharp serifs give these titles a premium, branded character.

    headlineLarge = TextStyle(
        fontFamily = CinzelFont,
        fontWeight = FontWeight.Bold,
        fontSize   = 28.sp,
        lineHeight = 36.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = CinzelFont,
        fontWeight = FontWeight.Bold,
        fontSize   = 24.sp,
        lineHeight = 32.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = CinzelFont,
        fontWeight = FontWeight.Normal,
        fontSize   = 20.sp,
        lineHeight = 28.sp
    ),

    // ── Titles — OutfitFont ───────────────────────────────────────────────────
    // Used for: card headers, section labels, dialog titles.
    // Outfit SemiBold at these sizes is clean and highly legible.

    titleLarge = TextStyle(
        fontFamily = OutfitFont,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 18.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = OutfitFont,
        fontWeight = FontWeight.Medium,
        fontSize   = 16.sp,
        lineHeight = 24.sp
    ),
    titleSmall = TextStyle(
        fontFamily = OutfitFont,
        fontWeight = FontWeight.Medium,
        fontSize   = 14.sp,
        lineHeight = 20.sp
    ),

    // ── Body — OutfitFont ─────────────────────────────────────────────────────
    // Used for: transaction notes, settings descriptions, all paragraph text.
    // Outfit Regular is noticeably more polished than generic SansSerif.

    bodyLarge = TextStyle(
        fontFamily = OutfitFont,
        fontWeight = FontWeight.Normal,
        fontSize   = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = OutfitFont,
        fontWeight = FontWeight.Normal,
        fontSize   = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = OutfitFont,
        fontWeight = FontWeight.Light,
        fontSize   = 12.sp,
        lineHeight = 16.sp
    ),

    // ── Labels — OutfitFont ───────────────────────────────────────────────────
    // Used for: chips, badges, captions, bottom nav labels.

    labelLarge = TextStyle(
        fontFamily = OutfitFont,
        fontWeight = FontWeight.Medium,
        fontSize   = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = OutfitFont,
        fontWeight = FontWeight.Medium,
        fontSize   = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = OutfitFont,
        fontWeight = FontWeight.Normal,
        fontSize   = 11.sp,
        lineHeight = 16.sp
    )
)