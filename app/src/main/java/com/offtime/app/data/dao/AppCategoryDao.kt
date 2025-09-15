package com.offtime.app.data.dao

import androidx.room.*
import com.offtime.app.data.entity.AppCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppCategoryDao {
    
    @Query("SELECT * FROM AppCategory_Users ORDER BY displayOrder ASC")
    fun getAllCategories(): Flow<List<AppCategoryEntity>>
    
    @Query("SELECT * FROM AppCategory_Users ORDER BY displayOrder ASC")
    suspend fun getAllCategoriesList(): List<AppCategoryEntity>
    
    @Query("SELECT * FROM AppCategory_Users WHERE id = :id")
    suspend fun getCategoryById(id: Int): AppCategoryEntity?
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: AppCategoryEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<AppCategoryEntity>)
    
    @Update
    suspend fun updateCategory(category: AppCategoryEntity)
    
    @Query("DELETE FROM AppCategory_Users WHERE id = :id AND isLocked = 0")
    suspend fun deleteCategory(id: Int)
    
    @Query("SELECT COUNT(*) FROM AppCategory_Users")
    suspend fun getCategoryCount(): Int
    
    @Query("UPDATE AppCategory_Users SET displayOrder = :newOrder, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateCategoryOrder(id: Int, newOrder: Int, updatedAt: Long = System.currentTimeMillis())
    
    /**
     * 获取在指定日期创建或更新的分类（用于增量备份）
     */
    @Query("""
        SELECT * FROM AppCategory_Users 
        WHERE date(createdAt/1000, 'unixepoch') = :date 
           OR date(updatedAt/1000, 'unixepoch') = :date
        ORDER BY displayOrder ASC
    """)
    suspend fun getCategoriesUpdatedOnDate(date: String): List<AppCategoryEntity>
} 