package com.offtime.app.data.dao

import androidx.room.*
import com.offtime.app.data.entity.AppInfoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppInfoDao {
    
    @Query("SELECT * FROM app_info_Users WHERE isEnabled = 1 ORDER BY appName ASC")
    fun getAllApps(): Flow<List<AppInfoEntity>>
    
    @Query("SELECT * FROM app_info_Users WHERE isEnabled = 1 ORDER BY appName ASC")
    suspend fun getAllAppsList(): List<AppInfoEntity>
    
    @Query("SELECT * FROM app_info_Users WHERE isExcluded = 1 AND isEnabled = 1 ORDER BY isSystemApp DESC, appName ASC")
    suspend fun getExcludedApps(): List<AppInfoEntity>
    
    @Query("SELECT * FROM app_info_Users WHERE categoryId = :categoryId AND isEnabled = 1 ORDER BY appName ASC")
    fun getAppsByCategory(categoryId: Int): Flow<List<AppInfoEntity>>
    
    @Query("SELECT * FROM app_info_Users WHERE packageName = :packageName")
    suspend fun getAppByPackageName(packageName: String): AppInfoEntity?
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertApp(app: AppInfoEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertApps(apps: List<AppInfoEntity>)
    
    @Update
    suspend fun updateApp(app: AppInfoEntity)
    
    @Query("UPDATE app_info_Users SET categoryId = :categoryId, updatedAt = :updatedAt WHERE packageName = :packageName")
    suspend fun updateAppCategory(packageName: String, categoryId: Int, updatedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE app_info_Users SET isExcluded = :isExcluded, updatedAt = :updatedAt WHERE packageName = :packageName")
    suspend fun updateAppExcludeStatus(packageName: String, isExcluded: Boolean, updatedAt: Long = System.currentTimeMillis())
    
    @Query("DELETE FROM app_info_Users WHERE packageName = :packageName")
    suspend fun deleteApp(packageName: String)
    
    @Query("DELETE FROM app_info_Users")
    suspend fun deleteAllApps()
    
    @Query("SELECT COUNT(*) FROM app_info_Users")
    suspend fun getAppCount(): Int
    
    // 别名方法用于调试
    suspend fun getAppInfoCount(): Int = getAppCount()
    
    @Query("SELECT * FROM app_info_Users ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun getRecentApps(limit: Int): List<AppInfoEntity>
    
    @Query("SELECT * FROM app_info_Users WHERE isSystemApp = 0 AND isEnabled = 1")
    suspend fun getNonSystemApps(): List<AppInfoEntity>
}