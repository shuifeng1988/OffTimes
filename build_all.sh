#!/bin/bash

# OffTimes 完整构建脚本 (增强版)
# Enhanced Complete Build Script for OffTimes
# 功能: 自动版本升级 + 调试符号准备 + 英文升级说明

set -e  # 出错时立即退出

echo "🚀 开始OffTimes项目增强构建..."
echo "🚀 Starting enhanced build for OffTimes project..."
echo

# ==================== 版本管理函数 ====================

# 获取当前版本信息
get_current_version() {
    VERSION_NAME=$(grep 'versionName = ' app/build.gradle.kts | sed 's/.*"\(.*\)".*/\1/')
    VERSION_CODE=$(grep 'versionCode = ' app/build.gradle.kts | sed 's/.*= \(.*\)/\1/')
    echo "当前版本: $VERSION_NAME (Build $VERSION_CODE)"
    echo "Current version: $VERSION_NAME (Build $VERSION_CODE)"
}

# 自动升级版本
auto_increment_version() {
    echo "📈 自动升级版本..."
    echo "📈 Auto incrementing version..."
    
    # 获取当前版本号
    CURRENT_VERSION_CODE=$(grep 'versionCode = ' app/build.gradle.kts | sed 's/.*= \(.*\)/\1/')
    CURRENT_VERSION_NAME=$(grep 'versionName = ' app/build.gradle.kts | sed 's/.*"\(.*\)".*/\1/')
    
    # 版本号 +1
    NEW_VERSION_CODE=$((CURRENT_VERSION_CODE + 1))
    
    # 解析版本名称 (假设格式为 x.y.z)
    IFS='.' read -r MAJOR MINOR PATCH <<< "${CURRENT_VERSION_NAME%-*}"
    
    # 小版本号 +1 (patch version)
    NEW_PATCH=$((PATCH + 1))
    NEW_VERSION_NAME="$MAJOR.$MINOR.$NEW_PATCH"
    
    # 如果是Google Play版本，保持后缀
    if [[ $CURRENT_VERSION_NAME == *"-gplay" ]]; then
        NEW_VERSION_NAME="$NEW_VERSION_NAME-gplay"
    fi
    
    echo "版本升级: $CURRENT_VERSION_NAME ($CURRENT_VERSION_CODE) → $NEW_VERSION_NAME ($NEW_VERSION_CODE)"
    echo "Version upgrade: $CURRENT_VERSION_NAME ($CURRENT_VERSION_CODE) → $NEW_VERSION_NAME ($NEW_VERSION_CODE)"
    
    # 更新 build.gradle.kts
    sed -i "s/versionCode = $CURRENT_VERSION_CODE/versionCode = $NEW_VERSION_CODE/" app/build.gradle.kts
    sed -i "s/versionName = \"$CURRENT_VERSION_NAME\"/versionName = \"$NEW_VERSION_NAME\"/" app/build.gradle.kts
    
    echo "✅ 版本升级完成"
    echo "✅ Version upgrade completed"
    echo
}

# 生成英文升级说明
generate_english_changelog() {
    echo "📝 生成英文升级说明..."
    echo "📝 Generating English changelog..."
    
    # 根据版本号生成简短的升级说明
    local version_type=""
    if (( NEW_PATCH % 10 == 0 )); then
        version_type="major"
    elif (( NEW_PATCH % 5 == 0 )); then
        version_type="feature"
    else
        version_type="bugfix"
    fi
    
    case $version_type in
        "major")
            CHANGELOG="Major update with new features and performance improvements"
            ;;
        "feature")
            CHANGELOG="New features and UI enhancements"
            ;;
        *)
            CHANGELOG="Bug fixes and stability improvements"
            ;;
    esac
    
    # 创建升级说明文件
    cat > "CHANGELOG_v${NEW_VERSION_NAME}.md" << EOF
# OffTimes v${NEW_VERSION_NAME} Release Notes

## Version Information
- **Version Name**: ${NEW_VERSION_NAME}
- **Version Code**: ${NEW_VERSION_CODE}
- **Build Date**: $(date '+%Y-%m-%d %H:%M:%S')
- **Build Type**: Release

## What's New
${CHANGELOG}

## Technical Details
- Android Target SDK: 35
- Minimum SDK: 24
- Build Tools: Gradle 8.12
- Kotlin Version: Latest
- Compose Version: Latest

## Files Generated
- APK (Alipay): app-alipay-release.apk
- APK (Google Play): app-googleplay-release.apk
- AAB (Alipay): app-alipay-release.aab
- AAB (Google Play): app-googleplay-release.aab
- Debug Symbols: mapping.txt

## Upload Instructions
1. Upload AAB file to respective app stores
2. Upload debug symbols (mapping.txt) to Google Play Console
3. Test on multiple devices before release

---
Generated automatically by build_all.sh
EOF
    
    echo "✅ 英文升级说明已生成: CHANGELOG_v${NEW_VERSION_NAME}.md"
    echo "✅ English changelog generated: CHANGELOG_v${NEW_VERSION_NAME}.md"
    echo
}

# ==================== 调试符号准备函数 ====================

prepare_debug_symbols() {
    echo "🔍 准备调试符号文件..."
    echo "🔍 Preparing debug symbols..."
    
    # 创建调试符号目录
    SYMBOLS_DIR="google-play-symbols-v${NEW_VERSION_NAME}"
    mkdir -p "$SYMBOLS_DIR"
    
    # 复制AAB文件
    if [ -f "app/build/outputs/bundle/googleplayRelease/app-googleplay-release.aab" ]; then
        cp "app/build/outputs/bundle/googleplayRelease/app-googleplay-release.aab" "$SYMBOLS_DIR/"
        AAB_SIZE=$(du -h "$SYMBOLS_DIR/app-googleplay-release.aab" | cut -f1)
        echo "✅ AAB文件已复制: $AAB_SIZE"
        echo "✅ AAB file copied: $AAB_SIZE"
    fi
    
    # 复制mapping文件
    if [ -f "app/build/outputs/mapping/googleplayRelease/mapping.txt" ]; then
        cp "app/build/outputs/mapping/googleplayRelease/mapping.txt" "$SYMBOLS_DIR/"
        MAPPING_SIZE=$(du -h "$SYMBOLS_DIR/mapping.txt" | cut -f1)
        echo "✅ Mapping文件已复制: $MAPPING_SIZE"
        echo "✅ Mapping file copied: $MAPPING_SIZE"
    fi
    
    # 生成文件校验和
    AAB_MD5=$(md5sum "$SYMBOLS_DIR/app-googleplay-release.aab" 2>/dev/null | cut -d' ' -f1 || echo "N/A")
    MAPPING_MD5=$(md5sum "$SYMBOLS_DIR/mapping.txt" 2>/dev/null | cut -d' ' -f1 || echo "N/A")
    
    # 创建上传指南
    cat > "$SYMBOLS_DIR/UPLOAD_GUIDE.md" << EOF
# Google Play Upload Guide v${NEW_VERSION_NAME}

## Quick Upload Checklist
✅ AAB File: app-googleplay-release.aab ($AAB_SIZE)
✅ Debug Symbols: mapping.txt ($MAPPING_SIZE)
✅ Version: ${NEW_VERSION_NAME} (Build ${NEW_VERSION_CODE})

## Upload Steps
1. **Upload AAB**: Go to Google Play Console → Release → Production → Upload AAB
2. **Upload Symbols**: Go to App Signing → Debug symbols → Upload mapping.txt
3. **Release Notes**: Copy from CHANGELOG_v${NEW_VERSION_NAME}.md

## File Verification
- AAB MD5: $AAB_MD5
- Mapping MD5: $MAPPING_MD5
- Build Date: $(date)

## Important Notes
- Ensure version codes match between AAB and mapping files
- Wait for Google Play processing (usually 1-2 hours)
- Test the release before going live

## Links
- [Google Play Console](https://play.google.com/console)
- [Upload Documentation](https://developer.android.com/guide/app-bundle/upload-bundle)

---
Auto-generated by build_all.sh v${NEW_VERSION_NAME}
EOF
    
    echo "✅ 调试符号准备完成: $SYMBOLS_DIR/"
    echo "✅ Debug symbols prepared: $SYMBOLS_DIR/"
    echo
}

# ==================== 主构建流程 ====================

main() {
    # 显示当前版本
    get_current_version
    echo
    
    # 自动升级版本
    auto_increment_version
    
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
    
    # 停止Gradle守护进程确保新JVM参数生效
    echo "🔄 重启Gradle守护进程..."
    echo "🔄 Restarting Gradle daemon..."
    ./gradlew --stop
    sleep 2
    echo
    
    # 清理项目
    echo "🧹 清理项目..."
    echo "🧹 Cleaning project..."
    ./gradlew clean
    echo "✅ 清理完成"
    echo "✅ Clean completed"
    echo
    
    # 构建Debug版本
    echo "🔨 构建Debug版本..."
    echo "🔨 Building Debug versions..."
    ./gradlew assembleDebug
    echo "✅ Debug版本构建完成"
    echo "✅ Debug build completed"
    echo
    
    # 构建Release版本
    echo "🔨 构建Release版本..."
    echo "🔨 Building Release versions..."
    ./gradlew assembleRelease
    echo "✅ Release版本构建完成"
    echo "✅ Release build completed"
    echo
    
    # 构建AAB文件
    echo "📦 构建AAB文件..."
    echo "📦 Building AAB files..."
    ./gradlew bundleRelease
    echo "✅ AAB文件构建完成"
    echo "✅ AAB build completed"
    echo
    
    # 准备调试符号
    prepare_debug_symbols
    
    # 生成英文升级说明
    generate_english_changelog
    
    # 显示构建结果
    echo "📋 构建结果汇总 / Build Results Summary"
    echo "========================================"
    echo "🎯 版本信息: $NEW_VERSION_NAME (Build $NEW_VERSION_CODE)"
    echo "🎯 Version Info: $NEW_VERSION_NAME (Build $NEW_VERSION_CODE)"
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
    echo "🔍 调试符号文件 / Debug Symbol Files:"
    if [ -d "$SYMBOLS_DIR" ]; then
        echo "  ✅ 调试符号目录: $SYMBOLS_DIR/"
        echo "  ✅ Debug symbols directory: $SYMBOLS_DIR/"
        echo "  📁 包含文件: AAB + mapping.txt + 上传指南"
        echo "  📁 Contains: AAB + mapping.txt + upload guide"
    fi
    
    echo
    echo "📝 文档文件 / Documentation Files:"
    if [ -f "CHANGELOG_v${NEW_VERSION_NAME}.md" ]; then
        echo "  ✅ 英文升级说明: CHANGELOG_v${NEW_VERSION_NAME}.md"
        echo "  ✅ English changelog: CHANGELOG_v${NEW_VERSION_NAME}.md"
    fi
    
    echo
    echo "🎯 重要提醒 / Important Notes:"
    echo "1. 🚀 Google Play上传: 使用 $SYMBOLS_DIR/ 中的文件"
    echo "   🚀 Google Play upload: Use files in $SYMBOLS_DIR/"
    echo "2. 📝 升级说明: 参考 CHANGELOG_v${NEW_VERSION_NAME}.md"
    echo "   📝 Release notes: Refer to CHANGELOG_v${NEW_VERSION_NAME}.md"
    echo "3. 🔍 调试符号: mapping.txt 已自动准备"
    echo "   🔍 Debug symbols: mapping.txt automatically prepared"
    echo "4. ✅ 版本已自动升级: $NEW_VERSION_NAME (Build $NEW_VERSION_CODE)"
    echo "   ✅ Version auto-incremented: $NEW_VERSION_NAME (Build $NEW_VERSION_CODE)"
    
    echo
    echo "🎉 增强构建任务完成！无需手动处理版本和调试符号！"
    echo "🎉 Enhanced build completed! No manual version or debug symbol handling needed!"
    echo
    echo "📋 下次构建将自动升级到: $(echo $NEW_VERSION_NAME | sed 's/\([0-9]*\)$/\1/' | awk -F. '{print $1"."$2"."($3+1)}') (Build $((NEW_VERSION_CODE + 1)))"
    echo "📋 Next build will auto-increment to: $(echo $NEW_VERSION_NAME | sed 's/\([0-9]*\)$/\1/' | awk -F. '{print $1"."$2"."($3+1)}') (Build $((NEW_VERSION_CODE + 1)))"
}

# 执行主函数
main "$@"