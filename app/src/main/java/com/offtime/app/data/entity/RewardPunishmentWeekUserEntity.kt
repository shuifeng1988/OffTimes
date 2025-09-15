package com.offtime.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reward_punishment_week_user")
data class RewardPunishmentWeekUserEntity(
    @PrimaryKey
    val id: String, // weekStart_catId (例如: 2024-01-01_1)
    val weekStart: String, // yyyy-MM-dd (周一日期)
    val weekEnd: String, // yyyy-MM-dd (周日日期)
    val catId: Int,
    val totalRewardCount: Int, // 本周应获得奖励次数
    val totalPunishCount: Int, // 本周应获得惩罚次数
    val doneRewardCount: Int,  // 本周已执行奖励次数
    val donePunishCount: Int,  // 本周已执行惩罚次数
    val updateTime: Long = System.currentTimeMillis()
) 