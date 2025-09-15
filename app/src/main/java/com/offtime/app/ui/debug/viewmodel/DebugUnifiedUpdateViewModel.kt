package com.offtime.app.ui.debug.viewmodel

import android.app.ActivityManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.offtime.app.manager.DataUpdateManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DebugUnifiedUpdateViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataUpdateManager: DataUpdateManager
) : ViewModel() {
    
    private val _serviceStatus = MutableStateFlow(false)
    val serviceStatus: StateFlow<Boolean> = _serviceStatus.asStateFlow()
    
    private val _lastUpdateTime = MutableStateFlow("未知")
    val lastUpdateTime: StateFlow<String> = _lastUpdateTime.asStateFlow()
    
    private val _updateCount = MutableStateFlow(0)
    val updateCount: StateFlow<Int> = _updateCount.asStateFlow()
    
    private val _isAutoRefresh = MutableStateFlow(true)
    val isAutoRefresh: StateFlow<Boolean> = _isAutoRefresh.asStateFlow()
    
    private var monitoringStarted = false
    
    init {
        // 监听数据更新事件
        viewModelScope.launch {
            dataUpdateManager.dataUpdateFlow.collect { event ->
                // 更新计数和时间
                _updateCount.value += 1
                val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                _lastUpdateTime.value = timeFormat.format(Date(event.timestamp))
            }
        }
    }
    
    /**
     * 开始监控服务状态
     */
    fun startMonitoring() {
        if (monitoringStarted) return
        monitoringStarted = true
        
        viewModelScope.launch {
            while (monitoringStarted) {
                if (_isAutoRefresh.value) {
                    refreshStatus()
                }
                delay(5000) // 每5秒检查一次
            }
        }
    }
    
    /**
     * 刷新服务状态
     */
    fun refreshStatus() {
        viewModelScope.launch {
            try {
                val isRunning = isUnifiedUpdateServiceRunning()
                _serviceStatus.value = isRunning
                
                android.util.Log.d("DebugUnifiedUpdateViewModel", 
                    "统一更新服务状态: ${if (isRunning) "运行中" else "已停止"}")
                
            } catch (e: Exception) {
                android.util.Log.e("DebugUnifiedUpdateViewModel", "检查服务状态失败", e)
                _serviceStatus.value = false
            }
        }
    }
    
    /**
     * 设置自动刷新状态
     */
    fun setAutoRefresh(enabled: Boolean) {
        _isAutoRefresh.value = enabled
    }
    
    /**
     * 检查统一更新服务是否正在运行
     */
    private fun isUnifiedUpdateServiceRunning(): Boolean {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val runningServices = activityManager.getRunningServices(Integer.MAX_VALUE)
            
            val serviceName = "com.offtime.app.service.UnifiedUpdateService"
            val isRunning = runningServices.any { service ->
                service.service.className == serviceName
            }
            
            android.util.Log.d("DebugUnifiedUpdateViewModel", 
                "检查服务: $serviceName, 运行状态: $isRunning")
            
            isRunning
        } catch (e: Exception) {
            android.util.Log.e("DebugUnifiedUpdateViewModel", "检查服务状态时出错", e)
            false
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        monitoringStarted = false
        android.util.Log.d("DebugUnifiedUpdateViewModel", "ViewModel已清理")
    }
}