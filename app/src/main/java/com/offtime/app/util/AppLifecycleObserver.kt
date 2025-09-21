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

    fun onActivityResumed() {
        _isActivityInForeground.value = true
    }

    fun onActivityPaused() {
        _isActivityInForeground.value = false
    }
}
