package com.offtime.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import com.offtime.app.service.DataAggregationService
import com.offtime.app.service.UnifiedUpdateService
import java.util.concurrent.TimeUnit
import java.util.Calendar

@HiltWorker
class UsageStatsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "usage_stats_work"
        
        fun scheduleWork(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<UsageStatsWorker>(
                1, TimeUnit.MINUTES // 改为每1分钟执行一次，与统一更新服务配合
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE, // 使用推荐的UPDATE策略
                workRequest
            )
            
            android.util.Log.d("UsageStatsWorker", "已安排每分钟统一更新任务")
        }
    }

    override suspend fun doWork(): Result {
        return try {
            android.util.Log.d("UsageStatsWorker", "开始执行统一更新任务")
            
            // 触发统一更新服务 - 手动执行一次完整更新
            UnifiedUpdateService.triggerManualUpdate(applicationContext)
            
            android.util.Log.d("UsageStatsWorker", "统一更新任务完成")
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("UsageStatsWorker", "统一更新任务失败", e)
            Result.retry()
        }
    }
} 