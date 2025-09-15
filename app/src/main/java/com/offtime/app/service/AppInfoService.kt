package com.offtime.app.service

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import com.offtime.app.data.entity.AppInfoEntity
import com.offtime.app.utils.PermissionUtils
import com.offtime.app.utils.AppCategoryUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppInfoService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appCategoryUtils: AppCategoryUtils
) {
    
    private val packageManager = context.packageManager
    
    /**
     * 获取所有用户可用应用（包括系统应用，但会智能分类）
     */
    suspend fun getAllInstalledApps(): List<AppInfoEntity> {
        val hasUsageStats = PermissionUtils.hasUsageStatsPermission(context)
        val hasQueryPackages = PermissionUtils.hasQueryAllPackagesPermission(context)
        
        android.util.Log.d("AppInfoService", "权限检查 - UsageStats: $hasUsageStats, QueryPackages: $hasQueryPackages")
        
        // 对于Android 11 (API 30) 及以上版本，需要QUERY_ALL_PACKAGES权限
        // 对于较低版本，通常不需要此权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !hasQueryPackages) {
            android.util.Log.w("AppInfoService", "Android 11+ 缺少查询应用列表权限，将返回空列表")
            return emptyList()
        }
        
        // 对于Android 11以下版本，即使没有QUERY_ALL_PACKAGES权限也继续尝试扫描
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            android.util.Log.d("AppInfoService", "Android 11以下版本，跳过QUERY_ALL_PACKAGES权限检查")
        }
        
        val apps = mutableListOf<AppInfoEntity>()
        
        try {
            val installedApps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            }
            
            android.util.Log.d("AppInfoService", "开始处理 ${installedApps.size} 个已安装应用")
            
            var filteredCount = 0
            var addedCount = 0
            
            for (appInfo in installedApps) {
                // 调试：记录每个应用的基本信息
                val appName = try {
                    appInfo.loadLabel(packageManager).toString()
                } catch (e: Exception) {
                    appInfo.packageName
                }
                
                // 不在扫描时过滤系统应用，而是在分类时将其分类到"排除统计"
                
                // 只过滤掉最核心的系统组件，其他应用都添加到数据库
                if (shouldCompletelyFilterApp(appInfo)) {
                    android.util.Log.v("AppInfoService", "完全过滤应用: $appName (${appInfo.packageName})")
                    filteredCount++
                    continue
                }
                
                try {
                    val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        packageManager.getPackageInfo(appInfo.packageName, PackageManager.PackageInfoFlags.of(0))
                    } else {
                        @Suppress("DEPRECATION")
                        packageManager.getPackageInfo(appInfo.packageName, 0)
                    }
                    
                    val versionName = packageInfo.versionName ?: "Unknown"
                    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        packageInfo.longVersionCode
                    } else {
                        @Suppress("DEPRECATION")
                        packageInfo.versionCode.toLong()
                    }
                    
                    // 使用智能分类工具确定应用分类
                    val smartCategoryId = appCategoryUtils.getCategoryIdByPackageName(appInfo.packageName)
                    
                    val appEntity = AppInfoEntity(
                        packageName = appInfo.packageName,
                        appName = appName,
                        versionName = versionName,
                        versionCode = versionCode,
                        isSystemApp = isSystemApp(appInfo),
                        categoryId = smartCategoryId, // 智能分类会将系统应用分类到"排除统计"
                        firstInstallTime = packageInfo.firstInstallTime,
                        lastUpdateTime = packageInfo.lastUpdateTime,
                        isEnabled = appInfo.enabled
                    )
                    
                    apps.add(appEntity)
                    addedCount++
                    android.util.Log.v("AppInfoService", "添加应用: $appName (${appInfo.packageName}) -> 分类ID=$smartCategoryId")
                } catch (e: PackageManager.NameNotFoundException) {
                    // 应用可能已被卸载，跳过
                    android.util.Log.w("AppInfoService", "应用包信息未找到: $appName (${appInfo.packageName})")
                    continue
                } catch (e: Exception) {
                    android.util.Log.e("AppInfoService", "处理应用时出错: $appName (${appInfo.packageName}): ${e.message}")
                    continue
                }
            }
            
            android.util.Log.d("AppInfoService", "应用处理完成 - 总数: ${installedApps.size}, 完全过滤: $filteredCount, 添加: $addedCount")
        } catch (e: SecurityException) {
            android.util.Log.e("AppInfoService", "权限不足，无法获取应用列表: ${e.message}")
            // 对于Android 11+，如果没有权限，返回空列表
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                return emptyList()
            }
        } catch (e: Exception) {
            // 处理其他异常
            android.util.Log.e("AppInfoService", "获取应用列表失败: ${e.message}", e)
        }
        
        android.util.Log.d("AppInfoService", "最终获取到 ${apps.size} 个应用（包括系统应用）")
        return apps
    }
    
    /**
     * 获取单个应用信息
     */
    suspend fun getAppInfo(packageName: String): AppInfoEntity? {
        try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
            
            val appName = appInfo.loadLabel(packageManager).toString()
            val versionName = packageInfo.versionName ?: "Unknown"
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            
            return AppInfoEntity(
                packageName = packageName,
                appName = appName,
                versionName = versionName,
                versionCode = versionCode,
                isSystemApp = isSystemApp(appInfo),
                firstInstallTime = packageInfo.firstInstallTime,
                lastUpdateTime = packageInfo.lastUpdateTime,
                isEnabled = appInfo.enabled
            )
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * 判断是否应该过滤这个应用
     * 只过滤掉真正的系统核心应用，保留用户应用和有用的预装应用
     */
    private fun shouldFilterApp(appInfo: ApplicationInfo): Boolean {
        val packageName = appInfo.packageName
        
        // 如果是用户应用（非系统应用），绝对不过滤
        val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        val isUpdatedSystemApp = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        
        if (!isSystemApp && !isUpdatedSystemApp) {
            android.util.Log.v("AppInfoService", "用户应用不过滤: $packageName")
            return false
        }
        
        // 检查是否有启动器Activity（可以被用户启动的应用）
        val hasLauncherActivity = try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            intent != null
        } catch (e: Exception) {
            false
        }
        
        // 如果有启动器Activity，通常是用户可用的应用，不过滤
        if (hasLauncherActivity) {
            android.util.Log.v("AppInfoService", "有启动器Activity的应用不过滤: $packageName")
            return false
        }
        
        // 白名单：常见的用户应用，即使是系统应用也不过滤
        val userAppWhitelist = setOf(
            "com.android.chrome",           // Chrome
            "com.google.android.youtube",   // YouTube
            "com.google.android.gm",        // Gmail
            "com.google.android.apps.maps", // Google Maps
            "com.whatsapp",                 // WhatsApp
            "com.facebook.katana",          // Facebook
            "com.instagram.android",        // Instagram
            "com.twitter.android",          // Twitter
            "com.tencent.mm",              // WeChat
            "com.tencent.mobileqq",        // QQ
            "com.alibaba.android.rimet",    // DingTalk
            "com.ss.android.ugc.aweme",     // TikTok
            "com.netease.cloudmusic",       // 网易云音乐
            "com.tencent.qqmusic",         // QQ音乐
            "tv.danmaku.bili",             // Bilibili
            "com.taobao.taobao",           // 淘宝
            "com.tmall.wireless",          // 天猫
            "com.jingdong.app.mall",       // 京东
            "com.baidu.BaiduMap",          // 百度地图
            "com.autonavi.minimap",        // 高德地图
            "com.sina.weibo",              // 新浪微博
            "com.zhihu.android",           // 知乎
            "com.ximalaya.ting.android",   // 喜马拉雅
            "com.duokan.phone.remotecontroller", // 万能遥控
            "com.miui.calculator",         // 计算器
            "com.android.gallery3d",       // 相册
            "com.miui.gallery",            // MIUI相册
            "com.google.android.apps.photos", // Google Photos
            "com.android.camera",          // 相机
            "com.android.camera2",         // 相机2
            "com.miui.calculator",         // 计算器
            "com.android.contacts",        // 联系人
            "com.miui.notes",              // 便签
            "com.android.deskclock",       // 时钟
            "com.google.android.calendar", // 日历
            "com.android.calendar",        // 系统日历
            "com.miui.compass",            // 指南针
            "com.miui.weather2",           // 天气
            "com.miui.player",             // 音乐播放器
            "com.miui.video",              // 视频播放器
            "com.android.fileexplorer",    // 文件管理器
            "com.miui.fileexplorer"        // MIUI文件管理器
        )
        
        if (userAppWhitelist.contains(packageName)) {
            android.util.Log.v("AppInfoService", "白名单应用不过滤: $packageName")
            return false
        }
        
        // 黑名单：需要过滤的系统核心组件
        val systemBlacklist = setOf(
            "android",
            "com.android.systemui",
            "com.android.settings",
            "com.android.vending",
            "com.google.android.gms",
            "com.google.android.gsf",
            "com.android.phone",
            "com.android.dialer",
            "com.android.mms",
            "com.android.bluetooth",
            "com.android.nfc",
            "com.android.server.telecom",
            "com.android.shell",
            "com.android.sharedstoragebackup",
            "com.android.externalstorage",
            "com.android.providers.media",
            "com.android.providers.downloads",
            "com.android.wallpaper.livepicker",
            "com.android.inputmethod.latin",
            "com.android.cellbroadcastreceiver",
            "com.android.emergency",
            "com.miui.securitycenter",     // MIUI安全中心
            "com.miui.securityadd",        // MIUI安全组件
            "com.xiaomi.finddevice",       // 查找设备
            // 小米/MIUI系统应用
            "com.miui.powerkeeper",        // MIUI电量和性能
            "com.miui.battery",            // MIUI电池管理
            "com.miui.cleanmaster",        // MIUI清理大师
            "com.miui.antispam",           // MIUI骚扰拦截
            "com.miui.personalassistant",  // MIUI智能助理
            "com.miui.voiceassist",        // 小爱同学
            "com.xiaomi.aiasst.service",   // 小爱助手服务
            "com.xiaomi.market",           // 小米应用商店
            "com.xiaomi.gamecenter",       // 小米游戏中心
            "com.xiaomi.payment",          // 小米支付
            "com.xiaomi.smarthome",        // 米家
            "com.xiaomi.xmsf",             // 小米服务框架
            // 华为/荣耀系统应用
            "com.huawei.appmarket",        // 华为应用市场
            "com.huawei.gamecenter",       // 华为游戏中心
            "com.huawei.health",           // 华为健康
            "com.huawei.hiassistant",      // 华为智慧助手
            "com.huawei.hicare",           // 华为服务
            "com.huawei.hicloud",          // 华为云空间
            "com.huawei.hivoice",          // 华为语音助手
            "com.huawei.securitymgr",      // 华为手机管家
            "com.huawei.systemmanager",    // 华为系统管理
            "com.huawei.wallet",           // 华为钱包
            // 三星系统应用
            "com.samsung.android.bixby.service", // Bixby服务
            "com.samsung.android.health",  // 三星健康
            "com.samsung.android.samsungpass", // 三星通行证
            "com.samsung.android.spay",    // 三星支付
            "com.samsung.android.wellbeing", // 三星数字健康
            "com.samsung.knox.securefolder", // 三星安全文件夹
            // vivo系统应用
            "com.vivo.appstore",           // vivo应用商店
            "com.vivo.gamecenter",         // vivo游戏中心
            "com.vivo.health",             // vivo健康
            "com.vivo.jovi",               // Jovi智能助手
            "com.vivo.pushservice",        // vivo推送服务
            "com.vivo.wallet",             // vivo钱包
            "com.bbk.account",             // BBK账户
            "com.bbk.cloud",               // BBK云服务
            // OPPO系统应用
            "com.oppo.battery",            // OPPO电池管理
            "com.oppo.powermanager",       // OPPO电源管理
            "com.oppo.breeno",             // 小布助手
            "com.oppo.breeno.service",     // 小布助手服务
            "com.oppo.breeno.speech",      // 小布语音
            "com.oppo.breeno.assistant",   // 小布助手主程序
            "com.oppo.safecenter",         // OPPO手机管家
            "com.oppo.usercenter",         // OPPO用户中心
            "com.oppo.ota",                // OPPO系统更新
            "com.oppo.oppopush",           // OPPO推送服务
            "com.oppo.statistics.rom",     // OPPO统计服务
            "com.oppo.secscanservice",     // OPPO安全扫描
            "com.oppo.securityguard",      // OPPO安全卫士
            "com.oppo.sysoptimizer",       // OPPO系统优化
            "com.oppo.usagestats",         // OPPO使用统计
            "com.oppo.wellbeing",          // OPPO数字健康
            // 启动器应用（系统桌面）- 不应被统计使用时间
            "com.google.android.apps.nexuslauncher", // Pixel Launcher
            "com.android.launcher",        // 原生Android Launcher
            "com.android.launcher3",       // Android Launcher3
            "com.miui.home",              // MIUI桌面
            "com.huawei.android.launcher", // 华为桌面
            "com.oppo.launcher",          // OPPO桌面
            "com.vivo.launcher",          // vivo桌面
            "com.oneplus.launcher",       // OnePlus桌面
            "com.sec.android.app.launcher", // 三星桌面
            "com.sonymobile.home"         // 索尼桌面
        )
        
        // 如果在黑名单中，过滤掉
        val shouldFilter = systemBlacklist.any { packageName.startsWith(it) }
        if (shouldFilter) {
            android.util.Log.v("AppInfoService", "黑名单应用过滤: $packageName")
            return true
        }
        
        // 对于其他系统应用，如果没有明确的启动Activity，通常是系统组件，过滤掉
        // 但放宽条件：只过滤那些明显是系统组件的应用
        val isSystemComponent = packageName.startsWith("com.android.") || 
                               packageName.startsWith("com.google.android.") ||
                               packageName.startsWith("android.")
        
        if (isSystemComponent && !hasLauncherActivity) {
            android.util.Log.v("AppInfoService", "系统组件过滤: $packageName")
            return true
        }
        
        // 默认不过滤，给用户更多选择
        android.util.Log.v("AppInfoService", "默认不过滤: $packageName")
        return false
    }
    
    /**
     * 判断是否应该完全过滤这个应用
     * 只过滤掉最核心的系统组件，其他应用都添加到数据库
     */
    private fun shouldCompletelyFilterApp(appInfo: ApplicationInfo): Boolean {
        val packageName = appInfo.packageName
        
        // 最核心的系统组件黑名单：这些真的不需要在数据库中
        val coreSystemBlacklist = setOf(
            "android",
            "com.android.systemui",
            "com.google.android.gms",
            "com.google.android.gsf",
            "com.android.shell",
            "com.android.sharedstoragebackup",
            "com.android.externalstorage",
            "com.android.providers.media",
            "com.android.providers.downloads",
            "com.android.providers.calendar",
            "com.android.providers.contacts",
            "com.android.inputmethod.latin",
            "com.android.cellbroadcastreceiver",
            "com.android.server.telecom",
            "com.miui.securityadd"        // MIUI安全组件
        )
        
        // 完全过滤核心系统组件
        val shouldCompletelyFilter = coreSystemBlacklist.any { packageName.startsWith(it) }
        
        if (shouldCompletelyFilter) {
            android.util.Log.v("AppInfoService", "完全过滤核心系统组件: $packageName")
            return true
        }
        
        // 其他应用都添加到数据库，由智能分类决定是否归类为"排除统计"
        return false
    }
    
    /**
     * 获取应用图标
     */
    fun getAppIcon(packageName: String): Drawable? {
        return try {
            packageManager.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }
    
    /**
     * 判断是否为系统应用（保留原方法供其他地方使用）
     */
    private fun isSystemApp(appInfo: ApplicationInfo): Boolean {
        return (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
    }
    

} 