package com.greenicephoenix.traceledger.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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

    // Daily Reminder keys — added for v1.1.0
    // Reminder is OFF by default to respect user data preferences
    val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
    val REMINDER_HOUR    = intPreferencesKey("reminder_hour")     // 0–23, default 22 (10 PM)
    val REMINDER_MINUTE  = intPreferencesKey("reminder_minute")   // 0–59, default 0
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

    // ── Daily Reminder ────────────────────────────────────────────────────────

    /** Whether the daily reminder alarm is active. Defaults to false. */
    val reminderEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { it[SettingsKeys.REMINDER_ENABLED] ?: false }

    /** Reminder hour in 24h format. Defaults to 22 (10 PM). */
    val reminderHour: Flow<Int> =
        context.settingsDataStore.data.map { it[SettingsKeys.REMINDER_HOUR] ?: 22 }

    /** Reminder minute. Defaults to 0. */
    val reminderMinute: Flow<Int> =
        context.settingsDataStore.data.map { it[SettingsKeys.REMINDER_MINUTE] ?: 0 }

    // ── Setters ───────────────────────────────────────────────────────────────

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

    suspend fun setReminderEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[SettingsKeys.REMINDER_ENABLED] = enabled }
    }

    suspend fun setReminderTime(hour: Int, minute: Int) {
        context.settingsDataStore.edit {
            it[SettingsKeys.REMINDER_HOUR]   = hour
            it[SettingsKeys.REMINDER_MINUTE] = minute
        }
    }
}