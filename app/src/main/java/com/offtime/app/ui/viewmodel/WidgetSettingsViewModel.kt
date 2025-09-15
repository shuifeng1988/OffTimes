package com.offtime.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offtime.app.data.repository.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WidgetSettingsViewModel @Inject constructor(
    private val appSettingsRepository: AppSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WidgetSettingsUiState())
    val uiState: StateFlow<WidgetSettingsUiState> = _uiState.asStateFlow()

    fun loadSettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val currentDays = appSettingsRepository.getWidgetDisplayDays()
                _uiState.value = _uiState.value.copy(
                    widgetDisplayDays = currentDays,
                    isLoading = false
                )
                android.util.Log.d("WidgetSettingsViewModel", "当前Widget显示天数: $currentDays")
            } catch (e: Exception) {
                android.util.Log.e("WidgetSettingsViewModel", "加载设置失败: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "加载设置失败: ${e.message}"
                )
            }
        }
    }

    fun updateDisplayDays(days: Int) {
        viewModelScope.launch {
            try {
                appSettingsRepository.setWidgetDisplayDays(days)
                _uiState.value = _uiState.value.copy(widgetDisplayDays = days)
                android.util.Log.d("WidgetSettingsViewModel", "更新Widget显示天数为: $days")
            } catch (e: Exception) {
                android.util.Log.e("WidgetSettingsViewModel", "更新设置失败: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    error = "更新设置失败: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class WidgetSettingsUiState(
    val widgetDisplayDays: Int = 30,
    val isLoading: Boolean = false,
    val error: String? = null
) 