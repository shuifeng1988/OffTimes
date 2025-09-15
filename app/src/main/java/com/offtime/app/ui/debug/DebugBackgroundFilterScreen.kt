package com.offtime.app.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.offtime.app.ui.debug.viewmodel.DebugBackgroundFilterViewModel
import com.offtime.app.utils.BackgroundAppFilterUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugBackgroundFilterScreen(
    onNavigateBack: () -> Unit,
    viewModel: DebugBackgroundFilterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    @Suppress("UNUSED_VARIABLE")
    val context = LocalContext.current

    // 页面加载时获取数据
    LaunchedEffect(Unit) {
        viewModel.loadFilterStats()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("智能后台过滤调试") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 过滤统计概览
            item {
                FilterStatsOverview(uiState.filterStats)
            }
            
            // 常驻后台应用列表
            item {
                BackgroundAppsSection(uiState.backgroundApps)
            }
            
            // 今日过滤详情
            item {
                Text(
                    text = "📊 今日过滤详情",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                items(uiState.filteredSessions) { session ->
                    FilteredSessionCard(session)
                }
            }
        }
    }
}

@Composable
fun FilterStatsOverview(stats: DebugBackgroundFilterViewModel.FilterStats?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "🎯 过滤统计概览",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            if (stats != null) {
                                 Row(
                     modifier = Modifier.fillMaxWidth(),
                     horizontalArrangement = Arrangement.SpaceEvenly
                 ) {
                     FilterStatItem("总会话", stats.totalSessions.toString())
                     FilterStatItem("已过滤", stats.filteredSessions.toString())
                     FilterStatItem("节省时间", "${stats.timeSavedMinutes}分钟")
                 }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                LinearProgressIndicator(
                    progress = if (stats.totalSessions > 0) stats.filteredSessions.toFloat() / stats.totalSessions.toFloat() else 0f,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text(
                    text = "过滤率: ${if (stats.totalSessions > 0) (stats.filteredSessions * 100 / stats.totalSessions) else 0}%",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                Text("加载中...")
            }
        }
    }
}

@Composable
fun FilterStatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun BackgroundAppsSection(backgroundApps: List<DebugBackgroundFilterViewModel.BackgroundAppInfo>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "🏠 常驻后台应用",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            if (backgroundApps.isNotEmpty()) {
                backgroundApps.forEach { app ->
                    BackgroundAppItem(app)
                }
            } else {
                Text(
                    text = "未检测到常驻后台应用",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun BackgroundAppItem(app: DebugBackgroundFilterViewModel.BackgroundAppInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
                 // 过滤级别指示器
         val (color, icon) = when (app.filterLevel) {
             BackgroundAppFilterUtils.FilterLevel.HIGH -> Color.Red to Icons.Default.Warning
             BackgroundAppFilterUtils.FilterLevel.MEDIUM -> Color(0xFFFFA500) to Icons.Default.Info // Orange color
             BackgroundAppFilterUtils.FilterLevel.LOW -> Color.Green to Icons.Default.CheckCircle
         }
        
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.appName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${app.packageName} • ${app.filterLevel}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            text = "${app.todayFilteredMinutes}min",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun FilteredSessionCard(session: DebugBackgroundFilterViewModel.FilteredSessionInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = session.appName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                
                                 val filterLevel = session.filterLevel
                 val (levelColor, levelText) = when (filterLevel) {
                     BackgroundAppFilterUtils.FilterLevel.HIGH -> Color.Red to "高级过滤"
                     BackgroundAppFilterUtils.FilterLevel.MEDIUM -> Color(0xFFFFA500) to "中级过滤" // Orange color
                     BackgroundAppFilterUtils.FilterLevel.LOW -> Color.Green to "轻度过滤"
                 }
                
                Surface(
                    color = levelColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = levelText,
                        fontSize = 10.sp,
                        color = levelColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "时间: ${session.timeRange}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "原始: ${session.originalDurationMinutes}分钟",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "调整后: ${session.adjustedDurationMinutes}分钟",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            if (session.originalDurationMinutes != session.adjustedDurationMinutes) {
                Spacer(modifier = Modifier.height(4.dp))
                
                val savedMinutes = session.originalDurationMinutes - session.adjustedDurationMinutes
                Text(
                    text = "✂️ 节省: ${savedMinutes}分钟 (${(savedMinutes * 100 / session.originalDurationMinutes)}%)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (session.isBackgroundWakeup) {
                Spacer(modifier = Modifier.height(4.dp))
                
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "⚠️ 检测为后台唤醒",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
} 