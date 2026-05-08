package com.greenicephoenix.traceledger.core.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * DB migration: version 10 → 11
 * Adds the transaction_templates table.
 * amount is stored as TEXT (nullable) so no Room type converter is needed here.
 */
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS transaction_templates (
                id            TEXT NOT NULL PRIMARY KEY,
                name          TEXT NOT NULL,
                type          TEXT NOT NULL,
                amount        TEXT,
                fromAccountId TEXT,
                toAccountId   TEXT,
                categoryId    TEXT,
                notes         TEXT,
                createdAt     INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}