package com.offtime.app.manager

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.offtime.app.BuildConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import com.offtime.app.manager.interfaces.PaymentManager
import com.offtime.app.manager.interfaces.PaymentResult
import com.offtime.app.manager.interfaces.PaymentProduct
import com.offtime.app.data.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.resume

/**
 * Google Play计费管理类
 * 负责处理Google Play内购相关功能
 */
@Singleton
class GooglePlayBillingManager @Inject constructor(
    private val context: Context,
    private val userRepository: UserRepository
) : PaymentManager, PurchasesUpdatedListener, BillingClientStateListener {
    
    companion object {
        private const val TAG = "GooglePlayBilling"
        
        // 一次性购买商品ID - 需要在Google Play Console中配置
        const val PREMIUM_LIFETIME_SKU = "premium_lifetime"
        
        // 价格配置
        const val PREMIUM_LIFETIME_PRICE_USD = "9.90"  // 9.9美元一次性购买
        
        // 连接重试配置
        private const val MAX_RETRY_COUNT = 3
    }
    
    private var billingClient: BillingClient? = null
    private var isServiceConnected = false
    private var purchaseCallback: ((PurchaseResult) -> Unit)? = null
    private var retryCount = 0
    
    override suspend fun pay(activity: Activity, productId: String): Flow<PaymentResult> = flow {
        Log.d(TAG, "🚀 开始支付流程: productId=$productId")
        emit(PaymentResult.Loading)
        
        if (!isServiceConnected) {
            Log.e(TAG, "❌ 计费服务未连接")
            emit(PaymentResult.Error("Billing service not connected"))
            return@flow
        }
        
        try {
            // 🧪 Debug模式下的模拟支付逻辑
            if (com.offtime.app.BuildConfig.DEBUG && isServiceConnected && billingClient?.isReady != true) {
                Log.w(TAG, "🧪 使用模拟支付模式（开发测试）")
                
                // 模拟用户确认支付的对话框
                Log.d(TAG, "💰 模拟支付: 产品=$productId, 价格=$PREMIUM_LIFETIME_PRICE_USD USD")
                
                // 延迟2秒模拟支付处理时间
                kotlinx.coroutines.delay(2000)
                
                Log.d(TAG, "✅ 模拟支付成功！")
                emit(PaymentResult.Success("模拟支付成功", null))
                return@flow
            }
            
            Log.d(TAG, "🔍 查询产品详情...")
            // 1. 查询一次性购买商品详情（不是订阅）
            val products = queryInAppProducts()
            Log.d(TAG, "📦 查询到 ${products.size} 个产品")
            
            val productDetails = products.find { it.productId == productId }
            
            if (productDetails == null) {
                Log.e(TAG, "❌ 产品未找到: $productId")
                
                // 🧪 完全模拟支付模式 - 当产品未在Google Play Console配置时
                if (com.offtime.app.BuildConfig.DEBUG) {
                    Log.w(TAG, "🧪 产品未配置，启用完全模拟支付模式")
                    Log.d(TAG, "💰 模拟支付: 产品=$productId, 价格=$PREMIUM_LIFETIME_PRICE_USD USD")
                    kotlinx.coroutines.delay(2000)
                    Log.d(TAG, "✅ 模拟支付成功！")
                    emit(PaymentResult.Success("模拟支付成功 (产品未配置)", null))
                    return@flow
                }
                
                emit(PaymentResult.Error("Product not found: $productId"))
                return@flow
            }
            
            Log.d(TAG, "✅ 找到产品: ${productDetails.name}, 价格: ${productDetails.oneTimePurchaseOfferDetails?.formattedPrice}")
            
            // 2. 启动购买流程
            Log.d(TAG, "🛒 启动购买流程...")
            val purchaseStarted = launchBillingFlow(activity, productDetails)
            if (!purchaseStarted) {
                Log.e(TAG, "❌ 启动购买流程失败")
                emit(PaymentResult.Error("Failed to launch billing flow"))
            } else {
                Log.d(TAG, "✅ 购买流程已启动，等待用户操作...")
            }
            // 购买结果将通过 onPurchasesUpdated 回调处理
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 支付流程异常", e)
            emit(PaymentResult.Error("Payment failed: ${e.message}", e))
        }
    }
    
    override suspend fun queryPaymentStatus(orderId: String): PaymentResult {
        return PaymentResult.Error("查询功能暂未实现")
    }
    
    override suspend fun getAvailableProducts(): List<PaymentProduct> {
        return listOf(
            PaymentProduct(
                productId = PREMIUM_LIFETIME_SKU,
                title = "OffTimes Premium Lifetime",
                description = "Unlock all premium features with one-time purchase",
                price = PREMIUM_LIFETIME_PRICE_USD,
                currency = "USD"
            )
        )
    }
    
    override fun isPaymentAvailable(): Boolean {
        return billingClient?.isReady == true
    }

    /**
     * 初始化计费客户端
     */
    fun initialize() {
        Log.d(TAG, "🔧 开始初始化GooglePlayBillingManager...")
        
        if (billingClient == null) {
            Log.d(TAG, "📦 创建新的BillingClient实例")
            @Suppress("DEPRECATION")
            billingClient = BillingClient.newBuilder(context)
                .setListener(this)
                .enablePendingPurchases() // Required for Google Play Billing Library 7.0.0
                .build()
            Log.d(TAG, "✅ BillingClient创建成功: $billingClient")
        } else {
            Log.d(TAG, "♻️ BillingClient已存在，跳过创建")
        }
        
        Log.d(TAG, "🔗 开始连接到Google Play计费服务...")
        startConnection()
    }
    
    private fun startConnection() {
        Log.d(TAG, "📡 调用BillingClient.startConnection()...")
        billingClient?.startConnection(this)
    }
    
    override fun onBillingSetupFinished(billingResult: BillingResult) {
        Log.d(TAG, "📋 收到计费服务连接结果: responseCode=${billingResult.responseCode}")
        Log.d(TAG, "📋 连接结果详情: ${billingResult.debugMessage}")
        
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            isServiceConnected = true
            Log.d(TAG, "✅ 计费服务连接成功！")
            Log.d(TAG, "🔍 BillingClient状态: isReady=${billingClient?.isReady}")
        } else {
            isServiceConnected = false
            Log.e(TAG, "❌ 计费服务连接失败: responseCode=${billingResult.responseCode}")
            Log.e(TAG, "❌ 失败详情: ${billingResult.debugMessage}")
            
            // 🔧 开发测试模式：如果是API版本不支持的错误，启用模拟模式
            if (billingResult.responseCode == BillingClient.BillingResponseCode.BILLING_UNAVAILABLE &&
                billingResult.debugMessage?.contains("API version") == true &&
                com.offtime.app.BuildConfig.DEBUG) {
                
                Log.w(TAG, "🧪 检测到Google Play API版本不兼容，启用开发测试模拟模式")
                isServiceConnected = true // 模拟连接成功
                return
            }
            
            // 尝试重新连接（但限制重试次数）
            if (retryCount < MAX_RETRY_COUNT) {
                retryCount++
                Log.d(TAG, "⏰ 5秒后尝试重新连接... (第${retryCount}次重试)")
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    Log.d(TAG, "🔄 开始重新连接计费服务...")
                    startConnection()
                }, 5000)
            } else {
                Log.e(TAG, "❌ 已达到最大重试次数($MAX_RETRY_COUNT)，停止重试")
                if (com.offtime.app.BuildConfig.DEBUG) {
                    Log.w(TAG, "🧪 Debug模式：启用模拟支付模式")
                    isServiceConnected = true // Debug模式下启用模拟模式
                }
            }
        }
    }
    
    override fun onBillingServiceDisconnected() {
        isServiceConnected = false
        Log.w(TAG, "⚠️ 计费服务连接断开")
        
        // 自动重连
        Log.d(TAG, "🔄 计费服务断开，开始自动重连...")
        startConnection()
    }
    
    /**
     * 查询可用的一次性购买商品
     */
    suspend fun queryInAppProducts(): List<ProductDetails> = suspendCancellableCoroutine { continuation ->
        if (!isServiceConnected) {
            Log.e(TAG, "计费服务未连接，无法查询产品")
            continuation.resume(emptyList())
            return@suspendCancellableCoroutine
        }
        
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PREMIUM_LIFETIME_SKU)
                .setProductType(BillingClient.ProductType.INAPP) // 一次性购买
                .build()
        )
        
        val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()
        
        billingClient?.queryProductDetailsAsync(queryProductDetailsParams) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "查询一次性购买产品成功: ${productDetailsList.size} 个产品")
                continuation.resume(productDetailsList)
            } else {
                Log.e(TAG, "查询一次性购买产品失败: ${billingResult.debugMessage}")
                continuation.resume(emptyList())
            }
        }
    }

    /**
     * 查询可用的订阅商品
     */
    suspend fun querySubscriptionProducts(): List<ProductDetails> = suspendCancellableCoroutine { continuation ->
        if (!isServiceConnected) {
            Log.e(TAG, "计费服务未连接")
            continuation.resume(emptyList())
            return@suspendCancellableCoroutine
        }
        
        val productList = arrayListOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PREMIUM_LIFETIME_SKU)
                .setProductType(BillingClient.ProductType.INAPP)  // 一次性购买商品
                .build()
        )
        
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()
        
        billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "查询到 ${productDetailsList.size} 个订阅商品")
                continuation.resume(productDetailsList)
            } else {
                Log.e(TAG, "查询订阅商品失败: ${billingResult.debugMessage}")
                continuation.resume(emptyList())
            }
        }
    }
    
    /**
     * 启动购买流程
     */
    suspend fun launchBillingFlow(
        activity: Activity,
        productDetails: ProductDetails,
        @Suppress("UNUSED_PARAMETER") offerToken: String? = null
    ): Boolean = suspendCancellableCoroutine { continuation ->
        if (!isServiceConnected) {
            Log.e(TAG, "计费服务未连接")
            continuation.resume(false)
            return@suspendCancellableCoroutine
        }
        
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .apply {
                    // 一次性购买商品不需要offer token
                }
                .build()
        )
        
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()
        
        // 设置购买回调，用于处理购买结果
        purchaseCallback = { result ->
            continuation.resume(result.success)
        }
        
        val billingResult = billingClient?.launchBillingFlow(activity, billingFlowParams)
        
        if (billingResult?.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e(TAG, "启动购买流程失败: ${billingResult?.debugMessage}")
            purchaseCallback = null
            continuation.resume(false)
        }
    }
    
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "用户取消了购买")
            purchaseCallback?.invoke(PurchaseResult(false, "用户取消了购买", null))
        } else {
            Log.e(TAG, "购买失败: ${billingResult.debugMessage}")
            purchaseCallback?.invoke(PurchaseResult(false, "购买失败: ${billingResult.debugMessage}", null))
        }
        purchaseCallback = null
    }
    
    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            // 购买成功，需要确认购买（对于订阅商品）
            if (!purchase.isAcknowledged) {
                acknowledgePurchase(purchase)
            }
            
            Log.d(TAG, "购买成功: ${purchase.products}")
            
            // 🔥 新增：服务器端验证购买
            verifyPurchaseOnServer(purchase)
            
            purchaseCallback?.invoke(PurchaseResult(true, "购买成功", purchase))
        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            Log.d(TAG, "购买待确认: ${purchase.products}")
            purchaseCallback?.invoke(PurchaseResult(false, "购买待确认", purchase))
        }
    }
    
    /**
     * 在服务器端验证购买
     */
    private fun verifyPurchaseOnServer(purchase: Purchase) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "🔍 开始服务器端购买验证...")
                
                val productId = purchase.products.firstOrNull() ?: PREMIUM_LIFETIME_SKU
                val result = userRepository.verifyPurchase(
                    platform = "google_play",
                    productId = productId,
                    purchaseToken = purchase.purchaseToken,
                    orderId = purchase.orderId
                )
                
                if (result.isSuccess) {
                    Log.d(TAG, "✅ 服务器端购买验证成功")
                    val verificationResponse = result.getOrNull()
                    Log.d(TAG, "验证结果: ${verificationResponse}")
                } else {
                    Log.e(TAG, "❌ 服务器端购买验证失败: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 服务器端购买验证异常", e)
            }
        }
    }
    
    private fun acknowledgePurchase(purchase: Purchase) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        
        billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "购买确认成功")
            } else {
                Log.e(TAG, "购买确认失败: ${billingResult.debugMessage}")
            }
        }
    }
    
    /**
     * 查询当前用户的购买记录
     */
    suspend fun queryPurchases(): List<Purchase> = suspendCancellableCoroutine { continuation ->
        if (!isServiceConnected) {
            Log.e(TAG, "计费服务未连接")
            continuation.resume(emptyList())
            return@suspendCancellableCoroutine
        }
        
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)  // 一次性购买商品
            .build()
        
        billingClient?.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "查询到 ${purchases.size} 个购买记录")
                continuation.resume(purchases)
            } else {
                Log.e(TAG, "查询购买记录失败: ${billingResult.debugMessage}")
                continuation.resume(emptyList())
            }
        }
    }
    
    /**
     * 检查用户是否有有效的订阅
     */
    suspend fun hasValidSubscription(): Boolean {
        val purchases = queryPurchases()
        return purchases.any { purchase ->
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
            purchase.products.contains(PREMIUM_LIFETIME_SKU)
        }
    }
    
    /**
     * 恢复购买（查询现有购买并同步到服务器）
     */
    suspend fun restorePurchases(): Flow<PaymentResult> = flow {
        Log.d(TAG, "🔄 开始恢复购买...")
        emit(PaymentResult.Loading)
        
        try {
            // 1. 查询本地Google Play购买记录
            val localPurchases = queryPurchases()
            Log.d(TAG, "📱 本地查询到 ${localPurchases.size} 个购买记录")
            
            // 2. 验证每个购买记录到服务器
            var validPurchases = 0
            for (purchase in localPurchases) {
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    val productId = purchase.products.firstOrNull() ?: PREMIUM_LIFETIME_SKU
                    val result = userRepository.verifyPurchase(
                        platform = "google_play",
                        productId = productId,
                        purchaseToken = purchase.purchaseToken,
                        orderId = purchase.orderId
                    )
                    
                    if (result.isSuccess) {
                        validPurchases++
                        Log.d(TAG, "✅ 购买记录验证成功: ${purchase.orderId}")
                    }
                }
            }
            
            // 3. 从服务器恢复购买
            val serverRestoreResult = userRepository.restorePurchases()
            if (serverRestoreResult.isSuccess) {
                val restoreResponse = serverRestoreResult.getOrNull()!!
                Log.d(TAG, "🔄 服务器恢复结果: 恢复了${restoreResponse.restoredCount}个购买")
                
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
     * 同步付费状态（应用启动时调用）
     */
    suspend fun syncPurchaseStatus(): Flow<PaymentResult> = flow {
        Log.d(TAG, "🔄 开始同步付费状态...")
        emit(PaymentResult.Loading)
        
        try {
            // 1. 查询本地Google Play购买记录
            val localPurchases = queryPurchases()
            val hasLocalPurchases = localPurchases.any { 
                it.purchaseState == Purchase.PurchaseState.PURCHASED 
            }
            
            // 2. 从服务器获取付费状态
            val serverStatusResult = userRepository.getPurchaseStatusFromServer()
            
            if (serverStatusResult.isSuccess) {
                val serverStatus = serverStatusResult.getOrNull()!!
                Log.d(TAG, "📊 服务器付费状态: isPremium=${serverStatus.isPremium}")
                
                // 3. 如果本地有购买但服务器没有，尝试验证
                if (hasLocalPurchases && !serverStatus.isPremium) {
                    Log.d(TAG, "🔍 本地有购买但服务器无记录，开始验证...")
                    for (purchase in localPurchases) {
                        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                            verifyPurchaseOnServer(purchase)
                        }
                    }
                }
                
                emit(PaymentResult.Success(
                    "付费状态同步完成", 
                    null
                ))
            } else {
                Log.w(TAG, "⚠️ 获取服务器付费状态失败，使用本地状态")
                emit(PaymentResult.Success(
                    "使用本地付费状态", 
                    null
                ))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 同步付费状态异常", e)
            emit(PaymentResult.Error("同步付费状态失败: ${e.message}"))
        }
    }
    
    /**
     * 释放资源
     */
    fun release() {
        billingClient?.endConnection()
        billingClient = null
        isServiceConnected = false
    }
}

/**
 * 购买结果数据类
 */
data class PurchaseResult(
    val success: Boolean,
    val message: String,
    val purchase: Purchase?
)