package com.offtime.app.ui.debug

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.offtime.app.R
import com.offtime.app.service.UsageStatsCollectorService
import com.offtime.app.utils.PermissionUtils
import com.offtime.app.utils.UsageStatsPermissionHelper
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.offtime.app.manager.UsageStatsManager as AppUsageStatsManager
import javax.inject.Inject

/**
 * 数据收集调试页面
 * 
 * 用于诊断为什么应用使用数据没有被正确收集
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugDataCollectionScreen(
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var events by remember { mutableStateOf<List<RecentEvent>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var hasUsagePermission by remember { mutableStateOf(false) }
    var hasQueryPackagesPermission by remember { mutableStateOf(false) }
    var isServiceRunning by remember { mutableStateOf(false) }
    
    // 检查权限和服务状态
    LaunchedEffect(Unit) {
        hasUsagePermission = UsageStatsPermissionHelper.hasUsageStatsPermission(context)
        hasQueryPackagesPermission = PermissionUtils.hasQueryAllPackagesPermission(context)
        isServiceRunning = isUsageStatsCollectorServiceRunning(context)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.debug_data_collection_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.debug_back))
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
            // 系统状态检查
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "系统状态检查",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        StatusItem(
                            label = "使用统计权限",
                            status = hasUsagePermission,
                            statusText = if (hasUsagePermission) "✅ 已授权" else "❌ 未授权"
                        )
                        
                        StatusItem(
                            label = "查询应用权限",
                            status = hasQueryPackagesPermission || Build.VERSION.SDK_INT < Build.VERSION_CODES.R,
                            statusText = when {
                                Build.VERSION.SDK_INT < Build.VERSION_CODES.R -> "✅ 无需此权限"
                                hasQueryPackagesPermission -> "✅ 已授权"
                                else -> "❌ 未授权"
                            }
                        )
                        
                        StatusItem(
                            label = "数据收集服务",
                            status = isServiceRunning,
                            statusText = if (isServiceRunning) "✅ 正在运行" else "❌ 未运行"
                        )
                        
                        Text(
                            text = "设备厂商: ${Build.MANUFACTURER}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Android版本: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // 操作按钮
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (!hasUsagePermission) {
                                UsageStatsPermissionHelper.openUsageAccessSettings(context)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !hasUsagePermission
                    ) {
                        Text("授权权限")
                    }
                    
                    Button(
                        onClick = {
                            startUsageStatsService(context)
                            isServiceRunning = isUsageStatsCollectorServiceRunning(context)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = hasUsagePermission
                    ) {
                        Text("启动服务")
                    }
                }
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            pullEventsNow(context)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = hasUsagePermission && isServiceRunning
                    ) {
                        Text("拉取事件")
                    }
                    
                    Button(
                        onClick = {
                            if (hasUsagePermission) {
                                isLoading = true
                                coroutineScope.launch {
                                    events = getRecentUsageEvents(context)
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = hasUsagePermission && !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("获取最近30分钟事件")
                        }
                    }
                }
            }
            
            // 事件列表
            item {
                if (events.isNotEmpty()) {
                    Text(
                        text = "最近事件 (${events.size}个)",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                } else if (!isLoading && hasUsagePermission) {
                    Text(
                        text = "📄 暂无会话数据",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            
            // 事件列表项
            items(events) { event ->
                EventItem(event = event)
            }
        }
    }
}

data class RecentEvent(
    val packageName: String,
    val appName: String,
    val eventType: String,
    val timestamp: Long,
    val isChrome: Boolean
)

/**
 * 获取最近的使用事件
 */
private suspend fun getRecentUsageEvents(context: Context): List<RecentEvent> = withContext(Dispatchers.IO) {
    val events = mutableListOf<RecentEvent>()
    
    try {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val begin = now - 30 * 60 * 1000 // 最近30分钟
        
        val usageEvents = usageStatsManager.queryEvents(begin, now)
        
        while (usageEvents.hasNextEvent()) {
            val event = UsageEvents.Event()
            usageEvents.getNextEvent(event)
            
            // 只关注应用启动和停止事件
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED || 
                event.eventType == UsageEvents.Event.ACTIVITY_PAUSED) {
                
                val isChrome = event.packageName.contains("chrome", ignoreCase = true) ||
                              event.packageName.contains("browser", ignoreCase = true) ||
                              event.packageName == "com.android.chrome" ||
                              event.packageName == "com.google.android.chrome"
                
                events.add(
                    RecentEvent(
                        packageName = event.packageName,
                        appName = when {
                            event.packageName.contains("launcher") -> "桌面"
                            event.packageName.contains("systemui") -> "系统界面"
                            event.packageName.contains("settings") -> "设置"
                            else -> event.packageName.split(".").lastOrNull() ?: event.packageName
                        },
                        eventType = when (event.eventType) {
                            UsageEvents.Event.ACTIVITY_RESUMED -> "启动"
                            UsageEvents.Event.ACTIVITY_PAUSED -> "停止"
                            else -> "未知(${event.eventType})"
                        },
                        timestamp = event.timeStamp,
                        isChrome = isChrome
                    )
                )
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("DebugDataCollection", "获取使用事件失败", e)
    }
    
    return@withContext events.sortedByDescending { it.timestamp }
}

/**
 * 事件项组件
 */
@Composable
private fun EventItem(event: RecentEvent) {
    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(event.timestamp)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (event.isChrome) Color(0xFFE3F2FD) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = event.appName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (event.isChrome) Color(0xFF1976D2) else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${event.eventType} $timeStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (event.isChrome) {
                Text(
                    text = "🔍 Chrome事件已检测到",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF1976D2),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Text(
                text = event.packageName,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}

/**
 * 检查UsageStatsCollectorService是否正在运行
 */
private fun isUsageStatsCollectorServiceRunning(context: Context): Boolean {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val services = activityManager.getRunningServices(Int.MAX_VALUE)
    for (service in services) {
        if (UsageStatsCollectorService::class.java.name == service.service.className) {
            return true
        }
    }
    return false
}

/**
 * 启动UsageStatsCollectorService
 */
private fun startUsageStatsService(context: Context) {
    val intent = Intent(context, UsageStatsCollectorService::class.java).apply {
        action = UsageStatsCollectorService.ACTION_START_COLLECTION
    }
    context.startService(intent)
}

/**
 * 拉取事件
 */
private fun pullEventsNow(context: Context) {
    val intent = Intent(context, UsageStatsCollectorService::class.java).apply {
        action = UsageStatsCollectorService.ACTION_PULL_EVENTS
    }
    context.startService(intent)
}

/**
 * 状态项组件
 */
@Composable
private fun StatusItem(
    label: String,
    status: Boolean,
    statusText: String
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
        Text(
            text = statusText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (status) Color(0xFF4CAF50) else Color(0xFFFF5722)
        )
    }
} 