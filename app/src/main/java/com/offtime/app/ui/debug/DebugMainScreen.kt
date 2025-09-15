package com.offtime.app.ui.debug

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.offtime.app.R

data class DebugTableItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val route: String,
    val category: String,
    val categoryColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugMainScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToTable: (String) -> Unit = {}
) {
    @Suppress("UNUSED_VARIABLE")
    val context = LocalContext.current
    
    val debugTables = listOf(
        DebugTableItem(
            title = stringResource(R.string.debug_app_categories_title),
            description = stringResource(R.string.debug_app_categories_desc),
            icon = Icons.Default.Category,
            route = "debug_categories",
            category = stringResource(R.string.debug_category_basic_config),
            categoryColor = Color(0xFF4CAF50)
        ),
        DebugTableItem(
            title = stringResource(R.string.debug_app_settings_title),
            description = stringResource(R.string.debug_app_settings_desc),
            icon = Icons.Default.Settings,
            route = "debug_settings",
            category = stringResource(R.string.debug_category_basic_config),
            categoryColor = Color(0xFF4CAF50)
        ),
        DebugTableItem(
            title = stringResource(R.string.debug_app_info_title),
            description = stringResource(R.string.debug_app_info_desc),
            icon = Icons.Default.Apps,
            route = "debug_apps",
            category = stringResource(R.string.debug_category_basic_config),
            categoryColor = Color(0xFF4CAF50)
        ),
        DebugTableItem(
            title = stringResource(R.string.debug_summary_usage_title),
            description = stringResource(R.string.debug_summary_usage_desc),
            icon = Icons.Default.CalendarToday,
            route = "debug_summary_usage_user",
            category = stringResource(R.string.debug_category_usage_stats),
            categoryColor = Color(0xFF2196F3)
        ),
        DebugTableItem(
            title = stringResource(R.string.debug_summary_usage_week_title),
            description = stringResource(R.string.debug_summary_usage_week_desc),
            icon = Icons.Default.DateRange,
            route = "debug_summary_usage_week",
            category = stringResource(R.string.debug_category_usage_stats),
            categoryColor = Color(0xFF2196F3)
        ),
        DebugTableItem(
            title = stringResource(R.string.debug_summary_usage_month_title),
            description = stringResource(R.string.debug_summary_usage_month_desc),
            icon = Icons.Default.Event,
            route = "debug_summary_usage_month",
            category = stringResource(R.string.debug_category_usage_stats),
            categoryColor = Color(0xFF2196F3)
        ),
        DebugTableItem(
            title = stringResource(R.string.debug_daily_usage_title),
            description = stringResource(R.string.debug_daily_usage_desc),
            icon = Icons.Default.Timeline,
            route = "debug_usage",
            category = stringResource(R.string.debug_category_usage_stats),
            categoryColor = Color(0xFF2196F3)
        ),
        DebugTableItem(
            title = stringResource(R.string.debug_daily_usage_summary_title),
            description = stringResource(R.string.debug_daily_usage_summary_desc),
            icon = Icons.Default.ListAlt,
            route = "daily_usage",
            category = stringResource(R.string.debug_category_usage_stats),
            categoryColor = Color(0xFF2196F3)
        ),
        DebugTableItem(
            title = stringResource(R.string.debug_reward_punishment_week_title),
            description = stringResource(R.string.debug_reward_punishment_week_desc),
            icon = Icons.Default.EmojiEvents,
            route = "debug_reward_punishment_week",
            category = stringResource(R.string.debug_category_reward_punishment),
            categoryColor = Color(0xFFFF9800)
        ),
        DebugTableItem(
            title = stringResource(R.string.debug_reward_punishment_month_title),
            description = stringResource(R.string.debug_reward_punishment_month_desc),
            icon = Icons.Default.WorkspacePremium,
            route = "debug_reward_punishment_month",
            category = stringResource(R.string.debug_category_reward_punishment),
            categoryColor = Color(0xFFFF9800)
        ),
        DebugTableItem(
            title = stringResource(R.string.debug_goals_title),
            description = stringResource(R.string.debug_goals_desc),
            icon = Icons.Default.Star,
            route = "debug_goals",
            category = stringResource(R.string.debug_category_reward_punishment),
            categoryColor = Color(0xFFFF9800)
        ),
        DebugTableItem(
            title = stringResource(R.string.debug_reward_punishment_user_title),
            description = stringResource(R.string.debug_reward_punishment_user_desc),
            icon = Icons.Default.Assignment,
            route = "debug_reward_punishment_user",
            category = stringResource(R.string.debug_category_reward_punishment),
            categoryColor = Color(0xFFFF9800)
        ),
        DebugTableItem(
            title = stringResource(R.string.debug_app_sessions_title),
            description = stringResource(R.string.debug_app_sessions_desc),
            icon = Icons.Default.Schedule,
            route = "debug_sessions",
            category = stringResource(R.string.debug_category_session_record),
            categoryColor = Color(0xFF9C27B0)
        ),
        DebugTableItem(
            title = stringResource(R.string.debug_timer_sessions_title),
            description = stringResource(R.string.debug_timer_sessions_desc),
            icon = Icons.Default.Timer,
            route = "debug_timer_sessions",
            category = stringResource(R.string.debug_category_session_record),
            categoryColor = Color(0xFF9C27B0)
        ),
        DebugTableItem(
            title = stringResource(R.string.debug_diagnosis_title),
            description = stringResource(R.string.debug_diagnosis_desc),
            icon = Icons.Default.BugReport,
            route = "debug_diagnosis",
            category = stringResource(R.string.debug_category_system_diagnosis),
            categoryColor = Color(0xFFF44336)
        ),
        DebugTableItem(
            title = stringResource(R.string.debug_data_consistency_title),
            description = stringResource(R.string.debug_data_consistency_desc),
            icon = Icons.Default.HealthAndSafety,
            route = "debug_data_consistency",
            category = stringResource(R.string.debug_category_system_diagnosis),
            categoryColor = Color(0xFFF44336)
        ),
        DebugTableItem(
            title = stringResource(R.string.debug_scaling_factor_title),
            description = stringResource(R.string.debug_scaling_factor_desc),
            icon = Icons.Default.Fullscreen,
            route = "debug_scaling_factor",
            category = stringResource(R.string.debug_category_system_diagnosis),
            categoryColor = Color(0xFFF44336)
        ),
        DebugTableItem(
            title = stringResource(R.string.debug_pie_chart_test_title),
            description = stringResource(R.string.debug_pie_chart_test_desc),
            icon = Icons.Default.PieChart,
            route = "debug_pie_chart_test",
            category = stringResource(R.string.debug_category_component_test),
            categoryColor = Color(0xFF673AB7)
        ),
        DebugTableItem(
            title = stringResource(R.string.debug_data_collection_title),
            description = stringResource(R.string.debug_data_collection_desc),
            icon = Icons.Default.DataUsage,
            route = "debug_data_collection",
            category = stringResource(R.string.debug_category_system_diagnosis),
            categoryColor = Color(0xFFF44336)
        ),
        DebugTableItem(
            title = stringResource(R.string.debug_realtime_stats_title),
            description = stringResource(R.string.debug_realtime_stats_desc),
            icon = Icons.Default.Speed,
            route = "debug_realtime_stats",
            category = stringResource(R.string.debug_category_system_diagnosis),
            categoryColor = Color(0xFFF44336)
        ),
        DebugTableItem(
            title = stringResource(R.string.debug_unified_update_title),
            description = stringResource(R.string.debug_unified_update_desc),
            icon = Icons.Default.Sync,
            route = "debug_unified_update",
            category = stringResource(R.string.debug_category_system_diagnosis),
            categoryColor = Color(0xFFF44336)
        ),
        DebugTableItem(
            title = stringResource(R.string.debug_data_validation_title),
            description = stringResource(R.string.debug_data_validation_desc),
            icon = Icons.Default.VerifiedUser,
            route = "debug_data_validation",
            category = stringResource(R.string.debug_category_system_diagnosis),
            categoryColor = Color(0xFFF44336)
        ),
        DebugTableItem(
            title = "数据初始化修复",
            description = "修复Google Play Debug版本无数据问题",
            icon = Icons.Default.Build,
            route = "debug_data_init",
            category = stringResource(R.string.debug_category_system_diagnosis),
            categoryColor = Color(0xFFF44336)
        )
    )
    
    // 按分类分组
    val groupedTables = debugTables.groupBy { it.category }
    val totalTables = debugTables.size
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            text = stringResource(R.string.debug_db_debug_center_title),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.debug_db_debug_center_subtitle),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.debug_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // 页面头部信息卡片
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.debug_data_table_overview_title),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = stringResource(R.string.debug_data_table_overview_desc, totalTables, groupedTables.size),
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = stringResource(R.string.debug_select_data_table_desc),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f),
                        lineHeight = 24.sp
                    )
                }
            }
            
            // 分类统计卡片
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                groupedTables.forEach { (category, tables) ->
                    val categoryColor = tables.first().categoryColor
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = categoryColor.copy(alpha = 0.1f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${tables.size}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = categoryColor
                            )
                            Text(
                                text = category,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 分类别显示数据表
            groupedTables.forEach { (category, tables) ->
                val categoryColor = tables.first().categoryColor
                
                // 分类标题
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(4.dp, 24.dp)
                            .background(
                                categoryColor,
                                RoundedCornerShape(2.dp)
                            )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = category,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = categoryColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${tables.size}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = categoryColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                
                // 该分类下的表格
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tables.forEach { table ->
                        DebugTableCard(
                            item = table,
                            onClick = { onNavigateToTable(table.route) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // 底部间距
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugTableCard(
    item: DebugTableItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        onClick = onClick
    ) {
        Box {
            // 渐变背景
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                item.categoryColor.copy(alpha = 0.8f),
                                item.categoryColor.copy(alpha = 0.3f)
                            )
                        )
                    )
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 图标背景
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = item.categoryColor.copy(alpha = 0.1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            modifier = Modifier.size(28.dp),
                            tint = item.categoryColor
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.description,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "进入",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
} 