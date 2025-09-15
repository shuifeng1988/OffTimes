package com.offtime.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    // 明确设置所有背景相关颜色为白色，确保一致的显示效果
    background = Color.White,
    surface = Color.White,
    surfaceVariant = Color(0xFFF8F8F8), // 稍微偏灰的白色，用于区分不同层级
    // 确保文本颜色为深色，在白色背景上清晰可见
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    onSurfaceVariant = Color(0xFF49454F),
    // 其他颜色保持默认
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White
)

@Composable
fun OffTimeTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = false, // 强制设置为false，始终使用浅色主题
    // Dynamic color is available on Android 12+
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false, // 禁用动态颜色，确保一致的显示效果
    content: @Composable () -> Unit
) {
    // 始终使用浅色主题配置
    val colorScheme = LightColorScheme
    
    // 获取响应式Typography
    val responsiveTypography = getResponsiveTypography()
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // 设置状态栏颜色为白色
            window.statusBarColor = Color.White.toArgb()
            // 设置状态栏图标为深色，在白色背景上清晰可见
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = responsiveTypography, // 使用响应式Typography
        content = content
    )
} 