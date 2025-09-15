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
import com.offtime.app.ui.theme.UILayoutMetricsManager
import com.offtime.app.ui.theme.UILayoutExtensions
import com.offtime.app.ui.theme.UIModule

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugLayoutMetricsScreen(
    modifier: Modifier = Modifier
) {
    val layoutMetrics = UILayoutMetricsManager.getHomeScreenLayoutMetrics()
    val screenInfo = UILayoutMetricsManager.getCurrentScreenInfo()
    val scaleFactor = UILayoutMetricsManager.getScaleFactor()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 标题
        Text(
            text = "UI布局度量调试信息",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // 屏幕信息卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "屏幕信息",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "宽度: ${screenInfo.widthDp}dp",
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "高度: ${screenInfo.heightDp}dp",
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "密度: ${String.format("%.2f", screenInfo.density)}",
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "缩放因子: ${String.format("%.2f", scaleFactor)}",
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        // 模块度量信息列表
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(layoutMetrics.toList()) { (module, metrics) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // 模块名称
                        Text(
                            text = getModuleDisplayName(module),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // 相对位置和大小
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "相对位置",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "X: ${String.format("%.2f%%", metrics.relativeX * 100)}",
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "Y: ${String.format("%.2f%%", metrics.relativeY * 100)}",
                                    fontSize = 12.sp
                                )
                            }
                            
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "相对大小",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "宽: ${String.format("%.2f%%", metrics.relativeWidth * 100)}",
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "高: ${String.format("%.2f%%", metrics.relativeHeight * 100)}",
                                    fontSize = 12.sp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // 绝对位置和大小
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "绝对位置",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "X: ${metrics.absoluteX}",
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "Y: ${metrics.absoluteY}",
                                    fontSize = 12.sp
                                )
                            }
                            
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "绝对大小",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "宽: ${metrics.absoluteWidth}",
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "高: ${metrics.absoluteHeight}",
                                    fontSize = 12.sp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // 间距信息
                        Column {
                            Text(
                                text = "间距信息",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "上: ${metrics.marginTop}",
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "下: ${metrics.marginBottom}",
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "左: ${metrics.marginLeft}",
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "右: ${metrics.marginRight}",
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 获取模块的显示名称
 */
private fun getModuleDisplayName(module: UIModule): String {
    return when (module) {
        UIModule.CATEGORY_BAR -> "分类选择栏"
        UIModule.TAB_SECTION -> "统计/详情选项卡"
        UIModule.MAIN_CHART -> "主图表区域 (饼图/柱状图)"
        UIModule.PERIOD_BUTTONS -> "日周月筛选按钮"
        UIModule.USAGE_LINE_CHART -> "使用时间折线图"
        UIModule.COMPLETION_LINE_CHART -> "完成率折线图"
        UIModule.REWARD_LINE_CHART -> "奖励完成度折线图"
        UIModule.PUNISHMENT_LINE_CHART -> "惩罚完成度折线图"
    }
} 