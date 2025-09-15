package com.offtime.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_info_Users")
data class AppInfoEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val isSystemApp: Boolean,
    val categoryId: Int = 1, // 默认分类为娱乐
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val isEnabled: Boolean = true,
    val isExcluded: Boolean = false, // 是否排除统计
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) 