package com.offtime.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 每日小时级汇总：线上 / 线下各自一条。
 * 复合主键 (date, catId, slotIndex, isOffline) 用 @PrimaryKey 无法表达，使用手动 id = hash。
 */
@Entity(tableName = "daily_usage_user")
data class DailyUsageUserEntity(
    @PrimaryKey
    val id: String, // date_catId_slot_isOffline 作为唯一键
    val date: String,            // yyyy-MM-dd
    val catId: Int,
    val slotIndex: Int,          // 0-23
    val isOffline: Int,          // 0=线上 1=线下
    val durationSec: Int,
    val updateTime: Long = System.currentTimeMillis()
) 