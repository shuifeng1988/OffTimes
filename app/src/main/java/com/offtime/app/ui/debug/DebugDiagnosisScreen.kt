package com.offtime.app.ui.debug

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.offtime.app.ui.debug.viewmodel.DebugDiagnosisViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugDiagnosisScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: DebugDiagnosisViewModel = hiltViewModel()
) {
    val diagnosisResult by viewModel.diagnosisResult.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.runDiagnosis()
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
            Text(
                text = "问题诊断",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            TextButton(onClick = onNavigateBack) {
                Text("返回")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 操作按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.runDiagnosis() },
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            ) {
                Text("重新诊断")
            }
            
            Button(
                onClick = { viewModel.fixYesterdayRewards() },
                modifier = Modifier.weight(1f),
                enabled = !isLoading && diagnosisResult.canFixYesterday
            ) {
                Text("修复昨天奖惩")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 诊断结果
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "诊断报告",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // 总体状态
                        DiagnosisStatusCard(
                            title = "总体状态",
                            status = diagnosisResult.overallStatus,
                            icon = when (diagnosisResult.overallStatus) {
                                "正常" -> Icons.Default.CheckCircle
                                "警告" -> Icons.Default.Warning
                                else -> Icons.Default.Error
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // 详细诊断信息
                        Text(
                            text = diagnosisResult.detailedReport,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 设备兼容性检查
                        DeviceCompatibilityCard()
                    }
                }
            }
        }
    }
}

@Composable
fun DiagnosisStatusCard(
    title: String,
    status: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (status) {
                "正常" -> MaterialTheme.colorScheme.primaryContainer
                "警告" -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = status,
                modifier = Modifier.size(24.dp),
                tint = when (status) {
                    "正常" -> MaterialTheme.colorScheme.primary
                    "警告" -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.error
                }
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = status,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun DeviceCompatibilityCard() {
    val context = LocalContext.current
    var deviceInfo by remember { mutableStateOf("加载中...") }
    var permissionStatus by remember { mutableStateOf("检查中...") }
    var usageStatsTest by remember { mutableStateOf("测试中...") }
    
    LaunchedEffect(Unit) {
        // 获取设备信息
        deviceInfo = buildString {
            append("制造商: ${android.os.Build.MANUFACTURER}\n")
            append("型号: ${android.os.Build.MODEL}\n")
            append("Android版本: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})\n")
            append("品牌: ${android.os.Build.BRAND}")
        }
        
        // 检查权限状态
        val hasPermission = com.offtime.app.utils.PermissionUtils.hasUsageStatsPermission(context)
        permissionStatus = if (hasPermission) "✅ 已授权" else "❌ 未授权"
        
        // 测试UsageStats功能
        try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? android.app.usage.UsageStatsManager
            if (usageStatsManager != null) {
                val now = System.currentTimeMillis()
                val events = usageStatsManager.queryEvents(now - 3600000, now) // 最近1小时
                var eventCount = 0
                while (events.hasNextEvent()) {
                    val event = android.app.usage.UsageEvents.Event()
                    events.getNextEvent(event)
                    eventCount++
                }
                usageStatsTest = "✅ 功能正常 (获取到${eventCount}个事件)"
            } else {
                usageStatsTest = "❌ 无法获取UsageStatsManager服务"
            }
        } catch (e: Exception) {
            usageStatsTest = "❌ 测试失败: ${e.message}"
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "📱 设备兼容性检查",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Text(
                text = "设备信息:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = deviceInfo,
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "使用统计权限: $permissionStatus",
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            Text(
                text = "UsageStats功能: $usageStatsTest",
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Samsung设备特殊提示
            if (android.os.Build.MANUFACTURER.equals("samsung", ignoreCase = true)) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "⚠️ Samsung设备注意事项:\n" +
                               "1. 确保在「设置→应用→特殊访问权限→使用情况访问权限」中授权本应用\n" +
                               "2. 检查「设备维护→电池→应用省电管理」中是否限制了本应用\n" +
                               "3. 在「设置→应用→本应用→电池」中设置为「不限制」",
                        fontSize = 11.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
} 