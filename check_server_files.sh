#!/bin/bash

# 检查服务器上的文件和配置状态
echo "🔍 检查服务器文件和配置状态"
echo "=========================="
echo

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

SERVER_IP="60.205.145.35"
SERVER_USER="root"

echo -e "${BLUE}📡 连接服务器: $SERVER_IP${NC}"
echo

# 检查可能的项目路径
POSSIBLE_PATHS=(
    "/var/www/offtimes-backend"
    "/var/www/html"
    "/app"
    "/root/offtimes"
    "/home/offtimes"
    "/opt/offtimes"
)

echo -e "${YELLOW}🔍 查找OffTimes项目目录...${NC}"
for path in "${POSSIBLE_PATHS[@]}"; do
    echo "检查路径: $path"
    if ssh $SERVER_USER@$SERVER_IP "[ -d '$path' ] && ls '$path' | grep -E '(server\.js|package\.json|routes)'" 2>/dev/null; then
        echo -e "${GREEN}✅ 找到项目目录: $path${NC}"
        PROJECT_PATH="$path"
        break
    fi
done

if [ -z "$PROJECT_PATH" ]; then
    echo -e "${RED}❌ 未找到OffTimes项目目录${NC}"
    echo "请手动指定项目路径，或检查以下常见位置："
    for path in "${POSSIBLE_PATHS[@]}"; do
        echo "  - $path"
    done
    exit 1
fi

echo
echo -e "${YELLOW}📂 项目路径: $PROJECT_PATH${NC}"
echo

# 检查关键文件
echo -e "${YELLOW}📋 检查关键文件...${NC}"

FILES_TO_CHECK=(
    "server.js"
    "package.json"
    "routes/auth.js"
    "services/smsService.js"
    ".env"
)

for file in "${FILES_TO_CHECK[@]}"; do
    echo -n "检查 $file: "
    if ssh $SERVER_USER@$SERVER_IP "[ -f '$PROJECT_PATH/$file' ]" 2>/dev/null; then
        echo -e "${GREEN}✅ 存在${NC}"
        
        # 特殊检查
        case $file in
            "routes/auth.js")
                if ssh $SERVER_USER@$SERVER_IP "grep -q 'sms-status' '$PROJECT_PATH/$file'" 2>/dev/null; then
                    echo -e "  ${GREEN}✅ 包含短信状态API${NC}"
                else
                    echo -e "  ${RED}❌ 未包含短信状态API (代码未更新)${NC}"
                fi
                
                if ssh $SERVER_USER@$SERVER_IP "grep -q '模拟发送短信' '$PROJECT_PATH/$file'" 2>/dev/null; then
                    echo -e "  ${RED}❌ 仍在使用模拟发送${NC}"
                else
                    echo -e "  ${GREEN}✅ 已更新为真实短信服务${NC}"
                fi
                ;;
            "services/smsService.js")
                if ssh $SERVER_USER@$SERVER_IP "grep -q 'AliCloudSmsService' '$PROJECT_PATH/$file'" 2>/dev/null; then
                    echo -e "  ${GREEN}✅ 阿里云短信服务模块正确${NC}"
                else
                    echo -e "  ${RED}❌ 阿里云短信服务模块有问题${NC}"
                fi
                ;;
            ".env")
                if ssh $SERVER_USER@$SERVER_IP "grep -q 'ALIBABA_CLOUD_ACCESS_KEY_ID' '$PROJECT_PATH/$file'" 2>/dev/null; then
                    echo -e "  ${GREEN}✅ 包含阿里云配置${NC}"
                else
                    echo -e "  ${RED}❌ 缺少阿里云配置${NC}"
                fi
                ;;
        esac
    else
        echo -e "${RED}❌ 不存在${NC}"
    fi
done

echo

# 检查npm包
echo -e "${YELLOW}📦 检查阿里云短信SDK安装状态...${NC}"
if ssh $SERVER_USER@$SERVER_IP "cd '$PROJECT_PATH' && npm list @alicloud/dysmsapi20170525" 2>/dev/null | grep -q "@alicloud/dysmsapi20170525"; then
    echo -e "${GREEN}✅ 阿里云短信SDK已安装${NC}"
else
    echo -e "${RED}❌ 阿里云短信SDK未安装${NC}"
fi

echo

# 检查进程状态
echo -e "${YELLOW}🔄 检查服务进程状态...${NC}"
PROCESS_INFO=$(ssh $SERVER_USER@$SERVER_IP "ps aux | grep -E '(node|pm2).*server\.js' | grep -v grep" 2>/dev/null)
if [ -n "$PROCESS_INFO" ]; then
    echo -e "${GREEN}✅ Node.js服务正在运行${NC}"
    echo "$PROCESS_INFO"
else
    echo -e "${RED}❌ Node.js服务未运行${NC}"
fi

echo

# 生成修复建议
echo -e "${BLUE}🔧 修复建议${NC}"
echo "=========="

echo "基于检查结果，需要执行以下操作："
echo
echo "1. 更新服务器代码："
echo "   scp server/routes/auth.js $SERVER_USER@$SERVER_IP:$PROJECT_PATH/routes/"
echo "   scp server/services/smsService.js $SERVER_USER@$SERVER_IP:$PROJECT_PATH/services/"
echo
echo "2. 安装阿里云SDK（如果未安装）："
echo "   ssh $SERVER_USER@$SERVER_IP 'cd $PROJECT_PATH && npm install @alicloud/dysmsapi20170525 @alicloud/openapi-client'"
echo
echo "3. 配置环境变量："
echo "   ssh $SERVER_USER@$SERVER_IP 'cd $PROJECT_PATH && nano .env'"
echo "   # 添加阿里云短信服务配置"
echo
echo "4. 重启服务："
echo "   ssh $SERVER_USER@$SERVER_IP 'pm2 restart all'"
echo
echo "5. 测试服务："
echo "   curl http://60.205.145.35:8080/api/auth/sms-status"
