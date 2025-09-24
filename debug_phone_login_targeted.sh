#!/bin/bash

# OffTimes 支付宝版本手机号登录目标调试脚本
# 专注于关键登录组件的日志监控
# 使用方法: ./debug_phone_login_targeted.sh [device_id]

echo "🎯 OffTimes 支付宝版本 - 手机号登录目标调试"
echo "============================================"
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

LOG_FILE="$HOME/phone_login_targeted_debug${DEVICE_SUFFIX}.log"

echo -e "${YELLOW}📝 目标日志文件: $LOG_FILE${NC}"
echo

# 清理旧日志
> "$LOG_FILE"

# 记录调试开始信息
{
    echo "🎯 OffTimes 支付宝版本手机号登录目标调试"
    echo "调试开始时间: $(date)"
    if [ -n "$DEVICE_ID" ]; then
        echo "目标设备: $DEVICE_ID"
    fi
    echo "目标组件:"
    echo "  • LoginViewModel - 登录界面状态管理"
    echo "  • UserRepository - 用户数据和API调用"
    echo "  • LoginApiService - 登录网络服务"
    echo "  • SMS验证码相关流程"
    echo "========================================"
    echo
} >> "$LOG_FILE"

echo -e "${BLUE}🎯 开始目标监控...${NC}"
echo
echo -e "${CYAN}🔍 专注监控的关键步骤:${NC}"
echo "  1. 手机号输入和验证"
echo "  2. 验证码发送请求"
echo "  3. 验证码输入和验证"
echo "  4. 登录请求和响应"
echo "  5. 登录状态更新"
echo
echo -e "${YELLOW}📱 请在应用中进行手机号登录操作${NC}"
echo -e "${PURPLE}按 Ctrl+C 停止监控${NC}"
echo

# 启动精确的日志监控（使用指定设备）
adb $ADB_DEVICE logcat -v time \
    LoginViewModel:D \
    UserRepository:D \
    LoginApiService:D \
    AlipayLoginManager:D \
    OffTimeApplication:D \
    *:S | \
    grep -E --line-buffered --color=never \
    "(setPhoneNumber|sendSmsCode|setSmsCode|loginWith|verifySmsCode|手机号|验证码|登录|SMS|phone|login|auth|token)" | \
    while read -r line; do
    
    # 记录到日志文件
    echo "$line" >> "$LOG_FILE"
    
    # 实时显示并分类
    timestamp=$(echo "$line" | cut -d' ' -f1-2)
    content=$(echo "$line" | cut -d' ' -f3-)
    
    if echo "$content" | grep -qi "error\|exception\|failed\|失败\|错误"; then
        echo -e "${RED}[$timestamp] ❌ $content${NC}"
    elif echo "$content" | grep -qi "success\|成功\|登录成功"; then
        echo -e "${GREEN}[$timestamp] ✅ $content${NC}"
    elif echo "$content" | grep -qi "setPhoneNumber\|手机号"; then
        echo -e "${BLUE}[$timestamp] 📞 $content${NC}"
    elif echo "$content" | grep -qi "sendSmsCode\|发送.*验证码"; then
        echo -e "${YELLOW}[$timestamp] 📤 $content${NC}"
    elif echo "$content" | grep -qi "setSmsCode\|验证码"; then
        echo -e "${CYAN}[$timestamp] 🔢 $content${NC}"
    elif echo "$content" | grep -qi "loginWith\|登录"; then
        echo -e "${PURPLE}[$timestamp] 🔐 $content${NC}"
    elif echo "$content" | grep -qi "token\|auth"; then
        echo -e "${YELLOW}[$timestamp] 🔑 $content${NC}"
    else
        echo -e "[$timestamp] 📝 $content"
    fi
done

echo
echo -e "${GREEN}🎯 目标调试完成！${NC}"
echo -e "${YELLOW}📁 日志文件: $LOG_FILE${NC}"
echo
echo -e "${BLUE}💡 使用建议:${NC}"
echo "  • 如果登录失败，查看日志中的错误信息"
echo "  • 关注验证码发送和验证的步骤"
echo "  • 检查网络请求是否成功"
