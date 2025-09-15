package com.offtime.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_sessions_defaults")
data class AppSessionDefaultEntity(
    @PrimaryKey
    val catId: Int,
    val pkgName: String = "",
    val date: String = "",
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val durationSec: Int = 0,
    val isOffline: Int = 0,
    val updateTime: Long = System.currentTimeMillis()
) 