package com.greenicephoenix.traceledger.core.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Migration from schema version 9 → 10
// Adds the isActive column to recurring_transactions table,
// which enables pause/resume functionality for recurring rules.
// DEFAULT 1 means all existing recurring rules are active after migration.
val MIGRATION_9_10 = object : Migration(9, 10) { // FIX: was incorrectly (8, 9)
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            ALTER TABLE recurring_transactions
            ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1
            """.trimIndent()
        )
    }
}