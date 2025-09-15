package com.offtime.app.ui.offlinetimer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offtime.app.data.repository.TimerSessionRepository
import com.offtime.app.data.entity.AppCategoryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
class OfflineTimerViewModel @Inject constructor(
    private val timerSessionRepository: TimerSessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OfflineTimerUiState())
    val uiState: StateFlow<OfflineTimerUiState> = _uiState.asStateFlow()

    private val _activeTimers = MutableStateFlow<Map<Int, TimerState>>(emptyMap())
    val activeTimers: StateFlow<Map<Int, TimerState>> = _activeTimers.asStateFlow()

    init {
        loadCategoriesWithDuration()
    }

    private fun loadCategoriesWithDuration() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            timerSessionRepository.getCategoriesWithTodayDuration()
                .catch { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "加载数据失败"
                    )
                }
                .collect { categoriesWithDuration ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        categoriesWithDuration = categoriesWithDuration,
                        error = null
                    )
                }
        }
    }

    fun startTimer(category: AppCategoryEntity) {
        viewModelScope.launch {
            try {
                val session = timerSessionRepository.startTimer(category.id)
                if (session != null) {
                    val currentTimers = _activeTimers.value.toMutableMap()
                    currentTimers[category.id] = TimerState(
                        sessionId = session.id,
                        categoryName = category.name,
                        startTime = session.startTime,
                        isRunning = true
                    )
                    _activeTimers.value = currentTimers
                    
                    _uiState.value = _uiState.value.copy(
                        message = "开始计时：${category.name}"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "启动计时失败"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "启动计时失败：${e.message}"
                )
            }
        }
    }

    fun stopTimer(catId: Int) {
        viewModelScope.launch {
            try {
                val timerState = _activeTimers.value[catId]
                if (timerState != null) {
                    val success = timerSessionRepository.stopTimer(timerState.sessionId)
                    if (success) {
                        val currentTimers = _activeTimers.value.toMutableMap()
                        currentTimers.remove(catId)
                        _activeTimers.value = currentTimers
                        
                        _uiState.value = _uiState.value.copy(
                            message = "停止计时：${timerState.categoryName}"
                        )
                        
                        // 等待聚合完成后刷新数据（聚合触发在UI层处理）
                        delay(3000) // 等待3秒让聚合完成
                        loadCategoriesWithDuration()
                    } else {
                        _uiState.value = _uiState.value.copy(
                            error = "停止计时失败"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "停止计时失败：${e.message}"
                )
            }
        }
    }

    fun isTimerRunning(catId: Int): Boolean {
        return _activeTimers.value[catId]?.isRunning == true
    }

    fun getTimerState(catId: Int): TimerState? {
        return _activeTimers.value[catId]
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // 更新计时状态（由UI调用，用于实时显示）
    fun updateTimerDuration(catId: Int, durationSec: Int) {
        val currentTimers = _activeTimers.value.toMutableMap()
        val timerState = currentTimers[catId]
        if (timerState != null) {
            currentTimers[catId] = timerState.copy(currentDurationSec = durationSec)
            _activeTimers.value = currentTimers
        }
    }

    // 格式化时长显示
    fun formatDuration(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format("%02d:%02d", minutes, secs)
        }
    }

}

data class OfflineTimerUiState(
    val isLoading: Boolean = false,
    val categoriesWithDuration: List<TimerSessionRepository.CategoryWithDuration> = emptyList(),
    val error: String? = null,
    val message: String? = null
)

data class TimerState(
    val sessionId: Int,
    val categoryName: String,
    val startTime: Long,
    val currentDurationSec: Int = 0,
    val isRunning: Boolean = false
) 