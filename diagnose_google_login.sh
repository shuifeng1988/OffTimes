#!/bin/bash
# Google登录问题诊断脚本
# Google Login Issue Diagnosis Script

echo "🔍 Google登录配置诊断"
echo "======================"
echo "时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo

# 1. 检查gradle.properties配置
echo "📋 1. 检查gradle.properties配置:"
echo "--------------------------------"
if [ -f "gradle.properties" ]; then
    echo "✅ gradle.properties 文件存在"
    
    # 检查GOOGLE_WEB_CLIENT_ID
    CLIENT_ID=$(grep "^GOOGLE_WEB_CLIENT_ID=" gradle.properties | cut -d'=' -f2)
    if [ -n "$CLIENT_ID" ]; then
        echo "✅ GOOGLE_WEB_CLIENT_ID: $CLIENT_ID"
        
        # 验证Client ID格式
        if [[ $CLIENT_ID =~ ^[0-9]+-[a-zA-Z0-9]+\.apps\.googleusercontent\.com$ ]]; then
            echo "✅ Client ID格式正确"
        else
            echo "❌ Client ID格式可能有问题"
        fi
    else
        echo "❌ GOOGLE_WEB_CLIENT_ID 未配置"
    fi
else
    echo "❌ gradle.properties 文件不存在"
fi

echo

# 2. 检查应用包名配置
echo "📱 2. 检查应用包名配置:"
echo "----------------------"
if [ -f "app/build.gradle.kts" ]; then
    echo "✅ build.gradle.kts 文件存在"
    
    # 检查Google Play版本包名
    GPLAY_PACKAGE=$(grep -A1 'create("googleplay")' app/build.gradle.kts | grep applicationId | sed 's/.*= *"\([^"]*\)".*/\1/')
    if [ "$GPLAY_PACKAGE" = "com.offtime.app.gplay" ]; then
        echo "✅ Google Play包名: $GPLAY_PACKAGE (正确)"
    else
        echo "❌ Google Play包名: $GPLAY_PACKAGE (应该是 com.offtime.app.gplay)"
    fi
    
    # 检查支付宝版本包名
    ALIPAY_PACKAGE=$(grep -A1 'create("alipay")' app/build.gradle.kts | grep applicationId | sed 's/.*= *"\([^"]*\)".*/\1/')
    if [ "$ALIPAY_PACKAGE" = "com.offtime.app" ]; then
        echo "✅ 支付宝包名: $ALIPAY_PACKAGE (正确)"
    else
        echo "❌ 支付宝包名: $ALIPAY_PACKAGE (应该是 com.offtime.app)"
    fi
else
    echo "❌ app/build.gradle.kts 文件不存在"
fi

echo

# 3. 检查签名文件和SHA-1指纹
echo "🔐 3. 检查签名配置:"
echo "------------------"
if [ -f "app-release-key.jks" ]; then
    echo "✅ Release签名文件存在: app-release-key.jks"
    
    # 获取Release SHA-1指纹
    echo "📋 Release SHA-1指纹:"
    keytool -list -v -keystore app-release-key.jks -alias offtime-key -storepass offtime2024 2>/dev/null | grep "SHA1:" | head -1 | sed 's/.*SHA1: //'
else
    echo "❌ Release签名文件不存在"
fi

# 获取Debug SHA-1指纹
DEBUG_KEYSTORE="$HOME/.android/debug.keystore"
if [ -f "$DEBUG_KEYSTORE" ]; then
    echo "✅ Debug签名文件存在"
    echo "📋 Debug SHA-1指纹:"
    keytool -list -v -keystore "$DEBUG_KEYSTORE" -alias androiddebugkey -storepass android -keypass android 2>/dev/null | grep "SHA1:" | head -1 | sed 's/.*SHA1: //'
else
    echo "❌ Debug签名文件不存在"
fi

echo

# 4. 检查构建产物
echo "📦 4. 检查构建产物:"
echo "------------------"
GPLAY_RELEASE_APK="app/build/outputs/apk/googleplay/release/app-googleplay-release.apk"
GPLAY_DEBUG_APK="app/build/outputs/apk/googleplay/debug/app-googleplay-debug.apk"

if [ -f "$GPLAY_RELEASE_APK" ]; then
    echo "✅ Google Play Release APK存在"
    SIZE=$(du -h "$GPLAY_RELEASE_APK" | cut -f1)
    echo "   大小: $SIZE"
else
    echo "❌ Google Play Release APK不存在"
fi

if [ -f "$GPLAY_DEBUG_APK" ]; then
    echo "✅ Google Play Debug APK存在"
    SIZE=$(du -h "$GPLAY_DEBUG_APK" | cut -f1)
    echo "   大小: $SIZE"
else
    echo "❌ Google Play Debug APK不存在"
fi

echo

# 5. 检查设备连接
echo "📱 5. 检查设备连接:"
echo "------------------"
DEVICES=$(adb devices | grep -v "List of devices" | grep -v "^$" | wc -l)
if [ $DEVICES -gt 0 ]; then
    echo "✅ 检测到 $DEVICES 个设备/模拟器"
    adb devices | grep -v "List of devices" | grep -v "^$"
else
    echo "❌ 没有检测到设备/模拟器"
fi

echo

# 6. 生成修复建议
echo "💡 修复建议:"
echo "============"
echo "1. 确保Google Cloud Console中的配置:"
echo "   - 应用类型: Android应用"
echo "   - 包名: com.offtime.app.gplay"
echo "   - SHA-1指纹: 添加上面显示的Debug和Release指纹"
echo
echo "2. 如果配置正确但仍然失败:"
echo "   - 等待5-10分钟让Google配置生效"
echo "   - 重新构建应用: ./gradlew clean assembleGoogleplayRelease"
echo "   - 在真实设备上测试（不要用模拟器）"
echo
echo "3. 检查Google Console中的API启用状态:"
echo "   - Google Sign-In API"
echo "   - People API"
echo
echo "4. 确保OAuth同意屏幕已配置完整"

echo
echo "🔧 快速修复命令:"
echo "================"
echo "# 重新构建应用"
echo "./gradlew clean"
echo "./gradlew assembleGoogleplayRelease"
echo
echo "# 安装到设备"
echo "adb install -r app/build/outputs/apk/googleplay/release/app-googleplay-release.apk"
echo
echo "# 查看登录日志"
echo "adb logcat | grep -E '(Google|Login|OAuth|offtime)'"

echo
echo "📞 如需进一步帮助，请提供:"
echo "=========================="
echo "1. Google Cloud Console的完整配置截图"
echo "2. 应用登录时的完整logcat日志"
echo "3. 确认测试设备类型（真机/模拟器）"
