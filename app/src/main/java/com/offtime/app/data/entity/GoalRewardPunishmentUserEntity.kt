package com.offtime.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals_reward_punishment_users")
data class GoalRewardPunishmentUserEntity(
    @PrimaryKey
    val catId: Int,
    val dailyGoalMin: Int,
    val goalTimeUnit: String = "分钟", // 时间单位：小时/分钟/秒
    val conditionType: Int, // 0 = ≤ 完成, 1 = ≥ 完成
    val rewardText: String,
    val rewardNumber: Int = 0, // 奖励数量
    val rewardUnit: String = "", // 奖励单位
    val rewardTimeUnit: String = "天", // 奖励时间单位：天/小时/分钟/秒（用于"每X时间"的频率）
    val punishText: String,
    val punishNumber: Int = 0, // 惩罚数量
    val punishUnit: String = "", // 惩罚单位（如：个、公里、组）
    val punishTimeUnit: String = "小时", // 惩罚时间单位：小时/分钟/秒（用于"每X时间"的频率）
    val updateTime: Long = System.currentTimeMillis()
) 