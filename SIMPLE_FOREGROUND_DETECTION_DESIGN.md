# 简化前台应用检测设计

## 🎯 核心需求
1. 检查屏幕是否亮屏
2. 获取当前前台应用
3. 统计前台应用使用时间
4. 过滤OffTimes和后台噪音

## 🏗️ 简化设计方案

### 1. 单一前台检测方法
```kotlin
private fun getCurrentForegroundApp(): String? {
    // 1. 检查屏幕状态
    if (!isScreenOn()) {
        return null // 屏幕关闭，无前台应用
    }
    
    // 2. 获取最近的前台应用（简单直接）
    val recentApp = getLastResumedApp()
    
    // 3. 过滤无效应用
    return if (isValidForegroundApp(recentApp)) recentApp else null
}
```

### 2. 屏幕状态检测
```kotlin
private fun isScreenOn(): Boolean {
    val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isInteractive
}
```

### 3. 最近前台应用检测
```kotlin
private fun getLastResumedApp(): String? {
    try {
        val now = System.currentTimeMillis()
        val events = usageStatsManager.queryEvents(now - 30_000, now)
        
        var lastApp: String? = null
        while (events.hasNextEvent()) {
            val event = UsageEvents.Event()
            events.getNextEvent(event)
            
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED, 1, 19 -> {
                    lastApp = event.packageName
                }
                UsageEvents.Event.ACTIVITY_PAUSED, 2, 20, 23 -> {
                    if (event.packageName == lastApp) {
                        lastApp = null // 应用已暂停
                    }
                }
            }
        }
        return lastApp
    } catch (e: Exception) {
        Log.w(TAG, "获取前台应用失败", e)
        return null
    }
}
```

### 4. 应用有效性验证
```kotlin
private fun isValidForegroundApp(packageName: String?): Boolean {
    if (packageName == null) return false
    
    // 过滤OffTimes（UI不在前台时）
    if (packageName.startsWith(applicationContext.packageName)) {
        return AppLifecycleObserver.isActivityInForeground.value
    }
    
    // 过滤Launcher
    if (isLauncherApp(packageName)) {
        return false
    }
    
    // 过滤系统应用
    return isUserApp(packageName)
}
```

### 5. 统计逻辑简化
```kotlin
suspend fun updateUsageStats() {
    val currentApp = getCurrentForegroundApp()
    
    when {
        currentApp == null -> {
            // 无前台应用，结束当前会话
            endCurrentSession()
            Log.d(TAG, "📱 无前台应用（屏幕关闭或桌面）")
        }
        
        currentApp != currentForegroundPackage -> {
            // 应用切换
            endCurrentSession()
            startNewSession(currentApp)
            Log.d(TAG, "📱 应用切换: $currentForegroundPackage -> $currentApp")
        }
        
        else -> {
            // 继续当前会话
            updateCurrentSession()
            Log.d(TAG, "📱 继续统计: $currentApp")
        }
    }
}
```

## 🎉 简化后的优势

1. **逻辑清晰**: 一个方法解决前台检测
2. **易于理解**: 每个步骤都很直观
3. **易于调试**: 减少复杂的状态判断
4. **性能更好**: 减少重复的API调用
5. **维护简单**: 问题容易定位和修复

## 🔄 实施计划

1. **保留现有复杂逻辑**（作为备份）
2. **实现简化版本**
3. **并行测试对比**
4. **确认无问题后替换**

您觉得这个简化方案如何？我们可以实现这个更清晰的版本！
