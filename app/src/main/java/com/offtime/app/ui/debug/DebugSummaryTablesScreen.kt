package com.offtime.app.ui.debug

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.offtime.app.R
import com.offtime.app.ui.viewmodel.DebugSummaryTablesViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugSummaryTablesScreen(
    onNavigateBack: () -> Unit,
    viewModel: DebugSummaryTablesViewModel = hiltViewModel()
) {
    val summaryUsageUser by viewModel.summaryUsageUser.collectAsState()
    val summaryUsageWeek by viewModel.summaryUsageWeek.collectAsState()
    val summaryUsageMonth by viewModel.summaryUsageMonth.collectAsState()
    val rewardPunishmentWeek by viewModel.rewardPunishmentWeek.collectAsState()
    val rewardPunishmentMonth by viewModel.rewardPunishmentMonth.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadData()
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部工具栏
        TopAppBar(
            title = { Text(stringResource(R.string.debug_summary_tables_screen_title)) },
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
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 概览卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "📊 汇总表概览",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1976D2)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("📅 日汇总: ${summaryUsageUser.size}", fontSize = 14.sp)
                                Text("📊 周汇总: ${summaryUsageWeek.size}", fontSize = 14.sp)
                                Text("📈 月汇总: ${summaryUsageMonth.size}", fontSize = 14.sp)
                            }
                            Column {
                                Text("🎁 周奖罚: ${rewardPunishmentWeek.size}", fontSize = 14.sp)
                                Text("🏆 月奖罚: ${rewardPunishmentMonth.size}", fontSize = 14.sp)
                            }
                        }
                    }
                }
                
                // 日使用汇总表
                SummaryUsageUserCard(
                    data = summaryUsageUser,
                    categories = categories
                )
                
                // 周使用汇总表
                SummaryUsageWeekCard(
                    data = summaryUsageWeek,
                    categories = categories
                )
                
                // 月使用汇总表
                SummaryUsageMonthCard(
                    data = summaryUsageMonth,
                    categories = categories
                )
                
                // 周奖罚汇总表
                RewardPunishmentWeekCard(
                    data = rewardPunishmentWeek,
                    categories = categories
                )
                
                // 月奖罚汇总表
                RewardPunishmentMonthCard(
                    data = rewardPunishmentMonth,
                    categories = categories
                )
                
                // 测试按钮
                TestButtonsCard(viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun SummaryUsageUserCard(
    data: List<com.offtime.app.data.entity.SummaryUsageUserEntity>,
    categories: List<com.offtime.app.data.entity.AppCategoryEntity>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "📅 日使用汇总表 (summary_usage_user)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF7B1FA2)
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Text("总记录数: ${data.size}", fontSize = 14.sp)
            
            if (data.isNotEmpty()) {
                Text("最新记录:", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                val latest = data.take(3)
                latest.forEach { record ->
                    val categoryName = categories.find { it.id == record.catId }?.name ?: "未知分类"
                    Text(
                        "  ${record.date} | $categoryName | ${record.totalSec/60}分钟",
                        fontSize = 12.sp,
                        color = Color(0xFF7B1FA2)
                    )
                }
                if (data.size > 3) {
                    Text("  ... 还有 ${data.size - 3} 条记录", fontSize = 12.sp, color = Color.Gray)
                }
            } else {
                Text("❌ 无数据", fontSize = 14.sp, color = Color.Red)
            }
        }
    }
}

@Composable
private fun SummaryUsageWeekCard(
    data: List<com.offtime.app.data.entity.SummaryUsageWeekUserEntity>,
    categories: List<com.offtime.app.data.entity.AppCategoryEntity>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "📊 周使用汇总表 (summary_usage_week_user)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32)
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Text("总记录数: ${data.size}", fontSize = 14.sp)
            
            if (data.isNotEmpty()) {
                Text("最新记录:", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                val latest = data.take(3)
                latest.forEach { record ->
                    val categoryName = categories.find { it.id == record.catId }?.name ?: "未知分类"
                    Text(
                        "  ${record.weekStartDate} | $categoryName | 平均${record.avgDailySec/60}分钟/天",
                        fontSize = 12.sp,
                        color = Color(0xFF2E7D32)
                    )
                }
                if (data.size > 3) {
                    Text("  ... 还有 ${data.size - 3} 条记录", fontSize = 12.sp, color = Color.Gray)
                }
            } else {
                Text("❌ 无数据", fontSize = 14.sp, color = Color.Red)
            }
        }
    }
}

@Composable
private fun SummaryUsageMonthCard(
    data: List<com.offtime.app.data.entity.SummaryUsageMonthUserEntity>,
    categories: List<com.offtime.app.data.entity.AppCategoryEntity>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "📈 月使用汇总表 (summary_usage_month_user)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF8F00)
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Text("总记录数: ${data.size}", fontSize = 14.sp)
            
            if (data.isNotEmpty()) {
                Text("最新记录:", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                val latest = data.take(3)
                latest.forEach { record ->
                    val categoryName = categories.find { it.id == record.catId }?.name ?: "未知分类"
                    Text(
                        "  ${record.month} | $categoryName | 平均${record.avgDailySec/60}分钟/天",
                        fontSize = 12.sp,
                        color = Color(0xFFFF8F00)
                    )
                }
                if (data.size > 3) {
                    Text("  ... 还有 ${data.size - 3} 条记录", fontSize = 12.sp, color = Color.Gray)
                }
            } else {
                Text("❌ 无数据", fontSize = 14.sp, color = Color.Red)
            }
        }
    }
}

@Composable
private fun RewardPunishmentWeekCard(
    data: List<com.offtime.app.data.entity.RewardPunishmentWeekUserEntity>,
    categories: List<com.offtime.app.data.entity.AppCategoryEntity>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE1F5FE))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "🎁 周奖罚汇总表 (reward_punishment_week_user)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0277BD)
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Text("总记录数: ${data.size}", fontSize = 14.sp)
            
            if (data.isNotEmpty()) {
                Text("最新记录:", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                val latest = data.take(3)
                latest.forEach { record ->
                    val categoryName = categories.find { it.id == record.catId }?.name ?: "未知分类"
                    Text(
                        "  ${record.weekStart} | $categoryName | 奖励${record.doneRewardCount}/${record.totalRewardCount} 惩罚${record.donePunishCount}/${record.totalPunishCount}",
                        fontSize = 12.sp,
                        color = Color(0xFF0277BD)
                    )
                }
                if (data.size > 3) {
                    Text("  ... 还有 ${data.size - 3} 条记录", fontSize = 12.sp, color = Color.Gray)
                }
            } else {
                Text("❌ 无数据", fontSize = 14.sp, color = Color.Red)
            }
        }
    }
}

@Composable
private fun RewardPunishmentMonthCard(
    data: List<com.offtime.app.data.entity.RewardPunishmentMonthUserEntity>,
    categories: List<com.offtime.app.data.entity.AppCategoryEntity>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "🏆 月奖罚汇总表 (reward_punishment_month_user)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD32F2F)
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Text("总记录数: ${data.size}", fontSize = 14.sp)
            
            if (data.isNotEmpty()) {
                Text("最新记录:", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                val latest = data.take(3)
                latest.forEach { record ->
                    val categoryName = categories.find { it.id == record.catId }?.name ?: "未知分类"
                    Text(
                        "  ${record.yearMonth} | $categoryName | 奖励${record.doneRewardCount}/${record.totalRewardCount} 惩罚${record.donePunishCount}/${record.totalPunishCount}",
                        fontSize = 12.sp,
                        color = Color(0xFFD32F2F)
                    )
                }
                if (data.size > 3) {
                    Text("  ... 还有 ${data.size - 3} 条记录", fontSize = 12.sp, color = Color.Gray)
                }
            } else {
                Text("❌ 无数据", fontSize = 14.sp, color = Color.Red)
            }
        }
    }
}

@Composable
private fun TestButtonsCard(viewModel: DebugSummaryTablesViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "🧪 测试功能",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF424242)
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.loadData() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("刷新数据", fontSize = 12.sp)
                }
                
                Button(
                    onClick = { viewModel.generateTestData() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                ) {
                    Text("生成测试数据", fontSize = 12.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.clearAllSummaryData() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                ) {
                    Text("清空汇总数据", fontSize = 12.sp)
                }
                
                Button(
                    onClick = { viewModel.rebuildSummaryData() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0))
                ) {
                    Text("重建汇总数据", fontSize = 12.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 新增按钮行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.generateBaseSummaryRecords() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BCD4))
                ) {
                    Text("🔧 生成基础记录", fontSize = 12.sp)
                }
            }
        }
    }
} 