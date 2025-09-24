#!/bin/bash

# 专门调试 "Response must include generic type" 错误的脚本
echo "🔍 调试 Response must include generic type 错误"
echo "=================================================="
echo

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

DEVICE_ID=${1:-emulator-5558}
LOG_FILE="$HOME/generic_type_error_debug.log"

echo -e "${BLUE}📱 目标设备: $DEVICE_ID${NC}"
echo -e "${YELLOW}📝 日志文件: $LOG_FILE${NC}"
echo

# 检查设备连接
if ! adb devices | grep -q "$DEVICE_ID.*device"; then
    echo -e "${RED}❌ 设备 $DEVICE_ID 未连接${NC}"
    exit 1
fi

echo -e "${GREEN}✅ 设备连接正常${NC}"

# 清理旧日志
> "$LOG_FILE"

echo -e "${YELLOW}🚀 启动专门的错误监控...${NC}"
echo
echo "请在应用中进行以下操作："
echo "1. 点击登录/注册"
echo "2. 选择 SMS Login"
echo "3. 输入手机号并点击 Send Code"
echo "4. 输入验证码并点击 Register"
echo
echo "监控中... (按 Ctrl+C 停止)"
echo

# 启动多个监控进程
{
    echo "=== 开始时间: $(date) ==="
    echo "=== 设备: $DEVICE_ID ==="
    echo
} >> "$LOG_FILE"

# 监控特定错误
adb -s "$DEVICE_ID" logcat -v time | while IFS= read -r line; do
    # 检查关键错误
    if echo "$line" | grep -qE "(Response must include|generic type|method f\.b|UserApiService|sendSmsCode|loginWith|retrofit|Retrofit|网络|登录|注册|验证码|手机号|Exception|Error.*com\.offtime)"; then
        echo -e "${RED}🚨 ERROR:${NC} $line"
        echo "ERROR: $line" >> "$LOG_FILE"
    elif echo "$line" | grep -qE "com\.offtime\.app"; then
        echo -e "${BLUE}📱 APP:${NC} $line"
        echo "APP: $line" >> "$LOG_FILE"
    elif echo "$line" | grep -qE "(HTTP|API|Request|Response)"; then
        echo -e "${GREEN}🌐 NET:${NC} $line"
        echo "NET: $line" >> "$LOG_FILE"
    fi
done
