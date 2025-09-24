#!/bin/bash

# 通过API诊断短信服务问题
echo "🔍 通过API诊断短信服务问题"
echo "========================="
echo

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

SERVER_URL="http://60.205.145.35:8080"

# 1. 测试多个手机号发送短信，观察日志模式
echo -e "${YELLOW}1. 测试发送短信到多个号码，分析响应模式...${NC}"

TEST_PHONES=("13800138000" "18669102657" "18088345891")

for phone in "${TEST_PHONES[@]}"; do
    echo "测试手机号: $phone"
    
    RESPONSE=$(curl -s -X POST "$SERVER_URL/api/auth/send-sms" \
        -H "Content-Type: application/json" \
        -d "{\"phoneNumber\":\"$phone\",\"type\":\"login\"}" 2>/dev/null)
    
    if [ $? -eq 0 ] && [ -n "$RESPONSE" ]; then
        echo "响应: $RESPONSE"
        
        # 检查响应时间（模拟发送通常很快）
        START_TIME=$(date +%s%N)
        curl -s -X POST "$SERVER_URL/api/auth/send-sms" \
            -H "Content-Type: application/json" \
            -d "{\"phoneNumber\":\"13900139000\",\"type\":\"login\"}" > /dev/null 2>&1
        END_TIME=$(date +%s%N)
        RESPONSE_TIME=$(( (END_TIME - START_TIME) / 1000000 ))
        
        echo "响应时间: ${RESPONSE_TIME}ms"
        
        # 分析响应特征
        if echo "$RESPONSE" | grep -q '"success":true'; then
            if [ $RESPONSE_TIME -lt 500 ]; then
                echo -e "${RED}⚠️ 响应过快，可能是模拟发送${NC}"
            else
                echo -e "${GREEN}✅ 响应时间正常，可能是真实发送${NC}"
            fi
        fi
    else
        echo -e "${RED}❌ API调用失败${NC}"
    fi
    echo "---"
done

echo

# 2. 检查错误处理机制
echo -e "${YELLOW}2. 测试错误处理机制...${NC}"

# 测试无效手机号
echo "测试无效手机号:"
INVALID_RESPONSE=$(curl -s -X POST "$SERVER_URL/api/auth/send-sms" \
    -H "Content-Type: application/json" \
    -d '{"phoneNumber":"invalid","type":"login"}' 2>/dev/null)
echo "无效手机号响应: $INVALID_RESPONSE"

# 测试频率限制
echo "测试频率限制（连续发送）:"
for i in {1..3}; do
    RATE_RESPONSE=$(curl -s -X POST "$SERVER_URL/api/auth/send-sms" \
        -H "Content-Type: application/json" \
        -d '{"phoneNumber":"13800138000","type":"login"}' 2>/dev/null)
    echo "第${i}次: $RATE_RESPONSE"
done

echo

# 3. 分析服务器响应头
echo -e "${YELLOW}3. 分析服务器响应头...${NC}"
HEADERS=$(curl -s -I -X POST "$SERVER_URL/api/auth/send-sms" \
    -H "Content-Type: application/json" \
    -d '{"phoneNumber":"13800138000","type":"login"}' 2>/dev/null)
echo "响应头信息:"
echo "$HEADERS"

echo

# 4. 生成诊断报告
echo -e "${BLUE}📋 诊断报告${NC}"
echo "============"

# 基于观察到的模式进行判断
echo "基于API测试结果分析:"

# 检查是否有短信状态API
if curl -s "$SERVER_URL/api/auth/sms-status" | grep -q '"isConfigured"'; then
    echo -e "${GREEN}✅ 服务器代码已更新（有短信状态API）${NC}"
    
    # 获取配置状态
    SMS_CONFIG=$(curl -s "$SERVER_URL/api/auth/sms-status" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    if 'data' in data:
        config = data['data']
        print(f\"配置状态: {'已配置' if config.get('isConfigured') else '未配置'}\")
        print(f\"工作模式: {config.get('mode', '未知')}\")
        print(f\"短信签名: {config.get('signName', '未设置')}\")
        print(f\"模板代码: {config.get('templateCode', '未设置')}\")
        print(f\"AccessKey: {'已设置' if config.get('hasAccessKey') else '未设置'}\")
        print(f\"SecretKey: {'已设置' if config.get('hasSecretKey') else '未设置'}\")
except:
    print('解析配置信息失败')
" 2>/dev/null)
    echo "$SMS_CONFIG"
else
    echo -e "${RED}❌ 服务器代码未更新（无短信状态API）${NC}"
fi

echo

# 给出具体的修复建议
echo -e "${YELLOW}🔧 具体问题和解决方案：${NC}"

if ! curl -s "$SERVER_URL/api/auth/sms-status" | grep -q '"isConfigured"'; then
    echo -e "${RED}问题1: 服务器代码未更新${NC}"
    echo "解决方案:"
    echo "- 将更新的代码文件上传到服务器"
    echo "- 重启服务器应用"
    echo
elif curl -s "$SERVER_URL/api/auth/sms-status" | grep -q '"isConfigured":false'; then
    echo -e "${RED}问题2: 阿里云短信服务未配置${NC}"
    echo "解决方案:"
    echo "- 在服务器的.env文件中添加阿里云配置"
    echo "- 确保AccessKey和Secret正确"
    echo "- 重启服务"
    echo
else
    echo -e "${RED}问题3: 其他配置问题${NC}"
    echo "可能原因:"
    echo "- 阿里云短信服务余额不足"
    echo "- 短信模板未审核通过"
    echo "- AccessKey权限不足"
    echo "- 网络连接问题"
fi

echo -e "${BLUE}💡 推荐操作：${NC}"
echo "1. 首先确认服务器代码是否为最新版本"
echo "2. 检查阿里云短信服务控制台配置"
echo "3. 验证AccessKey和Secret的有效性"
echo "4. 查看服务器端日志获取详细错误信息"
