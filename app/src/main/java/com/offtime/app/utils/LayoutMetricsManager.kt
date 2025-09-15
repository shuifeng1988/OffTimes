package com.offtime.app.utils

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * 模块位置和大小信息数据类
 * 
 * @param moduleId 模块唯一标识符
 * @param moduleName 模块名称（中文描述）
 * @param absolutePosition 模块在屏幕中的绝对位置（像素）
 * @param relativePosition 模块相对于屏幕的相对位置（0.0-1.0）
 * @param absoluteSize 模块的绝对大小（像素）
 * @param relativeSize 模块相对于屏幕的相对大小（0.0-1.0）
 * @param dpSize 模块的dp大小
 * @param isVisible 模块是否当前可见
 * @param zIndex 模块的层级（用于重叠判断）
 */
@Stable
data class ModuleLayoutInfo(
    val moduleId: String,
    val moduleName: String,
    val absolutePosition: Offset = Offset.Zero,
    val relativePosition: Offset = Offset.Zero,
    val absoluteSize: Size = Size.Zero,
    val relativeSize: Size = Size.Zero,
    val dpSize: DpSize = DpSize.Zero,
    val isVisible: Boolean = false,
    val zIndex: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * 获取模块在屏幕中的相对宽度百分比
     */
    val relativeWidthPercent: Float get() = relativeSize.width * 100f
    
    /**
     * 获取模块在屏幕中的相对高度百分比
     */
    val relativeHeightPercent: Float get() = relativeSize.height * 100f
    
    /**
     * 获取模块左上角的相对位置百分比
     */
    val relativeLeftPercent: Float get() = relativePosition.x * 100f
    val relativeTopPercent: Float get() = relativePosition.y * 100f
    
    /**
     * 获取模块右下角的相对位置百分比
     */
    val relativeRightPercent: Float get() = (relativePosition.x + relativeSize.width) * 100f
    val relativeBottomPercent: Float get() = (relativePosition.y + relativeSize.height) * 100f
    
    /**
     * 检查模块是否与另一个模块重叠
     */
    fun isOverlappingWith(other: ModuleLayoutInfo): Boolean {
        if (!isVisible || !other.isVisible) return false
        
        val thisLeft = relativePosition.x
        val thisRight = relativePosition.x + relativeSize.width
        val thisTop = relativePosition.y
        val thisBottom = relativePosition.y + relativeSize.height
        
        val otherLeft = other.relativePosition.x
        val otherRight = other.relativePosition.x + other.relativeSize.width
        val otherTop = other.relativePosition.y
        val otherBottom = other.relativePosition.y + other.relativeSize.height
        
        return !(thisRight <= otherLeft || thisLeft >= otherRight || 
                thisBottom <= otherTop || thisTop >= otherBottom)
    }
}

/**
 * 布局屏幕信息数据类
 */
@Stable
data class LayoutScreenInfo(
    val widthPx: Float = 0f,
    val heightPx: Float = 0f,
    val widthDp: Dp = 0.dp,
    val heightDp: Dp = 0.dp,
    val density: Float = 1f,
    val timestamp: Long = System.currentTimeMillis()
) {
    val aspectRatio: Float get() = if (heightPx > 0) widthPx / heightPx else 0f
    val isLandscape: Boolean get() = widthPx > heightPx
    val isPortrait: Boolean get() = heightPx > widthPx
}

/**
 * 全局布局指标管理器
 * 用于记录和管理首页各个模块的位置、大小信息
 */
object LayoutMetricsManager {
    
    // 屏幕信息
    var screenInfo by mutableStateOf(LayoutScreenInfo())
        private set
    
    // 所有模块的布局信息
    private val _moduleLayoutMap = mutableStateMapOf<String, ModuleLayoutInfo>()
    val moduleLayoutMap: Map<String, ModuleLayoutInfo> get() = _moduleLayoutMap.toMap()
    
    // 模块ID常量定义
    object ModuleIds {
        const val CATEGORY_BAR = "category_bar"
        const val TAB_SECTION = "tab_section"
        const val PIE_CHART = "pie_chart"
        const val HOURLY_BAR_CHART = "hourly_bar_chart"
        const val PERIOD_BUTTONS = "period_buttons"
        const val USAGE_LINE_CHART = "usage_line_chart"
        const val COMPLETION_LINE_CHART = "completion_line_chart"
        const val REWARD_CHART = "reward_chart"
        const val PUNISHMENT_CHART = "punishment_chart"
        const val REWARD_MODULE = "reward_module"
        const val PUNISHMENT_MODULE = "punishment_module"
        const val NO_DATA_MODULE = "no_data_module"
    }
    
    /**
     * 更新屏幕信息
     */
    fun updateScreenInfo(
        widthPx: Float,
        heightPx: Float,
        widthDp: Dp,
        heightDp: Dp,
        density: Float
    ) {
        screenInfo = LayoutScreenInfo(
            widthPx = widthPx,
            heightPx = heightPx,
            widthDp = widthDp,
            heightDp = heightDp,
            density = density
        )
        LogUtils.d("LayoutMetrics", "屏幕信息更新: ${widthDp}x${heightDp}dp, ${widthPx}x${heightPx}px, density=$density")
    }
    
    /**
     * 更新模块布局信息
     */
    fun updateModuleLayout(
        moduleId: String,
        moduleName: String,
        absolutePosition: Offset,
        absoluteSize: Size,
        dpSize: DpSize,
        isVisible: Boolean = true,
        zIndex: Float = 0f
    ) {
        val relativePosition = if (screenInfo.widthPx > 0 && screenInfo.heightPx > 0) {
            Offset(
                x = absolutePosition.x / screenInfo.widthPx,
                y = absolutePosition.y / screenInfo.heightPx
            )
        } else Offset.Zero
        
        val relativeSize = if (screenInfo.widthPx > 0 && screenInfo.heightPx > 0) {
            Size(
                width = absoluteSize.width / screenInfo.widthPx,
                height = absoluteSize.height / screenInfo.heightPx
            )
        } else Size.Zero
        
        val layoutInfo = ModuleLayoutInfo(
            moduleId = moduleId,
            moduleName = moduleName,
            absolutePosition = absolutePosition,
            relativePosition = relativePosition,
            absoluteSize = absoluteSize,
            relativeSize = relativeSize,
            dpSize = dpSize,
            isVisible = isVisible,
            zIndex = zIndex
        )
        
        _moduleLayoutMap[moduleId] = layoutInfo
        
        LogUtils.d("LayoutMetrics", "模块布局更新: $moduleName ($moduleId)")
        LogUtils.d("LayoutMetrics", "  位置: dp(${dpSize.width}x${dpSize.height}) 相对(${layoutInfo.relativeLeftPercent}%, ${layoutInfo.relativeTopPercent}%)")
        LogUtils.d("LayoutMetrics", "  大小: 相对(${layoutInfo.relativeWidthPercent}% x ${layoutInfo.relativeHeightPercent}%)")
    }
    
    /**
     * 获取指定模块的布局信息
     */
    fun getModuleLayout(moduleId: String): ModuleLayoutInfo? {
        return _moduleLayoutMap[moduleId]
    }
    
    /**
     * 获取所有可见模块
     */
    fun getVisibleModules(): List<ModuleLayoutInfo> {
        return _moduleLayoutMap.values.filter { it.isVisible }.sortedBy { it.zIndex }
    }
    
    /**
     * 检查两个模块是否重叠
     */
    fun areModulesOverlapping(moduleId1: String, moduleId2: String): Boolean {
        val module1 = _moduleLayoutMap[moduleId1]
        val module2 = _moduleLayoutMap[moduleId2]
        
        return if (module1 != null && module2 != null) {
            module1.isOverlappingWith(module2)
        } else false
    }
    
    /**
     * 获取所有模块的重叠情况
     */
    fun getOverlappingModules(): List<Pair<ModuleLayoutInfo, ModuleLayoutInfo>> {
        val visibleModules = getVisibleModules()
        val overlapping = mutableListOf<Pair<ModuleLayoutInfo, ModuleLayoutInfo>>()
        
        for (i in visibleModules.indices) {
            for (j in i + 1 until visibleModules.size) {
                val module1 = visibleModules[i]
                val module2 = visibleModules[j]
                if (module1.isOverlappingWith(module2)) {
                    overlapping.add(module1 to module2)
                }
            }
        }
        
        return overlapping
    }
    
    /**
     * 清除所有模块布局信息
     */
    fun clearAllModules() {
        _moduleLayoutMap.clear()
        LogUtils.d("LayoutMetrics", "清除所有模块布局信息")
    }
    
    /**
     * 清除指定模块
     */
    fun clearModule(moduleId: String) {
        _moduleLayoutMap.remove(moduleId)
        LogUtils.d("LayoutMetrics", "清除模块: $moduleId")
    }
    
    /**
     * 生成布局报告
     */
    fun generateLayoutReport(): String {
        val report = StringBuilder()
        report.appendLine("=== 首页布局报告 ===")
        report.appendLine("屏幕信息: ${screenInfo.widthDp} x ${screenInfo.heightDp} dp")
        report.appendLine("像素密度: ${screenInfo.density}")
        report.appendLine("屏幕比例: ${String.format("%.2f", screenInfo.aspectRatio)}")
        report.appendLine()
        
        val visibleModules = getVisibleModules()
        report.appendLine("可见模块数量: ${visibleModules.size}")
        report.appendLine()
        
        visibleModules.forEach { module ->
            report.appendLine("【${module.moduleName}】(${module.moduleId})")
            report.appendLine("  相对位置: (${String.format("%.1f", module.relativeLeftPercent)}%, ${String.format("%.1f", module.relativeTopPercent)}%)")
            report.appendLine("  相对大小: ${String.format("%.1f", module.relativeWidthPercent)}% x ${String.format("%.1f", module.relativeHeightPercent)}%")
            report.appendLine("  DP大小: ${module.dpSize.width} x ${module.dpSize.height}")
            report.appendLine("  层级: ${module.zIndex}")
            report.appendLine()
        }
        
        val overlapping = getOverlappingModules()
        if (overlapping.isNotEmpty()) {
            report.appendLine("重叠模块:")
            overlapping.forEach { (module1, module2) ->
                report.appendLine("  ${module1.moduleName} 与 ${module2.moduleName}")
            }
        }
        
        return report.toString()
    }
    
    /**
     * 打印布局报告到日志
     */
    fun printLayoutReport() {
        val report = generateLayoutReport()
        LogUtils.d("LayoutMetrics", report)
    }
} 