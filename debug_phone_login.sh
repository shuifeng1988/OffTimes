#!/bin/bash

# OffTimes 支付宝版本手机号登录调试脚本
# 监控手机号登录的完整流程
# 使用方法: ./debug_phone_login.sh [device_id]

echo "🔐 OffTimes 支付宝版本 - 手机号登录调试"
echo "========================================"
echo

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

# 获取设备ID参数
DEVICE_ID="$1"

# 检查设备连接
echo -e "${BLUE}📱 检查设备连接状态...${NC}"
DEVICE_COUNT=$(adb devices | grep -v "List of devices" | grep -c "device")
if [ "$DEVICE_COUNT" -eq 0 ]; then
    echo -e "${RED}❌ 未检测到Android设备，请确保设备已连接并开启USB调试${NC}"
    exit 1
fi

echo -e "${GREEN}✅ 检测到 $DEVICE_COUNT 个设备${NC}"
adb devices
echo

# 设备选择逻辑
if [ -n "$DEVICE_ID" ]; then
    # 验证指定的设备ID是否存在
    if ! adb devices | grep -q "$DEVICE_ID.*device"; then
        echo -e "${RED}❌ 设备 '$DEVICE_ID' 未找到或未连接${NC}"
        echo -e "${YELLOW}💡 可用设备列表:${NC}"
        adb devices | grep "device$" | awk '{print "   " $1}'
        exit 1
    fi
    
    echo -e "${GREEN}🎯 使用指定设备: $DEVICE_ID${NC}"
    ADB_DEVICE="-s $DEVICE_ID"
else
    # 如果有多个设备，提示用户指定
    if [ "$DEVICE_COUNT" -gt 1 ]; then
        echo -e "${YELLOW}⚠️ 检测到多个设备，请指定目标设备:${NC}"
        echo -e "${BLUE}使用方法: $0 <device_id>${NC}"
        echo -e "${YELLOW}可用设备:${NC}"
        adb devices | grep "device$" | awk '{print "   " $1}'
        echo
        echo -e "${CYAN}示例: $0 emulator-5558${NC}"
        exit 1
    else
        echo -e "${GREEN}🎯 使用唯一设备${NC}"
        ADB_DEVICE=""
    fi
fi
echo

# 日志文件（包含设备ID信息）
if [ -n "$DEVICE_ID" ]; then
    DEVICE_SUFFIX="_${DEVICE_ID}"
else
    DEVICE_SUFFIX=""
fi

LOG_FILE="$HOME/phone_login_debug${DEVICE_SUFFIX}.log"
FULL_LOG_FILE="$HOME/phone_login_full_debug${DEVICE_SUFFIX}.log"

echo -e "${YELLOW}📝 日志文件: $LOG_FILE${NC}"
echo -e "${YELLOW}📝 完整日志: $FULL_LOG_FILE${NC}"
echo

# 清理旧日志
> "$LOG_FILE"
> "$FULL_LOG_FILE"

echo -e "${BLUE}🚀 开始监控手机号登录流程...${NC}"
echo "请在应用中进行以下操作："
echo "1. 打开OffTimes支付宝版本"
echo "2. 进入登录界面"
echo "3. 选择手机号登录"
echo "4. 输入手机号并发送验证码"
echo "5. 输入验证码并登录"
echo
echo -e "${PURPLE}按 Ctrl+C 停止监控${NC}"
echo

# 启动日志监控（使用指定设备）
adb $ADB_DEVICE logcat -v time | tee "$FULL_LOG_FILE" | grep -E \
    --line-buffered \
    --color=always \
    "LoginViewModel|UserRepository|AlipayLoginManager|PhoneLogin|SMS|验证码|手机号|登录|offtime|OffTime" \
    | while read -r line; do
    
    # 记录到文件
    echo "$line" >> "$LOG_FILE"
    
    # 根据关键词高亮显示
    if echo "$line" | grep -qi "error\|exception\|failed\|失败\|错误"; then
        echo -e "${RED}❌ $line${NC}"
    elif echo "$line" | grep -qi "success\|成功\|登录成功"; then
        echo -e "${GREEN}✅ $line${NC}"
    elif echo "$line" | grep -qi "sms\|验证码\|发送"; then
        echo -e "${YELLOW}📱 $line${NC}"
    elif echo "$line" | grep -qi "phone\|手机号\|phoneNumber"; then
        echo -e "${BLUE}📞 $line${NC}"
    elif echo "$line" | grep -qi "login\|登录"; then
        echo -e "${PURPLE}🔐 $line${NC}"
    else
        echo "$line"
    fi
done

echo
echo -e "${GREEN}📋 调试完成！日志已保存到:${NC}"
echo "- 过滤日志: $LOG_FILE"
echo "- 完整日志: $FULL_LOG_FILE"
