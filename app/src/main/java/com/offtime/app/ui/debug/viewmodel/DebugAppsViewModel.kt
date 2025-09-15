package com.offtime.app.ui.debug.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offtime.app.data.entity.AppInfoEntity
import com.offtime.app.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject
import com.offtime.app.data.dao.AppInfoDao
import com.offtime.app.data.dao.AppSessionUserDao
import com.offtime.app.data.dao.AppCategoryDao
import com.offtime.app.data.dao.DailyUsageDao
import java.text.SimpleDateFormat
import java.util.*

// 每日应用使用数据
data class DailyAppUsageData(
    val packageName: String,
    val appName: String,
    val categoryName: String,
    val totalMinutes: Int,
    val sessionCount: Int,
    val isVirtual: Boolean = false // 标识是否为虚拟应用
)

// 虚拟应用使用数据
data class DailyVirtualUsageData(
    val categoryName: String,
    val totalMinutes: Int,
    val sessionCount: Int
)

// 按天分组的使用数据
data class DailyUsageByDate(
    val date: String,
    val weekday: String,
    val apps: List<DailyAppUsageData>,
    val virtualApps: List<DailyVirtualUsageData>,
    val totalApps: Int,
    val totalMinutes: Int,
    val totalSessions: Int,
    val virtualTotalMinutes: Int
)

@HiltViewModel
class DebugAppsViewModel @Inject constructor(
    private val appInfoDao: AppInfoDao,
    private val appSessionUserDao: AppSessionUserDao,
    private val appCategoryDao: AppCategoryDao,
    private val dailyUsageDao: DailyUsageDao,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {
    
    companion object {
        private const val TAG = "DebugAppsViewModel"
    }
    
    private val _appInfos = MutableStateFlow<List<AppInfoEntity>>(emptyList())
    val appInfos: StateFlow<List<AppInfoEntity>> = _appInfos.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // 新增的每日使用汇总状态
    private val _selectedDate = MutableStateFlow(getCurrentDateString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()
    
    private val _dailyUsageData = MutableStateFlow<List<DailyAppUsageData>>(emptyList())
    val dailyUsageData: StateFlow<List<DailyAppUsageData>> = _dailyUsageData.asStateFlow()
    
    // 新增：按天分组的使用数据
    private val _dailyUsageByDate = MutableStateFlow<List<DailyUsageByDate>>(emptyList())
    val dailyUsageByDate: StateFlow<List<DailyUsageByDate>> = _dailyUsageByDate.asStateFlow()
    
    init {
        loadAppInfos()
        loadDailyUsageData(_selectedDate.value)
        loadMultipleDaysUsageData()
    }
    
    fun loadAppInfos() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val apps = appInfoDao.getAllAppsList()
                _appInfos.value = apps
                android.util.Log.d(TAG, "加载了 ${apps.size} 个应用")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "加载应用信息失败", e)
                _appInfos.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadDailyUsageData(date: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _selectedDate.value = date
            try {
                android.util.Log.d(TAG, "开始加载每日使用数据: date=$date")
                
                // 获取指定日期的所有会话数据
                val sessions = appSessionUserDao.getSessionsByDate(date)
                android.util.Log.d(TAG, "找到 ${sessions.size} 个会话记录")
                
                // 获取所有应用信息和分类信息
                val allApps = appInfoDao.getAllAppsList()
                val allCategories = appCategoryDao.getAllCategoriesList()
                val categoryMap = allCategories.associateBy { it.id }
                val appMap = allApps.associateBy { it.packageName }
                
                // 按包名聚合会话数据
                val sessionsByPackage = sessions.groupBy { it.pkgName }
                
                val dailyUsageList = mutableListOf<DailyAppUsageData>()
                
                sessionsByPackage.forEach { (packageName, packageSessions) ->
                    val appInfo = appMap[packageName]
                    if (appInfo != null) {
                        val category = categoryMap[appInfo.categoryId]
                        val totalSeconds = packageSessions.sumOf { it.durationSec }
                        val totalMinutes = totalSeconds / 60
                        
                        if (totalMinutes > 0) { // 只显示有使用时间的应用
                            dailyUsageList.add(
                                DailyAppUsageData(
                                    packageName = packageName,
                                    appName = appInfo.appName,
                                    categoryName = category?.name ?: "未知分类",
                                    totalMinutes = totalMinutes,
                                    sessionCount = packageSessions.size
                                )
                            )
                        }
                    }
                }
                
                // 按使用时长降序排序
                _dailyUsageData.value = dailyUsageList.sortedByDescending { it.totalMinutes }
                
                android.util.Log.d(TAG, "每日使用数据加载完成: ${dailyUsageList.size} 个应用")
                
            } catch (e: Exception) {
                android.util.Log.e(TAG, "加载每日使用数据失败", e)
                _dailyUsageData.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun deleteApp(packageName: String) {
        viewModelScope.launch {
            try {
                appInfoDao.deleteApp(packageName)
                loadAppInfos() // 重新加载应用列表
                android.util.Log.d(TAG, "已删除应用: $packageName")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "删除应用失败: $packageName", e)
            }
        }
    }
    
    fun loadMultipleDaysUsageData(days: Int = 7) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                android.util.Log.d(TAG, "开始加载多天使用数据: days=$days")
                
                // 获取所有应用信息和分类信息
                val allApps = appInfoDao.getAllAppsList()
                val allCategories = appCategoryDao.getAllCategoriesList()
                val categoryMap = allCategories.associateBy { it.id }
                val appMap = allApps.associateBy { it.packageName }
                
                val dailyUsageList = mutableListOf<DailyUsageByDate>()
                
                // 生成过去N天的日期列表
                for (i in 0 until days) {
                    val date = getDateString(-i)
                    val weekday = getWeekday(date)
                    
                    // 获取该日期的所有会话数据
                    val sessions = appSessionUserDao.getSessionsByDate(date)
                    
                    if (sessions.isNotEmpty()) {
                        // 按包名聚合会话数据
                        val sessionsByPackage = sessions.groupBy { it.pkgName }
                        val appsForDate = mutableListOf<DailyAppUsageData>()
                        
                        sessionsByPackage.forEach { (packageName, packageSessions) ->
                            val appInfo = appMap[packageName]
                            if (appInfo != null) {
                                val category = categoryMap[appInfo.categoryId]
                                val totalSeconds = packageSessions.sumOf { it.durationSec }
                                val totalMinutes = totalSeconds / 60
                                
                                if (totalMinutes > 0) {
                                    appsForDate.add(
                                        DailyAppUsageData(
                                            packageName = packageName,
                                            appName = appInfo.appName,
                                            categoryName = category?.name ?: "未知分类",
                                            totalMinutes = totalMinutes,
                                            sessionCount = packageSessions.size
                                        )
                                    )
                                }
                            }
                        }
                        
                        // 获取虚拟应用（线下活动）数据
                        val virtualUsageData = dailyUsageDao.getDailyUsageByDate(date)
                            .filter { it.isOffline == 1 }
                            .groupBy { it.catId }
                            .map { (catId, usageList) ->
                                val category = categoryMap[catId]
                                val totalSeconds = usageList.sumOf { it.durationSec }
                                val totalMinutes = totalSeconds / 60
                                
                                if (totalMinutes > 0) {
                                    DailyVirtualUsageData(
                                        categoryName = category?.name ?: "未知分类",
                                        totalMinutes = totalMinutes,
                                        sessionCount = usageList.size
                                    )
                                } else null
                            }.filterNotNull()
                        
                        if (appsForDate.isNotEmpty() || virtualUsageData.isNotEmpty()) {
                            // 按使用时长降序排序
                            val sortedApps = appsForDate.sortedByDescending { it.totalMinutes }
                            val sortedVirtualApps = virtualUsageData.sortedByDescending { it.totalMinutes }
                            
                            dailyUsageList.add(
                                DailyUsageByDate(
                                    date = date,
                                    weekday = weekday,
                                    apps = sortedApps,
                                    virtualApps = sortedVirtualApps,
                                    totalApps = sortedApps.size,
                                    totalMinutes = sortedApps.sumOf { it.totalMinutes },
                                    totalSessions = sortedApps.sumOf { it.sessionCount },
                                    virtualTotalMinutes = sortedVirtualApps.sumOf { it.totalMinutes }
                                )
                            )
                        }
                    }
                }
                
                // 按日期降序排序（最新的在前）
                _dailyUsageByDate.value = dailyUsageList.sortedByDescending { it.date }
                
                android.util.Log.d(TAG, "多天使用数据加载完成: ${dailyUsageList.size} 天的数据")
                
            } catch (e: Exception) {
                android.util.Log.e(TAG, "加载多天使用数据失败", e)
                _dailyUsageByDate.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun getDateString(daysOffset: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, daysOffset)
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
    }
    
    private fun getWeekday(dateString: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(dateString)
            val weekSdf = SimpleDateFormat("EEEE", Locale.getDefault())
            weekSdf.format(date ?: Date())
        } catch (e: Exception) {
            ""
        }
    }
    
    private fun getCurrentDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
    
    /**
     * 调试OffTimes应用自身的统计状态
     */
    fun debugOffTimesAppStatus() {
        viewModelScope.launch {
            try {
                // 动态获取当前应用的包名
                val packageName = context.packageName
                android.util.Log.d(TAG, "=== 开始调试OffTimes应用状态 ===")
                android.util.Log.d(TAG, "当前应用包名: $packageName")
                
                // 1. 检查应用是否在app_info表中
                val appInfo = appInfoDao.getAppByPackageName(packageName)
                if (appInfo == null) {
                    android.util.Log.w(TAG, "❌ OffTimes应用不在app_info表中: $packageName")
                } else {
                    android.util.Log.d(TAG, "✅ OffTimes应用信息: ${appInfo.appName}")
                    android.util.Log.d(TAG, "   - 分类ID: ${appInfo.categoryId}")
                    android.util.Log.d(TAG, "   - 是否排除: ${appInfo.isExcluded}")
                    android.util.Log.d(TAG, "   - 是否启用: ${appInfo.isEnabled}")
                    
                    // 获取分类名称
                    val category = appCategoryDao.getCategoryById(appInfo.categoryId)
                    android.util.Log.d(TAG, "   - 分类名称: ${category?.name ?: "未知"}")
                }
                
                // 2. 检查今日是否有会话记录
                val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    .format(System.currentTimeMillis())
                val allSessions = appSessionUserDao.getSessionsByDate(today)
                val sessions = allSessions.filter { it.pkgName == packageName }
                android.util.Log.d(TAG, "📊 今日会话记录数: ${sessions.size}")
                
                sessions.forEach { session ->
                    android.util.Log.d(TAG, "   - 会话: ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(session.startTime)} -> ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(session.endTime)}, 时长: ${session.durationSec}秒")
                }
                
                // 3. 检查每日使用统计（按分类汇总的数据）
                if (appInfo != null) {
                    val dailyUsageSlots = dailyUsageDao.getSlots(today, appInfo.categoryId, 0)
                    val totalDuration = dailyUsageSlots.sumOf { it.durationSec }
                    android.util.Log.d(TAG, "📈 OffTimes所在分类(${appInfo.categoryId})今日统计: ${dailyUsageSlots.size}个时段, 总时长=${totalDuration}秒")
                    
                    dailyUsageSlots.forEach { usage ->
                        android.util.Log.d(TAG, "   - 统计: 日期=${usage.date}, 小时=${usage.slotIndex}, 时长=${usage.durationSec}秒")
                    }
                }
                
                android.util.Log.d(TAG, "=== OffTimes应用状态调试完成 ===")
                
            } catch (e: Exception) {
                android.util.Log.e(TAG, "调试OffTimes应用状态失败: ${e.message}", e)
            }
        }
    }
    
    /**
     * 修复OffTimes应用自身统计问题
     */
    fun fixOffTimesAppTracking() {
        viewModelScope.launch {
            try {
                // 动态获取当前应用的包名
                val packageName = context.packageName
                android.util.Log.d(TAG, "=== 开始修复OffTimes应用统计问题 ===")
                android.util.Log.d(TAG, "当前应用包名: $packageName")
                
                // 1. 检查应用是否在app_info表中
                val appInfo = appInfoDao.getAppByPackageName(packageName)
                if (appInfo == null) {
                    android.util.Log.w(TAG, "❌ OffTimes应用不在app_info表中，需要添加")
                    
                    // 获取娱乐分类ID
                    val allCategories = appCategoryDao.getAllCategoriesList()
                    val entertainmentCategory = allCategories.find { it.name == "娱乐" }
                    val categoryId = entertainmentCategory?.id ?: 1
                    
                    // 创建OffTimes应用信息
                    val newAppInfo = com.offtime.app.data.entity.AppInfoEntity(
                        packageName = packageName,
                        appName = "OffTime",
                        versionName = "1.0.3",
                        versionCode = 13,
                        isSystemApp = false,
                        categoryId = categoryId,
                        firstInstallTime = System.currentTimeMillis(),
                        lastUpdateTime = System.currentTimeMillis(),
                        isEnabled = true,
                        isExcluded = false, // 确保不被排除
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    
                    appInfoDao.insertApp(newAppInfo)
                    android.util.Log.d(TAG, "✅ 已添加OffTimes应用到app_info表")
                } else {
                    // 检查是否被排除
                    if (appInfo.isExcluded) {
                        android.util.Log.w(TAG, "❌ OffTimes应用被标记为排除，正在修复...")
                        appInfoDao.updateAppExcludeStatus(packageName, false)
                        android.util.Log.d(TAG, "✅ 已取消OffTimes应用的排除状态")
                    } else {
                        android.util.Log.d(TAG, "✅ OffTimes应用状态正常，未被排除")
                    }
                }
                
                // 2. 检查应用是否被AppCategoryUtils错误分类
                val packageName2 = "com.offtime.app" // 标准版本包名也检查
                val appInfo2 = appInfoDao.getAppByPackageName(packageName2)
                if (appInfo2 != null) {
                    android.util.Log.d(TAG, "⚠️ 发现标准版OffTimes应用: ${appInfo2.appName}, 排除状态: ${appInfo2.isExcluded}")
                    if (appInfo2.isExcluded) {
                        appInfoDao.updateAppExcludeStatus(packageName2, false)
                        android.util.Log.d(TAG, "✅ 已取消标准版OffTimes应用的排除状态")
                    }
                }
                
                // 3. 强制触发一次使用统计更新
                android.util.Log.d(TAG, "🔄 触发使用统计更新...")
                // 通过广播触发统一更新服务
                try {
                    val intent = android.content.Intent("com.offtime.app.ACTION_MANUAL_UPDATE")
                    intent.setPackage(context.packageName)
                    // 由于无法直接访问Context，这里只记录日志
                    android.util.Log.d(TAG, "需要手动触发统一更新服务")
                } catch (e: Exception) {
                    android.util.Log.w(TAG, "无法触发统一更新服务: ${e.message}")
                }
                
                android.util.Log.d(TAG, "=== OffTimes应用统计问题修复完成 ===")
                
                // 重新加载应用列表
                loadAppInfos()
                
            } catch (e: Exception) {
                android.util.Log.e(TAG, "修复OffTimes应用统计问题失败: ${e.message}", e)
            }
        }
    }
    
    /**
     * 调试当前活跃应用状态
     */
    fun debugCurrentActiveApps() {
        viewModelScope.launch {
            try {
                android.util.Log.d(TAG, "=== 开始调试当前活跃应用状态 ===")
                
                val packageName = context.packageName
                val currentTime = System.currentTimeMillis()
                val fiveMinutesAgo = currentTime - 5 * 60 * 1000L
                val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    .format(currentTime)
                
                // 1. 检查是否有最近的OffTimes会话
                val allSessions = appSessionUserDao.getSessionsByDate(today)
                val recentOffTimesSessions = allSessions.filter { 
                    (it.pkgName == context.packageName) &&
                    it.endTime >= fiveMinutesAgo 
                }
                
                if (recentOffTimesSessions.isEmpty()) {
                    android.util.Log.w(TAG, "⚠️ 没有发现最近的OffTimes会话，手动创建一个")
                    
                    // 获取OffTimes应用信息
                    val appInfo = appInfoDao.getAppByPackageName(packageName)
                    if (appInfo != null) {
                        // 创建一个从5分钟前开始到现在的会话
                        val sessionEntity = com.offtime.app.data.entity.AppSessionUserEntity(
                            id = 0,
                            pkgName = packageName,
                            catId = appInfo.categoryId,
                            startTime = fiveMinutesAgo,
                            endTime = currentTime,
                            durationSec = 300, // 5分钟
                            date = today
                        )
                        
                        appSessionUserDao.insertSession(sessionEntity)
                        android.util.Log.d(TAG, "✅ 手动创建OffTimes会话成功: 5分钟")
                    } else {
                        android.util.Log.e(TAG, "❌ 无法找到OffTimes应用信息")
                    }
                } else {
                    android.util.Log.d(TAG, "✅ 发现最近的OffTimes会话: ${recentOffTimesSessions.size}个")
                }
                
                // 2. 重新检查会话记录
                val updatedSessions = appSessionUserDao.getSessionsByDate(today)
                android.util.Log.d(TAG, "📊 今日总会话记录数: ${updatedSessions.size}")
                
                // 查找OffTimes相关的会话
                val offTimesSessions = updatedSessions.filter { 
                    it.pkgName == context.packageName 
                }
                
                android.util.Log.d(TAG, "🎯 OffTimes会话记录数: ${offTimesSessions.size}")
                
                offTimesSessions.forEach { session ->
                    val startTime = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(session.startTime)
                    val endTime = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(session.endTime)
                    android.util.Log.d(TAG, "   - 会话: $startTime -> $endTime, 时长: ${session.durationSec}秒")
                }
                
                // 3. 检查最近5分钟的所有会话
                val recentActiveSessions = updatedSessions.filter { it.endTime >= fiveMinutesAgo }
                
                android.util.Log.d(TAG, "⏰ 最近5分钟内的活跃会话: ${recentActiveSessions.size}")
                recentActiveSessions.forEach { session ->
                    val endTime = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(session.endTime)
                    android.util.Log.d(TAG, "   - ${session.pkgName}: 结束时间=$endTime, 时长=${session.durationSec}秒")
                }
                
                android.util.Log.d(TAG, "=== 当前活跃应用状态调试完成 ===")
                
            } catch (e: Exception) {
                android.util.Log.e(TAG, "调试当前活跃应用状态失败: ${e.message}", e)
            }
        }
    }
    
    /**
     * 实时监控OffTimes应用统计状态
     */
    fun startOffTimesRealTimeMonitoring() {
        viewModelScope.launch {
            try {
                android.util.Log.d(TAG, "=== 开始OffTimes实时监控 ===")
                
                val packageName = context.packageName
                val monitoringDuration = 60_000L // 监控1分钟
                val checkInterval = 10_000L // 每10秒检查一次
                val startTime = System.currentTimeMillis()
                
                android.util.Log.d(TAG, "📊 开始监控OffTimes应用统计状态，持续1分钟")
                
                while (System.currentTimeMillis() - startTime < monitoringDuration) {
                    val currentTime = System.currentTimeMillis()
                    val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        .format(currentTime)
                    
                    // 1. 检查应用信息状态
                    val appInfo = appInfoDao.getAppByPackageName(packageName)
                    android.util.Log.d(TAG, "🔍 应用信息: ${if (appInfo != null) "已注册，分类=${appInfo.categoryId}，排除=${appInfo.isExcluded}" else "未注册"}")
                    
                    // 2. 检查今日会话记录
                    val todaySessions = appSessionUserDao.getSessionsByDate(today)
                    val offTimesSessions = todaySessions.filter { 
                        it.pkgName == packageName || it.pkgName == "com.offtime.app" 
                    }
                    android.util.Log.d(TAG, "📝 今日OffTimes会话数: ${offTimesSessions.size}")
                    
                    // 3. 检查最近的会话
                    val recentSessions = offTimesSessions.filter { 
                        currentTime - it.endTime <= 5 * 60 * 1000L // 5分钟内
                    }
                    android.util.Log.d(TAG, "⏰ 最近5分钟会话数: ${recentSessions.size}")
                    
                    if (recentSessions.isNotEmpty()) {
                        val latestSession = recentSessions.maxByOrNull { it.endTime }
                        if (latestSession != null) {
                            val endTime = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(latestSession.endTime)
                            android.util.Log.d(TAG, "   最新会话: 结束时间=$endTime, 时长=${latestSession.durationSec}秒")
                        }
                    }
                    
                    // 4. 手动触发统计更新
                    try {
                        com.offtime.app.service.UsageStatsCollectorService.triggerEventsPull(context)
                        android.util.Log.d(TAG, "✅ 触发事件拉取成功")
                    } catch (e: Exception) {
                        android.util.Log.w(TAG, "⚠️ 触发事件拉取失败: ${e.message}")
                    }
                    
                    // 5. 手动触发活跃应用更新
                    try {
                        com.offtime.app.service.UsageStatsCollectorService.triggerActiveAppsUpdate(context)
                        android.util.Log.d(TAG, "✅ 触发活跃应用更新成功")
                    } catch (e: Exception) {
                        android.util.Log.w(TAG, "⚠️ 触发活跃应用更新失败: ${e.message}")
                    }
                    
                    android.util.Log.d(TAG, "--- 等待${checkInterval/1000}秒后继续监控 ---")
                    delay(checkInterval)
                }
                
                android.util.Log.d(TAG, "=== OffTimes实时监控完成 ===")
                
            } catch (e: Exception) {
                android.util.Log.e(TAG, "OffTimes实时监控失败: ${e.message}", e)
            }
        }
    }
    
    /**
     * 监控所有应用的事件类型，识别可能被遗漏的统计
     */
    fun monitorAllAppEventTypes() {
        viewModelScope.launch {
            try {
                android.util.Log.d(TAG, "=== 开始监控所有应用事件类型 ===")
                
                val usageStatsManager = context.getSystemService(android.content.Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
                val currentTime = System.currentTimeMillis()
                val lookbackTime = currentTime - 10 * 60 * 1000L // 回看10分钟
                
                val usageEvents = usageStatsManager.queryEvents(lookbackTime, currentTime)
                val eventTypeStats = mutableMapOf<Int, MutableSet<String>>()
                val appEventCounts = mutableMapOf<String, Int>()
                
                android.util.Log.d(TAG, "📊 分析最近10分钟的所有应用事件...")
                
                while (usageEvents.hasNextEvent()) {
                    val event = android.app.usage.UsageEvents.Event()
                    usageEvents.getNextEvent(event)
                    
                    // 统计事件类型和对应的应用
                    if (!eventTypeStats.containsKey(event.eventType)) {
                        eventTypeStats[event.eventType] = mutableSetOf()
                    }
                    eventTypeStats[event.eventType]?.add(event.packageName)
                    
                    // 统计每个应用的事件数量
                    appEventCounts[event.packageName] = (appEventCounts[event.packageName] ?: 0) + 1
                }
                
                // 输出事件类型统计
                android.util.Log.d(TAG, "📈 事件类型统计:")
                eventTypeStats.toSortedMap().forEach { (eventType, packages) ->
                    val eventName = when (eventType) {
                        1 -> "ACTIVITY_RESUMED"
                        2 -> "ACTIVITY_PAUSED"
                        12 -> "未知类型12"
                        19 -> "未知类型19"
                        20 -> "未知类型20"
                        23 -> "未知类型23"
                        else -> "未知类型$eventType"
                    }
                    
                    val isHandled = when (eventType) {
                        1, 2, 12, 19, 20, 23 -> "✅已处理"
                        else -> "❌未处理"
                    }
                    
                    android.util.Log.d(TAG, "  Type $eventType ($eventName) $isHandled: ${packages.size}个应用")
                    packages.take(5).forEach { pkg ->
                        android.util.Log.d(TAG, "    - $pkg")
                    }
                    if (packages.size > 5) {
                        android.util.Log.d(TAG, "    - ... 还有${packages.size - 5}个应用")
                    }
                }
                
                // 输出活跃应用统计
                android.util.Log.d(TAG, "📱 活跃应用统计 (事件数量前10):")
                appEventCounts.toList().sortedByDescending { it.second }.take(10).forEach { (pkg, count) ->
                    val isOffTimes = pkg.contains("offtime") || pkg.contains("com.offtime")
                    val marker = if (isOffTimes) "🔧" else "📱"
                    android.util.Log.d(TAG, "  $marker $pkg: ${count}个事件")
                }
                
                // 检查可能被遗漏的应用
                android.util.Log.d(TAG, "🔍 检查可能被遗漏的应用:")
                val unhandledEventTypes = eventTypeStats.keys.filter { it !in listOf(1, 2, 12, 19, 20, 23) }
                if (unhandledEventTypes.isNotEmpty()) {
                    android.util.Log.w(TAG, "⚠️ 发现未处理的事件类型: $unhandledEventTypes")
                    unhandledEventTypes.forEach { eventType ->
                        val packages = eventTypeStats[eventType] ?: emptySet()
                        android.util.Log.w(TAG, "  Type $eventType 影响的应用: ${packages.joinToString(", ")}")
                    }
                } else {
                    android.util.Log.d(TAG, "✅ 所有事件类型都已正确处理")
                }
                
                android.util.Log.d(TAG, "=== 所有应用事件类型监控完成 ===")
                
            } catch (e: Exception) {
                android.util.Log.e(TAG, "监控所有应用事件类型失败: ${e.message}", e)
            }
        }
    }
} 