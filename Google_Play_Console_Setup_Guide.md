# Google Play Console 完整配置指南 / Complete Google Play Console Setup Guide

## 🎯 第一步：创建应用内商品 / Step 1: Create In-App Product

### 在 Google Play Console 中：
1. 进入你的应用 → **Monetization** → **Products** → **In-app products**
2. 点击 **Create product**
3. 填写商品信息：
   - **Product ID**: `premium_lifetime` （必须与代码一致）
   - **Name**: `OffTimes Premium Lifetime`
   - **Description**: `Unlock all premium features with one-time purchase`
   - **Price**: `$9.90` （或你想要的价格）
   - **Product type**: `One-time`

4. **重要**：创建后必须点击 **Save** → **Activate** → 然后发布到测试轨道

## 🔑 第二步：获取 License Key / Step 2: Get License Key

1. 在 Google Play Console：**Monetization** → **Setup** → **Licensing**
2. 复制 **Base64-encoded RSA public key**（约440字符的长字符串）
3. 更新 `gradle.properties`：
```properties
GOOGLE_PLAY_LICENSE_KEY=MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...（你的License Key）
```

## 👥 第三步：设置测试人员 / Step 3: Setup Testers

### 内部测试（推荐）：
1. **Release** → **Testing** → **Internal testing**
2. 点击 **Create new release**
3. 上传你的 `googleplayRelease` AAB 文件
4. 在 **Testers** 标签页添加测试邮箱（必须是Gmail账号）
5. **Save** → **Review** → **Start rollout to internal testing**

### 重要提醒：
- 测试设备必须登录同一个测试邮箱的 Google Play 商店
- 商品必须在测试轨道中"已发布"状态才能购买

## 🔧 第四步：代码配置检查 / Step 4: Code Configuration Check

确认以下配置正确：

### gradle.properties
```properties
GOOGLE_WEB_CLIENT_ID=你的Web应用客户端ID
GOOGLE_PLAY_LICENSE_KEY=你的License Key
```

### 应用签名
- 确保使用与 Play Console 匹配的签名
- SHA-1 指纹已添加到 Google Cloud Console OAuth 凭据

## 🧪 第五步：测试流程 / Step 5: Testing Process

1. **安装测试版本**：
   ```bash
   ./gradlew assembleGoogleplayRelease
   adb install app/build/outputs/apk/googleplay/release/app-googleplay-release.apk
   ```

2. **登录测试账号**：设备 Play 商店登录测试邮箱

3. **测试购买**：
   - 打开应用 → 进入支付页面
   - 选择 Google Play 支付
   - 应该能看到 $9.90 的 "OffTimes Premium Lifetime"

4. **查看日志**：
   ```bash
   adb logcat | grep -E "(GooglePlayBilling|Purchase|Billing)"
   ```

## ❌ 常见错误及解决方案 / Common Issues & Solutions

### "未找到商品 premium_lifetime"
- 商品未发布到测试轨道
- Product ID 不匹配
- 测试账号未加入白名单

### "计费服务未连接"
- 设备未安装 Google Play 服务
- Play 商店版本过旧
- 网络连接问题

### "购买失败"
- License Key 错误或未配置
- 签名不匹配
- 权限缺失

### "Developer payload is missing"
- 这是正常的，新版本 Billing Library 不再需要 payload

## 🚀 第六步：发布准备 / Step 6: Release Preparation

发布前确认：
- [ ] 商品已在内部测试中验证可购买
- [ ] License Key 已正确配置
- [ ] 签名证书与 Play Console 一致
- [ ] 版本号已递增
- [ ] 所有测试通过

---

## 🆘 需要帮助？/ Need Help?

如果遇到问题，请提供：
1. 具体错误信息
2. logcat 日志（包含 GooglePlayBilling 标签）
3. 当前配置状态（商品是否已发布、测试账号是否已添加等）
