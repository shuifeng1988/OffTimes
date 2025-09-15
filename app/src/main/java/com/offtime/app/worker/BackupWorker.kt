package com.offtime.app.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.offtime.app.manager.BackupManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * 用户数据自动备份Worker
 * 
 * 负责在指定时间执行增量数据备份任务
 * 通过WorkManager调度，支持延时执行和重试机制
 */
@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val backupManager: BackupManager
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        private const val TAG = "BackupWorker"
        const val WORK_NAME = "user_data_backup"
        const val INPUT_SCHEDULED_HOUR = "scheduled_hour"
        const val INPUT_SCHEDULED_MINUTE = "scheduled_minute"
        private const val NOTIFICATION_CHANNEL_ID = "backup_channel"
        private const val BACKUP_NOTIFICATION_ID = 2001
    }
    
    override suspend fun doWork(): Result {
        return try {
            Log.i(TAG, "开始执行自动数据备份任务")
            
            // 获取调度的备份时间
            val scheduledHour = inputData.getInt(INPUT_SCHEDULED_HOUR, -1)
            val scheduledMinute = inputData.getInt(INPUT_SCHEDULED_MINUTE, -1)
            
            // 验证时间参数
            if (scheduledHour == -1 || scheduledMinute == -1) {
                Log.w(TAG, "备份时间参数无效: hour=$scheduledHour, minute=$scheduledMinute")
                return Result.failure()
            }
            
            // 检查当前时间是否接近调度时间（允许30分钟误差）
            val currentTime = java.util.Calendar.getInstance()
            val currentHour = currentTime.get(java.util.Calendar.HOUR_OF_DAY)
            val currentMinute = currentTime.get(java.util.Calendar.MINUTE)
            
            val scheduledTimeMinutes = scheduledHour * 60 + scheduledMinute
            val currentTimeMinutes = currentHour * 60 + currentMinute
            val timeDiff = Math.abs(currentTimeMinutes - scheduledTimeMinutes)
            
            // 如果时间差超过30分钟，跳过本次执行
            if (timeDiff > 30) {
                Log.i(TAG, "当前时间 $currentHour:$currentMinute 与调度时间 $scheduledHour:$scheduledMinute 相差过大（${timeDiff}分钟），跳过备份")
                return Result.success()
            }
            
            Log.i(TAG, "开始执行增量数据备份，调度时间: $scheduledHour:$scheduledMinute，当前时间: $currentHour:$currentMinute")
            
            // 执行增量备份
            val backupResult = backupManager.performIncrementalBackup()
            
            if (backupResult.success) {
                Log.i(TAG, "自动备份完成: ${backupResult.message}")
                Log.i(TAG, "备份详情: 成功表=${backupResult.backupedTables.size}, 总记录=${backupResult.totalRecords}")
                
                // 发送备份成功通知（可选）
                sendBackupNotification(
                    title = "数据备份完成",
                    message = "已备份 ${backupResult.backupedTables.size} 个数据表，共 ${backupResult.totalRecords} 条记录",
                    isSuccess = true
                )
                
                Result.success()
            } else {
                Log.e(TAG, "自动备份失败: ${backupResult.message}")
                Log.e(TAG, "错误详情: ${backupResult.errorDetails}")
                
                // 发送备份失败通知
                sendBackupNotification(
                    title = "数据备份失败",
                    message = backupResult.message,
                    isSuccess = false
                )
                
                // 根据错误类型决定重试策略
                when {
                    backupResult.message.contains("网络") -> {
                        Log.i(TAG, "网络问题导致备份失败，将重试")
                        Result.retry()
                    }
                    backupResult.message.contains("登录") -> {
                        Log.i(TAG, "登录问题导致备份失败，不重试")
                        Result.success() // 不重试，等待用户重新登录
                    }
                    else -> {
                        Log.i(TAG, "其他原因导致备份失败，将重试")
                        Result.retry()
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "执行备份任务时发生异常", e)
            
            // 发送异常通知
            sendBackupNotification(
                title = "备份任务异常",
                message = "备份过程中发生错误: ${e.message}",
                isSuccess = false
            )
            
            // 异常情况下重试
            Result.retry()
        }
    }
    
    /**
     * 发送备份结果通知
     */
    private fun sendBackupNotification(title: String, message: String, isSuccess: Boolean) {
        try {
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            
            // 创建通知渠道（Android 8.0+）
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "数据备份",
                    android.app.NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "数据备份状态通知"
                    enableVibration(false)
                    setSound(null, null)
                }
                notificationManager.createNotificationChannel(channel)
            }
            
            // 创建点击跳转Intent
            val intent = android.content.Intent(applicationContext, com.offtime.app.MainActivity::class.java)
            val pendingIntent = android.app.PendingIntent.getActivity(
                applicationContext,
                0,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            
            // 构建通知
            val notification = androidx.core.app.NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(if (isSuccess) android.R.drawable.ic_dialog_info else android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
            
            notificationManager.notify(BACKUP_NOTIFICATION_ID, notification)
            
        } catch (e: Exception) {
            Log.e(TAG, "发送备份通知失败", e)
            // 通知发送失败不影响备份结果
        }
    }
} 