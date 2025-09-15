package com.offtime.app.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.offtime.app.ui.debug.viewmodel.DebugTimerSessionsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugTimerSessionsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: DebugTimerSessionsViewModel = hiltViewModel()
) {
    val sessions by viewModel.sessions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadSessions()
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部工具栏
        TopAppBar(
            title = { Text("线下活动表调试") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
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
                    .padding(16.dp)
            ) {
                // 操作按钮
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
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
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
                            text = "🏃 统计信息",
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
                                Text("总活动数: ${sessions.size}", fontSize = 14.sp)
                                Text("今日活动: ${sessions.count { it.date == SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }}", fontSize = 14.sp)
                            }
                            Column {
                                Text("总时长: ${sessions.sumOf { it.durationSec } / 60} 分钟", fontSize = 14.sp)
                                Text("线下活动: ${sessions.count { it.isOffline == 1 }}", fontSize = 14.sp)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 活动列表 - 按日期分组
                if (sessions.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "🏃 暂无线下活动数据",
                                fontSize = 16.sp,
                                color = Color(0xFFD32F2F)
                            )
                        }
                    }
                } else {
                    // 按日期分组
                    val groupedSessions = sessions.groupBy { it.date }.toSortedMap(compareByDescending { it })
                    
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        groupedSessions.forEach { (date, sessionsInDate) ->
                            // 日期标题
                            item {
                                TimerDateHeaderCard(date = date, sessionCount = sessionsInDate.size)
                            }
                            
                            // 该日期的活动列表
                            itemsIndexed(sessionsInDate.sortedByDescending { it.startTime }) { index, session ->
                                val category = categories.find { it.id == session.catId }
                                val categoryColor = getTimerCategoryColor(session.catId)
                                val activityIcon = getActivityIcon(session.programName)
                                val sessionNumber = index + 1
                                
                                TimerSessionCard(
                                    session = session,
                                    category = category,
                                    categoryColor = categoryColor,
                                    activityIcon = activityIcon,
                                    sessionNumber = sessionNumber
                                )
                            }
                            
                            // 日期间隔
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimerDateHeaderCard(date: String, sessionCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                text = "$sessionCount 个活动",
                fontSize = 14.sp,
                color = Color.White
            )
        }
    }
}

@Composable
private fun TimerSessionCard(
    session: com.offtime.app.data.entity.TimerSessionUserEntity,
    category: com.offtime.app.data.entity.AppCategoryEntity?,
    categoryColor: Color,
    activityIcon: ImageVector,
    sessionNumber: Int
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
            // 编号和分类颜色指示器
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
                        text = sessionNumber.toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 分类指示器
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(categoryColor, CircleShape)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 活动图标
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = activityIcon,
                    contentDescription = "活动图标",
                    tint = categoryColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 活动信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 活动名称
                Text(
                    text = session.programName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF212121)
                )
                
                // 分类名称和线下标识
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = category?.name ?: "未知分类",
                        fontSize = 12.sp,
                        color = categoryColor,
                        fontWeight = FontWeight.Medium
                    )
                    
                    if (session.isOffline == 1) {
                        AssistChip(
                            onClick = { },
                            label = { 
                                Text(
                                    "线下活动", 
                                    fontSize = 10.sp,
                                    color = Color.White
                                ) 
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFF4CAF50)
                            ),
                            modifier = Modifier.height(20.dp)
                        )
                    }
                }
                
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

// 根据分类ID获取颜色（线下活动使用不同的色调）
private fun getTimerCategoryColor(catId: Int): Color {
    val colors = listOf(
        Color(0xFF4CAF50), // 绿色
        Color(0xFF2196F3), // 蓝色
        Color(0xFFFF9800), // 橙色
        Color(0xFF9C27B0), // 紫色
        Color(0xFF00BCD4), // 青色
        Color(0xFFF44336), // 红色
        Color(0xFF795548), // 棕色
        Color(0xFF607D8B), // 蓝灰色
        Color(0xFFE91E63), // 粉色
        Color(0xFF8BC34A)  // 浅绿色
    )
    return colors[catId % colors.size]
}

// 根据活动名称获取图标
private fun getActivityIcon(programName: String): ImageVector {
    return when {
        programName.contains("跑步", ignoreCase = true) || 
        programName.contains("running", ignoreCase = true) -> Icons.Default.DirectionsRun
        
        programName.contains("健身", ignoreCase = true) || 
        programName.contains("gym", ignoreCase = true) ||
        programName.contains("workout", ignoreCase = true) -> Icons.Default.FitnessCenter
        
        programName.contains("阅读", ignoreCase = true) || 
        programName.contains("reading", ignoreCase = true) ||
        programName.contains("学习", ignoreCase = true) ||
        programName.contains("study", ignoreCase = true) -> Icons.Default.MenuBook
        
        else -> Icons.Default.Timer
    }
} 