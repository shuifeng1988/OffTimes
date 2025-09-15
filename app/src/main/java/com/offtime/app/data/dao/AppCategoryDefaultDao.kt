package com.offtime.app.data.dao

import androidx.room.*
import com.offtime.app.data.entity.AppCategoryDefaultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppCategoryDefaultDao {
    
    @Query("SELECT * FROM AppCategory_Defaults ORDER BY displayOrder ASC")
    fun getAllDefaultCategories(): Flow<List<AppCategoryDefaultEntity>>
    
    @Query("SELECT * FROM AppCategory_Defaults ORDER BY displayOrder ASC")
    suspend fun getAllDefaultCategoriesList(): List<AppCategoryDefaultEntity>
    
    @Query("SELECT * FROM AppCategory_Defaults WHERE id = :id")
    suspend fun getDefaultCategoryById(id: Int): AppCategoryDefaultEntity?
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaultCategory(category: AppCategoryDefaultEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaultCategories(categories: List<AppCategoryDefaultEntity>)
    
    @Update
    suspend fun updateDefaultCategory(category: AppCategoryDefaultEntity)
    
    @Query("DELETE FROM AppCategory_Defaults WHERE id = :id")
    suspend fun deleteDefaultCategory(id: Int)
    
    @Query("DELETE FROM AppCategory_Defaults")
    suspend fun deleteAllDefaultCategories()
    
    @Query("SELECT COUNT(*) FROM AppCategory_Defaults")
    suspend fun getDefaultCategoryCount(): Int
    
    @Query("UPDATE AppCategory_Defaults SET displayOrder = :newOrder, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateDefaultCategoryOrder(id: Int, newOrder: Int, updatedAt: Long = System.currentTimeMillis())
} 