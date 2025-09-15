package com.offtime.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * UI布局扩展函数
 */
object UILayoutExtensions {
    
    /**
     * 获取模块的相对宽度（屏幕宽度的百分比）
     */
    @Composable
    fun getModuleRelativeWidth(module: UIModule): Float {
        val metrics = UILayoutMetricsManager.getHomeScreenLayoutMetrics()
        return metrics[module]?.relativeWidth ?: 0f
    }
    
    /**
     * 获取模块的相对高度（屏幕高度的百分比）
     */
    @Composable
    fun getModuleRelativeHeight(module: UIModule): Float {
        val metrics = UILayoutMetricsManager.getHomeScreenLayoutMetrics()
        return metrics[module]?.relativeHeight ?: 0f
    }
    
    /**
     * 获取模块的相对X位置（屏幕宽度的百分比）
     */
    @Composable
    fun getModuleRelativeX(module: UIModule): Float {
        val metrics = UILayoutMetricsManager.getHomeScreenLayoutMetrics()
        return metrics[module]?.relativeX ?: 0f
    }
    
    /**
     * 获取模块的相对Y位置（屏幕高度的百分比）
     */
    @Composable
    fun getModuleRelativeY(module: UIModule): Float {
        val metrics = UILayoutMetricsManager.getHomeScreenLayoutMetrics()
        return metrics[module]?.relativeY ?: 0f
    }
    
    /**
     * 获取模块的绝对宽度
     */
    @Composable
    fun getModuleAbsoluteWidth(module: UIModule): Dp {
        val metrics = UILayoutMetricsManager.getHomeScreenLayoutMetrics()
        return metrics[module]?.absoluteWidth ?: 0.dp
    }
    
    /**
     * 获取模块的绝对高度
     */
    @Composable
    fun getModuleAbsoluteHeight(module: UIModule): Dp {
        val metrics = UILayoutMetricsManager.getHomeScreenLayoutMetrics()
        return metrics[module]?.absoluteHeight ?: 0.dp
    }
    
    /**
     * 获取模块的间距信息
     */
    @Composable
    fun getModuleMargins(module: UIModule): Margins {
        val metrics = UILayoutMetricsManager.getHomeScreenLayoutMetrics()
        val moduleMetrics = metrics[module]
        return Margins(
            top = moduleMetrics?.marginTop ?: 0.dp,
            bottom = moduleMetrics?.marginBottom ?: 0.dp,
            left = moduleMetrics?.marginLeft ?: 0.dp,
            right = moduleMetrics?.marginRight ?: 0.dp
        )
    }
    
    /**
     * 根据屏幕百分比计算宽度
     */
    @Composable
    fun screenPercentageWidth(percentage: Float): Dp {
        val configuration = LocalConfiguration.current
        return (configuration.screenWidthDp * percentage).dp
    }
    
    /**
     * 根据屏幕百分比计算高度
     */
    @Composable
    fun screenPercentageHeight(percentage: Float): Dp {
        val configuration = LocalConfiguration.current
        return (configuration.screenHeightDp * percentage).dp
    }
    
    /**
     * 获取布局调试信息（仅在调试模式下显示）
     */
    @Composable
    fun getLayoutDebugInfo(): String {
        val metrics = UILayoutMetricsManager.getHomeScreenLayoutMetrics()
        val screenInfo = UILayoutMetricsManager.getCurrentScreenInfo()
        
        return buildString {
            appendLine("屏幕信息:")
            appendLine("  宽度: ${screenInfo.widthDp}dp")
            appendLine("  高度: ${screenInfo.heightDp}dp")
            appendLine("  密度: ${screenInfo.density}")
            appendLine()
            appendLine("模块布局信息:")
            metrics.forEach { (module, metric) ->
                appendLine("${module.name}:")
                appendLine("  相对位置: (${String.format("%.2f", metric.relativeX)}, ${String.format("%.2f", metric.relativeY)})")
                appendLine("  相对大小: ${String.format("%.2f", metric.relativeWidth)} x ${String.format("%.2f", metric.relativeHeight)}")
                appendLine("  绝对位置: (${metric.absoluteX}, ${metric.absoluteY})")
                appendLine("  绝对大小: ${metric.absoluteWidth} x ${metric.absoluteHeight}")
                appendLine()
            }
        }
    }
}

/**
 * 间距数据类
 */
data class Margins(
    val top: Dp,
    val bottom: Dp,
    val left: Dp,
    val right: Dp
)

/**
 * Modifier扩展：添加布局度量跟踪
 */
fun Modifier.trackLayoutMetrics(
    module: UIModule,
    isDebugMode: Boolean = false
): Modifier = this.then(
    if (isDebugMode) {
        drawBehind {
            // 在调试模式下绘制模块边界
            drawModuleBorder(module)
        }
    } else {
        Modifier
    }
)

/**
 * 绘制模块边界（调试用）
 */
private fun DrawScope.drawModuleBorder(module: UIModule) {
    val borderColor = when (module) {
        UIModule.CATEGORY_BAR -> Color.Red
        UIModule.TAB_SECTION -> Color.Blue
        UIModule.MAIN_CHART -> Color.Green
        UIModule.PERIOD_BUTTONS -> Color.Yellow
        UIModule.USAGE_LINE_CHART -> Color.Cyan
        UIModule.COMPLETION_LINE_CHART -> Color.Magenta
        UIModule.REWARD_LINE_CHART -> Color(0xFFFF8C00) // Orange
        UIModule.PUNISHMENT_LINE_CHART -> Color(0xFF8B0000) // DarkRed
    }
    
    drawRect(
        color = borderColor,
        topLeft = Offset.Zero,
        size = Size(size.width, size.height),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
    )
} 