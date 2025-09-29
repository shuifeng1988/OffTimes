package com.offtime.app.data.model

/**
 * 应用更新信息数据类
 */
data class AppUpdateInfo(
    val latestVersion: String,           // 最新版本号，如 "1.4.7"
    val latestVersionCode: Int,          // 最新版本代码，如 32
    val downloadUrl: String,             // 下载链接
    val releaseNotes: String,            // 更新说明
    val releaseDate: String,             // 发布日期
    val fileSize: String,                // 文件大小，如 "3.5MB"
    val isForceUpdate: Boolean = false,  // 是否强制更新
    val minSupportedVersion: String? = null  // 最低支持版本
) {
    /**
     * 检查是否有新版本
     */
    fun hasUpdate(currentVersionCode: Int): Boolean {
        return latestVersionCode > currentVersionCode
    }
    
    /**
     * 检查是否需要强制更新
     */
    fun needForceUpdate(currentVersionCode: Int): Boolean {
        return hasUpdate(currentVersionCode) && isForceUpdate
    }
    
    /**
     * 检查当前版本是否被支持
     */
    fun isCurrentVersionSupported(currentVersion: String): Boolean {
        if (minSupportedVersion == null) return true
        return compareVersions(currentVersion, minSupportedVersion) >= 0
    }
    
    /**
     * 比较版本号
     * @return 1: version1 > version2, 0: 相等, -1: version1 < version2
     */
    private fun compareVersions(version1: String, version2: String): Int {
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
}
