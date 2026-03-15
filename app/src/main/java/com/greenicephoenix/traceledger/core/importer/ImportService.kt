package com.greenicephoenix.traceledger.core.importer

import android.content.ContentResolver
import android.net.Uri
import androidx.room.withTransaction
import com.greenicephoenix.traceledger.core.database.TraceLedgerDatabase
import com.greenicephoenix.traceledger.core.export.ExportEnvelope
import com.greenicephoenix.traceledger.core.database.entity.AccountEntity
import com.greenicephoenix.traceledger.core.database.entity.CategoryEntity
import com.greenicephoenix.traceledger.core.database.entity.TransactionEntity
import com.greenicephoenix.traceledger.feature.budgets.data.BudgetEntity
import kotlinx.serialization.json.Json
import java.io.InputStreamReader
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

/**
 * Result returned by [ImportService.importCsvTransactions].
 * Previously the function returned Unit, so callers had no way to show a
 * completion message. Now they can display "X imported, Y skipped."
 */
data class ImportResult(
    val imported: Int,
    val skipped: Int
)

class ImportService(
    private val database: TraceLedgerDatabase,
    private val contentResolver: ContentResolver
) {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    // ── JSON IMPORT ───────────────────────────────────────────────────────────

    /**
     * Full replace import from a TraceLedger JSON backup.
     * Wipes all existing data, then inserts everything from the backup file.
     * Runs inside a single Room transaction — either all succeeds or nothing changes.
     *
     * [onProgress] receives 0–100 as a percentage of items processed.
     */
    suspend fun importJson(
        uri: Uri,
        onProgress: (Int) -> Unit
    ) {
        val envelope = readEnvelope(uri)
        validateEnvelope(envelope)

        database.withTransaction {
            wipeAll()

            val total =
                envelope.accounts.size +
                        envelope.categories.size +
                        envelope.budgets.size +
                        envelope.transactions.size

            var done = 0
            fun step() {
                done++
                if (total > 0) onProgress((done * 100) / total)
            }

            envelope.accounts.forEach {
                database.accountDao().insert(
                    AccountEntity(
                        id             = it.id,
                        name           = it.name,
                        balance        = BigDecimal(it.balance),
                        type           = it.type,
                        includeInTotal = it.includeInTotal,
                        details        = null,
                        color          = it.color
                    )
                )
                step()
            }

            envelope.categories.forEach {
                database.categoryDao().insert(
                    CategoryEntity(
                        id    = it.id,
                        name  = it.name,
                        type  = it.type,
                        color = it.color,
                        icon  = it.icon
                    )
                )
                step()
            }

            envelope.budgets.forEach {
                database.budgetDao().insert(
                    BudgetEntity(
                        id          = it.id,
                        categoryId  = it.categoryId,
                        limitAmount = BigDecimal(it.limitAmount),
                        month       = YearMonth.parse(it.month),
                        isActive    = it.isActive
                    )
                )
                step()
            }

            envelope.transactions.forEach {
                // For import we use insertTransaction() directly (not the WithBalance variant)
                // because this is a raw data restore — balances are already encoded in AccountEntity.
                database.transactionDao().insertTransaction(
                    TransactionEntity(
                        id            = it.id,
                        type          = it.type,
                        amount        = BigDecimal(it.amount),
                        date          = LocalDate.parse(it.date),
                        fromAccountId = it.fromAccountId,
                        toAccountId   = it.toAccountId,
                        categoryId    = it.categoryId,
                        note          = it.note,
                        createdAt     = Instant.ofEpochSecond(it.createdAtEpoch)
                    )
                )
                step()
            }
        }
    }

    // ── CSV IMPORT ────────────────────────────────────────────────────────────

    /**
     * Additive CSV import — transactions only, does not wipe existing data.
     *
     * Returns [ImportResult] with counts of imported and skipped rows so the
     * caller can show a completion message to the user.
     * Previously returned Unit and silently dropped skipped rows.
     *
     * Expected CSV columns (9 minimum):
     *   id, type, amount, date, fromAccountId, toAccountId, categoryId, note, createdAt
     */
    suspend fun importCsvTransactions(
        uri: Uri,
        onProgress: (Int) -> Unit
    ): ImportResult {
        val stream = contentResolver.openInputStream(uri)
            ?: error("Unable to open CSV file")

        val existingAccountIds =
            database.accountDao().getAllOnce().map { it.id }.toSet()
        val existingCategoryIds =
            database.categoryDao().getAllOnce().map { it.id }.toSet()

        val lines = stream.bufferedReader().readLines()
        // Return an empty result (not an error) for header-only files
        if (lines.size <= 1) return ImportResult(imported = 0, skipped = 0)

        val rows  = lines.drop(1)   // Skip header row
        val total = rows.size
        var processed = 0
        var imported  = 0
        var skipped   = 0

        database.withTransaction {
            rows.forEach { line ->
                processed++
                onProgress((processed * 100) / total)

                val cols = parseCsvLine(line)
                if (cols.size < 9) {
                    skipped++
                    return@forEach
                }

                val fromAccountId = cols[4].ifBlank { null }
                val toAccountId   = cols[5].ifBlank { null }
                val categoryId    = cols[6].ifBlank { null }

                // Skip rows that reference accounts/categories not in the DB
                val hasUnknownAccount =
                    (fromAccountId != null && fromAccountId !in existingAccountIds) ||
                            (toAccountId   != null && toAccountId   !in existingAccountIds)

                val hasUnknownCategory =
                    categoryId != null && categoryId !in existingCategoryIds

                if (hasUnknownAccount || hasUnknownCategory) {
                    skipped++
                    return@forEach
                }

                database.transactionDao().insertTransaction(
                    TransactionEntity(
                        id            = cols[0],
                        type          = cols[1],
                        amount        = BigDecimal(cols[2]),
                        date          = LocalDate.parse(cols[3]),
                        fromAccountId = fromAccountId,
                        toAccountId   = toAccountId,
                        categoryId    = categoryId,
                        note          = cols[7].ifBlank { null },
                        createdAt     = Instant.ofEpochSecond(cols[8].toLong())
                    )
                )
                imported++
            }
        }

        return ImportResult(imported = imported, skipped = skipped)
    }

    // ── PREVIEW ───────────────────────────────────────────────────────────────

    fun previewJson(uri: Uri): ImportPreview {
        val envelope = readEnvelope(uri)
        validateEnvelope(envelope)
        return ImportPreview(
            accounts     = envelope.accounts.size,
            categories   = envelope.categories.size,
            budgets      = envelope.budgets.size,
            transactions = envelope.transactions.size
        )
    }

    suspend fun previewCsv(uri: Uri): ImportPreview {
        val existingAccountIds =
            database.accountDao().getAllOnce().map { it.id }.toSet()
        val existingCategoryIds =
            database.categoryDao().getAllOnce().map { it.id }.toSet()

        var total   = 0
        var valid   = 0
        val skipped = mutableMapOf<SkipReason, Int>()

        val stream = contentResolver.openInputStream(uri)
            ?: error("Unable to open CSV file")

        stream.bufferedReader().use { reader ->
            val lines = reader.readLines()
            if (lines.isEmpty()) error("Empty CSV file")

            lines.drop(1).forEach { line ->
                total++
                val cols = parseCsvLine(line)

                if (cols.size < 9) {
                    skipped[SkipReason.INVALID_FORMAT] =
                        (skipped[SkipReason.INVALID_FORMAT] ?: 0) + 1
                    return@forEach
                }

                val fromAccountId = cols[4].ifBlank { null }
                val toAccountId   = cols[5].ifBlank { null }
                val categoryId    = cols[6].ifBlank { null }

                val unknownAccount =
                    (fromAccountId != null && fromAccountId !in existingAccountIds) ||
                            (toAccountId   != null && toAccountId   !in existingAccountIds)

                val unknownCategory =
                    categoryId != null && categoryId !in existingCategoryIds

                when {
                    unknownAccount  -> skipped[SkipReason.UNKNOWN_ACCOUNT]  =
                        (skipped[SkipReason.UNKNOWN_ACCOUNT]  ?: 0) + 1
                    unknownCategory -> skipped[SkipReason.UNKNOWN_CATEGORY] =
                        (skipped[SkipReason.UNKNOWN_CATEGORY] ?: 0) + 1
                    else            -> valid++
                }
            }
        }

        return ImportPreview(
            totalRows       = total,
            validRows       = valid,
            skippedRows     = total - valid,
            skippedByReason = skipped
        )
    }

    // ── INTERNALS ─────────────────────────────────────────────────────────────

    private fun readEnvelope(uri: Uri): ExportEnvelope {
        val stream = contentResolver.openInputStream(uri)
            ?: error("Unable to open input stream")
        return InputStreamReader(stream).use { reader ->
            json.decodeFromString(ExportEnvelope.serializer(), reader.readText())
        }
    }

    private fun validateEnvelope(envelope: ExportEnvelope) {
        require(envelope.meta.app == "TraceLedger") {
            "Invalid backup file (app mismatch)"
        }
        require(envelope.meta.schemaVersion <= database.openHelper.readableDatabase.version) {
            "Backup schema is newer than this version of TraceLedger"
        }
    }

    /**
     * Wipe all tables in dependency order.
     * Transactions first (they reference accounts + categories via FK).
     * Budgets next (they reference categories).
     * Then categories and accounts.
     */
    private suspend fun wipeAll() {
        database.transactionDao().deleteAllTransactions()
        database.budgetDao().deleteAll()
        database.categoryDao().deleteAll()
        database.accountDao().deleteAll()
    }

    /**
     * Parse a single CSV line, handling quoted fields that may contain commas.
     * Example: 'hello,"world, with comma",test' → ["hello", "world, with comma", "test"]
     */
    private fun parseCsvLine(line: String): List<String> {
        val result  = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false

        for (c in line) {
            when {
                c == '"'              -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current = StringBuilder()
                }
                else                  -> current.append(c)
            }
        }
        result.add(current.toString())
        return result
    }
}