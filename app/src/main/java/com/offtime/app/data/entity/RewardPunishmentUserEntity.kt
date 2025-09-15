package com.offtime.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reward_punishment_user")
data class RewardPunishmentUserEntity(
    @PrimaryKey
    val id: String, // date_catId
    val date: String, // yyyy-MM-dd
    val catId: Int,
    val isGoalMet: Int,    // 0/1
    val rewardDone: Int,   // 0/1 (保持兼容性)
    val punishDone: Int,   // 0/1 (保持兼容性)
    val rewardCompletionPercent: Int = 0,  // 奖励完成百分比 (0-100)
    val punishCompletionPercent: Int = 0,  // 惩罚完成百分比 (0-100)
    val updateTime: Long = System.currentTimeMillis()
) 