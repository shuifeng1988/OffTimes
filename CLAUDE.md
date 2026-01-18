# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

OffTimes is a time management and app usage monitoring Android application with dual-market support:
- **Alipay version** (`com.offtime.app`) - Chinese market with SMS/Password login + Alipay payments
- **Google Play version** (`com.offtime.app.gplay`) - International market with Google Sign-In + Google Play Billing

## Build Commands

```bash
# Build APKs
./gradlew assembleAlipayDebug        # Alipay debug APK
./gradlew assembleAlipayRelease      # Alipay release APK
./gradlew assembleGoogleplayDebug    # Google Play debug APK
./gradlew assembleGoogleplayRelease  # Google Play release APK

# Build Android App Bundles (for store upload)
./gradlew bundleAlipayRelease
./gradlew bundleGoogleplayRelease

# Install to connected device
./gradlew installAlipayDebug
./gradlew installGoogleplayDebug

# Run tests
./gradlew testDebugUnitTest                 # Unit tests
./gradlew testAlipayDebugUnitTest           # Alipay flavor unit tests
./gradlew testGoogleplayDebugUnitTest       # Google Play flavor unit tests
./gradlew connectedAndroidTest              # Instrumented tests

# Code analysis
./gradlew lint

# Clean
./gradlew clean
```

## Architecture

**Tech Stack:** Kotlin, Jetpack Compose, Hilt DI, Room Database (v30), Retrofit, Coroutines + Flow

**Pattern:** MVVM + Repository

### Data Flow
```
System UsageStats → AppSessionUserEntity → DailyUsageUserEntity → SummaryUsageUserEntity
```

### Key Directories
- `app/src/main/java/com/offtime/app/`
  - `data/` - Room entities, DAOs, repositories, network APIs
  - `service/` - 4 foreground services for background monitoring
  - `ui/screens/` - Compose screens
  - `ui/viewmodel/` - ViewModels (HomeViewModel is largest at 283KB)
  - `manager/` - Business logic managers (subscription, backup, reminders)
  - `utils/` - Utility classes (24 files)
  - `di/` - Hilt modules (DatabaseModule, NetworkModule)

### Critical Background Services
1. **UsageStatsCollectorService** - Continuous app monitoring, foreground detection
2. **DataAggregationService** - Periodic data aggregation to daily/summary tables
3. **UnifiedUpdateService** - Scheduled background updates with AlarmManager fallback
4. **OfflineTimerService** - Focus timer tracking

### Database Entities (Core)
- `AppSessionUserEntity` - Individual app usage sessions
- `DailyUsageUserEntity` - Daily aggregated usage
- `SummaryUsageUserEntity` - Time-based summaries (day/week/month)
- `AppInfoEntity` - Installed apps metadata
- `GoalRewardPunishmentEntity` - User goals with reward/punishment system

### Flavor-Specific Code
- `app/src/main/alipay/` - Alipay SDK integration, SMS login
- `app/src/main/googleplay/` - Google Sign-In, Play Billing

## Configuration

### gradle.properties keys
```properties
GOOGLE_WEB_CLIENT_ID       # Google OAuth client ID
GOOGLE_PLAY_LICENSE_KEY    # Play Billing license key
ALIPAY_APP_ID              # Alipay application ID
ALIPAY_MERCHANT_PRIVATE_KEY
ALIPAY_PUBLIC_KEY
```

### Signing
Release builds use `app-release-key.jks` in project root with alias `offtime-key`.

## Important Notes

- **API Base URL:** `http://60.205.145.35:8080/api/`
- **Min SDK:** 24 (Android 7.0) / **Target SDK:** 35 (Android 15)
- **Current Version:** v1.4.13 (versionCode: 38)
- Firebase has been removed to avoid foreground service conflicts
- 16 KB page size alignment enabled for Android 15+ compatibility
- ProGuard minification enabled for release builds with preserved service classes and Room entities

## Language

The codebase uses bilingual comments (Chinese/English). Maintain this convention when adding new code or comments.
