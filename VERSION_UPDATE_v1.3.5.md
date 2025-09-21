# OffTimes Android App Version 1.3.5 Update Notes

## Release Date: 2025-09-19

## 🎯 Core Fix: Background Task Event Filtering

- **Root Cause Identified and Fixed**:
  - **Problem**: OffTimes' background periodic tasks (UnifiedUpdateService, DataAggregationService) were being mistakenly recorded as foreground usage, even when the user never opened the OffTimes interface.
  - **Impact**: This caused continuous accumulation of OffTimes usage time and premature termination of other apps' sessions (like Chrome).
  - **Solution**: Implemented a background task identification system that tags and filters out OffTimes events triggered by internal background operations.

## 🛠️ Technical Implementation

### Background Task Identification System
- **New Constants**: Added `EXTRA_BACKGROUND_TASK` flag to distinguish background tasks from user interactions
- **New Methods**: 
  - `triggerEventsPullWithBackgroundFlag()` - For background event pulling
  - `triggerActiveAppsUpdateWithBackgroundFlag()` - For background active app updates
- **State Tracking**: Added `isBackgroundTaskRunning` flag to track when background tasks are executing

### Event Filtering Logic
```kotlin
// Core fix in processUsageEvents()
if (eventPackageName.startsWith("com.offtime.app")) {
    // If currently executing background task, filter ALL OffTimes events
    if (isBackgroundTaskRunning) {
        Log.d(TAG, "🚫 后台任务过滤: 检测到OffTimes事件但当前为后台任务触发，直接忽略")
        continue // Filter out background task triggered OffTimes events
    }
    // Additional foreground verification for non-background events...
}
```

### Power Optimization
- **Restored 60-second update interval** for full data aggregation (previously reduced to 10 seconds)
- **Background tasks only run when needed**, not for every quick update
- **Reduced unnecessary event pulls** to save battery life

## 🔧 Files Modified

1. **UnifiedUpdateService.kt**:
   - Modified `performQuickActiveAppsUpdate()` to use background-flagged calls
   - Modified `collectRawUsageData()` and `updateBaseDataTables()` to use background-flagged calls
   - Restored power-efficient 60-second full update cycle

2. **UsageStatsCollectorService.kt**:
   - Added background task identification constants and methods
   - Added `isBackgroundTaskRunning` state tracking
   - Modified `onStartCommand()` to detect background task flags
   - Enhanced `processUsageEvents()` with background task filtering logic

## 🧪 Testing Instructions

1. Install v1.3.5 on your device
2. **Close OffTimes completely** and use Chrome for extended periods
3. Monitor logcat for:
   - "🚫 后台任务过滤" messages when background tasks run
   - Absence of OffTimes usage accumulation when app is closed
4. Verify Chrome sessions are no longer interrupted by OffTimes background tasks

## 📋 Expected Behavior

- **Before**: Background tasks triggered false OffTimes usage records
- **After**: Background tasks are tagged and filtered, only genuine user interactions are recorded
- **Result**: Accurate usage tracking with proper app session continuity

## 🔋 Power Efficiency

- Restored 60-second update intervals for better battery life
- Background task filtering reduces unnecessary processing
- Optimized event pulling frequency

---
**Note**: This version addresses the fundamental issue where OffTimes' internal maintenance tasks were being confused with actual user usage, providing a clean separation between background operations and genuine user interactions.
