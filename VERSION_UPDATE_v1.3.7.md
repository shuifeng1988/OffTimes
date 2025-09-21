# OffTimes Android App Version 1.3.7 Update Notes

## Release Date: 2025-09-19

## 🎯 Critical Fix: Real-time Statistics Filtering

- **Root Cause Identified**: Previous versions (v1.3.5, v1.3.6) successfully filtered OffTimes events during background task execution, but the **real-time statistics system** continued to accumulate OffTimes usage time even during background tasks.

- **Problem**: The state machine remained stuck on `com.offtime.app.gplay` as the active app, and `updateActiveAppsDuration()` continued to accumulate usage time every 30 seconds, even when marked as background tasks.

- **Impact**: Users experienced continuous OffTimes usage accumulation (e.g., 330+ seconds over 5+ minutes) and interruption of other apps' sessions, despite OffTimes interface being closed.

## 🛠️ Technical Fix

### Real-time Statistics Filtering
- **Enhanced `updateActiveAppsDuration()`**: Added background task detection to prevent OffTimes usage accumulation during background operations.
- **Key Logic**:
  ```kotlin
  // Critical fix: If background task is running and current app is OffTimes, don't accumulate usage time
  if (isBackgroundTaskRunning && packageName.startsWith("com.offtime.app")) {
      Log.d(TAG, "🚫 实时统计过滤: 后台任务期间不累积OffTimes使用时间")
      return
  }
  ```

### Complete Background Task Isolation
1. **Event Processing**: Background task events are filtered out (v1.3.5)
2. **Screen State Changes**: ScreenStateReceiver uses background task flags (v1.3.6)  
3. **Real-time Statistics**: Usage time accumulation is blocked during background tasks (v1.3.7)

## 🔍 Log Analysis Evidence

From the provided logcat:
- **13:27:17 - 13:31:14**: OffTimes accumulated 330 seconds (5.5 minutes) of usage time
- **Pattern**: "实时统计 → 当前活跃应用: com.offtime.app.gplay" every 30 seconds
- **Issue**: Despite "后台任务更新活跃应用" logs, real-time statistics continued

## 📋 Expected Behavior After v1.3.7

1. **Background Task Execution**: You should see "🚫 实时统计过滤: 后台任务期间不累积OffTimes使用时间" in logs
2. **No OffTimes Accumulation**: OffTimes usage time should not increase when the app is closed
3. **Uninterrupted Sessions**: Chrome, Maps, and other apps should maintain continuous sessions
4. **Clean State Machine**: Status should properly reflect actual foreground apps

## 🧪 Testing Instructions

1. **Install v1.3.7** and clear previous erroneous data using "🧹 清理重复记录"
2. **Close OffTimes completely** and use Chrome/Maps for 20+ minutes
3. **Monitor logs** for "🚫 实时统计过滤" messages during background tasks
4. **Verify sessions** remain uninterrupted and OffTimes shows no false usage

## 🔧 Files Modified

- **UsageStatsCollectorService.kt**: Enhanced `updateActiveAppsDuration()` with background task filtering
- **Version**: Updated to v1.3.7 (versionCode: 29)

---
**Note**: This version completes the background task isolation system by preventing real-time usage accumulation during background operations, ensuring accurate usage tracking that reflects only genuine user interactions.
