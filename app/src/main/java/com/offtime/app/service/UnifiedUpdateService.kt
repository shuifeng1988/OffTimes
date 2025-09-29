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
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import android.os.PowerManager
import android.content.Context
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.os.SystemClock
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
 * 4. 数据清理检查：执行必要的数据清理和维护操作
 * 5. UI刷新通知：通知前端界面刷新数据显示
 * 6. Widget小插件更新：更新锁屏小插件显示最新数据
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
    
    // 服务协程作用域 - 使用更强的生命周期管理
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var periodicUpdateJob: Job? = null
    
    // 定时更新控制标志
    private var isPeriodicUpdateRunning = false
    
    // WakeLock 防止系统休眠影响定时更新
    private var wakeLock: PowerManager.WakeLock? = null
    
    // AlarmManager 备用唤醒机制
    private var alarmManager: AlarmManager? = null
    private var alarmPendingIntent: PendingIntent? = null
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d(TAG, "统一更新服务已创建")
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.util.Log.d(TAG, "🚀 onStartCommand called with action: ${intent?.action}")
        when (intent?.action) {
            ACTION_START_UNIFIED_UPDATE -> {
                android.util.Log.d(TAG, "🎯 处理 ACTION_START_UNIFIED_UPDATE")
                // 启动前台服务
                startForeground(NOTIFICATION_ID, createNotification())
                startPeriodicUpdate()
            }
            ACTION_STOP_UNIFIED_UPDATE -> {
                android.util.Log.d(TAG, "🛑 处理 ACTION_STOP_UNIFIED_UPDATE")
                stopPeriodicUpdate()
                stopSelf(startId)
            }
            ACTION_MANUAL_UPDATE -> {
                android.util.Log.d(TAG, "🔄 处理 ACTION_MANUAL_UPDATE")
                // 如果定时更新没有运行，先启动前台服务和定时循环
                if (!isPeriodicUpdateRunning) {
                    android.util.Log.d(TAG, "⚡ 定时更新未运行，启动前台服务和定时循环")
                    startForeground(NOTIFICATION_ID, createNotification())
                    startPeriodicUpdate()
                }
                
                // 执行手动更新，但不停止服务
                serviceScope.launch {
                    performUnifiedUpdate(DataUpdateManager.UPDATE_TYPE_MANUAL)
                }
            }
            "CHECK_SERVICE_STATUS" -> {
                android.util.Log.d(TAG, "🔍 检查服务状态 - 协程运行: $isPeriodicUpdateRunning")
                // 只有当协程真的停止了才重新启动
                if (!isPeriodicUpdateRunning) {
                    android.util.Log.w(TAG, "⚠️ 检测到协程已停止，重新启动服务")
                    startForeground(NOTIFICATION_ID, createNotification())
                    startPeriodicUpdate()
                } else {
                    android.util.Log.d(TAG, "✅ 协程正常运行，无需重启")
                }
            }
            else -> {
                android.util.Log.w(TAG, "⚠️ 未知的action: ${intent?.action}")
            }
        }
        return START_STICKY  // 服务被杀死后自动重启
    }
    
    /**
     * 启动定时更新
     */
    private fun startPeriodicUpdate() {
        if (isPeriodicUpdateRunning) {
            android.util.Log.d(TAG, "⏰ 定时更新已在运行中")
            return
        }
        
        // 获取WakeLock防止系统休眠
        acquireWakeLock()
        
        // 启动AlarmManager备用唤醒机制
        startAlarmManagerBackup()
        
        isPeriodicUpdateRunning = true
        android.util.Log.d(TAG, "🚀 启动定时更新机制 - 每${UPDATE_INTERVAL_MS / 1000}秒完整更新")
        
        // 取消之前的任务
        periodicUpdateJob?.cancel()
        
        // 启动新的协程任务
        periodicUpdateJob = serviceScope.launch {
            try {
                android.util.Log.d(TAG, "🎯 协程启动成功，开始执行定时循环")
            // 立即执行一次更新
                val startTime = System.currentTimeMillis()
            performUnifiedUpdate(DataUpdateManager.UPDATE_TYPE_PERIODIC)
            
                // 开始定时循环 - 严格按30秒间隔执行完整更新
                var nextUpdateTime = startTime + UPDATE_INTERVAL_MS
                
                while (isPeriodicUpdateRunning && isActive) {
                    val currentTime = System.currentTimeMillis()
                    val waitTime = nextUpdateTime - currentTime
                    
                    if (waitTime > 0) {
                        android.util.Log.d(TAG, "⏱️ 等待${waitTime}ms后执行下次更新，协程活跃: $isActive")
                        delay(waitTime)
                    }
                    
                    // 双重检查：既检查标志位也检查协程状态
                    if (isPeriodicUpdateRunning && isActive) {
                        android.util.Log.d(TAG, "🔄 执行完整更新 (严格30秒周期)")
                        performUnifiedUpdate(DataUpdateManager.UPDATE_TYPE_PERIODIC)
                        
                        // 计算下次更新时间，确保严格30秒间隔
                        nextUpdateTime += UPDATE_INTERVAL_MS
                    }
                }
                android.util.Log.d(TAG, "🛑 定时循环结束")
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ 定时循环异常", e)
                isPeriodicUpdateRunning = false
                
                // 异常时尝试重启（除非是取消异常）
                if (e !is kotlinx.coroutines.CancellationException) {
                    android.util.Log.w(TAG, "🔄 尝试重启定时循环")
                    delay(5000) // 等待5秒后重试
                    if (!isPeriodicUpdateRunning && isActive) {
                        startPeriodicUpdate()
                    }
                }
            } finally {
                // 释放WakeLock
                releaseWakeLock()
            }
        }
    }
    
    /**
     * 停止定时更新
     */
    private fun stopPeriodicUpdate() {
        isPeriodicUpdateRunning = false
        periodicUpdateJob?.cancel()
        stopAlarmManagerBackup()
        releaseWakeLock()
        android.util.Log.d(TAG, "🛑 停止定时更新机制")
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
 * 4. 数据清理检查 - 执行必要的数据清理和维护操作
 * 5. UI刷新通知 - 通知前端更新显示
 * 6. Widget小插件更新 - 更新锁屏小插件显示最新数据
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
            
            // === 第六阶段：Widget小插件更新 ===
            Log.d(TAG, "第六阶段：Widget小插件更新")
            updateWidgets()
            
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
            
            // 1. 触发UsageStatsCollectorService统一更新（事件拉取 + 活跃应用更新）
            UsageStatsCollectorService.triggerUnifiedUpdate(this)
            
            // 等待统一更新完成
            delay(300)
            
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
            
            // 1. 处理跨日期活跃会话
            handleCrossDayActiveSessions()
            
            // 2. 确保默认表存在
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
     * 更新Widget小插件
     * 第六步：更新锁屏小插件显示最新数据
     */
    private fun updateWidgets() {
        try {
            Log.d(TAG, "开始更新Widget小插件")
            
            // 使用WidgetUpdateManager更新所有锁屏小插件
            com.offtime.app.widget.WidgetUpdateManager.updateAllLockScreenWidgets(this)
            
            Log.d(TAG, "Widget小插件更新完成")
        } catch (e: Exception) {
            Log.e(TAG, "更新Widget小插件失败", e)
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
                
                // 移除重复的事件拉取，依赖collectRawUsageData()已拉取的最新事件
                // 跨日期会话处理逻辑由数据库层面的时间检查自动处理
                
                // 等待前面拉取的事件处理完成（进一步优化：减少延迟时间）
                delay(200)
                
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
        releaseWakeLock()
        android.util.Log.d(TAG, "🔥 统一更新服务已销毁")
    }
    
    /**
     * 获取WakeLock防止系统休眠
     * 针对真实手机加强保活策略
     */
    private fun acquireWakeLock() {
        try {
            if (wakeLock == null) {
                val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "OffTimes:UnifiedUpdateService"
                )
            }
            
            if (wakeLock?.isHeld != true) {
                // 针对真实手机：延长WakeLock超时时间到30分钟
                // 确保在长时间后台运行时不会被释放
                wakeLock?.acquire(30 * 60 * 1000L) // 30分钟超时
                android.util.Log.d(TAG, "🔋 WakeLock已获取 (30分钟超时)")
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ 获取WakeLock失败", e)
        }
    }
    
    /**
     * 释放WakeLock
     */
    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                android.util.Log.d(TAG, "🔋 WakeLock已释放")
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ 释放WakeLock失败", e)
        }
    }
    
    /**
     * 启动AlarmManager备用唤醒机制
     */
    private fun startAlarmManagerBackup() {
        try {
            if (alarmManager == null) {
                alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            }
            
            if (alarmPendingIntent == null) {
                val intent = android.content.Intent(this, AlarmReceiver::class.java)
                alarmPendingIntent = PendingIntent.getBroadcast(
                    this, 
                    0, 
                    intent, 
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }
            
            // 针对真实手机：使用更频繁的唤醒间隔（45秒）
            // 确保在Doze模式和应用待机时也能唤醒
            val alarmInterval = 45_000L // 45秒
            
            // 使用setExactAndAllowWhileIdle确保在Doze模式下也能唤醒
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Android 6.0+ 使用精确闹钟，即使在Doze模式下也会触发
                alarmManager?.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + alarmInterval,
                    alarmPendingIntent!!
                )
                android.util.Log.d(TAG, "⏰ AlarmManager精确唤醒机制已启动 (45秒间隔, 支持Doze模式)")
            } else {
                // Android 5.x 及以下使用普通重复闹钟
                alarmManager?.setRepeating(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + alarmInterval,
                    alarmInterval,
                    alarmPendingIntent!!
                )
                android.util.Log.d(TAG, "⏰ AlarmManager重复唤醒机制已启动 (45秒间隔)")
            }
            
            android.util.Log.d(TAG, "⏰ AlarmManager备用唤醒机制已启动")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ 启动AlarmManager失败", e)
        }
    }
    
    /**
     * 停止AlarmManager备用唤醒机制
     */
    private fun stopAlarmManagerBackup() {
        try {
            alarmPendingIntent?.let { pendingIntent ->
                alarmManager?.cancel(pendingIntent)
                android.util.Log.d(TAG, "⏰ AlarmManager备用唤醒机制已停止")
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ 停止AlarmManager失败", e)
        }
    }
    
    /**
     * AlarmManager广播接收器
     * 针对真实手机加强保活策略
     */
    class AlarmReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: android.content.Intent) {
            android.util.Log.d(TAG, "⏰ AlarmManager唤醒触发，检查服务状态")
            
            // 检查服务是否还在运行以及协程是否活跃
            val serviceIntent = android.content.Intent(context, UnifiedUpdateService::class.java)
            serviceIntent.action = "CHECK_SERVICE_STATUS"
            
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                android.util.Log.d(TAG, "⏰ AlarmManager检查服务状态")
                
                // 重新设置下一次闹钟（针对setExactAndAllowWhileIdle只触发一次的特性）
                scheduleNextAlarm(context)
                
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ AlarmManager检查服务失败", e)
            }
        }
        
        /**
         * 设置下一次AlarmManager唤醒
         */
        private fun scheduleNextAlarm(context: Context) {
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val intent = android.content.Intent(context, AlarmReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context, 
                    0, 
                    intent, 
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                val alarmInterval = 45_000L // 45秒
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        SystemClock.elapsedRealtime() + alarmInterval,
                        pendingIntent
                    )
                    android.util.Log.d(TAG, "⏰ 已设置下一次精确唤醒 (45秒后)")
                } else {
                    alarmManager.set(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        SystemClock.elapsedRealtime() + alarmInterval,
                        pendingIntent
                    )
                    android.util.Log.d(TAG, "⏰ 已设置下一次唤醒 (45秒后)")
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ 设置下一次闹钟失败", e)
            }
        }
    }
    
    /**
     * 创建通知渠道
     * 针对真实手机提升重要性以增强保活能力
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "统一数据更新",
                NotificationManager.IMPORTANCE_DEFAULT // 提升到DEFAULT级别
            ).apply {
                description = "后台收集应用使用数据和统计应用使用时间 - 30秒更新"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
                // 设置为系统级别的重要性，减少被杀死的可能性
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
            Log.d(TAG, "通知渠道已创建 (DEFAULT重要性)")
        }
    }
    
    /**
     * 创建前台服务通知
     * 针对真实手机增强保活能力
     */
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("OffTimes 数据更新")
            .setContentText("后台收集应用使用数据和统计应用使用时间 - 30秒更新")
            .setSmallIcon(R.drawable.ic_notification) // 确保这个图标存在
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // 与通知渠道保持一致
            .setOngoing(true) // 用户无法滑动删除
            .setSilent(true) // 静默通知
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // 锁屏可见
            .setAutoCancel(false) // 不可自动取消
            .setLocalOnly(true) // 本地通知，不同步到其他设备
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE) // 立即显示
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