package com.offtime.app.data.dao

import androidx.room.*
import com.offtime.app.data.entity.TimerSessionDefaultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimerSessionDefaultDao {
    
    @Query("SELECT * FROM timer_sessions_defaults ORDER BY catId ASC")
    fun getAllDefaultSessions(): Flow<List<TimerSessionDefaultEntity>>
    
    @Query("SELECT * FROM timer_sessions_defaults WHERE catId = :catId")
    suspend fun getDefaultSessionByCatId(catId: Int): TimerSessionDefaultEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDefaultSession(session: TimerSessionDefaultEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDefaultSessions(sessions: List<TimerSessionDefaultEntity>)
    
    @Update
    suspend fun updateDefaultSession(session: TimerSessionDefaultEntity)
    
    @Query("DELETE FROM timer_sessions_defaults WHERE catId = :catId")
    suspend fun deleteDefaultSession(catId: Int)
    
    @Query("DELETE FROM timer_sessions_defaults")
    suspend fun deleteAllDefaultSessions()
    
    @Query("SELECT COUNT(*) FROM timer_sessions_defaults")
    suspend fun getDefaultSessionCount(): Int
} 