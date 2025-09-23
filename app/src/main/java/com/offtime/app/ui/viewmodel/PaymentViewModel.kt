package com.offtime.app.ui.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.offtime.app.BuildConfig
import com.offtime.app.data.repository.UserRepository
import com.offtime.app.data.entity.UserEntity
import com.offtime.app.manager.interfaces.PaymentManager
import com.offtime.app.manager.interfaces.PaymentResult
import kotlinx.coroutines.flow.collect
import javax.inject.Named

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val userRepository: UserRepository,
    @Named("alipay") private val alipayPaymentManager: PaymentManager?,
    @Named("google") private val googlePaymentManager: PaymentManager?
) : ViewModel() {
    
    // 价格配置
    companion object {
        const val PREMIUM_PRICE = "9.90"  // 统一价格配置（美元）- 一次性购买
        const val PREMIUM_PRICE_DISPLAY = "9.90"  // 显示价格
        const val PREMIUM_CURRENCY_SYMBOL = "$"  // 货币符号
    }
    
    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()
    
    init {
        loadSubscriptionInfo()
    }
    
    /**
     * 加载订阅信息
     */
    fun loadSubscriptionInfo() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val subscriptionInfo = userRepository.getUserSubscriptionInfo()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    subscriptionInfo = subscriptionInfo?.let {
                        SubscriptionInfo(
                            isPremium = it.isPremium,
                            subscriptionStatus = it.subscriptionStatus,
                            remainingTrialDays = it.remainingTrialDays,
                            isTrialExpired = it.isTrialExpired,
                            paymentTime = it.paymentTime,
                            paymentAmount = it.paymentAmount
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load subscription info: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 选择支付方式
     */
    fun selectPaymentMethod(paymentMethod: PaymentMethod) {
        _uiState.value = _uiState.value.copy(
            selectedPaymentMethod = paymentMethod,
            errorMessage = null
        )
    }
    
    /**
     * 处理付费
     */
    fun processPayment(activity: Activity) {
        android.util.Log.d("PaymentViewModel", "🚀 开始处理支付请求")
        android.util.Log.d("PaymentViewModel", "📱 Activity类型: ${activity.javaClass.simpleName}")
        android.util.Log.d("PaymentViewModel", "💳 选择的支付方式: ${_uiState.value.selectedPaymentMethod}")
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null
            )
            
            val paymentManager = when (_uiState.value.selectedPaymentMethod) {
                PaymentMethod.ALIPAY -> {
                    android.util.Log.d("PaymentViewModel", "🔍 使用支付宝支付管理器: $alipayPaymentManager")
                    alipayPaymentManager
                }
                PaymentMethod.GOOGLE_PLAY -> {
                    android.util.Log.d("PaymentViewModel", "🔍 使用Google Play支付管理器: $googlePaymentManager")
                    googlePaymentManager
                }
                else -> {
                    android.util.Log.e("PaymentViewModel", "❌ 未知的支付方式: ${_uiState.value.selectedPaymentMethod}")
                    null
                }
            }

            if (paymentManager == null) {
                android.util.Log.e("PaymentViewModel", "❌ 支付管理器为空，无法处理支付")
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Payment method not available")
                return@launch
            }

            try {
                android.util.Log.d("PaymentViewModel", "🛒 开始调用支付管理器.pay()方法")
                android.util.Log.d("PaymentViewModel", "📦 产品ID: premium_lifetime")
                
                paymentManager.pay(activity, "premium_lifetime").collect { result ->
                    android.util.Log.d("PaymentViewModel", "📥 收到支付结果: $result")
                    when (result) {
                        is PaymentResult.Loading -> _uiState.value = _uiState.value.copy(isLoading = true)
                        is PaymentResult.Success -> {
                            userRepository.upgradeToPremium().fold(
                                onSuccess = {
                                    _uiState.value = _uiState.value.copy(isLoading = false, isPaymentSuccess = true)
                                    loadSubscriptionInfo()
                                },
                                onFailure = { error ->
                                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Upgrade failed: ${error.message}")
                                }
                            )
                        }
                        is PaymentResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                        is PaymentResult.Cancelled -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Payment cancelled")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Payment failed: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 生成订单号
     */
    private fun generateOrderNumber(): String {
        return "OFF_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
    
    /**
     * 模拟付费流程
     */
    private suspend fun simulatePayment() {
        try {
            // 模拟支付过程
            kotlinx.coroutines.delay(2000)
            
            // 升级为付费用户
            val result = userRepository.upgradeToPremium()
            
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isPaymentSuccess = true
                    )
                    
                    // 重新加载订阅信息
                    loadSubscriptionInfo()
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "升级失败: ${error.message}"
                    )
                }
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "支付过程出错: ${e.message}"
            )
        }
    }
    
    /**
     * 清除错误消息
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    /**
     * 重置付费成功状态
     */
    fun resetPaymentSuccess() {
        _uiState.value = _uiState.value.copy(isPaymentSuccess = false)
    }
    
    /**
     * 订阅信息数据类
     */
    data class SubscriptionInfo(
        val isPremium: Boolean,
        val subscriptionStatus: String,
        val remainingTrialDays: Int,
        val isTrialExpired: Boolean,
        val paymentTime: Long,
        val paymentAmount: Int
    )
    
    /**
     * UI状态数据类
     */
    data class PaymentUiState(
        val isLoading: Boolean = false,
        val subscriptionInfo: SubscriptionInfo? = null,
        val isPaymentSuccess: Boolean = false,
        val errorMessage: String? = null,
        val selectedPaymentMethod: PaymentMethod = if (BuildConfig.ENABLE_GOOGLE_PAY) PaymentMethod.GOOGLE_PLAY else PaymentMethod.ALIPAY
    )
}

/**
 * 支付方式枚举
 */
enum class PaymentMethod {
    ALIPAY,
    WECHAT,
    GOOGLE_PLAY,
    OTHER
}
