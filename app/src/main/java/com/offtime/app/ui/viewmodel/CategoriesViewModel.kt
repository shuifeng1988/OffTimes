package com.offtime.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offtime.app.data.entity.AppCategoryEntity
import com.offtime.app.data.entity.AppInfoEntity
import com.offtime.app.data.repository.AppRepository
import com.offtime.app.data.repository.GoalRewardPunishmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val goalRepository: GoalRewardPunishmentRepository
) : ViewModel() {
    
    private val _categories = MutableStateFlow<List<AppCategoryEntity>>(emptyList())
    val categories: StateFlow<List<AppCategoryEntity>> = _categories.asStateFlow()
    
    private val _apps = MutableStateFlow<List<AppInfoEntity>>(emptyList())
    val apps: StateFlow<List<AppInfoEntity>> = _apps.asStateFlow()
    
    private val _allApps = MutableStateFlow<List<AppInfoEntity>>(emptyList())
    private val _filteredApps = MutableStateFlow<List<AppInfoEntity>>(emptyList())
    val filteredApps: StateFlow<List<AppInfoEntity>> = _filteredApps.asStateFlow()
    
    private val _selectedCategory = MutableStateFlow<AppCategoryEntity?>(null)
    val selectedCategory: StateFlow<AppCategoryEntity?> = _selectedCategory.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()
    
    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 首先确保用户数据已初始化
                appRepository.ensureUserDataInitialized()
                // 确保目标数据已初始化
                goalRepository.ensureUserGoalDataInitialized()
                // 注释掉自动修复应用分类 - 保护用户设置
                // appRepository.fixExistingAppCategories()
                
                // 检查数据是否存在
                val categoryCount = appRepository.getCategoryCount()
                val appCount = appRepository.getAppCount()
                
                android.util.Log.d("CategoriesViewModel", "数据统计 - 分类: $categoryCount, 应用: $appCount")
                
                // 如果没有应用数据或数据很少，尝试初始化
                if (appCount <= 1) { // 允许只有1个应用（OffTime）的情况下重新初始化
                    android.util.Log.d("CategoriesViewModel", "应用数据不足，尝试初始化应用数据")
                    val initSuccess = appRepository.initializeAppData()
                    if (!initSuccess) {
                        android.util.Log.w("CategoriesViewModel", "应用数据初始化失败")
                    }
                }
                
                // 加载分类和应用数据
                launch {
                    appRepository.getAllCategories().collect { categoryList ->
                        _categories.value = categoryList
                        android.util.Log.d("CategoriesViewModel", "加载到 ${categoryList.size} 个分类")
                        // 默认选择第一个分类
                        if (_selectedCategory.value == null && categoryList.isNotEmpty()) {
                            _selectedCategory.value = categoryList.first()
                        }
                    }
                }
                
                launch {
                    appRepository.getAllApps().collect { appList ->
                        _apps.value = appList
                        _allApps.value = appList
                        android.util.Log.d("CategoriesViewModel", "加载到 ${appList.size} 个应用")
                        updateFilteredApps()
                    }
                }
                
                _isLoading.value = false
            } catch (e: Exception) {
                android.util.Log.e("CategoriesViewModel", "加载数据失败: ${e.message}", e)
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 强制刷新应用数据
     */
    fun forceRefreshAppData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                android.util.Log.d("CategoriesViewModel", "开始强制刷新应用数据")
                
                // 获取当前数据库中的应用数量
                val currentAppCount = appRepository.getAppCount()
                android.util.Log.d("CategoriesViewModel", "刷新前数据库中有 $currentAppCount 个应用")
                
                // 使用安全的初始化方法，保护用户设置
                val initSuccess = appRepository.initializeAppData()
                if (initSuccess) {
                    val newAppCount = appRepository.getAppCount()
                    android.util.Log.d("CategoriesViewModel", "应用数据刷新成功，现在有 $newAppCount 个应用")
                } else {
                    android.util.Log.w("CategoriesViewModel", "应用数据刷新失败")
                }
                
                _isLoading.value = false
            } catch (e: Exception) {
                android.util.Log.e("CategoriesViewModel", "强制刷新失败: ${e.message}", e)
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 重分类系统应用到排除统计分类
     */
    fun reclassifySystemApps() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                android.util.Log.d("CategoriesViewModel", "开始重分类系统应用")
                
                val success = appRepository.reclassifySystemAppsToExcludeStats()
                if (success) {
                    android.util.Log.d("CategoriesViewModel", "系统应用重分类成功")
                } else {
                    android.util.Log.w("CategoriesViewModel", "系统应用重分类失败")
                }
                
                _isLoading.value = false
            } catch (e: Exception) {
                android.util.Log.e("CategoriesViewModel", "重分类系统应用失败: ${e.message}", e)
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 智能分类所有应用
     * 根据应用包名和特征，对所有应用进行智能重新分类
     */
    fun smartCategorizeAllApps() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                android.util.Log.d("CategoriesViewModel", "开始智能分类所有应用")
                
                val success = appRepository.smartCategorizeAllApps()
                if (success) {
                    android.util.Log.d("CategoriesViewModel", "智能分类完成")
                } else {
                    android.util.Log.w("CategoriesViewModel", "智能分类失败")
                }
                
                _isLoading.value = false
            } catch (e: Exception) {
                android.util.Log.e("CategoriesViewModel", "智能分类失败: ${e.message}", e)
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 手动排除应用（设置排除状态）
     */
    fun excludeApp(packageName: String) {
        viewModelScope.launch {
            try {
                android.util.Log.d("CategoriesViewModel", "手动排除应用: $packageName")
                
                // 设置应用为排除状态
                appRepository.updateAppExcludeStatus(packageName, true)
                android.util.Log.d("CategoriesViewModel", "应用排除成功: $packageName")
                // 刷新数据
                loadData()
            } catch (e: Exception) {
                android.util.Log.e("CategoriesViewModel", "排除应用失败: ${e.message}", e)
            }
        }
    }
    
    /**
     * 批量排除多个应用
     */
    fun excludeApps(packageNames: List<String>) {
        viewModelScope.launch {
            try {
                android.util.Log.d("CategoriesViewModel", "批量排除 ${packageNames.size} 个应用")
                
                // 批量设置应用为排除状态
                for (packageName in packageNames) {
                    appRepository.updateAppExcludeStatus(packageName, true)
                }
                
                android.util.Log.d("CategoriesViewModel", "批量排除完成: ${packageNames.size} 个应用")
                // 刷新数据
                loadData()
            } catch (e: Exception) {
                android.util.Log.e("CategoriesViewModel", "批量排除应用失败: ${e.message}", e)
            }
        }
    }
    
    fun selectCategory(category: AppCategoryEntity) {
        _selectedCategory.value = category
        updateFilteredApps()
    }
    
    fun toggleEditMode() {
        _isEditMode.value = !_isEditMode.value
        updateFilteredApps()
    }
    
    private fun updateFilteredApps() {
        val selectedCat = _selectedCategory.value
        val allApps = _allApps.value
        
        if (selectedCat == null) {
            _filteredApps.value = if (_isEditMode.value) allApps else emptyList()
            return
        }
        
        val filtered = if (_isEditMode.value) {
            // 编辑模式：显示所有应用
            allApps
        } else {
            when (selectedCat.name) {
                "总使用" -> {
                    // 总使用分类：显示所有非排除的应用
                    allApps.filter { !it.isExcluded }
                }
                else -> {
                    // 其他分类：只显示该分类的非排除应用
                    allApps.filter { it.categoryId == selectedCat.id && !it.isExcluded }
                }
            }
        }
        
        _filteredApps.value = filtered
        android.util.Log.d("CategoriesViewModel", "过滤应用: 选中分类=${selectedCat.name}, 显示${filtered.size}个应用")
    }
    

    
    fun updateAppCategory(packageName: String, newCategoryId: Int) {
        viewModelScope.launch {
            try {
                appRepository.updateAppCategory(packageName, newCategoryId)
                // 数据会通过Flow自动更新
            } catch (e: Exception) {
                android.util.Log.e("CategoriesViewModel", "更新应用分类失败: ${e.message}")
            }
        }
    }
    
    /**
     * 为新分类创建默认目标
     */
    fun createGoalForNewCategory(categoryId: Int) {
        viewModelScope.launch {
            try {
                goalRepository.createDefaultGoalForCategory(categoryId)
                android.util.Log.d("CategoriesViewModel", "为新分类 $categoryId 创建默认目标")
            } catch (e: Exception) {
                android.util.Log.e("CategoriesViewModel", "为新分类创建目标失败: ${e.message}", e)
            }
        }
    }
} 