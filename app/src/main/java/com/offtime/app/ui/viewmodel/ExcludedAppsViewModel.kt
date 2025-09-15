package com.offtime.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offtime.app.data.entity.AppInfoEntity
import com.offtime.app.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExcludedAppsViewModel @Inject constructor(
    private val appRepository: AppRepository
) : ViewModel() {
    
    private val _excludedApps = MutableStateFlow<List<AppInfoEntity>>(emptyList())
    val excludedApps: StateFlow<List<AppInfoEntity>> = _excludedApps.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    /**
     * 加载所有排除统计的应用
     */
    fun loadExcludedApps() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val excludedAppsList = appRepository.getExcludedApps()
                _excludedApps.value = excludedAppsList.sortedWith(
                    compareBy<AppInfoEntity> { !it.isSystemApp } // 系统应用在前
                        .thenBy { it.appName } // 然后按名称排序
                )
                
                android.util.Log.d("ExcludedAppsViewModel", "加载到 ${excludedAppsList.size} 个排除应用")
                
            } catch (e: Exception) {
                android.util.Log.e("ExcludedAppsViewModel", "加载排除应用失败: ${e.message}", e)
                _excludedApps.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 恢复应用到统计中（取消排除状态）
     */
    fun restoreApp(packageName: String) {
        viewModelScope.launch {
            try {
                android.util.Log.d("ExcludedAppsViewModel", "恢复应用: $packageName")
                
                // 设置应用为不排除状态
                appRepository.updateAppExcludeStatus(packageName, false)
                
                android.util.Log.d("ExcludedAppsViewModel", "应用恢复成功: $packageName")
                
                // 重新加载数据
                loadExcludedApps()
                
            } catch (e: Exception) {
                android.util.Log.e("ExcludedAppsViewModel", "恢复应用失败: ${e.message}", e)
            }
        }
    }
    
    /**
     * 批量恢复多个应用
     */
    fun restoreApps(packageNames: List<String>) {
        viewModelScope.launch {
            try {
                android.util.Log.d("ExcludedAppsViewModel", "批量恢复 ${packageNames.size} 个应用")
                
                // 批量设置应用为不排除状态
                for (packageName in packageNames) {
                    appRepository.updateAppExcludeStatus(packageName, false)
                }
                
                android.util.Log.d("ExcludedAppsViewModel", "批量恢复完成: ${packageNames.size} 个应用")
                
                // 重新加载数据
                loadExcludedApps()
                
            } catch (e: Exception) {
                android.util.Log.e("ExcludedAppsViewModel", "批量恢复应用失败: ${e.message}", e)
            }
        }
    }
} 