package com.greenicephoenix.traceledger.core.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration 13 → 14.
 *
 * WHY drop+recreate instead of ALTER:
 * SQLite does not support changing column types (no ALTER COLUMN).
 * defaultCategoryId and defaultAccountId were incorrectly typed as INTEGER
 * but must be TEXT to hold String UUIDs (the app-wide ID convention).
 * No user has custom rules yet, so dropping is safe.
 *
 * isExclusionRule is included in the recreated schema
 * (was added in Migration_12_13 and is preserved here).
 */
val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE IF EXISTS `sms_custom_rules`")
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `sms_custom_rules` (
                `id`                INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name`              TEXT    NOT NULL,
                `senderPattern`     TEXT    NOT NULL,
                `amountPrefix`      TEXT    NOT NULL DEFAULT '',
                `debitKeywords`     TEXT    NOT NULL DEFAULT '',
                `creditKeywords`    TEXT    NOT NULL DEFAULT '',
                `merchantRegex`     TEXT    NOT NULL DEFAULT '',
                `defaultCategoryId` TEXT,
                `defaultAccountId`  TEXT,
                `isEnabled`         INTEGER NOT NULL DEFAULT 1,
                `priority`          INTEGER NOT NULL DEFAULT 10,
                `isAdvancedMode`    INTEGER NOT NULL DEFAULT 0,
                `rawRegex`          TEXT    NOT NULL DEFAULT '',
                `isExclusionRule`   INTEGER NOT NULL DEFAULT 0,
                `createdAt`         INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}