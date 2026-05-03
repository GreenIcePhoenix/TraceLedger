package com.greenicephoenix.traceledger.feature.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * WidgetUpdateHelper provides a simple way to trigger a widget data refresh
 * from anywhere in the app (MainActivity, after saving a transaction, etc.).
 *
 * WHY THIS IS NEEDED:
 * The widget only auto-refreshes every 30 minutes via updatePeriodMillis.
 * But when the user adds a transaction, we want the widget to update immediately.
 * We call requestUpdate() from MainActivity.onResume() so the widget is always
 * fresh when the user comes back to the home screen.
 *
 * updateAll() is an extension function from Glance that finds all placed instances
 * of TraceLedgerWidget and calls provideGlance() on each of them.
 */
object WidgetUpdateHelper {

    /**
     * Request an immediate refresh of all placed TraceLedger widgets.
     * Safe to call even if no widgets are placed — it's a no-op in that case.
     */
    fun requestUpdate(context: Context) {
        // We use IO dispatcher because updateAll() may read from disk
        CoroutineScope(Dispatchers.IO).launch {
            try {
                TraceLedgerWidget().updateAll(context)
            } catch (e: Exception) {
                // Silently ignore — widget update failure should never crash the app
            }
        }
    }
}