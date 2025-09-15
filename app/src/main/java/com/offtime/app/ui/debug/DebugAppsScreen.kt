package com.offtime.app.ui.debug

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import com.offtime.app.R
import com.offtime.app.ui.debug.viewmodel.DebugAppsViewModel
import com.offtime.app.ui.debug.viewmodel.DailyAppUsageData
import java.text.SimpleDateFormat
import java.util.*

/**
 * 应用信息表调试页面
 * 
 * 功能说明:
 * - 显示所有已安装应用的详细信息
 * - 按系统应用和用户应用分组显示
 * - 排除的应用显示在每组的底部
 * - 使用真实的应用图标提高识别度
 * - 提供详细的统计信息和状态标签
 * 
 * 数据来源: app_info 表
 * 
 * UI特性:
 * - 真实应用图标 + 首字母占位符
 * - 颜色编码的编号系统
 * - 分组显示(系统/用户应用)
 * - 排序逻辑(排除状态 -> 应用名)
 * - 实时统计信息
 * 
 * @param onNavigateBack 返回上级页面的回调
 * @param viewModel 应用信息调试的ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugAppsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: DebugAppsViewModel = hiltViewModel()
) {
    val appInfos by viewModel.appInfos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadAppInfos()
    }
    
    Scaffold(
        topBar = {
        TopAppBar(
                title = { 
                    Text(
                        text = stringResource(R.string.debug_app_info_screen_title),
                        fontWeight = FontWeight.Bold
                    ) 
                },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.debug_back))
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
        ) {
            // 应用信息内容
            AppInfoTab(appInfos, isLoading, viewModel)
        }
    }
}

@Composable
private fun AppInfoTab(
    appInfos: List<com.offtime.app.data.entity.AppInfoEntity>,
    isLoading: Boolean,
    viewModel: DebugAppsViewModel
) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
        LazyColumn {
            item {
                // 统计信息
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
    ) {
                    Column(modifier = Modifier.padding(16.dp)) {
            Text(
                            text = "统计信息",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("总应用数：${appInfos.size}")
                        Text("真实应用：${appInfos.count { !it.packageName.startsWith("com.offtime.offline.") }}")
                        Text("虚拟应用：${appInfos.count { it.packageName.startsWith("com.offtime.offline.") }}")
                        Text("已排除应用：${appInfos.count { it.isExcluded }}")
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.debugOffTimesAppStatus() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("调试OffTimes应用状态")
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = { viewModel.fixOffTimesAppTracking() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("修复OffTimes应用统计")
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = { viewModel.debugCurrentActiveApps() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Text("调试当前活跃应用")
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = { viewModel.startOffTimesRealTimeMonitoring() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("实时监控OffTimes统计")
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = { viewModel.monitorAllAppEventTypes() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("监控所有应用事件类型")
                        }
        }
    }
}

            items(appInfos) { appInfo ->
                AppInfoItem(appInfo = appInfo, onDeleteClick = {
                    viewModel.deleteApp(appInfo.packageName)
                })
            }
        }
    }
}





// 辅助函数
private fun getDateString(daysOffset: Int): String {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, daysOffset)
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
}

/**
 * 应用信息卡片
 * 显示单个应用的详细信息
 * 
 * 布局结构:
 * - 左侧: 编号和颜色指示器
 * - 中间: 真实应用图标
 * - 右侧: 应用信息(名称、包名、状态标签)
 * 
 * @param app 应用实体
 * @param appColor 应用对应的颜色
 * @param recordNumber 记录编号
 */
@Composable
private fun AppInfoItem(
    appInfo: com.offtime.app.data.entity.AppInfoEntity,
    onDeleteClick: () -> Unit
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
            NumberIndicator(
                number = 1,
                color = getAppDebugColor(appInfo.categoryId)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 应用图标 - 使用真实的应用图标
            AppIcon(
                packageName = appInfo.packageName,
                appName = appInfo.appName,
                modifier = Modifier.size(40.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 应用信息
            AppInfoContent(app = appInfo)
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 删除按钮
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, contentDescription = "删除应用")
            }
        }
    }
}

/**
 * 编号指示器
 * 显示彩色圆形编号和小的颜色指示点
 * 
 * @param number 编号
 * @param color 颜色
 */
@Composable
private fun NumberIndicator(number: Int, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(48.dp)
    ) {
        // 编号圆圈
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
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
                .background(color, CircleShape)
        )
    }
}

/**
 * 应用信息内容
 * 显示应用名称、包名和状态标签
 * 
 * @param app 应用实体
 */
@Composable
private fun AppInfoContent(app: com.offtime.app.data.entity.AppInfoEntity) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 应用名称 - 使用智能显示逻辑
        Text(
            text = getDisplayAppName(app.packageName, app.appName),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF212121)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // 包名
        Text(
            text = app.packageName,
            fontSize = 12.sp,
            color = Color(0xFF666666)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // 状态标签行
        StatusLabels(app = app)
    }
}

/**
 * 状态标签
 * 显示分类ID、排除状态、启用状态等标签
 * 
 * @param app 应用实体
 */
@Composable
private fun StatusLabels(app: com.offtime.app.data.entity.AppInfoEntity) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 分类ID标签
        Text(
            text = "分类ID: ${app.categoryId}",
            fontSize = 10.sp,
            color = Color(0xFF999999),
            modifier = Modifier
                .background(Color(0xFFF0F0F0), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        )
        
        // 排除状态标签
        if (app.isExcluded) {
            Text(
                text = "已排除",
                fontSize = 10.sp,
                color = Color.White,
                modifier = Modifier
                    .background(Color(0xFFF44336), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }
        
        // 禁用状态标签
        if (!app.isEnabled) {
            Text(
                text = "已禁用",
                fontSize = 10.sp,
                color = Color.White,
                modifier = Modifier
                    .background(Color(0xFF9E9E9E), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }
    }
}

/**
 * 应用图标组件
 * 
 * 功能特性:
 * - 优先显示真实的应用图标
 * - 获取失败时显示首字母占位符
 * - 使用LaunchedEffect进行异步加载
 * - 通过remember进行缓存优化
 * - 圆角矩形容器设计
 * 
 * 性能优化:
 * - 图标缓存避免重复加载
 * - 异步加载不阻塞UI
 * - 96x96像素适中的图标尺寸
 * - 优雅的错误处理
 * 
 * @param packageName 应用包名
 * @param appName 应用名称(用于占位符)
 * @param modifier 修饰符
 */
@Composable
private fun AppIcon(
    packageName: String,
    appName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // 图标状态管理 - 使用remember进行缓存
    var appIcon by remember { mutableStateOf<androidx.compose.ui.graphics.painter.Painter?>(null) }
    
    // 异步加载应用图标
    LaunchedEffect(packageName) {
        try {
            // 从PackageManager获取应用图标
            val drawable = context.packageManager.getApplicationIcon(packageName)
            // 转换为Bitmap并创建Painter
            val bitmap = drawable.toBitmap(96, 96) // 96x96像素的图标
            appIcon = BitmapPainter(bitmap.asImageBitmap())
        } catch (e: Exception) {
            // 获取失败时设置为null，显示占位符
            appIcon = null
        }
    }
    
    // 图标容器
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp)) // 圆角矩形
            .background(
                // 根据是否有图标设置背景色
                if (appIcon == null) MaterialTheme.colorScheme.primaryContainer 
                else Color.Transparent
            ),
        contentAlignment = Alignment.Center
    ) {
        if (appIcon != null) {
            // 显示真实应用图标
            Image(
                painter = appIcon!!,
                contentDescription = appName,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // 显示首字母占位符
            Text(
                text = appName.take(1).uppercase(), // 取应用名首字母并大写
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/**
 * 根据分类ID获取调试颜色
 * 
 * 使用10色调色板，根据分类ID循环使用
 * 未分类应用(categoryId <= 0)使用灰色
 * 
 * @param categoryId 分类ID
 * @return 对应的颜色
 */
private fun getAppDebugColor(categoryId: Int): Color {
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
    return if (categoryId > 0) colors[categoryId % colors.size] else Color(0xFF9E9E9E)
}

/**
 * 智能应用名称显示
 * 
 * 显示逻辑:
 * 1. 如果应用名称不为空且不等于包名，使用应用名称
 * 2. 否则根据包名特征进行智能识别
 * 3. 最后回退到包名解析
 * 
 * 支持的应用识别:
 * - 国际应用: YouTube, Chrome, Gmail, WhatsApp等
 * - 国内应用: 微信, QQ, 淘宝, 支付宝, 抖音等
 * - 厂商应用: 小米, 华为, OPPO, vivo, 三星等
 * 
 * @param packageName 应用包名
 * @param appName 应用名称
 * @return 显示用的应用名称
 */
private fun getDisplayAppName(packageName: String, appName: String): String {
    // 如果应用名称不为空且不等于包名，使用应用名称
    if (appName.isNotEmpty() && appName != packageName) {
        return appName
    }
    
    // 根据包名特征进行智能识别
    return when {
        // 国际知名应用
        packageName.contains("youtube", ignoreCase = true) -> "YouTube"
        packageName.contains("chrome", ignoreCase = true) -> "Chrome"
        packageName.contains("maps", ignoreCase = true) -> "Google Maps"
        packageName.contains("gmail", ignoreCase = true) -> "Gmail"
        packageName.contains("whatsapp", ignoreCase = true) -> "WhatsApp"
        
        // 国内主流应用
        packageName.contains("wechat", ignoreCase = true) -> "微信"
        packageName.contains("qq", ignoreCase = true) -> "QQ"
        packageName.contains("taobao", ignoreCase = true) -> "淘宝"
        packageName.contains("alipay", ignoreCase = true) -> "支付宝"
        packageName.contains("douyin", ignoreCase = true) -> "抖音"
        packageName.contains("bilibili", ignoreCase = true) -> "哔哩哔哩"
        packageName.contains("netease", ignoreCase = true) -> "网易云音乐"
        
        // 厂商应用识别
        packageName.contains("tencent", ignoreCase = true) -> "腾讯应用"
        packageName.contains("baidu", ignoreCase = true) -> "百度应用"
        packageName.contains("xiaomi", ignoreCase = true) -> "小米应用"
        packageName.contains("huawei", ignoreCase = true) -> "华为应用"
        packageName.contains("oppo", ignoreCase = true) -> "OPPO应用"
        packageName.contains("vivo", ignoreCase = true) -> "vivo应用"
        packageName.contains("samsung", ignoreCase = true) -> "三星应用"
        
        else -> {
            // 从包名中提取应用名称
            val parts = packageName.split(".")
            parts.lastOrNull()?.replaceFirstChar { it.uppercase() } ?: packageName
        }
    }
} 