package com.offtime.app.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A singleton object to observe the lifecycle of the main application UI.
 *
 * This provides a reliable, single source of truth to determine if the application's
 * main activity is in the foreground. It is used by background services, like
 * [com.offtime.app.service.UsageStatsCollectorService], to differentiate between
 * genuine user-initiated foreground sessions and background tasks that might
 * briefly appear as foreground events.
 *
 * This helps prevent the service from incorrectly interrupting other applications'
 * usage sessions or failing to record its own usage when the user is actively
 * interacting with it.
 */
object AppLifecycleObserver {

    private val _isActivityInForeground = MutableStateFlow(false)
    val isActivityInForeground = _isActivityInForeground.asStateFlow()

    // 🔧 新增：屏幕状态跟踪
    // 用于过滤黑屏时的后台应用使用（如黑屏听歌、后台微信等）
    private val _isScreenOn = MutableStateFlow(true)  // 默认认为屏幕是亮的
    val isScreenOn = _isScreenOn.asStateFlow()

    // 屏幕关闭时的时间戳，用于判断事件是否发生在黑屏期间
    private var screenOffTimestamp: Long = 0L

    fun onActivityResumed() {
        _isActivityInForeground.value = true
    }

    fun onActivityPaused() {
        _isActivityInForeground.value = false
    }

    /**
     * 屏幕点亮时调用
     */
    fun onScreenOn() {
        _isScreenOn.value = true
        android.util.Log.d("AppLifecycleObserver", "📱 屏幕点亮")
    }

    /**
     * 屏幕关闭时调用
     */
    fun onScreenOff() {
        _isScreenOn.value = false
        screenOffTimestamp = System.currentTimeMillis()
        android.util.Log.d("AppLifecycleObserver", "📴 屏幕关闭，时间戳: $screenOffTimestamp")
    }

    /**
     * 获取屏幕关闭的时间戳
     * 用于判断某个事件是否发生在屏幕关闭期间
     */
    fun getScreenOffTimestamp(): Long = screenOffTimestamp
}
