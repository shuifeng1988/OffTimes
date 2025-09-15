package com.offtime.app.data.repository

import android.content.Context
import android.content.res.Configuration
import com.offtime.app.data.dao.AppCategoryDao
import com.offtime.app.data.dao.GoalRewardPunishmentDefaultDao
import com.offtime.app.data.dao.GoalRewardPunishmentUserDao
import com.offtime.app.data.entity.GoalRewardPunishmentDefaultEntity
import com.offtime.app.data.entity.GoalRewardPunishmentUserEntity
import com.offtime.app.data.entity.AppCategoryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalRewardPunishmentRepository @Inject constructor(
    private val goalUserDao: GoalRewardPunishmentUserDao,
    private val goalDefaultDao: GoalRewardPunishmentDefaultDao,
    private val appCategoryDao: AppCategoryDao
) {
    
    /**
     * 获取数据库存储的标准奖励文本（中文版本）
     */
    private fun getStandardRewardText(): String {
        return "薯片"
    }
    
    /**
     * 获取数据库存储的标准惩罚文本（中文版本）
     */
    private fun getStandardPunishText(): String {
        return "俯卧撑"
    }
    
    /**
     * 获取数据库存储的标准奖励单位（中文版本）
     */
    private fun getStandardRewardUnit(): String {
        return "包"
    }
    
    /**
     * 获取数据库存储的标准惩罚单位（中文版本）
     */
    private fun getStandardPunishUnit(): String {
        return "个"
    }
    
    /**
     * 获取数据库存储的标准时间单位（中文版本）
     */
    private fun getStandardTimeUnit(unit: String): String {
        return when(unit) {
            "minutes", "分钟" -> "分钟"
            "hours", "小时" -> "小时"
            else -> unit
        }
    }
    
    // 用户目标数据操作
    fun getAllUserGoals(): Flow<List<GoalRewardPunishmentUserEntity>> = goalUserDao.getAllUserGoals()
    
    suspend fun getUserGoalByCatId(catId: Int): GoalRewardPunishmentUserEntity? = 
        goalUserDao.getUserGoalByCatId(catId)
    
    suspend fun insertUserGoal(goal: GoalRewardPunishmentUserEntity): Long = 
        goalUserDao.insertUserGoal(goal)
    
    suspend fun insertUserGoals(goals: List<GoalRewardPunishmentUserEntity>) = 
        goalUserDao.insertUserGoals(goals)
    
    suspend fun updateUserGoal(goal: GoalRewardPunishmentUserEntity) = 
        goalUserDao.updateUserGoal(goal)
    
    suspend fun deleteUserGoal(catId: Int) = goalUserDao.deleteUserGoal(catId)
    
    suspend fun getUserGoalCount(): Int = goalUserDao.getUserGoalCount()
    
    // 默认目标数据操作
    fun getAllDefaultGoals(): Flow<List<GoalRewardPunishmentDefaultEntity>> = goalDefaultDao.getAllDefaultGoals()
    
    suspend fun getDefaultGoalByCatId(catId: Int): GoalRewardPunishmentDefaultEntity? = 
        goalDefaultDao.getDefaultGoalByCatId(catId)
    
    // 业务逻辑方法
    
    /**
     * 确保用户目标数据已初始化
     */
    suspend fun ensureUserGoalDataInitialized(): Boolean {
        return try {
            android.util.Log.d("GoalRepository", "开始检查用户目标数据初始化")
            val userGoalCount = getUserGoalCount()
            android.util.Log.d("GoalRepository", "用户目标数据数量: $userGoalCount")
            
            if (userGoalCount == 0) {
                android.util.Log.d("GoalRepository", "用户目标数据为空，开始从默认表迁移")
                // 从默认表迁移数据到用户表
                migrateDefaultGoalsToUserTable()
            }
            android.util.Log.d("GoalRepository", "用户目标数据初始化完成")
            true
        } catch (e: Exception) {
            android.util.Log.e("GoalRepository", "用户目标数据初始化失败: ${e.message}", e)
            false
        }
    }
    
    /**
     * 从默认表迁移目标数据到用户表
     * 优先使用默认表数据，如果默认表为空则使用硬编码默认值
     */
    private suspend fun migrateDefaultGoalsToUserTable() {
        try {
            android.util.Log.d("GoalRepository", "开始从默认表迁移目标数据到用户表")
            
            // 获取所有分类（从AppCategory_Users表）
            val categories = appCategoryDao.getAllCategories().first()
            android.util.Log.d("GoalRepository", "从AppCategory_Users表找到 ${categories.size} 个分类")
            
            // 获取默认目标数据
            val defaultGoals = getAllDefaultGoals().first()
            android.util.Log.d("GoalRepository", "从默认表找到 ${defaultGoals.size} 个默认目标")
            
            if (categories.isNotEmpty()) {
                val userGoals = mutableListOf<GoalRewardPunishmentUserEntity>()
                
                // 为每个分类创建用户目标
                categories.forEach { category ->
                    // 首先尝试从默认表获取对应的目标设置
                    val defaultGoal = defaultGoals.find { 
                        // 由于默认表的catId可能不匹配，我们通过分类名称来推断
                        when(it.catId) {
                            1 -> category.name == "娱乐"
                            2 -> category.name == "学习" 
                            3 -> category.name == "健身"
                            4 -> category.name == "总使用"
                            else -> false
                        }
                    }
                    
                    val (dailyGoalMin, conditionType) = if (defaultGoal != null) {
                        // 使用默认表的数据
                        Pair(defaultGoal.dailyGoalMin, defaultGoal.conditionType)
                    } else {
                        // 使用硬编码的备用默认值
                        when (category.name) {
                            "娱乐" -> Pair(120, 0)  // 120分钟，≤目标算完成
                            "学习" -> Pair(30, 1)   // 30分钟，≥目标算完成
                            "健身" -> Pair(30, 1)   // 30分钟，≥目标算完成
                            "总使用" -> Pair(240, 0) // 240分钟，≤目标算完成
                            else -> Pair(30, 1)    // 其他分类默认30分钟，≥目标算完成
                        }
                    }
                    
                    val userGoal = GoalRewardPunishmentUserEntity(
                        catId = category.id, // 使用真实的分类ID
                        dailyGoalMin = dailyGoalMin,
                        goalTimeUnit = getStandardTimeUnit("分钟"),
                        conditionType = conditionType,
                        rewardText = getStandardRewardText(),
                        rewardNumber = 1,
                        rewardUnit = getStandardRewardUnit(),
                        rewardTimeUnit = getStandardTimeUnit("小时"),
                        punishText = getStandardPunishText(),
                        punishNumber = 30,
                        punishUnit = getStandardPunishUnit(),
                        punishTimeUnit = getStandardTimeUnit("小时"),
                        updateTime = System.currentTimeMillis()
                    )
                    userGoals.add(userGoal)
                    android.util.Log.d("GoalRepository", "为分类 ${category.name}(${category.id}) 创建目标：${dailyGoalMin}分钟")
                }
                
                insertUserGoals(userGoals)
                android.util.Log.d("GoalRepository", "成功迁移 ${userGoals.size} 个目标数据到用户表")
            } else {
                android.util.Log.w("GoalRepository", "没有找到分类，无法创建目标")
            }
        } catch (e: Exception) {
            android.util.Log.e("GoalRepository", "迁移目标数据失败: ${e.message}", e)
        }
    }
    

    
    /**
     * 为新分类创建默认目标
     */
    suspend fun createDefaultGoalForCategory(catId: Int): Boolean {
        return try {
            // 检查是否已存在
            val existingGoal = getUserGoalByCatId(catId)
            if (existingGoal == null) {
                // 创建默认目标（30分钟，≥目标算完成）
                val defaultGoal = GoalRewardPunishmentUserEntity(
                    catId = catId,
                    dailyGoalMin = 30,
                    goalTimeUnit = getStandardTimeUnit("分钟"),
                    conditionType = 1, // ≥ 目标算完成
                    rewardText = getStandardRewardText(),
                    rewardNumber = 1,
                    rewardUnit = getStandardRewardUnit(),
                    rewardTimeUnit = getStandardTimeUnit("小时"),
                    punishText = getStandardPunishText(),
                    punishNumber = 30,
                    punishUnit = getStandardPunishUnit(),
                    punishTimeUnit = getStandardTimeUnit("小时"),
                    updateTime = System.currentTimeMillis()
                )
                insertUserGoal(defaultGoal)
                android.util.Log.d("GoalRepository", "为分类 $catId 创建默认目标")
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("GoalRepository", "创建默认目标失败: ${e.message}", e)
            false
        }
    }
    
    /**
     * 检查目标是否完成
     */
    fun isGoalCompleted(usedSeconds: Long, dailyGoalMin: Int, conditionType: Int): Boolean {
        val usedMinutes = usedSeconds / 60
        return if (conditionType == 0) {
            // ≤ 目标算完成
            usedMinutes <= dailyGoalMin
        } else {
            // ≥ 目标算完成
            usedMinutes >= dailyGoalMin
        }
    }
    
    /**
     * 获取完成条件描述
     */
    fun getConditionDescription(conditionType: Int): String {
        return if (conditionType == 0) "≤" else "≥"
    }
} 