package com.offtime.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import com.offtime.app.utils.DateLocalizer
import androidx.compose.ui.res.stringResource
import com.offtime.app.R
import com.offtime.app.ui.debug.viewmodel.DebugAppsViewModel
import com.offtime.app.ui.debug.viewmodel.DailyAppUsageData
import com.offtime.app.ui.debug.viewmodel.DailyUsageByDate
import com.offtime.app.ui.debug.viewmodel.DailyVirtualUsageData
import java.text.SimpleDateFormat
import java.util.*

/**
 * 每日使用汇总界面
 * 
 * 功能说明:
 * - 独立的每日应用使用汇总界面
 * - 按天排列显示应用使用情况
 * - 使用真实的应用图标美化界面
 * - 显示使用时长、分类等详细信息
 * - 支持日期选择查看历史数据
 * 
 * 数据来源: summary_usage_user 表
 * 
 * UI特性:
 * - 真实应用图标 + 首字母占位符
 * - 按使用时长排序
 * - 美观的卡片式布局
 * - 汇总统计信息
 * - 日期导航功能
 * 
 * @param onNavigateBack 返回上级页面的回调
 * @param viewModel 应用信息调试的ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyUsageScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: DebugAppsViewModel = hiltViewModel()
) {
    val dailyUsageByDate by viewModel.dailyUsageByDate.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    // 加载多天数据
    LaunchedEffect(Unit) {
        viewModel.loadMultipleDaysUsageData(14) // 加载过去14天的数据
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "📊 每日使用汇总",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadMultipleDaysUsageData(14) },
                        enabled = !isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "刷新",
                            tint = if (isLoading) Color.Gray else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // 按天显示使用数据
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (dailyUsageByDate.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "没有找到使用数据",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(dailyUsageByDate) { dayData ->
                        DayUsageSection(dayData = dayData)
                    }
                }
            }
        }
    }
}

/**
 * 每天使用数据区域组件
 */
@Composable
private fun DayUsageSection(dayData: DailyUsageByDate) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 日期标题
            DayHeader(dayData = dayData)
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 线上应用列表
            if (dayData.apps.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = "线上应用",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "线上应用",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                dayData.apps.forEach { appData ->
                    DailyUsageItem(usageData = appData)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
            // 线下活动列表
            if (dayData.virtualApps.isNotEmpty()) {
                if (dayData.apps.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                        contentDescription = "线下活动",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.offline_label),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                
                dayData.virtualApps.forEach { virtualData ->
                    VirtualUsageItem(usageData = virtualData)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

/**
 * 日期标题组件
 */
@Composable
private fun DayHeader(dayData: DailyUsageByDate) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：日期和星期
            Column {
                Text(
                    text = dayData.date,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = dayData.weekday,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 右侧：汇总信息
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${dayData.totalApps}个应用",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${String.format("%.1f", dayData.totalMinutes / 60f)}小时",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
        
        // 统计条
        if (dayData.virtualTotalMinutes > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 线上应用统计
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = "线上应用",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${stringResource(R.string.online_label)} ${dayData.totalApps}个",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // 线下活动统计
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                        contentDescription = "线下活动",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${stringResource(R.string.offline_label)} ${String.format("%.1f", dayData.virtualTotalMinutes / 60f)}h",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}



/**
 * 每日使用项目组件 - 紧凑版本
 */
@Composable
private fun DailyUsageItem(usageData: DailyAppUsageData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 应用图标
        AppIcon(
            packageName = usageData.packageName,
            appName = usageData.appName,
            modifier = Modifier.size(40.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // 应用信息
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = usageData.appName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 分类标签
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = usageData.categoryName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(6.dp))
                
                // 启动次数
                Text(
                    text = "${usageData.sessionCount}次",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // 使用时长
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${usageData.totalMinutes}分钟",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "${String.format("%.1f", usageData.totalMinutes / 60f)}小时",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 虚拟应用使用项目组件
 */
@Composable
private fun VirtualUsageItem(usageData: DailyVirtualUsageData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 线下活动图标
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                contentDescription = "线下活动",
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // 活动信息
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (usageData.categoryName.contains("线下")) {
                    // 线下活动使用本地化的显示名称
                    DateLocalizer.getCategoryName(LocalContext.current, usageData.categoryName)
                } else {
                    "${usageData.categoryName}活动"
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 活动类型标签
                Surface(
                    color = MaterialTheme.colorScheme.secondary,
                    shape = RoundedCornerShape(8.dp)
                ) {
                                    Text(
                    text = stringResource(R.string.offline_label),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                )
                }
                
                Spacer(modifier = Modifier.width(6.dp))
                
                // 活动次数
                Text(
                    text = "${usageData.sessionCount}次",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // 活动时长
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${usageData.totalMinutes}分钟",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = "${String.format("%.1f", usageData.totalMinutes / 60f)}小时",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 应用图标组件
 */
@Composable
private fun AppIcon(
    packageName: String,
    appName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var appIcon by remember { mutableStateOf<androidx.compose.ui.graphics.painter.Painter?>(null) }
    
    // 异步加载应用图标
    LaunchedEffect(packageName) {
        try {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            val bitmap = drawable.toBitmap(128, 128) // 更高分辨率的图标
            appIcon = BitmapPainter(bitmap.asImageBitmap())
        } catch (e: Exception) {
            appIcon = null
        }
    }
    
    // 图标容器
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (appIcon == null) MaterialTheme.colorScheme.primaryContainer 
                else Color.Transparent
            ),
        contentAlignment = Alignment.Center
    ) {
        if (appIcon != null) {
            // 显示真实应用图标
            Image(
                painter = appIcon!!,
                contentDescription = appName,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // 显示首字母占位符
            Text(
                text = appName.take(1).uppercase(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

 