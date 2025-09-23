#!/bin/bash

# 🔍 全面的Google Play支付调试脚本
# 捕获应用启动到支付的完整流程

echo "🚀 开始全面Google Play支付调试..."
echo "📱 此脚本将捕获从应用启动到支付的所有日志"
echo "⏰ 调试时间: $(date)"
echo "=========================================="

# 清理之前的日志缓存
adb -s emulator-5556 logcat -c

echo "⏳ 准备就绪后按回车键开始捕获日志..."
read -r

echo "🎯 开始捕获全面日志..."
echo "💡 请按以下步骤操作："
echo "   1. 重启OffTimes应用（查看初始化日志）"
echo "   2. 导航到支付页面"
echo "   3. 点击支付按钮（多次点击）"
echo "   4. 观察应用反应"
echo "   5. 等待30秒后按Ctrl+C停止"

# 捕获所有应用相关日志
adb -s emulator-5556 logcat -v time \
  "OffTimeApplication:V" \
  "GooglePlayBilling:V" \
  "PaymentViewModel:V" \
  "PaymentScreen:V" \
  "MainActivity:V" \
  "BillingClient:V" \
  "GooglePlayBillingManager:V" \
  "*:E" \
  "com.offtime.app.gplay:V" | \
  tee ~/google_play_payment_full.log

echo ""
echo "✅ 全面日志已捕获！"
echo "📄 日志文件: ~/google_play_payment_full.log"

