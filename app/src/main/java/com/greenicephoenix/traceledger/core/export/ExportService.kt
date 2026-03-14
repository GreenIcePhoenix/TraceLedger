package com.greenicephoenix.traceledger.core.export

import android.content.ContentResolver
import android.net.Uri
import com.greenicephoenix.traceledger.BuildConfig
import com.greenicephoenix.traceledger.core.database.TraceLedgerDatabase
import kotlinx.serialization.json.Json
import java.io.OutputStreamWriter
import java.time.Instant
import java.time.format.DateTimeFormatter

enum class ExportFormat { JSON, CSV }

class ExportService(
    private val database: TraceLedgerDatabase,
    private val contentResolver: ContentResolver
) {

    private val json = Json {
        prettyPrint    = true
        encodeDefaults = true
        explicitNulls  = false
    }

    suspend fun export(format: ExportFormat, uri: Uri) {
        when (format) {
            ExportFormat.JSON -> exportJson(uri)
            ExportFormat.CSV  -> exportCsv(uri)
        }
    }

    // ── JSON EXPORT ───────────────────────────────────────────────────────────

    /**
     * Full backup export — includes accounts, categories, budgets, transactions.
     * Designed for re-import into TraceLedger. Preserves all IDs.
     */
    private suspend fun exportJson(uri: Uri) {
        val accounts     = database.accountDao().getAllOnce()
        val categories   = database.categoryDao().getAllOnce()
        val budgets      = database.budgetDao().getAllOnce()
        val transactions = database.transactionDao().getAllOnce()

        val envelope = ExportEnvelope(
            meta = ExportMeta(
                app            = "TraceLedger",
                appVersion     = BuildConfig.VERSION_NAME,
                schemaVersion  = database.openHelper.readableDatabase.version,
                exportedAtIso  = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
            ),
            accounts     = accounts.map     { it.toExport() },
            categories   = categories.map   { it.toExport() },
            budgets      = budgets.map      { it.toExport() },
            transactions = transactions.map { it.toExport() }
        )

        contentResolver.openOutputStream(uri)?.use { os ->
            OutputStreamWriter(os).use { writer ->
                writer.write(json.encodeToString(ExportEnvelope.serializer(), envelope))
            }
        } ?: error("Unable to open output stream for JSON export")
    }

    // ── CSV EXPORT ────────────────────────────────────────────────────────────

    /**
     * Human-readable CSV export of all transactions.
     *
     * FIX #17: Previously exported raw UUIDs for fromAccountId, toAccountId,
     * categoryId — completely unreadable when opened in Excel or Google Sheets.
     *
     * Now resolves names:
     *   fromAccountId → fromAccountName  (e.g. "HDFC Savings")
     *   toAccountId   → toAccountName    (e.g. "Cash Wallet")
     *   categoryId    → categoryName     (e.g. "Food & Dining")
     *
     * The raw IDs are kept as separate columns so the file can still be
     * used for re-import if needed.
     *
     * Columns:
     *   date, type, amount, category, fromAccount, toAccount, note,
     *   categoryId, fromAccountId, toAccountId, id
     */
    private suspend fun exportCsv(uri: Uri) {
        val transactions = database.transactionDao().getAllOnce()

        // Build lookup maps — O(n) lookups instead of O(n²) DB queries
        val accountMap  = database.accountDao().getAllOnce()
            .associate { it.id to it.name }
        val categoryMap = database.categoryDao().getAllOnce()
            .associate { it.id to it.name }

        contentResolver.openOutputStream(uri)?.use { os ->
            OutputStreamWriter(os).use { w ->

                // Header row — human-readable column names first, IDs at the end
                w.appendLine(
                    "date,type,amount,category,fromAccount,toAccount,note," +
                            "categoryId,fromAccountId,toAccountId,id"
                )

                transactions.forEach { t ->
                    val categoryName    = t.categoryId?.let    { categoryMap[it] } ?: ""
                    val fromAccountName = t.fromAccountId?.let { accountMap[it]  } ?: ""
                    val toAccountName   = t.toAccountId?.let   { accountMap[it]  } ?: ""

                    w.appendLine(
                        listOf(
                            t.date.toString(),                          // date
                            t.type,                                     // type
                            t.amount.toPlainString(),                   // amount
                            categoryName,                               // category (human)
                            fromAccountName,                            // fromAccount (human)
                            toAccountName,                              // toAccount (human)
                            (t.note ?: "").replace("\n", " "),          // note
                            t.categoryId    ?: "",                      // categoryId (for re-import)
                            t.fromAccountId ?: "",                      // fromAccountId (for re-import)
                            t.toAccountId   ?: "",                      // toAccountId (for re-import)
                            t.id                                        // transaction id
                        ).joinToString(",") { escapeCsv(it) }
                    )
                }
            }
        } ?: error("Unable to open output stream for CSV export")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Properly escape a CSV field.
    // Fields containing commas or double-quotes are wrapped in double-quotes.
    // Any existing double-quotes inside the value are doubled ("").
    // ─────────────────────────────────────────────────────────────────────────
    private fun escapeCsv(value: String): String {
        val needsQuotes = value.contains(",") || value.contains("\"") || value.contains("\n")
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuotes) "\"$escaped\"" else escaped
    }
}