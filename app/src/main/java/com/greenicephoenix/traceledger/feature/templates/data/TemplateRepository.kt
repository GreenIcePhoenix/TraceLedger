package com.greenicephoenix.traceledger.feature.templates.data

import com.greenicephoenix.traceledger.domain.model.TransactionType
import com.greenicephoenix.traceledger.feature.templates.domain.TransactionTemplateUiModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

/**
 * All template read/write operations.
 * Maps between TransactionTemplateEntity (DB layer) and
 * TransactionTemplateUiModel (UI layer) so the rest of the app
 * never touches the entity directly.
 */
class TemplateRepository(private val dao: TransactionTemplateDao) {

    /** Observe all templates as UI models. */
    fun observeAll(): Flow<List<TransactionTemplateUiModel>> =
        dao.observeAll().map { list -> list.map { it.toUiModel() } }

    /** One-shot fetch by ID. Returns null if not found. */
    suspend fun getById(id: String): TransactionTemplateUiModel? =
        dao.getById(id)?.toUiModel()

    /** Insert or update a template. */
    suspend fun save(template: TransactionTemplateUiModel) =
        dao.upsert(template.toEntity())

    /** Delete a template by ID. */
    suspend fun delete(id: String) = dao.deleteById(id)

    // ── Mappers ──────────────────────────────────────────────────────────────

    private fun TransactionTemplateEntity.toUiModel() = TransactionTemplateUiModel(
        id            = id,
        name          = name,
        type          = TransactionType.valueOf(type),
        amount        = amount?.toBigDecimalOrNull(),
        fromAccountId = fromAccountId,
        toAccountId   = toAccountId,
        categoryId    = categoryId,
        notes         = notes
    )

    private fun TransactionTemplateUiModel.toEntity() = TransactionTemplateEntity(
        id            = id,
        name          = name,
        type          = type.name,
        amount        = amount?.toPlainString(),
        fromAccountId = fromAccountId,
        toAccountId   = toAccountId,
        categoryId    = categoryId,
        notes         = notes,
        createdAt     = Instant.now().toEpochMilli()
    )
}