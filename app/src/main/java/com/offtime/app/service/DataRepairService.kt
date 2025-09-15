package com.offtime.app.service

import android.util.Log
import com.offtime.app.data.repository.AppSessionRepository
import com.offtime.app.utils.UsageDataValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 数据修复服务
 * 
 * 核心功能：
 * 1. 修复饼图总时长与详情时长不一致问题
 * 2. 清理重复会话记录
 * 3. 修复屏幕熄屏期间的错误统计
 * 4. 重新聚合不一致的数据
 * 5. 提供完整的数据修复报告
 */
@Singleton
class DataRepairService @Inject constructor(
    private val usageDataValidator: UsageDataValidator,
    private val appSessionRepository: AppSessionRepository
) {
    
    companion object {
        private const val TAG = "DataRepairService"
    }
    
    /**
     * 执行完整的数据修复流程
     */
    suspend fun performCompleteDataRepair(date: String = getCurrentDate()): DataRepairResult = withContext(Dispatchers.IO) {
        Log.i(TAG, "🔧 开始执行完整数据修复: $date")
        
        val result = DataRepairResult(date)
        
        try {
            // 第1步：数据一致性检查
            Log.i(TAG, "第1步：执行数据一致性检查...")
            val validationReports = usageDataValidator.performFullValidation(date)
            result.validationReports = validationReports
            
            val inconsistentCategories = validationReports.filter { !it.isConsistent }
            val totalDuplicates = validationReports.sumOf { it.duplicateSessions.size }
            val totalSuspicious = validationReports.sumOf { it.screenOffSessions.size }
            
            Log.i(TAG, "检查结果: 不一致分类=${inconsistentCategories.size}, 重复会话=${totalDuplicates}, 可疑会话=${totalSuspicious}")
            
            // 第2步：自动修复检测到的问题
            if (totalDuplicates > 0 || totalSuspicious > 0) {
                Log.i(TAG, "第2步：自动修复检测到的问题...")
                val fixedCount = usageDataValidator.autoFixDetectedIssues(validationReports)
                result.fixedIssuesCount = fixedCount
                Log.i(TAG, "修复完成，共修复 $fixedCount 个问题")
            } else {
                Log.i(TAG, "第2步：未发现需要修复的重复或可疑会话")
            }
            
            // 第3步：清理系统应用的错误记录
            Log.i(TAG, "第3步：清理系统应用错误记录...")
            try {
                appSessionRepository.cleanSystemAppSessions()
                result.systemAppCleaned = true
            } catch (e: Exception) {
                Log.e(TAG, "清理系统应用记录失败", e)
            }
            
            // 第4步：重新计算汇总数据
            if (inconsistentCategories.isNotEmpty() || result.fixedIssuesCount > 0) {
                Log.i(TAG, "第4步：重新计算汇总数据...")
                val recalculateSuccess = usageDataValidator.recalculateSummaryData(date)
                result.summaryRecalculated = recalculateSuccess
                
                if (recalculateSuccess) {
                    Log.i(TAG, "汇总数据重新计算成功")
                } else {
                    Log.w(TAG, "汇总数据重新计算失败")
                }
            } else {
                Log.i(TAG, "第4步：数据一致，无需重新计算汇总")
            }
            
            // 第5步：验证修复结果
            Log.i(TAG, "第5步：验证修复结果...")
            val finalValidation = usageDataValidator.performFullValidation(date)
            val finalInconsistentCount = finalValidation.count { !it.isConsistent }
            val finalDuplicateCount = finalValidation.sumOf { it.duplicateSessions.size }
            val finalSuspiciousCount = finalValidation.sumOf { it.screenOffSessions.size }
            
            result.finalValidationReports = finalValidation
            result.isRepairSuccessful = finalInconsistentCount <= inconsistentCategories.size / 2 && // 不一致分类减少一半以上
                    finalDuplicateCount == 0 && // 所有重复记录已清理
                    finalSuspiciousCount <= totalSuspicious / 2 // 可疑记录减少一半以上
            
            Log.i(TAG, "修复验证: 不一致分类=${finalInconsistentCount}, 重复会话=${finalDuplicateCount}, 可疑会话=${finalSuspiciousCount}")
            Log.i(TAG, "修复${if (result.isRepairSuccessful) "成功" else "部分完成"}")
            
            // 第6步：生成修复报告
            result.repairReport = generateRepairReport(validationReports, finalValidation, result)
            
        } catch (e: Exception) {
            Log.e(TAG, "数据修复过程出错", e)
            result.errorMessage = e.message
        }
        
        return@withContext result
    }
    
    /**
     * 针对特定分类的快速修复
     */
    suspend fun repairSpecificCategory(categoryId: Int, date: String = getCurrentDate()): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "🎯 开始修复特定分类: categoryId=$categoryId, date=$date")
        
        try {
            // 验证该分类的数据
            @Suppress("UNUSED_VARIABLE")
            val categories = mutableListOf<com.offtime.app.data.entity.AppCategoryEntity>()
            // 这里需要从数据库获取分类信息，简化处理
            val validationReports = usageDataValidator.performFullValidation(date)
            val categoryReport = validationReports.find { it.categoryId == categoryId }
            
            if (categoryReport == null) {
                Log.w(TAG, "未找到指定分类的数据: categoryId=$categoryId")
                return@withContext false
            }
            
            // 修复该分类的问题
            val fixedCount = usageDataValidator.autoFixDetectedIssues(listOf(categoryReport))
            
            // 重新计算该分类的汇总数据
            val recalculateSuccess = usageDataValidator.recalculateSummaryData(date)
            
            Log.i(TAG, "分类修复完成: 修复问题=$fixedCount, 重新计算=${recalculateSuccess}")
            return@withContext fixedCount > 0 || recalculateSuccess
            
        } catch (e: Exception) {
            Log.e(TAG, "修复特定分类失败", e)
            return@withContext false
        }
    }
    
    /**
     * 检测并修复抖音重复记录问题
     */
    suspend fun fixDouYinDuplicates(date: String = getCurrentDate()): Int = withContext(Dispatchers.IO) {
        Log.i(TAG, "🎵 专项修复抖音重复记录: $date")
        
        try {
            // 获取抖音的所有会话记录
            val douYinSessions = appSessionRepository.getSessionsByPackageName("com.ss.android.ugc.aweme", date)
            Log.i(TAG, "找到抖音会话记录: ${douYinSessions.size}条")
            
            if (douYinSessions.isEmpty()) {
                return@withContext 0
            }
            
            // 检测重复和异常长时间记录
            var fixedCount = 0
            val sortedSessions = douYinSessions.sortedBy { it.startTime }
            
            for (i in 0 until sortedSessions.size - 1) {
                val current = sortedSessions[i]
                val next = sortedSessions[i + 1]
                
                val currentEnd = current.startTime + (current.durationSec * 1000L)
                val nextStart = next.startTime
                
                // 检查重叠
                if (currentEnd > nextStart) {
                    val overlapMs = currentEnd - nextStart
                    if (overlapMs > 30_000) { // 重叠超过30秒
                        // 删除较新的记录
                        appSessionRepository.deleteSessionById(next.id)
                        Log.d(TAG, "删除抖音重复记录: sessionId=${next.id}, 重叠时间=${overlapMs/1000}秒")
                        fixedCount++
                    }
                }
                
                // 检查异常长时间（超过2小时）
                if (current.durationSec > 7200) {
                    // 调整为合理时长（最多1小时）
                    val reasonableDuration = minOf(current.durationSec, 3600)
                    appSessionRepository.updateSessionDuration(current.id, reasonableDuration)
                    Log.d(TAG, "调整抖音异常时长: sessionId=${current.id}, ${current.durationSec}s -> ${reasonableDuration}s")
                    fixedCount++
                }
            }
            
            Log.i(TAG, "抖音专项修复完成，修复记录: $fixedCount 条")
            return@withContext fixedCount
            
        } catch (e: Exception) {
            Log.e(TAG, "抖音专项修复失败", e)
            return@withContext 0
        }
    }
    
    /**
     * 生成修复报告
     */
    private fun generateRepairReport(
        beforeReports: List<UsageDataValidator.ValidationReport>,
        afterReports: List<UsageDataValidator.ValidationReport>,
        result: DataRepairResult
    ): String {
        val sb = StringBuilder()
        sb.appendLine("📋 数据修复报告")
        sb.appendLine("日期: ${result.date}")
        sb.appendLine("=".repeat(50))
        sb.appendLine()
        
        sb.appendLine("📊 修复前统计:")
        sb.appendLine("  不一致分类: ${beforeReports.count { !it.isConsistent }}")
        sb.appendLine("  重复会话: ${beforeReports.sumOf { it.duplicateSessions.size }}")
        sb.appendLine("  可疑会话: ${beforeReports.sumOf { it.screenOffSessions.size }}")
        sb.appendLine()
        
        sb.appendLine("🔧 执行的修复操作:")
        sb.appendLine("  修复问题数量: ${result.fixedIssuesCount}")
        sb.appendLine("  系统应用清理: ${if (result.systemAppCleaned) "✅" else "❌"}")
        sb.appendLine("  汇总数据重算: ${if (result.summaryRecalculated) "✅" else "❌"}")
        sb.appendLine()
        
        sb.appendLine("📊 修复后统计:")
        sb.appendLine("  不一致分类: ${afterReports.count { !it.isConsistent }}")
        sb.appendLine("  重复会话: ${afterReports.sumOf { it.duplicateSessions.size }}")
        sb.appendLine("  可疑会话: ${afterReports.sumOf { it.screenOffSessions.size }}")
        sb.appendLine()
        
        sb.appendLine("🎯 修复结果: ${if (result.isRepairSuccessful) "成功 ✅" else "部分完成 ⚠️"}")
        
        if (!result.errorMessage.isNullOrEmpty()) {
            sb.appendLine()
            sb.appendLine("❌ 错误信息: ${result.errorMessage}")
        }
        
        return sb.toString()
    }
    
    private fun getCurrentDate(): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
    }
    
    /**
     * 数据修复结果
     */
    data class DataRepairResult(
        val date: String,
        var validationReports: List<UsageDataValidator.ValidationReport> = emptyList(),
        var finalValidationReports: List<UsageDataValidator.ValidationReport> = emptyList(),
        var fixedIssuesCount: Int = 0,
        var systemAppCleaned: Boolean = false,
        var summaryRecalculated: Boolean = false,
        var isRepairSuccessful: Boolean = false,
        var repairReport: String = "",
        var errorMessage: String? = null
    )
} 