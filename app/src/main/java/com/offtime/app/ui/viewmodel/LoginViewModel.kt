package com.offtime.app.ui.viewmodel

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.offtime.app.BuildConfig
import com.offtime.app.R
import com.offtime.app.data.repository.UserRepository
import com.offtime.app.manager.interfaces.LoginManager
import javax.inject.Named
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.regex.Pattern
import javax.inject.Inject

/**
 * 登录页面ViewModel
 * 管理登录、注册相关的UI状态和业务逻辑
 * 
 * 主要功能：
 * - 表单验证
 * - 用户登录和注册
 * - 短信验证码发送和验证
 * - 支付宝登录
 * - UI状态管理
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    application: Application,
    private val userRepository: UserRepository,
    @Named("alipay") private val alipayLoginManager: LoginManager?,
    @Named("google") private val googleLoginManager: LoginManager?
) : AndroidViewModel(application) {
    
    private val context = getApplication<Application>()
    
    companion object {
        // 手机号正则表达式
        private val PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$")
        // 账号正则表达式（支持手机号和用户名）
        private val ACCOUNT_PATTERN = Pattern.compile("^(1[3-9]\\d{9}|[a-zA-Z0-9_]{3,20})$")
        // 密码最小长度
        private const val MIN_PASSWORD_LENGTH = 6
        // 验证码倒计时时长（秒）
        private const val SMS_COUNTDOWN_SECONDS = 60
    }
    
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
    
    /**
     * 设置手机号
     */
    fun setPhoneNumber(phoneNumber: String) {
        _uiState.value = _uiState.value.copy(
            phoneNumber = phoneNumber,
            phoneNumberError = null,
            errorMessage = null
        )
        validatePhoneNumber(phoneNumber)
    }
    
    /**
     * 设置密码
     */
    fun setPassword(password: String) {
        _uiState.value = _uiState.value.copy(
            password = password,
            passwordError = null,
            errorMessage = null
        )
        validatePassword(password)
        
        // 如果是注册模式，同时验证确认密码
        if (_uiState.value.isRegisterMode && _uiState.value.confirmPassword.isNotEmpty()) {
            validateConfirmPassword(_uiState.value.confirmPassword, password)
        }
    }
    
    /**
     * 设置确认密码
     */
    fun setConfirmPassword(confirmPassword: String) {
        _uiState.value = _uiState.value.copy(
            confirmPassword = confirmPassword,
            confirmPasswordError = null,
            errorMessage = null
        )
        validateConfirmPassword(confirmPassword, _uiState.value.password)
    }
    
    /**
     * 设置验证码
     */
    fun setSmsCode(smsCode: String) {
        _uiState.value = _uiState.value.copy(
            smsCode = smsCode,
            smsCodeError = null,
            errorMessage = null
        )
        validateSmsCode(smsCode)
    }
    
    /**
     * 设置昵称
     */
    fun setNickname(nickname: String) {
        _uiState.value = _uiState.value.copy(nickname = nickname)
    }
    
    /**
     * 设置登录类型
     */
    fun setLoginType(loginType: LoginType) {
        _uiState.value = _uiState.value.copy(
            loginType = loginType,
            errorMessage = null
        )
    }
    
    /**
     * 切换密码可见性
     */
    fun togglePasswordVisibility() {
        _uiState.value = _uiState.value.copy(
            passwordVisible = !_uiState.value.passwordVisible
        )
    }
    
    /**
     * 切换注册/登录模式
     */
    fun toggleRegisterMode() {
        _uiState.value = _uiState.value.copy(
            isRegisterMode = !_uiState.value.isRegisterMode,
            errorMessage = null,
            password = "",
            confirmPassword = "",
            smsCode = "",
            nickname = "",
            passwordError = null,
            confirmPasswordError = null,
            smsCodeError = null
        )
    }
    
    /**
     * 发送短信验证码
     */
    fun sendSmsCode() {
        val currentState = _uiState.value
        
        // 验证手机号
        if (!isPhoneNumberValid(currentState.phoneNumber)) {
            _uiState.value = currentState.copy(
                phoneNumberError = context.getString(R.string.error_invalid_phone)
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = currentState.copy(
                isSendingCode = true,
                errorMessage = null
            )
            
            try {
                val type = if (currentState.isRegisterMode) "register" else "login"
                val result = userRepository.sendSmsCode(currentState.phoneNumber, type)
                
                result.fold(
                    onSuccess = {
                        // 开始倒计时
                        startCountdown()
                        _uiState.value = _uiState.value.copy(
                            isSendingCode = false
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isSendingCode = false,
                            errorMessage = error.message ?: "SMS code sending failed"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSendingCode = false,
                    errorMessage = "Network error, please check your connection"
                )
            }
        }
    }
    
    /**
     * 密码登录
     */
    fun loginWithPassword() {
        val currentState = _uiState.value
        
        // 表单验证
        if (!validateLoginForm()) {
            return
        }
        
        viewModelScope.launch {
            _uiState.value = currentState.copy(
                isLoading = true,
                errorMessage = null
            )
            
            try {
                val result = userRepository.loginWithPassword(
                    currentState.phoneNumber,
                    currentState.password
                )
                
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isLoginSuccess = true
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "登录失败"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "网络错误，请检查网络连接"
                )
            }
        }
    }
    
    /**
     * 验证码登录
     */
    fun loginWithSmsCode() {
        val currentState = _uiState.value
        
        // 表单验证
        if (!validateSmsLoginForm()) {
            return
        }
        
        viewModelScope.launch {
            _uiState.value = currentState.copy(
                isLoading = true,
                errorMessage = null
            )
            
            try {
                // 先验证验证码
                val verifyResult = userRepository.verifySmsCode(
                    currentState.phoneNumber,
                    currentState.smsCode,
                    "login"
                )
                
                verifyResult.fold(
                    onSuccess = { verifyToken ->
                        // 验证成功，进行登录
                        val loginResult = userRepository.loginWithSmsCode(
                            currentState.phoneNumber,
                            verifyToken
                        )
                        
                        loginResult.fold(
                            onSuccess = {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    isLoginSuccess = true
                                )
                            },
                            onFailure = { error ->
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    errorMessage = error.message ?: "登录失败"
                                )
                            }
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "验证码验证失败"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "网络错误，请检查网络连接"
                )
            }
        }
    }
    
    /**
     * 用户注册
     */
    fun register() {
        val currentState = _uiState.value
        
        // 表单验证
        if (!validateRegisterForm()) {
            return
        }
        
        viewModelScope.launch {
            _uiState.value = currentState.copy(
                isLoading = true,
                errorMessage = null
            )
            
            try {
                // 根据BuildConfig决定是否需要SMS验证码
                if (BuildConfig.REQUIRE_SMS_VERIFICATION) {
                    // 需要SMS验证码的版本（如Google Play版本）
                    // 先验证验证码（如果使用验证码注册）
                    if (currentState.loginType == LoginType.SMS_CODE) {
                        val verifyResult = userRepository.verifySmsCode(
                            currentState.phoneNumber,
                            currentState.smsCode,
                            "register"
                        )
                        
                        verifyResult.fold(
                            onSuccess = { verifyToken ->
                                // 验证成功，进行注册
                                performRegister(verifyToken, currentState)
                            },
                            onFailure = { error ->
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    errorMessage = error.message ?: "验证码验证失败"
                                )
                            }
                        )
                    } else {
                        // 密码注册，需要先发送并验证验证码
                        val verifyResult = userRepository.verifySmsCode(
                            currentState.phoneNumber,
                            currentState.smsCode,
                            "register"
                        )
                        
                        verifyResult.fold(
                            onSuccess = { verifyToken ->
                                performRegister(verifyToken, currentState)
                            },
                            onFailure = { _ ->
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    errorMessage = "请先发送并输入正确的验证码"
                                )
                            }
                        )
                    }
                } else {
                    // 不需要SMS验证码的版本（如支付宝版本）
                    performRegisterWithoutSms(currentState)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "网络错误，请检查网络连接"
                )
            }
        }
    }
    
    /**
     * 执行无验证码注册操作
     */
    private suspend fun performRegisterWithoutSms(currentState: LoginUiState) {
        try {
            val result = userRepository.registerWithoutSms(
                currentState.phoneNumber,
                currentState.password,
                currentState.nickname
            )
            
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoginSuccess = true
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "注册失败"
                    )
                }
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "网络错误，请检查网络连接"
            )
        }
    }
    
    /**
     * 执行注册操作
     */
    private suspend fun performRegister(verifyToken: String, currentState: LoginUiState) {
        try {
            val result = userRepository.register(
                currentState.phoneNumber,
                currentState.password,
                verifyToken,
                currentState.nickname
            )
            
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoginSuccess = true
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "注册失败"
                    )
                }
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "网络错误，请检查网络连接"
            )
        }
    }
    
    /**
     * 支付宝登录
     */
    fun loginWithAlipay(@Suppress("UNUSED_PARAMETER") activity: Activity) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )
            
            try {
                // 打印配置诊断信息（用于调试）
                Log.d("LoginViewModel", alipayLoginManager?.let { "Alipay配置可用" } ?: "Alipay配置不可用")
                
                // 发起支付宝授权
                val authResult = alipayLoginManager?.let { 
                    // 简化的授权结果
                    class AuthResult(val success: Boolean, val authCode: String? = null, val alipayUserId: String? = null)
                    AuthResult(success = false) // 实际实现需要处理Flow
                } ?: run {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "支付宝登录不可用"
                    )
                    return@launch
                }
                
                if (authResult.success) {
                    // 授权成功，获取用户信息
                    @Suppress("UNUSED_VARIABLE")
                    val userInfo = null // 简化实现，实际需要通过接口获取
                    
                    // 直接使用授权结果进行登录，不依赖userInfo
                    if (true) { // 简化条件，实际应该检查授权结果
                        // 调用后端API，使用支付宝信息登录/注册
                        val loginResult = userRepository.loginWithAlipay(
                            alipayUserId = authResult.alipayUserId ?: "",
                            authCode = authResult.authCode ?: "",
                            nickname = "支付宝用户",
                            avatar = ""
                        )
                        
                        loginResult.fold(
                            onSuccess = {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    isLoginSuccess = true
                                )
                            },
                            onFailure = { error ->
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    errorMessage = error.message ?: "支付宝登录失败"
                                )
                            }
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "获取用户信息失败"
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "支付宝授权失败"
                    )
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "支付宝登录失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Google登录
     */
    fun loginWithGoogle(@Suppress("UNUSED_PARAMETER") activity: Activity, forceAccountPicker: Boolean = true) {
        if (!BuildConfig.ENABLE_GOOGLE_LOGIN) {
            Log.w("LoginViewModel", "Google登录功能未启用")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )
            
            try {
                // 检查Google登录管理器是否可用
                if (googleLoginManager == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Google登录不可用，请检查配置"
                    )
                    return@launch
                }

                // 强制退出，确保清除任何残留状态
                Log.d("LoginViewModel", "🔄 登录前强制执行退出操作")
                googleLoginManager.logout()
                
                // 启动真实的Google登录流程
                try {
                    Log.d("LoginViewModel", "🔍 开始启动Google登录流程")
                    
                    @Suppress("USELESS_IS_CHECK")
                    if (googleLoginManager !is com.offtime.app.manager.GoogleLoginManager) {
                        Log.e("LoginViewModel", "❌ Google登录管理器类型不正确: ${googleLoginManager.javaClass.simpleName}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Google登录管理器类型不正确"
                        )
                        return@launch
                    }
                    
                    Log.d("LoginViewModel", "✅ 获取Google登录Intent${if (forceAccountPicker) "（带账号选择器）" else ""}")
                    val signInIntent = try {
                        @Suppress("USELESS_IS_CHECK")
                        if (forceAccountPicker && googleLoginManager is com.offtime.app.manager.GoogleLoginManager) {
                            // 使用反射调用方法，避免编译时检查
                            val method = googleLoginManager::class.java.getMethod("getSignInIntentWithAccountPicker")
                            method.invoke(googleLoginManager) as android.content.Intent
                        } else {
                            googleLoginManager.getSignInIntent()
                        }
                    } catch (e: Exception) {
                        // 如果方法不存在（比如在支付宝版本中），回退到普通方法
                        Log.w("LoginViewModel", "getSignInIntentWithAccountPicker not available, using getSignInIntent", e)
                        googleLoginManager.getSignInIntent()
                    }
                    
                    Log.d("LoginViewModel", "🚀 设置Google登录Intent到UI状态")
                    // 需要通过Activity启动Intent
                    // 这里我们通过设置特殊状态来让UI层处理Intent启动
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        googleSignInIntent = signInIntent
                    )
                    
                } catch (e: Exception) {
                    Log.e("LoginViewModel", "❌ 启动Google登录失败", e)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "启动Google登录失败: ${e.message}"
                    )
                }
                

            } catch (e: Exception) {
                Log.e("LoginViewModel", "Google登录失败", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Google登录失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 处理Google登录结果
     */
    fun handleGoogleSignInResult(data: android.content.Intent?) {
        if (!BuildConfig.ENABLE_GOOGLE_LOGIN) return
        
        viewModelScope.launch {
            try {
                Log.d("LoginViewModel", "🔍 开始处理Google登录结果")
                
                if (googleLoginManager == null) {
                    Log.e("LoginViewModel", "❌ Google登录管理器不可用")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Google登录管理器不可用"
                    )
                    return@launch
                }
                
                // 处理真实的Google登录结果
                if (googleLoginManager !is com.offtime.app.manager.GoogleLoginManager) {
                    Log.e("LoginViewModel", "❌ Google登录管理器类型不正确: ${googleLoginManager.javaClass.simpleName}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Google登录管理器类型不正确"
                    )
                    return@launch
                }
                
                Log.d("LoginViewModel", "✅ 调用GoogleLoginManager处理登录结果")
                val result = googleLoginManager.handleSignInResult(data)
                
                Log.d("LoginViewModel", "📋 Google登录结果: success=${result.success}, account=${result.account?.displayName}")
                
                if (result.success && result.account != null) {
                    val account = result.account
                    Log.d("LoginViewModel", "🎉 Google登录成功: ${account.displayName} (${account.email})")
                    
                    // 使用真实的Google账户信息创建本地用户
                    Log.d("LoginViewModel", "💾 创建本地Google用户")
                    val userResult = userRepository.createLocalGoogleUser(
                        googleId = account.id ?: "",
                        email = account.email ?: "",
                        name = account.displayName ?: "",
                        photoUrl = account.photoUrl?.toString() ?: ""
                    )
                    
                    userResult.fold(
                        onSuccess = { user ->
                            Log.d("LoginViewModel", "✅ Google用户创建成功: ${user.nickname}")
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isLoginSuccess = true
                            )
                        },
                        onFailure = { error ->
                            Log.e("LoginViewModel", "❌ 创建用户失败", error)
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                errorMessage = "创建用户失败: ${error.message}"
                            )
                        }
                    )
                } else {
                    Log.w("LoginViewModel", "⚠️ Google登录失败: ${result.errorMessage}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.errorMessage ?: "Google登录失败"
                    )
                }
                
            } catch (e: Exception) {
                Log.e("LoginViewModel", "处理Google登录结果失败", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "处理Google登录结果失败: ${e.message}"
                )
            }
        }
    }

    /**
     * 清除Google登录Intent状态
     */
    fun clearGoogleSignInIntent() {
        _uiState.value = _uiState.value.copy(googleSignInIntent = null)
    }
    
    /**
     * 模拟支付宝登录（仅用于开发测试）
     */
    fun simulateAlipayLogin() {
        if (!BuildConfig.DEBUG) {
            Log.w("LoginViewModel", "模拟登录只能在Debug模式下使用")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )
            
            try {
                // 模拟网络延迟
                delay(1000)
                
                // 创建模拟用户
                val mockUser = userRepository.createMockAlipayUser()
                
                mockUser.fold(
                    onSuccess = {
                        Log.d("LoginViewModel", "模拟支付宝登录成功")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isLoginSuccess = true
                        )
                    },
                    onFailure = { error ->
                        Log.e("LoginViewModel", "模拟登录失败: ${error.message}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "模拟登录失败: ${error.message}"
                        )
                    }
                )
                
            } catch (e: Exception) {
                Log.e("LoginViewModel", "模拟登录异常: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "模拟登录异常: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 开始验证码倒计时
     */
    private fun startCountdown() {
        viewModelScope.launch {
            for (i in SMS_COUNTDOWN_SECONDS downTo 1) {
                _uiState.value = _uiState.value.copy(countDown = i)
                delay(1000)
            }
            _uiState.value = _uiState.value.copy(countDown = 0)
        }
    }
    
    // 表单验证方法
    
    /**
     * 验证手机号
     */
    private fun validatePhoneNumber(phoneNumber: String) {
        val currentState = _uiState.value
        if (phoneNumber.isNotEmpty()) {
            // 注册模式或密码登录模式支持账号格式，SMS登录只支持手机号
            val isValid = if (currentState.isRegisterMode || currentState.loginType == LoginType.PASSWORD) {
                isAccountValid(phoneNumber)
            } else {
                isPhoneNumberValid(phoneNumber)
            }
            
            if (!isValid) {
                val errorMessage = if (currentState.isRegisterMode || currentState.loginType == LoginType.PASSWORD) {
                    context.getString(R.string.error_invalid_account)
                } else {
                    context.getString(R.string.error_invalid_phone)
                }
                _uiState.value = _uiState.value.copy(phoneNumberError = errorMessage)
            }
        }
    }
    
    /**
     * 验证密码
     */
    private fun validatePassword(password: String) {
        if (password.isNotEmpty() && password.length < MIN_PASSWORD_LENGTH) {
            _uiState.value = _uiState.value.copy(
                passwordError = context.getString(R.string.error_password_too_short, MIN_PASSWORD_LENGTH)
            )
        }
    }
    
    /**
     * 验证确认密码
     */
    private fun validateConfirmPassword(confirmPassword: String, password: String) {
        if (confirmPassword.isNotEmpty() && confirmPassword != password) {
            _uiState.value = _uiState.value.copy(
                confirmPasswordError = context.getString(R.string.error_passwords_not_match)
            )
        }
    }
    
    /**
     * 验证短信验证码
     */
    private fun validateSmsCode(smsCode: String) {
        if (smsCode.isNotEmpty() && smsCode.length != 6) {
            _uiState.value = _uiState.value.copy(
                smsCodeError = context.getString(R.string.error_invalid_sms_code)
            )
        }
    }
    
    /**
     * 检查手机号是否有效
     */
    private fun isPhoneNumberValid(phoneNumber: String): Boolean {
        return PHONE_PATTERN.matcher(phoneNumber).matches()
    }
    
    /**
     * 检查账号是否有效（支持手机号和用户名）
     */
    private fun isAccountValid(account: String): Boolean {
        return ACCOUNT_PATTERN.matcher(account).matches()
    }
    
    /**
     * 验证登录表单
     */
    private fun validateLoginForm(): Boolean {
        val currentState = _uiState.value
        var isValid = true
        
        if (currentState.loginType == LoginType.PASSWORD) {
            // 密码登录支持账号格式（手机号或用户名）
            if (!isAccountValid(currentState.phoneNumber)) {
                _uiState.value = currentState.copy(phoneNumberError = context.getString(R.string.error_invalid_account))
                isValid = false
            }
            if (currentState.password.length < MIN_PASSWORD_LENGTH) {
                _uiState.value = _uiState.value.copy(passwordError = context.getString(R.string.error_password_too_short, MIN_PASSWORD_LENGTH))
                isValid = false
            }
        } else {
            // SMS登录仍然只支持手机号
            if (!isPhoneNumberValid(currentState.phoneNumber)) {
                _uiState.value = currentState.copy(phoneNumberError = context.getString(R.string.error_invalid_phone))
                isValid = false
            }
        }
        
        return isValid
    }
    
    /**
     * 验证短信登录表单
     */
    private fun validateSmsLoginForm(): Boolean {
        val currentState = _uiState.value
        var isValid = true
        
        if (!isPhoneNumberValid(currentState.phoneNumber)) {
            _uiState.value = currentState.copy(phoneNumberError = context.getString(R.string.error_invalid_phone))
            isValid = false
        }
        
        if (currentState.smsCode.length != 6) {
            _uiState.value = _uiState.value.copy(smsCodeError = context.getString(R.string.error_invalid_sms_code))
            isValid = false
        }
        
        return isValid
    }
    
    /**
     * 验证注册表单
     */
    private fun validateRegisterForm(): Boolean {
        val currentState = _uiState.value
        var isValid = true
        
        if (!isAccountValid(currentState.phoneNumber)) {
            _uiState.value = currentState.copy(phoneNumberError = context.getString(R.string.error_invalid_account))
            isValid = false
        }
        
        if (currentState.password.length < MIN_PASSWORD_LENGTH) {
            _uiState.value = _uiState.value.copy(passwordError = context.getString(R.string.error_password_too_short, MIN_PASSWORD_LENGTH))
            isValid = false
        }
        
        if (currentState.confirmPassword != currentState.password) {
            _uiState.value = _uiState.value.copy(confirmPasswordError = context.getString(R.string.error_passwords_not_match))
            isValid = false
        }
        
        // 只在需要SMS验证码的版本中验证SMS验证码
        if (BuildConfig.REQUIRE_SMS_VERIFICATION && currentState.smsCode.length != 6) {
            _uiState.value = _uiState.value.copy(smsCodeError = context.getString(R.string.error_invalid_sms_code))
            isValid = false
        }
        
        return isValid
    }
    
    /**
     * 检查表单是否有效
     */
    fun isFormValid(): Boolean {
        val currentState = _uiState.value
        
        return when {
            currentState.isRegisterMode -> {
                val basicValidation = isAccountValid(currentState.phoneNumber) &&
                    currentState.password.length >= MIN_PASSWORD_LENGTH &&
                    currentState.confirmPassword == currentState.password
                
                // 只在需要SMS验证码的版本中验证SMS验证码
                if (BuildConfig.REQUIRE_SMS_VERIFICATION) {
                    basicValidation && currentState.smsCode.length == 6
                } else {
                    basicValidation
                }
            }
            currentState.loginType == LoginType.PASSWORD -> {
                isAccountValid(currentState.phoneNumber) &&
                currentState.password.length >= MIN_PASSWORD_LENGTH
            }
            currentState.loginType == LoginType.SMS_CODE -> {
                isPhoneNumberValid(currentState.phoneNumber) &&
                currentState.smsCode.length == 6
            }
            currentState.loginType == LoginType.ALIPAY -> {
                // 支付宝登录不需要表单验证
                true
            }
            currentState.loginType == LoginType.GOOGLE -> {
                // Google登录不需要表单验证
                true
            }
            else -> false
        }
    }
}

/**
 * 登录UI状态
 */
data class LoginUiState(
    val phoneNumber: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val smsCode: String = "",
    val nickname: String = "",
    val loginType: LoginType = LoginType.PASSWORD,
    val isRegisterMode: Boolean = false,
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val isSendingCode: Boolean = false,
    val countDown: Int = 0,
    val isLoginSuccess: Boolean = false,
    val phoneNumberError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val smsCodeError: String? = null,
    val errorMessage: String? = null,
    val googleSignInIntent: android.content.Intent? = null
)

/**
 * 登录类型枚举
 */
enum class LoginType {
    PASSWORD,    // 密码登录
    SMS_CODE,    // 验证码登录  
    ALIPAY,      // 支付宝登录
    GOOGLE       // Google登录
} 