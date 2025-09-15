package com.offtime.app.data.repository

import com.offtime.app.data.dao.AppCategoryDao
import com.offtime.app.data.dao.TimerSessionDefaultDao
import com.offtime.app.data.dao.TimerSessionUserDao
import com.offtime.app.data.entity.AppCategoryEntity
import com.offtime.app.data.entity.TimerSessionDefaultEntity
import com.offtime.app.data.entity.TimerSessionUserEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerSessionRepository @Inject constructor(
    private val timerSessionDefaultDao: TimerSessionDefaultDao,
    private val timerSessionUserDao: TimerSessionUserDao,
    private val appCategoryDao: AppCategoryDao
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    // 获取今天的日期字符串
    private fun getTodayDateString(): String {
        return dateFormat.format(Date())
    }
    
    // 获取所有分类和对应的今日累计时长
    fun getCategoriesWithTodayDuration(): Flow<List<CategoryWithDuration>> {
        val today = getTodayDateString()
        return combine(
            appCategoryDao.getAllCategories(),
            timerSessionUserDao.getAllUserSessions()
        ) { categories, sessions ->
            categories.map { category ->
                val todaySessions = sessions.filter { 
                    it.catId == category.id && it.date == today 
                }
                val totalDuration = todaySessions.sumOf { it.durationSec }
                val sessionCount = todaySessions.size
                CategoryWithDuration(category, totalDuration, sessionCount)
            }
        }
    }
    
    // 启动计时 - 创建新的计时会话或继续上一个会话
    suspend fun startTimer(catId: Int): TimerSessionUserEntity? {
        val today = getTodayDateString()
        val now = System.currentTimeMillis()
        
        // 检查是否有最近的会话可以继续
        val latestSession = timerSessionUserDao.getLatestSessionByCatIdAndDate(catId, today)
        
        return if (latestSession != null && (now - latestSession.endTime) <= 10_000) {
            // 10秒内重新启动，继续上一个会话
            val updatedSession = latestSession.copy(
                endTime = now,
                updateTime = now
            )
            timerSessionUserDao.updateUserSession(updatedSession)
            updatedSession
        } else {
            // 创建新会话
            val newSession = TimerSessionUserEntity(
                catId = catId,
                programName = "线下活动",
                date = today,
                startTime = now,
                endTime = now,
                durationSec = 0,
                isOffline = 1,
                updateTime = now
            )
            val sessionId = timerSessionUserDao.insertUserSession(newSession)
            newSession.copy(id = sessionId.toInt())
        }
    }
    
    // 更新计时会话（每30秒调用）
    suspend fun updateTimer(sessionId: Int, durationSec: Int): Boolean {
        val session = timerSessionUserDao.getUserSessionById(sessionId) ?: return false
        val now = System.currentTimeMillis()
        
        // 检查是否跨日
        val sessionDate = session.date
        val currentDate = getTodayDateString()
        
        if (sessionDate != currentDate) {
            // 跨日处理
            handleCrossDayTimer(session, now)
            return false // 返回false表示需要重新开始计时
        } else {
            // 正常更新
            val updatedSession = session.copy(
                endTime = now,
                durationSec = durationSec,
                updateTime = now
            )
            timerSessionUserDao.updateUserSession(updatedSession)
            return true
        }
    }
    
    // 跨日处理
    private suspend fun handleCrossDayTimer(session: TimerSessionUserEntity, currentTime: Long) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentTime
        
        // 设置为前一天的23:59:59
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val yesterdayEnd = calendar.timeInMillis
        
        // 更新昨天的会话结束时间
        val yesterdayDuration = ((yesterdayEnd - session.startTime) / 1000).toInt()
        val updatedYesterdaySession = session.copy(
            endTime = yesterdayEnd,
            durationSec = yesterdayDuration,
            updateTime = currentTime
        )
        timerSessionUserDao.updateUserSession(updatedYesterdaySession)
        
        // 创建今天的新会话
        val todaySession = TimerSessionUserEntity(
            catId = session.catId,
            programName = "线下活动",
            date = getTodayDateString(),
            startTime = currentTime,
            endTime = currentTime,
            durationSec = 0,
            isOffline = 1,
            updateTime = currentTime
        )
        timerSessionUserDao.insertUserSession(todaySession)
    }
    
    // 停止计时
    suspend fun stopTimer(sessionId: Int): Boolean {
        val session = timerSessionUserDao.getUserSessionById(sessionId) ?: return false
        val now = System.currentTimeMillis()
        val finalDuration = ((now - session.startTime) / 1000).toInt()
        
        val updatedSession = session.copy(
            endTime = now,
            durationSec = finalDuration,
            updateTime = now
        )
        timerSessionUserDao.updateUserSession(updatedSession)
        return true
    }
    
    // 停止计时（指定自定义时长） - 用于定时计时器
    suspend fun stopTimer(sessionId: Int, customDurationSec: Int): Boolean {
        val session = timerSessionUserDao.getUserSessionById(sessionId) ?: return false
        val now = System.currentTimeMillis()
        
        val updatedSession = session.copy(
            endTime = now,
            durationSec = customDurationSec,
            updateTime = now
        )
        timerSessionUserDao.updateUserSession(updatedSession)
        return true
    }
    
    // 获取指定分类和日期的总时长
    suspend fun getTotalDurationByCatIdAndDate(catId: Int, date: String): Int {
        return timerSessionUserDao.getTotalDurationByCatIdAndDate(catId, date) ?: 0
    }
    
    // 获取今日指定分类的总时长
    suspend fun getTodayTotalDuration(catId: Int): Int {
        return getTotalDurationByCatIdAndDate(catId, getTodayDateString())
    }
    
    // 获取今日指定分类的计时次数
    suspend fun getTodaySessionCount(catId: Int): Int {
        val today = getTodayDateString()
        return timerSessionUserDao.getSessionCountByCatIdAndDate(catId, today)
    }
    
    // 删除指定分类的所有计时数据
    suspend fun deleteTimerSessionsByCatId(catId: Int) {
        timerSessionUserDao.deleteUserSessionsByCatId(catId)
        timerSessionDefaultDao.deleteDefaultSession(catId)
    }
    
    // 获取正在进行的计时会话
    suspend fun getActiveSession(catId: Int): TimerSessionUserEntity? {
        val today = getTodayDateString()
        return timerSessionUserDao.getLatestSessionByCatIdAndDate(catId, today)
    }
    
    // 清理过期数据 - 原始计时会话数据保留至少60天
    suspend fun cleanOldSessions(daysToKeep: Int = 60) = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -daysToKeep)
        val cutoffDate = dateFormat.format(calendar.time)
        timerSessionUserDao.deleteOldSessions(cutoffDate)
        android.util.Log.i("TimerSessionRepository", "清理了${cutoffDate}之前的timer_sessions_users数据")
    }

    // 获取总会话数量（用于统计）
    suspend fun getTotalSessionCount(): Int = withContext(Dispatchers.IO) {
        timerSessionUserDao.getTotalSessionCount()
    }

    // 数据类：分类和对应的时长与次数
    data class CategoryWithDuration(
        val category: AppCategoryEntity,
        val todayDurationSec: Int,
        val todaySessionCount: Int = 0
    )
} 