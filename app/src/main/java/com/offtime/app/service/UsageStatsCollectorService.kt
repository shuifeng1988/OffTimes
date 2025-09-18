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
            
            // 使用新的状态机变量
            if (currentForegroundPackage != null && currentSessionStartTime != null) {
                val packageName = currentForegroundPackage!!
                val startTime = currentSessionStartTime!!
                
                Log.d(TAG, "实时统计 → 当前活跃应用: $packageName")
                
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
                
                // 过滤明显的系统事件
                if (eventPackageName.startsWith("android") ||
                    eventPackageName.startsWith("com.android.systemui") ||
                    eventPackageName.startsWith("com.google.android.apps.nexuslauncher") ||
                    eventPackageName.startsWith("com.google.android.permissioncontroller") ||
                    eventPackageName.startsWith("com.android.vending") ||
                    eventPackageName.startsWith("com.google.android.gms")) {
                    continue
                }
                
                // 特殊处理OffTimes：只过滤后台事件，保留真正的前台使用
                if (eventPackageName.startsWith("com.offtime.app")) {
                    // 如果一个事件声称OffTimes到了前台...
                    if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED || event.eventType == 1) { // 不再检查19事件
                        // ...我们就去核实一下当前真正在前台的应用是不是它
                        val actualForegroundApp = getForegroundApp()
                        
                        // 如果实际前台应用不是OffTimes，那这个事件就是后台任务的干扰，必须忽略
                        // 增加白名单，允许从桌面启动
                        val launcherPackage = "com.google.android.apps.nexuslauncher"
                        if (actualForegroundApp != null && 
                            !actualForegroundApp.startsWith("com.offtime.app") &&
                            currentForegroundPackage != launcherPackage) {
                            Log.d(TAG, "🚫 背景任务干扰检测: UsageEvent显示OffTimes进入前台，但实际前台应用是 $actualForegroundApp。忽略此事件。")
                            continue // 核心修复：跳过这个虚假事件
                        }
                    }
                }
                
                // 额外保护：只处理用户真正使用的应用
                if (!isUserApp(eventPackageName)) {
                    continue
                }

                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED, 1, 19 -> {
                        resumeCount++
                        val eventTimeStr = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(event.timeStamp)

                        // 状态机核心：处理应用切换
                        if (currentForegroundPackage != null && currentForegroundPackage != eventPackageName) {
                            val duration = (event.timeStamp - currentSessionStartTime!!) / 1000
                            Log.d(TAG, "📱 应用切换: ${currentForegroundPackage} -> ${eventPackageName}, 上一个会话时长=${duration}秒")
                            
                            // 结束上一个会话
                            saveSession(currentForegroundPackage!!, currentSessionStartTime!!, event.timeStamp)
                        }

                        // 开始新的会话
                        if (currentForegroundPackage != eventPackageName) {
                            Log.d(TAG, "▶️ 应用启动: ${eventPackageName} at $eventTimeStr")
                            currentForegroundPackage = eventPackageName
                            currentSessionStartTime = event.timeStamp
                        }
                    }

                    UsageEvents.Event.ACTIVITY_PAUSED, 2, 20, 23 -> {
                        pauseCount++
                        // 状态机核心：处理应用进入后台
                        if (currentForegroundPackage == eventPackageName) {
                            val eventTimeStr = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(event.timeStamp)
                            Log.d(TAG, "⏹️ 应用停止: ${eventPackageName} at $eventTimeStr")
                            
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

        // 规则 2: 专门过滤OffTimes的后台任务产生的短会话
        if (packageName.startsWith("com.offtime.app")) {
            val minDurationForOffTimes = 10 // 单位：秒。时长低于此值的OffTimes会话将被忽略
            if (duration < minDurationForOffTimes) {
                Log.d(TAG, "🚫 过滤掉短暂的OffTimes会话 (时长: ${duration}秒)，疑似后台任务。")
                return // 不保存此记录
            }
        }

        // 通过所有过滤规则，保存会话
        appSessionRepository.upsertSessionSmart(packageName, startTime, endTime)
    }

    /**
     * 使用UsageStatsManager获取当前前台应用
     * 这是一个比ActivityManager.getRunningTasks更可靠的方法
     */
    private fun getForegroundApp(): String? {
        var foregroundApp: String? = null
        try {
            val usm = applicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val time = System.currentTimeMillis()
            // 查询最近1分钟的事件来找到最顶层的应用
            val appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 60 * 1000, time)
            if (appList != null && appList.isNotEmpty()) {
                val sortedMap = sortedMapOf<Long, android.app.usage.UsageStats>()
                for (usageStats in appList) {
                    sortedMap[usageStats.lastTimeUsed] = usageStats
                }
                if (sortedMap.isNotEmpty()) {
                    foregroundApp = sortedMap[sortedMap.lastKey()]?.packageName
                    Log.d(TAG, "getForegroundApp: 最近使用的应用是 $foregroundApp")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取前台应用失败", e)
        }
        return foregroundApp
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
    private fun isOffTimesBackgroundEvent(event: UsageEvents.Event): Boolean {
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

} 