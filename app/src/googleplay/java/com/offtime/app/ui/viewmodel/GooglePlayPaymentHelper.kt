package com.offtime.app.ui.viewmodel

import android.app.Activity
import com.offtime.app.manager.interfaces.PaymentManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Named

class GooglePlayPaymentHelper @Inject constructor(
    @Named("google") private val googlePaymentManager: PaymentManager?
) : PaymentHelper {

    override fun processPayment(activity: Activity, productId: String): Flow<PaymentResult> {
        if (googlePaymentManager == null) {
            return kotlinx.coroutines.flow.flowOf(PaymentResult.Error("Google Play payment is not available"))
        }
        return googlePaymentManager.pay(activity, productId)
    }
}
