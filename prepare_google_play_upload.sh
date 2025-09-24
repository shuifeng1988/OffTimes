#!/bin/bash

# Google Play 上传准备脚本
# 检查并准备所有必要的文件用于 Google Play Console 上传

echo "🚀 Google Play 上传准备脚本"
echo "=================================="

# 检查必要文件
AAB_FILE="app/build/outputs/bundle/googleplayRelease/app-googleplay-release.aab"
MAPPING_FILE="app/build/outputs/mapping/googleplayRelease/mapping.txt"
SYMBOLS_DIR="google-play-symbols"

echo "📋 检查构建文件..."

# 检查 AAB 文件
if [ -f "$AAB_FILE" ]; then
    AAB_SIZE=$(du -h "$AAB_FILE" | cut -f1)
    echo "✅ AAB文件: $AAB_FILE ($AAB_SIZE)"
else
    echo "❌ 错误: AAB文件不存在: $AAB_FILE"
    echo "请先运行: ./gradlew bundleGoogleplayRelease"
    exit 1
fi

# 检查 mapping 文件
if [ -f "$MAPPING_FILE" ]; then
    MAPPING_SIZE=$(du -h "$MAPPING_FILE" | cut -f1)
    echo "✅ Mapping文件: $MAPPING_FILE ($MAPPING_SIZE)"
else
    echo "❌ 错误: Mapping文件不存在: $MAPPING_FILE"
    echo "请先运行: ./gradlew bundleGoogleplayRelease"
    exit 1
fi

# 创建符号文件目录
mkdir -p "$SYMBOLS_DIR"

# 复制文件到符号目录
echo ""
echo "📦 准备上传文件..."
cp "$MAPPING_FILE" "$SYMBOLS_DIR/"
cp "$AAB_FILE" "$SYMBOLS_DIR/"

echo "✅ 文件已复制到: $SYMBOLS_DIR/"

# 显示版本信息
echo ""
echo "📱 版本信息:"
VERSION_NAME=$(grep 'versionName = ' app/build.gradle.kts | sed 's/.*"\(.*\)".*/\1/')
VERSION_CODE=$(grep 'versionCode = ' app/build.gradle.kts | sed 's/.*= \(.*\)/\1/')
echo "  版本名称: $VERSION_NAME"
echo "  版本代码: $VERSION_CODE"

# 生成文件校验和
echo ""
echo "🔍 文件校验信息:"
AAB_MD5=$(md5sum "$SYMBOLS_DIR/app-googleplay-release.aab" | cut -d' ' -f1)
MAPPING_MD5=$(md5sum "$SYMBOLS_DIR/mapping.txt" | cut -d' ' -f1)
echo "  AAB MD5: $AAB_MD5"
echo "  Mapping MD5: $MAPPING_MD5"

# 创建上传清单
cat > "$SYMBOLS_DIR/upload_checklist.txt" << EOF
Google Play Console 上传清单
============================

📦 文件清单:
✅ app-googleplay-release.aab ($AAB_SIZE)
✅ mapping.txt ($MAPPING_SIZE)
✅ README_UPLOAD_SYMBOLS.md

📱 版本信息:
- 应用包名: com.offtime.app.gplay
- 版本名称: $VERSION_NAME
- 版本代码: $VERSION_CODE
- 构建时间: $(date)

🔍 文件校验:
- AAB MD5: $AAB_MD5
- Mapping MD5: $MAPPING_MD5

📋 上传步骤:
1. 上传 AAB 文件到 Google Play Console
2. 在"应用签名"页面上传 mapping.txt
3. 等待处理完成并验证符号化效果

⚠️ 重要提醒:
- 确保版本号匹配
- 保留文件备份
- 验证符号化效果
EOF

echo ""
echo "📋 上传清单已生成: $SYMBOLS_DIR/upload_checklist.txt"

echo ""
echo "🎯 上传准备完成!"
echo "📁 文件位置: $SYMBOLS_DIR/"
echo ""
echo "🔗 Google Play Console: https://play.google.com/console"
echo "📖 详细说明: $SYMBOLS_DIR/README_UPLOAD_SYMBOLS.md"
echo ""
echo "✨ 现在可以安全地上传到 Google Play Store!"
