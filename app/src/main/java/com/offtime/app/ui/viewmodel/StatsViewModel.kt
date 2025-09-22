package com.offtime.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.offtime.app.data.dao.AppCategoryDao
import com.offtime.app.data.dao.AppInfoDao
import com.offtime.app.data.dao.AppSessionUserDao
import com.offtime.app.data.dao.TimerSessionUserDao
import com.offtime.app.data.dao.DailyUsageDao
import com.offtime.app.data.dao.SummaryUsageDao
import com.offtime.app.data.dao.RewardPunishmentUserDao
import com.offtime.app.data.dao.GoalRewardPunishmentUserDao
import com.offtime.app.data.entity.AppCategoryEntity
import com.offtime.app.data.entity.AppInfoEntity
import java.text.SimpleDateFormat
import java.util.*

data class CategoryUsageData(
    val category: AppCategoryEntity,
    val usageMinutes: Int,
    val emoji: String,
    val color: androidx.compose.ui.graphics.Color
)

data class CategoryGoalCompletionData(
    val category: AppCategoryEntity,
    val completedGoals: Int,
    val totalGoals: Int,
    val emoji: String,
    val color: androidx.compose.ui.graphics.Color
)

data class CategoryRewardPunishmentData(
    val category: AppCategoryEntity,
    val rewardCompletionRate: Float, // 0-100
    val punishmentCompletionRate: Float, // 0-100
    val emoji: String,
    val color: androidx.compose.ui.graphics.Color
)

data class RewardPunishmentSummaryData(
    val period: String,  // "昨天", "本周", "本月", "总计"
    val categoryName: String,
    val expectedRewardCount: Int,    // 应获得奖励次数
    val rewardCount: Int,            // 奖励完成次数
    val rewardContent: String,       // 奖励内容
    val rewardTotal: String,         // 奖励总量（如"4包薯片"）
    val expectedPunishCount: Int,    // 应获得惩罚次数
    val punishCount: Int,            // 惩罚完成次数  
    val punishContent: String,       // 惩罚内容
    val punishTotal: String          // 惩罚总量（如"300个俯卧撑"）
)

data class AppUsageChangeData(
    val app: AppInfoEntity,
    val categoryName: String,
    val currentAvgDaily: Int, // 当前时段平均每日使用分钟数
    val previousAvgDaily: Int, // 对比时段平均每日使用分钟数
    val changeMinutes: Int, // 变化分钟数（正数为增加，负数为减少）
    val changePercent: Float, // 变化百分比
    val emoji: String,
    val color: androidx.compose.ui.graphics.Color
)

data class AppDailyUsageRankingData(
    val app: AppInfoEntity,
    val categoryName: String,
    val avgDailyMinutes: Int, // 平均每日使用分钟数
    val emoji: String,
    val color: androidx.compose.ui.graphics.Color
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val appCategoryDao: AppCategoryDao,
    private val appInfoDao: AppInfoDao,
    private val appSessionUserDao: AppSessionUserDao,
    private val timerSessionUserDao: TimerSessionUserDao,
    private val dailyUsageDao: DailyUsageDao,
    private val summaryUsageDao: SummaryUsageDao,
    private val rewardPunishmentUserDao: RewardPunishmentUserDao,
    private val goalRewardPunishmentUserDao: GoalRewardPunishmentUserDao,
    private val appRepository: com.offtime.app.data.repository.AppRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    companion object {
        private const val TAG = "StatsViewModel"
    }
    
    // 每个模块独立的时段选择状态 ("今日", "昨日", "本周", "本月")
    private val _usageStatsPeriod = MutableStateFlow("今日")
    val usageStatsPeriod: StateFlow<String> = _usageStatsPeriod.asStateFlow()
    
    private val _goalCompletionPeriod = MutableStateFlow("昨日")
    val goalCompletionPeriod: StateFlow<String> = _goalCompletionPeriod.asStateFlow()
    
    private val _rewardPunishmentPeriod = MutableStateFlow("昨天")
    val rewardPunishmentPeriod: StateFlow<String> = _rewardPunishmentPeriod.asStateFlow()
    
    private val _usageChangePeriod = MutableStateFlow("今日")
    val usageChangePeriod: StateFlow<String> = _usageChangePeriod.asStateFlow()
    
    private val _dailyUsageRankingPeriod = MutableStateFlow("今日")
    val dailyUsageRankingPeriod: StateFlow<String> = _dailyUsageRankingPeriod.asStateFlow()
    
    // 保留原有的selectedPeriod用于向后兼容（已弃用）
    @Deprecated("使用各模块独立的period状态")
    private val _selectedPeriod = MutableStateFlow("今日")
    @Deprecated("使用各模块独立的period状态")
    @Suppress("DEPRECATION")
    val selectedPeriod: StateFlow<String> = _selectedPeriod.asStateFlow()
    
    // 分类使用数据
    private val _categoryUsageData = MutableStateFlow<List<CategoryUsageData>>(emptyList())
    val categoryUsageData: StateFlow<List<CategoryUsageData>> = _categoryUsageData.asStateFlow()
    
    // 目标完成统计数据
    private val _categoryGoalCompletionData = MutableStateFlow<List<CategoryGoalCompletionData>>(emptyList())
    val categoryGoalCompletionData: StateFlow<List<CategoryGoalCompletionData>> = _categoryGoalCompletionData.asStateFlow()
    
    // 奖罚完成统计数据
    private val _categoryRewardPunishmentData = MutableStateFlow<List<CategoryRewardPunishmentData>>(emptyList())
    val categoryRewardPunishmentData: StateFlow<List<CategoryRewardPunishmentData>> = _categoryRewardPunishmentData.asStateFlow()
    
    // 奖励详情数据
    private val _rewardDetailsData = MutableStateFlow<List<RewardPunishmentSummaryData>>(emptyList())
    val rewardDetailsData: StateFlow<List<RewardPunishmentSummaryData>> = _rewardDetailsData.asStateFlow()

    private val _isRewardDetailsLoading = MutableStateFlow(false)
    val isRewardDetailsLoading: StateFlow<Boolean> = _isRewardDetailsLoading.asStateFlow()

    private val _rewardDetailsPeriod = MutableStateFlow("昨天")
    val rewardDetailsPeriod: StateFlow<String> = _rewardDetailsPeriod.asStateFlow()

    // 惩罚详情数据
    private val _punishmentDetailsData = MutableStateFlow<List<RewardPunishmentSummaryData>>(emptyList())
    val punishmentDetailsData: StateFlow<List<RewardPunishmentSummaryData>> = _punishmentDetailsData.asStateFlow()

    private val _isPunishmentDetailsLoading = MutableStateFlow(false)
    val isPunishmentDetailsLoading: StateFlow<Boolean> = _isPunishmentDetailsLoading.asStateFlow()

    private val _punishmentDetailsPeriod = MutableStateFlow("昨天")
    val punishmentDetailsPeriod: StateFlow<String> = _punishmentDetailsPeriod.asStateFlow()

    // 奖罚详情统计数据（保留原有逻辑以防兼容性问题）
    private val _rewardPunishmentSummaryData = MutableStateFlow<List<RewardPunishmentSummaryData>>(emptyList())
    val rewardPunishmentSummaryData: StateFlow<List<RewardPunishmentSummaryData>> = _rewardPunishmentSummaryData.asStateFlow()

    private val _isRewardPunishmentSummaryLoading = MutableStateFlow(false)
    val isRewardPunishmentSummaryLoading: StateFlow<Boolean> = _isRewardPunishmentSummaryLoading.asStateFlow()

    private val _selectedCategoryForSummary = MutableStateFlow<AppCategoryEntity?>(null)
    val selectedCategoryForSummary: StateFlow<AppCategoryEntity?> = _selectedCategoryForSummary.asStateFlow()
    
    // 使用变化排序数据
    private val _appUsageChangeData = MutableStateFlow<List<AppUsageChangeData>>(emptyList())
    val appUsageChangeData: StateFlow<List<AppUsageChangeData>> = _appUsageChangeData.asStateFlow()
    
    // 使用时长排序数据
    private val _appDailyUsageRankingData = MutableStateFlow<List<AppDailyUsageRankingData>>(emptyList())
    val appDailyUsageRankingData: StateFlow<List<AppDailyUsageRankingData>> = _appDailyUsageRankingData.asStateFlow()
    
    // 展开状态
    private val _isUsageChangeExpanded = MutableStateFlow(false)
    val isUsageChangeExpanded: StateFlow<Boolean> = _isUsageChangeExpanded.asStateFlow()
    
    private val _isDailyUsageRankingExpanded = MutableStateFlow(false)
    val isDailyUsageRankingExpanded: StateFlow<Boolean> = _isDailyUsageRankingExpanded.asStateFlow()
    
    // 每个模块独立的加载状态
    private val _isUsageStatsLoading = MutableStateFlow(false)
    val isUsageStatsLoading: StateFlow<Boolean> = _isUsageStatsLoading.asStateFlow()
    
    private val _isGoalCompletionLoading = MutableStateFlow(false)
    val isGoalCompletionLoading: StateFlow<Boolean> = _isGoalCompletionLoading.asStateFlow()
    
    private val _isRewardPunishmentLoading = MutableStateFlow(false)
    val isRewardPunishmentLoading: StateFlow<Boolean> = _isRewardPunishmentLoading.asStateFlow()
    
    private val _isUsageChangeLoading = MutableStateFlow(false)
    val isUsageChangeLoading: StateFlow<Boolean> = _isUsageChangeLoading.asStateFlow()
    
    private val _isDailyUsageRankingLoading = MutableStateFlow(false)
    val isDailyUsageRankingLoading: StateFlow<Boolean> = _isDailyUsageRankingLoading.asStateFlow()

    // 保留原有的全局loading状态用于向后兼容（已弃用）
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 所有分类数据
    private val _allCategories = MutableStateFlow<List<AppCategoryEntity>>(emptyList())
    val allCategories: StateFlow<List<AppCategoryEntity>> = _allCategories.asStateFlow()
    
    // 下拉刷新状态
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    init {
        // 使用Flow的响应式方式加载数据
        observeUsageData()

        // 其他模块的加载保持不变
        loadGoalCompletionData()
        loadRewardPunishmentData()
        loadAppUsageChangeData()
        loadAppDailyUsageRankingData()
        
        // 加载所有分类数据
        loadAllCategories()
        
        // 加载新的奖励和惩罚详情数据
        loadRewardDetails()
        loadPunishmentDetails()
        
        // 原有的初始化逻辑保持不变
        loadRewardPunishmentSummary()
    }
    
    /**
     * [新] 使用Flow的响应式方式加载和监听使用数据
     */
    private fun observeUsageData() {
        viewModelScope.launch {
            // 当period或categories变化时，重新触发数据加载
            combine(
                usageStatsPeriod,
                appCategoryDao.getAllCategories()
            ) { period, categories ->
                Pair(period, categories)
            }.flatMapLatest { (period, categories) ->
                _isUsageStatsLoading.value = true
                loadCategoryUsageDataFlow(period, categories)
            }.collect { usageDataList ->
                _categoryUsageData.value = usageDataList.sortedByDescending { it.usageMinutes }
                _isUsageStatsLoading.value = false
                android.util.Log.d(TAG, "使用数据UI已更新: ${usageDataList.size}个分类")
            }
        }
    }

    private fun loadCategoryUsageDataFlow(period: String, categories: List<AppCategoryEntity>): kotlinx.coroutines.flow.Flow<List<CategoryUsageData>> {
        // 监听整个summary表的变化
        return summaryUsageDao.getAllSummaryUsageUserFlow().map { allSummaryData ->
            android.util.Log.d(TAG, "数据库变化，重新计算使用数据: period=$period, summary records=${allSummaryData.size}")
            
            val usageDataList = mutableListOf<CategoryUsageData>()
            
            for (category in categories) {
                // 在内存中直接从 allSummaryData 计算，避免多次查询数据库
                val usageMinutes = when (period) {
                    "今日" -> calculateTodayUsage(category.id, category.name, allSummaryData, categories)
                    "昨日" -> calculateYesterdayUsage(category.id, category.name, allSummaryData, categories)
                    "本周" -> calculateThisWeekUsage(category.id, category.name, allSummaryData, categories)
                    "本月" -> calculateThisMonthUsage(category.id, category.name, allSummaryData, categories)
                    "总共" -> calculateTotalUsage(category.id, category.name, allSummaryData, categories) // 仍然需要查询原始表
                    else -> 0
                }
                
                val categoryStyle = com.offtime.app.utils.CategoryUtils.getCategoryStyle(category.name)
                
                usageDataList.add(
                    CategoryUsageData(
                        category = category,
                        usageMinutes = usageMinutes,
                        emoji = categoryStyle.emoji,
                        color = categoryStyle.color
                    )
                )
            }
            usageDataList
        }
    }
    
    // 以下是将原有的挂起函数改造为在内存中计算的普通函数
    
    private fun calculateTodayUsage(categoryId: Int, categoryName: String, allSummaryData: List<SummaryUsageUserEntity>, allCategories: List<AppCategoryEntity>): Int {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        return calculateUsageForDate(categoryId, categoryName, today, allSummaryData, allCategories)
    }

    private fun calculateYesterdayUsage(categoryId: Int, categoryName: String, allSummaryData: List<SummaryUsageUserEntity>, allCategories: List<AppCategoryEntity>): Int {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        return calculateUsageForDate(categoryId, categoryName, yesterday, allSummaryData, allCategories)
    }
    
    private fun calculateUsageForDate(categoryId: Int, categoryName: String, date: String, allSummaryData: List<SummaryUsageUserEntity>, allCategories: List<AppCategoryEntity>): Int {
        return if (categoryName == "总使用") {
            val validCategories = allCategories.filter { it.id != categoryId && it.name != "总使用" && !it.name.contains("排除统计") }
            var totalSeconds = 0L
            validCategories.forEach { category ->
                val summaryRecord = allSummaryData.find { it.date == date && it.catId == category.id }
                totalSeconds += summaryRecord?.totalSec?.toLong() ?: 0L
            }
            (totalSeconds / 60).toInt()
        } else {
            val summaryRecord = allSummaryData.find { it.date == date && it.catId == categoryId }
            summaryRecord?.totalSec?.div(60) ?: 0
        }
    }

    private fun calculateThisWeekUsage(categoryId: Int, categoryName: String, allSummaryData: List<SummaryUsageUserEntity>, allCategories: List<AppCategoryEntity>): Int {
        val calendar = Calendar.getInstance()
        var totalSeconds = 0L
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysToCalculate = if (currentDayOfWeek == Calendar.SUNDAY) 7 else currentDayOfWeek - 1
        
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        
        val validCategories = if (categoryName == "总使用") {
            allCategories.filter { it.id != categoryId && it.name != "总使用" && !it.name.contains("排除统计") }
        } else {
            null
        }

        for (i in 0 until daysToCalculate) {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            if (validCategories != null) {
                validCategories.forEach { category ->
                    val summaryRecord = allSummaryData.find { it.date == date && it.catId == category.id }
                    totalSeconds += summaryRecord?.totalSec?.toLong() ?: 0L
                }
            } else {
                val summaryRecord = allSummaryData.find { it.date == date && it.catId == categoryId }
                totalSeconds += summaryRecord?.totalSec?.toLong() ?: 0L
            }
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return (totalSeconds / 60).toInt()
    }

    private fun calculateThisMonthUsage(categoryId: Int, categoryName: String, allSummaryData: List<SummaryUsageUserEntity>, allCategories: List<AppCategoryEntity>): Int {
        val calendar = Calendar.getInstance()
        var totalSeconds = 0L
        val daysInMonth = calendar.get(Calendar.DAY_OF_MONTH)
        calendar.set(Calendar.DAY_OF_MONTH, 1)

        val validCategories = if (categoryName == "总使用") {
            allCategories.filter { it.id != categoryId && it.name != "总使用" && !it.name.contains("排除统计") }
        } else {
            null
        }

        for (i in 0 until daysInMonth) {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            if (validCategories != null) {
                validCategories.forEach { category ->
                    val summaryRecord = allSummaryData.find { it.date == date && it.catId == category.id }
                    totalSeconds += summaryRecord?.totalSec?.toLong() ?: 0L
                }
            } else {
                val summaryRecord = allSummaryData.find { it.date == date && it.catId == categoryId }
                totalSeconds += summaryRecord?.totalSec?.toLong() ?: 0L
            }
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return (totalSeconds / 60).toInt()
    }
    
    private suspend fun calculateTotalUsage(categoryId: Int, categoryName: String, allSummaryData: List<SummaryUsageUserEntity>, allCategories: List<AppCategoryEntity>): Int {
         // 总使用量仍然需要直接查询原始表以确保准确性
        return getTotalUsageOptimized(categoryId, categoryName)
    }
    
    /**
     * 加载所有分类数据
     */
    private fun loadAllCategories() {
        viewModelScope.launch {
            try {
                val categories = appCategoryDao.getAllCategoriesList()
                _allCategories.value = categories
            } catch (e: Exception) {
                android.util.Log.e("StatsViewModel", "加载分类数据失败", e)
                _allCategories.value = emptyList()
            }
        }
    }
    
    // 每个模块独立的时间段设置方法
    fun setUsageStatsPeriod(period: String) {
        _usageStatsPeriod.value = period
        loadUsageData()
    }
    
    fun setGoalCompletionPeriod(period: String) {
        _goalCompletionPeriod.value = period
        loadGoalCompletionData()
    }
    
    fun setRewardPunishmentPeriod(period: String) {
        _rewardPunishmentPeriod.value = period
        loadRewardPunishmentData()
    }
    
    fun setUsageChangePeriod(period: String) {
        _usageChangePeriod.value = period
        loadAppUsageChangeData()
    }
    
    fun setDailyUsageRankingPeriod(period: String) {
        _dailyUsageRankingPeriod.value = period
        loadAppDailyUsageRankingData()
    }
    
    fun setSelectedCategoryForSummary(category: AppCategoryEntity?) {
        _selectedCategoryForSummary.value = category
        loadRewardPunishmentSummary()
    }
    
    // 保留原有方法用于向后兼容（已弃用）
    @Deprecated("使用各模块独立的setPeriod方法")
    @Suppress("DEPRECATION")
    fun setPeriod(period: String) {
        _selectedPeriod.value = period
        loadAllData()
    }
    
    fun toggleUsageChangeExpanded() {
        _isUsageChangeExpanded.value = !_isUsageChangeExpanded.value
    }
    
    fun toggleDailyUsageRankingExpanded() {
        _isDailyUsageRankingExpanded.value = !_isDailyUsageRankingExpanded.value
    }
    
    private fun loadAllData() {
        loadUsageData()
        loadGoalCompletionData()
        loadRewardPunishmentData()
        loadAppUsageChangeData()
        loadAppDailyUsageRankingData()
        loadRewardPunishmentSummary()
    }
    
    /**
     * 下拉刷新处理方法
     * 执行完整的数据更新流程：基础数据 → 聚合数据 → UI更新
     */
    fun onSwipeRefresh() {
        viewModelScope.launch {
            try {
                android.util.Log.d(TAG, "🔄 开始统计页下拉刷新")
                _isRefreshing.value = true
                
                // 触发统一更新服务进行完整的数据更新
                com.offtime.app.service.UnifiedUpdateService.triggerManualUpdate(
                    context
                )
                
                // 等待数据更新完成（包含基础数据→聚合数据→UI的完整流程）
                kotlinx.coroutines.delay(3000)
                
                // 刷新所有统计数据
                // observeUsageData会自动处理，无需手动调用
                // loadUsageData() 
                loadGoalCompletionData()
                loadRewardPunishmentData()
                loadAppUsageChangeData()
                loadAppDailyUsageRankingData()
                loadRewardDetails()
                loadPunishmentDetails()
                loadRewardPunishmentSummary()
                loadAllCategories()
                
                android.util.Log.d(TAG, "✅ 统计页下拉刷新完成")
                
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ 统计页下拉刷新失败", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }
    
    private fun loadUsageData() {
        viewModelScope.launch {
            _isUsageStatsLoading.value = true
            try {
                val allCategories = appCategoryDao.getAllCategoriesList()
                // 获取所有分类（排除统计已从数据库中删除）
                val categories = allCategories
                val period = _usageStatsPeriod.value
                
                android.util.Log.d(TAG, "开始加载使用数据: period=$period, 分类数=${categories.size}")
                
                val usageDataList = mutableListOf<CategoryUsageData>()
                
                for (category in categories) {
                    val usageMinutes = when (period) {
                        "今日" -> getTodayUsageOptimized(category.id, category.name)
                        "昨日" -> getYesterdayUsageOptimized(category.id, category.name)
                        "本周" -> getThisWeekUsageOptimized(category.id, category.name)
                        "本月" -> getThisMonthUsageOptimized(category.id, category.name)
                        "总共" -> getTotalUsageOptimized(category.id, category.name)
                        else -> 0
                    }
                    
                    val categoryStyle = com.offtime.app.utils.CategoryUtils.getCategoryStyle(category.name)
                    
                    usageDataList.add(
                        CategoryUsageData(
                            category = category,
                            usageMinutes = usageMinutes,
                            emoji = categoryStyle.emoji,
                            color = categoryStyle.color
                        )
                    )
                }
                
                // 按使用时间降序排序
                _categoryUsageData.value = usageDataList.sortedByDescending { it.usageMinutes }
                
                android.util.Log.d(TAG, "使用数据加载完成: ${usageDataList.size}个分类")
                
            } catch (e: Exception) {
                android.util.Log.e("StatsViewModel", "加载使用数据失败", e)
                _categoryUsageData.value = emptyList()
            } finally {
                _isUsageStatsLoading.value = false
            }
        }
    }
    
    // 优化后的方法，避免重复查询分类列表
    private suspend fun getTodayUsageOptimized(categoryId: Int, categoryName: String): Int {
        return try {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            
            if (categoryName == "总使用") {
                // 总使用分类：获取所有其他分类的汇总
                val categories = appCategoryDao.getAllCategoriesList()
                var totalSeconds = 0L
                android.util.Log.d(TAG, "今日总使用计算: 分类数=${categories.size}")
                val validCategories = categories.filter { 
                    it.id != categoryId && 
                    it.name != "总使用" && 
                    !it.name.contains("总使用") &&
                    !it.name.contains("排除统计")
                }
                android.util.Log.d(TAG, "今日有效分类: ${validCategories.map { "${it.name}(${it.id})" }}")
                validCategories.forEach { category ->
                    val summaryRecord = summaryUsageDao.getSummaryUsageById("${today}_${category.id}")
                    val dailySeconds = summaryRecord?.totalSec?.toLong() ?: 0L
                    totalSeconds += dailySeconds
                    if (dailySeconds > 0) {
                        android.util.Log.d(TAG, "今日总使用: 分类=${category.name}, 秒数=$dailySeconds")
                }
                }
                val totalMinutes = maxOf((totalSeconds / 60).toInt(), 0)
                android.util.Log.d(TAG, "今日总使用计算完成: 总秒数=$totalSeconds, 总分钟数=$totalMinutes")
                totalMinutes
            } else {
                // 普通分类：从summary表直接获取
                val summaryRecord = summaryUsageDao.getSummaryUsageById("${today}_${categoryId}")
                maxOf(summaryRecord?.totalSec?.div(60) ?: 0, 0)
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "获取今日使用数据失败: categoryId=$categoryId", e)
            0
        }
    }
    
    private suspend fun getYesterdayUsageOptimized(categoryId: Int, categoryName: String): Int {
        return try {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            
            if (categoryName == "总使用") {
                // 总使用分类：获取所有其他分类的汇总
                val categories = appCategoryDao.getAllCategoriesList()
                var totalSeconds = 0L
                android.util.Log.d(TAG, "昨日总使用计算: 分类数=${categories.size}")
                val validCategories = categories.filter { 
                    it.id != categoryId && 
                    it.name != "总使用" && 
                    !it.name.contains("总使用") &&
                    !it.name.contains("排除统计")
                }
                android.util.Log.d(TAG, "昨日有效分类: ${validCategories.map { "${it.name}(${it.id})" }}")
                validCategories.forEach { category ->
                    val summaryRecord = summaryUsageDao.getSummaryUsageById("${yesterday}_${category.id}")
                    val dailySeconds = summaryRecord?.totalSec?.toLong() ?: 0L
                    totalSeconds += dailySeconds
                    if (dailySeconds > 0) {
                        android.util.Log.d(TAG, "昨日总使用: 分类=${category.name}, 秒数=$dailySeconds")
                }
                }
                val totalMinutes = maxOf((totalSeconds / 60).toInt(), 0)
                android.util.Log.d(TAG, "昨日总使用计算完成: 总秒数=$totalSeconds, 总分钟数=$totalMinutes")
                totalMinutes
            } else {
                // 普通分类：从summary表直接获取
                val summaryRecord = summaryUsageDao.getSummaryUsageById("${yesterday}_${categoryId}")
                maxOf(summaryRecord?.totalSec?.div(60) ?: 0, 0)
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "获取昨日使用数据失败: categoryId=$categoryId", e)
            0
        }
    }
    
    private suspend fun getThisWeekUsageOptimized(categoryId: Int, categoryName: String): Int {
        return try {
            val calendar = Calendar.getInstance()
            
            if (categoryName == "总使用") {
                // 总使用分类：直接使用实时计算，避免平均值计算错误
                android.util.Log.d(TAG, "本周总使用使用实时计算，确保只统计有数据的天数")
                var totalSeconds = 0L
                val currentDayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
                val daysToCalculate = if (currentDayOfWeek == Calendar.SUNDAY) 7 else currentDayOfWeek - 1
                
                val categories = appCategoryDao.getAllCategoriesList()
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                
                val validCategories = categories.filter { 
                    it.id != categoryId && 
                    it.name != "总使用" && 
                    !it.name.contains("总使用") &&
                    !it.name.contains("排除统计")
                }
                android.util.Log.d(TAG, "本周有效分类: ${validCategories.map { "${it.name}(${it.id})" }}")
                
                for (i in 0 until daysToCalculate) {
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                    validCategories.forEach { category ->
                        val summaryRecord = summaryUsageDao.getSummaryUsageById("${date}_${category.id}")
                        val dailySeconds = summaryRecord?.totalSec?.toLong() ?: 0L
                        totalSeconds += dailySeconds
                        if (dailySeconds > 0) {
                            android.util.Log.d(TAG, "本周总使用: 日期=$date, 分类=${category.name}, 秒数=$dailySeconds")
                        }
                    }
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
                val result = maxOf((totalSeconds / 60).toInt(), 0)
                android.util.Log.d(TAG, "本周总使用实时计算完成: 总秒数=$totalSeconds, 总分钟数=$result")
                result
            } else {
                // 普通分类：直接累加每天的实际使用数据，避免平均值计算错误
                var totalSeconds = 0L
                val currentDayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
                val daysToCalculate = if (currentDayOfWeek == Calendar.SUNDAY) 7 else currentDayOfWeek - 1
                
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                for (i in 0 until daysToCalculate) {
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                    val summaryRecord = summaryUsageDao.getSummaryUsageById("${date}_${categoryId}")
                    totalSeconds += summaryRecord?.totalSec?.toLong() ?: 0L
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
                maxOf((totalSeconds / 60).toInt(), 0)
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "获取本周使用数据失败: categoryId=$categoryId", e)
            0
        }
    }
    
    private suspend fun getThisMonthUsageOptimized(categoryId: Int, categoryName: String): Int {
        return try {
            val calendar = Calendar.getInstance()
            
            if (categoryName == "总使用") {
                // 总使用分类：直接使用实时计算，确保数据准确性
                android.util.Log.d(TAG, "本月总使用使用实时计算，确保只统计有数据的天数")
                var totalSeconds = 0L
                        val daysInMonth = calendar.get(Calendar.DAY_OF_MONTH)
                val categories = appCategoryDao.getAllCategoriesList()
                
                android.util.Log.d(TAG, "本月总使用计算: 天数=$daysInMonth, 分类数=${categories.size}")
                
                val validCategories = categories.filter { 
                    it.id != categoryId && 
                    it.name != "总使用" && 
                    !it.name.contains("总使用") &&
                    !it.name.contains("排除统计")
                }
                android.util.Log.d(TAG, "有效分类: ${validCategories.map { "${it.name}(${it.id})" }}")
                
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                
                for (i in 0 until daysInMonth) {
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                    validCategories.forEach { category ->
                        val summaryRecord = summaryUsageDao.getSummaryUsageById("${date}_${category.id}")
                        val dailySeconds = summaryRecord?.totalSec?.toLong() ?: 0L
                        totalSeconds += dailySeconds
                        if (dailySeconds > 0) {
                            android.util.Log.d(TAG, "本月总使用: 日期=$date, 分类=${category.name}, 秒数=$dailySeconds")
                        }
                    }
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
                
                val totalMinutes = maxOf((totalSeconds / 60).toInt(), 0)
                android.util.Log.d(TAG, "本月总使用实时计算完成: 总秒数=$totalSeconds, 总分钟数=$totalMinutes")
                totalMinutes
            } else {
                // 普通分类：直接累加每天的实际使用数据，避免平均值计算错误
                var totalSeconds = 0L
                val daysInMonth = calendar.get(Calendar.DAY_OF_MONTH)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                
                for (i in 0 until daysInMonth) {
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                    val summaryRecord = summaryUsageDao.getSummaryUsageById("${date}_${categoryId}")
                    totalSeconds += summaryRecord?.totalSec?.toLong() ?: 0L
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
                maxOf((totalSeconds / 60).toInt(), 0)
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "获取本月使用数据失败: categoryId=$categoryId", e)
            0
        }
    }
    
    private suspend fun getTotalUsageOptimized(categoryId: Int, categoryName: String): Int {
        return try {
            if (categoryName == "总使用") {
                // 总使用分类：获取所有其他分类的历史汇总
                val categories = appCategoryDao.getAllCategoriesList()
                val allSessions = appSessionUserDao.getAllSessions()
                var totalSeconds = 0L // 使用Long避免溢出
                android.util.Log.d(TAG, "总共总使用计算: 分类数=${categories.size}")
                val validCategories = categories.filter { 
                    it.id != categoryId && 
                    it.name != "总使用" && 
                    !it.name.contains("总使用") &&
                    !it.name.contains("排除统计")
                }
                android.util.Log.d(TAG, "总共有效分类: ${validCategories.map { "${it.name}(${it.id})" }}")
                validCategories.forEach { category ->
                    val categorySessions = allSessions.filter { it.catId == category.id }
                    val categorySeconds = categorySessions.sumOf { it.durationSec.toLong() }
                    totalSeconds += categorySeconds
                    if (categorySeconds > 0) {
                        android.util.Log.d(TAG, "总共总使用: 分类=${category.name}, 秒数=$categorySeconds")
                    }
                }
                val totalMinutes = (totalSeconds / 60).toInt()
                // 确保结果为非负数
                val result = maxOf(totalMinutes, 0)
                android.util.Log.d(TAG, "总共总使用计算完成: 总秒数=$totalSeconds, 总分钟数=$result")
                result
            } else {
                // 普通分类：获取该分类的所有历史会话
                val allSessions = appSessionUserDao.getAllSessions()
                val categorySessions = allSessions.filter { it.catId == categoryId }
                val totalSeconds = categorySessions.sumOf { it.durationSec.toLong() }
                val totalMinutes = (totalSeconds / 60).toInt()
                // 确保结果为非负数
                maxOf(totalMinutes, 0)
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "获取总使用数据失败: categoryId=$categoryId", e)
            0
        }
    }
    
    private fun loadGoalCompletionData() {
        viewModelScope.launch {
            _isGoalCompletionLoading.value = true
            try {
                val allCategories = appCategoryDao.getAllCategoriesList()
                // 获取所有分类（排除统计已从数据库中删除）
                val categories = allCategories
                val period = _goalCompletionPeriod.value
                val goalCompletionDataList = mutableListOf<CategoryGoalCompletionData>()
                
                for (category in categories) {
                    val (completedGoals, totalGoals) = when (period) {
                        "今日" -> getTodayGoalCompletion(category.id)
                        "昨日" -> getYesterdayGoalCompletion(category.id)
                        "本周" -> getThisWeekGoalCompletion(category.id)
                        "本月" -> getThisMonthGoalCompletion(category.id)
                        "总共" -> getTotalGoalCompletion(category.id)
                        else -> Pair(0, 0)
                    }
                    
                    val categoryStyle = com.offtime.app.utils.CategoryUtils.getCategoryStyle(category.name)
                    
                    goalCompletionDataList.add(
                        CategoryGoalCompletionData(
                            category = category,
                            completedGoals = completedGoals,
                            totalGoals = totalGoals,
                            emoji = categoryStyle.emoji,
                            color = categoryStyle.color
                        )
                    )
                }
                
                _categoryGoalCompletionData.value = goalCompletionDataList
                
            } catch (e: Exception) {
                android.util.Log.e("StatsViewModel", "加载目标完成数据失败", e)
                _categoryGoalCompletionData.value = emptyList()
            } finally {
                _isGoalCompletionLoading.value = false
            }
        }
    }
    
    private fun loadRewardPunishmentData() {
        viewModelScope.launch {
            _isRewardPunishmentLoading.value = true
            try {
                val allCategories = appCategoryDao.getAllCategoriesList()
                // 获取所有分类（排除统计已从数据库中删除）
                val categories = allCategories
                val period = _rewardPunishmentPeriod.value
                val rewardPunishmentDataList = mutableListOf<CategoryRewardPunishmentData>()
                
                // 获取奖罚开关状态
                val enabledMap = appRepository.getCategoryRewardPunishmentEnabled()
                
                for (category in categories) {
                    // 检查该分类的奖罚开关是否开启
                    val isEnabled = enabledMap[category.id] ?: true // 默认开启
                    
                    // 检查奖励和惩罚内容是否为空
                    val goal = goalRewardPunishmentUserDao.getUserGoalByCatId(category.id)
                    val isRewardEmpty = goal?.rewardText.isNullOrBlank()
                    val isPunishmentEmpty = goal?.punishText.isNullOrBlank()
                    
                    val (rewardRate, punishmentRate) = if (isEnabled) {
                        // 开关开启时，检查内容是否为空
                        val (baseRewardRate, basePunishmentRate) = when (period) {
                            "今日" -> getTodayRewardPunishmentCompletion(category.id)
                            "昨日" -> getYesterdayRewardPunishmentCompletion(category.id)
                            "本周" -> getThisWeekRewardPunishmentCompletion(category.id)
                            "本月" -> getThisMonthRewardPunishmentCompletion(category.id)
                            else -> Pair(0f, 0f)
                        }
                        
                        // 如果内容为空，使用-2表示"NA"，-1表示开关关闭
                        val finalRewardRate = if (isRewardEmpty) -2f else baseRewardRate
                        val finalPunishmentRate = if (isPunishmentEmpty) -2f else basePunishmentRate
                        
                        Pair(finalRewardRate, finalPunishmentRate)
                    } else {
                        // 开关关闭时，返回-1表示不显示
                        Pair(-1f, -1f)
                    }
                    
                    val categoryStyle = com.offtime.app.utils.CategoryUtils.getCategoryStyle(category.name)
                    
                    rewardPunishmentDataList.add(
                        CategoryRewardPunishmentData(
                            category = category,
                            rewardCompletionRate = rewardRate,
                            punishmentCompletionRate = punishmentRate,
                            emoji = categoryStyle.emoji,
                            color = categoryStyle.color
                        )
                    )
                }
                
                _categoryRewardPunishmentData.value = rewardPunishmentDataList
                
            } catch (e: Exception) {
                android.util.Log.e("StatsViewModel", "加载奖罚完成数据失败", e)
                _categoryRewardPunishmentData.value = emptyList()
            } finally {
                _isRewardPunishmentLoading.value = false
            }
        }
    }
    
    // 目标完成统计相关方法
    private suspend fun getTodayGoalCompletion(categoryId: Int): Pair<Int, Int> {
        return try {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            
            // 查询reward_punishment_user表
            val rewardPunishmentRecord = rewardPunishmentUserDao.getRecordByCategoryAndDate(categoryId, today)
            if (rewardPunishmentRecord != null) {
                val completed = if (rewardPunishmentRecord.isGoalMet == 1) 1 else 0
                Pair(completed, 1)
            } else {
                Pair(0, 1)
            }
        } catch (e: Exception) {
            Pair(0, 1)
        }
    }
    
    private suspend fun getYesterdayGoalCompletion(categoryId: Int): Pair<Int, Int> {
        return try {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            
            // 查询reward_punishment_user表
            val rewardPunishmentRecord = rewardPunishmentUserDao.getRecordByCategoryAndDate(categoryId, yesterday)
            if (rewardPunishmentRecord != null) {
                val completed = if (rewardPunishmentRecord.isGoalMet == 1) 1 else 0
                Pair(completed, 1)
            } else {
                Pair(0, 1)
            }
        } catch (e: Exception) {
            Pair(0, 1)
        }
    }
    
    private suspend fun getThisWeekGoalCompletion(categoryId: Int): Pair<Int, Int> {
        return try {
            val calendar = Calendar.getInstance()
            var completed = 0
            // 计算本周已过去的天数（不包括今天）
            val currentDayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
            val daysPassedThisWeek = if (currentDayOfWeek == Calendar.SUNDAY) {
                6 // 周日算作第7天，已过去6天
            } else {
                currentDayOfWeek - 2 // 周一是第2天，已过去 currentDayOfWeek - 2 天
            }
            
            // 如果是周一（已过去天数为0），总目标数为0，但为了避免除零，设为1
            val total = maxOf(daysPassedThisWeek, 0)
            
            if (total > 0) {
                // 计算本周（从周一开始）已过去的天数
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                
                for (i in 0 until total) {
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                
                // 查询reward_punishment_user表
                val rewardPunishmentRecord = rewardPunishmentUserDao.getRecordByCategoryAndDate(categoryId, date)
                if (rewardPunishmentRecord != null && rewardPunishmentRecord.isGoalMet == 1) {
                    completed++
                }
                
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            
            // 如果是周一，返回特殊值表示没有过去的天数
            if (total == 0) {
                Pair(0, 0)
            } else {
            Pair(completed, total)
            }
        } catch (e: Exception) {
            Pair(0, 1)
        }
    }
    
    private suspend fun getThisMonthGoalCompletion(categoryId: Int): Pair<Int, Int> {
        return try {
            val calendar = Calendar.getInstance()
            var completed = 0
            
            // 计算本月已过去的天数（不包括今天）
            val currentDayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
            val daysPassedThisMonth = currentDayOfMonth - 1 // 不包括今天
            
            val total = maxOf(daysPassedThisMonth, 0)
            
            if (total > 0) {
                // 设置到月初
                calendar.set(Calendar.DAY_OF_MONTH, 1)
            
                for (i in 0 until total) {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                
                // 查询reward_punishment_user表
                val rewardPunishmentRecord = rewardPunishmentUserDao.getRecordByCategoryAndDate(categoryId, date)
                if (rewardPunishmentRecord != null && rewardPunishmentRecord.isGoalMet == 1) {
                    completed++
                }
                
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            
            // 如果是1号，返回特殊值表示没有过去的天数
            if (total == 0) {
                Pair(0, 0)
            } else {
            Pair(completed, total)
            }
        } catch (e: Exception) {
            Pair(0, 1)
        }
    }

    private suspend fun getTotalGoalCompletion(categoryId: Int): Pair<Int, Int> {
        return try {
            // 获取所有奖罚记录
            val allRecords = rewardPunishmentUserDao.getAllRecords()
            val categoryRecords = allRecords.filter { it.catId == categoryId }
            
            val totalCompleted = categoryRecords.count { it.isGoalMet == 1 }
            val totalDays = categoryRecords.size
            
            Pair(totalCompleted, totalDays)
        } catch (e: Exception) {
            Pair(0, 0)
        }
    }
    
    // 奖罚完成统计相关方法
    private suspend fun getTodayRewardPunishmentCompletion(categoryId: Int): Pair<Float, Float> {
        return try {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            
            // 查询reward_punishment_user表
            val rewardPunishmentRecord = rewardPunishmentUserDao.getRecordByCategoryAndDate(categoryId, today)
            if (rewardPunishmentRecord != null) {
                val rewardRate = if (rewardPunishmentRecord.rewardDone == 1) 100f else 0f
                val punishmentRate = if (rewardPunishmentRecord.punishDone == 1) 100f else 0f
                Pair(rewardRate, punishmentRate)
            } else {
                Pair(0f, 0f)
            }
        } catch (e: Exception) {
            Pair(0f, 0f)
        }
    }
    
    private suspend fun getYesterdayRewardPunishmentCompletion(categoryId: Int): Pair<Float, Float> {
        return try {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            
            // 查询reward_punishment_user表
            val rewardPunishmentRecord = rewardPunishmentUserDao.getRecordByCategoryAndDate(categoryId, yesterday)
            if (rewardPunishmentRecord != null) {
                val rewardRate = if (rewardPunishmentRecord.rewardDone == 1) 100f else 0f
                val punishmentRate = if (rewardPunishmentRecord.punishDone == 1) 100f else 0f
                Pair(rewardRate, punishmentRate)
            } else {
                Pair(0f, 0f)
            }
        } catch (e: Exception) {
            Pair(0f, 0f)
        }
    }
    
    private suspend fun getThisWeekRewardPunishmentCompletion(categoryId: Int): Pair<Float, Float> {
        return try {
            val calendar = Calendar.getInstance()
            var rewardCompleted = 0
            var punishmentCompleted = 0
            var total = 0
            
            // 计算本周（从周一开始）
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            val startOfWeek = calendar.clone() as Calendar
            
            for (i in 0..6) { // 一周7天
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(startOfWeek.time)
                
                total++
                
                // 查询reward_punishment_user表
                val rewardPunishmentRecord = rewardPunishmentUserDao.getRecordByCategoryAndDate(categoryId, date)
                if (rewardPunishmentRecord != null) {
                    if (rewardPunishmentRecord.rewardDone == 1) rewardCompleted++
                    if (rewardPunishmentRecord.punishDone == 1) punishmentCompleted++
                }
                
                startOfWeek.add(Calendar.DAY_OF_YEAR, 1)
            }
            
            val rewardRate = if (total > 0) (rewardCompleted * 100f) / total else 0f
            val punishmentRate = if (total > 0) (punishmentCompleted * 100f) / total else 0f
            
            Pair(rewardRate, punishmentRate)
        } catch (e: Exception) {
            Pair(0f, 0f)
        }
    }
    
    private suspend fun getThisMonthRewardPunishmentCompletion(categoryId: Int): Pair<Float, Float> {
        return try {
            val calendar = Calendar.getInstance()
            var rewardCompleted = 0
            var punishmentCompleted = 0
            var total = 0
            
            val daysInMonth = calendar.get(Calendar.DAY_OF_MONTH)
            calendar.set(Calendar.DAY_OF_MONTH, 1) // 设置到月初
            
            for (i in 0 until daysInMonth) {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                
                total++
                
                // 查询reward_punishment_user表
                val rewardPunishmentRecord = rewardPunishmentUserDao.getRecordByCategoryAndDate(categoryId, date)
                if (rewardPunishmentRecord != null) {
                    if (rewardPunishmentRecord.rewardDone == 1) rewardCompleted++
                    if (rewardPunishmentRecord.punishDone == 1) punishmentCompleted++
                }
                
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            
            val rewardRate = if (total > 0) (rewardCompleted * 100f) / total else 0f
            val punishmentRate = if (total > 0) (punishmentCompleted * 100f) / total else 0f
            
            Pair(rewardRate, punishmentRate)
        } catch (e: Exception) {
            Pair(0f, 0f)
        }
    }
    
    private fun loadAppUsageChangeData() {
        viewModelScope.launch {
            _isUsageChangeLoading.value = true
            try {
                val period = _usageChangePeriod.value
                android.util.Log.d(TAG, "开始加载APP使用变化数据: period=$period")
                
                val appUsageChangeDataList = mutableListOf<AppUsageChangeData>()
                
                // 获取所有应用和分类
                val allApps = appInfoDao.getAllAppsList()
                val categories = appCategoryDao.getAllCategoriesList()
                val categoryMap = categories.associateBy { it.id }
                
                // 批量获取当前和对比时段的使用数据
                val (currentUsageData, previousUsageData) = when (period) {
                    "昨日" -> {
                        val yesterday = getDateString(-1)
                        val dayBeforeYesterday = getDateString(-2)
                        Pair(
                            getBatchAppUsageByDate(yesterday),
                            getBatchAppUsageByDate(dayBeforeYesterday)
                        )
                    }
                    "本周" -> {
                        Pair(
                            getBatchAppUsageThisWeek(),
                            getBatchAppUsageLastWeek()
                        )
                    }
                    "本月" -> {
                        Pair(
                            getBatchAppUsageThisMonth(),
                            getBatchAppUsageLastMonth()
                        )
                    }
                    else -> { // "今日"
                        val today = getDateString(0)
                        val yesterday = getDateString(-1)
                        Pair(
                            getBatchAppUsageByDate(today),
                            getBatchAppUsageByDate(yesterday)
                        )
                    }
                }
                
                // 1. 处理真实APP
                for (app in allApps) {
                    val category = categoryMap[app.categoryId] ?: continue
                    
                    // 跳过已排除的应用
                    if (app.isExcluded) {
                        continue
                    }
                    
                    val currentUsage = currentUsageData[app.packageName] ?: 0
                    val previousUsage = previousUsageData[app.packageName] ?: 0
                    
                    val currentAvgDaily = when (period) {
                        "昨日" -> currentUsage
                        "本周" -> currentUsage / 7
                        "本月" -> {
                            val calendar = Calendar.getInstance()
                            val daysInMonth = calendar.get(Calendar.DAY_OF_MONTH)
                            if (daysInMonth > 0) currentUsage / daysInMonth else 0
                        }
                        else -> currentUsage
                    }
                    
                    val previousAvgDaily = when (period) {
                        "昨日" -> previousUsage
                        "本周" -> previousUsage / 7
                        "本月" -> previousUsage / 30 // 假设上月30天
                        else -> previousUsage
                    }
                    
                    val changeMinutes = currentAvgDaily - previousAvgDaily
                    val changePercent = if (previousAvgDaily > 0) {
                        (changeMinutes.toFloat() / previousAvgDaily.toFloat()) * 100f
                    } else if (currentAvgDaily > 0) {
                        100f // 从0增长到非0就是100%增长
                    } else {
                        0f
                    }
                    
                    // 只包含有变化的APP
                    if (changeMinutes != 0) {
                        val categoryStyle = com.offtime.app.utils.CategoryUtils.getCategoryStyle(category.name)
                        
                        appUsageChangeDataList.add(
                            AppUsageChangeData(
                                app = app,
                                categoryName = category.name,
                                currentAvgDaily = currentAvgDaily,
                                previousAvgDaily = previousAvgDaily,
                                changeMinutes = changeMinutes,
                                changePercent = changePercent,
                                emoji = categoryStyle.emoji,
                                color = categoryStyle.color
                            )
                        )
                    }
                }
                
                // 2. 处理线下虚拟APP（没有对应真实APP记录的程序）
                val existingPackageNames = allApps.map { it.packageName }.toSet()
                
                // 获取所有程序名（包含当前和对比时段的数据）
                val allProgramNames = (currentUsageData.keys + previousUsageData.keys).distinct()
                
                allProgramNames.forEach { programName ->
                    // 如果这个程序名不在真实APP列表中，说明是线下虚拟APP
                    if (!existingPackageNames.contains(programName)) {
                        val currentUsage = currentUsageData[programName] ?: 0
                        val previousUsage = previousUsageData[programName] ?: 0
                        
                        val currentAvgDaily = when (period) {
                            "昨日" -> currentUsage
                            "本周" -> currentUsage / 7
                            "本月" -> {
                                val calendar = Calendar.getInstance()
                                val daysInMonth = calendar.get(Calendar.DAY_OF_MONTH)
                                if (daysInMonth > 0) currentUsage / daysInMonth else 0
                            }
                            else -> currentUsage
                        }
                        
                        val previousAvgDaily = when (period) {
                            "昨日" -> previousUsage
                            "本周" -> previousUsage / 7
                            "本月" -> previousUsage / 30 // 假设上月30天
                            else -> previousUsage
                        }
                        
                        val changeMinutes = currentAvgDaily - previousAvgDaily
                        val changePercent = if (previousAvgDaily > 0) {
                            (changeMinutes.toFloat() / previousAvgDaily.toFloat()) * 100f
                        } else if (currentAvgDaily > 0) {
                            100f // 从0增长到非0就是100%增长
                        } else {
                            0f
                        }
                        
                        // 只包含有变化的虚拟APP
                        if (changeMinutes != 0) {
                            // 根据程序名猜测分类
                            val (categoryName, categoryId) = guessVirtualAppCategory(programName, categories)
                            val categoryStyle = com.offtime.app.utils.CategoryUtils.getCategoryStyle(categoryName)
                            
                            // 为线下虚拟APP创建虚拟的AppInfoEntity
                            val virtualApp = com.offtime.app.data.entity.AppInfoEntity(
                                packageName = programName,
                                appName = getVirtualAppDisplayName(programName), // 使用友好的显示名称
                                versionName = "1.0",
                                versionCode = 1L,
                                isSystemApp = false,
                                categoryId = categoryId,
                                firstInstallTime = System.currentTimeMillis(),
                                lastUpdateTime = System.currentTimeMillis(),
                                isEnabled = true,
                                isExcluded = false
                            )
                            
                            appUsageChangeDataList.add(
                                AppUsageChangeData(
                                    app = virtualApp,
                                    categoryName = getVirtualAppDisplayName(programName), // 使用本地化的显示名称
                                    currentAvgDaily = currentAvgDaily,
                                    previousAvgDaily = previousAvgDaily,
                                    changeMinutes = changeMinutes,
                                    changePercent = changePercent,
                                    emoji = categoryStyle.emoji,
                                    color = categoryStyle.color
                                )
                            )
                        }
                    }
                }
                
                // 按变化幅度排序（绝对值）
                _appUsageChangeData.value = appUsageChangeDataList.sortedByDescending { kotlin.math.abs(it.changeMinutes) }
                
                android.util.Log.d(TAG, "APP使用变化数据加载完成: ${appUsageChangeDataList.size}个应用")
                
            } catch (e: Exception) {
                android.util.Log.e("StatsViewModel", "加载APP使用变化数据失败", e)
                _appUsageChangeData.value = emptyList()
            } finally {
                _isUsageChangeLoading.value = false
            }
        }
    }
    
    /**
     * 获取指定偏移天数的日期字符串
     */
    private fun getDateString(daysOffset: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, daysOffset)
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
    }
    
    // 批量获取上周的所有应用使用数据（包含真实APP和线下虚拟APP）
    private suspend fun getBatchAppUsageLastWeek(): Map<String, Int> {
        return try {
            val calendar = Calendar.getInstance()
            // 获取上周的开始日期（上周一）
            calendar.add(Calendar.WEEK_OF_YEAR, -1)
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            val usageMap = mutableMapOf<String, Int>()
            
            for (i in 0..6) { // 上周7天
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                val dayUsage = getBatchAppUsageByDate(date)
                
                dayUsage.forEach { (packageName, minutes) ->
                    usageMap[packageName] = (usageMap[packageName] ?: 0) + minutes
                }
                
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            
            android.util.Log.d(TAG, "批量获取上周使用数据: 共${usageMap.size}个程序/APP")
            usageMap
        } catch (e: Exception) {
            android.util.Log.e(TAG, "批量获取上周使用数据失败", e)
            emptyMap()
        }
    }
    
    // 批量获取上月的所有应用使用数据（包含真实APP和线下虚拟APP）
    private suspend fun getBatchAppUsageLastMonth(): Map<String, Int> {
        return try {
            val calendar = Calendar.getInstance()
            // 获取上月
            calendar.add(Calendar.MONTH, -1)
            
            // 设置到上月1号
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            val daysInLastMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            val usageMap = mutableMapOf<String, Int>()
            
            for (i in 0 until daysInLastMonth) {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                val dayUsage = getBatchAppUsageByDate(date)
                
                dayUsage.forEach { (packageName, minutes) ->
                    usageMap[packageName] = (usageMap[packageName] ?: 0) + minutes
                }
                
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            
            android.util.Log.d(TAG, "批量获取上月使用数据: 共${usageMap.size}个程序/APP")
            usageMap
        } catch (e: Exception) {
            android.util.Log.e(TAG, "批量获取上月使用数据失败", e)
            emptyMap()
        }
    }
    
    private fun loadAppDailyUsageRankingData() {
        viewModelScope.launch {
            _isDailyUsageRankingLoading.value = true
            try {
                val period = _dailyUsageRankingPeriod.value
                android.util.Log.d(TAG, "开始加载APP使用时长排序数据: period=$period")
                
                val appDailyUsageRankingDataList = mutableListOf<AppDailyUsageRankingData>()
                
                // 获取所有应用和分类
                val allApps = appInfoDao.getAllAppsList()
                val categories = appCategoryDao.getAllCategoriesList()
                val categoryMap = categories.associateBy { it.id }
                
                // 根据时间段批量获取使用数据
                val usageData = when (period) {
                    "今日" -> {
                        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        getBatchAppUsageByDate(today)
                    }
                    "昨日" -> {
                        val calendar = Calendar.getInstance()
                        calendar.add(Calendar.DAY_OF_YEAR, -1)
                        val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                        getBatchAppUsageByDate(yesterday)
                    }
                    "本周" -> getBatchAppUsageThisWeek()
                    "本月" -> getBatchAppUsageThisMonth()
                    "总共" -> getBatchAppUsageTotal()
                    else -> emptyMap()
                }
                
                // 1. 处理真实APP
                for (app in allApps) {
                    val category = categoryMap[app.categoryId] ?: continue
                    
                    // 跳过已排除的应用
                    if (app.isExcluded) {
                        continue
                    }
                    
                    val totalUsage = usageData[app.packageName] ?: 0
                    
                    val avgDailyMinutes = when (period) {
                        "今日", "昨日" -> totalUsage
                        "本周" -> totalUsage / 7
                        "本月" -> {
                            val calendar = Calendar.getInstance()
                            val daysInMonth = calendar.get(Calendar.DAY_OF_MONTH)
                            if (daysInMonth > 0) totalUsage / daysInMonth else 0
                        }
                        "总共" -> {
                            // 计算总天数（从有记录开始到现在）
                            val totalDays = getTotalDaysWithRecords(app.packageName)
                            if (totalDays > 0) totalUsage / totalDays else 0
                        }
                        else -> 0
                    }
                    
                    // 只包含有使用时间的APP
                    if (avgDailyMinutes > 0) {
                        val categoryStyle = com.offtime.app.utils.CategoryUtils.getCategoryStyle(category.name)
                        
                        appDailyUsageRankingDataList.add(
                            AppDailyUsageRankingData(
                                app = app,
                                categoryName = category.name,
                                avgDailyMinutes = avgDailyMinutes,
                                emoji = categoryStyle.emoji,
                                color = categoryStyle.color
                            )
                        )
                    }
                }
                
                // 2. 处理线下虚拟APP（没有对应真实APP记录的程序）
                val existingPackageNames = allApps.map { it.packageName }.toSet()
                usageData.forEach { (programName, totalUsage) ->
                    // 如果这个程序名不在真实APP列表中，说明是线下虚拟APP
                    if (!existingPackageNames.contains(programName) && totalUsage > 0) {
                        val avgDailyMinutes = when (period) {
                            "今日", "昨日" -> totalUsage
                            "本周" -> totalUsage / 7
                            "本月" -> {
                                val calendar = Calendar.getInstance()
                                val daysInMonth = calendar.get(Calendar.DAY_OF_MONTH)
                                if (daysInMonth > 0) totalUsage / daysInMonth else 0
                            }
                            "总共" -> {
                                val totalDays = getTotalDaysWithVirtualAppRecords(programName)
                                if (totalDays > 0) totalUsage / totalDays else 0
                            }
                            else -> 0
                        }
                        
                        if (avgDailyMinutes > 0) {
                            // 根据程序名猜测分类
                            val (categoryName, categoryId) = guessVirtualAppCategory(programName, categories)
                            val categoryStyle = com.offtime.app.utils.CategoryUtils.getCategoryStyle(categoryName)
                            
                            // 为线下虚拟APP创建虚拟的AppInfoEntity
                            val virtualApp = com.offtime.app.data.entity.AppInfoEntity(
                                packageName = programName,
                                appName = getVirtualAppDisplayName(programName), // 使用友好的显示名称
                                versionName = "1.0",
                                versionCode = 1L,
                                isSystemApp = false,
                                categoryId = categoryId,
                                firstInstallTime = System.currentTimeMillis(),
                                lastUpdateTime = System.currentTimeMillis(),
                                isEnabled = true,
                                isExcluded = false
                            )
                            
                            appDailyUsageRankingDataList.add(
                                AppDailyUsageRankingData(
                                    app = virtualApp,
                                    categoryName = getVirtualAppDisplayName(programName), // 使用本地化的显示名称
                                    avgDailyMinutes = avgDailyMinutes,
                                    emoji = categoryStyle.emoji,
                                    color = categoryStyle.color
                                )
                            )
                        }
                    }
                }
                
                // 按平均每日使用时间降序排序
                _appDailyUsageRankingData.value = appDailyUsageRankingDataList.sortedByDescending { it.avgDailyMinutes }
                
                android.util.Log.d(TAG, "APP使用时长排序数据加载完成: ${appDailyUsageRankingDataList.size}个应用")
                
            } catch (e: Exception) {
                android.util.Log.e("StatsViewModel", "加载APP使用时长排序数据失败", e)
                _appDailyUsageRankingData.value = emptyList()
            } finally {
                _isDailyUsageRankingLoading.value = false
            }
        }
    }
    
    // 批量获取指定日期的所有应用使用数据（包含真实APP和线下虚拟APP）
    private suspend fun getBatchAppUsageByDate(date: String): Map<String, Int> {
        return try {
            val usageMap = mutableMapOf<String, Int>()
            val categories = appCategoryDao.getAllCategoriesList()
            val categoryMap = categories.associateBy { it.id }
            
            // 1. 获取真实APP会话数据
            val appSessions = appSessionUserDao.getSessionsByDate(date)
            appSessions.forEach { session ->
                usageMap[session.pkgName] = (usageMap[session.pkgName] ?: 0) + (session.durationSec / 60)
            }
            
            // 2. 获取线下虚拟APP计时数据，按分类区分
            val timerSessions = timerSessionUserDao.getSessionsByDate(date)
            timerSessions.forEach { session ->
                // 根据分类创建唯一的虚拟程序名
                val category = categoryMap[session.catId]
                val virtualProgramName = if (category != null) {
                    "线下活动_${category.name}_${session.catId}"
                } else {
                    "线下活动_未知分类_${session.catId}"
                }
                usageMap[virtualProgramName] = (usageMap[virtualProgramName] ?: 0) + (session.durationSec / 60)
            }
            
            android.util.Log.d(TAG, "批量获取日期使用数据: date=$date, 真实APP=${appSessions.size}个, 线下活动=${timerSessions.size}个")
            usageMap
        } catch (e: Exception) {
            android.util.Log.e(TAG, "批量获取日期使用数据失败: date=$date", e)
            emptyMap()
        }
    }
    
    // 批量获取本周的所有应用使用数据（包含真实APP和线下虚拟APP）
    private suspend fun getBatchAppUsageThisWeek(): Map<String, Int> {
        return try {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            val usageMap = mutableMapOf<String, Int>()
            
            for (i in 0..6) { // 一周7天
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                val dayUsage = getBatchAppUsageByDate(date)
                
                dayUsage.forEach { (packageName, minutes) ->
                    usageMap[packageName] = (usageMap[packageName] ?: 0) + minutes
                }
                
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            
            android.util.Log.d(TAG, "批量获取本周使用数据: 共${usageMap.size}个程序/APP")
            usageMap
        } catch (e: Exception) {
            android.util.Log.e(TAG, "批量获取本周使用数据失败", e)
            emptyMap()
        }
    }
    
    // 批量获取本月的所有应用使用数据（包含真实APP和线下虚拟APP）
    private suspend fun getBatchAppUsageThisMonth(): Map<String, Int> {
        return try {
            val calendar = Calendar.getInstance()
            val daysInMonth = calendar.get(Calendar.DAY_OF_MONTH)
            calendar.set(Calendar.DAY_OF_MONTH, 1) // 设置到月初
            val usageMap = mutableMapOf<String, Int>()
            
            for (i in 0 until daysInMonth) {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                val dayUsage = getBatchAppUsageByDate(date)
                
                dayUsage.forEach { (packageName, minutes) ->
                    usageMap[packageName] = (usageMap[packageName] ?: 0) + minutes
                }
                
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            
            android.util.Log.d(TAG, "批量获取本月使用数据: 共${usageMap.size}个程序/APP")
            usageMap
        } catch (e: Exception) {
            android.util.Log.e(TAG, "批量获取本月使用数据失败", e)
            emptyMap()
        }
    }

    // 批量获取所有时间的应用使用数据（包含真实APP和线下虚拟APP）
    private suspend fun getBatchAppUsageTotal(): Map<String, Int> {
        return try {
            val usageMap = mutableMapOf<String, Int>()
            val categories = appCategoryDao.getAllCategoriesList()
            val categoryMap = categories.associateBy { it.id }
            
            // 1. 获取所有真实APP会话数据
            val appSessions = appSessionUserDao.getAllSessions()
            appSessions.forEach { session ->
                usageMap[session.pkgName] = (usageMap[session.pkgName] ?: 0) + (session.durationSec / 60)
            }
            
            // 2. 获取所有线下虚拟APP计时数据，按分类区分
            val timerSessions = timerSessionUserDao.getAllUserSessionsList()
            timerSessions.forEach { session ->
                // 根据分类创建唯一的虚拟程序名
                val category = categoryMap[session.catId]
                val virtualProgramName = if (category != null) {
                    "线下活动_${category.name}_${session.catId}"
                } else {
                    "线下活动_未知分类_${session.catId}"
                }
                usageMap[virtualProgramName] = (usageMap[virtualProgramName] ?: 0) + (session.durationSec / 60)
            }
            
            android.util.Log.d(TAG, "批量获取总使用数据: 真实APP=${appSessions.size}个, 线下活动=${timerSessions.size}个")
            usageMap
        } catch (e: Exception) {
            android.util.Log.e(TAG, "批量获取总使用数据失败", e)
            emptyMap()
        }
    }

    // 获取应用有记录的总天数
    private suspend fun getTotalDaysWithRecords(packageName: String): Int {
        return try {
            val sessions = appSessionUserDao.getSessionsByPackageNameDebug(packageName)
            val uniqueDates = sessions.map { it.date }.distinct()
            uniqueDates.size
        } catch (e: Exception) {
            android.util.Log.e(TAG, "获取总天数失败: packageName=$packageName", e)
            1 // 默认返回1避免除零错误
        }
    }
    
    // 获取虚拟APP有记录的总天数
    private suspend fun getTotalDaysWithVirtualAppRecords(programName: String): Int {
        return try {
            // 如果是新格式的虚拟程序名，需要根据分类ID来查找
            if (programName.startsWith("线下活动_")) {
                val parts = programName.split("_")
                if (parts.size >= 3) {
                    val categoryId = parts[2].toIntOrNull()
                    if (categoryId != null) {
                        // 按分类ID查找Timer会话记录
                        val allSessions = timerSessionUserDao.getAllUserSessionsList()
                        val categorySessions = allSessions.filter { it.catId == categoryId }
                        val uniqueDates = categorySessions.map { it.date }.distinct()
                        return uniqueDates.size
                    }
                }
            }
            
            // 回退到原来的程序名匹配逻辑
            val allSessions = timerSessionUserDao.getAllUserSessionsList()
            val programSessions = allSessions.filter { it.programName == programName }
            val uniqueDates = programSessions.map { it.date }.distinct()
            uniqueDates.size
        } catch (e: Exception) {
            android.util.Log.e(TAG, "获取虚拟APP总天数失败: programName=$programName", e)
            1 // 默认返回1避免除零错误
        }
    }
    
    // 生成虚拟APP的友好显示名称
    private fun getVirtualAppDisplayName(programName: String): String {
        return if (programName.startsWith("线下活动_")) {
            val parts = programName.split("_")
            if (parts.size >= 3) {
                val categoryName = parts[1]
                // 根据分类名称返回本地化的显示名称
                when (categoryName) {
                    "娱乐" -> if (java.util.Locale.getDefault().language == "en") "Offline-Entertainment" else "线下娱乐"
                    "学习" -> if (java.util.Locale.getDefault().language == "en") "Offline-Education" else "线下学习"
                    "健身" -> if (java.util.Locale.getDefault().language == "en") "Offline-Fitness" else "线下健身"
                    "工作" -> if (java.util.Locale.getDefault().language == "en") "Offline-Work" else "线下工作"
                    "其他" -> if (java.util.Locale.getDefault().language == "en") "Offline-Other" else "线下其他"
                    else -> if (java.util.Locale.getDefault().language == "en") "Offline-$categoryName" else "线下$categoryName"
                }
            } else {
                if (java.util.Locale.getDefault().language == "en") "Offline Activity" else "线下活动"
            }
        } else {
            programName
        }
    }
    
    // 根据程序名猜测虚拟APP的分类
    private fun guessVirtualAppCategory(programName: String, categories: List<AppCategoryEntity>): Pair<String, Int> {
        return try {
            // 如果是新格式的虚拟程序名: "线下活动_分类名_分类ID"
            if (programName.startsWith("线下活动_")) {
                val parts = programName.split("_")
                if (parts.size >= 3) {
                    val categoryName = parts[1]
                    val categoryId = parts[2].toIntOrNull()
                    
                    // 验证分类是否存在
                    categoryId?.let { id ->
                        val category = categories.find { it.name == categoryName && it.id == id }
                        if (category != null) {
                            android.util.Log.d(TAG, "虚拟APP分类解析: $programName -> $categoryName (ID=$id)")
                            return Pair(categoryName, id)
                        }
                    }
                }
            }
            
            // 回退到原来的关键词匹配逻辑
            val lowerProgramName = programName.lowercase()
            
            // 根据程序名关键词猜测分类
            val categoryName = when {
                // 学习相关关键词
                lowerProgramName.contains("学习") || 
                lowerProgramName.contains("读书") || 
                lowerProgramName.contains("阅读") || 
                lowerProgramName.contains("学") ||
                lowerProgramName.contains("书") ||
                lowerProgramName.contains("课程") ||
                lowerProgramName.contains("作业") -> "学习"
                
                // 健身相关关键词
                lowerProgramName.contains("运动") || 
                lowerProgramName.contains("健身") || 
                lowerProgramName.contains("跑步") || 
                lowerProgramName.contains("锻炼") ||
                lowerProgramName.contains("瑜伽") ||
                lowerProgramName.contains("游泳") ||
                lowerProgramName.contains("篮球") ||
                lowerProgramName.contains("足球") -> "健身"
                
                // 默认归类为娱乐
                else -> "娱乐"
            }
            
            // 查找对应分类的ID
            val category = categories.find { it.name == categoryName }
            val categoryId = category?.id ?: categories.find { it.name == "娱乐" }?.id ?: 1
            
            android.util.Log.d(TAG, "虚拟APP分类推测: $programName -> $categoryName (ID=$categoryId)")
            Pair(categoryName, categoryId)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "推测虚拟APP分类失败: programName=$programName", e)
            // 默认返回娱乐分类
            val entertainmentCategory = categories.find { it.name == "娱乐" }
            Pair("娱乐", entertainmentCategory?.id ?: 1)
        }
    }
    
    /**
     * 加载奖罚详情统计数据
     */
    private fun loadRewardPunishmentSummary() {
        viewModelScope.launch {
            _isRewardPunishmentSummaryLoading.value = true
            try {
                val selectedCategory = _selectedCategoryForSummary.value
                if (selectedCategory == null) {
                    _rewardPunishmentSummaryData.value = emptyList()
                    return@launch
                }
                
                val categoryId = selectedCategory.id
                val categoryName = selectedCategory.name
                
                // 获取奖罚配置
                val goal = goalRewardPunishmentUserDao.getUserGoalByCatId(categoryId)
                if (goal == null) {
                    _rewardPunishmentSummaryData.value = emptyList()
                    return@launch
                }
                
                val summaryList = mutableListOf<RewardPunishmentSummaryData>()
                
                // 计算昨天的数据
                val yesterday = getDateString(-1)
                val yesterdayData = calculatePeriodSummary("昨天", categoryId, categoryName, 
                    listOf(yesterday), goal)
                if (yesterdayData != null) summaryList.add(yesterdayData)
                
                // 计算本周的数据
                val thisWeek = getCurrentWeekDates()
                val weekData = calculatePeriodSummary("本周", categoryId, categoryName, 
                    thisWeek, goal)
                if (weekData != null) summaryList.add(weekData)
                
                // 计算本月的数据
                val thisMonth = getCurrentMonthDates()
                val monthData = calculatePeriodSummary("本月", categoryId, categoryName, 
                    thisMonth, goal)
                if (monthData != null) summaryList.add(monthData)
                
                // 计算总共数据
                val totalData = calculateTotalSummary(categoryId, categoryName, goal)
                if (totalData != null) summaryList.add(totalData)
                
                _rewardPunishmentSummaryData.value = summaryList
                
            } catch (e: Exception) {
                android.util.Log.e("StatsViewModel", "加载奖罚详情统计失败", e)
                _rewardPunishmentSummaryData.value = emptyList()
            } finally {
                _isRewardPunishmentSummaryLoading.value = false
            }
        }
    }
    
    /**
     * 计算指定时间段的奖罚统计
     */
    private suspend fun calculatePeriodSummary(
        period: String,
        categoryId: Int,
        categoryName: String,
        dates: List<String>,
        goal: com.offtime.app.data.entity.GoalRewardPunishmentUserEntity
    ): RewardPunishmentSummaryData? {
        try {
            var expectedRewardCount = 0
            var expectedPunishCount = 0
            var rewardCount = 0
            var punishCount = 0
            var totalRewardAmount = 0
            var totalPunishAmount = 0
            
            // 累计各天的奖罚应获得次数、完成次数和实际数量
            dates.forEach { date ->
                val record = rewardPunishmentUserDao.getRecordByCategoryAndDate(categoryId, date)
                if (record != null) {
                    // 有奖罚记录的天，直接从记录读取
                    if (record.isGoalMet == 1) {
                        expectedRewardCount += 1  // 目标完成，应获得奖励
                    } else {
                        expectedPunishCount += 1  // 目标未完成，应获得惩罚
                    }
                    rewardCount += record.rewardDone
                    punishCount += record.punishDone
                } else {
                    // 没有奖罚记录的天，需要计算是否应该有奖罚
                    val realUsage = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, date, 0) ?: 0
                    val virtualUsage = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, date, 1) ?: 0
                    val totalUsageSeconds = realUsage + virtualUsage
                    
                    // 只有该天有使用数据时才计算奖罚
                    if (totalUsageSeconds > 0) {
                        val goalSeconds = goal.dailyGoalMin * 60
                        val goalMet = when (goal.conditionType) {
                            0 -> totalUsageSeconds <= goalSeconds // 娱乐类：不超过目标算完成
                            1 -> totalUsageSeconds >= goalSeconds // 学习类：达到目标算完成
                            else -> false
                        }
                        
                        if (goalMet) {
                            expectedRewardCount += 1  // 目标完成，应获得奖励
                        } else {
                            expectedPunishCount += 1  // 目标未完成，应获得惩罚
                        }
                        // 没有记录说明奖罚都没完成，rewardCount和punishCount保持0
                    }
                }
                
                if (record != null) {
                    
                    // 如果该天有惩罚完成记录，计算实际惩罚数量
                    if (record.punishDone == 1 && record.isGoalMet == 0) {
                        // 获取该天的实际使用时间
                        val realUsage = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, date, 0) ?: 0
                        val virtualUsage = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, date, 1) ?: 0
                        val totalUsageSeconds = realUsage + virtualUsage
                        val goalSeconds = goal.dailyGoalMin * 60
                        
                        // 根据分类类型计算惩罚数量
                        when (goal.conditionType) {
                            0 -> {
                                // 娱乐类：超时惩罚
                                if (totalUsageSeconds > goalSeconds) {
                                    val overSeconds = totalUsageSeconds - goalSeconds
                                    val timeUnitSeconds = when (goal.punishTimeUnit) {
                                        "小时" -> 3600
                                        "分钟" -> 60
                                        "秒" -> 1
                                        else -> 3600
                                    }
                                    val overTimeUnits = kotlin.math.ceil(overSeconds.toDouble() / timeUnitSeconds.toDouble()).toInt()
                                    totalPunishAmount += overTimeUnits * goal.punishNumber
                                }
                            }
                            1 -> {
                                // 学习/健身类：未达标惩罚
                                if (totalUsageSeconds < goalSeconds) {
                                    val shortSeconds = goalSeconds - totalUsageSeconds
                                    val timeUnitSeconds = when (goal.punishTimeUnit) {
                                        "小时" -> 3600
                                        "分钟" -> 60
                                        "秒" -> 1
                                        else -> 3600
                                    }
                                    val shortTimeUnits = kotlin.math.ceil(shortSeconds.toDouble() / timeUnitSeconds.toDouble()).toInt()
                                    totalPunishAmount += shortTimeUnits * goal.punishNumber
                                }
                            }
                        }
                    }
                    
                    // 如果该天有奖励完成记录，计算实际奖励数量
                    if (record.rewardDone == 1 && record.isGoalMet == 1) {
                        // 获取该天的实际使用时间
                        val realUsage = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, date, 0) ?: 0
                        val virtualUsage = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, date, 1) ?: 0
                        val totalUsageSeconds = realUsage + virtualUsage
                        val goalSeconds = goal.dailyGoalMin * 60
                        
                        // 根据分类类型计算奖励数量
                        when (goal.conditionType) {
                            0 -> {
                                // 娱乐类：少用奖励
                                if (totalUsageSeconds <= goalSeconds) {
                                    val savedSeconds = goalSeconds - totalUsageSeconds
                                    val timeUnitSeconds = when (goal.rewardTimeUnit) {
                                        "小时" -> 3600
                                        "分钟" -> 60
                                        "秒" -> 1
                                        else -> 3600
                                    }
                                    val savedTimeUnits = kotlin.math.ceil(savedSeconds.toDouble() / timeUnitSeconds.toDouble()).toInt()
                                    totalRewardAmount += savedTimeUnits * goal.rewardNumber
                                }
                            }
                            1 -> {
                                // 学习/健身类：超额完成奖励
                                if (totalUsageSeconds >= goalSeconds) {
                                    val extraSeconds = totalUsageSeconds - goalSeconds
                                    val timeUnitSeconds = when (goal.rewardTimeUnit) {
                                        "小时" -> 3600
                                        "分钟" -> 60
                                        "秒" -> 1
                                        else -> 3600
                                    }
                                    val extraTimeUnits = kotlin.math.ceil(extraSeconds.toDouble() / timeUnitSeconds.toDouble()).toInt()
                                    totalRewardAmount += extraTimeUnits * goal.rewardNumber
                                }
                            }
                        }
                    }
                }
            }
            
            // 计算总量（使用实际计算的数量，而不是简单的次数乘法）
            val rewardTotal = if (totalRewardAmount > 0) {
                "${totalRewardAmount}${goal.rewardUnit}"
            } else if (rewardCount > 0) {
                // 如果没有实际计算数量，回退到简单计算
                "${rewardCount * goal.rewardNumber}${goal.rewardUnit}"
            } else {
                "0${goal.rewardUnit}"
            }
            
            // 使用本地化的单位
            val localizedPunishUnit = if (java.util.Locale.getDefault().language == "en") {
                when (goal.punishText) {
                    "俯卧撑" -> "" // 英文下不需要单位
                    "仰卧起坐" -> "" // 英文下不需要单位
                    "跑步" -> " km" // 英文下使用km
                    else -> goal.punishUnit
                }
            } else {
                goal.punishUnit
            }
            
            val punishTotal = if (totalPunishAmount > 0) {
                "${totalPunishAmount}${localizedPunishUnit}"
            } else if (punishCount > 0) {
                // 如果没有实际计算数量，回退到简单计算
                "${punishCount * goal.punishNumber}${localizedPunishUnit}"
            } else {
                "0${localizedPunishUnit}"
            }
            
            android.util.Log.d(TAG, "期间[$period]奖罚统计: 应获得奖励=${expectedRewardCount}次, 完成奖励=${rewardCount}次, 实际奖励量=$totalRewardAmount, 应获得惩罚=${expectedPunishCount}次, 完成惩罚=${punishCount}次, 实际惩罚量=$totalPunishAmount")
            
            return RewardPunishmentSummaryData(
                period = period,
                categoryName = categoryName,
                expectedRewardCount = expectedRewardCount,
                rewardCount = rewardCount,
                rewardContent = goal.rewardText,
                rewardTotal = rewardTotal,
                expectedPunishCount = expectedPunishCount,
                punishCount = punishCount,
                punishContent = goal.punishText,
                punishTotal = punishTotal
            )
        } catch (e: Exception) {
            android.util.Log.e("StatsViewModel", "计算${period}奖罚统计失败", e)
            return null
        }
    }
    
    /**
     * 计算总共数据
     */
    private suspend fun calculateTotalSummary(
        categoryId: Int,
        categoryName: String, 
        goal: com.offtime.app.data.entity.GoalRewardPunishmentUserEntity
    ): RewardPunishmentSummaryData? {
        try {
            val allRecords = rewardPunishmentUserDao.getAllRecords()
            val categoryRecords = allRecords.filter { it.catId == categoryId }
            
            val expectedRewardCount = categoryRecords.count { it.isGoalMet == 1 }
            val expectedPunishCount = categoryRecords.count { it.isGoalMet == 0 }
            val totalRewardCount = categoryRecords.sumOf { it.rewardDone }
            val totalPunishCount = categoryRecords.sumOf { it.punishDone }
            var totalRewardAmount = 0
            var totalPunishAmount = 0
            
            // 计算实际奖罚数量
            categoryRecords.forEach { record ->
                // 如果该天有惩罚完成记录，计算实际惩罚数量
                if (record.punishDone == 1 && record.isGoalMet == 0) {
                    // 获取该天的实际使用时间
                    val realUsage = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, record.date, 0) ?: 0
                    val virtualUsage = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, record.date, 1) ?: 0
                    val totalUsageSeconds = realUsage + virtualUsage
                    val goalSeconds = goal.dailyGoalMin * 60
                    
                    // 根据分类类型计算惩罚数量
                    when (goal.conditionType) {
                        0 -> {
                            // 娱乐类：超时惩罚
                            if (totalUsageSeconds > goalSeconds) {
                                val overSeconds = totalUsageSeconds - goalSeconds
                                val timeUnitSeconds = when (goal.punishTimeUnit) {
                                    "小时" -> 3600
                                    "分钟" -> 60
                                    "秒" -> 1
                                    else -> 3600
                                }
                                val overTimeUnits = kotlin.math.ceil(overSeconds.toDouble() / timeUnitSeconds.toDouble()).toInt()
                                totalPunishAmount += overTimeUnits * goal.punishNumber
                            }
                        }
                        1 -> {
                            // 学习/健身类：未达标惩罚
                            if (totalUsageSeconds < goalSeconds) {
                                val shortSeconds = goalSeconds - totalUsageSeconds
                                val timeUnitSeconds = when (goal.punishTimeUnit) {
                                    "小时" -> 3600
                                    "分钟" -> 60
                                    "秒" -> 1
                                    else -> 3600
                                }
                                val shortTimeUnits = kotlin.math.ceil(shortSeconds.toDouble() / timeUnitSeconds.toDouble()).toInt()
                                totalPunishAmount += shortTimeUnits * goal.punishNumber
                            }
                        }
                    }
                }
                
                // 如果该天有奖励完成记录，计算实际奖励数量
                if (record.rewardDone == 1 && record.isGoalMet == 1) {
                    // 获取该天的实际使用时间
                    val realUsage = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, record.date, 0) ?: 0
                    val virtualUsage = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, record.date, 1) ?: 0
                    val totalUsageSeconds = realUsage + virtualUsage
                    val goalSeconds = goal.dailyGoalMin * 60
                    
                    // 根据分类类型计算奖励数量
                    when (goal.conditionType) {
                        0 -> {
                            // 娱乐类：少用奖励
                            if (totalUsageSeconds <= goalSeconds) {
                                val savedSeconds = goalSeconds - totalUsageSeconds
                                val timeUnitSeconds = when (goal.rewardTimeUnit) {
                                    "小时" -> 3600
                                    "分钟" -> 60
                                    "秒" -> 1
                                    else -> 3600
                                }
                                val savedTimeUnits = kotlin.math.ceil(savedSeconds.toDouble() / timeUnitSeconds.toDouble()).toInt()
                                totalRewardAmount += savedTimeUnits * goal.rewardNumber
                            }
                        }
                        1 -> {
                            // 学习/健身类：超额完成奖励
                            if (totalUsageSeconds >= goalSeconds) {
                                val extraSeconds = totalUsageSeconds - goalSeconds
                                val timeUnitSeconds = when (goal.rewardTimeUnit) {
                                    "小时" -> 3600
                                    "分钟" -> 60
                                    "秒" -> 1
                                    else -> 3600
                                }
                                val extraTimeUnits = kotlin.math.ceil(extraSeconds.toDouble() / timeUnitSeconds.toDouble()).toInt()
                                totalRewardAmount += extraTimeUnits * goal.rewardNumber
                            }
                        }
                    }
                }
            }
            
            val rewardTotal = if (totalRewardAmount > 0) {
                "${totalRewardAmount}${goal.rewardUnit}"
            } else if (totalRewardCount > 0) {
                // 如果没有实际计算数量，回退到简单计算
                "${totalRewardCount * goal.rewardNumber}${goal.rewardUnit}"
            } else {
                "0${goal.rewardUnit}"
            }
            
            // 使用本地化的单位
            val localizedPunishUnit = if (java.util.Locale.getDefault().language == "en") {
                when (goal.punishText) {
                    "俯卧撑" -> "" // 英文下不需要单位
                    "仰卧起坐" -> "" // 英文下不需要单位
                    "跑步" -> " km" // 英文下使用km
                    else -> goal.punishUnit
                }
            } else {
                goal.punishUnit
            }
            
            val punishTotal = if (totalPunishAmount > 0) {
                "${totalPunishAmount}${localizedPunishUnit}"
            } else if (totalPunishCount > 0) {
                // 如果没有实际计算数量，回退到简单计算
                "${totalPunishCount * goal.punishNumber}${localizedPunishUnit}"
            } else {
                "0${localizedPunishUnit}"
            }
            
            android.util.Log.d(TAG, "总共奖罚统计: 应获得奖励=${expectedRewardCount}次, 完成奖励=${totalRewardCount}次, 实际奖励量=$totalRewardAmount, 应获得惩罚=${expectedPunishCount}次, 完成惩罚=${totalPunishCount}次, 实际惩罚量=$totalPunishAmount")
            
            return RewardPunishmentSummaryData(
                period = "总共",
                categoryName = categoryName,
                expectedRewardCount = expectedRewardCount,
                rewardCount = totalRewardCount,
                rewardContent = goal.rewardText,
                rewardTotal = rewardTotal,
                expectedPunishCount = expectedPunishCount,
                punishCount = totalPunishCount,
                punishContent = goal.punishText,
                punishTotal = punishTotal
            )
        } catch (e: Exception) {
            android.util.Log.e("StatsViewModel", "计算总共奖罚统计失败", e)
            return null
        }
    }
    
    /**
     * 获取本周日期列表（从周一到今天）
     */
    private fun getCurrentWeekDates(): List<String> {
        val dates = mutableListOf<String>()
        val calendar = Calendar.getInstance()
        
        // 设置到本周一
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val startOfWeek = calendar.clone() as Calendar
        val today = Calendar.getInstance()
        
        // 从周一到今天（包含今天）
        while (startOfWeek.timeInMillis <= today.timeInMillis) {
            dates.add(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(startOfWeek.time))
            startOfWeek.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        return dates
    }
    
    /**
     * 获取本月日期列表（从1号到今天）
     */
    private fun getCurrentMonthDates(): List<String> {
        val dates = mutableListOf<String>()
        val calendar = Calendar.getInstance()
        
        // 设置到本月1号
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val startOfMonth = calendar.clone() as Calendar
        val today = Calendar.getInstance()
        
        // 从1号到今天（包含今天）
        while (startOfMonth.timeInMillis <= today.timeInMillis) {
            dates.add(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(startOfMonth.time))
            startOfMonth.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        return dates
    }

    /**
     * 设置奖励详情时间段
     */
    fun setRewardDetailsPeriod(period: String) {
        _rewardDetailsPeriod.value = period
        loadRewardDetails()
    }

    /**
     * 设置惩罚详情时间段
     */
    fun setPunishmentDetailsPeriod(period: String) {
        _punishmentDetailsPeriod.value = period
        loadPunishmentDetails()
    }

    /**
     * 加载奖励详情数据
     */
    private fun loadRewardDetails() {
        viewModelScope.launch {
            _isRewardDetailsLoading.value = true
            try {
                val period = _rewardDetailsPeriod.value
                val allCategories = appCategoryDao.getAllCategoriesList()
                val rewardDetailsList = mutableListOf<RewardPunishmentSummaryData>()
                
                allCategories.forEach { category ->
                    val categoryId = category.id
                    val categoryName = category.name
                    
                    // 获取奖罚配置
                    val goal = goalRewardPunishmentUserDao.getUserGoalByCatId(categoryId)
                    if (goal != null) {
                        val dates = when (period) {
                            "昨天" -> listOf(getDateString(-1))
                            "本周" -> getCurrentWeekDates()
                            "本月" -> getCurrentMonthDates()
                            "总共" -> getAllDatesSinceStart(categoryId)
                            else -> emptyList()
                        }
                        
                        val summaryData = calculatePeriodSummary(period, categoryId, categoryName, dates, goal)
                        if (summaryData != null) {
                            rewardDetailsList.add(summaryData)
                        }
                    }
                }
                
                _rewardDetailsData.value = rewardDetailsList
            } catch (e: Exception) {
                android.util.Log.e("StatsViewModel", "加载奖励详情失败", e)
                _rewardDetailsData.value = emptyList()
            } finally {
                _isRewardDetailsLoading.value = false
            }
        }
    }

    /**
     * 加载惩罚详情数据
     */
    private fun loadPunishmentDetails() {
        viewModelScope.launch {
            _isPunishmentDetailsLoading.value = true
            try {
                val period = _punishmentDetailsPeriod.value
                val allCategories = appCategoryDao.getAllCategoriesList()
                val punishmentDetailsList = mutableListOf<RewardPunishmentSummaryData>()
                
                allCategories.forEach { category ->
                    val categoryId = category.id
                    val categoryName = category.name
                    
                    // 获取奖罚配置
                    val goal = goalRewardPunishmentUserDao.getUserGoalByCatId(categoryId)
                    if (goal != null) {
                        val dates = when (period) {
                            "昨天" -> listOf(getDateString(-1))
                            "本周" -> getCurrentWeekDates()
                            "本月" -> getCurrentMonthDates()
                            "总共" -> getAllDatesSinceStart(categoryId)
                            else -> emptyList()
                        }
                        
                        val summaryData = calculatePeriodSummary(period, categoryId, categoryName, dates, goal)
                        if (summaryData != null) {
                            punishmentDetailsList.add(summaryData)
                        }
                    }
                }
                
                _punishmentDetailsData.value = punishmentDetailsList
            } catch (e: Exception) {
                android.util.Log.e("StatsViewModel", "加载惩罚详情失败", e)
                _punishmentDetailsData.value = emptyList()
            } finally {
                _isPunishmentDetailsLoading.value = false
            }
        }
    }

    /**
     * 获取自有记录以来的所有日期
     */
    private suspend fun getAllDatesSinceStart(categoryId: Int): List<String> {
        return try {
            val allRecords = rewardPunishmentUserDao.getAllRecords()
            val categoryRecords = allRecords.filter { it.catId == categoryId }
            categoryRecords.map { it.date }.distinct().sorted()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 手动触发完整数据更新
     * 使用统一更新机制，按序执行：数据收集 → 基础更新 → 聚合更新 → UI刷新
     */
    fun triggerDataAggregation() {
        viewModelScope.launch {
            try {
                android.util.Log.d(TAG, "手动触发统一数据更新...")
                com.offtime.app.service.UnifiedUpdateService.triggerManualUpdate(
                    context = context
                )
                android.util.Log.d(TAG, "统一数据更新触发成功")
                
                // 等待更新完成后重新加载数据
                kotlinx.coroutines.delay(5000) // 等待5秒，统一更新流程比单独聚合耗时更长
                loadAllData()
                android.util.Log.d(TAG, "数据重新加载完成")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "触发统一数据更新失败", e)
            }
        }
    }
} 