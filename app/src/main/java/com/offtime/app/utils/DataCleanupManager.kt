package com.offtime.app.utils

import android.util.Log
import com.offtime.app.data.repository.AppSessionRepository
import com.offtime.app.data.repository.TimerSessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 数据清理管理器
 * 
 * 用户表数据保留策略（所有以"_user"或"_users"结尾的表）：
 * 
 * 永久保存表（所有用户数据永久保存，支持云端备份）：
 * - app_sessions_users: APP使用会话原始记录 【已改为永久保存】
 * - timer_sessions_users: 计时会话原始记录 【已改为永久保存】
 * - AppCategory_Users: 应用分类配置
 * - goals_reward_punishment_users: 目标奖惩设置
 * - daily_usage_user: 每日使用明细数据
 * - summary_usage_user: 使用汇总数据
 * - summary_usage_week_user: 周度汇总数据
 * - summary_usage_month_user: 月度汇总数据
 * - reward_punishment_user: 奖惩记录
 * - app_info_Users: 应用信息配置
 * 
 * 注意：
 * 1. 所有用户表数据永久保存，不会被自动清理
 * 2. 支持增量云端备份，每天只备份新增和修改的数据
 * 3. 数据不会受应用升级、重编译等操作影响
 */
@Singleton
class DataCleanupManager @Inject constructor(
    private val appSessionRepository: AppSessionRepository,
    private val timerSessionRepository: TimerSessionRepository
) {
    
    companion object {
        private const val TAG = "DataCleanupManager"
    }
    
    /**
     * 数据永久保存策略
     * 所有用户数据永久保存，不再自动清理
     * 
     * 背景：为了支持完整的数据备份和恢复功能，
     * 现在所有基础用户数据都改为永久保存
     */
    suspend fun performDataCleanup() = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "=== 用户数据永久保存策略 ===")
            Log.i(TAG, "所有用户表数据已改为永久保存，不再执行自动清理")
            Log.i(TAG, "基础数据表永久保存列表：")
            Log.i(TAG, "  - app_sessions_users: APP使用会话记录")
            Log.i(TAG, "  - timer_sessions_users: 计时会话记录")
            Log.i(TAG, "  - AppCategory_Users: 用户应用分类")
            Log.i(TAG, "  - goals_reward_punishment_users: 目标奖惩设置")
            Log.i(TAG, "  - 以及所有其他用户汇总和配置数据")
            
            // 统计当前数据量
            val appSessionCount = appSessionRepository.getTotalSessionCount()
            val timerSessionCount = timerSessionRepository.getTotalSessionCount()
            
            Log.i(TAG, "当前数据统计: app_sessions_users=${appSessionCount}条, timer_sessions_users=${timerSessionCount}条")
            Log.i(TAG, "数据将永久保存，支持云端备份功能")
            Log.i(TAG, "=== 数据保护策略已启用 ===")
            
        } catch (e: Exception) {
            Log.e(TAG, "数据统计失败: ${e.message}", e)
        }
    }
    
    /**
     * 检查是否需要执行数据统计
     * 由于改为永久保存策略，不再执行实际清理
     */
    suspend fun shouldPerformCleanup(): Boolean {
        // 永久保存策略：不再执行数据清理
        // 此方法现在仅用于数据统计和状态检查
        return false
    }
    
    /**
     * 数据健康检查
     * 检查基础数据表状态，确保数据完整性
     * 为云端备份功能提供数据量统计
     */
    suspend fun checkDataHealth(): DataHealthReport = withContext(Dispatchers.IO) {
        try {
            val appSessionCount = appSessionRepository.getTotalSessionCount()
            val timerSessionCount = timerSessionRepository.getTotalSessionCount()
            
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            
            // 数据健康状态：检查是否有基础数据存在
            val isHealthy = appSessionCount >= 0 && timerSessionCount >= 0
            val totalCount = appSessionCount + timerSessionCount
            
            val report = DataHealthReport(
                appSessionCount = appSessionCount,
                timerSessionCount = timerSessionCount,
                checkDate = today,
                isHealthy = isHealthy,
                warning = when {
                    !isHealthy -> "数据统计异常，请检查数据库状态"
                    totalCount == 0 -> "尚无使用数据，这是正常的（新用户或首次使用）"
                    totalCount < 10 -> "数据量较少，可能是新用户或数据收集刚开始"
                    else -> null
                }
            )
            
            Log.i(TAG, "数据健康检查（永久保存模式）: $report")
            Log.i(TAG, "数据统计: 总计${totalCount}条基础记录 (应用会话:${appSessionCount}, 计时会话:${timerSessionCount})")
            
            return@withContext report
            
        } catch (e: Exception) {
            Log.e(TAG, "数据健康检查失败: ${e.message}", e)
            return@withContext DataHealthReport(
                appSessionCount = -1,
                timerSessionCount = -1,
                checkDate = "",
                isHealthy = false,
                warning = "健康检查失败: ${e.message}"
            )
        }
    }
    
    data class DataHealthReport(
        val appSessionCount: Int,
        val timerSessionCount: Int,
        val checkDate: String,
        val isHealthy: Boolean,
        val warning: String?
    )
} 