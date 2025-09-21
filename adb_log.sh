#!/bin/bash
adb -s emulator-5554 logcat -v time -s UsageStatsCollector:V AppSessionRepository:V | tee ~/offtimes_focus.log
