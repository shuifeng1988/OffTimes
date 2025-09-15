package com.offtime.app.ui.permissions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
// import androidx.lifecycle.Lifecycle
// import androidx.lifecycle.LifecycleEventObserver
// import androidx.lifecycle.compose.LocalLifecycleOwner
import com.offtime.app.utils.UsageStatsPermissionHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsagePermissionScreen(
    onPermissionGranted: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember { 
        mutableStateOf(UsageStatsPermissionHelper.hasUsageStatsPermission(context)) 
    }
    
    // 定期检查权限状态
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000) // 每秒检查一次
            val currentPermission = UsageStatsPermissionHelper.hasUsageStatsPermission(context)
            if (currentPermission != hasPermission) {
                hasPermission = currentPermission
                if (currentPermission) {
                    onPermissionGranted()
                    break
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("应用使用权限") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Security, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "需要应用使用情况访问权限",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "为什么需要这个权限？",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Text(
                        text = "• 统计各应用的实际使用时长",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Text(
                        text = "• 生成详细的使用报告和分析",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Text(
                        text = "• 帮助您更好地管理设备使用时间",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "如何开启权限？",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Text(
                        text = "1. 点击下方「前往设置」按钮",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Text(
                        text = "2. 在「应用使用情况访问」页面中找到 OffTime",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Text(
                        text = "3. 打开「允许使用情况访问」开关",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Text(
                        text = "4. 返回应用即可自动检测权限状态",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            if (hasPermission) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "权限已授予，可以开始统计应用使用时长了！",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
                
                Button(
                    onClick = onPermissionGranted,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("继续")
                }
            } else {
                Button(
                    onClick = { 
                        UsageStatsPermissionHelper.openUsageAccessSettings(context)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("前往设置")
                }
                
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("稍后再说")
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
} 