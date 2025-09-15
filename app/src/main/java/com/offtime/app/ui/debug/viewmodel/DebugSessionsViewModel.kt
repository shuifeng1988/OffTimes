package com.offtime.app.ui.debug.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offtime.app.data.dao.AppSessionUserDao
import com.offtime.app.data.dao.AppCategoryDao
import com.offtime.app.data.dao.AppInfoDao
import com.offtime.app.data.entity.AppSessionUserEntity
import com.offtime.app.data.entity.AppCategoryEntity
import com.offtime.app.data.entity.AppInfoEntity
import com.offtime.app.data.repository.AppSessionRepository
import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.offtime.app.service.UsageStatsCollectorService

@HiltViewModel
class DebugSessionsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appSessionUserDao: AppSessionUserDao,
    private val appCategoryDao: AppCategoryDao,
    private val appInfoDao: AppInfoDao,
    private val appSessionRepository: AppSessionRepository
) : ViewModel() {
    
    private val _sessions = MutableStateFlow<List<com.offtime.app.data.entity.AppSessionUserEntity>>(emptyList())
    val sessions: StateFlow<List<com.offtime.app.data.entity.AppSessionUserEntity>> = _sessions.asStateFlow()
    
    private val _categories = MutableStateFlow<List<com.offtime.app.data.entity.AppCategoryEntity>>(emptyList())
    val categories: StateFlow<List<com.offtime.app.data.entity.AppCategoryEntity>> = _categories.asStateFlow()
    
    private val _appInfos = MutableStateFlow<List<com.offtime.app.data.entity.AppInfoEntity>>(emptyList())
    val appInfos: StateFlow<List<com.offtime.app.data.entity.AppInfoEntity>> = _appInfos.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // 数据显示模式：原始数据还是合并后数据
    private val _showMergedData = MutableStateFlow(false)
    val showMergedData: StateFlow<Boolean> = _showMergedData.asStateFlow()
    
    fun toggleDataMode() {
        _showMergedData.value = !_showMergedData.value
        loadSessions() // 重新加载数据
    }
    
    fun cleanDuplicateRecords() {
        viewModelScope.launch {
            try {
                android.util.Log.d("DebugSessionsViewModel", "开始清理重复记录...")
                
                // 获取今日所有会话数据
                val allSessions = appSessionUserDao.getAllSessions()
                val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                val todaySessions = allSessions.filter { it.date == today }
                
                android.util.Log.d("DebugSessionsViewModel", "今日会话数量: ${todaySessions.size}")
                
                // 按应用分组检查重复
                val sessionsByPackage = todaySessions.groupBy { it.pkgName }
                var cleanedCount = 0
                
                sessionsByPackage.forEach { (packageName, packageSessions) ->
                    val sortedSessions = packageSessions.sortedBy { it.startTime }
                    
                    // 查找相同开始时间的会话（重复记录）
                    val duplicateGroups = sortedSessions.groupBy { it.startTime }
                    duplicateGroups.forEach { (_, duplicates) ->
                        if (duplicates.size > 1) {
                            // 保留时长最长的记录，删除其他的
                            val longestSession = duplicates.maxByOrNull { it.durationSec }
                            val toDelete = duplicates.filter { it.id != longestSession?.id }
                            
                            toDelete.forEach { session ->
                                appSessionUserDao.deleteSessionById(session.id)
                                cleanedCount++
                                android.util.Log.d("DebugSessionsViewModel", "删除重复记录: $packageName, ID=${session.id}")
                            }
                        }
                    }
                }
                
                android.util.Log.d("DebugSessionsViewModel", "清理完成，删除了 $cleanedCount 条重复记录")
                
                // 重新加载数据
                loadSessions()
                
            } catch (e: Exception) {
                android.util.Log.e("DebugSessionsViewModel", "清理重复记录失败", e)
            }
        }
    }
    
    fun loadSessions() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 步骤 1: 触发后台数据更新
                android.util.Log.d("DebugSessionsViewModel", "触发后台数据更新...")
                UsageStatsCollectorService.triggerEventsPull(context)
                
                // 步骤 2: 等待后台处理完成 (预估4秒)
                android.util.Log.d("DebugSessionsViewModel", "等待4秒以确保后台数据处理完成...")
                delay(4000L)
                
                // 步骤 3: 从数据库读取最新数据
                android.util.Log.d("DebugSessionsViewModel", "重新从数据库加载数据...")
                val sessionList = if (_showMergedData.value) {
                    // 显示合并后数据：使用appSessionRepository的智能合并逻辑
                    appSessionRepository.getTodaySessions()
                } else {
                    // 显示原始数据
                    appSessionUserDao.getAllSessions()
                }
                _sessions.value = sessionList
                
                val categoryList = appCategoryDao.getAllCategoriesList()
                _categories.value = categoryList
                
                val appInfoList = appInfoDao.getAllAppsList()
                _appInfos.value = appInfoList
                
                android.util.Log.d("DebugSessionsViewModel", "加载会话数据: ${sessionList.size} 条 (合并模式: ${_showMergedData.value})")
                android.util.Log.d("DebugSessionsViewModel", "加载分类数据: ${categoryList.size} 个")
                android.util.Log.d("DebugSessionsViewModel", "加载应用信息数据: ${appInfoList.size} 个")
                
            } catch (e: Exception) {
                android.util.Log.e("DebugSessionsViewModel", "加载数据失败", e)
                _sessions.value = emptyList()
                _categories.value = emptyList()
                _appInfos.value = emptyList()
            } finally {
                _isLoading.value = false
                android.util.Log.d("DebugSessionsViewModel", "加载流程结束.")
            }
        }
    }
} 