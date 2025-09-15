package com.offtime.app.ui.debug.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offtime.app.data.dao.TimerSessionUserDao
import com.offtime.app.data.dao.AppCategoryDao
import com.offtime.app.data.entity.TimerSessionUserEntity
import com.offtime.app.data.entity.AppCategoryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DebugTimerSessionsViewModel @Inject constructor(
    private val timerSessionUserDao: TimerSessionUserDao,
    private val appCategoryDao: AppCategoryDao
) : ViewModel() {
    
    private val _sessions = MutableStateFlow<List<TimerSessionUserEntity>>(emptyList())
    val sessions: StateFlow<List<TimerSessionUserEntity>> = _sessions.asStateFlow()
    
    private val _categories = MutableStateFlow<List<AppCategoryEntity>>(emptyList())
    val categories: StateFlow<List<AppCategoryEntity>> = _categories.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    fun loadSessions() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val sessionList = timerSessionUserDao.getAllUserSessionsList()
                _sessions.value = sessionList
                
                val categoryList = appCategoryDao.getAllCategoriesList()
                _categories.value = categoryList
                
                android.util.Log.d("DebugTimerSessionsViewModel", "加载活动数据: ${sessionList.size} 条")
                android.util.Log.d("DebugTimerSessionsViewModel", "加载分类数据: ${categoryList.size} 个")
            } catch (e: Exception) {
                android.util.Log.e("DebugTimerSessionsViewModel", "加载数据失败", e)
                _sessions.value = emptyList()
                _categories.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
} 