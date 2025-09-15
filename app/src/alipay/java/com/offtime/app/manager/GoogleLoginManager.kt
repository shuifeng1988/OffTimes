package com.offtime.app.manager

import android.content.Context
import android.content.Intent
import com.offtime.app.manager.interfaces.LoginManager
import com.offtime.app.manager.interfaces.LoginResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google登录管理类（支付宝版本空实现）
 * 在支付宝版本中，Google登录功能被禁用
 */
@Singleton
class GoogleLoginManager @Inject constructor(
    private val context: Context
) : LoginManager {
    
    /**
     * 获取登录Intent（空实现）
     */
    fun getSignInIntent(): Intent {
        return Intent() // 返回空Intent
    }
    
    /**
     * 处理登录结果（空实现）
     */
    suspend fun handleSignInResult(@Suppress("UNUSED_PARAMETER") data: Intent?): GoogleSignInResult {
        return GoogleSignInResult(
            success = false,
            errorMessage = "Google登录在支付宝版本中不可用",
            account = null
        )
    }
    
    // Interface implementations
    override suspend fun login(vararg params: String): Flow<LoginResult> = flow {
        emit(LoginResult.Error("Google登录在支付宝版本中不可用"))
    }
    
    override suspend fun logout(): Boolean {
        return false
    }
    
    override fun isLoggedIn(): Boolean {
        return false
    }
    
    override fun getCurrentUserId(): String? {
        return null
    }
}

/**
 * Google登录结果数据类（支付宝版本）
 */
data class GoogleSignInResult(
    val success: Boolean,
    val errorMessage: String?,
    val account: GoogleSignInAccount?
)

/**
 * Google登录账户类（支付宝版本空实现）
 */
data class GoogleSignInAccount(
    val id: String?,
    val displayName: String?,
    val email: String?,
    val photoUrl: android.net.Uri?
)
