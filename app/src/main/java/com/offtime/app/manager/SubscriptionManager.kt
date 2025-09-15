package com.offtime.app.manager

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import com.offtime.app.data.repository.UserRepository

/**
 * 订阅管理器
 * 
 * 基于应用安装日期管理试用期，不依赖用户登录状态
 * 从安装日起7天免费试用，之后必须付费使用
 */
@Singleton
class SubscriptionManager @Inject constructor(
    private val context: Context,
    private val userRepository: UserRepository
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("subscription_prefs", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_INSTALL_TIME = "app_install_time"
        private const val TRIAL_DURATION_DAYS = 7L
        private const val TAG = "SubscriptionManager"
    }
    
    // 应用安装时间（毫秒）
    private val appInstallTime: Long
        get() {
            var installTime = prefs.getLong(KEY_INSTALL_TIME, 0L)
            if (installTime == 0L) {
                // 首次启动，记录安装时间
                installTime = System.currentTimeMillis()
                prefs.edit().putLong(KEY_INSTALL_TIME, installTime).apply()
            }
            return installTime
        }
    
    // 试用期结束时间
    val trialEndTime: Long get() = appInstallTime + (TRIAL_DURATION_DAYS * 24 * 60 * 60 * 1000)
    
    // 试用期剩余天数
    val trialDaysRemaining: Int
        get() {
            val now = System.currentTimeMillis()
            val remaining = trialEndTime - now
            return if (remaining > 0) {
                (remaining / (24 * 60 * 60 * 1000)).toInt() + 1
            } else {
                0
            }
        }
    
    // 是否还在试用期内
    val isInTrialPeriod: Boolean
        get() = System.currentTimeMillis() < trialEndTime
    
    /**
     * 检查用户是否可以使用应用
     * 优先检查付费状态，其次检查试用期
     */
    suspend fun canUserUseApp(): Boolean {
        // 首先检查是否有付费用户
        return try {
            val currentUser = userRepository.getCurrentUser()
            if (currentUser?.isPremium == true) {
                return true
            }
            
            // 没有付费用户，检查试用期
            isInTrialPeriod
        } catch (e: Exception) {
            // 如果无法获取用户信息，检查试用期
            isInTrialPeriod
        }
    }
    
    /**
     * 获取用户订阅状态流
     */
    suspend fun getUserSubscriptionFlow(): Flow<SubscriptionStatus> {
        return userRepository.getCurrentUserFlow().map { user ->
            when {
                user?.isPremium == true -> SubscriptionStatus.PREMIUM
                isInTrialPeriod -> SubscriptionStatus.TRIAL_ACTIVE
                else -> SubscriptionStatus.TRIAL_EXPIRED
            }
        }
    }
    
    /**
     * 获取当前订阅状态
     */
    suspend fun getCurrentSubscriptionStatus(): SubscriptionStatus {
        return when {
            userRepository.getCurrentUser()?.isPremium == true -> SubscriptionStatus.PREMIUM
            isInTrialPeriod -> SubscriptionStatus.TRIAL_ACTIVE
            else -> SubscriptionStatus.TRIAL_EXPIRED
        }
    }
    
    /**
     * 获取详细的订阅信息
     */
    suspend fun getSubscriptionInfo(): SubscriptionInfo {
        val currentUser = try {
            userRepository.getCurrentUser()
        } catch (e: Exception) {
            null
        }
        
        val isPremiumUser = currentUser?.isPremium ?: false
        
        return SubscriptionInfo(
            isInTrial = if (isPremiumUser) false else isInTrialPeriod,
            trialDaysRemaining = if (isPremiumUser) 0 else trialDaysRemaining,
            isPremium = isPremiumUser,
            canUseApp = canUserUseApp(),
            trialStartTime = appInstallTime,
            trialEndTime = trialEndTime,
            hasLoggedInUser = currentUser != null && currentUser.isLoggedIn
        )
    }
    
    /**
     * 升级到付费版本
     */
    suspend fun upgradeToPremium(): Boolean {
        return try {
            val result = userRepository.upgradeToPremium()
            result.isSuccess
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 重置试用期（仅用于测试）
     */
    fun resetTrialForTesting() {
        prefs.edit().remove(KEY_INSTALL_TIME).apply()
    }
    
    /**
     * 订阅状态枚举
     */
    enum class SubscriptionStatus {
        TRIAL_ACTIVE,       // 试用期内
        TRIAL_EXPIRED,      // 试用期过期
        PREMIUM             // 已付费
    }
    
    /**
     * 订阅信息数据类
     */
    data class SubscriptionInfo(
        val isInTrial: Boolean,                // 是否在试用期内
        val trialDaysRemaining: Int,           // 试用期剩余天数
        val isPremium: Boolean,                // 是否是付费用户
        val canUseApp: Boolean,                // 是否可以使用应用
        val trialStartTime: Long,              // 试用期开始时间
        val trialEndTime: Long,                // 试用期结束时间
        val hasLoggedInUser: Boolean           // 是否有已登录用户
    )
}
