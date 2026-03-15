package com.greenicephoenix.traceledger.core.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

/**
 * Schedules and cancels the daily reminder alarm using AlarmManager.
 *
 * Why AlarmManager instead of WorkManager?
 * WorkManager is great for background work but doesn't guarantee exact timing.
 * For a "remind me at 10 PM every day" feature the user expects the notification
 * at the exact time they chose. setExactAndAllowWhileIdle() fires even in Doze
 * mode, which is what we need.
 *
 * Important: AlarmManager alarms are lost when the device reboots. That's why
 * DailyReminderReceiver also listens for BOOT_COMPLETED and calls schedule() again.
 */
object ReminderScheduler {

    // The action string used to identify our alarm intent
    private const val ACTION_REMINDER = "com.greenicephoenix.traceledger.DAILY_REMINDER"

    /**
     * Schedules a daily alarm at [hour]:[minute] (24h).
     *
     * If the time has already passed today, schedules for tomorrow.
     * After the alarm fires, DailyReminderReceiver calls this again for the next day —
     * creating a repeating daily alarm without using AlarmManager.setRepeating()
     * (which isn't exact on modern Android).
     *
     * Exact vs inexact:
     * On Android 12+ (API 31+), SCHEDULE_EXACT_ALARM requires the user to grant
     * "Alarms & Reminders" from Special App Access in system Settings — declaring the
     * permission in the manifest is not enough. We check canScheduleExactAlarms()
     * first. If not granted we fall back to setAndAllowWhileIdle() (inexact — fires
     * within a few minutes of the target time, perfectly acceptable for a daily
     * reminder). This avoids a SecurityException crash and avoids forcing the user
     * into a system settings detour just to use the reminder.
     */
    fun schedule(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)

        val triggerTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // If the alarm time is in the past today, push it to tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }.timeInMillis

        val pendingIntent = buildPendingIntent(context)

        // On Android 12+ check whether exact alarms are permitted before calling
        // setExactAndAllowWhileIdle(). Without this check the call throws a
        // SecurityException if the user hasn't explicitly granted the permission.
        val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true   // Pre-API 31: exact alarms are always allowed
        }

        if (canScheduleExact) {
            // Exact alarm — fires at precisely the chosen time, even in Doze mode
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } else {
            // Inexact fallback — fires within a window around the target time.
            // Good enough for a daily reminder; avoids the crash entirely.
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    /**
     * Cancels the pending alarm.
     * Called when the user toggles the reminder off in Settings.
     */
    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.cancel(buildPendingIntent(context))
    }

    private fun buildPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
        }
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}