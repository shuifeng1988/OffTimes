package com.offtime.app.ui.debug.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offtime.app.data.dao.RewardPunishmentUserDao
import com.offtime.app.data.entity.RewardPunishmentUserEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DebugRewardsViewModel @Inject constructor(
    private val rewardPunishmentUserDao: RewardPunishmentUserDao
) : ViewModel() {
    
    private val _rewards = MutableStateFlow<List<RewardPunishmentUserEntity>>(emptyList())
    val rewards: StateFlow<List<RewardPunishmentUserEntity>> = _rewards.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    fun loadRewards() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val rewardList = rewardPunishmentUserDao.getAllRecords()
                _rewards.value = rewardList
            } catch (e: Exception) {
                // Handle error - could add error state if needed
                _rewards.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
} 