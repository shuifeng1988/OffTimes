package com.offtime.app.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.offtime.app.R
import com.offtime.app.data.repository.TimerSessionRepository
import com.offtime.app.utils.CategoryUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class OfflineTimerService : Service() {

    @Inject
    lateinit var timerSessionRepository: TimerSessionRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var timerJob: Job? = null
    private var currentSessionId: Int = 0
    private var currentCatId: Int = 0
    private var currentCategoryName: String = ""
    private var startTime: Long = 0L
    private var currentDuration: Int = 0
    private var isPaused: Boolean = false
    private var pauseStartTime: Long = 0L
    private var totalPauseDuration: Long = 0L

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "offline_timer_channel"
        const val ACTION_START_TIMER = "start_timer"
        const val ACTION_STOP_TIMER = "stop_timer"
        const val ACTION_PAUSE_TIMER = "pause_timer"
        const val ACTION_RESUME_TIMER = "resume_timer"
        const val EXTRA_CAT_ID = "cat_id"
        const val EXTRA_CATEGORY_NAME = "category_name"
        const val EXTRA_SESSION_ID = "session_id"

        fun startTimer(context: Context, catId: Int, categoryName: String, sessionId: Int) {
            val intent = Intent(context, OfflineTimerService::class.java).apply {
                action = ACTION_START_TIMER
                putExtra(EXTRA_CAT_ID, catId)
                putExtra(EXTRA_CATEGORY_NAME, categoryName)
                putExtra(EXTRA_SESSION_ID, sessionId)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun pauseTimer(context: Context) {
            val intent = Intent(context, OfflineTimerService::class.java).apply {
                action = ACTION_PAUSE_TIMER
            }
            
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e("OfflineTimerService", "暂停计时器失败", e)
            }
        }

        fun resumeTimer(context: Context) {
            val intent = Intent(context, OfflineTimerService::class.java).apply {
                action = ACTION_RESUME_TIMER
            }
            
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e("OfflineTimerService", "恢复计时器失败", e)
            }
        }

        fun stopTimer(context: Context) {
            val intent = Intent(context, OfflineTimerService::class.java).apply {
                action = ACTION_STOP_TIMER
            }
            
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e("OfflineTimerService", "停止计时器失败", e)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TIMER -> {
                currentCatId = intent.getIntExtra(EXTRA_CAT_ID, 0)
                currentCategoryName = intent.getStringExtra(EXTRA_CATEGORY_NAME) ?: ""
                currentSessionId = intent.getIntExtra(EXTRA_SESSION_ID, 0)
                startTimer()
            }
            ACTION_PAUSE_TIMER -> {
                pauseTimer()
            }
            ACTION_RESUME_TIMER -> {
                resumeTimer()
            }
            ACTION_STOP_TIMER -> {
                stopTimer()
            }
        }
        return START_STICKY
    }

    private fun startTimer() {
        startTime = System.currentTimeMillis()
        currentDuration = 0
        isPaused = false
        totalPauseDuration = 0L
        
        // 启动前台服务
        startForeground(NOTIFICATION_ID, createNotification())

        // 启动计时任务
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive) {
                delay(1000) // 每秒更新一次，提供更精确的计时
                
                if (!isPaused) {
                    // 计算实际运行时间（扣除暂停时间）
                    val actualRunTime = System.currentTimeMillis() - startTime - totalPauseDuration
                    currentDuration = (actualRunTime / 1000).toInt()

                    // 每秒更新数据库，确保UI能实时显示
                    val success = timerSessionRepository.updateTimer(currentSessionId, currentDuration)
                    if (!success) {
                        // 跨日或其他错误，需要重新开始计时
                        restartTimerForNewDay()
                        break
                    }

                    // 每秒更新通知显示
                    updateNotification()
                }
            }
        }
    }

    private fun pauseTimer() {
        isPaused = true
        pauseStartTime = System.currentTimeMillis()
        updateNotification() // 更新通知显示暂停状态
        android.util.Log.d("OfflineTimerService", "计时器已暂停")
    }

    private fun resumeTimer() {
        if (isPaused) {
            // 累计暂停时间
            totalPauseDuration += System.currentTimeMillis() - pauseStartTime
            isPaused = false
            updateNotification() // 更新通知显示运行状态
            android.util.Log.d("OfflineTimerService", "计时器已恢复")
        }
    }

    private suspend fun restartTimerForNewDay() {
        // 为新的一天重新开始计时
        val newSession = timerSessionRepository.startTimer(currentCatId)
        if (newSession != null) {
            currentSessionId = newSession.id
            startTime = System.currentTimeMillis()
            currentDuration = 0
            isPaused = false
            totalPauseDuration = 0L
            updateNotification()
            
            // 重新启动计时循环，使用与主循环相同的逻辑
            timerJob = serviceScope.launch {
                while (isActive) {
                    delay(1000) // 每秒更新一次
                    
                    if (!isPaused) {
                        // 计算实际运行时间（扣除暂停时间）
                        val actualRunTime = System.currentTimeMillis() - startTime - totalPauseDuration
                        currentDuration = (actualRunTime / 1000).toInt()

                        // 每秒更新数据库，确保UI能实时显示
                        val success = timerSessionRepository.updateTimer(currentSessionId, currentDuration)
                        if (!success) {
                            // 跨日或其他错误，需要重新开始计时
                            restartTimerForNewDay()
                            break
                        }

                        // 每秒更新通知显示
                        updateNotification()
                    }
                }
            }
        } else {
            // 无法重新开始，停止服务
            stopSelf()
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        serviceScope.launch {
            timerSessionRepository.stopTimer(currentSessionId)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
        stopForeground(true)
        }
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "线下活动计时",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示线下活动计时进度"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val stopIntent = Intent(this, OfflineTimerService::class.java).apply {
            action = ACTION_STOP_TIMER
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val emoji = CategoryUtils.getCategoryEmoji(currentCategoryName)
        val formattedTime = formatDuration(currentDuration)
        
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)
            .setOngoing(true)
            .addAction(R.drawable.ic_stop, "停止", stopPendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        if (isPaused) {
            // 暂停状态的通知
            val resumeIntent = Intent(this, OfflineTimerService::class.java).apply {
                action = ACTION_RESUME_TIMER
            }
            val resumePendingIntent = PendingIntent.getService(
                this, 1, resumeIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            builder.setContentTitle("⏸️ $currentCategoryName 计时已暂停")
                .setContentText("已计时: $formattedTime (暂停中)")
                .addAction(R.drawable.ic_timer, "继续", resumePendingIntent)
        } else {
            // 运行状态的通知
            val pauseIntent = Intent(this, OfflineTimerService::class.java).apply {
                action = ACTION_PAUSE_TIMER
            }
            val pausePendingIntent = PendingIntent.getService(
                this, 1, pauseIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            builder.setContentTitle("$emoji $currentCategoryName 计时中")
                .setContentText("已计时: $formattedTime")
                .addAction(R.drawable.ic_stop, "暂停", pausePendingIntent)
        }

        return builder.build()
    }

    private fun updateNotification() {
        val notification = createNotification()
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun formatDuration(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format("%02d:%02d", minutes, secs)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        serviceScope.cancel()
    }
} 