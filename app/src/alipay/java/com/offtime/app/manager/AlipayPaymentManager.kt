package com.offtime.app.manager

import android.app.Activity
import android.content.Context
import android.text.TextUtils
import android.util.Log
import com.alipay.sdk.app.EnvUtils
import com.alipay.sdk.app.PayTask
import com.offtime.app.BuildConfig
import com.offtime.app.manager.interfaces.PaymentManager
import com.offtime.app.manager.interfaces.PaymentResult
import com.offtime.app.manager.interfaces.PaymentProduct
import com.offtime.app.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
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

/**
 * 支付宝支付管理类
 * 负责处理支付宝支付相关的功能
 * 
 * 主要功能：
 * - 发起支付宝支付
 * - 处理支付结果
 * - 查询支付状态
 */
@Singleton
class AlipayPaymentManager @Inject constructor(
    private val context: Context,
    private val userRepository: UserRepository
) : PaymentManager {
    
    companion object {
        private const val TAG = "AlipayPaymentManager"
        
        // 签名方式
        private const val SIGN_TYPE = "RSA2"
        
        // 字符编码格式
        private const val CHARSET = "utf-8"
        
        // 支付宝网关（正式环境）
        private const val URL = "https://openapi.alipay.com/gateway.do"
        
        // 支付宝网关（沙箱环境）
        private const val SANDBOX_URL = "https://openapi.alipaydev.com/gateway.do"
    }
    
    // 当前是否为沙箱环境
    private val isSandbox = BuildConfig.ALIPAY_IS_SANDBOX
    
    init {
        // 设置支付宝环境
        if (isSandbox) {
            EnvUtils.setEnv(EnvUtils.EnvEnum.SANDBOX)
        } else {
            EnvUtils.setEnv(EnvUtils.EnvEnum.ONLINE)
        }
        
        Log.d(TAG, "AlipayPaymentManager initialized, sandbox: $isSandbox")
    }
    
    override suspend fun pay(activity: Activity, productId: String): Flow<PaymentResult> = flow {
        // 在支付宝支付中，productId可能对应服务器上的不同商品
        // 这里为了简化，我们假设productId映射到固定的价格
        val amount = when (productId) {
            "premium_lifetime" -> "9.90"
            else -> "0.01" // 默认测试金额
        }

        emit(PaymentResult.Loading)
        
        try {
            // 构建订单信息
            val orderInfo = buildOrderInfo(productId, amount)
            Log.d(TAG, "Generated order info: $orderInfo")
            
            // 对订单信息进行签名
            val signedOrderInfo = signOrderInfo(orderInfo)
            Log.d(TAG, "Signed order info: $signedOrderInfo")
            
            // 在子线程中调用支付宝支付 - 使用传入的Activity而不是Context
            val result = withContext(Dispatchers.IO) {
                val payTask = PayTask(activity)
                payTask.payV2(signedOrderInfo, true)
            }
            
            Log.d(TAG, "Payment result: $result")
            
            // 解析支付结果
            val paymentResult = parsePaymentResult(result, productId)
            emit(paymentResult)
            
        } catch (e: Exception) {
            Log.e(TAG, "Payment failed", e)
            emit(PaymentResult.Error("支付异常: ${e.message}", e))
        }
    }
    
    override suspend fun queryPaymentStatus(orderId: String): PaymentResult {
        // 这里可以实现支付状态查询逻辑
        return PaymentResult.Error("支付状态查询暂未实现")
    }
    
    override suspend fun getAvailableProducts(): List<PaymentProduct> {
        // 返回可用的支付产品
        return listOf(
            PaymentProduct(
                productId = "premium_monthly",
                title = "高级版 - 月度订阅",
                description = "享受所有高级功能，月度订阅",
                price = "9.9",
                currency = "CNY"
            ),
            PaymentProduct(
                productId = "premium_yearly",
                title = "高级版 - 年度订阅",
                description = "享受所有高级功能，年度订阅更优惠",
                price = "99.9",
                currency = "CNY"
            )
        )
    }
    
    override fun isPaymentAvailable(): Boolean {
        return true // 支付宝支付总是可用的
    }
    
    /**
     * 构建订单信息字符串
     */
    private fun buildOrderInfo(productId: String, amount: String): String {
        val orderInfoMap = linkedMapOf<String, String>()
        
        // 基本参数
        orderInfoMap["app_id"] = BuildConfig.ALIPAY_APP_ID
        orderInfoMap["biz_content"] = buildBizContent(productId, amount)
        orderInfoMap["charset"] = CHARSET
        orderInfoMap["method"] = "alipay.trade.app.pay"
        orderInfoMap["sign_type"] = SIGN_TYPE
        orderInfoMap["timestamp"] = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        orderInfoMap["version"] = "1.0"
        
        return buildQuery(orderInfoMap, CHARSET)
    }
    
    /**
     * 构建业务参数
     */
    private fun buildBizContent(productId: String, amount: String): String {
        val bizContentMap = linkedMapOf<String, String>()
        bizContentMap["out_trade_no"] = generateOutTradeNo()
        bizContentMap["product_code"] = "QUICK_MSECURITY_PAY"
        bizContentMap["total_amount"] = amount
        bizContentMap["subject"] = getProductTitle(productId)
        
        return mapToJson(bizContentMap)
    }
    
    /**
     * 生成商户订单号
     */
    private fun generateOutTradeNo(): String {
        return "OFFTIME_${System.currentTimeMillis()}"
    }
    
    /**
     * 根据产品ID获取产品标题
     */
    private fun getProductTitle(productId: String): String {
        return when (productId) {
            "premium_monthly" -> "OffTimes 高级版月度订阅"
            "premium_yearly" -> "OffTimes 高级版年度订阅"
            else -> "OffTimes 高级版"
        }
    }
    
    /**
     * 对订单信息进行RSA签名
     */
    private fun signOrderInfo(orderInfo: String): String {
        return try {
            val privateKeyString = BuildConfig.ALIPAY_MERCHANT_PRIVATE_KEY.replace("\\n", "\n")
            val sign = rsaSign(orderInfo, privateKeyString, CHARSET)
            val encodedSign = URLEncoder.encode(sign, CHARSET)
            "$orderInfo&sign=$encodedSign"
        } catch (e: Exception) {
            Log.e(TAG, "Sign order info failed", e)
            orderInfo
        }
    }
    
    /**
     * RSA签名
     */
    private fun rsaSign(content: String, privateKey: String, charset: String): String {
        try {
            // 清理私钥格式：移除头尾标识和换行符
            val cleanPrivateKey = privateKey
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replace("\n", "")
                .replace("\r", "")
                .replace(" ", "")
                .trim()
            
            Log.d(TAG, "Private key length after cleaning: ${cleanPrivateKey.length}")
            
            val priPKCS8 = PKCS8EncodedKeySpec(Base64.decode(cleanPrivateKey, Base64.NO_WRAP))
            val keyFactory = KeyFactory.getInstance("RSA")
            val priKey: PrivateKey = keyFactory.generatePrivate(priPKCS8)
            val signature = Signature.getInstance("SHA256WithRSA")
            signature.initSign(priKey)
            signature.update(content.toByteArray(charset(charset)))
            val signed = signature.sign()
            return Base64.encodeToString(signed, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "RSA sign failed", e)
            throw e
        }
    }
    
    /**
     * 解析支付结果
     */
    private fun parsePaymentResult(result: Map<String, String>, productId: String): PaymentResult {
        val resultStatus = result["resultStatus"]
        
        return when (resultStatus) {
            "9000" -> {
                // 支付成功
                val resultData = result["result"] ?: ""
                val orderId = extractOrderId(resultData)
                
                // 🔥 新增：支付成功后进行服务器验证
                verifyPaymentOnServer(productId, orderId, resultData)
                
                PaymentResult.Success(orderId, resultData)
            }
            "8000" -> {
                // 支付结果确认中
                PaymentResult.Error("支付结果确认中，请稍后查询")
            }
            "6001" -> {
                // 用户中途取消
                PaymentResult.Cancelled
            }
            "6002" -> {
                // 网络连接出错
                PaymentResult.Error("网络连接出错，请检查网络设置")
            }
            "4000" -> {
                // 支付错误
                val memo = result["memo"] ?: "支付失败"
                PaymentResult.Error("支付失败: $memo")
            }
            else -> {
                val memo = result["memo"] ?: "未知错误"
                PaymentResult.Error("支付异常: $memo")
            }
        }
    }
    
    /**
     * 在服务器端验证支付宝支付
     */
    private fun verifyPaymentOnServer(productId: String, orderId: String, resultData: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "🔍 开始支付宝服务器端验证...")
                
                // 使用订单号作为购买令牌（支付宝特有）
                val result = userRepository.verifyPurchase(
                    platform = "alipay",
                    productId = productId,
                    purchaseToken = orderId,
                    orderId = orderId
                )
                
                if (result.isSuccess) {
                    Log.d(TAG, "✅ 支付宝服务器端验证成功")
                    val verificationResponse = result.getOrNull()
                    Log.d(TAG, "验证结果: ${verificationResponse}")
                } else {
                    Log.e(TAG, "❌ 支付宝服务器端验证失败: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 支付宝服务器端验证异常", e)
            }
        }
    }
    
    /**
     * 从支付结果中提取订单号
     */
    private fun extractOrderId(@Suppress("UNUSED_PARAMETER") result: String): String {
        // 这里应该解析返回的JSON获取订单号
        // 简化实现，实际项目中应该使用JSON解析库
        return "ORDER_${System.currentTimeMillis()}"
    }
    
    /**
     * 构建请求参数字符串
     */
    private fun buildQuery(params: Map<String, String>, charset: String): String {
        val sortedParams = params.toSortedMap()
        val query = StringBuilder()
        
        for ((key, value) in sortedParams) {
            if (query.isNotEmpty()) {
                query.append("&")
            }
            query.append(key).append("=").append(URLEncoder.encode(value, charset))
        }
        
        return query.toString()
    }
    
    /**
     * 将Map转换为JSON字符串（简化实现）
     */
    private fun mapToJson(map: Map<String, String>): String {
        val json = StringBuilder("{")
        val iterator = map.entries.iterator()
        
        while (iterator.hasNext()) {
            val entry = iterator.next()
            json.append("\"").append(entry.key).append("\":\"").append(entry.value).append("\"")
            if (iterator.hasNext()) {
                json.append(",")
            }
        }
        
        json.append("}")
        return json.toString()
    }
    
    // ===== 跨设备付费同步功能 =====
    
    /**
     * 恢复购买（支付宝版本）
     * 支付宝没有本地购买记录查询，主要依赖服务器端恢复
     */
    suspend fun restorePurchases(): Flow<PaymentResult> = flow {
        Log.d(TAG, "🔄 开始恢复支付宝购买...")
        emit(PaymentResult.Loading)
        
        try {
            // 支付宝主要通过服务器端恢复购买记录
            val serverRestoreResult = userRepository.restorePurchases()
            
            if (serverRestoreResult.isSuccess) {
                val restoreResponse = serverRestoreResult.getOrNull()!!
                Log.d(TAG, "🔄 支付宝恢复结果: 恢复了${restoreResponse.restoredCount}个购买")
                
                emit(PaymentResult.Success(
                    "恢复购买成功，共恢复${restoreResponse.restoredCount}个有效购买", 
                    null
                ))
            } else {
                emit(PaymentResult.Error("恢复购买失败: ${serverRestoreResult.exceptionOrNull()?.message}"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 恢复购买异常", e)
            emit(PaymentResult.Error("恢复购买失败: ${e.message}"))
        }
    }
    
    /**
     * 同步付费状态（支付宝版本）
     */
    suspend fun syncPurchaseStatus(): Flow<PaymentResult> = flow {
        Log.d(TAG, "🔄 开始同步支付宝付费状态...")
        emit(PaymentResult.Loading)
        
        try {
            // 从服务器获取付费状态
            val serverStatusResult = userRepository.getPurchaseStatusFromServer()
            
            if (serverStatusResult.isSuccess) {
                val serverStatus = serverStatusResult.getOrNull()!!
                Log.d(TAG, "📊 服务器付费状态: isPremium=${serverStatus.isPremium}")
                
                emit(PaymentResult.Success(
                    "付费状态同步完成", 
                    null
                ))
            } else {
                Log.w(TAG, "⚠️ 获取服务器付费状态失败")
                emit(PaymentResult.Error("同步付费状态失败: ${serverStatusResult.exceptionOrNull()?.message}"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 同步付费状态异常", e)
            emit(PaymentResult.Error("同步付费状态失败: ${e.message}"))
        }
    }
    
    /**
     * 检查支付宝配置状态
     */
    fun checkAlipayConfiguration(): Map<String, Any> {
        return mapOf(
            "isConfigured" to (BuildConfig.ALIPAY_APP_ID.isNotEmpty() && 
                              BuildConfig.ALIPAY_MERCHANT_PRIVATE_KEY.isNotEmpty()),
            "hasAppId" to BuildConfig.ALIPAY_APP_ID.isNotEmpty(),
            "hasPrivateKey" to BuildConfig.ALIPAY_MERCHANT_PRIVATE_KEY.isNotEmpty(),
            "hasPublicKey" to BuildConfig.ALIPAY_PUBLIC_KEY.isNotEmpty(),
            "isSandbox" to isSandbox,
            "appId" to BuildConfig.ALIPAY_APP_ID
        )
    }
}