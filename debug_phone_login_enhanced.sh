#!/bin/bash

# OffTimes 支付宝版本手机号登录增强调试脚本
# 详细监控手机号登录的每个步骤和网络请求
# 使用方法: ./debug_phone_login_enhanced.sh [device_id]

echo "🔐 OffTimes 支付宝版本 - 手机号登录增强调试"
echo "============================================="
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

LOG_FILE="$HOME/phone_login_enhanced_debug${DEVICE_SUFFIX}.log"
NETWORK_LOG_FILE="$HOME/phone_login_network_debug${DEVICE_SUFFIX}.log"
FULL_LOG_FILE="$HOME/phone_login_full_enhanced_debug${DEVICE_SUFFIX}.log"

echo -e "${YELLOW}📝 主要日志: $LOG_FILE${NC}"
echo -e "${YELLOW}📝 网络日志: $NETWORK_LOG_FILE${NC}"
echo -e "${YELLOW}📝 完整日志: $FULL_LOG_FILE${NC}"
echo

# 清理旧日志
> "$LOG_FILE"
> "$NETWORK_LOG_FILE"
> "$FULL_LOG_FILE"

# 记录调试开始时间
{
    echo "🔐 OffTimes 支付宝版本手机号登录调试"
    echo "调试开始时间: $(date)"
    if [ -n "$DEVICE_ID" ]; then
        echo "目标设备: $DEVICE_ID"
    fi
    echo "========================================"
    echo
} >> "$LOG_FILE"

echo -e "${BLUE}🚀 开始监控手机号登录流程...${NC}"
echo
echo -e "${CYAN}📋 监控的组件和流程:${NC}"
echo "  • LoginViewModel - 登录界面逻辑"
echo "  • UserRepository - 用户数据操作"
echo "  • LoginApiService - 登录API调用"
echo "  • SMS验证码发送和验证"
echo "  • 网络请求和响应"
echo "  • 错误处理和用户反馈"
echo
echo -e "${YELLOW}🎯 请在应用中按以下步骤操作:${NC}"
echo "  1. 打开OffTimes支付宝版本应用"
echo "  2. 进入登录界面"
echo "  3. 选择「手机号登录」方式"
echo "  4. 输入手机号码"
echo "  5. 点击「发送验证码」按钮"
echo "  6. 输入收到的验证码"
echo "  7. 点击「登录」按钮"
echo
echo -e "${PURPLE}按 Ctrl+C 停止监控${NC}"
echo
echo -e "${GREEN}开始实时监控...${NC}"
echo

# 启动日志监控（使用指定设备）
adb $ADB_DEVICE logcat -v time | tee "$FULL_LOG_FILE" | grep -E \
    --line-buffered \
    --color=never \
    "LoginViewModel|UserRepository|LoginApiService|AlipayLoginManager|PhoneLogin|SMS|验证码|手机号|登录|网络|HTTP|API|Request|Response|Token|Auth|offtime|OffTime|com.offtime.app" \
    | while read -r line; do
    
    # 记录到主日志文件
    echo "$line" >> "$LOG_FILE"
    
    # 网络相关日志单独记录
    if echo "$line" | grep -qi "http\|api\|request\|response\|network\|token\|auth"; then
        echo "$line" >> "$NETWORK_LOG_FILE"
    fi
    
    # 实时显示并高亮
    if echo "$line" | grep -qi "error\|exception\|failed\|失败\|错误\|crash"; then
        echo -e "${RED}❌ ERROR: $line${NC}"
    elif echo "$line" | grep -qi "success\|成功\|登录成功\|验证成功"; then
        echo -e "${GREEN}✅ SUCCESS: $line${NC}"
    elif echo "$line" | grep -qi "sms\|验证码\|发送\|sendSmsCode"; then
        echo -e "${YELLOW}📱 SMS: $line${NC}"
    elif echo "$line" | grep -qi "phone\|手机号\|phoneNumber\|setPhoneNumber"; then
        echo -e "${BLUE}📞 PHONE: $line${NC}"
    elif echo "$line" | grep -qi "login\|登录\|loginWith"; then
        echo -e "${PURPLE}🔐 LOGIN: $line${NC}"
    elif echo "$line" | grep -qi "http\|api\|request\|response\|network"; then
        echo -e "${CYAN}🌐 NETWORK: $line${NC}"
    elif echo "$line" | grep -qi "token\|auth\|jwt"; then
        echo -e "${YELLOW}🔑 AUTH: $line${NC}"
    elif echo "$line" | grep -qi "validation\|validate\|验证"; then
        echo -e "${BLUE}✔️ VALIDATE: $line${NC}"
    else
        echo "📝 $line"
    fi
done

echo
echo -e "${GREEN}📋 手机号登录调试完成！${NC}"
echo
echo -e "${YELLOW}📁 生成的日志文件:${NC}"
echo "  • 主要日志: $LOG_FILE"
echo "  • 网络日志: $NETWORK_LOG_FILE" 
echo "  • 完整日志: $FULL_LOG_FILE"
echo
echo -e "${BLUE}💡 分析建议:${NC}"
echo "  • 查看主要日志了解登录流程"
echo "  • 查看网络日志了解API调用情况"
echo "  • 如有问题，可将日志文件发送给开发者分析"
