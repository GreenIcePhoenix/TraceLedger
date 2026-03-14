package com.greenicephoenix.traceledger.core.repository

import android.database.sqlite.SQLiteConstraintException
import com.greenicephoenix.traceledger.core.database.dao.AccountDao
import com.greenicephoenix.traceledger.core.database.entity.AccountEntity
import com.greenicephoenix.traceledger.domain.model.AccountType
import com.greenicephoenix.traceledger.domain.model.AccountUiModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AccountRepository(private val accountDao: AccountDao) {

    fun observeAccounts(): Flow<List<AccountUiModel>> =
        accountDao.observeAllAccounts().map { it.map { e -> e.toUiModel() } }

    suspend fun upsert(account: AccountUiModel) =
        accountDao.upsertAccount(account.toEntity())

    /**
     * Delete an account by ID.
     *
     * Returns Result.success(Unit) if deleted.
     * Returns Result.failure with a readable message if Room's FK constraint
     * blocked deletion (i.e. transactions reference this account).
     *
     * The caller (ViewModel) surfaces this to the UI as an error state.
     */
    suspend fun delete(accountId: String): Result<Unit> {
        return try {
            accountDao.deleteAccount(accountId)
            Result.success(Unit)
        } catch (e: SQLiteConstraintException) {
            Result.failure(
                Exception("This account has transactions linked to it and cannot be deleted.")
            )
        }
    }

    private fun AccountEntity.toUiModel() = AccountUiModel(
        id             = id,
        name           = name,
        balance        = balance,
        type           = AccountType.valueOf(type),
        includeInTotal = includeInTotal,
        details        = details,
        color          = color
    )

    private fun AccountUiModel.toEntity() = AccountEntity(
        id             = id,
        name           = name,
        balance        = balance,
        type           = type.name,
        includeInTotal = includeInTotal,
        details        = details,
        color          = color
    )
}