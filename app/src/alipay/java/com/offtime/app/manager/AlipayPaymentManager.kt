package com.offtime.app.manager

import android.app.Activity
import android.content.Context
import android.util.Log
import com.alipay.sdk.app.PayTask
import com.offtime.app.BuildConfig
import com.offtime.app.manager.interfaces.PaymentManager
import com.offtime.app.manager.interfaces.PaymentResult
import com.offtime.app.manager.interfaces.PaymentProduct
import com.alipay.sdk.app.PayResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import com.offtime.app.utils.AlipaySignUtils

@Singleton
class AlipayPaymentManager @Inject constructor(
    private val context: Context
) : PaymentManager {

    override fun pay(activity: Activity, productId: String): Flow<PaymentResult> = flow {
        emit(PaymentResult.Loading)
        val amount = when (productId) {
            "premium_lifetime" -> "9.90"
            else -> "0.01"
        }
        try {
            val orderInfo = getOrderInfo("OffTimes Premium", "Lifetime Access", amount)
            val payTask = PayTask(activity)
            val result = withContext(Dispatchers.IO) {
                payTask.payV2(orderInfo, true)
            }
            val payResult = PayResult(result)
            
            when (payResult.resultStatus) {
                "9000" -> emit(PaymentResult.Success(payResult.result))
                "8000" -> emit(PaymentResult.Error("支付结果确认中"))
                "6001" -> emit(PaymentResult.Cancelled)
                else -> emit(PaymentResult.Error("支付失败: ${payResult.memo}"))
            }
        } catch (e: Exception) {
            emit(PaymentResult.Error("支付异常: ${e.message}", e))
        }
    }

    private fun getOrderInfo(subject: String, body: String, price: String): String {
        val orderParams = mutableMapOf<String, String>()
        orderParams["app_id"] = BuildConfig.ALIPAY_APP_ID
        orderParams["method"] = "alipay.trade.app.pay"
        orderParams["format"] = "JSON"
        orderParams["charset"] = "utf-8"
        orderParams["sign_type"] = "RSA2"
        orderParams["timestamp"] = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        orderParams["version"] = "1.0"
        orderParams["notify_url"] = "YOUR_NOTIFY_URL"

        val bizContent = mutableMapOf<String, String>()
        bizContent["subject"] = subject
        bizContent["body"] = body
        bizContent["out_trade_no"] = getOutTradeNo()
        bizContent["total_amount"] = price
        bizContent["product_code"] = "QUICK_MSECURITY_PAY"
        
        orderParams["biz_content"] = bizContent.map { (k, v) -> """"$k":"$v"""" }.joinToString(",", "{", "}")

        val orderInfo = orderParams.entries.joinToString("&") { (k, v) -> "$k=${URLEncoder.encode(v, "UTF-8")}" }
        
        val sign = AlipaySignUtils.sign(orderInfo, BuildConfig.ALIPAY_MERCHANT_PRIVATE_KEY, true)
        return "$orderInfo&sign=${URLEncoder.encode(sign, "UTF-8")}"
    }

    private fun getOutTradeNo(): String {
        return "OFFTIME_${System.currentTimeMillis()}${(100..999).random()}"
    }

    override suspend fun queryPaymentStatus(orderId: String): PaymentResult {
        return PaymentResult.Error("支付状态查询暂未实现")
    }

    override suspend fun getAvailableProducts(): List<PaymentProduct> {
        return listOf(
            PaymentProduct(
                productId = "premium_lifetime",
                title = "OffTimes Premium",
                description = "Lifetime Access",
                price = "9.90",
                currency = "CNY"
            )
        )
    }

    override fun isPaymentAvailable(): Boolean = true
}