#!/bin/bash

# 🔍 Google Play支付流程调试脚本
# 用于捕获完整的支付过程日志

echo "🚀 开始Google Play支付流程调试..."
echo "📱 请在应用中点击支付按钮后，观察日志输出"
echo "⏰ 调试时间: $(date)"
echo "=========================================="

# 清理之前的日志缓存
adb -s emulator-5556 logcat -c

# 捕获支付相关的所有日志
adb -s emulator-5556 logcat -v time \
  -s PaymentViewModel:V \
     PaymentScreen:V \
     GooglePlayBillingManager:V \
     AlipayPaymentManager:V \
     PaymentManager:V \
     BillingClient:V \
     GooglePlayBilling:V \
     InAppBilling:V \
     Purchase:V \
     SkuDetails:V \
     ProductDetails:V \
     BillingService:V \
     GooglePlay:V \
     PlayBilling:V \
     IabHelper:V \
     BillingHelper:V \
     PaymentActivity:V \
     MainActivity:V \
     OffTimesApplication:V \
     System.err:V | \
  tee ~/google_play_payment_debug.log

