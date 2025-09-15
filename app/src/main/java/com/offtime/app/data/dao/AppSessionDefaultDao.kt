package com.offtime.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.offtime.app.data.entity.AppSessionDefaultEntity

@Dao
interface AppSessionDefaultDao {
    
    @Query("SELECT * FROM app_sessions_defaults")
    suspend fun getAllDefaults(): List<AppSessionDefaultEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDefault(defaultEntity: AppSessionDefaultEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDefaults(defaults: List<AppSessionDefaultEntity>)
    
    @Query("DELETE FROM app_sessions_defaults")
    suspend fun deleteAllDefaults()
} 