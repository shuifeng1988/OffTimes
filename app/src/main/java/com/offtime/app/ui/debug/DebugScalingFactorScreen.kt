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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.offtime.app.utils.ScalingFactorUtils

/**
 * 缩放因子调试屏幕
 * 
 * 用于显示当前设备的屏幕信息和缩放因子，
 * 帮助开发者了解适配效果。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScalingFactorScreen(
    onNavigateBack: () -> Unit = {}
) {
    val screenMetrics = ScalingFactorUtils.getScreenMetrics()
    val scalingFactors = ScalingFactorUtils.calculateScalingFactors()
    val adaptiveSpacing = ScalingFactorUtils.getAdaptiveSpacing()
    val adaptiveFontSizes = ScalingFactorUtils.getAdaptiveFontSizes()
    val adaptiveChartSizes = ScalingFactorUtils.getAdaptiveChartSizes()
    val screenTypeDescription = ScalingFactorUtils.getScreenTypeDescription()
    val detailedScalingInfo = ScalingFactorUtils.getDetailedScalingInfo()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "缩放因子调试",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 屏幕类型描述
            InfoCard(
                title = "设备分类",
                content = screenTypeDescription
            )
            
            // 详细缩放信息
            InfoCard(
                title = "详细缩放信息",
                content = detailedScalingInfo
            )
            
            // 屏幕度量信息
            InfoCard(
                title = "屏幕度量信息",
                content = buildString {
                    append("宽度: ${screenMetrics.widthDp.toInt()}dp (${screenMetrics.widthPx}px)\n")
                    append("高度: ${screenMetrics.heightDp.toInt()}dp (${screenMetrics.heightPx}px)\n")
                    append("密度: ${String.format("%.2f", screenMetrics.density)}\n")
                    append("对角线: ${String.format("%.1f", screenMetrics.diagonalDp)}dp\n")
                    append("宽高比: ${String.format("%.2f", screenMetrics.aspectRatio)}")
                }
            )
            
            // 缩放因子信息
            InfoCard(
                title = "缩放因子",
                content = buildString {
                    append("全局缩放: ${String.format("%.2f", scalingFactors.globalScale)}\n")
                    append("字体缩放: ${String.format("%.2f", scalingFactors.fontScale)}\n")
                    append("间距缩放: ${String.format("%.2f", scalingFactors.spacingScale)}\n")
                    append("图标缩放: ${String.format("%.2f", scalingFactors.iconScale)}\n")
                    append("图表缩放: ${String.format("%.2f", scalingFactors.chartScale)}\n")
                    append("密度调整: ${String.format("%.2f", scalingFactors.densityAdjustment)}")
                }
            )
            
            // 适配间距信息
            InfoCard(
                title = "适配间距 (dp)",
                content = buildString {
                    append("XSmall: ${adaptiveSpacing.xSmall.value.toInt()}dp\n")
                    append("Small: ${adaptiveSpacing.small.value.toInt()}dp\n")
                    append("Medium: ${adaptiveSpacing.medium.value.toInt()}dp\n")
                    append("Large: ${adaptiveSpacing.large.value.toInt()}dp\n")
                    append("XLarge: ${adaptiveSpacing.xLarge.value.toInt()}dp")
                }
            )
            
            // 适配字体信息
            InfoCard(
                title = "适配字体 (sp)",
                content = buildString {
                    append("XSmall: ${String.format("%.1f", adaptiveFontSizes.xSmall.value)}sp\n")
                    append("Small: ${String.format("%.1f", adaptiveFontSizes.small.value)}sp\n")
                    append("Medium: ${String.format("%.1f", adaptiveFontSizes.medium.value)}sp\n")
                    append("Large: ${String.format("%.1f", adaptiveFontSizes.large.value)}sp\n")
                    append("XLarge: ${String.format("%.1f", adaptiveFontSizes.xLarge.value)}sp\n")
                    append("XXLarge: ${String.format("%.1f", adaptiveFontSizes.xxLarge.value)}sp")
                }
            )
            
            // 适配图表尺寸信息
            InfoCard(
                title = "适配图表尺寸 (dp)",
                content = buildString {
                    append("折线图高度: ${adaptiveChartSizes.lineChartHeight.value.toInt()}dp\n")
                    append("饼图高度: ${adaptiveChartSizes.pieChartHeight.value.toInt()}dp\n")
                    append("柱状图高度: ${adaptiveChartSizes.barChartHeight.value.toInt()}dp")
                }
            )
            
            // 百分比示例
            InfoCard(
                title = "百分比示例",
                content = buildString {
                    append("宽度10%: ${ScalingFactorUtils.widthPercent(0.1f).value.toInt()}dp\n")
                    append("宽度25%: ${ScalingFactorUtils.widthPercent(0.25f).value.toInt()}dp\n")
                    append("高度10%: ${ScalingFactorUtils.heightPercent(0.1f).value.toInt()}dp\n")
                    append("高度25%: ${ScalingFactorUtils.heightPercent(0.25f).value.toInt()}dp")
                }
            )
            
            // 视觉示例
            Text(
                text = "视觉示例",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            // 字体大小示例
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("XSmall字体示例", fontSize = adaptiveFontSizes.xSmall)
                Text("Small字体示例", fontSize = adaptiveFontSizes.small)
                Text("Medium字体示例", fontSize = adaptiveFontSizes.medium)
                Text("Large字体示例", fontSize = adaptiveFontSizes.large)
                Text("XLarge字体示例", fontSize = adaptiveFontSizes.xLarge)
                Text("XXLarge字体示例", fontSize = adaptiveFontSizes.xxLarge)
            }
            
            // 间距示例
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(adaptiveSpacing.medium),
                    verticalArrangement = Arrangement.spacedBy(adaptiveSpacing.small)
                ) {
                    Text("这是一个使用适配间距的卡片示例")
                    Text("内边距: Medium")
                    Text("垂直间距: Small")
                }
            }
            
            // 底部留白
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    content: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
} 