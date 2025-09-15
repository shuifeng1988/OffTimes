package com.offtime.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.offtime.app.data.entity.AppSessionUserEntity

// 用于调试页面的统计数据
data class SessionStatsData(
    val date: String,
    val sessionCount: Int,
    val totalDuration: Int
)

@Dao
interface AppSessionUserDao {
    
    @Query("SELECT * FROM app_sessions_users WHERE date = :date ORDER BY startTime ASC")
    suspend fun getSessionsByDate(date: String): List<AppSessionUserEntity>
    
    @Query("SELECT * FROM app_sessions_users WHERE catId = :catId AND date = :date ORDER BY startTime ASC")
    suspend fun getSessionsByCatIdAndDate(catId: Int, date: String): List<AppSessionUserEntity>
    
    @Query("SELECT * FROM app_sessions_users WHERE pkgName = :pkgName AND date = :date ORDER BY startTime DESC LIMIT 1")
    suspend fun getLatestSessionByPkgAndDate(pkgName: String, date: String): AppSessionUserEntity?
    
    /**
     * 获取指定应用在指定日期的最近一次会话记录
     * 用于智能合并连续使用的会话
     */
    @Query("SELECT * FROM app_sessions_users WHERE pkgName = :pkgName AND date = :date ORDER BY endTime DESC LIMIT 1")
    suspend fun getLatestSessionByPackageAndDate(pkgName: String, date: String): AppSessionUserEntity?
    
    /**
     * 查找可以合并的会话记录（检测时间重叠或紧密连接）
     * 增强版：更严格的重叠检测，防止重复计算
     * @param pkgName 应用包名
     * @param date 日期
     * @param newStartTime 新会话的开始时间
     * @param newEndTime 新会话的结束时间
     */
    @Query("""
        SELECT * FROM app_sessions_users 
        WHERE pkgName = :pkgName 
        AND date = :date 
        AND (
            -- 完全重叠：新会话被现有会话包含
            (startTime <= :newStartTime AND endTime >= :newEndTime) OR
            -- 完全重叠：现有会话被新会话包含
            (startTime >= :newStartTime AND endTime <= :newEndTime) OR
            -- 部分重叠：开始时间重叠
            (startTime <= :newStartTime AND endTime > :newStartTime AND endTime <= :newEndTime) OR
            -- 部分重叠：结束时间重叠
            (startTime >= :newStartTime AND startTime < :newEndTime AND endTime >= :newEndTime) OR
            -- 紧密连接：5秒内的间隔
            (ABS(endTime - :newStartTime) <= 5000) OR
            (ABS(:newEndTime - startTime) <= 5000)
        )
        ORDER BY startTime DESC 
        LIMIT 1
    """)
    suspend fun findOverlappingOrMergeableSession(
        pkgName: String, 
        date: String, 
        newStartTime: Long,
        newEndTime: Long
    ): AppSessionUserEntity?
    
    /**
     * 检查是否存在可以合并的连续会话（旧方法，保持向后兼容）
     */
    @Query("""
        SELECT * FROM app_sessions_users 
        WHERE pkgName = :pkgName 
        AND date = :date 
        AND endTime >= :endTimeThreshold 
        AND endTime <= :endTimeThreshold + :maxGapMs
        ORDER BY endTime DESC 
        LIMIT 1
    """)
    suspend fun findMergeableSession(
        pkgName: String, 
        date: String, 
        endTimeThreshold: Long, 
        maxGapMs: Long = 300000L // 默认5分钟
    ): AppSessionUserEntity?
    
    /**
     * 智能更新会话的结束时间和持续时长
     * 用于合并连续使用的会话记录
     */
    @Query("""
        UPDATE app_sessions_users 
        SET endTime = :newEndTime, 
            durationSec = :newDurationSec, 
            updateTime = :updateTime 
        WHERE id = :sessionId
    """)
    suspend fun updateSessionEndTimeAndDuration(
        sessionId: Int,
        newEndTime: Long,
        newDurationSec: Int,
        updateTime: Long = System.currentTimeMillis()
    )
    
    /**
     * 智能更新会话的开始时间、结束时间和持续时长
     * 用于合并重叠的会话记录
     */
    @Query("""
        UPDATE app_sessions_users 
        SET startTime = :newStartTime,
            endTime = :newEndTime, 
            durationSec = :newDurationSec, 
            updateTime = :updateTime 
        WHERE id = :sessionId
    """)
    suspend fun updateSessionTimeRange(
        sessionId: Int,
        newStartTime: Long,
        newEndTime: Long,
        newDurationSec: Int,
        updateTime: Long = System.currentTimeMillis()
    )
    
    @Query("SELECT SUM(durationSec) FROM app_sessions_users WHERE catId = :catId AND date = :date")
    suspend fun getTotalDurationByCatIdAndDate(catId: Int, date: String): Int?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: AppSessionUserEntity)
    
    @Update
    suspend fun updateSession(session: AppSessionUserEntity)
    
    @Query("DELETE FROM app_sessions_users WHERE date < :date")
    suspend fun deleteOldSessions(date: String)
    
    @Query("SELECT COUNT(*) FROM app_sessions_users WHERE catId = :catId AND date = :date")
    suspend fun getSessionCountByCatIdAndDate(catId: Int, date: String): Int
    
    @Query("SELECT * FROM app_sessions_users ORDER BY updateTime DESC")
    suspend fun getAllSessions(): List<AppSessionUserEntity>
    
    @Query("SELECT COUNT(*) FROM app_sessions_users")
    suspend fun getSessionCount(): Int
    
    @Query("SELECT * FROM app_sessions_users ORDER BY startTime DESC LIMIT :limit")
    suspend fun getRecentSessions(limit: Int): List<AppSessionUserEntity>

    @Query("SELECT * FROM app_sessions_users WHERE pkgName = :packageName ORDER BY startTime DESC")
    suspend fun getSessionsByPackageNameDebug(packageName: String): List<AppSessionUserEntity>
    
    @Query("SELECT COUNT(*) FROM app_sessions_users")
    suspend fun getTotalSessionCount(): Int
    
    @Query("DELETE FROM app_sessions_users WHERE pkgName = :packageName")
    suspend fun deleteSessionsByPackageName(packageName: String)
    
    @Query("DELETE FROM app_sessions_users WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Int)
    
    // 获取最近N天的所有数据（用于调试）
    @Query("SELECT * FROM app_sessions_users WHERE date >= :startDate ORDER BY date DESC, startTime DESC")
    suspend fun getSessionsSinceDate(startDate: String): List<AppSessionUserEntity>
    
    // 获取所有不同的日期（用于调试页面的日期列表）
    @Query("SELECT DISTINCT date FROM app_sessions_users ORDER BY date DESC")
    suspend fun getAllDates(): List<String>
    
    // 按日期分组统计（用于调试页面概览）
    @Query("""
        SELECT date, COUNT(*) as sessionCount, SUM(durationSec) as totalDuration 
        FROM app_sessions_users 
        WHERE date >= :startDate 
        GROUP BY date 
        ORDER BY date DESC
    """)
    suspend fun getSessionStatsByDate(startDate: String): List<SessionStatsData>
    
    // 检查指定分类和日期是否有任何会话记录
    @Query("SELECT COUNT(*) > 0 FROM app_sessions_users WHERE catId = :catId AND date = :date LIMIT 1")
    suspend fun hasAnySessionsForCategoryAndDate(catId: Int, date: String): Boolean
    
    @Query("UPDATE app_sessions_users SET endTime = :endTime, durationSec = :durationSec WHERE id = :sessionId")
    suspend fun updateSessionEndTime(sessionId: Int, endTime: Long, durationSec: Int)
    
    @Query("SELECT * FROM app_sessions_users WHERE pkgName = :pkgName AND endTime <= :currentStartTime AND endTime >= :minEndTime ORDER BY endTime DESC LIMIT 1")
    suspend fun getRecentSessionByPackage(pkgName: String, minEndTime: Long, currentStartTime: Long): AppSessionUserEntity?
    
    @Query("SELECT * FROM app_sessions_users WHERE pkgName = :pkgName AND date = :date AND startTime = :startTime LIMIT 1")
    suspend fun getActiveSessionByPackage(pkgName: String, date: String, startTime: Long): AppSessionUserEntity?
} 