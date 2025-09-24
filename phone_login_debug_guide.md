# OffTimes 支付宝版本手机号登录调试指南

## 🎯 调试脚本概览

### 📱 支持设备选择的调试脚本

所有脚本现在都支持指定设备参数，可以精确选择要调试的设备：

1. **增强版调试脚本** (推荐) - `debug_phone_login_enhanced.sh`
2. **基础调试脚本** - `debug_phone_login.sh`
3. **目标调试脚本** - `debug_phone_login_targeted.sh`

## 🚀 使用方法

### 基本语法
```bash
./debug_phone_login_enhanced.sh [device_id]
```

### 示例用法

#### 1. 指定模拟器设备
```bash
./debug_phone_login_enhanced.sh emulator-5558
```

#### 2. 指定真实设备
```bash
./debug_phone_login_enhanced.sh ABC123DEF456
```

#### 3. 自动选择设备
```bash
# 单设备时自动选择，多设备时会提示选择
./debug_phone_login_enhanced.sh
```

## 💡 智能设备检测功能

### ✅ 自动设备验证
- 检查指定设备是否存在
- 验证设备连接状态
- 多设备时强制要求指定设备

### 📋 友好的错误提示
```bash
❌ 设备 'emulator-9999' 未找到或未连接
💡 可用设备列表:
   emulator-5558
   emulator-5560
```

### 🔍 设备选择提示
```bash
⚠️ 检测到多个设备，请指定目标设备:
使用方法: ./debug_phone_login_enhanced.sh <device_id>
可用设备:
   emulator-5558
   emulator-5560

示例: ./debug_phone_login_enhanced.sh emulator-5558
```

## 📊 日志文件管理

### 自动命名规则
- **指定设备**: `~/phone_login_enhanced_debug_emulator-5558.log`
- **单设备**: `~/phone_login_enhanced_debug.log`

### 生成的日志文件
| 脚本类型 | 主日志文件 | 网络日志 | 完整日志 |
|---------|-----------|---------|---------|
| 增强版 | `phone_login_enhanced_debug_[device].log` | `phone_login_network_debug_[device].log` | `phone_login_full_enhanced_debug_[device].log` |
| 基础版 | `phone_login_debug_[device].log` | - | `phone_login_full_debug_[device].log` |
| 目标版 | `phone_login_targeted_debug_[device].log` | - | - |

## 🔧 调试步骤

### 1. 运行调试脚本
```bash
./debug_phone_login_enhanced.sh emulator-5558
```

### 2. 在应用中进行登录操作
1. 打开OffTimes支付宝版本
2. 进入登录界面
3. 选择「手机号登录」
4. 输入测试手机号：`13800138000`
5. 点击「发送验证码」
6. 输入任意6位验证码：`123456`
7. 点击「登录」按钮

### 3. 查看实时日志输出
脚本会实时显示彩色分类的日志：
- 🔐 LOGIN: 登录相关
- 📞 PHONE: 手机号相关
- 📱 SMS: 验证码相关
- 🌐 NETWORK: 网络请求
- ❌ ERROR: 错误信息
- ✅ SUCCESS: 成功信息

## 🎯 监控的关键组件

### 核心组件
- **LoginViewModel** - 登录界面状态管理
- **UserRepository** - 用户数据和API调用
- **LoginApiService** - 登录网络服务
- **AlipayLoginManager** - 支付宝登录管理

### 关键流程
1. 手机号输入和验证
2. 验证码发送请求
3. 验证码输入和验证
4. 登录请求和响应
5. Token生成和保存
6. 登录状态更新

## 🔍 问题排查建议

### 常见问题检查点
1. **手机号验证失败** - 检查正则表达式匹配
2. **验证码发送失败** - 查看网络请求日志
3. **验证码验证失败** - 检查验证逻辑
4. **登录请求失败** - 查看API调用和响应
5. **Token保存失败** - 检查本地存储逻辑

### 日志分析技巧
- 关注ERROR级别的日志
- 查看网络请求的响应码
- 检查验证码的生成和验证过程
- 观察登录状态的变化

## 🚨 注意事项

1. **多设备环境** - 必须指定设备ID
2. **权限要求** - 确保脚本有执行权限
3. **USB调试** - 确保设备开启USB调试
4. **网络连接** - 确保设备网络正常
5. **应用版本** - 确保是支付宝版本的OffTimes

## 📞 技术支持

如果遇到问题，请提供以下信息：
- 使用的脚本版本
- 设备ID和类型
- 完整的错误日志
- 操作步骤描述
