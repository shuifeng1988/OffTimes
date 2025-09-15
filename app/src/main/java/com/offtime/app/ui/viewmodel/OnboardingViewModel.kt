package com.offtime.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offtime.app.R
import com.offtime.app.data.repository.AppRepository
import com.offtime.app.data.repository.UserRepository
import com.offtime.app.data.repository.GoalRewardPunishmentRepository
import com.offtime.app.ui.screens.OnboardingStep
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appRepository: AppRepository,
    private val userRepository: UserRepository,
    private val goalRepository: GoalRewardPunishmentRepository
) : ViewModel() {

    private val _currentStep = MutableStateFlow(OnboardingStep.WELCOME)
    val currentStep: StateFlow<OnboardingStep> = _currentStep.asStateFlow()

    private val _isInitializing = MutableStateFlow(false)
    val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()

    private val _initProgress = MutableStateFlow(0f)
    val initProgress: StateFlow<Float> = _initProgress.asStateFlow()

    private val _initStatus = MutableStateFlow("")
    val initStatus: StateFlow<String> = _initStatus.asStateFlow()

    fun nextStep() {
        val nextStep = when (_currentStep.value) {
            OnboardingStep.WELCOME -> OnboardingStep.USAGE_PERMISSION
            OnboardingStep.USAGE_PERMISSION -> OnboardingStep.QUERY_PACKAGES_PERMISSION
            OnboardingStep.QUERY_PACKAGES_PERMISSION -> OnboardingStep.NOTIFICATION_PERMISSION
            OnboardingStep.NOTIFICATION_PERMISSION -> OnboardingStep.VIBRATE_PERMISSION
            OnboardingStep.VIBRATE_PERMISSION -> OnboardingStep.BATTERY_OPTIMIZATION
            OnboardingStep.BATTERY_OPTIMIZATION -> OnboardingStep.INITIALIZING
            OnboardingStep.INITIALIZING -> OnboardingStep.COMPLETED
            OnboardingStep.COMPLETED -> OnboardingStep.COMPLETED
        }
        _currentStep.value = nextStep
    }

    fun startInitialization() {
        viewModelScope.launch {
            _isInitializing.value = true
            _initProgress.value = 0f
            
            try {
                // 第一步：初始化用户数据
                _initStatus.value = context.getString(R.string.initialization_init_user_data)
                _initProgress.value = 0.1f
                appRepository.ensureUserDataInitialized()
                android.util.Log.d("OnboardingViewModel", "用户数据初始化完成")
                
                // 第二步：初始化分类数据
                _initStatus.value = context.getString(R.string.initialization_init_categories)
                _initProgress.value = 0.3f
                kotlinx.coroutines.delay(500)
                android.util.Log.d("OnboardingViewModel", "分类数据初始化完成")
                
                // 第三步：扫描并添加应用
                _initStatus.value = context.getString(R.string.initialization_scan_apps)
                _initProgress.value = 0.5f
                val initSuccess = appRepository.initializeAppData()
                if (initSuccess) {
                    android.util.Log.d("OnboardingViewModel", "应用数据初始化成功")
                } else {
                    android.util.Log.w("OnboardingViewModel", "应用数据初始化失败")
                }
                
                // 第四步：智能分类应用
                _initStatus.value = context.getString(R.string.initialization_smart_categorize)
                _initProgress.value = 0.6f
                val smartCategorizeSuccess = appRepository.smartCategorizeAllApps()
                if (smartCategorizeSuccess) {
                    android.util.Log.d("OnboardingViewModel", "智能分类完成")
                } else {
                    android.util.Log.w("OnboardingViewModel", "智能分类失败")
                }
                
                // 第五步：排除系统应用
                _initStatus.value = context.getString(R.string.initialization_exclude_system)
                _initProgress.value = 0.8f
                val systemAppsSuccess = appRepository.reclassifySystemAppsToExcludeStats()
                if (systemAppsSuccess) {
                    android.util.Log.d("OnboardingViewModel", "系统应用排除完成")
                } else {
                    android.util.Log.w("OnboardingViewModel", "系统应用排除失败")
                }
                
                // 第六步：初始化目标和奖罚数据
                _initStatus.value = context.getString(R.string.initialization_set_default_goals)
                _initProgress.value = 0.9f
                
                // 添加超时机制，防止无限等待
                val goalInitSuccess = try {
                    kotlinx.coroutines.withTimeout(30000) { // 30秒超时
                        goalRepository.ensureUserGoalDataInitialized()
                    }
                } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                    android.util.Log.e("OnboardingViewModel", "目标数据初始化超时")
                    false
                } catch (e: Exception) {
                    android.util.Log.e("OnboardingViewModel", "目标数据初始化异常: ${e.message}", e)
                    false
                }
                
                if (goalInitSuccess) {
                    android.util.Log.d("OnboardingViewModel", "目标数据初始化完成")
                } else {
                    android.util.Log.w("OnboardingViewModel", "目标数据初始化失败，但继续完成流程")
                }
                
                // 完成
                _initStatus.value = context.getString(R.string.initialization_completed)
                _initProgress.value = 1.0f
                kotlinx.coroutines.delay(500)
                
                _isInitializing.value = false
                nextStep() // 进入完成步骤
                
            } catch (e: Exception) {
                android.util.Log.e("OnboardingViewModel", "初始化过程出错: ${e.message}", e)
                _initStatus.value = "${context.getString(R.string.initialization_error_prefix)} ${e.message}"
                _isInitializing.value = false
            }
        }
    }
} 