package com.offtime.app.ui.debug.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.offtime.app.data.dao.DailyUsageDao
import com.offtime.app.data.dao.AppCategoryDao
import com.offtime.app.data.dao.SummaryUsageDao
import com.offtime.app.service.DataAggregationService
import android.content.Context
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar
import javax.inject.Inject

data class UsageDebugData(
    val catId: Int,
    val categoryName: String,
    val slotIndex: Int,
    val isOffline: Boolean,
    val durationSec: Int,
    val durationMin: Int,
    val date: String
)

// 按日期分组的数据
data class DailyUsageGroup(
    val date: String,
    val weekday: String,
    val usageItems: List<UsageDebugData>,
    val totalCount: Int,
    val totalMinutes: Int,
    val offlineCount: Int
)

@HiltViewModel
class DebugUsageViewModel @Inject constructor(
    private val dailyUsageDao: DailyUsageDao,
    private val appCategoryDao: AppCategoryDao,
    private val summaryUsageDao: SummaryUsageDao
) : ViewModel() {

    private val _dailyUsageGroups = MutableStateFlow<List<DailyUsageGroup>>(emptyList())
    val dailyUsageGroups: StateFlow<List<DailyUsageGroup>> = _dailyUsageGroups.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _diagnosisResult = MutableStateFlow<String?>(null)
    val diagnosisResult: StateFlow<String?> = _diagnosisResult.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())

    init {
        loadAllUsageData()
    }

    fun loadAllUsageData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val categories = appCategoryDao.getAllCategoriesList()
                
                // 获取过去30天的所有数据
                val calendar = Calendar.getInstance()
                val allUsageData = mutableListOf<UsageDebugData>()
                
                // 从今天开始往前30天
                for (dayOffset in 0..29) {
                    calendar.time = Date()
                    calendar.add(Calendar.DAY_OF_YEAR, -dayOffset)
                    val date = dateFormat.format(calendar.time)
                    
                    // 获取该日期所有分类的数据
                    for (category in categories) {
                        val onlineSlots = dailyUsageDao.getSlots(date, category.id, 0)
                        val offlineSlots = dailyUsageDao.getSlots(date, category.id, 1)
                        
                        onlineSlots.forEach { slot ->
                            allUsageData.add(
                                UsageDebugData(
                                    catId = slot.catId,
                                    categoryName = category.name,
                                    slotIndex = slot.slotIndex,
                                    isOffline = false,
                                    durationSec = slot.durationSec,
                                    durationMin = slot.durationSec / 60,
                                    date = date
                                )
                            )
                        }
                        
                        offlineSlots.forEach { slot ->
                            allUsageData.add(
                                UsageDebugData(
                                    catId = slot.catId,
                                    categoryName = category.name,
                                    slotIndex = slot.slotIndex,
                                    isOffline = true,
                                    durationSec = slot.durationSec,
                                    durationMin = slot.durationSec / 60,
                                    date = date
                                )
                            )
                        }
                    }
                }
                
                // 按日期分组数据
                val groupedData = allUsageData.groupBy { it.date }.map { (date, items) ->
                    val dateCalendar = Calendar.getInstance()
                    dateCalendar.time = dateFormat.parse(date) ?: Date()
                    val weekday = dayFormat.format(dateCalendar.time)
                    
                    // 每日内的数据按时间从晚到早排序（slotIndex从大到小）
                    val sortedItems = items.sortedWith(
                        compareByDescending<UsageDebugData> { it.slotIndex }
                            .thenBy { it.catId }
                            .thenBy { it.isOffline }
                    )
                    
                    DailyUsageGroup(
                        date = date,
                        weekday = weekday,
                        usageItems = sortedItems,
                        totalCount = items.size,
                        totalMinutes = items.sumOf { it.durationMin },
                        offlineCount = items.count { it.isOffline }
                    )
                }.filter { it.usageItems.isNotEmpty() } // 只显示有数据的日期
                
                // 按日期从新到旧排序
                _dailyUsageGroups.value = groupedData.sortedByDescending { it.date }
                
                android.util.Log.d("DebugUsage", "加载了 ${allUsageData.size} 条使用数据，覆盖 ${groupedData.size} 天")
            } catch (e: Exception) {
                android.util.Log.e("DebugUsage", "加载使用数据失败", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshData() {
        loadAllUsageData()
    }

    fun diagnoseTotalUsageData(context: Context) {
        viewModelScope.launch {
            try {
                android.util.Log.d("DebugUsage", "=== 开始诊断总使用数据 ===")
                
                // 1. 获取所有分类
                val categories = appCategoryDao.getAllCategoriesList()
                val totalUsageCategory = categories.find { it.name == "总使用" }
                
                if (totalUsageCategory == null) {
                    android.util.Log.e("DebugUsage", "未找到总使用分类")
                    return@launch
                }
                
                // 2. 获取有效分类
                val validCategories = categories.filter { 
                    it.id != totalUsageCategory.id && 
                    it.name != "总使用" && 
                    !it.name.contains("总使用") &&
                    !it.name.contains("排除统计")
                }
                android.util.Log.d("DebugUsage", "有效分类: ${validCategories.map { "${it.name}(${it.id})" }}")
                
                // 3. 检查本月每一天的summary数据
                val calendar = Calendar.getInstance()
                val daysInMonth = calendar.get(Calendar.DAY_OF_MONTH)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                
                var totalExpectedSeconds = 0L
                var totalActualSeconds = 0L
                
                for (i in 0 until daysInMonth) {
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                    
                    // 检查每个有效分类在该日期的数据
                    var dayExpectedSeconds = 0L
                    validCategories.forEach { category ->
                        val summaryRecord = summaryUsageDao.getSummaryUsageById("${date}_${category.id}")
                        val dailySeconds = summaryRecord?.totalSec?.toLong() ?: 0L
                        dayExpectedSeconds += dailySeconds
                        
                        if (dailySeconds > 0) {
                            android.util.Log.d("DebugUsage", "日期=$date, 分类=${category.name}, 秒数=$dailySeconds")
                        }
                    }
                    
                    // 检查总使用分类在该日期的实际数据
                    val totalUsageSummary = summaryUsageDao.getSummaryUsageById("${date}_${totalUsageCategory.id}")
                    val actualSeconds = totalUsageSummary?.totalSec?.toLong() ?: 0L
                    
                    totalExpectedSeconds += dayExpectedSeconds
                    totalActualSeconds += actualSeconds
                    
                    if (dayExpectedSeconds != actualSeconds) {
                        android.util.Log.w("DebugUsage", "数据不匹配 - 日期=$date, 预期=${dayExpectedSeconds}秒, 实际=${actualSeconds}秒")
                    }
                    
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }
                
                val expectedMinutes = (totalExpectedSeconds / 60).toInt()
                val actualMinutes = (totalActualSeconds / 60).toInt()
                val diffMinutes = expectedMinutes - actualMinutes
                
                // 构建诊断结果报告
                val resultReport = buildString {
                    appendLine("📊 本月总使用数据诊断报告")
                    appendLine()
                    appendLine("🔍 有效分类: ${validCategories.size}个")
                    validCategories.forEach { category ->
                        appendLine("  • ${category.name}(ID:${category.id})")
                    }
                    appendLine()
                    appendLine("📈 数据统计:")
                    appendLine("  预期总时长: ${expectedMinutes}分钟 (${String.format("%.1f", expectedMinutes/60.0)}小时)")
                    appendLine("  实际总时长: ${actualMinutes}分钟 (${String.format("%.1f", actualMinutes/60.0)}小时)")
                    appendLine("  数据差值: ${diffMinutes}分钟 (${String.format("%.1f", diffMinutes/60.0)}小时)")
                    appendLine()
                    if (diffMinutes == 0) {
                        appendLine("✅ 数据完全一致，无问题")
                    } else {
                        appendLine("⚠️ 数据不一致，已自动修复")
                        appendLine("📝 建议: 请等待数据重新聚合完成")
                    }
                }
                
                android.util.Log.d("DebugUsage", "=== 诊断结果 ===")
                android.util.Log.d("DebugUsage", "预期总时长: ${totalExpectedSeconds}秒 = ${expectedMinutes}分钟")
                android.util.Log.d("DebugUsage", "实际总时长: ${totalActualSeconds}秒 = ${actualMinutes}分钟")
                android.util.Log.d("DebugUsage", "差值: ${(totalExpectedSeconds - totalActualSeconds)}秒 = ${expectedMinutes - actualMinutes}分钟")
                
                // 设置诊断结果到UI状态
                _diagnosisResult.value = resultReport
                
                // 4. 重新触发数据聚合以修复可能的数据不一致
                if (diffMinutes != 0) {
                    android.util.Log.d("DebugUsage", "重新触发数据聚合...")
                    DataAggregationService.triggerAggregation(context)
                    kotlinx.coroutines.delay(3000)
                    loadAllUsageData()
                }
                
            } catch (e: Exception) {
                android.util.Log.e("DebugUsage", "诊断总使用数据失败", e)
                _diagnosisResult.value = "❌ 诊断失败: ${e.message}"
            }
        }
    }
    
    fun clearDiagnosisResult() {
        _diagnosisResult.value = null
    }

    fun cleanHistoricalData(context: Context) {
        viewModelScope.launch {
            try {
                android.util.Log.d("DebugUsage", "开始清理历史错误数据")
                
                // 触发清理历史数据服务
                DataAggregationService.cleanHistoricalData(context)
                
                // 清理后重新加载数据
                kotlinx.coroutines.delay(2000) // 等待清理完成
                loadAllUsageData()
                
                android.util.Log.d("DebugUsage", "历史错误数据清理完成")
                
            } catch (e: Exception) {
                android.util.Log.e("DebugUsage", "清理历史错误数据失败", e)
            }
        }
    }

    /**
     * 重新聚合所有历史数据
     * 修复被排除应用导致的数据不一致问题
     */
    fun reaggregateAllData(context: Context) {
        viewModelScope.launch {
            try {
                android.util.Log.d("DebugUsage", "开始重新聚合所有历史数据以修复被排除应用数据不一致")
                
                // 先清理所有汇总表数据
                summaryUsageDao.deleteAllSummaryData()
                android.util.Log.d("DebugUsage", "已清理所有汇总表数据")
                
                // 重新触发数据聚合（包含修复后的排除应用逻辑）
                DataAggregationService.triggerAggregation(context)
                android.util.Log.d("DebugUsage", "已触发数据重新聚合")
                
                // 等待聚合完成
                kotlinx.coroutines.delay(5000)
                
                // 重新加载数据以验证修复结果
                loadAllUsageData()
                
                // 自动诊断修复结果
                diagnoseTotalUsageData(context)
                
                android.util.Log.d("DebugUsage", "所有历史数据重新聚合完成")
                
            } catch (e: Exception) {
                android.util.Log.e("DebugUsage", "重新聚合所有历史数据失败", e)
                _diagnosisResult.value = "❌ 重新聚合失败: ${e.message}"
            }
        }
    }
} 