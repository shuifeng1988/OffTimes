#!/bin/bash

# 调试服务器端短信服务状态
echo "🔍 调试服务器端阿里云短信服务"
echo "============================="
echo

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

SERVER_URL="http://60.205.145.35:8080"

echo -e "${BLUE}🌐 服务器地址: $SERVER_URL${NC}"
echo

# 1. 检查服务器基本连接
echo -e "${YELLOW}1. 检查服务器连接状态...${NC}"
if curl -s --connect-timeout 5 "$SERVER_URL/api" > /dev/null; then
    echo -e "${GREEN}✅ 服务器连接正常${NC}"
else
    echo -e "${RED}❌ 服务器连接失败${NC}"
    exit 1
fi

# 2. 检查短信服务状态API
echo -e "${YELLOW}2. 检查短信服务状态API...${NC}"
SMS_STATUS_RESPONSE=$(curl -s "$SERVER_URL/api/auth/sms-status" 2>/dev/null)
if [ $? -eq 0 ] && [ -n "$SMS_STATUS_RESPONSE" ]; then
    echo -e "${GREEN}✅ 短信服务状态API可访问${NC}"
    echo "响应内容："
    echo "$SMS_STATUS_RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$SMS_STATUS_RESPONSE"
else
    echo -e "${RED}❌ 短信服务状态API不可访问${NC}"
    echo "可能原因："
    echo "- 服务器代码未更新"
    echo "- 路由配置问题"
    echo "- 服务未重启"
fi

echo

# 3. 检查发送短信API基本功能
echo -e "${YELLOW}3. 测试发送短信API...${NC}"
SMS_SEND_RESPONSE=$(curl -s -X POST "$SERVER_URL/api/auth/send-sms" \
    -H "Content-Type: application/json" \
    -d '{"phoneNumber":"13800138000","type":"login"}' 2>/dev/null)

if [ $? -eq 0 ] && [ -n "$SMS_SEND_RESPONSE" ]; then
    echo -e "${GREEN}✅ 发送短信API可访问${NC}"
    echo "响应内容："
    echo "$SMS_SEND_RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$SMS_SEND_RESPONSE"
    
    # 检查响应中是否包含成功标志
    if echo "$SMS_SEND_RESPONSE" | grep -q '"success":true'; then
        echo -e "${GREEN}✅ API返回成功状态${NC}"
    else
        echo -e "${RED}❌ API返回失败状态${NC}"
    fi
else
    echo -e "${RED}❌ 发送短信API不可访问${NC}"
fi

echo

# 4. 检查API路由列表
echo -e "${YELLOW}4. 检查API路由列表...${NC}"
API_ROUTES_RESPONSE=$(curl -s "$SERVER_URL/api" 2>/dev/null)
if [ $? -eq 0 ] && [ -n "$API_ROUTES_RESPONSE" ]; then
    echo -e "${GREEN}✅ API路由信息可访问${NC}"
    echo "可用端点："
    echo "$API_ROUTES_RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$API_ROUTES_RESPONSE"
else
    echo -e "${RED}❌ API路由信息不可访问${NC}"
fi

echo

# 5. 生成诊断报告
echo -e "${BLUE}📋 诊断总结${NC}"
echo "============"

# 检查关键指标
HAS_SMS_STATUS_API=false
HAS_ALIYUN_CONFIG=false
SMS_MODE="unknown"

if echo "$SMS_STATUS_RESPONSE" | grep -q '"isConfigured"'; then
    HAS_SMS_STATUS_API=true
    if echo "$SMS_STATUS_RESPONSE" | grep -q '"isConfigured":true'; then
        HAS_ALIYUN_CONFIG=true
        SMS_MODE="production"
    else
        SMS_MODE="simulation"
    fi
fi

echo "短信服务状态API: $([ "$HAS_SMS_STATUS_API" = true ] && echo "✅ 已部署" || echo "❌ 未部署")"
echo "阿里云短信配置: $([ "$HAS_ALIYUN_CONFIG" = true ] && echo "✅ 已配置" || echo "❌ 未配置")"
echo "当前工作模式: $SMS_MODE"

echo
echo -e "${YELLOW}🔧 建议的排查步骤：${NC}"

if [ "$HAS_SMS_STATUS_API" = false ]; then
    echo "1. ❌ 短信服务代码未部署到服务器"
    echo "   - 检查服务器上是否有 smsService.js 文件"
    echo "   - 检查 routes/auth.js 是否已更新"
    echo "   - 确认服务已重启"
elif [ "$HAS_ALIYUN_CONFIG" = false ]; then
    echo "2. ❌ 阿里云短信服务未正确配置"
    echo "   - 检查 .env 文件中的阿里云配置"
    echo "   - 确认 AccessKey ID 和 Secret 正确"
    echo "   - 检查短信签名和模板代码"
else
    echo "3. ✅ 配置看起来正常，检查具体错误"
    echo "   - 查看服务器日志获取详细错误信息"
    echo "   - 检查阿里云短信服务余额"
    echo "   - 确认短信模板已审核通过"
fi

echo
echo -e "${BLUE}📞 下一步调试命令：${NC}"
echo "# 查看服务器日志"
echo "ssh root@60.205.145.35 'tail -50 /var/log/your-app.log'"
echo
echo "# 检查环境变量配置"
echo "ssh root@60.205.145.35 'cat /path/to/your/project/.env | grep ALIBABA'"
echo
echo "# 重启服务"
echo "ssh root@60.205.145.35 'pm2 restart all'"
