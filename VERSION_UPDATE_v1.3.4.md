# OffTimes Android App Version 1.3.4 Update Notes

## Release Date: 2025-09-19

## 🚨 Critical Fix: App Switching Detection

- **Fixed App Switching Detection Delay Issue**:
  - **Root Cause**: The `UnifiedUpdateService` was using a 60-second interval for event pulling, meaning app switches (like OffTimes → Chrome) could take up to 60 seconds to be detected. This caused the state machine to remain "stuck" on the previous app.
  - **Impact**: When users switched from OffTimes to Chrome, the system continued to record OffTimes usage for up to 60 seconds until the next event pull cycle, leading to false usage records.
  - **Fix**: Modified `performQuickActiveAppsUpdate()` to also pull events every 10 seconds, ensuring app switches are detected within 10 seconds instead of 60 seconds.
  - **Code Change**:
    ```kotlin
    // Before: Quick updates only updated active app duration, no event pulling
    // After: Quick updates now include event pulling for timely app switch detection
    ```

## 🛠️ Technical Details

- **File Modified**: `app/src/main/java/com/offtime/app/service/UnifiedUpdateService.kt`
- **Method**: `performQuickActiveAppsUpdate()` - Lines 237-257
- **Update Intervals**:
  - **Complete Update**: Every 60 seconds (unchanged)
  - **Quick Update with Event Pull**: Every 10 seconds (NEW - now includes event pulling)
- **Version**: Updated to v1.3.4 (versionCode: 26)

## 🔧 How It Works Now

1. **Every 10 seconds**: Pull latest events + update active apps (Quick Update)
2. **Every 60 seconds**: Full data aggregation + UI refresh (Complete Update)
3. **Result**: App switches are detected within 10 seconds maximum

## 🧪 Testing Instructions

1. Install v1.3.4 on your device
2. Open OffTimes, then switch to Chrome
3. Check logcat - you should see Chrome startup events being detected within 10 seconds
4. Verify that OffTimes usage stops accumulating after switching to Chrome

## 📋 Expected Behavior

- **Before**: App switches took up to 60 seconds to detect
- **After**: App switches detected within 10 seconds
- **Result**: Accurate usage tracking with minimal delay

---
**Note**: This fix addresses the state machine "stuck" issue where the service continued to record the previous app's usage after switching to a new app.
