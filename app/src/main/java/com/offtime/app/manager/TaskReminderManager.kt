package com.offtime.app.manager

import android.content.Context
import android.content.SharedPreferences
import androidx.work.*
import com.offtime.app.worker.TaskReminderWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class TaskReminderSettings(
    val morningReminderEnabled: Boolean = true,
    val morningReminderHour: Int = 8,
    val morningReminderMinute: Int = 0,
    val eveningReminderEnabled: Boolean = true,
    val eveningReminderHour: Int = 20,
    val eveningReminderMinute: Int = 0
)

@Singleton
class TaskReminderManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("task_reminder_prefs", Context.MODE_PRIVATE)
    
    private val workManager = WorkManager.getInstance(context)
    
    companion object {
        private const val PREF_MORNING_ENABLED = "morning_reminder_enabled"
        private const val PREF_MORNING_HOUR = "morning_reminder_hour"
        private const val PREF_MORNING_MINUTE = "morning_reminder_minute"
        private const val PREF_EVENING_ENABLED = "evening_reminder_enabled"
        private const val PREF_EVENING_HOUR = "evening_reminder_hour"
        private const val PREF_EVENING_MINUTE = "evening_reminder_minute"
        
        private const val MORNING_REMINDER_WORK_NAME = "morning_reminder_work"
        private const val EVENING_REMINDER_WORK_NAME = "evening_reminder_work"
    }
    
    suspend fun getSettings(): TaskReminderSettings = withContext(Dispatchers.IO) {
        TaskReminderSettings(
            morningReminderEnabled = sharedPreferences.getBoolean(PREF_MORNING_ENABLED, true),
            morningReminderHour = sharedPreferences.getInt(PREF_MORNING_HOUR, 8),
            morningReminderMinute = sharedPreferences.getInt(PREF_MORNING_MINUTE, 0),
            eveningReminderEnabled = sharedPreferences.getBoolean(PREF_EVENING_ENABLED, true),
            eveningReminderHour = sharedPreferences.getInt(PREF_EVENING_HOUR, 20),
            eveningReminderMinute = sharedPreferences.getInt(PREF_EVENING_MINUTE, 0)
        )
    }
    
    suspend fun updateMorningReminderEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        sharedPreferences.edit().putBoolean(PREF_MORNING_ENABLED, enabled).apply()
    }
    
    suspend fun updateMorningReminderTime(hour: Int, minute: Int) = withContext(Dispatchers.IO) {
        sharedPreferences.edit()
            .putInt(PREF_MORNING_HOUR, hour)
            .putInt(PREF_MORNING_MINUTE, minute)
            .apply()
    }
    
    suspend fun updateEveningReminderEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        sharedPreferences.edit().putBoolean(PREF_EVENING_ENABLED, enabled).apply()
    }
    
    suspend fun updateEveningReminderTime(hour: Int, minute: Int) = withContext(Dispatchers.IO) {
        sharedPreferences.edit()
            .putInt(PREF_EVENING_HOUR, hour)
            .putInt(PREF_EVENING_MINUTE, minute)
            .apply()
    }
    
    fun scheduleMorningReminder() {
        val settings = runCatching { 
            sharedPreferences.run {
                TaskReminderSettings(
                    morningReminderEnabled = getBoolean(PREF_MORNING_ENABLED, true),
                    morningReminderHour = getInt(PREF_MORNING_HOUR, 8),
                    morningReminderMinute = getInt(PREF_MORNING_MINUTE, 0),
                    eveningReminderEnabled = getBoolean(PREF_EVENING_ENABLED, true),
                    eveningReminderHour = getInt(PREF_EVENING_HOUR, 20),
                    eveningReminderMinute = getInt(PREF_EVENING_MINUTE, 0)
                )
            }
        }.getOrNull() ?: return
        
        if (!settings.morningReminderEnabled) return
        
        val workRequest = PeriodicWorkRequestBuilder<TaskReminderWorker>(1, TimeUnit.DAYS)
            .setInputData(
                Data.Builder()
                    .putString("reminder_type", "morning")
                    .putInt("hour", settings.morningReminderHour)
                    .putInt("minute", settings.morningReminderMinute)
                    .build()
            )
            .setInitialDelay(calculateInitialDelay(settings.morningReminderHour, settings.morningReminderMinute), TimeUnit.MILLISECONDS)
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            MORNING_REMINDER_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }
    
    fun scheduleEveningReminder() {
        val settings = runCatching { 
            sharedPreferences.run {
                TaskReminderSettings(
                    morningReminderEnabled = getBoolean(PREF_MORNING_ENABLED, true),
                    morningReminderHour = getInt(PREF_MORNING_HOUR, 8),
                    morningReminderMinute = getInt(PREF_MORNING_MINUTE, 0),
                    eveningReminderEnabled = getBoolean(PREF_EVENING_ENABLED, true),
                    eveningReminderHour = getInt(PREF_EVENING_HOUR, 20),
                    eveningReminderMinute = getInt(PREF_EVENING_MINUTE, 0)
                )
            }
        }.getOrNull() ?: return
        
        if (!settings.eveningReminderEnabled) return
        
        val workRequest = PeriodicWorkRequestBuilder<TaskReminderWorker>(1, TimeUnit.DAYS)
            .setInputData(
                Data.Builder()
                    .putString("reminder_type", "evening")
                    .putInt("hour", settings.eveningReminderHour)
                    .putInt("minute", settings.eveningReminderMinute)
                    .build()
            )
            .setInitialDelay(calculateInitialDelay(settings.eveningReminderHour, settings.eveningReminderMinute), TimeUnit.MILLISECONDS)
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            EVENING_REMINDER_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }
    
    fun cancelMorningReminder() {
        workManager.cancelUniqueWork(MORNING_REMINDER_WORK_NAME)
    }
    
    fun cancelEveningReminder() {
        workManager.cancelUniqueWork(EVENING_REMINDER_WORK_NAME)
    }
    
    private fun calculateInitialDelay(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            
            // 如果目标时间已过，设置为明天
            if (before(now)) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        
        return target.timeInMillis - now.timeInMillis
    }
} 