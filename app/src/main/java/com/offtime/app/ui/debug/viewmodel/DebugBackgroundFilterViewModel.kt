package com.offtime.app.ui.debug.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offtime.app.data.dao.AppInfoDao
import com.offtime.app.data.dao.AppSessionUserDao
import com.offtime.app.utils.BackgroundAppFilterUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class DebugBackgroundFilterViewModel @Inject constructor(
    private val appSessionUserDao: AppSessionUserDao,
    private val appInfoDao: AppInfoDao
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DebugBackgroundFilterUiState())
    val uiState: StateFlow<DebugBackgroundFilterUiState> = _uiState.asStateFlow()
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    /**
     * 加载过滤统计数据
     */
    fun loadFilterStats() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val today = dateFormat.format(Date())
                
                // 获取今日所有会话
                val todaySessions = appSessionUserDao.getSessionsByDate(today)
                
                // 获取所有应用信息
                val allApps = appInfoDao.getAllAppsList()
                val appNameMap = allApps.associate { it.packageName to it.appName }
                
                // 分析过滤效果
                val filteredSessions = mutableListOf<FilteredSessionInfo>()
                var totalOriginalDuration = 0
                var totalAdjustedDuration = 0
                var filteredCount = 0
                
                for (session in todaySessions) {
                    val appName = appNameMap[session.pkgName] ?: session.pkgName
                    val isBackgroundApp = BackgroundAppFilterUtils.isBackgroundResidentApp(session.pkgName)
                    
                    if (isBackgroundApp) {
                        // 模拟原始时长（假设这是调整前的时长）
                        val originalDuration = simulateOriginalDuration(session.durationSec, session.pkgName)
                        val adjustedDuration = session.durationSec
                        
                        if (originalDuration != adjustedDuration) {
                            filteredCount++
                            
                            val isBackgroundWakeup = BackgroundAppFilterUtils.detectBackgroundWakeupPattern(
                                session.pkgName, session.durationSec, session.startTime, session.endTime
                            )
                            
                            filteredSessions.add(
                                FilteredSessionInfo(
                                    appName = appName,
                                    packageName = session.pkgName,
                                    timeRange = "${timeFormat.format(session.startTime)} - ${timeFormat.format(session.endTime)}",
                                    originalDurationMinutes = originalDuration / 60,
                                    adjustedDurationMinutes = adjustedDuration / 60,
                                    filterLevel = BackgroundAppFilterUtils.getFilterLevel(session.pkgName),
                                    isBackgroundWakeup = isBackgroundWakeup
                                )
                            )
                        }
                        
                        totalOriginalDuration += originalDuration
                        totalAdjustedDuration += adjustedDuration
                    }
                }
                
                // 统计常驻后台应用
                val backgroundApps = allApps
                    .filter { BackgroundAppFilterUtils.isBackgroundResidentApp(it.packageName) }
                    .map { app ->
                        val todayFilteredMinutes = filteredSessions
                            .filter { it.packageName == app.packageName }
                            .sumOf { it.originalDurationMinutes - it.adjustedDurationMinutes }
                        
                        BackgroundAppInfo(
                            appName = app.appName,
                            packageName = app.packageName,
                            filterLevel = BackgroundAppFilterUtils.getFilterLevel(app.packageName),
                            todayFilteredMinutes = todayFilteredMinutes
                        )
                    }
                    .sortedByDescending { it.todayFilteredMinutes }
                
                val filterStats = FilterStats(
                    totalSessions = todaySessions.size,
                    filteredSessions = filteredCount,
                    timeSavedMinutes = (totalOriginalDuration - totalAdjustedDuration) / 60
                )
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    filterStats = filterStats,
                    backgroundApps = backgroundApps,
                    filteredSessions = filteredSessions.sortedByDescending { it.originalDurationMinutes - it.adjustedDurationMinutes }
                )
                
            } catch (e: Exception) {
                android.util.Log.e("DebugBackgroundFilterVM", "加载过滤统计失败", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    filterStats = null,
                    backgroundApps = emptyList(),
                    filteredSessions = emptyList()
                )
            }
        }
    }
    
    /**
     * 模拟原始时长（在没有过滤的情况下应该是多长）
     * 这个方法用于演示过滤效果
     */
    private fun simulateOriginalDuration(adjustedDuration: Int, packageName: String): Int {
        return when {
            BackgroundAppFilterUtils.isHighFrequencyBackgroundApp(packageName) -> {
                // 高频后台应用：假设原始时长是调整后的1.5-3倍
                when {
                    adjustedDuration <= 60 -> adjustedDuration // 1分钟内保持不变
                    adjustedDuration <= 300 -> (adjustedDuration / 0.9).toInt() // 原来是90%
                    adjustedDuration <= 1800 -> (adjustedDuration / 0.7).toInt() // 原来是70%
                    else -> (adjustedDuration / 0.5).toInt() // 原来是50%
                }
            }
            BackgroundAppFilterUtils.isBackgroundResidentApp(packageName) -> {
                // 常驻后台应用：假设原始时长是调整后的1.2-1.5倍
                when {
                    adjustedDuration <= 300 -> adjustedDuration // 5分钟内保持不变
                    adjustedDuration <= 1800 -> (adjustedDuration / 0.9).toInt() // 原来是90%
                    adjustedDuration <= 3600 -> (adjustedDuration / 0.8).toInt() // 原来是80%
                    else -> (adjustedDuration / 0.7).toInt() // 原来是70%
                }
            }
            else -> adjustedDuration // 普通应用不调整
        }
    }
    
    /**
     * 刷新数据
     */
    fun refreshData() {
        loadFilterStats()
    }
    
    /**
     * UI状态数据类
     */
    data class DebugBackgroundFilterUiState(
        val isLoading: Boolean = false,
        val filterStats: FilterStats? = null,
        val backgroundApps: List<BackgroundAppInfo> = emptyList(),
        val filteredSessions: List<FilteredSessionInfo> = emptyList()
    )
    
    /**
     * 过滤统计数据类
     */
    data class FilterStats(
        val totalSessions: Int,
        val filteredSessions: Int,
        val timeSavedMinutes: Int
    )
    
    /**
     * 后台应用信息数据类
     */
    data class BackgroundAppInfo(
        val appName: String,
        val packageName: String,
        val filterLevel: BackgroundAppFilterUtils.FilterLevel,
        val todayFilteredMinutes: Int
    )
    
    /**
     * 过滤会话信息数据类
     */
    data class FilteredSessionInfo(
        val appName: String,
        val packageName: String,
        val timeRange: String,
        val originalDurationMinutes: Int,
        val adjustedDurationMinutes: Int,
        val filterLevel: BackgroundAppFilterUtils.FilterLevel,
        val isBackgroundWakeup: Boolean
    )
} 