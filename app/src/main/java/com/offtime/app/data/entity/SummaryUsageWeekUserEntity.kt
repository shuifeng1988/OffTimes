package com.offtime.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "summary_usage_week_user")
data class SummaryUsageWeekUserEntity(
    @PrimaryKey
    val id: String, // weekStart_catId
    val weekStartDate: String, // yyyy-MM-dd (周一)
    val catId: Int,
    val avgDailySec: Int,
    val updateTime: Long = System.currentTimeMillis()
) 