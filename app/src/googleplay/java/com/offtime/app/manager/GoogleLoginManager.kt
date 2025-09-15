package com.offtime.app.manager

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.offtime.app.BuildConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import com.offtime.app.manager.interfaces.LoginManager
import com.offtime.app.manager.interfaces.LoginResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.coroutines.resume

/**
 * Google登录管理类
 * 负责处理Google登录相关的功能
 */
@Singleton
class GoogleLoginManager @Inject constructor(
    private val context: Context
) : LoginManager {
    
    companion object {
        private const val TAG = "GoogleLoginManager"
        const val RC_SIGN_IN = 9001
    }
    
    private var googleSignInClient: GoogleSignInClient? = null
    
    /**
     * 初始化Google登录客户端
     */
    private fun initializeGoogleSignInClient(): GoogleSignInClient {
        if (googleSignInClient == null) {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .apply {
                    // 如果有Web Client ID，则请求ID Token
                    if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotEmpty()) {
                        requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                    }
                }
                .build()
            
            googleSignInClient = GoogleSignIn.getClient(context, gso)
        }
        return googleSignInClient!!
    }
    
    /**
     * 初始化Google登录客户端（强制账号选择）
     */
    private fun initializeGoogleSignInClientWithAccountPicker(): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .apply {
                // 如果有Web Client ID，则请求ID Token
                if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotEmpty()) {
                    requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                }
            }
            .build()
        
        return GoogleSignIn.getClient(context, gso)
    }
    
    /**
     * 启动Google登录流程（带账号选择）
     */
    fun getSignInIntent(): Intent {
        val client = initializeGoogleSignInClient()
        return client.signInIntent
    }
    
    /**
     * 启动Google登录流程（强制显示账号选择器）
     */
    fun getSignInIntentWithAccountPicker(): Intent {
        // 使用新的客户端实例，先退出当前登录状态，确保显示账号选择器
        val client = initializeGoogleSignInClientWithAccountPicker()
        client.signOut()
        return client.signInIntent
    }
    
    /**
     * 处理Google登录结果
     */
    suspend fun handleSignInResult(data: Intent?): GoogleSignInResult = suspendCancellableCoroutine { continuation ->
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInTask(task) { result ->
                continuation.resume(result)
            }
        } catch (e: Exception) {
            Log.e(TAG, "处理登录结果时发生错误", e)
            continuation.resume(
                GoogleSignInResult(
                    success = false,
                    errorMessage = "处理登录结果失败: ${e.message}",
                    account = null
                )
            )
        }
    }
    
    private fun handleSignInTask(completedTask: Task<GoogleSignInAccount>, callback: (GoogleSignInResult) -> Unit) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            Log.d(TAG, "Google登录成功: ${account.displayName} (${account.email})")
            
            callback(
                GoogleSignInResult(
                    success = true,
                    errorMessage = null,
                    account = account
                )
            )
        } catch (e: ApiException) {
            Log.w(TAG, "Google登录失败，错误代码: ${e.statusCode}", e)
            
            val errorMessage = when (e.statusCode) {
                10 -> "开发者配置错误，请检查Google Cloud Console配置"
                12501 -> "用户取消了登录"
                12502 -> "网络错误，请检查网络连接"
                12500 -> "Google Play服务不可用"
                else -> "登录失败，错误代码: ${e.statusCode}"
            }
            
            callback(
                GoogleSignInResult(
                    success = false,
                    errorMessage = errorMessage,
                    account = null
                )
            )
        }
    }
    
    // Interface implementations
    override suspend fun login(vararg params: String): Flow<LoginResult> = flow {
        emit(LoginResult.Loading)
        try {
            // Google登录逻辑实现
            // params可以包含必要的参数，但Google登录主要通过Intent处理
            emit(LoginResult.Error("请通过signInWithGoogle方法进行Google登录"))
        } catch (e: Exception) {
            emit(LoginResult.Error("Google登录失败: ${e.message}", e))
        }
    }
    
    override suspend fun logout(): Boolean {
        return try {
            signOut()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override fun isLoggedIn(): Boolean {
        return getLastSignedInAccount() != null
    }
    
    override fun getCurrentUserId(): String? {
        return getLastSignedInAccount()?.id
    }

    /**
     * 获取当前已登录的Google账户
     */
    fun getLastSignedInAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }
    
    /**
     * 退出Google登录
     */
    suspend fun signOut(): Boolean = suspendCancellableCoroutine { continuation ->
        val client = initializeGoogleSignInClient()
        client.signOut()
            .addOnCompleteListener { task ->
                val success = task.isSuccessful
                Log.d(TAG, "Google登录退出${if (success) "成功" else "失败"}")
                continuation.resume(success)
            }
    }
    
    /**
     * 撤销Google账户访问权限
     */
    suspend fun revokeAccess(): Boolean = suspendCancellableCoroutine { continuation ->
        val client = initializeGoogleSignInClient()
        client.revokeAccess()
            .addOnCompleteListener { task ->
                val success = task.isSuccessful
                Log.d(TAG, "Google账户访问权限撤销${if (success) "成功" else "失败"}")
                continuation.resume(success)
            }
    }
    
    /**
     * 检查Google Play服务是否可用
     */
    fun isGooglePlayServicesAvailable(): Boolean {
        return try {
            GoogleSignIn.getLastSignedInAccount(context)
            true // 如果能获取到账户信息，说明服务可用
        } catch (e: Exception) {
            Log.e(TAG, "Google Play服务不可用", e)
            false
        }
    }
}

/**
 * Google登录结果数据类
 */
data class GoogleSignInResult(
    val success: Boolean,
    val errorMessage: String?,
    val account: GoogleSignInAccount?
)