#!/bin/bash
adb -s emulator-5554 logcat -v time -s UsageStatsCollector:V AppSessionRepository:V | tee ~/offtimes_focus.log

SERIAL=$(adb devices | awk 'NR>1 && $2=="device"{print $1; exit}'); if [ -z "$SERIAL" ]; then echo "No connected devices"; exit 1; fi; echo Using device: $SERIAL; adb -s $SERIAL install -r -d /home/shuifeng/OffTimes/app/build/outputs/apk/googleplay/release/app-googleplay-release.apk | cat