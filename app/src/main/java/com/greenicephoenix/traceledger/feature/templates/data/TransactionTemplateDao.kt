package com.greenicephoenix.traceledger.feature.templates.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionTemplateDao {

    /** Observe all templates, sorted alphabetically by name. */
    @Query("SELECT * FROM transaction_templates ORDER BY name ASC")
    fun observeAll(): Flow<List<TransactionTemplateEntity>>

    /** One-shot fetch by ID (used when loading a template for editing). */
    @Query("SELECT * FROM transaction_templates WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TransactionTemplateEntity?

    /** Insert or replace (used for both create and update). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(template: TransactionTemplateEntity)

    /** Delete a single template by ID. */
    @Query("DELETE FROM transaction_templates WHERE id = :id")
    suspend fun deleteById(id: String)
}