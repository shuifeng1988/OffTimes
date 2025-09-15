package com.offtime.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "summary_usage_month_user")
data class SummaryUsageMonthUserEntity(
    @PrimaryKey
    val id: String, // month_catId
    val month: String, // yyyy-MM
    val catId: Int,
    val avgDailySec: Int,
    val updateTime: Long = System.currentTimeMillis()
) 