package com.greenicephoenix.traceledger.core.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from DB version 11 → 12.
 *
 * Adds two new tables:
 *  1. sms_pending_transactions  — queues parsed SMS transactions awaiting user review
 *  2. sms_custom_rules          — stores user-defined SMS parsing rules
 *
 * WHY two tables instead of one?
 * The queue is transient (rows get deleted after review).
 * The rules are permanent user data. Keeping them separate makes queries and
 * lifecycle management clean.
 */
val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {

        // --- TABLE 1: SMS pending transaction queue ---
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `sms_pending_transactions` (
                `id`                  INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `smsId`               INTEGER NOT NULL DEFAULT -1,
                `smsBody`             TEXT    NOT NULL,
                `sender`              TEXT    NOT NULL,
                `receivedAt`          INTEGER NOT NULL,
                `parsedAmount`        REAL    NOT NULL,
                `parsedDescription`   TEXT    NOT NULL,
                `parsedType`          TEXT    NOT NULL,
                `parsedDate`          INTEGER NOT NULL,
                `suggestedCategoryId` TEXT,
                `suggestedAccountId`  TEXT,
                `accountLastFour`     TEXT,
                `isProcessed`         INTEGER NOT NULL DEFAULT 0,
                `isAccepted`          INTEGER NOT NULL DEFAULT 0,
                `contentHash`         TEXT    NOT NULL DEFAULT '',
                `createdAt`           INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // --- TABLE 2: Custom SMS parsing rules ---
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `sms_custom_rules` (
                `id`                INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name`              TEXT    NOT NULL,
                `senderPattern`     TEXT    NOT NULL,
                `amountPrefix`      TEXT    NOT NULL DEFAULT '',
                `debitKeywords`     TEXT    NOT NULL DEFAULT '',
                `creditKeywords`    TEXT    NOT NULL DEFAULT '',
                `merchantRegex`     TEXT    NOT NULL DEFAULT '',
                `defaultCategoryId` INTEGER,
                `defaultAccountId`  INTEGER,
                `isEnabled`         INTEGER NOT NULL DEFAULT 1,
                `priority`          INTEGER NOT NULL DEFAULT 10,
                `isAdvancedMode`    INTEGER NOT NULL DEFAULT 0,
                `rawRegex`          TEXT    NOT NULL DEFAULT '',
                `createdAt`         INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}