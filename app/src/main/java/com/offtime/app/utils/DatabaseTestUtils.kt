package com.offtime.app.utils

import android.content.Context
import androidx.room.Room
import com.offtime.app.data.database.OffTimeDatabase
import kotlinx.coroutines.runBlocking

object DatabaseTestUtils {
    
    /**
     * 测试数据库表结构
     */
    fun testDatabaseStructure(context: Context): String {
        return try {
            val db = OffTimeDatabase.getDatabase(context)
            val result = StringBuilder()
            
            runBlocking {
                // 测试默认表
                val defaultCategoryCount = db.appCategoryDefaultDao().getDefaultCategoryCount()
                val defaultAppCount = db.appInfoDefaultDao().getDefaultAppCount()
                
                // 测试用户表
                val userCategoryCount = db.appCategoryDao().getCategoryCount()
                val userAppCount = db.appInfoDao().getAppCount()
                
                result.append("数据库表结构测试结果:\n")
                result.append("默认分类表 (AppCategory_Defaults): $defaultCategoryCount 条记录\n")
                result.append("默认应用表 (app_info_defaults): $defaultAppCount 条记录\n")
                result.append("用户分类表 (AppCategory_Users): $userCategoryCount 条记录\n")
                result.append("用户应用表 (app_info_Users): $userAppCount 条记录\n")
                
                // 检查默认分类数据
                if (defaultCategoryCount > 0) {
                    db.appCategoryDefaultDao().getAllDefaultCategories().collect { categories ->
                        result.append("\n默认分类详情:\n")
                        categories.forEach { category ->
                            result.append("- ${category.name} (ID: ${category.id}, 顺序: ${category.displayOrder})\n")
                        }
                    }
                }
                
                // 检查用户分类数据
                if (userCategoryCount > 0) {
                    db.appCategoryDao().getAllCategories().collect { categories ->
                        result.append("\n用户分类详情:\n")
                        categories.forEach { category ->
                            result.append("- ${category.name} (ID: ${category.id}, 顺序: ${category.displayOrder})\n")
                        }
                    }
                }
            }
            
            result.toString()
        } catch (e: Exception) {
            "数据库测试失败: ${e.message}"
        }
    }
} 