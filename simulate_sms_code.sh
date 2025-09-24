#!/bin/bash

# 模拟短信验证码发送到模拟器
echo "📱 模拟短信验证码发送工具"
echo "=========================="
echo

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

DEVICE_ID=${1:-emulator-5558}
PHONE_NUMBER=${2:-10086}
SMS_CODE=${3:-123456}

echo -e "${BLUE}📱 目标设备: $DEVICE_ID${NC}"
echo -e "${YELLOW}📞 发送号码: $PHONE_NUMBER${NC}"
echo -e "${GREEN}🔢 验证码: $SMS_CODE${NC}"
echo

# 检查设备连接
if ! adb devices | grep -q "$DEVICE_ID.*device"; then
    echo -e "${RED}❌ 设备 $DEVICE_ID 未连接${NC}"
    echo "可用设备："
    adb devices
    exit 1
fi

echo -e "${GREEN}✅ 设备连接正常${NC}"
echo

# 发送模拟短信
echo -e "${YELLOW}📤 正在发送模拟短信...${NC}"
adb -s $DEVICE_ID emu sms send $PHONE_NUMBER "您的OffTimes验证码是：$SMS_CODE，请在应用中输入此验证码完成登录。"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ 模拟短信发送成功！${NC}"
    echo
    echo -e "${BLUE}📋 接下来的步骤：${NC}"
    echo "1. 在模拟器中打开短信应用查看验证码"
    echo "2. 返回OffTimes应用"
    echo "3. 在验证码输入框中输入：$SMS_CODE"
    echo "4. 点击注册/登录按钮"
else
    echo -e "${RED}❌ 模拟短信发送失败${NC}"
    exit 1
fi

echo
echo -e "${YELLOW}💡 使用说明：${NC}"
echo "# 发送默认验证码 123456"
echo "./simulate_sms_code.sh"
echo
echo "# 发送自定义验证码"
echo "./simulate_sms_code.sh emulator-5558 10086 654321"
echo
echo "# 发送到真实设备"
echo "./simulate_sms_code.sh ABC123DEF456 10086 123456"

