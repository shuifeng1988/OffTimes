# Android Studio Logcat过滤完整指南
# Complete Guide for Filtering Logcat in Android Studio

## 🎯 在Android Studio中过滤Logcat

### 方法1: 使用Android Studio内置过滤器

#### 1.1 打开Logcat面板
1. 在Android Studio底部点击 **Logcat** 标签
2. 确保选择了你的设备（真实设备，不是模拟器）

#### 1.2 设置包名过滤器
1. 在Logcat面板顶部找到 **Package** 下拉菜单
2. 选择 **com.offtime.app.gplay**
3. 这样只会显示你的应用的日志

#### 1.3 设置日志级别
1. 在 **Log Level** 下拉菜单中选择：
   - **Verbose**: 显示所有日志
   - **Debug**: 显示Debug及以上级别
   - **Info**: 显示Info及以上级别（推荐）
   - **Warn**: 只显示警告和错误
   - **Error**: 只显示错误

#### 1.4 使用搜索过滤器
在搜索框中输入关键词：
```
GoogleSignIn
```
或者使用正则表达式：
```
GoogleSignIn|OAuth|Login|Google
```

### 方法2: 创建自定义过滤器

#### 2.1 创建Google登录专用过滤器
1. 点击Logcat面板右上角的 **配置过滤器** 图标（齿轮图标）
2. 点击 **+** 创建新过滤器
3. 设置过滤器参数：
   - **Filter Name**: `Google Login Debug`
   - **Package Name**: `com.offtime.app.gplay`
   - **Tag**: `GoogleSignIn|OAuth|Login`
   - **Log Level**: `Debug`

#### 2.2 使用过滤器
创建后，在过滤器下拉菜单中选择 **Google Login Debug**

## 🔧 命令行过滤方法

### 方法3: 使用Terminal命令过滤

#### 3.1 基本过滤命令
```bash
# 只显示你的应用日志
adb logcat | grep "com.offtime.app.gplay"

# 只显示Google相关日志
adb logcat | grep -E "(GoogleSignIn|OAuth|Google|Login)"

# 组合过滤：你的应用 + Google相关
adb logcat | grep "com.offtime.app.gplay" | grep -E "(GoogleSignIn|OAuth|Google|Login)"
```

#### 3.2 高级过滤命令
```bash
# 清除日志并开始过滤监控
adb logcat -c
adb logcat | grep -E "(GoogleSignIn|OAuth|Google|Login|offtime)"

# 只显示错误和警告
adb logcat *:W | grep -E "(GoogleSignIn|OAuth|Google|Login|offtime)"

# 显示特定标签的日志
adb logcat GoogleSignIn:D OAuth:D LoginViewModel:D *:S
```

#### 3.3 保存日志到文件
```bash
# 保存过滤后的日志到文件
adb logcat | grep -E "(GoogleSignIn|OAuth|Google|Login|offtime)" > google_login_debug.log

# 实时查看并保存
adb logcat | grep -E "(GoogleSignIn|OAuth|Google|Login|offtime)" | tee google_login_debug.log
```

## 📋 Google登录专用过滤关键词

### 重要的日志标签和关键词
```
GoogleSignIn
OAuth
GoogleLoginManager
LoginViewModel
LoginScreen
GooglePlayServices
GoogleApiClient
SignInResult
AuthException
GoogleSignInAccount
GoogleSignInOptions
```

### 错误相关关键词
```
Error
Exception
Failed
Unable
Timeout
NetworkError
AuthException
ApiException
```

## 🎯 实用过滤器配置

### 配置1: Google登录调试
```
Filter Name: Google Login Debug
Package Name: com.offtime.app.gplay
Tag: GoogleSignIn|OAuth|Login|GoogleLoginManager
Log Level: Debug
```

### 配置2: 错误监控
```
Filter Name: Google Login Errors
Package Name: com.offtime.app.gplay
Tag: GoogleSignIn|OAuth|Login
Log Level: Error
```

### 配置3: 完整调试
```
Filter Name: OffTimes Complete Debug
Package Name: com.offtime.app.gplay
Tag: (留空，显示所有)
Log Level: Debug
```

## 🔍 实时调试步骤

### 步骤1: 准备环境
1. 连接真实Android设备
2. 在Android Studio中打开Logcat
3. 设置过滤器为 **Google Login Debug**

### 步骤2: 清除日志
1. 点击Logcat面板的 **清除** 按钮（垃圾桶图标）
2. 或在Terminal中运行: `adb logcat -c`

### 步骤3: 开始测试
1. 在设备上启动OffTimes应用
2. 尝试Google登录
3. 观察Logcat中的实时日志

### 步骤4: 分析日志
查找以下关键信息：
- 错误代码（如：Error code 10, 12500）
- 异常信息（Exception, Failed）
- 成功信息（Success, Completed）

## 💡 调试技巧

### 技巧1: 使用颜色标记
在Android Studio的Logcat中：
- **红色**: 错误日志
- **橙色**: 警告日志
- **蓝色**: 信息日志
- **灰色**: 调试日志

### 技巧2: 使用时间戳
启用时间戳显示，帮助追踪问题发生的时间顺序

### 技巧3: 保存重要日志
当发现错误时，立即保存相关日志：
```bash
# 保存最近1000行日志
adb logcat -d | tail -1000 > error_log.txt
```

## 🚨 常见Google登录错误日志

### 错误代码10 (开发者配置错误)
```
GoogleSignIn: Error code: 10
ApiException: 10: 
```

### 错误代码12500 (Google Play服务不可用)
```
GoogleSignIn: Error code: 12500
GooglePlayServices: Google Play services is unavailable
```

### 网络错误
```
GoogleSignIn: NetworkError
OAuth: Connection timeout
```

## 📱 Android Studio快捷操作

### 快捷键
- **Ctrl + F** (Windows) / **Cmd + F** (Mac): 在Logcat中搜索
- **Ctrl + L** (Windows) / **Cmd + L** (Mac): 清除Logcat
- **Ctrl + A** (Windows) / **Cmd + A** (Mac): 全选日志

### 右键菜单
在Logcat中右键可以：
- 复制选中的日志
- 保存日志到文件
- 跳转到源代码

---

**使用建议**: 
1. 先使用包名过滤器只显示你的应用日志
2. 再使用关键词过滤器找到Google登录相关日志
3. 保存重要的错误日志用于分析
