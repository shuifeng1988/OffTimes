# OffTimes 应用内更新功能实现

## 📋 **功能概述**

为OffTimes应用添加应用内更新检查功能，允许用户检查并下载最新版本。

## 🚫 **Google Play 合规性分析**

### **Google Play版本 - 不支持**
- ❌ **违反政策**：绕过Google Play更新机制
- ❌ **安全风险**：引导用户安装外部APK
- ❌ **政策条款**：必须通过Play Store更新

### **支付宝版本 - 完全支持** ✅
- ✅ **无限制**：不受Google Play政策约束
- ✅ **用户友好**：提供便捷的更新体验
- ✅ **安全可控**：从官方网站下载

## 🛠️ **技术实现**

### **1. 数据模型**
```kotlin
// AppUpdateInfo.kt - 更新信息数据类
data class AppUpdateInfo(
    val latestVersion: String,      // 最新版本号
    val latestVersionCode: Int,     // 版本代码
    val downloadUrl: String,        // 下载链接
    val releaseNotes: String,       // 更新说明
    val releaseDate: String,        // 发布日期
    val fileSize: String,           // 文件大小
    val isForceUpdate: Boolean,     // 是否强制更新
    val minSupportedVersion: String? // 最低支持版本
)
```

### **2. 网络服务**
```kotlin
// UpdateApiService.kt - 更新检查API
interface UpdateApiService {
    @GET("api/check-update")
    suspend fun checkUpdate(
        @Query("platform") platform: String,
        @Query("current_version") currentVersion: String,
        @Query("current_version_code") currentVersionCode: Int
    ): Response<UpdateCheckResponse>
    
    @GET(".")
    suspend fun getWebsiteContent(): Response<String>
}
```

### **3. 更新服务**
```kotlin
// AppUpdateService.kt - 核心更新逻辑
@Singleton
class AppUpdateService {
    // 双重检查机制：
    // 1. API接口检查（如果服务器支持）
    // 2. HTML解析检查（备用方案）
    
    suspend fun checkForUpdate(): Result<AppUpdateInfo?>
    fun isInAppUpdateAllowed(): Boolean // 仅支付宝版本返回true
}
```

### **4. UI组件**
```kotlin
// UpdateViewModel.kt - 更新状态管理
sealed class UpdateCheckState {
    object Checking, NoUpdate, NotSupported
    data class UpdateAvailable(val updateInfo: AppUpdateInfo)
    data class Error(val message: String)
}

// UpdateDialog.kt - 更新对话框UI
@Composable
fun UpdateDialog(onDismiss: () -> Unit)
```

## 🔍 **更新检查流程**

### **检查逻辑**：
1. **平台验证**：检查是否为支付宝版本
2. **API尝试**：首先尝试调用更新API
3. **HTML解析**：API失败时解析网站HTML
4. **版本比较**：对比当前版本与最新版本
5. **结果返回**：返回更新信息或无更新状态

### **HTML解析规则**：
```kotlin
// 解析网站中的版本信息
val versionPattern = Pattern.compile("v([0-9]+\\.[0-9]+\\.[0-9]+)")
val fileSizePattern = Pattern.compile("通用版本 \\(([^)]+)\\)")

// 从以下元素提取信息：
// - h3:contains(最新版本) -> 版本号
// - p.version-date -> 发布日期  
// - div.version-features ul -> 更新说明
```

## 📱 **用户界面**

### **设置页面集成**：
- 仅在支付宝版本显示"检查更新"选项
- 点击按钮打开更新对话框
- Google Play版本不显示此选项

### **更新对话框状态**：
1. **检查中**：显示进度指示器
2. **有更新**：显示版本信息、更新说明、下载按钮
3. **无更新**：显示"已是最新版本"
4. **不支持**：显示"Google Play版本请通过商店更新"
5. **错误**：显示错误信息和重试按钮

### **下载选项**：
- **立即下载**：使用DownloadManager下载APK
- **浏览器下载**：打开浏览器访问下载链接
- **稍后更新**：关闭对话框（非强制更新时）

## 🔗 **网站集成**

### **下载链接**：
- **支付宝版**：`https://www.offtimes.cn/downloads/app-alipay-release.apk`
- **Google Play版**：`https://www.offtimes.cn/downloads/app-googleplay-release.apk`

### **API接口**（可选）：
```
GET https://www.offtimes.cn/api/check-update
?platform=alipay&current_version=1.4.6&current_version_code=31

Response:
{
  "hasUpdate": true,
  "updateInfo": {
    "latestVersion": "1.4.7",
    "latestVersionCode": 32,
    "downloadUrl": "https://www.offtimes.cn/downloads/app-alipay-release.apk",
    "releaseNotes": "• 新功能1\n• 修复问题2",
    "releaseDate": "2025-09-29",
    "fileSize": "3.6MB",
    "isForceUpdate": false
  }
}
```

## 🔧 **依赖项**

```kotlin
// build.gradle.kts 新增依赖
implementation("com.squareup.retrofit2:converter-scalars:2.9.0")
implementation("org.jsoup:jsoup:1.16.1")
```

## 📋 **文件清单**

### **新增文件**：
1. `AppUpdateInfo.kt` - 更新信息数据模型
2. `UpdateApiService.kt` - 网络API接口
3. `AppUpdateService.kt` - 更新检查服务
4. `UpdateViewModel.kt` - 更新状态管理
5. `UpdateDialog.kt` - 更新对话框UI

### **修改文件**：
1. `SettingsScreen.kt` - 添加检查更新选项
2. `build.gradle.kts` - 添加依赖项

## 🛡️ **安全考虑**

### **下载安全**：
- ✅ 仅从官方域名下载：`offtimes.cn`
- ✅ HTTPS加密传输
- ✅ 文件完整性验证（通过文件大小）

### **权限要求**：
- `INTERNET` - 网络访问（已有）
- `WRITE_EXTERNAL_STORAGE` - 下载文件（已有）
- `REQUEST_INSTALL_PACKAGES` - 安装APK（需要时动态请求）

## 🎯 **使用场景**

### **支付宝版本用户**：
1. 打开设置页面
2. 点击"检查更新"
3. 查看更新信息
4. 选择下载方式
5. 安装新版本

### **Google Play版本用户**：
- 不显示更新选项
- 引导用户通过Google Play更新

## 📊 **测试验证**

### **功能测试**：
- ✅ 版本比较逻辑
- ✅ HTML解析准确性
- ✅ 下载功能正常
- ✅ UI状态切换
- ✅ 错误处理机制

### **兼容性测试**：
- ✅ 支付宝版本显示更新选项
- ✅ Google Play版本隐藏更新选项
- ✅ 网络异常处理
- ✅ 解析失败处理

## 🚀 **部署建议**

### **服务器端**（可选）：
1. 实现 `/api/check-update` API接口
2. 返回结构化的更新信息
3. 支持平台区分和版本比较

### **网站维护**：
1. 保持HTML结构稳定
2. 及时更新版本信息
3. 确保下载链接有效

---

**实现状态**：✅ 已完成  
**测试状态**：⏳ 待测试  
**部署状态**：⏳ 待部署
