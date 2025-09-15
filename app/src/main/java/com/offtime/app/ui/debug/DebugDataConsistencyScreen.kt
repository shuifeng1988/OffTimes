package com.offtime.app.ui.debug

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.offtime.app.R
import com.offtime.app.ui.debug.viewmodel.DebugDataConsistencyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugDataConsistencyScreen(
    onBack: () -> Unit = {},
    viewModel: DebugDataConsistencyViewModel = hiltViewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val consistencyReport by viewModel.consistencyReport.collectAsState()
    val lastCheckTime by viewModel.lastCheckTime.collectAsState()
    val fixInProgress by viewModel.fixInProgress.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.checkDataConsistency()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Title bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "数据一致性诊断",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            TextButton(onClick = onBack) {
                Text("返回")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.checkDataConsistency() },
                modifier = Modifier.weight(1f),
                enabled = !isLoading && !fixInProgress
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text("检查一致性")
                }
            }
            
            Button(
                onClick = { viewModel.fixDataInconsistencies() },
                modifier = Modifier.weight(1f),
                enabled = !isLoading && !fixInProgress && (consistencyReport?.hasInconsistencies == true),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                if (fixInProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text("修复不一致", color = Color.White)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Last check time
        if (lastCheckTime.isNotEmpty()) {
            Text(
                text = "最后检查时间: $lastCheckTime",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        // Results card
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
                    text = "数据一致性报告",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                if (isLoading && consistencyReport == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("正在检查数据一致性...")
                        }
                    }
                } else if (fixInProgress) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("正在修复数据不一致问题...")
                            Text(
                                text = "这可能需要几分钟时间",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        consistencyReport?.let { report ->
                            // 总体状态
                            ConsistencyStatusCard(report)
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // 检查统计
                            ConsistencyStatsCard(report)
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // 不一致项目详情
                            if (report.inconsistentItems.isNotEmpty()) {
                                Text(
                                    text = "不一致项目详情",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                
                                report.inconsistentItems.forEach { item ->
                                    InconsistentItemCard(item)
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        } ?: run {
                            Text(
                                text = "${stringResource(R.string.no_data)}，请点击\"检查一致性\"开始检查",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConsistencyStatusCard(report: DebugDataConsistencyViewModel.DataConsistencyReport) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (report.hasInconsistencies) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
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
                imageVector = if (report.hasInconsistencies) Icons.Default.Error else Icons.Default.CheckCircle,
                contentDescription = if (report.hasInconsistencies) "有不一致" else "数据一致",
                modifier = Modifier.size(24.dp),
                tint = if (report.hasInconsistencies) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Text(
                    text = "数据一致性状态",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (report.hasInconsistencies) {
                        "发现 ${report.inconsistentItems.size} 项不一致"
                    } else {
                        "数据一致"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ConsistencyStatsCard(report: DebugDataConsistencyViewModel.DataConsistencyReport) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "检查统计",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("检查日期", "${report.datesChecked} 天")
                StatItem("检查分类", "${report.categoriesChecked} 个")
                StatItem("检查时间", report.checkTime)
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun InconsistentItemCard(item: DebugDataConsistencyViewModel.InconsistentItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${item.categoryName} (${item.date})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "原始数据: ${item.rawMinutes} 分钟",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "聚合数据: ${item.aggregatedMinutes} 分钟",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(
                        text = "差值: ${item.differenceMinutes}分钟",
                        fontSize = 12.sp,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            if (item.details.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.details,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
