#!/bin/bash

# 🔍 增强版Google Play支付流程调试脚本
# 捕获更全面的支付和应用生命周期日志

echo "🚀 开始增强版Google Play支付流程调试..."
echo "📱 请按以下步骤操作："
echo "   1. 运行此脚本"
echo "   2. 在应用中导航到支付页面"
echo "   3. 点击支付按钮"
echo "   4. 观察支付流程"
echo "   5. 按Ctrl+C停止日志捕获"
echo "⏰ 调试时间: $(date)"
echo "📄 日志将保存到: ~/google_play_payment_enhanced.log"
echo "=========================================="

# 清理之前的日志缓存
adb -s emulator-5556 logcat -c

# 等待用户准备
echo "⏳ 准备就绪后按回车键开始捕获日志..."
read -r

echo "🎯 开始捕获日志，现在可以进行支付操作..."

# 捕获全面的支付相关日志
adb -s emulator-5556 logcat -v time \
  -s PaymentViewModel:V \
     PaymentScreen:V \
     GooglePlayBillingManager:V \
     AlipayPaymentManager:V \
     PaymentManager:V \
     BillingClient:V \
     BillingClientImpl:V \
     BillingService:V \
     GooglePlayBilling:V \
     InAppBilling:V \
     Purchase:V \
     SkuDetails:V \
     ProductDetails:V \
     GooglePlay:V \
     PlayBilling:V \
     PlayStore:V \
     IabHelper:V \
     BillingHelper:V \
     PaymentActivity:V \
     MainActivity:V \
     HomeActivity:V \
     OffTimesApplication:V \
     ActivityManager:V \
     PackageManager:V \
     Intent:V \
     Bundle:V \
     "System.err":V \
     "System.out":V \
     AndroidRuntime:V \
     "com.offtime.app.gplay":V \
     "com.android.vending":V \
     "com.google.android.gms":V | \
  tee ~/google_play_payment_enhanced.log

echo ""
echo "✅ 日志捕获完成！"
echo "📄 日志文件位置: ~/google_play_payment_enhanced.log"
echo "🔍 您现在可以检查日志文件来分析支付问题"

