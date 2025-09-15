package com.offtime.app.data.dao

import androidx.room.*
import com.offtime.app.data.entity.RewardPunishmentMonthUserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RewardPunishmentMonthUserDao {
    
    @Query("SELECT * FROM reward_punishment_month_user ORDER BY yearMonth DESC")
    suspend fun getAllRecords(): List<RewardPunishmentMonthUserEntity>
    
    @Query("SELECT * FROM reward_punishment_month_user WHERE catId = :catId ORDER BY yearMonth DESC")
    suspend fun getRecordsByCategory(catId: Int): List<RewardPunishmentMonthUserEntity>
    
    @Query("SELECT * FROM reward_punishment_month_user WHERE catId = :catId AND yearMonth = :yearMonth")
    suspend fun getRecordByCategoryAndMonth(catId: Int, yearMonth: String): RewardPunishmentMonthUserEntity?
    
    @Query("SELECT * FROM reward_punishment_month_user WHERE yearMonth >= :startMonth AND yearMonth <= :endMonth ORDER BY yearMonth DESC")
    suspend fun getRecordsByMonthRange(startMonth: String, endMonth: String): List<RewardPunishmentMonthUserEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: RewardPunishmentMonthUserEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(records: List<RewardPunishmentMonthUserEntity>)
    
    @Update
    suspend fun update(record: RewardPunishmentMonthUserEntity)
    
    @Delete
    suspend fun delete(record: RewardPunishmentMonthUserEntity)
    
    @Query("DELETE FROM reward_punishment_month_user")
    suspend fun deleteAll()
} 