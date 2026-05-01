package com.greenicephoenix.traceledger.feature.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenicephoenix.traceledger.feature.templates.data.TemplateRepository
import com.greenicephoenix.traceledger.feature.templates.domain.TransactionTemplateUiModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the standalone Templates management screen (Settings → Templates).
 * Also used by NavGraph to pass a save callback to AddEditTemplateScreen.
 */
class TemplatesViewModel(
    private val repository: TemplateRepository
) : ViewModel() {

    /** Full list of saved templates, updated reactively. */
    val templates: StateFlow<List<TransactionTemplateUiModel>> =
        repository.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Insert or update a template. */
    fun saveTemplate(template: TransactionTemplateUiModel) {
        viewModelScope.launch { repository.save(template) }
    }

    /** Delete a template by ID. */
    fun deleteTemplate(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }
}