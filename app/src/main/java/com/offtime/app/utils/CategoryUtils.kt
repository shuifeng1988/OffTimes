package com.offtime.app.utils

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import android.content.Context

/**
 * 分类相关的工具类
 * 统一管理分类的颜色、图标等视觉元素，确保在不同页面保持一致
 */
object CategoryUtils {
    
    /**
     * 根据分类名称获取对应的图标emoji
     * 支持中英文分类名称
     */
    fun getCategoryEmoji(categoryName: String): String {
        // 首先检查是否为中文分类名称
        return when (categoryName) {
            "娱乐", "Entertainment" -> "🎮"
            "学习", "Study" -> "📚"
            "健身", "Fitness" -> "💪"
            "总使用", "Total Usage" -> "📱"
            "工作", "Work" -> "💼"
            "其他", "Other" -> "📱"
            else -> "📁"
        }
    }
    
    /**
     * 根据分类名称获取对应的背景颜色
     * 支持中英文分类名称
     */
    fun getCategoryColor(categoryName: String): Color {
        return when (categoryName) {
            "娱乐", "Entertainment" -> Color(0xFFFF5722)  // 橙色
            "学习", "Study" -> Color(0xFF2196F3)  // 蓝色
            "健身", "Fitness" -> Color(0xFF9C27B0)  // 紫色
            "总使用", "Total Usage" -> Color(0xFF607D8B) // 蓝灰色
            "工作", "Work" -> Color(0xFF4CAF50)  // 绿色
            "其他", "Other" -> Color(0xFF607D8B)  // 蓝灰色
            else -> Color(0xFF9E9E9E)   // 默认灰色
        }
    }
    
    /**
     * 根据分类名称获取颜色和图标的组合
     */
    fun getCategoryStyle(categoryName: String): CategoryStyle {
        return CategoryStyle(
            emoji = getCategoryEmoji(categoryName),
            color = getCategoryColor(categoryName)
        )
    }
    
    /**
     * 获取本地化的分类名称（使用DateLocalizer）
     */
    fun getLocalizedCategoryName(context: Context, categoryName: String): String {
        return DateLocalizer.getCategoryName(context, categoryName)
    }
    
    /**
     * 分类样式数据类
     */
    data class CategoryStyle(
        val emoji: String,
        val color: Color
    )
} 