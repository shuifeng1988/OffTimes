package com.offtime.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "backup_settings")
data class BackupSettingsEntity(
    @PrimaryKey
    val id: Int = 1, // 单用户设备，只需要一条记录
    val backupEnabled: Boolean = true, // 是否开启自动备份
    val backupTimeHour: Int = 2, // 备份时间-小时 (0-23)
    val backupTimeMinute: Int = 0, // 备份时间-分钟 (0-59)
    val lastBackupDate: String? = null, // 最后一次备份日期 (yyyy-MM-dd)
    val totalBackupsCount: Int = 0, // 总备份次数
    val lastBackupResult: String? = null, // 最后一次备份结果 (SUCCESS/FAILED/PARTIAL)
    val lastBackupError: String? = null, // 最后一次备份错误信息
    val wifiOnlyBackup: Boolean = true, // 仅在WiFi环境下备份
    val autoDeleteOldBackups: Boolean = true, // 自动删除旧备份（服务器端）
    val maxBackupDays: Int = 90, // 最大保留天数
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) 