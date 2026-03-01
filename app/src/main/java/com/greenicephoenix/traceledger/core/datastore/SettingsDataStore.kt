package com.greenicephoenix.traceledger.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val DATASTORE_NAME = "traceledger_settings"

val Context.settingsDataStore by preferencesDataStore(
    name = DATASTORE_NAME
)

object SettingsKeys {
    val CURRENCY_CODE = stringPreferencesKey("currency_code")
    val LAST_SEEN_VERSION = stringPreferencesKey("last_seen_version")
}

class SettingsDataStore(
    private val context: Context
) {

    val currencyCode: Flow<String?> =
        context.settingsDataStore.data.map { prefs ->
            prefs[SettingsKeys.CURRENCY_CODE]
        }

    val lastSeenVersion: Flow<String?> =
        context.settingsDataStore.data.map { prefs ->
            prefs[SettingsKeys.LAST_SEEN_VERSION]
        }

    suspend fun setCurrency(code: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[SettingsKeys.CURRENCY_CODE] = code
        }
    }

    suspend fun setLastSeenVersion(version: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[SettingsKeys.LAST_SEEN_VERSION] = version
        }
    }

}