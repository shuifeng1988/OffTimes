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
import com.offtime.app.service.UsageStatsCollectorService
import com.offtime.app.ui.debug.viewmodel.DebugRealTimeStatsViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

/**
 * 实时统计调试页面
 * 
 * 用于验证新的实时统计和智能合并功能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugRealTimeStatsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: DebugRealTimeStatsViewModel = hiltViewModel()
) {
    @Suppress("UNUSED_VARIABLE")
    val context = LocalContext.current
    
    // 状态收集
    val todaySessions by viewModel.todaySessions.collectAsState()
    val recentMergedSessions by viewModel.recentMergedSessions.collectAsState()
    val isCollectorServiceRunning by viewModel.isCollectorServiceRunning.collectAsState()
    val realtimeStatsEnabled by viewModel.realtimeStatsEnabled.collectAsState()
    val lastActiveAppsCheck by viewModel.lastActiveAppsCheck.collectAsState()
    
    // 自动刷新状态
    var autoRefresh by remember { mutableStateOf(true) }
    
    // 定期刷新数据
    LaunchedEffect(autoRefresh) {
        while (autoRefresh) {
            viewModel.refreshData()
            delay(5000) // 每5秒刷新一次
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 标题栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
                Text(
                    text = "实时统计调试",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Row {
                IconButton(
                    onClick = { autoRefresh = !autoRefresh }
                ) {
                    Icon(
                        if (autoRefresh) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (autoRefresh) "停止自动刷新" else "开始自动刷新"
                    )
                }
                IconButton(
                    onClick = { viewModel.refreshData() }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "手动刷新")
                }
            }
        }
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 服务状态卡片
            item {
                ServiceStatusCard(
                    isCollectorRunning = isCollectorServiceRunning,
                    realtimeStatsEnabled = realtimeStatsEnabled,
                    lastActiveAppsCheck = lastActiveAppsCheck,
                    onStartService = { viewModel.startCollectorService() },
                    onStopService = { viewModel.stopCollectorService() },
                    onPullEvents = { viewModel.pullEventsNow() }
                )
            }
            
            // 今日会话统计
            item {
                TodaySessionsCard(
                    sessions = todaySessions,
                    onRefresh = { viewModel.refreshTodaySessions() }
                )
            }
            
            // 智能合并效果展示
            item {
                SmartMergeCard(
                    mergedSessions = recentMergedSessions,
                    onRefresh = { viewModel.refreshMergedSessions() }
                )
            }
            
            // 实时统计测试工具
            item {
                RealTimeTestCard(
                    onSimulateUsage = { pkgName -> viewModel.simulateAppUsage(pkgName) },
                    onTestMerge = { viewModel.testSmartMerge() }
                )
            }
        }
    }
}

@Composable
private fun ServiceStatusCard(
    isCollectorRunning: Boolean,
    realtimeStatsEnabled: Boolean,
    lastActiveAppsCheck: String,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onPullEvents: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "📊 服务运行状态",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 服务状态指示器
            StatusRow(
                label = "数据收集服务",
                isRunning = isCollectorRunning,
                runningText = "运行中",
                stoppedText = "已停止"
            )
            
            StatusRow(
                label = "实时统计功能",
                isRunning = realtimeStatsEnabled,
                runningText = "已启用",
                stoppedText = "未启用"
            )
            
            if (lastActiveAppsCheck.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "最后检查时间:",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = lastActiveAppsCheck,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isCollectorRunning) {
                    Button(
                        onClick = onStopService,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF5722)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("停止服务")
                    }
                } else {
                    Button(
                        onClick = onStartService,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("启动服务")
                    }
                }
                
                OutlinedButton(
                    onClick = onPullEvents,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("拉取事件")
                }
            }
        }
    }
}

@Composable
private fun StatusRow(
    label: String,
    isRunning: Boolean,
    runningText: String,
    stoppedText: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = if (isRunning) Color(0xFF4CAF50) else Color(0xFFFF5722),
                        shape = RoundedCornerShape(4.dp)
                    )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isRunning) runningText else stoppedText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (isRunning) Color(0xFF4CAF50) else Color(0xFFFF5722)
            )
        }
    }
}

@Composable
private fun TodaySessionsCard(
    sessions: List<Any>, // 替换为实际的Session数据类型
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📱 今日会话记录",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "刷新")
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "总会话数: ${sessions.size}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            
            // 这里可以添加更详细的会话列表展示
            if (sessions.isEmpty()) {
                Text(
                    text = "暂无今日会话记录",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun SmartMergeCard(
    mergedSessions: List<Any>, // 替换为实际的MergedSession数据类型
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔗 智能合并效果",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "刷新")
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "最近合并次数: ${mergedSessions.size}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = "✅ 连续使用同一应用时自动合并会话记录",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun RealTimeTestCard(
    onSimulateUsage: (String) -> Unit,
    onTestMerge: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "🧪 测试工具",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "测试实时统计和智能合并功能:",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onSimulateUsage("com.offtime.app") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Apps, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("模拟OffTimes使用")
                }
                
                OutlinedButton(
                    onClick = onTestMerge,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.MergeType, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("测试智能合并")
                }
            }
        }
    }
} 