# OffTimes Android App Version 1.3.3 Update Notes

## Release Date: 2025-09-19

## 🚨 Critical Bug Fix

- **Fixed Persistent OffTimes Usage Recording Issue**:
  - **Root Cause**: The real-time usage tracking system in `AppSessionRepository.updateActiveSessionDuration()` was configured with a 0-second threshold specifically for OffTimes, causing ANY background activity (even millisecond-level tasks) to be recorded as valid usage sessions.
  - **Impact**: This led to continuous accumulation of OffTimes usage time even when the app was closed, resulting in inflated usage statistics (e.g., 10+ hours of usage when the app was actually closed).
  - **Fix**: Changed the real-time threshold for OffTimes from 0 seconds to 10 seconds, ensuring that only genuine user interactions (lasting 10+ seconds) are recorded as valid usage sessions.
  - **Code Change**: 
    ```kotlin
    // Before: val realtimeThreshold = if (pkgName.contains("offtime")) 0 else minValidDuration
    // After: val realtimeThreshold = if (pkgName.contains("offtime")) 10 else minValidDuration
    ```

## 🛠️ Technical Details

- **File Modified**: `app/src/main/java/com/offtime/app/data/repository/AppSessionRepository.kt`
- **Method**: `updateActiveSessionDuration()` - Line 519
- **Version**: Updated to v1.3.3 (versionCode: 25)

## 🧪 Testing Instructions

1. Install v1.3.3 on your device
2. Use the "🧹 清理重复记录" debug feature to clean existing erroneous OffTimes records
3. Close OffTimes completely and use other apps (Chrome, etc.)
4. Check after several hours - OffTimes usage should no longer accumulate when the app is closed

## 📋 Next Steps

- Test the fix by monitoring OffTimes usage statistics over the next few hours
- If historical erroneous data persists, use the debug cleanup feature in the app
- Monitor logcat to ensure background tasks no longer generate usage records

---
**Note**: This fix specifically targets the real-time usage tracking system that was incorrectly recording background task activities as foreground usage for OffTimes itself.
