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
import com.offtime.app.data.entity.AppSettingsEntity
import com.offtime.app.ui.viewmodel.DebugSettingsViewModel
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: DebugSettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadData()
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部工具栏
        TopAppBar(
            title = { Text(stringResource(R.string.debug_app_settings_screen_title)) },
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
                // 表信息
                TableInfoCard()
                
                // 设置数据
                SettingsDataCard(settings = settings)
                
                // 默认分类设置
                DefaultCategoryCard(
                    settings = settings,
                    categories = categories
                )
                
                // 分类奖罚开关设置
                CategoryRewardPunishmentCard(
                    settings = settings,
                    categories = categories
                )
                
                // 测试按钮
                TestButtonsCard(viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun TableInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "📊 表信息",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("表名: app_settings", fontSize = 14.sp)
            Text("用途: 存储应用全局设置", fontSize = 14.sp)
            Text("主要字段: defaultCategoryId, categoryRewardPunishmentEnabled", fontSize = 14.sp)
        }
    }
}

@Composable
private fun SettingsDataCard(settings: AppSettingsEntity?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "⚙️ 设置数据",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF7B1FA2)
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            if (settings != null) {
                Text("ID: ${settings.id}", fontSize = 14.sp)
                Text("默认分类ID: ${settings.defaultCategoryId}", fontSize = 14.sp)
                Text("创建时间: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(settings.createdAt))}", fontSize = 14.sp)
                Text("更新时间: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(settings.updatedAt))}", fontSize = 14.sp)
            } else {
                Text("❌ 没有设置数据", fontSize = 14.sp, color = Color.Red)
            }
        }
    }
}

@Composable
private fun DefaultCategoryCard(
    settings: AppSettingsEntity?,
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
                text = "🎯 默认分类设置",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32)
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            if (settings != null) {
                val defaultCategory = categories.find { it.id == settings.defaultCategoryId }
                if (defaultCategory != null) {
                    Text("✅ 默认分类: ${defaultCategory.name} (ID: ${defaultCategory.id})", fontSize = 14.sp)
                } else {
                    Text("⚠️ 默认分类ID ${settings.defaultCategoryId} 在分类表中不存在", fontSize = 14.sp, color = Color(0xFFFF9800))
                }
            } else {
                Text("❌ 无设置数据", fontSize = 14.sp, color = Color.Red)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text("可用分类:", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            categories.forEach { category ->
                val isDefault = settings?.defaultCategoryId == category.id
                Text(
                    "  ${if (isDefault) "👉" else "  "} ${category.name} (ID: ${category.id})",
                    fontSize = 12.sp,
                    color = if (isDefault) Color(0xFF2E7D32) else Color.Gray
                )
            }
        }
    }
}

@Composable
private fun CategoryRewardPunishmentCard(
    settings: AppSettingsEntity?,
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
                text = "🎁 分类奖罚开关",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF8F00)
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            if (settings != null && settings.categoryRewardPunishmentEnabled.isNotEmpty()) {
                // 在composable外部解析JSON
                val (jsonData, parseError, enabledMap) = remember(settings.categoryRewardPunishmentEnabled) {
                    try {
                        val json = JSONObject(settings.categoryRewardPunishmentEnabled)
                        val map = mutableMapOf<Int, Boolean>()
                        categories.forEach { category ->
                            map[category.id] = try {
                                json.getBoolean(category.id.toString())
                            } catch (e: Exception) {
                                true // 默认开启
                            }
                        }
                        Triple(settings.categoryRewardPunishmentEnabled, null, map)
                    } catch (e: Exception) {
                        Triple(settings.categoryRewardPunishmentEnabled, e.message, emptyMap<Int, Boolean>())
                    }
                }
                
                if (parseError != null) {
                    Text("❌ JSON解析失败: $parseError", fontSize = 14.sp, color = Color.Red)
                } else {
                    Text("JSON数据: $jsonData", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    categories.forEach { category ->
                        val isEnabled = enabledMap[category.id] ?: true
                        Text(
                            "  ${category.name} (ID: ${category.id}): ${if (isEnabled) "✅ 开启" else "❌ 关闭"}",
                            fontSize = 14.sp,
                            color = if (isEnabled) Color(0xFF4CAF50) else Color(0xFFF44336)
                        )
                    }
                }
            } else {
                Text("⚠️ 无奖罚开关数据（所有分类默认开启）", fontSize = 14.sp, color = Color(0xFFFF9800))
                categories.forEach { category ->
                    Text(
                        "  ${category.name} (ID: ${category.id}): ✅ 开启（默认）",
                        fontSize = 14.sp,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        }
    }
}

@Composable
private fun TestButtonsCard(viewModel: DebugSettingsViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "🧪 测试功能",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD32F2F)
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.testSetDefaultCategory() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("测试设置默认分类", fontSize = 12.sp)
                }
                
                Button(
                    onClick = { viewModel.testToggleRewardPunishment() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("测试切换奖罚开关", fontSize = 12.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.resetSettings() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                ) {
                    Text("重置设置", fontSize = 12.sp)
                }
                
                Button(
                    onClick = { viewModel.loadData() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("刷新数据", fontSize = 12.sp)
                }
            }
        }
    }
} 