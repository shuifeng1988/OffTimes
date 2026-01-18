package com.offtime.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.offtime.app.service.UsageStatsCollectorService
import com.offtime.app.utils.FirstLaunchManager
import com.offtime.app.utils.UsageStatsPermissionHelper
import com.offtime.app.util.DataUpdateEventManager
import com.offtime.app.util.AppLifecycleObserver
import com.offtime.app.BuildConfig
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ScreenStateReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ScreenStateReceiverEntryPoint {
        fun firstLaunchManager(): FirstLaunchManager
        fun dataUpdateEventManager(): DataUpdateEventManager
    }

    companion object {
        private const val TAG = "ScreenStateReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val hiltEntryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            ScreenStateReceiverEntryPoint::class.java
        )
        val firstLaunchManager = hiltEntryPoint.firstLaunchManager()
        val dataUpdateEventManager = hiltEntryPoint.dataUpdateEventManager()

        Log.d(TAG, "接收到广播: ${intent.action}")

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.d(TAG, "系统开机完成，启动数据收集服务")
                startUsageStatsCollectionIfReady(context, "开机启动", firstLaunchManager)
            }

            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Log.d(TAG, "应用更新完成，启动数据收集服务")
                startUsageStatsCollectionIfReady(context, "应用更新", firstLaunchManager)
            }

            Intent.ACTION_SCREEN_ON -> {
                Log.d(TAG, "屏幕点亮，触发数据更新")
                // 🔧 更新全局屏幕状态
                AppLifecycleObserver.onScreenOn()
                // 屏幕点亮时，确保服务运行
                startUsageStatsCollectionIfReady(context, "屏幕点亮", firstLaunchManager)
                // 统一通过UnifiedUpdateService进行数据更新
                dataUpdateEventManager.triggerScreenOnUpdate(context)
            }

            Intent.ACTION_SCREEN_OFF -> {
                Log.d(TAG, "屏幕关闭，触发数据更新并结算当前应用")
                // 🔧 更新全局屏幕状态
                AppLifecycleObserver.onScreenOff()
                // 🔧 关键：屏幕关闭时立即结算当前活跃应用，停止记录后台使用
                flushActiveSessionOnScreenOff(context)
                // 统一通过UnifiedUpdateService进行数据更新
                dataUpdateEventManager.triggerScreenOffUpdate(context)
            }

            Intent.ACTION_PACKAGE_ADDED,
            Intent.ACTION_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REMOVED -> {
                Log.d(TAG, "应用包变化，触发数据更新")
                // 统一通过UnifiedUpdateService进行数据更新
                dataUpdateEventManager.triggerManualRefreshUpdate(context)
            }
        }
    }

    /**
     * 如果条件满足则启动数据收集服务
     */
    private fun startUsageStatsCollectionIfReady(context: Context, trigger: String, firstLaunchManager: FirstLaunchManager) {
        try {
            // 在Release版本中，检查是否已完成初始化引导
            if (!BuildConfig.DEBUG && (firstLaunchManager.isFirstLaunch() || !firstLaunchManager.isOnboardingCompleted())) {
                Log.d(TAG, "$trigger: 应用尚未完成初始化，跳过服务启动")
                return
            }

            // 检查使用统计权限
            if (!UsageStatsPermissionHelper.hasUsageStatsPermission(context)) {
                Log.d(TAG, "$trigger: 缺少使用统计权限，无法启动服务")
                return
            }

            // 启动数据收集服务
            val serviceIntent = Intent(context, UsageStatsCollectorService::class.java).apply {
                action = UsageStatsCollectorService.ACTION_START_COLLECTION
            }

            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                Log.d(TAG, "$trigger: 数据收集服务启动成功")
            } catch (e: Exception) {
                Log.e(TAG, "$trigger: 启动数据收集服务失败", e)
            }

        } catch (e: Exception) {
            Log.e(TAG, "$trigger: 启动服务检查过程出错", e)
        }
    }

    /**
     * 确保服务运行并拉取事件
     */
    private fun ensureServiceRunningAndPullEvents(context: Context, firstLaunchManager: FirstLaunchManager) {
        try {
            // 首先确保服务正在运行
            startUsageStatsCollectionIfReady(context, "屏幕点亮", firstLaunchManager)

            // 然后延迟拉取事件（给服务启动时间）
            @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
            GlobalScope.launch {
                delay(1000) // 等待1秒
                pullEventsIfServiceRunning(context)
            }

        } catch (e: Exception) {
            Log.e(TAG, "确保服务运行出错", e)
        }
    }

    /**
     * 如果服务运行则拉取事件
     */
    private fun pullEventsIfServiceRunning(context: Context) {
        try {
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
                Log.e(TAG, "启动服务拉取事件失败", e)
            }

        } catch (e: Exception) {
            Log.e(TAG, "拉取事件失败", e)
        }
    }

    /**
     * 🔧 新增：屏幕关闭时立即结算当前活跃应用
     * 确保黑屏后台使用（如听歌、微信后台）不被记录
     */
    private fun flushActiveSessionOnScreenOff(context: Context) {
        try {
            Log.d(TAG, "🔧 屏幕关闭，通知服务结算当前活跃应用")

            val serviceIntent = Intent(context, UsageStatsCollectorService::class.java).apply {
                action = "FLUSH_ON_SCREEN_OFF"
            }

            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
                Log.d(TAG, "🔧 已通知服务结算活跃应用")
            } catch (e: Exception) {
                Log.e(TAG, "通知服务结算活跃应用失败", e)
            }

        } catch (e: Exception) {
            Log.e(TAG, "屏幕关闭结算失败", e)
        }
    }
} 