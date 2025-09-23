package com.offtime.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.offtime.app.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.offtime.app.data.repository.AppSessionRepository
import com.offtime.app.manager.DataUpdateManager
import com.offtime.app.utils.DataCleanupManager
import java.util.Date
import javax.inject.Inject

/**
 * 统一定时更新服务
 * 
 * 核心职责：
 * 1. 每分钟执行统一的数据更新流程
 * 2. 严格按序同步执行：原始数据收集 → 基础数据更新 → 聚合数据更新 → 前端UI刷新
 * 3. 确保数据的一致性和时效性
 * 
 * 统一更新流程（严格按序执行）：
 * 1. 原始数据收集：调用UsageStatsCollectorService拉取最新的应用使用事件
 * 2. 基础数据更新：处理跨日期会话，更新活跃应用状态，更新app_sessions_user表
 * 3. 聚合数据更新：将原始数据聚合到daily_usage_user、summary_usage_user等中间表
 * 4. UI刷新通知：通知前端界面刷新数据显示
 * 
 * 设计理念：
 * - 统一的更新时机：所有数据更新都在同一个时间点按序触发
 * - 同步的更新流程：确保数据依赖关系正确，避免竞争条件
 * - 集中的调度控制：取代分散的独立定时机制
 * - 前端感知机制：通过DataUpdateManager通知前端数据已更新
 */
@AndroidEntryPoint
class UnifiedUpdateService : Service() {
    
    companion object {
        const val ACTION_START_UNIFIED_UPDATE = "com.offtime.app.START_UNIFIED_UPDATE"
        const val ACTION_STOP_UNIFIED_UPDATE = "com.offtime.app.STOP_UNIFIED_UPDATE"
        const val ACTION_MANUAL_UPDATE = "com.offtime.app.MANUAL_UPDATE"
        
        private const val TAG = "UnifiedUpdateService"
        private const val UPDATE_INTERVAL_MS = 30_000L   // 30秒更新间隔
        private const val QUICK_UPDATE_INTERVAL_MS = 10_000L  // 快速更新间隔（活跃应用）
        private const val NOTIFICATION_ID = 2002
        private const val CHANNEL_ID = "unified_update_channel"
        
        /**
         * 启动统一更新服务
         */
        fun startUnifiedUpdate(context: android.content.Context) {
            val intent = android.content.Intent(context, UnifiedUpdateService::class.java)
            intent.action = ACTION_START_UNIFIED_UPDATE
            
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Android 8.0+ 使用前台服务
                    context.startForegroundService(intent)
                } else {
                    // Android 7.x 及以下使用普通服务
                    context.startService(intent)
                }
                android.util.Log.d(TAG, "启动统一更新服务")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "启动统一更新服务失败", e)
            }
        }
        
        /**
         * 停止统一更新服务
         */
        fun stopUnifiedUpdate(context: android.content.Context) {
            val intent = android.content.Intent(context, UnifiedUpdateService::class.java)
            intent.action = ACTION_STOP_UNIFIED_UPDATE
            
            try {
                // 停止服务不需要使用前台服务
                context.startService(intent)
                android.util.Log.d(TAG, "停止统一更新服务")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "停止统一更新服务失败", e)
            }
        }
        
        // 防重复触发机制
        private const val MIN_MANUAL_TRIGGER_INTERVAL = 2_000L  // 手动触发最小间隔降低到2秒
        private var lastManualTriggerTime = 0L
        
        /**
         * 手动触发立即更新（带防重复机制）
         */
        fun triggerManualUpdate(context: android.content.Context) {
            val currentTime = System.currentTimeMillis()
            
            // 检查是否在最小间隔内
            if (currentTime - lastManualTriggerTime < MIN_MANUAL_TRIGGER_INTERVAL) {
                android.util.Log.w(TAG, "⏱️ 手动触发被跳过 - 距离上次触发仅${(currentTime - lastManualTriggerTime)/1000}秒，小于${MIN_MANUAL_TRIGGER_INTERVAL/1000}秒限制")
                return
            }
            
            lastManualTriggerTime = currentTime
            val intent = android.content.Intent(context, UnifiedUpdateService::class.java)
            intent.action = ACTION_MANUAL_UPDATE
            
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Android 8.0+ 使用前台服务
                    context.startForegroundService(intent)
                } else {
                    // Android 7.x 及以下使用普通服务
                    context.startService(intent)
                }
                android.util.Log.d(TAG, "触发手动更新")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "触发手动更新失败", e)
            }
        }
    }
    
    @Inject
    lateinit var appSessionRepository: AppSessionRepository
    
    @Inject
    lateinit var dataUpdateManager: DataUpdateManager
    
    @Inject
    lateinit var dataCleanupManager: DataCleanupManager
    
    // 服务协程作用域
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // 定时更新控制标志
    private var isPeriodicUpdateRunning = false
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d(TAG, "统一更新服务已创建")
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_UNIFIED_UPDATE -> {
                // 启动前台服务
                startForeground(NOTIFICATION_ID, createNotification())
                startPeriodicUpdate()
            }
            ACTION_STOP_UNIFIED_UPDATE -> {
                stopPeriodicUpdate()
                stopSelf(startId)
            }
            ACTION_MANUAL_UPDATE -> {
                // 如果定时更新没有运行，先启动前台服务和定时循环
                if (!isPeriodicUpdateRunning) {
                    startForeground(NOTIFICATION_ID, createNotification())
                    startPeriodicUpdate()
                }
                
                // 执行手动更新，但不停止服务
                serviceScope.launch {
                    performUnifiedUpdate(DataUpdateManager.UPDATE_TYPE_MANUAL)
                }
            }
        }
        return START_STICKY  // 服务被杀死后自动重启
    }
    
    /**
     * 启动定时更新
     */
    private fun startPeriodicUpdate() {
        if (isPeriodicUpdateRunning) {
            Log.d(TAG, "定时更新已在运行中")
            return
        }
        
        isPeriodicUpdateRunning = true
        Log.d(TAG, "启动定时更新机制 - 每${UPDATE_INTERVAL_MS / 1000}秒完整更新，每${QUICK_UPDATE_INTERVAL_MS / 1000}秒快速更新")
        
        serviceScope.launch {
            // 立即执行一次更新
            performUnifiedUpdate(DataUpdateManager.UPDATE_TYPE_PERIODIC)
            
            var quickUpdateCounter = 0
            
            // 开始定时循环
            while (isPeriodicUpdateRunning) {
                // 使用更短的间隔检查，但不是每次都执行完整更新
                delay(QUICK_UPDATE_INTERVAL_MS)
                quickUpdateCounter++
                
                if (isPeriodicUpdateRunning) {
                    if (quickUpdateCounter >= 3) {  // 30秒 = 3 * 10秒
                        // 完整更新
                        performUnifiedUpdate(DataUpdateManager.UPDATE_TYPE_PERIODIC)
                        quickUpdateCounter = 0
                    } else {
                        // 快速更新：只更新活跃应用
                        performQuickActiveAppsUpdate()
                    }
                }
            }
        }
    }
    
    /**
     * 停止定时更新
     */
    private fun stopPeriodicUpdate() {
        isPeriodicUpdateRunning = false
        Log.d(TAG, "停止定时更新机制")
    }
    
    /**
     * 快速更新活跃应用
     * 只更新当前活跃应用的使用时长，不执行完整的数据聚合
     * 🔥 优化：直接调用实时拉取，减少延迟
     */
    private suspend fun performQuickActiveAppsUpdate() {
        try {
            Log.d(TAG, "执行快速活跃应用更新")
            
            // 🔥 直接触发活跃应用更新（内部已包含实时事件拉取）
            UsageStatsCollectorService.triggerActiveAppsUpdate(this)
            delay(100)  // 缩短等待时间
            
            // 通知UI更新（使用快速更新类型）
            dataUpdateManager.notifyDataUpdated("QUICK_UPDATE")
            
            Log.d(TAG, "快速活跃应用更新完成")
        } catch (e: Exception) {
            Log.e(TAG, "快速更新活跃应用失败", e)
        }
    }
    
    /**
     * 执行统一更新流程
     * 
     * 更新顺序（严格按序执行，确保数据依赖关系正确）：
     * 1. 原始数据收集 - UsageStatsCollectorService拉取最新事件
     * 2. 基础数据更新 - 更新原始会话数据和活跃应用状态
     * 3. 聚合数据更新 - 更新中间聚合表
     * 4. UI刷新通知 - 通知前端更新显示
     */
    private suspend fun performUnifiedUpdate(updateType: String) {
        try {
            val startTime = System.currentTimeMillis()
            Log.d(TAG, "开始执行统一更新流程 - $updateType")
            
            // === 第一阶段：原始数据收集 ===
            Log.d(TAG, "第一阶段：原始数据收集")
            collectRawUsageData()
            
            // === 第二阶段：基础数据更新 ===
            Log.d(TAG, "第二阶段：基础数据更新")
            updateBaseDataTables()
            
            // === 第三阶段：聚合数据更新 ===
            Log.d(TAG, "第三阶段：聚合数据更新")
            updateAggregatedDataTables()
            
            // === 第四阶段：数据清理检查 ===
            Log.d(TAG, "第四阶段：数据清理检查")
            performDataCleanupIfNeeded()
            
            // === 第五阶段：UI刷新通知 ===
            Log.d(TAG, "第五阶段：UI刷新通知")
            notifyUIDataUpdated(updateType)
            
            val duration = System.currentTimeMillis() - startTime
            Log.d(TAG, "统一更新流程完成 - $updateType，耗时：${duration}ms")
            
        } catch (e: Exception) {
            Log.e(TAG, "统一更新流程失败", e)
        }
    }
    
    /**
     * 收集原始使用数据
     * 第一步：从Android系统获取最新的应用使用事件
     */
    private suspend fun collectRawUsageData() {
        try {
            Log.d(TAG, "开始收集原始使用数据")
            
            // 1. 触发UsageStatsCollectorService拉取最新事件
            UsageStatsCollectorService.triggerEventsPull(this)
            
            // 等待事件拉取完成（缩短延迟）
            delay(500)
            
            Log.d(TAG, "原始使用数据收集完成")
            
        } catch (e: Exception) {
            Log.e(TAG, "收集原始使用数据失败", e)
        }
    }
    
    /**
     * 更新基础数据表
     * 第二步：处理跨日期会话，更新活跃应用状态
     */
    private suspend fun updateBaseDataTables() {
        try {
            Log.d(TAG, "更新基础数据表开始")
            
            // 1. 处理跨日期活跃会话（重要：必须在触发活跃应用更新之前处理）
            handleCrossDayActiveSessions()
            
            // 2. 触发更新当前活跃应用的使用时长
            UsageStatsCollectorService.triggerActiveAppsUpdate(this)
            
            // 等待活跃应用更新完成（缩短延迟）
            delay(300)
            
            // 3. 确保默认表存在
            appSessionRepository.initializeDefaultTableIfEmpty()
            
            Log.d(TAG, "基础数据表更新完成")
            
        } catch (e: Exception) {
            Log.e(TAG, "更新基础数据表失败", e)
        }
    }
    
    /**
     * 更新聚合数据表
     * 第三步：将原始会话数据聚合到各级汇总表
     * 包括：daily_usage_user, summary_usage_user, 周/月汇总表等
     */
    private suspend fun updateAggregatedDataTables() {
        try {
            Log.d(TAG, "更新聚合数据表开始")
            
            // 触发完整的数据聚合流程
            val aggregationIntent = Intent(this, DataAggregationService::class.java)
            aggregationIntent.action = DataAggregationService.ACTION_AGGREGATE_DATA
            startService(aggregationIntent)
            
            // 等待聚合完成（缩短延迟）
            delay(1000)
            
            Log.d(TAG, "聚合数据表更新完成")
            
        } catch (e: Exception) {
            Log.e(TAG, "更新聚合数据表失败", e)
        }
    }
    
    /**
     * 通知UI数据已更新
     * 通过DataUpdateManager发送更新事件，通知前端界面刷新数据
     */
    private fun notifyUIDataUpdated(updateType: String) {
        try {
            dataUpdateManager.notifyDataUpdated(updateType)
            Log.d(TAG, "已通知UI数据更新 - $updateType")
        } catch (e: Exception) {
            Log.e(TAG, "通知UI数据更新失败", e)
        }
    }
    
    /**
     * 处理跨日期活跃会话
     * 
     * 问题：当应用从昨天开始使用到今天，而今天没有其他应用使用时，
     * 不会触发ACTIVITY_PAUSED事件，导致使用时间不被记录
     * 
     * 解决：主动检查并处理跨日期的活跃会话，将其分割成昨天和今天的两个会话
     */
    private suspend fun handleCrossDayActiveSessions() {
        try {
            Log.d(TAG, "检查跨日期活跃会话")
            
            // 获取当前时间和日期
            val currentTime = System.currentTimeMillis()
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(Date(currentTime))
            val yesterdayTime = currentTime - 24 * 60 * 60 * 1000L
            val yesterday = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(Date(yesterdayTime))
            
            // 检查是否有跨日期会话需要处理
            val shouldHandleCrossDay = checkIfCrossDaySessionsExist(today, yesterday)
            
            if (shouldHandleCrossDay) {
                Log.d(TAG, "发现跨日期活跃会话，开始处理")
                
                // 1. 强制保存当前活跃会话到当前时间 - 这是导致问题的根源，予以注释
                // forceFlushActiveSessions()
                
                // 2. 通过拉取事件的方式触发会话保存
                // 这会让UsageStatsCollectorService检查并保存当前活跃的会话
                val pullEventsIntent = Intent(this, UsageStatsCollectorService::class.java)
                pullEventsIntent.action = UsageStatsCollectorService.ACTION_PULL_EVENTS
                startService(pullEventsIntent)
                
                // 等待事件处理完成
                delay(3000)
                
                Log.d(TAG, "跨日期活跃会话处理完成")
            } else {
                Log.d(TAG, "无跨日期活跃会话需要处理")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "处理跨日期活跃会话失败", e)
        }
    }
    
    /**
     * 强制保存当前活跃会话
     * 直接从UsageStatsCollectorService获取活跃会话并保存到当前时间
     */
    private suspend fun forceFlushActiveSessions() {
        try {
            Log.d(TAG, "强制保存当前活跃会话 - 此功能已因中断问题停用")
            
            // 触发UsageStatsCollectorService强制刷新所有活跃会话
            // val intent = Intent(this, UsageStatsCollectorService::class.java)
            // intent.action = "FORCE_FLUSH_ACTIVE_SESSIONS"
            // startService(intent)
            
            // 等待强制刷新完成
            // delay(1500)
            
            // Log.d(TAG, "强制保存活跃会话完成")
            
        } catch (e: Exception) {
            Log.e(TAG, "强制保存活跃会话失败", e)
        }
    }
    
    /**
     * 检查是否存在跨日期会话
     * 
     * 简化逻辑：主要检查最后一次数据更新的时间，如果跨日期了，需要处理
     */
    private suspend fun checkIfCrossDaySessionsExist(today: String, yesterday: String): Boolean {
        return try {
            // 检查最后一次数据更新的时间
            val lastUpdateTime = appSessionRepository.getLastTimestamp()
            val lastUpdateDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(Date(lastUpdateTime))
            
            // 如果最后更新时间不是今天，可能存在跨日期会话
            val hasOldLastUpdate = lastUpdateDate != today
            
            // 检查昨天是否有较晚的会话记录（可能跨日期到今天）
            val hasLateYesterdaySession = checkLateYesterdaySession(yesterday)
            
            val shouldHandle = hasOldLastUpdate || hasLateYesterdaySession
            
            if (shouldHandle) {
                Log.d(TAG, "跨日期会话检查结果: 最后更新日期=$lastUpdateDate, 今天=$today, 昨天有晚期会话=$hasLateYesterdaySession")
            }
            
            shouldHandle
            
        } catch (e: Exception) {
            Log.e(TAG, "检查跨日期会话失败", e)
            // 发生错误时，保险起见认为可能有跨日期会话
            true
        }
    }
    
    /**
     * 检查昨天是否有较晚的会话记录
     * 如果昨天22点后还有应用使用，可能跨日期到今天
     */
    private suspend fun checkLateYesterdaySession(@Suppress("UNUSED_PARAMETER") yesterday: String): Boolean {
        return try {
            // 获取昨天22点的时间戳
            val calendar = java.util.Calendar.getInstance()
            calendar.add(java.util.Calendar.DAY_OF_YEAR, -1)
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 22)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            val late22Time = calendar.timeInMillis
            
            // 简化检查：如果最后事件时间戳大于昨天22点，认为可能有跨日期会话
            val lastTimestamp = appSessionRepository.getLastTimestamp()
            val hasLateSession = lastTimestamp > late22Time
            
            if (hasLateSession) {
                Log.d(TAG, "检测到昨天22点后有活动，最后时间戳: ${java.text.SimpleDateFormat("MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(Date(lastTimestamp))}")
            }
            
            hasLateSession
            
        } catch (e: Exception) {
            Log.e(TAG, "检查昨天晚期会话失败", e)
            false
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopPeriodicUpdate()
        Log.d(TAG, "统一更新服务已销毁")
    }
    
    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "统一数据更新",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "每分钟统一更新应用数据"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
            Log.d(TAG, "通知渠道已创建")
        }
    }
    
    /**
     * 创建前台服务通知
     */
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("OffTimes 数据更新")
            .setContentText("正在每分钟自动更新使用统计数据")
            .setSmallIcon(R.drawable.ic_notification) // 确保这个图标存在
            .setOngoing(true) // 用户无法滑动删除
            .setSilent(true) // 静默通知
            .setShowWhen(false)
            .build()
    }
    
    /**
     * 执行数据清理检查
     * 如果需要清理，则执行数据清理任务
     */
    private suspend fun performDataCleanupIfNeeded() {
        try {
            if (dataCleanupManager.shouldPerformCleanup()) {
                Log.d(TAG, "开始执行数据清理...")
                dataCleanupManager.performDataCleanup()
                dataCleanupManager.markCleanupCompleted()
                Log.d(TAG, "数据清理完成")
            } else {
                Log.d(TAG, "暂不需要执行数据清理")
            }
        } catch (e: Exception) {
            Log.e(TAG, "数据清理检查失败", e)
        }
    }
}