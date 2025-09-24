#!/bin/bash

# 部署阿里云短信服务到远程服务器
echo "🚀 部署阿里云短信服务到远程服务器"
echo "================================="
echo

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

# 服务器配置
SERVER_IP="60.205.145.35"
SERVER_USER="root"
SERVER_PATH="/var/www/offtimes-backend"

echo -e "${BLUE}📡 目标服务器: $SERVER_IP${NC}"
echo -e "${YELLOW}👤 用户: $SERVER_USER${NC}"
echo -e "${GREEN}📁 部署路径: $SERVER_PATH${NC}"
echo

# 1. 创建服务器端目录结构
echo -e "${YELLOW}📂 创建服务器端目录结构...${NC}"
ssh $SERVER_USER@$SERVER_IP "mkdir -p $SERVER_PATH/services $SERVER_PATH/routes"

# 2. 上传短信服务文件
echo -e "${YELLOW}📤 上传短信服务代码...${NC}"
scp server/services/smsService.js $SERVER_USER@$SERVER_IP:$SERVER_PATH/services/
scp server/routes/auth.js $SERVER_USER@$SERVER_IP:$SERVER_PATH/routes/
scp server/env.example $SERVER_USER@$SERVER_IP:$SERVER_PATH/

# 3. 在服务器上安装阿里云短信SDK
echo -e "${YELLOW}📦 在服务器上安装阿里云短信SDK...${NC}"
ssh $SERVER_USER@$SERVER_IP "cd $SERVER_PATH && npm install @alicloud/dysmsapi20170525 @alicloud/openapi-client"

# 4. 创建环境变量配置指南
echo -e "${YELLOW}📝 创建配置指南...${NC}"
ssh $SERVER_USER@$SERVER_IP "cat > $SERVER_PATH/SMS_CONFIG_GUIDE.md << 'EOF'
# 阿里云短信服务配置指南

## 1. 获取阿里云短信服务密钥

1. 登录阿里云控制台
2. 进入短信服务控制台
3. 创建短信签名和短信模板
4. 获取 AccessKey ID 和 AccessKey Secret

## 2. 配置环境变量

复制 env.example 为 .env 并填入以下信息：

\`\`\`bash
cp env.example .env
\`\`\`

编辑 .env 文件：
\`\`\`
# 阿里云短信服务配置
ALIBABA_CLOUD_ACCESS_KEY_ID=你的AccessKey_ID
ALIBABA_CLOUD_ACCESS_KEY_SECRET=你的AccessKey_Secret
ALIBABA_CLOUD_SMS_SIGN_NAME=你的短信签名
ALIBABA_CLOUD_SMS_TEMPLATE_CODE=你的短信模板代码
\`\`\`

## 3. 重启服务

\`\`\`bash
pm2 restart all
# 或
systemctl restart your-service
\`\`\`

## 4. 测试短信服务

\`\`\`bash
curl http://localhost:8080/api/auth/sms-status
\`\`\`
EOF"

# 5. 检查部署结果
echo -e "${YELLOW}🔍 检查部署结果...${NC}"
ssh $SERVER_USER@$SERVER_IP "ls -la $SERVER_PATH/services/ $SERVER_PATH/routes/"

echo
echo -e "${GREEN}✅ 短信服务代码部署完成！${NC}"
echo
echo -e "${BLUE}📋 下一步操作：${NC}"
echo "1. 登录服务器配置阿里云短信服务密钥"
echo "2. 重启后端服务"
echo "3. 测试短信发送功能"
echo
echo -e "${YELLOW}🔧 服务器配置命令：${NC}"
echo "ssh $SERVER_USER@$SERVER_IP"
echo "cd $SERVER_PATH"
echo "cp env.example .env"
echo "nano .env  # 编辑配置文件"
echo "pm2 restart all  # 重启服务"

