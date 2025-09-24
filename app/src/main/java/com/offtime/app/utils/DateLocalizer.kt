package com.offtime.app.utils

import android.content.Context
import com.offtime.app.R
import java.text.SimpleDateFormat
import java.util.*

/**
 * 日期本地化工具类
 * 统一处理应用中所有日期和时间周期的本地化显示
 */
object DateLocalizer {

    /**
     * 格式化时间段标签
     * 处理中英文时间段的显示
     */
    fun formatPeriodLabel(context: Context, period: String): String {
        return when (period) {
            "今日", "今天", "Today" -> context.getString(R.string.period_today)
            "昨日", "昨天", "Yesterday" -> context.getString(R.string.period_yesterday)
            "本周", "Week" -> context.getString(R.string.period_week)
            "本月", "Month" -> context.getString(R.string.period_month)
            "总共", "All" -> context.getString(R.string.period_all)
            "前日" -> context.getString(R.string.period_day_before_yesterday)
            "上周" -> context.getString(R.string.period_last_week)
            "上月" -> context.getString(R.string.period_last_month)
            "日" -> context.getString(R.string.period_day_short)
            "周" -> context.getString(R.string.period_week_short)
            "月" -> context.getString(R.string.period_month_short)
            else -> period
        }
    }

    /**
     * 格式化Widget中的日期标签
     */
    fun formatWidgetDateLabel(context: Context, label: String, isToday: Boolean): String {
        return if (isToday) {
            context.getString(R.string.time_period_today)
        } else {
            val parts = label.split("-")
            "${parts[1]}-${parts[2]}"  // MM-dd格式
        }
    }

    /**
     * 获取时间周期标签（用于下拉选择等）
     */
    fun getPeriodLabels(context: Context): Map<String, String> {
        return mapOf(
            "今日" to context.getString(R.string.time_period_today),
            "昨日" to context.getString(R.string.time_period_yesterday),
            "本周" to context.getString(R.string.time_period_this_week),
            "本月" to context.getString(R.string.time_period_this_month),
            "日" to context.getString(R.string.time_period_day),
            "周" to context.getString(R.string.time_period_week),
            "月" to context.getString(R.string.time_period_month),
            "总共" to context.getString(R.string.time_period_total)
        )
    }

    /**
     * 获取分类名称的本地化
     */
    fun getCategoryName(context: Context, categoryName: String): String {
        return when {
            categoryName == "娱乐" || categoryName == "Entertainment" -> context.getString(R.string.category_entertainment)
            categoryName == "学习" || categoryName == "Study" || categoryName == "Education" -> context.getString(R.string.category_study)
            categoryName == "健身" || categoryName == "Fitness" -> context.getString(R.string.category_fitness)
            categoryName == "总使用" || categoryName == "Total Usage" -> context.getString(R.string.category_total_usage)
            categoryName == "工作" || categoryName == "Work" -> context.getString(R.string.category_work)
            categoryName == "其他" || categoryName == "Other" -> context.getString(R.string.category_other)
            categoryName == "未知分类" || categoryName == "Unknown Category" -> context.getString(R.string.category_unknown)
            // 处理线下活动的本地化 - 使用UnifiedTextManager的语言判断
            categoryName == "线下娱乐" -> if (UnifiedTextManager.isEnglish()) "Offline-Ent" else "线下娱乐"
            categoryName == "线下学习" -> if (UnifiedTextManager.isEnglish()) "Offline-Edu" else "线下学习"
            categoryName == "线下健身" -> if (UnifiedTextManager.isEnglish()) "Offline-Fit" else "线下健身"
            categoryName == "线下工作" -> if (UnifiedTextManager.isEnglish()) "Offline-Work" else "线下工作"
            categoryName == "线下其他" -> if (UnifiedTextManager.isEnglish()) "Offline-Other" else "线下其他"
            // 处理包含"(线下)"的情况
            categoryName.contains("(线下)") -> {
                val baseCategoryName = categoryName.substringBefore(" (线下)")
                val localizedBaseName = getCategoryName(context, baseCategoryName)
                "$localizedBaseName (${context.getString(R.string.offline_label)})"
            }
            else -> categoryName // 如果没有匹配的本地化，返回原始名称
        }
    }

    /**
     * 获取时间单位的本地化
     */
    fun getTimeUnit(context: Context, unitType: String): String {
        return when (unitType) {
            "minutes" -> context.getString(R.string.time_unit_minutes)
            "hours" -> context.getString(R.string.time_unit_hours)
            "apps" -> context.getString(R.string.time_unit_apps)
            else -> unitType
        }
    }

    /**
     * 获取时间单位的本地化（缩写形式，用于widget）
     */
    fun getTimeUnitShort(context: Context, unitType: String): String {
        return when (unitType) {
            "minutes" -> context.getString(R.string.time_unit_minutes_short)
            "hours" -> context.getString(R.string.time_unit_hours_short)
            "apps" -> context.getString(R.string.time_unit_apps_short)
            else -> unitType
        }
    }

    /**
     * 获取分类名称的本地化（全称形式，用于分类页面和设置）
     */
    fun getCategoryNameFull(context: Context, categoryName: String): String {
        return when {
            categoryName == "娱乐" || categoryName == "Entertainment" -> context.getString(R.string.category_entertainment_full)
            categoryName == "学习" || categoryName == "Study" || categoryName == "Education" -> context.getString(R.string.category_study_full)
            categoryName == "健身" || categoryName == "Fitness" -> context.getString(R.string.category_fitness_full)
            categoryName == "总使用" || categoryName == "Total Usage" -> context.getString(R.string.category_total_usage_full)
            categoryName == "工作" || categoryName == "Work" -> context.getString(R.string.category_work_full)
            categoryName == "其他" || categoryName == "Other" -> context.getString(R.string.category_other_full)
            categoryName == "未知分类" || categoryName == "Unknown Category" -> context.getString(R.string.category_unknown_full)
            // 处理线下活动的本地化 - 使用全称
            categoryName == "线下娱乐" -> if (UnifiedTextManager.isEnglish()) "Offline-Entertainment" else "线下娱乐"
            categoryName == "线下学习" -> if (UnifiedTextManager.isEnglish()) "Offline-Education" else "线下学习"
            categoryName == "线下健身" -> if (UnifiedTextManager.isEnglish()) "Offline-Fitness" else "线下健身"
            categoryName == "线下工作" -> if (UnifiedTextManager.isEnglish()) "Offline-Work" else "线下工作"
            categoryName == "线下其他" -> if (UnifiedTextManager.isEnglish()) "Offline-Other" else "线下其他"
            // 处理包含"(线下)"的情况
            categoryName.contains("(线下)") -> {
                val baseCategoryName = categoryName.substringBefore(" (线下)")
                val localizedBaseName = getCategoryNameFull(context, baseCategoryName)
                "$localizedBaseName (${context.getString(R.string.offline_label)})"
            }
            else -> categoryName // 如果没有匹配的本地化，返回原始名称
        }
    }

    /**
     * 本地化应用名称显示
     * 处理线下活动应用的名称本地化
     */
    fun localizeAppName(context: Context, appName: String): String {
        return when {
            // 线下活动应用的本地化
            appName.startsWith("🏃 线下活动 (") && appName.endsWith(")") -> {
                val categoryName = appName.substring("🏃 线下活动 (".length, appName.length - 1)
                val localizedCategoryName = getCategoryName(context, categoryName)
                "🏃 ${context.getString(R.string.offline_activity)} ($localizedCategoryName)"
            }
            else -> appName
        }
    }

    /**
     * 本地化惩罚文本
     * 使用UnifiedTextManager确保与应用内语言设置一致
     * 例如: "俯卧撑30个" -> 根据应用语言设置显示中文或英文
     */
    fun localizePunishmentText(@Suppress("UNUSED_PARAMETER") context: Context, punishmentText: String): String {
        // 使用UnifiedTextManager的语言判断，而不是系统语言
        val isEnglish = UnifiedTextManager.isEnglish()
        val text = punishmentText.trim()
        
        // 添加调试日志
        android.util.Log.d("DateLocalizer", "localizePunishmentText - 输入: '$text', 应用语言: ${if (isEnglish) "英文" else "中文"}")
        
        // 使用UnifiedTextManager的本地化方法，确保一致性
        val result = UnifiedTextManager.localizePunishmentText(text)
        
        // 添加调试日志
        android.util.Log.d("DateLocalizer", "localizePunishmentText - 输出: '$result'")
        
        return result
    }

    /**
     * 本地化奖励文本
     * 使用UnifiedTextManager确保与应用内语言设置一致
     * 例如: "薯片1包" -> 根据应用语言设置显示中文或英文
     */
    fun localizeRewardText(@Suppress("UNUSED_PARAMETER") context: Context, rewardText: String): String {
        // 使用UnifiedTextManager的语言判断，而不是系统语言
        val isEnglish = UnifiedTextManager.isEnglish()
        val text = rewardText.trim()
        
        // 添加调试日志
        android.util.Log.d("DateLocalizer", "localizeRewardText - 输入: '$text', 应用语言: ${if (isEnglish) "英文" else "中文"}")
        
        // 使用UnifiedTextManager的本地化方法，确保一致性
        val result = UnifiedTextManager.localizeRewardText(text)
        
        // 添加调试日志
        android.util.Log.d("DateLocalizer", "localizeRewardText - 输出: '$result'")
        
        return result
    }


} 