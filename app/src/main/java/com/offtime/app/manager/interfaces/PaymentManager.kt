package com.offtime.app.manager.interfaces

import kotlinx.coroutines.flow.Flow
import android.app.Activity

/**
 * 支付管理器接口
 * 为不同的支付方式提供统一的接口
 */
interface PaymentManager {
    
    /**
     * 执行支付操作
     * @param activity 用于启动支付UI的Activity
     * @param productId 产品ID
     * @return 支付结果的Flow
     */
    suspend fun pay(activity: Activity, productId: String): Flow<PaymentResult>
    
    /**
     * 查询支付状态
     * @param orderId 订单ID
     */
    suspend fun queryPaymentStatus(orderId: String): PaymentResult
    
    /**
     * 获取可用的支付产品列表
     */
    suspend fun getAvailableProducts(): List<PaymentProduct>
    
    /**
     * 检查支付功能是否可用
     */
    fun isPaymentAvailable(): Boolean
}

/**
 * 支付结果封装类
 */
sealed class PaymentResult {
    object Loading : PaymentResult()
    data class Success(val orderId: String, val transactionId: String? = null) : PaymentResult()
    data class Error(val message: String, val throwable: Throwable? = null) : PaymentResult()
    object Cancelled : PaymentResult()
}

/**
 * 支付产品信息
 */
data class PaymentProduct(
    val productId: String,
    val title: String,
    val description: String,
    val price: String,
    val currency: String = "CNY"
)