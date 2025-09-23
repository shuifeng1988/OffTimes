#!/bin/bash

# 🔍 真实设备Google Play支付调试脚本

echo "📱 开始真实设备Google Play支付调试..."
echo "🎯 设备ID: 3B1F6PE5MS116NFG"
echo "⏰ 调试时间: $(date)"
echo "=========================================="

# 清理之前的日志缓存
adb -s 3B1F6PE5MS116NFG logcat -c

echo "⏳ 准备就绪后按回车键开始捕获日志..."
read -r

echo "🎯 开始捕获真实设备支付日志..."
echo "💡 请在手机上："
echo "   1. 打开OffTimes应用"
echo "   2. 导航到Settings → OffTimes Premium"
echo "   3. 点击支付按钮"
echo "   4. 观察支付流程"
echo "   5. 完成后按Ctrl+C停止日志捕获"

# 捕获真实设备的支付相关日志
adb -s 3B1F6PE5MS116NFG logcat -v time \
  "OffTimeApplication:V" \
  "GooglePlayBilling:V" \
  "PaymentViewModel:V" \
  "BillingClient:V" \
  "com.android.vending:V" \
  "*:E" | \
  tee ~/real_device_payment_debug.log

echo ""
echo "✅ 真实设备支付日志已捕获！"
echo "📄 日志文件: ~/real_device_payment_debug.log"

