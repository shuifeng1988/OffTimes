package com.offtime.app.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil

/**
 * 屏幕尺寸分类
 */
enum class ScreenSize {
    Small,      // < 600dp
    Medium,     // 600dp - 840dp
    Large       // > 840dp
}

/**
 * 屏幕密度分类
 */
enum class ScreenDensity {
    Low,        // < 1.5
    Medium,     // 1.5 - 2.0
    High,       // 2.0 - 3.0
    XHigh,      // 3.0 - 4.0
    XXHigh      // >= 4.0
}

/**
 * 柱状图配置类 - 消除硬编码问题
 */
data class BarChartConfig(
    // 核心尺寸配置
    val chartHeight: Dp = 190.dp,
    val canvasPaddingTop: Dp = 8.dp,
    val canvasPaddingBottom: Dp = 2.5.dp,
    val canvasPaddingEnd: Dp = 16.dp,
    
    // Y轴配置（用于动态计算最大值和刻度）
    val yAxisStepMinutes: Int = 10,    // Y轴每步长代表的分钟数
    val yAxisMinorStepCount: Int = 1,  // 每两个主刻度之间的次刻度数
    val yAxisMaxValueCalculationRatio: Float = 1.2f, // 最大值计算比例（数据最大值 * 1.2f）
    val yAxisMajorTickCount: Int = 4,  // 主要刻度数量（0, 20, 40, 60）
    
    // X轴配置
    val xAxisHourCount: Int = 24,      // X轴小时数
    val xAxisLabelStep: Int = 2,       // X轴标签显示间隔（每2小时显示一次）
    
    // 柱子配置
    val barWidthRatio: Float = 0.6f,   // 柱宽占单元格比例
    val barStackingEnabled: Boolean = true, // 是否启用堆叠柱状图
    
    // 线条配置
    val axisStrokeWidth: Dp = 2.dp,    // 坐标轴线条粗细
    val gridStrokeWidth: Dp = 0.5.dp,  // 网格线粗细
    val tickStrokeWidth: Dp = 1.dp,    // 刻度线粗细
    val tickLength: Dp = 5.dp,         // 刻度线长度
    
    // 字体配置
    val labelFontSize: TextUnit = 18.sp, // 轴标签字体大小
    val labelPaddingTop: Dp = 8.dp,     // X轴标签上边距
    
    // 响应式适配
    val densityMultiplier: Float = 1.0f, // 密度倍数调整
) {
    /**
     * 动态计算Y轴最大值（智能范围适配）
     */
    fun calculateYAxisMaxValue(dataMaxValue: Int): Int {
        if (dataMaxValue == 0) return 60 // 默认1小时
        
        // 根据数据大小智能选择合适的范围
        val candidateMaxValues = listOf(
            15, 30, 60, 120, 180, 240, 300, 360, 480, 600, 720, 900, 1200, 1440 // 15分钟到24小时
        )
        
        // 找到第一个大于等于 (dataMaxValue * 1.2) 的候选值
        val targetValue = (dataMaxValue * yAxisMaxValueCalculationRatio).toInt()
        return candidateMaxValues.find { it >= targetValue } ?: candidateMaxValues.last()
    }
    
    /**
     * 生成Y轴刻度值列表（智能刻度分布）
     */
    fun generateYAxisTickValues(maxValue: Int): List<Int> {
        return when {
            maxValue <= 15 -> listOf(0, 5, 10, 15)           // 0-15分钟：每5分钟
            maxValue <= 30 -> listOf(0, 10, 20, 30)          // 0-30分钟：每10分钟
            maxValue <= 60 -> listOf(0, 20, 40, 60)          // 0-1小时：每20分钟
            maxValue <= 120 -> listOf(0, 30, 60, 90, 120)    // 0-2小时：每30分钟
            maxValue <= 180 -> listOf(0, 45, 90, 135, 180)   // 0-3小时：每45分钟
            maxValue <= 240 -> listOf(0, 60, 120, 180, 240)  // 0-4小时：每1小时
            maxValue <= 360 -> listOf(0, 90, 180, 270, 360)  // 0-6小时：每1.5小时
            maxValue <= 480 -> listOf(0, 120, 240, 360, 480) // 0-8小时：每2小时
            maxValue <= 720 -> listOf(0, 180, 360, 540, 720) // 0-12小时：每3小时
            else -> {
                // 超过12小时：动态计算合适的间隔
                val step = maxValue / 4
                val roundedStep = when {
                    step <= 60 -> ((step + 30) / 60) * 60     // 向上取整到小时
                    step <= 180 -> ((step + 90) / 180) * 180  // 向上取整到3小时
                    step <= 360 -> ((step + 180) / 360) * 360 // 向上取整到6小时
                    else -> ((step + 360) / 720) * 720        // 向上取整到12小时
                }
                (0..4).map { it * roundedStep }
            }
        }
    }
    
    /**
     * 生成网格线Y值列表（智能间隔）
     */
    fun generateGridLineValues(maxValue: Int): List<Int> {
        val gridStep = when {
            maxValue <= 15 -> 5     // 0-15分钟：每5分钟一条网格线
            maxValue <= 30 -> 5     // 0-30分钟：每5分钟一条网格线  
            maxValue <= 60 -> 10    // 0-1小时：每10分钟一条网格线
            maxValue <= 120 -> 15   // 0-2小时：每15分钟一条网格线
            maxValue <= 180 -> 15   // 0-3小时：每15分钟一条网格线
            maxValue <= 240 -> 30   // 0-4小时：每30分钟一条网格线
            maxValue <= 360 -> 30   // 0-6小时：每30分钟一条网格线
            maxValue <= 480 -> 60   // 0-8小时：每1小时一条网格线
            maxValue <= 720 -> 60   // 0-12小时：每1小时一条网格线
            else -> maxValue / 8    // 超过12小时：8条均匀分布的网格线
        }
        return (gridStep..maxValue step gridStep).toList()
    }
    
    /**
     * 生成X轴标签位置列表
     */
    fun generateXAxisLabelPositions(): List<Int> {
        return (0 until xAxisHourCount step xAxisLabelStep).toList()
    }
    
    /**
     * 计算实际的图表尺寸（响应式调整）
     */
    fun getAdjustedChartHeight(): Dp = chartHeight * densityMultiplier
    fun getAdjustedPaddingTop(): Dp = canvasPaddingTop * densityMultiplier
    fun getAdjustedPaddingBottom(): Dp = canvasPaddingBottom * densityMultiplier
    fun getAdjustedPaddingEnd(): Dp = canvasPaddingEnd * densityMultiplier
    fun getAdjustedLabelPaddingTop(): Dp = labelPaddingTop * densityMultiplier
}

/**
 * 响应式尺寸类
 */
data class ResponsiveDimensions(
    // 基础间距
    val spacingXSmall: Dp,
    val spacingSmall: Dp,
    val spacingMedium: Dp,
    val spacingLarge: Dp,
    val spacingXLarge: Dp,
    
    // 内边距
    val paddingSmall: Dp,
    val paddingMedium: Dp,
    val paddingLarge: Dp,
    val paddingXLarge: Dp,
    
    // 组件尺寸
    val iconSizeSmall: Dp,
    val iconSizeMedium: Dp,
    val iconSizeLarge: Dp,
    val buttonHeight: Dp,
    val cardElevation: Dp,
    val cornerRadius: Dp,
    
    // 图表尺寸
    val chartHeight: Dp,
    val pieChartHeight: Dp,
    val barChartHeight: Dp,
    
    // 字体大小
    val fontSizeXSmall: TextUnit,
    val fontSizeSmall: TextUnit,
    val fontSizeMedium: TextUnit,
    val fontSizeLarge: TextUnit,
    val fontSizeXLarge: TextUnit,
    val fontSizeXXLarge: TextUnit,
    
    // 线条粗细
    val strokeWidthThin: Dp,
    val strokeWidthMedium: Dp,
    val strokeWidthThick: Dp,
    
    // === 图表专用参数 ===
    // 坐标轴字体大小
    val chartAxisLabelFontSize: TextUnit,      // 坐标轴标签字体大小
    val chartAxisTitleFontSize: TextUnit,      // 坐标轴标题字体大小（如"使用时间"、"达标率"）
    val chartMinLabelFontSize: TextUnit,       // "min"标签字体大小
    
    // 坐标轴距离
    val chartAxisLabelToAxisDistance: Dp,      // 轴标签到轴线的距离
    val chartAxisTitleWidth: Dp,               // Y轴标题区域宽度
    val chartXAxisLabelTopPadding: Dp,         // X轴标签上边距
    
    // 图表间距和偏移
    val chartVerticalOffset: Dp,               // 图表向上偏移距离（用于统一所有图表的垂直位置）
    val chartSpacingBetween: Dp,               // 不同图表模块之间的间距
    val chartContainerPadding: Dp,             // 图表容器内边距
    
    // 坐标轴线条
    val chartAxisStrokeWidth: Dp,              // 坐标轴线条粗细
    val chartGridStrokeWidth: Dp,              // 网格线粗细
    
    // 图表特定尺寸
    val hourlyBarChartHeight: Dp,              // 小时使用图表高度
    val lineChartHeight: Dp,                   // 折线图高度
    val yAxisLabelColumnWidth: Dp,              // Y轴标签列宽度
    
    // === 24H柱状图专用参数 ===
    val chartYAxisLabelOffset: Dp = 95.dp,           // Y轴标签整体下移（原90dp+5dp）
    val chartCanvasPaddingTop: Dp = 8.dp,           // Canvas顶部padding
    val chartCanvasPaddingBottom: Dp = 2.5.dp,      // Canvas底部padding
    val chartCanvasPaddingEnd: Dp = 16.dp,          // Canvas右侧padding
    val barWidthRatio: Float = 0.6f,                // 柱宽占单元格比例
    val xAxisLabelPaddingTop: Dp = 5.dp,           // X轴标签上边距
    val xAxisLabelPaddingEnd: Dp = 16.dp,           // X轴标签右边距
    val xAxisTickLength: Dp = 5.dp,                 // X轴刻度线长度
    val yAxisTickLength: Dp = 5.dp,                 // Y轴刻度线长度
    val chartGridLineStroke: Dp = 0.5.dp,           // 网格线粗细
    val chartAxisLineStroke: Dp = 2.dp,             // 坐标轴线条粗细
    val chartTickLineStroke: Dp = 1.dp,              // 刻度线条粗细
    
    // === 柱状图配置 ===
    val barChartConfig: BarChartConfig = BarChartConfig(
        densityMultiplier = if (chartAxisStrokeWidth.value > 1.5f) 1.0f else 0.8f
    )
)

/**
 * 屏幕宽高数据类
 */
data class ScreenDpSize(val widthDp: Int, val heightDp: Int)

/**
 * 百分比转dp工具
 */
fun percentWidth(percent: Float, screenWidthDp: Int): Dp = (screenWidthDp * percent).dp
fun percentHeight(percent: Float, screenHeightDp: Int): Dp = (screenHeightDp * percent).dp

/**
 * 全局布局百分比参数
 */
data class UILayoutMetrics(
    val mainChartWidthPercent: Float = 0.92f,
    val mainChartHeightPercent: Float = 0.25f,
    val mainChartTopPercent: Float = 0.12f,
    val buttonWidthPercent: Float = 0.4f,
    val buttonHeightPercent: Float = 0.08f,
    val cardWidthPercent: Float = 0.9f,
    val cardHeightPercent: Float = 0.18f,
    // ...可继续补充其它模块
)

/**
 * 获取当前屏幕尺寸分类
 */
@Composable
fun getScreenSize(): ScreenSize {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp
    
    return when {
        screenWidthDp < 600.dp -> ScreenSize.Small
        screenWidthDp < 840.dp -> ScreenSize.Medium
        else -> ScreenSize.Large
    }
}

/**
 * 获取当前屏幕密度分类
 */
@Composable
fun getScreenDensity(): ScreenDensity {
    val density = LocalDensity.current.density
    
    return when {
        density < 1.5f -> ScreenDensity.Low
        density < 2.0f -> ScreenDensity.Medium
        density < 3.0f -> ScreenDensity.High
        density < 4.0f -> ScreenDensity.XHigh
        else -> ScreenDensity.XXHigh
    }
}

/**
 * 获取响应式尺寸
 */
@Composable
fun getResponsiveDimensions(): ResponsiveDimensions {
    val screenSize = getScreenSize()
    val screenDensity = getScreenDensity()
    
    return when (screenSize) {
        ScreenSize.Small -> getSmallScreenDimensions(screenDensity)
        ScreenSize.Medium -> getMediumScreenDimensions(screenDensity)
        ScreenSize.Large -> getLargeScreenDimensions(screenDensity)
    }
}

/**
 * 小屏幕尺寸配置
 */
private fun getSmallScreenDimensions(density: ScreenDensity): ResponsiveDimensions {
    val densityMultiplier = when (density) {
        ScreenDensity.Low -> 1.2f
        ScreenDensity.Medium -> 1.1f
        ScreenDensity.High -> 1.0f
        ScreenDensity.XHigh -> 0.9f
        ScreenDensity.XXHigh -> 0.8f
    }
    
    // 统一缩放因子：确保小屏幕在所有密度下都比标准屏小
    // 问题修复：之前使用when条件导致某些密度下小屏幕反而比标准屏大
    // 现在统一使用基础值 * densityMultiplier，确保比例一致
    // 小屏幕目标：iconSizeSmall=18dp*倍数, fontSizeSmall=14sp, 确保始终 < 标准屏
    @Suppress("UNUSED_VARIABLE")
    val bottomBarScaleFactor = 0.9f
    
    return ResponsiveDimensions(
        // 基础间距 (统一按0.9倍缩放)
        spacingXSmall = (2.dp * densityMultiplier),
        spacingSmall = (5.dp * densityMultiplier),  // 标准屏6dp * 0.9 = 5.4dp ≈ 5dp
        spacingMedium = (8.dp * densityMultiplier),
        spacingLarge = (12.dp * densityMultiplier),
        spacingXLarge = (16.dp * densityMultiplier),
        
        // 内边距 (统一按0.9倍缩放)
        paddingSmall = (11.dp * densityMultiplier),  // 标准屏12dp * 0.9 = 10.8dp ≈ 11dp
        paddingMedium = (12.dp * densityMultiplier),
        paddingLarge = (16.dp * densityMultiplier),
        paddingXLarge = (20.dp * densityMultiplier),
        
        // 组件尺寸 (大幅减小确保Pixel 4底栏正常显示)
        iconSizeSmall = (16.dp * densityMultiplier),  // 进一步减小：18dp -> 16dp
        iconSizeMedium = (20.dp * densityMultiplier), // 进一步减小：22dp -> 20dp
        iconSizeLarge = (32.dp * densityMultiplier),
        buttonHeight = (43.dp * densityMultiplier),  // 标准屏48dp * 0.9 = 43.2dp ≈ 43dp
        cardElevation = (2.dp * densityMultiplier),
        cornerRadius = (8.dp * densityMultiplier),
        
        // 图表尺寸
        chartHeight = (140.dp * densityMultiplier),
        pieChartHeight = (180.dp * densityMultiplier),
        barChartHeight = (120.dp * densityMultiplier),
        
        // 字体大小 - 直接设置为12sp以解决底栏显示问题
        fontSizeXSmall = when (density) {
            ScreenDensity.Low -> 12.sp    // 直接设置为12sp
            ScreenDensity.Medium -> 12.sp // 直接设置为12sp
            ScreenDensity.High -> 12.sp   // 直接设置为12sp
            ScreenDensity.XHigh -> 12.sp  // 直接设置为12sp
            ScreenDensity.XXHigh -> 12.sp // 直接设置为12sp
        },
        fontSizeSmall = when (density) {
            ScreenDensity.Low -> 12.sp    // 直接设置为12sp
            ScreenDensity.Medium -> 12.sp // 直接设置为12sp
            ScreenDensity.High -> 12.sp   // 直接设置为12sp
            ScreenDensity.XHigh -> 12.sp  // 直接设置为12sp
            ScreenDensity.XXHigh -> 12.sp // 直接设置为12sp
        },
        fontSizeMedium = when (density) {
            ScreenDensity.Low -> 16.sp
            ScreenDensity.Medium -> 16.sp
            ScreenDensity.High -> 16.sp
            ScreenDensity.XHigh -> 16.sp
            ScreenDensity.XXHigh -> 16.sp
        },
        fontSizeLarge = when (density) {
            ScreenDensity.Low -> 20.sp
            ScreenDensity.Medium -> 19.sp
            ScreenDensity.High -> 18.sp
            ScreenDensity.XHigh -> 17.sp
            ScreenDensity.XXHigh -> 16.sp
        },
        fontSizeXLarge = when (density) {
            ScreenDensity.Low -> 24.sp
            ScreenDensity.Medium -> 23.sp
            ScreenDensity.High -> 22.sp
            ScreenDensity.XHigh -> 21.sp
            ScreenDensity.XXHigh -> 20.sp
        },
        fontSizeXXLarge = when (density) {
            ScreenDensity.Low -> 28.sp
            ScreenDensity.Medium -> 27.sp
            ScreenDensity.High -> 26.sp
            ScreenDensity.XHigh -> 25.sp
            ScreenDensity.XXHigh -> 24.sp
        },
        
        // 线条粗细
        strokeWidthThin = (1.dp * densityMultiplier),
        strokeWidthMedium = (2.dp * densityMultiplier),
        strokeWidthThick = (3.dp * densityMultiplier),
        
        // === 图表专用参数 ===
        // 坐标轴字体大小
        chartAxisLabelFontSize = 18.sp,  // 恢复原始大小
        chartAxisTitleFontSize = 16.sp,  // 恢复原始大小
        chartMinLabelFontSize = 18.sp,   // 恢复原始大小
        
        // 坐标轴距离
        chartAxisLabelToAxisDistance = (6.dp * densityMultiplier),
        chartAxisTitleWidth = (28.dp * densityMultiplier),
        chartXAxisLabelTopPadding = (12.dp * densityMultiplier),  // 增加X轴标签顶部间距：8dp -> 12dp
        
        // 图表间距和偏移
        chartVerticalOffset = (7.dp * densityMultiplier),
        chartSpacingBetween = (18.dp * densityMultiplier),  // 增加图表间距：16dp -> 18dp
        chartContainerPadding = (16.dp * densityMultiplier),
        
        // 坐标轴线条
        chartAxisStrokeWidth = (2.dp * densityMultiplier),
        chartGridStrokeWidth = (0.5.dp * densityMultiplier),
        
        // 图表特定尺寸
        hourlyBarChartHeight = (240.dp * densityMultiplier),
        lineChartHeight = (160.dp * densityMultiplier),  // 增加折线图高度：140dp -> 160dp
        yAxisLabelColumnWidth = (45.dp * densityMultiplier),
        
        // === 24H柱状图专用参数 ===
        chartYAxisLabelOffset = 95.dp,
        chartCanvasPaddingTop = 8.dp,
        chartCanvasPaddingBottom = 4.dp,  // 增加底部间距：2.5dp -> 4dp
        chartCanvasPaddingEnd = 16.dp,
        barWidthRatio = 0.6f,
        xAxisLabelPaddingTop = 8.dp,  // 增加X轴标签上边距：5dp -> 8dp
        xAxisLabelPaddingEnd = 16.dp,
        xAxisTickLength = 5.dp,
        yAxisTickLength = 5.dp,
        chartGridLineStroke = 0.5.dp,
        chartAxisLineStroke = 2.dp,
        chartTickLineStroke = 1.dp
    )
}

/**
 * 中等屏幕尺寸配置
 */
private fun getMediumScreenDimensions(density: ScreenDensity): ResponsiveDimensions {
    val densityMultiplier = when (density) {
        ScreenDensity.Low -> 1.3f
        ScreenDensity.Medium -> 1.2f
        ScreenDensity.High -> 1.1f
        ScreenDensity.XHigh -> 1.0f
        ScreenDensity.XXHigh -> 0.9f
    }
    
    return ResponsiveDimensions(
        // 基础间距
        spacingXSmall = (3.dp * densityMultiplier),
        spacingSmall = (6.dp * densityMultiplier),
        spacingMedium = (12.dp * densityMultiplier),
        spacingLarge = (18.dp * densityMultiplier),
        spacingXLarge = (24.dp * densityMultiplier),
        
        // 内边距
        paddingSmall = (12.dp * densityMultiplier),
        paddingMedium = (16.dp * densityMultiplier),
        paddingLarge = (20.dp * densityMultiplier),
        paddingXLarge = (24.dp * densityMultiplier),
        
        // 组件尺寸 (大幅减小确保Pixel 4底栏正常显示)
        iconSizeSmall = (16.dp * densityMultiplier),  // 进一步减小：18dp -> 16dp
        iconSizeMedium = (20.dp * densityMultiplier), // 进一步减小：25dp -> 20dp  
        iconSizeLarge = (36.dp * densityMultiplier),
        buttonHeight = (43.dp * densityMultiplier),   // 减小：48dp -> 43dp
        cardElevation = (4.dp * densityMultiplier),
        cornerRadius = (12.dp * densityMultiplier),
        
        // 图表尺寸
        chartHeight = (160.dp * densityMultiplier),
        pieChartHeight = (200.dp * densityMultiplier),
        barChartHeight = (140.dp * densityMultiplier),
        
        // 字体大小 - 直接设置为12sp确保Pixel 4底栏文字正常显示
        fontSizeXSmall = when (density) {
            ScreenDensity.Low -> 12.sp    // 直接设置为12sp适配Pixel 4
            ScreenDensity.Medium -> 12.sp // 直接设置为12sp适配Pixel 4
            ScreenDensity.High -> 12.sp   // 直接设置为12sp适配Pixel 4
            ScreenDensity.XHigh -> 12.sp  // 直接设置为12sp适配Pixel 4
            ScreenDensity.XXHigh -> 12.sp // 直接设置为12sp适配Pixel 4
        },
        fontSizeSmall = when (density) {
            ScreenDensity.Low -> 12.sp    // 直接设置为12sp适配Pixel 4
            ScreenDensity.Medium -> 12.sp // 直接设置为12sp适配Pixel 4
            ScreenDensity.High -> 12.sp   // 直接设置为12sp适配Pixel 4
            ScreenDensity.XHigh -> 12.sp  // 直接设置为12sp适配Pixel 4
            ScreenDensity.XXHigh -> 12.sp // 直接设置为12sp适配Pixel 4
        },
        fontSizeMedium = when (density) {
            ScreenDensity.Low -> 19.sp    // 从18sp增大到19sp
            ScreenDensity.Medium -> 18.sp // 从17sp增大到18sp
            ScreenDensity.High -> 17.sp   // 从16sp增大到17sp
            ScreenDensity.XHigh -> 16.sp  // 从15sp增大到16sp
            ScreenDensity.XXHigh -> 16.sp // 从14sp增大到15sp
        },
        fontSizeLarge = when (density) {
            ScreenDensity.Low -> 22.sp
            ScreenDensity.Medium -> 21.sp
            ScreenDensity.High -> 20.sp
            ScreenDensity.XHigh -> 19.sp
            ScreenDensity.XXHigh -> 18.sp
        },
        fontSizeXLarge = when (density) {
            ScreenDensity.Low -> 26.sp
            ScreenDensity.Medium -> 25.sp
            ScreenDensity.High -> 24.sp
            ScreenDensity.XHigh -> 23.sp
            ScreenDensity.XXHigh -> 22.sp
        },
        fontSizeXXLarge = when (density) {
            ScreenDensity.Low -> 30.sp
            ScreenDensity.Medium -> 29.sp
            ScreenDensity.High -> 28.sp
            ScreenDensity.XHigh -> 27.sp
            ScreenDensity.XXHigh -> 26.sp
        },
        
        // 线条粗细
        strokeWidthThin = (1.dp * densityMultiplier),
        strokeWidthMedium = (2.dp * densityMultiplier),
        strokeWidthThick = (4.dp * densityMultiplier),
        
        // === 图表专用参数 ===
        // 坐标轴字体大小
        chartAxisLabelFontSize = 16.sp,  // 调整标准屏字体：18sp -> 16sp
        chartAxisTitleFontSize = 16.sp,  // 恢复原始大小
        chartMinLabelFontSize = 18.sp,   // 恢复原始大小
        
        // 坐标轴距离
        chartAxisLabelToAxisDistance = (6.dp * densityMultiplier),
        chartAxisTitleWidth = (32.dp * densityMultiplier),
        chartXAxisLabelTopPadding = (10.dp * densityMultiplier),
        
        // 图表间距和偏移
        chartVerticalOffset = (7.dp * densityMultiplier),
        chartSpacingBetween = (20.dp * densityMultiplier),
        chartContainerPadding = (20.dp * densityMultiplier),
        
        // 坐标轴线条
        chartAxisStrokeWidth = (2.dp * densityMultiplier),
        chartGridStrokeWidth = (0.5.dp * densityMultiplier),
        
        // 图表特定尺寸
        hourlyBarChartHeight = (240.dp * densityMultiplier),
        lineChartHeight = (160.dp * densityMultiplier),
        yAxisLabelColumnWidth = (45.dp * densityMultiplier),
        
        // === 24H柱状图专用参数 ===
        chartYAxisLabelOffset = 95.dp,
        chartCanvasPaddingTop = 8.dp,
        chartCanvasPaddingBottom = 2.5.dp,
        chartCanvasPaddingEnd = 16.dp,
        barWidthRatio = 0.6f,
        xAxisLabelPaddingTop = 5.dp,
        xAxisLabelPaddingEnd = 16.dp,
        xAxisTickLength = 5.dp,
        yAxisTickLength = 5.dp,
        chartGridLineStroke = 0.5.dp,
        chartAxisLineStroke = 2.dp,
        chartTickLineStroke = 1.dp
    )
}

/**
 * 大屏幕尺寸配置
 */
private fun getLargeScreenDimensions(density: ScreenDensity): ResponsiveDimensions {
    val densityMultiplier = when (density) {
        ScreenDensity.Low -> 1.4f
        ScreenDensity.Medium -> 1.3f
        ScreenDensity.High -> 1.2f
        ScreenDensity.XHigh -> 1.1f
        ScreenDensity.XXHigh -> 1.0f
    }
    
    return ResponsiveDimensions(
        // 基础间距
        spacingXSmall = (4.dp * densityMultiplier),
        spacingSmall = (8.dp * densityMultiplier),
        spacingMedium = (16.dp * densityMultiplier),
        spacingLarge = (24.dp * densityMultiplier),
        spacingXLarge = (32.dp * densityMultiplier),
        
        // 内边距
        paddingSmall = (16.dp * densityMultiplier),
        paddingMedium = (20.dp * densityMultiplier),
        paddingLarge = (24.dp * densityMultiplier),
        paddingXLarge = (32.dp * densityMultiplier),
        
        // 组件尺寸
        iconSizeSmall = (24.dp * densityMultiplier),
        iconSizeMedium = (32.dp * densityMultiplier),
        iconSizeLarge = (40.dp * densityMultiplier),
        buttonHeight = (56.dp * densityMultiplier),
        cardElevation = (6.dp * densityMultiplier),
        cornerRadius = (16.dp * densityMultiplier),
        
        // 图表尺寸
        chartHeight = (200.dp * densityMultiplier),
        pieChartHeight = (240.dp * densityMultiplier),
        barChartHeight = (180.dp * densityMultiplier),
        
        // 字体大小
        fontSizeXSmall = when (density) {
            ScreenDensity.Low -> 16.sp
            ScreenDensity.Medium -> 16.sp
            ScreenDensity.High -> 16.sp
            ScreenDensity.XHigh -> 16.sp
            ScreenDensity.XXHigh -> 16.sp
        },
        fontSizeSmall = when (density) {
            ScreenDensity.Low -> 18.sp
            ScreenDensity.Medium -> 17.sp
            ScreenDensity.High -> 16.sp
            ScreenDensity.XHigh -> 16.sp
            ScreenDensity.XXHigh -> 16.sp
        },
        fontSizeMedium = when (density) {
            ScreenDensity.Low -> 20.sp
            ScreenDensity.Medium -> 19.sp
            ScreenDensity.High -> 18.sp
            ScreenDensity.XHigh -> 17.sp
            ScreenDensity.XXHigh -> 16.sp
        },
        fontSizeLarge = when (density) {
            ScreenDensity.Low -> 24.sp
            ScreenDensity.Medium -> 23.sp
            ScreenDensity.High -> 22.sp
            ScreenDensity.XHigh -> 21.sp
            ScreenDensity.XXHigh -> 20.sp
        },
        fontSizeXLarge = when (density) {
            ScreenDensity.Low -> 28.sp
            ScreenDensity.Medium -> 27.sp
            ScreenDensity.High -> 26.sp
            ScreenDensity.XHigh -> 25.sp
            ScreenDensity.XXHigh -> 24.sp
        },
        fontSizeXXLarge = when (density) {
            ScreenDensity.Low -> 32.sp
            ScreenDensity.Medium -> 31.sp
            ScreenDensity.High -> 30.sp
            ScreenDensity.XHigh -> 29.sp
            ScreenDensity.XXHigh -> 28.sp
        },
        
        // 线条粗细
        strokeWidthThin = (1.dp * densityMultiplier),
        strokeWidthMedium = (3.dp * densityMultiplier),
        strokeWidthThick = (5.dp * densityMultiplier),
        
        // === 图表专用参数 ===
        // 坐标轴字体大小
        chartAxisLabelFontSize = 18.sp,  // 恢复原始大小
        chartAxisTitleFontSize = 16.sp,  // 恢复原始大小
        chartMinLabelFontSize = 18.sp,   // 恢复原始大小
        
        // 坐标轴距离
        chartAxisLabelToAxisDistance = (6.dp * densityMultiplier),
        chartAxisTitleWidth = (36.dp * densityMultiplier),
        chartXAxisLabelTopPadding = (16.dp * densityMultiplier),  // 增加X轴标签顶部间距：12dp -> 16dp
        
        // 图表间距和偏移
        chartVerticalOffset = (7.dp * densityMultiplier),
        chartSpacingBetween = (24.dp * densityMultiplier),
        chartContainerPadding = (24.dp * densityMultiplier),
        
        // 坐标轴线条
        chartAxisStrokeWidth = (2.dp * densityMultiplier),
        chartGridStrokeWidth = (0.5.dp * densityMultiplier),
        
        // 图表特定尺寸
        hourlyBarChartHeight = (240.dp * densityMultiplier),
        lineChartHeight = (220.dp * densityMultiplier),  // 增加折线图高度：200dp -> 220dp
        yAxisLabelColumnWidth = (45.dp * densityMultiplier),
        
        // === 24H柱状图专用参数 ===
        chartYAxisLabelOffset = 95.dp,
        chartCanvasPaddingTop = 8.dp,
        chartCanvasPaddingBottom = 4.dp,  // 增加底部间距：2.5dp -> 4dp
        chartCanvasPaddingEnd = 16.dp,
        barWidthRatio = 0.6f,
        xAxisLabelPaddingTop = 8.dp,  // 增加X轴标签上边距：5dp -> 8dp
        xAxisLabelPaddingEnd = 16.dp,
        xAxisTickLength = 5.dp,
        yAxisTickLength = 5.dp,
        chartGridLineStroke = 0.5.dp,
        chartAxisLineStroke = 2.dp,
        chartTickLineStroke = 1.dp
    )
}

/**
 * 便利的扩展属性，用于快速访问响应式尺寸
 */
val LocalResponsiveDimensions @Composable get() = getResponsiveDimensions()

/**
 * 获取当前屏幕宽高dp
 */
@Composable
fun getScreenDpSize(): ScreenDpSize {
    val config = LocalConfiguration.current
    return ScreenDpSize(config.screenWidthDp, config.screenHeightDp)
}

/**
 * 全局CompositionLocal
 */
val LocalUILayoutMetrics = staticCompositionLocalOf { UILayoutMetrics() } 