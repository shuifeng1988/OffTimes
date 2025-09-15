package com.offtime.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 小部件更新管理器
 * 用于从应用内触发小部件更新
 */
object WidgetUpdateManager {
    
    /**
     * 更新所有锁屏小部件
     */
    fun updateAllLockScreenWidgets(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, OffTimeLockScreenWidget::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                
                if (appWidgetIds.isNotEmpty()) {
                    // 发送更新广播
                    val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                        component = componentName
                    }
                    context.sendBroadcast(intent)
                    
                    android.util.Log.d("WidgetUpdateManager", "已触发${appWidgetIds.size}个锁屏小部件更新")
                } else {
                    android.util.Log.d("WidgetUpdateManager", "没有找到锁屏小部件实例")
                }
            } catch (e: Exception) {
                android.util.Log.e("WidgetUpdateManager", "更新锁屏小部件失败", e)
            }
        }
    }
    
    /**
     * 检查是否有锁屏小部件实例
     */
    fun hasLockScreenWidgets(context: Context): Boolean {
        return try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, OffTimeLockScreenWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            appWidgetIds.isNotEmpty()
        } catch (e: Exception) {
            android.util.Log.e("WidgetUpdateManager", "检查锁屏小部件失败", e)
            false
        }
    }
} 