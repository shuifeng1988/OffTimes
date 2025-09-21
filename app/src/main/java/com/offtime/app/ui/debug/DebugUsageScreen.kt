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
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.res.stringResource
import com.offtime.app.R
import com.offtime.app.ui.debug.viewmodel.DebugUsageViewModel
import com.offtime.app.ui.debug.viewmodel.DailyUsageGroup
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugUsageScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: DebugUsageViewModel = hiltViewModel()
) {
    val dailyUsageGroups by viewModel.dailyUsageGroups.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val diagnosisResult by viewModel.diagnosisResult.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadAllUsageData()
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部工具栏
        TopAppBar(
            title = { Text("📊 ${stringResource(R.string.debug_usage_screen_title)}") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.debug_back))
                }
            },
            actions = {
                val context = LocalContext.current
                
                // 诊断总使用数据按钮
                IconButton(
                    onClick = { viewModel.diagnoseTotalUsageData(context) },
                    enabled = !isLoading
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "诊断总使用数据",
                        tint = if (isLoading) Color.Gray else Color(0xFF2196F3)
                    )
                }
                
                // 重新聚合所有数据按钮
                IconButton(
                    onClick = { viewModel.reaggregateAllData(context) },
                    enabled = !isLoading
                ) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = "重新聚合数据",
                        tint = if (isLoading) Color.Gray else Color(0xFF4CAF50)
                    )
                }
                
                // 清理历史错误数据按钮
                IconButton(
                    onClick = { viewModel.cleanHistoricalData(context) },
                    enabled = !isLoading
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = "清理错误数据",
                        tint = if (isLoading) Color.Gray else Color(0xFFFF5722)
                    )
                }
                
                // 刷新按钮
                IconButton(
                    onClick = { viewModel.refreshData() },
                    enabled = !isLoading
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "刷新",
                        tint = if (isLoading) Color.Gray else MaterialTheme.colorScheme.onSurface
                    )
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
                                val totalRecords = dailyUsageGroups.sumOf { it.totalCount }
                                val totalDays = dailyUsageGroups.size
                                Text("总记录数: $totalRecords", fontSize = 14.sp)
                                Text("覆盖天数: $totalDays 天", fontSize = 14.sp)
                            }
                            Column {
                                val totalMinutes = dailyUsageGroups.sumOf { it.totalMinutes }
                                val totalOffline = dailyUsageGroups.sumOf { it.offlineCount }
                                Text("总时长: ${totalMinutes} 分钟", fontSize = 14.sp)
                                Text("线下活动: $totalOffline 条", fontSize = 14.sp)
                            }
                        }
                    }
                }
                
                // 使用数据列表
                if (dailyUsageGroups.isEmpty()) {
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
                                "📂 没有找到使用数据",
                                fontSize = 16.sp,
                                color = Color(0xFFD32F2F)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        dailyUsageGroups.forEach { dailyGroup ->
                            // 日期分组头部
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CalendarToday,
                                                contentDescription = "日期",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = "${dailyGroup.date} (${dailyGroup.weekday})",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                        Text(
                                            text = "${dailyGroup.totalCount} 条记录",
                                            fontSize = 14.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                            
                            // 该日期的使用数据项目
                            itemsIndexed(dailyGroup.usageItems) { index, usage ->
                                val usageColor = getUsageDebugColor(usage.catId)
                                val categoryIcon = getCategoryIcon(usage.categoryName)
                                val activityIcon = if (usage.isOffline) Icons.AutoMirrored.Filled.DirectionsRun else Icons.Default.PhoneAndroid
                                
                                UsageDebugCard(
                                    usage = usage,
                                    usageColor = usageColor,
                                    categoryIcon = categoryIcon,
                                    activityIcon = activityIcon,
                                    usageNumber = index + 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // 诊断结果对话框
    diagnosisResult?.let { result ->
        AlertDialog(
            onDismissRequest = { viewModel.clearDiagnosisResult() },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.clearDiagnosisResult() }
                ) {
                    Text("确定")
                }
            },
            title = {
                Text(
                    text = "🔍 诊断结果",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = result,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        )
    }
}

@Composable
private fun UsageDebugCard(
    usage: com.offtime.app.ui.debug.viewmodel.UsageDebugData,
    usageColor: Color,
    categoryIcon: ImageVector,
    activityIcon: ImageVector,
    usageNumber: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (usage.isOffline) Color(0xFFE8F5E8) else Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 编号和使用颜色指示器
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(48.dp)
            ) {
                // 编号
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(usageColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = usageNumber.toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 使用指示器
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(usageColor, CircleShape)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 分类图标
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon,
                    contentDescription = "分类图标",
                    tint = usageColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 使用信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 分类名称和时长
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = usage.categoryName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF212121)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "时长",
                            tint = usageColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${usage.durationMin}分钟",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = usageColor
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 时段信息
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "时段",
                        tint = Color(0xFF666666),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "时段: ${usage.slotIndex}:00-${usage.slotIndex + 1}:00",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                    
                    // 活动类型标签
                    AssistChip(
                        onClick = { },
                        label = { 
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = activityIcon,
                                    contentDescription = "活动类型",
                                    modifier = Modifier.size(12.dp),
                                    tint = Color.White
                                )
                                Text(
                                    if (usage.isOffline) stringResource(R.string.offline_label) else stringResource(R.string.online_label), 
                                    fontSize = 10.sp,
                                    color = Color.White
                                ) 
                            }
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (usage.isOffline) Color(0xFF4CAF50) else Color(0xFF2196F3)
                        ),
                        modifier = Modifier.height(20.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 详细信息
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "分类ID: ${usage.catId}",
                        fontSize = 10.sp,
                        color = Color(0xFF999999),
                        modifier = Modifier
                            .background(Color(0xFFF0F0F0), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                    
                    Text(
                        text = "时长: ${usage.durationSec}秒",
                        fontSize = 10.sp,
                        color = Color(0xFF999999),
                        modifier = Modifier
                            .background(Color(0xFFF0F0F0), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

// 根据分类ID获取调试颜色
private fun getUsageDebugColor(catId: Int): Color {
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

// 根据分类名称获取图标（支持中英文）
private fun getCategoryIcon(categoryName: String): ImageVector {
    return when {
        categoryName.contains("娱乐", ignoreCase = true) || 
        categoryName.contains("游戏", ignoreCase = true) ||
        categoryName.contains("entertainment", ignoreCase = true) ||
        categoryName.contains("ent", ignoreCase = true) -> Icons.Default.SportsEsports
        
        categoryName.contains("学习", ignoreCase = true) || 
        categoryName.contains("教育", ignoreCase = true) ||
        categoryName.contains("study", ignoreCase = true) ||
        categoryName.contains("edu", ignoreCase = true) -> Icons.Default.School
        
        categoryName.contains("健身", ignoreCase = true) || 
        categoryName.contains("运动", ignoreCase = true) ||
        categoryName.contains("fitness", ignoreCase = true) ||
        categoryName.contains("fit", ignoreCase = true) -> Icons.Default.FitnessCenter
        
        categoryName.contains("总使用", ignoreCase = true) ||
        categoryName.contains("total", ignoreCase = true) ||
        categoryName.contains("all", ignoreCase = true) -> Icons.Default.Block
        
        else -> Icons.Default.Category
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