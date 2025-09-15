package com.offtime.app.ui.debug

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.offtime.app.ui.debug.viewmodel.DebugRewardsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugRewardsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: DebugRewardsViewModel = hiltViewModel()
) {
    val rewards by viewModel.rewards.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadRewards()
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
                text = "奖惩记录表调试",
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
                onClick = { viewModel.loadRewards() },
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
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "统计信息",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text("总记录数: ${rewards.size}")
                Text("目标达成: ${rewards.count { it.isGoalMet == 1 }}")
                Text("奖励已执行: ${rewards.count { it.rewardDone == 1 }}")
                Text("惩罚已执行: ${rewards.count { it.punishDone == 1 }}")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 奖惩记录列表
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(rewards) { reward ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "日期: ${reward.date}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "分类ID: ${reward.catId}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "记录ID: ${reward.id}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                if (reward.isGoalMet == 1) {
                                    AssistChip(
                                        onClick = { },
                                        label = { Text("目标达成", fontSize = 10.sp) }
                                    )
                                } else {
                                    AssistChip(
                                        onClick = { },
                                        label = { Text("目标未达成", fontSize = 10.sp) }
                                    )
                                }
                                
                                if (reward.rewardDone == 1) {
                                    AssistChip(
                                        onClick = { },
                                        label = { Text("奖励已执行", fontSize = 10.sp) }
                                    )
                                }
                                
                                if (reward.punishDone == 1) {
                                    AssistChip(
                                        onClick = { },
                                        label = { Text("惩罚已执行", fontSize = 10.sp) }
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