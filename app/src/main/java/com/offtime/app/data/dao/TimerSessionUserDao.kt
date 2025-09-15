package com.offtime.app.data.dao

import androidx.room.*
import com.offtime.app.data.entity.TimerSessionUserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimerSessionUserDao {
    
    @Query("SELECT * FROM timer_sessions_users ORDER BY startTime DESC")
    fun getAllUserSessions(): Flow<List<TimerSessionUserEntity>>
    
    @Query("SELECT * FROM timer_sessions_users ORDER BY startTime DESC")
    suspend fun getAllUserSessionsList(): List<TimerSessionUserEntity>
    
    @Query("SELECT * FROM timer_sessions_users WHERE catId = :catId ORDER BY startTime DESC")
    fun getUserSessionsByCatId(catId: Int): Flow<List<TimerSessionUserEntity>>
    
    @Query("SELECT * FROM timer_sessions_users WHERE date = :date ORDER BY startTime DESC")
    fun getUserSessionsByDate(date: String): Flow<List<TimerSessionUserEntity>>
    
    @Query("SELECT * FROM timer_sessions_users WHERE date = :date ORDER BY startTime ASC")
    suspend fun getSessionsByDate(date: String): List<TimerSessionUserEntity>
    
    @Query("SELECT * FROM timer_sessions_users WHERE catId = :catId AND date = :date ORDER BY startTime DESC")
    fun getUserSessionsByCatIdAndDate(catId: Int, date: String): Flow<List<TimerSessionUserEntity>>
    
    @Query("SELECT * FROM timer_sessions_users WHERE id = :id")
    suspend fun getUserSessionById(id: Int): TimerSessionUserEntity?
    
    @Query("SELECT * FROM timer_sessions_users WHERE catId = :catId AND date = :date ORDER BY endTime DESC LIMIT 1")
    suspend fun getLatestSessionByCatIdAndDate(catId: Int, date: String): TimerSessionUserEntity?
    
    @Query("SELECT SUM(durationSec) FROM timer_sessions_users WHERE catId = :catId AND date = :date")
    suspend fun getTotalDurationByCatIdAndDate(catId: Int, date: String): Int?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserSession(session: TimerSessionUserEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserSessions(sessions: List<TimerSessionUserEntity>)
    
    @Update
    suspend fun updateUserSession(session: TimerSessionUserEntity)
    
    @Query("DELETE FROM timer_sessions_users WHERE catId = :catId")
    suspend fun deleteUserSessionsByCatId(catId: Int)
    
    @Query("DELETE FROM timer_sessions_users WHERE id = :id")
    suspend fun deleteUserSessionById(id: Int)
    
    @Query("DELETE FROM timer_sessions_users")
    suspend fun deleteAllUserSessions()
    
    @Query("SELECT COUNT(*) FROM timer_sessions_users")
    suspend fun getUserSessionCount(): Int
    
    @Query("SELECT COUNT(*) FROM timer_sessions_users WHERE catId = :catId AND date = :date")
    suspend fun getSessionCountByCatIdAndDate(catId: Int, date: String): Int
    
    @Query("DELETE FROM timer_sessions_users WHERE date < :date")
    suspend fun deleteOldSessions(date: String)
    
    @Query("SELECT COUNT(*) FROM timer_sessions_users")
    suspend fun getTotalSessionCount(): Int
    
    // 获取所有不同的日期（用于历史数据重建）
    @Query("SELECT DISTINCT date FROM timer_sessions_users ORDER BY date DESC")
    suspend fun getAllDates(): List<String>
} 