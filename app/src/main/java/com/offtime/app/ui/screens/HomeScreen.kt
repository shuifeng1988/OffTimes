package com.offtime.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.offtime.app.BuildConfig
import com.offtime.app.R
import com.offtime.app.data.entity.AppCategoryEntity
import com.offtime.app.utils.DateLocalizer
import com.offtime.app.ui.components.CategoryChip
import com.offtime.app.ui.components.PieChartWithCardsRefactored
import com.offtime.app.ui.components.HourlyBarChart
import com.offtime.app.ui.components.SimpleUsageLineChart
import com.offtime.app.ui.components.SimpleCompletionLineChart
import com.offtime.app.ui.components.SimpleRewardChart
import com.offtime.app.ui.components.SimplePunishmentChart
import com.offtime.app.ui.components.AppDetailDialog
import com.offtime.app.ui.components.OfflineTimerDialog
import com.offtime.app.ui.components.CompletionPercentageDialog
import com.offtime.app.ui.viewmodel.HomeViewModel
import com.offtime.app.ui.theme.LocalResponsiveDimensions
import com.offtime.app.utils.ScalingFactorUtils
import com.offtime.app.utils.ScaleType
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.offtime.app.manager.SubscriptionManager
import com.offtime.app.data.repository.UserRepository
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = hiltViewModel(),
    onNavigateToPayment: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // 收集ViewModel状态
    val categories by homeViewModel.categories.collectAsState()
    val selectedCategory by homeViewModel.selectedCategory.collectAsState()
    val isStatisticsView by homeViewModel.isStatisticsView.collectAsState()
    val realUsageSec by homeViewModel.realUsageSec.collectAsState()
    val virtualUsageSec by homeViewModel.virtualUsageSec.collectAsState()
    val goalSec by homeViewModel.goalSec.collectAsState()
    val yesterdayRewardDone by homeViewModel.yesterdayRewardDone.collectAsState()
    val yesterdayPunishDone by homeViewModel.yesterdayPunishDone.collectAsState()
    val yesterdayRewardPercent by homeViewModel.yesterdayRewardPercent.collectAsState()
    val yesterdayPunishPercent by homeViewModel.yesterdayPunishPercent.collectAsState()
    val yesterdayHasData by homeViewModel.yesterdayHasData.collectAsState()
    val yesterdayGoalMet by homeViewModel.yesterdayGoalMet.collectAsState()
    val rewardText by homeViewModel.rewardText.collectAsState()
    val punishText by homeViewModel.punishText.collectAsState()
    
    // 完成度选择对话框状态（暂时不使用，预留）
    val hourlyRealUsage by homeViewModel.hourlyRealUsage.collectAsState()
    val hourlyVirtualUsage by homeViewModel.hourlyVirtualUsage.collectAsState()
    val selectedLineChartPeriod by homeViewModel.selectedLineChartPeriod.collectAsState()
    val usageLineData by homeViewModel.usageLineData.collectAsState()
    val completionLineData by homeViewModel.completionLineData.collectAsState()
    val rewardPunishmentData by homeViewModel.rewardPunishmentData.collectAsState()
    val categoryUsageData by homeViewModel.categoryUsageData.collectAsState()
    val categoryHourlyData by homeViewModel.categoryHourlyData.collectAsState()
    val isRewardPunishmentEnabled by homeViewModel.isRewardPunishmentEnabled.collectAsState()
    val goalConditionType by homeViewModel.goalConditionType.collectAsState()
    val rewardPunishmentSummary by homeViewModel.rewardPunishmentSummary.collectAsState()
    val isPunishLoading by homeViewModel.isPunishLoading.collectAsState()
    
    // 对话框状态
    val showAppDetailDialog by homeViewModel.showAppDetailDialog.collectAsState()
    val appDetailList by homeViewModel.appDetailList.collectAsState()
    val appDetailTitle by homeViewModel.appDetailTitle.collectAsState()
    val showTimerDialog by homeViewModel.showTimerDialog.collectAsState()
    // 完成度选择对话框状态
    val showCompletionDialog by homeViewModel.showCompletionDialog.collectAsState()
    val completionDialogIsReward by homeViewModel.completionDialogIsReward.collectAsState()
    val completionDialogTaskDescription by homeViewModel.completionDialogTaskDescription.collectAsState()
    val completionDialogTargetNumber by homeViewModel.completionDialogTargetNumber.collectAsState()
    val defaultTimerTab by homeViewModel.defaultTimerTab.collectAsState()

    // 线下计时器状态
    val isTimerRunning by homeViewModel.isTimerRunning.collectAsState()
    val isTimerPaused by homeViewModel.isTimerPaused.collectAsState()
    val timerHours by homeViewModel.timerHours.collectAsState()
    val timerMinutes by homeViewModel.timerMinutes.collectAsState()
    val timerSecondsUnit by homeViewModel.timerSecondsUnit.collectAsState()
    
    // 计算当前分类是否正在计时
    val isCurrentCategoryTiming = homeViewModel.isCurrentCategoryTiming()
    
    // 背景计时状态
    val isTimerInBackground by homeViewModel.isTimerInBackground.collectAsState()
    
    // CountDown相关状态（重新添加以支持弹窗集成）
    val isCountdownTimerRunning by homeViewModel.isCountdownTimerRunning.collectAsState()
    val isCountdownTimerPaused by homeViewModel.isCountdownTimerPaused.collectAsState()
    val isCountdownInBackground by homeViewModel.isCountdownInBackground.collectAsState()
    val countdownHours by homeViewModel.countdownHours.collectAsState()
    val countdownMinutes by homeViewModel.countdownMinutes.collectAsState()
    val countdownSecondsUnit by homeViewModel.countdownSecondsUnit.collectAsState()
    
    // 计算当前分类是否正在CountDown
    val isCurrentCategoryCountdownTiming = homeViewModel.isCurrentCategoryCountdownTiming()
    
    // 下拉刷新状态
    val isRefreshing by homeViewModel.isRefreshing.collectAsState()
    @Suppress("DEPRECATION")
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)
    
    // 订阅状态
    var subscriptionInfo by remember { mutableStateOf<SubscriptionManager.SubscriptionInfo?>(null) }
    
    // 加载订阅状态
    LaunchedEffect(Unit) {
        try {
            // 通过HomeViewModel获取订阅信息
            subscriptionInfo = homeViewModel.getSubscriptionInfo()
        } catch (e: Exception) {
            android.util.Log.e("HomeScreen", "加载订阅信息失败", e)
        }
    }
    
    // 获取优化后的布局参数
    val optimizedLayout = ScalingFactorUtils.getOptimizedHomeLayoutSpacing()        // 各区域间距
    val optimizedCharts = ScalingFactorUtils.getOptimizedChartSizes()              // 图表高度和按钮间距
    
    // 当页面首次加载或重新进入时，自动触发一次完整的刷新
    LaunchedEffect(Unit) {
        android.util.Log.d("HomeScreen", "🔄 页面加载触发数据更新")
        homeViewModel.onSwipeRefresh()
    }

    LaunchedEffect(selectedCategory) {
        selectedCategory?.let {
            homeViewModel.loadUsageData(it.id)
        }
    }
    
    // ========== 核心布局结构：下拉刷新 + 垂直滚动列表 ==========
    @Suppress("DEPRECATION")
    SwipeRefresh(
        state = swipeRefreshState,
        onRefresh = { homeViewModel.onSwipeRefresh() },
        modifier = modifier.fillMaxSize()
    ) {
        // 使用LazyColumn实现高效的垂直滚动，内容按功能区域划分
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = optimizedLayout.horizontalPadding), // 使用屏幕宽度3.5%作为水平内边距
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ========== 第零区域：试用状态显示 ==========
            subscriptionInfo?.let { info ->
                val isTrialExpired = !info.isInTrial && !info.isPremium
                if (isTrialExpired) {
                    item {
                        TrialStatusBanner(
                            subscriptionInfo = info,
                            onUpgradeClick = {
                                android.util.Log.d("HomeScreen", "用户点击升级按钮，导航到付费页面")
                                onNavigateToPayment()
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
            
            // ========== 第一区域：分类栏 ==========
            item {
                CategoryBar(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { homeViewModel.selectCategory(it) }
                )
                Spacer(modifier = Modifier.height(optimizedLayout.categoryBarBottomSpacing)) // 使用优化设置
            }

            // ========== 第二区域：选项卡切换 ==========
            item {
                Spacer(modifier = Modifier.height(optimizedLayout.tabSectionTopSpacing)) // 使用优化设置
                TabSection(
                    isStatisticsView = isStatisticsView,
                    onTabChanged = { homeViewModel.setStatisticsView(it) }
                )
                Spacer(modifier = Modifier.height(optimizedLayout.tabSectionBottomSpacing)) // 使用优化设置
            }

            // 第三区域：所有图表的统一容器
            item {
                // 当有分类被选中时，显示所有图表
                selectedCategory?.let { category ->
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(optimizedLayout.sectionSpacing) // 使用统一的间距
                    ) {
                        // 主要图表区域（饼图或柱状图）
                        if (isStatisticsView) {
                            PieChartWithCardsRefactored(
                                realUsageSec = realUsageSec,
                                virtualUsageSec = virtualUsageSec,
                                goalSec = goalSec,
                                yesterdayRewardDone = yesterdayRewardDone,
                                yesterdayPunishDone = yesterdayPunishDone,
                                yesterdayHasData = yesterdayHasData,
                                yesterdayGoalMet = yesterdayGoalMet,
                                categoryId = category.id,
                                categoryName = category.name,
                                goalConditionType = goalConditionType,
                                rewardText = rewardText,
                                punishText = punishText,
                                yesterdayRewardPercent = yesterdayRewardPercent,
                                yesterdayPunishPercent = yesterdayPunishPercent,
                                onRewardComplete = { homeViewModel.showRewardCompletionDialog() },
                                onPunishmentComplete = { homeViewModel.showPunishmentCompletionDialog() },
                                onYesterdayInfoClick = { homeViewModel.showYesterdayDetailDialog() },
                                categoryUsageData = categoryUsageData,
                                isRewardPunishmentEnabled = isRewardPunishmentEnabled,
                                rewardPunishmentSummary = rewardPunishmentSummary,
                                isPunishLoading = isPunishLoading,
                                onCategoryClick = { categoryId, categoryName ->
                                    if (category.name == "总使用") {
                                        homeViewModel.showAllAppsDetailDialog()
                                    } else {
                                        homeViewModel.showAppDetailDialog(categoryId, categoryName)
                                    }
                                },
                                isTimerRunning = isTimerRunning,
                                isTimerPaused = isTimerPaused,
                                isTimerInBackground = isTimerInBackground,
                                isCurrentCategoryTiming = isCurrentCategoryTiming,
                                timerHours = timerHours,
                                timerMinutes = timerMinutes,
                                timerSeconds = timerSecondsUnit,
                                onShowTimerDialog = { homeViewModel.showTimerDialog() },
                                onTimerBackgroundClick = { homeViewModel.exitTimerBackgroundMode() },
                                isCountdownTimerRunning = isCountdownTimerRunning,
                                isCountdownTimerPaused = isCountdownTimerPaused,
                                isCountdownInBackground = isCountdownInBackground,
                                isCurrentCategoryCountdownTiming = isCurrentCategoryCountdownTiming,
                                countdownHours = countdownHours,
                                countdownMinutes = countdownMinutes,
                                countdownSecondsUnit = countdownSecondsUnit,
                                onCountdownBackgroundClick = { homeViewModel.exitCountdownBackgroundMode() },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            HourlyBarChart(
                                realUsageMinutes = hourlyRealUsage,
                                virtualUsageMinutes = hourlyVirtualUsage,
                                categoryName = category.name,
                                categoryHourlyData = categoryHourlyData,
                                onBarClick = { hour ->
                                    val currentCategory = selectedCategory
                                    if (currentCategory != null) {
                                        if (currentCategory.id == category.id) {
                                            homeViewModel.showHourlyAppDetailDialog(hour, currentCategory.id, currentCategory.name)
                                        } else {
                                            homeViewModel.showHourlyAppDetailDialog(hour, currentCategory.id, currentCategory.name)
                                        }
                                    } else {
                                        homeViewModel.showHourlyAppDetailDialog(hour, category.id, category.name)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // 日/周/月 筛选器
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = ScalingFactorUtils.uniformScaledDp(2.dp)),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val periods = listOf("日", "周", "月")
                            val context = LocalContext.current
                            periods.forEach { period ->
                                FilterChip(
                                    onClick = { homeViewModel.setLineChartPeriod(period) },
                                    label = { 
                                        Text(
                                            text = DateLocalizer.formatPeriodLabel(context, period),
                                            fontSize = ScalingFactorUtils.uniformScaledSp(14.sp)
                                        ) 
                                    },
                                    selected = selectedLineChartPeriod == period,
                                    modifier = Modifier.padding(end = ScalingFactorUtils.uniformScaledDp(8.dp)),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        labelColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }

                        // 所有折线图
                        SimpleUsageLineChart(
                            usageData = usageLineData,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(height = optimizedCharts.lineChartHeight)
                        )
                        
                        SimpleCompletionLineChart(
                            completionData = completionLineData,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(height = optimizedCharts.lineChartHeight)
                        )
                        
                        SimpleRewardChart(
                            rewardPunishmentData = rewardPunishmentData,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(height = optimizedCharts.lineChartHeight)
                        )
                        
                        SimplePunishmentChart(
                            rewardPunishmentData = rewardPunishmentData,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(height = optimizedCharts.lineChartHeight)
                        )
                    }
                }
                // 当没有分类被选中时，显示提示信息
                ?: run {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(ScalingFactorUtils.uniformScaledDp(200.dp)),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.select_category),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } // End LazyColumn
    } // End SwipeRefresh
    
    // 应用详情对话框
    AppDetailDialog(
        showDialog = showAppDetailDialog,
        title = appDetailTitle,
        appDetailList = appDetailList,
        onDismiss = { homeViewModel.hideAppDetailDialog() }
    )

    // 线下计时/倒计时 对话框
    OfflineTimerDialog(
        showDialog = showTimerDialog,
        isTimerRunning = isTimerRunning,
        isTimerPaused = isTimerPaused,
        hours = timerHours,
        minutes = timerMinutes,
        seconds = timerSecondsUnit,
        onStartTimer = { homeViewModel.startOfflineTimer() },
        onPauseTimer = { homeViewModel.pauseOfflineTimer() },
        onResumeTimer = { homeViewModel.resumeOfflineTimer() },
        onStopTimer = {
            homeViewModel.stopOfflineTimer()
            // 确保对话框立即关闭
            homeViewModel.hideTimerDialog()
        },
        onBackgroundMode = { homeViewModel.enterTimerBackgroundMode() },
        onDismiss = { homeViewModel.hideTimerDialog() },
        // CountDown 相关
        isCountdownRunning = isCurrentCategoryCountdownTiming,
        isCountdownPaused = isCountdownTimerPaused,
        countdownHours = countdownHours,
        countdownMinutes = countdownMinutes,
        countdownSeconds = countdownSecondsUnit,
        onStartCountdown = { minutes ->
            homeViewModel.setCountdownDuration(minutes)
            homeViewModel.startCountdownTimer()
        },
        onPauseCountdown = { homeViewModel.pauseCountdownTimer() },
        onResumeCountdown = { homeViewModel.resumeCountdownTimer() },
        onStopCountdown = {
            homeViewModel.stopCountdownTimer()
            // 确保对话框立即关闭
            homeViewModel.hideTimerDialog()
        },
        onCountdownBackgroundMode = { homeViewModel.enterCountdownBackgroundMode() },
        defaultTab = defaultTimerTab
    )

    // 奖励/惩罚 完成度选择对话框
    CompletionPercentageDialog(
        isVisible = showCompletionDialog,
        isReward = completionDialogIsReward,
        taskDescription = completionDialogTaskDescription,
        targetNumber = completionDialogTargetNumber,
        onDismiss = { homeViewModel.dismissCompletionDialog() },
        onConfirm = { percent ->
            if (completionDialogIsReward) {
                homeViewModel.completeYesterdayReward(percent)
            } else {
                homeViewModel.completeYesterdayPunishment(percent)
            }
        }
    )
}

@Composable
private fun CategoryBar(
    categories: List<AppCategoryEntity>,
    selectedCategory: AppCategoryEntity?,
    onCategorySelected: (AppCategoryEntity) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = ScalingFactorUtils.uniformScaledDp(4.dp),
                vertical = ScalingFactorUtils.uniformScaledDp(4.dp)
            ),
        horizontalArrangement = Arrangement.spacedBy(ScalingFactorUtils.uniformScaledDp(4.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        categories.forEach { category ->
            CategoryChip(
                category = category,
                isSelected = selectedCategory?.id == category.id,
                onClick = { onCategorySelected(category) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabSection(
    isStatisticsView: Boolean,
    onTabChanged: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        FilterChip(
            onClick = { onTabChanged(true) },
            label = { 
                Text(
                    text = stringResource(R.string.home_today_stats),
                    fontSize = ScalingFactorUtils.uniformScaledSp(14.sp),
                    fontWeight = FontWeight.Medium
                ) 
            },
            selected = isStatisticsView,
            modifier = Modifier
                .heightIn(min = ScalingFactorUtils.uniformScaledDp(42.dp))
                .padding(horizontal = ScalingFactorUtils.uniformScaledDp(2.dp))
        )
        
        Spacer(modifier = Modifier.width(ScalingFactorUtils.uniformScaledDp(16.dp)))
        
        FilterChip(
            onClick = { onTabChanged(false) },
            label = { 
                Text(
                    text = stringResource(R.string.home_24h_details),
                    fontSize = ScalingFactorUtils.uniformScaledSp(14.sp),
                    fontWeight = FontWeight.Medium
                ) 
            },
            selected = !isStatisticsView,
            modifier = Modifier
                .heightIn(min = ScalingFactorUtils.uniformScaledDp(42.dp))
                .padding(horizontal = ScalingFactorUtils.uniformScaledDp(2.dp))
        )
    }
}

@Composable
private fun YesterdayDetailDialog(
    detailData: HomeViewModel.YesterdayDetailData?,
    onDismiss: () -> Unit
) {
    if (detailData == null) return
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "📊",
                    fontSize = 24.sp
                )
                Text(
                    text = stringResource(R.string.yesterday_detail_title),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "📱",
                            fontSize = 20.sp
                        )
                        Text(
                            text = stringResource(R.string.category_label, DateLocalizer.getCategoryName(LocalContext.current, detailData.categoryName)),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF3F8FF)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "⏱️",
                                fontSize = 18.sp
                            )
                            Text(
                                text = stringResource(R.string.usage_time_details),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1976D2)
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "🌐",
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = stringResource(R.string.online_usage) + "：",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = formatTime(detailData.realUsageSeconds),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1976D2)
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "📱",
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = stringResource(R.string.offline_usage) + "：",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = formatTime(detailData.virtualUsageSeconds),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7B1FA2)
                            )
                        }
                        
                        HorizontalDivider(
                            color = Color(0xFF1976D2).copy(alpha = 0.2f),
                            thickness = 1.dp
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "📊",
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = stringResource(R.string.total_usage_time) + "：",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = formatTime(detailData.realUsageSeconds + detailData.virtualUsageSeconds),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF388E3C)
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "🎯",
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = stringResource(R.string.target_time) + "：",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = formatTime(detailData.goalSeconds),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF9800)
                            )
                        }
                    }
                }
                
                if (detailData.rewardContent.isNotEmpty() || detailData.punishmentContent.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (detailData.goalMet) 
                                Color(0xFFFFF3E0) 
                            else 
                                Color(0xFFFFEBEE)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (detailData.rewardContent.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.reward_content_display) + "：",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFFF9800)
                                    )
                                    Text(
                                        text = DateLocalizer.localizeRewardText(LocalContext.current, detailData.rewardContent),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFF9800)
                                    )
                                }
                            }
                            
                            if (detailData.punishmentContent.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.punishment_content_display) + "：",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFD32F2F)
                                    )
                                    Text(
                                        text = DateLocalizer.localizePunishmentText(LocalContext.current, detailData.punishmentContent),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFD32F2F)
                                    )
                                }
                            }
                        }
                    }
                }
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (detailData.goalMet) 
                            Color(0xFFE8F5E8) 
                        else 
                            Color(0xFFFFEBEE)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            val (statusText, statusColor) = when {
                                detailData.rewardContent.isNotEmpty() || detailData.punishmentContent.isNotEmpty() -> {
                                    val goalStatus = if (detailData.goalMet) 
                                        stringResource(R.string.goal_met)
                                    else 
                                        stringResource(R.string.goal_not_met)
                                    
                                    val (completionStatus, isCompleted) = if (detailData.goalMet && detailData.rewardContent.isNotEmpty()) {
                                        if (detailData.rewardCompleted) stringResource(R.string.status_completed) to true else stringResource(R.string.status_pending) to false
                                    } else if (!detailData.goalMet && detailData.punishmentContent.isNotEmpty()) {
                                        if (detailData.punishmentCompleted) stringResource(R.string.status_completed) to true else stringResource(R.string.status_pending) to false
                                    } else {
                                        stringResource(R.string.status_pending) to false
                                    }
                                    
                                    val fullStatus = "$goalStatus－$completionStatus"
                                    
                                    val (icon, color) = when {
                                        isCompleted -> "✅" to Color(0xFF388E3C)
                                        detailData.goalMet -> "⏳" to Color(0xFFFF8F00)
                                        else -> "❌" to Color(0xFFD32F2F)
                                    }
                                    
                                    "$icon $fullStatus" to color
                                }
                                else -> {
                                    if (detailData.goalMet) 
                                        "✅ ${stringResource(R.string.goal_met)}" to Color(0xFF2E7D32)
                                    else 
                                        "❌ ${stringResource(R.string.goal_not_met)}" to Color(0xFFD32F2F)
                                }
                            }
                            
                            Text(
                                text = statusText,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.dialog_confirm),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
}

private fun formatTime(seconds: Int): String {
    val hours = seconds / 3600.0
    return String.format("%.1fh", hours)
}

@Composable
fun TrialStatusBanner(
    subscriptionInfo: SubscriptionManager.SubscriptionInfo,
    onUpgradeClick: () -> Unit
) {
    val isTrialExpired = !subscriptionInfo.isInTrial && !subscriptionInfo.isPremium

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isTrialExpired) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (isTrialExpired) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.secondary
                    }
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = if (isTrialExpired) 
                            stringResource(R.string.trial_expired) 
                        else 
                            stringResource(R.string.trial_active),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isTrialExpired) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.secondary
                        }
                    )
                    
                    val statusText = if (isTrialExpired) {
                        stringResource(R.string.trial_expired_subtitle)
                    } else {
                        stringResource(R.string.trial_remaining_days, subscriptionInfo.trialDaysRemaining)
                    }
                    
                    Text(
                        text = statusText,
                        fontSize = 12.sp,
                        color = if (isTrialExpired) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        }
                    )
                }
            }
            
            Button(
                onClick = onUpgradeClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    text = if (isTrialExpired) {
                        stringResource(R.string.upgrade_now)
                    } else {
                        val currencySymbol = if (BuildConfig.ENABLE_GOOGLE_PAY) "$" else "¥"
                        val price = if (BuildConfig.ENABLE_GOOGLE_PAY) "9.9" else "9.9"
                        stringResource(R.string.upgrade_with_price, currencySymbol + price)
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
