package com.offtime.app.utils

import android.content.Context
import com.offtime.app.R
import java.util.Locale

/**
 * 默认值本地化工具类
 * 用于处理数据库默认值在不同语言环境下的显示
 */
object DefaultValueLocalizer {
    
    /**
     * 获取本地化的默认奖励内容
     */
    fun getLocalizedRewardText(context: Context): String {
        return context.getString(R.string.default_reward_chips)
    }
    
    /**
     * 获取本地化的默认奖励单位
     */
    fun getLocalizedRewardUnit(context: Context): String {
        return context.getString(R.string.default_reward_pack)
    }
    
    /**
     * 获取本地化的默认惩罚动作
     */
    fun getLocalizedPunishmentText(context: Context): String {
        return context.getString(R.string.default_punishment_pushups)
    }
    
    /**
     * 获取本地化的默认惩罚单位
     */
    fun getLocalizedPunishmentUnit(context: Context): String {
        return context.getString(R.string.default_punishment_count)
    }
    
    /**
     * 获取本地化的默认时间单位（分钟）
     */
    fun getLocalizedTimeUnitMinute(context: Context): String {
        return context.getString(R.string.default_time_unit_minute)
    }
    
    /**
     * 获取本地化的默认时间单位（小时）
     */
    fun getLocalizedTimeUnitHour(context: Context): String {
        return context.getString(R.string.default_time_unit_hour)
    }
    
    /**
     * 本地化奖励文本
     * 使用统一文本管理器确保一致的转换
     */
    fun localizeRewardText(context: Context, text: String): String {
        return UnifiedTextManager.localizeRewardText(text)
    }
    
    /**
     * 本地化惩罚文本  
     * 使用统一文本管理器确保一致的转换
     */
    fun localizePunishmentText(context: Context, text: String): String {
        return UnifiedTextManager.localizePunishmentText(text)
    }
    
    /**
     * 本地化奖励单位文本
     * 使用统一文本管理器确保一致的转换  
     */
    fun localizeRewardUnit(context: Context, text: String): String {
        return UnifiedTextManager.localizeUnitText(text)
    }
    
    /**
     * 本地化惩罚单位文本
     * 使用统一文本管理器确保一致的转换
     */
    fun localizePunishmentUnit(context: Context, text: String): String {
        return UnifiedTextManager.localizeUnitText(text)
    }
    
    /**
     * 本地化时间单位
     * 根据系统语言返回相应的时间单位
     */
    fun localizeTimeUnit(context: Context, text: String): String {
        return if (UnifiedTextManager.isEnglish()) {
            when (text) {
                "分钟" -> "minutes"
                "小时" -> "hours"
                "天" -> "days" 
                "周" -> "weeks"
                "月" -> "months"
                else -> text
            }
        } else {
            when (text) {
                "minutes" -> "分钟"
                "hours" -> "小时"
                "days" -> "天"
                "weeks" -> "周"
                "months" -> "月"
                else -> text
            }
        }
    }
    
    /**
     * 将本地化的时间单位转换回标准版本（中文）
     * 用于保存到数据库时的转换
     */
    fun reverseLocalizeTimeUnit(timeUnit: String): String {
        return when (timeUnit) {
            "hours", "Hours", "小时" -> "小时"
            "minutes", "Minutes", "分钟" -> "分钟"
            "seconds", "Seconds", "秒" -> "秒"
            "days", "Days", "天" -> "天"
            else -> timeUnit
        }
    }
    
    /**
     * 本地化完整的奖励描述
     * 格式：内容 + 数量 + 单位
     */
    fun localizeRewardDescription(context: Context, text: String, number: Int, unit: String): String {
        val localizedText = localizeRewardText(context, text)
        val localizedUnit = localizeRewardUnit(context, unit)
        
        return if (Locale.getDefault().language == "en") {
            // 英文环境：数字在前，添加空格，处理复数
            if (localizedUnit.isEmpty()) {
                "$localizedText $number" // e.g., "Chips 2"
            } else {
                val unit = if (number == 1) localizedUnit else {
                    // 简单的复数处理
                    when (localizedUnit) {
                        "pack" -> "packs"
                        else -> "${localizedUnit}s"
                    }
                }
                "$localizedText $number $unit" // e.g., "Chips 2 packs"
            }
        } else {
            // 中文环境：保持原格式
            "$localizedText$number$localizedUnit" // e.g., "薯片2包"
        }
    }
    
    /**
     * 本地化完整的惩罚描述
     * 格式：动作 + 数量 + 单位
     */
    fun localizePunishmentDescription(context: Context, text: String, number: Int, unit: String): String {
        val localizedText = localizePunishmentText(context, text)
        val localizedUnit = localizePunishmentUnit(context, unit)

        return if (Locale.getDefault().language == "en") {
            if (localizedUnit.isEmpty()) {
                "$number $localizedText" // e.g., "240 Push-ups"
            } else {
                "$number $localizedUnit $localizedText" // e.g., "5 km Running"
            }
        } else {
            "$localizedText$number$localizedUnit" // e.g., "俯卧撑240个"
        }
    }
    
    /**
     * 本地化带频率的奖励描述
     * 格式：每 + 时间单位 + 内容 + 数量 + 单位
     */
    fun localizeRewardWithFrequency(context: Context, timeUnit: String, text: String, number: Int, unit: String): String {
        val localizedTimeUnit = localizeTimeUnit(context, timeUnit)
        val localizedText = localizeRewardText(context, text)
        val localizedUnit = localizeRewardUnit(context, unit)
        return "每${localizedTimeUnit}${localizedText}${number}${localizedUnit}"
    }
    
    /**
     * 本地化带频率的惩罚描述
     * 格式：每 + 时间单位 + 动作 + 数量 + 单位
     */
    fun localizePunishmentWithFrequency(context: Context, timeUnit: String, text: String, number: Int, unit: String): String {
        val localizedTimeUnit = localizeTimeUnit(context, timeUnit)
        val localizedText = localizePunishmentText(context, text)
        val localizedUnit = localizePunishmentUnit(context, unit)
        return "每${localizedTimeUnit}${localizedText}${number}${localizedUnit}"
    }
} 