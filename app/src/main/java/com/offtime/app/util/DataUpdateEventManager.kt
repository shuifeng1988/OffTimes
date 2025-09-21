package com.offtime.app.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 数据更新事件管理器
 * 
 * 负责协调各种事件触发的数据更新，确保：
 * 1. 从其他应用切换到OffTimes时触发数据更新
 * 2. 亮屏/熄屏时触发适当的数据更新
 * 3. 避免重复触发和过度更新
 * 4. 为ViewModel提供统一的数据更新通知
 */
@Singleton
class DataUpdateEventManager @Inject constructor() {
    
    companion object {
        private const val TAG = "DataUpdateEventManager"
        
        // 更新类型
        const val UPDATE_TYPE_APP_RESUME = "app_resume"           // 应用恢复前台
        const val UPDATE_TYPE_SCREEN_ON = "screen_on"             // 亮屏
        const val UPDATE_TYPE_SCREEN_OFF = "screen_off"           // 熄屏
        const val UPDATE_TYPE_MANUAL_REFRESH = "manual_refresh"   // 手动刷新
        const val UPDATE_TYPE_PAGE_SWITCH = "page_switch"         // 页面切换
    }
    
    // 数据更新事件流
    private val _dataUpdateEvents = MutableSharedFlow<DataUpdateEvent>()
    val dataUpdateEvents = _dataUpdateEvents.asSharedFlow()
    
    // 防重复触发的时间戳记录
    private var lastAppResumeUpdate = 0L
    private var lastScreenOnUpdate = 0L
    private var lastScreenOffUpdate = 0L
    
    // 防重复触发的时间间隔（毫秒）
    private val minUpdateInterval = 5000L // 5秒
    
    /**
     * 触发应用恢复前台的数据更新
     * 当从其他应用切换到OffTimes时调用
     */
    fun triggerAppResumeUpdate(context: Context) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAppResumeUpdate < minUpdateInterval) {
            Log.d(TAG, "应用恢复前台更新被跳过：距离上次更新时间过短")
            return
        }
        
        lastAppResumeUpdate = currentTime
        Log.d(TAG, "🔄 触发应用恢复前台数据更新")
        
        try {
            // 触发统一数据更新服务
            com.offtime.app.service.UnifiedUpdateService.triggerManualUpdate(context)
            
            // 发送更新事件通知
            _dataUpdateEvents.tryEmit(DataUpdateEvent(UPDATE_TYPE_APP_RESUME, currentTime))
            
        } catch (e: Exception) {
            Log.e(TAG, "应用恢复前台数据更新失败", e)
        }
    }
    
    /**
     * 触发亮屏的数据更新
     * 当屏幕点亮时调用
     */
    fun triggerScreenOnUpdate(context: Context) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastScreenOnUpdate < minUpdateInterval) {
            Log.d(TAG, "亮屏数据更新被跳过：距离上次更新时间过短")
            return
        }
        
        lastScreenOnUpdate = currentTime
        Log.d(TAG, "🔄 触发亮屏数据更新")
        
        try {
            // 亮屏时触发完整的数据更新（用户可能长时间离开）
            com.offtime.app.service.UnifiedUpdateService.triggerManualUpdate(context)
            
            // 发送更新事件通知
            _dataUpdateEvents.tryEmit(DataUpdateEvent(UPDATE_TYPE_SCREEN_ON, currentTime))
            
        } catch (e: Exception) {
            Log.e(TAG, "亮屏数据更新失败", e)
        }
    }
    
    /**
     * 触发熄屏的数据更新
     * 当屏幕关闭时调用
     */
    fun triggerScreenOffUpdate(context: Context) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastScreenOffUpdate < minUpdateInterval) {
            Log.d(TAG, "熄屏数据更新被跳过：距离上次更新时间过短")
            return
        }
        
        lastScreenOffUpdate = currentTime
        Log.d(TAG, "🔄 触发熄屏数据更新")
        
        try {
            // 熄屏时进行轻量级数据更新（主要是事件拉取和基础聚合）
            val historyIntent = android.content.Intent(context, com.offtime.app.service.DataAggregationService::class.java)
            historyIntent.action = com.offtime.app.service.DataAggregationService.ACTION_PROCESS_HISTORICAL_DATA
            context.startService(historyIntent)
            
            // 发送更新事件通知
            _dataUpdateEvents.tryEmit(DataUpdateEvent(UPDATE_TYPE_SCREEN_OFF, currentTime))
            
        } catch (e: Exception) {
            Log.e(TAG, "熄屏数据更新失败", e)
        }
    }
    
    /**
     * 触发手动刷新的数据更新
     * 当用户下拉刷新时调用
     */
    fun triggerManualRefreshUpdate(context: Context) {
        val currentTime = System.currentTimeMillis()
        Log.d(TAG, "🔄 触发手动刷新数据更新")
        
        try {
            // 手动刷新时触发完整的数据更新
            com.offtime.app.service.UnifiedUpdateService.triggerManualUpdate(context)
            
            // 发送更新事件通知
            _dataUpdateEvents.tryEmit(DataUpdateEvent(UPDATE_TYPE_MANUAL_REFRESH, currentTime))
            
        } catch (e: Exception) {
            Log.e(TAG, "手动刷新数据更新失败", e)
        }
    }
    
    /**
     * 触发页面切换的数据更新
     * 当切换到首页或统计页面时调用
     */
    fun triggerPageSwitchUpdate(context: Context, pageName: String) {
        val currentTime = System.currentTimeMillis()
        Log.d(TAG, "🔄 触发页面切换数据更新: $pageName")
        
        try {
            // 页面切换时触发完整的数据更新
            com.offtime.app.service.UnifiedUpdateService.triggerManualUpdate(context)
            
            // 发送更新事件通知
            _dataUpdateEvents.tryEmit(DataUpdateEvent(UPDATE_TYPE_PAGE_SWITCH, currentTime, pageName))
            
        } catch (e: Exception) {
            Log.e(TAG, "页面切换数据更新失败", e)
        }
    }
}

/**
 * 数据更新事件
 */
data class DataUpdateEvent(
    val type: String,
    val timestamp: Long,
    val extra: String? = null
)
