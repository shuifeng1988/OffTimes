package com.offtime.app.service

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.offtime.app.R
import com.offtime.app.data.repository.AppSessionRepository
import com.offtime.app.util.AppLifecycleObserver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.delay
import javax.inject.Inject
import java.util.Locale

/**
 * 应用使用统计收集服务
 * 
 * 核心职责：
 * 1. 监控应用的启动和停止事件
 * 2. 收集应用使用统计数据(UsageStats API)
 * 3. 将使用会话数据保存到数据库
 * 4. 配合UnifiedUpdateService进行统一的数据更新
 * 
 * 工作原理：
 * - 作为前台服务持续运行，保持服务活跃状态
 * - 不再独立定时查询，改为接受UnifiedUpdateService的调用
 * - 监听ACTIVITY_RESUMED和ACTIVITY_PAUSED事件
 * - 计算应用的实际使用时长（最小5秒）
 * - 自动过滤无效的短时间使用记录
 * 
 * 数据处理流程：
 * 1. 从UsageStatsManager获取应用使用事件
 * 2. 解析事件类型和时间戳
 * 3. 计算使用时长并保存到app_sessions_user表
 * 4. 由UnifiedUpdateService统一触发后续聚合
 * 
 * 性能优化：
 * - 使用协程进行异步数据处理
 * - 维护当前活跃应用的内存映射
 * - 增量获取事件数据，避免重复处理
 * - 统一更新机制，避免重复触发
 * 
 * 权限要求：
 * - 使用情况访问权限 (USAGE_STATS_PERMISSION)
 * - 前台服务权限 (FOREGROUND_SERVICE)
 * - 通知权限 (用于显示前台服务通知)
 */
@AndroidEntryPoint
class UsageStatsCollectorService : Service() {
    
    @Inject
    lateinit var appSessionRepository: AppSessionRepository
    
    // 服务协程作用域，使用Default调度器优化CPU密集型任务
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // 使用统计管理器，用于获取应用使用事件
    private val usageStatsManager by lazy { 
        getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager 
    }
    
    // Activity管理器，用于获取当前前台应用
    private val activityManager by lazy {
        getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    }
    
    // 标识服务是否正在收集数据
    private var isCollecting = false
    
    // 新的状态机变量
    private var currentForegroundPackage: String? = null
    private var currentSessionStartTime: Long? = null
    
    // 暂停的会话信息（用于处理OffTimes短暂切换）
    private var pausedSessionPackage: String? = null
    private var pausedSessionStartTime: Long? = null
    private var pausedSessionPauseTime: Long? = null
    
    // 使用UsageStatsManager获取当前前台应用
    // 这是一个比ActivityManager.getRunningTasks更可靠的方法
    private var lastKnownForeground: String? = null
    private var lastKnownTs: Long = 0L

    companion object {
        private const val TAG = "UsageStatsCollector"
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "usage_stats_channel"
        
        const val ACTION_START_COLLECTION = "start_collection"
        const val ACTION_STOP_COLLECTION = "stop_collection"
        const val ACTION_PULL_EVENTS = "pull_events"
        const val ACTION_UPDATE_ACTIVE_APPS = "update_active_apps"
        
        /**
         * 触发事件拉取（供UnifiedUpdateService调用）
         * 这是新的统一更新机制的核心调用方法
         */
        fun triggerEventsPull(context: android.content.Context) {
            val intent = android.content.Intent(context, UsageStatsCollectorService::class.java)
            intent.action = ACTION_PULL_EVENTS
            context.startService(intent)
            android.util.Log.d(TAG, "触发事件拉取")
        }
        
        /**
         * 触发活跃应用更新
         * 供UnifiedUpdateService调用
         */
        fun triggerActiveAppsUpdate(context: android.content.Context) {
            val intent = android.content.Intent(context, UsageStatsCollectorService::class.java)
            intent.action = ACTION_UPDATE_ACTIVE_APPS
            context.startService(intent)
            android.util.Log.d(TAG, "触发活跃应用更新")
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d(TAG, "Usage stats collector service created")
    }
    
    /**
     * 当应用从任务列表中移除时调用
     * 实现保活机制，确保服务继续运行
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "应用从任务列表中移除，重启服务以保持运行")
        
        try {
            // 重新启动服务
            val restartIntent = Intent(this, UsageStatsCollectorService::class.java).apply {
                action = ACTION_START_COLLECTION
            }
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(restartIntent)
            } else {
                startService(restartIntent)
            }
            
            Log.d(TAG, "服务重启成功")
        } catch (e: Exception) {
            Log.e(TAG, "任务移除后重启服务失败", e)
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand: action=${intent?.action}, flags=$flags, startId=$startId")
        
        when (intent?.action) {
            ACTION_START_COLLECTION -> {
                if (!isCollecting) {
                    Log.d(TAG, "开始数据收集")
                    
                    // 启动前台服务
                    startForeground(NOTIFICATION_ID, createNotification())
                    
                    // 恢复活跃应用状态
                    restoreActiveApps()
                    
                    // 启动数据收集服务（不含独立定时循环）
                    startPeriodicCollection()
                    
                    isCollecting = true
                    Log.d(TAG, "数据收集服务已启动")
                } else {
                    Log.d(TAG, "数据收集服务已在运行")
                }
            }
            
            ACTION_STOP_COLLECTION -> {
                Log.d(TAG, "停止数据收集")
                stopCollection()
            }
            
            ACTION_PULL_EVENTS -> {
                if (isCollecting) {
                    Log.d(TAG, "手动拉取事件")
                    serviceScope.launch {
                        pullEvents()
                    }
                } else {
                    Log.d(TAG, "服务未运行，启动服务并拉取事件")
                    // 如果服务未运行，先启动服务
                    val startIntent = Intent(this, UsageStatsCollectorService::class.java).apply {
                        action = ACTION_START_COLLECTION
                    }
                    onStartCommand(startIntent, 0, startId)
                    
                    // 然后拉取事件
                    serviceScope.launch {
                        pullEvents()
                    }
                }
            }
            
            ACTION_UPDATE_ACTIVE_APPS -> {
                Log.d(TAG, "手动更新活跃应用")
                serviceScope.launch {
                    updateActiveAppsDuration()
                }
            }
            
            "FORCE_FLUSH_ACTIVE_SESSIONS" -> {
                Log.d(TAG, "强制刷新所有活跃会话")
                serviceScope.launch {
                    flushAllSessionsSync()
                }
            }
            
            else -> {
                Log.d(TAG, "未知的服务操作: ${intent?.action}")
            }
        }
        
        // 返回START_STICKY确保服务被系统杀死后能自动重启
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun startPeriodicCollection() {
        if (isCollecting) return
        
        isCollecting = true
        Log.d(TAG, "启动数据收集服务（不含独立定时循环）")
        
        serviceScope.launch {
            // 初始化默认表
            appSessionRepository.initializeDefaultTableIfEmpty()
            
            // 立即拉取一次事件
            pullEvents()
            
            Log.d(TAG, "数据收集服务已启动，启动定时更新机制")
            // 启动定时更新活跃应用时长的协程
            startPeriodicActiveAppsUpdate()
        }
    }
    
    /**
     * 获取当前真正的前台应用包名
     * 注意：在后台服务中，此API可能返回不准确的结果，谨慎使用
     */
    private fun getCurrentForegroundApp(): String? {
        return try {
            @Suppress("DEPRECATION")
            val runningTasks = activityManager.getRunningTasks(1)
            if (runningTasks.isNotEmpty()) {
                val topActivity = runningTasks[0].topActivity
                topActivity?.packageName
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "无法获取当前前台应用", e)
            null
        }
    }
    
    /**
     * 启动定期更新活跃应用时长的协程
     */
    private fun startPeriodicActiveAppsUpdate() {
        serviceScope.launch {
            while (isCollecting) {
                try {
                    // 每30秒更新一次活跃应用的使用时长
                    delay(30_000)
                    updateActiveAppsDuration()
                } catch (e: Exception) {
                    Log.e(TAG, "定期更新活跃应用时长失败", e)
                    // 出错后等待一段时间再继续
                    delay(10_000)
                }
            }
            Log.d(TAG, "定期更新活跃应用时长协程已停止")
        }
    }
    
    /**
     * 实时更新当前活跃应用的使用时长
     * 由UnifiedUpdateService按1分钟间隔调用
     */
    suspend fun updateActiveAppsDuration() {
        try {
            val currentTime = System.currentTimeMillis()
            
            // 🔥 关键优化：无论前台后台都主动拉取最新事件，确保实时统计准确
            Log.d(TAG, "🔄 主动拉取最新事件确保实时统计准确 (前台=${AppLifecycleObserver.isActivityInForeground.value})")
            pullLatestEventsForRealtime()
            
            // 🏠 若桌面/Launcher在前台，结束并清空当前会话，防止错误延长
            try {
                val nowTs = currentTime
                val ue = usageStatsManager.queryEvents(nowTs - 10_000, nowTs)
                var lastResumePkg: String? = null
                var lastResumeTs: Long = 0
                while (ue.hasNextEvent()) {
                    val e = UsageEvents.Event()
                    ue.getNextEvent(e)
                    if (e.eventType == UsageEvents.Event.ACTIVITY_RESUMED || e.eventType == 1 || e.eventType == 19) {
                        lastResumePkg = e.packageName
                        lastResumeTs = e.timeStamp
                    } else if ((e.eventType == UsageEvents.Event.ACTIVITY_PAUSED || e.eventType == 2 || e.eventType == 20 || e.eventType == 23) && e.packageName == lastResumePkg) {
                        lastResumePkg = null
                    }
                }
                if (lastResumePkg != null && isLauncherApp(lastResumePkg)) {
                    if (currentForegroundPackage != null) {
                        val start = currentSessionStartTime ?: (nowTs - 1000)
                        Log.d(TAG, "🏠 检测到Launcher在前台，结算并清空当前会话: $currentForegroundPackage")
                        saveSession(currentForegroundPackage!!, start, nowTs)
                        currentForegroundPackage = null
                        currentSessionStartTime = null
                    }
                    Log.d(TAG, "实时统计 → 当前无活跃应用(Launcher)")
                    return
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Launcher检测失败(忽略): ${t.message}")
            }
            
            // 移除有问题的清理逻辑，保持原本简单可靠的方式
            
            // 回退方案：若仍未知当前前台应用，则尝试通过 queryUsageStats 推断
            if (currentForegroundPackage == null) {
                val fg = getForegroundApp()
                val offTimesPrefix = "com.offtime.app"
                if (fg != null && !(fg.startsWith(offTimesPrefix) && !AppLifecycleObserver.isActivityInForeground.value)) {
                    currentForegroundPackage = fg
                    currentSessionStartTime = if (lastKnownTs > 0) lastKnownTs else currentTime
                    Log.d(TAG, "✅ 回退推断前台应用: $fg, startTs=${currentSessionStartTime}")
                }
            }
            
            // 使用新的状态机变量
            if (currentForegroundPackage != null && currentSessionStartTime != null) {
                val packageName = currentForegroundPackage!!
                val startTime = currentSessionStartTime!!
                
                Log.d(TAG, "实时统计 → 当前活跃应用: $packageName")

                // 如果是OffTimes自身，并且UI不在前台，则进行纠偏：
                if (packageName.startsWith("com.offtime.app") && !AppLifecycleObserver.isActivityInForeground.value) {
                    // 优先尝试通过近期UsageEvents找出真正的前台应用（避免等待下一次pullEvents导致的延迟）
                    try {
                        val nowTs = currentTime
                        val lookback = nowTs - (3 * 60 * 1000) // 回看3分钟
                        val ue = usageStatsManager.queryEvents(lookback, nowTs)
                        var lastResumePkg: String? = null
                        var lastResumeTs: Long = 0
                        val offTimesPrefix = "com.offtime.app"
                        while (ue.hasNextEvent()) {
                            val e = UsageEvents.Event()
                            ue.getNextEvent(e)
                            val p = e.packageName ?: continue
                            if (p.startsWith(offTimesPrefix)) continue
                            if (!isUserApp(p)) continue
                            if (e.eventType == UsageEvents.Event.ACTIVITY_RESUMED || e.eventType == 1 || e.eventType == 19) {
                                lastResumePkg = p
                                lastResumeTs = e.timeStamp
                            } else if ((e.eventType == UsageEvents.Event.ACTIVITY_PAUSED || e.eventType == 2 || e.eventType == 20 || e.eventType == 23) && p == lastResumePkg) {
                                // 被暂停，作废
                                lastResumePkg = null
                            }
                        }
                        if (lastResumePkg != null) {
                            Log.d(TAG, "🔧 实时纠偏: OffTimes误判为前台，切换到 $lastResumePkg 自$lastResumeTs 起")
                            currentForegroundPackage = lastResumePkg
                            currentSessionStartTime = lastResumeTs
                        } else {
                            // 最后兜底：通过getForegroundApp()再确认一次
                            val fg = getForegroundApp()
                            if (fg != null && !fg.startsWith(offTimesPrefix)) {
                                Log.d(TAG, "🔧 实时纠偏(getForegroundApp): 切换到 $fg")
                                currentForegroundPackage = fg
                                currentSessionStartTime = currentTime
                            } else {
                                Log.d(TAG, "🚫 实时统计过滤: OffTimes UI不在前台，不累积使用时间 (packageName=$packageName)")
                                return
                            }
                        }
                    } catch (t: Throwable) {
                        Log.d(TAG, "🚫 实时统计过滤: OffTimes UI不在前台，不累积使用时间 (packageName=$packageName)")
                        return
                    }
                }
                
                    val currentDuration = (currentTime - startTime) / 1000
                    Log.d(TAG, "实时统计 → $packageName, 已使用${currentDuration}秒")
                    
                    // 使用Repository的实时更新方法
                    try {
                        appSessionRepository.updateActiveSessionDuration(
                            pkgName = packageName,
                            currentStartTime = startTime,
                            currentTime = currentTime
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "实时更新应用时长失败: $packageName", e)
                }
            } else {
                Log.d(TAG, "实时统计 → 当前无活跃应用")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "实时统计更新失败", e)
        }
    }
    
    /**
     * 实时拉取最新事件（轻量级，用于实时统计纠偏）
     * 无论前台后台都会拉取，确保应用切换能及时检测到
     */
    private suspend fun pullLatestEventsForRealtime() {
        try {
            val currentTime = System.currentTimeMillis()
            val lookbackTime = currentTime - (3 * 60 * 1000) // 回看3分钟，缩短范围提高性能
            val lastProcessedTime = maxOf(lookbackTime, lastEventTs)
            
            // 缩短重复拉取间隔，提高检测灵敏度
            if (currentTime - lastProcessedTime < 5000) { // 5秒内不重复拉取
                return
            }
            
            Log.d(TAG, "🔄 实时事件拉取: ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(lastProcessedTime)} - ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(currentTime)}")
            
            val newLastTs = processUsageEvents(lastProcessedTime, currentTime)
            if (newLastTs > lastEventTs) {
                lastEventTs = newLastTs
                Log.d(TAG, "🔄 实时事件拉取完成，更新时间戳至: $newLastTs")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "实时事件拉取失败", e)
        }
    }
    
    /**
     * 恢复活跃应用状态（服务重启时），适配新的状态机
     */
    private fun restoreActiveApps() {
        serviceScope.launch {
            try {
                val now = System.currentTimeMillis()
                // 回看15分钟以防万一
                val lookbackTime = now - (15 * 60 * 1000)
                
                Log.d(TAG, "尝试恢复活跃应用状态（状态机）")
                
                val usageEvents = usageStatsManager.queryEvents(lookbackTime, now)
                var lastForegroundApp: String? = null
                var lastForegroundTime: Long = 0
                
                // 遍历近期事件，找到最后一个进入前台的应用
                while (usageEvents.hasNextEvent()) {
                    val event = UsageEvents.Event()
                    usageEvents.getNextEvent(event)
                    
                    val eventPackageName = event.packageName
                    if (eventPackageName == null || eventPackageName.startsWith("com.offtime.app")) {
                        continue
                    }

                    when (event.eventType) {
                        UsageEvents.Event.ACTIVITY_RESUMED, 1, 19 -> {
                            lastForegroundApp = eventPackageName
                            lastForegroundTime = event.timeStamp
                        }
                        UsageEvents.Event.ACTIVITY_PAUSED, 2, 20, 23 -> {
                            if (eventPackageName == lastForegroundApp) {
                                // 如果最后一个前台应用已经关闭，则没有需要恢复的应用
                                lastForegroundApp = null
                            }
                        }
                    }
                }

                // 如果找到了最后一个前台应用，并且它尚未关闭，则恢复它
                if (lastForegroundApp != null) {
                    currentForegroundPackage = lastForegroundApp
                    currentSessionStartTime = lastForegroundTime
                    Log.d(TAG, "✅ 恢复了活跃应用: $lastForegroundApp, 开始时间: $lastForegroundTime")
                } else {
                    Log.d(TAG, "没有发现需要恢复的活跃应用")
                    // 事件为空时的回退：用 queryUsageStats 推断最近可见的用户应用
                    val fg = getForegroundApp()
                    val offTimesPrefix = "com.offtime.app"
                    if (fg != null && !fg.startsWith(offTimesPrefix)) {
                        currentForegroundPackage = fg
                        currentSessionStartTime = if (lastKnownTs > 0) lastKnownTs else now
                        Log.d(TAG, "✅ 回退恢复活跃应用: $fg, 开始时间: ${currentSessionStartTime}")
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "恢复活跃应用状态失败", e)
            }
        }
    }
    
    private fun stopCollection() {
        isCollecting = false
        Log.d(TAG, "Stopping usage stats collection")
        // 先结算仍在前台的应用
        flushAllSessions()
    }
    
    private fun pullEvents() {
        serviceScope.launch {
            try {
                val begin = appSessionRepository.getLastTimestamp()
                val end = System.currentTimeMillis()

                // 第一次启动，从当天 0 点开始拉取
                val realBegin = if (begin == 0L) {
                    java.util.Calendar.getInstance().apply {
                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }.timeInMillis
                } else begin

                val lastTs = processUsageEvents(realBegin, end)

                // 使用最后处理到的事件时间戳做为新的 begin，避免跳过边界事件
                appSessionRepository.updateLastTimestamp(lastTs)
                Log.d(TAG, "Events pulled successfully from $realBegin to $end (last=$lastTs)")
                
                // 不再独立触发数据聚合，由UnifiedUpdateService统一管理

            } catch (e: Exception) {
                Log.e(TAG, "Error pulling usage events", e)
            }
        }
    }
    
    /**
     * 解析 [beginTime, endTime] 区间内的 UsageEvents。
     * 返回最后处理到的事件时间戳，用于作为下次拉取的 begin。
     */
    private suspend fun processUsageEvents(beginTime: Long, endTime: Long): Long {
        var lastEventTs = beginTime
        var eventCount = 0
        var resumeCount = 0
        var pauseCount = 0
        
        try {
            val usageEvents = usageStatsManager.queryEvents(beginTime, endTime)
            Log.d(TAG, "开始处理事件: ${java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(beginTime)} - ${java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(endTime)}")

            // 状态机事件处理循环
            while (usageEvents.hasNextEvent()) {
                val event = UsageEvents.Event()
                usageEvents.getNextEvent(event)
                lastEventTs = event.timeStamp
                eventCount++

                // 核心修复：过滤系统事件和OffTimes的后台事件
                val eventPackageName = event.packageName
                if (eventPackageName == null) {
                    continue
                }
                
                // 增加一个变量来跟踪上一个前台应用
                @Suppress("UNUSED_VARIABLE")
                val previousForegroundPackage = currentForegroundPackage

                // 如果OffTimes进入后台，重置标志
                if (eventPackageName.startsWith("com.offtime.app") && 
                    (event.eventType == UsageEvents.Event.ACTIVITY_PAUSED || event.eventType == 2)) {
                }

                // 过滤明显的系统事件
                if (eventPackageName.startsWith("android") ||
                    eventPackageName.startsWith("com.android.systemui") ||
                    eventPackageName.startsWith("com.google.android.apps.nexuslauncher") ||
                    eventPackageName.startsWith("com.google.android.permissioncontroller") ||
                    eventPackageName.startsWith("com.android.vending") ||
                    eventPackageName.startsWith("com.google.android.gms")) {
                    continue
                }
                
                // 🔥 关键过滤：OffTimes后台事件一律过滤，只保留真正的前台使用
                if (eventPackageName.startsWith("com.offtime.app")) {
                    // 如果一个事件声称OffTimes到了前台...
                    // 注意：必须包含所有可能触发前台的事件类型：ACTIVITY_RESUMED(1), FOREGROUND_SERVICE_START(19)
                    if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED || event.eventType == 1 || event.eventType == 19) {
                        
                        // 🔥 严格过滤：只有当OffTimes的UI真正在前台时，才保留事件
                        if (!AppLifecycleObserver.isActivityInForeground.value) {
                            Log.d(TAG, "🚫 过滤OffTimes后台任务: UI不在前台，忽略启动事件")
                            continue
                        }
                        
                        Log.d(TAG, "✅ OffTimes UI在前台，保留事件")
                    }
                }
                
                // 额外保护：只处理用户真正使用的应用
                if (!isUserApp(eventPackageName)) {
                    continue
                }

                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED, 1, 19 -> {
                        // 若Launcher成为前台，则结束当前会话并清空前台状态
                        if (isLauncherApp(eventPackageName)) {
                            if (currentForegroundPackage != null) {
                                val start = currentSessionStartTime ?: (event.timeStamp - 1000)
                                Log.d(TAG, "🏠 Launcher前台，结算当前会话: $currentForegroundPackage")
                                saveSession(currentForegroundPackage!!, start, event.timeStamp)
                                currentForegroundPackage = null
                                currentSessionStartTime = null
                            }
                            continue
                        }
                        // 状态机总闸保护：任何UI不在前台的OffTimes“前台事件”一律忽略
                        if (event.packageName != null &&
                            event.packageName.startsWith("com.offtime.app") &&
                            !AppLifecycleObserver.isActivityInForeground.value) {
                            Log.d(TAG, "\uD83D\uDEAB 状态机保护: 忽略OffTimes伪前台事件(type=${event.eventType})，UI不在前台")
                            continue
                        }
                        resumeCount++
                        val eventTimeStr = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(event.timeStamp)

                        // 状态机核心：处理应用切换
                        if (currentForegroundPackage != null && currentForegroundPackage != eventPackageName) {
                            // 若上一个应用仍是前台且与新事件属于不同应用，正常结算
                            if (!(eventPackageName.startsWith("com.offtime.app") && !AppLifecycleObserver.isActivityInForeground.value)) {
                                val endTs = event.timeStamp
                                // 兜底保护：如果缺少开始时间，使用最近已知时间或当前事件时间
                                if (currentSessionStartTime == null) {
                                    currentSessionStartTime = if (lastKnownTs > 0) lastKnownTs else endTs
                                    Log.w(TAG, " 9e0a 检测到缺失的会话起点，为 ${currentForegroundPackage} 使用兜底起点=${currentSessionStartTime}")
                                }
                                val startTs = currentSessionStartTime!!
                                val duration = (endTs - startTs) / 1000
                                Log.d(TAG, "📱 应用切换: ${currentForegroundPackage} -> ${eventPackageName}, 上一个会话时长=${duration}秒")
                                saveSession(currentForegroundPackage!!, startTs, endTs)
                            } else {
                                // 如果切到OffTimes但UI不在前台，视为后台干扰，不结算上一个会话，也不启动新会话
                                Log.d(TAG, "🛡️ 忽略到OffTimes的切换(后台干扰)，保持当前前台=${currentForegroundPackage}")
                                continue
                            }
                        }

                        // 开始新的会话
                        if (currentForegroundPackage != eventPackageName) {
                            Log.d(TAG, "▶️ 应用启动: ${eventPackageName} at $eventTimeStr")
                            currentForegroundPackage = eventPackageName
                            
                            // 🔥 关键修复：OffTimes的会话起点使用实际切换时间，不是进程启动时间
                            if (eventPackageName.startsWith("com.offtime.app")) {
                                // OffTimes能到达这里说明UI已在前台，使用当前时间作为会话起点（用户实际切换时间）
                                val actualSwitchTime = System.currentTimeMillis()
                                currentSessionStartTime = actualSwitchTime
                                Log.d(TAG, "🔧 OffTimes会话起点修正: 使用实际切换时间 ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(actualSwitchTime)} 而非进程启动时间 $eventTimeStr")
                            } else {
                                // 其他应用使用事件时间
                                currentSessionStartTime = event.timeStamp
                            }
                            lastForegroundSwitchTs = event.timeStamp
                        }
                    }

                    UsageEvents.Event.ACTIVITY_PAUSED, 2, 20, 23 -> {
                        // OffTimes在前台时忽略Pause，避免被后台任务打断
                        if (event.packageName != null &&
                            event.packageName.startsWith("com.offtime.app")) {
                            if (AppLifecycleObserver.isActivityInForeground.value) {
                                Log.d(TAG, "⏸️ 忽略OffTimes暂停事件: UI在前台，保持会话连续")
                                continue
                            } else {
                                // UI不在前台时，按原规则忽略(总闸)
                                Log.d(TAG, "\uD83D\uDEAB 状态机保护: 忽略OffTimes伪停止事件(type=${event.eventType})，UI不在前台")
                                continue
                            }
                        }
                        pauseCount++
                        // 状态机核心：处理应用进入后台
                        if (currentForegroundPackage == eventPackageName) {
                            val eventTimeStr = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(event.timeStamp)
                            Log.d(TAG, "⏹️ 应用停止: ${eventPackageName} at $eventTimeStr")
                            
                            // 兜底保护：如果缺少开始时间，使用最近已知时间或稍早于当前事件时间
                            if (currentSessionStartTime == null) {
                                currentSessionStartTime = if (lastKnownTs > 0) lastKnownTs else (event.timeStamp - 1000)
                                Log.w(TAG, " 9e0a 检测到缺失的会话起点，为 ${currentForegroundPackage} 使用兜底起点=${currentSessionStartTime}")
                            }
                            
                            saveSession(currentForegroundPackage!!, currentSessionStartTime!!, event.timeStamp)
                            
                            // 重置状态
                            currentForegroundPackage = null
                            currentSessionStartTime = null
                        }
                    }
                }
            }
            
            Log.d(TAG, "事件处理完成: 总计${eventCount}个事件 (启动${resumeCount}个, 结束${pauseCount}个)")
            if (currentForegroundPackage != null) {
                Log.d(TAG, "当前活跃应用: ${currentForegroundPackage}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "解析 UsageEvents 失败", e)
        }
        return lastEventTs
    }

    /**
     * 保存会话到数据库，并应用过滤规则
     */
    private suspend fun saveSession(packageName: String, startTime: Long, endTime: Long) {
        val duration = (endTime - startTime) / 1000

        // 规则 1: 忽略所有时长过短的会话
        if (duration < 1) {
            return
        }

        // 规则 2: 仅当OffTimes UI不在前台时，才过滤短会话（判为后台任务）
        if (packageName.startsWith("com.offtime.app")) {
            val minDurationForOffTimes = 10
            if (!AppLifecycleObserver.isActivityInForeground.value && duration < minDurationForOffTimes) {
                Log.d(TAG, "🚫 过滤OffTimes短会话(时长:${duration}秒)：UI不在前台，疑似后台任务")
                return
            }
        }

        // 通过所有过滤规则，保存会话
        appSessionRepository.upsertSessionSmart(packageName, startTime, endTime)
    }

    /**
     * 更可靠的前台应用获取：
     * - 回看近10秒UsageEvents，取最后一个有效RESUMED(1/19)，且未被后续PAUSED覆盖
     * - 事件与当前时间差>3秒视为过期，返回lastKnownForeground
     * - 若结果为OffTimes而UI不在前台，忽略该结果
     * - 兜底不确定时，返回lastKnownForeground，避免抖动
     */
    private fun getForegroundApp(): String? {
        val now = System.currentTimeMillis()
        val offTimesPrefix = "com.offtime.app"

        try {
            val usm = applicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val start = now - 60_000 // 增加回看时间到60秒，提高稳定性
            
            // 使用 queryUsageStats API 作为替代方案
            val usageStatsList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, now)
            
            if (usageStatsList != null && usageStatsList.isNotEmpty()) {
                // 寻找最近可见的应用
                var recentApp: android.app.usage.UsageStats? = null
                for (usageStats in usageStatsList) {
                    if (usageStats.lastTimeVisible > (recentApp?.lastTimeVisible ?: 0)) {
                        recentApp = usageStats
                    }
                }
                
                val candidate = recentApp?.packageName
                val candidateTs = recentApp?.lastTimeVisible ?: 0
                
                if (candidate != null) {
                    // 忽略Launcher/桌面
                    if (isLauncherApp(candidate)) {
                        Log.d(TAG, "getForegroundApp(new): 候选为Launcher，视为无前台")
                        return null
                    }
                    val age = now - candidateTs
                    // 放宽容忍时间到3分钟，适配部分系统上lastTimeVisible刷新不及时
                    if (age <= 180_000) {
                        if (candidate.startsWith(offTimesPrefix) && !AppLifecycleObserver.isActivityInForeground.value) {
                            Log.d(TAG, "getForegroundApp(new): 候选为OffTimes但UI不在前台，忽略，返回缓存=$lastKnownForeground")
                            return lastKnownForeground
                        }
                        lastKnownForeground = candidate
                        lastKnownTs = candidateTs
                        Log.d(TAG, "getForegroundApp(new): 即时前台=$candidate, age=${age}ms")
                        return candidate
                    } else {
                        // 若没有任何已知前台，则在兜底情况下也返回该候选，避免空值
                        if (lastKnownForeground == null) {
                            Log.d(TAG, "getForegroundApp(new): 候选较旧(age=${age}ms)但无缓存，兜底返回=$candidate")
                            lastKnownForeground = candidate
                            lastKnownTs = candidateTs
                            return candidate
                        }
                        Log.d(TAG, "getForegroundApp(new): 候选过期(age=${age}ms)，返回缓存=$lastKnownForeground")
                        return lastKnownForeground
                    }
                }
            }

            // 如果新方法失败，回退到旧的实现
            return getForegroundAppLegacy(now, offTimesPrefix)
            
        } catch (e: Exception) {
            Log.e(TAG, "获取前台应用失败", e)
            return getForegroundAppLegacy(now, offTimesPrefix)
        }
    }

    /**
     * 旧的基于 UsageEvents 的前台应用获取逻辑
     */
    private fun getForegroundAppLegacy(now: Long, offTimesPrefix: String): String? {
        try {
            val usm = applicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val start = now - 10_000
            val ue = usm.queryEvents(start, now)
            var candidate: String? = null
            var candidateTs: Long = 0
            val paused = mutableSetOf<String>()
            while (ue.hasNextEvent()) {
                val e = UsageEvents.Event()
                ue.getNextEvent(e)
                val p = e.packageName ?: continue
                if (isLauncherApp(p)) continue
                when (e.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED, 1, 19 -> {
                        if (!paused.contains(p)) {
                            candidate = p
                            candidateTs = e.timeStamp
                        }
                    }
                    UsageEvents.Event.ACTIVITY_PAUSED, 2, 20, 23 -> paused.add(p)
                }
            }

            // 结果有效性与新鲜度判断
            if (candidate != null) {
                val age = now - candidateTs
                if (age <= 3_000) {
                    if (candidate.startsWith(offTimesPrefix) && !AppLifecycleObserver.isActivityInForeground.value) {
                        Log.d(TAG, "getForegroundApp(legacy): 候选为OffTimes但UI不在前台，忽略，返回缓存=$lastKnownForeground")
                        return lastKnownForeground
                    }
                    lastKnownForeground = candidate
                    lastKnownTs = candidateTs
                    Log.d(TAG, "getForegroundApp(legacy): 即时前台=$candidate, age=${age}ms")
                    return candidate
                } else {
                    Log.d(TAG, "getForegroundApp(legacy): 候选过期(age=${age}ms)，返回缓存=$lastKnownForeground")
                    return lastKnownForeground
                }
            }

            return lastKnownForeground
        } catch (e: Exception) {
            Log.e(TAG, "获取前台应用(legacy)失败", e)
            return lastKnownForeground
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "应用使用统计",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "收集应用使用统计数据"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("正在统计应用使用时长")
            .setContentText("后台收集应用使用数据")
            .setSmallIcon(R.drawable.ic_timer)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    // 强制立即采集
    fun forceCollection() {
        if (isCollecting) {
            serviceScope.launch {
                pullEvents()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Usage stats collector service destroying...")
        
        try {
            // 先结算仍在前台的应用，避免数据丢失
            // 使用runBlocking确保在服务销毁前完成数据保存
            runBlocking {
                flushAllSessionsSync()
            }
            
            isCollecting = false
            serviceScope.cancel()
            
            Log.d(TAG, "Usage stats collector service destroyed")
            
            // 如果服务被系统杀死，尝试重新启动（保活机制）
            val restartIntent = Intent(this, UsageStatsCollectorService::class.java).apply {
                action = ACTION_START_COLLECTION
            }
            
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    startForegroundService(restartIntent)
                } else {
                    startService(restartIntent)
                }
                Log.d(TAG, "服务重启指令已发送")
            } catch (e: Exception) {
                Log.w(TAG, "无法发送服务重启指令: ${e.message}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "服务销毁过程出错", e)
        }
    }
    
    /**
     * 将仍在前台的应用全部结算到当前时间。
     */
    private fun flushAllSessions() {
        val now = System.currentTimeMillis()
        if (currentForegroundPackage == null) {
            Log.d(TAG, "No active sessions to flush")
            return
        }
        
        Log.d(TAG, "Flushing active session (async): $currentForegroundPackage")
        
        // 异步执行，用于正常停止时
        val pkg = currentForegroundPackage!!
        val start = currentSessionStartTime!!
        
            serviceScope.launch {
                try {
                    appSessionRepository.upsertSessionSmart(pkg, start, now)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to flush session for $pkg", e)
                }
            }
        currentForegroundPackage = null
        currentSessionStartTime = null
    }
    
    /**
     * 同步方式将仍在前台的应用全部结算到当前时间。
     * 用于服务销毁时确保数据及时保存
     */
    private suspend fun flushAllSessionsSync() {
        val now = System.currentTimeMillis()
        
        // 处理当前活跃会话
        if (currentForegroundPackage != null) {
            Log.d(TAG, "Flushing active session (sync): $currentForegroundPackage")
            val pkg = currentForegroundPackage!!
            val start = currentSessionStartTime!!
            
            try {
                appSessionRepository.upsertSessionSmart(pkg, start, now)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to flush session for $pkg", e)
            }
            
            currentForegroundPackage = null
            currentSessionStartTime = null
        }
        
        // 处理暂停的会话
        if (pausedSessionPackage != null && pausedSessionStartTime != null && pausedSessionPauseTime != null) {
            Log.d(TAG, "Flushing paused session (sync): $pausedSessionPackage")
            
            try {
                appSessionRepository.upsertSessionSmart(
                    pausedSessionPackage!!,
                    pausedSessionStartTime!!,
                    pausedSessionPauseTime!!
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to flush paused session for $pausedSessionPackage", e)
            }
            
            pausedSessionPackage = null
            pausedSessionStartTime = null
            pausedSessionPauseTime = null
        }
        
        if (currentForegroundPackage == null && pausedSessionPackage == null) {
            Log.d(TAG, "No active or paused sessions to flush")
        }
    }
    
    /**
     * 判断OffTimes事件是否为后台事件
     * 后台事件的特征：
     * 1. 在其他应用正在前台运行时发生的RESUMED事件
     * 2. 短暂的RESUMED后立即PAUSED的事件
     */
    private fun isOffTimesBackgroundEvent(@Suppress("UNUSED_PARAMETER") event: UsageEvents.Event): Boolean {
        // 此方法已废弃，新的逻辑直接在processUsageEvents中实现
        return false
    }
    
    /**
     * 判断是否为用户应用（非系统应用）
     */
    private fun isUserApp(packageName: String): Boolean {
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val applicationInfo = packageInfo.applicationInfo
            
            // 检查是否为用户安装的应用
            (applicationInfo?.flags?.and(ApplicationInfo.FLAG_SYSTEM) ?: 0) == 0 ||
            // 或者是系统应用但被用户更新过的（如Chrome等）
            (applicationInfo?.flags?.and(ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) ?: 0) != 0 ||
            // 或者是一些常见的用户应用
            packageName.startsWith("com.google.android.apps.maps") ||
            packageName.startsWith("com.android.chrome") ||
            packageName.startsWith("com.google.android.youtube") ||
            packageName.startsWith("com.whatsapp") ||
            packageName.startsWith("com.tencent") ||
            packageName.startsWith("com.alibaba") ||
            packageName.startsWith("com.taobao") ||
            packageName.startsWith("com.sina.weibo")
        } catch (e: Exception) {
            Log.w(TAG, "检查应用类型失败: $packageName", e)
            true // 默认认为是用户应用
        }
    }

    // 最近一次前台应用切换的时间戳（用于矫正实时统计的会话起点）
    private var lastForegroundSwitchTs: Long = 0L
    
    // 最后处理的事件时间戳（用于实时拉取避免重复）
    private var lastEventTs: Long = 0L

    private fun isLauncherApp(packageName: String?): Boolean {
        if (packageName == null) return false
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = packageManager.resolveActivity(intent, 0)
        return resolveInfo != null && packageName == resolveInfo.activityInfo.packageName
    }

} 