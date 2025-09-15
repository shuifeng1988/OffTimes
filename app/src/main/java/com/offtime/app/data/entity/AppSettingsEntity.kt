package com.offtime.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val id: Int = 1, // 单例设置，固定ID为1
    val defaultCategoryId: Int = 1, // 默认显示类别ID，默认为娱乐(1)
    val categoryRewardPunishmentEnabled: String = "", // JSON字符串，存储各类别的奖罚开关状态 {"1":true,"2":false,...}
    val widgetDisplayDays: Int = 30, // Widget显示天数，默认30天
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) 