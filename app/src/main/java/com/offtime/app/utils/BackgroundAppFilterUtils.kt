package com.offtime.app.utils

import android.content.Context
import android.util.Log

/**
 * 常驻后台应用智能过滤工具
 * 
 * 用于识别和处理常驻后台应用（如微信、支付宝）的使用时间统计问题
 * 
 * 核心功能：
 * 1. 识别常驻后台应用
 * 2. 应用更严格的使用时间过滤规则
 * 3. 检测可能的后台唤醒与真实使用
 * 4. 提供调整后的使用时间统计
 */
object BackgroundAppFilterUtils {
    
    private const val TAG = "BackgroundAppFilter"
    
    /**
     * 常驻后台应用列表
     * 这些应用经常在后台运行，需要更严格的使用时间过滤
     */
    private val BACKGROUND_RESIDENT_APPS = setOf(
        // 即时通讯应用
        "com.tencent.mm",                    // 微信
        "com.tencent.mobileqq",              // QQ
        "com.alibaba.android.rimet",         // 钉钉
        "com.ss.android.lark",               // 飞书
        "com.tencent.wework",                // 企业微信
        "com.whatsapp",                      // WhatsApp
        "com.facebook.orca",                 // Facebook Messenger
        "com.telegram.messenger",            // Telegram
        "com.viber.voip",                    // Viber
        "com.skype.raider",                  // Skype
        
        // 支付应用
        "com.eg.android.AlipayGphone",       // 支付宝
        "com.tencent.mm.plugin.payment",     // 微信支付
        "com.unionpay.mobile.android",       // 云闪付
        
        // 金融应用
        "com.chinamworld.bocmbci",           // 中国银行
        "com.icbc",                          // 工商银行
        "com.ccb.CCBMobile",                 // 建设银行
        "cmb.pb",                            // 招商银行
        "com.abc.mobile",                    // 农业银行
        
        // 音乐应用（后台播放）
        "com.netease.cloudmusic",            // 网易云音乐
        
        // 新闻资讯应用（容易后台常驻）
        "com.ss.android.article.news",       // 今日头条
        "com.tencent.news",                  // 腾讯新闻
        "com.netease.newsreader.activity",   // 网易新闻
        "com.sohu.newsclient",               // 搜狐新闻
        "com.sina.news",                     // 新浪新闻
        "com.ifeng.news2",                   // 凤凰新闻
        "com.baidu.news",                    // 百度新闻
        "com.UCMobile",                      // UC浏览器（含新闻）
        "com.qihoo.browser",                 // 360浏览器（含新闻）
        
        // 视频应用（后台预载和推送）
        "com.ss.android.ugc.aweme",         // 抖音
        "com.smile.gifmaker",                // 快手
        "com.tencent.mm.plugin.video",      // 微信视频号
        "com.baidu.tieba",                   // 百度贴吧
        "com.zhihu.android",                 // 知乎
        "com.tencent.qqmusic",               // QQ音乐
        "com.kugou.android",                 // 酷狗音乐
        "com.kuwo.kwmusic",                  // 酷我音乐
        "fm.xiami.main",                     // 虾米音乐
        "com.spotify.music",                 // Spotify
        "com.apple.android.music",           // Apple Music
        
        // 导航应用
        "com.baidu.BaiduMap",                // 百度地图
        "com.autonavi.minimap",              // 高德地图
        "com.google.android.apps.maps",      // Google Maps
        "com.tencent.map",                   // 腾讯地图
        
        // 系统安全和工具
        "com.miui.securitycenter",           // MIUI安全中心
        "com.qihoo360.mobilesafe",           // 360手机卫士
        "com.tencent.qqpimsecure",           // 腾讯手机管家
        "com.cleanmaster.mguard",            // 猎豹清理大师
        
        // 输入法
        "com.sohu.inputmethod.sogou",        // 搜狗输入法
        "com.baidu.input",                   // 百度输入法
        "com.iflytek.inputmethod",           // 讯飞输入法
        "com.google.android.inputmethod.latin", // Google输入法
        
        // 云存储和同步
        "com.baidu.netdisk",                 // 百度网盘
        "com.tencent.weiyun",                // 腾讯微云
        "com.alibaba.android.apps.yunpan",  // 阿里云盘
        "com.dropbox.android",               // Dropbox
        "com.google.android.apps.docs",     // Google Drive
    )
    
    /**
     * 高频后台应用
     * 这些应用的后台活动非常频繁，需要最严格的过滤
     */
    private val HIGH_FREQUENCY_BACKGROUND_APPS = setOf(
        "com.tencent.mm",                    // 微信
        "com.eg.android.AlipayGphone",       // 支付宝
        "com.tencent.mobileqq",              // QQ
        "com.miui.securitycenter",           // MIUI安全中心
        "com.google.android.gms",            // Google Play Services
        
        // 新闻和内容应用（频繁后台推送和预载）
        "com.ss.android.article.news",      // 今日头条
        "com.ss.android.ugc.aweme",         // 抖音
        "com.tencent.news",                  // 腾讯新闻
        "com.UCMobile",                      // UC浏览器
    )
    
    /**
     * 检查应用是否为常驻后台应用
     */
    fun isBackgroundResidentApp(packageName: String): Boolean {
        return BACKGROUND_RESIDENT_APPS.contains(packageName)
    }
    
    /**
     * 检查应用是否为高频后台应用
     */
    fun isHighFrequencyBackgroundApp(packageName: String): Boolean {
        return HIGH_FREQUENCY_BACKGROUND_APPS.contains(packageName)
    }
    
    /**
     * 获取应用的最小有效使用时长（秒）
     * 根据应用类型返回不同的最小时长阈值
     */
    fun getMinimumValidDuration(packageName: String): Int {
        return when {
            // OffTimes应用本身，最小记录时长
            packageName.contains("offtime") || 
            packageName.contains("com.offtime") -> 1         // OffTimes: 1秒
            
            // 系统设置类应用，通常是快速操作
            packageName.contains("settings") -> 2            // 设置: 2秒
            
            // 浏览器类应用，降低最小时长阈值以减少数据丢失
            packageName.contains("browser") || 
            packageName.contains("chrome") -> 2              // 浏览器: 2秒 (降低阈值)
            
            // 高频后台应用需要更长时间
            isHighFrequencyBackgroundApp(packageName) -> 10  // 高频后台应用：10秒
            
            // 常驻后台应用
            isBackgroundResidentApp(packageName) -> 5        // 常驻后台应用：5秒
            
            // 其他普通应用
            else -> 2                                        // 普通应用：2秒
        }
    }
    
    /**
     * 获取应用的最大连续使用时长（分钟）
     * 超过此时长的会话可能包含大量后台时间，需要进一步过滤
     */
    fun getMaxContinuousUsage(packageName: String): Int {
        return when {
            isHighFrequencyBackgroundApp(packageName) -> 30  // 高频后台应用：30分钟
            isBackgroundResidentApp(packageName) -> 60       // 常驻后台应用：60分钟
            else -> 180                                       // 普通应用：3小时
        }
    }
    
    /**
     * 验证会话时长是否合理
     * 检测异常的长时间会话（如7小时+单次使用）
     */
    fun validateSessionDuration(
        packageName: String,
        durationSeconds: Int,
        sessionStartTime: Long,
        sessionEndTime: Long
    ): Boolean {
        val durationMinutes = durationSeconds / 60
        val durationHours = durationMinutes / 60.0
        
        // 检测明显异常的超长会话
        val suspiciousThreshold = when {
            isHighFrequencyBackgroundApp(packageName) -> 120  // 高频后台应用：2小时
            isBackgroundResidentApp(packageName) -> 180       // 常驻后台应用：3小时  
            else -> 360                                        // 普通应用：6小时
        }
        
        if (durationMinutes > suspiciousThreshold) {
            Log.w(TAG, "检测到可疑的超长会话: ${packageName}, 时长:${durationHours}小时")
            Log.w(TAG, "  开始时间: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(sessionStartTime)}")
            Log.w(TAG, "  结束时间: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(sessionEndTime)}")
            return false
        }
        
        return true
    }
    
    /**
     * 智能调整使用时长
     * 对于常驻后台应用，应用智能算法减少后台时间的影响
     */
    fun adjustUsageDuration(
        packageName: String,
        originalDuration: Int,
        sessionStartTime: Long,
        sessionEndTime: Long,
        context: Context? = null
    ): Int {
        if (!isBackgroundResidentApp(packageName)) {
            return originalDuration
        }
        
        val adjustedDuration = when {
            isHighFrequencyBackgroundApp(packageName) -> {
                // 高频后台应用：更激进的过滤
                adjustHighFrequencyAppDuration(packageName, originalDuration, sessionStartTime, sessionEndTime)
            }
            isBackgroundResidentApp(packageName) -> {
                // 常驻后台应用：适度过滤
                adjustBackgroundAppDuration(packageName, originalDuration, sessionStartTime, sessionEndTime)
            }
            else -> originalDuration
        }
        
        if (adjustedDuration != originalDuration) {
            Log.d(TAG, "调整应用使用时长: $packageName, 原始:${originalDuration}秒 -> 调整后:${adjustedDuration}秒")
        }
        
        return adjustedDuration
    }
    
    /**
     * 调整高频后台应用的使用时长
     */
    private fun adjustHighFrequencyAppDuration(
        packageName: String,
        originalDuration: Int,
        sessionStartTime: Long,
        sessionEndTime: Long
    ): Int {
        val maxContinuous = getMaxContinuousUsage(packageName) * 60 // 转换为秒
        
        return when {
            originalDuration <= 60 -> originalDuration  // 1分钟内：保持原样
            originalDuration <= 300 -> {                // 1-5分钟：轻微调整
                (originalDuration * 0.9).toInt()
            }
            originalDuration <= 1800 -> {               // 5-30分钟：中度调整
                (originalDuration * 0.7).toInt()
            }
            originalDuration <= maxContinuous -> {      // 30分钟-最大时长：大幅调整
                (originalDuration * 0.5).toInt()
            }
            else -> {                                   // 超过最大时长：严格限制
                (maxContinuous * 0.5).toInt()
            }
        }
    }
    
    /**
     * 调整常驻后台应用的使用时长
     */
    private fun adjustBackgroundAppDuration(
        packageName: String,
        originalDuration: Int,
        sessionStartTime: Long,
        sessionEndTime: Long
    ): Int {
        val maxContinuous = getMaxContinuousUsage(packageName) * 60 // 转换为秒
        
        return when {
            originalDuration <= 300 -> originalDuration // 5分钟内：保持原样
            originalDuration <= 1800 -> {              // 5-30分钟：轻微调整
                (originalDuration * 0.9).toInt()
            }
            originalDuration <= 3600 -> {              // 30-60分钟：中度调整
                (originalDuration * 0.8).toInt()
            }
            originalDuration <= maxContinuous -> {     // 60分钟-最大时长：适度调整
                (originalDuration * 0.7).toInt()
            }
            else -> {                                  // 超过最大时长：限制
                (maxContinuous * 0.7).toInt()
            }
        }
    }
    
    /**
     * 检测可能的后台唤醒模式
     * 分析使用会话的时间模式，识别可能的后台活动
     */
    fun detectBackgroundWakeupPattern(
        packageName: String,
        sessionDuration: Int,
        sessionStartTime: Long,
        sessionEndTime: Long
    ): Boolean {
        if (!isBackgroundResidentApp(packageName)) {
            return false
        }
        
        val hour = java.util.Calendar.getInstance().apply { 
            timeInMillis = sessionStartTime 
        }.get(java.util.Calendar.HOUR_OF_DAY)
        
        // 深夜/凌晨时段的长时间使用很可能是后台活动
        val isNightTime = hour in 0..5 || hour in 23..23
        val isLongSession = sessionDuration > getMaxContinuousUsage(packageName) * 60
        
        return isNightTime && isLongSession
    }
    
    /**
     * 获取应用的过滤级别
     */
    fun getFilterLevel(packageName: String): FilterLevel {
        return when {
            isHighFrequencyBackgroundApp(packageName) -> FilterLevel.HIGH
            isBackgroundResidentApp(packageName) -> FilterLevel.MEDIUM
            else -> FilterLevel.LOW
        }
    }
    
    /**
     * 过滤级别枚举
     */
    enum class FilterLevel {
        LOW,    // 普通应用，最小过滤
        MEDIUM, // 常驻后台应用，适度过滤
        HIGH    // 高频后台应用，严格过滤
    }
    
    /**
     * 生成应用使用统计的诊断信息
     */
    fun generateDiagnosticInfo(packageName: String): String {
        val isResident = isBackgroundResidentApp(packageName)
        val isHighFreq = isHighFrequencyBackgroundApp(packageName)
        val minDuration = getMinimumValidDuration(packageName)
        val maxContinuous = getMaxContinuousUsage(packageName)
        val filterLevel = getFilterLevel(packageName)
        
        return buildString {
            appendLine("=== 应用统计诊断 ===")
            appendLine("应用包名: $packageName")
            appendLine("常驻后台: $isResident")
            appendLine("高频后台: $isHighFreq")
            appendLine("最小有效时长: ${minDuration}秒")
            appendLine("最大连续时长: ${maxContinuous}分钟")
            appendLine("过滤级别: $filterLevel")
            appendLine("==================")
        }
    }
} 