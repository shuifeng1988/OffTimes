package com.offtime.app.ui.debug

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offtime.app.utils.LayoutMetricsManager
import com.offtime.app.utils.ModuleLayoutInfo

/**
 * 布局调试页面
 * 显示首页各个模块的详细位置和大小信息
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugLayoutScreen(
    modifier: Modifier = Modifier
) {
    val screenInfo = LayoutMetricsManager.screenInfo
    
    // 定时刷新布局信息
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(2000) // 每2秒刷新一次
            // 触发重组以获取最新的布局信息
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "布局调试信息",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 屏幕信息卡片
            item {
                ScreenInfoCard(screenInfo = screenInfo)
            }
            
            // 模块列表
            item {
                Text(
                    text = "模块详细信息",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            val visibleModules = LayoutMetricsManager.getVisibleModules()
            items(visibleModules) { moduleInfo ->
                ModuleInfoCard(moduleInfo = moduleInfo)
            }
            
            // 重叠模块检测
            item {
                val overlappingModules = LayoutMetricsManager.getOverlappingModules()
                if (overlappingModules.isNotEmpty()) {
                    OverlappingModulesCard(overlappingPairs = overlappingModules)
                }
            }
            
            // 布局报告
            item {
                LayoutReportCard()
            }
        }
    }
}

/**
 * 屏幕信息卡片
 */
@Composable
private fun ScreenInfoCard(
    screenInfo: com.offtime.app.utils.LayoutScreenInfo
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "屏幕信息",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            InfoRow("尺寸 (DP)", "${screenInfo.widthDp} × ${screenInfo.heightDp}")
            InfoRow("尺寸 (PX)", "${screenInfo.widthPx.toInt()} × ${screenInfo.heightPx.toInt()}")
            InfoRow("密度", String.format("%.2f", screenInfo.density))
            InfoRow("宽高比", String.format("%.2f", screenInfo.aspectRatio))
            InfoRow("方向", if (screenInfo.isLandscape) "横屏" else "竖屏")
        }
    }
}

/**
 * 模块信息卡片
 */
@Composable
private fun ModuleInfoCard(
    moduleInfo: ModuleLayoutInfo
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // 模块标题
            Text(
                text = moduleInfo.moduleName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // 基本信息
            InfoRow("ID", moduleInfo.moduleId, isCode = true)
            InfoRow("可见", if (moduleInfo.isVisible) "是" else "否")
            InfoRow("层级", moduleInfo.zIndex.toString())
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // 大小信息
            Text(
                text = "大小信息",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            InfoRow("DP大小", "${moduleInfo.dpSize.width} × ${moduleInfo.dpSize.height}")
            InfoRow("像素大小", "${moduleInfo.absoluteSize.width.toInt()} × ${moduleInfo.absoluteSize.height.toInt()}")
            InfoRow("相对大小", "${String.format("%.1f", moduleInfo.relativeWidthPercent)}% × ${String.format("%.1f", moduleInfo.relativeHeightPercent)}%")
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // 位置信息
            Text(
                text = "位置信息",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            InfoRow("相对位置", "(${String.format("%.1f", moduleInfo.relativeLeftPercent)}%, ${String.format("%.1f", moduleInfo.relativeTopPercent)}%)")
            InfoRow("占屏比例", "左上(${String.format("%.1f", moduleInfo.relativeLeftPercent)}%, ${String.format("%.1f", moduleInfo.relativeTopPercent)}%) 右下(${String.format("%.1f", moduleInfo.relativeRightPercent)}%, ${String.format("%.1f", moduleInfo.relativeBottomPercent)}%)")
        }
    }
}

/**
 * 重叠模块卡片
 */
@Composable
private fun OverlappingModulesCard(
    overlappingPairs: List<Pair<ModuleLayoutInfo, ModuleLayoutInfo>>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "⚠️ 模块重叠检测",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            overlappingPairs.forEach { (module1, module2) ->
                Text(
                    text = "• ${module1.moduleName} 与 ${module2.moduleName}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

/**
 * 布局报告卡片
 */
@Composable
private fun LayoutReportCard() {
    var reportText by remember { mutableStateOf("") }
    
    LaunchedEffect(Unit) {
        reportText = LayoutMetricsManager.generateLayoutReport()
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "完整布局报告",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = reportText,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp)
            )
        }
    }
}

/**
 * 信息行组件
 */
@Composable
private fun InfoRow(
    label: String,
    value: String,
    isCode: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        
        Text(
            text = value,
            fontSize = 12.sp,
            fontFamily = if (isCode) FontFamily.Monospace else FontFamily.Default,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
} 