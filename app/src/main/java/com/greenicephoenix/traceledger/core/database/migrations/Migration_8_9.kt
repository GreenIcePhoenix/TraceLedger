package com.greenicephoenix.traceledger.core.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS recurring_transactions (
                id TEXT NOT NULL,
                type TEXT NOT NULL,
                amount TEXT NOT NULL,
                fromAccountId TEXT,
                toAccountId TEXT,
                categoryId TEXT,
                note TEXT,
                startDate TEXT NOT NULL,
                endDate TEXT,
                frequency TEXT NOT NULL,
                lastGeneratedDate TEXT,
                PRIMARY KEY(id)
            )
            """
        )

        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_recurring_transactions_frequency ON recurring_transactions(frequency)"
        )

        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_recurring_transactions_startDate ON recurring_transactions(startDate)"
        )

        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_recurring_transactions_endDate ON recurring_transactions(endDate)"
        )
    }
}