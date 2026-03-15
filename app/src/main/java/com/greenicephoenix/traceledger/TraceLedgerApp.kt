package com.greenicephoenix.traceledger

import android.app.Application
import com.greenicephoenix.traceledger.core.currency.NumberFormatManager
import com.greenicephoenix.traceledger.core.di.AppContainer
import com.greenicephoenix.traceledger.core.notifications.NotificationHelper

class TraceLedgerApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()

        container = AppContainer(this)

        // Initialise NumberFormatManager so CurrencyFormatter can read the
        // user's grouping preference (Indian vs International) from the first frame.
        // This mirrors how CurrencyManager is initialised in MainActivity.
        NumberFormatManager.init(this)

        // Create the daily reminder notification channel.
        // This must happen before any notification can be posted on Android 8+.
        // It's safe to call on every launch — Android ignores duplicate channel creation.
        NotificationHelper.createChannel(this)
    }
}