package com.greenicephoenix.traceledger.core.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(name = "theme_prefs")

object ThemeManager {

    private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")

    fun themeModeFlow(context: Context): Flow<ThemeMode> =
        context.themeDataStore.data.map { prefs ->
            when (prefs[THEME_MODE_KEY]) {
                ThemeMode.SYSTEM.name     -> ThemeMode.SYSTEM
                ThemeMode.LIGHT.name      -> ThemeMode.LIGHT
                ThemeMode.DARK.name       -> ThemeMode.DARK
                ThemeMode.ULTRA_DARK.name -> ThemeMode.ULTRA_DARK
                // Default: SYSTEM — new installs follow the device setting.
                // Existing users who had DARK saved will still load DARK correctly
                // because "DARK" is now an explicit case above.
                else -> ThemeMode.SYSTEM
            }
        }

    suspend fun setThemeMode(context: Context, mode: ThemeMode) {
        context.themeDataStore.edit { prefs ->
            prefs[THEME_MODE_KEY] = mode.name
        }
    }
}