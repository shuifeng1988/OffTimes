package com.offtime.app.data.dao

import androidx.room.*
import com.offtime.app.data.entity.SummaryUsageUserEntity
import com.offtime.app.data.entity.SummaryUsageWeekUserEntity
import com.offtime.app.data.entity.SummaryUsageMonthUserEntity
import kotlinx.coroutines.flow.Flow

data class UsageData(
    val period: String,  // 日期、周、月
    val usageMinutes: Int,
    val completionRate: Float // 完成度百分比
)

@Dao
interface SummaryUsageDao {
    
    // 插入或更新操作
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SummaryUsageUserEntity)
    
    // 日汇总相关查询
    @Query("SELECT * FROM summary_usage_user WHERE catId = :catId ORDER BY date DESC LIMIT :limit")
    suspend fun getDailyUsage(catId: Int, limit: Int = 7): List<SummaryUsageUserEntity>
    
    @Query("SELECT date as period, (totalSec / 60) as usageMinutes, 0.0 as completionRate FROM summary_usage_user WHERE catId = :catId ORDER BY date DESC LIMIT :limit")
    suspend fun getDailyUsageData(catId: Int, limit: Int = 7): List<UsageData>
    
    // 周汇总相关查询
    @Query("SELECT * FROM summary_usage_week_user WHERE catId = :catId ORDER BY weekStartDate DESC LIMIT :limit")
    suspend fun getWeeklyUsage(catId: Int, limit: Int = 4): List<SummaryUsageWeekUserEntity>
    
    @Query("SELECT weekStartDate as period, (avgDailySec / 60) as usageMinutes, 0.0 as completionRate FROM summary_usage_week_user WHERE catId = :catId ORDER BY weekStartDate DESC LIMIT :limit")
    suspend fun getWeeklyUsageData(catId: Int, limit: Int = 4): List<UsageData>
    
    // 月汇总相关查询
    @Query("SELECT * FROM summary_usage_month_user WHERE catId = :catId ORDER BY month DESC LIMIT :limit")
    suspend fun getMonthlyUsage(catId: Int, limit: Int = 6): List<SummaryUsageMonthUserEntity>
    
    @Query("SELECT month as period, (avgDailySec / 60) as usageMinutes, 0.0 as completionRate FROM summary_usage_month_user WHERE catId = :catId ORDER BY month DESC LIMIT :limit")
    suspend fun getMonthlyUsageData(catId: Int, limit: Int = 6): List<UsageData>
    
    // 获取完成度数据（需要结合目标数据计算）
    @Query("""
        SELECT s.date as period, 
               (s.totalSec / 60) as usageMinutes,
               CASE WHEN g.dailyGoalMin > 0 
                    THEN CAST((s.totalSec / 60.0) / g.dailyGoalMin * 100 AS REAL)
                    ELSE 0.0 
               END as completionRate
        FROM summary_usage_user s
        LEFT JOIN goals_reward_punishment_users g ON s.catId = g.catId
        WHERE s.catId = :catId 
        ORDER BY s.date DESC 
        LIMIT :limit
    """)
    suspend fun getDailyCompletionData(catId: Int, limit: Int = 7): List<UsageData>
    
    @Query("""
        SELECT s.weekStartDate as period,
               (s.avgDailySec / 60) as usageMinutes,
               CASE WHEN g.dailyGoalMin > 0 
                    THEN CAST((s.avgDailySec / 60.0) / g.dailyGoalMin * 100 AS REAL)
                    ELSE 0.0 
               END as completionRate
        FROM summary_usage_week_user s
        LEFT JOIN goals_reward_punishment_users g ON s.catId = g.catId
        WHERE s.catId = :catId 
        ORDER BY s.weekStartDate DESC 
        LIMIT :limit
    """)
    suspend fun getWeeklyCompletionData(catId: Int, limit: Int = 4): List<UsageData>
    
    /**
     * 删除所有summary_usage数据（用于调试）
     */
    @Query("DELETE FROM summary_usage_user")
    suspend fun deleteAllSummaryUsage()
    
    /**
     * 删除所有汇总数据，包括日、周、月汇总表（用于重新聚合）
     */
    suspend fun deleteAllSummaryData() {
        deleteAllSummaryUsage()
        deleteAllWeeklySummary()
        deleteAllMonthlySummary()
    }
    
    @Query("DELETE FROM summary_usage_week_user")
    suspend fun deleteAllWeeklySummary()
    
    @Query("DELETE FROM summary_usage_month_user")
    suspend fun deleteAllMonthlySummary()
    
    @Query("""
        SELECT s.month as period,
               (s.avgDailySec / 60) as usageMinutes,
               CASE WHEN g.dailyGoalMin > 0 
                    THEN CAST((s.avgDailySec / 60.0) / g.dailyGoalMin * 100 AS REAL)
                    ELSE 0.0 
               END as completionRate
        FROM summary_usage_month_user s
        LEFT JOIN goals_reward_punishment_users g ON s.catId = g.catId
        WHERE s.catId = :catId 
        ORDER BY s.month DESC 
        LIMIT :limit
    """)
    suspend fun getMonthlyCompletionData(catId: Int, limit: Int = 6): List<UsageData>
    
    // 获取所有数据用于迁移
    @Query("SELECT * FROM summary_usage_user ORDER BY updateTime DESC")
    suspend fun getAllSummaryUsageUser(): List<SummaryUsageUserEntity>
    
    @Query("SELECT * FROM summary_usage_week_user ORDER BY updateTime DESC")
    suspend fun getAllSummaryUsageWeek(): List<SummaryUsageWeekUserEntity>
    
    @Query("SELECT * FROM summary_usage_month_user ORDER BY updateTime DESC")
    suspend fun getAllSummaryUsageMonth(): List<SummaryUsageMonthUserEntity>
    
    @Query("SELECT * FROM summary_usage_user")
    fun getAllSummaryUsageUserFlow(): Flow<List<SummaryUsageUserEntity>>
    
    // 插入或更新汇总表（用于迁移）
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWeek(entity: SummaryUsageWeekUserEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMonth(entity: SummaryUsageMonthUserEntity)
    
    // New methods needed by HomeViewModel
    @Query("SELECT * FROM summary_usage_week_user WHERE catId = :categoryId AND weekStartDate LIKE :year||'%' AND weekStartDate LIKE '%W'||:week||'%'")
    suspend fun getByCategoryAndWeek(categoryId: Int, year: Int, week: Int): SummaryUsageWeekUserEntity?
    
    @Query("SELECT * FROM summary_usage_month_user WHERE catId = :categoryId AND month = :year||'-'||PRINTF('%02d', :month)")
    suspend fun getByCategoryAndMonth(categoryId: Int, year: Int, month: Int): SummaryUsageMonthUserEntity?
    
    // Methods needed for base record checking
    @Query("SELECT * FROM summary_usage_user WHERE id = :id")
    suspend fun getSummaryUsageById(id: String): SummaryUsageUserEntity?
    
    @Query("SELECT * FROM summary_usage_week_user WHERE id = :id")
    suspend fun getSummaryUsageWeekById(id: String): SummaryUsageWeekUserEntity?
    
    @Query("SELECT * FROM summary_usage_month_user WHERE id = :id")
    suspend fun getSummaryUsageMonthById(id: String): SummaryUsageMonthUserEntity?
    
    // 删除指定日期的数据（用于数据修复）
    @Query("DELETE FROM summary_usage_user WHERE date = :date")
    suspend fun deleteByDate(date: String)
    
    @Query("DELETE FROM summary_usage_week_user WHERE weekStartDate LIKE :weekPrefix||'%'")
    suspend fun deleteByWeek(weekPrefix: String)
    
    @Query("DELETE FROM summary_usage_month_user WHERE month = :month")
    suspend fun deleteByMonth(month: String)
} 