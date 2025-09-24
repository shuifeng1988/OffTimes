#!/bin/bash

# 使用SSH密钥部署阿里云短信服务到远程服务器
echo "🚀 部署阿里云短信服务到远程服务器"
echo "================================="
echo

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

# SSH配置
SSH_KEY="$HOME/.ssh/OffTimesKey.pem"
SERVER_ALIAS="offtime"
SERVER_IP="60.205.145.35"

echo -e "${BLUE}🔑 SSH密钥: $SSH_KEY${NC}"
echo -e "${BLUE}🖥️  服务器别名: $SERVER_ALIAS${NC}"
echo

# 检查SSH密钥
if [ ! -f "$SSH_KEY" ]; then
    echo -e "${RED}❌ SSH密钥文件不存在: $SSH_KEY${NC}"
    exit 1
fi

# 检查SSH别名连接
echo -e "${YELLOW}🔍 测试SSH连接...${NC}"
if ssh -o ConnectTimeout=10 $SERVER_ALIAS "echo 'SSH连接测试成功'" 2>/dev/null; then
    echo -e "${GREEN}✅ SSH连接正常${NC}"
    USE_ALIAS=true
else
    echo -e "${YELLOW}⚠️ SSH别名连接失败，尝试使用密钥直接连接...${NC}"
    if ssh -i "$SSH_KEY" -o ConnectTimeout=10 root@$SERVER_IP "echo 'SSH密钥连接测试成功'" 2>/dev/null; then
        echo -e "${GREEN}✅ SSH密钥连接正常${NC}"
        USE_ALIAS=false
    else
        echo -e "${RED}❌ SSH连接失败，请检查网络和密钥配置${NC}"
        exit 1
    fi
fi

# 设置SSH命令
if [ "$USE_ALIAS" = true ]; then
    SSH_CMD="ssh $SERVER_ALIAS"
    SCP_CMD="scp"
    SERVER_TARGET="$SERVER_ALIAS"
else
    SSH_CMD="ssh -i $SSH_KEY root@$SERVER_IP"
    SCP_CMD="scp -i $SSH_KEY"
    SERVER_TARGET="root@$SERVER_IP"
fi

echo

# 1. 查找服务器上的项目目录
echo -e "${YELLOW}🔍 查找服务器上的项目目录...${NC}"

POSSIBLE_PATHS=(
    "/var/www/offtimes-backend"
    "/var/www/html"
    "/app"
    "/root/offtimes"
    "/home/offtimes"
    "/opt/offtimes"
    "/var/www"
)

PROJECT_PATH=""
for path in "${POSSIBLE_PATHS[@]}"; do
    echo "检查路径: $path"
    if $SSH_CMD "[ -d '$path' ] && ls '$path' | grep -E '(server\.js|package\.json|routes)'" 2>/dev/null; then
        echo -e "${GREEN}✅ 找到项目目录: $path${NC}"
        PROJECT_PATH="$path"
        break
    fi
done

if [ -z "$PROJECT_PATH" ]; then
    echo -e "${YELLOW}⚠️ 未找到现有项目目录，将在 /var/www/offtimes-backend 创建${NC}"
    PROJECT_PATH="/var/www/offtimes-backend"
    $SSH_CMD "mkdir -p $PROJECT_PATH/services $PROJECT_PATH/routes"
fi

echo -e "${BLUE}📂 使用项目路径: $PROJECT_PATH${NC}"
echo

# 2. 创建目录结构
echo -e "${YELLOW}📂 创建必要的目录结构...${NC}"
$SSH_CMD "mkdir -p $PROJECT_PATH/services $PROJECT_PATH/routes"

# 3. 备份现有文件
echo -e "${YELLOW}💾 备份现有文件...${NC}"
$SSH_CMD "
if [ -f '$PROJECT_PATH/routes/auth.js' ]; then
    cp '$PROJECT_PATH/routes/auth.js' '$PROJECT_PATH/routes/auth.js.backup.$(date +%Y%m%d_%H%M%S)'
    echo '✅ 已备份 auth.js'
fi
"

# 4. 上传更新的文件
echo -e "${YELLOW}📤 上传短信服务代码...${NC}"

echo "上传 smsService.js..."
$SCP_CMD server/services/smsService.js $SERVER_TARGET:$PROJECT_PATH/services/

echo "上传 auth.js..."
$SCP_CMD server/routes/auth.js $SERVER_TARGET:$PROJECT_PATH/routes/

echo "上传环境变量示例..."
$SCP_CMD server/env.example $SERVER_TARGET:$PROJECT_PATH/

echo -e "${GREEN}✅ 文件上传完成${NC}"

# 5. 安装阿里云短信SDK
echo -e "${YELLOW}📦 安装阿里云短信SDK...${NC}"
$SSH_CMD "cd $PROJECT_PATH && npm install @alicloud/dysmsapi20170525 @alicloud/openapi-client"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ 阿里云短信SDK安装成功${NC}"
else
    echo -e "${RED}❌ 阿里云短信SDK安装失败${NC}"
fi

# 6. 检查部署结果
echo -e "${YELLOW}🔍 检查部署结果...${NC}"
$SSH_CMD "
echo '检查文件结构:'
ls -la $PROJECT_PATH/services/smsService.js 2>/dev/null && echo '✅ smsService.js 存在' || echo '❌ smsService.js 缺失'
ls -la $PROJECT_PATH/routes/auth.js 2>/dev/null && echo '✅ auth.js 存在' || echo '❌ auth.js 缺失'

echo
echo '检查关键代码特征:'
grep -q 'sms-status' $PROJECT_PATH/routes/auth.js 2>/dev/null && echo '✅ auth.js 包含短信状态API' || echo '❌ auth.js 缺少短信状态API'
grep -q 'AliCloudSmsService' $PROJECT_PATH/services/smsService.js 2>/dev/null && echo '✅ smsService.js 包含阿里云服务' || echo '❌ smsService.js 有问题'

echo
echo '检查npm包安装:'
npm list @alicloud/dysmsapi20170525 2>/dev/null | grep -q '@alicloud/dysmsapi20170525' && echo '✅ 阿里云SDK已安装' || echo '❌ 阿里云SDK未安装'
"

echo

# 7. 配置环境变量
echo -e "${YELLOW}📝 配置环境变量...${NC}"
$SSH_CMD "
if [ ! -f '$PROJECT_PATH/.env' ]; then
    echo '创建 .env 文件...'
    cp '$PROJECT_PATH/env.example' '$PROJECT_PATH/.env'
    echo '✅ 已创建 .env 文件'
else
    echo '✅ .env 文件已存在'
fi

echo
echo '检查阿里云配置:'
if grep -q 'ALIBABA_CLOUD_ACCESS_KEY_ID' '$PROJECT_PATH/.env'; then
    echo '✅ .env 包含阿里云配置项'
else
    echo '❌ .env 缺少阿里云配置，正在添加...'
    echo '' >> '$PROJECT_PATH/.env'
    echo '# 阿里云短信服务配置' >> '$PROJECT_PATH/.env'
    echo 'ALIBABA_CLOUD_ACCESS_KEY_ID=your_access_key_id' >> '$PROJECT_PATH/.env'
    echo 'ALIBABA_CLOUD_ACCESS_KEY_SECRET=your_access_key_secret' >> '$PROJECT_PATH/.env'
    echo 'ALIBABA_CLOUD_SMS_SIGN_NAME=OffTimes' >> '$PROJECT_PATH/.env'
    echo 'ALIBABA_CLOUD_SMS_TEMPLATE_CODE=SMS_123456789' >> '$PROJECT_PATH/.env'
    echo '✅ 已添加阿里云配置模板'
fi
"

# 8. 重启服务
echo -e "${YELLOW}🔄 重启服务...${NC}"
$SSH_CMD "
echo '查找并重启Node.js服务...'

# 尝试使用PM2重启
if command -v pm2 >/dev/null 2>&1; then
    echo '使用PM2重启服务...'
    pm2 restart all
    pm2 status
elif pgrep -f 'node.*server\.js' >/dev/null; then
    echo '重启Node.js进程...'
    pkill -f 'node.*server\.js'
    sleep 2
    cd $PROJECT_PATH && nohup node server.js > server.log 2>&1 &
    echo '✅ Node.js服务已重启'
else
    echo '启动Node.js服务...'
    cd $PROJECT_PATH && nohup node server.js > server.log 2>&1 &
    echo '✅ Node.js服务已启动'
fi

sleep 3
echo
echo '检查服务状态:'
if pgrep -f 'node.*server\.js' >/dev/null; then
    echo '✅ Node.js服务正在运行'
else
    echo '❌ Node.js服务未运行'
fi
"

echo

# 9. 测试部署结果
echo -e "${YELLOW}🧪 测试部署结果...${NC}"
sleep 5

echo "测试短信服务状态API:"
SMS_STATUS=$(curl -s http://60.205.145.35:8080/api/auth/sms-status 2>/dev/null)
if echo "$SMS_STATUS" | grep -q '"isConfigured"'; then
    echo -e "${GREEN}✅ 短信服务状态API工作正常${NC}"
    echo "$SMS_STATUS" | python3 -m json.tool 2>/dev/null || echo "$SMS_STATUS"
else
    echo -e "${RED}❌ 短信服务状态API仍不可用${NC}"
    echo "响应: $SMS_STATUS"
fi

echo
echo "测试发送短信API:"
SMS_SEND=$(curl -s -X POST http://60.205.145.35:8080/api/auth/send-sms \
    -H "Content-Type: application/json" \
    -d '{"phoneNumber":"13800138000","type":"login"}' 2>/dev/null)
echo "发送短信响应: $SMS_SEND"

echo

# 10. 生成配置指南
echo -e "${BLUE}📋 部署完成！下一步配置指南${NC}"
echo "==============================="
echo
echo -e "${YELLOW}🔧 现在需要配置阿里云短信服务密钥：${NC}"
echo
echo "1. 编辑环境变量配置："
echo "   ssh $SERVER_ALIAS 'nano $PROJECT_PATH/.env'"
echo
echo "2. 修改以下配置项："
echo "   ALIBABA_CLOUD_ACCESS_KEY_ID=你的AccessKey_ID"
echo "   ALIBABA_CLOUD_ACCESS_KEY_SECRET=你的AccessKey_Secret"
echo "   ALIBABA_CLOUD_SMS_SIGN_NAME=OffTimes"
echo "   ALIBABA_CLOUD_SMS_TEMPLATE_CODE=你的短信模板代码"
echo
echo "3. 重启服务："
echo "   ssh $SERVER_ALIAS 'cd $PROJECT_PATH && pm2 restart all'"
echo
echo "4. 测试配置："
echo "   curl http://60.205.145.35:8080/api/auth/sms-status"
echo
echo -e "${GREEN}🎉 代码部署完成！配置阿里云密钥后即可使用真实短信服务。${NC}"
