package com.offtime.app.data.dao

import androidx.room.*
import com.offtime.app.data.entity.AppSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSettingsDao {
    
    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun getSettings(): AppSettingsEntity?
    
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun getSettingsFlow(): Flow<AppSettingsEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: AppSettingsEntity)
    
    @Query("UPDATE app_settings SET defaultCategoryId = :categoryId, updatedAt = :updatedAt WHERE id = 1")
    suspend fun updateDefaultCategory(categoryId: Int, updatedAt: Long = System.currentTimeMillis())
    
    @Query("UPDATE app_settings SET categoryRewardPunishmentEnabled = :enabledJson, updatedAt = :updatedAt WHERE id = 1")
    suspend fun updateCategoryRewardPunishmentEnabled(enabledJson: String, updatedAt: Long = System.currentTimeMillis())
    
    @Query("DELETE FROM app_settings")
    suspend fun deleteAllSettings()
} 