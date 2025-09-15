package com.offtime.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offtime.app.service.DataRepairService
import com.offtime.app.utils.UsageDataValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 数据修复界面ViewModel
 */
@HiltViewModel
class DataRepairViewModel @Inject constructor(
    private val dataRepairService: DataRepairService,
    private val usageDataValidator: UsageDataValidator
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DataRepairUiState())
    val uiState: StateFlow<DataRepairUiState> = _uiState.asStateFlow()
    
    /**
     * 加载数据验证结果
     */
    fun loadValidationData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val reports = usageDataValidator.performFullValidation()
                _uiState.value = _uiState.value.copy(
                    validationReports = reports,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "加载数据失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 刷新验证数据
     */
    fun refreshValidation() {
        loadValidationData()
    }
    
    /**
     * 执行完整数据修复
     */
    fun performFullRepair() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRepairing = true)
            
            try {
                val result = dataRepairService.performCompleteDataRepair()
                _uiState.value = _uiState.value.copy(
                    repairResult = result,
                    repairReport = result.repairReport,
                    isRepairing = false
                )
                
                // 修复完成后重新加载验证数据
                loadValidationData()
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRepairing = false,
                    errorMessage = "修复失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 修复抖音重复问题
     */
    fun fixDouYinIssues() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRepairing = true)
            
            try {
                val fixedCount = dataRepairService.fixDouYinDuplicates()
                _uiState.value = _uiState.value.copy(
                    isRepairing = false,
                    repairReport = "抖音专项修复完成，修复了 $fixedCount 个问题"
                )
                
                // 修复完成后重新加载验证数据
                loadValidationData()
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRepairing = false,
                    errorMessage = "抖音修复失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 修复特定分类
     */
    fun fixSpecificCategory(categoryId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRepairing = true)
            
            try {
                val success = dataRepairService.repairSpecificCategory(categoryId)
                _uiState.value = _uiState.value.copy(
                    isRepairing = false,
                    repairReport = "分类修复${if (success) "成功" else "失败"}"
                )
                
                // 修复完成后重新加载验证数据
                loadValidationData()
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRepairing = false,
                    errorMessage = "分类修复失败: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 重新计算汇总数据
     */
    fun recalculateData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRepairing = true)
            
            try {
                val success = usageDataValidator.recalculateSummaryData(getCurrentDate())
                _uiState.value = _uiState.value.copy(
                    isRepairing = false,
                    repairReport = "汇总数据重新计算${if (success) "成功" else "失败"}"
                )
                
                // 重新计算后重新加载验证数据
                loadValidationData()
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRepairing = false,
                    errorMessage = "重新计算失败: ${e.message}"
                )
            }
        }
    }
    
    private fun getCurrentDate(): String {
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
    }
}

/**
 * 数据修复界面UI状态
 */
data class DataRepairUiState(
    val isLoading: Boolean = false,
    val isRepairing: Boolean = false,
    val validationReports: List<UsageDataValidator.ValidationReport> = emptyList(),
    val repairResult: DataRepairService.DataRepairResult? = null,
    val repairReport: String = "",
    val errorMessage: String? = null
) 