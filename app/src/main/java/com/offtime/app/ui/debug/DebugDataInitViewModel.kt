package com.offtime.app.ui.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offtime.app.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DebugDataInitUiState(
    val isLoading: Boolean = false,
    val defaultCategoryCount: Int = 0,
    val userCategoryCount: Int = 0,
    val appCount: Int = 0,
    val logs: List<String> = emptyList()
)

@HiltViewModel
class DebugDataInitViewModel @Inject constructor(
    private val appRepository: AppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DebugDataInitUiState())
    val uiState: StateFlow<DebugDataInitUiState> = _uiState.asStateFlow()

    init {
        refreshStatus()
    }

    fun refreshStatus() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val defaultCount = appRepository.getAllDefaultCategoriesList().size
                val userCount = appRepository.getCategoryCount()
                val appCount = appRepository.getAppCount()
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    defaultCategoryCount = defaultCount,
                    userCategoryCount = userCount,
                    appCount = appCount
                )
                
                addLog("📊 状态刷新完成 - 默认分类:$defaultCount, 用户分类:$userCount, 应用:$appCount")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                addLog("❌ 状态刷新失败: ${e.message}")
            }
        }
    }

    fun forceInitializeData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            addLog("🚀 开始强制初始化数据...")
            
            try {
                val success = appRepository.debugForceInitializeAllData()
                
                if (success) {
                    addLog("✅ 数据初始化成功完成！")
                    // 刷新状态以显示最新数据
                    refreshStatus()
                } else {
                    addLog("❌ 数据初始化失败")
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            } catch (e: Exception) {
                addLog("❌ 数据初始化异常: ${e.message}")
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private fun addLog(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
        val logMessage = "[$timestamp] $message"
        
        _uiState.value = _uiState.value.copy(
            logs = (_uiState.value.logs + logMessage).takeLast(20) // 只保留最近20条日志
        )
    }
}
