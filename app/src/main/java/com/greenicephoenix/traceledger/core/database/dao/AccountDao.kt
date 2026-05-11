package com.greenicephoenix.traceledger.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.greenicephoenix.traceledger.core.database.entity.AccountEntity
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

@Dao
interface AccountDao {

    @Query("SELECT * FROM accounts")
    fun observeAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :accountId LIMIT 1")
    suspend fun getAccountById(accountId: String): AccountEntity?

//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun upsertAccount(account: AccountEntity)

    @Upsert
    suspend fun upsertAccount(account: AccountEntity)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE id = :accountId")
    suspend fun deleteAccount(accountId: String)

    @Query("DELETE FROM accounts")
    suspend fun deleteAllAccounts()

    /**
     * Adjust account balance by a delta amount.
     *
     * Positive delta  -> increases balance
     * Negative delta  -> decreases balance
     *
     * This is an atomic SQL operation.
     */
    @Query(
        """
        UPDATE accounts
        SET balance = balance + :delta
        WHERE id = :accountId
        """
    )
    suspend fun updateBalanceByDelta(
        accountId: String,
        delta: BigDecimal
    )

    @Query("SELECT * FROM accounts")
    suspend fun getAllOnce(): List<AccountEntity>

    @Insert
    suspend fun insert(entity: AccountEntity)

    @Query("DELETE FROM accounts")
    suspend fun deleteAll()

    /**
     * Finds the first account whose name contains [fragment] (case-insensitive)
     * AND matches the given [type] (AccountType.name string).
     * Used by SmsQueueRepository for bank-name → account type matching.
     * Example: findByNameContainingAndType("hdfc", "CREDIT_CARD")
     */
    @Query("""
        SELECT * FROM accounts
        WHERE LOWER(name) LIKE '%' || LOWER(:fragment) || '%'
        AND type = :type
        LIMIT 1
    """)
    suspend fun findByNameContainingAndType(fragment: String, type: String): AccountEntity?

    /**
     * Fallback: finds any account whose name contains [fragment], ignoring type.
     * Used when no account of the expected type is found.
     */
    @Query("""
        SELECT * FROM accounts
        WHERE LOWER(name) LIKE '%' || LOWER(:fragment) || '%'
        LIMIT 1
    """)
    suspend fun findByNameContaining(fragment: String): AccountEntity?
}