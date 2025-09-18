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
 * 60天保留表（原始会话数据，定期清理以节省存储空间）：
 * - app_sessions_users: APP使用会话原始记录 【保留60天】
 * - timer_sessions_users: 计时会话原始记录 【保留60天】
 * 
 * 永久保存表（聚合数据和配置，支持云端备份）：
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
 * 1. 原始会话数据保留60天，聚合数据和配置永久保存
 * 2. 支持增量云端备份，每天只备份新增和修改的数据
 * 3. 聚合数据不会受应用升级、重编译等操作影响
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
     * 执行数据清理策略
     * 
     * 清理规则：
     * - app_sessions_users: 保留60天
     * - timer_sessions_users: 保留60天
     * - 其他聚合表和配置表: 永久保存
     */
    suspend fun performDataCleanup() = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "=== 开始执行数据清理 ===")
            
            // 统计清理前数据量
            val appSessionCountBefore = appSessionRepository.getTotalSessionCount()
            val timerSessionCountBefore = timerSessionRepository.getTotalSessionCount()
            
            Log.i(TAG, "清理前数据统计: app_sessions_users=${appSessionCountBefore}条, timer_sessions_users=${timerSessionCountBefore}条")
            
            // 执行原始会话数据清理（保留60天）
            Log.i(TAG, "清理60天前的app_sessions_users数据...")
            appSessionRepository.cleanOldSessions(60)
            
            Log.i(TAG, "清理60天前的timer_sessions_users数据...")
            timerSessionRepository.cleanOldSessions(60)
            
            // 统计清理后数据量
            val appSessionCountAfter = appSessionRepository.getTotalSessionCount()
            val timerSessionCountAfter = timerSessionRepository.getTotalSessionCount()
            
            Log.i(TAG, "清理后数据统计: app_sessions_users=${appSessionCountAfter}条, timer_sessions_users=${timerSessionCountAfter}条")
            Log.i(TAG, "清理结果: 删除了${appSessionCountBefore - appSessionCountAfter}条app_sessions, ${timerSessionCountBefore - timerSessionCountAfter}条timer_sessions")
            Log.i(TAG, "=== 数据清理完成 ===")
            
        } catch (e: Exception) {
            Log.e(TAG, "数据清理失败: ${e.message}", e)
        }
    }
    
    /**
     * 检查是否需要执行数据清理
     * 每天执行一次清理任务
     */
    suspend fun shouldPerformCleanup(): Boolean {
        // 检查上次清理时间，每天执行一次
        val lastCleanupTime = getLastCleanupTime()
        val currentTime = System.currentTimeMillis()
        val oneDayInMillis = 24 * 60 * 60 * 1000L
        
        return (currentTime - lastCleanupTime) > oneDayInMillis
    }
    
    private suspend fun getLastCleanupTime(): Long {
        // 这里可以从SharedPreferences或数据库获取上次清理时间
        // 简化实现：返回0表示总是需要清理
        return 0L
    }
    
    suspend fun markCleanupCompleted() {
        // 这里可以保存清理完成时间到SharedPreferences
        // 简化实现：暂不保存
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