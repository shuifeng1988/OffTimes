package com.offtime.app.ui.debug.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import com.offtime.app.data.dao.AppSessionUserDao
import com.offtime.app.data.dao.DailyUsageDao
import com.offtime.app.data.dao.TimerSessionUserDao
import com.offtime.app.data.dao.AppCategoryDao
import com.offtime.app.data.dao.SummaryUsageDao
import com.offtime.app.service.DataAggregationService
import kotlin.math.abs

@HiltViewModel
class DebugDataConsistencyViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appSessionUserDao: AppSessionUserDao,
    private val timerSessionUserDao: TimerSessionUserDao,
    private val dailyUsageDao: DailyUsageDao,
    private val summaryUsageDao: SummaryUsageDao,
    private val appCategoryDao: AppCategoryDao
) : ViewModel() {
    
    companion object {
        private const val TAG = "DebugDataConsistencyViewModel"
        private const val CONSISTENCY_THRESHOLD_MINUTES = 5
    }
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _consistencyReport = MutableStateFlow<DataConsistencyReport?>(null)
    val consistencyReport: StateFlow<DataConsistencyReport?> = _consistencyReport.asStateFlow()
    
    private val _lastCheckTime = MutableStateFlow("")
    val lastCheckTime: StateFlow<String> = _lastCheckTime.asStateFlow()
    
    private val _fixInProgress = MutableStateFlow(false)
    val fixInProgress: StateFlow<Boolean> = _fixInProgress.asStateFlow()
    
    data class DataConsistencyReport(
        val hasInconsistencies: Boolean,
        val datesChecked: Int,
        val categoriesChecked: Int,
        val inconsistentItems: List<InconsistentItem>,
        val checkTime: String
    )
    
    data class InconsistentItem(
        val date: String,
        val categoryId: Int,
        val categoryName: String,
        val rawMinutes: Int,
        val aggregatedMinutes: Int,
        val differenceMinutes: Int,
        val details: String
    )
    
    fun checkDataConsistency() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val report = performConsistencyCheck()
                _consistencyReport.value = report
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                _lastCheckTime.value = timestamp
            } catch (e: Exception) {
                android.util.Log.e(TAG, "数据一致性检查失败", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun fixDataInconsistencies() {
        viewModelScope.launch {
            _fixInProgress.value = true
            try {
                clearAggregatedData()
                reAggregateAllData()
                kotlinx.coroutines.delay(8000)
                val report = performConsistencyCheck()
                _consistencyReport.value = report
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                _lastCheckTime.value = timestamp
            } catch (e: Exception) {
                android.util.Log.e(TAG, "修复数据不一致问题失败", e)
            } finally {
                _fixInProgress.value = false
            }
        }
    }
    
    private suspend fun performConsistencyCheck(): DataConsistencyReport = withContext(Dispatchers.IO) {
        val inconsistentItems = mutableListOf<InconsistentItem>()
        val categories = appCategoryDao.getAllCategoriesList()
        val dates = getRecentDates(7)
        
        for (date in dates) {
            for (category in categories) {
                if (category.name == "总使用") continue
                
                try {
                    val inconsistency = checkCategoryDateConsistency(date, category.id, category.name)
                    if (inconsistency != null) {
                        inconsistentItems.add(inconsistency)
                    }
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "检查分类 ${category.name} 在 $date 的一致性失败", e)
                }
            }
        }
        
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        
        return@withContext DataConsistencyReport(
            hasInconsistencies = inconsistentItems.isNotEmpty(),
            datesChecked = dates.size,
            categoriesChecked = categories.size - 1,
            inconsistentItems = inconsistentItems,
            checkTime = timestamp
        )
    }
    
    private suspend fun checkCategoryDateConsistency(
        date: String, 
        categoryId: Int, 
        categoryName: String
    ): InconsistentItem? {
        
        val appSessions = appSessionUserDao.getSessionsByCatIdAndDate(categoryId, date)
        val timerSessions = timerSessionUserDao.getSessionsByDate(date).filter { it.catId == categoryId }
        
        val rawTotalSeconds = appSessions.sumOf { it.durationSec } + timerSessions.sumOf { it.durationSec }
        val rawMinutes = rawTotalSeconds / 60
        
        val realUsage = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, date, 0) ?: 0
        val virtualUsage = dailyUsageDao.getTotalUsageByCategoryAndType(categoryId, date, 1) ?: 0
        val aggregatedTotalSeconds = realUsage + virtualUsage
        val aggregatedMinutes = aggregatedTotalSeconds / 60
        
        val differenceMinutes = abs(rawMinutes - aggregatedMinutes)
        
        if (differenceMinutes > CONSISTENCY_THRESHOLD_MINUTES) {
            val details = buildString {
                append("原始APP会话: ${appSessions.size}个 (${appSessions.sumOf { it.durationSec } / 60}分钟)")
                if (timerSessions.isNotEmpty()) {
                    append(", 计时会话: ${timerSessions.size}个 (${timerSessions.sumOf { it.durationSec } / 60}分钟)")
                }
                append(", 聚合真实: ${realUsage / 60}分钟")
                if (virtualUsage > 0) {
                    append(", 聚合虚拟: ${virtualUsage / 60}分钟")
                }
            }
            
            return InconsistentItem(
                date = date,
                categoryId = categoryId,
                categoryName = categoryName,
                rawMinutes = rawMinutes,
                aggregatedMinutes = aggregatedMinutes,
                differenceMinutes = differenceMinutes,
                details = details
            )
        }
        
        return null
    }
    
    private suspend fun clearAggregatedData() = withContext(Dispatchers.IO) {
        dailyUsageDao.deleteAllDailyUsage()
        summaryUsageDao.deleteAllSummaryData()
    }
    
    private suspend fun reAggregateAllData() = withContext(Dispatchers.IO) {
        DataAggregationService.cleanHistoricalData(context)
        kotlinx.coroutines.delay(2000)
        DataAggregationService.triggerAggregation(context)
    }
    
    private fun getRecentDates(days: Int): List<String> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val dates = mutableListOf<String>()
        
        for (i in 0 until days) {
            dates.add(dateFormat.format(calendar.time))
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        
        return dates
    }
}
