package com.offtime.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import com.offtime.app.R
import com.offtime.app.data.entity.AppInfoEntity
import com.offtime.app.service.AppInfoService
import com.offtime.app.ui.viewmodel.ExcludedAppsViewModel
import dagger.hilt.android.EntryPointAccessors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcludedAppsScreen(
    onBackClick: () -> Unit,
    viewModel: ExcludedAppsViewModel = hiltViewModel()
) {
    val excludedApps by viewModel.excludedApps.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadExcludedApps()
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 顶部工具栏
        TopAppBar(
            title = { Text(stringResource(R.string.excluded_apps_title)) },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back_button_desc)
                    )
                }
            },
            actions = {
                // 刷新按钮
                IconButton(onClick = { viewModel.loadExcludedApps() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.refresh_button_desc)
                    )
                }
            }
        )
        
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            excludedApps.isEmpty() -> {
                EmptyExcludedAppsCard()
            }
            else -> {
                ExcludedAppsContent(
                    excludedApps = excludedApps,
                    onRestoreApp = { app ->
                        viewModel.restoreApp(app.packageName)
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyExcludedAppsCard() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Block,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.no_excluded_apps),
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.all_apps_included),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ExcludedAppsContent(
    excludedApps: List<AppInfoEntity>,
    onRestoreApp: (AppInfoEntity) -> Unit
) {
    Column {
        // 统计信息卡片
        StatsCard(excludedApps = excludedApps)
        
        // 应用列表
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(excludedApps) { app ->
                ExcludedAppItem(
                    app = app,
                    onRestoreClick = { onRestoreApp(app) }
                )
            }
        }
    }
}

@Composable
private fun StatsCard(excludedApps: List<AppInfoEntity>) {
    val systemApps = excludedApps.count { it.isSystemApp }
    val userApps = excludedApps.count { !it.isSystemApp }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = stringResource(R.string.exclusion_stats_info),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem(
                    label = stringResource(R.string.system_apps),
                    count = systemApps,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                StatItem(
                    label = stringResource(R.string.user_apps),
                    count = userApps,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                StatItem(
                    label = stringResource(R.string.total_count),
                    count = excludedApps.size,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    count: Int,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 16.sp,
            color = color.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun ExcludedAppItem(
    app: AppInfoEntity,
    onRestoreClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // 应用图标
                AppIcon(
                    packageName = app.packageName,
                    appName = app.appName,
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = app.appName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = app.packageName,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (app.isSystemApp) stringResource(R.string.system_app_label) else stringResource(R.string.user_app_label),
                        fontSize = 16.sp,
                        color = if (app.isSystemApp) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // 恢复按钮
            IconButton(
                onClick = onRestoreClick,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.RestoreFromTrash,
                    contentDescription = stringResource(R.string.restore_app_desc)
                )
            }
        }
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
                if (appIcon == null) MaterialTheme.colorScheme.surfaceVariant 
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
            // 回退到排除图标
            Icon(
                imageVector = Icons.Default.Block,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}