package com.offtime.app

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import com.offtime.app.manager.UsageStatsManager
import com.offtime.app.worker.UsageStatsWorker
import com.offtime.app.service.UnifiedUpdateService
import com.offtime.app.utils.FirstLaunchManager
import com.offtime.app.utils.LocaleUtils
import com.offtime.app.utils.UsageStatsPermissionHelper
import com.offtime.app.data.repository.AppRepository
import com.offtime.app.data.repository.GoalRewardPunishmentRepository
import com.offtime.app.data.database.OffTimeDatabase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class OffTimeApplication : Application() {
    
    companion object {
        lateinit var instance: OffTimeApplication
            private set
        
        private const val PREF_NAME = "app_language_settings"
        private const val KEY_LAST_LANGUAGE = "last_system_language"
    }
    
    @Inject
    lateinit var usageStatsManager: UsageStatsManager
    
    @Inject
    lateinit var firstLaunchManager: FirstLaunchManager
    
    @Inject
    lateinit var appRepository: AppRepository
    
    @Inject
    lateinit var goalRepository: GoalRewardPunishmentRepository
    
    @Inject
    lateinit var localeUtils: LocaleUtils
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // 注册语言变化监听器
        com.offtime.app.utils.LanguageChangeListener.register(this)
        
        // 应用语言设置
        applyLanguageConfiguration()
        
        // 检查系统语言变化并更新数据库
        checkAndUpdateLanguageData()
        
        android.util.Log.d("OffTimeApplication", "应用启动 - 首次启动: ${firstLaunchManager.isFirstLaunch()}, 引导完成: ${firstLaunchManager.isOnboardingCompleted()}")
        
        // 检查首次启动状态
        if (!firstLaunchManager.isFirstLaunch() && firstLaunchManager.isOnboardingCompleted()) {
            // 非首次启动且引导完成，启动正常服务
            startDataCollectionServices()
            
            // 后台检查并初始化数据（避免用户数据丢失）
            GlobalScope.launch {
                try {
                    // 首先检查默认分类数据是否存在
                    val defaultCount = appRepository.getDefaultCategoryCount()
                    android.util.Log.d("OffTimeApplication", "启动时检查默认分类数量: $defaultCount")
                    
                    if (defaultCount == 0) {
                        android.util.Log.w("OffTimeApplication", "检测到默认分类缺失，这是Google Play Debug版本无数据的根本原因，开始强制修复")
                        val fixResult = appRepository.debugForceInitializeAllData()
                        android.util.Log.d("OffTimeApplication", "强制修复结果: $fixResult")
                    } else {
                        // 正常的数据初始化
                        appRepository.ensureUserDataInitialized()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("OffTimeApplication", "后台数据初始化失败: ${e.message}", e)
                }
            }
        } else {
            android.util.Log.d("OffTimeApplication", "应用尚未完成初始化引导，跳过服务启动")
        }
    }
    
    /**
     * 启动数据收集相关服务
     */
    private fun startDataCollectionServices() {
        try {
            // 检查使用统计权限
            if (UsageStatsPermissionHelper.hasUsageStatsPermission(this)) {
                // 启动使用统计收集服务
                usageStatsManager.startUsageStatsCollection()
                android.util.Log.d("OffTimeApplication", "数据收集服务启动成功")
                
                // 启动统一更新服务（替代原来的分散更新机制）
                UnifiedUpdateService.startUnifiedUpdate(this)
                android.util.Log.d("OffTimeApplication", "统一更新服务启动成功")
                
                // 启动每分钟更新定时任务（作为备份机制）
                UsageStatsWorker.scheduleWork(this)
                android.util.Log.d("OffTimeApplication", "每分钟更新定时任务已调度")
                
            } else {
                android.util.Log.w("OffTimeApplication", "缺少使用统计权限，无法启动数据收集服务")
            }
        } catch (e: Exception) {
            android.util.Log.e("OffTimeApplication", "启动数据收集服务失败", e)
        }
    }
    
    /**
     * 应用语言配置
     */
    private fun applyLanguageConfiguration() {
        try {
            val effectiveLocale = localeUtils.getEffectiveLocale()
            val config = Configuration(resources.configuration)
            config.setLocale(effectiveLocale)
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                createConfigurationContext(config)
            } else {
                @Suppress("DEPRECATION")
                resources.updateConfiguration(config, resources.displayMetrics)
            }
            
            android.util.Log.d("OffTimeApplication", "应用语言配置完成: ${effectiveLocale.language}-${effectiveLocale.country}")
        } catch (e: Exception) {
            android.util.Log.e("OffTimeApplication", "应用语言配置失败", e)
        }
    }
    
    /**
     * 检查系统语言变化并更新数据库
     */
    private fun checkAndUpdateLanguageData() {
        try {
            val currentLanguage = java.util.Locale.getDefault().language
            val sharedPrefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            val lastLanguage = sharedPrefs.getString(KEY_LAST_LANGUAGE, null)
            
            android.util.Log.d("OffTimeApplication", "语言检查 - 当前: $currentLanguage, 上次: $lastLanguage")
            
            if (lastLanguage != null && lastLanguage != currentLanguage) {
                android.util.Log.d("OffTimeApplication", "检测到系统语言变化: $lastLanguage → $currentLanguage, 开始更新数据库")
                
                // 后台更新数据库中的语言相关数据
                GlobalScope.launch {
                    try {
                        updateLanguageRelatedData()
                        android.util.Log.d("OffTimeApplication", "语言相关数据更新完成")
                    } catch (e: Exception) {
                        android.util.Log.e("OffTimeApplication", "语言相关数据更新失败", e)
                    }
                }
            }
            
            // 保存当前语言
            sharedPrefs.edit().putString(KEY_LAST_LANGUAGE, currentLanguage).apply()
            
        } catch (e: Exception) {
            android.util.Log.e("OffTimeApplication", "检查系统语言变化失败", e)
        }
    }
    
    /**
     * 更新数据库中与语言相关的数据
     */
    private suspend fun updateLanguageRelatedData() {
        try {
            val database = OffTimeDatabase.getDatabase(this)
            val goalUserDao = database.goalRewardPunishmentUserDao()
            val goalDefaultDao = database.goalRewardPunishmentDefaultDao()
            
            // 清除语言缓存确保获取最新语言设置
            com.offtime.app.utils.UnifiedTextManager.clearLanguageCache()
            
            // 使用统一文本管理器获取标准文本
            val standardRewardText = com.offtime.app.utils.UnifiedTextManager.getStandardRewardText()
            val standardRewardUnit = com.offtime.app.utils.UnifiedTextManager.getStandardRewardUnit()
            val standardPunishText = com.offtime.app.utils.UnifiedTextManager.getStandardPunishmentText()
            val standardPunishUnit = com.offtime.app.utils.UnifiedTextManager.getStandardPunishmentUnit()
            val standardTimeUnitMinute = if (com.offtime.app.utils.UnifiedTextManager.isEnglish()) "minutes" else "分钟"
            val standardTimeUnitHour = if (com.offtime.app.utils.UnifiedTextManager.isEnglish()) "hours" else "小时"
            
            android.util.Log.d("OffTimeApplication", "更新为${if (com.offtime.app.utils.UnifiedTextManager.isEnglish()) "英文" else "中文"}标准: 奖励='$standardRewardText $standardRewardUnit', 惩罚='$standardPunishText $standardPunishUnit'")
            
            // 获取所有现有的目标数据
            val userGoals = goalUserDao.getAllUserGoalsList()
            val defaultGoals = goalDefaultDao.getAllDefaultGoalsList()
            
            // 更新用户目标数据
            for (goal in userGoals) {
                // 只更新默认的奖励惩罚文本，不覆盖用户自定义的内容
                if (isDefaultRewardPunishmentText(goal.rewardText, goal.punishText)) {
                    val updatedGoal = goal.copy(
                        rewardText = standardRewardText,
                        rewardUnit = standardRewardUnit,
                        punishText = standardPunishText,
                        punishUnit = standardPunishUnit,
                        goalTimeUnit = standardTimeUnitMinute,
                        rewardTimeUnit = standardTimeUnitHour,  
                        punishTimeUnit = standardTimeUnitHour,
                        updateTime = System.currentTimeMillis()
                    )
                    goalUserDao.updateUserGoal(updatedGoal)
                    android.util.Log.d("OffTimeApplication", "更新用户目标 catId=${goal.catId}: '${goal.rewardText}/${goal.punishText}' → '$standardRewardText/$standardPunishText'")
                }
            }
            
            // 更新默认目标数据
            for (goal in defaultGoals) {
                val updatedGoal = goal.copy(
                    rewardText = standardRewardText,
                    rewardUnit = standardRewardUnit,
                    punishText = standardPunishText,
                    punishUnit = standardPunishUnit,
                    goalTimeUnit = standardTimeUnitMinute,
                    rewardTimeUnit = standardTimeUnitHour,
                    punishTimeUnit = standardTimeUnitHour,
                    updateTime = System.currentTimeMillis()
                )
                goalDefaultDao.updateDefaultGoal(updatedGoal)
            }
            
            android.util.Log.d("OffTimeApplication", "语言相关数据更新完成: 用户目标${userGoals.size}条, 默认目标${defaultGoals.size}条")
            
        } catch (e: Exception) {
            android.util.Log.e("OffTimeApplication", "更新语言相关数据时出错", e)
            throw e
        }
    }
    
    /**
     * 判断是否为默认的奖励惩罚文本（需要跟随系统语言更新）
     */
    private fun isDefaultRewardPunishmentText(rewardText: String, punishText: String): Boolean {
        val defaultRewardTexts = setOf("薯片", "Chips")
        val defaultPunishTexts = setOf("俯卧撑", "Push-ups")
        
        return defaultRewardTexts.contains(rewardText) && defaultPunishTexts.contains(punishText)
    }
    
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        // 在依赖注入完成之前，我们先使用默认配置
        // 实际的语言配置会在onCreate中处理
    }
} 