package com.offtime.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.offtime.app.ui.viewmodel.OnboardingViewModel
import com.offtime.app.utils.UsageStatsPermissionHelper
import com.offtime.app.utils.PermissionUtils
import androidx.compose.ui.res.stringResource
import com.offtime.app.R
// import com.offtime.app.ui.theme.LocalResponsiveDimensions

/**
 * 首次启动引导页面
 * 
 * 功能包括：
 * 1. 欢迎介绍
 * 2. 权限申请引导
 * 3. 应用数据自动初始化
 * 4. 完成设置
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onCompleted: () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    // 使用固定尺寸代替响应式尺寸
    // val dimensions = LocalResponsiveDimensions
    
    // UI状态
    val currentStep by viewModel.currentStep.collectAsState()
    val isInitializing by viewModel.isInitializing.collectAsState()
    val initProgress by viewModel.initProgress.collectAsState()
    val initStatus by viewModel.initStatus.collectAsState()
    
    // 权限状态
    var hasUsagePermission by remember { mutableStateOf(false) }
    var hasQueryAllPackagesPermission by remember { mutableStateOf(false) }
    var hasNotificationPermission by remember { mutableStateOf(false) }
    var hasVibratePermission by remember { mutableStateOf(false) }
    var hasBatteryOptimizationPermission by remember { mutableStateOf(false) }
    
    // 权限请求启动器
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            viewModel.nextStep()
        }
    }
    
    // 检查权限状态
    LaunchedEffect(Unit) {
        while (true) {
            hasUsagePermission = UsageStatsPermissionHelper.hasUsageStatsPermission(context)
            hasQueryAllPackagesPermission = PermissionUtils.hasQueryAllPackagesPermission(context)
            hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
            hasVibratePermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.VIBRATE
            ) == PackageManager.PERMISSION_GRANTED
            hasBatteryOptimizationPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.FOREGROUND_SERVICE
            ) == PackageManager.PERMISSION_GRANTED
            
            // 如果当前步骤需要的权限已获得，自动进入下一步
            when (currentStep) {
                OnboardingStep.USAGE_PERMISSION -> {
                    if (hasUsagePermission) {
                        viewModel.nextStep()
                    }
                }
                OnboardingStep.QUERY_PACKAGES_PERMISSION -> {
                    // 对于Android 11以下版本，自动跳过QUERY_ALL_PACKAGES权限检查
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                        android.util.Log.d("OnboardingScreen", "Android 11以下版本，自动跳过QUERY_ALL_PACKAGES权限步骤")
                        viewModel.nextStep()
                    } else if (hasQueryAllPackagesPermission) {
                        viewModel.nextStep()
                    }
                }
                OnboardingStep.NOTIFICATION_PERMISSION -> {
                    if (hasNotificationPermission) {
                        viewModel.nextStep()
                    }
                }
                OnboardingStep.VIBRATE_PERMISSION -> {
                    if (hasVibratePermission) {
                        viewModel.nextStep()
                    }
                }
                OnboardingStep.BATTERY_OPTIMIZATION -> {
                    if (hasBatteryOptimizationPermission) {
                        viewModel.nextStep()
                    }
                }
                OnboardingStep.WELCOME,
                OnboardingStep.INITIALIZING,
                OnboardingStep.COMPLETED -> {
                    // 这些步骤不需要自动进入下一步
                }
            }
            
            kotlinx.coroutines.delay(1000) // 每秒检查一次
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(stringResource(R.string.onboarding_first_setup)) 
                },
                navigationIcon = {
                    // 只有在提供了返回回调时才显示返回按钮（即从设置页面进入时）
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    }
                },
                actions = {
                    if (currentStep != OnboardingStep.WELCOME && currentStep != OnboardingStep.INITIALIZING) {
                        TextButton(
                            onClick = onCompleted
                        ) {
                            Text(stringResource(R.string.onboarding_skip), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp), // 减少外边距：24dp -> 16dp
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp) // 减少间距：24dp -> 16dp
        ) {
            // 进度指示器
            if (currentStep != OnboardingStep.WELCOME) {
                LinearProgressIndicator(
                    progress = currentStep.progress,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp)) // 减少间距：16dp -> 8dp
            }
            
            // 根据当前步骤显示不同内容
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                when (currentStep) {
                    OnboardingStep.WELCOME -> WelcomeStep(
                        onContinue = { viewModel.nextStep() }
                    )
                    OnboardingStep.USAGE_PERMISSION -> UsagePermissionStep(
                        hasPermission = hasUsagePermission,
                        onRequestPermission = {
                            UsageStatsPermissionHelper.openUsageAccessSettings(context)
                        }
                    )
                    OnboardingStep.QUERY_PACKAGES_PERMISSION -> QueryPackagesPermissionStep(
                        hasPermission = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                            true // Android 11以下版本不需要此权限
                        } else {
                            hasQueryAllPackagesPermission
                        },
                        onRequestPermission = {
                            PermissionUtils.openAppDetailsSettings(context)
                        }
                    )
                    OnboardingStep.NOTIFICATION_PERMISSION -> NotificationPermissionStep(
                        hasPermission = hasNotificationPermission,
                        onRequestPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.nextStep()
                            }
                        }
                    )
                    OnboardingStep.VIBRATE_PERMISSION -> VibratePermissionStep(
                        hasPermission = hasVibratePermission,
                        onRequestPermission = {
                            PermissionUtils.openAppDetailsSettings(context)
                        }
                    )
                    OnboardingStep.BATTERY_OPTIMIZATION -> BatteryOptimizationStep(
                        hasPermission = hasBatteryOptimizationPermission,
                        onRequestPermission = {
                            PermissionUtils.openBackgroundRunSettingsIntelligent(context)
                        }
                    )
                    OnboardingStep.INITIALIZING -> InitializingStep(
                        isInitializing = isInitializing,
                        progress = initProgress,
                        status = initStatus,
                        onStartInitialization = { viewModel.startInitialization() }
                    )
                    OnboardingStep.COMPLETED -> CompletedStep(
                        onFinish = onCompleted
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep(
    onContinue: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = stringResource(R.string.onboarding_welcome, "OffTime"),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = stringResource(R.string.onboarding_subtitle),
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.onboarding_setup_includes),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                
                WelcomeFeatureItem(
                    icon = Icons.Default.Security,
                    title = stringResource(R.string.onboarding_feature_permission_title),
                    description = stringResource(R.string.onboarding_feature_permission_desc)
                )
                
                WelcomeFeatureItem(
                    icon = Icons.Default.Apps,
                    title = stringResource(R.string.onboarding_feature_auto_read_title),
                    description = stringResource(R.string.onboarding_feature_auto_read_desc)
                )
                
                WelcomeFeatureItem(
                    icon = Icons.Default.Done,
                    title = stringResource(R.string.onboarding_feature_start_title),
                    description = stringResource(R.string.onboarding_feature_start_desc)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = stringResource(R.string.onboarding_start_setup),
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun WelcomeFeatureItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun UsagePermissionStep(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    PermissionStepContent(
        icon = Icons.Default.Security,
        title = stringResource(R.string.onboarding_usage_permission_title),
        description = stringResource(R.string.onboarding_usage_permission_desc),
        hasPermission = hasPermission,
        onRequestPermission = onRequestPermission,
        permissions = listOf(
            stringResource(R.string.onboarding_permission_usage_time),
            stringResource(R.string.onboarding_permission_manage_habit),
            stringResource(R.string.onboarding_permission_target_supervision)
        ),
        instructions = listOf(
            stringResource(R.string.onboarding_permission_step_1),
            stringResource(R.string.onboarding_permission_step_2),
            stringResource(R.string.onboarding_permission_step_3),
            stringResource(R.string.onboarding_permission_step_4)
        )
    )
}

@Composable
private fun QueryPackagesPermissionStep(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    @Suppress("UNUSED_VARIABLE")
    val context = LocalContext.current
    val isAndroid11Plus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    
    PermissionStepContent(
        icon = Icons.Default.Apps,
        title = stringResource(R.string.onboarding_query_packages_permission_title),
        description = if (isAndroid11Plus) {
            stringResource(R.string.onboarding_query_packages_permission_desc_android11)
        } else {
            stringResource(R.string.onboarding_query_packages_permission_desc_below11)
        },
        hasPermission = hasPermission,
        onRequestPermission = onRequestPermission,
        permissions = listOf(
            stringResource(R.string.onboarding_permission_basic_info),
            stringResource(R.string.onboarding_permission_app_classification),
            stringResource(R.string.onboarding_permission_app_usage_data)
        ),
        instructions = if (isAndroid11Plus) {
            listOf(
                stringResource(R.string.onboarding_permission_step_1_android11),
                stringResource(R.string.onboarding_permission_step_2_android11),
                stringResource(R.string.onboarding_permission_step_3_android11)
            )
        } else {
            listOf(
                stringResource(R.string.onboarding_permission_step_1_below11),
                stringResource(R.string.onboarding_permission_step_2_below11),
                stringResource(R.string.onboarding_permission_step_3_below11)
            )
        }
    )
}

@Composable
private fun NotificationPermissionStep(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    PermissionStepContent(
        icon = Icons.Default.Notifications,
        title = stringResource(R.string.onboarding_notification_permission_title),
        description = stringResource(R.string.onboarding_notification_permission_desc),
        hasPermission = hasPermission,
        onRequestPermission = onRequestPermission,
        permissions = listOf(
            stringResource(R.string.onboarding_permission_target_completion_reminder),
            stringResource(R.string.onboarding_permission_reward_punishment_reminder),
            stringResource(R.string.onboarding_permission_time_management_habit)
        ),
        instructions = listOf(
            stringResource(R.string.onboarding_permission_step_1_notification),
            stringResource(R.string.onboarding_permission_step_2_notification)
        )
    )
}

@Composable
private fun VibratePermissionStep(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    PermissionStepContent(
        icon = Icons.Default.PhoneAndroid,
        title = stringResource(R.string.onboarding_vibrate_permission_title),
        description = stringResource(R.string.onboarding_vibrate_permission_desc),
        hasPermission = hasPermission,
        onRequestPermission = onRequestPermission,
        permissions = listOf(
            stringResource(R.string.onboarding_permission_timer_end_vibration),
            stringResource(R.string.onboarding_permission_enhanced_sound_effect),
            stringResource(R.string.onboarding_permission_silent_mode_reminder)
        ),
        instructions = listOf(
            stringResource(R.string.onboarding_permission_step_1_vibrate),
            stringResource(R.string.onboarding_permission_step_2_vibrate),
            stringResource(R.string.onboarding_permission_step_3_vibrate)
        )
    )
}

@Composable
private fun BatteryOptimizationStep(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    PermissionStepContent(
        icon = Icons.Default.Settings,
        title = stringResource(R.string.onboarding_battery_optimization_permission_title),
        description = stringResource(R.string.onboarding_battery_optimization_permission_desc),
        hasPermission = hasPermission,
        onRequestPermission = onRequestPermission,
        permissions = listOf(
            stringResource(R.string.onboarding_permission_background_usage_data),
            stringResource(R.string.onboarding_permission_real_time_widget_update),
            stringResource(R.string.onboarding_permission_target_supervision),
            stringResource(R.string.onboarding_permission_timely_reminder)
        ),
        instructions = listOf(
            stringResource(R.string.onboarding_permission_step_1_battery),
            stringResource(R.string.onboarding_permission_step_2_battery),
            stringResource(R.string.onboarding_permission_step_3_battery)
        )
    )
}

@Composable
private fun PermissionStepContent(
    icon: ImageVector,
    title: String,
    description: String,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    permissions: List<String>,
    instructions: List<String>
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = if (hasPermission) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = description,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        // 权限用途说明
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.permission_usage_title),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                
                permissions.forEach { permission ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "•",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = permission,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
        
        // 操作指南
        if (!hasPermission) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.permission_how_to_enable_title),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    instructions.forEachIndexed { index, instruction ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${index + 1}.",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = instruction,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
        
        // 状态和按钮
        if (hasPermission) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE8F5E8)
                )
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50)
                    )
                    Text(
                        text = stringResource(R.string.permission_granted_message),
                        fontSize = 16.sp,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            Button(
                onClick = onRequestPermission,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = stringResource(R.string.permission_request_permission),
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun InitializingStep(
    isInitializing: Boolean,
    progress: Float,
    status: String,
    onStartInitialization: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CloudDownload,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = stringResource(R.string.onboarding_initializing_title),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = stringResource(R.string.onboarding_initializing_desc),
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        if (isInitializing) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.initialization_progress_title),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = status,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.initialization_will_perform_title),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    listOf(
                        stringResource(R.string.initialization_step_scan_apps),
                        stringResource(R.string.initialization_step_smart_classification),
                        stringResource(R.string.initialization_step_auto_exclude),
                        stringResource(R.string.initialization_step_initialize_data),
                        stringResource(R.string.initialization_step_set_default_target)
                    ).forEach { step ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "•",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = step,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
            
            Button(
                onClick = onStartInitialization,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = stringResource(R.string.initialization_start_title),
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun CompletedStep(
    onFinish: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp) // 减少间距：24dp -> 16dp
    ) {
        Spacer(modifier = Modifier.height(16.dp)) // 减少顶部间距：48dp -> 16dp
        
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp), // 减少图标大小：120dp -> 80dp
            tint = Color(0xFF4CAF50)
        )
        
        Text(
            text = stringResource(R.string.onboarding_completed_title),
            fontSize = 24.sp, // 减少字体大小：32sp -> 24sp
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = Color(0xFF4CAF50)
        )
        
        Text(
            text = stringResource(R.string.onboarding_completed_desc),
            fontSize = 16.sp, // 减少字体大小：18sp -> 16sp
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp)) // 减少间距：32dp -> 8dp
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFE8F5E8)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp), // 减少内边距：20dp -> 16dp
                verticalArrangement = Arrangement.spacedBy(12.dp) // 减少间距：16dp -> 12dp
            ) {
                Text(
                    text = stringResource(R.string.onboarding_next_steps_title),
                    fontSize = 16.sp, // 恢复为16sp
                    fontWeight = FontWeight.Medium
                )
                
                listOf(
                    stringResource(R.string.onboarding_next_step_view_stats),
                    stringResource(R.string.onboarding_next_step_manage_categories),
                    stringResource(R.string.onboarding_next_step_configure_target),
                    stringResource(R.string.onboarding_next_step_start_management)
                ).forEach { tip ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp) // 减少间距：8dp -> 6dp
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp), // 减少图标大小：16dp -> 14dp
                            tint = Color(0xFF4CAF50)
                        )
                        Text(
                            text = tip,
                            fontSize = 16.sp // 增加为16sp
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp)) // 减少间距：24dp -> 12dp
        
        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50)
            )
        ) {
            Text(
                text = stringResource(R.string.onboarding_start_using_offtime),
                fontSize = 16.sp,
                color = Color.White
            )
        }
    }
}

// 引导步骤枚举
enum class OnboardingStep(val progress: Float) {
    WELCOME(0.0f),
    USAGE_PERMISSION(0.15f),
    QUERY_PACKAGES_PERMISSION(0.3f),
    NOTIFICATION_PERMISSION(0.45f),
    VIBRATE_PERMISSION(0.6f),
    BATTERY_OPTIMIZATION(0.75f),
    INITIALIZING(0.9f),
    COMPLETED(1.0f)
} 