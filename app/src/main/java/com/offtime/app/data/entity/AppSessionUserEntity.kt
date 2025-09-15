package com.offtime.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_sessions_users")
data class AppSessionUserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val catId: Int,
    val pkgName: String,
    val date: String,
    val startTime: Long,
    val endTime: Long,
    val durationSec: Int,
    val isOffline: Int = 0,
    val updateTime: Long = System.currentTimeMillis()
) 