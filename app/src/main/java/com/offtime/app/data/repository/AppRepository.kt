package com.offtime.app.data.repository

import com.offtime.app.data.dao.AppCategoryDao
import com.offtime.app.data.dao.AppCategoryDefaultDao
import com.offtime.app.data.dao.AppInfoDao
import com.offtime.app.data.dao.AppInfoDefaultDao
import com.offtime.app.data.dao.GoalRewardPunishmentUserDao
import com.offtime.app.data.entity.AppCategoryEntity
import com.offtime.app.data.entity.AppCategoryDefaultEntity
import com.offtime.app.data.entity.AppInfoEntity
import com.offtime.app.data.entity.AppInfoDefaultEntity
import com.offtime.app.service.AppInfoService
import com.offtime.app.utils.AppCategoryUtils
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    private val appInfoDao: AppInfoDao,
    private val appCategoryDao: AppCategoryDao,
    private val appInfoDefaultDao: AppInfoDefaultDao,
    private val appCategoryDefaultDao: AppCategoryDefaultDao,
    private val goalUserDao: GoalRewardPunishmentUserDao,
    private val appInfoService: AppInfoService,
    private val appSettingsRepository: AppSettingsRepository,
    private val appCategoryUtils: AppCategoryUtils
) {
    

    
    // App Info 相关操作
    fun getAllApps(): Flow<List<AppInfoEntity>> = appInfoDao.getAllApps()
    
    suspend fun getAllAppsList(): List<AppInfoEntity> = appInfoDao.getAllAppsList()
    
    fun getAppsByCategory(categoryId: Int): Flow<List<AppInfoEntity>> = 
        appInfoDao.getAppsByCategory(categoryId)
    
    suspend fun getAppByPackageName(packageName: String): AppInfoEntity? = 
        appInfoDao.getAppByPackageName(packageName)
    
    suspend fun insertApp(app: AppInfoEntity): Long = appInfoDao.insertApp(app)
    
    suspend fun insertApps(apps: List<AppInfoEntity>) = appInfoDao.insertApps(apps)
    
    suspend fun updateApp(app: AppInfoEntity) = appInfoDao.updateApp(app)
    
    suspend fun updateAppCategory(packageName: String, categoryId: Int) = 
        appInfoDao.updateAppCategory(packageName, categoryId)
    
    suspend fun updateAppExcludeStatus(packageName: String, isExcluded: Boolean) = 
        appInfoDao.updateAppExcludeStatus(packageName, isExcluded)
    
    suspend fun getExcludedApps(): List<AppInfoEntity> = appInfoDao.getExcludedApps()
    
    suspend fun deleteApp(packageName: String) = appInfoDao.deleteApp(packageName)
    
    suspend fun getAppCount(): Int = appInfoDao.getAppCount()
    
    suspend fun getNonSystemApps(): List<AppInfoEntity> = appInfoDao.getNonSystemApps()
    
    // Category 相关操作
    fun getAllCategories(): Flow<List<AppCategoryEntity>> = appCategoryDao.getAllCategories()
    
    suspend fun getCategoryById(id: Int): AppCategoryEntity? = appCategoryDao.getCategoryById(id)
    
    suspend fun insertCategory(category: AppCategoryEntity): Long = 
        appCategoryDao.insertCategory(category)
    
    suspend fun insertCategories(categories: List<AppCategoryEntity>) = 
        appCategoryDao.insertCategories(categories)
    
    suspend fun updateCategory(category: AppCategoryEntity) = appCategoryDao.updateCategory(category)
    
    suspend fun deleteCategory(id: Int) {
        // 级联删除：先删除对应的目标数据，再删除分类
        try {
            goalUserDao.deleteUserGoal(id)
            appCategoryDao.deleteCategory(id)
            android.util.Log.d("AppRepository", "删除分类 $id 及其对应的目标数据")
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "删除分类时级联删除失败: ${e.message}", e)
            // 即使目标删除失败，也要删除分类
            appCategoryDao.deleteCategory(id)
        }
    }
    
    suspend fun getCategoryCount(): Int = appCategoryDao.getCategoryCount()
    
    suspend fun updateCategoryOrder(id: Int, newOrder: Int) = 
        appCategoryDao.updateCategoryOrder(id, newOrder)
    
    // Default Category相关操作
    suspend fun getAllDefaultCategoriesList(): List<AppCategoryDefaultEntity> = 
        appCategoryDefaultDao.getAllDefaultCategoriesList()
    
    // 业务逻辑方法
    
    /**
     * 确保用户表存在并已从默认表迁移数据
     */
    suspend fun ensureUserDataInitialized(): Boolean {
        return try {
            // 检查用户分类表是否有数据
            val userCategoryCount = getCategoryCount()
            android.util.Log.d("AppRepository", "当前用户分类数量: $userCategoryCount")
            
            if (userCategoryCount == 0) {
                // 从默认表迁移分类数据
                migrateDefaultCategoriesToUserTable()
            } else {
                // 如果已有数据，检查并清理重复分类
                cleanupDuplicateCategories()
            }
            
            // 清理不需要的虚拟线下活动APP
            cleanupUnnecessaryOfflineApps()
            
            // 确保每个分类都有对应的虚拟线下活动APP
            ensureOfflineActivityApps()
            
            true
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "用户数据初始化失败: ${e.message}", e)
            false
        }
    }
    
    /**
     * 从默认表迁移分类数据到用户表
     */
    private suspend fun migrateDefaultCategoriesToUserTable() {
        try {
            // 使用同步方法获取默认分类列表
            val categories = appCategoryDefaultDao.getAllDefaultCategoriesList()
            android.util.Log.d("AppRepository", "从默认表获取到 ${categories.size} 个分类")
            
            if (categories.isNotEmpty()) {
                val userCategories = categories.map { defaultCategory ->
                    AppCategoryEntity(
                        id = 0, // 让数据库自动生成ID
                        name = defaultCategory.name,
                        displayOrder = defaultCategory.displayOrder,
                        isDefault = defaultCategory.isDefault,
                        isLocked = defaultCategory.isLocked,
                        targetType = defaultCategory.targetType,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                }
                insertCategories(userCategories)
                android.util.Log.d("AppRepository", "成功迁移 ${userCategories.size} 个分类到用户表")
                
                // 验证迁移结果
                val migratedCount = getCategoryCount()
                android.util.Log.d("AppRepository", "迁移完成后用户表分类数量: $migratedCount")
            } else {
                android.util.Log.w("AppRepository", "默认表中没有分类数据！正在检查并修复...")
                
                // 如果默认表为空，尝试手动插入默认分类
                val manualCategories = listOf(
                    AppCategoryEntity(
                        id = 0,
                        name = "娱乐",
                        displayOrder = 1,
                        isDefault = true,
                        isLocked = false,
                        targetType = "LESS_THAN",
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    ),
                    AppCategoryEntity(
                        id = 0,
                        name = "学习",
                        displayOrder = 2,
                        isDefault = true,
                        isLocked = false,
                        targetType = "MORE_THAN",
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    ),
                    AppCategoryEntity(
                        id = 0,
                        name = "健身",
                        displayOrder = 3,
                        isDefault = true,
                        isLocked = false,
                        targetType = "MORE_THAN",
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    ),
                    AppCategoryEntity(
                        id = 0,
                        name = "总使用",
                        displayOrder = 4,
                        isDefault = true,
                        isLocked = true,
                        targetType = "LESS_THAN",
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                )
                
                android.util.Log.d("AppRepository", "手动创建 ${manualCategories.size} 个默认分类")
                insertCategories(manualCategories)
                
                val finalCount = getCategoryCount()
                android.util.Log.d("AppRepository", "手动创建完成后用户表分类数量: $finalCount")
            }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "迁移分类数据失败: ${e.message}", e)
        }
    }
    
    /**
     * 清理重复的分类数据
     */
    private suspend fun cleanupDuplicateCategories() {
        try {
            val allCategories = appCategoryDao.getAllCategoriesList()
            android.util.Log.d("AppRepository", "检查重复分类 - 总共有 ${allCategories.size} 个分类")
            
            // 按分类名称分组，找出重复的分类
            val duplicateGroups = allCategories.groupBy { it.name }.filter { it.value.size > 1 }
            
            for ((categoryName, duplicates) in duplicateGroups) {
                android.util.Log.d("AppRepository", "发现重复分类: $categoryName (${duplicates.size}个)")
                
                // 保留最早创建的分类，删除其他重复的
                val toKeep = duplicates.minByOrNull { it.createdAt }
                val toDelete = duplicates.filter { it.id != toKeep?.id }
                
                for (duplicate in toDelete) {
                    android.util.Log.d("AppRepository", "删除重复分类: ${duplicate.name} (ID=${duplicate.id})")
                    // 先删除相关目标，再删除分类
                    try {
                        goalUserDao.deleteUserGoal(duplicate.id)
                    } catch (e: Exception) {
                        android.util.Log.w("AppRepository", "删除分类 ${duplicate.id} 的目标失败: ${e.message}")
                    }
                    appCategoryDao.deleteCategory(duplicate.id)
                }
            }
            
            val finalCount = getCategoryCount()
            android.util.Log.d("AppRepository", "清理完成后分类数量: $finalCount")
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "清理重复分类失败: ${e.message}", e)
        }
    }
    
    /**
     * 清理不需要的虚拟线下活动APP
     * 清理已删除分类对应的虚拟线下活动APP
     */
    private suspend fun cleanupUnnecessaryOfflineApps() {
        try {
            // 清理"排除统计"分类对应的虚拟线下活动APP（该分类已删除）
            val excludeStatsPackageNames = listOf(
                "com.offtime.offline.排除统计",
                "com.offtime.offline.exclude_stats"
            )
            
            for (packageName in excludeStatsPackageNames) {
                val existingApp = appInfoDao.getAppByPackageName(packageName)
                if (existingApp != null) {
                    android.util.Log.d("AppRepository", "删除不需要的虚拟APP: ${existingApp.appName}")
                    appInfoDao.deleteApp(packageName)
                }
            }
            
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "清理不需要的虚拟线下活动APP失败: ${e.message}", e)
        }
    }
    
    /**
     * 确保每个分类都有对应的虚拟线下活动APP
     * 包括"总使用"分类，因为设置中的线下活动功能需要它
     */
    private suspend fun ensureOfflineActivityApps() {
        try {
            val categories = appCategoryDao.getAllCategoriesList()
            android.util.Log.d("AppRepository", "为 ${categories.size} 个分类创建虚拟线下活动APP")
            
            for (category in categories) {
                // 使用英文分类名创建包名，确保智能分类能够正确识别
                val categoryEnglishName = when (category.name) {
                    "娱乐" -> "yule"
                    "学习" -> "xuexi" 
                    "健身" -> "jianshen"
                    "总使用" -> "zongshiyong"
                    else -> category.name.lowercase(java.util.Locale.getDefault())
                }
                val newPackageName = "com.offtime.offline.$categoryEnglishName"
                val oldPackageName = "com.offtime.offline.${category.name.lowercase(java.util.Locale.getDefault())}"
                
                // 检查是否存在旧格式的虚拟APP，如果存在则迁移
                val oldApp = appInfoDao.getAppByPackageName(oldPackageName)
                if (oldApp != null && oldPackageName != newPackageName) {
                    android.util.Log.d("AppRepository", "迁移旧格式虚拟APP: $oldPackageName -> $newPackageName")
                    // 删除旧的
                    appInfoDao.deleteApp(oldPackageName)
                    // 创建新的（下面的逻辑会处理）
                }
                
                val existingApp = appInfoDao.getAppByPackageName(newPackageName)
                
                if (existingApp == null) {
                    val virtualApp = AppInfoEntity(
                        packageName = newPackageName,
                        appName = "🏃 线下活动 (${category.name})",
                        versionName = "1.0.0",
                        versionCode = 1,
                        isSystemApp = false,
                        categoryId = category.id,
                        firstInstallTime = System.currentTimeMillis(),
                        lastUpdateTime = System.currentTimeMillis(),
                        isEnabled = true,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    
                    appInfoDao.insertApp(virtualApp)
                    android.util.Log.d("AppRepository", "创建虚拟APP: ${virtualApp.appName} -> 分类ID=${category.id}")
                } else {
                    // 检查现有虚拟APP的分类是否正确
                    if (existingApp.categoryId != category.id) {
                        android.util.Log.d("AppRepository", "更新虚拟APP分类: ${existingApp.appName} ${existingApp.categoryId} -> ${category.id}")
                        appInfoDao.updateAppCategory(newPackageName, category.id)
                    } else {
                        android.util.Log.d("AppRepository", "虚拟APP已存在且分类正确: ${existingApp.appName}")
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "创建虚拟线下活动APP失败: ${e.message}", e)
        }
    }
    
    /**
     * 修复已存在的应用分类数据
     * 注意：这个方法会覆盖用户的分类设置，请谨慎使用！
     * 只应在调试或特殊修复情况下手动调用
     */
    suspend fun fixExistingAppCategories() {
        try {
            android.util.Log.d("AppRepository", "开始修复现有应用的分类数据")
            
            val allApps = appInfoDao.getAllAppsList()
            val categories = appCategoryDao.getAllCategoriesList()
            
            android.util.Log.d("AppRepository", "找到 ${allApps.size} 个应用，${categories.size} 个分类")
            if (categories.none { it.name == "娱乐" }) {
                android.util.Log.e("AppRepository", "错误：'娱乐' 分类未在数据库中找到！")
                return
            }
            if (categories.none { it.name == "学习" }) {
                android.util.Log.e("AppRepository", "错误：'学习' 分类未在数据库中找到！")
                return
            }
            if (categories.none { it.name == "健身" }) {
                android.util.Log.e("AppRepository", "错误：'健身' 分类未在数据库中找到！")
                return
            }

            for (app in allApps) {
                val correctCategoryName = when {
                    app.packageName == "com.google.android.youtube" || 
                    app.packageName == "tv.danmaku.bili" ||
                    app.packageName == "com.ss.android.ugc.aweme" -> "娱乐"
                    
                    app.packageName == "com.duolingo" ||
                    app.packageName == "com.youdao.dict" -> "学习"
                    
                    app.packageName.contains("fitness") || app.packageName.contains("health") -> "健身"
                    
                    else -> "娱乐" // Default to Entertainment
                }
                
                val correctCategory = categories.find { it.name == correctCategoryName }
                
                if (app.packageName == "com.google.android.youtube") {
                    android.util.Log.i("AppRepository", "调试YouTube: 当前 pkg=${app.packageName}, appName=${app.appName}, currentCatId=${app.categoryId}, 预期分类名='$correctCategoryName', 预期catId=${correctCategory?.id}")
                }

                if (correctCategory != null) {
                    if (app.categoryId != correctCategory.id) {
                        android.util.Log.i("AppRepository", 
                            "修正应用分类: ${app.appName} (${app.packageName}) 从 ${app.categoryId} -> ${correctCategory.id} (目标名称: ${correctCategory.name})")
                        appInfoDao.updateAppCategory(app.packageName, correctCategory.id)
                    } else {
                        if (app.packageName == "com.google.android.youtube") {
                             android.util.Log.i("AppRepository", "调试YouTube: 分类已正确为 ${app.categoryId} (${correctCategory.name})")
                        }
                    }
                } else {
                    android.util.Log.w("AppRepository", "警告: 应用 ${app.appName} (${app.packageName}) 的目标分类 '$correctCategoryName' 未在数据库中找到。将保持当前分类 ${app.categoryId}。")
                }
            }
            
            android.util.Log.d("AppRepository", "应用分类修复完成")
            
            // After fixing, log YouTube's category from app_info_users table directly
            val youtubeAppInfo = appInfoDao.getAppByPackageName("com.google.android.youtube")
            if (youtubeAppInfo != null) {
                android.util.Log.i("AppRepository", "修复后检查YouTube (app_info_users): pkg=${youtubeAppInfo.packageName}, appName=${youtubeAppInfo.appName}, categoryId=${youtubeAppInfo.categoryId}")
            } else {
                android.util.Log.w("AppRepository", "修复后检查YouTube: 在app_info_users中未找到YouTube应用条目。")
            }

        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "修复应用分类失败: ${e.message}", e)
        }
    }
    
    /**
     * 初始化应用数据 - 首次授权后调用
     * 注意：保护用户现有的分类设置，只添加新应用
     */
    suspend fun initializeAppData(): Boolean {
        return try {
            // 首先确保用户数据已初始化
            ensureUserDataInitialized()
            
            // 重新扫描所有应用
            val currentSystemApps = appInfoService.getAllInstalledApps()
            android.util.Log.d("AppRepository", "系统中扫描到 ${currentSystemApps.size} 个应用")
            
            // 获取数据库中已有的应用
            val existingApps = appInfoDao.getAllAppsList()
            val existingPackageNames = existingApps.map { it.packageName }.toSet()
            android.util.Log.d("AppRepository", "数据库中已有 ${existingApps.size} 个应用")
            
            // 只处理新安装的应用，保护现有应用的分类设置
            val newApps = currentSystemApps.filter { it.packageName !in existingPackageNames }
            val updatedApps = currentSystemApps.filter { newApp ->
                existingPackageNames.contains(newApp.packageName)
            }
            
            if (newApps.isNotEmpty()) {
                android.util.Log.d("AppRepository", "发现 ${newApps.size} 个新安装的应用，将添加到数据库")
                // 为新应用使用智能分类
                for (newApp in newApps) {
                    android.util.Log.d("AppRepository", "新增应用: ${newApp.appName} (${newApp.packageName}) -> 分类ID=${newApp.categoryId}")
                }
                insertApps(newApps)
            }
            
            if (updatedApps.isNotEmpty()) {
                android.util.Log.d("AppRepository", "更新 ${updatedApps.size} 个现有应用的版本信息（保持分类不变）")
                for (updatedApp in updatedApps) {
                    val existingApp = existingApps.find { it.packageName == updatedApp.packageName }
                    if (existingApp != null) {
                        // 只更新版本等信息，保持用户的分类设置
                        val preservedApp = updatedApp.copy(
                            categoryId = existingApp.categoryId, // 保持用户的分类设置
                            createdAt = existingApp.createdAt    // 保持创建时间
                        )
                        updateApp(preservedApp)
                    }
                }
            }
            
            // 移除已卸载的应用
            val currentPackageNames = currentSystemApps.map { it.packageName }.toSet()
            val uninstalledApps = existingApps.filter { it.packageName !in currentPackageNames }
            if (uninstalledApps.isNotEmpty()) {
                android.util.Log.d("AppRepository", "发现 ${uninstalledApps.size} 个已卸载的应用，将从数据库移除")
                for (uninstalledApp in uninstalledApps) {
                    deleteApp(uninstalledApp.packageName)
                }
            }
            
            android.util.Log.d("AppRepository", "应用数据初始化完成：新增${newApps.size}个，更新${updatedApps.size}个，移除${uninstalledApps.size}个")
            true
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "初始化应用数据失败: ${e.message}", e)
            false
        }
    }
    
    /**
     * 强制重新扫描并添加所有应用 - 调试用
     * 这个方法会清除现有非线下活动应用，重新添加所有真实应用
     */
    suspend fun forceReinitializeAllApps(): Boolean {
        return try {
            android.util.Log.d("AppRepository", "开始强制重新初始化所有应用")
            
            // 确保用户数据已初始化
            ensureUserDataInitialized()
            
            // 获取所有线下活动应用（保留这些）
            val existingApps = appInfoDao.getAllAppsList()
            val offlineApps = existingApps.filter { it.packageName.startsWith("com.offtime.offline.") }
            android.util.Log.d("AppRepository", "保留 ${offlineApps.size} 个线下活动应用")
            
            // 删除所有非线下活动的现有应用
            val realApps = existingApps.filter { !it.packageName.startsWith("com.offtime.offline.") }
            for (realApp in realApps) {
                deleteApp(realApp.packageName)
            }
            android.util.Log.d("AppRepository", "删除了 ${realApps.size} 个现有真实应用")
            
            // 重新扫描所有应用
            val currentSystemApps = appInfoService.getAllInstalledApps()
            android.util.Log.d("AppRepository", "系统中扫描到 ${currentSystemApps.size} 个应用")
            
            // 添加所有扫描到的应用
            if (currentSystemApps.isNotEmpty()) {
                insertApps(currentSystemApps)
                android.util.Log.d("AppRepository", "重新添加了 ${currentSystemApps.size} 个应用")
                
                // 详细记录添加的应用
                for (app in currentSystemApps.take(10)) {  // 只记录前10个，避免日志过长
                    android.util.Log.d("AppRepository", "添加应用: ${app.appName} (${app.packageName}) -> 分类ID=${app.categoryId}")
                }
                if (currentSystemApps.size > 10) {
                    android.util.Log.d("AppRepository", "... 还有 ${currentSystemApps.size - 10} 个应用")
                }
            }
            
            val finalAppCount = getAppCount()
            android.util.Log.d("AppRepository", "强制重新初始化完成，最终应用数量: $finalAppCount")
            true
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "强制重新初始化失败: ${e.message}", e)
            false
        }
    }
    
    /**
     * 增量更新应用信息
     */
    suspend fun updateAppInfo(packageName: String): Boolean {
        return try {
            val appInfo = appInfoService.getAppInfo(packageName)
            if (appInfo != null) {
                val existingApp = getAppByPackageName(packageName)
                if (existingApp != null) {
                    // 更新现有应用
                    updateApp(appInfo.copy(
                        categoryId = existingApp.categoryId, // 保持原有分类
                        createdAt = existingApp.createdAt
                    ))
                } else {
                    // 新安装的应用
                    insertApp(appInfo)
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * 移除已卸载的应用
     */
    suspend fun removeUninstalledApp(packageName: String) {
        try {
            deleteApp(packageName)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 智能设置系统应用为排除状态
     * 只设置那些明显是系统应用但当前未排除的应用
     * 保护用户手动设置的分类
     */
    suspend fun reclassifySystemAppsToExcludeStats(): Boolean {
        return try {
            android.util.Log.d("AppRepository", "开始设置系统应用为排除状态")
            
            // 确保用户数据已初始化
            ensureUserDataInitialized()
            
            // 获取所有现有应用
            val existingApps = appInfoDao.getAllAppsList()
            android.util.Log.d("AppRepository", "检查 ${existingApps.size} 个现有应用")
            
            var reclassifiedCount = 0
            
            for (app in existingApps) {
                // 检查是否是系统应用且当前未被排除
                if (shouldReclassifyAsSystemApp(app.packageName) && !app.isExcluded) {
                    android.util.Log.d("AppRepository", "设置系统应用为排除状态: ${app.appName} (${app.packageName})")
                    appInfoDao.updateAppExcludeStatus(app.packageName, true)
                    reclassifiedCount++
                }
            }
            
            android.util.Log.d("AppRepository", "系统应用排除设置完成，共设置 $reclassifiedCount 个应用为排除状态")
            true
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "设置系统应用排除状态失败: ${e.message}", e)
            false
        }
    }
    
    /**
     * 判断应用是否应该被设置为排除状态
     * 这个方法用于识别系统应用
     */
    private fun shouldReclassifyAsSystemApp(packageName: String): Boolean {
        // 系统应用包名列表 - 这些应用应该被设置为排除状态
        val systemAppPackages = setOf(
            // 用户反馈需要排除的系统应用
            "com.android.mtp",                      // MTP Host
            "com.google.android.modulemetadata",    // Main components (模块元数据)
            "com.google.android.mainline.adservices", // Main components (广告服务)
            "com.google.android.feedback",          // Market Feedback Agent
            "com.google.android.markup",            // Markup
            "com.google.android.providers.media.module", // Media picker
            "com.google.android.avatarpicker",      // Choose a picture
            
            // 启动器应用（系统桌面）- 不应被统计使用时间
            "com.google.android.apps.nexuslauncher", // Pixel Launcher
            "com.android.launcher",                 // 原生Android Launcher
            "com.android.launcher3",                // Android Launcher3
            "com.miui.home",                       // MIUI桌面
            "com.huawei.android.launcher",         // 华为桌面
            "com.oppo.launcher",                   // OPPO桌面
            "com.vivo.launcher",                   // vivo桌面
            "com.oneplus.launcher",                // OnePlus桌面
            "com.sec.android.app.launcher",        // 三星桌面
            "com.sonymobile.home",                 // 索尼桌面
            
            // 小米/MIUI系统应用
            "com.miui.powerkeeper",                // MIUI电量和性能
            "com.miui.battery",                    // MIUI电池管理
            "com.miui.cleanmaster",                // MIUI清理大师
            "com.miui.antispam",                   // MIUI骚扰拦截
            "com.miui.personalassistant",          // MIUI智能助理
            "com.miui.voiceassist",                // 小爱同学
            "com.xiaomi.aiasst.service",           // 小爱助手服务
            "com.xiaomi.market",                   // 小米应用商店
            "com.xiaomi.gamecenter",               // 小米游戏中心
            "com.xiaomi.payment",                  // 小米支付
            "com.xiaomi.smarthome",                // 米家
            "com.xiaomi.xmsf",                     // 小米服务框架
            "com.xiaomi.scanner",                  // 小米扫一扫
            "com.xiaomi.shop",                     // 小米商城
            "com.xiaomi.vipaccount",               // 小米VIP
            "com.miui.cloudservice",               // MIUI云服务
            "com.miui.screenrecorder",             // MIUI屏幕录制
            "com.miui.touchassistant",             // MIUI悬浮球
            "com.miui.networkassistant",           // MIUI网络助手
            "com.miui.yellowpage",                 // MIUI黄页
            
            // 华为/荣耀系统应用
            "com.huawei.appmarket",                // 华为应用市场
            "com.huawei.gamecenter",               // 华为游戏中心
            "com.huawei.health",                   // 华为健康
            "com.huawei.hiassistant",              // 华为智慧助手
            "com.huawei.hicare",                   // 华为服务
            "com.huawei.hicloud",                  // 华为云空间
            "com.huawei.hivoice",                  // 华为语音助手
            "com.huawei.securitymgr",              // 华为手机管家
            "com.huawei.systemmanager",            // 华为系统管理
            "com.huawei.wallet",                   // 华为钱包
            "com.huawei.browser",                  // 华为浏览器
            "com.huawei.himovie",                  // 华为视频
            "com.huawei.himusic",                  // 华为音乐
            "com.huawei.hireader",                 // 华为阅读
            "com.huawei.scanner",                  // 华为智慧识屏
            "com.huawei.screenrecorder",           // 华为屏幕录制
            "com.huawei.search",                   // 华为搜索
            "com.huawei.mirror",                   // 华为多屏协同
            "com.huawei.powergenie",               // 华为省电精灵
            "com.hihonor.health",                  // 荣耀健康
            "com.hihonor.hicare",                  // 荣耀服务
            "com.hihonor.wallet",                  // 荣耀钱包
            
            // 三星系统应用
            "com.samsung.android.bixby.service",   // Bixby服务
            "com.samsung.android.health",          // 三星健康
            "com.samsung.android.samsungpass",     // 三星通行证
            "com.samsung.android.spay",            // 三星支付
            "com.samsung.android.wellbeing",       // 三星数字健康
            "com.samsung.knox.securefolder",       // 三星安全文件夹
            "com.samsung.android.bixbyvision",     // Bixby视觉
            "com.samsung.android.app.galaxyfinder", // 三星查找
            "com.samsung.android.calendar",        // 三星日历
            "com.samsung.android.contacts",        // 三星联系人
            "com.samsung.android.messaging",       // 三星信息
            "com.samsung.android.themestore",      // 三星主题商店
            "com.samsung.android.game.gamehome",   // 三星游戏启动器
            "com.samsung.android.gametuner",       // 三星游戏优化器
            "com.samsung.android.oneconnect",      // 三星SmartThings
            "com.samsung.android.privateshare",    // 三星私密分享
            "com.samsung.android.smartface",       // 三星智能识别
            "com.samsung.android.smartmirroring",  // 三星智能镜像
            "com.samsung.android.stickercenter",   // 三星贴纸中心
            "com.samsung.android.voicewakeup",     // 三星语音唤醒
            
            // vivo系统应用
            "com.vivo.appstore",                   // vivo应用商店
            "com.vivo.gamecenter",                 // vivo游戏中心
            "com.vivo.health",                     // vivo健康
            "com.vivo.jovi",                       // Jovi智能助手
            "com.vivo.pushservice",                // vivo推送服务
            "com.vivo.wallet",                     // vivo钱包
            "com.vivo.browser",                    // vivo浏览器
            "com.vivo.scanner",                    // vivo扫一扫
            "com.vivo.screen.recorder",            // vivo屏幕录制
            "com.vivo.video",                      // vivo视频
            "com.vivo.weather",                    // vivo天气
            "com.vivo.childrenmode",               // vivo儿童模式
            "com.vivo.globalsearch",               // vivo全局搜索
            "com.vivo.magazine",                   // vivo杂志锁屏
            "com.vivo.minscreen",                  // vivo小窗模式
            "com.vivo.permissionmanager",          // vivo权限管理
            "com.vivo.securedpay",                 // vivo安全支付
            "com.vivo.smartmultiwindow",           // vivo智能多窗口
            "com.vivo.touchassistant",             // vivo悬浮球
            "com.vivo.translator",                 // vivo翻译
            "com.vivo.yellowpage",                 // vivo黄页
            "com.bbk.account",                     // BBK账户
            "com.bbk.cloud",                       // BBK云服务
            "com.bbk.appstore",                    // BBK应用商店
            "com.bbk.theme",                       // BBK主题
            "com.iqoo.secure",                     // iQOO安全中心
            
            // OPPO系统应用
            "com.oppo.battery",                    // OPPO电池管理
            "com.oppo.powermanager",               // OPPO电源管理
            "com.oppo.breeno",                     // 小布助手
            "com.oppo.breeno.service",             // 小布助手服务
            "com.oppo.breeno.speech",              // 小布语音
            "com.oppo.breeno.assistant",           // 小布助手主程序
            "com.oppo.breeno.suggestion",          // 小布建议
            "com.oppo.breeno.quicksearch",         // 小布快搜
            "com.oppo.breeno.weather",             // 小布天气
            "com.oppo.breeno.cards",               // 小布卡片
            "com.oppo.breeno.launcher",            // 小布启动器
            "com.oppo.safecenter",                 // OPPO手机管家
            "com.oppo.usercenter",                 // OPPO用户中心
            "com.oppo.market",                     // OPPO软件商店
            "com.oppo.ota",                        // OPPO系统更新
            "com.oppo.otaui",                      // OPPO更新界面
            "com.oppo.oppopush",                   // OPPO推送服务
            "com.oppo.statistics.rom",             // OPPO统计服务
            "com.oppo.secscanservice",             // OPPO安全扫描
            "com.oppo.securityguard",              // OPPO安全卫士
            "com.oppo.securitykeyboard",           // OPPO安全键盘
            "com.oppo.securitypermission",         // OPPO安全权限
            "com.oppo.sysoptimizer",               // OPPO系统优化
            "com.oppo.usagestats",                 // OPPO使用统计
            "com.oppo.wellbeing",                  // OPPO数字健康
            "com.oppo.smartsidebar",               // OPPO智能侧边栏
            "com.oppo.assistantscreen",            // OPPO助手屏幕
            "com.oppo.gamespace",                  // OPPO游戏空间
            "com.oppo.games",                      // OPPO游戏中心
            "com.oppo.childrenspace",              // OPPO儿童空间
            "com.oppo.oppomultiapp",               // OPPO应用分身
            "com.oppo.activateservice",            // OPPO激活服务
            "com.oppo.autoregistration",           // OPPO自动注册
            "com.oppo.crashbox",                   // OPPO崩溃收集
            "com.oppo.deviceinfo",                 // OPPO设备信息
            "com.oppo.healthservice",              // OPPO健康服务
            "com.oppo.location",                   // OPPO定位服务
            "com.oppo.nhs",                        // OPPO网络健康服务
            "com.oppo.resmonitor",                 // OPPO资源监控
            "com.oppo.romupdate",                  // OPPO ROM更新
            "com.oppo.timeservice",                // OPPO时间服务
            
            // 系统UI和核心服务
            "com.android.systemui",                // 系统UI
            "com.android.settings",                // 系统设置
            "com.google.android.gms",              // Google Play Services
            "com.google.android.gsf",              // Google服务框架
            "com.android.vending",                 // Google Play商店
            "com.android.packageinstaller",        // 包安装器
            
            // 系统通信和网络
            "com.android.phone",                   // 电话
            "com.android.dialer",                  // 拨号器
            "com.android.mms",                     // 短信
            "com.android.bluetooth",               // 蓝牙
            "com.android.nfc",                     // NFC
            "com.google.android.bluetooth",        // Google蓝牙服务
            "com.google.android.networkstack",     // 网络栈
            
            // 系统数据提供者
            "com.android.providers.media",         // 媒体提供者
            "com.android.providers.downloads",     // 下载提供者
            "com.android.providers.calendar",      // 日历提供者
            "com.android.providers.contacts",      // 联系人提供者
            "com.android.providers.telephony",     // 电话提供者
            "com.android.providers.settings",      // 设置提供者
            
            // Google系统服务
            "com.google.android.ext.services",     // 扩展服务
            "com.google.android.permissioncontroller", // 权限控制器
            "com.google.android.adservices",       // 广告服务
            "com.google.android.webview",          // WebView
            "com.google.android.configupdater",    // 配置更新器
            
            // 安全和管理
            "com.miui.securitycenter",             // MIUI安全中心
            "com.huawei.systemmanager",            // 华为手机管家
            "com.oppo.safe",                       // OPPO手机管家
            "com.vivo.abe",                        // vivo应用管理
            "com.xiaomi.finddevice",               // 查找设备
            
            // 其他常见系统应用
            "android",                             // Android系统
            "com.android.shell",                   // Shell
            "com.android.externalstorage",         // 外部存储
            "com.android.sharedstoragebackup",     // 共享存储备份
            "com.android.keychain",                // 密钥链
            "com.android.captiveportallogin",      // 认证门户登录
            "com.android.emergency",               // 紧急服务
            "com.android.wallpaper.livepicker"     // 动态壁纸选择器
        )
        
        // 检查精确包名匹配
        if (systemAppPackages.contains(packageName)) {
            return true
        }
        
        // 通过包名模式识别系统应用
        return when {
            // Android系统核心组件
            packageName.startsWith("com.android.internal.") -> true
            packageName.startsWith("com.android.providers.") -> true
            packageName.startsWith("com.android.server.") -> true
            packageName.startsWith("com.android.systemui") -> true
            packageName.startsWith("com.android.bluetooth") -> true
            packageName.startsWith("com.android.dreams") -> true
            packageName.startsWith("com.android.egg") -> true
            packageName.startsWith("com.android.wallpaper") -> true
            packageName.startsWith("com.android.keychain") -> true
            packageName.startsWith("com.android.captive") -> true
            packageName.startsWith("com.android.managedprovisioning") -> true
            packageName.startsWith("com.android.provision") -> true
            packageName.startsWith("com.android.storagemanager") -> true
            packageName.startsWith("com.android.bips") -> true
            packageName.startsWith("com.android.cellbroadcast") -> true
            packageName.startsWith("com.android.emergency") -> true
            packageName.startsWith("com.android.inputmethod") -> true
            packageName.startsWith("com.android.printspooler") -> true
            packageName.startsWith("com.android.sharedstoragebackup") -> true
            packageName.startsWith("com.android.externalstorage") -> true
            
            // 系统角色和权限
            packageName.startsWith("com.android.role.") -> true
            
            // 系统主题、字体、图标
            packageName.startsWith("com.android.theme.") -> true
            
            // 网络相关系统组件
            packageName.startsWith("com.android.pacprocessor") -> true
            
            // 相机扩展和系统组件
            packageName.startsWith("com.android.cameraextensions") -> true
            packageName.startsWith("com.android.carrierdefault") -> true
            
            // Google智能和扩展服务
            packageName.startsWith("com.google.android.as") -> true
            packageName.startsWith("com.google.android.ext.") -> true
            packageName.startsWith("com.google.android.apps.restore") -> true
            packageName.startsWith("com.google.android.cellbroadcast") -> true
            packageName.startsWith("com.google.android.captiveportal") -> true
            packageName.startsWith("com.google.android.configupdater") -> true
            packageName.startsWith("com.google.android.contacts") -> true
            packageName.startsWith("com.google.android.apps.wellbeing") -> true
            
            // 设备管理和安全
            packageName.startsWith("com.android.compos") -> true
            packageName.startsWith("com.android.companiondevice") -> true
            packageName.startsWith("com.android.credential") -> true
            packageName.startsWith("com.android.devicediagnostics") -> true
            packageName.startsWith("com.android.devicelock") -> true
            
            // Google系统初始化和设置
            packageName.startsWith("com.google.android.onetimeinitializer") -> true
            packageName.startsWith("com.google.android.partnersetup") -> true
            packageName.startsWith("com.google.android.odad") -> true
            packageName.startsWith("com.google.android.safetycenter") -> true
            packageName.startsWith("com.google.android.printservice") -> true
            packageName.startsWith("com.google.android.rkpdapp") -> true
            packageName.startsWith("com.google.android.settings.intelligence") -> true
            packageName.startsWith("com.google.android.connectivity.resources") -> true
            packageName.startsWith("com.google.android.uwb.resources") -> true
            packageName.startsWith("com.google.android.wifi.resources") -> true
            packageName.startsWith("com.google.android.tag") -> true
            packageName.startsWith("com.google.android.apps.wallpaper") -> true
            packageName.startsWith("com.google.android.federatedcompute") -> true
            packageName.startsWith("com.google.android.googlesdksetup") -> true
            packageName.startsWith("com.google.android.health.connect") -> true
            packageName.startsWith("com.google.android.sdksandbox") -> true
            packageName.startsWith("com.google.android.server.deviceconfig") -> true
            packageName.startsWith("com.google.android.telephony.satellite") -> true
            packageName.startsWith("com.google.android.wifi.dialog") -> true
            
            // 系统工具和服务
            packageName.startsWith("com.android.htmlviewer") -> true
            packageName.startsWith("com.android.mms.service") -> true
            packageName.startsWith("com.android.proxyhandler") -> true
            packageName.startsWith("com.android.stk") -> true
            packageName.startsWith("com.android.se") -> true
            packageName.startsWith("com.android.traceur") -> true
            packageName.startsWith("com.android.vpndialogs") -> true
            packageName.startsWith("com.android.DeviceAsWebcam") -> true
            packageName.startsWith("com.android.backupconfirm") -> true
            packageName.startsWith("com.android.carrierconfig") -> true
            packageName.startsWith("com.android.cts") -> true
            packageName.startsWith("com.android.emulator") -> true
            packageName.startsWith("com.android.imsserviceentitlement") -> true
            packageName.startsWith("com.android.localtransport") -> true
            packageName.startsWith("com.android.microdroid") -> true
            packageName.startsWith("com.android.networkstack.tethering") -> true
            packageName.startsWith("com.android.ons") -> true
            packageName.startsWith("com.android.simappdialog") -> true
            
            // 自动生成的RRO (Runtime Resource Overlay)
            packageName.contains(".auto_generated_") -> true
            packageName.contains(".overlay.") -> true
            packageName.contains(".goldfish.overlay") -> true
            
            // === 抽象化系统应用识别规则 ===
            
            // 1. Google模块化系统组件
            packageName.startsWith("com.google.mainline.") -> true
            
            // 2. 系统功能性组件 (通过包名关键词)
            packageName.contains(".system") -> true
            packageName.contains(".internal") -> true
            packageName.contains(".config") -> true
            packageName.contains(".resources") && !packageName.contains("user") -> true
            packageName.contains(".service") && !packageName.contains("user") -> true
            packageName.contains(".provider") && !packageName.contains("user") -> true
            
            // 3. 系统工具和管理器 (通过包名末尾)
            packageName.endsWith("installer") -> true
            packageName.endsWith("resolver") -> true
            packageName.endsWith("picker") && packageName.startsWith("com.android") -> true
            packageName.endsWith("manager") && (packageName.startsWith("com.android") || packageName.startsWith("com.google.android")) -> true
            packageName.endsWith("host") && packageName.startsWith("com.android") -> true
            packageName.endsWith("agent") && packageName.startsWith("com.google.android") -> true
            
            // 4. 系统模块化组件
            packageName.contains("modulemetadata") -> true
            packageName.contains("appsearch") -> true
            packageName.contains("virtualmachine") -> true
            packageName.contains("ondevicepersonalization") -> true
            
            // 5. 系统特殊功能
            packageName.contains("dynsystem") -> true
            packageName.contains("location.fused") -> true
            packageName.contains("inputdevices") -> true
            packageName.contains("intentresolver") -> true
            packageName.contains("certinstaller") -> true
            packageName.contains("soundpicker") -> true
            packageName.contains("avatarpicker") -> true
            packageName.contains("feedback") && packageName.startsWith("com.google.android") -> true
            packageName.contains("markup") && packageName.startsWith("com.google.android") -> true
            packageName.contains("telemetry") && packageName.startsWith("com.google") -> true
            packageName.contains("tts") && packageName.startsWith("com.google.android") -> true
            
            // Google系统服务
            packageName.startsWith("com.google.android.gms") -> true
            packageName.startsWith("com.google.android.gsf") -> true
            packageName.startsWith("com.google.android.ext.services") -> true
            packageName.startsWith("com.google.android.adservices") -> true
            packageName.startsWith("com.google.android.permissioncontroller") -> true
            packageName.startsWith("com.google.android.packageinstaller") -> true
            packageName.startsWith("com.google.android.marvin") -> true
            packageName.startsWith("com.google.android.projection") -> true
            packageName.startsWith("com.google.android.webview") -> true
            packageName.startsWith("com.google.android.bluetooth") -> true
            packageName.startsWith("com.google.android.networkstack") -> true
            packageName.startsWith("com.google.android.hotspot2") -> true
            
            // 排除常见用户应用
            packageName.startsWith("com.android.chrome") -> false
            packageName.startsWith("com.google.android.youtube") -> false
            packageName.startsWith("com.google.android.gm") -> false // Gmail
            packageName.startsWith("com.google.android.apps.maps") -> false // Maps
            
            else -> false
        }
    }
    
    /**
     * 设置相关方法 - 委托给AppSettingsRepository
     */
    suspend fun getDefaultCategoryId(): Int {
        return appSettingsRepository.getDefaultCategoryId()
    }
    
    suspend fun getCategoryRewardPunishmentEnabled(): Map<Int, Boolean> {
        return appSettingsRepository.getCategoryRewardPunishmentEnabled()
    }
    
    suspend fun setDefaultCategoryId(categoryId: Int) {
        appSettingsRepository.setDefaultCategoryId(categoryId)
        android.util.Log.d("AppRepository", "设置默认分类ID: $categoryId")
    }
    
    suspend fun setCategoryRewardPunishmentEnabled(categoryId: Int, enabled: Boolean) {
        appSettingsRepository.setCategoryRewardPunishmentEnabled(categoryId, enabled)
        android.util.Log.d("AppRepository", "设置分类奖罚开关: categoryId=$categoryId, enabled=$enabled")
    }
    
    /**
     * 智能分类所有应用
     * 根据应用包名和特征，对所有非系统应用进行智能重新分类
     */
    suspend fun smartCategorizeAllApps(): Boolean {
        return try {
            android.util.Log.d("AppRepository", "开始智能分类所有应用")
            
            // 确保用户数据已初始化
            ensureUserDataInitialized()
            
            // 获取所有现有应用
            val existingApps = appInfoDao.getAllAppsList()
            android.util.Log.d("AppRepository", "对 ${existingApps.size} 个应用进行智能分类")
            
            var categorizedCount = 0
            
            for (app in existingApps) {
                // 跳过已排除的应用和系统应用，只对用户应用进行智能分类
                if (app.isExcluded || shouldReclassifyAsSystemApp(app.packageName)) {
                    continue
                }
                
                // 使用智能分类工具获取新的分类ID
                val newCategoryId = appCategoryUtils.getCategoryIdByPackageName(app.packageName)
                
                // 如果分类发生变化，则更新
                if (newCategoryId != app.categoryId) {
                    android.util.Log.d("AppRepository", 
                        "智能分类更新: ${app.appName} (${app.packageName}) ${app.categoryId} -> $newCategoryId")
                    appInfoDao.updateAppCategory(app.packageName, newCategoryId)
                    categorizedCount++
                    
                    // 特别记录虚拟APP的分类更新
                    if (app.packageName.startsWith("com.offtime.offline.")) {
                        android.util.Log.i("AppRepository", 
                            "虚拟线下活动APP分类更新: ${app.appName} -> 分类ID=$newCategoryId")
                    }
                }
            }
            
            android.util.Log.d("AppRepository", "智能分类完成，共更新 $categorizedCount 个应用的分类")
            true
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "智能分类失败: ${e.message}", e)
            false
        }
    }
    
    /**
     * 获取默认分类数量
     */
    suspend fun getDefaultCategoryCount(): Int {
        return appCategoryDefaultDao.getDefaultCategoryCount()
    }

    /**
     * 调试方法：强制重新初始化所有基础数据
     * 用于修复Google Play Debug版本无数据的问题
     */
    suspend fun debugForceInitializeAllData(): Boolean {
        return try {
            android.util.Log.d("AppRepository", "🚀 开始调试：强制重新初始化所有基础数据")
            
            // 1. 检查并修复默认分类表
            val defaultCategoryCount = appCategoryDefaultDao.getDefaultCategoryCount()
            android.util.Log.d("AppRepository", "默认分类表当前数量: $defaultCategoryCount")
            
            if (defaultCategoryCount == 0) {
                android.util.Log.w("AppRepository", "默认分类表为空！这是导致无数据的根本原因")
                
                // 手动插入默认分类到默认表
                val defaultCategories = listOf(
                    AppCategoryDefaultEntity(
                        id = 0,
                        name = "娱乐",
                        displayOrder = 1,
                        isDefault = true,
                        isLocked = false,
                        targetType = "LESS_THAN"
                    ),
                    AppCategoryDefaultEntity(
                        id = 0,
                        name = "学习",
                        displayOrder = 2,
                        isDefault = true,
                        isLocked = false,
                        targetType = "MORE_THAN"
                    ),
                    AppCategoryDefaultEntity(
                        id = 0,
                        name = "健身",
                        displayOrder = 3,
                        isDefault = true,
                        isLocked = false,
                        targetType = "MORE_THAN"
                    ),
                    AppCategoryDefaultEntity(
                        id = 0,
                        name = "总使用",
                        displayOrder = 4,
                        isDefault = true,
                        isLocked = true,
                        targetType = "LESS_THAN"
                    )
                )
                
                appCategoryDefaultDao.insertDefaultCategories(defaultCategories)
                android.util.Log.d("AppRepository", "✅ 已修复默认分类表，插入了 ${defaultCategories.size} 个分类")
            }
            
            // 2. 强制重新初始化用户数据
            val initialUserCategoryCount = getCategoryCount()
            android.util.Log.d("AppRepository", "用户分类表当前数量: $initialUserCategoryCount")
            
            if (initialUserCategoryCount == 0) {
                android.util.Log.d("AppRepository", "用户分类表为空，开始迁移数据")
                migrateDefaultCategoriesToUserTable()
                
                val finalUserCategoryCount = getCategoryCount()
                android.util.Log.d("AppRepository", "✅ 用户分类表修复完成，最终数量: $finalUserCategoryCount")
            }
            
            // 3. 确保虚拟线下活动APP存在
            ensureOfflineActivityApps()
            
            // 4. 如果有权限，重新扫描应用
            val appCount = getAppCount()
            android.util.Log.d("AppRepository", "应用表当前数量: $appCount")
            
            if (appCount == 0) {
                android.util.Log.d("AppRepository", "应用表为空，尝试初始化应用数据")
                initializeAppData()
                
                val finalAppCount = getAppCount()
                android.util.Log.d("AppRepository", "✅ 应用表修复完成，最终数量: $finalAppCount")
            }
            
            android.util.Log.d("AppRepository", "🎉 调试：强制重新初始化完成")
            true
        } catch (e: Exception) {
            android.util.Log.e("AppRepository", "❌ 调试：强制重新初始化失败: ${e.message}", e)
            false
        }
    }
} 