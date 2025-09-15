package com.offtime.app.ui.debug.viewmodel

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offtime.app.data.entity.AppSessionUserEntity
import com.offtime.app.data.repository.AppSessionRepository
import com.offtime.app.service.UsageStatsCollectorService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class DebugRealTimeStatsViewModel @Inject constructor(
    private val appSessionRepository: AppSessionRepository,
    private val context: Context
) : ViewModel() {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    
    // 今日会话记录
    private val _todaySessions = MutableStateFlow<List<AppSessionUserEntity>>(emptyList())
    val todaySessions: StateFlow<List<AppSessionUserEntity>> = _todaySessions.asStateFlow()
    
    // 最近合并的会话
    private val _recentMergedSessions = MutableStateFlow<List<String>>(emptyList())
    val recentMergedSessions: StateFlow<List<String>> = _recentMergedSessions.asStateFlow()
    
    // 数据收集服务状态
    private val _isCollectorServiceRunning = MutableStateFlow(false)
    val isCollectorServiceRunning: StateFlow<Boolean> = _isCollectorServiceRunning.asStateFlow()
    
    // 实时统计功能状态
    private val _realtimeStatsEnabled = MutableStateFlow(false)
    val realtimeStatsEnabled: StateFlow<Boolean> = _realtimeStatsEnabled.asStateFlow()
    
    // 最后活跃应用检查时间
    private val _lastActiveAppsCheck = MutableStateFlow("")
    val lastActiveAppsCheck: StateFlow<String> = _lastActiveAppsCheck.asStateFlow()
    
    init {
        refreshData()
    }
    
    /**
     * 刷新所有数据
     */
    fun refreshData() {
        viewModelScope.launch {
            refreshServiceStatus()
            refreshTodaySessions()
            refreshMergedSessions()
            updateLastCheckTime()
        }
    }
    
    /**
     * 刷新服务状态
     */
    private fun refreshServiceStatus() {
        try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val services = activityManager.getRunningServices(Int.MAX_VALUE)
            
            val isRunning = services.any { 
                it.service.className == UsageStatsCollectorService::class.java.name 
            }
            
            _isCollectorServiceRunning.value = isRunning
            _realtimeStatsEnabled.value = isRunning // 如果服务运行则认为实时统计已启用
            
        } catch (e: Exception) {
            android.util.Log.e("DebugRealTimeStatsVM", "检查服务状态失败", e)
            _isCollectorServiceRunning.value = false
            _realtimeStatsEnabled.value = false
        }
    }
    
    /**
     * 刷新今日会话记录
     */
    fun refreshTodaySessions() {
        viewModelScope.launch {
            try {
                val sessions = appSessionRepository.getTodaySessions()
                _todaySessions.value = sessions
                android.util.Log.d("DebugRealTimeStatsVM", "刷新今日会话: ${sessions.size} 条")
            } catch (e: Exception) {
                android.util.Log.e("DebugRealTimeStatsVM", "刷新今日会话失败", e)
                _todaySessions.value = emptyList()
            }
        }
    }
    
    /**
     * 刷新合并会话记录
     */
    fun refreshMergedSessions() {
        viewModelScope.launch {
            try {
                // 这里可以添加具体的合并统计逻辑
                // 目前先显示一些模拟数据作为示例
                val mergedInfo = listOf(
                    "OffTimes: 14:00-14:05 合并为 5分钟",
                    "Chrome: 15:30-15:35 合并为 5分钟",
                    "微信: 16:45-16:50 合并为 5分钟"
                )
                _recentMergedSessions.value = mergedInfo
            } catch (e: Exception) {
                android.util.Log.e("DebugRealTimeStatsVM", "刷新合并会话失败", e)
                _recentMergedSessions.value = emptyList()
            }
        }
    }
    
    /**
     * 更新最后检查时间
     */
    private fun updateLastCheckTime() {
        val currentTime = timeFormat.format(Date())
        _lastActiveAppsCheck.value = currentTime
    }
    
    /**
     * 启动数据收集服务
     */
    fun startCollectorService() {
        try {
            val intent = Intent(context, UsageStatsCollectorService::class.java).apply {
                action = UsageStatsCollectorService.ACTION_START_COLLECTION
            }
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            
            android.util.Log.d("DebugRealTimeStatsVM", "启动数据收集服务")
            
            // 延迟检查状态
            viewModelScope.launch {
                kotlinx.coroutines.delay(2000)
                refreshServiceStatus()
            }
            
        } catch (e: Exception) {
            android.util.Log.e("DebugRealTimeStatsVM", "启动服务失败", e)
        }
    }
    
    /**
     * 停止数据收集服务
     */
    fun stopCollectorService() {
        try {
            val intent = Intent(context, UsageStatsCollectorService::class.java).apply {
                action = UsageStatsCollectorService.ACTION_STOP_COLLECTION
            }
            context.startService(intent)
            
            android.util.Log.d("DebugRealTimeStatsVM", "停止数据收集服务")
            
            // 延迟检查状态
            viewModelScope.launch {
                kotlinx.coroutines.delay(2000)
                refreshServiceStatus()
            }
            
        } catch (e: Exception) {
            android.util.Log.e("DebugRealTimeStatsVM", "停止服务失败", e)
        }
    }
    
    /**
     * 立即拉取事件
     */
    fun pullEventsNow() {
        try {
            val intent = Intent(context, UsageStatsCollectorService::class.java).apply {
                action = UsageStatsCollectorService.ACTION_PULL_EVENTS
            }
            context.startService(intent)
            
            android.util.Log.d("DebugRealTimeStatsVM", "手动触发事件拉取")
            
            // 延迟刷新数据
            viewModelScope.launch {
                kotlinx.coroutines.delay(3000)
                refreshTodaySessions()
            }
            
        } catch (e: Exception) {
            android.util.Log.e("DebugRealTimeStatsVM", "拉取事件失败", e)
        }
    }
    
    /**
     * 模拟应用使用（用于测试）
     */
    fun simulateAppUsage(packageName: String) {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val startTime = now - 120000 // 2分钟前开始
                
                // 使用新的智能合并方法
                appSessionRepository.upsertSessionSmart(
                    pkgName = packageName,
                    startTime = startTime,
                    endTime = now
                )
                
                android.util.Log.d("DebugRealTimeStatsVM", "模拟应用使用: $packageName")
                
                // 延迟刷新数据
                kotlinx.coroutines.delay(1000)
                refreshTodaySessions()
                
            } catch (e: Exception) {
                android.util.Log.e("DebugRealTimeStatsVM", "模拟应用使用失败", e)
            }
        }
    }
    
    /**
     * 测试智能合并功能
     */
    fun testSmartMerge() {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val packageName = "com.offtime.app"
                
                // 创建两个连续的会话记录，测试智能合并
                val session1Start = now - 600000 // 10分钟前
                val session1End = now - 300000   // 5分钟前
                
                val session2Start = now - 240000 // 4分钟前（间隔1分钟）
                val session2End = now            // 现在
                
                // 插入第一个会话
                appSessionRepository.upsertSessionSmart(
                    pkgName = packageName,
                    startTime = session1Start,
                    endTime = session1End
                )
                
                kotlinx.coroutines.delay(500)
                
                // 插入第二个会话（应该与第一个合并）
                appSessionRepository.upsertSessionSmart(
                    pkgName = packageName,
                    startTime = session2Start,
                    endTime = session2End
                )
                
                android.util.Log.d("DebugRealTimeStatsVM", "测试智能合并: 创建了两个连续会话")
                
                // 延迟刷新数据
                kotlinx.coroutines.delay(1000)
                refreshTodaySessions()
                refreshMergedSessions()
                
            } catch (e: Exception) {
                android.util.Log.e("DebugRealTimeStatsVM", "测试智能合并失败", e)
            }
        }
    }
} 