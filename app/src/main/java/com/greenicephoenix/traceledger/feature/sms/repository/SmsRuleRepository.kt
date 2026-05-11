package com.greenicephoenix.traceledger.feature.sms.repository

import com.greenicephoenix.traceledger.core.database.dao.SmsCustomRuleDao
import com.greenicephoenix.traceledger.core.database.entity.SmsCustomRuleEntity
import kotlinx.coroutines.flow.Flow

/**
 * Simple CRUD repository for the user's custom SMS rules.
 * The Rule Editor UI (Phase 2) will use this.
 */
class SmsRuleRepository(
    private val dao: SmsCustomRuleDao
) {
    fun observeRules(): Flow<List<SmsCustomRuleEntity>> = dao.observeAll()

    suspend fun addRule(rule: SmsCustomRuleEntity): Long = dao.insert(rule)

    suspend fun updateRule(rule: SmsCustomRuleEntity) = dao.update(rule)

    suspend fun deleteRule(rule: SmsCustomRuleEntity) = dao.delete(rule)

    /**
     * Insert if id == 0 (new rule), replace if id > 0 (editing existing rule).
     * The DAO uses OnConflictStrategy.REPLACE so both cases work with a single insert call.
     */
    suspend fun addOrUpdateRule(rule: SmsCustomRuleEntity) {
        dao.insert(rule)
    }
}