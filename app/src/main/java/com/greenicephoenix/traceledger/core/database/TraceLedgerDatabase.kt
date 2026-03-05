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
import com.greenicephoenix.traceledger.core.database.migrations.*

@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        CategoryEntity::class,
        RecurringTransactionEntity::class // NEW
    ],
    version = 10,
    exportSchema = true
)
@TypeConverters(RoomConverters::class)
abstract class TraceLedgerDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun categoryDao(): CategoryDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao

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
                        MIGRATION_9_10
                    )

            if (BuildConfig.DEBUG) {
                // ✅ Debug-only safety valve
                builder.fallbackToDestructiveMigration()
            }

            return builder.build()
        }
    }
}