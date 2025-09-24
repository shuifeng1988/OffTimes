#!/bin/bash

# 获取Google Play版本的签名信息脚本

echo "🔐 OffTimes Google Play 签名信息提取"
echo "===================================="
echo

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${YELLOW}📋 检查可用的密钥文件...${NC}"
echo

# 检查密钥文件
KEYSTORE_FILES=(
    "keystore/google_play.jks"
    "app-release-key.jks" 
    "upload-key.jks"
)

for keystore in "${KEYSTORE_FILES[@]}"; do
    if [ -f "$keystore" ]; then
        echo -e "${GREEN}✅ 找到密钥文件: $keystore${NC}"
        
        echo -e "${BLUE}📊 密钥文件信息:${NC}"
        ls -la "$keystore"
        echo
        
        echo -e "${YELLOW}🔍 尝试提取证书信息 (需要密码):${NC}"
        echo "密钥文件: $keystore"
        echo "命令: keytool -list -v -keystore $keystore"
        echo
        
        # 尝试提取SHA1和MD5指纹
        echo -e "${BLUE}如果你知道密码，请手动运行以下命令:${NC}"
        echo "keytool -list -v -keystore $keystore -alias <alias_name>"
        echo
        
        echo -e "${BLUE}或者从APK文件中提取签名信息:${NC}"
        echo "aapt dump badging app/build/outputs/apk/googleplay/release/app-googleplay-release.apk"
        echo
    else
        echo -e "${RED}❌ 未找到: $keystore${NC}"
    fi
done

echo
echo -e "${YELLOW}📱 从已构建的APK中提取签名信息...${NC}"

# 检查APK文件
APK_FILE="app/build/outputs/apk/googleplay/release/app-googleplay-release.apk"
if [ -f "$APK_FILE" ]; then
    echo -e "${GREEN}✅ 找到Google Play APK文件${NC}"
    
    # 使用aapt提取包名和签名信息
    if command -v aapt &> /dev/null; then
        echo -e "${BLUE}📊 APK基本信息:${NC}"
        aapt dump badging "$APK_FILE" | grep -E "(package|application-label)"
        echo
    fi
    
    # 使用apksigner验证签名
    if command -v apksigner &> /dev/null; then
        echo -e "${BLUE}🔐 APK签名验证:${NC}"
        apksigner verify --print-certs "$APK_FILE"
        echo
    fi
    
    # 使用keytool从APK提取证书
    echo -e "${BLUE}🔍 提取APK证书信息:${NC}"
    echo "可以使用以下命令手动提取:"
    echo "unzip -p $APK_FILE META-INF/*.RSA | keytool -printcert"
    echo
    
else
    echo -e "${RED}❌ 未找到Google Play APK文件: $APK_FILE${NC}"
    echo "请先运行构建命令: ./build_all.sh"
fi

echo
echo -e "${YELLOW}📋 Google Play Console 需要的信息:${NC}"
echo "1. 安卓平台软件包名称: com.offtime.app.gplay"
echo "2. 公钥 (需要从证书中提取)"
echo "3. 证书MD5指纹 (需要从证书中提取)"
echo

echo -e "${BLUE}💡 获取完整签名信息的步骤:${NC}"
echo "1. 找到正确的密钥文件和密码"
echo "2. 运行: keytool -list -v -keystore <keystore_file>"
echo "3. 查找 'Certificate fingerprints' 部分"
echo "4. 复制 MD5 和 SHA1 指纹"
echo "5. 提取公钥信息"
echo

echo -e "${GREEN}🎯 如果你有密钥密码，请告诉我，我可以帮你提取完整信息！${NC}"
