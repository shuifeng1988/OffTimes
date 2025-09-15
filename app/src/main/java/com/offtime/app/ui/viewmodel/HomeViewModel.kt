package com.offtime.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.offtime.app.manager.DataUpdateManager
import com.offtime.app.data.dao.AppCategoryDao
import com.offtime.app.data.dao.AppSessionUserDao
import com.offtime.app.data.dao.TimerSessionUserDao
import com.offtime.app.data.dao.DailyUsageDao
import com.offtime.app.data.dao.SummaryUsageDao
import com.offtime.app.data.dao.RewardPunishmentData
import com.offtime.app.data.dao.UsageData
import com.offtime.app.data.entity.AppCategoryEntity
import com.offtime.app.data.entity.AppSessionUserEntity
import com.offtime.app.utils.DataMigrationHelper
import javax.inject.Inject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar
import com.offtime.app.data.dao.AppInfoDao
import com.offtime.app.data.dao.GoalRewardPunishmentUserDao
import com.offtime.app.data.dao.RewardPunishmentUserDao
import com.offtime.app.data.dao.RewardPunishmentWeekUserDao
import com.offtime.app.data.dao.RewardPunishmentMonthUserDao
import com.offtime.app.data.entity.RewardPunishmentUserEntity
import com.offtime.app.data.repository.AppRepository
import com.offtime.app.data.repository.AppSessionRepository
import com.offtime.app.data.repository.GoalRewardPunishmentRepository
import com.offtime.app.data.repository.TimerSessionRepository
import com.offtime.app.data.repository.UserRepository
import com.offtime.app.utils.DefaultValueLocalizer
import com.offtime.app.utils.DateLocalizer
import com.offtime.app.utils.UnifiedTextManager
import kotlinx.coroutines.Job

/**
 * OffTime应用首页视图模型
 * 
 * 这是应用的核心ViewModel，负责管理首页界面的所有数据和业务逻辑。
 * 经过多次优化和重构，现在提供稳定、高效的数据管理和状态同步。
 * 
 * == 核心功能模块 ==
 * 
 * 1. 【分类管理】
 *    - 应用分类的加载、选择和切换
 *    - 支持"全部"分类和具体应用分类
 *    - 动态加载分类对应的目标配置
 * 
 * 2. 【使用统计数据】
 *    - 今日实时使用时间统计（实际使用+虚拟使用）
 *    - 小时级使用分布数据（24小时柱状图）
 *    - 多时间维度数据聚合（日/周/月趋势）
 * 
 * 3. 【奖罚机制】★核心特性★
 *    - 基于昨日数据的奖励/惩罚状态管理
 *    - 支持灵活的完成百分比输入（0%-100%）
 *    - 智能计算目标数量，避免重复计算
 *    - 多语言本地化支持（中文/英文）
 * 
 * 4. 【图表数据】
 *    - 饼图：今日使用时间分布和目标完成情况
 *    - 柱状图：24小时使用时间分布
 *    - 折线图：使用趋势、完成率、奖罚完成度
 * 
 * 5. 【计时器功能】
 *    - 线下专注计时器
 *    - 倒计时计时器
 *    - 实时状态更新和会话管理
 * 
 * == 数据架构 ==
 * 
 * 主要数据表：
 * - app_sessions_user: 原始应用使用会话记录
 * - daily_usage_user: 按日聚合的使用统计
 * - summary_usage_user: 多维度汇总统计表
 * - reward_punishment_user: 奖罚完成记录（支持百分比完成）
 * - goal_reward_punishment_user: 用户目标奖罚配置
 * - timer_sessions_user: 计时器会话记录
 * 
 * == 性能优化策略 ==
 * 
 * 1. 【缓存机制】
 *    - lastLoadedCategoryId/lastLoadedPeriod避免重复查询
 *    - StateFlow状态缓存，减少UI重建
 * 
 * 2. 【异步处理】
 *    - 所有数据库操作使用协程异步执行
 *    - viewModelScope管理生命周期
 *    - 智能错误处理和回退机制
 * 
 * 3. 【数据聚合】
 *    - 优先使用预聚合表（summary_usage_user）
 *    - 回退到原始数据计算（性能保障）
 *    - 智能数据过滤和时间范围处理
 * 
 * == 奖罚机制详解 ==
 * 
 * 奖罚系统是应用的核心功能，经过多次优化：
 * 
 * 1. 【数据源优先级】
 *    - 最高：昨日详细数据（yesterdayDetailData）
 *    - 中等：数据库中的奖罚记录
 *    - 最低：实时计算的默认值
 * 
 * 2. 【完成百分比支持】
 *    - 用户输入完成数量，自动计算百分比
 *    - 支持部分完成（如完成30个俯卧撑中的20个 = 67%）
 *    - 百分比显示在P/R按钮上
 * 
 * 3. 【本地化处理】
 *    - UnifiedTextManager统一管理多语言文本
 *    - DefaultValueLocalizer处理数值格式化
 *    - 支持中英文动态切换
 * 
 * == 重要修复历史 ==
 * 
 * 1. 修复了数值在30和60之间来回变化的问题
 * 2. 统一了奖罚数据源，确保显示一致性
 * 3. 优化了目标时间显示（解决0.0h显示问题）
 * 4. 修复了Stats页面完成百分比显示错误
 * 5. 解决了Android 8.0+后台服务启动限制问题
 * 
 * @author OffTime Team
 * @version 2.0 - 重构版本，优化奖罚机制和数据一致性
 * @since 1.0
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    // === 核心数据仓库 ===
    private val appRepository: AppRepository,                    // 应用信息仓库
    private val appSessionRepository: AppSessionRepository,      // 会话数据仓库
    
    // === 数据访问对象 ===
    private val categoryDao: AppCategoryDao,                     // 分类数据访问
    private val appSessionUserDao: AppSessionUserDao,            // 用户会话数据访问
    private val timerSessionUserDao: TimerSessionUserDao,        // 计时器会话数据访问
    private val dailyUsageDao: DailyUsageDao,                    // 日使用数据访问
    private val summaryUsageDao: SummaryUsageDao,                // 汇总数据访问
    
    // === 目标奖罚相关DAO ===
    private val goalRewardPunishmentUserDao: GoalRewardPunishmentUserDao,          // 用户目标奖罚配置
    private val rewardPunishmentUserDao: RewardPunishmentUserDao,                  // 日奖罚记录
    private val rewardPunishmentWeekUserDao: RewardPunishmentWeekUserDao,          // 周奖罚记录
    private val rewardPunishmentMonthUserDao: RewardPunishmentMonthUserDao,        // 月奖罚记录
    
    // === 辅助工具 ===
    private val dataMigrationHelper: DataMigrationHelper,        // 数据迁移助手
    private val dataUpdateManager: DataUpdateManager,            // 数据更新管理器
    @ApplicationContext private val context: Context,            // 应用上下文
    private val repository: GoalRewardPunishmentRepository,      // 目标奖罚仓库
    private val timerSessionRepository: TimerSessionRepository,  // 计时器会话仓库
    private val userRepository: UserRepository                   // 用户仓库
) : ViewModel() {



    private val _categories = MutableStateFlow<List<AppCategoryEntity>>(emptyList())
    val categories: StateFlow<List<AppCategoryEntity>> = _categories.asStateFlow()

    private val _selectedCategory = MutableStateFlow<AppCategoryEntity?>(null)
    val selectedCategory: StateFlow<AppCategoryEntity?> = _selectedCategory.asStateFlow()

    private val _isStatisticsView = MutableStateFlow(true) // true=今日统计, false=今日详情
    val isStatisticsView: StateFlow<Boolean> = _isStatisticsView.asStateFlow()

    // 饼图数据
    private val _realUsageSec = MutableStateFlow(0)
    val realUsageSec: StateFlow<Int> = _realUsageSec.asStateFlow()

    private val _virtualUsageSec = MutableStateFlow(0)
    val virtualUsageSec: StateFlow<Int> = _virtualUsageSec.asStateFlow()

    private val _goalSec = MutableStateFlow(7200) // 默认2小时
    val goalSec: StateFlow<Int> = _goalSec.asStateFlow()

    // 奖励惩罚相关状态
    private val _rewardText = MutableStateFlow("")
    val rewardText: StateFlow<String> = _rewardText.asStateFlow()
    
    private val _punishText = MutableStateFlow("")
    val punishText: StateFlow<String> = _punishText.asStateFlow()
    
    private val _goalConditionType = MutableStateFlow(0) // 0: ≤目标算完成(娱乐类), 1: ≥目标算完成(学习类)
    val goalConditionType: StateFlow<Int> = _goalConditionType.asStateFlow()
    
    private val _yesterdayRewardDone = MutableStateFlow(true)
    val yesterdayRewardDone: StateFlow<Boolean> = _yesterdayRewardDone.asStateFlow()
    
    private val _yesterdayPunishDone = MutableStateFlow(true)
    val yesterdayPunishDone: StateFlow<Boolean> = _yesterdayPunishDone.asStateFlow()
    
    // 奖罚完成百分比
    private val _yesterdayRewardPercent = MutableStateFlow(0)
    val yesterdayRewardPercent: StateFlow<Int> = _yesterdayRewardPercent.asStateFlow()
    
    private val _yesterdayPunishPercent = MutableStateFlow(0)
    val yesterdayPunishPercent: StateFlow<Int> = _yesterdayPunishPercent.asStateFlow()
    
    // 完成度选择对话框状态
    private val _showCompletionDialog = MutableStateFlow(false)
    val showCompletionDialog: StateFlow<Boolean> = _showCompletionDialog.asStateFlow()
    
    private val _completionDialogIsReward = MutableStateFlow(true)
    val completionDialogIsReward: StateFlow<Boolean> = _completionDialogIsReward.asStateFlow()
    
    private val _completionDialogTaskDescription = MutableStateFlow("")
    val completionDialogTaskDescription: StateFlow<String> = _completionDialogTaskDescription.asStateFlow()
    
    private val _completionDialogTargetNumber = MutableStateFlow(0)
    val completionDialogTargetNumber: StateFlow<Int> = _completionDialogTargetNumber.asStateFlow()
    
    private val _yesterdayHasData = MutableStateFlow(false)
    val yesterdayHasData: StateFlow<Boolean> = _yesterdayHasData.asStateFlow()
    
    private val _yesterdayGoalMet = MutableStateFlow(false)
    val yesterdayGoalMet: StateFlow<Boolean> = _yesterdayGoalMet.asStateFlow()
    
    private val _isRewardPunishmentEnabled = MutableStateFlow(true)
    val isRewardPunishmentEnabled: StateFlow<Boolean> = _isRewardPunishmentEnabled.asStateFlow()
    
    // 昨日详细数据对话框状态
    private val _showYesterdayDetailDialog = MutableStateFlow(false)
    val showYesterdayDetailDialog: StateFlow<Boolean> = _showYesterdayDetailDialog.asStateFlow()
    
    private val _yesterdayDetailData = MutableStateFlow<YesterdayDetailData?>(null)
    val yesterdayDetailData: StateFlow<YesterdayDetailData?> = _yesterdayDetailData.asStateFlow()
    
    // 昨日详细数据类
    data class YesterdayDetailData(
        val realUsageSeconds: Int,
        val virtualUsageSeconds: Int,
        val goalSeconds: Int,
        val categoryName: String,
        val goalMet: Boolean,
        val rewardContent: String = "",
        val punishmentContent: String = "",
        val rewardCompleted: Boolean = false,
        val punishmentCompleted: Boolean = false
    )

    // 应用详情对话框状态
    private val _showAppDetailDialog = MutableStateFlow(false)
    val showAppDetailDialog: StateFlow<Boolean> = _showAppDetailDialog.asStateFlow()
    
    private val _appDetailList = MutableStateFlow<List<AppDetailItem>>(emptyList())
    val appDetailList: StateFlow<List<AppDetailItem>> = _appDetailList.asStateFlow()
    
    private val _appDetailTitle = MutableStateFlow("")
    val appDetailTitle: StateFlow<String> = _appDetailTitle.asStateFlow()

    // 柱状图数据 (24小时，每小时的分钟数)
    private val _hourlyRealUsage = MutableStateFlow<List<Int>>(List(24) { 0 })
    val hourlyRealUsage: StateFlow<List<Int>> = _hourlyRealUsage.asStateFlow()

    private val _hourlyVirtualUsage = MutableStateFlow<List<Int>>(List(24) { 0 })
    val hourlyVirtualUsage: StateFlow<List<Int>> = _hourlyVirtualUsage.asStateFlow()
    
    // 下拉刷新状态
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    // 周期选择状态 ("日", "周", "月") - 上半部分饼图使用
    private val _selectedPeriod = MutableStateFlow("日")
    val selectedPeriod: StateFlow<String> = _selectedPeriod.asStateFlow()
    
    // 下半部分折线图的周期选择状态
    private val _selectedLineChartPeriod = MutableStateFlow("日")
    val selectedLineChartPeriod: StateFlow<String> = _selectedLineChartPeriod.asStateFlow()
    
    // 折线图数据
    private val _usageLineData = MutableStateFlow<List<UsageData>>(emptyList())
    val usageLineData: StateFlow<List<UsageData>> = _usageLineData.asStateFlow()
    
    private val _completionLineData = MutableStateFlow<List<UsageData>>(emptyList())
    val completionLineData: StateFlow<List<UsageData>> = _completionLineData.asStateFlow()
    
    // 奖罚完成度数据
    private val _rewardPunishmentData = MutableStateFlow<List<RewardPunishmentData>>(emptyList())
    val rewardPunishmentData: StateFlow<List<RewardPunishmentData>> = _rewardPunishmentData.asStateFlow()
    
    // 奖罚详情统计数据
    private val _rewardPunishmentSummary = MutableStateFlow<List<RewardPunishmentSummaryData>>(emptyList())
    val rewardPunishmentSummary: StateFlow<List<RewardPunishmentSummaryData>> = _rewardPunishmentSummary.asStateFlow()
    
    // 奖罚详情统计数据类
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
    
    // 缓存机制 - 避免重复查询
    private var lastLoadedCategoryId: Int? = null
    private var lastLoadedPeriod: String? = null

    // 线下计时器状态管理
    private val _showTimerDialog = MutableStateFlow(false)
    val showTimerDialog: StateFlow<Boolean> = _showTimerDialog.asStateFlow()
    
    private val _defaultTimerTab = MutableStateFlow(0) // 0: Timer, 1: CountDown
    val defaultTimerTab: StateFlow<Int> = _defaultTimerTab.asStateFlow()
    
    // 按分类分离的计时器状态管理
    data class CategoryTimerState(
        val isRunning: Boolean = false,
        val isPaused: Boolean = false,
        val seconds: Int = 0,
        val hours: Int = 0,
        val minutes: Int = 0,
        val secondsUnit: Int = 0,
        val sessionId: Int? = null,
        val startTime: Long = 0,
        val pausedDuration: Long = 0,
        val lastPauseTime: Long = 0,
        val timerJob: Job? = null
    )
    
    // 每个分类的计时器状态
    private val categoryTimerStates = mutableMapOf<Int, CategoryTimerState>()
    
    /**
     * 获取指定分类的计时器状态
     */
    private fun getCategoryTimerState(categoryId: Int): CategoryTimerState {
        return categoryTimerStates[categoryId] ?: CategoryTimerState()
    }
    
    /**
     * 更新指定分类的计时器状态
     */
    private fun updateCategoryTimerState(categoryId: Int, state: CategoryTimerState) {
        categoryTimerStates[categoryId] = state
        
        // 如果是当前选中的分类，同步更新UI状态
        if (_selectedCategory.value?.id == categoryId) {
            updateCurrentTimerUIState(state)
        }
    }
    
    /**
     * 更新当前显示的计时器UI状态
     */
    private fun updateCurrentTimerUIState(state: CategoryTimerState) {
        _isTimerRunning.value = state.isRunning
        _isTimerPaused.value = state.isPaused
        _timerSeconds.value = state.seconds
        _timerHours.value = state.hours
        _timerMinutes.value = state.minutes
        _timerSecondsUnit.value = state.secondsUnit
    }
    
    // 当前显示的计时器状态（基于选中的分类）
    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()
    
    private val _isTimerPaused = MutableStateFlow(false)
    val isTimerPaused: StateFlow<Boolean> = _isTimerPaused.asStateFlow()
    
    private val _timerSeconds = MutableStateFlow(0)
    val timerSeconds: StateFlow<Int> = _timerSeconds.asStateFlow()
    
    private val _timerHours = MutableStateFlow(0)
    val timerHours: StateFlow<Int> = _timerHours.asStateFlow()
    
    private val _timerMinutes = MutableStateFlow(0)
    val timerMinutes: StateFlow<Int> = _timerMinutes.asStateFlow()
    
    private val _timerSecondsUnit = MutableStateFlow(0)
    val timerSecondsUnit: StateFlow<Int> = _timerSecondsUnit.asStateFlow()
    
    // 全局计时器管理（保留兼容性）
    private var timerJob: Job? = null
    private var timerStartTime: Long = 0
    private var pausedDuration: Long = 0 // 累计暂停的时间
    private var lastPauseTime: Long = 0
    private var currentTimerSessionId: Int? = null
    private var currentTimingCategoryId: Int? = null // 当前正在计时的分类ID

    // 定时计时器状态管理
    private val _showCountdownDialog = MutableStateFlow(false)
    val showCountdownDialog: StateFlow<Boolean> = _showCountdownDialog.asStateFlow()
    
    private val _isCountdownTimerRunning = MutableStateFlow(false)
    val isCountdownTimerRunning: StateFlow<Boolean> = _isCountdownTimerRunning.asStateFlow()
    
    private val _isCountdownTimerPaused = MutableStateFlow(false)
    val isCountdownTimerPaused: StateFlow<Boolean> = _isCountdownTimerPaused.asStateFlow()
    
    private val _countdownTimerSeconds = MutableStateFlow(1800) // 默认30分钟 = 1800秒
    val countdownTimerSeconds: StateFlow<Int> = _countdownTimerSeconds.asStateFlow()
    
    private val _countdownHours = MutableStateFlow(0)
    val countdownHours: StateFlow<Int> = _countdownHours.asStateFlow()
    
    private val _countdownMinutes = MutableStateFlow(30)
    val countdownMinutes: StateFlow<Int> = _countdownMinutes.asStateFlow()
    
    private val _countdownSecondsUnit = MutableStateFlow(0)
    val countdownSecondsUnit: StateFlow<Int> = _countdownSecondsUnit.asStateFlow()
    
    private val _countdownInitialMinutes = MutableStateFlow(30)
    val countdownInitialMinutes: StateFlow<Int> = _countdownInitialMinutes.asStateFlow()
    
    private var countdownJob: Job? = null
    private var countdownStartTime: Long = 0
    private var countdownPausedDuration: Long = 0 // 累计暂停的时间
    private var lastCountdownPauseTime: Long = 0
    private var currentCountdownSessionId: Int? = null
    private var currentCountdownTimingCategoryId: Int? = null // 当前正在定时的分类ID
    private var initialCountdownSeconds: Int = 1800 // 初始设定的倒计时秒数

    // 背景计时状态管理
    private val _isTimerInBackground = MutableStateFlow(false)
    val isTimerInBackground: StateFlow<Boolean> = _isTimerInBackground.asStateFlow()
    
    private val _isCountdownInBackground = MutableStateFlow(false)
    val isCountdownInBackground: StateFlow<Boolean> = _isCountdownInBackground.asStateFlow()

    private val _isPunishLoading = MutableStateFlow(false)
    val isPunishLoading: StateFlow<Boolean> = _isPunishLoading.asStateFlow()

    /**
     * 设置过滤后的奖罚数据
     */
    private suspend fun setFilteredRewardPunishmentData(data: List<RewardPunishmentData>, categoryId: Int) {
        // 检查该分类的奖罚开关是否开启
        val enabledMap = appRepository.getCategoryRewardPunishmentEnabled()
        val isEnabled = enabledMap[categoryId] ?: true // 默认开启
        
        if (isEnabled) {
            // 开关开启时，显示正常数据
            _rewardPunishmentData.value = data
        } else {
            // 开关关闭时，将数据替换为-1（表示不显示）
            val filteredData = data.map { item ->
                item.copy(
                    rewardValue = -1f,
                    punishmentValue = -1f
                )
            }
            _rewardPunishmentData.value = filteredData
        }
        
        android.util.Log.d("HomeViewModel", "奖罚数据过滤: categoryId=$categoryId, enabled=$isEnabled, dataSize=${data.size}")
    }

    // 各分类使用时间数据（用于总使用分类的多分类显示）
    private val _categoryUsageData = MutableStateFlow<List<CategoryUsageItem>>(emptyList())
    val categoryUsageData: StateFlow<List<CategoryUsageItem>> = _categoryUsageData.asStateFlow()
    
    private val _categoryHourlyData = MutableStateFlow<List<CategoryHourlyItem>>(emptyList())
    val categoryHourlyData: StateFlow<List<CategoryHourlyItem>> = _categoryHourlyData.asStateFlow()
    
    // 数据更新事件监听
    private fun observeDataUpdates() {
        viewModelScope.launch {
            dataUpdateManager.dataUpdateFlow.collect { event ->
                android.util.Log.d("HomeViewModel", "收到数据更新事件: ${event.updateType}")
                
                // 刷新当前选中分类的数据
                _selectedCategory.value?.let { category ->
                    loadUsageData(category.id)
                    loadCategoryGoal(category.id)
                    loadRewardPunishmentSummary()
                }
            }
        }
    }
    
    /**
     * 手动刷新当前数据
     * 用于用户操作后（如线下计时结束）需要立即看到最新数据的场景
     */
    fun refreshCurrentData() {
        viewModelScope.launch {
            try {
                android.util.Log.d("HomeViewModel", "手动刷新当前数据")
                
                // 触发数据更新
                com.offtime.app.service.UnifiedUpdateService.triggerManualUpdate(context)
                
                // 短暂延迟后刷新UI数据
                kotlinx.coroutines.delay(2000)
                
                _selectedCategory.value?.let { category ->
                    loadUsageData(category.id)
                    loadCategoryGoal(category.id)
                    loadRewardPunishmentSummary()
                }
                
                android.util.Log.d("HomeViewModel", "手动刷新完成")
                
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "手动刷新失败", e)
            }
        }
    }
    
    /**
     * 下拉刷新处理方法
     * 执行完整的数据更新流程：基础数据 → 聚合数据 → UI更新
     */
    fun onSwipeRefresh() {
        viewModelScope.launch {
            try {
                android.util.Log.d("HomeViewModel", "🔄 开始下拉刷新")
                _isRefreshing.value = true
                
                // 触发统一更新服务进行完整的数据更新
                com.offtime.app.service.UnifiedUpdateService.triggerManualUpdate(context)
                
                // 等待数据更新完成（包含基础数据→聚合数据→UI的完整流程）
                kotlinx.coroutines.delay(3000)
                
                // 刷新当前页面的所有数据
                _selectedCategory.value?.let { category ->
                    loadUsageData(category.id)
                    loadCategoryGoal(category.id)
                    loadRewardPunishmentSummary()
                }
                
                // 重新加载分类列表（以防有新增或删除）
                loadCategories()
                
                android.util.Log.d("HomeViewModel", "✅ 下拉刷新完成")
                
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "❌ 下拉刷新失败", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }
    
    init {
        loadInitialData()
        observeDataUpdates()
    }
    


    fun loadInitialData() {
        loadCategories()
        // Load additional data as needed
    }

    fun loadCategories() {
        viewModelScope.launch {
            categoryDao.getAllCategories().collect { categoriesList ->
                // 获取所有分类（排除统计已从数据库中删除）
                val filteredCategories = categoriesList
                _categories.value = filteredCategories
                
                // 使用默认显示类别或第一个分类
                if (filteredCategories.isNotEmpty() && _selectedCategory.value == null) {
                    // 暂时使用第一个分类作为默认，后续会添加设置功能
                    selectCategory(filteredCategories.first())
                }
            }
        }
    }

    fun selectCategory(category: AppCategoryEntity) {
        // 避免重复选择同一个分类
        if (_selectedCategory.value?.id == category.id) {
            return
        }
        
        _selectedCategory.value = category
        
        // 更新当前显示的计时器状态为该分类的状态
        val categoryTimerState = getCategoryTimerState(category.id)
        updateCurrentTimerUIState(categoryTimerState)
        
        // 从数据库查询该分类的目标时间
        loadCategoryGoal(category.id)
        // 加载奖罚开关状态
        loadRewardPunishmentEnabled(category.id)
        // 立即加载该分类的数据
        loadUsageData(category.id)
        // 加载奖罚详情统计
        loadRewardPunishmentSummary()
        
        android.util.Log.d("HomeViewModel", "已选择分类: ${category.name}, ID: ${category.id}, Timer状态: 运行=${categoryTimerState.isRunning}, 暂停=${categoryTimerState.isPaused}")
    }
    
    private fun loadCategoryGoal(categoryId: Int) {
        viewModelScope.launch {
            try {
                val goal = goalRewardPunishmentUserDao.getUserGoalByCatId(categoryId)
                if (goal != null) {
                    // 将分钟转换为秒
                    _goalSec.value = goal.dailyGoalMin * 60
                    // 设置目标类型
                    _goalConditionType.value = goal.conditionType
                    
                    // 奖罚模块只显示昨日数据，不再需要今日奖惩文本更新
                    // 直接加载昨日奖罚数据（确保奖罚模块显示昨日数据）
                    loadYesterdayRewardPunishmentStatus(categoryId, getYesterdayDate())
                    
                    android.util.Log.d("HomeViewModel", "分类 $categoryId 的目标时间: ${goal.dailyGoalMin}分钟 = ${_goalSec.value}秒, 类型: ${goal.conditionType}")
                } else {
                    // 如果没有找到目标，使用默认值，但不覆盖已有的正确计算结果
                    _goalSec.value = 7200 // 2小时
                    _goalConditionType.value = 0 // 默认为娱乐类
                    
                    // 只在奖惩文本为空或默认值时才设置，避免覆盖正确的计算结果
                    if (_rewardText.value.isBlank() || _rewardText.value.contains("薯片")) {
                        _rewardText.value = DefaultValueLocalizer.localizeRewardDescription(context, "薯片", 1, "包")
                    }
                    if (_punishText.value.isBlank() || _punishText.value.contains("俯卧撑")) {
                        _punishText.value = DefaultValueLocalizer.localizePunishmentDescription(context, "俯卧撑", 30, "个")
                    }
                    
                    android.util.Log.w("HomeViewModel", "⚠️ 分类 $categoryId 没有找到目标，使用默认值2小时，类型0(娱乐类)")
                    android.util.Log.w("HomeViewModel", "  当前奖惩文本: reward='${_rewardText.value}', punish='${_punishText.value}'")
                    android.util.Log.w("HomeViewModel", "  避免覆盖已有的正确计算结果")
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "❌ 查询分类目标失败: ${e.message}", e)
                _goalSec.value = 7200 // 出错时使用默认值
                _goalConditionType.value = 0 // 默认为娱乐类
                
                // 只在奖惩文本为空或默认值时才设置，避免覆盖正确的计算结果
                if (_rewardText.value.isBlank() || _rewardText.value.contains("薯片")) {
                    _rewardText.value = DefaultValueLocalizer.localizeRewardDescription(context, "薯片", 1, "包")
                }
                if (_punishText.value.isBlank() || _punishText.value.contains("俯卧撑")) {
                    _punishText.value = DefaultValueLocalizer.localizePunishmentDescription(context, "俯卧撑", 30, "个")
                }
                
                android.util.Log.e("HomeViewModel", "  异常时当前奖惩文本: reward='${_rewardText.value}', punish='${_punishText.value}'")
                android.util.Log.e("HomeViewModel", "  避免覆盖已有的正确计算结果")
            }
        }
    }



    /**
     * 获取今日日期字符串
     */
    private fun getTodayDate(): String {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return dateFormat.format(java.util.Date())
    }



    /**
     * 计算具体的奖励内容 - 核心奖励逻辑实现
     * 
     * 根据不同分类类型和使用情况计算实际奖励数量：
     * - 娱乐类（conditionType=0）：少用时间根据结构化数据计算奖励（例如少用1小时奖励1包薯片）
     * - 学习/健身类（conditionType=1）：超额完成根据结构化数据计算奖励
     * 
     * @param categoryId 分类ID，用于查询具体的奖惩配置
     * @param usageSeconds 实际使用时间（秒）
     * @param goalSeconds 目标时间（秒）
     * @param baseRewardText 基础奖励文本（作为fallback使用）
     * @param conditionType 完成条件类型：0=≤目标算完成（娱乐类），1=≥目标算完成（学习/健身类）
     * @return 计算后的奖励文本，格式如"薯片2包"或基础文本
     */
    private suspend fun calculateRewardText(categoryId: Int, usageSeconds: Int, goalSeconds: Int, baseRewardText: String, conditionType: Int): String {
        return try {
            // 获取目标配置以获取结构化数据
            val goal = goalRewardPunishmentUserDao.getUserGoalByCatId(categoryId)
            
            when (conditionType) {
                0 -> {
                    // 娱乐类：≤目标算完成，少用时间有奖励
                    if (usageSeconds <= goalSeconds) {
                        val savedSeconds = goalSeconds - usageSeconds
                        
                        if (goal != null && goal.rewardNumber > 0 && savedSeconds > 0) {
                            // 根据奖励时间单位计算奖励倍数
                            val timeUnitSeconds = when (goal.rewardTimeUnit) {
                                "小时" -> 3600
                                "分钟" -> 60
                                "秒" -> 1
                                else -> 3600 // 默认小时
                            }
                            
                            // 向上取整：任何节省时间都能获得奖励
                            val savedTimeUnits = kotlin.math.ceil(savedSeconds.toDouble() / timeUnitSeconds.toDouble()).toInt()
                            val rewardCount = savedTimeUnits * goal.rewardNumber
                            // 直接使用数据库中的标准文本，不进行本地化处理
                            DefaultValueLocalizer.localizeRewardDescription(context, goal.rewardText, rewardCount, goal.rewardUnit)
                        } else {
                            // 刚好达标或没有额外奖励配置，使用基础奖励
                            if (goal != null && goal.rewardNumber > 0) {
                                // 直接使用数据库中的标准文本，不进行本地化处理
                                DefaultValueLocalizer.localizeRewardDescription(context, goal.rewardText, goal.rewardNumber, goal.rewardUnit)
                            } else {
                                DefaultValueLocalizer.localizeRewardDescription(context, "薯片", 1, "包")
                            }
                        }
                    } else {
                        // 未达标，无奖励
                        baseRewardText
                    }
                }
                1 -> {
                    // 学习/健身类：≥目标算完成，超额完成有奖励
                    if (usageSeconds >= goalSeconds) {
                        val extraSeconds = usageSeconds - goalSeconds
                        
                        if (goal != null && goal.rewardNumber > 0 && extraSeconds > 0) {
                            // 根据奖励时间单位计算奖励倍数
                            val timeUnitSeconds = when (goal.rewardTimeUnit) {
                                "小时" -> 3600
                                "分钟" -> 60
                                "秒" -> 1
                                else -> 3600 // 默认小时
                            }
                            
                            // 向上取整：任何超额时间都能获得奖励
                            val extraTimeUnits = kotlin.math.ceil(extraSeconds.toDouble() / timeUnitSeconds.toDouble()).toInt()
                            val rewardCount = extraTimeUnits * goal.rewardNumber
                            // 直接使用数据库中的标准文本，不进行本地化处理
                            DefaultValueLocalizer.localizeRewardDescription(context, goal.rewardText, rewardCount, goal.rewardUnit)
                        } else {
                            // 刚好达标或没有额外奖励配置，使用基础奖励
                            if (goal != null && goal.rewardNumber > 0) {
                                // 直接使用数据库中的标准文本，不进行本地化处理
                                DefaultValueLocalizer.localizeRewardDescription(context, goal.rewardText, goal.rewardNumber, goal.rewardUnit)
                            } else {
                                DefaultValueLocalizer.localizeRewardDescription(context, "薯片", 1, "包")
                            }
                        }
                    } else {
                        // 未达标，无奖励
                        baseRewardText
                    }
                }
                else -> baseRewardText
            }
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "计算奖励内容失败: ${e.message}", e)
            baseRewardText
        }
    }

    /**
     * 计算具体的惩罚内容 - 核心奖惩逻辑实现
     * 
     * 根据不同分类类型和使用情况计算实际惩罚数量：
     * - 娱乐类（conditionType=0）：超出目标时间根据结构化数据计算惩罚（例如每小时30个俯卧撑）
     * - 学习/健身类（conditionType=1）：未达标根据差值计算惩罚
     * 
     * @param categoryId 分类ID，用于查询具体的奖惩配置
     * @param usageSeconds 实际使用时间（秒）
     * @param goalSeconds 目标时间（秒）
     * @param basePunishText 基础惩罚文本（作为fallback使用）
     * @param conditionType 完成条件类型：0=≤目标算完成（娱乐类），1=≥目标算完成（学习/健身类）
     * @return 计算后的惩罚文本，格式如"俯卧撑60个"或基础文本
     */
    private suspend fun calculatePunishmentText(categoryId: Int, usageSeconds: Int, goalSeconds: Int, basePunishText: String, conditionType: Int): String {
        return try {
            // 获取目标配置以获取结构化数据
            val goal = goalRewardPunishmentUserDao.getUserGoalByCatId(categoryId)
            android.util.Log.d("HomeViewModel", "=== calculatePunishmentText Debug ===")
            android.util.Log.d("HomeViewModel", "categoryId=$categoryId, usageSeconds=$usageSeconds, goalSeconds=$goalSeconds")
            android.util.Log.d("HomeViewModel", "goal=${goal?.punishText}, punishNumber=${goal?.punishNumber}, punishUnit=${goal?.punishUnit}")
            android.util.Log.d("HomeViewModel", "punishTimeUnit=${goal?.punishTimeUnit}, conditionType=$conditionType")
            
            when (conditionType) {
                0 -> {
                    // 娱乐类：≤目标算完成
                    if (usageSeconds > goalSeconds) {
                        // 超出目标时间，计算具体惩罚
                        val overSeconds = usageSeconds - goalSeconds
                        
                        if (goal != null && goal.punishNumber > 0) {
                            // 根据惩罚时间单位计算惩罚倍数
                            val timeUnitSeconds = when (goal.punishTimeUnit) {
                                "小时" -> 3600
                                "分钟" -> 60
                                "秒" -> 1
                                else -> 3600 // 默认小时
                            }
                            
                            val overTimeUnits = kotlin.math.ceil(overSeconds.toDouble() / timeUnitSeconds.toDouble()).toInt()
                            val punishmentCount = overTimeUnits * goal.punishNumber
                            // 直接使用数据库中的标准文本，不进行本地化处理
                            DefaultValueLocalizer.localizePunishmentDescription(context, goal.punishText, punishmentCount, goal.punishUnit)
                        } else {
                            // 回退到默认计算（每小时30个俯卧撑）
                            val overHours = kotlin.math.ceil(overSeconds.toDouble() / 3600.0).toInt()
                            val punishmentCount = overHours * 30
                            DefaultValueLocalizer.localizePunishmentDescription(context, "俯卧撑", punishmentCount, "个")
                        }
                    } else {
                        // 未超出目标，无惩罚
                        basePunishText
                    }
                }
                1 -> {
                    // 学习/健身类：≥目标算完成，未达标根据差值计算惩罚
                    android.util.Log.d("HomeViewModel", "学习/健身类别惩罚计算开始")
                    if (usageSeconds < goalSeconds) {
                        val shortSeconds = goalSeconds - usageSeconds
                        android.util.Log.d("HomeViewModel", "未达标: 差值=${shortSeconds}秒")
                        
                        if (goal != null && goal.punishNumber > 0) {
                            android.util.Log.d("HomeViewModel", "使用目标配置: punishNumber=${goal.punishNumber}, punishTimeUnit=${goal.punishTimeUnit}")
                            // 根据惩罚时间单位计算惩罚倍数
                            val timeUnitSeconds = when (goal.punishTimeUnit) {
                                "小时" -> 3600
                                "分钟" -> 60
                                "秒" -> 1
                                else -> 3600 // 默认小时
                            }
                            
                            // 向上取整：哪怕少做1秒也算少做1个时间单位
                            val shortTimeUnits = kotlin.math.ceil(shortSeconds.toDouble() / timeUnitSeconds.toDouble()).toInt()
                            val punishmentCount = shortTimeUnits * goal.punishNumber
                            android.util.Log.d("HomeViewModel", "时间单位=${timeUnitSeconds}秒, 缺少时间单位=${shortTimeUnits}, 最终惩罚数量=${punishmentCount}")
                            // 直接使用数据库中的标准文本，不进行本地化处理
                            val result = DefaultValueLocalizer.localizePunishmentDescription(context, goal.punishText, punishmentCount, goal.punishUnit)
                            android.util.Log.d("HomeViewModel", "本地化结果: '$result'")
                            result
                        } else {
                            android.util.Log.d("HomeViewModel", "目标配置无效，使用默认计算: goal=$goal, punishNumber=${goal?.punishNumber}")
                            // 回退到默认计算（每小时30个俯卧撑）
                            val shortHours = kotlin.math.ceil(shortSeconds.toDouble() / 3600.0).toInt()
                            val punishmentCount = shortHours * 30
                            android.util.Log.d("HomeViewModel", "默认计算: 缺少${shortHours}小时, 惩罚${punishmentCount}个俯卧撑")
                            DefaultValueLocalizer.localizePunishmentDescription(context, "俯卧撑", punishmentCount, "个")
                        }
                    } else {
                        android.util.Log.d("HomeViewModel", "已达标，无惩罚")
                        // 达标，无惩罚
                        basePunishText
                    }
                }
                else -> basePunishText
            }
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "计算惩罚内容失败: ${e.message}", e)
            basePunishText
        }
    }

    private fun loadRewardPunishmentEnabled(categoryId: Int) {
        viewModelScope.launch {
            try {
                val enabledMap = appRepository.getCategoryRewardPunishmentEnabled()
                val isEnabled = enabledMap[categoryId] ?: true // 默认开启
                _isRewardPunishmentEnabled.value = isEnabled
                android.util.Log.d("HomeViewModel", "分类 $categoryId 的奖罚开关状态: $isEnabled")
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "查询分类奖罚开关状态失败: ${e.message}", e)
                _isRewardPunishmentEnabled.value = true // 出错时默认开启
            }
        }
    }

    fun setStatisticsView(isStatistics: Boolean) {
        _isStatisticsView.value = isStatistics
    }
    
    fun setPeriod(period: String) {
        _selectedPeriod.value = period
        // 上半部分饼图的周期改变，这里暂时不重新加载折线图数据
        // 折线图数据由下半部分的周期选择单独控制
    }
    
    fun setLineChartPeriod(period: String) {
        _selectedLineChartPeriod.value = period
        // 重新加载折线图数据
        _selectedCategory.value?.let { category ->
            loadLineChartData(category.id, period)
        }
    }
    
    fun refreshData() {
        _selectedCategory.value?.let { category ->
            android.util.Log.d("HomeViewModel", "手动刷新数据: 选中分类=${category.name}, ID=${category.id}")
            loadUsageData(category.id)
            
            // 更新锁屏小部件
            try {
                com.offtime.app.widget.WidgetUpdateManager.updateAllLockScreenWidgets(context)
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "更新锁屏小部件失败", e)
            }
        } ?: run {
            android.util.Log.e("HomeViewModel", "刷新数据失败: 没有选中的分类")
        }
    }
    
    /**
     * 获取当前页面的布局信息报告
     */
    fun getLayoutReport(): String {
        return try {
            com.offtime.app.utils.LayoutMetricsManager.generateLayoutReport()
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "生成布局报告失败: ${e.message}", e)
            "布局报告生成失败: ${e.message}"
        }
    }
    
    /**
     * 打印布局报告到日志
     */
    fun printLayoutReport() {
        try {
            com.offtime.app.utils.LayoutMetricsManager.printLayoutReport()
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "打印布局报告失败: ${e.message}", e)
        }
    }
    
    // 临时调试方法：直接设置测试数据
    fun setTestData() {
        viewModelScope.launch {
            try {
                android.util.Log.d("HomeViewModel", "开始设置测试数据")
                
                // 从数据库获取所有数据
                val allData = dailyUsageDao.getAllDailyUsageData()
                android.util.Log.d("HomeViewModel", "数据库中总共有 ${allData.size} 条记录")
                
                allData.forEach { data ->
                    android.util.Log.d("HomeViewModel", "记录: id=${data.id}, catId=${data.catId}, date=${data.date}, hour=${data.slotIndex}, isOffline=${data.isOffline}, duration=${data.durationSec}s")
                }
                
                val selectedCatId = _selectedCategory.value?.id ?: run {
            // 动态查找健身分类ID，避免硬编码
            val categories = _categories.value
            val fitnessCategory = categories.find { it.name == "健身" }
            fitnessCategory?.id ?: 3 // 如果找不到健身分类，使用默认值3
        }
                android.util.Log.d("HomeViewModel", "当前选中分类ID: $selectedCatId")
                
                // 筛选当前分类的数据
                val categoryData = allData.filter { it.catId == selectedCatId }
                android.util.Log.d("HomeViewModel", "分类 $selectedCatId 的数据有 ${categoryData.size} 条")
                
                if (categoryData.isNotEmpty()) {
                    val totalReal = categoryData.filter { it.isOffline == 0 }.sumOf { it.durationSec }
                    val totalVirtual = categoryData.filter { it.isOffline == 1 }.sumOf { it.durationSec }
                    
                    android.util.Log.d("HomeViewModel", "计算结果: totalReal=${totalReal}s, totalVirtual=${totalVirtual}s")
                    
                    _realUsageSec.value = totalReal
                    _virtualUsageSec.value = totalVirtual
                    
                    android.util.Log.d("HomeViewModel", "已更新状态: realUsageSec=${_realUsageSec.value}, virtualUsageSec=${_virtualUsageSec.value}")
                } else {
                    android.util.Log.w("HomeViewModel", "没有找到分类 $selectedCatId 的数据")
                    _realUsageSec.value = 0
                    _virtualUsageSec.value = 0
                }
                
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "设置测试数据失败", e)
            }
        }
    }
    
    // 修复分类ID不匹配问题 - 使用新的迁移模块 + 重新聚合
    fun fixCategoryIdMismatch() {
        viewModelScope.launch {
            try {
                android.util.Log.d("HomeViewModel", "🔧 开始完整的数据修复流程")
                
                // 1. 检查是否需要迁移
                if (!dataMigrationHelper.needsMigration()) {
                    android.util.Log.d("HomeViewModel", "✅ 数据已经正确，无需迁移")
                    return@launch
                }
                
                android.util.Log.d("HomeViewModel", "⚠️ 检测到数据错误，开始修复...")
                
                // 2. 停止自动聚合（避免在修复过程中生成新的错误数据）
                android.util.Log.d("HomeViewModel", "⏸️ 暂停数据聚合...")
                
                // 3. 执行数据迁移
                android.util.Log.d("HomeViewModel", "🔄 开始数据迁移...")
                val result = dataMigrationHelper.executeFullMigration()
                
                if (result.success) {
                    android.util.Log.d("HomeViewModel", "✅ 数据迁移成功!")
                    android.util.Log.d("HomeViewModel", "📊 迁移统计:")
                    android.util.Log.d("HomeViewModel", "  - daily_usage: ${result.dailyUsageMigrated} 条")
                    android.util.Log.d("HomeViewModel", "  - app_sessions: ${result.appSessionsMigrated} 条")
                    android.util.Log.d("HomeViewModel", "  - timer_sessions: ${result.timerSessionsMigrated} 条")
                    android.util.Log.d("HomeViewModel", "  - summary_usage: ${result.summaryUsageMigrated} 条")
                    android.util.Log.d("HomeViewModel", "  - 总计: ${result.totalMigrated} 条")
                    
                    // 4. 清理所有聚合数据，准备重新聚合
                    android.util.Log.d("HomeViewModel", "🧹 清理错误的聚合数据...")
                    
                    // 删除今天的聚合数据（daily_usage_user 和 summary_usage_user）
                    val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                    
                    // 只删除错误ID的记录
                    try {
                        // 清理错误的daily_usage_user记录（catId=6的记录）
                        val allDailyData = dailyUsageDao.getAllDailyUsageData()
                        val wrongDailyData = allDailyData.filter { it.catId == 6 && it.date == today }
                        android.util.Log.d("HomeViewModel", "发现 ${wrongDailyData.size} 条错误的daily_usage记录需要清理")
                        
                        // 清理错误的summary_usage记录
                        val allSummaryData = summaryUsageDao.getAllSummaryUsageUser()
                        val wrongSummaryData = allSummaryData.filter { it.catId == 6 && it.date == today }
                        android.util.Log.d("HomeViewModel", "发现 ${wrongSummaryData.size} 条错误的summary_usage记录需要清理")
                        
                    } catch (e: Exception) {
                        android.util.Log.e("HomeViewModel", "清理聚合数据时出错", e)
                    }
                    
                    // 5. 触发重新聚合
                    android.util.Log.d("HomeViewModel", "🔄 触发数据重新聚合...")
                    try {
                        val intent = android.content.Intent(context, com.offtime.app.service.DataAggregationService::class.java)
                        intent.action = com.offtime.app.service.DataAggregationService.ACTION_AGGREGATE_DATA
                        context.startService(intent)
                        android.util.Log.d("HomeViewModel", "✅ 数据聚合服务已启动")
                    } catch (e: Exception) {
                        android.util.Log.e("HomeViewModel", "启动聚合服务失败", e)
                    }
                    
                    // 6. 等待聚合完成，然后重新加载数据
                    kotlinx.coroutines.delay(3000) // 等待3秒让聚合完成
                    
                    android.util.Log.d("HomeViewModel", "🔄 重新加载UI数据...")
                    loadCategories()
                    
                    // 如果当前选择的是健身分类，重新加载数据
                    if (_selectedCategory.value?.name == "健身") {
                        categoryDao.getAllCategories().collect { categories ->
                            val fitnessCategory = categories.find { it.name == "健身" }
                            fitnessCategory?.let { 
                                loadUsageData(it.id)
                                android.util.Log.d("HomeViewModel", "✅ 健身分类数据已重新加载")
                            }
                        }
                    }
                    
                    android.util.Log.d("HomeViewModel", "🎉 数据修复流程完成！")
                    
                } else {
                    android.util.Log.e("HomeViewModel", "❌ 数据迁移失败: ${result.errorMessage}")
                }
                
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "❌ 修复过程中发生异常", e)
            }
        }
    }
    
    // 显示迁移报告
    fun showMigrationReport() {
        viewModelScope.launch {
            try {
                android.util.Log.d("HomeViewModel", "===== 数据迁移状态报告 =====")
                
                val report = dataMigrationHelper.getMigrationReport()
                
                android.util.Log.d("HomeViewModel", "")
                android.util.Log.d("HomeViewModel", "=== 分类映射 ===")
                report.categoryMapping.forEach { (id, name) ->
                    android.util.Log.d("HomeViewModel", "ID=$id -> $name")
                }
                
                android.util.Log.d("HomeViewModel", "")
                android.util.Log.d("HomeViewModel", "=== 各表数据分布 ===")
                android.util.Log.d("HomeViewModel", "daily_usage_user:")
                report.dailyUsageByCategory.forEach { (catId, count) ->
                    val catName = report.categoryMapping[catId] ?: "未知分类"
                    android.util.Log.d("HomeViewModel", "  分类ID=$catId ($catName): $count 条记录")
                }
                
                android.util.Log.d("HomeViewModel", "app_sessions_users:")
                report.appSessionsByCategory.forEach { (catId, count) ->
                    val catName = report.categoryMapping[catId] ?: "未知分类"
                    android.util.Log.d("HomeViewModel", "  分类ID=$catId ($catName): $count 条记录")
                }
                
                android.util.Log.d("HomeViewModel", "timer_sessions_users:")
                report.timerSessionsByCategory.forEach { (catId, count) ->
                    val catName = report.categoryMapping[catId] ?: "未知分类"
                    android.util.Log.d("HomeViewModel", "  分类ID=$catId ($catName): $count 条记录")
                }
                
                android.util.Log.d("HomeViewModel", "summary_usage_user:")
                report.summaryUsageByCategory.forEach { (catId, count) ->
                    val catName = report.categoryMapping[catId] ?: "未知分类"
                    android.util.Log.d("HomeViewModel", "  分类ID=$catId ($catName): $count 条记录")
                }
                
                if (report.invalidCategoryIds.isNotEmpty()) {
                    android.util.Log.e("HomeViewModel", "")
                    android.util.Log.e("HomeViewModel", "❌ 发现无效的分类ID: ${report.invalidCategoryIds}")
                    android.util.Log.e("HomeViewModel", "这些ID在AppCategory_Users表中不存在，需要迁移修复")
                } else {
                    android.util.Log.d("HomeViewModel", "")
                    android.util.Log.d("HomeViewModel", "✅ 所有分类ID都有效")
                }
                
                android.util.Log.d("HomeViewModel", "")
                android.util.Log.d("HomeViewModel", "===== 报告结束 =====")
                
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "生成迁移报告失败", e)
            }
        }
    }
    

    
    fun loadUsageData(categoryId: Int) {
        viewModelScope.launch {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = formatter.format(Date())
            
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val yesterday = formatter.format(calendar.time)
            
            android.util.Log.d("HomeViewModel", "开始加载使用数据: categoryId=$categoryId, today=$today")
            
            // 确保加载该分类的目标时间
            loadCategoryGoal(categoryId)
            
            // 加载今日使用数据
            loadTodayUsageData(categoryId, today)
            
            // 加载昨日奖励惩罚状态（临时使用假数据）
            loadYesterdayRewardPunishmentStatus(categoryId, yesterday)
            
            // 加载24小时详情数据
            loadHourlyUsageData(categoryId, today)
            
            // 加载折线图数据 - 使用下半部分的周期选择
            loadLineChartData(categoryId, _selectedLineChartPeriod.value)
        }
    }

    private suspend fun loadTodayUsageData(categoryId: Int, date: String) {
        try {
            android.util.Log.d("HomeViewModel", "查询指定日期数据: catId=$categoryId, date=$date")
            
            // 检查是否为"总使用"分类
            val category = categoryDao.getAllCategoriesList().find { it.id == categoryId }
            
            if (category?.name == "总使用") {
                // 总使用分类：从聚合表获取数据（已经是汇总后的数据）
                android.util.Log.d("HomeViewModel", "加载总使用分类数据")
                var realUsage = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, date, 0) ?: 0
                var virtualUsage = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, date, 1) ?: 0
                
                // 如果是今天的数据，需要加上当前活跃会话的时间
                val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                if (date == today) {
                    val activeUsage = getCurrentActiveUsage(categoryId)
                    realUsage += activeUsage
                    android.util.Log.d("HomeViewModel", "今日总使用分类加上活跃会话: +${activeUsage}s")
                }
                
                _realUsageSec.value = realUsage
                _virtualUsageSec.value = virtualUsage
                
                // 加载各分类的详细数据用于多分类显示
                loadCategoryUsageData(date)
                
                android.util.Log.d("HomeViewModel", "总使用分类数据: realUsage=${realUsage}s, virtualUsage=${virtualUsage}s")
            } else {
                // 普通分类：直接查询指定日期的数据
                var realUsage = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, date, 0) ?: 0
                var virtualUsage = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, date, 1) ?: 0
                
                // 如果是今天的数据，需要加上当前活跃会话的时间
                val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                if (date == today) {
                    val activeUsage = getCurrentActiveUsage(categoryId)
                    realUsage += activeUsage
                    android.util.Log.d("HomeViewModel", "今日普通分类加上活跃会话: +${activeUsage}s")
                }
                
                _realUsageSec.value = realUsage
                _virtualUsageSec.value = virtualUsage
                
                android.util.Log.d("HomeViewModel", "普通分类数据: realUsage=${realUsage}s, virtualUsage=${virtualUsage}s")
            }
            
            android.util.Log.d("HomeViewModel", "最终设置: realUsageSec=${_realUsageSec.value}, virtualUsageSec=${_virtualUsageSec.value}")
            
            // 注意：奖罚模块永远显示昨日数据，不需要更新今日奖惩文本
            android.util.Log.d("HomeViewModel", "数据加载完成，奖罚模块显示昨日数据")
            
            // 如果当天没有数据，记录日志但不使用历史数据
            if (_realUsageSec.value == 0 && _virtualUsageSec.value == 0) {
                android.util.Log.d("HomeViewModel", "指定日期($date)暂无使用数据，这是正常的")
            }
            
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "加载指定日期数据失败", e)
            // 出错时设置为0，不使用示例数据
            _realUsageSec.value = 0
            _virtualUsageSec.value = 0
        }
    }
    
    /**
     * 获取当前活跃会话的使用时间（针对指定分类）
     * 用于实时显示正在进行的跨日期会话时间
     */
    private suspend fun getCurrentActiveUsage(categoryId: Int): Int {
        return try {
            // 获取当前活跃的应用会话
            val activeUsage = appSessionRepository.getCurrentActiveUsageByCategory(categoryId)
            android.util.Log.d("HomeViewModel", "当前活跃会话使用时间: ${activeUsage}s (分类ID: $categoryId)")
            activeUsage
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "获取当前活跃会话使用时间失败", e)
            0
        }
    }

    /**
     * 加载昨日奖罚状态
     */
    private suspend fun loadYesterdayRewardPunishmentStatus(categoryId: Int, date: String) {
        try {
            android.util.Log.d("HomeViewModel", "查询昨日奖罚状态: categoryId=$categoryId, date=$date")
            
            // 检查是否为首次安装（通过应用安装时间判断）
            val isRecentlyInstalled = isAppInstalledToday()
            if (isRecentlyInstalled) {
                // 今天安装的应用，昨日肯定无数据
                android.util.Log.d("HomeViewModel", "检测到应用今天安装，直接设置昨日无数据状态")
                _yesterdayRewardDone.value = true   // 默认已处理
                _yesterdayPunishDone.value = true   // 默认已处理
                _yesterdayGoalMet.value = false     // 默认未完成目标
                _yesterdayHasData.value = false     // 标记昨日无数据
                return
            }
            
            // 非首次安装，确保昨天的基础汇总记录存在
            val summaryId = "${date}_${categoryId}"
            val summaryRecord = summaryUsageDao.getSummaryUsageById(summaryId)
            if (summaryRecord == null) {
                android.util.Log.w("HomeViewModel", "昨天的汇总记录不存在，先生成基础记录")
                // 生成基础记录
                com.offtime.app.service.DataAggregationService.ensureBaseRecords(context)
                // 等待生成完成
                kotlinx.coroutines.delay(1000)
            }
            
            // 1. 优先查询奖惩记录表 (reward_punishment_user表) - 主要数据源
            val rewardPunishmentRecord = rewardPunishmentUserDao.getRecordByCategoryAndDate(categoryId, date)
            
            if (rewardPunishmentRecord != null) {
                // 从奖惩记录表查询（主要数据源）
                _yesterdayRewardDone.value = rewardPunishmentRecord.rewardDone == 1
                _yesterdayPunishDone.value = rewardPunishmentRecord.punishDone == 1
                _yesterdayGoalMet.value = rewardPunishmentRecord.isGoalMet == 1
                _yesterdayHasData.value = true // 有数据
                
                // 加载完成百分比
                _yesterdayRewardPercent.value = rewardPunishmentRecord.rewardCompletionPercent
                _yesterdayPunishPercent.value = rewardPunishmentRecord.punishCompletionPercent
                
                // 总是使用昨日详细数据中的正确文本，确保一致性
                if (rewardPunishmentRecord.isGoalMet == 0) {
                    // 目标未完成，使用昨日详细数据的惩罚文本
                    updatePunishmentTextFromYesterdayDetailData(categoryId, date)
                }
                
                if (rewardPunishmentRecord.isGoalMet == 1) {
                    // 目标完成，使用昨日详细数据的奖励文本
                    updateRewardTextFromYesterdayDetailData(categoryId, date)
                }
                
                android.util.Log.d("HomeViewModel", "从奖惩记录表查询昨日奖罚状态: reward=${rewardPunishmentRecord.rewardDone}, punishment=${rewardPunishmentRecord.punishDone}, goalMet=${rewardPunishmentRecord.isGoalMet}")
            } else {
                // 2. 如果新表没有数据，需要根据昨日使用情况自动判断
                val yesterdayGoalResult = checkYesterdayGoalCompletion(categoryId, date)
                
                if (yesterdayGoalResult.hasData) {
                    // 有使用数据，自动生成奖惩记录
                    autoGenerateYesterdayRewardPunishmentRecord(categoryId, date, yesterdayGoalResult.goalCompleted)
                    
                    // 根据目标完成情况设置奖罚状态
                    _yesterdayHasData.value = true // 有数据
                    _yesterdayGoalMet.value = yesterdayGoalResult.goalCompleted
                    if (yesterdayGoalResult.goalCompleted) {
                        // 目标完成 -> 有奖励，无惩罚
                        _yesterdayRewardDone.value = false  // 奖励待领取
                        _yesterdayPunishDone.value = true   // 无惩罚需要执行（不显示惩罚模块）
                        
                        // 计算具体的奖励内容
                        updateRewardTextFromYesterdayDetailData(categoryId, date)
                    } else {
                        // 目标未完成 -> 无奖励，有惩罚
                        _yesterdayRewardDone.value = true   // 无奖励可领取（不显示奖励模块）
                        _yesterdayPunishDone.value = false  // 惩罚待执行
                        
                        // 计算具体的惩罚内容
                        updatePunishmentTextFromYesterdayDetailData(categoryId, date)
                    }
                    android.util.Log.d("HomeViewModel", "自动生成昨日奖罚记录并设置状态: goalCompleted=${yesterdayGoalResult.goalCompleted}, reward=${_yesterdayRewardDone.value}, punishment=${_yesterdayPunishDone.value}")
                    android.util.Log.d("HomeViewModel", "最终UI状态: goalMet=${_yesterdayGoalMet.value}, rewardDone=${_yesterdayRewardDone.value}, punishDone=${_yesterdayPunishDone.value}")
                } else {
                    // 没有任何数据，设置默认状态
                    _yesterdayRewardDone.value = true   // 默认已处理
                    _yesterdayPunishDone.value = true   // 默认已处理
                    _yesterdayGoalMet.value = false     // 默认未完成目标
                    _yesterdayHasData.value = false     // 标记昨日无数据
                    android.util.Log.d("HomeViewModel", "昨日无数据，设置默认奖罚状态: 全部已处理")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "查询昨日奖罚状态失败", e)
            // 出错时设置默认状态
            _yesterdayRewardDone.value = true
            _yesterdayPunishDone.value = true
            _yesterdayGoalMet.value = false
        }
    }
    
    /**
     * 自动生成昨日奖惩记录
     */
    private suspend fun autoGenerateYesterdayRewardPunishmentRecord(categoryId: Int, yesterday: String, goalCompleted: Boolean) {
        try {
            android.util.Log.d("HomeViewModel", "自动生成昨日奖惩记录: categoryId=$categoryId, date=$yesterday, goalCompleted=$goalCompleted")
            
            // 创建奖惩记录
            val record = com.offtime.app.data.entity.RewardPunishmentUserEntity(
                id = "${yesterday}_$categoryId",
                date = yesterday,
                catId = categoryId,
                isGoalMet = if (goalCompleted) 1 else 0,
                rewardDone = 0, // 初始状态：奖惩都未执行
                punishDone = 0,
                updateTime = System.currentTimeMillis()
            )
            
            // 插入记录
            rewardPunishmentUserDao.upsert(record)
            
            android.util.Log.d("HomeViewModel", "昨日奖惩记录已自动生成")
            
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "自动生成昨日奖惩记录失败", e)
        }
    }
    
    /**
     * 检查昨日目标完成情况
     */
    private suspend fun checkYesterdayGoalCompletion(categoryId: Int, yesterday: String): YesterdayGoalResult {
        return try {
            // 获取分类信息
            val category = categoryDao.getCategoryById(categoryId)
            val categoryName = category?.name ?: "未知分类"
            
            // 更精准的判断：检查是否有任何昨日的使用记录
            val hasAnyYesterdayData = checkIfHasAnyYesterdayData(categoryId, yesterday)
            
            if (hasAnyYesterdayData) {
                // 有使用记录，获取汇总数据（即使使用时间为0也要计算奖罚）
                val summaryRecord = summaryUsageDao.getSummaryUsageById("${yesterday}_${categoryId}")
                val totalUsageSeconds = summaryRecord?.totalSec ?: 0
                
                // 获取目标配置
                val goal = goalRewardPunishmentUserDao.getUserGoalByCatId(categoryId)
                
                if (goal != null) {
                    val goalSeconds = goal.dailyGoalMin * 60
                    val goalCompleted = when (goal.conditionType) {
                        0 -> totalUsageSeconds <= goalSeconds  // 娱乐类：使用时间 ≤ 目标时间算完成
                        1 -> totalUsageSeconds >= goalSeconds  // 学习类：使用时间 ≥ 目标时间算完成
                        else -> false
                    }
                    
                    android.util.Log.d("HomeViewModel", "昨日目标检查[$categoryName]: 使用${totalUsageSeconds}s(${totalUsageSeconds/60}min), 目标${goalSeconds}s(${goal.dailyGoalMin}min), 类型${goal.conditionType}, 完成$goalCompleted")
                    YesterdayGoalResult(hasData = true, goalCompleted = goalCompleted)
                } else {
                    // 有数据但无目标配置
                    android.util.Log.d("HomeViewModel", "昨日目标检查[$categoryName]: 有数据但无目标配置")
                    YesterdayGoalResult(hasData = true, goalCompleted = false)
                }
            } else {
                // 昨日真正无任何数据（程序第一次安装或昨日未启动监控）
                android.util.Log.d("HomeViewModel", "昨日目标检查[$categoryName]: 昨日真正无任何使用数据")
                YesterdayGoalResult(hasData = false, goalCompleted = false)
            }
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "检查昨日目标完成情况失败", e)
            YesterdayGoalResult(hasData = false, goalCompleted = false)
        }
    }
    
    /**
     * 检查昨日是否有任何数据记录
     * 更精准地判断"真正的无数据"vs"有监控但使用时间为0"
     */
    private suspend fun checkIfHasAnyYesterdayData(categoryId: Int, yesterday: String): Boolean {
        return try {
            // 检查多个表，确认昨日是否有任何记录
            
            // 1. 检查app_sessions表（最原始的会话记录）
            val hasSessionData = appSessionUserDao.hasAnySessionsForCategoryAndDate(categoryId, yesterday)
            if (hasSessionData) {
                android.util.Log.d("HomeViewModel", "昨日数据检查: 在app_sessions表中找到记录")
                true
            } else {
                // 2. 检查daily_usage表（日使用汇总）
                val hasDailyUsageData = dailyUsageDao.hasAnyUsageForCategoryAndDate(categoryId, yesterday)
                if (hasDailyUsageData) {
                    android.util.Log.d("HomeViewModel", "昨日数据检查: 在daily_usage表中找到记录")
                    true
                } else {
                    // 3. 检查summary_usage表（汇总记录）
                    val summaryRecord = summaryUsageDao.getSummaryUsageById("${yesterday}_${categoryId}")
                    if (summaryRecord != null) {
                        android.util.Log.d("HomeViewModel", "昨日数据检查: 在summary_usage表中找到记录")
                        true
                    } else {
                        // 如果所有表都没有记录，说明昨日真正无数据
                        android.util.Log.d("HomeViewModel", "昨日数据检查: 所有表中都未找到昨日记录，确认为真正无数据")
                        false
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "检查昨日数据记录失败", e)
            // 出错时保守处理，认为无数据
            false
        }
    }
    
    /**
     * 昨日目标检查结果
     */
    private data class YesterdayGoalResult(
        val hasData: Boolean,      // 是否有数据
        val goalCompleted: Boolean // 目标是否完成
    )
    
    /**
     * 各分类使用时间数据项（用于总使用分类的饼图显示）
     */
    data class CategoryUsageItem(
        val categoryId: Int,
        val categoryName: String,
        val realUsageSec: Int,
        val virtualUsageSec: Int,
        val color: androidx.compose.ui.graphics.Color
    )

    /**
     * 各分类24小时数据项（用于总使用分类的柱状图显示）
     */
    data class CategoryHourlyItem(
        val categoryId: Int,
        val categoryName: String,
        val hourlyRealUsage: List<Int>,  // 24小时真实使用分钟数
        val hourlyVirtualUsage: List<Int>, // 24小时虚拟使用分钟数
        val color: androidx.compose.ui.graphics.Color
    )

    /**
     * 应用详情数据项（用于饼图点击后显示应用使用详情）
     */
    data class AppDetailItem(
        val packageName: String,
        val appName: String,
        val categoryId: Int,
        val categoryName: String,
        val totalUsageSeconds: Int,
        val realUsageSeconds: Int,
        val virtualUsageSeconds: Int,
        val usagePercentage: Float // 在该分类中的使用占比
    )

    /**
     * 显示奖励完成度选择对话框
     */
    fun showRewardCompletionDialog() {
        viewModelScope.launch {
            try {
                // 首先确保昨日详细数据已经计算完成
                val categoryId = _selectedCategory.value?.id ?: return@launch
                if (_yesterdayDetailData.value == null || 
                    _yesterdayDetailData.value?.categoryName != _selectedCategory.value?.name) {
                    android.util.Log.d("HomeViewModel", "需要重新计算昨日详细数据，当前分类: ${_selectedCategory.value?.name}")
                    calculateYesterdayDetailDataOnly() // 只计算数据，不显示对话框
                }
                
                // 确保数据已经计算完成后再获取
                val yesterdayData = _yesterdayDetailData.value
                val rewardText = if (yesterdayData != null && yesterdayData.rewardContent.isNotBlank()) {
                    android.util.Log.d("HomeViewModel", "使用昨日详细数据的奖励文本: ${yesterdayData.rewardContent}")
                    yesterdayData.rewardContent
                } else {
                    android.util.Log.d("HomeViewModel", "使用当前状态的奖励文本: ${_rewardText.value}")
                    _rewardText.value
                }
                
                // 直接从昨日详细数据计算准确的奖励数量，而不是从文本提取
                val targetNumber = calculateRewardCountFromYesterdayData(categoryId)
                
                _completionDialogIsReward.value = true
                _completionDialogTaskDescription.value = rewardText
                _completionDialogTargetNumber.value = targetNumber
                _showCompletionDialog.value = true
                android.util.Log.d("HomeViewModel", "显示奖励完成度对话框: $rewardText, 从昨日详细数据计算数量: $targetNumber")
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "显示奖励对话框失败", e)
            }
        }
    }
    
    /**
     * 完成昨日奖励（带百分比）
     */
    fun completeYesterdayReward(completionPercent: Int = 100) {
        viewModelScope.launch {
            try {
                val categoryId = _selectedCategory.value?.id ?: return@launch
                val yesterday = getYesterdayDate()
                
                android.util.Log.d("HomeViewModel", "完成昨日奖励: categoryId=$categoryId, date=$yesterday, percent=$completionPercent%")
                
                // 更新reward_punishment_user表
                val existingRecord = rewardPunishmentUserDao.getRecordByCategoryAndDate(categoryId, yesterday)
                if (existingRecord != null) {
                    // 使用新的百分比更新方法
                    rewardPunishmentUserDao.updateRewardCompletion(categoryId, yesterday, completionPercent)
                    android.util.Log.d("HomeViewModel", "已更新reward_punishment_user表的奖励状态: $completionPercent%")
                } else {
                    android.util.Log.d("HomeViewModel", "reward_punishment_user表中没有对应记录，跳过更新")
                }
                
                // 更新UI状态
                _yesterdayRewardDone.value = completionPercent > 0
                _yesterdayRewardPercent.value = completionPercent
                _showCompletionDialog.value = false
                
                android.util.Log.d("HomeViewModel", "昨日奖励已完成: $completionPercent%")
                
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "完成昨日奖励失败", e)
            }
        }
    }

    /**
     * 显示惩罚完成度选择对话框
     */
    fun showPunishmentCompletionDialog() {
        viewModelScope.launch {
            try {
                // 首先确保昨日详细数据已经计算完成
                val categoryId = _selectedCategory.value?.id ?: return@launch
                if (_yesterdayDetailData.value == null || 
                    _yesterdayDetailData.value?.categoryName != _selectedCategory.value?.name) {
                    android.util.Log.d("HomeViewModel", "需要重新计算昨日详细数据，当前分类: ${_selectedCategory.value?.name}")
                    calculateYesterdayDetailDataOnly() // 只计算数据，不显示对话框
                }
                
                // 确保数据已经计算完成后再获取
                val yesterdayData = _yesterdayDetailData.value
                val punishText = if (yesterdayData != null && yesterdayData.punishmentContent.isNotBlank()) {
                    android.util.Log.d("HomeViewModel", "使用昨日详细数据的惩罚文本: ${yesterdayData.punishmentContent}")
                    yesterdayData.punishmentContent
                } else {
                    android.util.Log.d("HomeViewModel", "使用当前状态的惩罚文本: ${_punishText.value}")
                    _punishText.value
                }
                
                // 直接从昨日详细数据计算准确的惩罚数量，而不是从文本提取
                val targetNumber = calculatePunishmentCountFromYesterdayData(categoryId)
                
                _completionDialogIsReward.value = false
                _completionDialogTaskDescription.value = punishText
                _completionDialogTargetNumber.value = targetNumber
                _showCompletionDialog.value = true
                android.util.Log.d("HomeViewModel", "显示惩罚完成度对话框:")
                android.util.Log.d("HomeViewModel", "  标题文本: $punishText")
                android.util.Log.d("HomeViewModel", "  目标数量: $targetNumber")
                android.util.Log.d("HomeViewModel", "  当前分类: ${_selectedCategory.value?.name}")
                android.util.Log.d("HomeViewModel", "  昨日数据: ${_yesterdayDetailData.value?.punishmentContent}")
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "显示惩罚对话框失败", e)
            }
        }
    }
    
    /**
     * 完成昨日惩罚（带百分比）
     */
    fun completeYesterdayPunishment(completionPercent: Int = 100) {
        viewModelScope.launch {
            try {
                val categoryId = _selectedCategory.value?.id ?: return@launch
                val yesterday = getYesterdayDate()
                
                android.util.Log.d("HomeViewModel", "完成昨日惩罚: categoryId=$categoryId, date=$yesterday, percent=$completionPercent%")
                
                // 更新reward_punishment_user表
                val existingRecord = rewardPunishmentUserDao.getRecordByCategoryAndDate(categoryId, yesterday)
                if (existingRecord != null) {
                    // 使用新的百分比更新方法
                    rewardPunishmentUserDao.updatePunishmentCompletion(categoryId, yesterday, completionPercent)
                    android.util.Log.d("HomeViewModel", "已更新reward_punishment_user表的惩罚状态: $completionPercent%")
                } else {
                    android.util.Log.d("HomeViewModel", "reward_punishment_user表中没有对应记录，跳过更新")
                }
                
                // 更新UI状态
                _yesterdayPunishDone.value = completionPercent > 0
                _yesterdayPunishPercent.value = completionPercent
                _showCompletionDialog.value = false
                
                android.util.Log.d("HomeViewModel", "昨日惩罚已完成: $completionPercent%")
                
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "完成昨日惩罚失败", e)
            }
        }
    }

    /**
     * 关闭完成度选择对话框
     */
    fun dismissCompletionDialog() {
        _showCompletionDialog.value = false
    }

    /**
     * 获取昨日日期字符串
     */
    private fun getYesterdayDate(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
    }
    
    /**
     * 检查应用是否今天安装
     */
    private fun isAppInstalledToday(): Boolean {
        return try {
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageInfo(context.packageName, 0)
            val installTime = packageInfo.firstInstallTime
            
            // 获取今天零点的时间戳
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            val isInstalledToday = installTime >= today
            android.util.Log.d("HomeViewModel", "应用安装时间检查: installTime=${java.util.Date(installTime)}, today=${java.util.Date(today)}, isInstalledToday=$isInstalledToday")
            
            isInstalledToday
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "检查应用安装时间失败", e)
            // 出错时保守处理，认为不是今天安装
            false
        }
    }
    
    /**
     * 获取应用安装日期字符串
     */
    private fun getAppInstallDate(): String {
        return try {
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageInfo(context.packageName, 0)
            val installTime = packageInfo.firstInstallTime
            
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val installDate = dateFormat.format(java.util.Date(installTime))
            
            android.util.Log.d("HomeViewModel", "应用安装日期: $installDate")
            installDate
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "获取应用安装日期失败", e)
            // 出错时返回很早的日期，确保所有数据都显示
            "2020-01-01"
        }
    }
    
    /**
     * 只计算昨日详细数据，不显示对话框
     * 用于奖罚对话框需要获取昨日数据但不想显示昨日详情对话框的情况
     */
    private suspend fun calculateYesterdayDetailDataOnly() {
        try {
            val categoryId = _selectedCategory.value?.id ?: return
            val categoryName = _selectedCategory.value?.name ?: ""
            val yesterday = getYesterdayDate()
            
            // 获取昨日使用数据
            val realUsageSeconds = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, yesterday, 0) ?: 0
            val virtualUsageSeconds = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, yesterday, 1) ?: 0
            
            // 获取目标数据
            val goal = goalRewardPunishmentUserDao.getUserGoalByCatId(categoryId)
            val goalSeconds = goal?.dailyGoalMin?.times(60) ?: 0
            
            android.util.Log.d("HomeViewModel", "=== 计算昨日详细数据（不显示对话框） ===")
            android.util.Log.d("HomeViewModel", "categoryId=$categoryId, categoryName=$categoryName")
            
            // 计算目标完成情况
            val totalUsageSeconds = realUsageSeconds + virtualUsageSeconds
            val goalMet = if (goal != null) {
                when (goal.conditionType) {
                    0 -> totalUsageSeconds <= goalSeconds // 娱乐类：不超过目标算完成
                    1 -> totalUsageSeconds >= goalSeconds // 学习类：达到目标算完成
                    else -> false
                }
            } else {
                false
            }
            
            // 计算奖励和惩罚内容
            var rewardContent = ""
            var punishmentContent = ""
            var rewardCompleted = false
            var punishmentCompleted = false
            
            if (goal != null) {
                if (goalMet) {
                    // 目标完成，计算奖励内容
                    rewardContent = calculateRewardText(
                        categoryId = categoryId,
                        usageSeconds = totalUsageSeconds,
                        goalSeconds = goalSeconds,
                        baseRewardText = goal.rewardText,
                        conditionType = goal.conditionType
                    )
                    android.util.Log.d("HomeViewModel", "目标已完成，奖励内容: $rewardContent")
                } else {
                    // 目标未完成，计算惩罚内容
                    punishmentContent = calculatePunishmentText(
                        categoryId = categoryId,
                        usageSeconds = totalUsageSeconds,
                        goalSeconds = goalSeconds,
                        basePunishText = goal.punishText,
                        conditionType = goal.conditionType
                    )
                    android.util.Log.d("HomeViewModel", "目标未完成，惩罚内容: $punishmentContent")
                }
            }
            
            _yesterdayDetailData.value = YesterdayDetailData(
                realUsageSeconds = realUsageSeconds,
                virtualUsageSeconds = virtualUsageSeconds,
                goalSeconds = goalSeconds,
                categoryName = categoryName,
                goalMet = goalMet,
                rewardContent = rewardContent,
                punishmentContent = punishmentContent,
                rewardCompleted = rewardCompleted,
                punishmentCompleted = punishmentCompleted
            )
            
            android.util.Log.d("HomeViewModel", "计算完成昨日详细数据: 线上${realUsageSeconds}s, 线下${virtualUsageSeconds}s, 目标${goalSeconds}s, 完成$goalMet")
            
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "计算昨日详细数据失败", e)
        }
    }

    /**
     * 显示昨日详细数据对话框
     */
    fun showYesterdayDetailDialog() {
        viewModelScope.launch {
            try {
                val categoryId = _selectedCategory.value?.id ?: return@launch
                val categoryName = _selectedCategory.value?.name ?: ""
                val yesterday = getYesterdayDate()
                
                // 获取昨日使用数据
                val realUsageSeconds = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, yesterday, 0) ?: 0
                val virtualUsageSeconds = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, yesterday, 1) ?: 0
                
                // 获取目标数据
                val goal = goalRewardPunishmentUserDao.getUserGoalByCatId(categoryId)
                val goalSeconds = goal?.dailyGoalMin?.times(60) ?: 0
                
                android.util.Log.d("HomeViewModel", "=== 昨日详细数据调试 ===")
                android.util.Log.d("HomeViewModel", "categoryId=$categoryId, categoryName=$categoryName")
                android.util.Log.d("HomeViewModel", "goal=$goal")
                android.util.Log.d("HomeViewModel", "goalSeconds=$goalSeconds (${goalSeconds/3600.0}h)")
                if (goal != null) {
                    android.util.Log.d("HomeViewModel", "dailyGoalMin=${goal.dailyGoalMin}, conditionType=${goal.conditionType}")
                    android.util.Log.d("HomeViewModel", "rewardText=${goal.rewardText}, punishText=${goal.punishText}")
                    android.util.Log.d("HomeViewModel", "rewardNumber=${goal.rewardNumber}, punishNumber=${goal.punishNumber}")
                    android.util.Log.d("HomeViewModel", "数据库中dailyGoalMin=${goal.dailyGoalMin}分钟，转换为秒=${goal.dailyGoalMin * 60}秒，转换为小时=${goal.dailyGoalMin / 60.0}小时")
                } else {
                    android.util.Log.w("HomeViewModel", "⚠️ 未找到分类$categoryId($categoryName)的目标配置！这可能是Target Time显示0.0h的原因")
                }
                
                // 获取目标完成状态
                val rewardPunishmentRecord = rewardPunishmentUserDao.getRecordByCategoryAndDate(categoryId, yesterday)
                val goalMet = rewardPunishmentRecord?.isGoalMet == 1
                val rewardCompleted = rewardPunishmentRecord?.rewardDone == 1
                val punishmentCompleted = rewardPunishmentRecord?.punishDone == 1
                
                // 获取奖励和惩罚内容
                var rewardContent = ""
                var punishmentContent = ""
                if (goal != null) {
                    val totalUsageSeconds = realUsageSeconds + virtualUsageSeconds
                    android.util.Log.d("HomeViewModel", "totalUsageSeconds=$totalUsageSeconds, goalMet=$goalMet")
                    
                    if (goalMet) {
                        // 目标完成，计算奖励内容
                        rewardContent = calculateRewardText(
                            categoryId = categoryId,
                            usageSeconds = totalUsageSeconds,
                            goalSeconds = goalSeconds,
                            baseRewardText = goal.rewardText,
                            conditionType = goal.conditionType
                        )
                        android.util.Log.d("HomeViewModel", "目标已完成，奖励内容: $rewardContent")
                    } else {
                        // 目标未完成，计算惩罚内容
                        punishmentContent = calculatePunishmentText(
                            categoryId = categoryId,
                            usageSeconds = totalUsageSeconds,
                            goalSeconds = goalSeconds,
                            basePunishText = goal.punishText,
                            conditionType = goal.conditionType
                        )
                        android.util.Log.d("HomeViewModel", "目标未完成，惩罚内容: $punishmentContent")
                    }
                }
                
                _yesterdayDetailData.value = YesterdayDetailData(
                    realUsageSeconds = realUsageSeconds,
                    virtualUsageSeconds = virtualUsageSeconds,
                    goalSeconds = goalSeconds,
                    categoryName = categoryName,
                    goalMet = goalMet,
                    rewardContent = rewardContent,
                    punishmentContent = punishmentContent,
                    rewardCompleted = rewardCompleted,
                    punishmentCompleted = punishmentCompleted
                )
                
                _showYesterdayDetailDialog.value = true
                
                android.util.Log.d("HomeViewModel", "显示昨日详细数据: 线上${realUsageSeconds}s, 线下${virtualUsageSeconds}s, 目标${goalSeconds}s, 完成$goalMet")
                
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "获取昨日详细数据失败", e)
            }
        }
    }
    
    /**
     * 隐藏昨日详细数据对话框
     */
    fun hideYesterdayDetailDialog() {
        _showYesterdayDetailDialog.value = false
    }
    
    /**
     * 调试：修复Ent分类的目标设置和昨日奖罚记录
     */
    fun debugFixEntCategoryGoalAndReward() {
        viewModelScope.launch {
            try {
                android.util.Log.d("HomeViewModel", "=== 开始修复Ent分类目标设置 ===")
                
                // 1. 检查Ent分类的ID
                val allCategories = categoryDao.getAllCategoriesList()
                val entCategory = allCategories.find { it.name == "娱乐" }
                if (entCategory == null) {
                    android.util.Log.e("HomeViewModel", "找不到娱乐分类")
                    return@launch
                }
                val entCategoryId = entCategory.id
                android.util.Log.d("HomeViewModel", "娱乐分类ID: $entCategoryId")
                
                // 2. 检查当前目标设置
                val currentGoal = goalRewardPunishmentUserDao.getUserGoalByCatId(entCategoryId)
                android.util.Log.d("HomeViewModel", "当前目标设置: $currentGoal")
                
                // 3. 如果目标为空或不正确，重新设置
                if (currentGoal == null || currentGoal.dailyGoalMin != 120) {
                    val correctGoal = com.offtime.app.data.entity.GoalRewardPunishmentUserEntity(
                        catId = entCategoryId,
                        dailyGoalMin = 120, // 2小时 = 120分钟
                        goalTimeUnit = "分钟",
                        conditionType = 0, // ≤目标算完成
                        rewardText = "薯片",
                        rewardNumber = 2, // 每小时2包薯片
                        rewardUnit = "包",
                        rewardTimeUnit = "小时",
                        punishText = "俯卧撑",
                        punishNumber = 30, // 每小时30个俯卧撑
                        punishUnit = "个",
                        punishTimeUnit = "小时",
                        updateTime = System.currentTimeMillis()
                    )
                    goalRewardPunishmentUserDao.insertUserGoal(correctGoal)
                    android.util.Log.d("HomeViewModel", "已修复娱乐分类目标设置: 120分钟, conditionType=0")
                }
                
                // 4. 检查昨日奖罚记录
                val yesterday = getYesterdayDate()
                val yesterdayRecord = rewardPunishmentUserDao.getRecordByCategoryAndDate(entCategoryId, yesterday)
                android.util.Log.d("HomeViewModel", "昨日奖罚记录: $yesterdayRecord")
                
                // 5. 如果没有昨日记录，创建一个
                if (yesterdayRecord == null) {
                    val realUsage = dailyUsageDao.getTotalUsageByCategoryAndType(entCategoryId, yesterday, 0) ?: 0
                    val virtualUsage = dailyUsageDao.getTotalUsageByCategoryAndType(entCategoryId, yesterday, 1) ?: 0
                    val totalUsageSeconds = realUsage + virtualUsage
                    
                    // 娱乐分类：使用0h，目标2h，conditionType=0 (≤目标算完成)
                    // 0h ≤ 2h = true，所以目标完成，应该获得奖励
                    val goalMet = totalUsageSeconds <= (120 * 60) // 120分钟 = 7200秒
                    
                    val newRecord = com.offtime.app.data.entity.RewardPunishmentUserEntity(
                        id = "${yesterday}_${entCategoryId}",
                        date = yesterday,
                        catId = entCategoryId,
                        isGoalMet = if (goalMet) 1 else 0,
                        rewardDone = 0, // 奖励未完成
                        punishDone = 0, // 惩罚未完成
                        rewardCompletionPercent = 0,
                        punishCompletionPercent = 0,
                        updateTime = System.currentTimeMillis()
                    )
                    rewardPunishmentUserDao.upsert(newRecord)
                    android.util.Log.d("HomeViewModel", "已创建昨日奖罚记录: 使用${totalUsageSeconds}秒, 目标完成=$goalMet")
                }
                
                android.util.Log.d("HomeViewModel", "=== Ent分类修复完成 ===")
                
                // 6. 重新加载数据以更新UI
                val currentCategoryId = _selectedCategory.value?.id ?: entCategoryId
                loadYesterdayRewardPunishmentStatus(currentCategoryId, yesterday)
                
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "修复Ent分类失败", e)
            }
        }
    }

    /**
     * 显示应用详情对话框
     * @param categoryId 分类ID，如果为null则表示显示当前选中分类的详情
     * @param categoryName 分类名称，用于显示标题
     */
    fun showAppDetailDialog(categoryId: Int? = null, categoryName: String? = null) {
        viewModelScope.launch {
            try {
                val targetCategoryId = categoryId ?: _selectedCategory.value?.id ?: return@launch
                val targetCategoryName = categoryName ?: _selectedCategory.value?.name ?: "应用详情"
                val today = getTodayDate()
                
                android.util.Log.d("HomeViewModel", "显示应用详情对话框: categoryId=$targetCategoryId, categoryName=$targetCategoryName")
                
                // 查询该分类下的应用使用详情
                val appDetails = getAppDetailsByCategory(targetCategoryId, today)
                
                _appDetailList.value = appDetails
                _appDetailTitle.value = "${targetCategoryName}分类应用详情"
                _showAppDetailDialog.value = true
                
                android.util.Log.d("HomeViewModel", "应用详情查询完成: 找到${appDetails.size}个应用")
                
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "显示应用详情对话框失败", e)
            }
        }
    }
    
    /**
     * 显示所有应用的详情对话框（用于总使用分类点击）
     */
    fun showAllAppsDetailDialog() {
        viewModelScope.launch {
            try {
                val today = getTodayDate()
                
                android.util.Log.d("HomeViewModel", "显示所有应用详情对话框")
                
                // 查询所有应用的使用详情
                val appDetails = getAllAppsDetails(today)
                
                _appDetailList.value = appDetails
                _appDetailTitle.value = "总使用分类应用详情"
                _showAppDetailDialog.value = true
                
                android.util.Log.d("HomeViewModel", "所有应用详情查询完成: 找到${appDetails.size}个应用")
                
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "显示所有应用详情对话框失败", e)
            }
        }
    }
    
    /**
     * 隐藏应用详情对话框
     */
    fun hideAppDetailDialog() {
        _showAppDetailDialog.value = false
    }
    

    
    /**
     * 查询指定分类在指定日期的应用使用详情
     * 直接从原始会话表(app_sessions_users和timer_sessions_users)查询数据
     * @param categoryId 分类ID
     * @param date 日期
     * @return 按使用时长从长到短排序的应用详情列表
     */
    private suspend fun getAppDetailsByCategory(categoryId: Int, date: String): List<AppDetailItem> {
        return try {
            android.util.Log.d("HomeViewModel", "=== 直接从原始会话表查询应用详情 ===")
            android.util.Log.d("HomeViewModel", "查询参数: categoryId=$categoryId, date=$date")
            
            // 先检查该分类是否存在
            val category = categoryDao.getAllCategoriesList().find { it.id == categoryId }
            val categoryName = category?.name ?: "未知分类"
            android.util.Log.d("HomeViewModel", "找到分类: $categoryName (ID: $categoryId)")
            
            // 查询应用会话数据
            val appSessions = appSessionUserDao.getSessionsByCatIdAndDate(categoryId, date)
            android.util.Log.d("HomeViewModel", "应用会话表: 找到${appSessions.size}条记录")
            
            // 查询定时器会话数据
            val timerSessions = timerSessionUserDao.getSessionsByDate(date).filter { it.catId == categoryId }
            android.util.Log.d("HomeViewModel", "定时器会话表: 找到${timerSessions.size}条记录")
            
            // 合并处理两种会话数据
            val appUsageMap = mutableMapOf<String, Triple<Int, Int, Int>>() // key -> (realUsage, virtualUsage, totalUsage)
            
            // 处理应用会话数据
            appSessions.forEach { session ->
                val key = session.pkgName
                val currentUsage = appUsageMap[key] ?: Triple(0, 0, 0)
                val realUsage = if (session.isOffline == 0) session.durationSec else 0
                val virtualUsage = if (session.isOffline == 1) session.durationSec else 0
                
                appUsageMap[key] = Triple(
                    currentUsage.first + realUsage,
                    currentUsage.second + virtualUsage,
                    currentUsage.third + session.durationSec
                )
                
                android.util.Log.d("HomeViewModel", "应用会话: ${session.pkgName}, 时长:${session.durationSec}秒, 类型:${if(session.isOffline == 0) "真实" else "虚拟"}")
            }
            
            // 处理定时器会话数据（通常是线下活动）
            timerSessions.forEach { session ->
                val key = "timer_${session.programName}" // 使用特殊前缀区分定时器会话
                val currentUsage = appUsageMap[key] ?: Triple(0, 0, 0)
                val realUsage = if (session.isOffline == 0) session.durationSec else 0
                val virtualUsage = if (session.isOffline == 1) session.durationSec else 0
                
                appUsageMap[key] = Triple(
                    currentUsage.first + realUsage,
                    currentUsage.second + virtualUsage,
                    currentUsage.third + session.durationSec
                )
                
                android.util.Log.d("HomeViewModel", "定时器会话: ${session.programName}, 时长:${session.durationSec}秒, 类型:${if(session.isOffline == 0) "真实" else "虚拟"}")
            }
            
            if (appUsageMap.isEmpty()) {
                android.util.Log.w("HomeViewModel", "⚠️ 两个原始会话表都没有找到该分类的数据")
                return emptyList()
            }
            
            android.util.Log.d("HomeViewModel", "合并统计: 共${appUsageMap.size}个项目")
            
            // 计算总使用时间用于计算占比
            val totalCategoryUsage = appUsageMap.values.sumOf { it.third }
            android.util.Log.d("HomeViewModel", "分类总使用时长: ${totalCategoryUsage}秒 (${totalCategoryUsage/60.0}分钟)")
            
            // 转换为AppDetailItem并按总使用时长排序
            val appDetailList = appUsageMap.map { (key, usage) ->
                val (displayName, packageName) = if (key.startsWith("timer_")) {
                    // 定时器会话
                    val programName = key.substringAfter("timer_")
                    Pair("⏱️ $programName", key)
                } else {
                    // 应用会话 - 尝试获取应用名称
                    val appName = try {
                        val packageManager = context.packageManager
                        val appInfo = packageManager.getApplicationInfo(key, 0)
                        packageManager.getApplicationLabel(appInfo).toString()
                    } catch (e: Exception) {
                        // 如果获取失败，使用包名
                        key
                    }
                    Pair(appName, key)
                }
                
                val usagePercentage = if (totalCategoryUsage > 0) {
                    (usage.third.toFloat() / totalCategoryUsage * 100)
                } else 0f
                
                android.util.Log.d("HomeViewModel", "应用详情: $displayName ($packageName), 使用${usage.third}秒, 占比${usagePercentage}%")
                
                AppDetailItem(
                    packageName = packageName,
                    appName = displayName,
                    categoryId = categoryId,
                    categoryName = categoryName,
                    totalUsageSeconds = usage.third,
                    realUsageSeconds = usage.first,
                    virtualUsageSeconds = usage.second,
                    usagePercentage = usagePercentage
                )
            }.sortedByDescending { it.totalUsageSeconds } // 按总使用时长从长到短排序
            
            android.util.Log.d("HomeViewModel", "=== 应用详情查询完成 ===")
            android.util.Log.d("HomeViewModel", "最终结果: ${appDetailList.size}个项目，总使用时长${totalCategoryUsage}秒")
            appDetailList
            
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "查询应用详情失败", e)
            emptyList()
        }
    }

    /**
     * 获取所有应用的使用详情（跨所有分类）
     * @param date 日期
     * @return 按使用时长从长到短排序的所有应用详情列表
     */
    private suspend fun getAllAppsDetails(date: String): List<AppDetailItem> {
        return try {
            android.util.Log.d("HomeViewModel", "=== 查询所有应用详情 ===")
            android.util.Log.d("HomeViewModel", "查询参数: date=$date")
            
            // 获取所有分类，排除"排除统计"分类
            val allCategories = categoryDao.getAllCategoriesList().filter { category ->
                !category.name.contains("排除统计") && !category.name.contains("总使用")
            }
            android.util.Log.d("HomeViewModel", "有效分类: ${allCategories.map { "${it.name}(${it.id})" }}")
            
            // 合并处理所有分类的应用数据
            val appUsageMap = mutableMapOf<String, Triple<Int, Int, Int>>() // key -> (realUsage, virtualUsage, totalUsage)
            val packageToCategoryMap = mutableMapOf<String, Pair<Int, String>>() // packageName -> (categoryId, categoryName)
            
            // 查询所有应用会话数据
            val allAppSessions = appSessionUserDao.getSessionsByDate(date)
            android.util.Log.d("HomeViewModel", "查询到应用会话: ${allAppSessions.size}条")
            
            allAppSessions.forEach { session ->
                // 检查该会话所属分类是否在有效分类中
                val category = allCategories.find { it.id == session.catId }
                if (category != null) {
                    val key = session.pkgName
                    val currentUsage = appUsageMap[key] ?: Triple(0, 0, 0)
                    val realUsage = if (session.isOffline == 0) session.durationSec else 0
                    val virtualUsage = if (session.isOffline == 1) session.durationSec else 0
                    
                    appUsageMap[key] = Triple(
                        currentUsage.first + realUsage,
                        currentUsage.second + virtualUsage,
                        currentUsage.third + session.durationSec
                    )
                    
                    // 记录应用所属的分类（可能会被覆盖，但这里我们主要关心总使用时间）
                    packageToCategoryMap[key] = Pair(category.id, category.name)
                    
                    android.util.Log.d("HomeViewModel", "应用会话: ${session.pkgName}, 分类:${category.name}, 时长:${session.durationSec}秒")
                }
            }
            
            // 查询所有定时器会话数据
            val allTimerSessions = timerSessionUserDao.getSessionsByDate(date)
            android.util.Log.d("HomeViewModel", "查询到定时器会话: ${allTimerSessions.size}条")
            
            allTimerSessions.forEach { session ->
                // 检查该会话所属分类是否在有效分类中
                val category = allCategories.find { it.id == session.catId }
                if (category != null) {
                    val key = "timer_${session.programName}"
                    val currentUsage = appUsageMap[key] ?: Triple(0, 0, 0)
                    val realUsage = if (session.isOffline == 0) session.durationSec else 0
                    val virtualUsage = if (session.isOffline == 1) session.durationSec else 0
                    
                    appUsageMap[key] = Triple(
                        currentUsage.first + realUsage,
                        currentUsage.second + virtualUsage,
                        currentUsage.third + session.durationSec
                    )
                    
                    packageToCategoryMap[key] = Pair(category.id, category.name)
                    
                    android.util.Log.d("HomeViewModel", "定时器会话: ${session.programName}, 分类:${category.name}, 时长:${session.durationSec}秒")
                }
            }
            
            if (appUsageMap.isEmpty()) {
                android.util.Log.w("HomeViewModel", "⚠️ 没有找到任何应用数据")
                return emptyList()
            }
            
            android.util.Log.d("HomeViewModel", "合并统计: 共${appUsageMap.size}个应用")
            
            // 计算总使用时间用于计算占比
            val totalUsage = appUsageMap.values.sumOf { it.third }
            android.util.Log.d("HomeViewModel", "总使用时长: ${totalUsage}秒 (${totalUsage/60.0}分钟)")
            
            // 转换为AppDetailItem并按总使用时长排序
            val appDetailList = appUsageMap.map { (key, usage) ->
                val (displayName, packageName) = if (key.startsWith("timer_")) {
                    // 定时器会话
                    val programName = key.substringAfter("timer_")
                    Pair("⏱️ $programName", key)
                } else {
                    // 应用会话 - 尝试获取应用名称
                    val appName = try {
                        val packageManager = context.packageManager
                        val appInfo = packageManager.getApplicationInfo(key, 0)
                        packageManager.getApplicationLabel(appInfo).toString()
                    } catch (e: Exception) {
                        // 如果获取失败，使用包名
                        key
                    }
                    Pair(appName, key)
                }
                
                val usagePercentage = if (totalUsage > 0) {
                    (usage.third.toFloat() / totalUsage * 100)
                } else 0f
                
                val categoryInfo = packageToCategoryMap[key] ?: Pair(-1, "未知分类")
                
                android.util.Log.d("HomeViewModel", "应用详情: $displayName ($packageName), 分类:${categoryInfo.second}, 使用${usage.third}秒, 占比${usagePercentage}%")
                
                AppDetailItem(
                    packageName = packageName,
                    appName = displayName,
                    categoryId = categoryInfo.first,
                    categoryName = categoryInfo.second,
                    totalUsageSeconds = usage.third,
                    realUsageSeconds = usage.first,
                    virtualUsageSeconds = usage.second,
                    usagePercentage = usagePercentage
                )
            }.sortedByDescending { it.totalUsageSeconds } // 按总使用时长从长到短排序
            
            android.util.Log.d("HomeViewModel", "=== 所有应用详情查询完成 ===")
            android.util.Log.d("HomeViewModel", "最终结果: ${appDetailList.size}个应用，总使用时长${totalUsage}秒")
            appDetailList
            
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "查询所有应用详情失败", e)
            emptyList()
        }
    }

    private suspend fun loadHourlyUsageData(categoryId: Int, date: String) {
        try {
            android.util.Log.d("HomeViewModel", "开始加载24小时数据: categoryId=$categoryId, date=$date")
            
            // 获取当前时间，用于确定应该显示到哪个小时
            val currentTime = Calendar.getInstance()
            val currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            
            android.util.Log.d("HomeViewModel", "当前时间: ${currentTime.time}, 当前小时: $currentHour, 今天: $today")
            
            // 检查是否为"总使用"分类
            val category = categoryDao.getAllCategoriesList().find { it.id == categoryId }
            
            if (category?.name == "总使用") {
                android.util.Log.d("HomeViewModel", "加载总使用分类的24小时数据")
                // 总使用分类：从聚合表获取数据（已经是汇总后的数据）
                val realHourlyData = dailyUsageDao.getHourlyUsage(categoryId, date, 0)
                val virtualHourlyData = dailyUsageDao.getHourlyUsage(categoryId, date, 1)
                
                android.util.Log.d("HomeViewModel", "总使用分类查询到真实小时数据: ${realHourlyData.size}条, 虚拟小时数据: ${virtualHourlyData.size}条")
                
                // 构建24小时数据
                val realHourlyMinutes = mutableListOf<Int>()
                val virtualHourlyMinutes = mutableListOf<Int>()
                
                for (hour in 0..23) {
                    val realSeconds = realHourlyData.find { it.hour == hour }?.totalSeconds ?: 0
                    val virtualSeconds = virtualHourlyData.find { it.hour == hour }?.totalSeconds ?: 0
                    
                    // 如果是今天，只显示当前时间之前的数据
                    if (date == today && hour > currentHour) {
                        realHourlyMinutes.add(0)
                        virtualHourlyMinutes.add(0)
                    } else {
                        realHourlyMinutes.add(realSeconds / 60) // 转换为分钟
                        virtualHourlyMinutes.add(virtualSeconds / 60)
                    }
                }
                
                _hourlyRealUsage.value = realHourlyMinutes
                _hourlyVirtualUsage.value = virtualHourlyMinutes
                
                // 加载各分类的24小时详细数据用于多分类显示
                loadCategoryHourlyData(date, today, currentHour)
                
                android.util.Log.d("HomeViewModel", "总使用分类24小时真实数据(分钟): $realHourlyMinutes")
                android.util.Log.d("HomeViewModel", "总使用分类24小时虚拟数据(分钟): $virtualHourlyMinutes")
            } else {
                // 普通分类：查询指定日期的数据
                val realHourlyData = dailyUsageDao.getHourlyUsage(categoryId, date, 0)
                val virtualHourlyData = dailyUsageDao.getHourlyUsage(categoryId, date, 1)
                
                android.util.Log.d("HomeViewModel", "普通分类查询到真实小时数据: ${realHourlyData.size}条, 虚拟小时数据: ${virtualHourlyData.size}条")
                
                // 构建24小时数据
                val realHourlyMinutes = mutableListOf<Int>()
                val virtualHourlyMinutes = mutableListOf<Int>()
                
                for (hour in 0..23) {
                    val realSeconds = realHourlyData.find { it.hour == hour }?.totalSeconds ?: 0
                    val virtualSeconds = virtualHourlyData.find { it.hour == hour }?.totalSeconds ?: 0
                    
                    // 如果是今天，只显示当前时间之前的数据
                    if (date == today && hour > currentHour) {
                        realHourlyMinutes.add(0)
                        virtualHourlyMinutes.add(0)
                    } else {
                        realHourlyMinutes.add(realSeconds / 60) // 转换为分钟
                        virtualHourlyMinutes.add(virtualSeconds / 60)
                    }
                }
                
                _hourlyRealUsage.value = realHourlyMinutes
                _hourlyVirtualUsage.value = virtualHourlyMinutes
                
                android.util.Log.d("HomeViewModel", "普通分类24小时真实数据(分钟): $realHourlyMinutes")
                android.util.Log.d("HomeViewModel", "普通分类24小时虚拟数据(分钟): $virtualHourlyMinutes")
            }
            
            android.util.Log.d("HomeViewModel", "当前是$currentHour 点，所以$currentHour 点之后的数据都设置为0")
            
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "加载24小时数据失败", e)
            // 出错时使用全0数据
            _hourlyRealUsage.value = List(24) { 0 }
            _hourlyVirtualUsage.value = List(24) { 0 }
        }
    }
    
    /**
     * 加载各分类的使用时间数据（用于总使用分类的饼图显示）
     */
    private suspend fun loadCategoryUsageData(date: String) {
        try {
            android.util.Log.d("HomeViewModel", "开始加载各分类使用数据用于多分类显示")
            
            val allCategories = categoryDao.getAllCategoriesList()
            val totalUsageCategory = allCategories.find { it.name == "总使用" }
            
            // 过滤掉"总使用"分类本身
            val validCategories = allCategories.filter { 
                it.id != totalUsageCategory?.id 
            }
            
            val categoryUsageList = mutableListOf<CategoryUsageItem>()
            
            for (category in validCategories) {
                val realUsage = dailyUsageDao.getTotalUsageByCategoryAndType(category.id, date, 0) ?: 0
                val virtualUsage = dailyUsageDao.getTotalUsageByCategoryAndType(category.id, date, 1) ?: 0
                
                // 只添加有使用时间的分类
                if (realUsage > 0 || virtualUsage > 0) {
                    val categoryColor = com.offtime.app.utils.CategoryUtils.getCategoryColor(category.name)
                    
                    categoryUsageList.add(CategoryUsageItem(
                        categoryId = category.id,
                        categoryName = category.name,
                        realUsageSec = realUsage,
                        virtualUsageSec = virtualUsage,
                        color = categoryColor
                    ))
                }
            }
            
            _categoryUsageData.value = categoryUsageList
            android.util.Log.d("HomeViewModel", "加载了${categoryUsageList.size}个分类的使用数据")
            
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "加载各分类使用数据失败", e)
            _categoryUsageData.value = emptyList()
        }
    }
    
    /**
     * 加载各分类的24小时数据（用于总使用分类的柱状图显示）
     */
    private suspend fun loadCategoryHourlyData(date: String, today: String, currentHour: Int) {
        try {
            android.util.Log.d("HomeViewModel", "开始加载各分类24小时数据用于多分类显示")
            
            val allCategories = categoryDao.getAllCategoriesList()
            val totalUsageCategory = allCategories.find { it.name == "总使用" }
            
            // 过滤掉"总使用"分类本身
            val validCategories = allCategories.filter { 
                it.id != totalUsageCategory?.id 
            }
            
            val categoryHourlyList = mutableListOf<CategoryHourlyItem>()
            
            for (category in validCategories) {
                val realHourlyData = dailyUsageDao.getHourlyUsage(category.id, date, 0)
                val virtualHourlyData = dailyUsageDao.getHourlyUsage(category.id, date, 1)
                
                // 构建24小时数据
                val realHourlyMinutes = mutableListOf<Int>()
                val virtualHourlyMinutes = mutableListOf<Int>()
                
                for (hour in 0..23) {
                    val realSeconds = realHourlyData.find { it.hour == hour }?.totalSeconds ?: 0
                    val virtualSeconds = virtualHourlyData.find { it.hour == hour }?.totalSeconds ?: 0
                    
                    // 如果是今天，只显示当前时间之前的数据
                    if (date == today && hour > currentHour) {
                        realHourlyMinutes.add(0)
                        virtualHourlyMinutes.add(0)
                    } else {
                        realHourlyMinutes.add(realSeconds / 60) // 转换为分钟
                        virtualHourlyMinutes.add(virtualSeconds / 60)
                    }
                }
                
                // 只添加有使用时间的分类
                val hasUsage = realHourlyMinutes.any { it > 0 } || virtualHourlyMinutes.any { it > 0 }
                if (hasUsage) {
                    val categoryColor = com.offtime.app.utils.CategoryUtils.getCategoryColor(category.name)
                    
                    categoryHourlyList.add(CategoryHourlyItem(
                        categoryId = category.id,
                        categoryName = category.name,
                        hourlyRealUsage = realHourlyMinutes,
                        hourlyVirtualUsage = virtualHourlyMinutes,
                        color = categoryColor
                    ))
                }
            }
            
            _categoryHourlyData.value = categoryHourlyList
            android.util.Log.d("HomeViewModel", "加载了${categoryHourlyList.size}个分类的24小时数据")
            
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "加载各分类24小时数据失败", e)
            _categoryHourlyData.value = emptyList()
        }
    }
    
    private fun loadLineChartData(categoryId: Int, period: String) {
        // 暂时禁用缓存以测试数据修复
        // if (lastLoadedCategoryId == categoryId && lastLoadedPeriod == period) {
        //     android.util.Log.d("HomeViewModel", "使用缓存的折线图数据: categoryId=$categoryId, period=$period")
        //     return
        // }
        
        viewModelScope.launch {
            try {
                android.util.Log.d("HomeViewModel", "加载折线图数据: categoryId=$categoryId, period=$period")
                
                // 注意：数据更新现在通过页面切换时的统一触发机制处理
                // 不再在此处单独检查和触发数据更新，避免重复触发
                
                when (period) {
                    "日", "今日" -> {
                        // 获取最近15天的每日数据
                        loadLast15DaysData(categoryId, period)
                    }
                    "周", "近7日" -> {
                        // 获取最近15周的平均每日使用量
                        loadLast15WeeksData(categoryId)
                    }
                    "月", "近30日" -> {
                        // 获取最近15月的平均每日使用量
                        loadLast15MonthsData(categoryId)
                    }
                }
                
                // 更新缓存标记
                lastLoadedCategoryId = categoryId
                lastLoadedPeriod = period
                
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "加载折线图数据失败", e)
                // 设置空数据
                _usageLineData.value = emptyList()
                _completionLineData.value = emptyList()
                _rewardPunishmentData.value = emptyList()
            }
        }
    }
    
    /**
     * 加载最近15天的每日数据
     */
    private suspend fun loadLast15DaysData(categoryId: Int, period: String) {
        android.util.Log.d("HomeViewModel", "开始加载最近15天数据 - 使用聚合表")
        
        try {
            // 1. 尝试从聚合表获取数据
            val dailyUsageData = summaryUsageDao.getDailyUsageData(categoryId, 15)
            val dailyCompletionData = summaryUsageDao.getDailyCompletionData(categoryId, 15)
            
            android.util.Log.d("HomeViewModel", "聚合表查询结果: usage=${dailyUsageData.size}条, completion=${dailyCompletionData.size}条")
            
            // 如果汇总表没有数据，使用原始数据回退计算
            if (dailyUsageData.isEmpty()) {
                android.util.Log.w("HomeViewModel", "汇总表无数据，使用原始数据回退计算")
                loadLast15DaysDataFromRaw(categoryId, period)
                return
            }
            
            // 2. 生成完整的15天时间轴（从14天前到今天）
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val calendar = Calendar.getInstance()
            
            // 获取应用安装日期，从安装日开始显示所有数据
            val appInstallDate = getAppInstallDate()
            android.util.Log.d("HomeViewModel", "应用安装日期: $appInstallDate")
            
            val usageData = mutableListOf<UsageData>()
            val completionData = mutableListOf<UsageData>()
            val rewardPunishmentData = mutableListOf<RewardPunishmentData>()
            
            for (i in 14 downTo 0) {
                calendar.time = Date()
                calendar.add(Calendar.DAY_OF_YEAR, -i)
                val date = dateFormat.format(calendar.time)
                
                // 生成显示标签
                val displayPeriod = if (i == 0) "今天" else {
                    val month = calendar.get(Calendar.MONTH) + 1
                    val day = calendar.get(Calendar.DAY_OF_MONTH)
                    "${month}/${day}"
                }
                
                // 查找对应的聚合数据
                val usageRecord = dailyUsageData.find { it.period == date }
                val completionRecord = dailyCompletionData.find { it.period == date }
                
                // 基于应用安装日期来判断，从安装日开始显示所有数据
                val isAfterInstall = date >= appInstallDate
                    
                val totalUsageMinutes = if (isAfterInstall) {
                    // 程序安装后（即使使用时间为0）→ 彩色实线
                        usageRecord?.usageMinutes ?: 0
                    } else {
                    // 程序安装前 → 灰色虚线
                    -1
                }
                
                val dayCompletionRate = if (isAfterInstall) {
                    // 程序安装后（即使完成率为0）→ 彩色实线
                    completionRecord?.completionRate ?: 0f
                        } else {
                    // 程序安装前 → 灰色虚线
                    -1f
                }
                
                usageData.add(UsageData(
                    period = displayPeriod,
                    usageMinutes = totalUsageMinutes,
                    completionRate = 0f
                ))
                
                completionData.add(UsageData(
                    period = displayPeriod,
                    usageMinutes = totalUsageMinutes,
                    completionRate = dayCompletionRate
                ))
                
                // 添加奖罚完成度数据
                val rewardValue = if (i == 0) {
                    // 今天的奖励数据不显示
                    -1f
                } else if (isAfterInstall) {
                    // 程序安装后，查询奖罚记录
                    val rewardRecord = rewardPunishmentUserDao.getRecordByCategoryAndDate(categoryId, date)
                    if (rewardRecord != null) {
                        // 有奖罚记录，根据是否完成目标来判断奖励完成度
                        if (rewardRecord.isGoalMet == 1) {
                            // 目标完成，奖励完成度取决于是否已领取奖励
                            if (rewardRecord.rewardCompletionPercent > 0) {
                                rewardRecord.rewardCompletionPercent.toFloat()
                            } else if (rewardRecord.rewardDone == 1) {
                                100f
                            } else {
                                0f
                            }
                        } else {
                            // 目标未完成，没有奖励
                            100f // 无奖励可领取，视为100%完成
                        }
                    } else {
                        // 无奖罚记录，需要实时计算
                        val realUsage = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, date, 0) ?: 0
                        val virtualUsage = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, date, 1) ?: 0
                        val totalUsageSeconds = realUsage + virtualUsage
                        val goal = goalRewardPunishmentUserDao.getUserGoalByCatId(categoryId)
                        if (goal != null) {
                            val goalSeconds = goal.dailyGoalMin * 60
                            val goalMet = when (goal.conditionType) {
                                0 -> totalUsageSeconds <= goalSeconds // 娱乐类：使用时间不超过目标
                                1 -> totalUsageSeconds >= goalSeconds // 学习类：使用时间达到目标
                                else -> false
                            }
                            if (goalMet) 0f else 100f // 目标完成有奖励待领取(0%)，目标未完成无奖励(100%)
                    } else {
                            100f // 无目标配置，无奖励
                        }
                    }
                } else {
                    // 程序安装前 → 灰色虚线
                    -1f
                }
                
                val punishmentValue = if (isAfterInstall) {
                    // 程序安装后，查询奖罚记录
                    val rewardRecord = rewardPunishmentUserDao.getRecordByCategoryAndDate(categoryId, date)
                    if (rewardRecord != null) {
                        // 有奖罚记录，根据是否完成目标来判断惩罚完成度
                        if (rewardRecord.isGoalMet == 1) {
                            // 目标完成，没有惩罚
                            100f // 无惩罚需要执行，视为100%完成
                        } else {
                            // 目标未完成，惩罚完成度取决于是否已执行惩罚
                            if (rewardRecord.punishCompletionPercent > 0) {
                                rewardRecord.punishCompletionPercent.toFloat()
                            } else if (rewardRecord.punishDone == 1) {
                                100f
                            } else {
                                0f
                            }
                        }
                    } else {
                        // 无奖罚记录，需要实时计算
                        val realUsage = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, date, 0) ?: 0
                        val virtualUsage = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, date, 1) ?: 0
                        val totalUsageSeconds = realUsage + virtualUsage
                        val goal = goalRewardPunishmentUserDao.getUserGoalByCatId(categoryId)
                        if (goal != null) {
                            val goalSeconds = goal.dailyGoalMin * 60
                            val goalMet = when (goal.conditionType) {
                                0 -> totalUsageSeconds <= goalSeconds // 娱乐类：使用时间不超过目标
                                1 -> totalUsageSeconds >= goalSeconds // 学习类：使用时间达到目标
                                else -> false
                            }
                            if (goalMet) 100f else 0f // 目标完成无惩罚(100%)，目标未完成有惩罚待执行(0%)
                        } else {
                            100f // 无目标配置，无惩罚
                        }
                    }
                } else {
                    // 程序安装前 → 灰色虚线
                    -1f
                }
                
                rewardPunishmentData.add(RewardPunishmentData(
                period = displayPeriod,
                    rewardValue = rewardValue,
                    punishmentValue = punishmentValue
                ))
                
                val dataStatus = if (totalUsageMinutes >= 0) "安装后" else "安装前"
                android.util.Log.d("HomeViewModel", "日数据处理: $displayPeriod ($date), 状态=$dataStatus, 使用时间=${totalUsageMinutes}分钟, 完成率=$dayCompletionRate, 奖励=${rewardValue}%, 惩罚=${punishmentValue}%")
        }

            // 3. 设置数据
            _usageLineData.value = usageData
            _completionLineData.value = completionData
            _rewardPunishmentData.value = rewardPunishmentData
            
            android.util.Log.d("HomeViewModel", "最近15天数据加载完成: usage=${usageData.size}条, completion=${completionData.size}条, reward=${rewardPunishmentData.size}条")
            
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "加载最近15天数据失败", e)
            // 尝试使用原始数据回退
            loadLast15DaysDataFromRaw(categoryId, period)
        }
    }
    
    /**
     * 从原始数据计算最近15天的数据（回退方案）
     */
    private suspend fun loadLast15DaysDataFromRaw(categoryId: Int, @Suppress("UNUSED_PARAMETER") period: String) {
        android.util.Log.d("HomeViewModel", "使用原始数据计算最近15天数据")
        
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val calendar = Calendar.getInstance()
            
            // 获取应用安装日期，从安装日开始显示所有数据
            val appInstallDate = getAppInstallDate()
            android.util.Log.d("HomeViewModel", "回退方法 - 应用安装日期: $appInstallDate")
            
            val usageData = mutableListOf<UsageData>()
            val completionData = mutableListOf<UsageData>()
            val rewardPunishmentData = mutableListOf<RewardPunishmentData>()
            
            for (i in 14 downTo 0) {
                calendar.time = Date()
                calendar.add(Calendar.DAY_OF_YEAR, -i)
                val date = dateFormat.format(calendar.time)
                
                // 生成显示标签
                val displayPeriod = if (i == 0) "今天" else {
                    val month = calendar.get(Calendar.MONTH) + 1
                    val day = calendar.get(Calendar.DAY_OF_MONTH)
                    "${month}/${day}"
                }
                
                // 基于应用安装日期来判断，从安装日开始显示所有数据
                val isAfterInstall = date >= appInstallDate
                
                val realUsage = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, date, 0) ?: 0
                val virtualUsage = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, date, 1) ?: 0
                
                val totalUsageMinutes = if (isAfterInstall) {
                    // 程序安装后（即使使用时间为0）→ 彩色实线
                    val minutes = (realUsage + virtualUsage) / 60
                    android.util.Log.d("HomeViewModel", "程序安装后$date: totalMinutes=$minutes")
                    minutes
                } else {
                    // 程序安装前 → 灰色虚线
                    android.util.Log.d("HomeViewModel", "程序安装前$date: 设置为-1")
                    -1
                }
                
                usageData.add(UsageData(
                    period = displayPeriod,
                    usageMinutes = totalUsageMinutes,
                    completionRate = 0f
                ))
                
                // 计算完成率 - 使用相同的逻辑
                val completionRate = if (isAfterInstall) {
                    // 程序安装后 → 彩色实线
                    val rewardPunishmentRecord = rewardPunishmentUserDao.getByDateAndCategory(categoryId, date)
                    if (rewardPunishmentRecord != null) {
                        // 从奖罚记录中获取完成率
                        if (rewardPunishmentRecord.isGoalMet == 1) 100f else 0f
                    } else {
                        // 有真实使用数据但无奖罚记录，需要计算目标完成情况
                        val totalUsageSeconds = realUsage + virtualUsage
                        val goal = goalRewardPunishmentUserDao.getUserGoalByCatId(categoryId)
                        if (goal != null) {
                            val goalSeconds = goal.dailyGoalMin * 60
                            val goalMet = when (goal.conditionType) {
                                0 -> totalUsageSeconds <= goalSeconds // 使用时间不超过目标
                                1 -> totalUsageSeconds >= goalSeconds // 使用时间达到目标
                                else -> false
                            }
                            if (goalMet) 100f else 0f
                        } else {
                            0f
                        }
                    }
                    } else {
                    // 程序安装前 → 灰色虚线
                        -1f
                    }
                
                completionData.add(UsageData(
                    period = displayPeriod,
                    usageMinutes = totalUsageMinutes,
                    completionRate = completionRate
                ))
                
                // 添加奖罚完成度数据（与主方法相同的逻辑）
                val rewardValue = if (i == 0) {
                    // 今天的奖励数据不显示
                    -1f
                } else if (isAfterInstall) {
                    val rewardRecord = rewardPunishmentUserDao.getByDateAndCategory(categoryId, date)
                    if (rewardRecord != null) {
                        if (rewardRecord.isGoalMet == 1) {
                            if (rewardRecord.rewardCompletionPercent > 0) {
                                rewardRecord.rewardCompletionPercent.toFloat()
                            } else if (rewardRecord.rewardDone == 1) {
                                100f
                            } else {
                                0f
                            }
                        } else {
                            100f // 目标未完成，无奖励
                        }
                        } else {
                        val totalUsageSeconds = realUsage + virtualUsage
                        val goal = goalRewardPunishmentUserDao.getUserGoalByCatId(categoryId)
                        if (goal != null) {
                            val goalSeconds = goal.dailyGoalMin * 60
                            val goalMet = when (goal.conditionType) {
                                0 -> totalUsageSeconds <= goalSeconds
                                1 -> totalUsageSeconds >= goalSeconds
                                else -> false
                            }
                            if (goalMet) 0f else 100f
                        } else {
                            100f
                        }
                    }
                    } else {
                    -1f
                }
                
                val punishmentValue = if (isAfterInstall) {
                    val rewardRecord = rewardPunishmentUserDao.getByDateAndCategory(categoryId, date)
                    if (rewardRecord != null) {
                        if (rewardRecord.isGoalMet == 1) {
                            100f // 目标完成，无惩罚
                        } else {
                            // 使用惩罚完成百分比，如果有部分完成则显示百分比
                            if (rewardRecord.punishCompletionPercent > 0) {
                                rewardRecord.punishCompletionPercent.toFloat()
                            } else if (rewardRecord.punishDone == 1) {
                                100f
                            } else {
                                0f
                            }
                        }
                    } else {
                        val totalUsageSeconds = realUsage + virtualUsage
                        val goal = goalRewardPunishmentUserDao.getUserGoalByCatId(categoryId)
                        if (goal != null) {
                            val goalSeconds = goal.dailyGoalMin * 60
                            val goalMet = when (goal.conditionType) {
                                0 -> totalUsageSeconds <= goalSeconds
                                1 -> totalUsageSeconds >= goalSeconds
                                else -> false
                            }
                            if (goalMet) 100f else 0f
                    } else {
                            100f
                        }
                    }
                    } else {
                    -1f
                }
                
                rewardPunishmentData.add(RewardPunishmentData(
                period = displayPeriod,
                rewardValue = rewardValue,
                punishmentValue = punishmentValue
                ))
                
                val dataStatus = if (totalUsageMinutes >= 0) "安装后" else "安装前"
                android.util.Log.d("HomeViewModel", "原始数据计算: $displayPeriod ($date), 状态=$dataStatus, 使用时间=${totalUsageMinutes}分钟, 完成率=$completionRate, 奖励=${rewardValue}%, 惩罚=${punishmentValue}%")
            }
            
            _usageLineData.value = usageData
            _completionLineData.value = completionData
            _rewardPunishmentData.value = rewardPunishmentData
            
            android.util.Log.d("HomeViewModel", "原始数据回退计算完成: usage=${usageData.size}条, completion=${completionData.size}条, reward=${rewardPunishmentData.size}条")
            
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "原始数据回退计算失败", e)
            _usageLineData.value = emptyList()
            _completionLineData.value = emptyList()
            _rewardPunishmentData.value = emptyList()
        }
    }
    
    /**
     * 加载最近15周的平均每日使用量 - 使用聚合表优化
     */
    private suspend fun loadLast15WeeksData(categoryId: Int) {
        android.util.Log.d("HomeViewModel", "开始加载最近15周数据 - 使用聚合表")
        
        try {
            // 1. 从聚合表获取数据
            val weeklyUsageData = summaryUsageDao.getWeeklyUsageData(categoryId, 15)
            val weeklyCompletionData = summaryUsageDao.getWeeklyCompletionData(categoryId, 15)
            
            android.util.Log.d("HomeViewModel", "聚合表查询结果: usage=${weeklyUsageData.size}条, completion=${weeklyCompletionData.size}条")
            
            // 2. 生成完整的15周时间轴（从14周前到本周）
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val calendar = Calendar.getInstance()
            val today = Calendar.getInstance()
            
            // 先找到第一个有真实使用数据的周，确定程序安装时间点
            var firstUsageWeek: String? = null
            for (i in 14 downTo 0) {
                calendar.time = Date()
                calendar.add(Calendar.WEEK_OF_YEAR, -i)
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                val weekStart = dateFormat.format(calendar.time)
                
                // 检查该周是否有真实使用数据
                var hasRealUsage = false
                    val tempCalendar = Calendar.getInstance()
                    tempCalendar.time = calendar.time
                    
                    for (day in 0..6) {
                        val checkDate = dateFormat.format(tempCalendar.time)
                        val realUsage = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, checkDate, 0) ?: 0
                        val virtualUsage = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, checkDate, 1) ?: 0
                        if (realUsage > 0 || virtualUsage > 0) {
                        hasRealUsage = true
                        break
                        }
                        tempCalendar.add(Calendar.DAY_OF_YEAR, 1)
                        
                        // 不能超过今天
                        if (tempCalendar.time.after(today.time)) break
                    }
                
                if (hasRealUsage) {
                    firstUsageWeek = weekStart
                    break
                }
            }
            
            android.util.Log.d("HomeViewModel", "第一个有使用数据的周: $firstUsageWeek")
            
            val usageData = mutableListOf<UsageData>()
            val completionData = mutableListOf<UsageData>()
            val rewardPunishmentData = mutableListOf<RewardPunishmentData>()
            
            for (i in 14 downTo 0) {
                calendar.time = Date()
                calendar.add(Calendar.WEEK_OF_YEAR, -i)
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                val weekStart = dateFormat.format(calendar.time)
                
                // 生成显示标签
                val displayPeriod = if (i == 0) "本周" else {
                    val weekNumber = calendar.get(Calendar.WEEK_OF_YEAR)
                    val year = calendar.get(Calendar.YEAR).toString().takeLast(2)
                    "$year-$weekNumber"
                }
                
                // 查找对应的聚合数据
                val usageRecord = weeklyUsageData.find { it.period == weekStart }
                val completionRecord = weeklyCompletionData.find { it.period == weekStart }
                
                // 判断该周是否在程序安装后
                val isAfterInstall = if (firstUsageWeek != null) {
                    // 如果找到了第一个使用周，则该周及之后都视为安装后
                    weekStart >= firstUsageWeek
                } else {
                    // 如果没有找到任何使用数据，则只有本周视为安装后
                    i == 0
                }
                
                // 使用时间数据：根据是否在安装后来判断
                val weekUsageMinutes = if (isAfterInstall) {
                    usageRecord?.usageMinutes ?: 0 // 安装后，使用实际值或0 → 彩色实线
                } else {
                    -1 // 安装前 → 灰色虚线
                }
                
                usageData.add(UsageData(
                    period = displayPeriod,
                    usageMinutes = weekUsageMinutes,
                    completionRate = 0f
                ))
                
                // 完成率数据：根据是否在安装后来判断
                val weekCompletionRate = if (isAfterInstall) {
                    completionRecord?.completionRate ?: 0f // 安装后，使用实际值或0 → 彩色实线
                } else {
                    -1f // 安装前 → 灰色虚线
                }
                
                completionData.add(UsageData(
                    period = displayPeriod,
                    usageMinutes = weekUsageMinutes, // 使用相同的逻辑判断是否有数据
                    completionRate = weekCompletionRate
                ))
                
                val dataStatus = if (weekUsageMinutes >= 0) "安装后" else "安装前"
                android.util.Log.d("HomeViewModel", "周数据处理: $displayPeriod, 状态=$dataStatus, 使用时间=${weekUsageMinutes}分钟, 完成率=$weekCompletionRate")
                
                // 奖励惩罚数据
                try {
                    if (i == 0) {
                        // 本周：实时计算本周到目前为止的奖罚完成度
                        val weekDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val tempCalendar = Calendar.getInstance()
                        tempCalendar.time = calendar.time // 本周周一
                        
                        var totalRewardCount = 0
                        var doneRewardCount = 0
                        var totalPunishCount = 0
                        var donePunishCount = 0
                        var hasAnyData = false
                        
                        // 从本周周一到今天（不包括今天）
                        android.util.Log.d("HomeViewModel", "本周奖罚数据计算开始: 本周周一=${weekDateFormat.format(tempCalendar.time)}, 今天=${weekDateFormat.format(today.time)}")
                        while (tempCalendar.time.before(today.time)) {
                            val checkDate = weekDateFormat.format(tempCalendar.time)
                            val rewardRecord = rewardPunishmentUserDao.getRecordByCategoryAndDate(categoryId, checkDate)
                            
                            android.util.Log.d("HomeViewModel", "检查日期 $checkDate: 奖罚记录=${rewardRecord != null}")
                            
                            if (rewardRecord != null) {
                                hasAnyData = true
                                totalRewardCount++
                                totalPunishCount++
                                if (rewardRecord.rewardDone == 1) doneRewardCount++
                                if (rewardRecord.punishDone == 1) donePunishCount++
                                android.util.Log.d("HomeViewModel", "$checkDate 有奖罚记录: reward=${rewardRecord.rewardDone}, punishment=${rewardRecord.punishDone}")
                            } else {
                                // 检查是否有使用数据
                                val realUsage = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, checkDate, 0) ?: 0
                                val virtualUsage = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, checkDate, 1) ?: 0
                                android.util.Log.d("HomeViewModel", "$checkDate 无奖罚记录, 使用数据: real=$realUsage, virtual=$virtualUsage")
                                if (realUsage > 0 || virtualUsage > 0) {
                                    hasAnyData = true
                                    totalRewardCount++
                                    totalPunishCount++
                                    android.util.Log.d("HomeViewModel", "$checkDate 有使用数据但无奖罚记录，算作未完成")
                                    // 没有奖罚记录但有使用数据，算作未完成
                                }
                            }
                            tempCalendar.add(Calendar.DAY_OF_YEAR, 1)
                        }
                        
                        android.util.Log.d("HomeViewModel", "本周奖罚统计: hasData=$hasAnyData, 总奖励=$totalRewardCount, 完成奖励=$doneRewardCount, 总惩罚=$totalPunishCount, 完成惩罚=$donePunishCount")
                        
                        if (hasAnyData) {
                            val rewardCompletion = if (totalRewardCount > 0) {
                                (doneRewardCount.toFloat() / totalRewardCount * 100).coerceAtMost(100f)
                            } else 0f
                            val punishmentCompletion = if (totalPunishCount > 0) {
                                (donePunishCount.toFloat() / totalPunishCount * 100).coerceAtMost(100f)
                            } else 0f
                            
                            android.util.Log.d("HomeViewModel", "本周最终完成率: 奖励=$rewardCompletion%, 惩罚=$punishmentCompletion%")
                            
                            rewardPunishmentData.add(RewardPunishmentData(
                                period = displayPeriod,
                                rewardValue = rewardCompletion,
                                punishmentValue = punishmentCompletion
                            ))
                        } else {
                            // 本周没有任何数据，但应该认为有数据（奖罚完成度为0）
                            android.util.Log.d("HomeViewModel", "本周没有任何数据，设置为0%完成度")
                            rewardPunishmentData.add(RewardPunishmentData(
                                period = displayPeriod,
                                rewardValue = 0f,
                                punishmentValue = 0f
                            ))
                        }
                    } else {
                        // 历史周：使用聚合表数据
                        val weekRecord = rewardPunishmentWeekUserDao.getRecordByCategoryAndWeek(categoryId, weekStart)
                        android.util.Log.d("HomeViewModel", "历史周 $displayPeriod ($weekStart): 聚合记录=${weekRecord != null}")
                        
                        // 检查该周是否有使用数据，而不仅仅是奖罚记录
                        val hasUsageData = isAfterInstall && weekUsageMinutes >= 0
                        
                        if (hasUsageData) {
                            // 该周有使用数据，应该显示实线
                            if (weekRecord != null && (weekRecord.totalRewardCount > 0 || weekRecord.totalPunishCount > 0)) {
                                android.util.Log.d("HomeViewModel", "历史周 $displayPeriod 聚合数据: 总奖励=${weekRecord.totalRewardCount}, 完成奖励=${weekRecord.doneRewardCount}, 总惩罚=${weekRecord.totalPunishCount}, 完成惩罚=${weekRecord.donePunishCount}")
                                
                                val rewardCompletion = if (weekRecord.totalRewardCount > 0) {
                                    (weekRecord.doneRewardCount.toFloat() / weekRecord.totalRewardCount * 100).coerceAtMost(100f)
                                } else 0f
                                val punishmentCompletion = if (weekRecord.totalPunishCount > 0) {
                                    (weekRecord.donePunishCount.toFloat() / weekRecord.totalPunishCount * 100).coerceAtMost(100f)
                                } else 0f
                                
                                android.util.Log.d("HomeViewModel", "历史周 $displayPeriod 最终完成率: 奖励=$rewardCompletion%, 惩罚=$punishmentCompletion%")
                                
                                rewardPunishmentData.add(RewardPunishmentData(
                                    period = displayPeriod,
                                    rewardValue = rewardCompletion,
                                    punishmentValue = punishmentCompletion
                                ))
                            } else {
                                // 有使用数据但没有奖罚记录，完成度为0%（实线显示）
                                android.util.Log.d("HomeViewModel", "历史周 $displayPeriod 有使用数据但无奖罚记录，设置为0%完成度")
                                rewardPunishmentData.add(RewardPunishmentData(
                                    period = displayPeriod,
                                    rewardValue = 0f,
                                    punishmentValue = 0f
                                ))
                            }
                        } else {
                            // 该周没有使用数据（安装前或真正无数据）
                            android.util.Log.d("HomeViewModel", "历史周 $displayPeriod 没有使用数据")
                            rewardPunishmentData.add(RewardPunishmentData(
                                period = displayPeriod,
                                rewardValue = -1f,
                                punishmentValue = -1f
                            ))
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("HomeViewModel", "获取周奖励惩罚数据失败: $e")
                    rewardPunishmentData.add(RewardPunishmentData(
                        period = displayPeriod,
                        rewardValue = -1f,
                        punishmentValue = -1f
                    ))
                }
                
                android.util.Log.d("HomeViewModel", "周数据: $displayPeriod, 使用时间=${usageRecord?.usageMinutes ?: 0}分钟")
            }
            
            _usageLineData.value = usageData
            _completionLineData.value = completionData
            _rewardPunishmentData.value = rewardPunishmentData
            
            android.util.Log.d("HomeViewModel", "最近15周数据加载完成: usage=${usageData.size}条, completion=${completionData.size}条, reward=${rewardPunishmentData.size}条")
            android.util.Log.d("HomeViewModel", "周标签: ${usageData.map { it.period }}")
            
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "从聚合表加载周数据失败，回退到原有逻辑", e)
            // 如果聚合表查询失败，回退到原有的实时计算逻辑
            loadLast15WeeksDataFallback(categoryId)
        }
    }
    
    /**
     * 周数据加载的回退方法（原有的实时计算逻辑）- 修改为使用实际天数
     */
    private suspend fun loadLast15WeeksDataFallback(categoryId: Int) {
        android.util.Log.d("HomeViewModel", "使用回退方法加载周数据")
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val today = Calendar.getInstance()
        
        // 生成最近15周的周期列表
        val weekList = mutableListOf<String>()
        for (i in 14 downTo 0) {
            calendar.time = Date()
            calendar.add(Calendar.WEEK_OF_YEAR, -i)
            
            if (i == 0) {
                weekList.add("本周")
            } else {
                val weekNumber = calendar.get(Calendar.WEEK_OF_YEAR)
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                val mondayYear = calendar.get(Calendar.YEAR)
                val finalYear = mondayYear.toString().takeLast(2)
                weekList.add("$finalYear-$weekNumber")
            }
        }
        
        // 实时计算逻辑（修改为使用实际天数）
        val usageData = mutableListOf<UsageData>()
        for (i in weekList.indices) {
            calendar.time = Date()
            calendar.add(Calendar.WEEK_OF_YEAR, -(14-i))
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            
            var weeklyTotalMinutes = 0
            var actualDaysWithData = 0
            
            for (day in 0..6) {
                val date = dateFormat.format(calendar.time)
                val realUsage = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, date, 0) ?: 0
                val virtualUsage = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, date, 1) ?: 0
                val dailyMinutes = (realUsage + virtualUsage) / 60
                
                weeklyTotalMinutes += dailyMinutes
                if (dailyMinutes > 0) {
                    actualDaysWithData++
                }
                
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                
                // 不能超过今天
                if (calendar.time.after(today.time)) break
            }
            
            // 使用实际有数据的天数计算平均值，如果没有数据则使用0
            val avgDailyMinutes = if (actualDaysWithData > 0) {
                weeklyTotalMinutes / actualDaysWithData
            } else {
                0
            }
            
            usageData.add(UsageData(
                period = weekList[i],
                usageMinutes = avgDailyMinutes,
                completionRate = 0f
            ))
            
            android.util.Log.d("HomeViewModel", "周数据回退: ${weekList[i]}, 总时长=${weeklyTotalMinutes}分钟, 有数据天数=$actualDaysWithData, 平均每日=${avgDailyMinutes}分钟")
        }
        
        val completionData = mutableListOf<UsageData>()
        for (i in weekList.indices) {
            calendar.time = Date()
            calendar.add(Calendar.WEEK_OF_YEAR, -(14-i))
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            
            var totalCompletionRate = 0f
            var actualDaysWithData = 0
            
            for (day in 0..6) {
                val date = dateFormat.format(calendar.time)
                val dailyCompletionRate = calculateDailyGoalCompletionRate(categoryId, date)
                
                // 检查该日期是否有使用数据
                val realUsage = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, date, 0) ?: 0
                val virtualUsage = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, date, 1) ?: 0
                
                if (realUsage > 0 || virtualUsage > 0) {
                    totalCompletionRate += dailyCompletionRate
                    actualDaysWithData++
                }
                
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                
                // 不能超过今天
                if (calendar.time.after(today.time)) break
            }
            
            // 使用实际有数据的天数计算平均完成率
            val avgCompletionRate = if (actualDaysWithData > 0) {
                totalCompletionRate / actualDaysWithData
            } else {
                0f
            }
            
            completionData.add(UsageData(
                period = weekList[i],
                usageMinutes = 0,
                completionRate = avgCompletionRate
            ))
        }
        
        _usageLineData.value = usageData
        _completionLineData.value = completionData
        _rewardPunishmentData.value = emptyList()
    }
    
    /**
     * 加载最近15月的平均每日使用量 - 使用聚合表优化
     */
    private suspend fun loadLast15MonthsData(categoryId: Int) {
        android.util.Log.d("HomeViewModel", "开始加载最近15月数据 - 使用聚合表")
        
        try {
            // 1. 从聚合表获取数据
            val monthlyUsageData = summaryUsageDao.getMonthlyUsageData(categoryId, 15)
            val monthlyCompletionData = summaryUsageDao.getMonthlyCompletionData(categoryId, 15)
            
            android.util.Log.d("HomeViewModel", "聚合表查询结果: usage=${monthlyUsageData.size}条, completion=${monthlyCompletionData.size}条")
            
            // 2. 生成完整的15月时间轴（从14月前到本月）
            val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val calendar = Calendar.getInstance()
            val today = Calendar.getInstance()
            
            val usageData = mutableListOf<UsageData>()
            val completionData = mutableListOf<UsageData>()
            val rewardPunishmentData = mutableListOf<RewardPunishmentData>()
            
            for (i in 14 downTo 0) {
                calendar.time = Date()
                calendar.add(Calendar.MONTH, -i)
                val month = monthFormat.format(calendar.time)
                
                // 生成显示标签
                val displayPeriod = if (i == 0) "本月" else {
                    val parts = month.split("-")
                    if (parts.size == 2) {
                        val year = parts[0].takeLast(2)
                        val monthNum = parts[1]
                        "$year-$monthNum"
                    } else {
                        month
                    }
                }
                
                // 查找对应的聚合数据
                val usageRecord = monthlyUsageData.find { it.period == month }
                val completionRecord = monthlyCompletionData.find { it.period == month }
                
                // 计算实际有数据的天数（对于本月和历史月的处理）
                val actualDaysInMonth = if (i == 0) {
                    // 本月：从1号到今天的天数
                    today.get(Calendar.DAY_OF_MONTH)
                } else {
                    // 历史月：检查该月实际有数据的天数
                    calendar.time = Date()
                    calendar.add(Calendar.MONTH, -i)
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    val monthStart = calendar.time
                    calendar.add(Calendar.MONTH, 1)
                    calendar.add(Calendar.DAY_OF_YEAR, -1)
                    val monthEnd = calendar.time
                    
                    var daysWithData = 0
                    val tempCalendar = Calendar.getInstance()
                    tempCalendar.time = monthStart
                    
                    while (tempCalendar.time <= monthEnd && !tempCalendar.time.after(today.time)) {
                        val checkDate = dateFormat.format(tempCalendar.time)
                        // 检查该日期是否有使用数据
                        val realUsage = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, checkDate, 0) ?: 0
                        val virtualUsage = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, checkDate, 1) ?: 0
                        if (realUsage > 0 || virtualUsage > 0) {
                            daysWithData++
                        }
                        tempCalendar.add(Calendar.DAY_OF_YEAR, 1)
                    }
                    
                    if (daysWithData == 0) {
                        // 如果没有数据，使用该月的实际天数
                        val daysInMonth = Calendar.getInstance().apply {
                            time = monthStart
                            add(Calendar.MONTH, 1)
                            add(Calendar.DAY_OF_YEAR, -1)
                        }.get(Calendar.DAY_OF_MONTH)
                        daysInMonth
                    } else {
                        daysWithData
                    }
                }
                
                // 判断该月是否有真实使用数据
                val hasMonthRealData = if (i == 0) {
                    // 本月：应该总是有数据（类似今天的逻辑）
                    true
                } else {
                    // 历史月：检查该月是否有真实使用数据
                    calendar.time = Date()
                    calendar.add(Calendar.MONTH, -i)
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    val monthStart = calendar.time
                    calendar.add(Calendar.MONTH, 1)
                    calendar.add(Calendar.DAY_OF_YEAR, -1)
                    val monthEnd = calendar.time
                    
                    var hasRealUsage = false
                    val tempCalendar = Calendar.getInstance()
                    tempCalendar.time = monthStart
                    
                    while (tempCalendar.time <= monthEnd && !tempCalendar.time.after(today.time)) {
                        val checkDate = dateFormat.format(tempCalendar.time)
                        val realUsage = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, checkDate, 0) ?: 0
                        val virtualUsage = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, checkDate, 1) ?: 0
                        if (realUsage > 0 || virtualUsage > 0) {
                            hasRealUsage = true
                            break
                        }
                        tempCalendar.add(Calendar.DAY_OF_YEAR, 1)
                    }
                    hasRealUsage
                }
                
                // 使用时间数据：根据是否有真实使用数据来判断
                val monthUsageMinutes = if (hasMonthRealData) {
                    usageRecord?.usageMinutes ?: 0 // 有真实数据，使用实际值或0
                } else {
                    -1 // 无真实数据（程序安装前）
                }
                
                usageData.add(UsageData(
                    period = displayPeriod,
                    usageMinutes = monthUsageMinutes,
                    completionRate = 0f
                ))
                
                // 完成率数据：根据是否有真实使用数据来判断
                val monthCompletionRate = if (hasMonthRealData) {
                    completionRecord?.completionRate ?: 0f // 有真实数据，使用实际值或0
                } else {
                    -1f // 无真实数据（程序安装前）
                }
                
                completionData.add(UsageData(
                    period = displayPeriod,
                    usageMinutes = monthUsageMinutes, // 使用相同的逻辑判断是否有数据
                    completionRate = monthCompletionRate
                ))
                
                val dataStatus = if (monthUsageMinutes >= 0) "安装后" else "安装前"
                android.util.Log.d("HomeViewModel", "月数据处理: $displayPeriod, 状态=$dataStatus, 使用时间=${monthUsageMinutes}分钟, 完成率=$monthCompletionRate")
                
                // 奖励惩罚数据
                try {
                    if (i == 0) {
                        // 本月：实时计算本月到目前为止的奖罚完成度
                        val monthDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val tempCalendar = Calendar.getInstance()
                        tempCalendar.time = Date()
                        tempCalendar.add(Calendar.MONTH, -i)
                        tempCalendar.set(Calendar.DAY_OF_MONTH, 1) // 本月1号
                        
                        var totalRewardCount = 0
                        var doneRewardCount = 0
                        var totalPunishCount = 0
                        var donePunishCount = 0
                        var hasAnyData = false
                        
                        // 从本月1号到今天（不包括今天）
                        while (tempCalendar.time.before(today.time)) {
                            val checkDate = monthDateFormat.format(tempCalendar.time)
                            val rewardRecord = rewardPunishmentUserDao.getRecordByCategoryAndDate(categoryId, checkDate)
                            
                            if (rewardRecord != null) {
                                hasAnyData = true
                                totalRewardCount++
                                totalPunishCount++
                                if (rewardRecord.rewardDone == 1) doneRewardCount++
                                if (rewardRecord.punishDone == 1) donePunishCount++
                            } else {
                                // 检查是否有使用数据
                                val realUsage = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, checkDate, 0) ?: 0
                                val virtualUsage = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, checkDate, 1) ?: 0
                                if (realUsage > 0 || virtualUsage > 0) {
                                    hasAnyData = true
                                    totalRewardCount++
                                    totalPunishCount++
                                    // 没有奖罚记录但有使用数据，算作未完成
                                }
                            }
                            tempCalendar.add(Calendar.DAY_OF_YEAR, 1)
                        }
                        
                        if (hasAnyData) {
                            val rewardCompletion = if (totalRewardCount > 0) {
                                (doneRewardCount.toFloat() / totalRewardCount * 100).coerceAtMost(100f)
                            } else 0f
                            val punishmentCompletion = if (totalPunishCount > 0) {
                                (donePunishCount.toFloat() / totalPunishCount * 100).coerceAtMost(100f)
                            } else 0f
                            
                            rewardPunishmentData.add(RewardPunishmentData(
                                period = displayPeriod,
                                rewardValue = rewardCompletion,
                                punishmentValue = punishmentCompletion
                            ))
                        } else {
                            // 本月没有任何数据，但应该认为有数据（奖罚完成度为0）
                            rewardPunishmentData.add(RewardPunishmentData(
                                period = displayPeriod,
                                rewardValue = 0f,
                                punishmentValue = 0f
                            ))
                        }
                    } else {
                        // 历史月：使用聚合表数据
                        val monthRecord = rewardPunishmentMonthUserDao.getRecordByCategoryAndMonth(categoryId, month)
                        if (monthRecord != null && (monthRecord.totalRewardCount > 0 || monthRecord.totalPunishCount > 0)) {
                            val rewardCompletion = if (monthRecord.totalRewardCount > 0) {
                                (monthRecord.doneRewardCount.toFloat() / monthRecord.totalRewardCount * 100).coerceAtMost(100f)
                            } else 0f
                            val punishmentCompletion = if (monthRecord.totalPunishCount > 0) {
                                (monthRecord.donePunishCount.toFloat() / monthRecord.totalPunishCount * 100).coerceAtMost(100f)
                            } else 0f
                            
                            rewardPunishmentData.add(RewardPunishmentData(
                                period = displayPeriod,
                                rewardValue = rewardCompletion,
                                punishmentValue = punishmentCompletion
                            ))
                        } else {
                            // 该月没有数据
                            rewardPunishmentData.add(RewardPunishmentData(
                                period = displayPeriod,
                                rewardValue = -1f,
                                punishmentValue = -1f
                            ))
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("HomeViewModel", "获取月奖励惩罚数据失败: $e")
                    rewardPunishmentData.add(RewardPunishmentData(
                        period = displayPeriod,
                        rewardValue = -1f,
                        punishmentValue = -1f
                    ))
                }
                
                android.util.Log.d("HomeViewModel", "月数据: $displayPeriod, 实际天数=$actualDaysInMonth, 使用时间=${usageRecord?.usageMinutes ?: 0}分钟")
            }
            
            _usageLineData.value = usageData
            _completionLineData.value = completionData
            _rewardPunishmentData.value = rewardPunishmentData
            
            android.util.Log.d("HomeViewModel", "最近15月数据加载完成: usage=${usageData.size}条, completion=${completionData.size}条, reward=${rewardPunishmentData.size}条")
            android.util.Log.d("HomeViewModel", "月标签: ${usageData.map { it.period }}")
            
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "从聚合表加载月数据失败，回退到原有逻辑", e)
            // 如果聚合表查询失败，回退到原有的实时计算逻辑
            loadLast15MonthsDataFallback(categoryId)
        }
    }
    
    /**
     * 月数据加载的回退方法（原有的实时计算逻辑）- 修改为使用实际天数
     */
    private suspend fun loadLast15MonthsDataFallback(categoryId: Int) {
        android.util.Log.d("HomeViewModel", "使用回退方法加载月数据")
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val today = Calendar.getInstance()
        val monthList = mutableListOf<String>()
        val periodList = mutableListOf<String>()
        
        // 生成最近15月的月份列表
        for (i in 14 downTo 0) {
            calendar.time = Date()
            calendar.add(Calendar.MONTH, -i)
            val month = monthFormat.format(calendar.time)
            monthList.add(month)
            
            // 生成显示标签
            if (i == 0) {
                periodList.add("本月")
            } else {
                val parts = month.split("-")
                if (parts.size == 2) {
                    val year = parts[0].takeLast(2)
                    val monthNum = parts[1]
                    periodList.add("$year-$monthNum")
                } else {
                    periodList.add(month)
                }
            }
        }
        
        // 实时计算逻辑（修改为使用实际天数）
        val usageData = mutableListOf<UsageData>()
        for (i in monthList.indices) {
            val month = monthList[i]
            calendar.time = monthFormat.parse(month + "-01")!!
            val monthStart = calendar.time
            calendar.add(Calendar.MONTH, 1)
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val monthEnd = calendar.time
            
            var monthlyTotalMinutes = 0
            var actualDaysWithData = 0
            
            calendar.time = monthStart
            while (calendar.time <= monthEnd && !calendar.time.after(today.time)) {
                val date = dateFormat.format(calendar.time)
                val realUsage = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, date, 0) ?: 0
                val virtualUsage = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, date, 1) ?: 0
                val dailyMinutes = (realUsage + virtualUsage) / 60
                
                monthlyTotalMinutes += dailyMinutes
                if (dailyMinutes > 0) {
                    actualDaysWithData++
                }
                
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            
            // 使用实际有数据的天数计算平均值，如果没有数据则使用0
            val avgDailyMinutes = if (actualDaysWithData > 0) {
                monthlyTotalMinutes / actualDaysWithData
            } else {
                0
            }
            
            usageData.add(UsageData(
                period = periodList[i],
                usageMinutes = avgDailyMinutes,
                completionRate = 0f
            ))
            
            android.util.Log.d("HomeViewModel", "月数据回退: ${periodList[i]}, 总时长=${monthlyTotalMinutes}分钟, 有数据天数=$actualDaysWithData, 平均每日=${avgDailyMinutes}分钟")
        }
        
        val completionData = mutableListOf<UsageData>()
        for (i in monthList.indices) {
            val month = monthList[i]
            calendar.time = monthFormat.parse(month + "-01")!!
            val monthStart = calendar.time
            calendar.add(Calendar.MONTH, 1)
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val monthEnd = calendar.time
            
            var totalCompletionRate = 0f
            var actualDaysWithData = 0
            
            calendar.time = monthStart
            while (calendar.time <= monthEnd && !calendar.time.after(today.time)) {
                val date = dateFormat.format(calendar.time)
                val dailyCompletionRate = calculateDailyGoalCompletionRate(categoryId, date)
                
                // 检查该日期是否有使用数据
                val realUsage = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, date, 0) ?: 0
                val virtualUsage = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, date, 1) ?: 0
                
                if (realUsage > 0 || virtualUsage > 0) {
                    totalCompletionRate += dailyCompletionRate
                    actualDaysWithData++
                }
                
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            
            // 使用实际有数据的天数计算平均完成率
            val avgCompletionRate = if (actualDaysWithData > 0) {
                totalCompletionRate / actualDaysWithData
            } else {
                0f
            }
            
            completionData.add(UsageData(
                period = periodList[i],
                usageMinutes = 0,
                completionRate = avgCompletionRate
            ))
        }
        
        _usageLineData.value = usageData
        _completionLineData.value = completionData
        _rewardPunishmentData.value = emptyList()
    }
    
    /**
     * 计算某日的目标完成率
     */
    private suspend fun calculateDailyGoalCompletionRate(categoryId: Int, date: String): Float {
        return try {
            // 获取该分类的目标时间（从goals_reward_punishment_users表）
            val goal = goalRewardPunishmentUserDao.getUserGoalByCatId(categoryId)
            val goalMinutes = goal?.dailyGoalMin ?: 120 // 默认2小时
            
            // 获取实际使用时间
            val realUsage = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, date, 0) ?: 0
            val virtualUsage = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, date, 1) ?: 0
            val actualMinutes = (realUsage + virtualUsage) / 60
            
            // 计算完成率
            if (goalMinutes > 0) {
                (actualMinutes.toFloat() / goalMinutes * 100).coerceAtMost(100f)
            } else {
                0f
            }
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "计算目标完成率失败: $e")
            0f
        }
    }
    
    /**
     * 生成日期时间轴：智能调整时间轴显示逻辑
     * - 如果没有历史数据，今天放在左侧，显示今天+未来14天
     * - 如果有历史数据，今天放在右侧，显示过去14天+今天
     */
    private suspend fun generateDailyTimeAxis(): List<String> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val today = dateFormat.format(calendar.time)
        
        val timeAxis = mutableListOf<String>()
        
        // 检查是否有历史数据
        val hasHistoricalData = checkHasHistoricalData()
        
        if (hasHistoricalData) {
            // 有历史数据：今天在右侧，显示过去14天+今天
            calendar.time = dateFormat.parse(today)!!
            calendar.add(Calendar.DAY_OF_YEAR, -14)
            
            for (i in 0 until 15) {
                timeAxis.add(dateFormat.format(calendar.time))
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        } else {
            // 没有历史数据：今天在左侧，显示今天+未来14天
            calendar.time = dateFormat.parse(today)!!
            
            for (i in 0 until 15) {
                timeAxis.add(dateFormat.format(calendar.time))
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        
        return timeAxis
    }
    
    /**
     * 检查是否有历史数据（今天之前的数据）
     */
    private suspend fun checkHasHistoricalData(): Boolean {
        return try {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            
            // 检查是否有昨天或更早的数据
            val historicalData = summaryUsageDao.getDailyUsage(_selectedCategory.value?.id ?: 1, 30)
            historicalData.any { it.date < today }
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "检查历史数据失败", e)
            false
        }
    }
    
    /**
     * 加载奖罚详情统计数据
     */
    fun loadRewardPunishmentSummary() {
        viewModelScope.launch {
            try {
                val categoryId = _selectedCategory.value?.id ?: return@launch
                val categoryName = _selectedCategory.value?.name ?: return@launch
                
                // 获取奖罚配置
                val goal = goalRewardPunishmentUserDao.getUserGoalByCatId(categoryId)
                if (goal == null) {
                    _rewardPunishmentSummary.value = emptyList()
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
                
                // 计算总计数据
                val totalData = calculateTotalSummary(categoryId, categoryName, goal)
                if (totalData != null) summaryList.add(totalData)
                
                _rewardPunishmentSummary.value = summaryList
                
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "加载奖罚详情统计失败", e)
                _rewardPunishmentSummary.value = emptyList()
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
            
            android.util.Log.d("HomeViewModel", "期间[$period]奖罚统计: 应获得奖励=${expectedRewardCount}次, 完成奖励=${rewardCount}次, 实际奖励量=$totalRewardAmount, 应获得惩罚=${expectedPunishCount}次, 完成惩罚=${punishCount}次, 实际惩罚量=$totalPunishAmount")
            
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
            android.util.Log.e("HomeViewModel", "计算${period}奖罚统计失败", e)
            return null
        }
    }
    
    /**
     * 计算总计数据
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
            
            android.util.Log.d("HomeViewModel", "总计奖罚统计: 应获得奖励=${expectedRewardCount}次, 完成奖励=${totalRewardCount}次, 实际奖励量=$totalRewardAmount, 应获得惩罚=${expectedPunishCount}次, 完成惩罚=${totalPunishCount}次, 实际惩罚量=$totalPunishAmount")
            
            return RewardPunishmentSummaryData(
                period = "总计",
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
            android.util.Log.e("HomeViewModel", "计算总计奖罚统计失败", e)
            return null
        }
    }
    
    /**
     * 获取指定偏移天数的日期字符串
     */
    private fun getDateString(dayOffset: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, dayOffset)
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
    }
    
    /**
     * 获取本周的所有日期
     */
    private fun getCurrentWeekDates(): List<String> {
        val calendar = Calendar.getInstance()
        val dates = mutableListOf<String>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        // 设置到本周一
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
        calendar.add(Calendar.DAY_OF_YEAR, -daysFromMonday)
        
        // 获取一周的日期
        for (i in 0 until 7) {
            dates.add(dateFormat.format(calendar.time))
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        return dates
    }
    
    /**
     * 获取本月的所有日期
     */
    private fun getCurrentMonthDates(): List<String> {
        val calendar = Calendar.getInstance()
        val dates = mutableListOf<String>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        // 设置到本月1号
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        // 获取本月的所有日期
        for (i in 1..maxDay) {
            calendar.set(Calendar.DAY_OF_MONTH, i)
            dates.add(dateFormat.format(calendar.time))
        }
        
        return dates
    }

    /**
     * 显示特定小时的应用详情对话框
     * @param hour 小时（0-23）
     * @param categoryId 分类ID，如果为null则表示显示当前选中分类的详情
     * @param categoryName 分类名称，用于显示标题
     */
    fun showHourlyAppDetailDialog(hour: Int, categoryId: Int? = null, categoryName: String? = null) {
        viewModelScope.launch {
            try {
                val targetCategoryId = categoryId ?: _selectedCategory.value?.id ?: return@launch
                val targetCategoryName = categoryName ?: _selectedCategory.value?.name ?: "应用详情"
                val today = getTodayDate()
                
                android.util.Log.d("HomeViewModel", "=== showHourlyAppDetailDialog 参数检查 ===")
                android.util.Log.d("HomeViewModel", "传入参数: hour=$hour, categoryId=$categoryId, categoryName=$categoryName")
                android.util.Log.d("HomeViewModel", "当前选中分类: ID=${_selectedCategory.value?.id}, 名称=${_selectedCategory.value?.name}")
                android.util.Log.d("HomeViewModel", "最终使用: categoryId=$targetCategoryId, categoryName=$targetCategoryName")
                
                // 检查是否为"总使用"分类
                if (targetCategoryName == "总使用") {
                    android.util.Log.d("HomeViewModel", "检测到总使用分类，显示${hour}点所有应用详情")
                    
                    // 查询该小时所有分类的应用使用详情
                    val appDetails = getHourlyAllAppsDetails(today, hour)
                    
                    _appDetailList.value = appDetails
                    _appDetailTitle.value = "总使用分类 ${hour}点应用详情"
                    _showAppDetailDialog.value = true
                    
                    android.util.Log.d("HomeViewModel", "总使用${hour}点应用详情查询完成: 找到${appDetails.size}个应用")
                    return@launch
                }
                
                // 检查分类ID是否一致
                if (categoryId != null && _selectedCategory.value != null && categoryId != _selectedCategory.value!!.id) {
                    android.util.Log.w("HomeViewModel", "⚠️ 警告：传入的分类ID($categoryId)与当前选中分类ID(${_selectedCategory.value!!.id})不一致！")
                    android.util.Log.w("HomeViewModel", "   传入分类名称: $categoryName")
                    android.util.Log.w("HomeViewModel", "   当前选中分类名称: ${_selectedCategory.value!!.name}")
                    android.util.Log.i("HomeViewModel", "💡 使用当前选中的分类以确保一致性")
                    
                    // 强制使用当前选中的分类
                    val currentSelected = _selectedCategory.value!!
                    val appDetails = getHourlyAppDetailsByCategory(currentSelected.id, today, hour)
                    _appDetailList.value = appDetails
                    _appDetailTitle.value = "${currentSelected.name}分类 ${hour}点应用详情"
                    _showAppDetailDialog.value = true
                    return@launch
                }
                
                android.util.Log.d("HomeViewModel", "显示${hour}点应用详情对话框: categoryId=$targetCategoryId, categoryName=$targetCategoryName")
                
                // 查询该分类下该小时的应用使用详情
                val appDetails = getHourlyAppDetailsByCategory(targetCategoryId, today, hour)
                
                _appDetailList.value = appDetails
                _appDetailTitle.value = "${targetCategoryName}分类 ${hour}点应用详情"
                _showAppDetailDialog.value = true
                
                android.util.Log.d("HomeViewModel", "${hour}点应用详情查询完成: 找到${appDetails.size}个应用")
                
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "显示${hour}点应用详情对话框失败", e)
            }
        }
    }

    /**
     * 获取指定分类指定小时的应用详情
     * @param categoryId 分类ID
     * @param date 日期字符串
     * @param hour 小时（0-23）
     * @return 应用详情列表，按使用时长排序
     */
    private suspend fun getHourlyAppDetailsByCategory(categoryId: Int, date: String, hour: Int): List<AppDetailItem> {
        return try {
            android.util.Log.d("HomeViewModel", "=== 查询特定小时应用详情 ===")
            android.util.Log.d("HomeViewModel", "查询参数: categoryId=$categoryId, date=$date, hour=$hour")
            
            // 先检查该分类是否存在
            val category = categoryDao.getAllCategoriesList().find { it.id == categoryId }
            val categoryName = category?.name ?: "未知分类"
            android.util.Log.d("HomeViewModel", "找到分类: $categoryName (ID: $categoryId)")
            
            // 计算指定小时的时间范围（时间戳）
            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            calendar.time = dateFormat.parse(date) ?: Date()
            
            // 设置到指定小时的开始时间
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val hourStartTime = calendar.timeInMillis
            
            // 设置到指定小时的结束时间（下一个小时的开始时间）
            calendar.set(Calendar.HOUR_OF_DAY, hour + 1)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val hourEndTime = calendar.timeInMillis
            
            android.util.Log.d("HomeViewModel", "目标小时: ${hour}点")
            android.util.Log.d("HomeViewModel", "时间范围: ${hourStartTime} - ${hourEndTime}")
            android.util.Log.d("HomeViewModel", "时间范围（可读）: ${Date(hourStartTime)} 至 ${Date(hourEndTime)} (不包含结束时间)")
            android.util.Log.d("HomeViewModel", "时间区间: [${hour}:00:00, ${hour+1}:00:00)")
            
            android.util.Log.d("HomeViewModel", "开始处理跨时段会话分割逻辑...")
            
            // 查询该分类下的所有会话用于调试
            val allAppSessions = appSessionUserDao.getSessionsByCatIdAndDate(categoryId, date)
            android.util.Log.d("HomeViewModel", "查询到该分类下的应用会话: ${allAppSessions.size}条")
            
            val allTimerSessions = timerSessionUserDao.getSessionsByDate(date)
                .filter { it.catId == categoryId }
            android.util.Log.d("HomeViewModel", "查询到该分类下的定时器会话: ${allTimerSessions.size}条")
            
            // 合并处理两种会话数据
            val appUsageMap = mutableMapOf<String, Triple<Int, Int, Int>>() // key -> (realUsage, virtualUsage, totalUsage)
            
            // 处理应用会话数据 - 需要正确分割跨时段的会话
            val allAppSessionsForSplitting = appSessionUserDao.getSessionsByCatIdAndDate(categoryId, date)
            allAppSessionsForSplitting.forEach { session ->
                // 计算会话的结束时间
                val sessionEndTime = session.startTime + (session.durationSec * 1000L)
                
                // 检查会话是否与当前小时有重叠
                val sessionOverlapsCurrentHour = !(sessionEndTime <= hourStartTime || session.startTime > hourEndTime)
                
                if (sessionOverlapsCurrentHour) {
                    // 计算会话在当前小时内的实际时长
                    val effectiveStartTime = maxOf(session.startTime, hourStartTime)
                    val effectiveEndTime = minOf(sessionEndTime, hourEndTime)
                    val effectiveDurationMs = effectiveEndTime - effectiveStartTime
                    val effectiveDurationSec = (effectiveDurationMs / 1000).toInt()
                    
                    if (effectiveDurationSec > 0) {
                        val key = session.pkgName
                        val currentUsage = appUsageMap[key] ?: Triple(0, 0, 0)
                        val realUsage = if (session.isOffline == 0) effectiveDurationSec else 0
                        val virtualUsage = if (session.isOffline == 1) effectiveDurationSec else 0
                        
                        appUsageMap[key] = Triple(
                            currentUsage.first + realUsage,
                            currentUsage.second + virtualUsage,
                            currentUsage.third + effectiveDurationSec
                        )
                        
                        android.util.Log.d("HomeViewModel", "${hour}点应用会话(分割后): ${session.pkgName}")
                        android.util.Log.d("HomeViewModel", "  原始: ${Date(session.startTime)} - ${Date(sessionEndTime)}, 总时长:${session.durationSec}秒")
                        android.util.Log.d("HomeViewModel", "  有效: ${Date(effectiveStartTime)} - ${Date(effectiveEndTime)}, 有效时长:${effectiveDurationSec}秒")
                        android.util.Log.d("HomeViewModel", "  类型: ${if(session.isOffline == 0) "真实" else "虚拟"}")
                        android.util.Log.d("HomeViewModel", "  计算验证: ${effectiveDurationMs}ms / 1000 = ${effectiveDurationSec}秒 = ${effectiveDurationSec/60.0}分钟")
                    }
                }
            }
            
            // 处理定时器会话数据 - 同样需要正确分割跨时段的会话
            val allTimerSessionsForSplitting = timerSessionUserDao.getSessionsByDate(date)
                .filter { it.catId == categoryId }
            allTimerSessionsForSplitting.forEach { session ->
                // 计算会话的结束时间
                val sessionEndTime = session.startTime + (session.durationSec * 1000L)
                
                // 检查会话是否与当前小时有重叠
                val sessionOverlapsCurrentHour = !(sessionEndTime <= hourStartTime || session.startTime > hourEndTime)
                
                if (sessionOverlapsCurrentHour) {
                    // 计算会话在当前小时内的实际时长
                    val effectiveStartTime = maxOf(session.startTime, hourStartTime)
                    val effectiveEndTime = minOf(sessionEndTime, hourEndTime)
                    val effectiveDurationMs = effectiveEndTime - effectiveStartTime
                    val effectiveDurationSec = (effectiveDurationMs / 1000).toInt()
                    
                    if (effectiveDurationSec > 0) {
                        val key = "timer_${session.programName}"
                        val currentUsage = appUsageMap[key] ?: Triple(0, 0, 0)
                        val realUsage = if (session.isOffline == 0) effectiveDurationSec else 0
                        val virtualUsage = if (session.isOffline == 1) effectiveDurationSec else 0
                        
                        appUsageMap[key] = Triple(
                            currentUsage.first + realUsage,
                            currentUsage.second + virtualUsage,
                            currentUsage.third + effectiveDurationSec
                        )
                        
                        android.util.Log.d("HomeViewModel", "${hour}点定时器会话(分割后): ${session.programName}")
                        android.util.Log.d("HomeViewModel", "  原始: ${Date(session.startTime)} - ${Date(sessionEndTime)}, 总时长:${session.durationSec}秒")
                        android.util.Log.d("HomeViewModel", "  有效: ${Date(effectiveStartTime)} - ${Date(effectiveEndTime)}, 有效时长:${effectiveDurationSec}秒")
                        android.util.Log.d("HomeViewModel", "  类型: ${if(session.isOffline == 0) "真实" else "虚拟"}")
                        android.util.Log.d("HomeViewModel", "  计算验证: ${effectiveDurationMs}ms / 1000 = ${effectiveDurationSec}秒 = ${effectiveDurationSec/60.0}分钟")
                    }
                }
            }
            
            if (appUsageMap.isEmpty()) {
                android.util.Log.w("HomeViewModel", "⚠️ ${hour}点没有找到该分类的使用数据")
                
                // 查询该小时是否有其他分类的使用记录
                val allCategoriesAppSessions = appSessionUserDao.getSessionsByDate(date)
                val hourlyAllAppSessions = allCategoriesAppSessions.filter { session ->
                    session.startTime >= hourStartTime && session.startTime <= hourEndTime
                }
                
                if (hourlyAllAppSessions.isNotEmpty()) {
                    // 找到其他分类的使用记录，显示提示信息
                    val otherCategoryUsage = mutableMapOf<Int, MutableList<String>>()
                    hourlyAllAppSessions.forEach { session ->
                        if (session.catId != categoryId) {
                            otherCategoryUsage.getOrPut(session.catId) { mutableListOf() }.add(session.pkgName)
                        }
                    }
                    
                    if (otherCategoryUsage.isNotEmpty()) {
                        // 创建一个特殊的提示项目
                        val allCategories = categoryDao.getAllCategoriesList()
                        val suggestionText = otherCategoryUsage.map { (catId, apps) ->
                            val otherCategoryName = allCategories.find { it.id == catId }?.name ?: "未知分类"
                            val uniqueApps = apps.distinct()
                            "${otherCategoryName}分类(${uniqueApps.size}个应用)"
                        }.joinToString("、")
                        
                        // 检查是否有OffTimes应用的记录
                        val offTimesRecords = hourlyAllAppSessions.filter { it.pkgName == "com.offtime.app" }
                        val hasOffTimesRecord = offTimesRecords.isNotEmpty()
                        
                        val tipList = mutableListOf<AppDetailItem>()
                        
                        if (hasOffTimesRecord) {
                            val offTimesCategory = allCategories.find { it.id == offTimesRecords.first().catId }?.name ?: "未知分类"
                            val offTimesTotalTime = offTimesRecords.sumOf { it.durationSec }
                            
                            tipList.add(AppDetailItem(
                                packageName = "suggestion_tip",
                                appName = "💡 检测到您在该时段主要使用了OffTimes应用",
                                categoryId = categoryId,
                                categoryName = categoryName,
                                totalUsageSeconds = 0,
                                realUsageSeconds = 0,
                                virtualUsageSeconds = 0,
                                usagePercentage = 0f
                            ))
                            
                            tipList.add(AppDetailItem(
                                packageName = "suggestion_detail",
                                appName = "OffTimes被分类到了\"${offTimesCategory}\"，使用了${formatDuration(offTimesTotalTime)}",
                                categoryId = categoryId,
                                categoryName = categoryName,
                                totalUsageSeconds = 0,
                                realUsageSeconds = 0,
                                virtualUsageSeconds = 0,
                                usagePercentage = 0f
                            ))
                        } else {
                            tipList.add(AppDetailItem(
                                packageName = "suggestion_tip",
                                appName = "💡 该时段您主要使用了其他分类的应用",
                                categoryId = categoryId,
                                categoryName = categoryName,
                                totalUsageSeconds = 0,
                                realUsageSeconds = 0,
                                virtualUsageSeconds = 0,
                                usagePercentage = 0f
                            ))
                            
                            tipList.add(AppDetailItem(
                                packageName = "suggestion_detail",
                                appName = "建议查看: $suggestionText",
                                categoryId = categoryId,
                                categoryName = categoryName,
                                totalUsageSeconds = 0,
                                realUsageSeconds = 0,
                                virtualUsageSeconds = 0,
                                usagePercentage = 0f
                            ))
                        }
                        
                        return tipList
                    }
                }
                
                // 如果确实没有任何使用记录，返回空列表
                return emptyList()
            }
            
            android.util.Log.d("HomeViewModel", "${hour}点合并统计: 共${appUsageMap.size}个项目")
            
            // 计算总使用时间用于计算占比
            val totalCategoryUsage = appUsageMap.values.sumOf { it.third }
            android.util.Log.d("HomeViewModel", "${hour}点分类总使用时长: ${totalCategoryUsage}秒 (${totalCategoryUsage/60.0}分钟)")
            
            // 转换为AppDetailItem并按总使用时长排序
            val appDetailList = appUsageMap.map { (key, usage) ->
                val (displayName, packageName) = if (key.startsWith("timer_")) {
                    // 定时器会话
                    val programName = key.substringAfter("timer_")
                    Pair("⏱️ $programName", key)
                } else {
                    // 应用会话 - 尝试获取应用名称
                    val appName = try {
                        val packageManager = context.packageManager
                        val appInfo = packageManager.getApplicationInfo(key, 0)
                        packageManager.getApplicationLabel(appInfo).toString()
                    } catch (e: Exception) {
                        // 如果获取失败，使用包名
                        key
                    }
                    Pair(appName, key)
                }
                
                val usagePercentage = if (totalCategoryUsage > 0) {
                    (usage.third.toFloat() / totalCategoryUsage * 100)
                } else 0f
                
                android.util.Log.d("HomeViewModel", "${hour}点应用详情: $displayName ($packageName), 使用${usage.third}秒, 占比${usagePercentage}%")
                
                AppDetailItem(
                    packageName = packageName,
                    appName = displayName,
                    categoryId = categoryId,
                    categoryName = categoryName,
                    totalUsageSeconds = usage.third,
                    realUsageSeconds = usage.first,
                    virtualUsageSeconds = usage.second,
                    usagePercentage = usagePercentage
                )
            }.sortedByDescending { it.totalUsageSeconds } // 按总使用时长从长到短排序
            
            android.util.Log.d("HomeViewModel", "=== ${hour}点应用详情查询完成 ===")
            android.util.Log.d("HomeViewModel", "最终结果: ${appDetailList.size}个项目，总使用时长${totalCategoryUsage}秒")
            appDetailList
            
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "查询${hour}点应用详情失败", e)
            emptyList()
        }
    }

    /**
     * 格式化时长显示
     * @param seconds 秒数
     * @return 格式化后的时长字符串，如"1小时23分"、"45分钟"、"30秒"
     */
    private fun formatDuration(seconds: Int): String {
        return when {
            seconds < 60 -> "${seconds}秒"
            seconds < 3600 -> "${seconds / 60}分钟"
            else -> {
                val hours = seconds / 3600
                val minutes = (seconds % 3600) / 60
                if (minutes > 0) {
                    "${hours}小时${minutes}分"
                } else {
                    "${hours}小时"
                }
            }
        }
    }



    /**
     * 获取指定小时所有应用的详情（用于总使用分类的柱状图点击）
     * @param date 日期字符串
     * @param hour 小时（0-23）
     * @return 应用详情列表，按使用时长排序
     */
    private suspend fun getHourlyAllAppsDetails(date: String, hour: Int): List<AppDetailItem> {
        return try {
            android.util.Log.d("HomeViewModel", "=== 查询指定小时所有应用详情 ===")
            android.util.Log.d("HomeViewModel", "查询参数: date=$date, hour=$hour")
            
            // 计算指定小时的时间范围（时间戳）
            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            calendar.time = dateFormat.parse(date) ?: Date()
            
            // 设置到指定小时的开始时间
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val hourStartTime = calendar.timeInMillis
            
            // 设置到指定小时的结束时间（下一个小时的开始时间）
            calendar.set(Calendar.HOUR_OF_DAY, hour + 1)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val hourEndTime = calendar.timeInMillis
            
            android.util.Log.d("HomeViewModel", "目标小时: ${hour}点")
            android.util.Log.d("HomeViewModel", "时间范围: ${hourStartTime} - ${hourEndTime}")
            android.util.Log.d("HomeViewModel", "时间范围（可读）: ${Date(hourStartTime)} 至 ${Date(hourEndTime)} (不包含结束时间)")
            
            // 获取有效分类列表
            val allCategories = categoryDao.getAllCategoriesList()
            val validCategories = allCategories.filter { it.id > 0 }
            android.util.Log.d("HomeViewModel", "有效分类: ${validCategories.map { "${it.name}(${it.id})" }}")
            
            // 建立包名到分类的映射
            val packageToCategoryMap = mutableMapOf<String, Pair<Int, String>>()
            
            // 处理应用会话数据
            val appUsageMap = mutableMapOf<String, Triple<Int, Int, Int>>() // key -> (realUsage, virtualUsage, totalUsage)
            
            // 查询该小时的所有应用会话
            val allAppSessions = appSessionUserDao.getSessionsByDate(date)
            android.util.Log.d("HomeViewModel", "查询到的应用会话总数: ${allAppSessions.size}")
            
            allAppSessions.forEach { session ->
                // 只处理有效分类的会话
                if (validCategories.any { it.id == session.catId }) {
                    // 记录包名到分类的映射
                    val category = validCategories.find { it.id == session.catId }
                    if (category != null) {
                        packageToCategoryMap[session.pkgName] = Pair(category.id, category.name)
                    }
                    
                    // 计算会话的结束时间
                    val sessionEndTime = session.startTime + (session.durationSec * 1000L)
                    
                    // 检查会话是否与当前小时有重叠
                    val sessionOverlapsCurrentHour = !(sessionEndTime <= hourStartTime || session.startTime > hourEndTime)
                    
                    if (sessionOverlapsCurrentHour) {
                        // 计算会话在当前小时内的实际时长
                        val effectiveStartTime = maxOf(session.startTime, hourStartTime)
                        val effectiveEndTime = minOf(sessionEndTime, hourEndTime)
                        val effectiveDurationMs = effectiveEndTime - effectiveStartTime
                        val effectiveDurationSec = (effectiveDurationMs / 1000).toInt()
                        
                        if (effectiveDurationSec > 0) {
                            val key = session.pkgName
                            val currentUsage = appUsageMap[key] ?: Triple(0, 0, 0)
                            val realUsage = if (session.isOffline == 0) effectiveDurationSec else 0
                            val virtualUsage = if (session.isOffline == 1) effectiveDurationSec else 0
                            
                            appUsageMap[key] = Triple(
                                currentUsage.first + realUsage,
                                currentUsage.second + virtualUsage,
                                currentUsage.third + effectiveDurationSec
                            )
                            
                            android.util.Log.d("HomeViewModel", "${hour}点应用会话: ${session.pkgName}, 分类:${category?.name}, 有效时长:${effectiveDurationSec}秒")
                        }
                    }
                }
            }
            
            // 查询该小时的所有定时器会话
            val allTimerSessions = timerSessionUserDao.getSessionsByDate(date)
            android.util.Log.d("HomeViewModel", "查询到的定时器会话总数: ${allTimerSessions.size}")
            
            allTimerSessions.forEach { session ->
                // 只处理有效分类的会话
                if (validCategories.any { it.id == session.catId }) {
                    // 记录定时器到分类的映射
                    val category = validCategories.find { it.id == session.catId }
                    if (category != null) {
                        val key = "timer_${session.programName}"
                        packageToCategoryMap[key] = Pair(category.id, category.name)
                    }
                    
                    // 计算会话的结束时间
                    val sessionEndTime = session.startTime + (session.durationSec * 1000L)
                    
                    // 检查会话是否与当前小时有重叠
                    val sessionOverlapsCurrentHour = !(sessionEndTime <= hourStartTime || session.startTime > hourEndTime)
                    
                    if (sessionOverlapsCurrentHour) {
                        // 计算会话在当前小时内的实际时长
                        val effectiveStartTime = maxOf(session.startTime, hourStartTime)
                        val effectiveEndTime = minOf(sessionEndTime, hourEndTime)
                        val effectiveDurationMs = effectiveEndTime - effectiveStartTime
                        val effectiveDurationSec = (effectiveDurationMs / 1000).toInt()
                        
                        if (effectiveDurationSec > 0) {
                            val key = "timer_${session.programName}"
                            val currentUsage = appUsageMap[key] ?: Triple(0, 0, 0)
                            val realUsage = if (session.isOffline == 0) effectiveDurationSec else 0
                            val virtualUsage = if (session.isOffline == 1) effectiveDurationSec else 0
                            
                            appUsageMap[key] = Triple(
                                currentUsage.first + realUsage,
                                currentUsage.second + virtualUsage,
                                currentUsage.third + effectiveDurationSec
                            )
                            
                            android.util.Log.d("HomeViewModel", "${hour}点定时器会话: ${session.programName}, 分类:${category?.name}, 有效时长:${effectiveDurationSec}秒")
                        }
                    }
                }
            }
            
            if (appUsageMap.isEmpty()) {
                android.util.Log.w("HomeViewModel", "⚠️ ${hour}点没有找到任何应用使用数据")
                return emptyList()
            }
            
            android.util.Log.d("HomeViewModel", "${hour}点合并统计: 共${appUsageMap.size}个项目")
            
            // 计算总使用时间用于计算占比
            val totalUsage = appUsageMap.values.sumOf { it.third }
            android.util.Log.d("HomeViewModel", "${hour}点总使用时长: ${totalUsage}秒 (${totalUsage/60.0}分钟)")
            
            // 转换为AppDetailItem并按总使用时长排序
            val appDetailList = appUsageMap.map { (key, usage) ->
                val (displayName, packageName) = if (key.startsWith("timer_")) {
                    // 定时器会话
                    val programName = key.substringAfter("timer_")
                    Pair("⏱️ $programName", key)
                } else {
                    // 应用会话 - 尝试获取应用名称
                    val appName = try {
                        val packageManager = context.packageManager
                        val appInfo = packageManager.getApplicationInfo(key, 0)
                        packageManager.getApplicationLabel(appInfo).toString()
                    } catch (e: Exception) {
                        // 如果获取失败，使用包名
                        key
                    }
                    Pair(appName, key)
                }
                
                val usagePercentage = if (totalUsage > 0) {
                    (usage.third.toFloat() / totalUsage * 100)
                } else 0f
                
                val categoryInfo = packageToCategoryMap[key] ?: Pair(-1, "未知分类")
                
                android.util.Log.d("HomeViewModel", "${hour}点应用详情: $displayName ($packageName), 分类:${categoryInfo.second}, 使用${usage.third}秒, 占比${usagePercentage}%")
                
                AppDetailItem(
                    packageName = packageName,
                    appName = displayName,
                    categoryId = categoryInfo.first,
                    categoryName = categoryInfo.second,
                    totalUsageSeconds = usage.third,
                    realUsageSeconds = usage.first,
                    virtualUsageSeconds = usage.second,
                    usagePercentage = usagePercentage
                )
            }.sortedByDescending { it.totalUsageSeconds } // 按总使用时长从长到短排序
            
            android.util.Log.d("HomeViewModel", "=== ${hour}点所有应用详情查询完成 ===")
            android.util.Log.d("HomeViewModel", "最终结果: ${appDetailList.size}个应用，总使用时长${totalUsage}秒")
            appDetailList
            
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "查询${hour}点所有应用详情失败", e)
            emptyList()
        }
    }

    // ============ 线下计时功能 ============

    /**
     * 显示计时器对话框
     */
    fun showTimerDialog() {
        _showTimerDialog.value = true
    }
    
    /**
     * 隐藏计时器对话框
     */
    fun hideTimerDialog() {
        _showTimerDialog.value = false
        
        // 如果计时器正在运行，停止计时
        if (_isTimerRunning.value) {
            stopOfflineTimer()
        }
    }
    
    /**
     * 进入背景计时模式（线下计时）
     */
    fun enterTimerBackgroundMode() {
        _showTimerDialog.value = false
        _isTimerInBackground.value = true
        android.util.Log.d("HomeViewModel", "线下计时进入背景模式")
    }
    
    /**
     * 退出背景计时模式（线下计时）
     */
    fun exitTimerBackgroundMode() {
        _isTimerInBackground.value = false
        _showTimerDialog.value = true
        android.util.Log.d("HomeViewModel", "线下计时退出背景模式")
    }
    
    /**
     * 开始线下计时
     */
    fun startOfflineTimer() {
        viewModelScope.launch {
            try {
                val currentCategory = _selectedCategory.value ?: return@launch
                val categoryId = currentCategory.id
                
                android.util.Log.d("HomeViewModel", "开始线下计时: 分类=${currentCategory.name}")
                
                // 创建计时器会话
                val session = timerSessionRepository.startTimer(categoryId)
                if (session != null) {
                    val startTime = System.currentTimeMillis()
                    
                    // 启动UI更新循环
                    val timerJob = viewModelScope.launch {
                        while (isActive) {
                            delay(1000)
                            
                            val currentState = getCategoryTimerState(categoryId)
                            if (currentState.isRunning && !currentState.isPaused) {
                                val elapsed = (System.currentTimeMillis() - currentState.startTime - currentState.pausedDuration) / 1000
                                val newState = currentState.copy(
                                    seconds = (elapsed % 60).toInt(),
                                    minutes = ((elapsed / 60) % 60).toInt(),
                                    hours = (elapsed / 3600).toInt(),
                                    secondsUnit = (elapsed % 60).toInt()
                                )
                                updateCategoryTimerState(categoryId, newState)
                            }
                        }
                    }
                    
                    // 更新该分类的计时器状态
                    val newState = CategoryTimerState(
                        isRunning = true,
                        isPaused = false,
                        seconds = 0,
                        hours = 0,
                        minutes = 0,
                        secondsUnit = 0,
                        sessionId = session.id,
                        startTime = startTime,
                        pausedDuration = 0,
                        lastPauseTime = 0,
                        timerJob = timerJob
                    )
                    updateCategoryTimerState(categoryId, newState)
                    
                    // 启动前台服务进行后台计时
                    com.offtime.app.service.OfflineTimerService.startTimer(
                        context,
                        categoryId,
                        currentCategory.name,
                        session.id
                    )
                    
                    // 更新兼容性字段
                    currentTimerSessionId = session.id
                    currentTimingCategoryId = categoryId
                    
                    android.util.Log.d("HomeViewModel", "线下计时开始成功: 分类=${currentCategory.name}, sessionId=${session.id}，前台服务已启动")
                } else {
                    android.util.Log.e("HomeViewModel", "创建计时器会话失败")
                }
                
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "开始线下计时失败", e)
            }
        }
    }
    
    /**
     * 暂停线下计时
     */
    fun pauseOfflineTimer() {
        val currentCategory = _selectedCategory.value ?: return
        val categoryId = currentCategory.id
        android.util.Log.d("HomeViewModel", "暂停线下计时: 分类=${currentCategory.name}")
        
        // 通知服务暂停计时
        com.offtime.app.service.OfflineTimerService.pauseTimer(context)
        
        // 更新该分类的计时器状态
        val currentState = getCategoryTimerState(categoryId)
        if (currentState.isRunning && !currentState.isPaused) {
            val newState = currentState.copy(
                isPaused = true,
                lastPauseTime = System.currentTimeMillis()
            )
            updateCategoryTimerState(categoryId, newState)
        }
    }
    
    /**
     * 继续线下计时
     */
    fun resumeOfflineTimer() {
        val currentCategory = _selectedCategory.value ?: return
        val categoryId = currentCategory.id
        android.util.Log.d("HomeViewModel", "继续线下计时: 分类=${currentCategory.name}")
        
        // 通知服务恢复计时
        com.offtime.app.service.OfflineTimerService.resumeTimer(context)
        
        // 更新该分类的计时器状态
        val currentState = getCategoryTimerState(categoryId)
        if (currentState.isRunning && currentState.isPaused) {
            val pauseDuration = System.currentTimeMillis() - currentState.lastPauseTime
            val newState = currentState.copy(
                isPaused = false,
                pausedDuration = currentState.pausedDuration + pauseDuration,
                lastPauseTime = 0
            )
            updateCategoryTimerState(categoryId, newState)
        }
    }
    
    /**
     * 停止线下计时并保存数据
     */
    fun stopOfflineTimer() {
        viewModelScope.launch {
            try {
                val currentCategory = _selectedCategory.value ?: return@launch
                val categoryId = currentCategory.id
                android.util.Log.d("HomeViewModel", "停止线下计时: 分类=${currentCategory.name}")
                
                // 停止前台服务
                com.offtime.app.service.OfflineTimerService.stopTimer(context)
                
                // 停止该分类的计时器状态
                val currentState = getCategoryTimerState(categoryId)
                if (currentState.isRunning) {
                    // 停止UI更新循环
                    currentState.timerJob?.cancel()
                    
                    // 重置该分类的计时器状态
                    val resetState = CategoryTimerState()
                    updateCategoryTimerState(categoryId, resetState)
                    
                    android.util.Log.d("HomeViewModel", "线下计时停止，数据由服务自动保存")
                    
                    // 刷新数据显示
                    loadUsageData(categoryId)
                } else {
                    android.util.Log.w("HomeViewModel", "该分类没有正在运行的计时器")
                }
                
                _showTimerDialog.value = false
                _isTimerInBackground.value = false
                
                // 更新兼容性字段
                if (currentTimingCategoryId == categoryId) {
                    currentTimerSessionId = null
                    currentTimingCategoryId = null
                    pausedDuration = 0
                    timerJob?.cancel()
                    timerJob = null
                }
                
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "停止线下计时失败", e)
            }
        }
    }
    
    /**
     * 开始UI更新循环（仅用于界面显示，实际计时由服务负责）
     */
    private fun startTimerUIUpdateLoop() {
        timerJob = viewModelScope.launch {
            while (isActive && _isTimerRunning.value) {
                // 从数据库获取最新的计时进度
                currentTimerSessionId?.let { sessionId ->
                    try {
                        val session = timerSessionUserDao.getUserSessionById(sessionId)
                        if (session != null) {
                            val elapsedSeconds = session.durationSec
                            
                            // 更新UI显示
                            _timerSeconds.value = elapsedSeconds
                            
                            // 更新独立的时间单位显示
                            val hours = elapsedSeconds / 3600
                            val minutes = (elapsedSeconds % 3600) / 60
                            val seconds = elapsedSeconds % 60
                            
                            _timerHours.value = hours
                            _timerMinutes.value = minutes
                            _timerSecondsUnit.value = seconds
                            
                            android.util.Log.d("HomeViewModel", "UI更新: 当前计时 ${elapsedSeconds}秒")
                        } else {
                            android.util.Log.w("HomeViewModel", "找不到计时会话: $sessionId")
                            return@launch  // 使用return@launch替代break
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("HomeViewModel", "更新UI计时显示失败", e)
                    }
                }
                
                kotlinx.coroutines.delay(1000) // 每秒更新一次UI
            }
        }
    }
    
    /**
     * 格式化计时器显示文本
     * @param totalSeconds 总秒数
     * @return 格式化的时间字符串，如"1时23分45秒"
     */
    private fun formatTimerDisplay(totalSeconds: Int): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return "${hours}时${minutes}分${seconds}秒"
    }
    
    /**
     * 判断当前选中的分类是否正在计时
     */
    fun isCurrentCategoryTiming(): Boolean {
        val selectedCategoryId = _selectedCategory.value?.id
        return if (selectedCategoryId != null) {
            val categoryState = getCategoryTimerState(selectedCategoryId)
            categoryState.isRunning
        } else {
            false
        }
    }
    
    /**
     * 判断是否有任何分类正在计时
     */
    fun isAnyTimerRunning(): Boolean {
        return categoryTimerStates.values.any { it.isRunning }
    }
    
    /**
     * 判断当前选中的分类是否正在定时
     */
    fun isCurrentCategoryCountdownTiming(): Boolean {
        val selectedCategoryId = _selectedCategory.value?.id
        return selectedCategoryId != null && 
               selectedCategoryId == currentCountdownTimingCategoryId && 
               _isCountdownTimerRunning.value
    }
    
    /**
     * 判断是否有任何分类正在定时
     */
    fun isAnyCountdownTimerRunning(): Boolean {
        return currentCountdownTimingCategoryId != null && _isCountdownTimerRunning.value
    }

    // ============ 定时计时功能 ============

    /**
     * 显示定时计时器对话框
     */
    fun showCountdownDialog() {
        _showCountdownDialog.value = true
    }
    
    /**
     * 隐藏定时计时器对话框
     */
    fun hideCountdownDialog() {
        _showCountdownDialog.value = false
        
        // 如果定时器正在运行，停止计时
        if (_isCountdownTimerRunning.value) {
            stopCountdownTimer()
        }
    }
    
    /**
     * 进入背景计时模式（定时计时）
     */
    fun enterCountdownBackgroundMode() {
        _showCountdownDialog.value = false
        _showTimerDialog.value = false  // 关闭Timer弹窗
        _isCountdownInBackground.value = true

    }
    
    /**
     * 退出背景计时模式（定时计时）
     */
    fun exitCountdownBackgroundMode() {
        _isCountdownInBackground.value = false
        _showTimerDialog.value = true  // 显示Timer对话框而不是独立的CountDown对话框
        _defaultTimerTab.value = 1     // 默认显示CountDown标签页

    }
    
    /**
     * 测试定时器结束提醒音
     */
    fun testCountdownFinishedSound() {
        try {
            val soundManager = com.offtime.app.utils.SoundManager.getInstance(context)
            soundManager.playCountdownFinishedSound()
            android.util.Log.d("HomeViewModel", "测试定时器结束提醒音已播放")
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "播放测试提醒音失败", e)
        }
    }

    /**
     * 设置定时时长
     */
    fun setCountdownDuration(minutes: Int) {
        if (!_isCountdownTimerRunning.value) {
            val seconds = minutes * 60
            initialCountdownSeconds = seconds
            _countdownTimerSeconds.value = seconds
            _countdownInitialMinutes.value = minutes
            
            // 更新显示
            updateCountdownDisplay(seconds)
            
            android.util.Log.d("HomeViewModel", "设置定时时长: ${minutes}分钟")
        }
    }
    
    /**
     * 开始定时计时
     */
    fun startCountdownTimer() {
        viewModelScope.launch {
            try {
                val currentCategory = _selectedCategory.value ?: return@launch
                
                android.util.Log.d("HomeViewModel", "开始定时计时: 分类=${currentCategory.name}, 时长=${_countdownInitialMinutes.value}分钟")
                
                // 创建计时器会话
                val session = timerSessionRepository.startTimer(currentCategory.id)
                if (session != null) {
                    currentCountdownSessionId = session.id
                    currentCountdownTimingCategoryId = currentCategory.id // 记录正在定时的分类ID
                    countdownStartTime = System.currentTimeMillis()
                    countdownPausedDuration = 0
                    
                    _isCountdownTimerRunning.value = true
                    _isCountdownTimerPaused.value = false
                    
                    // 开始倒计时循环
                    startCountdownLoop()
                    
                    android.util.Log.d("HomeViewModel", "定时计时开始成功: sessionId=${session.id}")
                } else {
                    android.util.Log.e("HomeViewModel", "创建定时计时器会话失败")
                }
                
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "开始定时计时失败", e)
            }
        }
    }
    
    /**
     * 暂停定时计时
     */
    fun pauseCountdownTimer() {
        android.util.Log.d("HomeViewModel", "暂停定时计时")
        
        lastCountdownPauseTime = System.currentTimeMillis()
        _isCountdownTimerPaused.value = true
        
        // 停止倒计时循环
        countdownJob?.cancel()
        countdownJob = null
    }
    
    /**
     * 继续定时计时
     */
    fun resumeCountdownTimer() {
        android.util.Log.d("HomeViewModel", "继续定时计时")
        
        // 累计暂停时间
        countdownPausedDuration += System.currentTimeMillis() - lastCountdownPauseTime
        _isCountdownTimerPaused.value = false
        
        // 重新开始倒计时循环
        startCountdownLoop()
    }
    
    /**
     * 停止定时计时并保存数据
     */
    fun stopCountdownTimer() {
        viewModelScope.launch {
            try {
                android.util.Log.d("HomeViewModel", "停止定时计时")
                
                // 停止倒计时循环
                countdownJob?.cancel()
                countdownJob = null
                
                val sessionId = currentCountdownSessionId
                if (sessionId != null) {
                    // 计算实际使用时长（初始时长 - 剩余时长）
                    val remainingSeconds = _countdownTimerSeconds.value
                    val actualUsedSeconds = initialCountdownSeconds - remainingSeconds
                    
                    android.util.Log.d("HomeViewModel", "定时计时数据: 初始${initialCountdownSeconds}秒, 剩余${remainingSeconds}秒, 实际使用${actualUsedSeconds}秒")
                    
                    // 保存到数据库（使用实际使用时长）
                    val success = timerSessionRepository.stopTimer(sessionId, actualUsedSeconds)
                    if (success) {
                        android.util.Log.d("HomeViewModel", "定时计时数据保存成功: 时长=${actualUsedSeconds}秒")
                        
                        // 刷新数据显示
                        _selectedCategory.value?.let { category ->
                            loadUsageData(category.id)
                        }
                    } else {
                        android.util.Log.e("HomeViewModel", "保存定时计时数据失败")
                    }
                } else {
                    android.util.Log.w("HomeViewModel", "没有找到当前定时计时会话ID")
                }
                
                // 重置状态
                resetCountdownState()
                
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "停止定时计时失败", e)
            }
        }
    }
    
    /**
     * 开始倒计时循环（每秒更新一次）
     */
    private fun startCountdownLoop() {
        countdownJob = viewModelScope.launch {
            while (_isCountdownTimerRunning.value && !_isCountdownTimerPaused.value) {
                val currentTime = System.currentTimeMillis()
                val elapsedSeconds = ((currentTime - countdownStartTime - countdownPausedDuration) / 1000).toInt()
                val remainingSeconds = (initialCountdownSeconds - elapsedSeconds).coerceAtLeast(0)
                
                _countdownTimerSeconds.value = remainingSeconds
                updateCountdownDisplay(remainingSeconds)
                
                // 更新数据库中的会话时长（使用已经过的时间）
                currentCountdownSessionId?.let { sessionId ->
                    timerSessionRepository.updateTimer(sessionId, elapsedSeconds)
                }
                
                // 如果倒计时结束，自动停止
                if (remainingSeconds <= 0) {
                    android.util.Log.d("HomeViewModel", "倒计时结束，自动停止")
                    
                    // 播放定时器结束提醒音
                    try {
                        val soundManager = com.offtime.app.utils.SoundManager.getInstance(context)
                        soundManager.playCountdownFinishedSound()
                        android.util.Log.d("HomeViewModel", "定时器结束提醒音已播放")
                    } catch (e: Exception) {
                        android.util.Log.e("HomeViewModel", "播放定时器结束提醒音失败", e)
                    }
                    
                    stopCountdownTimer()
                    break
                }
                
                kotlinx.coroutines.delay(1000) // 每秒更新一次
            }
        }
    }
    
    /**
     * 更新倒计时显示
     */
    private fun updateCountdownDisplay(totalSeconds: Int) {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        
        _countdownHours.value = hours
        _countdownMinutes.value = minutes
        _countdownSecondsUnit.value = seconds
    }
    
    /**
     * 重置定时计时状态
     */
    private fun resetCountdownState() {
        _isCountdownTimerRunning.value = false
        _isCountdownTimerPaused.value = false
        _showCountdownDialog.value = false
        _isCountdownInBackground.value = false
        currentCountdownSessionId = null
        currentCountdownTimingCategoryId = null // 清除正在定时的分类ID
        countdownPausedDuration = 0
        
        // 重置为默认时长
        val defaultSeconds = _countdownInitialMinutes.value * 60
        _countdownTimerSeconds.value = defaultSeconds
        initialCountdownSeconds = defaultSeconds
        updateCountdownDisplay(defaultSeconds)
    }
    
    // ================== 订阅相关方法 ==================
    
    /**
     * 获取用户订阅信息
     */
    suspend fun getSubscriptionInfo(): UserRepository.SubscriptionInfo? {
        return try {
            userRepository.getUserSubscriptionInfo()
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "获取订阅信息失败", e)
            null
        }
    }

    /**
     * 从昨日详细数据更新奖励文本
     */
    private suspend fun updateRewardTextFromYesterdayDetailData(categoryId: Int, date: String) {
        try {
            // 先计算昨日详细数据（如果还没有）
            if (_yesterdayDetailData.value == null || 
                _yesterdayDetailData.value?.categoryName != _selectedCategory.value?.name) {
                
                val realUsageSeconds = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, date, 0) ?: 0
                val virtualUsageSeconds = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, date, 1) ?: 0
                val goal = goalRewardPunishmentUserDao.getUserGoalByCatId(categoryId)
                val goalSeconds = goal?.dailyGoalMin?.times(60) ?: 7200 // 默认2小时
                
                if (goal != null) {
                    val totalUsageSeconds = realUsageSeconds + virtualUsageSeconds
                    val rewardContent = calculateRewardText(
                        categoryId = categoryId,
                        usageSeconds = totalUsageSeconds,
                        goalSeconds = goalSeconds,
                        baseRewardText = goal.rewardText,
                        conditionType = goal.conditionType
                    )
                    
                    // 更新奖励文本
                    _rewardText.value = rewardContent
                    android.util.Log.d("HomeViewModel", "从昨日详细数据更新奖励文本: $rewardContent")
                }
            } else {
                // 直接使用已有的昨日详细数据
                val yesterdayData = _yesterdayDetailData.value
                if (yesterdayData != null && yesterdayData.rewardContent.isNotBlank()) {
                    _rewardText.value = yesterdayData.rewardContent
                    android.util.Log.d("HomeViewModel", "使用已有昨日详细数据的奖励文本: ${yesterdayData.rewardContent}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "从昨日详细数据更新奖励文本失败", e)
        }
    }

    /**
     * 从昨日详细数据更新惩罚文本
     */
    private suspend fun updatePunishmentTextFromYesterdayDetailData(categoryId: Int, date: String) {
        try {
            // 先计算昨日详细数据（如果还没有）
            if (_yesterdayDetailData.value == null || 
                _yesterdayDetailData.value?.categoryName != _selectedCategory.value?.name) {
                
                val realUsageSeconds = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, date, 0) ?: 0
                val virtualUsageSeconds = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, date, 1) ?: 0
                val goal = goalRewardPunishmentUserDao.getUserGoalByCatId(categoryId)
                val goalSeconds = goal?.dailyGoalMin?.times(60) ?: 7200 // 默认2小时
                
                if (goal != null) {
                    val totalUsageSeconds = realUsageSeconds + virtualUsageSeconds
                    val punishmentContent = calculatePunishmentText(
                        categoryId = categoryId,
                        usageSeconds = totalUsageSeconds,
                        goalSeconds = goalSeconds,
                        basePunishText = goal.punishText,
                        conditionType = goal.conditionType
                    )
                    
                    // 更新惩罚文本
                    _punishText.value = punishmentContent
                    android.util.Log.d("HomeViewModel", "从昨日详细数据更新惩罚文本: $punishmentContent")
                } else {
                    // 如果没有目标配置，使用默认计算
                    val totalUsageSeconds = realUsageSeconds + virtualUsageSeconds
                    val defaultGoalSeconds = 7200 // 默认2小时
                    if (totalUsageSeconds > defaultGoalSeconds) {
                        val overSeconds = totalUsageSeconds - defaultGoalSeconds
                        val overHours = kotlin.math.ceil(overSeconds.toDouble() / 3600.0).toInt()
                        val punishmentCount = overHours * 30 // 每小时30个俯卧撑
                        
                        // 使用本地化的惩罚文本和单位
                        val localizedPunishText = UnifiedTextManager.getStandardPunishmentText()
                        val localizedPunishUnit = UnifiedTextManager.getStandardPunishmentUnit()
                        val punishmentContent = DefaultValueLocalizer.localizePunishmentDescription(context, localizedPunishText, punishmentCount, localizedPunishUnit)
                        _punishText.value = punishmentContent
                        android.util.Log.d("HomeViewModel", "使用默认计算的惩罚文本: $punishmentContent")
                    }
                }
            } else {
                // 直接使用已有的昨日详细数据
                val yesterdayData = _yesterdayDetailData.value
                if (yesterdayData != null && yesterdayData.punishmentContent.isNotBlank()) {
                    _punishText.value = yesterdayData.punishmentContent
                    android.util.Log.d("HomeViewModel", "使用已有昨日详细数据的惩罚文本: ${yesterdayData.punishmentContent}")
                } else {
                    android.util.Log.w("HomeViewModel", "昨日详细数据中惩罚内容为空: ${yesterdayData?.punishmentContent}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "从昨日详细数据更新惩罚文本失败", e)
        }
    }









    /**
     * 从昨日详细数据直接提取奖励数量
     * 
     * 这是奖罚机制的核心函数之一，确保奖励数量与昨日详细数据保持一致。
     * 直接从已计算好的rewardContent中提取数量，确保与Yesterday's Details一致。
     * 
     * 工作流程：
     * 1. 获取已缓存的昨日详细数据
     * 2. 使用正则表达式从奖励内容中提取数字
     * 3. 返回提取的数量，失败时返回默认值1
     * 
     * 数据源优先级：
     * - 昨日详细数据中的rewardContent（最高优先级）
     * - 默认值1（兜底保障）
     * 
     * @param categoryId 分类ID（当前未使用，为将来扩展预留）
     * @return 奖励数量，最小值为1
     */
    private suspend fun calculateRewardCountFromYesterdayData(@Suppress("UNUSED_PARAMETER") categoryId: Int): Int {
        return try {
            val yesterdayData = _yesterdayDetailData.value
            if (yesterdayData == null) {
                android.util.Log.w("HomeViewModel", "昨日详细数据为空，使用默认奖励数量")
                return 1
            }

            // 直接从昨日详细数据的rewardContent中提取数量
            if (yesterdayData.rewardContent.isNotBlank()) {
                val regex = Regex("(\\d+)")
                val matchResult = regex.find(yesterdayData.rewardContent)
                val extractedNumber = matchResult?.groupValues?.get(1)?.toInt()
                
                if (extractedNumber != null && extractedNumber > 0) {
                    android.util.Log.d("HomeViewModel", "从昨日详细数据提取奖励数量: ${yesterdayData.rewardContent} -> $extractedNumber")
                    return extractedNumber
                }
            }
            
            // 如果提取失败，回退到默认值
            android.util.Log.w("HomeViewModel", "无法从奖励内容中提取数量: ${yesterdayData.rewardContent}，使用默认值")
            return 1
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "从昨日详细数据提取奖励数量失败", e)
            1
        }
    }

    /**
     * 从昨日详细数据直接提取惩罚数量
     * 
     * 这是奖罚机制的核心函数之一，确保惩罚数量与昨日详细数据保持一致。
     * 直接从已计算好的punishmentContent中提取数量，确保与Yesterday's Details一致。
     * 
     * 工作流程：
     * 1. 获取已缓存的昨日详细数据
     * 2. 使用正则表达式从惩罚内容中提取数字
     * 3. 返回提取的数量，失败时返回默认值30
     * 
     * 数据源优先级：
     * - 昨日详细数据中的punishmentContent（最高优先级）
     * - 默认值30（兜底保障）
     * 
     * @param categoryId 分类ID（当前未使用，为将来扩展预留）
     * @return 惩罚数量，默认值为30
     */
    private suspend fun calculatePunishmentCountFromYesterdayData(@Suppress("UNUSED_PARAMETER") categoryId: Int): Int {
        return try {
            val yesterdayData = _yesterdayDetailData.value
            if (yesterdayData == null) {
                android.util.Log.w("HomeViewModel", "昨日详细数据为空，使用默认惩罚数量")
                return 30
            }

            // 直接从昨日详细数据的punishmentContent中提取数量
            if (yesterdayData.punishmentContent.isNotBlank()) {
                val regex = Regex("(\\d+)")
                val matchResult = regex.find(yesterdayData.punishmentContent)
                val extractedNumber = matchResult?.groupValues?.get(1)?.toInt()
                
                if (extractedNumber != null && extractedNumber > 0) {
                    android.util.Log.d("HomeViewModel", "从昨日详细数据提取惩罚数量: ${yesterdayData.punishmentContent} -> $extractedNumber")
                    return extractedNumber
                }
            }
            
            // 如果提取失败，回退到默认值
            android.util.Log.w("HomeViewModel", "无法从惩罚内容中提取数量: ${yesterdayData.punishmentContent}，使用默认值")
            return 30
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "从昨日详细数据提取惩罚数量失败", e)
            30
        }
    }

} 