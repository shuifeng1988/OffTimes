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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
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
    }
    
    private var billingClient: BillingClient? = null
    private var isServiceConnected = false
    private var purchaseCallback: ((PurchaseResult) -> Unit)? = null
    
    override fun pay(activity: Activity, productId: String): Flow<PaymentResult> = flow {
        emit(PaymentResult.Loading)
        if (!isServiceConnected) {
            emit(PaymentResult.Error("Billing service not connected"))
            return@flow
        }
        
        try {
            // 1. 查询商品详情
            val products = querySubscriptionProducts()
            val productDetails = products.find { it.productId == productId }
            
            if (productDetails == null) {
                emit(PaymentResult.Error("Product not found: $productId"))
                return@flow
            }
            
            // 2. 启动购买流程
            val purchaseStarted = launchBillingFlow(activity, productDetails)
            if (!purchaseStarted) {
                emit(PaymentResult.Error("Failed to launch billing flow"))
            }
            // The result will be delivered asynchronously to onPurchasesUpdated
            // We can emit a success here to indicate the flow was launched.
            // Or better, let the callback handle the final state.
            // For now, we assume the launch is the "Success" of this step.
            
        } catch (e: Exception) {
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
        if (billingClient == null) {
            @Suppress("DEPRECATION")
            billingClient = BillingClient.newBuilder(context)
                .setListener(this)
                .enablePendingPurchases() // Required for Google Play Billing Library 7.0.0
                .build()
        }
        
        startConnection()
    }
    
    private fun startConnection() {
        billingClient?.startConnection(this)
    }
    
    override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            isServiceConnected = true
            Log.d(TAG, "计费服务连接成功")
        } else {
            Log.e(TAG, "计费服务连接失败: ${billingResult.debugMessage}")
        }
    }
    
    override fun onBillingServiceDisconnected() {
        isServiceConnected = false
        Log.w(TAG, "计费服务连接断开")
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
            // 购买成功
            Log.d(TAG, "✅ 购买成功: ${purchase.products}")

            // 确认购买
            if (!purchase.isAcknowledged) {
                acknowledgePurchase(purchase)
            } else {
                // 如果已经确认，直接处理用户升级
                CoroutineScope(Dispatchers.IO).launch {
                    userRepository.upgradeToPremium()
                }
            }

            purchaseCallback?.invoke(PurchaseResult(true, "购买成功", purchase))
        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            Log.d(TAG, "⏳ 购买待确认: ${purchase.products}")
            purchaseCallback?.invoke(PurchaseResult(false, "购买待确认", purchase))
        }
    }
    
    private fun acknowledgePurchase(purchase: Purchase) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        
        billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "✅ 购买确认成功: ${purchase.products}")
                // 确认成功后，升级用户为付费会员
                CoroutineScope(Dispatchers.IO).launch {
                    userRepository.upgradeToPremium()
                }
            } else {
                Log.e(TAG, "❌ 购买确认失败: ${billingResult.debugMessage}")
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