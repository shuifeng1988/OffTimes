package com.offtime.app.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min
import kotlin.math.max

/**
 * 缩放因子工具类
 * 
 * 用于实现屏幕适配的统一缩放系统，确保在不同屏幕尺寸和分辨率下
 * 保持一致的布局比例和视觉效果。
 * 
 * 设计原则：
 * 1. 以6.2英寸、1080x2340分辨率的设备为基准（约400dp宽度、842dp高度）
 * 2. 不同尺寸的屏幕保持相同的布局比例
 * 3. 同一物理尺寸、不同分辨率的屏幕显示相同的大小
 * 4. 字体和UI元素按比例缩放
 */
object ScalingFactorUtils {
    
    // 基准屏幕参数（6.2英寸，1080x2340分辨率）
    private const val REFERENCE_WIDTH_DP = 400f      // 基准宽度
    private const val REFERENCE_HEIGHT_DP = 842f     // 基准高度
    private const val REFERENCE_DENSITY = 2.625f     // 基准密度（420dpi/160）
    private const val REFERENCE_DIAGONAL_DP = 935f   // 基准对角线长度
    
    /**
     * 屏幕信息数据类
     */
    data class ScreenMetrics(
        val widthDp: Float,
        val heightDp: Float,
        val density: Float,
        val widthPx: Int,
        val heightPx: Int,
        val diagonalDp: Float,
        val aspectRatio: Float
    )
    
    /**
     * 缩放因子数据类
     */
    data class ScalingFactors(
        val globalScale: Float,         // 全局缩放因子
        val fontScale: Float,          // 字体缩放因子
        val spacingScale: Float,       // 间距缩放因子
        val iconScale: Float,          // 图标缩放因子
        val chartScale: Float,         // 图表缩放因子
        val densityAdjustment: Float   // 密度调整因子
    )
    
    /**
     * 获取当前屏幕度量信息
     */
    @Composable
    fun getScreenMetrics(): ScreenMetrics {
        val configuration = LocalConfiguration.current
        val density = LocalDensity.current
        
        val widthDp = configuration.screenWidthDp.toFloat()
        val heightDp = configuration.screenHeightDp.toFloat()
        val densityValue = density.density
        val widthPx = (widthDp * densityValue).toInt()
        val heightPx = (heightDp * densityValue).toInt()
        val diagonalDp = kotlin.math.sqrt(widthDp * widthDp + heightDp * heightDp)
        val aspectRatio = heightDp / widthDp
        
        return ScreenMetrics(
            widthDp = widthDp,
            heightDp = heightDp,
            density = densityValue,
            widthPx = widthPx,
            heightPx = heightPx,
            diagonalDp = diagonalDp,
            aspectRatio = aspectRatio
        )
    }
    
    /**
     * 计算缩放因子 - 保持视觉一致性的自适应缩放
     * 
     * 设计理念：无论屏幕大小，UI元素在用户眼中应该看起来一样大
     * - 6.2寸屏幕：缩放因子 = 1.0 （基准）
     * - 6.8寸屏幕：缩放因子 ≈ 1.12 （元素放大12%，视觉上和6.2寸一样大）
     */
    @Composable
    fun calculateScalingFactors(): ScalingFactors {
        val metrics = getScreenMetrics()
        
        // 基于屏幕对角线计算缩放因子，以6.2寸屏幕为基准
        val baseDiagonalDp = REFERENCE_DIAGONAL_DP  // 935f (约6.2寸)
        val currentDiagonalDp = metrics.diagonalDp
        
        // 计算基础缩放因子：屏幕越大，元素按比例放大，保持视觉一致性
        val linearScale = currentDiagonalDp / baseDiagonalDp
        
        // 增强型缩放：放大差异效果，让大屏幕更明显
        // 当屏幕比基准大时，额外放大；当屏幕比基准小时，额外缩小
        val enhancementFactor = 1.3f  // 增强系数：1.3倍差异放大，适度缩放
        val baseScale = if (linearScale > 1.0f) {
            // 大屏幕：1 + (linear_scale - 1) * enhancement_factor
            1.0f + (linearScale - 1.0f) * enhancementFactor
        } else {
            // 小屏幕：1 - (1 - linear_scale) * enhancement_factor  
            1.0f - (1.0f - linearScale) * enhancementFactor
        }
        
        // 应用合理的缩放范围限制，避免过度缩放
        val clampedScale = baseScale.coerceIn(0.6f, 1.6f)
        
        // 根据屏幕密度进行微调
        val densityAdjustment = when {
            metrics.density < 2.0f -> 1.05f      // 低密度屏幕稍微放大
            metrics.density > 4.0f -> 0.95f      // 超高密度屏幕稍微缩小
            else -> 1.0f                         // 标准密度保持不变
        }
        
        val finalScale = clampedScale * densityAdjustment
        
        // 统一缩放策略：所有元素使用相同缩放因子，确保成比例缩放
        return ScalingFactors(
            globalScale = finalScale,                    // 全局缩放
            fontScale = finalScale,                      // 字体缩放与全局一致，确保比例协调
            spacingScale = finalScale,                   // 间距缩放与全局一致
            iconScale = finalScale,                      // 图标缩放与全局一致
            chartScale = finalScale,                     // 图表缩放与全局一致
            densityAdjustment = densityAdjustment        // 密度调整因子
        )
    }
    
    /**
     * 获取缩放后的Dp值
     */
    @Composable
    fun scaledDp(baseDp: Dp, scaleType: ScaleType = ScaleType.GLOBAL): Dp {
        val factors = calculateScalingFactors()
        val scale = when (scaleType) {
            ScaleType.GLOBAL -> factors.globalScale
            ScaleType.SPACING -> factors.spacingScale
            ScaleType.ICON -> factors.iconScale
            ScaleType.CHART -> factors.chartScale
        }
        return baseDp * scale
    }
    
    /**
     * 获取缩放后的TextUnit值
     */
    @Composable
    fun scaledSp(baseSp: TextUnit): TextUnit {
        val factors = calculateScalingFactors()
        return (baseSp.value * factors.fontScale).sp
    }
    
    /**
     * 获取缩放后的Float值
     */
    @Composable
    fun scaledFloat(baseFloat: Float, scaleType: ScaleType = ScaleType.GLOBAL): Float {
        val factors = calculateScalingFactors()
        val scale = when (scaleType) {
            ScaleType.GLOBAL -> factors.globalScale
            ScaleType.SPACING -> factors.spacingScale
            ScaleType.ICON -> factors.iconScale
            ScaleType.CHART -> factors.chartScale
        }
        return baseFloat * scale
    }
    
    /**
     * 根据屏幕宽度百分比计算Dp值
     */
    @Composable
    fun widthPercent(percent: Float): Dp {
        val metrics = getScreenMetrics()
        return (metrics.widthDp * percent).dp
    }
    
    /**
     * 根据屏幕高度百分比计算Dp值
     */
    @Composable
    fun heightPercent(percent: Float): Dp {
        val metrics = getScreenMetrics()
        return (metrics.heightDp * percent).dp
    }
    
    /**
     * 获取适配后的间距值
     */
    @Composable
    fun getAdaptiveSpacing(
        xSmall: Dp = 2.dp,
        small: Dp = 4.dp,
        medium: Dp = 8.dp,
        large: Dp = 16.dp,
        xLarge: Dp = 24.dp
    ): AdaptiveSpacing {
        return AdaptiveSpacing(
            xSmall = scaledDp(xSmall, ScaleType.SPACING),
            small = scaledDp(small, ScaleType.SPACING),
            medium = scaledDp(medium, ScaleType.SPACING),
            large = scaledDp(large, ScaleType.SPACING),
            xLarge = scaledDp(xLarge, ScaleType.SPACING)
        )
    }
    
    /**
     * 获取适配后的字体大小
     */
    @Composable
    fun getAdaptiveFontSizes(
        xSmall: TextUnit = 10.sp,
        small: TextUnit = 12.sp,
        medium: TextUnit = 14.sp,
        large: TextUnit = 16.sp,
        xLarge: TextUnit = 20.sp,
        xxLarge: TextUnit = 24.sp
    ): AdaptiveFontSizes {
        return AdaptiveFontSizes(
            xSmall = scaledSp(xSmall),
            small = scaledSp(small),
            medium = scaledSp(medium),
            large = scaledSp(large),
            xLarge = scaledSp(xLarge),
            xxLarge = scaledSp(xxLarge)
        )
    }
    
    /**
     * 获取适配后的图表尺寸
     */
    @Composable
    fun getAdaptiveChartSizes(
        lineChartHeight: Dp = 250.dp,    // 基础高度：250dp
        pieChartHeight: Dp = 280.dp,     // 基础高度：280dp  
        barChartHeight: Dp = 260.dp      // 基础高度：260dp
    ): AdaptiveChartSizes {
        // 移除额外的heightFactor逻辑，确保纯粹的比例缩放
        return AdaptiveChartSizes(
            lineChartHeight = scaledDp(lineChartHeight, ScaleType.CHART),
            pieChartHeight = scaledDp(pieChartHeight, ScaleType.CHART),
            barChartHeight = scaledDp(barChartHeight, ScaleType.CHART)
        )
    }
    
    /**
     * 判断是否为小屏幕
     */
    @Composable
    fun isSmallScreen(): Boolean {
        val metrics = getScreenMetrics()
        return metrics.widthDp < 360f || metrics.heightDp < 640f
    }
    
    /**
     * 判断是否为大屏幕
     */
    @Composable
    fun isLargeScreen(): Boolean {
        val metrics = getScreenMetrics()
        return metrics.widthDp > 480f || metrics.heightDp > 920f
    }
    
    /**
     * 判断是否为高密度屏幕
     */
    @Composable
    fun isHighDensityScreen(): Boolean {
        val metrics = getScreenMetrics()
        return metrics.density >= 3.0f
    }
    
    /**
     * 获取屏幕类型描述（用于调试）
     */
    @Composable
    fun getScreenTypeDescription(): String {
        val metrics = getScreenMetrics()
        val factors = calculateScalingFactors()
        
        val sizeCategory = when {
            isSmallScreen() -> "小屏"
            isLargeScreen() -> "大屏"
            else -> "中屏"
        }
        
        val densityCategory = when {
            metrics.density >= 4.0f -> "超高密度"
            metrics.density >= 3.0f -> "高密度"
            metrics.density >= 2.0f -> "中密度"
            metrics.density >= 1.5f -> "中低密度"
            else -> "低密度"
        }
        
        return "${sizeCategory}${densityCategory}(${metrics.widthDp.toInt()}×${metrics.heightDp.toInt()}dp, ${String.format("%.1f", metrics.diagonalDp)}dp对角线, 缩放${String.format("%.3f", factors.globalScale)})"
    }
    
    /**
     * 获取详细的屏幕适配信息（用于调试）
     */
    @Composable
    fun getDetailedScalingInfo(): String {
        val metrics = getScreenMetrics()
        val factors = calculateScalingFactors()
        
        return buildString {
            appendLine("=== 屏幕适配信息 ===")
            appendLine("屏幕尺寸: ${metrics.widthDp.toInt()} × ${metrics.heightDp.toInt()} dp")
            appendLine("物理尺寸: ${metrics.widthPx} × ${metrics.heightPx} px")
            appendLine("屏幕密度: ${String.format("%.2f", metrics.density)}")
            appendLine("对角线长度: ${String.format("%.1f", metrics.diagonalDp)} dp")
            appendLine("宽高比: ${String.format("%.2f", metrics.aspectRatio)}")
            appendLine("")
            appendLine("=== 缩放因子 ===")
            appendLine("全局缩放: ${String.format("%.3f", factors.globalScale)}")
            appendLine("字体缩放: ${String.format("%.3f", factors.fontScale)}")
            appendLine("间距缩放: ${String.format("%.3f", factors.spacingScale)}")
            appendLine("图标缩放: ${String.format("%.3f", factors.iconScale)}")
            appendLine("图表缩放: ${String.format("%.3f", factors.chartScale)}")
            appendLine("密度调整: ${String.format("%.3f", factors.densityAdjustment)}")
            appendLine("")
            appendLine("=== 基准对比 ===")
            appendLine("基准对角线: ${REFERENCE_DIAGONAL_DP} dp (6.2寸)")
            appendLine("当前对角线: ${String.format("%.1f", metrics.diagonalDp)} dp")
            appendLine("尺寸比例: ${String.format("%.3f", metrics.diagonalDp / REFERENCE_DIAGONAL_DP)}")
        }
    }
    
    /**
     * 简化的缩放因子 - 用于解决黑屏和点击检测问题
     * 
     * 此方法避免复杂计算，提供稳定的缩放效果
     * 特别适用于饼图和柱状图的渲染和点击检测
     */
    @Composable
    fun getSimpleScalingFactor(): Float {
        val configuration = LocalConfiguration.current
        @Suppress("UNUSED_VARIABLE")
        val density = LocalDensity.current
        
        val widthDp = configuration.screenWidthDp.toFloat()
        val heightDp = configuration.screenHeightDp.toFloat()
        
        // 基于屏幕宽度的简单缩放
        // 400dp 为基准宽度 (6.2英寸标准屏幕)
        val baseWidthDp = 400f
        val widthScale = widthDp / baseWidthDp
        
        // 限制缩放范围，避免极端情况
        val clampedScale = widthScale.coerceIn(0.8f, 1.3f)
        
        android.util.Log.d("SimpleScaling", "设备型号: ${android.os.Build.MODEL}")
        android.util.Log.d("SimpleScaling", "屏幕尺寸: ${widthDp}x${heightDp}dp")
        android.util.Log.d("SimpleScaling", "简化缩放因子: $clampedScale")
        
        return clampedScale
    }
    
    /**
     * 简化版的缩放Dp值 - 避免复杂计算
     */
    @Composable
    fun simpleScaledDp(baseDp: Dp): Dp {
        val scale = getSimpleScalingFactor()
        return baseDp * scale
    }
    
    /**
     * 简化版的缩放TextUnit值 - 避免复杂计算
     */
    @Composable
    fun simpleScaledSp(baseSp: TextUnit): TextUnit {
        val scale = getSimpleScalingFactor()
        return (baseSp.value * scale).sp
    }
    
    /**
     * 检测是否为问题设备（Pixel 4, Pixel 9 Pro XL）
     */
    @Composable
    fun isProblematicDevice(): Boolean {
        val model = android.os.Build.MODEL
        val manufacturer = android.os.Build.MANUFACTURER
        val device = android.os.Build.DEVICE
        val product = android.os.Build.PRODUCT
        
        // 检测Pixel 4和Pixel 9 Pro XL的多种可能识别方式
        val isPixel4 = model.contains("Pixel 4", ignoreCase = true) ||
                       device.contains("pixel_4", ignoreCase = true) ||
                       product.contains("pixel_4", ignoreCase = true)
        
        val isPixel9Pro = model.contains("Pixel 9 Pro", ignoreCase = true) ||
                         model.contains("Pixel 9 XL", ignoreCase = true) ||
                         device.contains("pixel_9", ignoreCase = true) ||
                         product.contains("pixel_9", ignoreCase = true) ||
                         model.contains("9 Pro XL", ignoreCase = true)
        
        // 模拟器环境检测 - 扩展检测范围
        val isEmulator = model.contains("sdk_gphone", ignoreCase = true) ||
                        model.contains("emulator", ignoreCase = true) ||
                        device.contains("emu", ignoreCase = true) ||
                        product.contains("sdk", ignoreCase = true)
        
        // 对于模拟器，我们也当作问题设备处理（因为可能模拟问题设备的行为）
        val isEmulatorPixel = isEmulator
        
        android.util.Log.d("DeviceDetection", "=== 详细设备信息 ===")
        android.util.Log.d("DeviceDetection", "型号(MODEL): $model")
        android.util.Log.d("DeviceDetection", "制造商(MANUFACTURER): $manufacturer")
        android.util.Log.d("DeviceDetection", "设备(DEVICE): $device")
        android.util.Log.d("DeviceDetection", "产品(PRODUCT): $product")
        android.util.Log.d("DeviceDetection", "是否为Pixel 4: $isPixel4")
        android.util.Log.d("DeviceDetection", "是否为Pixel 9 Pro: $isPixel9Pro")
        android.util.Log.d("DeviceDetection", "是否为模拟器Pixel: $isEmulatorPixel")
        android.util.Log.d("DeviceDetection", "最终判定为问题设备: ${isPixel4 || isPixel9Pro || isEmulatorPixel}")
        
        return isPixel4 || isPixel9Pro || isEmulatorPixel
    }
    
    /**
     * 智能缩放因子 - 根据设备类型选择适当的缩放策略
     */
    @Composable
    fun getIntelligentScalingFactor(): Float {
        return if (isProblematicDevice()) {
            // 对于有问题的设备，使用简化缩放
            getSimpleScalingFactor()
        } else {
            // 对于其他设备，使用完整的缩放系统
            calculateScalingFactors().globalScale
        }
    }
    
    /**
     * 智能缩放Dp值
     */
    @Composable
    fun intelligentScaledDp(baseDp: Dp): Dp {
        val scale = getIntelligentScalingFactor()
        return baseDp * scale
    }
    
    /**
     * 智能缩放TextUnit值
     */
    @Composable
    fun intelligentScaledSp(baseSp: TextUnit): TextUnit {
        val scale = getIntelligentScalingFactor()
        return (baseSp.value * scale).sp
    }
    
    /**
     * 获取统一缩放后的Dp值（所有元素使用相同缩放因子）
     */
    @Composable
    fun uniformScaledDp(baseDp: Dp): Dp {
        val factors = calculateScalingFactors()
        return baseDp * factors.globalScale
    }
    
    /**
     * 获取统一缩放后的TextUnit值（使用全局缩放因子）
     */
    @Composable
    fun uniformScaledSp(baseSp: TextUnit): TextUnit {
        val factors = calculateScalingFactors()
        return (baseSp.value * factors.globalScale).sp
    }
    
    /**
     * 获取统一缩放的图表尺寸（所有图表使用相同的全局缩放）
     */
    @Composable
    fun getUniformChartSizes(
        lineChartHeight: Dp = 250.dp,
        pieChartHeight: Dp = 280.dp,
        barChartHeight: Dp = 260.dp
    ): AdaptiveChartSizes {
        return AdaptiveChartSizes(
            lineChartHeight = uniformScaledDp(lineChartHeight),
            pieChartHeight = uniformScaledDp(pieChartHeight),
            barChartHeight = uniformScaledDp(barChartHeight)
        )
    }
    
    /**
     * 获取自适应的X轴标签间距
     */
    @Composable
    fun getAdaptiveXAxisLabelSpacing(): Dp {
        val metrics = getScreenMetrics()
        
        // 根据图表高度计算合适的X轴标签间距
        val baseSpacing = when {
            metrics.heightDp < 700f -> 8.dp   // 小屏幕：减小间距
            metrics.heightDp > 900f -> 12.dp  // 大屏幕：增大间距
            else -> 10.dp                     // 中等屏幕：标准间距
        }
        
        return scaledDp(baseSpacing, ScaleType.SPACING)
    }
    
    /**
     * 获取自适应的图表内边距
     */
    @Composable  
    fun getAdaptiveChartPadding(): Dp {
        val metrics = getScreenMetrics()
        val baseContainerPadding = when {
            metrics.heightDp < 700f -> 14.dp  // 小屏幕：减小内边距
            metrics.heightDp > 900f -> 18.dp  // 大屏幕：增大内边距
            else -> 16.dp                     // 中等屏幕：标准内边距
        }
        
        return scaledDp(baseContainerPadding, ScaleType.SPACING)
    }
    
    /**
     * 获取优化的分类栏尺寸（记录用户满意的布局参数）
     * 
     * 此函数记录了经过用户验证的最佳分类栏布局参数：
     * - 高度：35dp，在保持字体清晰的同时不会过高
     * - 字体：emoji使用xLarge，文本使用large，确保清晰可读
     * - 内边距：经过优化的紧凑布局，适配更扁的形状
     * - 间距：分类项之间有适当间隔，底部间距为1.5倍基础间距
     * 
     * 所有尺寸都会根据屏幕大小和密度自动缩放，确保在不同设备上
     * 保持一致的视觉效果和用户体验。
     * 
     * @return CategoryBarSettings 包含所有分类栏布局参数的数据类
     */
    @Composable
    fun getOptimizedCategoryBarSettings(): CategoryBarSettings {
        return CategoryBarSettings(
            height = uniformScaledDp(35.dp), // 分类栏高度：标准35dp + 缩放
            cornerRadius = uniformScaledDp(8.dp), // 圆角：标准8dp + 缩放
            horizontalPadding = uniformScaledDp(4.dp), // 水平内边距：标准4dp + 缩放
            verticalPadding = uniformScaledDp(2.dp), // 垂直内边距：标准2dp + 缩放
            itemSpacing = uniformScaledDp(4.dp), // 分类项之间的间距：标准4dp + 缩放
            containerVerticalPadding = uniformScaledDp(4.dp), // 容器垂直内边距：标准4dp + 缩放
            bottomSpacing = uniformScaledDp(6.dp), // 分类栏下方间距：标准6dp + 缩放
            emojiSize = uniformScaledSp(20.sp), // emoji大小：标准20sp + 缩放
            textSize = uniformScaledSp(16.sp) // 文本大小：标准16sp + 缩放
        )
    }
    
    /**
     * 获取优化的首页布局间距（记录用户满意的间距设置）
     * 
     * 此函数记录了经过用户验证的最佳首页布局间距参数：
     * - 选项卡位置：在饼图上方但稍微下移，通过增加上边距实现
     * - 折线图间距：增加5dp用于下移折线图，确保第三张图表有足够显示空间
     * - 日周月按钮：使用缩放后的2dp垂直内边距，8dp水平间距
     * - 水平布局：使用屏幕宽度的3.5%作为水平内边距
     * 
     * 这些间距设置确保了：
     * 1. 分类栏紧凑但清晰
     * 2. 选项卡位置合适，不会太靠上或太靠下
     * 3. 图表区域有充足的显示空间
     * 4. 折线图之间有适当的分隔，X轴标签不会被覆盖
     * 
     * @return HomeLayoutSpacing 包含所有首页布局间距参数的数据类
     */
    @Composable
    fun getOptimizedHomeLayoutSpacing(): HomeLayoutSpacing {
        return HomeLayoutSpacing(
            categoryBarBottomSpacing = uniformScaledDp(6.dp), // 分类栏下方间距：标准6dp + 缩放
            tabSectionTopSpacing = uniformScaledDp(4.dp), // 选项卡上方间距：标准4dp + 缩放
            tabSectionBottomSpacing = uniformScaledDp(5.dp), // 选项卡下方间距：减少3dp上移柱状图 + 缩放
            periodButtonVerticalPadding = uniformScaledDp(2.dp), // 日周月按钮垂直内边距：标准2dp + 缩放
            lineChartSpacing = uniformScaledDp(13.dp), // 折线图之间间距：增加5dp下移折线图 + 缩放
            sectionSpacing = uniformScaledDp(2.dp), // 其他区域标准间距：减少2dp上移下方模块 + 缩放
            horizontalPadding = uniformScaledDp(14.dp) // 水平内边距：标准14dp（约为400dp宽度的3.5%）+ 缩放
        )
    }
    
    /**
     * 获取优化的图表尺寸（记录用户满意的图表大小）
     * 
     * 此函数记录了经过用户验证的最佳图表尺寸参数：
     * - 折线图高度：180dp，确保数据清晰显示但不占用过多空间
     * - 饼图/柱状图高度：增加5dp优化饼图直径显示
     * - 按钮间距：8dp，日周月按钮之间的合适间隔
     * 
     * 所有高度都使用CHART缩放类型，确保图表在不同屏幕上保持
     * 稳定的高度比例，避免过度缩放影响数据可读性。
     * 
     * 间距使用SPACING缩放类型，确保按钮间隔在各种屏幕上都合适。
     * 
     * @return OptimizedChartSizes 包含所有图表尺寸参数的数据类
     */
    @Composable
    fun getOptimizedChartSizes(): OptimizedChartSizes {
        return OptimizedChartSizes(
            lineChartHeight = uniformScaledDp(180.dp), // 折线图高度：标准180dp + 缩放
            pieChartHeight = uniformScaledDp(180.dp),  // 饼图/柱状图高度：增加5dp优化饼图显示 + 缩放
            periodButtonEndPadding = uniformScaledDp(8.dp) // 日周月按钮间距：标准8dp + 缩放
        )
    }
}

/**
 * 缩放类型枚举
 */
enum class ScaleType {
    GLOBAL,    // 全局缩放
    SPACING,   // 间距缩放
    ICON,      // 图标缩放
    CHART      // 图表缩放
}

/**
 * 适配间距数据类
 */
data class AdaptiveSpacing(
    val xSmall: Dp,
    val small: Dp,
    val medium: Dp,
    val large: Dp,
    val xLarge: Dp
)

/**
 * 适配字体大小数据类
 */
data class AdaptiveFontSizes(
    val xSmall: TextUnit,
    val small: TextUnit,
    val medium: TextUnit,
    val large: TextUnit,
    val xLarge: TextUnit,
    val xxLarge: TextUnit
)

/**
 * 适配图表尺寸数据类
 */
data class AdaptiveChartSizes(
    val lineChartHeight: Dp,
    val pieChartHeight: Dp,
    val barChartHeight: Dp
)

/**
 * 分类栏设置数据类
 */
data class CategoryBarSettings(
    val height: Dp,
    val cornerRadius: Dp,
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val itemSpacing: Dp,
    val containerVerticalPadding: Dp,
    val bottomSpacing: Dp,
    val emojiSize: TextUnit,
    val textSize: TextUnit
)

/**
 * 首页布局间距数据类
 */
data class HomeLayoutSpacing(
    val categoryBarBottomSpacing: Dp,
    val tabSectionTopSpacing: Dp,
    val tabSectionBottomSpacing: Dp,
    val periodButtonVerticalPadding: Dp,
    val lineChartSpacing: Dp,
    val sectionSpacing: Dp,
    val horizontalPadding: Dp
)

/**
 * 优化图表尺寸数据类
 */
data class OptimizedChartSizes(
    val lineChartHeight: Dp,
    val pieChartHeight: Dp,
    val periodButtonEndPadding: Dp
) 