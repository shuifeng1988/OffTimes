package com.offtime.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reward_punishment_month_user")
data class RewardPunishmentMonthUserEntity(
    @PrimaryKey
    val id: String, // yearMonth_catId (例如: 2024-01_1)
    val yearMonth: String, // yyyy-MM
    val catId: Int,
    val totalRewardCount: Int, // 本月应获得奖励次数
    val totalPunishCount: Int, // 本月应获得惩罚次数
    val doneRewardCount: Int,  // 本月已执行奖励次数
    val donePunishCount: Int,  // 本月已执行惩罚次数
    val updateTime: Long = System.currentTimeMillis()
) 