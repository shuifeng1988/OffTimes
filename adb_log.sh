#!/bin/bash
adb -s emulator-5558 logcat -v time -s UsageStatsCollector:V AppSessionRepository:V | tee ~/offtimes_focus.pixel8.log

SERIAL=$(adb devices | awk 'NR>1 && $2=="device"{print $1; exit}'); if [ -z "$SERIAL" ]; then echo "No connected devices"; exit 1; fi; echo Using device: $SERIAL; adb -s $SERIAL install -r -d /home/shuifeng/OffTimes/app/build/outputs/apk/googleplay/release/app-googleplay-release.apk | cat

adb -s emulator-5554 logcat -v time -s UsageStatsCollector:V AppSessionRepository:V UnifiedUpdateService:V DataUpdateManager:V HomeViewModel:V OffTimeApplication:V DataAggregationService:V DataCleanupManager:V | tee ~/offtimes_complete.log
“”“
这个命令会捕捉完整的30秒更新流程：
启动阶段应该看到：
OffTimeApplication: 数据收集服务启动成功
OffTimeApplication: 统一更新服务启动成功
UnifiedUpdateService: 统一更新服务已创建
UnifiedUpdateService: 启动定时更新机制 - 每30秒完整更新，每10秒快速更新
每30秒应该看到：
UnifiedUpdateService: 开始执行统一更新流程 - periodic
UsageStatsCollector: 触发事件拉取
DataAggregationService: 开始聚合数据
DataUpdateManager: 发送数据更新事件: periodic
HomeViewModel: 收到数据更新事件: periodic
HomeViewModel: 🔄 响应数据更新事件，刷新分类
每10秒应该看到：
UnifiedUpdateService: 执行快速活跃应用更新
DataUpdateManager: 发送数据更新事件: QUICK_UPDATE
如果缺少UnifiedUpdateService的启动日志，说明启动失败；如果有启动但没有周期性日志，说明定时循环有问题。
”“”