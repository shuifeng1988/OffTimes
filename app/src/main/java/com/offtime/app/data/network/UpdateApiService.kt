package com.offtime.app.data.network

import com.offtime.app.data.model.AppUpdateInfo
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 应用更新API服务接口
 */
interface UpdateApiService {
    
    /**
     * 检查应用更新
     * @param platform 平台类型：alipay 或 googleplay
     * @param currentVersion 当前版本号
     * @param currentVersionCode 当前版本代码
     */
    @GET("api/check-update")
    suspend fun checkUpdate(
        @Query("platform") platform: String,
        @Query("current_version") currentVersion: String,
        @Query("current_version_code") currentVersionCode: Int
    ): Response<UpdateCheckResponse>
    
    /**
     * 获取最新版本信息（备用方案：解析网站HTML）
     */
    @GET(".")
    suspend fun getWebsiteContent(): Response<String>
}

/**
 * 更新检查响应数据类
 */
data class UpdateCheckResponse(
    val hasUpdate: Boolean,
    val updateInfo: AppUpdateInfo?
)
