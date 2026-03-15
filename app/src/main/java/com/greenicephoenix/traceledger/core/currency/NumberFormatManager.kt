package com.greenicephoenix.traceledger.core.currency

import android.content.Context
import com.greenicephoenix.traceledger.core.datastore.NumberFormat
import com.greenicephoenix.traceledger.core.datastore.SettingsDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Single source of truth for the selected number format (Indian vs International grouping).
 *
 * Mirrors CurrencyManager's pattern: backed by DataStore, exposed as a StateFlow so
 * CurrencyFormatter can read it synchronously without requiring a Context or coroutine.
 *
 * Initialised once from TraceLedgerApp.onCreate() alongside CurrencyManager.
 */
object NumberFormatManager {

    private val scope = CoroutineScope(Dispatchers.IO)

    // Default to INDIAN grouping (1,00,000) — the app's primary market
    private val _format = MutableStateFlow(NumberFormat.INDIAN)
    val format: StateFlow<NumberFormat> = _format.asStateFlow()

    private var dataStore: SettingsDataStore? = null

    fun init(context: Context) {
        // Guard against double-initialisation (e.g. if called from multiple places)
        if (dataStore != null) return

        dataStore = SettingsDataStore(context)

        scope.launch {
            dataStore!!.numberFormat.collect { saved ->
                // If user has never picked a format, keep the INDIAN default
                val resolved = when (saved) {
                    NumberFormat.INTERNATIONAL.name -> NumberFormat.INTERNATIONAL
                    else                            -> NumberFormat.INDIAN
                }
                _format.value = resolved
            }
        }
    }

    /** Called from SettingsScreen when the user picks a new format. */
    fun setFormat(format: NumberFormat) {
        _format.value = format
        scope.launch {
            dataStore?.setNumberFormat(format)
        }
    }
}