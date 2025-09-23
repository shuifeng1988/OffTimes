#!/bin/bash

# 🔍 完整版Google Play支付调试脚本
# 捕获所有可能相关的日志，不使用过滤器

echo "🚀 开始完整版Google Play支付流程调试..."
echo "📱 此脚本将捕获所有日志，请准备好进行支付操作"
echo "⏰ 调试时间: $(date)"
echo "📄 日志将保存到: ~/google_play_payment_complete.log"
echo "=========================================="

# 清理之前的日志缓存
adb -s emulator-5556 logcat -c

# 等待用户准备
echo "⏳ 准备就绪后按回车键开始捕获日志..."
read -r

echo "🎯 开始捕获所有日志，现在可以进行支付操作..."
echo "💡 建议操作步骤："
echo "   1. 导航到支付页面"
echo "   2. 点击支付按钮"
echo "   3. 观察应用反应"
echo "   4. 等待10-20秒后按Ctrl+C停止"

# 捕获所有日志（不使用过滤器）
adb -s emulator-5556 logcat -v time | tee ~/google_play_payment_complete.log

