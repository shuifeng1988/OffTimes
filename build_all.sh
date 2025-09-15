#!/bin/bash

# OffTimes 完整构建脚本
# Complete Build Script for OffTimes

echo "🚀 开始OffTimes项目完整构建..."
echo "🚀 Starting complete build for OffTimes project..."
echo

# 检查必要文件
echo "📋 检查构建环境..."
echo "📋 Checking build environment..."

if [ ! -f "gradle.properties" ]; then
    echo "❌ 错误: gradle.properties 文件不存在"
    echo "❌ Error: gradle.properties file not found"
    echo "请复制 gradle.properties.example 并配置相关密钥"
    echo "Please copy gradle.properties.example and configure the keys"
    exit 1
fi

if [ ! -f "app-release-key.jks" ]; then
    echo "⚠️  警告: 未找到发布签名文件 app-release-key.jks"
    echo "⚠️  Warning: Release keystore app-release-key.jks not found"
    echo "将使用调试签名构建"
    echo "Will build with debug signing"
fi

echo "✅ 环境检查完成"
echo "✅ Environment check completed"
echo

# 清理项目
echo "🧹 清理项目..."
echo "🧹 Cleaning project..."
./gradlew clean
if [ $? -ne 0 ]; then
    echo "❌ 清理失败"
    echo "❌ Clean failed"
    exit 1
fi
echo "✅ 清理完成"
echo "✅ Clean completed"
echo

# 构建Debug版本
echo "🔨 构建Debug版本..."
echo "🔨 Building Debug versions..."
./gradlew assembleDebug
if [ $? -ne 0 ]; then
    echo "❌ Debug构建失败"
    echo "❌ Debug build failed"
    exit 1
fi
echo "✅ Debug版本构建完成"
echo "✅ Debug build completed"
echo

# 构建Release版本
echo "🔨 构建Release版本..."
echo "🔨 Building Release versions..."
./gradlew assembleRelease
if [ $? -ne 0 ]; then
    echo "❌ Release构建失败"
    echo "❌ Release build failed"
    exit 1
fi
echo "✅ Release版本构建完成"
echo "✅ Release build completed"
echo

# 构建AAB文件
echo "📦 构建AAB文件..."
echo "📦 Building AAB files..."
./gradlew bundleRelease
if [ $? -ne 0 ]; then
    echo "❌ AAB构建失败"
    echo "❌ AAB build failed"
    exit 1
fi
echo "✅ AAB文件构建完成"
echo "✅ AAB build completed"
echo

# 显示构建结果
echo "📋 构建结果汇总 / Build Results Summary"
echo "========================================"

echo
echo "🍎 APK文件 / APK Files:"
if [ -f "app/build/outputs/apk/alipay/debug/app-alipay-debug.apk" ]; then
    size=$(du -h "app/build/outputs/apk/alipay/debug/app-alipay-debug.apk" | cut -f1)
    echo "  ✅ 支付宝Debug版: app-alipay-debug.apk ($size)"
    echo "  ✅ Alipay Debug: app-alipay-debug.apk ($size)"
fi

if [ -f "app/build/outputs/apk/googleplay/debug/app-googleplay-debug.apk" ]; then
    size=$(du -h "app/build/outputs/apk/googleplay/debug/app-googleplay-debug.apk" | cut -f1)
    echo "  ✅ Google Play Debug版: app-googleplay-debug.apk ($size)"
    echo "  ✅ Google Play Debug: app-googleplay-debug.apk ($size)"
fi

if [ -f "app/build/outputs/apk/alipay/release/app-alipay-release.apk" ]; then
    size=$(du -h "app/build/outputs/apk/alipay/release/app-alipay-release.apk" | cut -f1)
    echo "  ✅ 支付宝Release版: app-alipay-release.apk ($size)"
    echo "  ✅ Alipay Release: app-alipay-release.apk ($size)"
fi

if [ -f "app/build/outputs/apk/googleplay/release/app-googleplay-release.apk" ]; then
    size=$(du -h "app/build/outputs/apk/googleplay/release/app-googleplay-release.apk" | cut -f1)
    echo "  ✅ Google Play Release版: app-googleplay-release.apk ($size)"
    echo "  ✅ Google Play Release: app-googleplay-release.apk ($size)"
fi

echo
echo "📦 AAB文件 / AAB Files:"
if [ -f "app/build/outputs/bundle/alipayRelease/app-alipay-release.aab" ]; then
    size=$(du -h "app/build/outputs/bundle/alipayRelease/app-alipay-release.aab" | cut -f1)
    echo "  ✅ 支付宝AAB: app-alipay-release.aab ($size)"
    echo "  ✅ Alipay AAB: app-alipay-release.aab ($size)"
fi

if [ -f "app/build/outputs/bundle/googleplayRelease/app-googleplay-release.aab" ]; then
    size=$(du -h "app/build/outputs/bundle/googleplayRelease/app-googleplay-release.aab" | cut -f1)
    echo "  ✅ Google Play AAB: app-googleplay-release.aab ($size)"
    echo "  ✅ Google Play AAB: app-googleplay-release.aab ($size)"
fi

echo
echo "🎯 重要提醒 / Important Notes:"
echo "1. 上传到Google Play的文件: app-googleplay-release.aab"
echo "   File for Google Play upload: app-googleplay-release.aab"
echo "2. 确保在Google Console中配置了正确的SHA-1指纹"
echo "   Ensure correct SHA-1 fingerprint is configured in Google Console"
echo "3. 测试Google登录功能需要正确的OAuth配置"
echo "   Google login testing requires proper OAuth configuration"

echo
echo "🎉 所有构建任务完成！"
echo "🎉 All build tasks completed successfully!"
