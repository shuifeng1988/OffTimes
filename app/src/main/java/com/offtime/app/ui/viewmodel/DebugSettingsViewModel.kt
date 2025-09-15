package com.offtime.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.offtime.app.data.entity.AppSettingsEntity
import com.offtime.app.data.entity.AppCategoryEntity
import com.offtime.app.data.repository.AppSettingsRepository
import com.offtime.app.data.dao.AppCategoryDao

@HiltViewModel
class DebugSettingsViewModel @Inject constructor(
    private val appSettingsRepository: AppSettingsRepository,
    private val appCategoryDao: AppCategoryDao
) : ViewModel() {

    private val _settings = MutableStateFlow<AppSettingsEntity?>(null)
    val settings: StateFlow<AppSettingsEntity?> = _settings.asStateFlow()
    
    private val _categories = MutableStateFlow<List<AppCategoryEntity>>(emptyList())
    val categories: StateFlow<List<AppCategoryEntity>> = _categories.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 加载设置数据
                val settingsData = appSettingsRepository.getSettings()
                _settings.value = settingsData
                android.util.Log.d("DebugSettingsViewModel", "加载设置数据: $settingsData")
                
                // 加载分类数据
                val categoriesData = appCategoryDao.getAllCategoriesList()
                _categories.value = categoriesData
                android.util.Log.d("DebugSettingsViewModel", "加载分类数据: ${categoriesData.size} 个分类")
                
            } catch (e: Exception) {
                android.util.Log.e("DebugSettingsViewModel", "加载数据失败", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun testSetDefaultCategory() {
        viewModelScope.launch {
            try {
                val categories = _categories.value
                if (categories.isNotEmpty()) {
                    // 随机选择一个分类作为默认分类
                    val randomCategory = categories.random()
                    appSettingsRepository.setDefaultCategoryId(randomCategory.id)
                    android.util.Log.d("DebugSettingsViewModel", "测试设置默认分类: ${randomCategory.name} (ID: ${randomCategory.id})")
                    
                    // 重新加载数据
                    loadData()
                }
            } catch (e: Exception) {
                android.util.Log.e("DebugSettingsViewModel", "测试设置默认分类失败", e)
            }
        }
    }
    
    fun testToggleRewardPunishment() {
        viewModelScope.launch {
            try {
                val categories = _categories.value
                if (categories.isNotEmpty()) {
                    // 随机选择一个分类切换其奖罚开关
                    val randomCategory = categories.random()
                    val currentEnabled = appSettingsRepository.isCategoryRewardPunishmentEnabled(randomCategory.id)
                    val newEnabled = !currentEnabled
                    
                    appSettingsRepository.setCategoryRewardPunishmentEnabled(randomCategory.id, newEnabled)
                    android.util.Log.d("DebugSettingsViewModel", "测试切换奖罚开关: ${randomCategory.name} (ID: ${randomCategory.id}) -> $newEnabled")
                    
                    // 重新加载数据
                    loadData()
                }
            } catch (e: Exception) {
                android.util.Log.e("DebugSettingsViewModel", "测试切换奖罚开关失败", e)
            }
        }
    }
    
    fun resetSettings() {
        viewModelScope.launch {
            try {
                appSettingsRepository.resetToDefaults()
                android.util.Log.d("DebugSettingsViewModel", "重置设置为默认值")
                
                // 重新加载数据
                loadData()
            } catch (e: Exception) {
                android.util.Log.e("DebugSettingsViewModel", "重置设置失败", e)
            }
        }
    }
} 