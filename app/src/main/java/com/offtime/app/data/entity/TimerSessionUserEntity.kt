package com.offtime.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "timer_sessions_users")
data class TimerSessionUserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val catId: Int,
    val programName: String = "线下活动",
    val date: String, // yyyy-MM-dd
    val startTime: Long, // UNIX ms
    val endTime: Long, // UNIX ms
    val durationSec: Int, // (end - start)/1000
    val isOffline: Int = 1,
    val updateTime: Long = System.currentTimeMillis()
) 