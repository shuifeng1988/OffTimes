package com.offtime.app.ui.screens

import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.offtime.app.ui.viewmodel.TaskReminderViewModel
import androidx.compose.ui.res.stringResource
import com.offtime.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskReminderScreen(
    onNavigateBack: () -> Unit,
    viewModel: TaskReminderViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 顶部应用栏
        TopAppBar(
            title = { 
                Text(
                    stringResource(R.string.task_reminder_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                ) 
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_button))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 页面标题和描述
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.NotificationsActive,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.smart_reminder_assistant),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.smart_reminder_subtitle),
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            
            // 提醒设置卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 20.dp)
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                            Text(
                            text = stringResource(R.string.reminder_settings),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                    // 早晨目标完成度提醒
                    ReminderSettingItem(
                        icon = Icons.Default.LightMode,
                        title = stringResource(R.string.morning_goal_reminder_title),
                        description = stringResource(R.string.morning_goal_reminder_desc),
                        isEnabled = uiState.morningReminderEnabled,
                        onToggle = { viewModel.updateMorningReminderEnabled(it) },
                        hour = uiState.morningReminderHour,
                        minute = uiState.morningReminderMinute,
                        onTimeClick = {
                                    showTimePickerDialog(
                                        context = context,
                                        currentHour = uiState.morningReminderHour,
                                        currentMinute = uiState.morningReminderMinute
                                    ) { hour, minute ->
                                        viewModel.updateMorningReminderTime(hour, minute)
                                    }
                        },
                        accentColor = Color(0xFFFFA726) // 橙色代表早晨
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // 晚间奖罚完成提醒
                    ReminderSettingItem(
                        icon = Icons.Default.DarkMode,
                        title = stringResource(R.string.evening_reward_reminder_title),
                        description = stringResource(R.string.evening_reward_reminder_desc),
                        isEnabled = uiState.eveningReminderEnabled,
                        onToggle = { viewModel.updateEveningReminderEnabled(it) },
                        hour = uiState.eveningReminderHour,
                        minute = uiState.eveningReminderMinute,
                        onTimeClick = {
                                    showTimePickerDialog(
                                        context = context,
                                        currentHour = uiState.eveningReminderHour,
                                        currentMinute = uiState.eveningReminderMinute
                                    ) { hour, minute ->
                                        viewModel.updateEveningReminderTime(hour, minute)
                                    }
                        },
                        accentColor = Color(0xFF5C6BC0) // 紫色代表晚间
                    )
                }
            }
            
            // 说明卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 16.dp)
                ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.reminder_explanation_title),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    val explanationItems = listOf(
                        stringResource(R.string.morning_reminder_explanation),
                        stringResource(R.string.evening_reminder_explanation),
                        stringResource(R.string.only_enabled_categories_reminded),
                        stringResource(R.string.early_open_app_reminder),
                        stringResource(R.string.completed_tasks_no_repeat),
                        stringResource(R.string.notification_permission_tip)
                    )
                    
                    explanationItems.forEach { item ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "•",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 8.dp, top = 2.dp)
                            )
                            Text(
                                text = item,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 22.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderSettingItem(
    icon: ImageVector,
    title: String,
    description: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    hour: Int,
    minute: Int,
    onTimeClick: () -> Unit,
    accentColor: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = accentColor
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = description,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }
            
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = accentColor,
                    checkedTrackColor = accentColor.copy(alpha = 0.5f)
                )
            )
        }
        
        if (isEnabled) {
            Spacer(modifier = Modifier.height(12.dp))
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = accentColor.copy(alpha = 0.08f)
                ),
                onClick = onTimeClick
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.reminder_time),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = accentColor
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = String.format("%02d:%02d", hour, minute),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }
                }
            }
        }
    }
}

private fun showTimePickerDialog(
    context: Context,
    currentHour: Int,
    currentMinute: Int,
    onTimeSelected: (Int, Int) -> Unit
) {
    TimePickerDialog(
        context,
        { _, hour, minute ->
            onTimeSelected(hour, minute)
        },
        currentHour,
        currentMinute,
        true
    ).show()
}
