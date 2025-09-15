package com.offtime.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * UI模块标识
 */
enum class UIModule {
    CATEGORY_BAR,           // 分类选择栏
    TAB_SECTION,           // 统计/详情选项卡
    MAIN_CHART,            // 主图表区域（饼图/柱状图）
    PERIOD_BUTTONS,        // 日周月筛选按钮
    USAGE_LINE_CHART,      // 使用时间折线图
    COMPLETION_LINE_CHART, // 完成率折线图
    REWARD_LINE_CHART,     // 奖励完成度折线图
    PUNISHMENT_LINE_CHART  // 惩罚完成度折线图
}

/**
 * UI模块度量数据
 */
data class UIModuleMetrics(
    // 相对位置 (相对于屏幕的百分比 0.0-1.0)
    val relativeX: Float,          // 相对于屏幕宽度的X位置
    val relativeY: Float,          // 相对于屏幕高度的Y位置
    
    // 相对大小 (相对于屏幕的百分比 0.0-1.0)
    val relativeWidth: Float,      // 相对于屏幕宽度的宽度
    val relativeHeight: Float,     // 相对于屏幕高度的高度
    
    // 绝对位置和大小 (用于计算和参考)
    val absoluteX: Dp,             // 绝对X位置
    val absoluteY: Dp,             // 绝对Y位置
    val absoluteWidth: Dp,         // 绝对宽度
    val absoluteHeight: Dp,        // 绝对高度
    
    // 间距信息
    val marginTop: Dp,             // 上边距
    val marginBottom: Dp,          // 下边距
    val marginLeft: Dp,            // 左边距
    val marginRight: Dp            // 右边距
)

/**
 * UI布局度量管理器
 */
object UILayoutMetricsManager {
    
    // 存储各模块的度量数据
    private val moduleMetrics = mutableMapOf<UIModule, UIModuleMetrics>()
    
    /**
     * 计算并获取首页各模块的标准布局度量
     */
    @Composable
    fun getHomeScreenLayoutMetrics(): Map<UIModule, UIModuleMetrics> {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp.dp
        val screenHeight = configuration.screenHeightDp.dp
        
        // 根据屏幕尺寸计算自适应参数（保持与原HomeScreen逻辑一致）
        val isSmallScreen = screenHeight < 700.dp
        val isMediumScreen = screenHeight >= 700.dp && screenHeight <= 800.dp
        val isLargeScreen = screenHeight > 800.dp
        
        val baseVerticalSpacing = when {
            isSmallScreen -> 3.dp
            isMediumScreen -> 3.dp
            isLargeScreen -> 6.dp
            else -> 4.dp
        }
        
        val chartSpacing = when {
            isSmallScreen -> baseVerticalSpacing * 3
            isMediumScreen -> baseVerticalSpacing * 3
            isLargeScreen -> baseVerticalSpacing * 4
            else -> baseVerticalSpacing * 4
        }
        
        val buttonToChartSpacing = when {
            isSmallScreen -> 6.dp
            isMediumScreen -> 6.dp
            isLargeScreen -> 12.dp
            else -> 8.dp
        }
        
        val horizontalPadding = when {
            screenWidth < 360.dp -> 12.dp
            screenWidth > 480.dp -> 20.dp
            else -> 14.dp
        }
        
        val chartHeight = when {
            isSmallScreen -> 154.dp
            isMediumScreen -> 165.dp
            isLargeScreen -> 198.dp
            else -> 176.dp
        }
        
        val pieChartHeight = when {
            isSmallScreen -> 180.dp
            isMediumScreen -> 190.dp
            isLargeScreen -> 220.dp
            else -> 200.dp
        }
        
        // 计算各模块的布局度量
        val metrics = mutableMapOf<UIModule, UIModuleMetrics>()
        
        var currentY = baseVerticalSpacing
        
        // 1. 分类选择栏
        val categoryBarHeight = 40.dp
        metrics[UIModule.CATEGORY_BAR] = UIModuleMetrics(
            relativeX = horizontalPadding.value / screenWidth.value,
            relativeY = currentY.value / screenHeight.value,
            relativeWidth = (screenWidth - horizontalPadding * 2).value / screenWidth.value,
            relativeHeight = categoryBarHeight.value / screenHeight.value,
            absoluteX = horizontalPadding,
            absoluteY = currentY,
            absoluteWidth = screenWidth - horizontalPadding * 2,
            absoluteHeight = categoryBarHeight,
            marginTop = baseVerticalSpacing,
            marginBottom = baseVerticalSpacing * 2,
            marginLeft = horizontalPadding,
            marginRight = horizontalPadding
        )
        currentY += categoryBarHeight + baseVerticalSpacing * 2
        
        // 2. 选项卡区域
        val tabSectionHeight = 32.dp
        metrics[UIModule.TAB_SECTION] = UIModuleMetrics(
            relativeX = horizontalPadding.value / screenWidth.value,
            relativeY = currentY.value / screenHeight.value,
            relativeWidth = (screenWidth - horizontalPadding * 2).value / screenWidth.value,
            relativeHeight = tabSectionHeight.value / screenHeight.value,
            absoluteX = horizontalPadding,
            absoluteY = currentY,
            absoluteWidth = screenWidth - horizontalPadding * 2,
            absoluteHeight = tabSectionHeight,
            marginTop = 0.dp,
            marginBottom = baseVerticalSpacing,
            marginLeft = horizontalPadding,
            marginRight = horizontalPadding
        )
        currentY += tabSectionHeight + baseVerticalSpacing
        
        // 3. 主图表区域 (饼图/柱状图)
        metrics[UIModule.MAIN_CHART] = UIModuleMetrics(
            relativeX = horizontalPadding.value / screenWidth.value,
            relativeY = currentY.value / screenHeight.value,
            relativeWidth = (screenWidth - horizontalPadding * 2).value / screenWidth.value,
            relativeHeight = pieChartHeight.value / screenHeight.value,
            absoluteX = horizontalPadding,
            absoluteY = currentY,
            absoluteWidth = screenWidth - horizontalPadding * 2,
            absoluteHeight = pieChartHeight,
            marginTop = 0.dp,
            marginBottom = buttonToChartSpacing,
            marginLeft = horizontalPadding,
            marginRight = horizontalPadding
        )
        currentY += pieChartHeight + buttonToChartSpacing
        
        // 4. 日周月筛选按钮
        val periodButtonHeight = 32.dp
        metrics[UIModule.PERIOD_BUTTONS] = UIModuleMetrics(
            relativeX = 0.25f, // 居中显示，占屏幕中央50%宽度
            relativeY = currentY.value / screenHeight.value,
            relativeWidth = 0.5f,
            relativeHeight = periodButtonHeight.value / screenHeight.value,
            absoluteX = screenWidth * 0.25f,
            absoluteY = currentY,
            absoluteWidth = screenWidth * 0.5f,
            absoluteHeight = periodButtonHeight,
            marginTop = 0.dp,
            marginBottom = chartSpacing,
            marginLeft = 0.dp,
            marginRight = 0.dp
        )
        currentY += periodButtonHeight + chartSpacing
        
        // 5. 使用时间折线图
        metrics[UIModule.USAGE_LINE_CHART] = UIModuleMetrics(
            relativeX = horizontalPadding.value / screenWidth.value,
            relativeY = currentY.value / screenHeight.value,
            relativeWidth = (screenWidth - horizontalPadding * 2).value / screenWidth.value,
            relativeHeight = chartHeight.value / screenHeight.value,
            absoluteX = horizontalPadding,
            absoluteY = currentY,
            absoluteWidth = screenWidth - horizontalPadding * 2,
            absoluteHeight = chartHeight,
            marginTop = 0.dp,
            marginBottom = chartSpacing,
            marginLeft = horizontalPadding,
            marginRight = horizontalPadding
        )
        currentY += chartHeight + chartSpacing
        
        // 6. 完成率折线图
        metrics[UIModule.COMPLETION_LINE_CHART] = UIModuleMetrics(
            relativeX = horizontalPadding.value / screenWidth.value,
            relativeY = currentY.value / screenHeight.value,
            relativeWidth = (screenWidth - horizontalPadding * 2).value / screenWidth.value,
            relativeHeight = chartHeight.value / screenHeight.value,
            absoluteX = horizontalPadding,
            absoluteY = currentY,
            absoluteWidth = screenWidth - horizontalPadding * 2,
            absoluteHeight = chartHeight,
            marginTop = 0.dp,
            marginBottom = chartSpacing,
            marginLeft = horizontalPadding,
            marginRight = horizontalPadding
        )
        currentY += chartHeight + chartSpacing
        
        // 7. 奖励完成度折线图
        metrics[UIModule.REWARD_LINE_CHART] = UIModuleMetrics(
            relativeX = horizontalPadding.value / screenWidth.value,
            relativeY = currentY.value / screenHeight.value,
            relativeWidth = (screenWidth - horizontalPadding * 2).value / screenWidth.value,
            relativeHeight = chartHeight.value / screenHeight.value,
            absoluteX = horizontalPadding,
            absoluteY = currentY,
            absoluteWidth = screenWidth - horizontalPadding * 2,
            absoluteHeight = chartHeight,
            marginTop = 0.dp,
            marginBottom = chartSpacing,
            marginLeft = horizontalPadding,
            marginRight = horizontalPadding
        )
        currentY += chartHeight + chartSpacing
        
        // 8. 惩罚完成度折线图
        metrics[UIModule.PUNISHMENT_LINE_CHART] = UIModuleMetrics(
            relativeX = horizontalPadding.value / screenWidth.value,
            relativeY = currentY.value / screenHeight.value,
            relativeWidth = (screenWidth - horizontalPadding * 2).value / screenWidth.value,
            relativeHeight = chartHeight.value / screenHeight.value,
            absoluteX = horizontalPadding,
            absoluteY = currentY,
            absoluteWidth = screenWidth - horizontalPadding * 2,
            absoluteHeight = chartHeight,
            marginTop = 0.dp,
            marginBottom = baseVerticalSpacing,
            marginLeft = horizontalPadding,
            marginRight = horizontalPadding
        )
        
        return metrics
    }
    
    /**
     * 更新模块度量数据
     */
    fun updateModuleMetrics(module: UIModule, metrics: UIModuleMetrics) {
        moduleMetrics[module] = metrics
    }
    
    /**
     * 获取特定模块的度量数据
     */
    fun getModuleMetrics(module: UIModule): UIModuleMetrics? {
        return moduleMetrics[module]
    }
    
    /**
     * 获取所有模块的度量数据
     */
    fun getAllModuleMetrics(): Map<UIModule, UIModuleMetrics> {
        return moduleMetrics.toMap()
    }
    
    /**
     * 清除所有度量数据
     */
    fun clearMetrics() {
        moduleMetrics.clear()
    }
    
    /**
     * 根据屏幕尺寸获取缩放因子
     */
    @Composable
    fun getScaleFactor(): Float {
        val configuration = LocalConfiguration.current
        val screenHeight = configuration.screenHeightDp.dp
        
        return when {
            screenHeight < 700.dp -> 0.9f    // 小屏幕缩小
            screenHeight > 800.dp -> 1.1f    // 大屏幕放大
            else -> 1.0f                     // 中等屏幕标准
        }
    }
    
    /**
     * 获取当前屏幕信息
     */
    @Composable
    fun getCurrentScreenInfo(): ScreenInfo {
        val configuration = LocalConfiguration.current
        return ScreenInfo(
            widthDp = configuration.screenWidthDp,
            heightDp = configuration.screenHeightDp,
            density = configuration.densityDpi / 160f
        )
    }
}

/**
 * 屏幕信息数据类
 */
data class ScreenInfo(
    val widthDp: Int,
    val heightDp: Int,
    val density: Float
) 