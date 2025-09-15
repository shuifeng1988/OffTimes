package com.offtime.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offtime.app.data.repository.UserRepository
import com.offtime.app.data.entity.UserEntity
import com.offtime.app.data.entity.BackupSettingsEntity
import com.offtime.app.data.dao.BackupSettingsDao
import com.offtime.app.manager.SubscriptionManager
import com.offtime.app.manager.BackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val subscriptionManager: SubscriptionManager,
    private val backupSettingsDao: BackupSettingsDao,
    private val backupManager: BackupManager
) : ViewModel() {
    
    // 当前用户信息
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()
    
    // 订阅信息
    private val _subscriptionInfo = MutableStateFlow<SubscriptionManager.SubscriptionInfo?>(null)
    val subscriptionInfo: StateFlow<SubscriptionManager.SubscriptionInfo?> = _subscriptionInfo.asStateFlow()
    
    // 备份设置
    private val _backupSettings = MutableStateFlow<BackupSettingsEntity?>(null)
    val backupSettings: StateFlow<BackupSettingsEntity?> = _backupSettings.asStateFlow()
    
    // 备份信息
    private val _backupInfo = MutableStateFlow<BackupManager.BackupInfo?>(null)
    val backupInfo: StateFlow<BackupManager.BackupInfo?> = _backupInfo.asStateFlow()
    
    // 备份加载状态
    private val _backupLoading = MutableStateFlow(false)
    val backupLoading: StateFlow<Boolean> = _backupLoading.asStateFlow()
    
    // UI状态
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadData()
    }
    
    private fun loadData() {
        viewModelScope.launch {
            // 加载用户信息
            userRepository.getCurrentUserFlow().collect { user ->
                _currentUser.value = user
                // 当用户改变时，重新加载备份设置
                if (user != null) {
                    loadBackupSettings()
                }
            }
        }
        
        viewModelScope.launch {
            // 加载订阅信息
            try {
                val info = subscriptionManager.getSubscriptionInfo()
                _subscriptionInfo.value = info
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "加载订阅信息失败: ${e.message}"
                )
            }
        }
    }
    
    private fun loadBackupSettings() {
        viewModelScope.launch {
            try {
                backupSettingsDao.getBackupSettingsFlow().collect { settings ->
                    _backupSettings.value = settings
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "加载备份设置失败: ${e.message}"
                )
            }
        }
    }
    
    fun updateBackupSettings(settings: BackupSettingsEntity) {
        viewModelScope.launch {
            try {
                backupSettingsDao.upsertBackupSettings(settings)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "更新备份设置失败: ${e.message}"
                )
            }
        }
    }
    
    fun performManualBackup() {
        viewModelScope.launch {
            try {
                _backupLoading.value = true
                val result = backupManager.performManualBackup()
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        error = "备份成功完成"
                    )
                    // 刷新备份信息
                    loadBackupInfo()
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "备份失败: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "备份失败: ${e.message}"
                )
            } finally {
                _backupLoading.value = false
            }
        }
    }
    
    fun restoreBackupData() {
        viewModelScope.launch {
            try {
                _backupLoading.value = true
                val result = backupManager.restoreFromBackup()
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        error = "数据恢复成功"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "数据恢复失败: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "数据恢复失败: ${e.message}"
                )
            } finally {
                _backupLoading.value = false
            }
        }
    }
    
    private fun loadBackupInfo() {
        viewModelScope.launch {
            try {
                val info = backupManager.getBackupInfo()
                _backupInfo.value = info
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "加载备份信息失败: ${e.message}"
                )
            }
        }
    }
    
    fun refreshSubscriptionInfo() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                val info = subscriptionManager.getSubscriptionInfo()
                _subscriptionInfo.value = info
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "刷新订阅信息失败: ${e.message}"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                userRepository.logout()
                _currentUser.value = null
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "退出登录失败: ${e.message}"
                )
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    data class SettingsUiState(
        val isLoading: Boolean = false,
        val error: String? = null
    )
}