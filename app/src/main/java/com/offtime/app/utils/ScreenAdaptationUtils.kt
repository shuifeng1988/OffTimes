package com.offtime.app.utils

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offtime.app.ui.theme.LocalResponsiveDimensions
import com.offtime.app.ui.theme.ScreenDensity
import com.offtime.app.ui.theme.ScreenSize
import com.offtime.app.ui.theme.getScreenDensity
import com.offtime.app.ui.theme.getScreenSize
import kotlin.math.sqrt

/**
 * 屏幕适配工具类
 * 提供便捷的屏幕尺寸和密度适配功能
 */
object ScreenAdaptationUtils {
    
    /**
     * 检测是否为Pixel 4（5.7英寸，1080×2280，444 PPI，设备像素比2.75）
     * 与widget中的检测逻辑保持一致
     */
    fun isPixel4(context: Context): Boolean {
        val displayMetrics = context.resources.displayMetrics
        val density = displayMetrics.density
        val screenWidthPixels = displayMetrics.widthPixels
        val screenHeightPixels = displayMetrics.heightPixels
        
        // 计算屏幕对角线尺寸（英寸）
        val diagonalPixels = sqrt(
            (screenWidthPixels * screenWidthPixels + screenHeightPixels * screenHeightPixels).toDouble()
        )
        val diagonalInches = diagonalPixels / displayMetrics.densityDpi
        
        return kotlin.math.abs(diagonalInches - 5.7) < 0.1 && 
               kotlin.math.abs(density - 2.75f) < 0.1f &&
               screenWidthPixels == 1080 && screenHeightPixels == 2280
    }

    /**
     * 检测是否为其他5.7英寸屏幕（如Pixel 5，不包括Pixel 4）
     * 与widget中的检测逻辑保持一致
     */
    fun isSmallScreen57Inch(context: Context): Boolean {
        val displayMetrics = context.resources.displayMetrics
        val density = displayMetrics.density
        val screenWidthPixels = displayMetrics.widthPixels
        val screenHeightPixels = displayMetrics.heightPixels
        
        // 计算屏幕对角线尺寸（英寸）
        val diagonalPixels = sqrt(
            (screenWidthPixels * screenWidthPixels + screenHeightPixels * screenHeightPixels).toDouble()
        )
        val diagonalInches = diagonalPixels / displayMetrics.densityDpi
        
        // 排除Pixel 4
        val isPixel4Device = kotlin.math.abs(diagonalInches - 5.7) < 0.1 && 
                            kotlin.math.abs(density - 2.75f) < 0.1f &&
                            screenWidthPixels == 1080 && screenHeightPixels == 2280
        
        // 判断是否为其他5.7英寸屏幕（Pixel 5类型，排除Pixel 4）
        return diagonalInches <= 6.0 && density >= 2.5f && !isPixel4Device
    }
    
    /**
     * Composable版本的Pixel 4检测
     */
    @Composable
    fun isPixel4(): Boolean {
        val context = LocalContext.current
        return isPixel4(context)
    }

    /**
     * Composable版本的5.7英寸屏幕检测（不包括Pixel 4）
     */
    @Composable
    fun isSmallScreen57Inch(): Boolean {
        val context = LocalContext.current
        return isSmallScreen57Inch(context)
    }
    
    /**
     * 获取屏幕适配信息
     */
    @Composable
    fun getScreenInfo(): ScreenInfo {
        val configuration = LocalConfiguration.current
        val density = LocalDensity.current
        val screenSize = getScreenSize()
        val screenDensity = getScreenDensity()
        
        return ScreenInfo(
            screenWidthDp = configuration.screenWidthDp,
            screenHeightDp = configuration.screenHeightDp,
            density = density.density,
            screenSize = screenSize,
            screenDensity = screenDensity,
            isSmallScreen57Inch = isSmallScreen57Inch()
        )
    }
    
    /**
     * 根据屏幕尺寸计算自适应值
     */
    @Composable
    fun adaptiveValue(
        small: Float,
        medium: Float,
        large: Float
    ): Float {
        val screenSize = getScreenSize()
        return when (screenSize) {
            ScreenSize.Small -> small
            ScreenSize.Medium -> medium
            ScreenSize.Large -> large
        }
    }
    
    /**
     * 根据屏幕密度计算自适应值
     */
    @Composable
    fun densityAdaptiveValue(
        low: Float,
        medium: Float,
        high: Float,
        xHigh: Float,
        xxHigh: Float
    ): Float {
        val screenDensity = getScreenDensity()
        return when (screenDensity) {
            ScreenDensity.Low -> low
            ScreenDensity.Medium -> medium
            ScreenDensity.High -> high
            ScreenDensity.XHigh -> xHigh
            ScreenDensity.XXHigh -> xxHigh
        }
    }
    
    /**
     * 为5.7英寸屏幕优化的自适应值计算
     */
    @Composable
    fun adaptiveValueFor57Inch(
        smallScreen57: Float,
        normalScreen: Float
    ): Float {
        return if (isSmallScreen57Inch()) {
            smallScreen57
        } else {
            normalScreen
        }
    }
    
    /**
     * 获取自适应的间距值
     */
    @Composable
    fun getAdaptiveSpacing(baseSpacing: Dp): Dp {
        val dimensions = LocalResponsiveDimensions
        val ratio = baseSpacing / 8.dp // 以8dp为基准
        return dimensions.spacingMedium * ratio
    }
    
    /**
     * 获取自适应的字体大小
     */
    @Composable
    fun getAdaptiveFontSize(baseFontSize: TextUnit): TextUnit {
        val dimensions = LocalResponsiveDimensions
        val ratio = baseFontSize.value / 14f // 以14sp为基准
        return (dimensions.fontSizeMedium.value * ratio).sp
    }
    
    /**
     * 获取自适应的图标大小
     */
    @Composable
    fun getAdaptiveIconSize(baseIconSize: Dp): Dp {
        val dimensions = LocalResponsiveDimensions
        val ratio = baseIconSize / 24.dp // 以24dp为基准
        return dimensions.iconSizeMedium * ratio
    }
    
    /**
     * 判断是否为小屏幕
     */
    @Composable
    fun isSmallScreen(): Boolean {
        return getScreenSize() == ScreenSize.Small
    }
    
    /**
     * 判断是否为大屏幕
     */
    @Composable
    fun isLargeScreen(): Boolean {
        return getScreenSize() == ScreenSize.Large
    }
    
    /**
     * 判断是否为高密度屏幕
     */
    @Composable
    fun isHighDensityScreen(): Boolean {
        val density = getScreenDensity()
        return density == ScreenDensity.XHigh || density == ScreenDensity.XXHigh
    }
    
    /**
     * 获取图表的自适应高度
     */
    @Composable
    fun getAdaptiveChartHeight(chartType: com.offtime.app.utils.ChartType): Dp {
        val dimensions = LocalResponsiveDimensions
        return when (chartType) {
            com.offtime.app.utils.ChartType.LINE -> dimensions.chartHeight
            com.offtime.app.utils.ChartType.PIE -> dimensions.pieChartHeight
            com.offtime.app.utils.ChartType.BAR -> dimensions.barChartHeight
        }
    }
    
    /**
     * 获取自适应的按钮高度
     */
    @Composable
    fun getAdaptiveButtonHeight(): Dp {
        return LocalResponsiveDimensions.buttonHeight
    }
    
    /**
     * 获取自适应的卡片圆角
     */
    @Composable
    fun getAdaptiveCornerRadius(multiplier: Float = 1.0f): Dp {
        return LocalResponsiveDimensions.cornerRadius * multiplier
    }
    
    /**
     * 获取自适应的卡片阴影
     */
    @Composable
    fun getAdaptiveCardElevation(multiplier: Float = 1.0f): Dp {
        return LocalResponsiveDimensions.cardElevation * multiplier
    }
}

/**
 * 屏幕信息数据类
 */
data class ScreenInfo(
    val screenWidthDp: Int,
    val screenHeightDp: Int,
    val density: Float,
    val screenSize: ScreenSize,
    val screenDensity: ScreenDensity,
    val isSmallScreen57Inch: Boolean = false
)

// ChartType 已移动到 OptimizedScalingUtils.kt 