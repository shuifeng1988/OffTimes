#!/bin/bash

# 快速测试手机号登录调试脚本
echo "🧪 测试手机号登录调试脚本"
echo "=========================="
echo

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

DEVICE_ID="emulator-5558"

echo -e "${BLUE}📱 测试设备: $DEVICE_ID${NC}"
echo

# 检查设备连接
if ! adb devices | grep -q "$DEVICE_ID.*device"; then
    echo -e "${RED}❌ 设备 $DEVICE_ID 未连接${NC}"
    exit 1
fi

echo -e "${GREEN}✅ 设备连接正常${NC}"

# 检查应用安装
if ! adb -s $DEVICE_ID shell pm list packages | grep -q "offtime"; then
    echo -e "${RED}❌ OffTimes应用未安装${NC}"
    exit 1
fi

echo -e "${GREEN}✅ OffTimes应用已安装${NC}"

# 测试日志文件路径
LOG_FILE="$HOME/phone_login_test_debug.log"
echo "测试日志写入" > "$LOG_FILE"

if [ -f "$LOG_FILE" ]; then
    echo -e "${GREEN}✅ 日志文件路径正常${NC}"
    rm "$LOG_FILE"
else
    echo -e "${RED}❌ 日志文件路径有问题${NC}"
    exit 1
fi

# 启动应用
echo -e "${YELLOW}🚀 启动OffTimes应用...${NC}"
adb -s $DEVICE_ID shell am start -n com.offtime.app/.MainActivity

sleep 2

echo -e "${GREEN}✅ 应用已启动${NC}"
echo
echo -e "${BLUE}💡 现在可以运行完整的调试脚本:${NC}"
echo "./debug_phone_login_enhanced.sh emulator-5558"
echo
echo -e "${YELLOW}📋 测试步骤:${NC}"
echo "1. 在应用中点击登录/注册"
echo "2. 选择手机号登录方式"  
echo "3. 输入手机号: 18669102657"
echo "4. 点击发送验证码"
echo "5. 输入验证码: 123456"
echo "6. 点击登录"
echo
echo -e "${GREEN}🎯 调试脚本将实时显示登录过程中的所有关键日志！${NC}"
