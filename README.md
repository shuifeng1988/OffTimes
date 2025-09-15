# OffTimes - 离线时刻应用

## 项目概述 / Project Overview

**OffTimes** 是一款专业的时间管理和应用使用监控应用，帮助用户更好地管理数字生活，提高工作效率。

**OffTimes** is a professional time management and app usage monitoring application that helps users better manage their digital life and improve productivity.

## 功能特性 / Features

### 核心功能 / Core Features
- 📱 **应用使用监控** / App Usage Monitoring - 实时监控应用使用时间
- ⏰ **离线计时器** / Offline Timer - 专注时间管理工具
- 📊 **数据统计分析** / Data Analytics - 详细的使用统计报告
- 🎯 **目标设定** / Goal Setting - 设置使用时间目标和奖惩机制
- 🔄 **数据同步** / Data Sync - 支持云端数据备份和同步

### 平台支持 / Platform Support
- **支付宝版本** (com.offtime.app) - 适用于国内用户
- **Google Play版本** (com.offtime.app.gplay) - 适用于国际用户

## 技术架构 / Technical Architecture

### 开发技术栈 / Tech Stack
- **语言** / Language: Kotlin
- **UI框架** / UI Framework: Jetpack Compose
- **架构模式** / Architecture: MVVM + Repository Pattern
- **依赖注入** / DI: Hilt
- **数据库** / Database: Room
- **网络请求** / Network: Retrofit + OkHttp
- **异步处理** / Async: Coroutines + Flow

### 项目结构 / Project Structure
```
OffTimes/
├── app/                          # 主应用模块
│   ├── src/
│   │   ├── main/                 # 主要源代码
│   │   │   ├── java/com/offtime/app/
│   │   │   │   ├── data/         # 数据层 (Repository, Database, API)
│   │   │   │   ├── di/           # 依赖注入模块
│   │   │   │   ├── manager/      # 业务管理器
│   │   │   │   ├── receiver/     # 广播接收器
│   │   │   │   ├── service/      # 后台服务
│   │   │   │   ├── ui/           # UI层 (Compose, ViewModel)
│   │   │   │   ├── utils/        # 工具类
│   │   │   │   └── worker/       # 后台任务
│   │   │   └── res/              # 资源文件
│   │   ├── alipay/               # 支付宝版本特定代码
│   │   └── googleplay/           # Google Play版本特定代码
│   └── build.gradle.kts          # 应用构建配置
├── server/                       # 后端服务器 (Node.js)
├── gradle.properties             # Gradle配置
├── build.gradle.kts              # 项目构建配置
└── settings.gradle.kts           # 项目设置
```

## 构建配置 / Build Configuration

### 版本信息 / Version Info
- **版本号** / Version: 1.1.0
- **版本代码** / Version Code: 20
- **最小SDK** / Min SDK: 24 (Android 7.0)
- **目标SDK** / Target SDK: 35 (Android 15)

### 构建变体 / Build Variants
项目支持两个产品变体：

#### 支付宝版本 (Alipay Flavor)
- **包名** / Package: `com.offtime.app`
- **功能** / Features: 短信登录、密码登录、支付宝支付
- **目标市场** / Target: 中国大陆用户

#### Google Play版本 (GooglePlay Flavor)
- **包名** / Package: `com.offtime.app.gplay`
- **功能** / Features: Google登录、Google支付、SMS验证
- **目标市场** / Target: 国际用户

## 开发环境设置 / Development Setup

### 前置要求 / Prerequisites
- Android Studio Hedgehog | 2023.1.1 或更高版本
- JDK 8 或更高版本
- Android SDK 35
- Gradle 8.12

### 配置步骤 / Setup Steps

1. **克隆项目** / Clone Repository
```bash
git clone <repository-url>
cd OffTimes
```

2. **配置gradle.properties**
复制 `gradle.properties.example` 为 `gradle.properties` 并配置：
```properties
# Google Play配置
GOOGLE_WEB_CLIENT_ID=your_google_client_id
GOOGLE_PLAY_LICENSE_KEY=your_license_key

# 支付宝配置
ALIPAY_APP_ID=your_alipay_app_id
ALIPAY_MERCHANT_PRIVATE_KEY=your_private_key
ALIPAY_PUBLIC_KEY=your_public_key
```

3. **配置签名** / Configure Signing
将你的签名文件放在项目根目录：
- `app-release-key.jks` - Release签名文件

4. **构建项目** / Build Project
```bash
# 构建所有变体
./gradlew assembleRelease

# 构建AAB文件
./gradlew bundleRelease
```

## 核心模块说明 / Core Modules

### 数据层 / Data Layer
- **Repository**: 统一数据访问接口
- **Database**: Room数据库，存储用户数据和使用统计
- **API**: 网络请求接口，处理云端同步

### 业务层 / Business Layer
- **Manager**: 各种业务管理器（登录、支付、数据同步等）
- **Service**: 后台服务（数据收集、定时任务等）
- **Worker**: 后台任务处理

### UI层 / UI Layer
- **Compose**: 现代化UI组件
- **ViewModel**: MVVM架构的视图模型
- **Navigation**: 页面导航管理

## API集成 / API Integration

### Google服务集成 / Google Services
- **Google Sign-In**: 用户认证
- **Google Play Billing**: 应用内购买
- **OAuth 2.0**: 安全认证机制

### 支付宝集成 / Alipay Integration
- **支付宝SDK**: 支付功能
- **用户认证**: 支付宝登录

## 数据安全 / Data Security

### 隐私保护 / Privacy Protection
- 所有敏感数据本地加密存储
- 网络传输使用HTTPS加密
- 遵循GDPR和相关隐私法规

### 权限管理 / Permission Management
- 最小权限原则
- 运行时权限请求
- 透明的权限使用说明

## 测试 / Testing

### 单元测试 / Unit Tests
```bash
./gradlew testDebugUnitTest
```

### UI测试 / UI Tests
```bash
./gradlew connectedAndroidTest
```

## 发布流程 / Release Process

### Google Play发布 / Google Play Release
1. 构建AAB文件：`./gradlew bundleGoogleplayRelease`
2. 上传到Google Play Console
3. 配置应用签名和SHA-1指纹

### 支付宝版本发布 / Alipay Version Release
1. 构建APK文件：`./gradlew assembleAlipayRelease`
2. 上传到相应的应用商店

## 故障排除 / Troubleshooting

### 常见问题 / Common Issues

#### Google登录问题
- 确认SHA-1指纹配置正确
- 检查OAuth客户端ID设置
- 验证包名匹配

#### 构建问题
- 清理项目：`./gradlew clean`
- 检查Gradle版本兼容性
- 验证签名文件路径

## 贡献指南 / Contributing

### 代码规范 / Code Standards
- 遵循Kotlin官方编码规范
- 使用Compose最佳实践
- 保持代码注释的中英文对照

### 提交规范 / Commit Standards
- 使用语义化提交信息
- 中英文对照的提交描述
- 详细的变更说明

## 许可证 / License

本项目采用私有许可证，版权归开发者所有。

This project is under private license. All rights reserved.

## 联系方式 / Contact

- **开发者** / Developer: 盘龙区离线时刻软件开发工作室
- **邮箱** / Email: [联系邮箱]
- **官网** / Website: [官方网站]

---

**最后更新** / Last Updated: 2025年9月15日 / September 15, 2025
