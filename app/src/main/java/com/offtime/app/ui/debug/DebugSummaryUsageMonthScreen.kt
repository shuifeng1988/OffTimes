package com.offtime.app.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Block
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
import com.offtime.app.ui.viewmodel.DebugSummaryTablesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugSummaryUsageMonthScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: DebugSummaryTablesViewModel = hiltViewModel()
) {
    val summaryUsageMonth by viewModel.summaryUsageMonth.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadData()
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("📈 月使用汇总表调试") },
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
                    onClick = { viewModel.loadData() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    Text("刷新")
                }
                
                // 统计信息
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "📊 统计信息",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("总记录数: ${summaryUsageMonth.size}", fontSize = 14.sp)
                                val totalMinutes = summaryUsageMonth.sumOf { it.avgDailySec * 30 } / 60
                                Text("总使用时长: ${totalMinutes} 分钟", fontSize = 14.sp)
                            }
                            Column {
                                val uniqueMonths = summaryUsageMonth.map { it.month }.distinct().size
                                val uniqueCategories = summaryUsageMonth.map { it.catId }.distinct().size
                                Text("覆盖月数: $uniqueMonths 月", fontSize = 14.sp)
                                Text("涉及分类: $uniqueCategories 个", fontSize = 14.sp)
                            }
                        }
                    }
                }
                
                // 数据列表
                if (summaryUsageMonth.isEmpty()) {
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
                                "📂 暂无月使用汇总数据",
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
                        // 按月份分组
                        val groupedData = summaryUsageMonth.groupBy { it.month }.toList().sortedByDescending { it.first }
                        
                        groupedData.forEach { (month, records) ->
                            // 月分组头部
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE65100))
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
                                                imageVector = Icons.Default.Event,
                                                contentDescription = "月",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = "月份: $month",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                        Text(
                                            text = "${records.size} 个分类",
                                            fontSize = 14.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                            
                            // 该月的记录
                            itemsIndexed(records.sortedByDescending { it.avgDailySec }) { index, record ->
                                val categoryName = categories.find { it.id == record.catId }?.name ?: "未知分类"
                                val recordColor = getSummaryDebugColor(record.catId)
                                val categoryIcon = getCategoryIcon(categoryName)
                                
                                SummaryUsageMonthCard(
                                    record = record,
                                    categoryName = categoryName,
                                    recordColor = recordColor,
                                    categoryIcon = categoryIcon,
                                    recordNumber = index + 1
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
private fun SummaryUsageMonthCard(
    record: com.offtime.app.data.entity.SummaryUsageMonthUserEntity,
    categoryName: String,
    recordColor: Color,
    categoryIcon: ImageVector,
    recordNumber: Int
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
                        .background(recordColor, CircleShape),
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
                        .background(recordColor, CircleShape)
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
                    tint = recordColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 记录信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 分类名称和平均时长
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = categoryName,
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
                            tint = recordColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "平均${record.avgDailySec / 60}分钟/天",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = recordColor
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 详细信息
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "分类ID: ${record.catId}",
                        fontSize = 10.sp,
                        color = Color(0xFF999999),
                        modifier = Modifier
                            .background(Color(0xFFF0F0F0), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                    
                    Text(
                        text = "月总时长: ${record.avgDailySec * 30 / 60}分钟",
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
private fun getSummaryDebugColor(catId: Int): Color {
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