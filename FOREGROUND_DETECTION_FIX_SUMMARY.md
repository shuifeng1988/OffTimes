# 前台应用检测逻辑修复总结

## 问题描述

在支付宝版本中发现了前台应用检测的矛盾日志：
- 同一个更新周期内，状态机显示 `com.offtime.app` 为前台应用
- 但实时检测却返回"无前台应用(可能在桌面)"
- 导致使用时间统计不准确，1小时只能记录9分钟左右

## 根本原因分析

### 1. 包名硬编码问题 ✅ 已修复
- **问题**: `UsageStatsCollectorService.kt` 中硬编码使用 `"com.offtime.app"`
- **影响**: 支付宝版本(`com.offtime.app`)正常，但Google Play版本(`com.offtime.app.gplay`)检测异常
- **修复**: 将所有硬编码包名替换为 `applicationContext.packageName`

### 2. 前台检测结果不一致问题 🔧 本次修复
- **问题**: 同一更新周期内 `getForegroundApp()` 返回不同结果
- **原因**: 
  - 两次调用之间存在时间差
  - `UsageStats` 数据可能发生微小变化
  - `AppLifecycleObserver.isActivityInForeground` 状态可能变化
  - `isLauncherApp()` 方法检测不稳定

## 修复方案

### 1. 改进 `isLauncherApp()` 方法
```kotlin
private fun isLauncherApp(packageName: String?): Boolean {
    // 1. 首先检查常见Launcher包名列表
    val commonLaunchers = setOf(
        "com.android.launcher", "com.android.launcher2", "com.android.launcher3",
        "com.google.android.apps.nexuslauncher", "com.oneplus.launcher",
        "com.samsung.android.launcher", "com.huawei.android.launcher",
        "com.miui.home", "com.oppo.launcher", "com.vivo.launcher", ...
    )
    
    // 2. 然后使用系统API验证
    val resolveInfos = packageManager.queryIntentActivities(intent, 0)
    // ...
}
```

### 2. 添加前台检测缓存机制
```kotlin
// 缓存变量
private var foregroundAppCache: String? = null
private var foregroundAppCacheTime: Long = 0L
private val FOREGROUND_CACHE_DURATION = 5000L // 5秒缓存

private fun getForegroundApp(): String? {
    // 检查缓存是否有效
    if (foregroundAppCacheTime > 0 && (now - foregroundAppCacheTime) < FOREGROUND_CACHE_DURATION) {
        return foregroundAppCache // 返回缓存结果
    }
    
    // 执行检测逻辑...
    // 更新缓存
    foregroundAppCache = result
    foregroundAppCacheTime = now
    
    return result
}
```

### 3. 每个更新周期清除缓存
```kotlin
ACTION_UNIFIED_UPDATE -> {
    serviceScope.launch {
        // 清除前台应用检测缓存，确保新的更新周期使用最新数据
        foregroundAppCache = null
        foregroundAppCacheTime = 0L
        
        // 执行更新逻辑...
    }
}
```

### 4. 增强调试日志
- 添加详细的候选应用检查日志
- 显示 `AppLifecycleObserver.isActivityInForeground` 状态
- 记录缓存使用情况

## 预期效果

1. **消除矛盾日志**: 同一更新周期内前台检测结果保持一致
2. **提高检测准确性**: 改进的Launcher检测逻辑更稳定
3. **优化性能**: 缓存机制减少重复的系统API调用
4. **增强调试能力**: 详细日志便于问题排查

## 测试建议

使用 `debug_foreground_detection_detailed.sh` 脚本进行测试：
1. 保持OffTimes在前台观察日志
2. 切换到桌面观察检测变化
3. 切回OffTimes验证状态恢复
4. 检查是否还有矛盾的日志输出

## 文件修改列表

- `app/src/main/java/com/offtime/app/service/UsageStatsCollectorService.kt`
  - 添加前台检测缓存机制
  - 改进 `isLauncherApp()` 方法
  - 增强调试日志
  - 每个更新周期清除缓存

## 版本信息

- 修复版本: v1.4.6
- 影响平台: 主要修复支付宝版本，Google Play版本也受益
- 构建状态: ✅ 编译成功
