package com.greenicephoenix.traceledger.feature.sms.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.greenicephoenix.traceledger.core.datastore.SettingsDataStore
import com.greenicephoenix.traceledger.feature.sms.receiver.SmsTransactionReceiver
import com.greenicephoenix.traceledger.feature.sms.repository.SmsQueueRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SmsSettingsUiState(
    val isRealtimeEnabled: Boolean = false,
    val inboxScanState: InboxScanState = InboxScanState.Idle,
    val pendingCount: Int = 0,
)

sealed class InboxScanState {
    object Idle : InboxScanState()
    data class Scanning(val current: Int, val total: Int) : InboxScanState()
    data class Done(val newItemsFound: Int) : InboxScanState()
    data class Error(val message: String) : InboxScanState()
}

class SmsSettingsViewModel(
    application: Application,
    private val smsQueueRepository: SmsQueueRepository,
    private val settingsDataStore: SettingsDataStore,
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(SmsSettingsUiState())
    val state: StateFlow<SmsSettingsUiState> = _state.asStateFlow()

    init {
        // Read current receiver enabled state
        viewModelScope.launch {
            val isEnabled = isReceiverEnabled(application)
            _state.update { it.copy(isRealtimeEnabled = isEnabled) }
        }
        viewModelScope.launch {
            smsQueueRepository.observePendingCount().collect { count ->
                _state.update { it.copy(pendingCount = count) }
            }
        }
    }

    /**
     * Toggles the real-time SMS receiver on/off.
     * This does NOT request permissions — the UI layer handles that.
     * Call this only AFTER the user has granted RECEIVE_SMS permission.
     */
    fun setRealtimeEnabled(enabled: Boolean) {
        val context = getApplication<Application>()
        val component = ComponentName(context, SmsTransactionReceiver::class.java)
        val newState = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        context.packageManager.setComponentEnabledSetting(
            component,
            newState,
            PackageManager.DONT_KILL_APP
        )
        _state.update { it.copy(isRealtimeEnabled = enabled) }
    }

    /**
     * Scans the SMS inbox for past financial transactions.
     * Requires READ_SMS permission — the UI layer must verify before calling.
     */
    fun startInboxScan(daysBack: Int = 90) {
        viewModelScope.launch {
            _state.update { it.copy(inboxScanState = InboxScanState.Scanning(0, 0)) }
            try {
                val count = smsQueueRepository.scanInbox(daysBack) { current, total ->
                    _state.update {
                        it.copy(inboxScanState = InboxScanState.Scanning(current, total))
                    }
                }
                _state.update { it.copy(inboxScanState = InboxScanState.Done(count)) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(inboxScanState = InboxScanState.Error(e.message ?: "Scan failed"))
                }
            }
        }
    }

    fun resetScanState() {
        _state.update { it.copy(inboxScanState = InboxScanState.Idle) }
    }

    private fun isReceiverEnabled(context: Context): Boolean {
        val component = ComponentName(context, SmsTransactionReceiver::class.java)
        val state = context.packageManager.getComponentEnabledSetting(component)
        return state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    }
}