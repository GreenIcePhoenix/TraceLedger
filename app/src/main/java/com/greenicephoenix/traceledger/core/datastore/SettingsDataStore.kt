package com.greenicephoenix.traceledger.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val DATASTORE_NAME = "traceledger_settings"

val Context.settingsDataStore by preferencesDataStore(name = DATASTORE_NAME)

object SettingsKeys {
    val CURRENCY_CODE        = stringPreferencesKey("currency_code")
    val LAST_SEEN_VERSION    = stringPreferencesKey("last_seen_version")
    val NUMBER_FORMAT        = stringPreferencesKey("number_format")
    val ONBOARDING_COMPLETE  = booleanPreferencesKey("onboarding_complete")
}

enum class NumberFormat(val label: String, val example: String) {
    INDIAN("Indian (1,00,000)", "1,00,000"),
    INTERNATIONAL("International (100,000)", "100,000")
}

class SettingsDataStore(private val context: Context) {

    val currencyCode: Flow<String?> =
        context.settingsDataStore.data.map { it[SettingsKeys.CURRENCY_CODE] }

    val lastSeenVersion: Flow<String?> =
        context.settingsDataStore.data.map { it[SettingsKeys.LAST_SEEN_VERSION] }

    val numberFormat: Flow<String?> =
        context.settingsDataStore.data.map { it[SettingsKeys.NUMBER_FORMAT] }

    // Emits null on first install (never written), true once onboarding is done.
    // We treat null as "not completed" so new installs always see onboarding.
    val onboardingComplete: Flow<Boolean?> =
        context.settingsDataStore.data.map { it[SettingsKeys.ONBOARDING_COMPLETE] }

    suspend fun setCurrency(code: String) {
        context.settingsDataStore.edit { it[SettingsKeys.CURRENCY_CODE] = code }
    }

    suspend fun setLastSeenVersion(version: String) {
        context.settingsDataStore.edit { it[SettingsKeys.LAST_SEEN_VERSION] = version }
    }

    suspend fun setNumberFormat(format: NumberFormat) {
        context.settingsDataStore.edit { it[SettingsKeys.NUMBER_FORMAT] = format.name }
    }

    suspend fun completeOnboarding() {
        context.settingsDataStore.edit { it[SettingsKeys.ONBOARDING_COMPLETE] = true }
    }
}