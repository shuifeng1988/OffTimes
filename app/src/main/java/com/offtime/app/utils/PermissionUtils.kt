package com.offtime.app.utils

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.os.PowerManager
import android.provider.Settings
import android.net.Uri
import android.content.ComponentName
import androidx.core.content.ContextCompat

/**
 * 权限管理工具类
 * 
 * 提供智能的权限设置页面跳转功能，支持多种Android设备厂商的特定实现。
 * 
 * 主要功能：
 * - 使用情况访问权限管理
 * - 读取手机应用列表权限管理  
 * - 通知权限管理
 * - 开机自启动权限管理
 * - 后台运行权限管理
 * 
 * 支持的厂商：
 * - 小米/红米 (MIUI)
 * - 华为/荣耀 (EMUI/HarmonyOS)
 * - OPPO (ColorOS)
 * - vivo/iQOO
 * - 三星 (OneUI)
 * - 一加 (OxygenOS)
 * - 魅族 (Flyme)
 * - 通用Android系统
 * 
 * @author OffTime Team
 * @version 2.0
 * @since 1.0
 */
object PermissionUtils {
    
    private const val TAG = "PermissionUtils"
    
    /**
     * 检查是否有使用情况访问权限
     * 
     * @param context 应用上下文
     * @return true表示有权限，false表示无权限
     */
    fun hasUsageStatsPermission(context: Context): Boolean {
        return try {
            val packageManager = context.packageManager
            val applicationInfo = packageManager.getApplicationInfo(context.packageName, 0)
            val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
            @Suppress("DEPRECATION")
            val mode = appOpsManager.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                applicationInfo.uid,
                context.packageName
            )
            mode == android.app.AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            android.util.Log.e(TAG, "检查使用情况访问权限失败: ${e.message}")
            false
        }
    }
    
    /**
     * 检查是否有查询所有应用包权限
     * 
     * @param context 应用上下文
     * @return true表示有权限，false表示无权限
     */
    fun hasQueryAllPackagesPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.QUERY_ALL_PACKAGES
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * 跳转到应用详情设置页面
     * 
     * @param context 应用上下文
     */
    fun openAppDetailsSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            android.util.Log.d(TAG, "已跳转到应用详情设置页面")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "无法打开应用详情设置页面: ${e.message}")
        }
    }
    
    /**
     * 智能跳转到开机自启动设置页面
     * 
     * 采用多层级尝试策略：
     * 1. 直接定位到应用的自启动设置
     * 2. 通用的自启动管理页面
     * 3. 厂商特定的自启动管理
     * 4. 应用详情页面（兜底）
     * 
     * @param context 应用上下文
     */
    fun openAutoStartSettingsIntelligent(context: Context) {
        val packageName = context.packageName
        val manufacturer = Build.MANUFACTURER.lowercase()
        
        android.util.Log.d(TAG, "开机自启动 - 设备厂商: $manufacturer, 包名: $packageName")
        
        // 第一层：直接定位方法
        val directIntents = listOf(
            Intent().apply {
                action = "android.settings.APPLICATION_DETAILS_SETTINGS"
                data = Uri.fromParts("package", packageName, null)
                putExtra("auto_start", true)
                putExtra("autostart", true)
                putExtra("startup", true)
            },
            Intent().apply {
                action = "android.settings.AUTO_START_SETTINGS"
                putExtra("package_name", packageName)
            }
        )
        
        // 尝试直接方法
        for (intent in directIntents) {
            if (tryStartActivity(context, intent, "直接定位自启动设置")) {
                return
            }
        }
        
        // 第二层：通用自启动Intent
        val genericIntents = listOf(
            Intent("android.settings.AUTO_START_SETTINGS"),
            Intent("android.settings.AUTOSTART_SETTINGS"),
            Intent("android.settings.STARTUP_APP_SETTINGS")
        )
        
        for (intent in genericIntents) {
            if (tryStartActivity(context, intent, "通用自启动设置")) {
                return
            }
        }
        
        // 第三层：厂商特定方法
        openAutoStartSettings(context)
    }
    
    /**
     * 智能跳转到读取手机应用列表权限设置页面
     * 
     * @param context 应用上下文
     */
    fun openQueryAllPackagesSettingsIntelligent(context: Context) {
        val packageName = context.packageName
        
        android.util.Log.d(TAG, "读取应用列表权限 - 包名: $packageName")
        
        // 直接定位方法
        val directIntents = listOf(
            Intent().apply {
                action = "android.settings.APPLICATION_DETAILS_SETTINGS"
                data = Uri.fromParts("package", packageName, null)
                putExtra("special_access", true)
            },
            Intent().apply {
                action = "android.settings.SPECIAL_APPLICATION_ACCESS_SETTINGS"
                putExtra("package_name", packageName)
            },
            Intent().apply {
                action = Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS
                putExtra("package_name", packageName)
            }
        )
        
        // 尝试直接方法
        for (intent in directIntents) {
            if (tryStartActivity(context, intent, "直接定位应用列表权限")) {
                return
            }
        }
        
        // 通用方法
        val genericIntents = listOf(
            Intent("android.settings.SPECIAL_APPLICATION_ACCESS_SETTINGS"),
            Intent("android.settings.MANAGE_ALL_APPLICATIONS_SETTINGS")
        )
        
        for (intent in genericIntents) {
            if (tryStartActivity(context, intent, "通用应用列表权限")) {
                return
            }
        }
        
        // 兜底方案
        openQueryAllPackagesSettings(context)
    }
    
    /**
     * 智能跳转到通知权限设置页面
     * 
     * @param context 应用上下文
     */
    fun openNotificationSettingsIntelligent(context: Context) {
        val packageName = context.packageName
        
        android.util.Log.d(TAG, "通知权限 - 包名: $packageName")
        
        // Android 8.0+ 的应用通知设置
        val notificationIntent = Intent().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            } else {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = Uri.fromParts("package", packageName, null)
            }
        }
        
        if (tryStartActivity(context, notificationIntent, "通知权限设置")) {
            return
        }
        
        // 兜底方案
        openNotificationSettings(context)
    }
    
    /**
     * 智能跳转到后台运行权限设置页面
     * 
     * 主要针对电池优化相关设置
     * 
     * @param context 应用上下文
     */
    fun openBackgroundRunSettingsIntelligent(context: Context) {
        val packageName = context.packageName
        val manufacturer = Build.MANUFACTURER.lowercase()
        
        android.util.Log.d(TAG, "后台运行权限 - 设备厂商: $manufacturer, 包名: $packageName")
        
        // 方法1：最直接的电池优化请求
        val batteryOptimizationIntent = Intent().apply {
            action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
            data = Uri.parse("package:$packageName")
        }
        
        if (tryStartActivity(context, batteryOptimizationIntent, "电池优化请求")) {
            return
        }
        
        // 方法2：电池优化设置列表
        val batterySettingsIntent = Intent().apply {
            action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
        }
        
        if (tryStartActivity(context, batterySettingsIntent, "电池优化设置列表")) {
            return
        }
        
        // 方法3：应用详情页面
        val appDetailsIntent = Intent().apply {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = Uri.fromParts("package", packageName, null)
        }
        
        if (tryStartActivity(context, appDetailsIntent, "应用详情页面")) {
            return
        }
        
        // 方法4：厂商特定的电池管理页面
        val manufacturerIntent = when {
            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") -> {
                Intent().apply {
                    component = ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.powercenter.PowerSettings"
                    )
                }
            }
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> {
                Intent().apply {
                    component = ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.power.ui.HwPowerManagerActivity"
                    )
                }
            }
            manufacturer.contains("samsung") -> {
                Intent().apply {
                    component = ComponentName(
                        "com.samsung.android.sm",
                        "com.samsung.android.sm.ui.battery.BatteryActivity"
                    )
                }
            }
            else -> null
        }
        
        if (manufacturerIntent != null && tryStartActivity(context, manufacturerIntent, "厂商特定电池管理")) {
            return
        }
        
        // 最后的兜底方案
        android.util.Log.w(TAG, "所有方法都失败，使用原有的后台运行权限方法")
        openBackgroundRunSettings(context)
    }
    
    /**
     * 尝试启动Activity的通用方法
     * 
     * @param context 应用上下文
     * @param intent 要启动的Intent
     * @param description 描述信息，用于日志记录
     * @return true表示成功启动，false表示失败
     */
    private fun tryStartActivity(context: Context, intent: Intent, description: String): Boolean {
        return try {
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            val resolveInfo = context.packageManager.resolveActivity(intent, 0)
            if (resolveInfo != null) {
                context.startActivity(intent)
                android.util.Log.d(TAG, "成功跳转: $description")
                true
            } else {
                android.util.Log.d(TAG, "Intent无法解析: $description")
                false
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "$description 失败: ${e.message}")
            false
        }
    }
    
    // ========== 以下为原有的厂商特定实现方法 ==========
    
    /**
     * 厂商特定的开机自启动设置页面跳转
     * 
     * @param context 应用上下文
     */
    private fun openAutoStartSettings(context: Context) {
        try {
            val manufacturer = Build.MANUFACTURER.lowercase()
            val packageName = context.packageName
            
            val intent = when {
                manufacturer.contains("xiaomi") || manufacturer.contains("redmi") -> {
                    tryMultipleIntents(context, listOf(
                        Intent().apply {
                            component = ComponentName(
                                "com.miui.securitycenter",
                                "com.miui.permcenter.autostart.AutoStartDetailManagementActivity"
                            )
                            putExtra("extra_pkgname", packageName)
                        },
                        Intent().apply {
                            component = ComponentName(
                                "com.miui.securitycenter",
                                "com.miui.permcenter.autostart.AutoStartManagementActivity"
                            )
                        }
                    ))
                }
                manufacturer.contains("huawei") || manufacturer.contains("honor") -> {
                    tryMultipleIntents(context, listOf(
                        Intent().apply {
                            component = ComponentName(
                                "com.huawei.systemmanager",
                                "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                            )
                        }
                    ))
                }
                manufacturer.contains("oppo") -> {
                    tryMultipleIntents(context, listOf(
                        Intent().apply {
                            component = ComponentName(
                                "com.coloros.safecenter",
                                "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                            )
                        }
                    ))
                }
                manufacturer.contains("vivo") -> {
                    tryMultipleIntents(context, listOf(
                        Intent().apply {
                            component = ComponentName(
                                "com.vivo.permissionmanager",
                                "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                            )
                        }
                    ))
                }
                manufacturer.contains("samsung") -> {
                    tryMultipleIntents(context, listOf(
                        Intent().apply {
                            component = ComponentName(
                                "com.samsung.android.sm",
                                "com.samsung.android.sm.ui.battery.BatteryActivity"
                            )
                        }
                    ))
                }
                manufacturer.contains("oneplus") -> {
                    tryMultipleIntents(context, listOf(
                        Intent().apply {
                            component = ComponentName(
                                "com.oneplus.security",
                                "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
                            )
                        }
                    ))
                }
                manufacturer.contains("meizu") -> {
                    tryMultipleIntents(context, listOf(
                        Intent().apply {
                            component = ComponentName(
                                "com.meizu.safe",
                                "com.meizu.safe.permission.SmartBGActivity"
                            )
                        }
                    ))
                }
                else -> {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                    }
                }
            }
            
            if (intent != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                android.util.Log.d(TAG, "已跳转到开机自启动设置页面 (厂商: $manufacturer)")
            } else {
                throw Exception("无法找到合适的自启动设置页面")
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "无法打开开机自启动设置页面，使用应用详情页面: ${e.message}")
            openAppDetailsSettings(context)
        }
    }
    
    /**
     * 尝试多个Intent，返回第一个成功的
     * 
     * @param context 应用上下文
     * @param intents Intent列表
     * @return 成功的Intent，如果都失败则返回null
     */
    private fun tryMultipleIntents(context: Context, intents: List<Intent>): Intent? {
        for (intent in intents) {
            try {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                val resolveInfo = context.packageManager.resolveActivity(intent, 0)
                if (resolveInfo != null) {
                    android.util.Log.d(TAG, "找到可用的Intent: ${intent.component ?: intent.action}")
                    return intent
                }
            } catch (e: Exception) {
                android.util.Log.d(TAG, "Intent失败: ${intent.component ?: intent.action}, 错误: ${e.message}")
                continue
            }
        }
        android.util.Log.w(TAG, "所有Intent都失败，返回null")
        return null
    }
    
    /**
     * 原有的读取应用列表权限设置方法
     * 
     * @param context 应用上下文
     */
    private fun openQueryAllPackagesSettings(context: Context) {
        try {
            val intent = Intent().apply {
                action = Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            android.util.Log.d(TAG, "已跳转到应用列表权限设置页面")
        } catch (e: Exception) {
            android.util.Log.w(TAG, "无法打开应用列表权限设置页面: ${e.message}")
            openAppDetailsSettings(context)
        }
    }
    
    /**
     * 原有的通知权限设置方法
     * 
     * @param context 应用上下文
     */
    private fun openNotificationSettings(context: Context) {
        try {
            val intent = Intent().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                } else {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", context.packageName, null)
                }
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            android.util.Log.d(TAG, "已跳转到通知权限设置页面")
        } catch (e: Exception) {
            android.util.Log.w(TAG, "无法打开通知权限设置页面: ${e.message}")
            openAppDetailsSettings(context)
        }
    }
    
    /**
     * 原有的后台运行权限设置方法
     * 
     * @param context 应用上下文
     */
    private fun openBackgroundRunSettings(context: Context) {
        try {
            val intent = Intent().apply {
                action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            android.util.Log.d(TAG, "已跳转到电池优化设置页面")
        } catch (e: Exception) {
            android.util.Log.w(TAG, "无法打开电池优化设置页面: ${e.message}")
            try {
                val intent = Intent().apply {
                    action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                android.util.Log.d(TAG, "已跳转到电池优化设置列表")
            } catch (e2: Exception) {
                android.util.Log.w(TAG, "所有方案都失败，使用应用详情页面: ${e2.message}")
                openAppDetailsSettings(context)
            }
        }
    }
} 