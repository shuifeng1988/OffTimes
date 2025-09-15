package com.offtime.app.ui.debug.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.offtime.app.data.dao.GoalRewardPunishmentUserDao
import com.offtime.app.data.dao.AppCategoryDao
import javax.inject.Inject

data class GoalDebugData(
    val catId: Int,
    val categoryName: String,
    val dailyGoalMin: Int,
    val conditionType: Int,
    val rewardContent: String,
    val punishmentContent: String
)

@HiltViewModel
class DebugGoalsViewModel @Inject constructor(
    private val goalRewardPunishmentUserDao: GoalRewardPunishmentUserDao,
    private val appCategoryDao: AppCategoryDao
) : ViewModel() {

    private val _goals = MutableStateFlow<List<GoalDebugData>>(emptyList())
    val goals: StateFlow<List<GoalDebugData>> = _goals.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadGoals() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val goalsList = goalRewardPunishmentUserDao.getAllUserGoalsList()
                val categories = appCategoryDao.getAllCategoriesList()
                val categoryMap = categories.associateBy { it.id }
                
                val goalsDebugData = goalsList.map { goal ->
                    GoalDebugData(
                        catId = goal.catId,
                        categoryName = categoryMap[goal.catId]?.name ?: "未知分类",
                        dailyGoalMin = goal.dailyGoalMin,
                        conditionType = goal.conditionType,
                        rewardContent = goal.rewardText,
                        punishmentContent = goal.punishText
                    )
                }
                
                _goals.value = goalsDebugData
                android.util.Log.d("DebugGoals", "加载了 ${goalsDebugData.size} 个目标配置")
            } catch (e: Exception) {
                android.util.Log.e("DebugGoals", "加载目标配置失败", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
} 