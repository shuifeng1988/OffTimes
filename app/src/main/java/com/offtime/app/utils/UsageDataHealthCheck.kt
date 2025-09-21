package com.offtime.app.utils

import android.content.Context
import android.util.Log
import com.offtime.app.data.database.OffTimeDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 使用数据健康检查工具
 * 用于检测和修复异常的使用时间记录
 */
object UsageDataHealthCheck {
    
    private const val TAG = "UsageDataHealthCheck"
    
    /**
     * 检查结果数据类
     */
    data class HealthCheckResult(
        val totalSessions: Int,
        val suspiciousSessions: List<SuspiciousSession>,
        val duplicateSessions: List<DuplicateSessionGroup>,
        val recommendations: List<String>
    )
    
    /**
     * 可疑会话数据类
     */
    data class SuspiciousSession(
        val packageName: String,
        val date: String,
        val duration: Int, // 秒
        val durationHours: Double,
        val startTime: Long,
        val endTime: Long,
        val reason: String
    )
    
    /**
     * 重复会话组数据类
     */
    data class DuplicateSessionGroup(
        val packageName: String,
        val date: String,
        val sessions: List<SessionInfo>,
        val totalDuplicateDuration: Int
    )
    
    /**
     * 会话信息数据类
     */
    data class SessionInfo(
        val id: Int,
        val startTime: Long,
        val endTime: Long,
        val duration: Int
    )
    
    /**
     * 执行健康检查
     */
    suspend fun performHealthCheck(context: Context): HealthCheckResult = withContext(Dispatchers.IO) {
        val database = OffTimeDatabase.getDatabase(context)
        val appSessionDao = database.appSessionUserDao()
        
        Log.d(TAG, "开始使用数据健康检查...")
        
        // 获取最近7天的所有会话
        val recentDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L)
        
        val allSessions = appSessionDao.getSessionsSinceDate(recentDate)
        Log.d(TAG, "检查 ${allSessions.size} 个会话记录")
        
        val suspiciousSessions = mutableListOf<SuspiciousSession>()
        val duplicateGroups = mutableListOf<DuplicateSessionGroup>()
        
        // 1. 检查异常长时间会话
        allSessions.forEach { session ->
            val durationHours = session.durationSec / 3600.0
            val threshold = 6.0 // 普通应用：6小时
            
            if (durationHours > threshold) {
                suspiciousSessions.add(
                    SuspiciousSession(
                        packageName = session.pkgName,
                        date = session.date,
                        duration = session.durationSec,
                        durationHours = durationHours,
                        startTime = session.startTime,
                        endTime = session.endTime,
                        reason = "超长使用时间(${String.format("%.1f", durationHours)}小时)"
                    )
                )
            }
        }
        
        // 2. 检查重复和重叠会话
        val sessionsByApp = allSessions.groupBy { "${it.pkgName}_${it.date}" }
        
        sessionsByApp.forEach { (appDate, sessions) ->
            if (sessions.size > 1) {
                val sortedSessions = sessions.sortedBy { it.startTime }
                val overlapping = mutableListOf<SessionInfo>()
                
                for (i in 0 until sortedSessions.size - 1) {
                    val current = sortedSessions[i]
                    val next = sortedSessions[i + 1]
                    
                    // 检查重叠（当前会话的结束时间晚于下一个会话的开始时间）
                    if (current.endTime > next.startTime) {
                        overlapping.add(
                            SessionInfo(current.id, current.startTime, current.endTime, current.durationSec)
                        )
                        overlapping.add(
                            SessionInfo(next.id, next.startTime, next.endTime, next.durationSec)
                        )
                    }
                }
                
                if (overlapping.isNotEmpty()) {
                    val parts = appDate.split("_")
                    duplicateGroups.add(
                        DuplicateSessionGroup(
                            packageName = parts[0],
                            date = parts[1],
                            sessions = overlapping.distinctBy { it.id },
                            totalDuplicateDuration = overlapping.distinctBy { it.id }.sumOf { it.duration }
                        )
                    )
                }
            }
        }
        
        // 生成建议
        val recommendations = mutableListOf<String>()
        
        if (suspiciousSessions.isNotEmpty()) {
            recommendations.add("发现 ${suspiciousSessions.size} 个异常超长使用记录，建议检查是否为后台运行干扰")
        }
        
        if (duplicateGroups.isNotEmpty()) {
            recommendations.add("发现 ${duplicateGroups.size} 组重复/重叠会话，可能导致使用时间重复计算")
        }
        
        if (suspiciousSessions.isEmpty() && duplicateGroups.isEmpty()) {
            recommendations.add("使用数据健康状况良好，未发现异常")
        }
        
        val result = HealthCheckResult(
            totalSessions = allSessions.size,
            suspiciousSessions = suspiciousSessions,
            duplicateSessions = duplicateGroups,
            recommendations = recommendations
        )
        
        Log.d(TAG, "健康检查完成: 总会话${result.totalSessions}个, 可疑${result.suspiciousSessions.size}个, 重复${result.duplicateSessions.size}组")
        
        result
    }
    
    /**
     * 清理异常的超长会话
     */
    suspend fun cleanupSuspiciousSessions(
        context: Context,
        suspiciousSessions: List<SuspiciousSession>
    ): Int = withContext(Dispatchers.IO) {
        val database = OffTimeDatabase.getDatabase(context)
        val appSessionDao = database.appSessionUserDao()
        
        var cleanedCount = 0
        
        suspiciousSessions.forEach { suspicious ->
            try {
                // 查找对应的会话记录
                val sessions = appSessionDao.getSessionsByDate(suspicious.date)
                val targetSession = sessions.find { 
                    it.pkgName == suspicious.packageName && 
                    it.startTime == suspicious.startTime &&
                    it.endTime == suspicious.endTime
                }
                
                if (targetSession != null) {
                    // 删除异常会话
                    appSessionDao.deleteSessionById(targetSession.id)
                    cleanedCount++
                    
                    Log.d(TAG, "已清理异常会话: ${suspicious.packageName}, 时长:${suspicious.durationHours}小时")
                }
            } catch (e: Exception) {
                Log.e(TAG, "清理异常会话失败: ${suspicious.packageName}", e)
            }
        }
        
        Log.d(TAG, "清理完成，共清理 $cleanedCount 个异常会话")
        cleanedCount
    }
    
    /**
     * 修复重复会话（合并重叠的会话）
     */
    suspend fun fixDuplicateSessions(
        context: Context,
        duplicateGroups: List<DuplicateSessionGroup>
    ): Int = withContext(Dispatchers.IO) {
        val database = OffTimeDatabase.getDatabase(context)
        val appSessionDao = database.appSessionUserDao()
        
        var fixedCount = 0
        
        duplicateGroups.forEach { group ->
            try {
                val sessions = group.sessions.sortedBy { it.startTime }
                if (sessions.size >= 2) {
                    // 合并为一个会话：使用最早的开始时间和最晚的结束时间
                    val mergedStartTime = sessions.minOf { it.startTime }
                    val mergedEndTime = sessions.maxOf { it.endTime }
                    val mergedDuration = ((mergedEndTime - mergedStartTime) / 1000).toInt()
                    
                    // 保留第一个会话，更新其时间范围
                    val firstSession = sessions.first()
                    appSessionDao.updateSessionTimeRange(
                        sessionId = firstSession.id,
                        newStartTime = mergedStartTime,
                        newEndTime = mergedEndTime,
                        newDurationSec = mergedDuration
                    )
                    
                    // 删除其他重复会话
                    sessions.drop(1).forEach { session ->
                        appSessionDao.deleteSessionById(session.id)
                    }
                    
                    fixedCount++
                    Log.d(TAG, "已修复重复会话: ${group.packageName}, 合并了${sessions.size}个会话")
                }
            } catch (e: Exception) {
                Log.e(TAG, "修复重复会话失败: ${group.packageName}", e)
            }
        }
        
        Log.d(TAG, "修复完成，共修复 $fixedCount 组重复会话")
        fixedCount
    }
    
    /**
     * 获取使用数据摘要信息
     */
    suspend fun getUsageDataSummary(context: Context): String = withContext(Dispatchers.IO) {
        val healthResult = performHealthCheck(context)
        
        buildString {
            appendLine("📊 使用数据健康报告")
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine("📈 总会话数: ${healthResult.totalSessions}")
            appendLine("⚠️ 异常会话: ${healthResult.suspiciousSessions.size}")
            appendLine("🔄 重复会话组: ${healthResult.duplicateSessions.size}")
            appendLine()
            
            if (healthResult.suspiciousSessions.isNotEmpty()) {
                appendLine("🚨 异常超长会话:")
                healthResult.suspiciousSessions.take(5).forEach { session ->
                    appendLine("  • ${session.packageName}: ${String.format("%.1f", session.durationHours)}小时")
                }
                if (healthResult.suspiciousSessions.size > 5) {
                    appendLine("  • ...还有${healthResult.suspiciousSessions.size - 5}个")
                }
                appendLine()
            }
            
            appendLine("💡 建议:")
            healthResult.recommendations.forEach { recommendation ->
                appendLine("  • $recommendation")
            }
        }
    }
} 