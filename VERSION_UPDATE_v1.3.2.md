# OffTimes v1.3.2 版本更新

## 📅 发布信息
- **版本号**: v1.3.2 (versionCode: 24)
- **发布日期**: 2025-09-19
- **构建类型**: Release

## 🔧 关键修复

### 1. 数据库安全性增强
- **移除破坏性迁移**: 删除 `fallbackToDestructiveMigration()`，防止应用升级时清空用户数据
- **数据保护**: 确保用户的历史使用数据在应用更新时不会丢失

### 2. 使用时间统计修复
- **修复虚假使用时间**: 解决显示错误的OffTimes使用时间问题
- **活跃会话计算优化**: 重构 `getCurrentActiveUsageByCategory` 方法，移除错误的活跃时间推算逻辑
- **会话中断修复**: 修复Chrome等应用长时间使用被OffTimes后台任务意外中断的问题

### 3. 数据管理策略优化
- **数据清理策略**: 恢复原始会话表(app_sessions_user, timer_sessions_user)的60天清理机制
- **永久保存策略**: 聚合表和配置表数据永久保存，支持长期数据分析
- **自动清理调度**: 在UnifiedUpdateService中集成数据清理检查，每天自动执行一次

### 4. 后台应用过滤增强
- **TIM应用支持**: 将com.tencent.tim添加到后台应用黑名单
- **过滤准确性**: 提升TIM、QQ、微信、支付宝等常驻后台应用的时间统计准确性

### 5. 构建系统修复
- **Hilt兼容性**: 修复BroadcastReceiver在Release构建中的Hilt依赖注入问题
- **ProGuard规则**: 添加必要的代码保护规则，确保Release版本正常运行
- **EntryPoint重构**: 使用EntryPointAccessors替代@AndroidEntryPoint，解决R8混淆冲突

## 🛠️ 技术细节

### 修改的核心文件
- `OffTimeDatabase.kt` - 移除破坏性迁移
- `AppSessionRepository.kt` - 修复活跃会话计算
- `DataCleanupManager.kt` - 恢复数据清理策略
- `UnifiedUpdateService.kt` - 集成数据清理调度
- `UsageStatsCollectorService.kt` - 优化使用统计收集
- `BackgroundAppFilterUtils.kt` - 增强后台应用过滤
- `AppInstallReceiver.kt` & `ScreenStateReceiver.kt` - Hilt重构
- `proguard-rules.pro` - 添加保护规则

### 构建验证
- ✅ Debug版本构建成功
- ✅ Release版本构建成功  
- ✅ APK和AAB文件生成正常
- ✅ ProGuard混淆无错误

## 📊 影响评估

### 用户体验改善
- 🔧 修复了使用时间统计不准确的问题
- 🔧 消除了应用切换时的异常中断
- 🔧 提升了数据统计的可靠性
- 🔧 保护了用户的历史数据

### 系统稳定性
- 🛡️ 数据库升级更安全
- 🛡️ Release版本构建稳定
- 🛡️ 后台服务运行更可靠
- 🛡️ 内存和存储使用优化

## 🔄 升级注意事项

### 兼容性
- ✅ 向下兼容现有数据
- ✅ 不影响用户设置和偏好
- ✅ 支持从v1.3.1直接升级

### 数据迁移
- 现有聚合数据保持不变
- 会话数据按新策略管理
- 配置数据完整保留

## 🧪 测试建议

### 关键测试场景
1. **使用时间统计准确性**
   - 长时间使用Chrome等浏览器
   - 验证OffTimes不会显示虚假使用时间
   - 确认会话不会被意外中断

2. **后台应用过滤**
   - 测试TIM、QQ、微信、支付宝的后台时间过滤
   - 验证熄屏后的使用时间统计

3. **数据持久性**
   - 应用升级后检查历史数据
   - 验证聚合表数据完整性

4. **系统稳定性**
   - 长期运行稳定性测试
   - 内存使用监控
   - 后台服务运行状态

---

**开发团队**: OffTimes Development Team  
**技术支持**: developer@offtimes.app
