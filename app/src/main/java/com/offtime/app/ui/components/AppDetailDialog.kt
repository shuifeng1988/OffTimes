package com.offtime.app.ui.components

import android.content.pm.PackageManager

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import com.offtime.app.ui.viewmodel.HomeViewModel
import com.offtime.app.utils.CategoryUtils
import kotlinx.coroutines.delay
import com.offtime.app.utils.DateLocalizer



/**
 * 应用详情弹出对话框
 * 显示某个分类下的应用使用时长排列，按时长从长到短排序
 * 
 * @param showDialog 是否显示对话框
 * @param title 对话框标题，格式："娱乐分类 14点应用详情"
 * @param appDetailList 应用详情列表
 * @param onDismiss 关闭对话框的回调
 * @param autoCloseDelay 自动关闭延迟时间（毫秒），默认15秒
 */
@Composable
fun AppDetailDialog(
    showDialog: Boolean,
    title: String,
    appDetailList: List<HomeViewModel.AppDetailItem>,
    onDismiss: () -> Unit,
    autoCloseDelay: Long = 15000L // 15秒自动关闭
) {

    
    // 自动关闭机制
    LaunchedEffect(showDialog) {
        if (showDialog) {
            delay(autoCloseDelay)
            onDismiss()
        }
    }
    
    if (showDialog) {
        Dialog(onDismissRequest = onDismiss) {
            // 根据应用数量动态计算对话框高度
            val dynamicHeight = calculateDialogHeight(appDetailList.size)
            
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .fillMaxHeight(dynamicHeight),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // 解析标题信息
                    val titleInfo = parseTitleInfo(title)
                    
                    // 第一行：分类图标 + 分类名称 + 时间段 + 关闭按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // 分类emoji图标
                            Text(
                                text = CategoryUtils.getCategoryEmoji(titleInfo.categoryName),
                                fontSize = 24.sp
                            )
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            // 分类名称
                            Text(
                                text = DateLocalizer.getCategoryName(LocalContext.current, titleInfo.categoryName),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937)
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            // 时间段
                            Text(
                                text = titleInfo.timeRange,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF6B7280),
                                modifier = Modifier
                                    .background(
                                        Color(0xFFF3F4F6),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        
                        // 关闭按钮
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "关闭",
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 第二行：统计信息
                    val realAppList = appDetailList.filter { !it.packageName.startsWith("suggestion_") }
                    val totalUsage = realAppList.sumOf { it.totalUsageSeconds }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        // 使用时间（新格式）
                        Text(
                            text = "使用时间：${formatDurationWithUnit(totalUsage)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF374151)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // 第三行开始：应用列表
                    if (appDetailList.isEmpty()) {
                        // 空状态
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Apps,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = Color(0xFFBDBDBD)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "该时段暂无应用使用记录",
                                    fontSize = 16.sp,
                                    color = Color(0xFF9E9E9E)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(realAppList) { appDetail ->
                                SimpleAppDetailItem(appDetail = appDetail)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 标题信息数据类
 */
private data class TitleInfo(
    val categoryName: String,
    val timeRange: String
)

/**
 * 解析标题信息
 * 从"娱乐分类 14点应用详情"解析出分类名称和时间段
 */
private fun parseTitleInfo(title: String): TitleInfo {
    // 提取分类名称
    val parts = title.split("分类")
    val categoryName = if (parts.isNotEmpty()) parts[0].trim() else "应用"
    
    // 提取小时信息
    val hourMatch = Regex("(\\d+)点").find(title)
    val hour = hourMatch?.groupValues?.get(1)?.toIntOrNull()
    
    // 构造时间段
    val timeRange = if (hour != null) {
        // 柱状图点击：显示具体小时段
        String.format("%02d:00-%02d:59", hour, hour)
    } else {
        // 饼图点击：显示今天00:00到现在时间
        val calendar = java.util.Calendar.getInstance()
        val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(java.util.Calendar.MINUTE)
        String.format("00:00-%02d:%02d", currentHour, currentMinute)
    }
    
    return TitleInfo(categoryName, timeRange)
}



/**
 * 格式化时长显示（简化版）- 自动选择合适单位
 */
private fun formatDurationSimple(seconds: Int): String {
    return if (seconds < 3600) {
        // 小于1小时，显示分钟
        val minutes = (seconds / 60.0)
        "${String.format("%.1f", minutes)}m"
    } else {
        // 大于等于1小时，显示小时
        val hours = seconds / 3600.0
        "${String.format("%.1f", hours)}h"
    }
}

/**
 * 格式化时长显示（带单位标识）- 自动选择合适单位
 */
private fun formatDurationWithUnit(seconds: Int): String {
    return if (seconds < 3600) {
        // 小于1小时，显示分钟
        val minutes = (seconds / 60.0)
        "${String.format("%.1f", minutes)}分钟(m)"
    } else {
        // 大于等于1小时，显示小时
        val hours = seconds / 3600.0
        "${String.format("%.1f", hours)}小时(h)"
    }
}

/**
 * 格式化时长显示（应用列表专用）- 自动选择合适单位
 */
private fun formatDurationForAppList(seconds: Int): String {
    return if (seconds < 3600) {
        // 小于1小时，显示分钟
        val minutes = (seconds / 60.0)
        "${String.format("%.1f", minutes)}m"
    } else {
        // 大于等于1小时，显示小时
        val hours = seconds / 3600.0
        "${String.format("%.1f", hours)}h"
    }
}

/**
 * 根据应用数量计算合适的对话框高度
 */
private fun calculateDialogHeight(appCount: Int): Float {
    return when {
        appCount == 0 -> 0.4f  // 空状态，较小高度
        appCount <= 2 -> 0.5f  // 1-2个应用
        appCount <= 4 -> 0.65f // 3-4个应用
        appCount <= 6 -> 0.75f // 5-6个应用
        else -> 0.85f          // 7个以上应用，接近全屏
    }
}

/**
 * 简化版应用详情项 - 图标、应用名、时长、占比
 */
@Composable
private fun SimpleAppDetailItem(
    appDetail: HomeViewModel.AppDetailItem
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(0xFFFAFAFA),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 应用图标
        AppIcon(
            packageName = appDetail.packageName,
            appName = appDetail.appName,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(10.dp))
        
        // 应用名称
        Text(
            text = appDetail.appName,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1F2937),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        
        // 使用时长（使用新格式）
        Text(
            text = formatDurationForAppList(appDetail.totalUsageSeconds),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF1976D2)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // 使用占比
        Text(
            text = "${String.format("%.1f", appDetail.usagePercentage)}%",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF6B7280)
        )
    }
}

/**
 * 应用图标组件
 */
@Composable
private fun AppIcon(
    packageName: String,
    appName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var appIconBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var hasValidIcon by remember { mutableStateOf(false) }
    
    LaunchedEffect(packageName) {
        try {
            if (packageName.startsWith("timer_")) {
                // 定时器会话，使用特殊图标
                appIconBitmap = null
                hasValidIcon = false
            } else {
                // 普通应用，获取真实图标
                val pm = context.packageManager
                val appIcon = pm.getApplicationIcon(packageName)
                val bitmap = appIcon.toBitmap(96, 96)
                appIconBitmap = bitmap.asImageBitmap()
                hasValidIcon = true
            }
        } catch (e: Exception) {
            appIconBitmap = null
            hasValidIcon = false
        }
    }
    
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                if (hasValidIcon) Color.Transparent else Color(0xFFE3F2FD)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (hasValidIcon && appIconBitmap != null) {
            // 显示真实应用图标
            Image(
                bitmap = appIconBitmap!!,
                contentDescription = appName,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // 显示默认图标（定时器或加载失败）
            DefaultAppIcon(appName)
        }
    }
}

/**
 * 默认应用图标
 */
@Composable
private fun DefaultAppIcon(appName: String) {
    val iconText = if (appName.startsWith("⏱️")) {
        "⏱️"
    } else {
        appName.firstOrNull()?.uppercase() ?: "A"
    }
    
    Text(
        text = iconText,
        fontSize = if (iconText == "⏱️") 24.sp else 20.sp,
        fontWeight = FontWeight.Bold,
        color = if (iconText == "⏱️") Color(0xFFFF6B35) else Color(0xFF1976D2)
    )
}

/**
 * 格式化时长显示
 */
private fun formatDuration(seconds: Int): String {
    return when {
        seconds < 60 -> "${seconds}秒"
        seconds < 3600 -> "${seconds / 60}分${seconds % 60}秒"
        else -> {
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60
            if (minutes > 0) {
                "${hours}小时${minutes}分"
            } else {
                "${hours}小时"
            }
        }
    }
} 