package com.offtime.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.content.Context
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.offtime.app.R
import com.offtime.app.data.repository.AppSessionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

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
    
    // 标识服务是否正在收集数据
    private var isCollecting = false
    
    /**
     * 当前活跃应用映射表
     * 
     * 在整个Service生命周期内保存仍在前台运行、尚未收到ACTIVITY_PAUSED的应用
     * - key: 应用包名(packageName)
     * - value: 应用启动时间戳(startTime)
     * 
     * 用途：
     * - 匹配ACTIVITY_RESUMED和ACTIVITY_PAUSED事件
     * - 计算应用的实际使用时长
     * - 处理服务重启时的状态恢复
     */
    private val currentActive: MutableMap<String, Long> = mutableMapOf()
    
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
            
            Log.d(TAG, "数据收集服务已启动，等待UnifiedUpdateService的统一调度")
            // 不再启动独立的定时循环，改为等待UnifiedUpdateService的调用
        }
    }
    
    /**
     * 实时更新当前活跃应用的使用时长
     * 由UnifiedUpdateService按1分钟间隔调用
     */
    suspend fun updateActiveAppsDuration() {
        try {
            val currentTime = System.currentTimeMillis()
            
            if (currentActive.isNotEmpty()) {
                Log.d(TAG, "实时统计 → 当前活跃应用: ${currentActive.keys.joinToString(", ")}")
                
                // 为每个当前活跃的应用更新使用时长
                currentActive.forEach { (packageName, startTime) ->
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
                }
            } else {
                Log.d(TAG, "实时统计 → 当前无活跃应用")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "实时统计更新失败", e)
        }
    }
    
    /**
     * 恢复活跃应用状态（服务重启时）
     */
    private fun restoreActiveApps() {
        serviceScope.launch {
            try {
                val now = System.currentTimeMillis()
                val lookbackTime = now - 300_000 // 回看5分钟
                
                Log.d(TAG, "尝试恢复活跃应用状态")
                
                val usageEvents = usageStatsManager.queryEvents(lookbackTime, now)
                val tempActive = mutableMapOf<String, Long>()
                
                // 收集最近的RESUMED和PAUSED事件
                while (usageEvents.hasNextEvent()) {
                    val event = UsageEvents.Event()
                    usageEvents.getNextEvent(event)
                    
                    // 使用与主事件处理相同的逻辑
                    val isForegroundEvent = when (event.eventType) {
                        UsageEvents.Event.ACTIVITY_RESUMED,  // type=1
                        1 -> true  // ACTIVITY_RESUMED/MOVE_TO_FOREGROUND
                        19 -> true  // ACTIVITY_RESUMED (某些设备)
                        12 -> true  // USER_INTERACTION
                        else -> false
                    }
                    
                    val isBackgroundEvent = when (event.eventType) {
                        UsageEvents.Event.ACTIVITY_PAUSED,   // type=2
                        2 -> true  // ACTIVITY_PAUSED/MOVE_TO_BACKGROUND
                        20 -> true  // ACTIVITY_PAUSED (某些设备)
                        23 -> true  // ACTIVITY_STOPPED
                        else -> false
                    }
                    
                    when {
                        isForegroundEvent -> {
                            tempActive[event.packageName] = event.timeStamp
                        }
                        isBackgroundEvent -> {
                            tempActive.remove(event.packageName)
                        }
                    }
                }
                
                // 将仍然活跃的应用添加到currentActive
                if (tempActive.isNotEmpty()) {
                    currentActive.putAll(tempActive)
                    Log.d(TAG, "恢复了${tempActive.size}个活跃应用: ${tempActive.keys.joinToString(", ")}")
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
            Log.d(TAG, "开始处理事件: ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(beginTime)} - ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(endTime)}")

            // Samsung设备特殊检查
            val isSamsung = android.os.Build.MANUFACTURER.equals("samsung", ignoreCase = true)
            if (isSamsung) {
                Log.d(TAG, "检测到Samsung设备，启用兼容性处理")
            }

            while (usageEvents.hasNextEvent()) {
                val event = UsageEvents.Event()
                usageEvents.getNextEvent(event)
                lastEventTs = event.timeStamp
                eventCount++

                // Samsung设备的Chrome包名可能不同
                val isChrome = event.packageName.contains("chrome", ignoreCase = true) || 
                              event.packageName.contains("browser", ignoreCase = true)
                
                if (isChrome) {
                    val eventTime = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(event.timeStamp)
                    Log.d(TAG, "🔍 检测到浏览器事件: ${event.packageName}, 类型=${event.eventType}, 时间=$eventTime")
                }

                // 判断事件是否表示前台状态
                val isForegroundEvent = when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED,  // type=1, 标准前台事件
                    1 -> true  // ACTIVITY_RESUMED/MOVE_TO_FOREGROUND
                    19 -> true  // ACTIVITY_RESUMED (某些设备)
                    12 -> true  // USER_INTERACTION
                    else -> false
                }
                
                // 判断事件是否表示后台状态  
                val isBackgroundEvent = when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_PAUSED,   // type=2, 标准后台事件
                    2 -> true  // ACTIVITY_PAUSED/MOVE_TO_BACKGROUND
                    20 -> true  // ACTIVITY_PAUSED (某些设备)
                    23 -> true  // ACTIVITY_STOPPED
                    else -> false
                }
                
                when {
                    isForegroundEvent -> {
                        // 应用进入前台（只统计前台亮屏使用时间）
                        resumeCount++
                        val eventTime = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(event.timeStamp)
                        
                        // 特殊标记OffTimes应用和Chrome浏览器
                        val isOffTimes = event.packageName.contains("offtime") || event.packageName.contains("com.offtime")
                        val isChromeBrowser = event.packageName.contains("chrome", ignoreCase = true) || event.packageName.contains("browser", ignoreCase = true)
                        val logPrefix = when {
                            isOffTimes -> "🔧 OffTimes"
                            isChromeBrowser -> "🔍 Chrome"
                            else -> "📱"
                        }
                        
                        // 检查是否已经在活跃状态（处理应用切换场景）
                        val existingStartTime = currentActive[event.packageName]
                        if (existingStartTime != null) {
                            // 应用已经在前台，可能是切换回来
                            val gap = (event.timeStamp - existingStartTime) / 1000
                            if (gap > 30) {
                                // 如果间隔超过30秒，先保存之前的会话
                                Log.d(TAG, "$logPrefix 检测到应用切换回前台，先保存之前的会话")
                                // 这里使用当前时间作为结束时间可能不准确，但是是最佳估计
                                val estimatedEndTime = event.timeStamp - 5000 // 估计5秒前离开
                                appSessionRepository.upsertSessionSmart(
                                    event.packageName,
                                    existingStartTime,
                                    estimatedEndTime
                                )
                            }
                        }
                        
                        Log.d(TAG, "$logPrefix 应用启动 (type=${event.eventType}) → ${event.packageName} at $eventTime")
                        currentActive[event.packageName] = event.timeStamp
                    }
                    
                    isBackgroundEvent -> {
                        // 应用离开前台（结束统计）
                        pauseCount++
                        val startTime = currentActive[event.packageName]
                        val eventTime = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(event.timeStamp)
                        
                        if (startTime != null) {
                            val duration = (event.timeStamp - startTime) / 1000
                            val startTimeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(startTime)
                            
                            // 特殊标记OffTimes应用
                            val isOffTimes = event.packageName.contains("offtime") || event.packageName.contains("com.offtime")
                            val logPrefix = if (isOffTimes) "🔧 OffTimes" else "📱"
                            
                            // 对于OffTimes应用，定期保存会话但不一定结束
                            if (isOffTimes) {
                                // 始终保存当前会话状态（用于周期性更新）
                                if (duration > 0) {
                                    appSessionRepository.upsertSessionSmart(
                                        event.packageName,
                                        startTime,
                                        event.timeStamp
                                    )
                                    Log.d(TAG, "$logPrefix 周期性保存会话: ${duration}秒")
                                }
                                
                                // 对于OffTimes，总是保持活跃状态，除非有其他应用启动
                                // 这样可以确保连续使用时不会被分割
                                Log.d(TAG, "$logPrefix OffTimes暂停事件，保持活跃并更新开始时间")
                                // 更新开始时间为当前时间，以便继续记录后续使用
                                currentActive[event.packageName] = event.timeStamp
                            } else {
                                // 非OffTimes应用，正常处理
                                Log.d(TAG, "$logPrefix 应用结束 (type=${event.eventType}) → ${event.packageName}, ${startTimeStr}-${eventTime}, 时长=${duration}s")
                                
                                // 移除活跃状态
                                currentActive.remove(event.packageName)
                                
                                // 保存会话（即使很短也要记录，让Repository层决定是否过滤）
                                if (duration > 0) {
                                    // 只保存前台使用时间（排除后台和黑屏时间）
                                    appSessionRepository.upsertSessionSmart(
                                        event.packageName,
                                        startTime,
                                        event.timeStamp
                                    )
                                }
                            }
                        } else {
                            val isOffTimes = event.packageName.contains("offtime") || event.packageName.contains("com.offtime")
                            val isChromeBrowser = event.packageName.contains("chrome", ignoreCase = true) || event.packageName.contains("browser", ignoreCase = true)
                            val logPrefix = when {
                                isOffTimes -> "🔧 OffTimes"
                                isChromeBrowser -> "🔍 Chrome"
                                else -> "📱"
                            }
                            Log.w(TAG, "$logPrefix 应用结束但无启动记录 (type=${event.eventType}) → ${event.packageName} at $eventTime")
                        }
                    }
                    
                    else -> {
                        // 未知事件类型的处理
                        val eventTime = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(event.timeStamp)
                        val isOffTimes = event.packageName.contains("offtime") || event.packageName.contains("com.offtime")
                        
                        if (isOffTimes) {
                            // OffTimes应用的未知事件详细记录
                            android.util.Log.w(TAG, "🔍 OffTimes未知事件类型: type=${event.eventType}, package=${event.packageName}, time=$eventTime")
                            android.util.Log.w(TAG, "   已处理类型: 前台事件(1,19,12), 后台事件(2,20,23)")
                        } else {
                            // 其他应用的未知事件简单记录
                            android.util.Log.v(TAG, "其他应用事件: type=${event.eventType}, package=${event.packageName}")
                        }
                    }
                }
            }
            
            Log.d(TAG, "事件处理完成: 总计${eventCount}个事件 (启动${resumeCount}个, 结束${pauseCount}个)")
            if (currentActive.isNotEmpty()) {
                Log.d(TAG, "当前活跃应用: ${currentActive.keys.joinToString(", ")}")
            }
            
            // Samsung设备额外检查
            if (isSamsung && eventCount == 0) {
                Log.w(TAG, "Samsung设备未获取到任何事件，可能需要额外权限设置")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "解析 UsageEvents 失败", e)
        }
        return lastEventTs
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
        if (currentActive.isEmpty()) {
            Log.d(TAG, "No active sessions to flush")
            return
        }
        
        Log.d(TAG, "Flushing ${currentActive.size} active sessions (async)")
        
        // 异步执行，用于正常停止时
        currentActive.forEach { (pkg, start) ->
            val duration = (now - start) / 1000
            Log.d(TAG, "Flush session (async) → $pkg, ${duration}s")
            serviceScope.launch {
                try {
                    appSessionRepository.upsertSessionSmart(pkg, start, now)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to flush session for $pkg", e)
                }
            }
        }
        currentActive.clear()
    }
    
    /**
     * 同步方式将仍在前台的应用全部结算到当前时间。
     * 用于服务销毁时确保数据及时保存
     */
    private suspend fun flushAllSessionsSync() {
        val now = System.currentTimeMillis()
        if (currentActive.isEmpty()) {
            Log.d(TAG, "No active sessions to flush")
            return
        }
        
        Log.d(TAG, "Flushing ${currentActive.size} active sessions (sync)")
        
        // 同步执行，用于服务销毁时
        currentActive.forEach { (pkg, start) ->
            val duration = (now - start) / 1000
            Log.d(TAG, "Flush session (sync) → $pkg, ${duration}s")
            try {
                appSessionRepository.upsertSessionSmart(pkg, start, now)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to flush session for $pkg", e)
            }
        }
        currentActive.clear()
    }
    

} 