package com.offtime.app.data.repository

import com.offtime.app.data.dao.AppSettingsDao
import com.offtime.app.data.entity.AppSettingsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSettingsRepository @Inject constructor(
    private val appSettingsDao: AppSettingsDao
) {
    
    /**
     * 获取设置Flow
     */
    fun getSettingsFlow(): Flow<AppSettingsEntity?> = appSettingsDao.getSettingsFlow()
    
    /**
     * 获取设置
     */
    suspend fun getSettings(): AppSettingsEntity {
        return appSettingsDao.getSettings() ?: AppSettingsEntity()
    }
    
    /**
     * 获取默认显示类别ID
     */
    suspend fun getDefaultCategoryId(): Int {
        val settings = getSettings()
        return settings.defaultCategoryId
    }
    
    /**
     * 设置默认显示类别
     */
    suspend fun setDefaultCategoryId(categoryId: Int) {
        val currentSettings = getSettings()
        val updatedSettings = currentSettings.copy(
            defaultCategoryId = categoryId,
            updatedAt = System.currentTimeMillis()
        )
        appSettingsDao.insertOrUpdateSettings(updatedSettings)
    }
    
    /**
     * 获取Widget显示天数
     */
    suspend fun getWidgetDisplayDays(): Int {
        val settings = getSettings()
        return settings.widgetDisplayDays
    }
    
    /**
     * 设置Widget显示天数
     */
    suspend fun setWidgetDisplayDays(days: Int) {
        val currentSettings = getSettings()
        val updatedSettings = currentSettings.copy(
            widgetDisplayDays = days,
            updatedAt = System.currentTimeMillis()
        )
        appSettingsDao.insertOrUpdateSettings(updatedSettings)
    }
    
    /**
     * 获取类别奖罚开关状态
     */
    suspend fun getCategoryRewardPunishmentEnabled(): Map<Int, Boolean> {
        val settings = getSettings()
        return if (settings.categoryRewardPunishmentEnabled.isNotEmpty()) {
            try {
                val json = JSONObject(settings.categoryRewardPunishmentEnabled)
                val result = mutableMapOf<Int, Boolean>()
                json.keys().forEach { key ->
                    result[key.toInt()] = json.getBoolean(key)
                }
                result
            } catch (e: Exception) {
                android.util.Log.e("AppSettingsRepository", "解析类别奖罚开关失败: ${e.message}", e)
                emptyMap()
            }
        } else {
            emptyMap()
        }
    }
    
    /**
     * 检查指定类别的奖罚是否开启
     */
    suspend fun isCategoryRewardPunishmentEnabled(categoryId: Int): Boolean {
        val enabledMap = getCategoryRewardPunishmentEnabled()
        return enabledMap[categoryId] ?: true // 默认开启
    }
    
    /**
     * 设置类别奖罚开关状态
     */
    suspend fun setCategoryRewardPunishmentEnabled(categoryId: Int, enabled: Boolean) {
        val currentMap = getCategoryRewardPunishmentEnabled().toMutableMap()
        currentMap[categoryId] = enabled
        
        val json = JSONObject()
        currentMap.forEach { (key, value) ->
            json.put(key.toString(), value)
        }
        
        val currentSettings = getSettings()
        val updatedSettings = currentSettings.copy(
            categoryRewardPunishmentEnabled = json.toString(),
            updatedAt = System.currentTimeMillis()
        )
        appSettingsDao.insertOrUpdateSettings(updatedSettings)
    }
    
    /**
     * 批量设置类别奖罚开关状态
     */
    suspend fun setCategoryRewardPunishmentEnabledBatch(enabledMap: Map<Int, Boolean>) {
        val json = JSONObject()
        enabledMap.forEach { (key, value) ->
            json.put(key.toString(), value)
        }
        
        val currentSettings = getSettings()
        val updatedSettings = currentSettings.copy(
            categoryRewardPunishmentEnabled = json.toString(),
            updatedAt = System.currentTimeMillis()
        )
        appSettingsDao.insertOrUpdateSettings(updatedSettings)
    }
    
    /**
     * 重置所有设置为默认值
     */
    suspend fun resetToDefaults() {
        val defaultSettings = AppSettingsEntity()
        appSettingsDao.insertOrUpdateSettings(defaultSettings)
    }
} 