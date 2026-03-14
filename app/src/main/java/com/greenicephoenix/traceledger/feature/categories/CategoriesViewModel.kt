package com.greenicephoenix.traceledger.feature.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenicephoenix.traceledger.core.database.entity.CategoryEntity
import com.greenicephoenix.traceledger.core.repository.CategoryRepository
import com.greenicephoenix.traceledger.domain.model.CategoryType
import com.greenicephoenix.traceledger.domain.model.CategoryUiModel
import com.greenicephoenix.traceledger.feature.categories.data.CategorySeed
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CategoriesViewModel(
    private val repository: CategoryRepository
) : ViewModel() {

    val categories: StateFlow<List<CategoryUiModel>> =
        repository.observeCategories()
            .map { entities ->
                entities.map {
                    CategoryUiModel(
                        id    = it.id,
                        name  = it.name,
                        type  = CategoryType.valueOf(it.type),
                        color = it.color,
                        icon  = it.icon
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { repository.seedIfEmpty(CategorySeed.defaults()) }
    }

    // Null = no error. Non-null = message to show in error dialog.
    private val _deleteError = MutableStateFlow<String?>(null)
    val deleteError: StateFlow<String?> = _deleteError.asStateFlow()

    fun clearDeleteError() { _deleteError.value = null }

    fun addCategory(category: CategoryUiModel) {
        viewModelScope.launch { repository.upsert(category.toEntity()) }
    }

    fun updateCategory(category: CategoryUiModel) {
        viewModelScope.launch { repository.upsert(category.toEntity()) }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            repository.delete(categoryId)
                .onFailure { e -> _deleteError.value = e.message }
        }
    }

    private fun CategoryUiModel.toEntity() = CategoryEntity(
        id    = id,
        name  = name,
        type  = type.name,
        color = color,
        icon  = icon
    )
}