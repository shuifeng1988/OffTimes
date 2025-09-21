package com.offtime.app.ui.viewmodel

import android.app.Activity
import com.offtime.app.manager.interfaces.PaymentManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Named

class AlipayPaymentHelper @Inject constructor(
    @Named("alipay") private val alipayPaymentManager: PaymentManager?
) : PaymentHelper {

    override fun processPayment(activity: Activity, productId: String): Flow<PaymentResult> {
        if (alipayPaymentManager == null) {
            return kotlinx.coroutines.flow.flowOf(PaymentResult.Error("Alipay payment is not available"))
        }
        return alipayPaymentManager.pay(activity, productId)
    }
}
