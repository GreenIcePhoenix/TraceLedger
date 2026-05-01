package com.greenicephoenix.traceledger.feature.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.greenicephoenix.traceledger.feature.templates.data.TemplateRepository

class TemplatesViewModelFactory(
    private val repository: TemplateRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == TemplatesViewModel::class.java)
        return TemplatesViewModel(repository) as T
    }
}