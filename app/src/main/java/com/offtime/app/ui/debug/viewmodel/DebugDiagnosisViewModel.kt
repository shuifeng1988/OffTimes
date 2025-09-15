package com.offtime.app.ui.debug.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.offtime.app.data.dao.AppCategoryDao
import com.offtime.app.data.dao.DailyUsageDao
import com.offtime.app.data.dao.GoalRewardPunishmentUserDao
import com.offtime.app.data.dao.RewardPunishmentUserDao
import com.offtime.app.data.entity.RewardPunishmentUserEntity
import javax.inject.Inject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar

data class DiagnosisResult(
    val overallStatus: String = "未知",
    val detailedReport: String = "",
    val canFixYesterday: Boolean = false
)

@HiltViewModel
class DebugDiagnosisViewModel @Inject constructor(
    private val appCategoryDao: AppCategoryDao,
    private val dailyUsageDao: DailyUsageDao,
    private val goalRewardPunishmentUserDao: GoalRewardPunishmentUserDao,
    private val rewardPunishmentUserDao: RewardPunishmentUserDao
) : ViewModel() {

    private val _diagnosisResult = MutableStateFlow(DiagnosisResult())
    val diagnosisResult: StateFlow<DiagnosisResult> = _diagnosisResult.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun runDiagnosis() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val report = StringBuilder()
                var overallStatus = "正常"
                var canFixYesterday = false
                
                report.appendLine("=== 奖惩完成度折线图问题诊断 ===")
                report.appendLine("诊断时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
                report.appendLine()

                // 获取昨天和今天的日期
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

                report.appendLine("检查日期: 昨天($yesterday), 今天($today)")
                report.appendLine()

                // 1. 检查基础数据
                report.appendLine("【1. 基础数据检查】")
                val categories = appCategoryDao.getAllCategoriesList()
                val goals = goalRewardPunishmentUserDao.getAllUserGoalsList()
                
                report.appendLine("✓ 应用分类: ${categories.size}个")
                report.appendLine("✓ 目标配置: ${goals.size}个")
                
                if (categories.isEmpty() || goals.isEmpty()) {
                    overallStatus = "错误"
                    report.appendLine("❌ 基础数据缺失！")
                } else {
                    report.appendLine("✓ 基础数据正常")
                }
                report.appendLine()

                // 2. 检查昨天的使用数据
                report.appendLine("【2. 昨天使用数据检查】")
                var hasYesterdayUsage = false
                for (category in categories) {
                    val realUsage = dailyUsageDao.getTotalUsageByCategoryAndType(category.id, yesterday, 0) ?: 0
                    val virtualUsage = dailyUsageDao.getTotalUsageByCategoryAndType(category.id, yesterday, 1) ?: 0
                    val totalUsage = realUsage + virtualUsage
                    
                    if (totalUsage > 0) {
                        hasYesterdayUsage = true
                        report.appendLine("✓ ${category.name}: ${totalUsage}秒 (${totalUsage/60}分钟)")
                    } else {
                        report.appendLine("⚠ ${category.name}: 无使用数据")
                    }
                }
                
                if (!hasYesterdayUsage) {
                    overallStatus = "警告"
                    report.appendLine("⚠️ 昨天没有任何使用数据，这是正常的如果昨天确实没有使用应用")
                } else {
                    report.appendLine("✓ 昨天有使用数据")
                }
                report.appendLine()

                // 3. 检查奖惩记录表(新表)
                report.appendLine("【3. 奖惩记录表检查 (reward_punishment_user)】")
                val rewardRecords = rewardPunishmentUserDao.getAllRecords()
                val yesterdayRewardRecords = rewardRecords.filter { it.date == yesterday }
                
                report.appendLine("总记录数: ${rewardRecords.size}")
                report.appendLine("昨天记录数: ${yesterdayRewardRecords.size}")
                
                if (rewardRecords.isEmpty()) {
                    overallStatus = "错误"
                    report.appendLine("❌ 新表完全没有数据！这是奖惩完成度折线图没有数据的主要原因")
                } else if (yesterdayRewardRecords.isEmpty() && hasYesterdayUsage) {
                    overallStatus = "警告"
                    canFixYesterday = true
                    report.appendLine("⚠️ 昨天有使用数据但没有奖惩记录，可以修复")
                } else {
                    report.appendLine("✓ 新表有数据")
                    yesterdayRewardRecords.forEach { record ->
                        val categoryName = categories.find { it.id == record.catId }?.name ?: "未知"
                        report.appendLine("  $categoryName: 目标${if (record.isGoalMet == 1) "完成" else "未完成"}, 奖励${if (record.rewardDone == 1) "已完成" else "待处理"}, 惩罚${if (record.punishDone == 1) "已完成" else "待处理"}")
                    }
                }
                report.appendLine()

                // 4. 检查目标完成逻辑验证
                report.appendLine("【4. 目标完成逻辑验证】")
                if (hasYesterdayUsage) {
                    for (category in categories) {
                        val goal = goals.find { it.catId == category.id }
                        if (goal != null) {
                            val realUsage = dailyUsageDao.getTotalUsageByCategoryAndType(category.id, yesterday, 0) ?: 0
                            val virtualUsage = dailyUsageDao.getTotalUsageByCategoryAndType(category.id, yesterday, 1) ?: 0
                            val totalUsageSeconds = realUsage + virtualUsage
                            
                            if (totalUsageSeconds > 0) {
                                val targetSeconds = goal.dailyGoalMin * 60
                                val goalCompleted = when (goal.conditionType) {
                                    0 -> totalUsageSeconds <= targetSeconds // ≤目标算完成
                                    1 -> totalUsageSeconds >= targetSeconds // ≥目标算完成
                                    else -> false
                                }
                                
                                report.appendLine("${category.name}:")
                                report.appendLine("  使用时间: ${totalUsageSeconds}秒 (${totalUsageSeconds/60}分钟)")
                                report.appendLine("  目标时间: ${targetSeconds}秒 (${goal.dailyGoalMin}分钟)")
                                report.appendLine("  条件类型: ${goal.conditionType} (${if (goal.conditionType == 0) "≤目标算完成" else "≥目标算完成"})")
                                report.appendLine("  结果: ${if (goalCompleted) "✓ 目标完成" else "✗ 目标未完成"}")
                                report.appendLine()
                            }
                        }
                    }
                } else {
                    report.appendLine("昨天无使用数据，跳过目标完成逻辑验证")
                }

                // 5. 诊断结论
                report.appendLine("【5. 诊断结论】")
                when (overallStatus) {
                    "正常" -> {
                        report.appendLine("✅ 系统状态正常")
                        report.appendLine("如果奖惩完成度折线图仍然没有数据，请检查:")
                        report.appendLine("1. 是否点击过奖励/惩罚按钮完成操作")
                        report.appendLine("2. 图表的日期范围设置")
                        report.appendLine("3. 图表的数据查询逻辑")
                    }
                    "警告" -> {
                        report.appendLine("⚠️ 发现潜在问题")
                        if (canFixYesterday) {
                            report.appendLine("建议: 点击'修复昨天奖惩'按钮生成缺失的奖惩记录")
                        }
                        report.appendLine("建议: 检查数据生成和同步逻辑")
                    }
                    "错误" -> {
                        report.appendLine("❌ 发现严重问题")
                        report.appendLine("建议: 检查基础数据配置和奖惩记录生成逻辑")
                    }
                }

                _diagnosisResult.value = DiagnosisResult(
                    overallStatus = overallStatus,
                    detailedReport = report.toString(),
                    canFixYesterday = canFixYesterday
                )

            } catch (e: Exception) {
                _diagnosisResult.value = DiagnosisResult(
                    overallStatus = "错误",
                    detailedReport = "诊断过程中发生错误: ${e.message}",
                    canFixYesterday = false
                )
                android.util.Log.e("DebugDiagnosis", "诊断失败", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fixYesterdayRewards() {
        viewModelScope.launch {
            try {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                
                val categories = appCategoryDao.getAllCategoriesList()
                val goals = goalRewardPunishmentUserDao.getAllUserGoalsList()
                
                for (category in categories) {
                    val goal = goals.find { it.catId == category.id }
                    if (goal != null) {
                        // 检查是否已有记录
                        val existingRecord = rewardPunishmentUserDao.getRecordByCategoryAndDate(category.id, yesterday)
                        if (existingRecord == null) {
                            val realUsage = dailyUsageDao.getTotalUsageByCategoryAndType(category.id, yesterday, 0) ?: 0
                            val virtualUsage = dailyUsageDao.getTotalUsageByCategoryAndType(category.id, yesterday, 1) ?: 0
                            val totalUsageSeconds = realUsage + virtualUsage
                            
                            if (totalUsageSeconds > 0) {
                                val targetSeconds = goal.dailyGoalMin * 60
                                val goalCompleted = when (goal.conditionType) {
                                    0 -> totalUsageSeconds <= targetSeconds
                                    1 -> totalUsageSeconds >= targetSeconds
                                    else -> false
                                }
                                
                                val newRecord = RewardPunishmentUserEntity(
                                    id = "${yesterday}_${category.id}",
                                    date = yesterday,
                                    catId = category.id,
                                    isGoalMet = if (goalCompleted) 1 else 0,
                                    rewardDone = 0,
                                    punishDone = 0,
                                    updateTime = System.currentTimeMillis()
                                )
                                rewardPunishmentUserDao.upsert(newRecord)
                                
                                android.util.Log.d("DebugDiagnosis", "为${category.name}创建昨天奖惩记录")
                            }
                        }
                    }
                }
                
                // 重新运行诊断
                runDiagnosis()
                
            } catch (e: Exception) {
                android.util.Log.e("DebugDiagnosis", "修复昨天奖惩失败", e)
            }
        }
    }
} 