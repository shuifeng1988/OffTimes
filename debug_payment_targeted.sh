#!/bin/bash

# 🔍 针对性Google Play支付调试脚本
# 专门查找支付相关的问题

echo "🚀 开始针对性Google Play支付调试..."
echo "📱 请在应用中点击支付按钮，我们将查找关键日志"
echo "⏰ 调试时间: $(date)"
echo "=========================================="

# 清理之前的日志缓存
adb -s emulator-5556 logcat -c

echo "⏳ 准备就绪后按回车键开始捕获日志..."
read -r

echo "🎯 开始捕获关键日志..."
echo "💡 现在请点击支付按钮！"

# 捕获关键的支付和应用相关日志
adb -s emulator-5556 logcat -v time \
  GooglePlayBilling:V \
  PaymentViewModel:V \
  BillingClient:V \
  "com.offtime.app.gplay":V \
  "System.err":V \
  AndroidRuntime:V \
  "com.android.vending":V | \
  head -100 | \
  tee ~/google_play_payment_targeted.log

echo ""
echo "✅ 关键日志已捕获！"
echo "📄 日志文件: ~/google_play_payment_targeted.log"

