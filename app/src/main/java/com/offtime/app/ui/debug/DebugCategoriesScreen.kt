package com.offtime.app.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.offtime.app.R
import com.offtime.app.ui.debug.viewmodel.DebugCategoriesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugCategoriesScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: DebugCategoriesViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadCategories()
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部工具栏
        TopAppBar(
            title = { Text("🎨 ${stringResource(R.string.debug_app_categories_screen_title)}") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.debug_back))
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
                    onClick = { viewModel.loadCategories() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    Text("刷新")
                }
                
                // 统计信息
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "📊 统计信息",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("总分类数: ${categories.size}", fontSize = 14.sp)
                                Text("锁定分类: ${categories.count { it.isLocked }}", fontSize = 14.sp)
                            }
                            Column {
                                Text("可编辑分类: ${categories.count { !it.isLocked }}", fontSize = 14.sp)
                                Text("目标类型: ${categories.map { it.targetType }.distinct().size} 种", fontSize = 14.sp)
                            }
                        }
                    }
                }
                
                // 分类列表 - 使用日期分组的样式
                if (categories.isEmpty()) {
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
                                "📂 暂无分类数据",
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
                                            imageVector = Icons.Default.Category,
                                            contentDescription = "分类",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "应用分类配置",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Text(
                                        text = "${categories.size} 个分类",
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        
                        // 分类项目
                        itemsIndexed(categories.sortedBy { it.displayOrder }) { index, category ->
                            val categoryColor = getCategoryDebugColor(category.id)
                            val categoryIcon = getCategoryIcon(category.name, category.targetType)
                            
                            CategoryDebugCard(
                                category = category,
                                categoryColor = categoryColor,
                                categoryIcon = categoryIcon,
                                categoryNumber = index + 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryDebugCard(
    category: com.offtime.app.data.entity.AppCategoryEntity,
    categoryColor: Color,
    categoryIcon: ImageVector,
    categoryNumber: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (category.isLocked) Color(0xFFFFEBEE) else Color.White
        )
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
                        text = categoryNumber.toString(),
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
                    tint = categoryColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 分类信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 分类名称和ID
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = category.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF212121)
                    )
                    Text(
                        text = "ID: ${category.id}",
                        fontSize = 12.sp,
                        color = Color(0xFF666666),
                        modifier = Modifier
                            .background(Color(0xFFF0F0F0), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 目标类型和显示顺序
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 目标类型标签
                    val targetTypeText = when (category.targetType) {
                        "LESS_THAN" -> "≤目标算完成"
                        "MORE_THAN" -> "≥目标算完成"
                        else -> category.targetType
                    }
                    
                    AssistChip(
                        onClick = { },
                        label = { 
                            Text(
                                targetTypeText, 
                                fontSize = 10.sp,
                                color = Color.White
                            ) 
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = categoryColor
                        ),
                        modifier = Modifier.height(20.dp)
                    )
                    
                    Text(
                        text = "显示顺序: ${category.displayOrder}",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 锁定状态
                if (category.isLocked) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "锁定",
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "系统锁定分类",
                            fontSize = 12.sp,
                            color = Color(0xFFD32F2F),
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "可编辑",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "用户可编辑",
                            fontSize = 12.sp,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// 根据分类ID获取调试颜色
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

// 根据分类名称和目标类型获取图标（支持中英文）
private fun getCategoryIcon(categoryName: String, targetType: String): ImageVector {
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
        categoryName.contains("all", ignoreCase = true) ||
        targetType == "LESS_THAN" -> Icons.Default.Block
        
        else -> Icons.Default.Category
    }
} 