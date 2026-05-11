package com.greenicephoenix.traceledger.core.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration 12 → 13.
 * Adds isExclusionRule column to sms_custom_rules.
 * When true, any SMS from the matching sender is silently discarded —
 * useful for senders that only send spam/statements you never want to track.
 */
val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE sms_custom_rules ADD COLUMN isExclusionRule INTEGER NOT NULL DEFAULT 0"
        )
    }
}