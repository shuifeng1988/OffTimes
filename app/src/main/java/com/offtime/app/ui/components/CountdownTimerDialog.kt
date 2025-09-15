package com.offtime.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.res.stringResource
import com.offtime.app.R

/**
 * 定时计时器对话框
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CountdownTimerDialog(
    showDialog: Boolean,
    isTimerRunning: Boolean,
    isTimerPaused: Boolean,
    hours: Int,
    minutes: Int,
    seconds: Int,
    initialMinutes: Int = 30,
    onTimerDurationChange: (minutes: Int) -> Unit,
    onStartTimer: () -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onStopTimer: () -> Unit,
    onBackgroundMode: () -> Unit,
    onTestSound: () -> Unit = {},
    onDismiss: () -> Unit
) {
    if (showDialog) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // 标题
                    Text(
                        text = stringResource(R.string.countdown_timer_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333)
                    )
                    
                    // 可编辑的计时显示区域
                    EditableCountdownDisplay(
                        hours = hours,
                        minutes = minutes,
                        seconds = seconds,
                        initialMinutes = initialMinutes,
                        isTimerRunning = isTimerRunning,
                        onTimeChange = { h, m, s ->
                            val totalMinutes = h * 60 + m + if (s > 0) 1 else 0
                            if (totalMinutes > 0) {
                                onTimerDurationChange(totalMinutes)
                            }
                        }
                    )
                    
                    // 控制按钮区域
                    if (!isTimerRunning) {
                        // 开始状态
                        Button(
                            onClick = onStartTimer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.start_countdown),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    } else {
                        // 计时状态 - 两个按钮
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // 暂停/继续按钮
                            Button(
                                onClick = if (isTimerPaused) onResumeTimer else onPauseTimer,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isTimerPaused) Color(0xFF4CAF50) else Color(0xFFFFC107)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (isTimerPaused) stringResource(R.string.resume_timer) else stringResource(R.string.pause_timer),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            }
                            
                            // 停止按钮
                            Button(
                                onClick = onStopTimer,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF44336)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.stop_timer),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    
                    // 测试提醒音按钮（仅在未运行时显示）
                    if (!isTimerRunning) {
                        Button(
                            onClick = onTestSound,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF9C27B0)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "🔊",
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = stringResource(R.string.test_sound),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    
                    // 底部按钮区域
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 取消按钮
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stringResource(R.string.cancel),
                                fontSize = 14.sp,
                                color = Color(0xFF666666)
                            )
                        }
                        
                        // 背景计时按钮（仅在计时器运行时显示）
                        if (isTimerRunning) {
                            Button(
                                onClick = onBackgroundMode,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2196F3)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.background_timer),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 可编辑的倒计时显示组件
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EditableCountdownDisplay(
    hours: Int,
    minutes: Int,
    seconds: Int,
    initialMinutes: Int,
    isTimerRunning: Boolean,
    onTimeChange: (hours: Int, minutes: Int, seconds: Int) -> Unit
) {
    // 计算显示值
    val displayHours = if (!isTimerRunning && hours == 0 && minutes == 0 && seconds == 0) {
        initialMinutes / 60
    } else {
        hours
    }
    val displayMinutes = if (!isTimerRunning && hours == 0 && minutes == 0 && seconds == 0) {
        initialMinutes % 60
    } else {
        minutes
    }
    val displaySeconds = if (!isTimerRunning && hours == 0 && minutes == 0 && seconds == 0) {
        0
    } else {
        seconds
    }
    
    // 内部编辑状态
    var editingHours by remember { mutableStateOf(displayHours) }
    var editingMinutes by remember { mutableStateOf(displayMinutes) }
    var editingSeconds by remember { mutableStateOf(displaySeconds) }
    
    // 当计时器运行时，使用实际值
    if (isTimerRunning) {
        editingHours = hours
        editingMinutes = minutes
        editingSeconds = seconds
    }
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 小时
        SimpleTimeUnit(
            value = editingHours,
            label = stringResource(R.string.time_unit_hour),
            maxValue = 23,
            isEditable = !isTimerRunning,
            onValueChange = { newValue ->
                editingHours = newValue
                onTimeChange(newValue, editingMinutes, editingSeconds)
            }
        )
        
        // 分隔符
        Text(
            text = ":",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFF5722)
        )
        
        // 分钟
        SimpleTimeUnit(
            value = editingMinutes,
            label = stringResource(R.string.time_unit_minute),
            maxValue = 59,
            isEditable = !isTimerRunning,
            onValueChange = { newValue ->
                editingMinutes = newValue
                onTimeChange(editingHours, newValue, editingSeconds)
            }
        )
        
        // 分隔符
        Text(
            text = ":",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFF5722)
        )
        
        // 秒
        SimpleTimeUnit(
            value = editingSeconds,
            label = stringResource(R.string.time_unit_second),
            maxValue = 59,
            isEditable = !isTimerRunning,
            onValueChange = { newValue ->
                editingSeconds = newValue
                onTimeChange(editingHours, editingMinutes, newValue)
            }
        )
    }
}

/**
 * 简化的时间单位组件
 */
@Composable
private fun SimpleTimeUnit(
    value: Int,
    label: String,
    maxValue: Int,
    isEditable: Boolean,
    onValueChange: (Int) -> Unit
) {
    var textValue by remember { mutableStateOf(String.format("%02d", value)) }
    var isEditing by remember { mutableStateOf(false) }
    
    // 当值改变时更新显示文本
    LaunchedEffect(value) {
        if (!isEditing) {
            textValue = String.format("%02d", value)
        }
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 数字背景块
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    color = Color(0xFFFF5722),
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { 
                    if (isEditable) {
                        isEditing = true
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (isEditable && isEditing) {
                BasicTextField(
                    value = textValue,
                    onValueChange = { newValue ->
                        // 限制输入为数字，最多两位
                        val filtered = newValue.filter { it.isDigit() }.take(2)
                        textValue = filtered
                        
                        // 如果输入有效，更新值
                        val intValue = filtered.toIntOrNull()
                        if (intValue != null && intValue <= maxValue) {
                            onValueChange(intValue)
                        }
                        
                        // 如果输入了两位数字，结束编辑
                        if (filtered.length == 2) {
                            isEditing = false
                            textValue = String.format("%02d", intValue?.coerceIn(0, maxValue) ?: 0)
                        }
                    },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            isEditing = false
                            val intValue = textValue.toIntOrNull()?.coerceIn(0, maxValue) ?: 0
                            textValue = String.format("%02d", intValue)
                            onValueChange(intValue)
                        }
                    )
                )
            } else {
                Text(
                    text = String.format("%02d", value),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        // 单位标签
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF999999),
            textAlign = TextAlign.Center
        )
    }
} 