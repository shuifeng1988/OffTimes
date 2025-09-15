package com.offtime.app.data.dao

import androidx.room.*
import com.offtime.app.data.entity.RewardPunishmentUserEntity

@Dao
interface RewardPunishmentUserDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RewardPunishmentUserEntity)

    @Query("SELECT * FROM reward_punishment_user WHERE catId = :catId AND date = :date")
    suspend fun getRecordByCategoryAndDate(catId: Int, date: String): RewardPunishmentUserEntity?

    @Query("SELECT * FROM reward_punishment_user WHERE date = :date")
    suspend fun getRecordsByDate(date: String): List<RewardPunishmentUserEntity>

    @Query("UPDATE reward_punishment_user SET rewardDone = :done WHERE catId = :catId AND date = :date")
    suspend fun markRewardDone(catId: Int, date: String, done: Int)

    @Query("UPDATE reward_punishment_user SET punishDone = :done WHERE catId = :catId AND date = :date")
    suspend fun markPunishmentDone(catId: Int, date: String, done: Int)
    
    @Query("UPDATE reward_punishment_user SET rewardCompletionPercent = :percent, rewardDone = :done WHERE catId = :catId AND date = :date")
    suspend fun updateRewardCompletion(catId: Int, date: String, percent: Int, done: Int = if (percent > 0) 1 else 0)
    
    @Query("UPDATE reward_punishment_user SET punishCompletionPercent = :percent, punishDone = :done WHERE catId = :catId AND date = :date")
    suspend fun updatePunishmentCompletion(catId: Int, date: String, percent: Int, done: Int = if (percent > 0) 1 else 0)

    @Query("SELECT * FROM reward_punishment_user ORDER BY date DESC, catId ASC")
    suspend fun getAllRecords(): List<RewardPunishmentUserEntity>

    // 获取记录总数（用于迁移验证）
    @Query("SELECT COUNT(*) FROM reward_punishment_user")
    suspend fun getTotalCount(): Int

    // 调试功能：清除所有奖惩记录
    @Query("DELETE FROM reward_punishment_user")
    suspend fun deleteAllRecords()
    
    // 别名方法用于调试
    suspend fun deleteAll() = deleteAllRecords()

    // 注意：reward_punishment_user表为用户表，数据永久保存，不提供删除方法（除了调试）

    // 别名方法，用于数据迁移
    suspend fun getByDateAndCategory(catId: Int, date: String): RewardPunishmentUserEntity? {
        return getRecordByCategoryAndDate(catId, date)
    }

    // 别名方法，用于数据迁移
    suspend fun markPunishDone(catId: Int, date: String, done: Int) {
        markPunishmentDone(catId, date, done)
    }
} 