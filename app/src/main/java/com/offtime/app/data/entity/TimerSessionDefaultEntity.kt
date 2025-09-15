package com.offtime.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "timer_sessions_defaults")
data class TimerSessionDefaultEntity(
    @PrimaryKey
    val catId: Int,
    val programName: String = "线下活动",
    val date: String = "",
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val durationSec: Int = 0,
    val isOffline: Int = 1,
    val updateTime: Long = System.currentTimeMillis()
) 