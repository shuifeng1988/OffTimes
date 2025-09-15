package com.offtime.app.data.dao

import androidx.room.*
import com.offtime.app.data.entity.AppInfoDefaultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppInfoDefaultDao {
    
    @Query("SELECT * FROM app_info_defaults WHERE isEnabled = 1 ORDER BY appName ASC")
    fun getAllDefaultApps(): Flow<List<AppInfoDefaultEntity>>
    
    @Query("SELECT * FROM app_info_defaults WHERE categoryId = :categoryId AND isEnabled = 1 ORDER BY appName ASC")
    fun getDefaultAppsByCategory(categoryId: Int): Flow<List<AppInfoDefaultEntity>>
    
    @Query("SELECT * FROM app_info_defaults WHERE packageName = :packageName")
    suspend fun getDefaultAppByPackageName(packageName: String): AppInfoDefaultEntity?
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaultApp(app: AppInfoDefaultEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaultApps(apps: List<AppInfoDefaultEntity>)
    
    @Update
    suspend fun updateDefaultApp(app: AppInfoDefaultEntity)
    
    @Query("DELETE FROM app_info_defaults WHERE packageName = :packageName")
    suspend fun deleteDefaultApp(packageName: String)
    
    @Query("DELETE FROM app_info_defaults")
    suspend fun deleteAllDefaultApps()
    
    @Query("SELECT COUNT(*) FROM app_info_defaults")
    suspend fun getDefaultAppCount(): Int
} 