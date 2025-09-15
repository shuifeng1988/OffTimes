package com.offtime.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.offtime.app.ui.viewmodel.WidgetSettingsViewModel
import androidx.compose.ui.res.stringResource
import com.offtime.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: WidgetSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCustomDaysDialog by remember { mutableStateOf(false) }
    var customDays by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadSettings()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部工具栏
        TopAppBar(
            title = { Text(stringResource(R.string.widget_settings_title)) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.action_back))
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.widget_display_days_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = stringResource(R.string.widget_display_days_description),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 预设选项
            val presetOptions = listOf(
                7 to stringResource(R.string.widget_one_week),
                15 to stringResource(R.string.widget_half_month),
                30 to stringResource(R.string.widget_one_month),
                90 to stringResource(R.string.widget_three_months),
                180 to stringResource(R.string.widget_half_year),
                365 to stringResource(R.string.widget_one_year)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.widget_preset_options),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    presetOptions.forEach { (days, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = uiState.widgetDisplayDays == days,
                                    onClick = {
                                        viewModel.updateDisplayDays(days)
                                    },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.widgetDisplayDays == days,
                                onClick = null // handled by selectable above
                            )
                            Text(
                                text = label,
                                modifier = Modifier.padding(start = 12.dp),
                                fontSize = 16.sp
                            )
                        }
                    }

                    // 自定义选项
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = !presetOptions.any { it.first == uiState.widgetDisplayDays },
                                onClick = {
                                    showCustomDaysDialog = true
                                },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = !presetOptions.any { it.first == uiState.widgetDisplayDays },
                            onClick = null
                        )
                        Text(
                            text = if (!presetOptions.any { it.first == uiState.widgetDisplayDays }) {
                                stringResource(R.string.widget_custom_option_with_days, uiState.widgetDisplayDays)
                            } else {
                                stringResource(R.string.widget_custom_option)
                            },
                            modifier = Modifier.padding(start = 12.dp),
                            fontSize = 16.sp
                        )
                    }
                }
            }

            // 说明信息
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.widget_usage_hint_title),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = stringResource(R.string.widget_usage_hint),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    // 自定义天数对话框
    if (showCustomDaysDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDaysDialog = false },
            title = { Text("自定义显示天数") },
            text = {
                Column {
                    Text(
                        text = "请输入要显示的天数（1-999）",
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    OutlinedTextField(
                        value = customDays,
                        onValueChange = { value ->
                            // 只允许输入数字
                            if (value.all { it.isDigit() } && value.length <= 3) {
                                customDays = value
                            }
                        },
                        label = { Text("天数") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val days = customDays.toIntOrNull()
                        if (days != null && days in 1..999) {
                            viewModel.updateDisplayDays(days)
                            showCustomDaysDialog = false
                            customDays = ""
                        }
                    },
                    enabled = customDays.toIntOrNull()?.let { it in 1..999 } == true
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCustomDaysDialog = false
                        customDays = ""
                    }
                ) {
                    Text("取消")
                }
            }
        )
    }
} 