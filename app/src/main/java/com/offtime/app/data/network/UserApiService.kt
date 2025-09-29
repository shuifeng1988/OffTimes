package com.offtime.app.data.network

import retrofit2.Response
import retrofit2.http.*
import com.google.gson.annotations.SerializedName

/**
 * 用户账户API服务接口
 * 定义与服务器交互的所有用户相关API端点
 * 
 * 基础URL将在RetrofitModule中配置
 * 所有API都使用标准的RESTful设计
 */
interface UserApiService {
    
    /**
     * 发送短信验证码
     * @param request 发送验证码请求体
     * @return API响应结果
     */
    @POST("auth/send-sms")
    suspend fun sendSmsCode(@Body request: SendSmsCodeRequest): Response<ApiResponse<Unit>>
    
    /**
     * 验证短信验证码
     * @param request 验证码验证请求体
     * @return API响应结果，包含验证token
     */
    @POST("auth/verify-sms")
    suspend fun verifySmsCode(@Body request: VerifySmsCodeRequest): Response<ApiResponse<VerifyCodeResponse>>
    
    /**
     * 用户注册
     * @param request 注册请求体
     * @return API响应结果，包含用户信息和token
     */
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<AuthResponse>>
    
    /**
     * 用户注册（无需短信验证码）
     * @param request 注册请求体
     * @return API响应结果，包含用户信息和token
     */
    @POST("auth/register-no-sms")
    suspend fun registerWithoutSms(@Body request: RegisterWithoutSmsRequest): Response<ApiResponse<AuthResponse>>
    
    /**
     * 密码登录
     * @param request 密码登录请求体
     * @return API响应结果，包含用户信息和token
     */
    @POST("auth/login")
    suspend fun loginWithPassword(@Body request: LoginRequest): Response<ApiResponse<AuthResponse>>
    
    /**
     * 验证码登录
     * @param request 验证码登录请求体
     * @return API响应结果，包含用户信息和token
     */
    @POST("auth/login-sms")
    suspend fun loginWithSmsCode(@Body request: SmsLoginRequest): Response<ApiResponse<AuthResponse>>
    
    /**
     * SMS登录（合并注册和登录）
     * @param request SMS登录请求体
     * @return API响应结果，包含用户信息和token
     */
    @POST("auth/sms-login")
    suspend fun smsLogin(@Body request: DirectSmsLoginRequest): Response<ApiResponse<AuthResponse>>
    
    /**
     * 支付宝登录
     * @param request 支付宝登录请求体
     * @return API响应结果，包含用户信息和token
     */
    @POST("auth/login-alipay")
    suspend fun loginWithAlipay(@Body request: AlipayLoginRequest): Response<ApiResponse<AuthResponse>>
    
    /**
     * Google登录
     * @param request Google登录请求体
     * @return API响应结果，包含用户信息和token
     */
    @POST("auth/login-google")
    suspend fun loginWithGoogle(@Body request: GoogleLoginRequest): Response<ApiResponse<AuthResponse>>
    
    /**
     * 刷新访问令牌
     * @param request 刷新token请求体
     * @return API响应结果，包含新的token信息
     */
    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<ApiResponse<TokenResponse>>
    
    /**
     * 用户登出
     * @param authorization 认证头，格式为 "Bearer {token}"
     * @return API响应结果
     */
    @POST("auth/logout")
    suspend fun logout(@Header("Authorization") authorization: String): Response<ApiResponse<Unit>>
    
    /**
     * 获取用户信息
     * @param authorization 认证头
     * @return API响应结果，包含用户详细信息
     */
    @GET("user/profile")
    suspend fun getUserProfile(@Header("Authorization") authorization: String): Response<ApiResponse<UserProfileResponse>>
    
    /**
     * 更新用户信息
     * @param authorization 认证头
     * @param request 更新用户信息请求体
     * @return API响应结果
     */
    @PUT("user/profile")
    suspend fun updateUserProfile(
        @Header("Authorization") authorization: String,
        @Body request: UpdateProfileRequest
    ): Response<ApiResponse<UserProfileResponse>>
    
    /**
     * 修改密码
     * @param authorization 认证头
     * @param request 修改密码请求体
     * @return API响应结果
     */
    @PUT("user/password")
    suspend fun changePassword(
        @Header("Authorization") authorization: String,
        @Body request: ChangePasswordRequest
    ): Response<ApiResponse<Unit>>
    
    // ===== 数据备份相关API =====
    
    /**
     * 上传用户数据备份
     * @param authorization 认证头
     * @param request 备份上传请求体
     * @return API响应结果
     */
    @POST("backup/upload")
    suspend fun uploadBackup(
        @Header("Authorization") authorization: String,
        @Body request: BackupUploadRequest
    ): Response<ApiResponse<BackupUploadResponse>>
    
    /**
     * 下载用户数据备份
     * @param authorization 认证头
     * @param tableName 表名（可选）
     * @param dateFrom 开始日期（可选）
     * @param dateTo 结束日期（可选）
     * @return API响应结果
     */
    @GET("backup/download")
    suspend fun downloadBackup(
        @Header("Authorization") authorization: String,
        @Query("table_name") tableName: String? = null,
        @Query("date_from") dateFrom: String? = null,
        @Query("date_to") dateTo: String? = null
    ): Response<ApiResponse<BackupDownloadResponse>>
    
    /**
     * 获取用户备份信息概览
     * @param authorization 认证头
     * @return API响应结果
     */
    @GET("backup/info")
    suspend fun getBackupInfo(
        @Header("Authorization") authorization: String
    ): Response<ApiResponse<BackupInfoResponse>>
    
    /**
     * 更新用户备份设置
     * @param authorization 认证头
     * @param request 备份设置更新请求体
     * @return API响应结果
     */
    @PUT("backup/settings")
    suspend fun updateBackupSettings(
        @Header("Authorization") authorization: String,
        @Body request: BackupSettingsRequest
    ): Response<ApiResponse<Unit>>
    
    /**
     * 清空用户所有备份数据
     * @param authorization 认证头
     * @return API响应结果
     */
    @DELETE("backup/clear")
    suspend fun clearBackupData(
        @Header("Authorization") authorization: String
    ): Response<ApiResponse<BackupClearResponse>>
    
    // ===== 购买验证相关API =====
    
    /**
     * 验证购买收据
     * @param authorization 认证头
     * @param request 购买验证请求体
     * @return API响应结果
     */
    @POST("purchase/verify")
    suspend fun verifyPurchase(
        @Header("Authorization") authorization: String,
        @Body request: PurchaseVerificationRequest
    ): Response<ApiResponse<PurchaseVerificationResponse>>
    
    /**
     * 获取用户付费状态
     * @param authorization 认证头
     * @return API响应结果
     */
    @GET("purchase/status")
    suspend fun getPurchaseStatus(
        @Header("Authorization") authorization: String
    ): Response<ApiResponse<PurchaseStatusResponse>>
    
    /**
     * 恢复购买
     * @param authorization 认证头
     * @return API响应结果
     */
    @POST("purchase/restore")
    suspend fun restorePurchases(
        @Header("Authorization") authorization: String
    ): Response<ApiResponse<PurchaseRestoreResponse>>
    
    /**
     * 检查Google Play配置状态
     * @return API响应结果
     */
    @GET("purchase/config-status")
    suspend fun getGooglePlayConfigStatus(): Response<ApiResponse<GooglePlayConfigResponse>>
}

// API请求和响应数据类

/**
 * 统一API响应格式
 */
data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T? = null,
    val success: Boolean = code == 200
)

/**
 * 发送短信验证码请求
 */
data class SendSmsCodeRequest(
    val phoneNumber: String,
    val type: String // "register" 或 "login"
)

/**
 * 验证短信验证码请求
 */
data class VerifySmsCodeRequest(
    val phoneNumber: String,
    val code: String,
    val type: String
)

/**
 * 验证码验证响应
 */
data class VerifyCodeResponse(
    val verifyToken: String,
    val expiresIn: Long
)

/**
 * 用户注册请求
 */
data class RegisterRequest(
    val phoneNumber: String,
    val password: String,
    val verifyToken: String,
    val nickname: String = ""
)

/**
 * 用户注册请求（无需短信验证码）
 */
data class RegisterWithoutSmsRequest(
    val phoneNumber: String,
    val password: String,
    val nickname: String = ""
)

/**
 * 密码登录请求
 */
data class LoginRequest(
    val phoneNumber: String,
    val password: String
)

/**
 * 验证码登录请求
 */
data class SmsLoginRequest(
    val phoneNumber: String,
    val verifyToken: String
)

/**
 * 直接SMS登录请求（合并注册和登录）
 */
data class DirectSmsLoginRequest(
    val phoneNumber: String,
    val code: String,
    val nickname: String? = null
)

/**
 * 支付宝登录请求
 */
data class AlipayLoginRequest(
    val alipayUserId: String,
    val authCode: String,
    val nickname: String,
    val avatar: String
)

/**
 * Google登录请求
 */
data class GoogleLoginRequest(
    val googleId: String,
    val email: String,
    val name: String,
    val photoUrl: String,
    val idToken: String
)

/**
 * 认证响应（登录/注册成功）
 */
data class AuthResponse(
    val user: UserProfileResponse,
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long
)

/**
 * 刷新token请求
 */
data class RefreshTokenRequest(
    val refreshToken: String
)

/**
 * Token响应
 */
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long
)

/**
 * 用户信息响应
 */
data class UserProfileResponse(
    @SerializedName("user_id")
    val userId: String = "",
    @SerializedName("phone_number")
    val phoneNumber: String = "",
    val nickname: String = "",
    val avatar: String = "",
    @SerializedName("register_time")
    val registerTime: Long = 0L,
    @SerializedName("last_login_time")
    val lastLoginTime: Long = 0L
)

/**
 * 更新用户信息请求
 */
data class UpdateProfileRequest(
    val nickname: String,
    val avatar: String
)

/**
 * 修改密码请求
 */
data class ChangePasswordRequest(
    val oldPassword: String,
    val newPassword: String
)

// ===== 备份相关数据类 =====

/**
 * 备份上传请求
 */
data class BackupUploadRequest(
    val tableName: String,
    val backupData: List<Any>,
    val backupDate: String
)

/**
 * 备份上传响应
 */
data class BackupUploadResponse(
    @SerializedName("table_name")
    val tableName: String,
    @SerializedName("backup_date")
    val backupDate: String,
    @SerializedName("record_count")
    val recordCount: Int,
    @SerializedName("file_size")
    val fileSize: Int,
    @SerializedName("data_hash")
    val dataHash: String,
    val skipped: Boolean = false,
    val unchanged: Boolean = false
)

/**
 * 备份下载响应
 */
data class BackupDownloadResponse(
    val backups: List<BackupFileInfo>,
    val totalCount: Int,
    val queryParams: BackupQueryParams
)

/**
 * 备份文件信息
 */
data class BackupFileInfo(
    @SerializedName("table_name")
    val tableName: String,
    @SerializedName("backup_date")
    val backupDate: String,
    @SerializedName("backup_data")
    val backupData: List<Any>,
    @SerializedName("record_count")
    val recordCount: Int,
    @SerializedName("data_hash")
    val dataHash: String
)

/**
 * 备份查询参数
 */
data class BackupQueryParams(
    val tableName: String?,
    val dateFrom: String?,
    val dateTo: String?
)

/**
 * 备份信息概览响应
 */
data class BackupInfoResponse(
    val settings: BackupSettingsInfo,
    val tableStats: List<BackupTableStat>
)

/**
 * 备份设置信息
 */
data class BackupSettingsInfo(
    @SerializedName("backup_enabled")
    val backupEnabled: Boolean,
    @SerializedName("backup_time")
    val backupTime: String,
    @SerializedName("last_backup_date")
    val lastBackupDate: String?,
    @SerializedName("total_backups")
    val totalBackups: Int,
    @SerializedName("total_data_size")
    val totalDataSize: Long
)

/**
 * 备份表统计信息
 */
data class BackupTableStat(
    @SerializedName("table_name")
    val tableName: String,
    @SerializedName("backup_count")
    val backupCount: Int,
    @SerializedName("latest_backup")
    val latestBackup: String?,
    @SerializedName("total_records")
    val totalRecords: Int,
    @SerializedName("total_size")
    val totalSize: Long
)

/**
 * 备份设置更新请求
 */
data class BackupSettingsRequest(
    val backupEnabled: Boolean? = null,
    val backupTimeHour: Int? = null,
    val backupTimeMinute: Int? = null
)

/**
 * 清空备份响应
 */
data class BackupClearResponse(
    val deletedCount: Int
)

// ===== 购买验证相关数据类 =====

/**
 * 购买验证请求
 */
data class PurchaseVerificationRequest(
    val platform: String, // "google_play" 或 "alipay"
    val productId: String,
    val purchaseToken: String,
    val orderId: String? = null
)

/**
 * 购买验证响应
 */
data class PurchaseVerificationResponse(
    val subscriptionId: String,
    val status: String,
    val purchaseTime: String,
    val expiryTime: String?,
    val isValid: Boolean
)

/**
 * 购买状态响应
 */
data class PurchaseStatusResponse(
    val isPremium: Boolean,
    val premiumExpiresAt: String?,
    val subscriptions: List<SubscriptionInfo>,
    val latestSubscription: SubscriptionInfo?
)

/**
 * 订阅信息
 */
data class SubscriptionInfo(
    val id: String,
    val platform: String,
    val productId: String,
    val purchaseTime: String,
    val expiryTime: String?,
    val status: String
)

/**
 * 购买恢复响应
 */
data class PurchaseRestoreResponse(
    val restoredCount: Int,
    val totalSubscriptions: Int,
    val isPremium: Boolean,
    val validSubscriptions: List<SubscriptionInfo>
)

/**
 * Google Play配置状态响应
 */
data class GooglePlayConfigResponse(
    val isConfigured: Boolean,
    val hasServiceAccountKey: Boolean,
    val hasPackageName: Boolean,
    val canInitialize: Boolean,
    val errorMessage: String?
) 