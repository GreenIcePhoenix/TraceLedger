package com.greenicephoenix.traceledger.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.greenicephoenix.traceledger.core.datastore.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver that handles two events:
 *
 * 1. Our custom alarm action (DAILY_REMINDER) — post the notification and
 *    re-schedule for the same time tomorrow.
 *
 * 2. BOOT_COMPLETED — device just restarted. AlarmManager alarms don't
 *    survive reboots, so if the user has the reminder enabled we must
 *    re-schedule it here.
 *
 * This receiver is declared in AndroidManifest.xml with both intent-filters.
 * android:exported="false" for the reminder action (only this app triggers it),
 * but BOOT_COMPLETED requires exported="true" — handled in the manifest.
 */
class DailyReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {

            // ── Alarm fired — post notification + re-schedule ─────────────────
            "com.greenicephoenix.traceledger.DAILY_REMINDER" -> {
                NotificationHelper.postReminder(context)

                // Re-schedule for the same time tomorrow
                // We need to read the stored time from DataStore
                CoroutineScope(Dispatchers.IO).launch {
                    val store  = SettingsDataStore(context)
                    val hour   = store.reminderHour.first()
                    val minute = store.reminderMinute.first()
                    ReminderScheduler.schedule(context, hour, minute)
                }
            }

            // ── Device rebooted — re-schedule if reminder was enabled ─────────
            Intent.ACTION_BOOT_COMPLETED -> {
                CoroutineScope(Dispatchers.IO).launch {
                    val store   = SettingsDataStore(context)
                    val enabled = store.reminderEnabled.first()
                    if (enabled) {
                        val hour   = store.reminderHour.first()
                        val minute = store.reminderMinute.first()
                        ReminderScheduler.schedule(context, hour, minute)
                    }
                }
            }
        }
    }
}