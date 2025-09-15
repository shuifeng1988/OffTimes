package com.offtime.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.offtime.app.data.entity.*
import com.offtime.app.data.dao.*
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import com.offtime.app.service.DataAggregationService

@HiltViewModel
class DebugSummaryTablesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val summaryUsageDao: SummaryUsageDao,
    private val rewardPunishmentWeekUserDao: RewardPunishmentWeekUserDao,
    private val rewardPunishmentMonthUserDao: RewardPunishmentMonthUserDao,
    private val rewardPunishmentUserDao: RewardPunishmentUserDao,
    private val appCategoryDao: AppCategoryDao,
    private val appSessionUserDao: AppSessionUserDao,
    private val timerSessionUserDao: TimerSessionUserDao
) : ViewModel() {

    private val _summaryUsageUser = MutableStateFlow<List<SummaryUsageUserEntity>>(emptyList())
    val summaryUsageUser: StateFlow<List<SummaryUsageUserEntity>> = _summaryUsageUser.asStateFlow()
    
    private val _summaryUsageWeek = MutableStateFlow<List<SummaryUsageWeekUserEntity>>(emptyList())
    val summaryUsageWeek: StateFlow<List<SummaryUsageWeekUserEntity>> = _summaryUsageWeek.asStateFlow()
    
    private val _summaryUsageMonth = MutableStateFlow<List<SummaryUsageMonthUserEntity>>(emptyList())
    val summaryUsageMonth: StateFlow<List<SummaryUsageMonthUserEntity>> = _summaryUsageMonth.asStateFlow()
    
    private val _rewardPunishmentWeek = MutableStateFlow<List<RewardPunishmentWeekUserEntity>>(emptyList())
    val rewardPunishmentWeek: StateFlow<List<RewardPunishmentWeekUserEntity>> = _rewardPunishmentWeek.asStateFlow()
    
    private val _rewardPunishmentMonth = MutableStateFlow<List<RewardPunishmentMonthUserEntity>>(emptyList())
    val rewardPunishmentMonth: StateFlow<List<RewardPunishmentMonthUserEntity>> = _rewardPunishmentMonth.asStateFlow()
    
    private val _rewardPunishmentUser = MutableStateFlow<List<RewardPunishmentUserEntity>>(emptyList())
    val rewardPunishmentUser: StateFlow<List<RewardPunishmentUserEntity>> = _rewardPunishmentUser.asStateFlow()
    
    private val _categories = MutableStateFlow<List<AppCategoryEntity>>(emptyList())
    val categories: StateFlow<List<AppCategoryEntity>> = _categories.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 加载所有汇总表数据
                val summaryUserData = summaryUsageDao.getAllSummaryUsageUser()
                _summaryUsageUser.value = summaryUserData.sortedByDescending { it.updateTime }
                android.util.Log.d("DebugSummaryTablesViewModel", "加载日汇总数据: ${summaryUserData.size} 条")
                
                val summaryWeekData = summaryUsageDao.getAllSummaryUsageWeek()
                _summaryUsageWeek.value = summaryWeekData.sortedByDescending { it.updateTime }
                android.util.Log.d("DebugSummaryTablesViewModel", "加载周汇总数据: ${summaryWeekData.size} 条")
                
                val summaryMonthData = summaryUsageDao.getAllSummaryUsageMonth()
                _summaryUsageMonth.value = summaryMonthData.sortedByDescending { it.updateTime }
                android.util.Log.d("DebugSummaryTablesViewModel", "加载月汇总数据: ${summaryMonthData.size} 条")
                
                val rewardWeekData = rewardPunishmentWeekUserDao.getAllRecords()
                _rewardPunishmentWeek.value = rewardWeekData.sortedByDescending { it.updateTime }
                android.util.Log.d("DebugSummaryTablesViewModel", "加载周奖罚数据: ${rewardWeekData.size} 条")
                
                val rewardMonthData = rewardPunishmentMonthUserDao.getAllRecords()
                _rewardPunishmentMonth.value = rewardMonthData.sortedByDescending { it.updateTime }
                android.util.Log.d("DebugSummaryTablesViewModel", "加载月奖罚数据: ${rewardMonthData.size} 条")
                
                val rewardUserData = rewardPunishmentUserDao.getAllRecords()
                _rewardPunishmentUser.value = rewardUserData.sortedByDescending { it.updateTime }
                android.util.Log.d("DebugSummaryTablesViewModel", "加载日奖罚数据: ${rewardUserData.size} 条")
                
                // 加载分类数据
                val categoriesData = appCategoryDao.getAllCategoriesList()
                _categories.value = categoriesData
                android.util.Log.d("DebugSummaryTablesViewModel", "加载分类数据: ${categoriesData.size} 个分类")
                
            } catch (e: Exception) {
                android.util.Log.e("DebugSummaryTablesViewModel", "加载数据失败", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun generateTestData() {
        viewModelScope.launch {
            try {
                android.util.Log.d("DebugSummaryTablesViewModel", "开始生成测试数据")
                
                val categories = _categories.value
                if (categories.isEmpty()) {
                    android.util.Log.w("DebugSummaryTablesViewModel", "没有分类数据，无法生成测试数据")
                    return@launch
                }
                
                // 重新加载数据
                loadData()
                
            } catch (e: Exception) {
                android.util.Log.e("DebugSummaryTablesViewModel", "生成测试数据失败", e)
            }
        }
    }
    
    fun clearAllSummaryData() {
        viewModelScope.launch {
            try {
                android.util.Log.d("DebugSummaryTablesViewModel", "开始清空汇总数据")
                
                // 只清空聚合表，不清空原始数据
                summaryUsageDao.deleteAllSummaryUsage()
                rewardPunishmentWeekUserDao.deleteAll()
                rewardPunishmentMonthUserDao.deleteAll()
                
                android.util.Log.d("DebugSummaryTablesViewModel", "汇总数据清空完成")
                
                // 重新加载数据
                loadData()
                
            } catch (e: Exception) {
                android.util.Log.e("DebugSummaryTablesViewModel", "清空汇总数据失败", e)
            }
        }
    }
    
    fun checkOriginalData() {
        viewModelScope.launch {
            try {
                android.util.Log.d("DebugSummaryTablesViewModel", "开始检查原始数据")
                
                val originalData = rewardPunishmentUserDao.getAllRecords()
                android.util.Log.d("DebugSummaryTablesViewModel", "原始奖惩记录数量: ${originalData.size}")
                
                originalData.forEach { record ->
                    android.util.Log.d("DebugSummaryTablesViewModel", "原始记录: ${record.date}, catId=${record.catId}, 目标达成=${record.isGoalMet}, 奖励完成=${record.rewardDone}, 惩罚完成=${record.punishDone}")
                }
                
            } catch (e: Exception) {
                android.util.Log.e("DebugSummaryTablesViewModel", "检查原始数据失败", e)
            }
        }
    }
    
    /**
     * 重建历史汇总数据
     */
    fun rebuildSummaryData() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                android.util.Log.d("DebugSummaryTablesViewModel", "开始重建历史汇总数据")
                
                rebuildHistoricalSummaryData()
                
                // 重新加载数据以显示更新结果
                loadData()
                
                android.util.Log.d("DebugSummaryTablesViewModel", "历史汇总数据重建完成")
                
            } catch (e: Exception) {
                android.util.Log.e("DebugSummaryTablesViewModel", "重建历史汇总数据失败", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 生成基础汇总记录
     */
    fun generateBaseSummaryRecords() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                android.util.Log.d("DebugSummaryTablesViewModel", "开始生成基础汇总记录")
                
                // 触发基础记录生成服务
                com.offtime.app.service.DataAggregationService.ensureBaseRecords(context)
                
                // 等待生成完成
                kotlinx.coroutines.delay(3000)
                
                // 重新加载数据以显示更新结果
                loadData()
                
                android.util.Log.d("DebugSummaryTablesViewModel", "基础汇总记录生成完成")
                
            } catch (e: Exception) {
                android.util.Log.e("DebugSummaryTablesViewModel", "生成基础汇总记录失败", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 批量重建历史汇总数据
     * 从原始会话表中读取所有历史数据，重新生成汇总表
     */
    fun rebuildHistoricalSummaryData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                android.util.Log.d("DebugSummaryTablesViewModel", "开始批量重建历史汇总数据")
                
                // 1. 清空现有汇总数据
                summaryUsageDao.deleteAllSummaryUsage()
                rewardPunishmentWeekUserDao.deleteAll()
                rewardPunishmentMonthUserDao.deleteAll()
                
                // 2. 获取所有历史日期
                val appSessionDates = appSessionUserDao.getAllDates()
                val timerSessionDates = timerSessionUserDao.getAllDates()
                val allDates = (appSessionDates + timerSessionDates).distinct().sorted()
                
                android.util.Log.d("DebugSummaryTablesViewModel", "找到历史数据日期: ${allDates.size} 天")
                
                // 3. 逐日重建汇总数据
                allDates.forEach { date ->
                    try {
                        rebuildSummaryDataForDate(date)
                        android.util.Log.d("DebugSummaryTablesViewModel", "重建日期 $date 的汇总数据完成")
                    } catch (e: Exception) {
                        android.util.Log.e("DebugSummaryTablesViewModel", "重建日期 $date 的汇总数据失败", e)
                    }
                }
                
                // 4. 重建周汇总和月汇总数据
                rebuildWeeklyAndMonthlySummary()
                
                android.util.Log.d("DebugSummaryTablesViewModel", "历史汇总数据重建完成")
                
                // 5. 重新加载数据
                loadData()
                
            } catch (e: Exception) {
                android.util.Log.e("DebugSummaryTablesViewModel", "批量重建历史汇总数据失败", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 重建指定日期的汇总数据
     */
    private suspend fun rebuildSummaryDataForDate(date: String) {
        // 获取该日期的所有原始会话数据
        val appSessions = appSessionUserDao.getSessionsByDate(date)
        val timerSessions = timerSessionUserDao.getSessionsByDate(date)
        
        // 按分类分组聚合
        val dailyData = mutableMapOf<Int, Int>() // catId -> totalSeconds
        
        // 聚合APP会话数据
        appSessions.forEach { session ->
            dailyData[session.catId] = (dailyData[session.catId] ?: 0) + session.durationSec
        }
        
        // 聚合计时会话数据
        timerSessions.forEach { session ->
            dailyData[session.catId] = (dailyData[session.catId] ?: 0) + session.durationSec
        }
        
        // 插入到summary_usage_user表
        dailyData.forEach { (catId, totalSec) ->
            val id = "${date}_${catId}"
            val entity = com.offtime.app.data.entity.SummaryUsageUserEntity(
                id = id,
                date = date,
                catId = catId,
                totalSec = totalSec
            )
            summaryUsageDao.upsert(entity)
        }
        
        // 为"总使用"分类生成汇总数据
        generateTotalUsageSummaryForDate(date, dailyData)
    }
    
    /**
     * 为指定日期生成"总使用"分类的汇总数据
     */
    private suspend fun generateTotalUsageSummaryForDate(date: String, dailyData: Map<Int, Int>) {
        try {
            val categories = appCategoryDao.getAllCategoriesList()
            val totalUsageCategory = categories.find { it.name == "总使用" }
            
            if (totalUsageCategory == null) {
                android.util.Log.w("DebugSummaryTablesViewModel", "未找到'总使用'分类，跳过汇总")
                return
            }
            
            // 计算总使用时间（排除"总使用"分类本身）
            var totalUsageSeconds = 0
            dailyData.forEach { (catId, totalSec) ->
                if (catId != totalUsageCategory.id) {
                    totalUsageSeconds += totalSec
                }
            }
            
            // 为"总使用"分类创建汇总记录
            val totalUsageId = "${date}_${totalUsageCategory.id}"
            val totalUsageEntity = com.offtime.app.data.entity.SummaryUsageUserEntity(
                id = totalUsageId,
                date = date,
                catId = totalUsageCategory.id,
                totalSec = totalUsageSeconds
            )
            
            summaryUsageDao.upsert(totalUsageEntity)
            
        } catch (e: Exception) {
            android.util.Log.e("DebugSummaryTablesViewModel", "生成总使用汇总失败: $date", e)
        }
    }
    
    /**
     * 重建周汇总和月汇总数据
     */
    private suspend fun rebuildWeeklyAndMonthlySummary() {
        try {
            // 获取所有日汇总数据
            val allDailySummary = summaryUsageDao.getAllSummaryUsageUser()
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val weekFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val monthFormat = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
            
            // 按周分组
            val weeklyGroups = mutableMapOf<String, MutableMap<Int, MutableList<com.offtime.app.data.entity.SummaryUsageUserEntity>>>()
            // 按月分组
            val monthlyGroups = mutableMapOf<String, MutableMap<Int, MutableList<com.offtime.app.data.entity.SummaryUsageUserEntity>>>()
            
            allDailySummary.forEach { summary ->
                val date = dateFormat.parse(summary.date)
                if (date != null) {
                    val calendar = java.util.Calendar.getInstance()
                    calendar.time = date
                    
                    // 计算周开始日期（周一）
                    calendar.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
                    val weekStart = weekFormat.format(calendar.time)
                    
                    // 计算月份
                    val month = monthFormat.format(date)
                    
                    // 分组到周
                    weeklyGroups.getOrPut(weekStart) { mutableMapOf() }
                        .getOrPut(summary.catId) { mutableListOf() }
                        .add(summary)
                    
                    // 分组到月
                    monthlyGroups.getOrPut(month) { mutableMapOf() }
                        .getOrPut(summary.catId) { mutableListOf() }
                        .add(summary)
                }
            }
            
            // 生成周汇总数据
            weeklyGroups.forEach { (weekStart, categoryData) ->
                categoryData.forEach { (catId, summaries) ->
                    val totalSec = summaries.sumOf { it.totalSec }
                    val dayCount = summaries.size
                    val avgDailySec = if (dayCount > 0) totalSec / dayCount else 0
                    
                    val id = "${weekStart}_${catId}"
                    val entity = com.offtime.app.data.entity.SummaryUsageWeekUserEntity(
                        id = id,
                        weekStartDate = weekStart,
                        catId = catId,
                        avgDailySec = avgDailySec
                    )
                    summaryUsageDao.upsertWeek(entity)
                }
            }
            
            // 生成月汇总数据
            monthlyGroups.forEach { (month, categoryData) ->
                categoryData.forEach { (catId, summaries) ->
                    val totalSec = summaries.sumOf { it.totalSec }
                    val dayCount = summaries.size
                    val avgDailySec = if (dayCount > 0) totalSec / dayCount else 0
                    
                    val id = "${month}_${catId}"
                    val entity = com.offtime.app.data.entity.SummaryUsageMonthUserEntity(
                        id = id,
                        month = month,
                        catId = catId,
                        avgDailySec = avgDailySec
                    )
                    summaryUsageDao.upsertMonth(entity)
                }
            }
            
            android.util.Log.d("DebugSummaryTablesViewModel", "周汇总和月汇总数据重建完成")
            
        } catch (e: Exception) {
            android.util.Log.e("DebugSummaryTablesViewModel", "重建周汇总和月汇总数据失败", e)
        }
    }
} 