package com.greenicephoenix.traceledger.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.staticCompositionLocalOf

data class TraceLedgerExtraColors(
    val warningBanner: Color,
    val errorBanner: Color
)

val LocalExtraColors = staticCompositionLocalOf {
    TraceLedgerExtraColors(
        warningBanner = WarningBannerLight,
        errorBanner = ErrorBannerLight
    )
}

// ─────────────────────────────────────────────
// DARK THEME (Balanced)
// ─────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(

    // PRIMARY (Sovereign Violet)
    primary            = SovereignViolet,
    onPrimary          = WhitePure,
    primaryContainer   = SovereignVioletDarkContainer,
    onPrimaryContainer = TextPrimaryDark,

    // SECONDARY (semantic only)
    secondary            = SuccessGreen,
    onSecondary          = WhitePure,
    secondaryContainer   = Color(0xFF1A4D2E),
    onSecondaryContainer = Color(0xFFA8E6C0),

    // SURFACES
    background       = DarkBackground,
    onBackground     = TextPrimaryDark,
    surface          = DarkSurface,
    onSurface        = TextPrimaryDark,
    surfaceVariant   = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline          = DarkBorder,
    outlineVariant   = DarkBorderVariant,

    // ERROR
    error            = ErrorRed,
    onError          = WhitePure,
    errorContainer   = Color(0xFF4A1010),
    onErrorContainer = Color(0xFFFFB4AB),

    // MISC
    surfaceTint      = Color.Transparent,
    scrim            = BlackPure
)

private val DarkExtraColors = TraceLedgerExtraColors(
    warningBanner = WarningBannerDark,
    errorBanner = ErrorBannerDark
)

// ─────────────────────────────────────────────
// ULTRA DARK THEME (OLED)
// ─────────────────────────────────────────────
private val UltraDarkColorScheme = darkColorScheme(

    primary            = SovereignViolet,
    onPrimary          = WhitePure,
    primaryContainer   = SovereignVioletDarkContainer,
    onPrimaryContainer = TextPrimaryDark,

    secondary            = SuccessGreen,
    onSecondary          = WhitePure,
    secondaryContainer   = Color(0xFF1A4D2E),
    onSecondaryContainer = Color(0xFFA8E6C0),

    background       = UltraDarkBackground,
    onBackground     = TextPrimaryDark,
    surface          = UltraDarkSurface,
    onSurface        = TextPrimaryDark,
    surfaceVariant   = UltraDarkSecondary,
    onSurfaceVariant = TextSecondaryDark,
    outline          = UltraDarkBorder,
    outlineVariant   = UltraDarkBorderVariant,

    error            = ErrorRed,
    onError          = WhitePure,
    errorContainer   = Color(0xFF3A0808),
    onErrorContainer = Color(0xFFFFB4AB),

    surfaceTint      = Color.Transparent,
    scrim            = BlackPure
)

private val UltraDarkExtraColors = TraceLedgerExtraColors(
    warningBanner = WarningBannerDark,
    errorBanner = ErrorBannerDark
)

// ─────────────────────────────────────────────
// LIGHT THEME
// ─────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(

    primary            = SovereignVioletDark,
    onPrimary          = WhitePure,
    primaryContainer   = SovereignVioletContainer,
    onPrimaryContainer = SovereignVioletOnContainer,

    secondary            = SuccessGreen,
    onSecondary          = WhitePure,
    secondaryContainer   = Color(0xFFCCF0DC),
    onSecondaryContainer = Color(0xFF004D20),

    background       = LightBackground,
    onBackground     = LightTextPrimary,
    surface          = LightSurface,
    onSurface        = LightTextPrimary,
    surfaceVariant   = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline          = LightBorder,
    outlineVariant   = LightBorderVariant,

    error            = ErrorRed,
    onError          = WhitePure,
    errorContainer   = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    surfaceTint      = Color.Transparent,
    scrim            = BlackPure
)

private val LightExtraColors = TraceLedgerExtraColors(
    warningBanner = WarningBannerLight,
    errorBanner = ErrorBannerLight
)

// ─────────────────────────────────────────────
// ROOT THEME
// ─────────────────────────────────────────────
@Composable
fun TraceLedgerTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    val (colors, extraColors) = when (themeMode) {

        ThemeMode.SYSTEM -> {
            if (isSystemInDarkTheme()) {
                DarkColorScheme to DarkExtraColors
            } else {
                LightColorScheme to LightExtraColors
            }
        }

        ThemeMode.LIGHT ->
            LightColorScheme to LightExtraColors

        ThemeMode.DARK ->
            DarkColorScheme to DarkExtraColors

        ThemeMode.ULTRA_DARK ->
            UltraDarkColorScheme to UltraDarkExtraColors
    }

    CompositionLocalProvider(
        LocalExtraColors provides extraColors
    ) {
        MaterialTheme(
            colorScheme = colors,
            typography  = TraceLedgerTypography,
            content     = content
        )
    }
}

object TraceLedgerThemeExtras {

    val warningBanner: Color
        @Composable
        get() = LocalExtraColors.current.warningBanner

    val errorBanner: Color
        @Composable
        get() = LocalExtraColors.current.errorBanner
}