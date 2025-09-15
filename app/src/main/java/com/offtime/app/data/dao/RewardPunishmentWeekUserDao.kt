package com.offtime.app.data.dao

import androidx.room.*
import com.offtime.app.data.entity.RewardPunishmentWeekUserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RewardPunishmentWeekUserDao {
    
    @Query("SELECT * FROM reward_punishment_week_user ORDER BY weekStart DESC")
    suspend fun getAllRecords(): List<RewardPunishmentWeekUserEntity>
    
    @Query("SELECT * FROM reward_punishment_week_user WHERE catId = :catId ORDER BY weekStart DESC")
    suspend fun getRecordsByCategory(catId: Int): List<RewardPunishmentWeekUserEntity>
    
    @Query("SELECT * FROM reward_punishment_week_user WHERE catId = :catId AND weekStart = :weekStart")
    suspend fun getRecordByCategoryAndWeek(catId: Int, weekStart: String): RewardPunishmentWeekUserEntity?
    
    @Query("SELECT * FROM reward_punishment_week_user WHERE weekStart >= :startWeek AND weekStart <= :endWeek ORDER BY weekStart DESC")
    suspend fun getRecordsByWeekRange(startWeek: String, endWeek: String): List<RewardPunishmentWeekUserEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: RewardPunishmentWeekUserEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(records: List<RewardPunishmentWeekUserEntity>)
    
    @Update
    suspend fun update(record: RewardPunishmentWeekUserEntity)
    
    @Delete
    suspend fun delete(record: RewardPunishmentWeekUserEntity)
    
    @Query("DELETE FROM reward_punishment_week_user")
    suspend fun deleteAll()
} 