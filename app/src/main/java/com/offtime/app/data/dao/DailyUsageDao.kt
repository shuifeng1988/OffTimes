package com.offtime.app.data.dao

import androidx.room.*
import com.offtime.app.data.entity.DailyUsageUserEntity

data class HourlyUsageData(
    val hour: Int,
    val totalSeconds: Int
)

data class RewardPunishmentData(
    val period: String,
    val rewardValue: Float,
    val punishmentValue: Float
)

@Dao
interface DailyUsageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DailyUsageUserEntity)

    @Query("UPDATE daily_usage_user SET durationSec = durationSec + :delta, updateTime = :updateTime WHERE id = :id")
    suspend fun accumulate(id: String, delta: Int, updateTime: Long)

    @Query("SELECT * FROM daily_usage_user WHERE date = :date AND catId = :catId AND isOffline = :isOffline")
    suspend fun getSlots(date: String, catId: Int, isOffline: Int): List<DailyUsageUserEntity>

    /**
     * 获取指定日期、分类、类型的总使用时长（秒）
     */
    @Query("SELECT SUM(durationSec) FROM daily_usage_user WHERE date = :date AND catId = :catId AND isOffline = :isOffline")
    suspend fun getTotalUsageByCategoryAndType(catId: Int, date: String, isOffline: Int): Int?

    /**
     * 获取24小时分布数据
     */
    @Query("""
        SELECT slotIndex as hour, SUM(durationSec) as totalSeconds 
        FROM daily_usage_user 
        WHERE date = :date AND catId = :catId AND isOffline = :isOffline 
        GROUP BY slotIndex
        ORDER BY slotIndex
    """)
    suspend fun getHourlyUsage(catId: Int, date: String, isOffline: Int): List<HourlyUsageData>

    /**
     * 删除指定日期的所有小时级聚合数据
     * 用于重新聚合时清理旧数据，避免重复计算
     */
    @Query("DELETE FROM daily_usage_user WHERE date = :date")
    suspend fun deleteByDate(date: String)
    
    /**
     * 获取指定日期的所有小时数据记录
     * 用于数据验证和异常检测
     */
    @Query("SELECT * FROM daily_usage_user WHERE date = :date")
    suspend fun getAllHourlyDataByDate(date: String): List<DailyUsageUserEntity>

    /**
     * 清理历史错误数据：删除单小时超过60分钟的异常记录
     */
    @Query("DELETE FROM daily_usage_user WHERE durationSec > 3600")
    suspend fun deleteAbnormalData()

    /**
     * 获取包含异常数据的日期列表（单小时超过60分钟）
     */
    @Query("SELECT DISTINCT date FROM daily_usage_user WHERE durationSec > 3600 ORDER BY date")
    suspend fun getAbnormalDates(): List<String>

    /**
     * 获取指定日期和分类的所有数据（真实+虚拟）
     */
    @Query("SELECT * FROM daily_usage_user WHERE date = :date AND catId = :catId ORDER BY slotIndex, isOffline")
    suspend fun getDailyUsageByCategory(date: String, catId: Int): List<DailyUsageUserEntity>
    
    /**
     * 获取指定日期、分类、类型的数据
     */
    @Query("SELECT * FROM daily_usage_user WHERE date = :date AND catId = :catId AND isOffline = :isOffline ORDER BY slotIndex")
    suspend fun getDailyUsageByCategoryAndType(date: String, catId: Int, isOffline: Int): List<DailyUsageUserEntity>
    
    /**
     * 获取指定日期的所有数据
     */
    @Query("SELECT * FROM daily_usage_user WHERE date = :date ORDER BY catId, slotIndex, isOffline")
    suspend fun getDailyUsageByDate(date: String): List<DailyUsageUserEntity>
    
    /**
     * 获取所有日期的数据（用于调试）
     */
    @Query("SELECT * FROM daily_usage_user ORDER BY date DESC, catId, slotIndex, isOffline LIMIT 100")
    suspend fun getAllDailyUsageData(): List<DailyUsageUserEntity>
    
    /**
     * 获取所有有使用数据的日期列表
     */
    @Query("SELECT DISTINCT date FROM daily_usage_user ORDER BY date DESC")
    suspend fun getAllUsageDates(): List<String>
    
    /**
     * 删除所有daily_usage数据（用于调试）
     */
    @Query("DELETE FROM daily_usage_user")
    suspend fun deleteAllDailyUsage()
    
    /**
     * 检查指定分类和日期是否有任何使用记录
     */
    @Query("SELECT COUNT(*) > 0 FROM daily_usage_user WHERE catId = :catId AND date = :date LIMIT 1")
    suspend fun hasAnyUsageForCategoryAndDate(catId: Int, date: String): Boolean
} 