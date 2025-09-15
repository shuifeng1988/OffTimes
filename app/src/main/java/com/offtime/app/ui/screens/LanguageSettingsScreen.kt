package com.offtime.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offtime.app.R
import com.offtime.app.utils.LocaleUtils
import com.offtime.app.widget.OffTimeLockScreenWidget
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LanguageSettingsViewModel @Inject constructor(
    private val localeUtils: LocaleUtils
) : ViewModel() {
    
    data class UiState(
        val currentLanguage: String = "auto",
        val needsRestart: Boolean = false
    )
    
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    init {
        loadCurrentLanguage()
    }
    
    private fun loadCurrentLanguage() {
        val currentLanguage = localeUtils.getCurrentLanguage()
        _uiState.value = _uiState.value.copy(currentLanguage = currentLanguage)
    }
    
    fun selectLanguage(languageCode: String, context: android.content.Context) {
        val oldLanguage = localeUtils.getCurrentLanguage()
        localeUtils.setLanguage(languageCode)
        
        // 清除UnifiedTextManager的语言缓存，确保新语言设置立即生效
        if (oldLanguage != languageCode) {
            com.offtime.app.utils.UnifiedTextManager.clearLanguageCache()
            com.offtime.app.utils.LanguageChangeListener.triggerLanguageChange()
            
            // 刷新所有Widget实例以应用新语言
            try {
                OffTimeLockScreenWidget.refreshAllWidgets(context)
                android.util.Log.d("LanguageSettingsViewModel", "已刷新Widget以应用新语言设置")
            } catch (e: Exception) {
                android.util.Log.w("LanguageSettingsViewModel", "刷新Widget失败: ${e.message}")
            }
            
            android.util.Log.d("LanguageSettingsViewModel", "应用内语言切换: $oldLanguage → $languageCode，已清理缓存")
        }
        
        _uiState.value = _uiState.value.copy(
            currentLanguage = languageCode,
            needsRestart = oldLanguage != languageCode
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSettingsScreen(
    onNavigateBack: () -> Unit = {},
    onRestartRequired: () -> Unit = {},
    viewModel: LanguageSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // 当需要重启时显示提示
    LaunchedEffect(uiState.needsRestart) {
        if (uiState.needsRestart) {
            onRestartRequired()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = context.getString(R.string.nav_settings) + " - " + 
                               context.getString(R.string.settings_language),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = context.getString(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // 说明卡片
            InfoCard()
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 语言选择列表
            LanguageSelectionList(
                currentLanguage = uiState.currentLanguage,
                onLanguageSelected = { languageCode ->
                    viewModel.selectLanguage(languageCode, context)
                }
            )
            
            // 重启提示
            if (uiState.needsRestart) {
                Spacer(modifier = Modifier.height(16.dp))
                RestartNotificationCard()
            }
        }
    }
}

@Composable
private fun InfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "语言设置 / Language Settings",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "选择应用显示语言。更改语言后需要重新启动应用才能完全生效。\n\n" +
                       "Select the app display language. Restart required for complete language change.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun LanguageSelectionList(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        LazyColumn {
            items(LocaleUtils.supportedLanguages) { language ->
                LanguageSelectionItem(
                    language = language,
                    isSelected = currentLanguage == language.code,
                    onSelected = { onLanguageSelected(language.code) }
                )
                
                if (language != LocaleUtils.supportedLanguages.last()) {
                    Divider()
                }
            }
        }
    }
}

@Composable
private fun LanguageSelectionItem(
    language: LocaleUtils.Language,
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onSelected
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = language.nameChinese,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Text(
                text = language.nameEnglish,
                fontSize = 14.sp,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun RestartNotificationCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "重启应用 / Restart Required",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "语言设置已保存，请重新启动应用以完全应用新的语言设置。\n\n" +
                       "Language settings saved. Please restart the app to fully apply the new language.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                lineHeight = 20.sp
            )
        }
    }
} 