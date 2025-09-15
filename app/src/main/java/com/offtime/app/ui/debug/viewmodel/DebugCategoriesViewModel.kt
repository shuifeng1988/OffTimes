package com.offtime.app.ui.debug.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.offtime.app.data.dao.AppCategoryDao
import com.offtime.app.data.entity.AppCategoryEntity
import javax.inject.Inject

@HiltViewModel
class DebugCategoriesViewModel @Inject constructor(
    private val appCategoryDao: AppCategoryDao
) : ViewModel() {

    private val _categories = MutableStateFlow<List<AppCategoryEntity>>(emptyList())
    val categories: StateFlow<List<AppCategoryEntity>> = _categories.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadCategories() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val categoriesList = appCategoryDao.getAllCategoriesList()
                _categories.value = categoriesList
                android.util.Log.d("DebugCategories", "加载了 ${categoriesList.size} 个分类")
            } catch (e: Exception) {
                android.util.Log.e("DebugCategories", "加载分类失败", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
} 