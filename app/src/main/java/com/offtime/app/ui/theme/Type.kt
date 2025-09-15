package com.offtime.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 基础Typography（静态版本，用于预览等场景）
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

/**
 * 响应式Typography
 * 根据屏幕尺寸和密度自动调整字体大小
 */
@Composable
fun getResponsiveTypography(): Typography {
    val dimensions = getResponsiveDimensions()
    
    return Typography(
        // 显示样式 - 最大的文字
        displayLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = dimensions.fontSizeXXLarge,
            lineHeight = dimensions.fontSizeXXLarge * 1.12f,
            letterSpacing = (-0.25).sp
        ),
        displayMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = dimensions.fontSizeXLarge,
            lineHeight = dimensions.fontSizeXLarge * 1.15f,
            letterSpacing = 0.sp
        ),
        displaySmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = dimensions.fontSizeLarge,
            lineHeight = dimensions.fontSizeLarge * 1.22f,
            letterSpacing = 0.sp
        ),
        
        // 标题样式
        headlineLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = dimensions.fontSizeXLarge,
            lineHeight = dimensions.fontSizeXLarge * 1.25f,
            letterSpacing = 0.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = dimensions.fontSizeLarge,
            lineHeight = dimensions.fontSizeLarge * 1.29f,
            letterSpacing = 0.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = dimensions.fontSizeMedium,
            lineHeight = dimensions.fontSizeMedium * 1.33f,
            letterSpacing = 0.sp
        ),
        
        // 大标题样式
        titleLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = dimensions.fontSizeLarge,
            lineHeight = dimensions.fontSizeLarge * 1.27f,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = dimensions.fontSizeMedium,
            lineHeight = dimensions.fontSizeMedium * 1.5f,
            letterSpacing = 0.15.sp
        ),
        titleSmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = dimensions.fontSizeSmall,
            lineHeight = dimensions.fontSizeSmall * 1.43f,
            letterSpacing = 0.1.sp
        ),
        
        // 正文样式
        bodyLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = dimensions.fontSizeMedium,
            lineHeight = dimensions.fontSizeMedium * 1.5f,
            letterSpacing = 0.5.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = dimensions.fontSizeSmall,
            lineHeight = dimensions.fontSizeSmall * 1.43f,
            letterSpacing = 0.25.sp
        ),
        bodySmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = dimensions.fontSizeXSmall,
            lineHeight = dimensions.fontSizeXSmall * 1.33f,
            letterSpacing = 0.4.sp
        ),
        
        // 标签样式
        labelLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = dimensions.fontSizeSmall,
            lineHeight = dimensions.fontSizeSmall * 1.43f,
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = dimensions.fontSizeXSmall,
            lineHeight = dimensions.fontSizeXSmall * 1.33f,
            letterSpacing = 0.5.sp
        ),
        labelSmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = dimensions.fontSizeXSmall * 0.91f,
            lineHeight = dimensions.fontSizeXSmall * 1.45f,
            letterSpacing = 0.5.sp
        )
    )
}

/**
 * 快速访问响应式Typography的扩展属性
 */
val LocalResponsiveTypography @Composable get() = getResponsiveTypography() 