#!/bin/bash

# 测试新的SMS登录功能
echo "🧪 测试新的SMS登录功能（合并注册和登录）"
echo "========================================"

DEVICE="emulator-5556"
LOG_FILE="$HOME/new_sms_login_test.log"

# 清除旧日志
> "$LOG_FILE"

echo "📱 设备: $DEVICE"
echo "📝 日志文件: $LOG_FILE"
echo

# 启动日志监控
echo "🔍 开始监控应用日志..."
adb -s "$DEVICE" logcat -c
adb -s "$DEVICE" logcat \
  -s "OffTimeApplication:*" \
  -s "LoginViewModel:*" \
  -s "UserRepository:*" \
  -s "UserApiService:*" \
  -s "MainActivity:*" \
  -s "LoginScreen:*" \
  -s "DirectSmsLogin:*" \
  -s "SMS_LOGIN:*" \
  -s "System.out:*" \
  | tee "$LOG_FILE" &

LOGCAT_PID=$!

echo "📋 日志监控已启动 (PID: $LOGCAT_PID)"
echo
echo "🎯 测试步骤:"
echo "1. 在应用中选择 'SMS Login' 标签"
echo "2. 输入手机号（如：15012345678）"
echo "3. 点击 'Send Code' 发送验证码"
echo "4. 输入收到的验证码"
echo "5. 输入昵称（可选）"
echo "6. 点击登录按钮"
echo
echo "⏹️  按任意键停止日志监控..."
read -n 1

# 停止日志监控
kill $LOGCAT_PID 2>/dev/null

echo
echo "✅ 测试完成！日志已保存到: $LOG_FILE"
echo
echo "📊 关键日志摘要:"
echo "=================="
grep -E "(directSmsLogin|sms-login|SMS登录|登录成功|登录失败)" "$LOG_FILE" || echo "未找到关键日志"
