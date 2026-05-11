package com.greenicephoenix.traceledger.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.greenicephoenix.traceledger.core.database.converters.RoomConverters
import com.greenicephoenix.traceledger.core.database.dao.AccountDao
import com.greenicephoenix.traceledger.core.database.dao.TransactionDao
import com.greenicephoenix.traceledger.core.database.dao.CategoryDao
import com.greenicephoenix.traceledger.core.database.entity.AccountEntity
import com.greenicephoenix.traceledger.core.database.entity.TransactionEntity
import com.greenicephoenix.traceledger.core.database.entity.CategoryEntity
import com.greenicephoenix.traceledger.core.database.entity.RecurringTransactionEntity
import com.greenicephoenix.traceledger.feature.budgets.data.BudgetDao
import com.greenicephoenix.traceledger.feature.budgets.data.BudgetEntity
import com.greenicephoenix.traceledger.BuildConfig
import com.greenicephoenix.traceledger.core.database.dao.RecurringTransactionDao
import com.greenicephoenix.traceledger.feature.templates.data.TransactionTemplateDao
import com.greenicephoenix.traceledger.feature.templates.data.TransactionTemplateEntity
import com.greenicephoenix.traceledger.core.database.migrations.*
import com.greenicephoenix.traceledger.core.database.dao.SmsPendingTransactionDao
import com.greenicephoenix.traceledger.core.database.dao.SmsCustomRuleDao
import com.greenicephoenix.traceledger.core.database.entity.SmsPendingTransactionEntity
import com.greenicephoenix.traceledger.core.database.entity.SmsCustomRuleEntity

@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        CategoryEntity::class,
        RecurringTransactionEntity::class,
        TransactionTemplateEntity::class,
        SmsPendingTransactionEntity::class,   // ← NEW
        SmsCustomRuleEntity::class,
    ],
    version = 14,
    exportSchema = true
)
@TypeConverters(RoomConverters::class)
abstract class TraceLedgerDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun categoryDao(): CategoryDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun transactionTemplateDao(): TransactionTemplateDao
    abstract fun smsPendingTransactionDao(): SmsPendingTransactionDao
    abstract fun smsCustomRuleDao(): SmsCustomRuleDao

    companion object {

        @Volatile
        private var INSTANCE: TraceLedgerDatabase? = null

        fun getInstance(context: Context): TraceLedgerDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also {
                    INSTANCE = it
                }
            }
        }

        private fun buildDatabase(context: Context): TraceLedgerDatabase {
            val builder =
                Room.databaseBuilder(
                    context.applicationContext,
                    TraceLedgerDatabase::class.java,
                    "traceledger.db"
                )
                    .addMigrations(
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11,
                        MIGRATION_11_12,
                        MIGRATION_12_13,
                        MIGRATION_13_14
                    )

            if (BuildConfig.DEBUG) {
                // ✅ Debug-only safety valve
                builder.fallbackToDestructiveMigration()
            }

            return builder.build()
        }
    }
}