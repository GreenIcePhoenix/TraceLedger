package com.greenicephoenix.traceledger.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.greenicephoenix.traceledger.MainActivity
import com.greenicephoenix.traceledger.R

/**
 * Manages notification channels and posting of the daily reminder notification.
 *
 * Android 8 (API 26) and above require notifications to be assigned to a channel.
 * The channel is created here. Creating it is idempotent — calling it multiple times
 * is safe and has no effect after the first call.
 *
 * Why a single object? Because both the receiver (DailyReminderReceiver) and the
 * app startup (TraceLedgerApp) need to call createChannel(), and we want one place
 * for all notification-related logic.
 */
object NotificationHelper {

    const val CHANNEL_ID   = "daily_reminder"
    const val NOTIFICATION_ID = 1001

    /**
     * Creates the notification channel for the daily reminder.
     * Must be called before any notification can be posted on Android 8+.
     * Safe to call multiple times — Android ignores duplicate channel creation.
     */
    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Daily Reminder",                          // Channel name shown in Settings
            NotificationManager.IMPORTANCE_DEFAULT     // Shows in shade, plays sound, no heads-up
        ).apply {
            description = "Remind you to log your daily transactions"
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    /**
     * Posts the daily reminder notification.
     * Tapping it opens MainActivity (which then shows the app's last state).
     *
     * FLAG_IMMUTABLE is required on Android 12+ for PendingIntents.
     */
    fun postReminder(context: Context) {
        // Create a PendingIntent so tapping the notification opens the app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)          // app icon in the notification shade
            .setContentTitle("TraceLedger")
            .setContentText("Don't forget to log today's transactions!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)                         // dismiss when tapped
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }
}