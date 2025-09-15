package com.offtime.app.utils

import android.content.Context
import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirstLaunchManager @Inject constructor(
    private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "first_launch_prefs", 
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val KEY_IS_FIRST_LAUNCH = "is_first_launch"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }
    
    /**
     * 检查是否为首次启动
     */
    fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(KEY_IS_FIRST_LAUNCH, true)
    }
    
    /**
     * 标记已不是首次启动
     */
    fun setFirstLaunchCompleted() {
        prefs.edit()
            .putBoolean(KEY_IS_FIRST_LAUNCH, false)
            .apply()
    }
    
    /**
     * 检查引导流程是否已完成
     */
    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }
    
    /**
     * 标记引导流程已完成
     */
    fun setOnboardingCompleted() {
        prefs.edit()
            .putBoolean(KEY_ONBOARDING_COMPLETED, true)
            .apply()
    }
    
    /**
     * 重置首次启动状态（调试用）
     */
    fun resetFirstLaunch() {
        prefs.edit()
            .putBoolean(KEY_IS_FIRST_LAUNCH, true)
            .putBoolean(KEY_ONBOARDING_COMPLETED, false)
            .apply()
    }
} 