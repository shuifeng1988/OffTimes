package com.offtime.app.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.offtime.app.utils.LayoutMetricsManager

/**
 * 用于追踪模块布局信息的自定义修饰符
 * 
 * @param moduleId 模块唯一标识符
 * @param moduleName 模块名称（中文描述）
 * @param isVisible 模块是否可见（默认为true）
 * @param zIndex 模块层级（默认为0）
 * @param onLayoutChanged 布局变化回调（可选）
 */
fun Modifier.trackLayout(
    moduleId: String,
    moduleName: String,
    isVisible: Boolean = true,
    zIndex: Float = 0f,
    onLayoutChanged: ((ModuleLayoutInfo) -> Unit)? = null
): Modifier = composed {
    val density = LocalDensity.current
    var lastPosition by remember { mutableStateOf(Offset.Zero) }
    var lastSize by remember { mutableStateOf(Size.Zero) }
    
    this.onGloballyPositioned { coordinates ->
        val size = Size(
            width = coordinates.size.width.toFloat(),
            height = coordinates.size.height.toFloat()
        )
        
        // 对于位置，我们使用零值作为占位符，主要关注大小信息
        val position = Offset.Zero
        
        // 只在大小发生变化时更新
        if (size != lastSize) {
            lastPosition = position
            lastSize = size
            
            val dpSize = with(density) {
                DpSize(
                    width = size.width.toDp(),
                    height = size.height.toDp()
                )
            }
            
            // 更新全局布局管理器
            LayoutMetricsManager.updateModuleLayout(
                moduleId = moduleId,
                moduleName = moduleName,
                absolutePosition = position,
                absoluteSize = size,
                dpSize = dpSize,
                isVisible = isVisible,
                zIndex = zIndex
            )
            
            // 触发回调
            onLayoutChanged?.invoke(
                LayoutMetricsManager.getModuleLayout(moduleId)!!
            )
        }
    }
}

/**
 * 专门用于追踪大小变化的修饰符
 */
fun Modifier.trackSize(
    moduleId: String,
    moduleName: String,
    isVisible: Boolean = true,
    zIndex: Float = 0f
): Modifier = composed {
    val density = LocalDensity.current
    
    this.onSizeChanged { size ->
        val sizeF = Size(
            width = size.width.toFloat(),
            height = size.height.toFloat()
        )
        
        val dpSize = with(density) {
            DpSize(
                width = sizeF.width.toDp(),
                height = sizeF.height.toDp()
            )
        }
        
        // 获取现有的位置信息，如果没有则使用零值
        val existingLayout = LayoutMetricsManager.getModuleLayout(moduleId)
        val position = existingLayout?.absolutePosition ?: Offset.Zero
        
        LayoutMetricsManager.updateModuleLayout(
            moduleId = moduleId,
            moduleName = moduleName,
            absolutePosition = position,
            absoluteSize = sizeF,
            dpSize = dpSize,
            isVisible = isVisible,
            zIndex = zIndex
        )
    }
}

/**
 * 数据类：模块布局信息（与LayoutMetricsManager中的相同，避免循环依赖）
 */
typealias ModuleLayoutInfo = com.offtime.app.utils.ModuleLayoutInfo 