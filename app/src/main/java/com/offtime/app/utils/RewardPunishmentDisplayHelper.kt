package com.offtime.app.utils

import com.offtime.app.data.dao.RewardPunishmentData
import com.offtime.app.data.dao.UsageData
import com.offtime.app.data.repository.AppSettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RewardPunishmentDisplayHelper @Inject constructor(
    private val appSettingsRepository: AppSettingsRepository
) {
    
    /**
     * 检查指定类别的奖罚是否应该显示
     */
    suspend fun shouldShowRewardPunishment(categoryId: Int): Boolean {
        return appSettingsRepository.isCategoryRewardPunishmentEnabled(categoryId)
    }
    
    /**
     * 过滤奖罚数据，将关闭的类别显示为灰色线条
     */
    suspend fun filterRewardPunishmentData(
        data: List<RewardPunishmentData>,
        categoryId: Int
    ): List<RewardPunishmentData> {
        val isEnabled = shouldShowRewardPunishment(categoryId)
        return if (isEnabled) {
            data
        } else {
            // 返回全灰色数据（所有值设为0）
            data.map { item ->
                item.copy(
                    rewardValue = 0f,
                    punishmentValue = 0f
                )
            }
        }
    }
    
    /**
     * 同步版本的过滤方法，用于非协程环境
     */
    fun filterRewardPunishmentDataSync(
        data: List<RewardPunishmentData>,
        isEnabled: Boolean
    ): List<RewardPunishmentData> {
        return if (isEnabled) {
            data
        } else {
            // 返回全灰色数据（所有值设为0）
            data.map { item ->
                item.copy(
                    rewardValue = 0f,
                    punishmentValue = 0f
                )
            }
        }
    }
    
    /**
     * 获取奖罚显示文本，关闭时显示"---"
     */
    suspend fun getRewardPunishmentDisplayText(
        categoryId: Int,
        rewardCompletionRate: Float,
        punishmentCompletionRate: Float
    ): String {
        val isEnabled = shouldShowRewardPunishment(categoryId)
        return if (isEnabled) {
            "${rewardCompletionRate.toInt()}%/${punishmentCompletionRate.toInt()}%"
        } else {
            "---"
        }
    }
    
    /**
     * 获取奖罚显示颜色，关闭时显示灰色
     */
    suspend fun getRewardPunishmentDisplayColor(
        categoryId: Int,
        rewardCompletionRate: Float,
        punishmentCompletionRate: Float
    ): androidx.compose.ui.graphics.Color {
        val isEnabled = shouldShowRewardPunishment(categoryId)
        return if (isEnabled) {
            // 原有的颜色逻辑
            when {
                rewardCompletionRate > 0 && punishmentCompletionRate > 0 -> 
                    androidx.compose.ui.graphics.Color(0xFF4CAF50) // 绿色 - 奖励和惩罚都有完成
                rewardCompletionRate > 0 || punishmentCompletionRate > 0 -> 
                    androidx.compose.ui.graphics.Color(0xFFFF9800) // 橙色 - 部分完成
                else -> androidx.compose.ui.graphics.Color(0xFFF44336) // 红色 - 都未完成
            }
        } else {
            androidx.compose.ui.graphics.Color.Gray // 灰色 - 已关闭
        }
    }
    
    /**
     * 检查是否应该显示奖罚模块（饼图两边的奖罚和完成度模块）
     */
    suspend fun shouldShowRewardPunishmentModule(categoryId: Int): Boolean {
        return shouldShowRewardPunishment(categoryId)
    }
} 