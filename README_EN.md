# OffTimes - Time Management Application

## Overview

OffTimes is a professional time management and app usage monitoring application designed to help users better manage their digital life and improve productivity.

## Key Features

- 📱 **App Usage Monitoring** - Real-time tracking of application usage
- ⏰ **Offline Timer** - Focus time management tool
- 📊 **Data Analytics** - Detailed usage statistics and reports
- 🎯 **Goal Setting** - Set usage goals with reward/punishment system
- 🔄 **Data Sync** - Cloud backup and synchronization support

## Technical Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM + Repository Pattern
- **Dependency Injection**: Hilt
- **Database**: Room
- **Network**: Retrofit + OkHttp
- **Async Processing**: Coroutines + Flow

## Build Variants

### Alipay Version (com.offtime.app)
- SMS Login, Password Login
- Alipay Payment Integration
- Target: Chinese Mainland Users

### Google Play Version (com.offtime.app.gplay)
- Google Sign-In
- Google Play Billing
- SMS Verification
- Target: International Users

## Quick Start

1. **Clone the repository**
```bash
git clone <repository-url>
cd OffTimes
```

2. **Configure gradle.properties**
```properties
GOOGLE_WEB_CLIENT_ID=your_google_client_id
GOOGLE_PLAY_LICENSE_KEY=your_license_key
ALIPAY_APP_ID=your_alipay_app_id
```

3. **Build the project**
```bash
./gradlew assembleRelease
```

## Version Information

- **Version**: 1.1.0 (Build 20)
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 35 (Android 15)

## License

Private License. All rights reserved.

---

**Last Updated**: September 15, 2025
