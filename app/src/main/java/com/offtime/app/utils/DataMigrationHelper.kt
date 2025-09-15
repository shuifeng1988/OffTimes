package com.offtime.app.utils

import android.util.Log
import android.content.Context
import com.offtime.app.data.dao.AppCategoryDao
import com.offtime.app.data.dao.AppSessionUserDao
import com.offtime.app.data.dao.DailyUsageDao
import com.offtime.app.data.dao.SummaryUsageDao
import com.offtime.app.data.dao.TimerSessionUserDao
import com.offtime.app.service.DataAggregationService
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 数据迁移助手类
 * 用于修复数据库中错误的分类ID，彻底覆盖原始数据
 */
@Singleton
class DataMigrationHelper @Inject constructor(
    private val categoryDao: AppCategoryDao,
    private val dailyUsageDao: DailyUsageDao,
    private val appSessionUserDao: AppSessionUserDao,
    private val timerSessionUserDao: TimerSessionUserDao,
    private val summaryUsageDao: SummaryUsageDao
) {
    
    companion object {
        private const val TAG = "DataMigrationHelper"
        private const val WRONG_FITNESS_ID = 6 // 错误的健身分类ID
    }
    
    /**
     * 执行完整的数据迁移
     * @return 迁移结果报告
     */
    suspend fun executeFullMigration(): MigrationResult {
        Log.d(TAG, "===== 开始执行完整数据迁移 =====")
        
        val result = MigrationResult()
        
        try {
            // 1. 获取正确的分类映射
            val categoryMapping = buildCategoryMapping()
            if (categoryMapping.isEmpty()) {
                Log.e(TAG, "❌ 无法获取分类映射，迁移中止")
                result.success = false
                result.errorMessage = "无法获取分类映射"
                return result
            }
            
            Log.d(TAG, "📊 分类映射: $categoryMapping")
            
            // 2. 迁移 daily_usage_user 表
            val dailyMigrated = migrateDailyUsageTable(categoryMapping)
            result.dailyUsageMigrated = dailyMigrated
            Log.d(TAG, "daily_usage_user 表迁移完成: $dailyMigrated 条记录")
            
            // 3. 迁移 app_sessions_users 表
            val sessionsMigrated = migrateAppSessionsTable(categoryMapping)
            result.appSessionsMigrated = sessionsMigrated
            Log.d(TAG, "app_sessions_users 表迁移完成: $sessionsMigrated 条记录")
            
            // 4. 迁移 timer_sessions_users 表
            val timerMigrated = migrateTimerSessionsTable(categoryMapping)
            result.timerSessionsMigrated = timerMigrated
            Log.d(TAG, "timer_sessions_users 表迁移完成: $timerMigrated 条记录")
            
            // 5. 迁移 summary_usage 相关表
            val summaryMigrated = migrateSummaryUsageTables(categoryMapping)
            result.summaryUsageMigrated = summaryMigrated
            Log.d(TAG, "summary_usage 相关表迁移完成: $summaryMigrated 条记录")
            
            result.success = true
            result.totalMigrated = dailyMigrated + sessionsMigrated + timerMigrated + summaryMigrated
            
            Log.d(TAG, "✅ 数据迁移完成! 总计迁移: ${result.totalMigrated} 条记录")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 数据迁移失败", e)
            result.success = false
            result.errorMessage = e.message ?: "未知错误"
        }
        
        Log.d(TAG, "===== 数据迁移结束 =====")
        return result
    }
    
    /**
     * 构建分类ID映射关系 (错误ID -> 正确ID)
     */
    private suspend fun buildCategoryMapping(): Map<Int, Int> {
        val mapping = mutableMapOf<Int, Int>()
        
        val categories = categoryDao.getAllCategories().first()
        // 查找健身分类的正确ID
        val fitnessCategory = categories.find { it.name == "健身" }
        if (fitnessCategory != null && fitnessCategory.id != WRONG_FITNESS_ID) {
            mapping[WRONG_FITNESS_ID] = fitnessCategory.id
            Log.d(TAG, "健身分类映射: $WRONG_FITNESS_ID -> ${fitnessCategory.id}")
        }
        
        // 这里可以扩展其他错误映射...
        
        return mapping
    }
    
    /**
     * 迁移 daily_usage_user 表
     */
    private suspend fun migrateDailyUsageTable(mapping: Map<Int, Int>): Int {
        Log.d(TAG, "开始迁移 daily_usage_user 表...")
        
        var migratedCount = 0
        val allData = dailyUsageDao.getAllDailyUsageData()
        
        for (record in allData) {
            val correctId = mapping[record.catId]
            if (correctId != null) {
                // 更新记录的分类ID
                val updatedRecord = record.copy(
                    id = "${record.date}_${correctId}_${record.slotIndex}_${record.isOffline}",
                    catId = correctId,
                    updateTime = System.currentTimeMillis()
                )
                
                // 使用 upsert 覆盖原数据
                dailyUsageDao.upsert(updatedRecord)
                migratedCount++
                
                Log.d(TAG, "迁移记录: ${record.id} -> catId ${record.catId} to $correctId")
            }
        }
        
        return migratedCount
    }
    
    /**
     * 迁移 app_sessions_users 表
     */
    private suspend fun migrateAppSessionsTable(mapping: Map<Int, Int>): Int {
        Log.d(TAG, "开始迁移 app_sessions_users 表...")
        
        var migratedCount = 0
        val allSessions = appSessionUserDao.getAllSessions()
        
        for (session in allSessions) {
            val correctId = mapping[session.catId]
            if (correctId != null) {
                // 更新会话的分类ID
                val updatedSession = session.copy(
                    catId = correctId,
                    updateTime = System.currentTimeMillis()
                )
                
                // 使用 insertSession 的 REPLACE 策略来覆盖
                appSessionUserDao.updateSession(updatedSession)
                migratedCount++
                
                Log.d(TAG, "迁移会话: ID=${session.id}, 包名=${session.pkgName}, catId ${session.catId} -> $correctId")
            }
        }
        
        return migratedCount
    }
    
    /**
     * 迁移 timer_sessions_users 表
     */
    private suspend fun migrateTimerSessionsTable(mapping: Map<Int, Int>): Int {
        Log.d(TAG, "开始迁移 timer_sessions_users 表...")
        
        var migratedCount = 0
        val allTimerSessions = timerSessionUserDao.getAllUserSessionsList()
        
        for (session in allTimerSessions) {
            val correctId = mapping[session.catId]
            if (correctId != null) {
                // 更新计时会话的分类ID
                val updatedSession = session.copy(
                    catId = correctId,
                    updateTime = System.currentTimeMillis()
                )
                
                timerSessionUserDao.updateUserSession(updatedSession)
                migratedCount++
                
                Log.d(TAG, "迁移计时会话: ID=${session.id}, 程序=${session.programName}, catId ${session.catId} -> $correctId")
            }
        }
        
        return migratedCount
    }
    
    /**
     * 迁移 summary_usage 相关表
     */
    private suspend fun migrateSummaryUsageTables(mapping: Map<Int, Int>): Int {
        Log.d(TAG, "开始迁移 summary_usage 相关表...")
        
        var migratedCount = 0
        
        // 迁移日汇总表
        val dailySummary = summaryUsageDao.getAllSummaryUsageUser()
        for (summary in dailySummary) {
            val correctId = mapping[summary.catId]
            if (correctId != null) {
                val updatedSummary = summary.copy(
                    id = "${summary.date}_$correctId",
                    catId = correctId,
                    updateTime = System.currentTimeMillis()
                )
                summaryUsageDao.upsert(updatedSummary)
                migratedCount++
            }
        }
        
        // 迁移周汇总表
        val weeklySummary = summaryUsageDao.getAllSummaryUsageWeek()
        for (summary in weeklySummary) {
            val correctId = mapping[summary.catId]
            if (correctId != null) {
                val updatedSummary = summary.copy(
                    id = "${summary.weekStartDate}_$correctId",
                    catId = correctId,
                    updateTime = System.currentTimeMillis()
                )
                summaryUsageDao.upsertWeek(updatedSummary)
                migratedCount++
            }
        }
        
        // 迁移月汇总表
        val monthlySummary = summaryUsageDao.getAllSummaryUsageMonth()
        for (summary in monthlySummary) {
            val correctId = mapping[summary.catId]
            if (correctId != null) {
                val updatedSummary = summary.copy(
                    id = "${summary.month}_$correctId",
                    catId = correctId,
                    updateTime = System.currentTimeMillis()
                )
                summaryUsageDao.upsertMonth(updatedSummary)
                migratedCount++
            }
        }
        
        return migratedCount
    }
    
    /**
     * 检查是否需要迁移
     */
    suspend fun needsMigration(): Boolean {
        try {
            val categories = categoryDao.getAllCategories().first()
            val fitnessCategory = categories.find { it.name == "健身" }
            
            if (fitnessCategory == null || fitnessCategory.id == WRONG_FITNESS_ID) {
                return false // 没有健身分类或ID已经是6，不需要迁移
            } else {
                // 检查是否存在错误的ID=6的数据
                val dailyData = dailyUsageDao.getAllDailyUsageData()
                val hasWrongData = dailyData.any { it.catId == WRONG_FITNESS_ID }
                
                Log.d(TAG, "检查迁移需求: 健身分类正确ID=${fitnessCategory.id}, 是否有错误数据=$hasWrongData")
                return hasWrongData
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "检查迁移需求失败", e)
            return false
        }
    }
    
    /**
     * 获取迁移状态报告
     */
    suspend fun getMigrationReport(): MigrationReport {
        val report = MigrationReport()
        
        val categories = categoryDao.getAllCategories().first()
        // 统计各表中的分类ID分布
        val dailyData = dailyUsageDao.getAllDailyUsageData()
        report.dailyUsageByCategory = dailyData.groupBy { it.catId }.mapValues { it.value.size }
        
        val sessionsData = appSessionUserDao.getAllSessions()
        report.appSessionsByCategory = sessionsData.groupBy { it.catId }.mapValues { it.value.size }
        
        val timerData = timerSessionUserDao.getAllUserSessionsList()
        report.timerSessionsByCategory = timerData.groupBy { it.catId }.mapValues { it.value.size }
        
        val summaryData = summaryUsageDao.getAllSummaryUsageUser()
        report.summaryUsageByCategory = summaryData.groupBy { it.catId }.mapValues { it.value.size }
        
        // 分析问题
        val validCategoryIds = categories.map { it.id }.toSet()
        report.invalidCategoryIds = (dailyData.map { it.catId } + 
                                   sessionsData.map { it.catId } + 
                                   timerData.map { it.catId } + 
                                   summaryData.map { it.catId })
                                  .distinct()
                                  .filter { it !in validCategoryIds }
        
        report.categoryMapping = categories.associate { it.id to it.name }
        
        return report
    }
}

/**
 * 迁移结果
 */
data class MigrationResult(
    var success: Boolean = false,
    var totalMigrated: Int = 0,
    var dailyUsageMigrated: Int = 0,
    var appSessionsMigrated: Int = 0,
    var timerSessionsMigrated: Int = 0,
    var summaryUsageMigrated: Int = 0,
    var errorMessage: String? = null
)

/**
 * 迁移状态报告
 */
data class MigrationReport(
    var dailyUsageByCategory: Map<Int, Int> = emptyMap(),
    var appSessionsByCategory: Map<Int, Int> = emptyMap(),
    var timerSessionsByCategory: Map<Int, Int> = emptyMap(),
    var summaryUsageByCategory: Map<Int, Int> = emptyMap(),
    var invalidCategoryIds: List<Int> = emptyList(),
    var categoryMapping: Map<Int, String> = emptyMap()
) 