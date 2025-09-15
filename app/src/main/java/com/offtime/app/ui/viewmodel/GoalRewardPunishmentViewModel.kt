package com.offtime.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offtime.app.data.entity.AppCategoryEntity
import com.offtime.app.data.entity.GoalRewardPunishmentUserEntity
import com.offtime.app.data.repository.AppRepository
import com.offtime.app.data.repository.GoalRewardPunishmentRepository
// import com.offtime.app.data.repository.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GoalCategoryItem(
    val category: AppCategoryEntity,
    val goal: GoalRewardPunishmentUserEntity?
)

@HiltViewModel
class GoalRewardPunishmentViewModel @Inject constructor(
    private val goalRepository: GoalRewardPunishmentRepository,
    private val appRepository: AppRepository
) : ViewModel() {
    
    private val _goalCategoryItems = MutableStateFlow<List<GoalCategoryItem>>(emptyList())
    val goalCategoryItems: StateFlow<List<GoalCategoryItem>> = _goalCategoryItems.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // 设置相关状态
    private val _defaultCategoryId = MutableStateFlow(1)
    val defaultCategoryId: StateFlow<Int> = _defaultCategoryId.asStateFlow()
    
    private val _categoryRewardPunishmentEnabled = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val categoryRewardPunishmentEnabled: StateFlow<Map<Int, Boolean>> = _categoryRewardPunishmentEnabled.asStateFlow()
    
    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 确保目标数据已初始化
                goalRepository.ensureUserGoalDataInitialized()
                
                // 启动数据监听
                launch {
                    combine(
                        appRepository.getAllCategories(),
                        goalRepository.getAllUserGoals()
                    ) { categories, goals ->
                        android.util.Log.d("GoalViewModel", "组合数据 - 分类: ${categories.size}, 目标: ${goals.size}")
                        categories.map { category ->
                            val goal = goals.find { it.catId == category.id }
                            android.util.Log.d("GoalViewModel", "分类 ${category.name}(${category.id}) -> 目标: ${goal?.dailyGoalMin ?: "无"}")
                            GoalCategoryItem(category, goal)
                        }
                    }.collect { items ->
                        _goalCategoryItems.value = items
                        android.util.Log.d("GoalViewModel", "最终加载到 ${items.size} 个目标分类项")
                    }
                }
                
                // 加载设置数据
                launch {
                    _defaultCategoryId.value = appRepository.getDefaultCategoryId()
                    _categoryRewardPunishmentEnabled.value = appRepository.getCategoryRewardPunishmentEnabled()
                }
                
                // 等待一小段时间让数据加载完成
                delay(500)
                _isLoading.value = false
                
            } catch (e: Exception) {
                android.util.Log.e("GoalViewModel", "加载数据失败: ${e.message}", e)
                _isLoading.value = false
            }
        }
    }
    
    fun updateGoal(goal: GoalRewardPunishmentUserEntity) {
        viewModelScope.launch {
            try {
                goalRepository.updateUserGoal(goal.copy(updateTime = System.currentTimeMillis()))
                android.util.Log.d("GoalViewModel", "更新目标成功: catId=${goal.catId}")
            } catch (e: Exception) {
                android.util.Log.e("GoalViewModel", "更新目标失败: ${e.message}", e)
            }
        }
    }
    
    fun createGoalForCategory(catId: Int) {
        viewModelScope.launch {
            try {
                goalRepository.createDefaultGoalForCategory(catId)
                android.util.Log.d("GoalViewModel", "为分类 $catId 创建目标成功")
            } catch (e: Exception) {
                android.util.Log.e("GoalViewModel", "创建目标失败: ${e.message}", e)
            }
        }
    }
    
    fun getConditionDescription(conditionType: Int): String {
        return goalRepository.getConditionDescription(conditionType)
    }
    
    // 设置相关方法
    fun setDefaultCategory(categoryId: Int) {
        viewModelScope.launch {
            try {
                appRepository.setDefaultCategoryId(categoryId)
                _defaultCategoryId.value = categoryId
                android.util.Log.d("GoalViewModel", "设置默认分类成功: $categoryId")
            } catch (e: Exception) {
                android.util.Log.e("GoalViewModel", "设置默认分类失败: ${e.message}", e)
            }
        }
    }
    
    fun setCategoryRewardPunishmentEnabled(categoryId: Int, enabled: Boolean) {
        viewModelScope.launch {
            try {
                appRepository.setCategoryRewardPunishmentEnabled(categoryId, enabled)
                val currentMap = _categoryRewardPunishmentEnabled.value.toMutableMap()
                currentMap[categoryId] = enabled
                _categoryRewardPunishmentEnabled.value = currentMap
                android.util.Log.d("GoalViewModel", "设置分类奖罚开关成功: categoryId=$categoryId, enabled=$enabled")
            } catch (e: Exception) {
                android.util.Log.e("GoalViewModel", "设置分类奖罚开关失败: ${e.message}", e)
            }
        }
    }
    
    fun isCategoryRewardPunishmentEnabled(categoryId: Int): Boolean {
        return _categoryRewardPunishmentEnabled.value[categoryId] ?: true // 默认开启
    }
} 