package com.offtime.app

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

import androidx.navigation.compose.rememberNavController
import com.offtime.app.navigation.bottomNavItems
import com.offtime.app.ui.components.ModernBottomNavigation
import com.offtime.app.ui.screens.*
import com.offtime.app.ui.screens.OnboardingScreen
import com.offtime.app.ui.screens.WidgetSettingsScreen
import com.offtime.app.ui.screens.PaymentScreen
import com.offtime.app.ui.screens.LoginScreen
import com.offtime.app.ui.offlinetimer.OfflineTimerScreen
import com.offtime.app.ui.permissions.UsagePermissionScreen
import com.offtime.app.ui.debug.*
import com.offtime.app.ui.debug.DebugPieChartTestScreen
import com.offtime.app.ui.debug.DebugDataCollectionScreen
import com.offtime.app.ui.debug.DebugUnifiedUpdateScreen
import com.offtime.app.ui.theme.OffTimeTheme
import com.offtime.app.utils.FirstLaunchManager
import com.offtime.app.utils.LocaleUtils
import com.offtime.app.utils.ProvideLocaleUtils
import com.offtime.app.service.DataAggregationService
import com.offtime.app.service.UnifiedUpdateService
import com.offtime.app.manager.SubscriptionManager
import com.offtime.app.util.AppLifecycleObserver
import com.offtime.app.util.DataUpdateEventManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.runBlocking

/**
 * OffTimes应用主活动
 * 
 * 作为应用的入口点，承担以下职责：
 * 1. 初始化应用主题和导航系统
 * 2. 管理整个应用的导航状态
 * 3. 提供底部导航栏的全局导航
 * 4. 协调各个功能模块的界面切换
 * 
 * 技术特点：
 * - 使用Jetpack Compose构建现代化UI
 * - 采用Navigation Compose进行页面导航
 * - 集成Dagger Hilt依赖注入框架
 * - 支持Material 3设计规范
 * 
 * 导航结构：
 * - 主要功能：首页、分类管理、统计分析、设置
 * - 设置子页面：目标奖罚、任务提醒、离线计时、权限管理
 * - 调试功能：数据库调试、诊断工具、数据表查看
 * 
 * 生命周期：
 * - onCreate(): 初始化UI内容和主题
 * - 自动管理Compose生命周期和状态保存
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var firstLaunchManager: FirstLaunchManager
    
    @Inject
    lateinit var subscriptionManager: SubscriptionManager
    
    @Inject
    lateinit var localeUtils: LocaleUtils
    
    @Inject
    lateinit var dataUpdateEventManager: DataUpdateEventManager
    
    /**
     * 活动创建时的初始化
     * 
     * 执行流程：
     * 1. 调用父类onCreate完成基础初始化
     * 2. 设置Compose UI内容
     * 3. 应用OffTimeTheme主题
     * 4. 启动OffTimeApp主界面
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProvideLocaleUtils(localeUtils = localeUtils) {
                OffTimeTheme {
                    OffTimeApp(
                        firstLaunchManager = firstLaunchManager,
                        subscriptionManager = subscriptionManager
                    )
                }
            }
        }
    }
    
    override fun attachBaseContext(newBase: Context) {
        // 在Activity启动前应用语言设置
        val localeUtils = try {
            (newBase.applicationContext as OffTimeApplication).localeUtils
        } catch (e: Exception) {
            // 如果依赖注入还未完成，使用临时实例
            LocaleUtils(newBase)
        }
        
        val localizedContext = localeUtils.applyLanguageToContext(newBase)
        super.attachBaseContext(localizedContext)
    }
    
    /**
     * 活动恢复时的数据更新触发
     * 
     * 触发场景：
     * 1. 打开OffTimes应用
     * 2. 从其他应用切换到OffTimes应用
     * 3. 从系统后台切换回OffTimes应用
     * 
     * 执行完整的三阶段数据更新：
     * - 基础数据表更新（app_sessions_user、timer_sessions_user等）
     * - 中间聚合表更新（daily_usage_user、summary_usage_user等）
     * - 前端UI图像更新
     */
    override fun onResume() {
        super.onResume()
        AppLifecycleObserver.onActivityResumed()
        
        // 检查是否已完成引导
        if (!firstLaunchManager.isFirstLaunch() && firstLaunchManager.isOnboardingCompleted()) {
            try {
                // 🔧 使用新的数据更新事件管理器触发应用恢复前台的数据更新
                dataUpdateEventManager.triggerAppResumeUpdate(this)
                android.util.Log.d("MainActivity", "应用前台切换 → 通过DataUpdateEventManager触发完整数据更新")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "触发前台切换数据更新失败", e)
            }
        } else {
            android.util.Log.d("MainActivity", "应用尚未完成引导，跳过前台切换数据更新")
        }
    }

    override fun onPause() {
        super.onPause()
        AppLifecycleObserver.onActivityPaused()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OffTimeApp(
    firstLaunchManager: FirstLaunchManager,
    subscriptionManager: SubscriptionManager
) {
    val navController = rememberNavController()
    
    // 检查是否需要显示引导页面
    val isFirstLaunch = remember { firstLaunchManager.isFirstLaunch() }
    val isOnboardingCompleted = remember { firstLaunchManager.isOnboardingCompleted() }
    var shouldShowOnboarding by remember { 
        mutableStateOf(isFirstLaunch && !isOnboardingCompleted) 
    }
    
    // 检查付费状态
    var subscriptionStatus by remember { mutableStateOf(SubscriptionManager.SubscriptionStatus.TRIAL_ACTIVE) }
    var shouldShowPaymentScreen by remember { mutableStateOf(false) }
    
    // 在完成引导后检查付费状态
    LaunchedEffect(shouldShowOnboarding) {
        if (!shouldShowOnboarding) {
            try {
                subscriptionStatus = subscriptionManager.getCurrentSubscriptionStatus()
                shouldShowPaymentScreen = when (subscriptionStatus) {
                    SubscriptionManager.SubscriptionStatus.TRIAL_EXPIRED -> true
                    else -> false
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "检查付费状态失败", e)
            }
        }
    }
    
    if (shouldShowOnboarding) {
        // 显示引导页面
        OnboardingScreen(
            onCompleted = {
                firstLaunchManager.setOnboardingCompleted()
                firstLaunchManager.setFirstLaunchCompleted()
                shouldShowOnboarding = false
            }
        )
    } else if (shouldShowPaymentScreen) {
        // 显示付费页面（试用期过期）
        PaymentScreen(
            onNavigateBack = {
                // 允许用户返回，但显示受限模式警告
                shouldShowPaymentScreen = false
            },
            onPaymentSuccess = {
                shouldShowPaymentScreen = false
                // 重新检查付费状态
                runBlocking {
                    subscriptionStatus = subscriptionManager.getCurrentSubscriptionStatus()
                }
            }
        )
    } else {
        // 显示主应用界面
        Column {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    BottomNavigationBar(navController = navController)
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = "home",
                    modifier = Modifier.padding(innerPadding)
                ) {
                composable("home") {
                    HomeScreen(
                        onNavigateToPayment = {
                            navController.navigate("payment")
                        }
                    )
                }
                composable("categories") {
                    CategoriesScreen(
                        onNavigateToExcludedApps = {
                            navController.navigate("excluded_apps")
                        }
                    )
                }
                composable("stats") {
                    StatsScreen()
                }
                composable("settings") {
                    SettingsScreen(
                        onNavigateToGoalRewardPunishment = {
                            navController.navigate("goal_reward_punishment")
                        },
                        onNavigateToOfflineTimer = {
                            navController.navigate("offline_timer")
                        },
                        onNavigateToUsagePermission = {
                            navController.navigate("usage_permission")
                        },
                        onNavigateToTaskReminder = {
                            navController.navigate("task_reminder")
                        },
                        onNavigateToWidgetSettings = {
                            navController.navigate("widget_settings")
                        },
                        onNavigateToDebug = {
                            navController.navigate("debug_main")
                        },
                        onNavigateToOnboarding = {
                            navController.navigate("onboarding")
                        },
                        onNavigateToPayment = {
                            navController.navigate("payment")
                        },
                        onNavigateToLogin = {
                            navController.navigate("login")
                        },
                        onNavigateToLanguageSettings = {
                            navController.navigate("language_settings")
                        },
                        onNavigateToAbout = {
                            navController.navigate("about")
                        }
                    )
                }
                composable("goal_reward_punishment") {
                    GoalRewardPunishmentScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
                composable("task_reminder") {
                    TaskReminderScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
                composable("widget_settings") {
                    WidgetSettingsScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
                composable("offline_timer") {
                    OfflineTimerScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
                composable("payment") {
                    PaymentScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onPaymentSuccess = {
                            navController.popBackStack()
                        }
                    )
                }
                composable("login") {
                    LoginScreen(
                        onBack = {
                            navController.popBackStack()
                        },
                        onLoginSuccess = {
                            navController.popBackStack()
                        }
                    )
                }
                composable("usage_permission") {
                    UsagePermissionScreen(
                        onPermissionGranted = {
                            navController.popBackStack()
                        },
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }
                composable("debug_main") {
                    DebugMainScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onNavigateToTable = { route ->
                            navController.navigate(route)
                        }
                    )
                }
                composable("debug_categories") {
                    DebugCategoriesScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
                composable("debug_settings") {
                    DebugSettingsScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
                composable("debug_summary_tables") {
                    DebugSummaryTablesScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
                composable("debug_summary_usage_user") {
                    DebugSummaryUsageUserScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
                composable("debug_summary_usage_week") {
                    DebugSummaryUsageWeekScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
                composable("debug_summary_usage_month") {
                    DebugSummaryUsageMonthScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
                composable("debug_reward_punishment_week") {
                    DebugRewardPunishmentWeekScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
                composable("debug_reward_punishment_month") {
                    DebugRewardPunishmentMonthScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
                composable("debug_reward_punishment_user") {
                    DebugRewardPunishmentUserScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
                composable("debug_usage") {
                    DebugUsageScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
                composable("debug_diagnosis") {
                    DebugDiagnosisScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
                composable("debug_data_consistency") {
                    DebugDataConsistencyScreen(
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }
                composable("debug_scaling_factor") {
                    DebugScalingFactorScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
                composable("debug_pie_chart_test") {
                    DebugPieChartTestScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
                composable("debug_data_collection") {
                    DebugDataCollectionScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
                composable("debug_unified_update") {
                    DebugUnifiedUpdateScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
                composable("debug_goals") {
                    DebugGoalsScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
                composable("debug_apps") {
                    DebugAppsScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
                composable("debug_sessions") {
                    DebugSessionsScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
                composable("debug_timer_sessions") {
                    DebugTimerSessionsScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
                composable("debug_rewards") {
                    DebugRewardsScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
                composable("debug_data_init") {
                    DebugDataInitScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
                composable("excluded_apps") {
                    ExcludedAppsScreen(
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                }
                composable("daily_usage") {
                    DailyUsageScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
                composable("language_settings") {
                    LanguageSettingsScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onRestartRequired = {
                            // 显示重启提示，但不强制重启
                            // 用户可以选择继续使用或手动重启应用
                        }
                    )
                }
                composable("about") {
                    AboutScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
                composable("onboarding") {
                    OnboardingScreen(
                        onCompleted = {
                            navController.navigate("home") {
                                popUpTo("onboarding") { inclusive = true }
                            }
                        },
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    ModernBottomNavigation(navController = navController)
} 