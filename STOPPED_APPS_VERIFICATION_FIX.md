# 已停止应用验证修复

## 🎯 问题描述

用户在01:15:20左右切换到桌面，但系统仍然显示Chrome为活跃应用：

```
01:15:49.961 D/UsageStatsCollector: ⏹️ 应用停止: com.android.chrome at 01:15:28
01:15:49.698 D/UsageStatsCollector: getForegroundApp(new): 即时前台=com.android.chrome, age=19945ms
01:15:49.698 D/UsageStatsCollector: 实时统计 → 检测到新的前台应用: com.android.chrome
```

## 🔍 根本原因

**关键矛盾**：
1. **事件流显示**: Chrome在01:15:28已经停止
2. **UsageStats显示**: Chrome的 `lastTimeVisible` 仍然是19秒前，被认为是"最新"前台应用

**问题根源**：
- `UsageStats.lastTimeVisible` 时间戳不会因为停止事件立即更新
- `getForegroundApp()` 只检查时间戳，不验证应用是否已被停止事件终止
- 导致已停止的应用仍被误认为在前台

## 🛠️ 修复方案

### 1. 添加停止事件验证机制

```kotlin
// 首先获取最近的停止事件，用于验证候选应用是否已停止
val recentStoppedApps = getRecentStoppedApps(now)

// 检查候选应用是否已被最近的停止事件终止
if (recentStoppedApps.contains(candidate)) {
    Log.v(TAG, "getForegroundApp(new): 跳过已停止的候选: $candidate")
    continue
}
```

### 2. 实现停止应用检测方法

```kotlin
private fun getRecentStoppedApps(now: Long): Set<String> {
    val stoppedApps = mutableSetOf<String>()
    try {
        val usm = applicationContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val lookback = now - 60_000 // 回看60秒内的停止事件
        
        val usageEvents = usm.queryEvents(lookback, now)
        while (usageEvents.hasNextEvent()) {
            val event = UsageEvents.Event()
            usageEvents.getNextEvent(event)
            
            // 检查停止事件 (PAUSED, type 2, 20, 23)
            if (event.eventType == UsageEvents.Event.ACTIVITY_PAUSED || 
                event.eventType == 2 || event.eventType == 20 || event.eventType == 23) {
                event.packageName?.let { pkg ->
                    stoppedApps.add(pkg)
                    Log.v(TAG, "记录停止应用: $pkg at ${event.timeStamp}")
                }
            }
        }
    } catch (e: Exception) {
        Log.w(TAG, "获取停止应用列表失败", e)
    }
    return stoppedApps
}
```

## 📊 修复效果

### 修复前的错误流程
1. Chrome在01:15:28停止 → 收到停止事件
2. `getForegroundApp()` 检查 → Chrome的 `lastTimeVisible` 仍然很新
3. 误判Chrome仍在前台 → 开始新的Chrome会话
4. 导致桌面时间被错误统计为Chrome使用时间

### 修复后的正确流程
1. Chrome在01:15:28停止 → 收到停止事件并记录
2. `getForegroundApp()` 检查 → 发现Chrome在停止列表中
3. 跳过Chrome候选 → 继续检查其他应用或返回null
4. 正确识别桌面状态 → 不会错误统计使用时间

## 🎉 修复亮点

1. **事件与状态双重验证**: 结合 `UsageEvents` 和 `UsageStats` 的优势
2. **实时停止检测**: 60秒内的停止事件都会被考虑
3. **防止误判**: 确保已停止的应用不会被误认为前台
4. **提高准确性**: 桌面切换能被正确识别

## 🧪 测试场景

1. **应用切换**: 应用A → 桌面 → 应用B
2. **长时间桌面**: 应用 → 桌面停留 → 检查是否误统计
3. **快速切换**: 连续切换多个应用
4. **后台返回**: 应用最小化后重新打开

这次修复应该彻底解决已停止应用被误认为前台的问题！
