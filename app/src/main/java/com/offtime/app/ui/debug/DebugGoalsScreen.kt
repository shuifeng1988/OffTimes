package com.offtime.app.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Category
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
import com.offtime.app.ui.debug.viewmodel.DebugGoalsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugGoalsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: DebugGoalsViewModel = hiltViewModel()
) {
    val goals by viewModel.goals.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadGoals()
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部工具栏
        TopAppBar(
            title = { Text("目标奖罚配置调试") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
                Button(
                    onClick = { viewModel.loadGoals() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    Text("刷新")
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
                                Text("总目标数: ${goals.size}", fontSize = 14.sp)
                                Text("≤目标算完成: ${goals.count { it.conditionType == 0 }}", fontSize = 14.sp)
                            }
                            Column {
                                Text("≥目标算完成: ${goals.count { it.conditionType == 1 }}", fontSize = 14.sp)
                                Text("有奖励设置: ${goals.count { it.rewardContent.isNotEmpty() }}", fontSize = 14.sp)
                            }
                        }
                    }
                }
                
                // 目标列表
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "🎯 目标详情",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        if (goals.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "🎯 暂无目标配置",
                                    fontSize = 16.sp,
                                    color = Color(0xFFD32F2F)
                                )
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(goals.sortedBy { it.catId }) { index, goal ->
                                    val goalColor = getGoalDebugColor(goal.catId)
                                    val categoryIcon = getCategoryIcon(goal.categoryName)
                                    
                                    GoalDebugCard(
                                        goal = goal,
                                        goalColor = goalColor,
                                        categoryIcon = categoryIcon,
                                        goalNumber = index + 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalDebugCard(
    goal: com.offtime.app.ui.debug.viewmodel.GoalDebugData,
    goalColor: Color,
    categoryIcon: ImageVector,
    goalNumber: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (goal.conditionType) {
                0 -> Color(0xFFE3F2FD) // 蓝色背景 - ≤目标算完成
                1 -> Color(0xFFE8F5E8) // 绿色背景 - ≥目标算完成
                else -> Color.White
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 编号和目标颜色指示器
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(48.dp)
            ) {
                // 编号
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(goalColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = goalNumber.toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 目标指示器
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(goalColor, CircleShape)
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
                    tint = goalColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 目标信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 分类名称和目标时间
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = goal.categoryName,
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
                            contentDescription = "时间",
                            tint = goalColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${goal.dailyGoalMin}分钟",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = goalColor
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 条件类型
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val conditionIcon = if (goal.conditionType == 0) Icons.Default.CheckCircle else Icons.Default.Cancel
                    val conditionColor = if (goal.conditionType == 0) Color(0xFF1976D2) else Color(0xFF388E3C)
                    val conditionText = if (goal.conditionType == 0) "≤目标算完成" else "≥目标算完成"
                    
                    Icon(
                        imageVector = conditionIcon,
                        contentDescription = "条件",
                        tint = conditionColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "条件: $conditionText",
                        fontSize = 12.sp,
                        color = conditionColor,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // 奖励和惩罚
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (goal.rewardContent.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = "奖励",
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "奖励: ${goal.rewardContent}",
                                fontSize = 11.sp,
                                color = Color(0xFF666666)
                            )
                        }
                    }
                    
                    if (goal.punishmentContent.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "惩罚",
                                tint = Color(0xFFF44336),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "惩罚: ${goal.punishmentContent}",
                                fontSize = 11.sp,
                                color = Color(0xFF666666)
                            )
                        }
                    }
                    
                    // 分类ID
                    Text(
                        text = "分类ID: ${goal.catId}",
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
private fun getGoalDebugColor(catId: Int): Color {
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

// 根据分类名称获取图标
private fun getCategoryIcon(categoryName: String): ImageVector {
    return when {
        categoryName.contains("娱乐", ignoreCase = true) || 
        categoryName.contains("游戏", ignoreCase = true) -> Icons.Default.SportsEsports
        
        categoryName.contains("学习", ignoreCase = true) || 
        categoryName.contains("教育", ignoreCase = true) -> Icons.Default.School
        
        categoryName.contains("健身", ignoreCase = true) || 
        categoryName.contains("运动", ignoreCase = true) -> Icons.Default.FitnessCenter
        
        categoryName.contains("总使用", ignoreCase = true) -> Icons.Default.Block
        
        else -> Icons.Default.Category
    }
} 