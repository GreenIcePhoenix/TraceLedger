package com.greenicephoenix.traceledger.core.repository

import android.database.sqlite.SQLiteConstraintException
import com.greenicephoenix.traceledger.core.database.dao.CategoryDao
import com.greenicephoenix.traceledger.core.database.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

class CategoryRepository(private val dao: CategoryDao) {

    fun observeCategories(): Flow<List<CategoryEntity>> = dao.observeAll()

    suspend fun upsert(category: CategoryEntity) = dao.upsert(category)

    /**
     * Delete a category by ID.
     * Returns Result.failure if transactions reference this category (FK constraint).
     */
    suspend fun delete(categoryId: String): Result<Unit> {
        return try {
            dao.delete(categoryId)
            Result.success(Unit)
        } catch (e: SQLiteConstraintException) {
            Result.failure(
                Exception("This category has transactions linked to it and cannot be deleted.")
            )
        }
    }

    suspend fun seedIfEmpty(defaults: List<CategoryEntity>) {
        if (dao.count() == 0) dao.insertAll(defaults)
    }
}