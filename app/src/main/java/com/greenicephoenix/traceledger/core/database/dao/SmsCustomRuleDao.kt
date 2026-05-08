package com.greenicephoenix.traceledger.core.database.dao

import androidx.room.*
import com.greenicephoenix.traceledger.core.database.entity.SmsCustomRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsCustomRuleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: SmsCustomRuleEntity): Long

    @Update
    suspend fun update(rule: SmsCustomRuleEntity)

    @Delete
    suspend fun delete(rule: SmsCustomRuleEntity)

    @Query("SELECT * FROM sms_custom_rules ORDER BY priority DESC, createdAt ASC")
    fun observeAll(): Flow<List<SmsCustomRuleEntity>>

    /** The engine calls this on every SMS to get all active custom rules in priority order */
    @Query("SELECT * FROM sms_custom_rules WHERE isEnabled = 1 ORDER BY priority DESC")
    suspend fun getEnabledRulesSorted(): List<SmsCustomRuleEntity>
}