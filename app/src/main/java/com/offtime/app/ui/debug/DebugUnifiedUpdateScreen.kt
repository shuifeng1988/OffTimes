package com.offtime.app.ui.debug

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.offtime.app.ui.debug.viewmodel.DebugUnifiedUpdateViewModel
import com.offtime.app.service.UnifiedUpdateService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugUnifiedUpdateScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: DebugUnifiedUpdateViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val serviceStatus by viewModel.serviceStatus.collectAsState()
    val lastUpdateTime by viewModel.lastUpdateTime.collectAsState()
    val updateCount by viewModel.updateCount.collectAsState()
    val isAutoRefresh by viewModel.isAutoRefresh.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.startMonitoring()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("统一更新服务调试") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 服务状态卡片
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "服务状态",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("统一更新服务")
                            Text(
                                text = if (serviceStatus) "运行中" else "已停止",
                                color = if (serviceStatus) MaterialTheme.colorScheme.primary 
                                       else MaterialTheme.colorScheme.error
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("最后更新时间")
                            Text(lastUpdateTime)
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("累计更新次数")
                            Text("$updateCount 次")
                        }
                    }
                }
            }
            
            // 控制按钮
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "服务控制",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { 
                                    UnifiedUpdateService.startUnifiedUpdate(context)
                                    viewModel.refreshStatus()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("启动服务")
                            }
                            
                            Button(
                                onClick = { 
                                    UnifiedUpdateService.stopUnifiedUpdate(context)
                                    viewModel.refreshStatus()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("停止服务")
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = { 
                                UnifiedUpdateService.triggerManualUpdate(context)
                                viewModel.refreshStatus()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("手动触发更新")
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Switch(
                                checked = isAutoRefresh,
                                onCheckedChange = { viewModel.setAutoRefresh(it) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("自动刷新状态")
                        }
                    }
                }
            }
            
            // 日志信息
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "调试信息",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "• 查看Logcat输出日志: 'UnifiedUpdateService'\n" +
                                  "• 每分钟应该看到更新日志\n" +
                                  "• 如果没有日志说明服务未运行\n" +
                                  "• 检查应用是否在电池优化白名单中",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}