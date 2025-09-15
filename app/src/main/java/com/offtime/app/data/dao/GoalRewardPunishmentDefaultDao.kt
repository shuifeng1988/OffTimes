package com.offtime.app.data.dao

import androidx.room.*
import com.offtime.app.data.entity.GoalRewardPunishmentDefaultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalRewardPunishmentDefaultDao {
    
    @Query("SELECT * FROM goals_reward_punishment_defaults ORDER BY catId ASC")
    fun getAllDefaultGoals(): Flow<List<GoalRewardPunishmentDefaultEntity>>
    
    @Query("SELECT * FROM goals_reward_punishment_defaults ORDER BY catId ASC")
    suspend fun getAllDefaultGoalsList(): List<GoalRewardPunishmentDefaultEntity>
    
    @Query("SELECT * FROM goals_reward_punishment_defaults WHERE catId = :catId")
    suspend fun getDefaultGoalByCatId(catId: Int): GoalRewardPunishmentDefaultEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDefaultGoal(goal: GoalRewardPunishmentDefaultEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDefaultGoals(goals: List<GoalRewardPunishmentDefaultEntity>)
    
    @Update
    suspend fun updateDefaultGoal(goal: GoalRewardPunishmentDefaultEntity)
    
    @Query("DELETE FROM goals_reward_punishment_defaults WHERE catId = :catId")
    suspend fun deleteDefaultGoal(catId: Int)
    
    @Query("DELETE FROM goals_reward_punishment_defaults")
    suspend fun deleteAllDefaultGoals()
    
    @Query("SELECT COUNT(*) FROM goals_reward_punishment_defaults")
    suspend fun getDefaultGoalCount(): Int
} 