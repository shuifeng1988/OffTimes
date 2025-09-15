package com.offtime.app.manager

import android.app.Activity
import android.content.Context
import android.util.Log
import com.alipay.sdk.app.AuthTask
import com.alipay.sdk.app.EnvUtils
import com.offtime.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import android.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import com.offtime.app.manager.interfaces.LoginManager
import com.offtime.app.manager.interfaces.LoginResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 支付宝登录管理类
 * 负责处理支付宝登录相关的功能
 * 
 * 主要功能：
 * - 发起支付宝登录授权
 * - 处理授权结果
 * - 获取用户信息
 */
@Singleton
class AlipayLoginManager @Inject constructor(
    private val context: Context
) : LoginManager {
    
    companion object {
        private const val TAG = "AlipayLoginManager"
        
        // 签名方式
        private const val SIGN_TYPE = "RSA2"
        
        // 字符编码格式
        private const val CHARSET = "utf-8"
        
        // 授权结果
        const val AUTH_CODE_SUCCESS = "9000"
        const val AUTH_CODE_PROCESSING = "8000"
        const val AUTH_CODE_FAIL = "4000"
        const val AUTH_CODE_CANCEL = "6001"
        const val AUTH_CODE_NETWORK_ERROR = "6002"
    }
    
    init {
        // 根据配置设置支付宝环境
        if (BuildConfig.ALIPAY_IS_SANDBOX) {
            EnvUtils.setEnv(EnvUtils.EnvEnum.SANDBOX) // 沙箱环境用于测试
            Log.d(TAG, "支付宝环境设置为：沙箱环境")
        } else {
            EnvUtils.setEnv(EnvUtils.EnvEnum.ONLINE)  // 生产环境
            Log.d(TAG, "支付宝环境设置为：生产环境")
        }
        
        // 打印配置信息（不包含敏感信息）
        Log.d(TAG, "支付宝配置 - APP_ID: ${BuildConfig.ALIPAY_APP_ID}")
        Log.d(TAG, "支付宝配置 - 是否沙箱: ${BuildConfig.ALIPAY_IS_SANDBOX}")
        Log.d(TAG, "支付宝配置 - 私钥长度: ${BuildConfig.ALIPAY_MERCHANT_PRIVATE_KEY.length}")
        Log.d(TAG, "支付宝配置 - 公钥长度: ${BuildConfig.ALIPAY_PUBLIC_KEY.length}")
    }
    
    /**
     * 验证支付宝配置
     */
    private fun validateConfiguration(): String? {
        return when {
            BuildConfig.ALIPAY_APP_ID.isEmpty() || BuildConfig.ALIPAY_APP_ID == "YOUR_APP_ID" -> 
                "APP_ID未配置或使用默认值"
            BuildConfig.ALIPAY_MERCHANT_PRIVATE_KEY.isEmpty() || BuildConfig.ALIPAY_MERCHANT_PRIVATE_KEY == "YOUR_MERCHANT_PRIVATE_KEY" -> 
                "商户私钥未配置或使用默认值"
            BuildConfig.ALIPAY_PUBLIC_KEY.isEmpty() || BuildConfig.ALIPAY_PUBLIC_KEY == "YOUR_ALIPAY_PUBLIC_KEY" -> 
                "支付宝公钥未配置或使用默认值"
            BuildConfig.ALIPAY_APP_ID.length != 16 -> 
                "APP_ID长度不正确，应为16位数字"
            !BuildConfig.ALIPAY_APP_ID.all { it.isDigit() } -> 
                "APP_ID应为纯数字"
            BuildConfig.ALIPAY_MERCHANT_PRIVATE_KEY.length < 100 -> 
                "商户私钥长度过短，可能配置错误"
            BuildConfig.ALIPAY_PUBLIC_KEY.length < 100 -> 
                "支付宝公钥长度过短，可能配置错误"
            else -> null
        }
    }
    
    /**
     * 发起支付宝登录授权
     * 
     * @param activity 当前Activity
     * @param scopes 授权范围，如 "auth_user" 获取用户信息
     * @return 授权结果
     */
    suspend fun authorize(
        activity: Activity,
        scopes: String = "auth_user"
    ): AuthResult = withContext(Dispatchers.IO) {
        try {
            // 调试模式：允许跳过真实授权
            if (BuildConfig.DEBUG && shouldUseDebugMode()) {
                Log.d(TAG, "🚀 调试模式已启用 - 跳过真实支付宝授权")
                Log.d(TAG, "🚀 BuildConfig.DEBUG = ${BuildConfig.DEBUG}")
                Log.d(TAG, "🚀 shouldUseDebugMode() = ${shouldUseDebugMode()}")
                return@withContext AuthResult(
                    success = true,
                    resultCode = AUTH_CODE_SUCCESS,
                    resultMessage = "🚀 调试模式：模拟授权成功",
                    authCode = "debug_auth_code_${System.currentTimeMillis()}",
                    userId = "debug_user_${System.currentTimeMillis()}",
                    alipayUserId = "debug_alipay_user_${System.currentTimeMillis()}"
                )
            }
            
            Log.d(TAG, "⚠️ 调试模式未启用，将进行真实授权")
            Log.d(TAG, "⚠️ BuildConfig.DEBUG = ${BuildConfig.DEBUG}")
            Log.d(TAG, "⚠️ shouldUseDebugMode() = ${shouldUseDebugMode()}")
            
            // 首先验证配置
            val configError = validateConfiguration()
            if (configError != null) {
                Log.e(TAG, "配置验证失败: $configError")
                return@withContext AuthResult(
                    success = false,
                    resultCode = AUTH_CODE_FAIL,
                    resultMessage = "配置错误: $configError",
                    authCode = "",
                    userId = "",
                    alipayUserId = ""
                )
            }
            
            Log.d(TAG, "开始支付宝授权，scope: $scopes")
            Log.d(TAG, "支付宝环境: ${if (BuildConfig.ALIPAY_IS_SANDBOX) "沙箱" else "生产"}")
            
            // 生成授权参数
            val authParam = buildAuthParam(scopes)
            
            if (authParam.isEmpty()) {
                return@withContext AuthResult(
                    success = false,
                    resultCode = AUTH_CODE_FAIL,
                    resultMessage = "授权参数生成失败，请检查APP_ID和密钥配置",
                    authCode = "",
                    userId = "",
                    alipayUserId = ""
                )
            }
            
            Log.d(TAG, "授权参数长度: ${authParam.length}")
            
            // 创建授权任务
            val authTask = AuthTask(activity)
            
            // 调用授权接口
            Log.d(TAG, "调用支付宝SDK authV2方法...")
            val result = authTask.authV2(authParam, true)
            Log.d(TAG, "支付宝SDK返回结果: $result")
            
            // 解析授权结果
            parseAuthResult(result)
            
        } catch (e: Exception) {
            Log.e(TAG, "授权失败: ${e.message}", e)
            
            // 提供更详细的错误信息
            val detailedMessage = when {
                e.message?.contains("NETWORK") == true -> "网络连接失败，请检查网络连接"
                e.message?.contains("APP_ID") == true -> "应用ID配置错误，请检查支付宝配置"
                e.message?.contains("SIGN") == true -> "签名验证失败，请检查密钥配置"
                BuildConfig.ALIPAY_APP_ID.isEmpty() -> "支付宝APP_ID未配置"
                BuildConfig.ALIPAY_MERCHANT_PRIVATE_KEY.isEmpty() -> "支付宝私钥未配置"
                else -> "授权失败: ${e.message}"
            }
            
            Log.e(TAG, "详细错误信息: $detailedMessage")
            
            AuthResult(
                success = false,
                resultCode = AUTH_CODE_FAIL,
                resultMessage = detailedMessage,
                authCode = "",
                userId = "",
                alipayUserId = ""
            )
        }
    }
    
    /**
     * 获取配置诊断信息
     * 用于调试和排查配置问题
     */
    fun getConfigurationDiagnostics(): String {
        val sb = StringBuilder()
        sb.append("=== 支付宝配置诊断 ===\n")
        sb.append("环境: ${if (BuildConfig.ALIPAY_IS_SANDBOX) "沙箱" else "生产"}\n")
        sb.append("APP_ID: ${BuildConfig.ALIPAY_APP_ID}\n")
        sb.append("APP_ID长度: ${BuildConfig.ALIPAY_APP_ID.length}\n")
        sb.append("APP_ID格式: ${if (BuildConfig.ALIPAY_APP_ID.all { it.isDigit() }) "正确" else "错误（应为纯数字）"}\n")
        sb.append("私钥长度: ${BuildConfig.ALIPAY_MERCHANT_PRIVATE_KEY.length}\n")
        sb.append("公钥长度: ${BuildConfig.ALIPAY_PUBLIC_KEY.length}\n")
        
        val configError = validateConfiguration()
        if (configError != null) {
            sb.append("配置错误: $configError\n")
        } else {
            sb.append("配置状态: 基本验证通过\n")
        }
        
        sb.append("==================")
        return sb.toString()
    }
    
    /**
     * 构建授权参数（使用支付宝App授权格式）
     */
    private fun buildAuthParam(scopes: String): String {
        try {
            Log.d(TAG, "构建支付宝App授权参数，scope: $scopes")
            
            // 支付宝App授权使用特定的参数格式
            val authParams = mutableMapOf<String, String>()
            
            // 使用标准API格式（避免App授权特殊参数导致的错误）
            authParams["app_id"] = BuildConfig.ALIPAY_APP_ID
            authParams["method"] = "alipay.open.auth.sdk.code.get"
            authParams["format"] = "JSON"
            authParams["charset"] = CHARSET
            authParams["sign_type"] = SIGN_TYPE
            authParams["timestamp"] = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            authParams["version"] = "1.0"
            authParams["scope"] = scopes
            
            Log.d(TAG, "授权参数详情:")
            authParams.forEach { (key, value) ->
                Log.d(TAG, "  $key = $value")
            }
            
            // 生成待签名字符串（按字母序排序，不包含URL编码）
            val sortedParams = authParams.toSortedMap()
            val signString = sortedParams.map { "${it.key}=${it.value}" }.joinToString("&")
            Log.d(TAG, "待签名字符串: $signString")
            
            // 使用私钥签名
            val sign = signWithRSA2(signString, BuildConfig.ALIPAY_MERCHANT_PRIVATE_KEY)
            if (sign.isEmpty()) {
                Log.e(TAG, "签名生成失败")
                return ""
            }
            
            Log.d(TAG, "生成的签名: ${sign.take(20)}...（已截断）")
            
            // 添加签名
            authParams["sign"] = sign
            
            // 构建最终参数字符串（对值进行URL编码）
            val finalParams = authParams.toSortedMap().map { 
                "${it.key}=${urlEncode(it.value)}" 
            }.joinToString("&")
            
            Log.d(TAG, "最终授权参数长度: ${finalParams.length}")
            Log.d(TAG, "最终授权参数: ${finalParams.take(200)}...（已截断）")
            return finalParams
            
        } catch (e: Exception) {
            Log.e(TAG, "构建授权参数失败", e)
            return ""
        }
    }
    
    /**
     * 生成状态码
     */
    private fun generateState(): String {
        return "alipay_login_${System.currentTimeMillis()}"
    }
    
    /**
     * 判断是否使用调试模式
     * 在开发阶段可以跳过真实的支付宝授权，用于测试其他功能
     */
    private fun shouldUseDebugMode(): Boolean {
        // 只有在支付宝配置不完整时才启用调试模式
        // 这样既能测试真实流程，又能在配置缺失时继续开发
        val configError = validateConfiguration()
        return BuildConfig.DEBUG && configError != null
    }
    
    /**
     * 生成签名
     */
    private fun generateSign(params: Map<String, String>): String {
        return try {
            // 1. 过滤并排序参数
            val filteredParams = params.filter { it.key != "sign" && it.value.isNotEmpty() }
            val sortedParams = filteredParams.toSortedMap()
            
            // 2. 拼接参数字符串
            val stringToSign = sortedParams.map { "${it.key}=${it.value}" }.joinToString("&")
            Log.d(TAG, "待签名字符串: $stringToSign")
            
            // 3. 使用RSA2签名
            signWithRSA2(stringToSign, BuildConfig.ALIPAY_MERCHANT_PRIVATE_KEY)
        } catch (e: Exception) {
            Log.e(TAG, "签名生成失败", e)
            ""
        }
    }
    
    /**
     * RSA2签名实现
     */
    private fun signWithRSA2(content: String, privateKey: String): String {
        return try {
            // 1. 解码私钥
            val priKey = parsePrivateKey(privateKey)
            
            // 2. 执行签名
            val signature = Signature.getInstance("SHA256WithRSA")
            signature.initSign(priKey)
            signature.update(content.toByteArray(charset("UTF-8")))
            
            // 3. Base64编码
            Base64.encodeToString(signature.sign(), Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "RSA2签名失败", e)
            ""
        }
    }
    
    /**
     * 解析私钥，支持PKCS1和PKCS8格式
     */
    private fun parsePrivateKey(privateKeyStr: String): PrivateKey {
        val keyFactory = KeyFactory.getInstance("RSA")
        
        return try {
            // 首先尝试PKCS8格式
            val keySpec = PKCS8EncodedKeySpec(Base64.decode(privateKeyStr, Base64.NO_WRAP))
            keyFactory.generatePrivate(keySpec)
        } catch (e: Exception) {
            Log.d(TAG, "PKCS8解析失败，尝试PKCS1格式")
            // 如果PKCS8失败，尝试PKCS1格式
            try {
                val pkcs1Bytes = Base64.decode(privateKeyStr, Base64.NO_WRAP)
                val pkcs8Bytes = convertPKCS1ToPKCS8(pkcs1Bytes)
                val keySpec = PKCS8EncodedKeySpec(pkcs8Bytes)
                keyFactory.generatePrivate(keySpec)
            } catch (e2: Exception) {
                Log.e(TAG, "私钥格式解析失败", e2)
                throw e2
            }
        }
    }
    
    /**
     * 将PKCS1格式私钥转换为PKCS8格式
     */
    private fun convertPKCS1ToPKCS8(pkcs1Bytes: ByteArray): ByteArray {
        // PKCS8 RSA private key header
        val pkcs8Header = byteArrayOf(
            0x30, 0x82.toByte(), 0x00, 0x00, // SEQUENCE, length (will be filled)
            0x02, 0x01, 0x00, // INTEGER version = 0
            0x30, 0x0d, // SEQUENCE
            0x06, 0x09, 0x2a.toByte(), 0x86.toByte(), 0x48, 0x86.toByte(), 0xf7.toByte(), 0x0d, 0x01, 0x01, 0x01, // rsaEncryption OID
            0x05, 0x00, // NULL
            0x04, 0x82.toByte(), 0x00, 0x00 // OCTET STRING, length (will be filled)
        )
        
        val totalLength = pkcs8Header.size + pkcs1Bytes.size
        val result = ByteArray(totalLength)
        
        // Copy header
        System.arraycopy(pkcs8Header, 0, result, 0, pkcs8Header.size)
        
        // Fill in lengths
        val keyLength = pkcs1Bytes.size
        result[2] = ((totalLength - 4) shr 8).toByte()
        result[3] = ((totalLength - 4) and 0xff).toByte()
        result[pkcs8Header.size - 2] = (keyLength shr 8).toByte()
        result[pkcs8Header.size - 1] = (keyLength and 0xff).toByte()
        
        // Copy PKCS1 key
        System.arraycopy(pkcs1Bytes, 0, result, pkcs8Header.size, pkcs1Bytes.size)
        
        return result
    }
    
    /**
     * 构建授权参数字符串
     */
    private fun buildAuthParamString(params: Map<String, String>): String {
        val sortedParams = params.toSortedMap()
        val stringBuilder = StringBuilder()
        
        for ((key, value) in sortedParams) {
            if (stringBuilder.isNotEmpty()) {
                stringBuilder.append("&")
            }
            stringBuilder.append(key).append("=").append(urlEncode(value))
        }
        
        return stringBuilder.toString()
    }
    
    /**
     * URL编码
     */
    private fun urlEncode(value: String): String {
        return try {
            URLEncoder.encode(value, CHARSET)
        } catch (e: UnsupportedEncodingException) {
            value
        }
    }
    
    /**
     * 解析授权结果
     */
    private fun parseAuthResult(result: Map<String, String>): AuthResult {
        val resultStatus = result["resultStatus"] ?: ""
        val resultCode = result["result"] ?: ""
        val memo = result["memo"] ?: ""
        
        Log.d(TAG, "授权结果: resultStatus=$resultStatus, resultCode=$resultCode, memo=$memo")
        
        return when (resultStatus) {
            AUTH_CODE_SUCCESS -> {
                // 解析成功结果中的用户信息
                val authCode = extractAuthCode(resultCode)
                val userId = extractUserId(resultCode)
                val alipayUserId = extractAlipayUserId(resultCode)
                
                AuthResult(
                    success = true,
                    resultCode = resultStatus,
                    resultMessage = "授权成功",
                    authCode = authCode,
                    userId = userId,
                    alipayUserId = alipayUserId
                )
            }
            AUTH_CODE_PROCESSING -> {
                AuthResult(
                    success = false,
                    resultCode = resultStatus,
                    resultMessage = "授权处理中",
                    authCode = "",
                    userId = "",
                    alipayUserId = ""
                )
            }
            AUTH_CODE_CANCEL -> {
                AuthResult(
                    success = false,
                    resultCode = resultStatus,
                    resultMessage = "用户取消授权",
                    authCode = "",
                    userId = "",
                    alipayUserId = ""
                )
            }
            AUTH_CODE_NETWORK_ERROR -> {
                AuthResult(
                    success = false,
                    resultCode = resultStatus,
                    resultMessage = "网络错误",
                    authCode = "",
                    userId = "",
                    alipayUserId = ""
                )
            }
            else -> {
                // 根据错误码和描述提供更具体的错误信息
                val errorMessage = when {
                    memo.contains("交换订单处理失败") || memo.contains("exchange order") -> 
                        "交换订单处理失败，可能的原因：\n1. APP_ID未在支付宝开放平台审核通过\n2. 应用密钥配置错误\n3. 应用授权功能未开通\n4. 请检查支付宝开放平台应用状态"
                    memo.contains("系统繁忙") || memo.contains("系统正忙") -> 
                        "系统繁忙，可能的原因：\n1. APP_ID配置错误\n2. 密钥配置错误\n3. 网络连接问题\n4. 支付宝服务暂时不可用"
                    memo.contains("应用未上线") -> 
                        "应用尚未在支付宝开放平台上线，请在支付宝开放平台提交应用审核"
                    memo.contains("参数错误") || memo.contains("INVALID_PARAMETER") -> 
                        "参数配置错误，请检查支付宝APP_ID和密钥配置"
                    memo.contains("签名错误") || memo.contains("SIGN_ERROR") -> 
                        "签名验证失败，请检查RSA私钥和公钥配置"
                    memo.contains("应用被禁用") -> 
                        "应用已被禁用，请联系支付宝客服或检查开放平台应用状态"
                    memo.contains("权限不足") -> 
                        "权限不足，请确认已开通用户信息授权功能"
                    resultStatus == "6001" -> 
                        "用户中途取消了授权"
                    resultStatus == "6002" -> 
                        "网络连接出错，请检查网络状况"
                    resultStatus == "4000" -> 
                        "授权失败，请检查支付宝配置或联系技术支持"
                    memo.isNotEmpty() -> 
                        "授权失败: $memo\n\n错误码: $resultStatus\n请检查支付宝开放平台配置"
                    else -> 
                        "未知授权错误，错误码：$resultStatus\n请检查支付宝配置或联系技术支持"
                }
                
                Log.e(TAG, "授权失败详情:")
                Log.e(TAG, "  resultStatus: $resultStatus")
                Log.e(TAG, "  memo: $memo") 
                Log.e(TAG, "  resultCode: $resultCode")
                Log.e(TAG, "  处理建议: $errorMessage")
                
                AuthResult(
                    success = false,
                    resultCode = resultStatus,
                    resultMessage = errorMessage,
                    authCode = "",
                    userId = "",
                    alipayUserId = ""
                )
            }
        }
    }
    
    /**
     * 从结果中提取授权码
     */
    private fun extractAuthCode(resultCode: String): String {
        // 实际项目中需要根据支付宝返回的格式来解析
        // 这里是示例实现
        return try {
            val params = parseResultParams(resultCode)
            params["auth_code"] ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "提取授权码失败: ${e.message}")
            ""
        }
    }
    
    /**
     * 从结果中提取用户ID
     */
    private fun extractUserId(resultCode: String): String {
        return try {
            val params = parseResultParams(resultCode)
            params["user_id"] ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "提取用户ID失败: ${e.message}")
            ""
        }
    }
    
    /**
     * 从结果中提取支付宝用户ID
     */
    private fun extractAlipayUserId(resultCode: String): String {
        return try {
            val params = parseResultParams(resultCode)
            params["alipay_user_id"] ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "提取支付宝用户ID失败: ${e.message}")
            ""
        }
    }
    
    /**
     * 解析结果参数
     */
    private fun parseResultParams(resultCode: String): Map<String, String> {
        val params = mutableMapOf<String, String>()
        
        // 简单的参数解析实现
        val pairs = resultCode.split("&")
        for (pair in pairs) {
            val keyValue = pair.split("=")
            if (keyValue.size == 2) {
                params[keyValue[0]] = keyValue[1]
            }
        }
        
        return params
    }
    
    /**
     * 获取用户信息
     * 
     * @param authCode 授权码
     * @return 用户信息
     */
    suspend fun getUserInfo(@Suppress("UNUSED_PARAMETER") authCode: String): UserInfo? = withContext(Dispatchers.IO) {
        try {
            // 这里需要调用支付宝的用户信息API
            // 实际项目中需要实现具体的API调用
            
            // 示例返回
            UserInfo(
                userId = "example_user_id",
                nickName = "支付宝用户",
                avatar = "",
                gender = "未知",
                province = "",
                city = ""
            )
        } catch (e: Exception) {
            Log.e(TAG, "获取用户信息失败: ${e.message}")
            null
        }
    }
    
    /**
     * 授权结果数据类
     */
    data class AuthResult(
        val success: Boolean,
        val resultCode: String,
        val resultMessage: String,
        val authCode: String,
        val userId: String,
        val alipayUserId: String
    )
    
    /**
     * 用户信息数据类
     */
    data class UserInfo(
        val userId: String,
        val nickName: String,
        val avatar: String,
        val gender: String,
        val province: String,
        val city: String
    )
    
    // Interface implementations
    override suspend fun login(vararg params: String): Flow<LoginResult> = flow {
        emit(LoginResult.Loading)
        try {
            // Alipay登录逻辑实现
            // params可以包含必要的参数，但Alipay登录主要通过Activity处理
            emit(LoginResult.Error("请通过authorize方法进行支付宝登录"))
        } catch (e: Exception) {
            emit(LoginResult.Error("支付宝登录失败: ${e.message}", e))
        }
    }
    
    override suspend fun logout(): Boolean {
        return try {
            // 支付宝没有特定的登出方法，返回true表示成功
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override fun isLoggedIn(): Boolean {
        // 简化实现 - 实际应该检查本地存储的登录状态
        return false
    }
    
    override fun getCurrentUserId(): String? {
        // 简化实现 - 实际应该返回当前登录用户的支付宝ID
        return null
    }
} 