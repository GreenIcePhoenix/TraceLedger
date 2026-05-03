package com.greenicephoenix.traceledger.core.ui.theme

/**
 * All supported theme modes.
 *
 * SYSTEM     — Follows the device's light/dark setting automatically.
 * LIGHT      — Always light, regardless of device setting.
 * DARK       — Always dark. Surfaces use the Void palette (#0F0F18 base).
 * ULTRA_DARK — Deepest black. Surfaces use Void Deep (#050508 base).
 *              Intended for OLED screens — near-zero backlight usage.
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    ULTRA_DARK
}