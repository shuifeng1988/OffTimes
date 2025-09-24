package com.offtime.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.offtime.app.BuildConfig
import com.offtime.app.data.dao.UserDao
import com.offtime.app.data.entity.UserEntity
import com.offtime.app.data.network.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.security.MessageDigest
import java.util.*
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * 用户仓库类
 * 管理用户相关的所有数据操作，包括本地数据库和远程API调用
 * 
 * 主要功能：
 * - 用户注册和登录
 * - 用户信息管理
 * - 身份验证token管理
 * - 本地和远程数据同步
 */
@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val userApiService: UserApiService,
    private val context: Context,
    @Named("google") private val googleLoginManager: com.offtime.app.manager.interfaces.LoginManager? = null
) {
    
    companion object {
        private const val PREFS_NAME = "user_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_TOKEN_EXPIRES_AT = "token_expires_at"
    }
    
    private val sharedPrefs: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * 获取当前登录用户的Flow
     */
    fun getCurrentUserFlow(): Flow<UserEntity?> = userDao.getCurrentUserFlow()
    
    /**
     * 获取当前登录用户
     */
    suspend fun getCurrentUser(): UserEntity? = userDao.getCurrentUser()
    
    /**
     * 检查用户是否已登录
     */
    suspend fun isUserLoggedIn(): Boolean {
        val user = getCurrentUser()
        val hasValidToken = !getAccessToken().isNullOrEmpty()
        return user != null && user.isLoggedIn && hasValidToken
    }
    
    /**
     * 获取访问令牌（公共方法，供BackupManager等使用）
     */
    fun getAccessToken(): String? {
        return sharedPrefs.getString(KEY_ACCESS_TOKEN, null)
    }
    
    /**
     * 发送短信验证码
     * @param phoneNumber 手机号
     * @param type 验证码类型（register/login）
     * @return 操作结果
     */
    suspend fun sendSmsCode(phoneNumber: String, type: String): Result<Unit> {
        return try {
            val response = userApiService.sendSmsCode(
                SendSmsCodeRequest(phoneNumber, type)
            )
            
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to send SMS code"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 验证短信验证码
     * @param phoneNumber 手机号
     * @param code 验证码
     * @param type 验证码类型
     * @return 验证结果，包含验证token
     */
    suspend fun verifySmsCode(phoneNumber: String, code: String, type: String): Result<String> {
        return try {
            val response = userApiService.verifySmsCode(
                VerifySmsCodeRequest(phoneNumber, code, type)
            )
            
            if (response.isSuccessful && response.body()?.success == true) {
                val verifyToken = response.body()?.data?.verifyToken
                if (verifyToken != null) {
                    Result.success(verifyToken)
                } else {
                    Result.failure(Exception("验证token为空"))
                }
            } else {
                Result.failure(Exception(response.body()?.message ?: "验证码验证失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 用户注册
     * @param phoneNumber 手机号
     * @param password 密码
     * @param verifyToken 验证token
     * @param nickname 昵称
     * @return 注册结果
     */
    suspend fun register(
        phoneNumber: String, 
        password: String, 
        verifyToken: String,
        nickname: String = ""
    ): Result<UserEntity> {
        return try {
            // 检查手机号是否已存在
            if (userDao.isPhoneNumberExists(phoneNumber)) {
                return Result.failure(Exception("该手机号已注册"))
            }
            
            val response = userApiService.register(
                RegisterRequest(phoneNumber, password, verifyToken, nickname)
            )
            
            if (response.isSuccessful && response.body()?.success == true) {
                val authResponse = response.body()?.data!!
                
                // 保存token
                saveTokens(authResponse.accessToken, authResponse.refreshToken, authResponse.expiresIn)
                
                // 创建本地用户实体，新用户开始7天免费试用
                val currentTime = System.currentTimeMillis()
                val user = UserEntity(
                    userId = UUID.randomUUID().toString(),
                    phoneNumber = phoneNumber,
                    passwordHash = hashPassword(password),
                    nickname = authResponse.user.nickname,
                    avatar = authResponse.user.avatar,
                    isLoggedIn = true,
                    lastLoginTime = currentTime,
                    registerTime = authResponse.user.registerTime,
                    serverUserId = authResponse.user.userId,
                    isDataSyncEnabled = true,
                    isPremium = false,
                    trialStartTime = currentTime,
                    subscriptionStatus = UserEntity.STATUS_TRIAL,
                    paymentTime = 0L,
                    paymentAmount = 0,
                    alipayUserId = ""
                )
                
                // 先登出所有用户，再保存新用户
                userDao.logoutAllUsers()
                userDao.insertUser(user)
                
                Result.success(user)
            } else {
                Result.failure(Exception(response.body()?.message ?: "注册失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 用户注册（无需短信验证码）
     * @param phoneNumber 手机号
     * @param password 密码
     * @param nickname 昵称
     * @return 注册结果
     */
    suspend fun registerWithoutSms(
        phoneNumber: String, 
        password: String,
        nickname: String = ""
    ): Result<UserEntity> {
        return try {
            // 检查手机号是否已存在
            if (userDao.isPhoneNumberExists(phoneNumber)) {
                return Result.failure(Exception("该手机号已注册"))
            }
            
            val response = userApiService.registerWithoutSms(
                RegisterWithoutSmsRequest(phoneNumber, password, nickname)
            )
            
            if (response.isSuccessful && response.body()?.success == true) {
                val authResponse = response.body()?.data!!
                
                // 保存token
                saveTokens(authResponse.accessToken, authResponse.refreshToken, authResponse.expiresIn)
                
                // 创建本地用户实体，新用户开始7天免费试用
                val currentTime = System.currentTimeMillis()
                val user = UserEntity(
                    userId = UUID.randomUUID().toString(),
                    phoneNumber = phoneNumber,
                    passwordHash = hashPassword(password),
                    nickname = authResponse.user.nickname,
                    avatar = authResponse.user.avatar,
                    isLoggedIn = true,
                    lastLoginTime = currentTime,
                    registerTime = authResponse.user.registerTime,
                    serverUserId = authResponse.user.userId,
                    isDataSyncEnabled = true,
                    isPremium = false,
                    trialStartTime = currentTime,
                    subscriptionStatus = UserEntity.STATUS_TRIAL,
                    paymentTime = 0L,
                    paymentAmount = 0,
                    alipayUserId = ""
                )
                
                // 先登出所有用户，再保存新用户
                userDao.logoutAllUsers()
                userDao.insertUser(user)
                
                Result.success(user)
            } else {
                Result.failure(Exception(response.body()?.message ?: "注册失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 密码登录
     * @param phoneNumber 手机号
     * @param password 密码
     * @return 登录结果
     */
    suspend fun loginWithPassword(phoneNumber: String, password: String): Result<UserEntity> {
        return try {
            val response = userApiService.loginWithPassword(
                LoginRequest(phoneNumber, password)
            )
            
            if (response.isSuccessful && response.body()?.success == true) {
                val authResponse = response.body()?.data!!
                
                // 保存token
                saveTokens(authResponse.accessToken, authResponse.refreshToken, authResponse.expiresIn)
                
                // 更新或创建本地用户
                val existingUser = userDao.getUserByPhoneNumber(phoneNumber)
                val user = if (existingUser != null) {
                    existingUser.copy(
                        passwordHash = hashPassword(password),
                        nickname = authResponse.user.nickname,
                        avatar = authResponse.user.avatar,
                        isLoggedIn = true,
                        lastLoginTime = System.currentTimeMillis(),
                        serverUserId = authResponse.user.userId,
                        isDataSyncEnabled = true
                    )
                } else {
                    // 新用户开始7天免费试用
                    val currentTime = System.currentTimeMillis()
                    UserEntity(
                        userId = UUID.randomUUID().toString(),
                        phoneNumber = phoneNumber,
                        passwordHash = hashPassword(password),
                        nickname = authResponse.user.nickname,
                        avatar = authResponse.user.avatar,
                        isLoggedIn = true,
                        lastLoginTime = currentTime,
                        registerTime = authResponse.user.registerTime,
                        serverUserId = authResponse.user.userId,
                        isDataSyncEnabled = true,
                        isPremium = false,
                        trialStartTime = currentTime,
                        subscriptionStatus = UserEntity.STATUS_TRIAL,
                        paymentTime = 0L,
                        paymentAmount = 0,
                        alipayUserId = ""
                    )
                }
                
                // 先登出所有用户，再更新当前用户
                userDao.logoutAllUsers()
                userDao.insertUser(user)
                
                Result.success(user)
            } else {
                Result.failure(Exception(response.body()?.message ?: "登录失败"))
            }
        } catch (e: Exception) {
            // 支付宝版本：网络请求失败时，检查是否是测试账号
            if (BuildConfig.ENABLE_PASSWORD_LOGIN && 
                (phoneNumber == "13800138000" || phoneNumber == "admin") && 
                password == "offtime123") {
                try {
                    // 先登出所有用户
                    userDao.logoutAllUsers()
                    
                    // 创建测试用户
                    val testUser = UserEntity(
                        userId = "test_user_id",
                        phoneNumber = phoneNumber,
                        passwordHash = hashPassword(password),
                        nickname = if (phoneNumber == "admin") "管理员" else "测试用户",
                        avatar = "",
                        isLoggedIn = true,
                        lastLoginTime = System.currentTimeMillis(),
                        registerTime = System.currentTimeMillis(),
                        serverUserId = "test_server_id",
                        isDataSyncEnabled = false,
                        isPremium = true,  // 测试用户设为付费用户
                        trialStartTime = System.currentTimeMillis(),
                        subscriptionStatus = UserEntity.STATUS_PREMIUM,
                        paymentTime = System.currentTimeMillis(),
                        paymentAmount = 990,  // 9.9元，单位：分
                        alipayUserId = ""
                    )
                    
                    userDao.insertUser(testUser)
                    Result.success(testUser)
                } catch (dbException: Exception) {
                    Result.failure(dbException)
                }
            } else {
                Result.failure(e)
            }
        }
    }
    
    /**
     * 直接SMS登录（合并注册和登录）
     * @param phoneNumber 手机号
     * @param code 验证码
     * @param nickname 昵称（可选）
     * @return 登录结果
     */
    suspend fun directSmsLogin(phoneNumber: String, code: String, nickname: String? = null): Result<UserEntity> {
        return try {
            val response = userApiService.smsLogin(
                DirectSmsLoginRequest(phoneNumber, code, nickname)
            )
            
            if (response.isSuccessful && response.body()?.success == true) {
                val authResponse = response.body()?.data!!
                
                // 保存token
                saveTokens(authResponse.accessToken, authResponse.refreshToken, authResponse.expiresIn)
                
                // 更新或创建本地用户
                val existingUser = userDao.getUserByPhoneNumber(phoneNumber)
                val user = if (existingUser != null) {
                    existingUser.copy(
                        nickname = authResponse.user.nickname,
                        avatar = authResponse.user.avatar,
                        isLoggedIn = true,
                        lastLoginTime = System.currentTimeMillis(),
                        serverUserId = authResponse.user.userId,
                        isDataSyncEnabled = true
                    )
                } else {
                    // 新用户开始7天免费试用
                    val currentTime = System.currentTimeMillis()
                    UserEntity(
                        userId = UUID.randomUUID().toString(),
                        phoneNumber = phoneNumber,
                        passwordHash = "",
                        nickname = authResponse.user.nickname,
                        avatar = authResponse.user.avatar,
                        isLoggedIn = true,
                        lastLoginTime = currentTime,
                        registerTime = authResponse.user.registerTime,
                        serverUserId = authResponse.user.userId,
                        isDataSyncEnabled = true,
                        isPremium = false,
                        trialStartTime = currentTime,
                        subscriptionStatus = UserEntity.STATUS_TRIAL,
                        paymentTime = 0L,
                        paymentAmount = 0,
                        alipayUserId = ""
                    )
                }
                
                // 保存用户到数据库
                userDao.insertUser(user)
                
                Result.success(user)
            } else {
                val errorMessage = response.body()?.message ?: "登录失败"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 验证码登录
     * @param phoneNumber 手机号
     * @param verifyToken 验证token
     * @return 登录结果
     */
    suspend fun loginWithSmsCode(phoneNumber: String, verifyToken: String): Result<UserEntity> {
        return try {
            val response = userApiService.loginWithSmsCode(
                SmsLoginRequest(phoneNumber, verifyToken)
            )
            
            if (response.isSuccessful && response.body()?.success == true) {
                val authResponse = response.body()?.data!!
                
                // 保存token
                saveTokens(authResponse.accessToken, authResponse.refreshToken, authResponse.expiresIn)
                
                // 更新或创建本地用户
                val existingUser = userDao.getUserByPhoneNumber(phoneNumber)
                val user = if (existingUser != null) {
                    existingUser.copy(
                        nickname = authResponse.user.nickname,
                        avatar = authResponse.user.avatar,
                        isLoggedIn = true,
                        lastLoginTime = System.currentTimeMillis(),
                        serverUserId = authResponse.user.userId,
                        isDataSyncEnabled = true
                    )
                } else {
                    // 新用户开始7天免费试用
                    val currentTime = System.currentTimeMillis()
                    UserEntity(
                        userId = UUID.randomUUID().toString(),
                        phoneNumber = phoneNumber,
                        passwordHash = "",
                        nickname = authResponse.user.nickname,
                        avatar = authResponse.user.avatar,
                        isLoggedIn = true,
                        lastLoginTime = currentTime,
                        registerTime = authResponse.user.registerTime,
                        serverUserId = authResponse.user.userId,
                        isDataSyncEnabled = true,
                        isPremium = false,
                        trialStartTime = currentTime,
                        subscriptionStatus = UserEntity.STATUS_TRIAL,
                        paymentTime = 0L,
                        paymentAmount = 0,
                        alipayUserId = ""
                    )
                }
                
                // 先登出所有用户，再更新当前用户
                userDao.logoutAllUsers()
                userDao.insertUser(user)
                
                Result.success(user)
            } else {
                Result.failure(Exception(response.body()?.message ?: "登录失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 支付宝登录
     * @param alipayUserId 支付宝用户ID
     * @param authCode 授权码
     * @param nickname 用户昵称
     * @param avatar 头像URL
     * @return 登录结果
     */
    suspend fun loginWithAlipay(
        alipayUserId: String,
        authCode: String,
        nickname: String,
        avatar: String
    ): Result<UserEntity> {
        return try {
            val response = userApiService.loginWithAlipay(
                AlipayLoginRequest(alipayUserId, authCode, nickname, avatar)
            )
            
            if (response.isSuccessful && response.body()?.success == true) {
                val authResponse = response.body()?.data!!
                
                // 保存token
                saveTokens(authResponse.accessToken, authResponse.refreshToken, authResponse.expiresIn)
                
                // 检查是否已有本地用户（通过支付宝用户ID）
                val existingUser = userDao.getUserByAlipayUserId(alipayUserId)
                val user = if (existingUser != null) {
                    existingUser.copy(
                        nickname = nickname,
                        avatar = avatar,
                        isLoggedIn = true,
                        lastLoginTime = System.currentTimeMillis(),
                        serverUserId = authResponse.user.userId.takeIf { it.isNotEmpty() } ?: existingUser.serverUserId,
                        isDataSyncEnabled = true
                    )
                } else {
                    // 新用户开始7天免费试用
                    val currentTime = System.currentTimeMillis()
                    UserEntity(
                        userId = UUID.randomUUID().toString(),
                        phoneNumber = "", // 支付宝登录不需要手机号
                        passwordHash = "",
                        nickname = nickname,
                        avatar = avatar,
                        isLoggedIn = true,
                        lastLoginTime = currentTime,
                        registerTime = currentTime,
                        serverUserId = authResponse.user.userId.takeIf { it.isNotEmpty() } ?: "",
                        isDataSyncEnabled = true,
                        isPremium = false,
                        trialStartTime = currentTime,
                        subscriptionStatus = UserEntity.STATUS_TRIAL,
                        paymentTime = 0L,
                        paymentAmount = 0,
                        alipayUserId = alipayUserId
                    )
                }
                
                // 先登出所有用户，再更新当前用户
                userDao.logoutAllUsers()
                userDao.insertUser(user)
                
                Result.success(user)
            } else {
                Result.failure(Exception(response.body()?.message ?: "支付宝登录失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Google登录
     * @param googleId Google用户ID
     * @param email 邮箱地址
     * @param name 用户姓名
     * @param photoUrl 头像URL
     * @param idToken ID Token
     * @return 登录结果
     */
    suspend fun loginWithGoogle(
        googleId: String,
        email: String,
        name: String,
        photoUrl: String,
        idToken: String
    ): Result<UserEntity> {
        return try {
            val response = userApiService.loginWithGoogle(
                GoogleLoginRequest(googleId, email, name, photoUrl, idToken)
            )
            
            if (response.isSuccessful && response.body()?.success == true) {
                val authResponse = response.body()?.data!!
                
                // 保存token
                saveTokens(authResponse.accessToken, authResponse.refreshToken, authResponse.expiresIn)
                
                // 检查是否已有本地用户（通过Google用户ID）
                val existingUser = userDao.getUserByGoogleId(googleId)
                val user = if (existingUser != null) {
                    existingUser.copy(
                        nickname = name,
                        avatar = photoUrl,
                        isLoggedIn = true,
                        lastLoginTime = System.currentTimeMillis(),
                        serverUserId = authResponse.user.userId.takeIf { it.isNotEmpty() } ?: existingUser.serverUserId,
                        isDataSyncEnabled = true
                    )
                } else {
                    // 新用户开始7天免费试用
                    val currentTime = System.currentTimeMillis()
                    UserEntity(
                        userId = UUID.randomUUID().toString(),
                        phoneNumber = "", // Google登录不需要手机号
                        passwordHash = "",
                        nickname = name,
                        avatar = photoUrl,
                        isLoggedIn = true,
                        lastLoginTime = currentTime,
                        registerTime = currentTime,
                        serverUserId = authResponse.user.userId.takeIf { it.isNotEmpty() } ?: "",
                        isDataSyncEnabled = true,
                        isPremium = false,
                        trialStartTime = currentTime,
                        subscriptionStatus = UserEntity.STATUS_TRIAL,
                        paymentTime = 0L,
                        paymentAmount = 0,
                        alipayUserId = "",
                        email = email,
                        googleId = googleId
                    )
                }
                
                // 先登出所有用户，再更新当前用户
                userDao.logoutAllUsers()
                userDao.insertUser(user)
                
                Result.success(user)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Google登录失败"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Google登录失败: ${e.message}"))
        }
    }
    
    /**
     * 创建本地Google用户（调试模式用）
     * 不调用服务器API，直接在本地创建用户
     */
    suspend fun createLocalGoogleUser(
        googleId: String,
        email: String,
        name: String,
        photoUrl: String
    ): Result<UserEntity> {
        return try {
            // 检查是否已有本地用户（通过Google用户ID）
            val existingUser = userDao.getUserByGoogleId(googleId)
            val user = if (existingUser != null) {
                existingUser.copy(
                    nickname = name,
                    avatar = photoUrl,
                    isLoggedIn = true,
                    lastLoginTime = System.currentTimeMillis(),
                    isDataSyncEnabled = false // 本地模拟用户不同步数据
                )
            } else {
                // 新用户开始7天免费试用
                val currentTime = System.currentTimeMillis()
                UserEntity(
                    userId = UUID.randomUUID().toString(),
                    phoneNumber = "", // Google登录不需要手机号
                    passwordHash = "",
                    nickname = name,
                    avatar = photoUrl,
                    isLoggedIn = true,
                    lastLoginTime = currentTime,
                    registerTime = currentTime,
                    serverUserId = "", // 本地模拟用户没有服务器ID
                    isDataSyncEnabled = false, // 本地模拟用户不同步数据
                    isPremium = false,
                    trialStartTime = currentTime,
                    subscriptionStatus = UserEntity.STATUS_TRIAL,
                    paymentTime = 0L,
                    paymentAmount = 0,
                    alipayUserId = "",
                    email = email,
                    googleId = googleId
                )
            }
            
            // 先登出所有用户，再更新当前用户
            userDao.logoutAllUsers()
            userDao.insertUser(user)
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception("创建本地Google用户失败: ${e.message}"))
        }
    }
    
    /**
     * 用户登出
     */
    suspend fun logout(): Result<Unit> {
        return try {
            val token = getAccessToken()
            if (!token.isNullOrEmpty()) {
                // 调用服务器登出API
                userApiService.logout("Bearer $token")
            }
            
            // 如果是Google登录用户，清除Google账号缓存
            val currentUser = getCurrentUser()
            if (currentUser?.googleId?.isNotEmpty() == true && googleLoginManager != null) {
                try {
                    Log.d("UserRepository", "🔄 开始执行Google账号退出流程")
                    val logoutSuccess = googleLoginManager.logout()
                    if (logoutSuccess) {
                        Log.d("UserRepository", "✅ Google账号退出成功")
                    } else {
                        Log.w("UserRepository", "⚠️ Google账号退出失败")
                    }
                } catch (e: Exception) {
                    Log.e("UserRepository", "❌ 执行Google账号退出时发生异常", e)
                }
            }
            
            // 清除本地数据
            userDao.logoutAllUsers()
            clearTokens()
            
            Result.success(Unit)
        } catch (e: Exception) {
            // 即使服务器调用失败，也要清除本地数据
            try {
                // 尝试清除Google账号缓存
                val currentUser = getCurrentUser()
                if (currentUser?.googleId?.isNotEmpty() == true && googleLoginManager != null) {
                    googleLoginManager.logout()
                }
            } catch (googleException: Exception) {
                Log.w("UserRepository", "清除Google账号缓存失败", googleException)
            }
            
            userDao.logoutAllUsers()
            clearTokens()
            Result.success(Unit)
        }
    }
    
    /**
     * 更新用户信息
     * @param nickname 昵称
     * @param avatar 头像URL
     * @return 更新结果
     */
    suspend fun updateUserProfile(nickname: String, avatar: String): Result<UserEntity> {
        return try {
            val currentUser = getCurrentUser() ?: return Result.failure(Exception("用户未登录"))
            val token = getAccessToken() ?: return Result.failure(Exception("认证token无效"))
            
            val response = userApiService.updateUserProfile(
                "Bearer $token",
                UpdateProfileRequest(nickname, avatar)
            )
            
            if (response.isSuccessful && response.body()?.success == true) {
                val updatedUser = currentUser.copy(
                    nickname = nickname,
                    avatar = avatar
                )
                userDao.updateUser(updatedUser)
                Result.success(updatedUser)
            } else {
                Result.failure(Exception(response.body()?.message ?: "更新失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 创建模拟支付宝用户（仅用于开发测试）
     */
    suspend fun createMockAlipayUser(): Result<UserEntity> {
        return try {
            Log.d("UserRepository", "创建模拟支付宝用户")
            
            // 先登出所有用户
            userDao.logoutAllUsers()
            
            // 创建模拟用户
            val mockUser = UserEntity(
                userId = "mock_alipay_user_${System.currentTimeMillis()}",
                phoneNumber = "",
                passwordHash = "",
                nickname = "支付宝测试用户",
                avatar = "",
                isLoggedIn = true,
                lastLoginTime = System.currentTimeMillis(),
                registerTime = System.currentTimeMillis(),
                serverUserId = "mock_server_user_id",
                isDataSyncEnabled = false,
                isPremium = false,
                trialStartTime = System.currentTimeMillis(),
                subscriptionStatus = UserEntity.STATUS_TRIAL,
                paymentTime = 0L,
                paymentAmount = 0,
                alipayUserId = "mock_alipay_user_id_${System.currentTimeMillis()}"
            )
            
            userDao.insertUser(mockUser)
            Log.d("UserRepository", "模拟用户创建成功: ${mockUser.nickname}")
            
            Result.success(mockUser)
        } catch (e: Exception) {
            Log.e("UserRepository", "创建模拟用户失败: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    // Token管理方法
    
    private fun saveTokens(accessToken: String, refreshToken: String, expiresIn: Long) {
        val expiresAt = System.currentTimeMillis() + (expiresIn * 1000)
        sharedPrefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putLong(KEY_TOKEN_EXPIRES_AT, expiresAt)
            apply()
        }
    }
    

    
    private fun getRefreshToken(): String? = sharedPrefs.getString(KEY_REFRESH_TOKEN, null)
    
    private fun clearTokens() {
        sharedPrefs.edit().apply {
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            remove(KEY_TOKEN_EXPIRES_AT)
            apply()
        }
    }
    
    /**
     * 密码哈希处理
     */
    private fun hashPassword(password: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val hashBytes = md.digest(password.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
    
    // ================== 付费相关方法 ==================
    
    /**
     * 检查用户是否可以使用应用
     */
    suspend fun canUserUseApp(): Boolean {
        val user = getCurrentUser() ?: return false
        return user.canUseApp()
    }
    
    /**
     * 获取用户付费状态信息
     */
    suspend fun getUserSubscriptionInfo(): SubscriptionInfo? {
        val user = getCurrentUser() ?: return null
        val isInTrial = !user.isPremium && user.trialStartTime > 0L
        return SubscriptionInfo(
            isPremium = user.isPremium,
            subscriptionStatus = user.subscriptionStatus,
            remainingTrialDays = user.getRemainingTrialDays(),
            isTrialExpired = user.isTrialExpired(),
            isInTrial = isInTrial,
            paymentTime = user.paymentTime,
            paymentAmount = user.paymentAmount
        )
    }
    
    /**
     * 升级为付费用户
     */
    suspend fun upgradeToPremium(): Result<UserEntity> {
        return try {
            val currentUser = getCurrentUser() ?: return Result.failure(Exception("用户未登录"))
            
            val updatedUser = currentUser.copy(
                isPremium = true,
                subscriptionStatus = UserEntity.STATUS_PREMIUM,
                paymentTime = System.currentTimeMillis(),
                paymentAmount = UserEntity.PREMIUM_PRICE_CENTS
            )
            
            userDao.updateUser(updatedUser)
            Result.success(updatedUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 更新用户订阅状态
     */
    suspend fun updateSubscriptionStatus(status: String): Result<UserEntity> {
        return try {
            val currentUser = getCurrentUser() ?: return Result.failure(Exception("用户未登录"))
            
            val updatedUser = currentUser.copy(
                subscriptionStatus = status
            )
            
            userDao.updateUser(updatedUser)
            Result.success(updatedUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 订阅信息数据类
     */
    data class SubscriptionInfo(
        val isPremium: Boolean,
        val subscriptionStatus: String,
        val remainingTrialDays: Int,
        val isTrialExpired: Boolean,
        val isInTrial: Boolean,
        val paymentTime: Long,
        val paymentAmount: Int
    )
} 