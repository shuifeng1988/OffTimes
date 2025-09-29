package com.offtime.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.offtime.app.utils.UsageStatsPermissionHelper
import com.offtime.app.utils.PermissionUtils
import com.offtime.app.utils.FirstLaunchManager
import com.offtime.app.data.repository.UserRepository
import com.offtime.app.data.entity.UserEntity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import com.offtime.app.ui.theme.LocalResponsiveDimensions
import com.offtime.app.ui.viewmodel.SettingsViewModel
import com.offtime.app.manager.SubscriptionManager
import com.offtime.app.R
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToGoalRewardPunishment: () -> Unit = {},
    onNavigateToOfflineTimer: () -> Unit = {},
    onNavigateToUsagePermission: () -> Unit = {},
    onNavigateToTaskReminder: () -> Unit = {},
    onNavigateToWidgetSettings: () -> Unit = {},
    onNavigateToDebug: () -> Unit = {},
    onNavigateToOnboarding: () -> Unit = {},
    onNavigateToPayment: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    onNavigateToLanguageSettings: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    
    // 获取状态
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val subscriptionInfo by viewModel.subscriptionInfo.collectAsStateWithLifecycle()
    
    // 获取屏幕配置信息
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    
    // 根据屏幕尺寸计算自适应参数
    val isSmallScreen = screenHeight < 700.dp
    val isLargeScreen = screenHeight > 900.dp
    
    // 自适应间距和尺寸
    val verticalSpacing = when {
        isSmallScreen -> 12.dp
        isLargeScreen -> 20.dp
        else -> 16.dp
    }
    
    val horizontalPadding = when {
        screenWidth < 360.dp -> 12.dp
        screenWidth > 480.dp -> 20.dp
        else -> 16.dp
    }
    

    var hasUsagePermission by remember { mutableStateOf(false) }
    var hasQueryAllPackagesPermission by remember { mutableStateOf(false) }
    var hasNotificationPermission by remember { mutableStateOf(false) }
    var hasAutoStartPermission by remember { mutableStateOf(false) }
    var hasBatteryOptimizationPermission by remember { mutableStateOf(false) }
    var hasVibratePermission by remember { mutableStateOf(false) }
    var showUsagePermissionDialog by remember { mutableStateOf(false) }
    var showQueryAllPackagesDialog by remember { mutableStateOf(false) }
    var showNotificationPermissionDialog by remember { mutableStateOf(false) }
    var showAutoStartDialog by remember { mutableStateOf(false) }
    var showBatteryOptimizationDialog by remember { mutableStateOf(false) }
    var showVibratePermissionDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    
    // 通知权限请求
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }
    
    // 检查权限状态
    LaunchedEffect(Unit) {
        // 初始检查
        hasUsagePermission = UsageStatsPermissionHelper.hasUsageStatsPermission(context)
        hasQueryAllPackagesPermission = PermissionUtils.hasQueryAllPackagesPermission(context)
        
        // 检查通知权限
        hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Android 13以下默认有通知权限
        }
        
        // 检查开机自启动权限
        hasAutoStartPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECEIVE_BOOT_COMPLETED
        ) == PackageManager.PERMISSION_GRANTED
        
        // 检查后台运行权限
        hasBatteryOptimizationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.FOREGROUND_SERVICE
        ) == PackageManager.PERMISSION_GRANTED
        
        // 检查震动权限
        hasVibratePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.VIBRATE
        ) == PackageManager.PERMISSION_GRANTED
        
        // 定期检查权限状态，当用户从设置页面返回时能及时更新
        while (true) {
            kotlinx.coroutines.delay(2000) // 每2秒检查一次
            val currentUsagePermission = UsageStatsPermissionHelper.hasUsageStatsPermission(context)
            val currentQueryPackagesPermission = PermissionUtils.hasQueryAllPackagesPermission(context)
            val currentNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
            val currentVibratePermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.VIBRATE
            ) == PackageManager.PERMISSION_GRANTED
            
            if (currentUsagePermission != hasUsagePermission) {
                hasUsagePermission = currentUsagePermission
            }
            if (currentQueryPackagesPermission != hasQueryAllPackagesPermission) {
                hasQueryAllPackagesPermission = currentQueryPackagesPermission
            }
            if (currentNotificationPermission != hasNotificationPermission) {
                hasNotificationPermission = currentNotificationPermission
            }
            if (currentVibratePermission != hasVibratePermission) {
                hasVibratePermission = currentVibratePermission
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontalPadding)
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            fontSize = 18.sp, // 标题使用18sp
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = verticalSpacing * 1.5f)
        )
        
        // 账户管理卡片
        AccountManagementCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = verticalSpacing),
            currentUser = currentUser,
            subscriptionInfo = subscriptionInfo,
            onLoginClick = onNavigateToLogin,
            onLogoutClick = { viewModel.logout() },
            onProfileClick = { /* 编辑个人信息 */ },
            onUpgradeClick = onNavigateToPayment
        )
        
        // 权限管理卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = verticalSpacing),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(verticalSpacing)
            ) {
                Text(
                    text = stringResource(R.string.settings_permission_management),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color.Black,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // 使用情况访问权限
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.permission_usage_stats),
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (hasUsagePermission) stringResource(R.string.permission_granted) else stringResource(R.string.permission_denied),
                            fontSize = 16.sp,
                            color = if (hasUsagePermission) 
                                MaterialTheme.colorScheme.primary else 
                                MaterialTheme.colorScheme.error
                        )
                    }
                    
                    Button(
                        onClick = {
                            if (hasUsagePermission) {
                                UsageStatsPermissionHelper.openUsageAccessSettings(context)
                            } else {
                                onNavigateToUsagePermission()
                            }
                        }
                    ) {
                        Text(
                            text = if (hasUsagePermission) stringResource(R.string.settings_action_manage) else stringResource(R.string.settings_action_enable),
                            fontSize = 16.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 读取手机应用列表权限
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.permission_query_packages),
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (hasQueryAllPackagesPermission) stringResource(R.string.permission_granted) else stringResource(R.string.permission_denied),
                            fontSize = 16.sp,
                            color = if (hasQueryAllPackagesPermission) 
                                MaterialTheme.colorScheme.primary else 
                                MaterialTheme.colorScheme.error
                        )
                    }
                    
                    Button(
                        onClick = {
                            // 使用智能检测跳转到最合适的读取手机应用列表权限设置页面
                            PermissionUtils.openQueryAllPackagesSettingsIntelligent(context)
                        }
                    ) {
                        Text(
                            text = if (hasQueryAllPackagesPermission) stringResource(R.string.settings_action_manage) else stringResource(R.string.settings_action_enable),
                            fontSize = 16.sp
                        )
                    }
                }
                
                // 通知权限 - 仅在Android 13+显示
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.permission_notification),
                                fontSize = 16.sp
                            )
                            Text(
                                text = if (hasNotificationPermission) stringResource(R.string.permission_granted) else stringResource(R.string.permission_denied),
                                fontSize = 16.sp,
                                color = if (hasNotificationPermission) 
                                    MaterialTheme.colorScheme.primary else 
                                    MaterialTheme.colorScheme.error
                            )
                        }
                        
                        Button(
                            onClick = {
                                // 使用智能检测跳转到最合适的通知权限设置页面
                                PermissionUtils.openNotificationSettingsIntelligent(context)
                            }
                        ) {
                            Text(
                                text = if (hasNotificationPermission) stringResource(R.string.settings_action_manage) else stringResource(R.string.settings_action_enable),
                                fontSize = 16.sp
                            )
                        }
                    }
                }
                
                // 开机自启动权限
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.permission_auto_start),
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (hasAutoStartPermission) stringResource(R.string.permission_granted) else stringResource(R.string.permission_denied),
                            fontSize = 16.sp,
                            color = if (hasAutoStartPermission) 
                                MaterialTheme.colorScheme.primary else 
                                MaterialTheme.colorScheme.error
                        )
                    }
                    
                    Button(
                        onClick = {
                            // 使用智能检测跳转到最合适的自启动设置页面
                            PermissionUtils.openAutoStartSettingsIntelligent(context)
                        }
                    ) {
                        Text(
                            text = if (hasAutoStartPermission) stringResource(R.string.settings_action_manage) else stringResource(R.string.settings_action_enable),
                            fontSize = 16.sp
                        )
                    }
                }
                
                // 后台运行权限
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.permission_background_run),
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (hasBatteryOptimizationPermission) stringResource(R.string.permission_granted) else stringResource(R.string.permission_denied),
                            fontSize = 16.sp,
                            color = if (hasBatteryOptimizationPermission) 
                                MaterialTheme.colorScheme.primary else 
                                MaterialTheme.colorScheme.error
                        )
                    }
                    
                    Button(
                        onClick = {
                            // 使用简化的智能检测跳转到最合适的后台运行权限设置页面
                            PermissionUtils.openBackgroundRunSettingsIntelligent(context)
                        }
                    ) {
                        Text(
                            text = if (hasBatteryOptimizationPermission) stringResource(R.string.settings_action_manage) else stringResource(R.string.settings_action_enable),
                            fontSize = 16.sp
                        )
                    }
                }
                
                // 震动权限
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.permission_vibrate),
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (hasVibratePermission) stringResource(R.string.permission_granted) else stringResource(R.string.permission_denied),
                            fontSize = 16.sp,
                            color = if (hasVibratePermission) 
                                MaterialTheme.colorScheme.primary else 
                                MaterialTheme.colorScheme.error
                        )
                    }
                    
                    Button(
                        onClick = {
                            if (hasVibratePermission) {
                                PermissionUtils.openAppDetailsSettings(context)
                            } else {
                                showVibratePermissionDialog = true
                            }
                        }
                    ) {
                        Text(
                            text = if (hasVibratePermission) stringResource(R.string.settings_action_manage) else stringResource(R.string.settings_action_enable),
                            fontSize = 16.sp
                        )
                    }
                }
                
            }
        }
        
        // 功能管理卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_feature_management),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color.Black,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // 目标与奖罚
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.goals_title_short),
                            fontSize = 16.sp
                        )
                        Text(
                            text = stringResource(R.string.goals_description),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Button(
                        onClick = onNavigateToGoalRewardPunishment
                    ) {
                        Text(
                            text = stringResource(R.string.settings_action_setup),
                            fontSize = 16.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 线下活动计时
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.offline_timer_title),
                            fontSize = 16.sp
                        )
                        Text(
                            text = stringResource(R.string.offline_timer_description),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Button(
                        onClick = onNavigateToOfflineTimer
                    ) {
                        Text(
                            text = stringResource(R.string.settings_action_enter),
                            fontSize = 16.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 任务提醒
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.task_reminder_title),
                            fontSize = 16.sp
                        )
                        Text(
                            text = stringResource(R.string.task_reminder_description),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Button(
                        onClick = onNavigateToTaskReminder
                    ) {
                        Text(
                            text = stringResource(R.string.settings_action_setup),
                            fontSize = 16.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 语言设置
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_language),
                            fontSize = 16.sp
                        )
                        Text(
                            text = stringResource(R.string.settings_language_description),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Button(
                        onClick = onNavigateToLanguageSettings
                    ) {
                        Text(
                            text = stringResource(R.string.settings_action_setup),
                            fontSize = 16.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 数据备份
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_data_backup),
                            fontSize = 16.sp
                        )
                        Text(
                            text = stringResource(R.string.settings_data_backup_description),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Button(
                        onClick = {
                            showBackupDialog = true
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.settings_action_manage),
                            fontSize = 16.sp
                        )
                    }
                }

                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Widget显示天数设置
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_widget_display),
                            fontSize = 16.sp
                        )
                        Text(
                            text = stringResource(R.string.settings_widget_display_description),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Button(
                        onClick = onNavigateToWidgetSettings
                    ) {
                        Text(
                            text = stringResource(R.string.settings_action_setup),
                            fontSize = 16.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 调试功能
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_database_debug),
                            fontSize = 16.sp
                        )
                        Text(
                            text = stringResource(R.string.settings_database_debug_description),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Button(
                        onClick = onNavigateToDebug
                    ) {
                        Text(
                            text = stringResource(R.string.settings_action_debug),
                            fontSize = 16.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 重置引导功能
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_reset_onboarding),
                            fontSize = 16.sp
                        )
                        Text(
                            text = stringResource(R.string.settings_reset_onboarding_description),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Button(
                        onClick = {
                            val firstLaunchManager = FirstLaunchManager(context)
                            firstLaunchManager.resetFirstLaunch()
                            onNavigateToOnboarding()
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.settings_action_reset),
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                // 检查更新 (仅支付宝版本)
                if (com.offtime.app.BuildConfig.FLAVOR == "alipay") {
                    var showUpdateDialog by remember { mutableStateOf(false) }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(com.offtime.app.R.string.check_update),
                                fontSize = 16.sp
                            )
                            Text(
                                text = stringResource(com.offtime.app.R.string.check_update_description),
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Button(
                            onClick = { showUpdateDialog = true }
                        ) {
                            Text(
                                text = stringResource(com.offtime.app.R.string.check_update),
                                fontSize = 16.sp
                            )
                        }
                    }
                    
                    // 更新对话框
                    if (showUpdateDialog) {
                        com.offtime.app.ui.screen.UpdateDialog(
                            onDismiss = { showUpdateDialog = false }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // 关于应用
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.about),
                            fontSize = 16.sp
                        )
                        Text(
                            text = stringResource(R.string.app_description),
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Button(
                        onClick = onNavigateToAbout
                    ) {
                        Text(
                            text = stringResource(R.string.about),
                            fontSize = 16.sp
                        )
                    }
                }

            }
        }
    }
    
    // 使用情况访问权限说明对话框
    if (showUsagePermissionDialog) {
        AlertDialog(
            onDismissRequest = { showUsagePermissionDialog = false },
            title = { Text("需要使用情况访问权限") },
            text = { 
                Text(
                    "为了统计各应用的使用时间，OffTime需要获取使用情况访问权限。" +
                    "\n\n该权限仅用于：\n" +
                    "• 统计应用使用时长\n" +
                    "• 帮助您管理手机使用习惯\n" +
                    "• 实现目标监督功能\n\n" +
                    "点击确定跳转到设置页面开启权限。"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUsagePermissionDialog = false
                        UsageStatsPermissionHelper.openUsageAccessSettings(context)
                    }
                ) {
                    Text(
                        text = stringResource(R.string.action_confirm),
                        fontSize = 16.sp
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showUsagePermissionDialog = false }
                ) {
                    Text(
                        text = stringResource(R.string.action_cancel),
                        fontSize = 16.sp
                    )
                }
            }
        )
    }
    
    // 读取手机应用列表权限说明对话框
    if (showQueryAllPackagesDialog) {
        AlertDialog(
            onDismissRequest = { showQueryAllPackagesDialog = false },
            title = { Text("需要读取手机应用列表权限") },
            text = { 
                Text(
                    "为了获取手机中安装的应用信息，OffTime需要获取读取应用列表权限。" +
                    "\n\n该权限仅用于：\n" +
                    "• 获取已安装应用的基本信息\n" +
                    "• 实现应用分类管理功能\n" +
                    "• 统计和展示应用使用数据\n\n" +
                    "请注意：此权限已在应用安装时自动获取，如显示未开启，可能是系统检测异常。" +
                    "\n\n点击确定跳转到应用列表权限设置页面。"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showQueryAllPackagesDialog = false
                        PermissionUtils.openQueryAllPackagesSettingsIntelligent(context)
                    }
                ) {
                    Text(
                        text = stringResource(R.string.action_confirm),
                        fontSize = 16.sp
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showQueryAllPackagesDialog = false }
                ) {
                    Text(
                        text = stringResource(R.string.action_cancel),
                        fontSize = 16.sp
                    )
                }
            }
        )
    }
    
    // 通知权限说明对话框
    if (showNotificationPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationPermissionDialog = false },
            title = { Text("需要通知权限") },
            text = { 
                Text(
                    "为了及时提醒您完成目标和奖罚任务，OffTime需要获取通知权限。" +
                    "\n\n该权限仅用于：\n" +
                    "• 发送目标完成度提醒\n" +
                    "• 发送奖罚任务提醒\n" +
                    "• 帮助您养成良好的时间管理习惯\n\n" +
                    "点击确定开启通知权限。"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNotificationPermissionDialog = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                ) {
                    Text(
                        text = stringResource(R.string.action_confirm),
                        fontSize = 16.sp
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showNotificationPermissionDialog = false }
                ) {
                    Text(
                        text = stringResource(R.string.action_cancel),
                        fontSize = 16.sp
                    )
                }
            }
        )
    }
    
    // 开机自启动权限说明对话框
    if (showAutoStartDialog) {
        AlertDialog(
            onDismissRequest = { showAutoStartDialog = false },
            title = { Text("需要开机自启动权限") },
            text = { 
                Text(
                    "为了在手机开机后自动启动数据统计服务，OffTime需要获取开机自启动权限。" +
                    "\n\n该权限仅用于：\n" +
                    "• 开机后自动启动应用服务\n" +
                    "• 确保使用时间统计的连续性\n" +
                    "• 保证小部件数据的实时更新\n\n" +
                    "请注意：此权限通常在应用安装时自动获取。如显示未开启，请在自启动管理中手动开启。" +
                    "\n\n点击确定跳转到开机自启动设置页面。"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAutoStartDialog = false
                        PermissionUtils.openAutoStartSettingsIntelligent(context)
                    }
                ) {
                    Text(
                        text = stringResource(R.string.action_confirm),
                        fontSize = 16.sp
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAutoStartDialog = false }
                ) {
                    Text(
                        text = stringResource(R.string.action_cancel),
                        fontSize = 16.sp
                    )
                }
            }
        )
    }
    
    // 后台运行权限说明对话框
    if (showBatteryOptimizationDialog) {
        AlertDialog(
            onDismissRequest = { showBatteryOptimizationDialog = false },
            title = { Text("需要后台运行权限") },
            text = { 
                Text(
                    "为了持续统计应用使用时间和更新小部件数据，OffTime需要获取后台运行权限。" +
                    "\n\n该权限仅用于：\n" +
                    "• 在后台持续收集使用时间数据\n" +
                    "• 实时更新锁屏小部件内容\n" +
                    "• 确保目标监督功能正常工作\n" +
                    "• 发送及时的提醒通知\n\n" +
                    "请注意：需要在电池优化设置中将OffTime设为\"不优化\"，以确保应用能够在后台正常运行。" +
                    "\n\n点击确定跳转到电池优化设置页面。"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBatteryOptimizationDialog = false
                        PermissionUtils.openBackgroundRunSettingsIntelligent(context)
                    }
                ) {
                    Text(
                        text = stringResource(R.string.action_confirm),
                        fontSize = 16.sp
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBatteryOptimizationDialog = false }
                ) {
                    Text(
                        text = stringResource(R.string.action_cancel),
                        fontSize = 16.sp
                    )
                }
            }
        )
    }
    
    // 震动权限说明对话框
    if (showVibratePermissionDialog) {
        AlertDialog(
            onDismissRequest = { showVibratePermissionDialog = false },
            title = { Text("需要震动权限") },
            text = { 
                Text(
                    "为了在定时器结束时提供震动提醒，OffTime需要获取震动权限。" +
                    "\n\n该权限仅用于：\n" +
                    "• 定时器结束时的震动提醒\n" +
                    "• 增强声音提醒的效果\n" +
                    "• 在静音模式下提供提醒功能\n\n" +
                    "请注意：此权限通常在应用安装时自动获取。如显示未开启，请在应用详情页面手动开启。" +
                    "\n\n点击确定跳转到应用详情页面检查权限状态。"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showVibratePermissionDialog = false
                        PermissionUtils.openAppDetailsSettings(context)
                    }
                ) {
                    Text(
                        text = stringResource(R.string.action_confirm),
                        fontSize = 16.sp
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showVibratePermissionDialog = false }
                ) {
                    Text(
                        text = stringResource(R.string.action_cancel),
                        fontSize = 16.sp
                    )
                }
            }
        )
    }
    
    // 数据备份对话框
    if (showBackupDialog) {
        BackupSettingsDialog(
            onDismiss = { showBackupDialog = false },
            viewModel = viewModel
        )
    }
}

/**
 * 数据备份设置对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSettingsDialog(
    onDismiss: () -> Unit,
    viewModel: SettingsViewModel
) {
    val backupSettings by viewModel.backupSettings.collectAsStateWithLifecycle()
    val backupInfo by viewModel.backupInfo.collectAsStateWithLifecycle()
    val isLoading by viewModel.backupLoading.collectAsStateWithLifecycle()
    
    var localBackupEnabled by remember(backupSettings) { mutableStateOf(backupSettings?.backupEnabled ?: true) }
    var selectedHour by remember(backupSettings) { mutableStateOf(backupSettings?.backupTimeHour ?: 2) }
    var selectedMinute by remember(backupSettings) { mutableStateOf(backupSettings?.backupTimeMinute ?: 0) }
    var showTimePicker by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.backup_settings_title),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 备份开关
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.backup_auto_backup),
                        fontSize = 16.sp
                    )
                    
                    Switch(
                        checked = localBackupEnabled,
                        onCheckedChange = { 
                            localBackupEnabled = it
                            backupSettings?.let { settings ->
                                viewModel.updateBackupSettings(settings.copy(backupEnabled = it))
                            }
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 备份时间
                if (localBackupEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.backup_time),
                            fontSize = 16.sp
                        )
                        
                        TextButton(
                            onClick = { showTimePicker = true }
                        ) {
                            Text(
                                text = "${String.format("%02d", selectedHour)}:${String.format("%02d", selectedMinute)}",
                                fontSize = 16.sp
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // 备份状态信息
                backupInfo?.let { info ->
                    Column {
                        Text(
                            text = stringResource(R.string.backup_status),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "${stringResource(R.string.backup_last_backup)}: ${info.lastBackupDate ?: stringResource(R.string.backup_not_backed_up)}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Text(
                            text = "${stringResource(R.string.backup_count)}: ${info.totalBackups}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Text(
                            text = "${stringResource(R.string.backup_data_size)}: ${formatFileSize(info.totalDataSize)}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } ?: run {
                    if (isLoading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.backup_loading_info),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 立即备份和恢复按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.performManualBackup()
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading && localBackupEnabled
                    ) {
                        Text(stringResource(R.string.backup_manual_backup), fontSize = 14.sp)
                    }
                    
                    OutlinedButton(
                        onClick = {
                            viewModel.restoreBackupData()
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    ) {
                        Text(stringResource(R.string.backup_restore_data), fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    text = stringResource(R.string.backup_done),
                    fontSize = 16.sp
                )
            }
        }
    )
    
    // 时间选择对话框
    if (showTimePicker) {
        TimePickerDialog(
            hour = selectedHour,
            minute = selectedMinute,
            onTimeSelected = { hour, minute ->
                selectedHour = hour
                selectedMinute = minute
                backupSettings?.let { settings ->
                    viewModel.updateBackupSettings(
                        settings.copy(
                            backupTimeHour = hour,
                            backupTimeMinute = minute
                        )
                    )
                }
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

/**
 * 时间选择对话框
 */
@Composable
fun TimePickerDialog(
    hour: Int,
    minute: Int,
    onTimeSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedHour by remember { mutableStateOf(hour) }
    var selectedMinute by remember { mutableStateOf(minute) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.backup_select_time),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 小时选择
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("小时", fontSize = 14.sp)
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { 
                                selectedHour = if (selectedHour > 0) selectedHour - 1 else 23
                            }
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "减少小时")
                        }
                    }
                    
                    Text(
                        text = String.format("%02d", selectedHour),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { 
                                selectedHour = if (selectedHour < 23) selectedHour + 1 else 0
                            }
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "增加小时")
                        }
                    }
                }
                
                Text(
                    text = ":",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                // 分钟选择
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("分钟", fontSize = 14.sp)
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { 
                                selectedMinute = if (selectedMinute > 0) selectedMinute - 1 else 59
                            }
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "减少分钟")
                        }
                    }
                    
                    Text(
                        text = String.format("%02d", selectedMinute),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { 
                                selectedMinute = if (selectedMinute < 59) selectedMinute + 1 else 0
                            }
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "增加分钟")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onTimeSelected(selectedHour, selectedMinute)
                }
            ) {
                Text(
                    text = "确定",
                    fontSize = 16.sp
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    text = stringResource(R.string.action_cancel),
                    fontSize = 16.sp
                )
            }
        }
    )
}

/**
 * 格式化文件大小
 */
fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "${bytes}B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1fKB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.1fMB", mb)
    val gb = mb / 1024.0
    return String.format("%.1fGB", gb)
}

/**
 * 账户管理卡片组件
 * 显示当前登录状态、试用期信息和付费提示
 */
@Composable
fun AccountManagementCard(
    modifier: Modifier = Modifier,
    currentUser: UserEntity?,
    subscriptionInfo: SubscriptionManager.SubscriptionInfo?,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onProfileClick: () -> Unit,
    onUpgradeClick: () -> Unit
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.account_management_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // 试用期状态显示（无论是否登录都显示）
            subscriptionInfo?.let { info ->
                TrialStatusSection(
                    subscriptionInfo = info,
                    onUpgradeClick = onUpgradeClick
                )
                
                if (currentUser != null || info.isInTrial) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            
            if (currentUser != null && currentUser.isLoggedIn) {
                // 已登录状态
                AccountLoggedInContent(
                    user = currentUser,
                    onLogoutClick = onLogoutClick,
                    onProfileClick = onProfileClick
                )
            } else {
                // 未登录状态
                AccountNotLoggedInContent(
                    onLoginClick = onLoginClick
                )
            }
        }
    }
}

/**
 * 已登录状态的账户信息显示
 */
@Composable
private fun AccountLoggedInContent(
    user: UserEntity,
    onLogoutClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // 用户头像
            Card(
                modifier = Modifier.size(50.dp),
                shape = CircleShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (user.avatar.isNotEmpty()) {
                        // 如果有头像URL，这里可以使用图片加载库
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "用户头像",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        // 默认头像
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "默认头像",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 用户信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (user.nickname.isNotEmpty()) user.nickname else stringResource(R.string.account_nickname_not_set),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = user.phoneNumber,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.account_data_sync_status, if (user.isDataSyncEnabled) stringResource(R.string.account_data_sync_enabled) else stringResource(R.string.account_data_sync_disabled)),
                    fontSize = 16.sp,
                    color = if (user.isDataSyncEnabled) 
                        MaterialTheme.colorScheme.primary else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // 操作按钮
        Column(
            horizontalAlignment = Alignment.End
        ) {
            TextButton(
                onClick = onProfileClick
            ) {
                Text(
                    text = stringResource(R.string.account_edit_profile),
                    fontSize = 16.sp
                )
            }
            
            TextButton(
                onClick = onLogoutClick,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(
                    text = stringResource(R.string.account_logout),
                    fontSize = 16.sp
                )
            }
        }
    }
}

/**
 * 未登录状态的登录引导
 */
@Composable
private fun AccountNotLoggedInContent(
    onLoginClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // 默认头像
            Card(
                modifier = Modifier.size(50.dp),
                shape = CircleShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonOutline,
                        contentDescription = stringResource(R.string.account_not_logged_in),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 登录提示信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(R.string.account_not_logged_in),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.account_login_to_sync),
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.account_multi_device_sync),
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // 登录按钮
        Button(
            onClick = onLoginClick,
            modifier = Modifier.wrapContentWidth()
        ) {
            Text(
                text = stringResource(R.string.account_login_register),
                fontSize = 16.sp
            )
        }
    }
}

/**
 * 试用期状态显示组件
 */
@Composable
private fun TrialStatusSection(
    subscriptionInfo: SubscriptionManager.SubscriptionInfo,
    onUpgradeClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                when {
                    subscriptionInfo.isPremium -> {
                        Text(
                            text = stringResource(R.string.account_premium_version),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.account_enjoy_full_features),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    subscriptionInfo.isInTrial -> {
                        Text(
                            text = stringResource(R.string.account_free_trial),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.account_trial_days_remaining, subscriptionInfo.trialDaysRemaining),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {
                        Text(
                            text = stringResource(R.string.account_trial_expired_title),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = stringResource(R.string.account_trial_expired_description),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            // 升级按钮
            if (!subscriptionInfo.isPremium) {
                Button(
                    onClick = onUpgradeClick,
                    colors = if (subscriptionInfo.isInTrial) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    }
                ) {
                    Text(
                        text = if (subscriptionInfo.isInTrial) stringResource(R.string.account_upgrade_to_premium) else stringResource(R.string.account_pay_now),
                        fontSize = 14.sp
                    )
                }
            }
        }
        
        // 试用期内显示付费提示
        if (subscriptionInfo.isInTrial && !subscriptionInfo.isPremium) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.account_upgrade_reminder),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.account_upgrade_reminder_text),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}