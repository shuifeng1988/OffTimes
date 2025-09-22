package com.offtime.app.ui.debug

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
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
import androidx.compose.ui.res.stringResource
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import com.offtime.app.R
import com.offtime.app.ui.debug.viewmodel.DebugSessionsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugSessionsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: DebugSessionsViewModel = hiltViewModel()
) {
    val sessions by viewModel.sessions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val appInfos by viewModel.appInfos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val showMergedData by viewModel.showMergedData.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadSessions()
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("⏱️ ${stringResource(R.string.debug_sessions_screen_title)}") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.debug_back))
                }
            }
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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 操作按钮
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.loadSessions() },
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading
                        ) {
                            Text("刷新")
                        }
                        
                        Button(
                            onClick = { viewModel.toggleDataMode() },
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (showMergedData) Color(0xFF4CAF50) else Color(0xFF2196F3)
                            )
                        ) {
                            Text(if (showMergedData) "合并数据" else "原始数据")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { viewModel.cleanDuplicateSessions() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFFFA500)) // Orange color
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("清理重复记录")
                    }
                }
                
                // 统计信息
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "📊 统计信息",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("总会话数: ${sessions.size}", fontSize = 14.sp)
                                val totalDuration = sessions.sumOf { it.durationSec }
                                Text("总时长: ${totalDuration / 60}分${totalDuration % 60}秒", fontSize = 14.sp)
                            }
                            Column {
                                val uniqueApps = sessions.map { it.pkgName }.distinct().size
                                val avgDuration = if (sessions.isNotEmpty()) sessions.sumOf { it.durationSec } / sessions.size else 0
                                Text("涉及应用: $uniqueApps 个", fontSize = 14.sp)
                                Text("平均时长: ${avgDuration / 60}分${avgDuration % 60}秒", fontSize = 14.sp)
                            }
                        }
                    }
                }
                
                // 数据列表
                if (sessions.isEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "📂 暂无会话数据",
                                fontSize = 16.sp,
                                color = Color(0xFFD32F2F)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 按日期分组显示
                        val groupedSessions = sessions.groupBy { 
                            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.startTime))
                        }.toList().sortedByDescending { it.first }
                        
                        groupedSessions.forEach { (date, sessionList) ->
                            // 日期分组头部
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1976D2))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "📅 $date",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "${sessionList.size} 个会话",
                                            fontSize = 14.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                            
                            // 该日期的会话
                            itemsIndexed(sessionList.sortedByDescending { it.startTime }) { index, session ->
                                val category = categories.find { it.id == session.catId }
                                val categoryColor = getCategoryDebugColor(session.catId)
                                
                                SessionCard(
                                    session = session,
                                    category = category,
                                    categoryColor = categoryColor,
                                    recordNumber = index + 1,
                                    appInfos = appInfos
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionCard(
    session: com.offtime.app.data.entity.AppSessionUserEntity,
    category: com.offtime.app.data.entity.AppCategoryEntity?,
    categoryColor: Color,
    recordNumber: Int,
    appInfos: List<com.offtime.app.data.entity.AppInfoEntity>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 编号和颜色指示器
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(48.dp)
            ) {
                // 编号
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(categoryColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = recordNumber.toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 颜色指示器
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(categoryColor, CircleShape)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 应用图标 - 使用真实的应用图标
            AppIcon(
                packageName = session.pkgName,
                appName = getRealAppName(session.pkgName, appInfos),
                modifier = Modifier.size(40.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 应用信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 应用名称
                Text(
                    text = getRealAppName(session.pkgName, appInfos),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF212121)
                )
                
                // 分类名称
                Text(
                    text = category?.name ?: "未知分类",
                    fontSize = 12.sp,
                    color = categoryColor,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 时间信息
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "⏱️ ${session.durationSec / 60}分${session.durationSec % 60}秒",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                }
                
                // 时间段
                Text(
                    text = "${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(session.startTime))} - ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(session.endTime))}",
                    fontSize = 11.sp,
                    color = Color(0xFF999999)
                )
            }
        }
    }
}

@Composable
private fun AppIcon(
    packageName: String,
    appName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // 尝试获取应用图标
    var appIcon by remember { mutableStateOf<androidx.compose.ui.graphics.painter.Painter?>(null) }
    
    LaunchedEffect(packageName) {
        try {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            val bitmap = drawable.toBitmap(96, 96) // 创建96x96的bitmap
            appIcon = BitmapPainter(bitmap.asImageBitmap())
        } catch (e: Exception) {
            appIcon = null
        }
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (appIcon == null) MaterialTheme.colorScheme.primaryContainer 
                else Color.Transparent
            ),
        contentAlignment = Alignment.Center
    ) {
        if (appIcon != null) {
            Image(
                painter = appIcon!!,
                contentDescription = appName,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // 回退到字母占位符
            Text(
                text = appName.take(1).uppercase(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

// 根据分类ID获取颜色
private fun getCategoryDebugColor(catId: Int): Color {
    val colors = listOf(
        Color(0xFF2196F3), // 蓝色
        Color(0xFF4CAF50), // 绿色
        Color(0xFFFF9800), // 橙色
        Color(0xFF9C27B0), // 紫色
        Color(0xFFF44336), // 红色
        Color(0xFF00BCD4), // 青色
        Color(0xFFFFEB3B), // 黄色
        Color(0xFF795548), // 棕色
        Color(0xFF607D8B), // 蓝灰色
        Color(0xFFE91E63)  // 粉色
    )
    return colors[catId % colors.size]
}

// 获取真实应用名称（优先从app_info_users表获取，否则使用智能识别）
private fun getRealAppName(packageName: String, appInfos: List<com.offtime.app.data.entity.AppInfoEntity>): String {
    // 首先尝试从app_info_users表获取真实的应用名称
    val appInfo = appInfos.find { it.packageName == packageName }
    if (appInfo != null && appInfo.appName.isNotEmpty()) {
        return appInfo.appName
    }
    
    // 如果找不到，使用智能识别作为备选方案
    return getDisplayAppNameFromPackage(packageName)
}

// 智能显示应用名称（从包名识别）
private fun getDisplayAppNameFromPackage(packageName: String): String {
    return when {
        packageName.contains("youtube", ignoreCase = true) -> "YouTube"
        packageName.contains("chrome", ignoreCase = true) -> "Chrome"
        packageName.contains("maps", ignoreCase = true) -> "Google Maps"
        packageName.contains("gmail", ignoreCase = true) -> "Gmail"
        packageName.contains("whatsapp", ignoreCase = true) -> "WhatsApp"
        packageName.contains("wechat", ignoreCase = true) -> "微信"
        packageName.contains("qq", ignoreCase = true) -> "QQ"
        packageName.contains("taobao", ignoreCase = true) -> "淘宝"
        packageName.contains("alipay", ignoreCase = true) -> "支付宝"
        packageName.contains("douyin", ignoreCase = true) -> "抖音"
        packageName.contains("bilibili", ignoreCase = true) -> "哔哩哔哩"
        packageName.contains("netease", ignoreCase = true) -> "网易云音乐"
        packageName.contains("tencent", ignoreCase = true) -> "腾讯应用"
        packageName.contains("baidu", ignoreCase = true) -> "百度应用"
        packageName.contains("xiaomi", ignoreCase = true) -> "小米应用"
        packageName.contains("huawei", ignoreCase = true) -> "华为应用"
        packageName.contains("oppo", ignoreCase = true) -> "OPPO应用"
        packageName.contains("vivo", ignoreCase = true) -> "vivo应用"
        packageName.contains("samsung", ignoreCase = true) -> "三星应用"
        else -> {
            // 从包名中提取应用名称
            val parts = packageName.split(".")
            parts.lastOrNull()?.replaceFirstChar { it.uppercase() } ?: packageName
        }
    }
} 