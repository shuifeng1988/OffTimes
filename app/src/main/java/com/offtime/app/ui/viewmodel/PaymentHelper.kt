package com.offtime.app.ui.viewmodel

import android.app.Activity
import kotlinx.coroutines.flow.Flow

interface PaymentHelper {
    fun processPayment(activity: Activity, productId: String): Flow<PaymentResult>
}
