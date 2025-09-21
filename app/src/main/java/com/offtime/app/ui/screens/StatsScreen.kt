package com.offtime.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.offtime.app.ui.viewmodel.StatsViewModel
import com.offtime.app.ui.viewmodel.CategoryUsageData
import com.offtime.app.ui.viewmodel.CategoryGoalCompletionData
import com.offtime.app.ui.viewmodel.CategoryRewardPunishmentData
import com.offtime.app.ui.viewmodel.AppUsageChangeData
import com.offtime.app.ui.viewmodel.AppDailyUsageRankingData
import com.offtime.app.ui.theme.LocalResponsiveDimensions
import com.offtime.app.ui.components.RewardDetailsCard
import com.offtime.app.ui.components.PunishmentDetailsCard
import com.offtime.app.R
import com.offtime.app.utils.DateLocalizer
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import androidx.compose.ui.text.style.TextOverflow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel()
) {
    // 获取响应式尺寸参数db -s emulator-5554 shell "logcat -v time -f offtimes_focus.log -s UsageStatsCollector:V UnifiedUpdateService:V AppSessionRepository:V ScreenStateReceiver:V DataAggregationService:V"
    val dimensions = LocalResponsiveDimensions
    
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
    
    // 每个模块独立的时间段状态
    val usageStatsPeriod by viewModel.usageStatsPeriod.collectAsState()
    val goalCompletionPeriod by viewModel.goalCompletionPeriod.collectAsState()

    val usageChangePeriod by viewModel.usageChangePeriod.collectAsState()
    val dailyUsageRankingPeriod by viewModel.dailyUsageRankingPeriod.collectAsState()
    
    // 每个模块独立的加载状态
    val isUsageStatsLoading by viewModel.isUsageStatsLoading.collectAsState()
    val isGoalCompletionLoading by viewModel.isGoalCompletionLoading.collectAsState()
    @Suppress("UNUSED_VARIABLE")
    val isRewardPunishmentLoading by viewModel.isRewardPunishmentLoading.collectAsState()
    val isUsageChangeLoading by viewModel.isUsageChangeLoading.collectAsState()
    val isDailyUsageRankingLoading by viewModel.isDailyUsageRankingLoading.collectAsState()
    
    val categoryUsageData by viewModel.categoryUsageData.collectAsState()
    val categoryGoalCompletionData by viewModel.categoryGoalCompletionData.collectAsState()
    val categoryRewardPunishmentData by viewModel.categoryRewardPunishmentData.collectAsState()
    val appUsageChangeData by viewModel.appUsageChangeData.collectAsState()
    val appDailyUsageRankingData by viewModel.appDailyUsageRankingData.collectAsState()
    val isUsageChangeExpanded by viewModel.isUsageChangeExpanded.collectAsState()
    val isDailyUsageRankingExpanded by viewModel.isDailyUsageRankingExpanded.collectAsState()
    
    // 新的奖励和惩罚详情状态
    val rewardDetailsData by viewModel.rewardDetailsData.collectAsState()
    val rewardDetailsPeriod by viewModel.rewardDetailsPeriod.collectAsState()
    val isRewardDetailsLoading by viewModel.isRewardDetailsLoading.collectAsState()
    
    val punishmentDetailsData by viewModel.punishmentDetailsData.collectAsState()
    val punishmentDetailsPeriod by viewModel.punishmentDetailsPeriod.collectAsState()
    val isPunishmentDetailsLoading by viewModel.isPunishmentDetailsLoading.collectAsState()
    
    // 获取所有分类数据用于奖励和惩罚详情的按钮网格
    val allCategories by viewModel.allCategories.collectAsState()
    
    // 下拉刷新状态
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    @Suppress("DEPRECATION")
    val swipeRefreshState = rememberSwipeRefreshState(isRefreshing)

    // 当页面首次加载或重新进入时，自动触发一次完整的刷新
    LaunchedEffect(Unit) {
        android.util.Log.d("StatsScreen", "🔄 页面加载触发数据更新")
        viewModel.onSwipeRefresh()
    }
    
    @Suppress("DEPRECATION")
    SwipeRefresh(
        state = swipeRefreshState,
        onRefresh = { viewModel.onSwipeRefresh() }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5)),
            contentPadding = PaddingValues(
                horizontal = horizontalPadding,
                vertical = verticalSpacing
            ),
            verticalArrangement = Arrangement.spacedBy(verticalSpacing)
        ) {
        item {
            UsageStatisticsCard(
                selectedPeriod = usageStatsPeriod,
                categoryUsageData = categoryUsageData,
                isLoading = isUsageStatsLoading,
                onPeriodSelected = { viewModel.setUsageStatsPeriod(it) },
                dimensions = dimensions
            )
        }
        
        item {
            GoalCompletionStatisticsCard(
                selectedPeriod = goalCompletionPeriod,
                categoryGoalCompletionData = categoryGoalCompletionData,
                isLoading = isGoalCompletionLoading,
                onPeriodSelected = { viewModel.setGoalCompletionPeriod(it) },
                dimensions = dimensions
            )
        }
        

        
        item {
            RewardDetailsCard(
                summaryData = rewardDetailsData,
                categories = allCategories,
                selectedPeriod = rewardDetailsPeriod,
                isLoading = isRewardDetailsLoading,
                onPeriodSelected = { viewModel.setRewardDetailsPeriod(it) },
                categoryRewardPunishmentData = categoryRewardPunishmentData,
                dimensions = dimensions
            )
        }
        
        item {
            PunishmentDetailsCard(
                summaryData = punishmentDetailsData,
                categories = allCategories,
                selectedPeriod = punishmentDetailsPeriod,
                isLoading = isPunishmentDetailsLoading,
                onPeriodSelected = { viewModel.setPunishmentDetailsPeriod(it) },
                categoryRewardPunishmentData = categoryRewardPunishmentData,
                dimensions = dimensions
            )
        }
        
        item {
            UsageChangeStatisticsCard(
                selectedPeriod = usageChangePeriod,
                appUsageChangeData = appUsageChangeData,
                isExpanded = isUsageChangeExpanded,
                isLoading = isUsageChangeLoading,
                onPeriodSelected = { viewModel.setUsageChangePeriod(it) },
                onToggleExpanded = { viewModel.toggleUsageChangeExpanded() },
                dimensions = dimensions
            )
        }
        
        item {
            DailyUsageRankingCard(
                selectedPeriod = dailyUsageRankingPeriod,
                appDailyUsageRankingData = appDailyUsageRankingData,
                isExpanded = isDailyUsageRankingExpanded,
                isLoading = isDailyUsageRankingLoading,
                onPeriodSelected = { viewModel.setDailyUsageRankingPeriod(it) },
                onToggleExpanded = { viewModel.toggleDailyUsageRankingExpanded() },
                dimensions = dimensions
            )
        }
    } // End LazyColumn
    } // End SwipeRefresh
}

@Composable
private fun UsageStatisticsCard(
    selectedPeriod: String,
    categoryUsageData: List<CategoryUsageData>,
    isLoading: Boolean,
    onPeriodSelected: (String) -> Unit,
    dimensions: com.offtime.app.ui.theme.ResponsiveDimensions
) {
    var showInfoDialog by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 标题和信息按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 标题和图标
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📊",
                        fontSize = dimensions.chartAxisTitleFontSize,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = stringResource(R.string.stats_usage_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333)
                    )
                }
                
                // 信息按钮
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF6B73FF))
                        .clickable { showInfoDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "i",
                        fontSize = dimensions.chartAxisTitleFontSize,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            
            // 时段选择按钮 - 独立一行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                CompactPeriodSelector(
                    selectedPeriod = selectedPeriod,
                    onPeriodSelected = onPeriodSelected
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 分类使用数据
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                CategoryUsageList(categoryUsageData = categoryUsageData)
            }
        }
    }
    
    // 信息对话框
    if (showInfoDialog) {
        InfoDialog(
            title = stringResource(R.string.info_usage_stats_title),
            content = stringResource(R.string.info_usage_stats_content),
            onDismiss = { showInfoDialog = false }
        )
    }
}

@Composable
private fun CompactPeriodSelector(
    selectedPeriod: String,
    onPeriodSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val periods = listOf("今日", "昨日", "本周", "本月", "总共")
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        periods.forEach { period ->
            CompactPeriodButton(
                text = DateLocalizer.formatPeriodLabel(context, period),
                isSelected = period == selectedPeriod,
                onClick = { onPeriodSelected(period) }
            )
        }
    }
}

@Composable
private fun CompactPeriodSelectorWithoutToday(
    selectedPeriod: String,
    onPeriodSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val periods = listOf("昨日", "本周", "本月", "总共")
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        periods.forEach { period ->
            CompactPeriodButton(
                text = DateLocalizer.formatPeriodLabel(context, period),
                isSelected = period == selectedPeriod,
                onClick = { onPeriodSelected(period) }
            )
        }
    }
}

@Composable
private fun PeriodSelector(
    selectedPeriod: String,
    onPeriodSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val periods = listOf("今日", "昨日", "本周", "本月")
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        periods.forEach { period ->
            PeriodButton(
                text = DateLocalizer.formatPeriodLabel(context, period),
                isSelected = period == selectedPeriod,
                onClick = { onPeriodSelected(period) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CompactPeriodButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val dimensions = LocalResponsiveDimensions
    val textColor = if (isSelected) Color(0xFF6B73FF) else Color(0xFF666666)
    
    Text(
        text = text,
        color = textColor,
        fontSize = dimensions.chartAxisTitleFontSize,
        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
private fun PeriodButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = LocalResponsiveDimensions
    val backgroundColor = if (isSelected) Color(0xFF6B73FF) else Color(0xFFF0F0F0)
    val textColor = if (isSelected) Color.White else Color(0xFF666666)
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = dimensions.chartAxisTitleFontSize,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun CategoryUsageList(
    categoryUsageData: List<CategoryUsageData>
) {
    val dimensions = LocalResponsiveDimensions
    if (categoryUsageData.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.no_data),
                color = Color.Gray,
                fontSize = dimensions.chartAxisTitleFontSize
            )
        }
        return
    }
    
    // 使用第一行显示第4个和第5个分类（如果存在）
    val visibleCategories = categoryUsageData.take(5)
    
    // 第一行：显示前4个分类
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        visibleCategories.take(4).forEach { categoryData ->
            CategoryUsageItem(
                categoryData = categoryData,
                modifier = Modifier.weight(1f)
            )
        }
        
        // 如果少于4个分类，用空白填充
        repeat(4 - visibleCategories.take(4).size) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
    
    // 第二行：显示第5个分类（如果存在）
    if (visibleCategories.size > 4) {
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CategoryUsageItem(
                categoryData = visibleCategories[4],
                modifier = Modifier.weight(1f)
            )
            // 其余位置留空
            repeat(3) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CategoryUsageItem(
    categoryData: CategoryUsageData,
    modifier: Modifier = Modifier
) {
    val dimensions = LocalResponsiveDimensions
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 圆形背景和图标
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(categoryData.color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = categoryData.emoji,
                fontSize = 24.sp
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 分类名称
        Text(
            text = DateLocalizer.getCategoryName(LocalContext.current, categoryData.category.name),
            fontSize = dimensions.chartAxisTitleFontSize,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF333333),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // 使用时间
        Text(
            text = formatUsageTime(categoryData.usageMinutes),
            fontSize = dimensions.chartAxisTitleFontSize,
            color = Color(0xFF666666),
            textAlign = TextAlign.Center
        )
    }
}

private fun formatUsageTime(minutes: Int): String {
    return when {
        minutes < 0 -> "0.0h" // 处理异常负数情况
        else -> {
            val hours = minutes / 60.0
            when {
                hours >= 10000.0 -> {
                    // 超过10000小时使用科学计数法
                    String.format("%.1e h", hours)
                }
                else -> {
                    String.format("%.1fh", hours)
                }
            }
        }
    }
}

private fun formatChangeMinutes(minutes: Int): String {
    val absMinutes = kotlin.math.abs(minutes)
    val hours = absMinutes / 60.0
    return when {
        hours >= 10000.0 -> {
            // 数值极大时用科学计数法
            String.format("%.1eh", hours)
        }
        else -> {
            // 统一使用带一位小数的小时
            String.format("%.1fh", hours)
        }
    }
}

@Composable
private fun GoalCompletionStatisticsCard(
    selectedPeriod: String,
    categoryGoalCompletionData: List<CategoryGoalCompletionData>,
    isLoading: Boolean,
    onPeriodSelected: (String) -> Unit,
    dimensions: com.offtime.app.ui.theme.ResponsiveDimensions
) {
    var showInfoDialog by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE5E5)), // 浅红色背景
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 标题和时段选择按钮在同一行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 标题和图标
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📊",
                        fontSize = dimensions.chartAxisTitleFontSize,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = stringResource(R.string.stats_goal_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333)
                    )
                }
                
                // 信息按钮
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF6B73FF))
                        .clickable { showInfoDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "i",
                        fontSize = dimensions.chartAxisTitleFontSize,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            
            // 时段选择按钮 - 独立一行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                CompactPeriodSelectorWithoutToday(
                    selectedPeriod = selectedPeriod,
                    onPeriodSelected = onPeriodSelected
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 分类目标完成数据
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                CategoryGoalCompletionList(categoryGoalCompletionData = categoryGoalCompletionData)
            }
        }
    }
    
    // 信息对话框
    if (showInfoDialog) {
        InfoDialog(
            title = stringResource(R.string.info_goal_stats_title),
            content = stringResource(R.string.info_goal_stats_content),
            onDismiss = { showInfoDialog = false }
        )
    }
}



@Composable
private fun CategoryGoalCompletionList(
    categoryGoalCompletionData: List<CategoryGoalCompletionData>
) {
    val dimensions = LocalResponsiveDimensions
    if (categoryGoalCompletionData.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.no_data),
                color = Color.Gray,
                fontSize = dimensions.chartAxisTitleFontSize
            )
        }
        return
    }
    
    // 使用第一行显示第4个和第5个分类（如果存在）
    val visibleCategories = categoryGoalCompletionData.take(5)
    
    // 第一行：显示前4个分类
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        visibleCategories.take(4).forEach { categoryData ->
            CategoryGoalCompletionItem(
                categoryData = categoryData,
                modifier = Modifier.weight(1f)
            )
        }
        
        // 如果少于4个分类，用空白填充
        repeat(4 - visibleCategories.take(4).size) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
    
    // 第二行：显示第5个分类（如果存在）
    if (visibleCategories.size > 4) {
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CategoryGoalCompletionItem(
                categoryData = visibleCategories[4],
                modifier = Modifier.weight(1f)
            )
            // 其余位置留空
            repeat(3) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}



@Composable
private fun CategoryGoalCompletionItem(
    categoryData: CategoryGoalCompletionData,
    modifier: Modifier = Modifier
) {
    val dimensions = LocalResponsiveDimensions
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 圆形背景和图标 - 保持与使用统计一致的样式
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(categoryData.color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = categoryData.emoji,
                fontSize = 24.sp
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 分类名称
        Text(
            text = DateLocalizer.getCategoryName(LocalContext.current, categoryData.category.name),
            fontSize = dimensions.chartAxisTitleFontSize,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF333333),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // 第一行：完成数:目标数 格式
        Text(
            text = "${categoryData.completedGoals}:${categoryData.totalGoals}",
            fontSize = dimensions.chartAxisTitleFontSize,
            color = Color(0xFF333333),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        // 第二行：完成百分比
        val completionPercentage = if (categoryData.totalGoals > 0) {
            (categoryData.completedGoals * 100) / categoryData.totalGoals
        } else {
            // 特殊情况：分母为0时显示100%（表示没有目标时默认完成）
            100
        }
        Text(
            text = "${completionPercentage}%",
            fontSize = dimensions.chartAxisTitleFontSize,
            color = if (completionPercentage >= 100) Color(0xFF4CAF50) else if (completionPercentage >= 50) Color(0xFFFF9800) else Color(0xFFF44336),
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}



@Composable
private fun UsageChangeStatisticsCard(
    selectedPeriod: String,
    appUsageChangeData: List<AppUsageChangeData>,
    isExpanded: Boolean,
    isLoading: Boolean,
    onPeriodSelected: (String) -> Unit,
    onToggleExpanded: () -> Unit,
    dimensions: com.offtime.app.ui.theme.ResponsiveDimensions
) {
    var showInfoDialog by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F8FF)), // 浅蓝色背景
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 标题和展开按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 标题和图标
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📊",
                        fontSize = dimensions.chartAxisTitleFontSize,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = stringResource(R.string.stats_usage_change_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333)
                    )
                }
                
                // 展开按钮和信息按钮
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                Text(
                    text = stringResource(R.string.show_all),
                    fontSize = 16.sp,
                    color = Color(0xFF6B73FF),
                    modifier = Modifier.clickable { onToggleExpanded() }
                )
                    
                    // 信息按钮
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF6B73FF))
                            .clickable { showInfoDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "i",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
            
            // 时段选择按钮 - 独立一行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                CompactPeriodSelector(
                    selectedPeriod = selectedPeriod,
                    onPeriodSelected = onPeriodSelected
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 使用变化数据
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                AppUsageChangeList(
                    appUsageChangeData = appUsageChangeData,
                    isExpanded = isExpanded
                )
            }
        }
    }
    
    // 信息对话框
    if (showInfoDialog) {
        InfoDialog(
            title = stringResource(R.string.info_usage_change_title),
            content = stringResource(R.string.info_usage_change_content),
            onDismiss = { showInfoDialog = false }
        )
    }
}

@Composable
private fun DailyUsageRankingCard(
    selectedPeriod: String,
    appDailyUsageRankingData: List<AppDailyUsageRankingData>,
    isExpanded: Boolean,
    isLoading: Boolean,
    onPeriodSelected: (String) -> Unit,
    onToggleExpanded: () -> Unit,
    dimensions: com.offtime.app.ui.theme.ResponsiveDimensions
) {
    var showInfoDialog by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F0FF)), // 浅紫色背景
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 标题和展开按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 标题和图标
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📊",
                        fontSize = dimensions.chartAxisTitleFontSize,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = stringResource(R.string.stats_usage_duration_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333)
                    )
                }
                
                // 展开按钮和信息按钮
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                Text(
                    text = stringResource(R.string.show_all),
                    fontSize = 16.sp,
                    color = Color(0xFF6B73FF),
                    modifier = Modifier.clickable { onToggleExpanded() }
                )
                    
                    // 信息按钮
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF6B73FF))
                            .clickable { showInfoDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "i",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
            
            // 时段选择按钮 - 独立一行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                CompactPeriodSelector(
                    selectedPeriod = selectedPeriod,
                    onPeriodSelected = onPeriodSelected
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 使用时长排序数据
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                AppDailyUsageRankingList(
                    appDailyUsageRankingData = appDailyUsageRankingData,
                    isExpanded = isExpanded
                )
            }
        }
    }
    
    // 信息对话框
    if (showInfoDialog) {
        InfoDialog(
            title = stringResource(R.string.info_usage_duration_title),
            content = stringResource(R.string.info_usage_duration_content),
            onDismiss = { showInfoDialog = false }
        )
    }
}

@Composable
private fun AppUsageChangeList(
    appUsageChangeData: List<AppUsageChangeData>,
    isExpanded: Boolean
) {
    val dimensions = LocalResponsiveDimensions
    if (appUsageChangeData.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.no_data),
                color = Color.Gray,
                fontSize = dimensions.chartAxisTitleFontSize
            )
        }
        return
    }
    
    // 分别显示增长和减少最多的应用
    val increasedApps = appUsageChangeData.filter { it.changeMinutes > 0 }
    val decreasedApps = appUsageChangeData.filter { it.changeMinutes < 0 }
    
    if (isExpanded) {
        // 展开模式：显示所有APP
        Column {
            if (increasedApps.isNotEmpty()) {
                Text(
                    text = "📈 ${stringResource(R.string.most_increased)}",
                    fontSize = dimensions.chartAxisTitleFontSize,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFF44336),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                increasedApps.forEach { changeData ->
                    AppUsageChangeItem(changeData = changeData, isIncrease = true)
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            if (decreasedApps.isNotEmpty()) {
                Text(
                    text = "📉 ${stringResource(R.string.most_decreased)}",
                    fontSize = dimensions.chartAxisTitleFontSize,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                decreasedApps.forEach { changeData ->
                    AppUsageChangeItem(changeData = changeData, isIncrease = false)
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    } else {
        // 折叠模式：左右布局显示前3名
        val topIncreasedApps = increasedApps.take(3)
        val topDecreasedApps = decreasedApps.take(3)
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 左侧：使用增长最多
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "📈",
                        fontSize = dimensions.chartAxisTitleFontSize,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        text = stringResource(R.string.most_increased),
                        fontSize = dimensions.chartAxisTitleFontSize,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFF44336)
                    )
                }
                
                if (topIncreasedApps.isNotEmpty()) {
                    topIncreasedApps.forEach { changeData ->
                        CompactAppUsageChangeItem(changeData = changeData, isIncrease = true)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                } else {
                    Text(
                        text = stringResource(R.string.no_data),
                        fontSize = dimensions.chartAxisTitleFontSize,
                        color = Color.Gray
                    )
                }
            }
            
            // 右侧：使用减少最多
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = "📉",
                        fontSize = dimensions.chartAxisTitleFontSize,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        text = stringResource(R.string.most_decreased),
                        fontSize = dimensions.chartAxisTitleFontSize,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4CAF50)
                    )
                }
                
                if (topDecreasedApps.isNotEmpty()) {
                    topDecreasedApps.forEach { changeData ->
                        CompactAppUsageChangeItem(changeData = changeData, isIncrease = false)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                } else {
                    Text(
                        text = stringResource(R.string.no_data),
                        fontSize = dimensions.chartAxisTitleFontSize,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun AppDailyUsageRankingList(
    appDailyUsageRankingData: List<AppDailyUsageRankingData>,
    isExpanded: Boolean
) {
    val dimensions = LocalResponsiveDimensions
    if (appDailyUsageRankingData.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.no_data),
                color = Color.Gray,
                fontSize = dimensions.chartAxisTitleFontSize
            )
        }
        return
    }
    
    // 显示的APP数量基于展开状态
    val displayApps = if (isExpanded) {
        appDailyUsageRankingData.filter { it.avgDailyMinutes > 0 }
    } else {
        appDailyUsageRankingData.filter { it.avgDailyMinutes > 0 }.take(5)
    }
    
    displayApps.forEachIndexed { index, rankingData ->
        AppDailyUsageRankingItem(
            rankingData = rankingData,
            rank = index + 1
        )
        if (index < displayApps.size - 1) {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AppUsageChangeItem(
    changeData: AppUsageChangeData,
    isIncrease: Boolean
) {
    val dimensions = LocalResponsiveDimensions
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // APP图标
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(changeData.color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = changeData.emoji,
                fontSize = dimensions.chartAxisTitleFontSize
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // APP名称和变化信息
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = DateLocalizer.localizeAppName(LocalContext.current, changeData.app.appName),
                fontSize = dimensions.chartAxisTitleFontSize,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF333333),
                maxLines = 1
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isIncrease) "▲" else "▼",
                    fontSize = dimensions.chartAxisTitleFontSize,
                    color = if (isIncrease) Color(0xFFF44336) else Color(0xFF4CAF50)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formatChangeMinutes(changeData.changeMinutes),
                    fontSize = dimensions.chartAxisTitleFontSize,
                    color = if (isIncrease) Color(0xFFF44336) else Color(0xFF4CAF50),
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        // 变化百分比
        Text(
            text = if (changeData.changePercent > 0) "+${changeData.changePercent.toInt()}%" else "${changeData.changePercent.toInt()}%",
            fontSize = dimensions.chartAxisTitleFontSize,
            color = if (isIncrease) Color(0xFFF44336) else Color(0xFF4CAF50),
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun CompactAppUsageChangeItem(
    changeData: AppUsageChangeData,
    isIncrease: Boolean
) {
    val dimensions = LocalResponsiveDimensions
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 简化的APP名称
        Text(
            text = DateLocalizer.localizeAppName(LocalContext.current, changeData.app.appName),
            fontSize = dimensions.chartAxisTitleFontSize,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF333333),
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
        
        Spacer(modifier = Modifier.width(4.dp))
        
        // 变化信息
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isIncrease) "▲" else "▼",
                fontSize = dimensions.chartAxisTitleFontSize,
                color = if (isIncrease) Color(0xFFF44336) else Color(0xFF4CAF50)
            )
            Text(
                text = formatChangeMinutes(changeData.changeMinutes),
                fontSize = dimensions.chartAxisTitleFontSize,
                color = if (isIncrease) Color(0xFFF44336) else Color(0xFF4CAF50),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AppDailyUsageRankingItem(
    rankingData: AppDailyUsageRankingData,
    rank: Int
) {
    val dimensions = LocalResponsiveDimensions
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 排名
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    when (rank) {
                        1 -> Color(0xFFFFD700) // 金色
                        2 -> Color(0xFFC0C0C0) // 银色
                        3 -> Color(0xFFCD7F32) // 铜色
                        else -> Color(0xFFE0E0E0) // 灰色
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = rank.toString(),
                fontSize = dimensions.chartAxisTitleFontSize,
                fontWeight = FontWeight.Bold,
                color = if (rank <= 3) Color.White else Color(0xFF666666)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // APP图标
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(rankingData.color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = rankingData.emoji,
                fontSize = dimensions.chartAxisTitleFontSize
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // APP名称（左侧）
        val appName = DateLocalizer.localizeAppName(LocalContext.current, rankingData.app.appName)
        Text(
            text = appName,
            fontSize = dimensions.chartAxisTitleFontSize,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF333333),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        
        // 类别名称（居中）
        val categoryName = DateLocalizer.getCategoryName(LocalContext.current, rankingData.categoryName)
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = categoryName,
                fontSize = dimensions.chartAxisTitleFontSize, // 与应用名字体大小相同
                fontWeight = FontWeight.Normal,
                color = Color(0xFF666666),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        // 平均每日使用时间
        Text(
            text = formatUsageTime(rankingData.avgDailyMinutes),
            fontSize = dimensions.chartAxisTitleFontSize,
            color = Color(0xFF666666),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun InfoDialog(
    title: String,
    content: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = content,
                    fontSize = 16.sp,
                    color = Color(0xFF666666),
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text(
                            text = stringResource(R.string.dialog_confirm),
                            color = Color(0xFF6B73FF),
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}