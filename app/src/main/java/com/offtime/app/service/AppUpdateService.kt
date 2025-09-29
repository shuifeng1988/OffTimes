package com.offtime.app.service

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.offtime.app.BuildConfig
import com.offtime.app.data.model.AppUpdateInfo
import com.offtime.app.data.network.UpdateApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 应用更新服务
 * 负责检查应用更新、解析版本信息等
 */
@Singleton
class AppUpdateService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "AppUpdateService"
        private const val WEBSITE_BASE_URL = "https://www.offtimes.cn/"
        
        // 支付宝版本下载链接
        private const val ALIPAY_DOWNLOAD_URL = "${WEBSITE_BASE_URL}downloads/app-alipay-release.apk"
        // Google Play版本下载链接  
        private const val GOOGLEPLAY_DOWNLOAD_URL = "${WEBSITE_BASE_URL}downloads/app-googleplay-release.apk"
    }
    
    private val updateApiService: UpdateApiService by lazy {
        Retrofit.Builder()
            .baseUrl(WEBSITE_BASE_URL)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UpdateApiService::class.java)
    }
    
    /**
     * 检查应用更新
     */
    suspend fun checkForUpdate(): Result<AppUpdateInfo?> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔍 开始检查应用更新...")
            
            val currentVersionName = getCurrentVersionName()
            val currentVersionCode = getCurrentVersionCode()
            val platform = getCurrentPlatform()
            
            Log.d(TAG, "当前版本: $currentVersionName ($currentVersionCode), 平台: $platform")
            
            // 方案1: 尝试API接口（如果服务器支持）
            try {
                val apiResponse = updateApiService.checkUpdate(platform, currentVersionName, currentVersionCode)
                if (apiResponse.isSuccessful) {
                    val response = apiResponse.body()
                    if (response?.hasUpdate == true && response.updateInfo != null) {
                        Log.d(TAG, "✅ 通过API检测到更新: ${response.updateInfo.latestVersion}")
                        return@withContext Result.success(response.updateInfo)
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "API检查失败，使用备用方案: ${e.message}")
            }
            
            // 方案2: 解析网站HTML（备用方案）
            val websiteResponse = updateApiService.getWebsiteContent()
            if (websiteResponse.isSuccessful) {
                val htmlContent = websiteResponse.body()
                if (htmlContent != null) {
                    val updateInfo = parseWebsiteForUpdate(htmlContent, platform, currentVersionCode)
                    if (updateInfo != null) {
                        Log.d(TAG, "✅ 通过网站解析检测到更新: ${updateInfo.latestVersion}")
                        return@withContext Result.success(updateInfo)
                    } else {
                        Log.d(TAG, "✅ 当前已是最新版本")
                        return@withContext Result.success(null)
                    }
                }
            }
            
            Log.w(TAG, "⚠️ 无法获取更新信息")
            Result.failure(Exception("无法获取更新信息"))
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 检查更新失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 解析网站HTML获取更新信息
     */
    private fun parseWebsiteForUpdate(htmlContent: String, platform: String, currentVersionCode: Int): AppUpdateInfo? {
        try {
            val doc = Jsoup.parse(htmlContent)
            
            // 解析版本号 (例如: "最新版本 v1.4.6")
            val versionElement = doc.select("h3:contains(最新版本)").first()
            val versionText = versionElement?.text() ?: ""
            val versionPattern = Pattern.compile("v([0-9]+\\.[0-9]+\\.[0-9]+)")
            val versionMatcher = versionPattern.matcher(versionText)
            
            if (!versionMatcher.find()) {
                Log.w(TAG, "无法解析版本号: $versionText")
                return null
            }
            
            val latestVersion = versionMatcher.group(1) ?: return null
            
            // 获取当前版本信息进行比较
            val currentVersion = getCurrentVersionName()
            
            Log.d(TAG, "版本比较: 当前=$currentVersion (code=$currentVersionCode), 网站=$latestVersion")
            
            // 使用字符串版本比较而不是简单的数字计算
            val versionComparison = compareVersionStrings(currentVersion.replace("-alipay", "").replace("-gplay", ""), latestVersion)
            
            Log.d(TAG, "版本比较结果: $versionComparison (1=网站更新, 0=相同, -1=当前更新)")
            
            // 只有当网站版本更高时才返回更新信息
            if (versionComparison >= 0) {
                Log.d(TAG, "当前版本 >= 网站版本，无需更新")
                return null // 没有更新
            }
            
            // 计算网站版本的版本代码（用于显示）
            val versionParts = latestVersion.split(".")
            val latestVersionCode = if (versionParts.size >= 3) {
                (versionParts[0].toIntOrNull() ?: 1) * 100 + 
                (versionParts[1].toIntOrNull() ?: 4) * 10 + 
                (versionParts[2].toIntOrNull() ?: 6)
            } else {
                currentVersionCode + 1
            }
            
            // 解析发布日期
            val dateElement = doc.select("p.version-date").first()
            val releaseDate = dateElement?.text()?.replace("发布日期：", "") ?: ""
            
            // 解析更新说明
            val featuresElement = doc.select("div.version-features ul").first()
            val releaseNotes = featuresElement?.select("li")?.joinToString("\n") { "• ${it.text()}" } ?: "版本更新"
            
            // 解析文件大小
            val fileSizePattern = if (platform == "alipay") {
                Pattern.compile("通用版本 \\(([^)]+)\\)")
            } else {
                Pattern.compile("Google Play版.*?\\(([^)]+)\\)")
            }
            val fileSizeMatcher = fileSizePattern.matcher(htmlContent)
            val fileSize = if (fileSizeMatcher.find()) {
                fileSizeMatcher.group(1) ?: "未知"
            } else {
                if (platform == "alipay") "3.5MB" else "5.2MB" // 默认值
            }
            
            // 确定下载链接
            val downloadUrl = if (platform == "alipay") ALIPAY_DOWNLOAD_URL else GOOGLEPLAY_DOWNLOAD_URL
            
            return AppUpdateInfo(
                latestVersion = latestVersion,
                latestVersionCode = latestVersionCode,
                downloadUrl = downloadUrl,
                releaseNotes = releaseNotes,
                releaseDate = releaseDate,
                fileSize = fileSize,
                isForceUpdate = false // 从网站解析暂不支持强制更新标识
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "解析网站内容失败", e)
            return null
        }
    }
    
    /**
     * 获取当前应用版本名称
     */
    private fun getCurrentVersionName(): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            Log.e(TAG, "获取版本名称失败", e)
            "1.0.0"
        }
    }
    
    /**
     * 获取当前应用版本代码
     */
    private fun getCurrentVersionCode(): Int {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取版本代码失败", e)
            1
        }
    }
    
    /**
     * 获取当前平台类型
     */
    private fun getCurrentPlatform(): String {
        return if (BuildConfig.FLAVOR == "alipay") "alipay" else "googleplay"
    }
    
    /**
     * 比较版本字符串
     * @return 1: version1 > version2, 0: 相等, -1: version1 < version2
     */
    private fun compareVersionStrings(version1: String, version2: String): Int {
        val v1Parts = version1.split(".").map { it.toIntOrNull() ?: 0 }
        val v2Parts = version2.split(".").map { it.toIntOrNull() ?: 0 }
        
        val maxLength = maxOf(v1Parts.size, v2Parts.size)
        
        for (i in 0 until maxLength) {
            val v1Part = v1Parts.getOrElse(i) { 0 }
            val v2Part = v2Parts.getOrElse(i) { 0 }
            
            when {
                v1Part > v2Part -> return 1
                v1Part < v2Part -> return -1
            }
        }
        
        return 0
    }
    
    /**
     * 检查是否允许应用内更新
     * Google Play版本不允许，支付宝版本允许
     */
    fun isInAppUpdateAllowed(): Boolean {
        return BuildConfig.FLAVOR == "alipay"
    }
}
