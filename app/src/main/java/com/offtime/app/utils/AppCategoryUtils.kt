package com.offtime.app.utils

import com.offtime.app.data.dao.AppCategoryDao
import com.offtime.app.data.entity.AppCategoryEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 应用智能分类工具类
 * 统一管理应用的分类逻辑，确保应用信息表和会话记录表使用相同的分类策略
 */
@Singleton
class AppCategoryUtils @Inject constructor(
    private val appCategoryDao: AppCategoryDao
) {
    
    /**
     * 根据分类名称获取分类ID
     */
    suspend fun getCategoryIdByName(categoryName: String): Int {
        return try {
            val categories = appCategoryDao.getAllCategoriesList()
            val category = categories.find { it.name == categoryName }
            category?.id ?: run {
                android.util.Log.w("AppCategoryUtils", "找不到分类: $categoryName，返回默认娱乐分类")
                val entertainmentCategory = categories.find { it.name == "娱乐" }
                entertainmentCategory?.id ?: 1
            }
        } catch (e: Exception) {
            android.util.Log.e("AppCategoryUtils", "获取分类ID失败: ${e.message}", e)
            1 // 默认返回1
        }
    }

    /**
     * 根据应用包名智能确定分类ID
     */
    suspend fun getCategoryIdByPackageName(packageName: String): Int {
        return try {
            // 使用同步方法获取分类列表
            val categories = appCategoryDao.getAllCategoriesList()
            
            // 根据应用包名进行智能分类
            val targetCategoryName = getSmartCategoryName(packageName)
            
            // 查找对应分类的ID
            val targetCategory = categories.find { it.name == targetCategoryName }
            val categoryId = targetCategory?.id ?: run {
                // 如果找不到对应分类，默认使用娱乐分类
                val entertainmentCategory = categories.find { it.name == "娱乐" }
                entertainmentCategory?.id ?: 1
            }
            
            // 调试日志
            android.util.Log.d("AppCategoryUtils", 
                "智能分类: $packageName -> $targetCategoryName (ID=$categoryId)")
            
            categoryId
        } catch (e: Exception) {
            android.util.Log.e("AppCategoryUtils", "获取分类ID失败: ${e.message}", e)
            1 // 默认返回1
        }
    }
    
    /**
     * 应用分类结果
     */
    data class AppCategoryResult(
        val categoryName: String,
        val isExcluded: Boolean
    )
    
    /**
     * 根据包名获取分类结果（包含分类名称和是否排除）
     */
    private fun getSmartCategoryResult(packageName: String): AppCategoryResult {
        return when {
            // OffTimes应用本身 - 确保不被排除统计
            packageName == "com.offtime.app" || packageName == "com.offtime.app.gplay" -> {
                android.util.Log.d("AppCategoryUtils", "OffTimes应用 $packageName 分类为娱乐，不排除统计")
                AppCategoryResult("娱乐", false) // 确保不被排除
            }
            
            // 虚拟线下活动APP - 根据包名确定分类
            packageName.startsWith("com.offtime.offline.") -> {
                val categoryName = when {
                    packageName.contains("yule") || packageName.contains("娱乐") -> "娱乐"
                    packageName.contains("xuexi") || packageName.contains("学习") -> "学习"
                    packageName.contains("jianshen") || packageName.contains("健身") -> "健身"
                    packageName.contains("zongshiyong") || packageName.contains("总使用") -> "总使用"
                    else -> "娱乐" // 默认为娱乐
                }
                android.util.Log.d("AppCategoryUtils", "虚拟线下活动APP $packageName 分类为: $categoryName")
                AppCategoryResult(categoryName, false)
            }
            
            // 系统启动器应用 - 排除统计
            packageName in launcherApps -> {
                android.util.Log.d("AppCategoryUtils", "启动器应用 $packageName 设置为排除统计")
                AppCategoryResult("娱乐", true)
            }
            
            // 系统核心应用 - 排除统计
            packageName in systemCoreApps -> {
                android.util.Log.d("AppCategoryUtils", "系统核心应用 $packageName 设置为排除统计")
                AppCategoryResult("娱乐", true)
            }
            
            // 通过包名前缀识别系统应用 - 排除统计
            packageName.startsWith("com.android.") && isSystemApp(packageName) -> {
                android.util.Log.d("AppCategoryUtils", "通过前缀识别的系统应用 $packageName 设置为排除统计")
                AppCategoryResult("娱乐", true)
            }
            
            // Google系统服务 - 排除统计
            packageName.startsWith("com.google.android.") && isGoogleSystemApp(packageName) -> {
                android.util.Log.d("AppCategoryUtils", "Google系统应用 $packageName 设置为排除统计")
                AppCategoryResult("娱乐", true)
            }
            
            // 娱乐类应用
            packageName in entertainmentApps -> AppCategoryResult("娱乐", false)
            
            // 学习类应用
            packageName in learningApps -> AppCategoryResult("学习", false)
            
            // 健身类应用
            packageName in fitnessApps -> AppCategoryResult("健身", false)
            
            // 工作/办公类应用 (归为学习类)
            packageName in workApps -> AppCategoryResult("学习", false)
            
            // 其他情况默认为娱乐
            else -> AppCategoryResult("娱乐", false)
        }
    }
    
    /**
     * 根据包名获取分类名称（保持向后兼容）
     */
    private fun getSmartCategoryName(packageName: String): String {
        return getSmartCategoryResult(packageName).categoryName
    }
    
    /**
     * 判断是否为系统应用（通过包名特征）
     */
    private fun isSystemApp(packageName: String): Boolean {
        return when {
            // 系统核心组件
            packageName.startsWith("com.android.internal.") -> true
            packageName.startsWith("com.android.providers.") -> true
            packageName.startsWith("com.android.server.") -> true
            packageName.startsWith("com.android.systemui") -> true
            packageName.startsWith("com.android.bluetooth") -> true
            packageName.startsWith("com.android.dreams") -> true
            packageName.startsWith("com.android.egg") -> true
            packageName.startsWith("com.android.wallpaper") -> true
            packageName.startsWith("com.android.keychain") -> true
            packageName.startsWith("com.android.captive") -> true
            packageName.startsWith("com.android.managedprovisioning") -> true
            packageName.startsWith("com.android.provision") -> true
            packageName.startsWith("com.android.storagemanager") -> true
            packageName.startsWith("com.android.bips") -> true
            packageName.startsWith("com.android.cellbroadcast") -> true
            packageName.startsWith("com.android.emergency") -> true
            packageName.startsWith("com.android.inputmethod") -> true
            packageName.startsWith("com.android.printspooler") -> true
            packageName.startsWith("com.android.sharedstoragebackup") -> true
            packageName.startsWith("com.android.externalstorage") -> true
            
            // 系统角色和权限
            packageName.startsWith("com.android.role.") -> true
            
            // 系统主题、字体、图标
            packageName.startsWith("com.android.theme.") -> true
            
            // 网络相关系统组件
            packageName.startsWith("com.android.pacprocessor") -> true
            
            // 相机扩展和系统组件
            packageName.startsWith("com.android.cameraextensions") -> true
            packageName.startsWith("com.android.carrierdefault") -> true
            
            // Google智能和扩展服务
            packageName.startsWith("com.google.android.as") -> true
            packageName.startsWith("com.google.android.ext.") -> true
            packageName.startsWith("com.google.android.apps.restore") -> true
            packageName.startsWith("com.google.android.cellbroadcast") -> true
            packageName.startsWith("com.google.android.captiveportal") -> true
            packageName.startsWith("com.google.android.configupdater") -> true
            packageName.startsWith("com.google.android.contacts") -> true
            packageName.startsWith("com.google.android.apps.wellbeing") -> true
            
            // 设备管理和安全
            packageName.startsWith("com.android.compos") -> true
            packageName.startsWith("com.android.companiondevice") -> true
            packageName.startsWith("com.android.credential") -> true
            packageName.startsWith("com.android.devicediagnostics") -> true
            packageName.startsWith("com.android.devicelock") -> true
            
            // Google系统初始化和设置
            packageName.startsWith("com.google.android.onetimeinitializer") -> true
            packageName.startsWith("com.google.android.partnersetup") -> true
            packageName.startsWith("com.google.android.odad") -> true
            packageName.startsWith("com.google.android.safetycenter") -> true
            packageName.startsWith("com.google.android.printservice") -> true
            packageName.startsWith("com.google.android.rkpdapp") -> true
            packageName.startsWith("com.google.android.settings.intelligence") -> true
            packageName.startsWith("com.google.android.connectivity.resources") -> true
            packageName.startsWith("com.google.android.uwb.resources") -> true
            packageName.startsWith("com.google.android.wifi.resources") -> true
            packageName.startsWith("com.google.android.tag") -> true
            packageName.startsWith("com.google.android.apps.wallpaper") -> true
            packageName.startsWith("com.google.android.federatedcompute") -> true
            packageName.startsWith("com.google.android.googlesdksetup") -> true
            packageName.startsWith("com.google.android.health.connect") -> true
            packageName.startsWith("com.google.android.sdksandbox") -> true
            packageName.startsWith("com.google.android.server.deviceconfig") -> true
            packageName.startsWith("com.google.android.telephony.satellite") -> true
            packageName.startsWith("com.google.android.wifi.dialog") -> true
            
            // 系统工具和服务
            packageName.startsWith("com.android.htmlviewer") -> true
            packageName.startsWith("com.android.mms.service") -> true
            packageName.startsWith("com.android.proxyhandler") -> true
            packageName.startsWith("com.android.stk") -> true
            packageName.startsWith("com.android.se") -> true
            packageName.startsWith("com.android.traceur") -> true
            packageName.startsWith("com.android.vpndialogs") -> true
            packageName.startsWith("com.android.DeviceAsWebcam") -> true
            packageName.startsWith("com.android.backupconfirm") -> true
            packageName.startsWith("com.android.carrierconfig") -> true
            packageName.startsWith("com.android.cts") -> true
            packageName.startsWith("com.android.emulator") -> true
            packageName.startsWith("com.android.imsserviceentitlement") -> true
            packageName.startsWith("com.android.localtransport") -> true
            packageName.startsWith("com.android.microdroid") -> true
            packageName.startsWith("com.android.networkstack.tethering") -> true
            packageName.startsWith("com.android.ons") -> true
            packageName.startsWith("com.android.simappdialog") -> true
            
            // 自动生成的RRO (Runtime Resource Overlay)
            packageName.contains(".auto_generated_") -> true
            packageName.contains(".overlay.") -> true
            packageName.contains(".goldfish.overlay") -> true
            
            // === 抽象化系统应用识别规则 ===
            
            // 1. Google模块化系统组件
            packageName.startsWith("com.google.mainline.") -> true
            
            // 2. 系统功能性组件 (通过包名关键词)
            packageName.contains(".system") -> true
            packageName.contains(".internal") -> true
            packageName.contains(".config") -> true
            packageName.contains(".resources") && !packageName.contains("user") -> true
            packageName.contains(".service") && !packageName.contains("user") -> true
            packageName.contains(".provider") && !packageName.contains("user") -> true
            
            // 3. 系统工具和管理器 (通过包名末尾)
            packageName.endsWith("installer") -> true
            packageName.endsWith("resolver") -> true
            packageName.endsWith("picker") && packageName.startsWith("com.android") -> true
            packageName.endsWith("manager") && (packageName.startsWith("com.android") || packageName.startsWith("com.google.android")) -> true
            packageName.endsWith("host") && packageName.startsWith("com.android") -> true
            packageName.endsWith("agent") && packageName.startsWith("com.google.android") -> true
            
            // 4. 系统模块化组件
            packageName.contains("modulemetadata") -> true
            packageName.contains("appsearch") -> true
            packageName.contains("virtualmachine") -> true
            packageName.contains("ondevicepersonalization") -> true
            
            // 5. 系统特殊功能
            packageName.contains("dynsystem") -> true
            packageName.contains("location.fused") -> true
            packageName.contains("inputdevices") -> true
            packageName.contains("intentresolver") -> true
            packageName.contains("certinstaller") -> true
            packageName.contains("soundpicker") -> true
            packageName.contains("avatarpicker") -> true
            packageName.contains("feedback") && packageName.startsWith("com.google.android") -> true
            packageName.contains("markup") && packageName.startsWith("com.google.android") -> true
            packageName.contains("telemetry") && packageName.startsWith("com.google") -> true
            packageName.contains("tts") && packageName.startsWith("com.google.android") -> true
            
            // 6. MTP和媒体系统组件
            packageName.contains("mtp") && packageName.startsWith("com.android") -> true
            packageName.contains("media.module") && packageName.startsWith("com.google.android.providers") -> true
            
            // 7. Google系统主线模块和广告服务
            packageName.contains("mainline.adservices") && packageName.startsWith("com.google.android") -> true
            
            // 常见用户应用，不是系统应用
            packageName.startsWith("com.android.chrome") -> false
            packageName.startsWith("com.android.calendar") -> false // 日历可能是用户应用
            packageName.startsWith("com.android.camera") -> false  // 相机可能是用户应用
            packageName.startsWith("com.android.gallery") -> false // 图库可能是用户应用
            
            else -> false
        }
    }
    
    /**
     * 判断是否为Google系统应用
     */
    private fun isGoogleSystemApp(packageName: String): Boolean {
        return when {
            // Google系统服务
            packageName.startsWith("com.google.android.gms") -> true
            packageName.startsWith("com.google.android.gsf") -> true
            packageName.startsWith("com.google.android.ext.services") -> true
            packageName.startsWith("com.google.android.adservices") -> true
            packageName.startsWith("com.google.android.permissioncontroller") -> true
            packageName.startsWith("com.google.android.packageinstaller") -> true
            packageName.startsWith("com.google.android.marvin") -> true
            packageName.startsWith("com.google.android.projection") -> true
            packageName.startsWith("com.google.android.webview") -> true
            packageName.startsWith("com.google.android.bluetooth") -> true
            packageName.startsWith("com.google.android.networkstack") -> true
            packageName.startsWith("com.google.android.hotspot2") -> true
            
            // Google用户应用，不是系统应用
            packageName.startsWith("com.google.android.youtube") -> false
            packageName.startsWith("com.google.android.gm") -> false // Gmail
            packageName.startsWith("com.google.android.apps.maps") -> false // Maps
            packageName.startsWith("com.google.android.calendar") -> false // Calendar
            
            else -> false
        }
    }
    
    companion object {
        // 系统启动器应用列表（系统桌面，不应被统计）
        private val launcherApps = setOf(
            "com.google.android.apps.nexuslauncher", // Pixel Launcher
            "com.android.launcher",                  // 原生Android Launcher
            "com.android.launcher3",                 // Android Launcher3
            "com.miui.home",                        // MIUI桌面
            "com.huawei.android.launcher",          // 华为桌面
            "com.oppo.launcher",                    // OPPO桌面
            "com.vivo.launcher",                    // vivo桌面
            "com.oneplus.launcher",                 // OnePlus桌面
            "com.sec.android.app.launcher",         // 三星桌面
            "com.sonymobile.home",                  // 索尼桌面
            "com.lge.launcher2",                    // LG桌面
            "com.asus.launcher"                     // 华硕桌面
        )
        
        // 系统核心应用列表（不应被统计使用时间）
        private val systemCoreApps = setOf(
            "com.android.settings",                 // 系统设置
            "com.android.systemui",                 // 系统UI
            "com.android.phone",                    // 电话
            "com.android.contacts",                 // 联系人
            "com.android.mms",                      // 短信
            "com.android.dialer",                   // 拨号器
            "com.android.calculator2",              // 计算器
            "com.android.calendar",                 // 日历
            "com.android.camera2",                  // 相机
            "com.android.camera",                   // 相机
            "com.android.gallery3d",                // 图库
            "com.android.music",                    // 音乐播放器
            "com.android.filemanager",              // 文件管理器
            "com.android.documentsui",              // 文档
            "com.android.packageinstaller",         // 包安装器
            "com.android.vending",                  // Google Play商店
            "com.google.android.gms",               // Google Play服务
            "com.google.android.gsf",               // Google服务框架
            "com.miui.securitycenter",              // MIUI安全中心
            "com.miui.cleanmaster",                 // MIUI清理大师
            "com.huawei.systemmanager",             // 华为手机管家
            "com.oppo.safe",                        // OPPO手机管家
            "com.vivo.abe",                         // vivo应用管理
            "com.samsung.android.app.spage",        // 三星负一屏
            "com.sec.android.app.launcher",         // 三星桌面
            "android",                              // Android系统
            "com.android.shell",                    // Shell
            
            // 系统UI和导航相关
            "com.android.internal.systemui.navbar.twobutton",
            "com.android.internal.systemui.navbar.threebutton",
            "com.android.internal.systemui",
            
            // Google服务和隐私
            "com.google.android.adservices.api",
            "com.google.android.ext.services",
            "com.google.android.permissioncontroller",
            "com.google.android.packageinstaller",
            
            // 无障碍和辅助功能
            "com.google.android.marvin.talkback",
            "com.android.accessibility.suite",
            "com.google.android.marvin",
            
            // Android Auto和投屏
            "com.google.android.projection.gearhead",
            "com.android.projection",
            
            // 系统彩蛋和测试
            "com.android.egg",
            "com.android.test",
            
            // 其他常见系统应用
            "com.android.printspooler",
            "com.android.wallpaper",
            "com.android.wallpaper.livepicker",
            "com.android.keychain",
            "com.android.captiveportallogin",
            "com.android.managedprovisioning",
            "com.android.provision",
            "com.android.storagemanager",
            "com.android.bips",                     // 内置打印服务
            "com.android.bookmarkprovider",
            "com.android.cellbroadcastreceiver",
            "com.android.emergency",
            "com.android.inputmethod.latin",
            "com.android.nfc",
            "com.android.bluetooth",
            "com.android.server.telecom",
            "com.android.sharedstoragebackup",
            "com.android.externalstorage",
            "com.android.providers.media",
            "com.android.providers.downloads",
            "com.android.providers.calendar",
            "com.android.providers.contacts",
            
            // 新增系统应用（用户反馈需排除的应用）
            "com.android.mtp",                      // MTP Host
            "com.google.android.modulemetadata",    // Main components (模块元数据)
            "com.google.android.mainline.adservices", // Main components (广告服务)
            "com.google.android.feedback",          // Market Feedback Agent
            "com.google.android.markup",            // Markup
            "com.google.android.providers.media.module", // Media picker
            "com.google.android.avatarpicker",      // Choose a picture
            
            // MIUI/小米系统应用
            "com.miui.accessibility",
            "com.miui.analytics",
            "com.miui.backup",
            "com.miui.finddevice",
            "com.miui.systemAdSolution",
            "com.xiaomi.account",
            "com.xiaomi.mi_connect_service",
            "com.xiaomi.micloud.sdk",
            "com.miui.securitycenter",              // MIUI安全中心
            "com.miui.securityadd",                 // MIUI安全组件
            "com.miui.powerkeeper",                 // MIUI电量和性能
            "com.miui.battery",                     // MIUI电池管理
            "com.miui.cleanmaster",                 // MIUI清理大师
            "com.miui.antispam",                    // MIUI骚扰拦截
            "com.miui.guardprovider",               // MIUI安全守护
            "com.miui.personalassistant",           // MIUI智能助理
            "com.miui.voiceassist",                 // 小爱同学
            "com.xiaomi.aiasst.service",            // 小爱助手服务
            "com.xiaomi.aiasst.vision",             // 小爱视觉
            "com.xiaomi.xmsf",                      // 小米服务框架
            "com.xiaomi.xmsfkeeper",                // 小米服务保活
            "com.xiaomi.market",                    // 小米应用商店
            "com.xiaomi.gamecenter",                // 小米游戏中心
            "com.xiaomi.payment",                   // 小米支付
            "com.xiaomi.scanner",                   // 小米扫一扫
            "com.xiaomi.shop",                      // 小米商城
            "com.xiaomi.smarthome",                 // 米家
            "com.xiaomi.vipaccount",                // 小米VIP
            "com.xiaomi.ab",                        // 小米广告
            "com.xiaomi.analytics",                 // 小米统计
            "com.xiaomi.bluetooth",                 // 小米蓝牙
            "com.xiaomi.channel",                   // 小米推送
            "com.xiaomi.discover",                  // 小米发现
            "com.xiaomi.glgm",                      // 小米游戏加速
            "com.xiaomi.joyose",                    // 小米快应用
            "com.xiaomi.location.fused",            // 小米定位服务
            "com.xiaomi.metoknlp",                  // 小米网络定位
            "com.xiaomi.mipicks",                   // 小米精选
            "com.xiaomi.mirror",                    // 小米投屏
            "com.xiaomi.mtb",                       // 小米钱包
            "com.xiaomi.oversea.ecom",              // 小米海外商城
            "com.xiaomi.payment",                   // 小米支付
            "com.xiaomi.powerchecker",              // 小米电量检测
            "com.xiaomi.simactivate.service",       // 小米SIM卡激活
            "com.xiaomi.upnp",                      // 小米UPNP服务
            "com.xiaomi.wearable",                  // 小米穿戴
            "com.miui.bugreport",                   // MIUI错误报告
            "com.miui.cloudservice",                // MIUI云服务
            "com.miui.daemon",                      // MIUI守护进程
            "com.miui.fmservice",                   // MIUI收音机服务
            "com.miui.hybrid",                      // MIUI混合服务
            "com.miui.klo.bugreport",               // MIUI错误收集
            "com.miui.mishare.connectivity",        // MIUI互传连接
            "com.miui.networkassistant",            // MIUI网络助手
            "com.miui.notification",                // MIUI通知管理
            "com.miui.player",                      // MIUI音乐
            "com.miui.screenrecorder",              // MIUI屏幕录制
            "com.miui.securitycore",                // MIUI安全核心
            "com.miui.systemui.plugin",             // MIUI系统UI插件
            "com.miui.touchassistant",              // MIUI悬浮球
            "com.miui.translationservice",          // MIUI翻译服务
            "com.miui.userguide",                   // MIUI用户指南
            "com.miui.virtualsim",                  // MIUI虚拟SIM
            "com.miui.whetstone",                   // MIUI系统优化
            "com.miui.yellowpage",                  // MIUI黄页
            
            // 华为/荣耀系统应用
            "com.huawei.android.internal.app",
            "com.huawei.android.pushagent",
            "com.huawei.hwid",
            "com.huawei.appmarket",                 // 华为应用市场
            "com.huawei.browser",                   // 华为浏览器
            "com.huawei.gamecenter",                // 华为游戏中心
            "com.huawei.health",                    // 华为健康
            "com.huawei.hiassistant",               // 华为智慧助手
            "com.huawei.hiai",                      // 华为AI引擎
            "com.huawei.hiboard",                   // 华为智慧桌面
            "com.huawei.hicare",                    // 华为服务
            "com.huawei.hicloud",                   // 华为云空间
            "com.huawei.hifolder",                  // 华为文件夹
            "com.huawei.himovie",                   // 华为视频
            "com.huawei.himusic",                   // 华为音乐
            "com.huawei.hireader",                  // 华为阅读
            "com.huawei.hisuite",                   // 华为手机助手
            "com.huawei.hitouch",                   // 华为智感支付
            "com.huawei.hiview",                    // 华为系统诊断
            "com.huawei.hivoice",                   // 华为语音助手
            "com.huawei.hwasm",                     // 华为应用助手
            "com.huawei.hwdetectrepair",            // 华为智能检测
            "com.huawei.hwid.core",                 // 华为账号核心
            "com.huawei.intelligent",               // 华为智慧服务
            "com.huawei.kidsmode",                  // 华为儿童模式
            "com.huawei.livewallpaper.paradise",    // 华为动态壁纸
            "com.huawei.magazine",                  // 华为杂志锁屏
            "com.huawei.mirror",                    // 华为多屏协同
            "com.huawei.motionservice",             // 华为运动健康服务
            "com.huawei.nearby",                    // 华为畅连
            "com.huawei.parentcontrol",             // 华为学生模式
            "com.huawei.phoneservice",              // 华为电话服务
            "com.huawei.powergenie",                // 华为省电精灵
            "com.huawei.recsys",                    // 华为推荐系统
            "com.huawei.scanner",                   // 华为智慧识屏
            "com.huawei.screenrecorder",            // 华为屏幕录制
            "com.huawei.search",                    // 华为搜索
            "com.huawei.securitymgr",               // 华为手机管家
            "com.huawei.stylus",                    // 华为手写笔
            "com.huawei.synergy",                   // 华为多设备协同
            "com.huawei.systemmanager",             // 华为系统管理
            "com.huawei.trustspace",                // 华为安全空间
            "com.huawei.vassistant",                // 华为语音助手
            "com.huawei.wallet",                    // 华为钱包
            "com.huawei.watch.sync",                // 华为穿戴
            "com.huawei.works",                     // 华为办公
            "com.hihonor.cloudmusic",               // 荣耀音乐
            "com.hihonor.gamecenter",               // 荣耀游戏中心
            "com.hihonor.health",                   // 荣耀健康
            "com.hihonor.hicare",                   // 荣耀服务
            "com.hihonor.intelligent",              // 荣耀智慧服务
            "com.hihonor.magazine",                 // 荣耀杂志锁屏
            "com.hihonor.parentcontrol",            // 荣耀学生模式
            "com.hihonor.scanner",                  // 荣耀智慧识屏
            "com.hihonor.wallet",                   // 荣耀钱包
            
            // 三星系统应用
            "com.samsung.android.app.settings.bixby",
            "com.samsung.android.bixby.agent",
            "com.samsung.android.game.gametools",
            "com.samsung.android.bixby.service",    // Bixby服务
            "com.samsung.android.bixby.wakeup",     // Bixby唤醒
            "com.samsung.android.bixbyvision",      // Bixby视觉
            "com.samsung.android.app.galaxyfinder", // 三星查找
            "com.samsung.android.app.spage",        // 三星负一屏
            "com.samsung.android.app.tips",         // 三星使用技巧
            "com.samsung.android.app.watchmanager", // 三星手表管理
            "com.samsung.android.aremoji",          // 三星AR表情
            "com.samsung.android.authfw",           // 三星认证框架
            "com.samsung.android.bbc.bbcagent",     // 三星BBC代理
            "com.samsung.android.beaconmanager",    // 三星信标管理
            "com.samsung.android.biometrics.app.setting", // 三星生物识别设置
            "com.samsung.android.calendar",         // 三星日历
            "com.samsung.android.clipboarduiservice", // 三星剪贴板
            "com.samsung.android.contacts",         // 三星联系人
            "com.samsung.android.dqagent",          // 三星诊断代理
            "com.samsung.android.email.provider",   // 三星邮件提供商
            "com.samsung.android.fmm",              // 三星查找我的手机
            "com.samsung.android.game.gamehome",    // 三星游戏启动器
            "com.samsung.android.gametuner",        // 三星游戏优化器
            "com.samsung.android.health",           // 三星健康
            "com.samsung.android.incallui",         // 三星通话界面
            "com.samsung.android.kidsinstaller",    // 三星儿童模式安装器
            "com.samsung.android.lool",             // 三星Live Focus
            "com.samsung.android.mateagent",        // 三星Mate代理
            "com.samsung.android.messaging",        // 三星信息
            "com.samsung.android.oneconnect",       // 三星SmartThings
            "com.samsung.android.privateshare",     // 三星私密分享
            "com.samsung.android.samsungpass",      // 三星通行证
            "com.samsung.android.samsungpositioning", // 三星定位服务
            "com.samsung.android.securitylogagent", // 三星安全日志
            "com.samsung.android.service.peoplestripe", // 三星人员条纹
            "com.samsung.android.smartcallprovider", // 三星智能通话
            "com.samsung.android.smartface",        // 三星智能识别
            "com.samsung.android.smartmirroring",   // 三星智能镜像
            "com.samsung.android.smartswitchassistant", // 三星智能切换助手
            "com.samsung.android.spay",             // 三星支付
            "com.samsung.android.spayfw",           // 三星支付框架
            "com.samsung.android.stickercenter",    // 三星贴纸中心
            "com.samsung.android.svoice",           // 三星S Voice
            "com.samsung.android.tadownloader",     // 三星TA下载器
            "com.samsung.android.themestore",       // 三星主题商店
            "com.samsung.android.visionintelligence", // 三星Bixby视觉
            "com.samsung.android.voicewakeup",      // 三星语音唤醒
            "com.samsung.android.wellbeing",        // 三星数字健康
            "com.samsung.clipboardsaveservice",     // 三星剪贴板保存
            "com.samsung.crane",                    // 三星Crane
            "com.samsung.desktopsystemui",          // 三星桌面系统UI
            "com.samsung.gametuner",                // 三星游戏调节器
            "com.samsung.knox.securefolder",        // 三星安全文件夹
            "com.samsung.safetyinformation",        // 三星安全信息
            "com.samsung.sree",                     // 三星Sree
            "com.samsung.storyservice",             // 三星故事服务
            "com.samsung.systemui.bixby2",          // 三星Bixby2系统UI
            "com.samsung.voiceserviceplatform",     // 三星语音服务平台
            "com.samsung.vsimmanager",              // 三星虚拟SIM管理
            
            // vivo系统应用
            "com.vivo.easyshare",
            "com.vivo.smartshot",
            "com.vivo.appstore",                    // vivo应用商店
            "com.vivo.browser",                     // vivo浏览器
            "com.vivo.childrenmode",                // vivo儿童模式
            "com.vivo.compass",                     // vivo指南针
            "com.vivo.dream.clock",                 // vivo时钟
            "com.vivo.dream.music",                 // vivo音乐
            "com.vivo.dream.weather",               // vivo天气
            "com.vivo.ewarranty",                   // vivo电子保修卡
            "com.vivo.futureboard",                 // vivo未来桌面
            "com.vivo.gamecenter",                  // vivo游戏中心
            "com.vivo.globalsearch",                // vivo全局搜索
            "com.vivo.health",                      // vivo健康
            "com.vivo.hiboard",                     // vivo智慧桌面
            "com.vivo.iqoo.secure",                 // vivo安全中心
            "com.vivo.jovi",                        // Jovi智能助手
            "com.vivo.magazine",                    // vivo杂志锁屏
            "com.vivo.market",                      // vivo应用市场
            "com.vivo.minscreen",                   // vivo小窗模式
            "com.vivo.numbermark",                  // vivo号码标记
            "com.vivo.permissionmanager",           // vivo权限管理
            "com.vivo.pushservice",                 // vivo推送服务
            "com.vivo.scanner",                     // vivo扫一扫
            "com.vivo.screen.recorder",             // vivo屏幕录制
            "com.vivo.securedpay",                  // vivo安全支付
            "com.vivo.smartmultiwindow",            // vivo智能多窗口
            "com.vivo.space",                       // vivo个人空间
            "com.vivo.tips",                        // vivo使用技巧
            "com.vivo.touchassistant",              // vivo悬浮球
            "com.vivo.translator",                  // vivo翻译
            "com.vivo.unionpay",                    // vivo钱包
            "com.vivo.vcoin",                       // vivo金币
            "com.vivo.video",                       // vivo视频
            "com.vivo.wallet",                      // vivo钱包
            "com.vivo.weather",                     // vivo天气
            "com.vivo.website",                     // vivo官网
            "com.vivo.yellowpage",                  // vivo黄页
            "com.bbk.account",                      // BBK账户
            "com.bbk.appstore",                     // BBK应用商店
            "com.bbk.cloud",                        // BBK云服务
            "com.bbk.launcher2",                    // BBK启动器
            "com.bbk.theme",                        // BBK主题
            "com.iqoo.engineermode",                // iQOO工程模式
            "com.iqoo.powersaving",                 // iQOO省电模式
            "com.iqoo.secure",                      // iQOO安全中心
            
            // OPPO/OnePlus系统应用
            "com.oppo.operationManual",
            "com.oppo.atlas",
            "com.oneplus.account",
            "com.oppo.battery",                     // OPPO电池管理
            "com.oppo.powermanager",                // OPPO电源管理
            "com.oppo.smartsidebar",                // OPPO智能侧边栏
            "com.oppo.assistantscreen",             // OPPO助手屏幕
            "com.oppo.breeno",                      // 小布助手(Breeno)
            "com.oppo.breeno.service",              // 小布助手服务
            "com.oppo.breeno.speech",               // 小布语音
            "com.oppo.breeno.suggestion",           // 小布建议
            "com.oppo.breeno.quicksearch",          // 小布快搜
            "com.oppo.breeno.assistant",            // 小布助手主程序
            "com.oppo.breeno.weather",              // 小布天气
            "com.oppo.breeno.cards",                // 小布卡片
            "com.oppo.breeno.launcher",             // 小布启动器
            "com.oppo.usercenter",                  // OPPO用户中心
            "com.oppo.market",                      // OPPO软件商店
            "com.oppo.music",                       // OPPO音乐
            "com.oppo.video",                       // OPPO视频
            "com.oppo.gallery3d",                   // OPPO相册
            "com.oppo.camera",                      // OPPO相机
            "com.oppo.filemanager",                 // OPPO文件管理
            "com.oppo.safecenter",                  // OPPO手机管家
            "com.oppo.securepay",                   // OPPO安全支付
            "com.oppo.quicksearchbox",              // OPPO快速搜索
            "com.oppo.gamespace",                   // OPPO游戏空间
            "com.oppo.games",                       // OPPO游戏中心
            "com.oppo.childrenspace",               // OPPO儿童空间
            "com.oppo.ota",                         // OPPO系统更新
            "com.oppo.otaui",                       // OPPO更新界面
            "com.oppo.logkit",                      // OPPO日志工具
            "com.oppo.engineermode",                // OPPO工程模式
            "com.oppo.qualityprotect",              // OPPO质量保护
            "com.oppo.oppomultiapp",                // OPPO应用分身
            "com.oppo.oppopush",                    // OPPO推送服务
            "com.oppo.statistics.rom",              // OPPO统计服务
            "com.oppo.secscanservice",              // OPPO安全扫描
            "com.oppo.securitykeyboard",            // OPPO安全键盘
            "com.oppo.securitypermission",          // OPPO安全权限
            "com.oppo.activateservice",             // OPPO激活服务
            "com.oppo.autoregistration",            // OPPO自动注册
            "com.oppo.crashbox",                    // OPPO崩溃收集
            "com.oppo.deviceinfo",                  // OPPO设备信息
            "com.oppo.fingerprints.fingerprintpay", // OPPO指纹支付
            "com.oppo.healthservice",               // OPPO健康服务
            "com.oppo.location",                    // OPPO定位服务
            "com.oppo.nhs",                         // OPPO网络健康服务
            "com.oppo.partnerbrowsercustomizations", // OPPO浏览器定制
            "com.oppo.resmonitor",                  // OPPO资源监控
            "com.oppo.romupdate",                   // OPPO ROM更新
            "com.oppo.screenrecorder",              // OPPO屏幕录制
            "com.oppo.securityguard",               // OPPO安全卫士
            "com.oppo.sysoptimizer",                // OPPO系统优化
            "com.oppo.timeservice",                 // OPPO时间服务
            "com.oppo.usagestats",                  // OPPO使用统计
            "com.oppo.wallpapers",                  // OPPO壁纸
            "com.oppo.wellbeing",                   // OPPO数字健康
            "com.oppo.widget.smallweather",         // OPPO天气小部件
            
            // vivo系统应用
            "com.vivo.easyshare",
            "com.vivo.smartshot",
            
            // WebView和浏览器相关
            "com.google.android.webview",
            "com.android.webview",
            
            // 系统屏保和梦境
            "com.android.dreams.basic",
            "com.android.dreams.phototable",
            "com.android.dreams",
            
            // 数据提供者和存储
            "com.android.providers.blockednumber",
            "com.android.providers.userdictionary",
            "com.android.providers.telephony",
            "com.android.providers.settings",
            "com.android.providers.partnerbookmarks",
            
            // 蓝牙相关
            "com.google.android.bluetooth",
            "com.android.bluetooth",
            "com.android.bluetoothmidiservice",
            
            // 书签和同步
            "com.android.bookmarkprovider",
            "com.android.calllogbackup",
            
            // 日历应用
            "com.google.android.calendar",
            "com.android.calendar",
            
            // 通话记录
            "com.android.calllog",
            "com.android.calllogbackup",
            
            // 网络和连接相关
            "com.google.android.networkstack",
            "com.android.pacprocessor",
            "com.google.android.hotspot2.osulogin",
            
            // 系统角色和权限
            "com.android.role.notes.enabled",
            "com.android.role.dialer",
            "com.android.role.browser",
            "com.android.role.sms",
            "com.android.role.home",
            
            // 系统主题和字体
            "com.android.theme.font.notoserifsource",
            "com.android.theme.color.cinnamon",
            "com.android.theme.color.ocean",
            "com.android.theme.color.space",
            "com.android.theme.color.orchid",
            "com.android.theme.icon.pebble",
            "com.android.theme.icon.filled",
            "com.android.theme.icon.rounded",
            
            // 系统壁纸和主题
            "com.android.wallpaper.livepicker",
            "com.android.wallpaper.nexus",
            "com.android.theme.font",
            "com.android.theme.color",
            "com.android.theme.icon",
            
                            // 截图中新发现的系统应用
                "com.google.android.ext.shared",         // Android Shared Library
                "com.google.android.apps.restore",       // Android Switch
                "com.google.android.as",                 // Android System Intelligence
                "com.android.cameraextensions",          // CameraExtensionsProxy
                "com.google.android.captiveportallogin", // Captive Portal Login
                "com.android.carrierdefaultapp",         // Carrier Communications
                "com.google.android.cellbroadcastservice", // Cell Broadcast Service
                
                // 第二批截图中的系统应用
                "com.android.compos.payload",            // CompOS
                "com.android.companiondevicemanager",    // Companion Device Manager
                "com.google.android.configupdater",      // ConfigUpdater
                "com.google.android.contacts",           // Contacts
                "com.android.credentialmanager",         // Credential Manager
                "com.android.devicediagnostics",         // DeviceDiagnostics
                "com.android.devicelockcontroller",      // DeviceLockController
                "com.google.android.apps.wellbeing",     // Digital Wellbeing
                
                // 第三批 - Google系统服务
                "com.google.android.onetimeinitializer", // Google One Time Init
                "com.google.android.partnersetup",       // Google Partner Setup
                "com.google.android.odad",               // Google Play Protect Service
                "com.google.android.safetycenter.resources", // Google Safety Center Resources
                "com.android.htmlviewer",                 // HTML Viewer
                "com.android.mms.service",               // MmsService
                "com.google.android.apps.customization.pixel", // Pixel Themes
                "com.google.android.printservice.recommendation", // Print Service Recommendation
                "com.android.proxyhandler",              // ProxyHandler
                "com.google.android.rkpdapp",            // RemoteProvisioner
                "com.android.stk",                       // SIM Toolkit
                "com.android.se",                        // SecureElementApplication
                "com.google.android.settings.intelligence", // Settings Services
                "com.google.android.connectivity.resources", // System Connectivity Resources
                "com.android.traceur",                   // System Tracing
                "com.google.android.uwb.resources",     // System UWB Resources
                "com.google.android.wifi.resources",    // System Wi-Fi Resources
                "com.google.android.tag",               // Tags
                "com.android.vpndialogs",               // VpnDialogs
                "com.google.android.apps.wallpaper",    // Wallpaper & style
                "com.android.DeviceAsWebcam",            // Webcam Service
                
                // 第四批 - 系统配置和RRO
                "com.android.backupconfirm",            // Backup confirm
                "com.android.carrierconfig",            // Carrier config
                "com.android.cts.ctsshim",              // CTS Shim
                "com.android.cts.priv.ctsshim",         // CTS Private Shim
                "com.android.emulator.radio.config",    // Emulator radio config
                "com.android.imsserviceentitlement",    // IMS service entitlement
                "com.android.localtransport",           // Local transport
                "com.android.microdroid.empty_payload", // Microdroid empty payload
                "com.android.networkstack.tethering.emulator", // Network stack tethering emulator
                "com.android.ons",                      // ONS
                "com.android.simappdialog",             // SIM app dialog
                
                // 第五批 - Google高级服务
                "com.google.android.federatedcompute",  // Federated compute
                "com.google.android.googlesdksetup",    // Google SDK setup
                "com.google.android.health.connect.backuprestore", // Health connect backup
                "com.google.android.sdksandbox",        // SDK sandbox
                "com.google.android.server.deviceconfig.resources", // Server device config
                "com.google.android.telephony.satellite", // Telephony satellite
                "com.google.android.wifi.dialog"        // Wi-Fi dialog
        )
        
        // 娱乐类应用包名列表
        private val entertainmentApps = setOf(
            "com.google.android.youtube",           // YouTube
            "tv.danmaku.bili",                      // Bilibili
            "com.ss.android.ugc.aweme",            // TikTok
            "com.tencent.qqmusic",                  // QQ音乐
            "com.netease.cloudmusic",               // 网易云音乐
            "com.spotify.music",                    // Spotify
            "com.instagram.android",                // Instagram
            "com.facebook.katana",                  // Facebook
            "com.twitter.android",                  // Twitter
            "com.sina.weibo",                       // 新浪微博
            "com.zhihu.android",                    // 知乎
            "com.ximalaya.ting.android",           // 喜马拉雅
            "com.tencent.tmgp.sgame",              // 王者荣耀
            "com.tencent.tmgp.pubgmhd",            // 和平精英
            "com.miHoYo.GenshinImpact",            // 原神
            "com.netease.dwrg",                     // 第五人格
            "com.taobao.taobao",                   // 淘宝
            "com.tmall.wireless",                  // 天猫
            "com.jingdong.app.mall",               // 京东
            "com.tencent.mm",                      // 微信
            "com.tencent.mobileqq",                // QQ
            "com.whatsapp",                        // WhatsApp
            "com.douban.frodo",                    // 豆瓣
            "com.youku.phone",                     // 优酷
            "com.iqiyi.i18n",                      // 爱奇艺
            "com.tencent.qqlive",                  // 腾讯视频
            "com.baidu.tieba",                     // 百度贴吧
            "com.reddit.frontpage",                // Reddit
            "com.discord"                          // Discord
        )
        
        // 学习类应用包名列表
        private val learningApps = setOf(
            "com.duolingo",                        // 多邻国
            "com.youdao.dict",                     // 有道词典
            "com.kingsoft.powerword",              // 金山词霸
            "com.chaoxing.mobile",                 // 超星学习通
            "com.ss.android.article.news",        // 今日头条（知识学习）
            "com.evernote",                        // 印象笔记
            "com.notion.id",                       // Notion
            "com.adobe.reader",                    // Adobe Reader
            "com.microsoft.office.word",           // Word
            "com.microsoft.office.excel",          // Excel
            "com.microsoft.office.powerpoint",     // PowerPoint
            "com.wps.moffice",                     // WPS Office
            "org.coursera.android",                // Coursera
            "com.udemy.android",                   // Udemy
            "com.khanacademy.android",             // Khan Academy
            "com.busuu.android",                   // Busuu
            "com.babbel.mobile.android.en",        // Babbel
            "org.edx.mobile",                      // edX
            "com.anki.android",                    // Anki
            
            // Chrome浏览器 - 用于学习和研究
            "com.android.chrome",                  // Chrome浏览器
            "com.chrome.beta",                     // Chrome Beta
            "com.chrome.dev",                      // Chrome Dev
            "com.chrome.canary",                   // Chrome Canary
            
            // Google搜索和学习相关应用
            "com.google.android.googlequicksearchbox", // Google搜索
            "com.google.android.apps.searchlite",  // Google Go
            "com.google.android.apps.books",       // Google Play Books
            "com.google.android.apps.translate",   // Google翻译
            "com.google.android.apps.classroom",   // Google Classroom
            "com.google.android.apps.docs.editors.docs", // Google Docs
            "com.google.android.apps.docs.editors.sheets", // Google Sheets
            "com.google.android.apps.docs.editors.slides", // Google Slides
            "com.google.android.keep",             // Google Keep
            "com.google.android.apps.photos",      // Google Photos (学习资料管理)
            "com.google.android.apps.drive"       // Google Drive (学习资料存储)
        )
        
        // 健身类应用包名列表
        private val fitnessApps = setOf(
            "com.nike.ntc",                        // Nike Training Club
            "com.adidas.app",                      // Adidas Training
            "com.jefit.android",                   // JEFIT
            "com.MyFitnessPal.Android",            // MyFitnessPal
            "com.strava",                          // Strava
            "com.runtastic.android",               // Runtastic
            "com.google.android.apps.fitness",    // Google Fit
            "com.samsung.android.app.health",     // Samsung Health
            "com.xiaomi.hm.health",                // 小米运动健康
            "com.huawei.health",                   // 华为健康
            "com.mi.health",                       // 小米健康
            "com.keep.app",                        // Keep
            "com.endomondo.android",               // Endomondo
            "com.fitbit.FitbitMobile",             // Fitbit
            "com.garmin.android.apps.connectmobile", // Garmin Connect
            "com.polar.polarflow",                 // Polar Flow
            "com.kinomap.kinomap",                 // Kinomap
            "com.google.android.apps.maps"        // Google Maps (户外运动导航)
        )
        
        // 工作/办公类应用包名列表（归为学习类）
        private val workApps = setOf(
            "com.alibaba.android.rimet",           // 钉钉
            "com.tencent.wework",                  // 企业微信
            "com.microsoft.teams",                 // Teams
            "us.zoom.videomeetings",               // Zoom
            "com.slack",                           // Slack
            "com.google.android.gm",               // Gmail
            "com.microsoft.office.outlook",        // Outlook
            "com.google.android.apps.docs.editors.docs", // Google Docs
            "com.google.android.apps.docs.editors.sheets", // Google Sheets
            "com.trello",                          // Trello
            "com.asana.app",                       // Asana
            "com.todoist",                         // Todoist
            "com.any.do"                           // Any.do
        )
    }
    
    /**
     * 获取分类的描述信息
     */
    fun getCategoryDescription(categoryName: String): String {
        return when (categoryName) {
            "娱乐" -> "视频、音乐、游戏、社交等娱乐应用"
            "学习" -> "学习、办公、阅读等提升类应用"
            "健身" -> "运动、健康、健身等相关应用"
            "总使用" -> "所有应用的总体使用统计"

            else -> "其他类型应用"
        }
    }
    
    /**
     * 批量更新应用分类
     */
    suspend fun updateAppCategoriesBatch(apps: List<String>): Map<String, Int> {
        val results = mutableMapOf<String, Int>()
        
        for (packageName in apps) {
            val categoryId = getCategoryIdByPackageName(packageName)
            results[packageName] = categoryId
        }
        
        return results
    }
} 