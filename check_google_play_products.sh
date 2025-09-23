#!/bin/bash

# 🔍 Google Play商品同步状态检查脚本

echo "📱 检查Google Play商品同步状态..."
echo "🎯 设备: 3B1F6PE5MS116NFG"
echo "⏰ 检查时间: $(date)"
echo "=========================================="

echo "🔍 启动支付测试，查看产品查询结果..."
echo "💡 请在手机上点击支付按钮，然后观察日志..."

# 清理之前的日志
adb -s 3B1F6PE5MS116NFG logcat -c

# 捕获支付相关日志，重点关注产品查询结果
adb -s 3B1F6PE5MS116NFG logcat -v time | grep -E "(GooglePlayBilling.*查询到.*个产品|GooglePlayBilling.*产品未找到|GooglePlayBilling.*找到产品)" --line-buffered

