package com.offtime.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offtime.app.manager.TaskReminderManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskReminderUiState(
    val morningReminderEnabled: Boolean = true,
    val morningReminderHour: Int = 8,
    val morningReminderMinute: Int = 0,
    val eveningReminderEnabled: Boolean = true,
    val eveningReminderHour: Int = 20,
    val eveningReminderMinute: Int = 0
)

@HiltViewModel
class TaskReminderViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskReminderManager: TaskReminderManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TaskReminderUiState())
    val uiState: StateFlow<TaskReminderUiState> = _uiState.asStateFlow()
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            val settings = taskReminderManager.getSettings()
            _uiState.value = _uiState.value.copy(
                morningReminderEnabled = settings.morningReminderEnabled,
                morningReminderHour = settings.morningReminderHour,
                morningReminderMinute = settings.morningReminderMinute,
                eveningReminderEnabled = settings.eveningReminderEnabled,
                eveningReminderHour = settings.eveningReminderHour,
                eveningReminderMinute = settings.eveningReminderMinute
            )
        }
    }
    
    fun updateMorningReminderEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(morningReminderEnabled = enabled)
        viewModelScope.launch {
            taskReminderManager.updateMorningReminderEnabled(enabled)
            if (enabled) {
                taskReminderManager.scheduleMorningReminder()
            } else {
                taskReminderManager.cancelMorningReminder()
            }
        }
    }
    
    fun updateMorningReminderTime(hour: Int, minute: Int) {
        _uiState.value = _uiState.value.copy(
            morningReminderHour = hour,
            morningReminderMinute = minute
        )
        viewModelScope.launch {
            taskReminderManager.updateMorningReminderTime(hour, minute)
            if (_uiState.value.morningReminderEnabled) {
                taskReminderManager.scheduleMorningReminder()
            }
        }
    }
    
    fun updateEveningReminderEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(eveningReminderEnabled = enabled)
        viewModelScope.launch {
            taskReminderManager.updateEveningReminderEnabled(enabled)
            if (enabled) {
                taskReminderManager.scheduleEveningReminder()
            } else {
                taskReminderManager.cancelEveningReminder()
            }
        }
    }
    
    fun updateEveningReminderTime(hour: Int, minute: Int) {
        _uiState.value = _uiState.value.copy(
            eveningReminderHour = hour,
            eveningReminderMinute = minute
        )
        viewModelScope.launch {
            taskReminderManager.updateEveningReminderTime(hour, minute)
            if (_uiState.value.eveningReminderEnabled) {
                taskReminderManager.scheduleEveningReminder()
            }
        }
    }
} 