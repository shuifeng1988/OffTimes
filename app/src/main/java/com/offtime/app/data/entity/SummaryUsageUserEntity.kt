package com.offtime.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "summary_usage_user")
data class SummaryUsageUserEntity(
    @PrimaryKey
    val id: String, // date_catId
    val date: String, // yyyy-MM-dd
    val catId: Int,
    val totalSec: Int,
    val updateTime: Long = System.currentTimeMillis()
) 