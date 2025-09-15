package com.offtime.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.offtime.app.MainActivity
import com.offtime.app.R
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class TaskReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        private const val CHANNEL_ID = "task_reminder_channel"
        private const val MORNING_NOTIFICATION_ID = 1001
        private const val EVENING_NOTIFICATION_ID = 1002
    }
    
    override suspend fun doWork(): Result {
        val reminderType = inputData.getString("reminder_type") ?: return Result.failure()
        val hour = inputData.getInt("hour", 8)
        val minute = inputData.getInt("minute", 0)
        
        // 检查当前时间是否匹配设定的提醒时间
        val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val currentMinute = java.util.Calendar.getInstance().get(java.util.Calendar.MINUTE)
        
        // 允许5分钟的误差
        val timeDiff = Math.abs((currentHour * 60 + currentMinute) - (hour * 60 + minute))
        if (timeDiff > 5) {
            return Result.success()
        }
        
        when (reminderType) {
            "morning" -> showMorningReminder()
            "evening" -> showEveningReminder()
        }
        
        return Result.success()
    }
    
    private fun showMorningReminder() {
        createNotificationChannel()
        
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("show_morning_reminder", true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            MORNING_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("早安！查看昨日目标完成情况")
            .setContentText("点击查看昨日目标完成度和奖罚内容")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(MORNING_NOTIFICATION_ID, notification)
    }
    
    private fun showEveningReminder() {
        createNotificationChannel()
        
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("show_evening_reminder", true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            EVENING_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("完成今日奖罚任务")
            .setContentText("点击查看并完成今日奖罚任务")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(EVENING_NOTIFICATION_ID, notification)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "任务提醒"
            val descriptionText = "OffTime任务提醒通知"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
} 