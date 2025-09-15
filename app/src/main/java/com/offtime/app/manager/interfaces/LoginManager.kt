package com.offtime.app.manager.interfaces

import kotlinx.coroutines.flow.Flow

/**
 * 登录管理器接口
 * 为不同的登录方式提供统一的接口
 */
interface LoginManager {
    
    /**
     * 执行登录操作
     * @param params 登录参数（可以是手机号、密码等）
     * @return 登录结果的Flow
     */
    suspend fun login(vararg params: String): Flow<LoginResult>
    
    /**
     * 注销登录
     */
    suspend fun logout(): Boolean
    
    /**
     * 检查是否已登录
     */
    fun isLoggedIn(): Boolean
    
    /**
     * 获取当前用户ID
     */
    fun getCurrentUserId(): String?
}

/**
 * 登录结果封装类
 */
sealed class LoginResult {
    object Loading : LoginResult()
    data class Success(val userId: String, val userInfo: Map<String, Any>? = null) : LoginResult()
    data class Error(val message: String, val throwable: Throwable? = null) : LoginResult()
}