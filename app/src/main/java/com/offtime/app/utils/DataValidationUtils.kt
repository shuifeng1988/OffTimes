package com.offtime.app.utils

import android.content.Context
import android.util.Log
import com.offtime.app.data.dao.DailyUsageDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 数据验证和修复工具类
 * 
 * 功能：
 * - 检测柱状图数据中单小时使用时间超过60分钟的异常
 * - 修正异常数据，确保单小时不超过3600秒
 * - 提供数据完整性验证
 */
@Singleton
class DataValidationUtils @Inject constructor(
    private val dailyUsageDao: DailyUsageDao
) {
    companion object {
        private const val TAG = "DataValidationUtils"
        private const val MAX_HOUR_DURATION_SEC = 3600 // 1小时 = 3600秒
    }
    
    /**
     * 检测和修复指定日期的异常小时数据
     * 
     * @param date 要检查的日期，格式: yyyy-MM-dd
     * @return 修复的记录数量
     */
    suspend fun fixAnomalousHourlyData(date: String): Int {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "开始检查和修复异常小时数据: $date")
                
                // 查询所有当天的小时数据
                val allHourlyData = dailyUsageDao.getAllHourlyDataByDate(date)
                Log.d(TAG, "找到 ${allHourlyData.size} 条小时数据记录")
                
                var fixedCount = 0
                
                allHourlyData.forEach { entity ->
                    if (entity.durationSec > MAX_HOUR_DURATION_SEC) {
                        val originalDuration = entity.durationSec
                        val originalMinutes = originalDuration / 60
                        
                        Log.w(TAG, "发现异常数据: catId=${entity.catId}, hour=${entity.slotIndex}, " +
                                "duration=${originalDuration}s(${originalMinutes}分钟) 超过1小时限制")
                        
                        // 修正为最大允许值
                        val fixedEntity = entity.copy(durationSec = MAX_HOUR_DURATION_SEC)
                        dailyUsageDao.upsert(fixedEntity)
                        
                        fixedCount++
                        Log.w(TAG, "已修正异常数据: catId=${entity.catId}, hour=${entity.slotIndex}, " +
                                "${originalDuration}s -> ${MAX_HOUR_DURATION_SEC}s")
                    }
                }
                
                Log.d(TAG, "异常数据修复完成: 共修复 $fixedCount 条记录")
                fixedCount
                
            } catch (e: Exception) {
                Log.e(TAG, "修复异常数据时发生错误", e)
                0
            }
        }
    }
    
    /**
     * 检测和修复最近N天的异常小时数据
     * 
     * @param days 要检查的天数，从今天往前推
     * @return 修复的记录数量
     */
    suspend fun fixRecentAnomalousData(days: Int = 7): Int {
        return withContext(Dispatchers.IO) {
            var totalFixedCount = 0
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val calendar = java.util.Calendar.getInstance()
            
            Log.d(TAG, "开始检查最近 $days 天的异常数据")
            
            for (i in 0 until days) {
                calendar.time = java.util.Date()
                calendar.add(java.util.Calendar.DAY_OF_YEAR, -i)
                val date = dateFormat.format(calendar.time)
                
                val dailyFixedCount = fixAnomalousHourlyData(date)
                totalFixedCount += dailyFixedCount
                
                if (dailyFixedCount > 0) {
                    Log.w(TAG, "日期 $date: 修复了 $dailyFixedCount 条异常记录")
                }
            }
            
            Log.d(TAG, "最近 $days 天异常数据修复完成: 总共修复 $totalFixedCount 条记录")
            totalFixedCount
        }
    }
    
    /**
     * 验证指定日期的数据完整性
     * 
     * @param date 要验证的日期
     * @return 验证报告
     */
    suspend fun validateDataIntegrity(date: String): DataValidationReport {
        return withContext(Dispatchers.IO) {
            try {
                val allHourlyData = dailyUsageDao.getAllHourlyDataByDate(date)
                
                var totalRecords = allHourlyData.size
                var anomalousRecords = 0
                var maxAnomalousDuration = 0
                var totalDuration = 0
                
                allHourlyData.forEach { entity ->
                    totalDuration += entity.durationSec
                    
                    if (entity.durationSec > MAX_HOUR_DURATION_SEC) {
                        anomalousRecords++
                        if (entity.durationSec > maxAnomalousDuration) {
                            maxAnomalousDuration = entity.durationSec
                        }
                    }
                }
                
                DataValidationReport(
                    date = date,
                    totalRecords = totalRecords,
                    anomalousRecords = anomalousRecords,
                    maxAnomalousDurationSec = maxAnomalousDuration,
                    totalDurationSec = totalDuration,
                    isValid = anomalousRecords == 0
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "验证数据完整性时发生错误", e)
                DataValidationReport(
                    date = date,
                    totalRecords = 0,
                    anomalousRecords = 0,
                    maxAnomalousDurationSec = 0,
                    totalDurationSec = 0,
                    isValid = false,
                    error = e.message
                )
            }
        }
    }
    
    /**
     * 数据验证报告
     */
    data class DataValidationReport(
        val date: String,
        val totalRecords: Int,
        val anomalousRecords: Int,
        val maxAnomalousDurationSec: Int,
        val totalDurationSec: Int,
        val isValid: Boolean,
        val error: String? = null
    ) {
        fun getMaxAnomalousDurationMinutes(): Double = maxAnomalousDurationSec / 60.0
        fun getTotalDurationHours(): Double = totalDurationSec / 3600.0
        
        override fun toString(): String {
            return if (error != null) {
                "验证失败: $error"
            } else {
                """
                数据验证报告 ($date):
                - 总记录数: $totalRecords
                - 异常记录数: $anomalousRecords
                - 最大异常时长: ${getMaxAnomalousDurationMinutes()}分钟
                - 总使用时长: ${getTotalDurationHours()}小时
                - 数据状态: ${if (isValid) "正常" else "异常"}
                """.trimIndent()
            }
        }
    }
} 