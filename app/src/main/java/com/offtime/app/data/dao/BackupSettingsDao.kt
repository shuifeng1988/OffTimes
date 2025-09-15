package com.offtime.app.data.dao

import androidx.room.*
import com.offtime.app.data.entity.BackupSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BackupSettingsDao {
    
    /**
     * 获取备份设置（实时监听）
     */
    @Query("SELECT * FROM backup_settings WHERE id = 1")
    fun getBackupSettingsFlow(): Flow<BackupSettingsEntity?>
    
    /**
     * 获取备份设置（一次性）
     */
    @Query("SELECT * FROM backup_settings WHERE id = 1")
    suspend fun getBackupSettings(): BackupSettingsEntity?
    
    /**
     * 插入或更新备份设置
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBackupSettings(settings: BackupSettingsEntity)
    
    /**
     * 更新备份开关状态
     */
    @Query("UPDATE backup_settings SET backupEnabled = :enabled, updatedAt = :timestamp WHERE id = 1")
    suspend fun updateBackupEnabled(enabled: Boolean, timestamp: Long = System.currentTimeMillis())
    
    /**
     * 更新备份时间
     */
    @Query("UPDATE backup_settings SET backupTimeHour = :hour, backupTimeMinute = :minute, updatedAt = :timestamp WHERE id = 1")
    suspend fun updateBackupTime(hour: Int, minute: Int, timestamp: Long = System.currentTimeMillis())
    
    /**
     * 更新最后备份信息
     */
    @Query("""
        UPDATE backup_settings 
        SET lastBackupDate = :date, 
            lastBackupResult = :result, 
            lastBackupError = :error,
            totalBackupsCount = totalBackupsCount + 1,
            updatedAt = :timestamp 
        WHERE id = 1
    """)
    suspend fun updateLastBackupInfo(
        date: String, 
        result: String, 
        error: String? = null,
        timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * 检查是否开启了备份功能
     */
    @Query("SELECT backupEnabled FROM backup_settings WHERE id = 1")
    suspend fun isBackupEnabled(): Boolean?
    
    /**
     * 获取备份时间
     */
    @Query("SELECT backupTimeHour, backupTimeMinute FROM backup_settings WHERE id = 1")
    suspend fun getBackupTime(): BackupTime?
    
    /**
     * 获取最后备份日期
     */
    @Query("SELECT lastBackupDate FROM backup_settings WHERE id = 1")
    suspend fun getLastBackupDate(): String?
    
    /**
     * 重置备份统计信息
     */
    @Query("""
        UPDATE backup_settings 
        SET totalBackupsCount = 0,
            lastBackupDate = NULL,
            lastBackupResult = NULL,
            lastBackupError = NULL,
            updatedAt = :timestamp 
        WHERE id = 1
    """)
    suspend fun resetBackupStats(timestamp: Long = System.currentTimeMillis())
    
    /**
     * 创建默认备份设置（如果不存在）
     */
    suspend fun ensureDefaultSettings() {
        val existing = getBackupSettings()
        if (existing == null) {
            // 生成随机备份时间（1-6点之间）
            val randomHour = (1..6).random()
            val randomMinute = (0..59).random()
            
            upsertBackupSettings(
                BackupSettingsEntity(
                    backupTimeHour = randomHour,
                    backupTimeMinute = randomMinute
                )
            )
        }
    }
    
    /**
     * 删除所有备份设置（用于重置）
     */
    @Query("DELETE FROM backup_settings")
    suspend fun deleteAllBackupSettings()
}

/**
 * 备份时间数据类
 */
data class BackupTime(
    val backupTimeHour: Int,
    val backupTimeMinute: Int
) 