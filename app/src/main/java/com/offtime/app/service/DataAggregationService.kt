package com.offtime.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.offtime.app.data.dao.AppSessionUserDao
import com.offtime.app.data.dao.TimerSessionUserDao
import com.offtime.app.data.dao.DailyUsageDao
import com.offtime.app.data.dao.SummaryUsageDao
import com.offtime.app.data.dao.AppCategoryDao
import com.offtime.app.data.dao.AppInfoDao
import com.offtime.app.data.dao.RewardPunishmentUserDao
import com.offtime.app.data.dao.RewardPunishmentWeekUserDao
import com.offtime.app.data.dao.RewardPunishmentMonthUserDao
import com.offtime.app.data.entity.AppSessionUserEntity
import com.offtime.app.data.entity.DailyUsageUserEntity
import com.offtime.app.data.entity.SummaryUsageUserEntity
import com.offtime.app.data.entity.SummaryUsageWeekUserEntity
import com.offtime.app.data.entity.SummaryUsageMonthUserEntity
import com.offtime.app.data.entity.TimerSessionUserEntity
import com.offtime.app.data.entity.RewardPunishmentWeekUserEntity
import com.offtime.app.data.entity.RewardPunishmentMonthUserEntity
import com.offtime.app.utils.LogUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import com.offtime.app.widget.OffTimeLockScreenWidget
import com.offtime.app.R

/**
 * 数据聚合服务
 * 
 * 核心职责：
 * 1. 将原始会话数据(app_sessions_user)聚合到日统计表(daily_usage_user)
 * 2. 将日统计数据聚合到汇总表(summary_usage_user)
 * 3. 生成周统计和月统计数据
 * 4. 计算奖罚完成度
 * 5. 确保所有时间周期都有基础记录
 * 
 * 数据流转路径：
 * app_sessions_user → daily_usage_user → summary_usage_user → 周/月汇总表
 * 
 * 触发时机：
 * - UsageStatsCollectorService收集到新数据时
 * - 每日凌晨定时任务
 * - 用户手动触发刷新
 * - 应用启动时确保基础记录
 * 
 * 性能优化：
 * - 使用协程进行异步处理
 * - 分步骤聚合，避免大量数据一次性处理
 * - 增量更新，只处理有变化的数据
 */
@AndroidEntryPoint
class DataAggregationService : Service() {
    
    companion object {
        const val ACTION_AGGREGATE_DATA = "com.offtime.app.AGGREGATE_DATA"
        const val ACTION_ENSURE_BASE_RECORDS = "com.offtime.app.ENSURE_BASE_RECORDS"
        const val ACTION_CLEAN_HISTORICAL_DATA = "com.offtime.app.CLEAN_HISTORICAL_DATA"
        const val ACTION_PROCESS_HISTORICAL_DATA = "com.offtime.app.PROCESS_HISTORICAL_DATA"
        const val ACTION_CLEAN_DUPLICATE_SESSIONS = "com.offtime.app.CLEAN_DUPLICATE_SESSIONS"
        private const val TAG = "DataAggregationService"
        private const val CHANNEL_ID = "data_aggregation_channel"
        private const val NOTIFICATION_ID = 2011
        
        /**
         * 手动触发数据聚合
         * 
         * 使用场景：
         * - 用户刷新统计界面时
         * - 检测到大量新数据时
         * - 数据不一致时的修复操作
         */
        fun triggerAggregation(context: android.content.Context) {
            val intent = android.content.Intent(context, DataAggregationService::class.java)
            intent.action = ACTION_AGGREGATE_DATA
            
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                LogUtils.i(TAG, "手动触发数据聚合服务")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "启动数据聚合服务失败", e)
            }
        }
        
        /**
         * 确保基础记录存在
         * 
         * 使用场景：
         * - 应用首次启动
         * - 新的一天开始
         * - 添加新分类后
         * - 数据库升级后
         */
        fun ensureBaseRecords(context: android.content.Context) {
            val intent = android.content.Intent(context, DataAggregationService::class.java)
            intent.action = ACTION_ENSURE_BASE_RECORDS
            
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                LogUtils.i(TAG, "手动触发基础记录生成服务")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "启动基础记录生成服务失败", e)
            }
        }

        /**
         * 清理历史错误数据并重新聚合
         * 
         * 使用场景：
         * - 修复之前重复计算导致的数据错误
         * - 单小时超过60分钟的异常数据清理
         */
        fun cleanHistoricalData(context: android.content.Context) {
            val intent = android.content.Intent(context, DataAggregationService::class.java)
            intent.action = ACTION_CLEAN_HISTORICAL_DATA
            
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                LogUtils.i(TAG, "手动触发历史错误数据清理服务")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "启动历史数据清理服务失败", e)
            }
        }

        /**
         * 手动触发清理重复会话记录
         */
        fun cleanDuplicateSessions(context: android.content.Context) {
            val intent = android.content.Intent(context, DataAggregationService::class.java)
            intent.action = ACTION_CLEAN_DUPLICATE_SESSIONS
            
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                LogUtils.i(TAG, "手动触发清理重复会话记录服务")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "启动清理重复会话记录服务失败", e)
            }
        }
    }
    
    // === 数据访问层依赖注入 ===
    @Inject
    lateinit var appSessionDao: AppSessionUserDao          // 原始会话数据访问
    
    @Inject
    lateinit var timerSessionDao: TimerSessionUserDao      // 定时器会话数据访问
    
    @Inject
    lateinit var dailyUsageDao: DailyUsageDao              // 日统计数据访问
    
    @Inject
    lateinit var summaryUsageDao: SummaryUsageDao          // 汇总统计数据访问
    
    @Inject
    lateinit var appCategoryDao: AppCategoryDao            // 应用分类数据访问
    
    @Inject
    lateinit var appInfoDao: AppInfoDao                    // 应用信息数据访问
    
    @Inject
    lateinit var rewardPunishmentUserDao: RewardPunishmentUserDao          // 日奖罚记录访问
    
    @Inject
    lateinit var rewardPunishmentWeekUserDao: RewardPunishmentWeekUserDao  // 周奖罚记录访问
    
    @Inject
    lateinit var rewardPunishmentMonthUserDao: RewardPunishmentMonthUserDao // 月奖罚记录访问
    
    // 服务协程作用域，使用IO调度器优化数据库操作性能
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
    }

    /**
     * 服务启动命令处理
     * 
     * 支持三种操作模式：
     * 1. ACTION_AGGREGATE_DATA: 完整的数据聚合流程
     * 2. ACTION_ENSURE_BASE_RECORDS: 仅确保基础记录存在
     * 3. ACTION_CLEAN_HISTORICAL_DATA: 清理历史错误数据并重新聚合
     * 4. ACTION_PROCESS_HISTORICAL_DATA: 单独处理历史未聚合数据
     * 5. ACTION_CLEAN_DUPLICATE_SESSIONS: 清理重复会话记录
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Android 8+ 要求前台服务在启动后短时间内调用 startForeground
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, buildNotification(), 0)
        } else {
            startForeground(NOTIFICATION_ID, buildNotification())
        }
        when (intent?.action) {
            ACTION_AGGREGATE_DATA -> {
                serviceScope.launch {
                    aggregateData()
                    stopSelf(startId)  // 完成后自动停止服务
                }
            }
            ACTION_ENSURE_BASE_RECORDS -> {
                serviceScope.launch {
                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    ensureBaseSummaryRecords(today)
                    stopSelf(startId)  // 完成后自动停止服务
                }
            }
            ACTION_CLEAN_HISTORICAL_DATA -> {
                serviceScope.launch {
                    cleanAndReaggregateHistoricalData()
                    stopSelf(startId)  // 完成后自动停止服务
                }
            }
            ACTION_PROCESS_HISTORICAL_DATA -> {
                serviceScope.launch {
                    LogUtils.i(TAG, "🔧 单独处理历史未聚合数据")
                    processHistoricalUnprocessedData()
                    stopSelf(startId)  // 完成后自动停止服务
                }
            }
            ACTION_CLEAN_DUPLICATE_SESSIONS -> {
                serviceScope.launch {
                    cleanDuplicateAppSessions()
                    // 清理后，自动触发一次完整聚合来刷新数据
                    aggregateData()
                    stopSelf(startId)
                }
            }
        }
        return START_STICKY  // 服务被杀死后自动重启，确保历史数据能被处理
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "数据聚合",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "用于后台执行数据聚合与清理"
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("正在处理数据")
            .setContentText("聚合与清理中…")
            .setSmallIcon(R.drawable.ic_timer)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    /**
     * 核心数据聚合流程
     * 
     * 聚合步骤（按顺序执行）：
     * 0. 确保基础汇总记录存在 - 为每个分类创建默认记录
     * 1. 聚合到日统计表 - 从原始会话数据计算每日使用量
     * 2. 聚合到汇总表 - 从日统计数据生成可查询的汇总数据
     * 3. 聚合周数据 - 计算周使用统计
     * 4. 聚合月数据 - 计算月使用统计
     * 5. 聚合周奖罚数据 - 计算周奖罚完成度
     * 6. 聚合月奖罚数据 - 计算月奖罚完成度
     * 
     * 错误处理：
     * - 使用try-catch包装整个流程
     * - 单个步骤失败不影响其他步骤
     * - 详细的日志记录便于问题排查
     */
    private suspend fun aggregateData() {
        try {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            LogUtils.i(TAG, "开始聚合数据: $today")
            
            // 🔧 关键修复：处理历史未聚合数据
            processHistoricalUnprocessedData()
            
            // 步骤0: 确保基础汇总记录存在
            // 目的：为每个分类在每个时间周期创建基础记录，避免查询时返回null
            LogUtils.d(TAG, "步骤0: 确保基础汇总记录存在")
            ensureBaseSummaryRecords(today)
            
            // 步骤1: 聚合今日数据到 daily_usage_user
            // 数据源：app_sessions_user（原始会话记录）
            // 目标：daily_usage_user（按日期+应用包名聚合的使用时长）
            LogUtils.d(TAG, "步骤1: 聚合今日数据到 daily_usage_user")
            aggregateToDailyUsage(today)
            
            // 步骤2: 聚合今日数据到 summary_usage_user
            // 数据源：daily_usage_user（日统计数据）
            // 目标：summary_usage_user（按日期+分类聚合的使用时长）
            LogUtils.d(TAG, "步骤2: 聚合今日数据到 summary_usage_user")
            aggregateToSummaryUsage(today)
            
            // 步骤3: 聚合周汇总数据
            // 数据源：summary_usage_user（日汇总数据）
            // 目标：summary_usage_week_user（按周+分类聚合的使用时长）
            LogUtils.d(TAG, "步骤3: 聚合周汇总数据")
            aggregateToWeeklySummary(today)
            
            // 步骤4: 聚合月汇总数据
            // 数据源：summary_usage_user（日汇总数据）
            // 目标：summary_usage_month_user（按月+分类聚合的使用时长）
            LogUtils.d(TAG, "步骤4: 聚合月汇总数据")
            aggregateToMonthlySummary(today)
            
            // 步骤5: 聚合奖惩周汇总数据
            // 数据源：reward_punishment_user（日奖罚记录）
            // 目标：reward_punishment_week_user（按周+分类聚合的奖罚完成度）
            LogUtils.d(TAG, "步骤5: 聚合奖惩周汇总数据")
            aggregateRewardPunishmentWeekly(today)
            
            // 步骤6: 聚合奖惩月汇总数据
            // 数据源：reward_punishment_user（日奖罚记录）
            // 目标：reward_punishment_month_user（按月+分类聚合的奖罚完成度）
            LogUtils.d(TAG, "步骤6: 聚合奖惩月汇总数据")
            aggregateRewardPunishmentMonthly(today)
            
            LogUtils.i(TAG, "数据聚合完成: $today")

            // 🚀 新增：发送广播通知Widget更新
            notifyWidgetUpdate()
            
        } catch (e: Exception) {
            Log.e(TAG, "数据聚合失败", e)
        }
    }
    
    /**
     * 🔧 关键修复：处理历史未聚合数据
     * 
     * 问题场景：用户某天不打开应用，该天的原始会话数据存在但从未被聚合
     * 解决方案：检查最近7天内有原始数据但没有聚合数据的日期，并进行补聚合
     * 
     * 🚨 重要：添加同步锁防止并发聚合导致的数据重复
     */
    @Volatile
    private var isProcessingHistoricalData = false
    
    private suspend fun processHistoricalUnprocessedData() {
        // 防止并发执行历史数据处理
        if (isProcessingHistoricalData) {
            LogUtils.w(TAG, "⚠️ 历史数据处理已在进行中，跳过本次处理")
            return
        }
        
        isProcessingHistoricalData = true
        try {
            LogUtils.i(TAG, "🔍 开始检查历史未聚合数据")
            
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val calendar = Calendar.getInstance()
            val unprocessedDates = mutableListOf<String>()
            
            // 检查最近7天（不包括今天，因为今天会在后续正常流程中处理）
            for (i in 1..7) {
                calendar.time = Date()
                calendar.add(Calendar.DAY_OF_YEAR, -i)
                val checkDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                
                // 检查该日期是否有原始会话数据但缺少聚合数据
                val hasRawData = appSessionDao.getSessionsByDate(checkDate).isNotEmpty()
                // 检查是否有该日期的汇总数据（查询所有汇总数据，看是否包含该日期）
                val hasSummaryData = try {
                    val allSummaryData = summaryUsageDao.getAllSummaryUsageUser()
                    allSummaryData.any { it.date == checkDate }
                } catch (e: Exception) {
                    false
                }
                
                if (hasRawData && !hasSummaryData) {
                    unprocessedDates.add(checkDate)
                    LogUtils.w(TAG, "🔍 发现未聚合日期: $checkDate (有原始数据但无聚合数据)")
                }
            }
            
            if (unprocessedDates.isNotEmpty()) {
                LogUtils.i(TAG, "📅 开始补聚合历史数据，共${unprocessedDates.size}天: ${unprocessedDates.joinToString()}")
                
                // 按时间顺序处理每个未聚合的日期
                unprocessedDates.sortedDescending().forEach { date ->
                    LogUtils.i(TAG, "🔧 补聚合日期: $date")
                    
                    // 确保基础记录存在
                    ensureBaseSummaryRecords(date)
                    
                    // 聚合该日期的数据
                    aggregateToDailyUsage(date)
                    aggregateToSummaryUsage(date)
                    
                    // 注意：周月数据会在处理完所有日期后统一更新
                    LogUtils.d(TAG, "✅ 日期 $date 补聚合完成")
                }
                
                // 重新计算周月汇总（因为可能影响了当前周月的统计）
                LogUtils.d(TAG, "🔄 重新计算周月汇总数据")
                aggregateToWeeklySummary(today)
                aggregateToMonthlySummary(today)
                
                LogUtils.i(TAG, "🎉 历史数据补聚合完成！修复了${unprocessedDates.size}天的缺失数据")
            } else {
                LogUtils.d(TAG, "✅ 未发现需要补聚合的历史数据")
            }
            
        } catch (e: Exception) {
            LogUtils.e(TAG, "❌ 处理历史未聚合数据失败", e)
        } finally {
            isProcessingHistoricalData = false
            LogUtils.d(TAG, "🔓 历史数据处理锁已释放")
        }
    }
    
    /**
     * 确保基础汇总记录存在
     * 
     * 目的：
     * 在每天/每周/每月的开始自动生成每个分类的基础记录（使用时间为0）
     * 这样可以确保UI查询时总是有数据返回，避免null值处理
     * 
     * 生成策略：
     * 1. 每日记录：每天都生成
     * 2. 历史记录：生成过去30天的记录（用于图表显示）
     * 3. 周记录：仅在周一生成
     * 4. 月记录：仅在月初生成
     * 
     * 数据完整性：
     * - 使用INSERT OR IGNORE避免重复插入
     * - 为每个分类都创建对应记录
     * - 初始使用时间设为0，后续聚合时累加
     */
    private suspend fun ensureBaseSummaryRecords(date: String) {
        withContext(Dispatchers.IO) {
            try {
                LogUtils.d(TAG, "开始确保基础汇总记录存在: $date")
                
                // 获取所有分类（包括用户自定义分类）
                val categories = appCategoryDao.getAllCategoriesList()
                LogUtils.d(TAG, "找到 ${categories.size} 个分类")
                
                // 1. 确保每日基础记录存在
                ensureDailyBaseSummaryRecords(date, categories)
                
                // 2. 生成历史基础记录（过去30天）
                // 目的：确保统计图表有足够的历史数据点
                // ⚠️ 警告：该功能存在缺陷，可能导致数据不一致，暂时禁用
                // ensureHistoricalBaseSummaryRecords(date, categories)
                
                // 3. 确保每周基础记录存在（仅在周一执行）
                if (isStartOfWeek(date)) {
                    ensureWeeklyBaseSummaryRecords(date, categories)
                }
                
                // 4. 确保每月基础记录存在（仅在月初执行）
                if (isStartOfMonth(date)) {
                    ensureMonthlyBaseSummaryRecords(date, categories)
                }
                
                LogUtils.d(TAG, "基础汇总记录确保完成: $date")
                
            } catch (e: Exception) {
                Log.e(TAG, "确保基础汇总记录失败", e)
            }
        }
    }
    
    /**
     * 确保每日基础汇总记录存在
     * 
     * 为指定日期的每个分类创建基础记录
     * 记录格式：{日期}_{分类ID}，使用时间初始为0
     */
    private suspend fun ensureDailyBaseSummaryRecords(date: String, categories: List<com.offtime.app.data.entity.AppCategoryEntity>) {
        categories.forEach { category ->
            val id = "${date}_${category.id}"
            
            // 检查是否已存在记录，避免重复创建
            val existingRecord = summaryUsageDao.getSummaryUsageById(id)
            if (existingRecord == null) {
                // 创建基础记录（使用时间为0）
                val baseRecord = SummaryUsageUserEntity(
                    id = id,
                    date = date,
                    catId = category.id,
                    totalSec = 0 // 基础记录使用时间为0，后续聚合时会累加实际使用时间
                )
                
                summaryUsageDao.upsert(baseRecord)
                LogUtils.v(TAG, "创建每日基础记录: 分类=${category.name}, 日期=$date")
            }
        }
    }
    
    /**
     * 生成历史基础记录（过去30天）
     * 确保过去的日期也有基础汇总记录
     */
    private suspend fun ensureHistoricalBaseSummaryRecords(currentDate: String, categories: List<com.offtime.app.data.entity.AppCategoryEntity>) {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val calendar = Calendar.getInstance()
            calendar.time = dateFormat.parse(currentDate) ?: return
            
            // 生成过去30天的基础记录
            for (i in 1..30) {
                calendar.time = dateFormat.parse(currentDate) ?: break
                calendar.add(Calendar.DAY_OF_YEAR, -i)
                val historyDate = dateFormat.format(calendar.time)
                
                // 为每个分类生成基础记录
                categories.forEach { category ->
                    val id = "${historyDate}_${category.id}"
                    
                    // 检查是否已存在记录
                    val existingRecord = summaryUsageDao.getSummaryUsageById(id)
                    if (existingRecord == null) {
                        // 创建基础记录（使用时间为0）
                        val baseRecord = SummaryUsageUserEntity(
                            id = id,
                            date = historyDate,
                            catId = category.id,
                            totalSec = 0 // 基础记录使用时间为0，后续聚合时会累加
                        )
                        
                        summaryUsageDao.upsert(baseRecord)
                        LogUtils.v(TAG, "创建历史基础记录: 分类=${category.name}, 日期=$historyDate")
                    }
                }
            }
            
            LogUtils.d(TAG, "历史基础记录生成完成，生成了过去30天的记录")
            
        } catch (e: Exception) {
            Log.e(TAG, "生成历史基础记录失败", e)
        }
    }
    
    /**
     * 确保每周基础汇总记录存在
     */
    private suspend fun ensureWeeklyBaseSummaryRecords(date: String, categories: List<com.offtime.app.data.entity.AppCategoryEntity>) {
        // 计算周开始日期（周一）
        val weekStart = getWeekStartDate(date)
        
        categories.forEach { category ->
            val id = "${weekStart}_${category.id}"
            
            // 检查是否已存在记录
            val existingRecord = summaryUsageDao.getSummaryUsageWeekById(id)
            if (existingRecord == null) {
                // 创建基础记录（平均每日使用时间为0）
                val baseRecord = SummaryUsageWeekUserEntity(
                    id = id,
                    weekStartDate = weekStart,
                    catId = category.id,
                    avgDailySec = 0 // 基础记录平均每日使用时间为0，后续聚合时会更新
                )
                
                summaryUsageDao.upsertWeek(baseRecord)
                LogUtils.v(TAG, "创建每周基础记录: 分类=${category.name}, 周开始=$weekStart")
            }
        }
    }
    
    /**
     * 确保每月基础汇总记录存在
     */
    private suspend fun ensureMonthlyBaseSummaryRecords(date: String, categories: List<com.offtime.app.data.entity.AppCategoryEntity>) {
        // 计算月份
        val month = date.substring(0, 7) // yyyy-MM
        
        categories.forEach { category ->
            val id = "${month}_${category.id}"
            
            // 检查是否已存在记录
            val existingRecord = summaryUsageDao.getSummaryUsageMonthById(id)
            if (existingRecord == null) {
                // 创建基础记录（平均每日使用时间为0）
                val baseRecord = SummaryUsageMonthUserEntity(
                    id = id,
                    month = month,
                    catId = category.id,
                    avgDailySec = 0 // 基础记录平均每日使用时间为0，后续聚合时会更新
                )
                
                summaryUsageDao.upsertMonth(baseRecord)
                LogUtils.v(TAG, "创建每月基础记录: 分类=${category.name}, 月份=$month")
            }
        }
    }
    
    /**
     * 判断是否是一周的开始（周一）
     */
    private fun isStartOfWeek(date: String): Boolean {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val calendar = Calendar.getInstance()
            calendar.time = dateFormat.parse(date) ?: return false
            
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            dayOfWeek == Calendar.MONDAY
        } catch (e: Exception) {
            Log.e(TAG, "判断是否是周一失败", e)
            false
        }
    }
    
    /**
     * 判断是否是一个月的开始（1号）
     */
    private fun isStartOfMonth(date: String): Boolean {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val calendar = Calendar.getInstance()
            calendar.time = dateFormat.parse(date) ?: return false
            
            val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
            dayOfMonth == 1
        } catch (e: Exception) {
            Log.e(TAG, "判断是否是月初失败", e)
            false
        }
    }
    
    /**
     * 获取指定日期所在周的开始日期（周一）
     */
    private fun getWeekStartDate(date: String): String {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val calendar = Calendar.getInstance()
            calendar.time = dateFormat.parse(date) ?: return date
            
            // 设置为本周一
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
            calendar.add(Calendar.DAY_OF_MONTH, -daysFromMonday)
            
            dateFormat.format(calendar.time)
        } catch (e: Exception) {
            Log.e(TAG, "计算周开始日期失败", e)
            date
        }
    }
    
    private suspend fun aggregateToDailyUsage(date: String) {
        withContext(Dispatchers.IO) {
            // 关键修复：先清理当天的小时级别聚合数据，避免重复计算
            LogUtils.d(TAG, "清理当天的小时聚合数据以避免重复计算: $date")
            dailyUsageDao.deleteByDate(date)
            
            // 🔧 重要修复：获取当日所有真实APP会话数据（isOffline=0）并进行去重
            val appSessions = appSessionDao.getSessionsByDate(date)
            LogUtils.d(TAG, "找到 ${appSessions.size} 个真实APP会话记录（去重前）")
            
            // 会话去重：相同包名、开始时间、结束时间的会话只保留一个
            val uniqueAppSessions = appSessions.groupBy { 
                "${it.pkgName}_${it.startTime}_${it.endTime}_${it.durationSec}" 
            }.mapValues { it.value.first() }.values.toList()
            
            if (uniqueAppSessions.size != appSessions.size) {
                val duplicateCount = appSessions.size - uniqueAppSessions.size
                LogUtils.w(TAG, "⚠️ 发现并过滤了 $duplicateCount 个重复会话记录")
            }
            LogUtils.d(TAG, "去重后：${uniqueAppSessions.size} 个有效会话记录")
            
            // 获取当日所有线下活动计时数据（isOffline=1）
            val timerSessions = timerSessionDao.getSessionsByDate(date)
            LogUtils.d(TAG, "找到 ${timerSessions.size} 个线下活动记录")
            
            // **关键修复**: 获取所有应用信息以检查排除状态
            val allApps = appInfoDao.getAllAppsList()
            val appExcludeStatusMap = allApps.associate { it.packageName to it.isExcluded }
            
            // 按分类和小时分组聚合
            val hourlyData = mutableMapOf<String, Int>() // key: catId_hour_isOffline, value: duration
            
            // 处理真实APP会话（isOffline=0）- 支持跨时段会话分拆，排除被标记为排除的应用
            uniqueAppSessions.forEach { session ->
                val isExcluded = appExcludeStatusMap[session.pkgName] ?: false
                if (!isExcluded) {
                    distributeSessionAcrossHours(session, hourlyData, 0)
                } else {
                    LogUtils.v(TAG, "跳过排除APP的小时分配: ${session.pkgName}")
                }
            }
            
            // 处理线下活动计时（isOffline=1）- 支持跨时段会话分拆
            timerSessions.forEach { session ->
                distributeTimerSessionAcrossHours(session, hourlyData, 1)
            }
            
            // 插入或更新 daily_usage_user
            hourlyData.forEach { (key, duration) ->
                val parts = key.split("_")
                val catId = parts[0].toInt()
                val hour = parts[1].toInt()
                val isOffline = parts[2].toInt()
                
                // 强制数据验证和修正：单小时使用时间不应超过3600秒(60分钟)
                val finalDuration = if (duration > 3600) {
                    Log.w(TAG, "⚠️ 强制修正异常数据：catId=$catId, hour=$hour, 原值=${duration}s(${duration/60}分钟) -> 修正为3600s(60分钟)")
                    3600 // 强制限制为1小时
                } else {
                    duration
                }
                
                val id = "${date}_${catId}_${hour}_${isOffline}"
                val entity = DailyUsageUserEntity(
                    id = id,
                    date = date,
                    catId = catId,
                    slotIndex = hour,
                    isOffline = isOffline,
                    durationSec = finalDuration
                )
                
                dailyUsageDao.upsert(entity)
                val type = if (isOffline == 1) "线下活动" else "真实APP"
                val minutes = finalDuration / 60
                if (finalDuration != duration) {
                    Log.w(TAG, "修正后聚合小时数据: catId=$catId, hour=$hour, 类型=$type, duration=${finalDuration}s (${minutes}m) [原值=${duration}s被修正]")
                } else {
                    LogUtils.v(TAG, "聚合小时数据: catId=$catId, hour=$hour, 类型=$type, duration=${finalDuration}s (${minutes}m)")
                }
            }
            
            // 为"总使用"分类生成小时级别的汇总数据
            generateTotalUsageHourlyData(date, hourlyData)
            
            val excludedAppCount = appSessions.count { appExcludeStatusMap[it.pkgName] == true }
            LogUtils.i(TAG, "daily_usage_user聚合完成: 处理了${appSessions.size}个真实APP会话(排除${excludedAppCount}个被排除应用)和${timerSessions.size}个线下活动")
        }
    }
    
    private suspend fun aggregateToSummaryUsage(date: String) {
        withContext(Dispatchers.IO) {
            // 获取当日所有真实APP会话数据
            val appSessions = appSessionDao.getSessionsByDate(date)
            
            // 获取当日所有线下活动计时数据
            val timerSessions = timerSessionDao.getSessionsByDate(date)
            
            // **关键修复**: 获取所有应用信息以检查排除状态
            val allApps = appInfoDao.getAllAppsList()
            val appExcludeStatusMap = allApps.associate { it.packageName to it.isExcluded }
            
            // 按分类分组聚合（包含真实APP和线下活动）
            val dailyData = mutableMapOf<Int, Int>() // catId -> totalSeconds
            
            // 聚合真实APP使用时间 - 排除被标记为排除的应用
            appSessions.forEach { session ->
                val isExcluded = appExcludeStatusMap[session.pkgName] ?: false
                if (!isExcluded) {
                    dailyData[session.catId] = (dailyData[session.catId] ?: 0) + session.durationSec
                    LogUtils.v(TAG, "聚合真实APP: ${session.pkgName}, catId=${session.catId}, duration=${session.durationSec}s")
                } else {
                    LogUtils.v(TAG, "跳过排除APP: ${session.pkgName}, catId=${session.catId}, duration=${session.durationSec}s")
                }
            }
            
            // 聚合线下活动时间（线下活动不需要检查排除状态）
            timerSessions.forEach { session ->
                dailyData[session.catId] = (dailyData[session.catId] ?: 0) + session.durationSec
                LogUtils.v(TAG, "聚合线下活动: catId=${session.catId}, duration=${session.durationSec}s")
            }
            
            // 插入或更新 summary_usage_user
            dailyData.forEach { (catId, totalSec) ->
                val id = "${date}_${catId}"
                val entity = SummaryUsageUserEntity(
                    id = id,
                    date = date,
                    catId = catId,
                    totalSec = totalSec
                )
                
                summaryUsageDao.upsert(entity)
                LogUtils.d(TAG, "聚合日数据: catId=$catId, totalSec=${totalSec}s (排除被排除应用后)")
            }
            
            // 为"总使用"分类生成汇总数据
            generateTotalUsageSummary(date, dailyData)
            
            val excludedAppCount = appSessions.count { appExcludeStatusMap[it.pkgName] == true }
            LogUtils.i(TAG, "summary_usage_user聚合完成: 处理了${appSessions.size}个真实APP会话(排除${excludedAppCount}个被排除应用)和${timerSessions.size}个线下活动")
        }
    }
    
    private suspend fun aggregateToWeeklySummary(currentDate: String) {
        withContext(Dispatchers.IO) {
            try {
                // 计算当前日期所在周的开始日期（周一）
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val current = dateFormat.parse(currentDate) ?: return@withContext
                
                val calendar = Calendar.getInstance()
                calendar.time = current
                
                // 设置为本周一
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
                calendar.add(Calendar.DAY_OF_MONTH, -daysFromMonday)
                
                val weekStartDate = dateFormat.format(calendar.time)
                
                // 计算周日日期

                
                // 获取本周所有日期（从周一到今天）
                val weekDates = mutableListOf<String>()
                val tempCalendar = Calendar.getInstance()
                tempCalendar.time = calendar.time
                
                val today = Calendar.getInstance()
                today.time = current
                
                while (tempCalendar.get(Calendar.YEAR) <= today.get(Calendar.YEAR) &&
                       tempCalendar.get(Calendar.DAY_OF_YEAR) <= today.get(Calendar.DAY_OF_YEAR)) {
                    weekDates.add(dateFormat.format(tempCalendar.time))
                    tempCalendar.add(Calendar.DAY_OF_MONTH, 1)
                }
                
                LogUtils.d(TAG, "计算周汇总: 周开始=$weekStartDate, 包含天数=${weekDates.size}")
                
                // 按分类聚合本周数据
                val weeklyData = mutableMapOf<Int, Pair<Int, Int>>() // catId -> (totalSec, dayCount)
                
                weekDates.forEach { date ->
                    val dailySummaries = dailyUsageDao.getDailyUsageByDate(date)
                    val categoryTotals = mutableMapOf<Int, Int>()
                    
                    // 按分类汇总当日数据
                    dailySummaries.forEach { usage ->
                        categoryTotals[usage.catId] = (categoryTotals[usage.catId] ?: 0) + usage.durationSec
                    }
                    
                    // 累加到周数据中
                    categoryTotals.forEach { (catId, totalSec) ->
                        val existing = weeklyData[catId] ?: Pair(0, 0)
                        weeklyData[catId] = Pair(existing.first + totalSec, existing.second + 1)
                    }
                }
                
                // 插入或更新周汇总数据
                weeklyData.forEach { (catId, data) ->
                    val (totalSec, dayCount) = data
                    val avgDailySec = if (dayCount > 0) totalSec / dayCount else 0
                    
                    val id = "${weekStartDate}_${catId}"
                    val entity = SummaryUsageWeekUserEntity(
                        id = id,
                        weekStartDate = weekStartDate,
                        catId = catId,
                        avgDailySec = avgDailySec
                    )
                    
                    summaryUsageDao.upsertWeek(entity)
                    LogUtils.d(TAG, "周汇总: catId=$catId, 总时长=${totalSec}s, 天数=$dayCount, 日均=${avgDailySec}s")
                }
                
                // 为"总使用"分类生成周汇总数据
                generateTotalUsageWeeklySummary(weekStartDate, weeklyData)
                
                LogUtils.i(TAG, "周汇总完成: 处理了${weeklyData.size}个分类的周数据")
                
            } catch (e: Exception) {
                Log.e(TAG, "周汇总聚合失败", e)
            }
        }
    }
    
    private suspend fun aggregateToMonthlySummary(currentDate: String) {
        withContext(Dispatchers.IO) {
            try {
                // 计算当前月份
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                val current = dateFormat.parse(currentDate) ?: return@withContext
                
                val calendar = Calendar.getInstance()
                calendar.time = current
                
                val currentMonth = monthFormat.format(calendar.time)
                
                // 获取本月1号到今天的所有日期
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val monthStart = calendar.time
                
                val monthDates = mutableListOf<String>()
                val tempCalendar = Calendar.getInstance()
                tempCalendar.time = monthStart
                
                val today = Calendar.getInstance()
                today.time = current
                
                while (tempCalendar.get(Calendar.YEAR) <= today.get(Calendar.YEAR) &&
                       tempCalendar.get(Calendar.DAY_OF_YEAR) <= today.get(Calendar.DAY_OF_YEAR)) {
                    monthDates.add(dateFormat.format(tempCalendar.time))
                    tempCalendar.add(Calendar.DAY_OF_MONTH, 1)
                }
                
                LogUtils.d(TAG, "计算月汇总: 月份=$currentMonth, 包含天数=${monthDates.size}")
                
                // 按分类聚合本月数据
                val monthlyData = mutableMapOf<Int, Pair<Int, Int>>() // catId -> (totalSec, dayCount)
                
                monthDates.forEach { date ->
                    val dailySummaries = dailyUsageDao.getDailyUsageByDate(date)
                    val categoryTotals = mutableMapOf<Int, Int>()
                    
                    // 按分类汇总当日数据
                    dailySummaries.forEach { usage ->
                        categoryTotals[usage.catId] = (categoryTotals[usage.catId] ?: 0) + usage.durationSec
                    }
                    
                    // 累加到月数据中
                    categoryTotals.forEach { (catId, totalSec) ->
                        val existing = monthlyData[catId] ?: Pair(0, 0)
                        monthlyData[catId] = Pair(existing.first + totalSec, existing.second + 1)
                    }
                }
                
                // 插入或更新月汇总数据
                monthlyData.forEach { (catId, data) ->
                    val (totalSec, dayCount) = data
                    val avgDailySec = if (dayCount > 0) totalSec / dayCount else 0
                    
                    val id = "${currentMonth}_${catId}"
                    val entity = SummaryUsageMonthUserEntity(
                        id = id,
                        month = currentMonth,
                        catId = catId,
                        avgDailySec = avgDailySec
                    )
                    
                    summaryUsageDao.upsertMonth(entity)
                    Log.d(TAG, "月汇总: catId=$catId, 总时长=${totalSec}s, 天数=$dayCount, 日均=${avgDailySec}s")
                }
                
                // 为"总使用"分类生成月汇总数据
                generateTotalUsageMonthlySummary(currentMonth, monthlyData)
                
                LogUtils.i(TAG, "月汇总完成: 处理了${monthlyData.size}个分类的月数据")
                
            } catch (e: Exception) {
                Log.e(TAG, "月汇总聚合失败", e)
            }
        }
    }
    
    /**
     * 将APP会话按实际时间分配到各个小时时段
     * 支持跨时段长会话的正确分拆
     */
    private fun distributeSessionAcrossHours(
        session: AppSessionUserEntity, 
        hourlyData: MutableMap<String, Int>, 
        isOffline: Int
    ) {
        val startTime = session.startTime
        val endTime = session.endTime
        val totalDuration = session.durationSec
        
        // 如果会话时长为0，跳过
        if (totalDuration <= 0) return
        
        val startCalendar = java.util.Calendar.getInstance().apply { timeInMillis = startTime }
        val endCalendar = java.util.Calendar.getInstance().apply { timeInMillis = endTime }
        
        val startHour = startCalendar.get(java.util.Calendar.HOUR_OF_DAY)
        val endHour = endCalendar.get(java.util.Calendar.HOUR_OF_DAY)
        
        // 如果在同一小时内，直接分配
        if (startHour == endHour && 
            startCalendar.get(java.util.Calendar.DAY_OF_YEAR) == endCalendar.get(java.util.Calendar.DAY_OF_YEAR)) {
            val key = "${session.catId}_${startHour}_${isOffline}"
            val currentValue = hourlyData[key] ?: 0
            val newValue = currentValue + totalDuration
            
            // 强制数据验证：单小时不应超过3600秒
            if (newValue > 3600) {
                Log.w(TAG, "⚠️ 数据异常修正: catId=${session.catId}, hour=$startHour, 原值=${currentValue}s, 新增=${totalDuration}s, 修正前=${newValue}s")
                // 将新增时长限制为不超过剩余可用时间
                val remainingTime = 3600 - currentValue
                val adjustedDuration = minOf(totalDuration, maxOf(0, remainingTime))
                hourlyData[key] = currentValue + adjustedDuration
                Log.w(TAG, "⚠️ 数据异常修正: 调整后=${currentValue + adjustedDuration}s, 丢弃时长=${totalDuration - adjustedDuration}s")
            } else {
                hourlyData[key] = newValue
            }
            return
        }
        
        // 跨时段会话：按实际时间比例分配
        var currentTime = startTime
        var remainingDuration = totalDuration
        val totalRealTimeSpan = endTime - startTime // 实际经过的时间
        
        if (totalRealTimeSpan <= 0) {
            Log.w(TAG, "跨时段会话时间范围异常: startTime=$startTime, endTime=$endTime")
            return
        }
        
        while (currentTime < endTime && remainingDuration > 0) {
            val currentCalendar = java.util.Calendar.getInstance().apply { timeInMillis = currentTime }
            val currentHour = currentCalendar.get(java.util.Calendar.HOUR_OF_DAY)
            
            // 计算当前小时时段的结束时间
            val hourEndCalendar = java.util.Calendar.getInstance().apply {
                timeInMillis = currentTime
                set(java.util.Calendar.MINUTE, 59)
                set(java.util.Calendar.SECOND, 59)
                set(java.util.Calendar.MILLISECOND, 999)
            }
            
            val hourEndTime = minOf(hourEndCalendar.timeInMillis, endTime)
            val hourRealTimeMs = hourEndTime - currentTime
            
            // 按实际时间比例分配使用时长
            val hourUsageDurationSec = if (totalRealTimeSpan > 0) {
                minOf(
                    ((hourRealTimeMs.toDouble() / totalRealTimeSpan.toDouble()) * totalDuration).toInt(),
                    remainingDuration,
                    3600 // 绝对不能超过1小时
                )
            } else {
                0
            }
            
            if (hourUsageDurationSec > 0) {
                val key = "${session.catId}_${currentHour}_${isOffline}"
                val currentValue = hourlyData[key] ?: 0
                val newValue = currentValue + hourUsageDurationSec
                
                // 强制数据验证：单小时不应超过3600秒
                if (newValue > 3600) {
                    Log.w(TAG, "⚠️ 跨时段分配异常修正: catId=${session.catId}, hour=$currentHour")
                    Log.w(TAG, "   原值=${currentValue}s, 新增=${hourUsageDurationSec}s, 修正前=${newValue}s")
                    // 将新增时长限制为不超过剩余可用时间
                    val remainingTime = 3600 - currentValue
                    val adjustedHourDuration = minOf(hourUsageDurationSec, maxOf(0, remainingTime))
                    hourlyData[key] = currentValue + adjustedHourDuration
                    Log.w(TAG, "   调整后=${currentValue + adjustedHourDuration}s, 丢弃时长=${hourUsageDurationSec - adjustedHourDuration}s")
                } else {
                    hourlyData[key] = newValue
                }
                
                LogUtils.v(TAG, "跨时段分配: ${session.pkgName}, ${currentHour}点分配${hourUsageDurationSec}秒")
            }
            
            // 移动到下一个小时的开始
            currentTime = hourEndTime + 1
            remainingDuration -= hourUsageDurationSec
        }
    }
    
    /**
     * 将Timer会话按实际时间分配到各个小时时段
     */
    private fun distributeTimerSessionAcrossHours(
        session: TimerSessionUserEntity,
        hourlyData: MutableMap<String, Int>,
        isOffline: Int
    ) {
        val startTime = session.startTime
        val endTime = session.endTime
        val totalDuration = session.durationSec
        
        // 如果会话时长为0，跳过
        if (totalDuration <= 0) return
        
        val startCalendar = java.util.Calendar.getInstance().apply { timeInMillis = startTime }
        val endCalendar = java.util.Calendar.getInstance().apply { timeInMillis = endTime }
        
        val startHour = startCalendar.get(java.util.Calendar.HOUR_OF_DAY)
        val endHour = endCalendar.get(java.util.Calendar.HOUR_OF_DAY)
        
        // 如果在同一小时内，直接分配
        if (startHour == endHour && 
            startCalendar.get(java.util.Calendar.DAY_OF_YEAR) == endCalendar.get(java.util.Calendar.DAY_OF_YEAR)) {
            val key = "${session.catId}_${startHour}_${isOffline}"
            val currentValue = hourlyData[key] ?: 0
            val newValue = currentValue + totalDuration
            
            // 强制数据验证：单小时不应超过3600秒
            if (newValue > 3600) {
                Log.w(TAG, "⚠️ 定时器数据异常修正: catId=${session.catId}, hour=$startHour, 原值=${currentValue}s, 新增=${totalDuration}s, 修正前=${newValue}s")
                // 将新增时长限制为不超过剩余可用时间
                val remainingTime = 3600 - currentValue
                val adjustedDuration = minOf(totalDuration, maxOf(0, remainingTime))
                hourlyData[key] = currentValue + adjustedDuration
                Log.w(TAG, "⚠️ 定时器数据异常修正: 调整后=${currentValue + adjustedDuration}s, 丢弃时长=${totalDuration - adjustedDuration}s")
            } else {
                hourlyData[key] = newValue
            }
            return
        }
        
        // 跨时段会话：按实际时间比例分配
        var currentTime = startTime
        var remainingDuration = totalDuration
        val totalRealTimeSpan = endTime - startTime // 实际经过的时间
        
        if (totalRealTimeSpan <= 0) {
            Log.w(TAG, "定时器跨时段会话时间范围异常: startTime=$startTime, endTime=$endTime")
            return
        }
        
        while (currentTime < endTime && remainingDuration > 0) {
            val currentCalendar = java.util.Calendar.getInstance().apply { timeInMillis = currentTime }
            val currentHour = currentCalendar.get(java.util.Calendar.HOUR_OF_DAY)
            
            // 计算当前小时时段的结束时间
            val hourEndCalendar = java.util.Calendar.getInstance().apply {
                timeInMillis = currentTime
                set(java.util.Calendar.MINUTE, 59)
                set(java.util.Calendar.SECOND, 59)
                set(java.util.Calendar.MILLISECOND, 999)
            }
            
            val hourEndTime = minOf(hourEndCalendar.timeInMillis, endTime)
            val hourRealTimeMs = hourEndTime - currentTime
            
            // 按实际时间比例分配使用时长
            val hourUsageDurationSec = if (totalRealTimeSpan > 0) {
                minOf(
                    ((hourRealTimeMs.toDouble() / totalRealTimeSpan.toDouble()) * totalDuration).toInt(),
                    remainingDuration,
                    3600 // 绝对不能超过1小时
                )
            } else {
                0
            }
            
            if (hourUsageDurationSec > 0) {
                val key = "${session.catId}_${currentHour}_${isOffline}"
                val currentValue = hourlyData[key] ?: 0
                val newValue = currentValue + hourUsageDurationSec
                
                // 强制数据验证：单小时不应超过3600秒
                if (newValue > 3600) {
                    Log.w(TAG, "⚠️ 定时器跨时段分配异常修正: catId=${session.catId}, hour=$currentHour")
                    Log.w(TAG, "   原值=${currentValue}s, 新增=${hourUsageDurationSec}s, 修正前=${newValue}s")
                    // 将新增时长限制为不超过剩余可用时间
                    val remainingTime = 3600 - currentValue
                    val adjustedHourDuration = minOf(hourUsageDurationSec, maxOf(0, remainingTime))
                    hourlyData[key] = currentValue + adjustedHourDuration
                    Log.w(TAG, "   调整后=${currentValue + adjustedHourDuration}s, 丢弃时长=${hourUsageDurationSec - adjustedHourDuration}s")
                } else {
                    hourlyData[key] = newValue
                }
                
                LogUtils.v(TAG, "跨时段分配(线下): ${session.programName}, ${currentHour}点分配${hourUsageDurationSec}秒")
            }
            
            // 移动到下一个小时的开始
            currentTime = hourEndTime + 1
            remainingDuration -= hourUsageDurationSec
        }
    }
    
    /**
     * 为"总使用"分类生成汇总数据
     * 汇总除"排除统计"外的所有分类的使用时间
     */
    private suspend fun generateTotalUsageSummary(date: String, dailyData: Map<Int, Int>) {
        try {
            // 获取所有分类
            val categories = appCategoryDao.getAllCategoriesList()
            val totalUsageCategory = categories.find { it.name == "总使用" }
            
            if (totalUsageCategory == null) {
                Log.w(TAG, "未找到'总使用'分类，跳过汇总")
                return
            }
            
            // 计算总使用时间（排除已排除的应用）
            var totalUsageSeconds = 0
            LogUtils.d(TAG, "开始计算总使用日汇总，原始数据条目数: ${dailyData.size}")
            
            dailyData.forEach { (catId, totalSec) ->
                // 跳过"总使用"分类本身
                if (catId != totalUsageCategory.id) {
                    // 检查该分类是否应该被排除
                    val category = categories.find { it.id == catId }
                    val shouldExclude = category?.name?.contains("总使用") == true || 
                                      category?.name?.contains("排除统计") == true
                    
                    LogUtils.v(TAG, "总使用日汇总检查: 分类=${category?.name}(${catId}), 秒数=${totalSec}, 排除=${shouldExclude}")
                    
                    if (!shouldExclude) {
                        totalUsageSeconds += totalSec
                        LogUtils.v(TAG, "总使用日汇总包含: 分类=${category?.name}(${catId}), 秒数=${totalSec}, 累计=${totalUsageSeconds}")
                    } else {
                        android.util.Log.d(TAG, "总使用日汇总排除: 分类=${category?.name}(${catId}), 秒数=${totalSec}")
                    }
                } else {
                    android.util.Log.d(TAG, "跳过总使用分类本身: catId=${catId}")
                }
            }
            
            LogUtils.d(TAG, "总使用日汇总计算完成: 总秒数=${totalUsageSeconds}, 约${totalUsageSeconds/3600.0}小时")
            
            // 为"总使用"分类创建或更新汇总记录
            val totalUsageId = "${date}_${totalUsageCategory.id}"
            val totalUsageEntity = SummaryUsageUserEntity(
                id = totalUsageId,
                date = date,
                catId = totalUsageCategory.id,
                totalSec = totalUsageSeconds
            )
            
            summaryUsageDao.upsert(totalUsageEntity)
            LogUtils.i(TAG, "总使用汇总: date=$date, totalSec=${totalUsageSeconds}s (汇总${dailyData.size}个分类)")
            LogUtils.d(TAG, "总使用汇总详情: ${totalUsageSeconds / 3600.0}小时")
            
        } catch (e: Exception) {
            Log.e(TAG, "生成总使用汇总失败", e)
        }
    }
    
    /**
     * 为"总使用"分类生成周汇总数据
     */
    private suspend fun generateTotalUsageWeeklySummary(weekStartDate: String, weeklyData: Map<Int, Pair<Int, Int>>) {
        try {
            // 获取所有分类
            val categories = appCategoryDao.getAllCategoriesList()
            val totalUsageCategory = categories.find { it.name == "总使用" }
            
            if (totalUsageCategory == null) {
                Log.w(TAG, "未找到'总使用'分类，跳过周汇总")
                return
            }
            
            // 计算总使用时间（排除已排除的应用）
            var totalUsageSeconds = 0
            var totalDayCount = 0
            weeklyData.forEach { (catId, data) ->
                // 跳过"总使用"分类本身
                if (catId != totalUsageCategory.id) {
                    // 检查该分类是否应该被排除
                    val category = categories.find { it.id == catId }
                    val shouldExclude = category?.name?.contains("总使用") == true || 
                                      category?.name?.contains("排除统计") == true
                    
                    if (!shouldExclude) {
                        val (totalSec, dayCount) = data
                        totalUsageSeconds += totalSec
                        totalDayCount = maxOf(totalDayCount, dayCount) // 使用最大天数
                        android.util.Log.d(TAG, "总使用周汇总包含: 分类=${category?.name}(${catId}), 秒数=${totalSec}")
                    } else {
                        android.util.Log.d(TAG, "总使用周汇总排除: 分类=${category?.name}(${catId})")
                    }
                }
            }
            
            val avgDailySec = if (totalDayCount > 0) totalUsageSeconds / totalDayCount else 0
            
            // 为"总使用"分类创建或更新周汇总记录
            val totalUsageId = "${weekStartDate}_${totalUsageCategory.id}"
            val totalUsageEntity = SummaryUsageWeekUserEntity(
                id = totalUsageId,
                weekStartDate = weekStartDate,
                catId = totalUsageCategory.id,
                avgDailySec = avgDailySec
            )
            
            summaryUsageDao.upsertWeek(totalUsageEntity)
            Log.d(TAG, "总使用周汇总: week=$weekStartDate, totalSec=${totalUsageSeconds}s, dayCount=$totalDayCount, avgDaily=${avgDailySec}s")
            
        } catch (e: Exception) {
            Log.e(TAG, "生成总使用周汇总失败", e)
        }
    }
    
    /**
     * 为"总使用"分类生成月汇总数据
     */
    private suspend fun generateTotalUsageMonthlySummary(currentMonth: String, monthlyData: Map<Int, Pair<Int, Int>>) {
        try {
            // 获取所有分类
            val categories = appCategoryDao.getAllCategoriesList()
            val totalUsageCategory = categories.find { it.name == "总使用" }
            
            if (totalUsageCategory == null) {
                Log.w(TAG, "未找到'总使用'分类，跳过月汇总")
                return
            }
            
            // 计算总使用时间（排除已排除的应用）
            var totalUsageSeconds = 0
            var totalDayCount = 0
            monthlyData.forEach { (catId, data) ->
                // 跳过"总使用"分类本身
                if (catId != totalUsageCategory.id) {
                    // 检查该分类是否应该被排除
                    val category = categories.find { it.id == catId }
                    val shouldExclude = category?.name?.contains("总使用") == true || 
                                      category?.name?.contains("排除统计") == true
                    
                    if (!shouldExclude) {
                        val (totalSec, dayCount) = data
                        totalUsageSeconds += totalSec
                        totalDayCount = maxOf(totalDayCount, dayCount) // 使用最大天数
                        android.util.Log.d(TAG, "总使用月汇总包含: 分类=${category?.name}(${catId}), 秒数=${totalSec}")
                    } else {
                        android.util.Log.d(TAG, "总使用月汇总排除: 分类=${category?.name}(${catId})")
                    }
                }
            }
            
            val avgDailySec = if (totalDayCount > 0) totalUsageSeconds / totalDayCount else 0
            
            // 为"总使用"分类创建或更新月汇总记录
            val totalUsageId = "${currentMonth}_${totalUsageCategory.id}"
            val totalUsageEntity = SummaryUsageMonthUserEntity(
                id = totalUsageId,
                month = currentMonth,
                catId = totalUsageCategory.id,
                avgDailySec = avgDailySec
            )
            
            summaryUsageDao.upsertMonth(totalUsageEntity)
            Log.d(TAG, "总使用月汇总: month=$currentMonth, totalSec=${totalUsageSeconds}s, dayCount=$totalDayCount, avgDaily=${avgDailySec}s")
            
        } catch (e: Exception) {
            Log.e(TAG, "生成总使用月汇总失败", e)
        }
    }
    
    /**
     * 为"总使用"分类生成小时级别的汇总数据
     */
    private suspend fun generateTotalUsageHourlyData(date: String, hourlyData: Map<String, Int>) {
        try {
            // 获取所有分类
            val categories = appCategoryDao.getAllCategoriesList()
            val totalUsageCategory = categories.find { it.name == "总使用" }
            
            if (totalUsageCategory == null) {
                Log.w(TAG, "未找到'总使用'分类，跳过小时汇总")
                return
            }
            
            // 按小时汇总所有非排除的分类数据
            val totalHourlyData = mutableMapOf<String, Int>() // "hour_isOffline" -> totalSeconds
            
            hourlyData.forEach { (key, duration) ->
                val parts = key.split("_")
                val catId = parts[0].toInt()
                val hour = parts[1].toInt()
                val isOffline = parts[2].toInt()
                
                // 跳过"总使用"分类本身
                if (catId != totalUsageCategory.id) {
                    // 检查该分类是否应该被排除
                    val category = categories.find { it.id == catId }
                    val shouldExclude = category?.name?.contains("总使用") == true || 
                                      category?.name?.contains("排除统计") == true
                    
                    if (!shouldExclude) {
                        val totalKey = "${hour}_${isOffline}"
                        totalHourlyData[totalKey] = (totalHourlyData[totalKey] ?: 0) + duration
                        android.util.Log.d(TAG, "总使用小时汇总包含: 分类=${category?.name}(${catId}), ${hour}点, 秒数=${duration}")
                    } else {
                        android.util.Log.d(TAG, "总使用小时汇总排除: 分类=${category?.name}(${catId}), ${hour}点, 秒数=${duration}")
                    }
                }
            }
            
            // 为"总使用"分类创建小时级别的记录
            totalHourlyData.forEach { (key, duration) ->
                val parts = key.split("_")
                val hour = parts[0].toInt()
                val isOffline = parts[1].toInt()
                
                val id = "${date}_${totalUsageCategory.id}_${hour}_${isOffline}"
                val entity = DailyUsageUserEntity(
                    id = id,
                    date = date,
                    catId = totalUsageCategory.id,
                    slotIndex = hour,
                    isOffline = isOffline,
                    durationSec = duration
                )
                
                dailyUsageDao.upsert(entity)
                val type = if (isOffline == 1) "线下活动" else "真实APP"
                Log.d(TAG, "总使用小时汇总: hour=$hour, 类型=$type, duration=${duration}s")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "生成总使用小时汇总失败", e)
        }
    }
    
    /**
     * 聚合奖惩周数据
     */
    private suspend fun aggregateRewardPunishmentWeekly(currentDate: String) {
        withContext(Dispatchers.IO) {
            try {
                // 计算上一个完整周的开始日期（上周一）
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val current = dateFormat.parse(currentDate) ?: return@withContext
                
                val calendar = Calendar.getInstance()
                calendar.time = current
                
                // 设置为上周一（聚合上一个完整周的数据）
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
                calendar.add(Calendar.DAY_OF_MONTH, -daysFromMonday - 7) // 减去额外的7天到上周
                
                val weekStartDate = dateFormat.format(calendar.time)
                
                // 计算周日日期
                val weekEndCalendar = Calendar.getInstance()
                weekEndCalendar.time = calendar.time
                weekEndCalendar.add(Calendar.DAY_OF_MONTH, 6)
                val weekEndDate = dateFormat.format(weekEndCalendar.time)
                
                // 获取本周所有日期（从周一到今天）
                val weekDates = mutableListOf<String>()
                val tempCalendar = Calendar.getInstance()
                tempCalendar.time = calendar.time
                
                // 生成完整的一周（7天）
                for (i in 0..6) {
                    weekDates.add(dateFormat.format(tempCalendar.time))
                    tempCalendar.add(Calendar.DAY_OF_MONTH, 1)
                }
                
                Log.d(TAG, "计算奖惩周汇总: 周开始=$weekStartDate, 周结束=$weekEndDate, 包含天数=${weekDates.size}")
                Log.d(TAG, "周日期列表: ${weekDates.joinToString(", ")}")
                
                // 按分类聚合本周奖惩数据
                val weeklyRewardData = mutableMapOf<Int, MutableMap<String, Int>>() // catId -> Map("totalReward", "doneReward", "totalPunish", "donePunish")
                
                weekDates.forEach { date ->
                    val dailyRewards = rewardPunishmentUserDao.getRecordsByDate(date)
                    Log.d(TAG, "日期 $date 找到 ${dailyRewards.size} 条奖惩记录")
                    
                    dailyRewards.forEach { record ->
                        if (!weeklyRewardData.containsKey(record.catId)) {
                            weeklyRewardData[record.catId] = mutableMapOf(
                                "totalReward" to 0,
                                "doneReward" to 0, 
                                "totalPunish" to 0,
                                "donePunish" to 0
                            )
                        }
                        
                        val catData = weeklyRewardData[record.catId]!!
                        
                        // 既然有奖惩记录，说明该分类该天有奖惩设置，各自统计一次
                        catData["totalReward"] = catData["totalReward"]!! + 1
                        catData["totalPunish"] = catData["totalPunish"]!! + 1
                        
                        if (record.rewardDone == 1) {
                            catData["doneReward"] = catData["doneReward"]!! + 1
                        }
                        if (record.punishDone == 1) {
                            catData["donePunish"] = catData["donePunish"]!! + 1
                        }
                        
                        Log.d(TAG, "$date catId=${record.catId}: 目标达成=${record.isGoalMet}, 奖励完成=${record.rewardDone}, 惩罚完成=${record.punishDone}")
                    }
                }
                
                // 插入或更新周奖惩汇总数据
                weeklyRewardData.forEach { (catId, data) ->
                    val totalRewardCount = data["totalReward"]!!
                    val doneRewardCount = data["doneReward"]!!
                    val totalPunishCount = data["totalPunish"]!!
                    val donePunishCount = data["donePunish"]!!
                    
                    val id = "${weekStartDate}_${catId}"
                    val entity = RewardPunishmentWeekUserEntity(
                        id = id,
                        weekStart = weekStartDate,
                        weekEnd = weekEndDate,
                        catId = catId,
                        totalRewardCount = totalRewardCount,
                        doneRewardCount = doneRewardCount,
                        totalPunishCount = totalPunishCount,
                        donePunishCount = donePunishCount
                    )
                    
                    rewardPunishmentWeekUserDao.upsert(entity)
                    Log.d(TAG, "周奖惩汇总保存成功: id=$id, catId=$catId, 总奖励=$totalRewardCount, 完成奖励=$doneRewardCount, 总惩罚=$totalPunishCount, 完成惩罚=$donePunishCount")
                }
                
                Log.d(TAG, "奖惩周汇总完成: 处理了${weeklyRewardData.size}个分类")
                
            } catch (e: Exception) {
                Log.e(TAG, "奖惩周汇总失败", e)
            }
        }
    }
    
    /**
     * 聚合奖惩月数据
     */
    private suspend fun aggregateRewardPunishmentMonthly(currentDate: String) {
        withContext(Dispatchers.IO) {
            try {
                // 计算当前日期所在月
                val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val current = dateFormat.parse(currentDate) ?: return@withContext
                
                val currentMonth = monthFormat.format(current)
                
                val calendar = Calendar.getInstance()
                calendar.time = current
                
                // 设置为本月1号
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                
                // 获取本月所有日期（从1号到今天）
                val monthDates = mutableListOf<String>()
                val tempCalendar = Calendar.getInstance()
                tempCalendar.time = calendar.time
                
                val today = Calendar.getInstance()
                today.time = current
                
                while (tempCalendar.get(Calendar.YEAR) <= today.get(Calendar.YEAR) &&
                       tempCalendar.get(Calendar.DAY_OF_YEAR) <= today.get(Calendar.DAY_OF_YEAR)) {
                    monthDates.add(dateFormat.format(tempCalendar.time))
                    tempCalendar.add(Calendar.DAY_OF_MONTH, 1)
                }
                
                Log.d(TAG, "计算奖惩月汇总: 月份=$currentMonth, 包含天数=${monthDates.size}")
                
                // 按分类聚合本月奖惩数据
                val monthlyRewardData = mutableMapOf<Int, MutableMap<String, Int>>() // catId -> Map("totalReward", "doneReward", "totalPunish", "donePunish")
                
                monthDates.forEach { date ->
                    val dailyRewards = rewardPunishmentUserDao.getRecordsByDate(date)
                    Log.d(TAG, "日期 $date 找到 ${dailyRewards.size} 条奖惩记录")
                    
                    dailyRewards.forEach { record ->
                        if (!monthlyRewardData.containsKey(record.catId)) {
                            monthlyRewardData[record.catId] = mutableMapOf(
                                "totalReward" to 0,
                                "doneReward" to 0,
                                "totalPunish" to 0,
                                "donePunish" to 0
                            )
                        }
                        
                        val catData = monthlyRewardData[record.catId]!!
                        
                        // 既然有奖惩记录，说明该分类该天有奖惩设置，各自统计一次
                        catData["totalReward"] = catData["totalReward"]!! + 1
                        catData["totalPunish"] = catData["totalPunish"]!! + 1
                        
                        if (record.rewardDone == 1) {
                            catData["doneReward"] = catData["doneReward"]!! + 1
                        }
                        if (record.punishDone == 1) {
                            catData["donePunish"] = catData["donePunish"]!! + 1
                        }
                        
                        Log.d(TAG, "$date catId=${record.catId}: 目标达成=${record.isGoalMet}, 奖励完成=${record.rewardDone}, 惩罚完成=${record.punishDone}")
                    }
                }
                
                // 插入或更新月奖惩汇总数据
                monthlyRewardData.forEach { (catId, data) ->
                    val totalRewardCount = data["totalReward"]!!
                    val doneRewardCount = data["doneReward"]!!
                    val totalPunishCount = data["totalPunish"]!!
                    val donePunishCount = data["donePunish"]!!
                    
                    val id = "${currentMonth}_${catId}"
                    val entity = RewardPunishmentMonthUserEntity(
                        id = id,
                        yearMonth = currentMonth,
                        catId = catId,
                        totalRewardCount = totalRewardCount,
                        doneRewardCount = doneRewardCount,
                        totalPunishCount = totalPunishCount,
                        donePunishCount = donePunishCount
                    )
                    
                    rewardPunishmentMonthUserDao.upsert(entity)
                    Log.d(TAG, "月奖惩汇总: catId=$catId, 总奖励=$totalRewardCount, 完成奖励=$doneRewardCount, 总惩罚=$totalPunishCount, 完成惩罚=$donePunishCount")
                }
                
                Log.d(TAG, "奖惩月汇总完成: 处理了${monthlyRewardData.size}个分类")
                
            } catch (e: Exception) {
                Log.e(TAG, "奖惩月汇总失败", e)
            }
        }
    }

    /**
     * 清理历史错误数据并重新聚合
     * 解决之前重复计算导致的单小时超过60分钟的问题
     */
    private suspend fun cleanAndReaggregateHistoricalData() {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "开始清理历史错误数据")
                
                // 1. 获取包含异常数据的日期
                val abnormalDates = dailyUsageDao.getAbnormalDates()
                Log.d(TAG, "发现${abnormalDates.size}个包含异常数据的日期: $abnormalDates")
                
                if (abnormalDates.isEmpty()) {
                    Log.d(TAG, "没有发现异常数据，无需清理")
                    return@withContext
                }
                
                // 2. 删除所有异常的小时级数据
                dailyUsageDao.deleteAbnormalData()
                Log.d(TAG, "已删除所有单小时超过60分钟的异常记录")
                
                // 3. 重新聚合这些日期的数据
                abnormalDates.forEach { date ->
                    Log.d(TAG, "重新聚合日期: $date")
                    aggregateToDailyUsage(date)
                    aggregateToSummaryUsage(date)
                    // 等待一小段时间避免过快操作
                    kotlinx.coroutines.delay(100)
                }
                
                Log.d(TAG, "历史错误数据清理完成，共处理${abnormalDates.size}个日期")
                
            } catch (e: Exception) {
                Log.e(TAG, "清理历史错误数据失败", e)
            }
        }
    }

    /**
     * 清理 app_sessions_user 表中的重复记录
     * 🔧 增强版：特别处理跨天分割产生的重复记录
     * - 重复定义：pkgName, startTime, endTime, durationSec 均相同 OR 跨天分割重复
     * - 保留策略：保留id最小的记录，或时长最长的记录
     */
    private suspend fun cleanDuplicateAppSessions() {
        withContext(Dispatchers.IO) {
            Log.i(TAG, "开始清理重复的App会话记录（增强版）...")
            try {
                val allSessions = appSessionDao.getAllSessions()
                if (allSessions.isEmpty()) {
                    Log.i(TAG, "没有会话记录可供清理")
                    return@withContext
                }

                val sessionsToKeep = mutableSetOf<Int>()
                val sessionsToDelete = mutableListOf<Int>()
                
                // 🔧 第一步：清理完全相同的重复记录
                val exactDuplicates = allSessions.groupBy { 
                    "${it.pkgName}_${it.startTime}_${it.endTime}_${it.durationSec}"
                }

                exactDuplicates.forEach { (_, sessions) ->
                    if (sessions.size > 1) {
                        val sessionToKeep = sessions.minByOrNull { it.id }
                        if (sessionToKeep != null) {
                            sessionsToKeep.add(sessionToKeep.id)
                            sessions.forEach { session ->
                                if (session.id != sessionToKeep.id) {
                                    sessionsToDelete.add(session.id)
                                }
                            }
                            Log.w(TAG, "清理完全重复会话: ${sessionToKeep.pkgName}, 删除 ${sessions.size - 1} 个重复项")
                        }
                    }
                }

                // 🔧 第二步：清理跨天分割产生的重复记录
                val remainingSessions = allSessions.filter { !sessionsToDelete.contains(it.id) }
                val sessionsByPackageAndDate = remainingSessions.groupBy { "${it.pkgName}_${it.date}" }
                
                sessionsByPackageAndDate.forEach { (key, sessions) ->
                    if (sessions.size > 1) {
                        // 检查是否有时间重叠的会话（可能是跨天分割重复）
                        val sortedSessions = sessions.sortedBy { it.startTime }
                        val overlappingSessions = mutableListOf<List<AppSessionUserEntity>>()
                        
                        for (i in sortedSessions.indices) {
                            val currentSession = sortedSessions[i]
                            val overlappingGroup = mutableListOf(currentSession)
                            
                            for (j in i + 1 until sortedSessions.size) {
                                val nextSession = sortedSessions[j]
                                // 检查时间重叠或紧密相邻（5秒内）
                                if (nextSession.startTime <= currentSession.endTime + 5000) {
                                    overlappingGroup.add(nextSession)
                                }
                            }
                            
                            if (overlappingGroup.size > 1) {
                                overlappingSessions.add(overlappingGroup)
                            }
                        }
                        
                        // 对每组重叠会话，保留时长最长的
                        overlappingSessions.forEach { group ->
                            val sessionToKeep = group.maxByOrNull { it.durationSec }
                            if (sessionToKeep != null && !sessionsToKeep.contains(sessionToKeep.id)) {
                                sessionsToKeep.add(sessionToKeep.id)
                                group.forEach { session ->
                                    if (session.id != sessionToKeep.id && !sessionsToDelete.contains(session.id)) {
                                        sessionsToDelete.add(session.id)
                                    }
                                }
                                Log.w(TAG, "清理跨天重复会话: ${sessionToKeep.pkgName}, 保留最长会话(${sessionToKeep.durationSec}s), 删除 ${group.size - 1} 个重复项")
                            }
                        }
                    }
                }

                if (sessionsToDelete.isNotEmpty()) {
                    // 批量删除重复记录
                    appSessionDao.deleteSessionsByIds(sessionsToDelete)
                    Log.i(TAG, "🎯 清理完成：删除了 ${sessionsToDelete.size} 条重复的会话记录")
                } else {
                    Log.i(TAG, "✅ 未发现需要清理的重复会话记录")
                }

            } catch (e: Exception) {
                Log.e(TAG, "清理重复会话记录时发生错误", e)
            }
        }
    }

    /**
     * 发送广播以更新所有Widget
     */
    private fun notifyWidgetUpdate() {
        try {
            Log.i(TAG, "🚀 发送Widget更新广播...")
            val intent = Intent(this, OffTimeLockScreenWidget.WidgetUpdateReceiver::class.java).apply {
                action = OffTimeLockScreenWidget.ACTION_UPDATE_WIDGET
            }
            sendBroadcast(intent)
            Log.i(TAG, "✅ Widget更新广播发送成功")
        } catch (e: Exception) {
            Log.e(TAG, "❌ 发送Widget更新广播失败", e)
        }
    }
} 