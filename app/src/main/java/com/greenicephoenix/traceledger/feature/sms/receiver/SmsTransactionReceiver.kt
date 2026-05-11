package com.greenicephoenix.traceledger.feature.sms.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.greenicephoenix.traceledger.TraceLedgerApp
import com.greenicephoenix.traceledger.feature.sms.repository.SmsQueueRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Listens for incoming SMS messages and processes them in the background.
 *
 * LIFECYCLE:
 *  Android calls onReceive() on the main thread. Processing an SMS involves
 *  DB queries + regex matching, so we use goAsync() to get a brief extended
 *  window (~30s) for background work on an IO coroutine.
 *
 * IMPORTANT:
 *  This receiver is ONLY active when the user has enabled SMS detection in Settings.
 *  We enable/disable it programmatically via PackageManager.setComponentEnabledSetting()
 *  — it's declared in the manifest but starts DISABLED.
 *
 * REGISTRATION (AndroidManifest.xml — see File 22):
 *  <receiver android:name=".feature.sms.receiver.SmsTransactionReceiver"
 *            android:enabled="false"       ← starts disabled, user opts in
 *            android:exported="true">
 *      <intent-filter android:priority="999">
 *          <action android:name="android.provider.Telephony.SMS_RECEIVED" />
 *      </intent-filter>
 *  </receiver>
 */
class SmsTransactionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        // goAsync() gives us time to do background work
        // (default BroadcastReceiver timeout is ~5 seconds — too short for DB + regex)
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                if (messages.isNullOrEmpty()) return@launch

                val app = context.applicationContext as TraceLedgerApp
                val repository: SmsQueueRepository = app.container.smsQueueRepository

                // Multi-part SMS: combine all parts into one body before parsing
                // (Long SMSes arrive as multiple PDUs but represent one message)
                val sender = messages.first().originatingAddress ?: return@launch
                val body = messages.joinToString("") { it.messageBody ?: "" }
                val timestamp = messages.first().timestampMillis

                repository.processIncomingSms(
                    sender = sender,
                    body = body,
                    timestamp = timestamp,
                    smsId = -1L // Real-time SMS doesn't give us the DB ID — use -1
                )
            } finally {
                // Always call finish() or Android ANRs
                pendingResult.finish()
            }
        }
    }
}