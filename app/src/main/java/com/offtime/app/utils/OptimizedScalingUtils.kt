package com.offtime.app.utils

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sqrt

/**
 * 优化的屏幕适配工具类
 * 
 * 解决的问题：
 * 1. 避免传统资源适配与动态缩放的冲突
 * 2. 提供缓存机制提升性能
 * 3. 简化参数体系
 * 4. 支持更多设备类型
 */
object OptimizedScalingUtils {
    
    // 设备类型分类
    enum class DeviceType {
        PHONE_SMALL,    // < 5" (3.5" - 4.9")
        PHONE_MEDIUM,   // 5" - 6.5"
        PHONE_LARGE,    // 6.5" - 7"
        TABLET_SMALL,   // 7" - 9"
        TABLET_LARGE    // > 9"
    }
    
    // 缓存相关
    private var lastConfiguration: Pair<Int, Int>? = null
    private var cachedDeviceProfile: DeviceProfile? = null
    
    /**
     * 设备特征数据类
     */
    data class DeviceProfile(
        val deviceType: DeviceType,
        val screenWidthDp: Int,
        val screenHeightDp: Int,
        val density: Float,
        val diagonalInches: Float,
        val scalingStrategy: ScalingStrategy
    )
    
    /**
     * 缩放策略
     */
    sealed class ScalingStrategy {
        object ResourceOnly : ScalingStrategy()           // 仅使用资源文件适配
        data class DynamicOnly(val factor: Float) : ScalingStrategy()  // 仅使用动态缩放
        data class Hybrid(val baseFactor: Float, val dynamicAdjustment: Float) : ScalingStrategy()  // 混合策略
    }
    
    /**
     * 获取设备特征（带缓存）
     */
    @Composable
    fun getDeviceProfile(): DeviceProfile {
        val configuration = LocalConfiguration.current
        val density = LocalDensity.current
        
        val currentConfig = configuration.screenWidthDp to configuration.screenHeightDp
        
        // 检查缓存
        if (lastConfiguration == currentConfig && cachedDeviceProfile != null) {
            return cachedDeviceProfile!!
        }
        
        // 计算设备特征
        val widthDp = configuration.screenWidthDp
        val heightDp = configuration.screenHeightDp
        val densityValue = density.density
        
        // 计算屏幕对角线（英寸）
        val widthInches = widthDp / (160 * densityValue)
        val heightInches = heightDp / (160 * densityValue)
        val diagonalInches = sqrt(widthInches * widthInches + heightInches * heightInches)
        
        // 判断设备类型
        val deviceType = when {
            diagonalInches < 5.0f -> DeviceType.PHONE_SMALL
            diagonalInches < 6.5f -> DeviceType.PHONE_MEDIUM
            diagonalInches < 7.0f -> DeviceType.PHONE_LARGE
            diagonalInches < 9.0f -> DeviceType.TABLET_SMALL
            else -> DeviceType.TABLET_LARGE
        }
        
        // 确定缩放策略
        val scalingStrategy = determineScalingStrategy(deviceType, densityValue, diagonalInches)
        
        val profile = DeviceProfile(
            deviceType = deviceType,
            screenWidthDp = widthDp,
            screenHeightDp = heightDp,
            density = densityValue,
            diagonalInches = diagonalInches,
            scalingStrategy = scalingStrategy
        )
        
        // 更新缓存
        lastConfiguration = currentConfig
        cachedDeviceProfile = profile
        
        return profile
    }
    
    /**
     * 确定最适合的缩放策略
     */
    private fun determineScalingStrategy(
        deviceType: DeviceType,
        density: Float,
        @Suppress("UNUSED_PARAMETER") diagonalInches: Float
    ): ScalingStrategy {
        return when {
            // 已有完善资源适配的密度范围，优先使用资源文件
            density in 1.0f..1.5f ||  // ldpi
            density in 1.5f..2.25f || // hdpi  
            density in 2.25f..3.375f || // xhdpi
            density in 3.375f..5.0f -> // xxhdpi, xxxhdpi
                ScalingStrategy.ResourceOnly
            
            // 平板设备使用动态缩放
            deviceType == DeviceType.TABLET_SMALL ||
            deviceType == DeviceType.TABLET_LARGE -> {
                val scaleFactor = when (deviceType) {
                    DeviceType.TABLET_SMALL -> 1.1f
                    DeviceType.TABLET_LARGE -> 1.2f
                    else -> 1.0f
                }
                ScalingStrategy.DynamicOnly(scaleFactor)
            }
            
            // 特殊尺寸设备使用混合策略
            else -> {
                val baseFactor = when (deviceType) {
                    DeviceType.PHONE_SMALL -> 0.9f
                    DeviceType.PHONE_LARGE -> 1.05f
                    else -> 1.0f
                }
                ScalingStrategy.Hybrid(baseFactor, 0.1f)
            }
        }
    }
    
    /**
     * 获取适配后的尺寸
     */
    @Composable
    fun adaptiveSize(baseSize: Dp): Dp {
        val profile = getDeviceProfile()
        return when (profile.scalingStrategy) {
            is ScalingStrategy.ResourceOnly -> baseSize // 使用资源文件中的值
            is ScalingStrategy.DynamicOnly -> baseSize * profile.scalingStrategy.factor
            is ScalingStrategy.Hybrid -> baseSize * (profile.scalingStrategy.baseFactor + profile.scalingStrategy.dynamicAdjustment)
        }
    }
    
    /**
     * 获取适配后的字体大小
     */
    @Composable
    fun adaptiveFontSize(baseSize: TextUnit): TextUnit {
        val profile = getDeviceProfile()
        return when (profile.scalingStrategy) {
            is ScalingStrategy.ResourceOnly -> baseSize // 使用资源文件中的值
            is ScalingStrategy.DynamicOnly -> (baseSize.value * profile.scalingStrategy.factor).sp
            is ScalingStrategy.Hybrid -> {
                val factor = profile.scalingStrategy.baseFactor + profile.scalingStrategy.dynamicAdjustment
                (baseSize.value * factor).sp
            }
        }
    }
    
    /**
     * 智能间距计算
     */
    @Composable
    fun smartSpacing(level: SpacingLevel): Dp {
        val profile = getDeviceProfile()
        val baseSpacing = when (level) {
            SpacingLevel.XS -> 2.dp
            SpacingLevel.SM -> 4.dp
            SpacingLevel.MD -> 8.dp
            SpacingLevel.LG -> 12.dp
            SpacingLevel.XL -> 16.dp
            SpacingLevel.XXL -> 24.dp
        }
        
        // 根据设备类型调整间距
        val deviceAdjustment = when (profile.deviceType) {
            DeviceType.PHONE_SMALL -> 0.8f
            DeviceType.PHONE_MEDIUM -> 1.0f
            DeviceType.PHONE_LARGE -> 1.1f
            DeviceType.TABLET_SMALL -> 1.3f
            DeviceType.TABLET_LARGE -> 1.5f
        }
        
        return adaptiveSize(baseSpacing * deviceAdjustment)
    }
    
    /**
     * 图表尺寸适配
     */
    @Composable
    fun adaptiveChartSize(chartType: ChartType): Dp {
        val profile = getDeviceProfile()
        val baseHeight = when (chartType) {
            ChartType.PIE -> 200.dp
            ChartType.LINE -> 160.dp
            ChartType.BAR -> 140.dp
        }
        
        // 平板设备图表可以更大
        val typeAdjustment = when (profile.deviceType) {
            DeviceType.TABLET_SMALL -> 1.2f
            DeviceType.TABLET_LARGE -> 1.4f
            else -> 1.0f
        }
        
        return adaptiveSize(baseHeight * typeAdjustment)
    }
    
    /**
     * 获取设备适配信息（调试用）
     */
    @Composable
    fun getAdaptationInfo(): String {
        val profile = getDeviceProfile()
        return buildString {
            appendLine("设备信息:")
            appendLine("  类型: ${profile.deviceType}")
            appendLine("  尺寸: ${profile.screenWidthDp}x${profile.screenHeightDp}dp")
            appendLine("  密度: ${profile.density}")
            appendLine("  对角线: ${String.format("%.1f", profile.diagonalInches)}\"")
            appendLine("  策略: ${profile.scalingStrategy}")
        }
    }
}

/**
 * 间距级别枚举
 */
enum class SpacingLevel {
    XS, SM, MD, LG, XL, XXL
}

/**
 * 图表类型枚举
 */
enum class ChartType {
    PIE, LINE, BAR
}

/**
 * 便捷的Composable函数
 */

@Composable
fun adaptiveSpacing(level: SpacingLevel): Dp {
    return OptimizedScalingUtils.smartSpacing(level)
}

@Composable
fun adaptiveChartHeight(type: ChartType): Dp {
    return OptimizedScalingUtils.adaptiveChartSize(type)
}

@Composable
fun isTablet(): Boolean {
    val profile = OptimizedScalingUtils.getDeviceProfile()
    return profile.deviceType == OptimizedScalingUtils.DeviceType.TABLET_SMALL ||
           profile.deviceType == OptimizedScalingUtils.DeviceType.TABLET_LARGE
}

@Composable
fun isSmallPhone(): Boolean {
    val profile = OptimizedScalingUtils.getDeviceProfile()
    return profile.deviceType == OptimizedScalingUtils.DeviceType.PHONE_SMALL
}

/**
 * 使用示例：
 * 
 * @Composable
 * fun MyComponent() {
 *     // 使用智能间距
 *     val padding = adaptiveSpacing(SpacingLevel.MD)
 *     
 *     // 使用自适应图表高度
 *     val chartHeight = adaptiveChartHeight(ChartType.PIE)
 *     
 *     // 使用设备类型判断
 *     if (isTablet()) {
 *         // 平板布局
 *     } else {
 *         // 手机布局
 *     }
 *     
 *     Card(
 *         modifier = Modifier.padding(padding)
 *     ) {
 *         PieChart(
 *             modifier = Modifier.height(chartHeight)
 *         )
 *     }
 * }
 */ 