package com.offtime.app.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 语言管理工具类
 * 
 * 功能：
 * - 管理应用语言设置
 * - 提供语言切换功能
 * - 处理系统语言变化
 * - 支持中英文双语言
 */
@Singleton
class LocaleUtils @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val PREF_LANGUAGE = "app_language"
        private const val LANGUAGE_AUTO = "auto"
        private const val LANGUAGE_CHINESE = "zh"
        private const val LANGUAGE_ENGLISH = "en"
        
        // 支持的语言列表
        val supportedLanguages = listOf(
            Language(LANGUAGE_AUTO, "跟随系统", "Follow System"),
            Language(LANGUAGE_CHINESE, "简体中文", "Simplified Chinese"),
            Language(LANGUAGE_ENGLISH, "English", "English")
        )
    }
    
    data class Language(
        val code: String,
        val nameChinese: String,
        val nameEnglish: String
    ) {
        fun getDisplayName(currentLocale: Locale): String {
            return when {
                currentLocale.language == "zh" -> nameChinese
                else -> nameEnglish
            }
        }
    }
    
    private val sharedPreferences = context.getSharedPreferences("locale_prefs", Context.MODE_PRIVATE)
    
    /**
     * 获取当前设置的语言
     */
    fun getCurrentLanguage(): String {
        return sharedPreferences.getString(PREF_LANGUAGE, LANGUAGE_AUTO) ?: LANGUAGE_AUTO
    }
    
    /**
     * 设置应用语言
     */
    fun setLanguage(languageCode: String) {
        sharedPreferences.edit()
            .putString(PREF_LANGUAGE, languageCode)
            .apply()
    }
    
    /**
     * 获取实际使用的Locale
     */
    fun getEffectiveLocale(): Locale {
        val savedLanguage = getCurrentLanguage()
        
        return when (savedLanguage) {
            LANGUAGE_CHINESE -> Locale.SIMPLIFIED_CHINESE
            LANGUAGE_ENGLISH -> Locale.ENGLISH
            LANGUAGE_AUTO -> {
                // 跟随系统语言
                val systemLocale = getSystemLocale()
                when {
                    systemLocale.language == "zh" -> Locale.SIMPLIFIED_CHINESE
                    else -> Locale.ENGLISH
                }
            }
            else -> Locale.ENGLISH // 默认英文
        }
    }
    
    /**
     * 获取系统默认语言
     */
    private fun getSystemLocale(): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
    }
    
    /**
     * 应用语言到Context
     */
    fun applyLanguageToContext(context: Context): Context {
        val locale = getEffectiveLocale()
        val configuration = Configuration(context.resources.configuration)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale)
        } else {
            @Suppress("DEPRECATION")
            configuration.locale = locale
        }
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            context.createConfigurationContext(configuration)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
            context
        }
    }
    
    /**
     * 检查当前是否为中文环境
     */
    fun isChineseLocale(): Boolean {
        return getEffectiveLocale().language == "zh"
    }
    
    /**
     * 检查当前是否为英文环境
     */
    fun isEnglishLocale(): Boolean {
        return getEffectiveLocale().language == "en"
    }
    
    /**
     * 获取语言显示名称
     */
    fun getLanguageDisplayName(languageCode: String): String {
        val currentLocale = getEffectiveLocale()
        return supportedLanguages.find { it.code == languageCode }
            ?.getDisplayName(currentLocale) ?: languageCode
    }
}

/**
 * CompositionLocal for LocaleUtils
 */
val LocalLocaleUtils = compositionLocalOf<LocaleUtils> {
    error("LocaleUtils not provided")
}

/**
 * Composable函数用于提供LocaleUtils
 */
@Composable
fun ProvideLocaleUtils(
    localeUtils: LocaleUtils,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalLocaleUtils provides localeUtils) {
        content()
    }
}

/**
 * 获取本地化的字符串资源
 */
@Composable
fun stringResource(resId: Int, vararg formatArgs: Any): String {
    val context = LocalContext.current
    val localeUtils = LocalLocaleUtils.current
    val localizedContext = localeUtils.applyLanguageToContext(context)
    
    return if (formatArgs.isEmpty()) {
        localizedContext.getString(resId)
    } else {
        localizedContext.getString(resId, *formatArgs)
    }
}

/**
 * 获取本地化的字符串资源（带默认值）
 */
@Composable
fun stringResourceWithFallback(resId: Int, fallback: String, vararg formatArgs: Any): String {
    val context = LocalContext.current
    val localeUtils = LocalLocaleUtils.current
    val localizedContext = localeUtils.applyLanguageToContext(context)
    
    return runCatching {
        if (formatArgs.isEmpty()) {
            localizedContext.getString(resId)
        } else {
            localizedContext.getString(resId, *formatArgs)
        }
    }.getOrElse { fallback }
} 