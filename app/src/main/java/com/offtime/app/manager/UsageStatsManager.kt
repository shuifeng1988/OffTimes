package com.offtime.app.manager

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.work.*
import com.offtime.app.receiver.ScreenStateReceiver
import com.offtime.app.service.UsageStatsCollectorService
import com.offtime.app.utils.UsageStatsPermissionHelper
import com.offtime.app.worker.UsageStatsWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageStatsManager @Inject constructor(
    private val context: Context
) {
    private var screenStateReceiver: ScreenStateReceiver? = null
    private var isReceiverRegistered = false
    
    fun startUsageStatsCollection() {
        if (!UsageStatsPermissionHelper.hasUsageStatsPermission(context)) {
            android.util.Log.w("UsageStatsManager", "缺少使用统计权限，无法启动数据收集")
            return
        }
        
        android.util.Log.d("UsageStatsManager", "开始启动使用统计收集服务")
        
        // 启动前台服务 - 使用更加安全的方式
        val serviceIntent = Intent(context, UsageStatsCollectorService::class.java).apply {
            action = UsageStatsCollectorService.ACTION_START_COLLECTION
        }
        
        var serviceStarted = false
        
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                // Android 8.0+ 使用前台服务
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            serviceStarted = true
            android.util.Log.d("UsageStatsManager", "使用统计收集服务启动成功")
        } catch (e: Exception) {
            android.util.Log.e("UsageStatsManager", "启动前台服务失败", e)
        }
        
        // 如果前台服务启动失败，尝试备用方案
        if (!serviceStarted) {
            android.util.Log.w("UsageStatsManager", "前台服务启动失败，使用WorkManager作为备用方案")
            schedulePeriodicWork()
        } else {
            // 启动定期工作任务作为备用保障
            schedulePeriodicWork()
        }
        
        // 注册屏幕状态广播接收器
        registerScreenStateReceiver()
    }
    
    fun stopUsageStatsCollection() {
        // 停止前台服务
        val serviceIntent = Intent(context, UsageStatsCollectorService::class.java).apply {
            action = UsageStatsCollectorService.ACTION_STOP_COLLECTION
        }
        
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            android.util.Log.e("UsageStatsManager", "停止服务失败", e)
        }
        
        // 取消注册广播接收器
        unregisterScreenStateReceiver()
        
        // 取消定期工作任务
        cancelPeriodicWork()
    }
    
    fun pullEventsNow() {
        if (!UsageStatsPermissionHelper.hasUsageStatsPermission(context)) {
            return
        }
        
        val serviceIntent = Intent(context, UsageStatsCollectorService::class.java).apply {
            action = UsageStatsCollectorService.ACTION_PULL_EVENTS
        }
        
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            // 如果服务启动失败，使用WorkManager立即执行一次工作
            android.util.Log.w("UsageStatsManager", "Failed to start service for pull events, using WorkManager", e)
            val workRequest = OneTimeWorkRequestBuilder<UsageStatsWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }
    
    private fun registerScreenStateReceiver() {
        if (isReceiverRegistered) return
        
        try {
            screenStateReceiver = ScreenStateReceiver()
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
            context.registerReceiver(screenStateReceiver, filter)
            isReceiverRegistered = true
        } catch (e: Exception) {
            // 忽略注册失败
        }
    }
    
    private fun unregisterScreenStateReceiver() {
        if (!isReceiverRegistered) return
        
        try {
            screenStateReceiver?.let { receiver ->
                context.unregisterReceiver(receiver)
            }
            screenStateReceiver = null
            isReceiverRegistered = false
        } catch (e: Exception) {
            // 忽略取消注册失败
        }
    }
    
    private fun schedulePeriodicWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(false)
            .setRequiresCharging(false)
            .setRequiresDeviceIdle(false)
            .setRequiresStorageNotLow(false)
            .build()
        
        val workRequest = PeriodicWorkRequestBuilder<UsageStatsWorker>(
            30, TimeUnit.SECONDS,
            15, TimeUnit.SECONDS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "usage_stats_collection",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
    
    private fun cancelPeriodicWork() {
        WorkManager.getInstance(context).cancelUniqueWork("usage_stats_collection")
    }
} 