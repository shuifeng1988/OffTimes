package com.offtime.app.data.dao

import androidx.room.*
import com.offtime.app.data.entity.GoalRewardPunishmentUserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalRewardPunishmentUserDao {
    
    @Query("SELECT * FROM goals_reward_punishment_users ORDER BY catId ASC")
    fun getAllUserGoals(): Flow<List<GoalRewardPunishmentUserEntity>>
    
    @Query("SELECT * FROM goals_reward_punishment_users WHERE catId = :catId")
    suspend fun getUserGoalByCatId(catId: Int): GoalRewardPunishmentUserEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserGoal(goal: GoalRewardPunishmentUserEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserGoals(goals: List<GoalRewardPunishmentUserEntity>)
    
    @Update
    suspend fun updateUserGoal(goal: GoalRewardPunishmentUserEntity)
    
    @Query("DELETE FROM goals_reward_punishment_users WHERE catId = :catId")
    suspend fun deleteUserGoal(catId: Int)
    
    @Query("DELETE FROM goals_reward_punishment_users")
    suspend fun deleteAllUserGoals()
    
    @Query("SELECT COUNT(*) FROM goals_reward_punishment_users")
    suspend fun getUserGoalCount(): Int
    
    @Query("SELECT * FROM goals_reward_punishment_users ORDER BY catId ASC")
    suspend fun getAllUserGoalsList(): List<GoalRewardPunishmentUserEntity>
    
    /**
     * 获取在指定日期更新的目标奖惩设置（用于增量备份）
     */
    @Query("""
        SELECT * FROM goals_reward_punishment_users 
        WHERE date(updateTime/1000, 'unixepoch') = :date
        ORDER BY catId ASC
    """)
    suspend fun getGoalsUpdatedOnDate(date: String): List<GoalRewardPunishmentUserEntity>
} 