package com.offtime.app.manager

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 数据更新管理器
 * 
 * 使用现代的Flow机制替代已弃用的LocalBroadcastManager
 * 提供数据更新事件的发送和接收功能
 */
@Singleton
class DataUpdateManager @Inject constructor() {
    
    // 数据更新事件流
    private val _dataUpdateFlow = MutableSharedFlow<DataUpdateEvent>(
        replay = 0,
        extraBufferCapacity = 10
    )
    val dataUpdateFlow: SharedFlow<DataUpdateEvent> = _dataUpdateFlow.asSharedFlow()
    
    /**
     * 发送数据更新事件
     * 
     * @param updateType 更新类型：periodic（定时更新）或 manual（手动更新）
     */
    fun notifyDataUpdated(updateType: String) {
        try {
            val event = DataUpdateEvent(
                updateType = updateType,
                timestamp = System.currentTimeMillis()
            )
            _dataUpdateFlow.tryEmit(event)
            android.util.Log.d("DataUpdateManager", "发送数据更新事件: $updateType")
        } catch (e: Exception) {
            android.util.Log.e("DataUpdateManager", "发送数据更新事件失败", e)
        }
    }
    
    companion object {
        // 更新类型常量
        const val UPDATE_TYPE_PERIODIC = "periodic"      // 定时更新
        const val UPDATE_TYPE_MANUAL = "manual"          // 手动更新
    }
}

/**
 * 数据更新事件
 * 
 * @param updateType 更新类型
 * @param timestamp 更新时间戳
 */
data class DataUpdateEvent(
    val updateType: String,
    val timestamp: Long
)