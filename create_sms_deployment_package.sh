#!/bin/bash

# 创建阿里云短信服务部署包
echo "📦 创建阿里云短信服务部署包"
echo "=========================="
echo

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

PACKAGE_DIR="sms_service_deployment"
PACKAGE_FILE="sms_service_deployment.tar.gz"

# 清理旧的部署包
rm -rf $PACKAGE_DIR $PACKAGE_FILE

# 创建部署目录结构
echo -e "${YELLOW}📂 创建部署目录结构...${NC}"
mkdir -p $PACKAGE_DIR/services
mkdir -p $PACKAGE_DIR/routes

# 复制必要文件
echo -e "${YELLOW}📄 复制短信服务文件...${NC}"
cp server/services/smsService.js $PACKAGE_DIR/services/
cp server/routes/auth.js $PACKAGE_DIR/routes/
cp server/env.example $PACKAGE_DIR/

# 创建package.json用于安装依赖
echo -e "${YELLOW}📦 创建package.json...${NC}"
cat > $PACKAGE_DIR/package.json << 'EOF'
{
  "name": "offtimes-sms-service",
  "version": "1.0.0",
  "description": "OffTimes阿里云短信服务",
  "dependencies": {
    "@alicloud/dysmsapi20170525": "^3.0.1",
    "@alicloud/openapi-client": "^0.4.5"
  }
}
EOF

# 创建安装和配置脚本
echo -e "${YELLOW}🔧 创建安装脚本...${NC}"
cat > $PACKAGE_DIR/install.sh << 'EOF'
#!/bin/bash

echo "🚀 安装阿里云短信服务"
echo "===================="
echo

# 安装依赖
echo "📦 安装阿里云短信SDK..."
npm install

# 复制文件到正确位置
echo "📁 复制文件到项目目录..."
cp services/smsService.js ../services/
cp routes/auth.js ../routes/

# 创建环境变量配置
echo "📝 创建环境变量配置..."
cp env.example ../.env.sms.example

echo
echo "✅ 安装完成！"
echo
echo "📋 下一步操作："
echo "1. 编辑 .env 文件，添加阿里云短信服务配置："
echo "   ALIBABA_CLOUD_ACCESS_KEY_ID=你的AccessKey_ID"
echo "   ALIBABA_CLOUD_ACCESS_KEY_SECRET=你的AccessKey_Secret"
echo "   ALIBABA_CLOUD_SMS_SIGN_NAME=你的短信签名"
echo "   ALIBABA_CLOUD_SMS_TEMPLATE_CODE=你的短信模板代码"
echo
echo "2. 重启服务："
echo "   pm2 restart all"
echo
echo "3. 测试短信服务："
echo "   curl http://localhost:8080/api/auth/sms-status"
EOF

chmod +x $PACKAGE_DIR/install.sh

# 创建详细的部署指南
echo -e "${YELLOW}📚 创建部署指南...${NC}"
cat > $PACKAGE_DIR/DEPLOYMENT_GUIDE.md << 'EOF'
# 阿里云短信服务部署指南

## 概述
此部署包包含了OffTimes应用的阿里云短信服务集成，用于替换原有的模拟短信发送功能。

## 文件说明
- `services/smsService.js` - 阿里云短信服务核心模块
- `routes/auth.js` - 更新的认证路由（集成短信服务）
- `env.example` - 环境变量配置示例
- `package.json` - 依赖包配置
- `install.sh` - 自动安装脚本

## 部署步骤

### 1. 上传部署包到服务器
```bash
scp sms_service_deployment.tar.gz root@60.205.145.35:/var/www/
```

### 2. 解压并安装
```bash
ssh root@60.205.145.35
cd /var/www/
tar -xzf sms_service_deployment.tar.gz
cd sms_service_deployment
./install.sh
```

### 3. 配置阿里云短信服务

#### 3.1 获取阿里云短信服务密钥
1. 登录[阿里云控制台](https://ecs.console.aliyun.com/)
2. 进入"短信服务"控制台
3. 创建短信签名（如：OffTimes）
4. 创建短信模板，内容如：`您的验证码是${code}，5分钟内有效。`
5. 获取AccessKey ID和AccessKey Secret

#### 3.2 配置环境变量
编辑项目根目录的`.env`文件：
```bash
cd /var/www/offtimes-backend  # 或你的项目目录
nano .env
```

添加以下配置：
```env
# 阿里云短信服务配置
ALIBABA_CLOUD_ACCESS_KEY_ID=你的AccessKey_ID
ALIBABA_CLOUD_ACCESS_KEY_SECRET=你的AccessKey_Secret
ALIBABA_CLOUD_SMS_SIGN_NAME=OffTimes
ALIBABA_CLOUD_SMS_TEMPLATE_CODE=SMS_123456789
```

### 4. 重启服务
```bash
# 如果使用PM2
pm2 restart all

# 如果使用systemd
systemctl restart your-service-name

# 或直接重启Node.js进程
pkill -f "node.*server.js"
cd /path/to/your/project
node server.js
```

### 5. 测试短信服务

#### 5.1 检查服务状态
```bash
curl http://localhost:8080/api/auth/sms-status
```

预期响应：
```json
{
  "code": 200,
  "message": "短信服务状态",
  "success": true,
  "data": {
    "isConfigured": true,
    "mode": "production",
    "signName": "OffTimes",
    "templateCode": "SMS_123456789",
    "hasAccessKey": true,
    "hasSecretKey": true
  }
}
```

#### 5.2 测试发送短信
```bash
curl -X POST http://localhost:8080/api/auth/send-sms \
  -H "Content-Type: application/json" \
  -d '{"phoneNumber":"你的手机号","type":"login"}'
```

## 故障排查

### 问题1：服务状态显示未配置
- 检查`.env`文件是否存在且包含正确的配置
- 确认环境变量名称拼写正确
- 重启服务后再次测试

### 问题2：短信发送失败
- 检查阿里云短信服务余额
- 确认短信签名和模板已审核通过
- 查看服务器日志获取详细错误信息

### 问题3：接口404错误
- 确认`routes/auth.js`文件已正确更新
- 检查服务器是否成功重启
- 验证路由配置是否正确

## 回退方案
如果新的短信服务出现问题，系统会自动回退到模拟发送模式，不会影响其他功能。

## 支持
如有问题，请检查服务器日志：
```bash
tail -f /var/log/your-app.log
# 或
pm2 logs
```
EOF

# 创建压缩包
echo -e "${YELLOW}🗜️ 创建压缩包...${NC}"
tar -czf $PACKAGE_FILE $PACKAGE_DIR

# 显示结果
echo
echo -e "${GREEN}✅ 部署包创建完成！${NC}"
echo
echo -e "${BLUE}📦 部署包信息：${NC}"
echo "文件名：$PACKAGE_FILE"
echo "大小：$(du -h $PACKAGE_FILE | cut -f1)"
echo
echo -e "${YELLOW}📋 部署包内容：${NC}"
tar -tzf $PACKAGE_FILE

echo
echo -e "${GREEN}🚀 下一步操作：${NC}"
echo "1. 将 $PACKAGE_FILE 上传到服务器"
echo "2. 在服务器上解压并运行 install.sh"
echo "3. 配置阿里云短信服务密钥"
echo "4. 重启服务并测试"
echo
echo -e "${BLUE}📚 详细说明请查看：${NC}"
echo "- $PACKAGE_DIR/DEPLOYMENT_GUIDE.md"

