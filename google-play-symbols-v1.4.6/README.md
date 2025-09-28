# Google Play 符号文件 v1.4.6

## 📦 包含文件
- `app-googleplay-release.aab` - Google Play AAB文件 (11MB)
- `mapping.txt` - ProGuard映射文件 (120MB) - **未包含在Git中**
- `UPLOAD_GUIDE.md` - 上传指南

## ⚠️ 重要说明
由于GitHub文件大小限制（100MB），`mapping.txt`文件未包含在Git仓库中。

## 📋 获取完整文件
完整的符号文件包（包含mapping.txt）可以通过以下方式获取：
1. 本地构建目录：`/home/shuifeng/OffTimes/google-play-symbols-v1.4.6/`
2. 重新运行构建脚本：`./build_all.sh`

## 🚀 上传到Google Play Console
1. 登录 Google Play Console
2. 选择应用 → 发布管理 → 应用签名
3. 上传 `app-googleplay-release.aab`
4. 在"反混淆文件"部分上传 `mapping.txt`

## 📝 版本信息
- 版本名称: 1.4.6
- 版本代码: 31
- 构建日期: 2025-09-28
- 目标SDK: 35
- 最小SDK: 24
