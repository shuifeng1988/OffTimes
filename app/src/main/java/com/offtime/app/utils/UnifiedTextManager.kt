package com.offtime.app.utils

import android.content.Context
import java.util.Locale
import com.offtime.app.OffTimeApplication

/**
 * 统一文本管理器
 * 负责处理所有多语言文本转换，确保快速且一致的显示
 * 解决文本显示不一致的问题
 */
object UnifiedTextManager {
    
    // 语言检测缓存，避免重复获取
    private var cachedLanguage: String? = null
    private var lastLanguageCheckTime: Long = 0L
    private const val LANGUAGE_CACHE_DURATION = 1000L // 1秒缓存
    
    /**
     * 获取当前应用设置的语言（带缓存优化）
     * 注意：使用应用内语言设置，而不是系统语言
     */
    private fun getCurrentLanguage(): String {
        val currentTime = System.currentTimeMillis()
        if (cachedLanguage == null || currentTime - lastLanguageCheckTime > LANGUAGE_CACHE_DURATION) {
            try {
                // 通过OffTimeApplication获取LocaleUtils实例
                val context = OffTimeApplication.instance
                val localeUtils = LocaleUtils(context)
                val effectiveLocale = localeUtils.getEffectiveLocale()
                cachedLanguage = effectiveLocale.language
                lastLanguageCheckTime = currentTime
            } catch (e: Exception) {
                android.util.Log.w("UnifiedTextManager", "获取应用语言设置失败，回退到系统语言: ${e.message}")
                // 回退到系统语言
                cachedLanguage = Locale.getDefault().language
            }
        }
        return cachedLanguage ?: "zh"
    }
    
    /**
     * 判断是否为英文环境
     * 基于应用内语言设置，而不是系统语言
     */
    fun isEnglish(): Boolean = getCurrentLanguage() == "en"
    
    /**
     * 清除语言缓存（在语言切换时调用）
     */
    fun clearLanguageCache() {
        cachedLanguage = null
        lastLanguageCheckTime = 0L
    }
    
    // 文本映射表 - 确保一一对应
    private val rewardTextMap = mapOf(
        "薯片" to "Chips",
        "饮料" to "Drinks", 
        "休息时间" to "Rest time",
        "点心" to "Snacks",
        "咖啡" to "Coffee"
    )
    
    private val punishmentTextMap = mapOf(
        "俯卧撑" to "Push-ups",
        "仰卧起坐" to "Sit-ups",
        "跑步" to "Running",
        "深蹲" to "Squats",
        "平板支撑" to "Plank"
    )
    
    private val unitTextMap = mapOf(
        "包" to "pack",
        "瓶" to "bottle",
        "杯" to "cup",
        "个" to "",
        "分钟" to "minutes",
        "小时" to "hours",
        "公里" to "km"
    )
    
    /**
     * 统一本地化奖励文本
     * 直接根据当前语言返回对应文本，不做复杂解析
     */
    fun localizeRewardText(text: String): String {
        if (text.isBlank()) return text
        
        val cleanText = text.trim()
        return if (isEnglish()) {
            // 中文转英文
            rewardTextMap[cleanText] ?: run {
                // 如果不在映射表中，检查是否已经是英文
                val reverseMap = rewardTextMap.entries.associate { it.value to it.key }
                if (reverseMap.containsKey(cleanText)) cleanText else text
            }
        } else {
            // 英文转中文
            val reverseMap = rewardTextMap.entries.associate { it.value to it.key }
            reverseMap[cleanText] ?: run {
                // 如果不在映射表中，检查是否已经是中文
                if (rewardTextMap.containsKey(cleanText)) cleanText else text
            }
        }
    }
    
    /**
     * 统一本地化惩罚文本
     */
    fun localizePunishmentText(text: String): String {
        if (text.isBlank()) return text
        
        val cleanText = text.trim()
        return if (isEnglish()) {
            // 中文转英文
            punishmentTextMap[cleanText] ?: run {
                val reverseMap = punishmentTextMap.entries.associate { it.value to it.key }
                if (reverseMap.containsKey(cleanText)) cleanText else text
            }
        } else {
            // 英文转中文
            val reverseMap = punishmentTextMap.entries.associate { it.value to it.key }
            reverseMap[cleanText] ?: run {
                if (punishmentTextMap.containsKey(cleanText)) cleanText else text
            }
        }
    }
    
    /**
     * 统一本地化单位文本
     */
    fun localizeUnitText(text: String): String {
        if (text.isBlank()) return text
        
        val cleanText = text.trim()
        return if (isEnglish()) {
            unitTextMap[cleanText] ?: run {
                val reverseMap = unitTextMap.entries.associate { it.value to it.key }
                if (reverseMap.containsKey(cleanText)) cleanText else text
            }
        } else {
            val reverseMap = unitTextMap.entries.associate { it.value to it.key }
            reverseMap[cleanText] ?: run {
                if (unitTextMap.containsKey(cleanText)) cleanText else text
            }
        }
    }
    
    /**
     * 获取标准化的默认奖励文本
     */
    fun getStandardRewardText(): String {
        return if (isEnglish()) "Chips" else "薯片"
    }
    
    /**
     * 获取标准化的默认惩罚文本
     */
    fun getStandardPunishmentText(): String {
        return if (isEnglish()) "Push-ups" else "俯卧撑"
    }
    
    /**
     * 获取标准化的默认奖励单位
     */
    fun getStandardRewardUnit(): String {
        return if (isEnglish()) "pack" else "包"
    }
    
    /**
     * 获取标准化的默认惩罚单位
     */
    fun getStandardPunishmentUnit(): String {
        return if (isEnglish()) "" else "个"
    }
    
    /**
     * 智能解析和格式化文本
     * 替代复杂的parseRewardPunishmentText逻辑
     */
    fun parseAndFormatText(text: String): Pair<String, String> {
        if (text.isBlank()) return Pair("", "")
        
        val cleanText = text.replace("每小时", "").replace("Every Hours", "").trim()
        
        // 简化的解析逻辑
        val numberRegex = """\d+""".toRegex()
        val numberMatch = numberRegex.find(cleanText)
        
        return if (numberMatch != null) {
            val number = numberMatch.value
            val textWithoutNumber = cleanText.replace(number, "").trim()
            
            // 识别内容和单位
            val (content, unit) = if (isEnglish()) {
                // 英文格式处理
                when {
                    textWithoutNumber.contains("packs") -> {
                        val content = textWithoutNumber.replace("packs", "").trim()
                        val unit = if (number.toIntOrNull() == 1) "pack" else "packs"
                        Pair(localizeRewardText(content), "$number $unit")
                    }
                    textWithoutNumber.contains("pack") -> {
                        val content = textWithoutNumber.replace("pack", "").trim()
                        val unit = if (number.toIntOrNull() == 1) "pack" else "packs"
                        Pair(localizeRewardText(content), "$number $unit")
                    }
                    textWithoutNumber.contains("Push-ups") -> {
                        Pair("Push-ups", number)
                    }
                    textWithoutNumber.contains("Chips") -> {
                        Pair("Chips", "$number packs")
                    }
                    else -> {
                        // 默认处理
                        Pair(localizeRewardText(textWithoutNumber), number)
                    }
                }
            } else {
                // 中文格式处理
                when {
                    textWithoutNumber.contains("包") -> {
                        val content = textWithoutNumber.replace("包", "").trim()
                        Pair(localizeRewardText(content), "${number}包")
                    }
                    textWithoutNumber.contains("个") -> {
                        val content = textWithoutNumber.replace("个", "").trim()
                        Pair(localizePunishmentText(content), "${number}个")
                    }
                    textWithoutNumber.contains("俯卧撑") -> {
                        Pair("俯卧撑", "${number}个")
                    }
                    textWithoutNumber.contains("薯片") -> {
                        Pair("薯片", "${number}包")
                    }
                    else -> {
                        // 尝试智能识别
                        val localizedContent = if (punishmentTextMap.containsKey(textWithoutNumber) || 
                                                   punishmentTextMap.containsValue(textWithoutNumber)) {
                            localizePunishmentText(textWithoutNumber)
                        } else {
                            localizeRewardText(textWithoutNumber)
                        }
                        val defaultUnit = if (punishmentTextMap.containsKey(textWithoutNumber) || 
                                            punishmentTextMap.containsValue(textWithoutNumber)) {
                            if (isEnglish()) number else "${number}个"
                        } else {
                            if (isEnglish()) {
                                val unit = if (number.toIntOrNull() == 1) "pack" else "packs"
                                "$number $unit"
                            } else "${number}包"
                        }
                        Pair(localizedContent, defaultUnit)
                    }
                }
            }
            
            Pair(content, unit)
        } else {
            // 没有数字，返回本地化的文本和默认数量
            val localizedText = if (punishmentTextMap.containsKey(cleanText) || 
                                   punishmentTextMap.containsValue(cleanText)) {
                localizePunishmentText(cleanText)
            } else {
                localizeRewardText(cleanText)
            }
            
            val defaultQuantity = if (punishmentTextMap.containsKey(cleanText) || 
                                    punishmentTextMap.containsValue(cleanText)) {
                if (isEnglish()) "30" else "30个"
            } else {
                if (isEnglish()) "1 pack" else "1包"
            }
            
            Pair(localizedText, defaultQuantity)
        }
    }
    
    /**
     * 格式化完整的奖励惩罚文本用于显示
     */
    fun formatCompleteText(content: String, quantity: String): String {
        return if (isEnglish()) {
            if (quantity.contains(" ")) {
                "$quantity $content"  // "1 pack Chips"
            } else {
                "$quantity $content"  // "30 Push-ups"
            }
        } else {
            "$content$quantity"  // "薯片1包" 或 "俯卧撑30个"
        }
    }
} 