package com.offtime.app.ui.components

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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.offtime.app.R

/**
 * 线下计时器对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineTimerDialog(
    showDialog: Boolean,
    isTimerRunning: Boolean,
    isTimerPaused: Boolean,
    hours: Int,
    minutes: Int,
    seconds: Int,
    onStartTimer: () -> Unit,
    onPauseTimer: () -> Unit,
    onResumeTimer: () -> Unit,
    onStopTimer: () -> Unit,
    onBackgroundMode: () -> Unit,
    onDismiss: () -> Unit,
    // CountDown相关状态和回调
    isCountdownRunning: Boolean = false,
    isCountdownPaused: Boolean = false,
    countdownHours: Int = 0,
    countdownMinutes: Int = 30,
    countdownSeconds: Int = 0,
    onStartCountdown: ((initialMinutes: Int) -> Unit)? = null,
    onPauseCountdown: () -> Unit = {},
    onResumeCountdown: () -> Unit = {},
    onStopCountdown: () -> Unit = {},
    onCountdownBackgroundMode: () -> Unit = {},
    defaultTab: Int = 0 // 0: Timer, 1: CountDown
) {
    // Tab状态管理
    var selectedTab by remember { mutableStateOf(defaultTab) } // 0: Timer, 1: CountDown
    
    // 当defaultTab改变时，更新selectedTab
    LaunchedEffect(defaultTab) {
        selectedTab = defaultTab
    }
    
    // CountDown时间编辑状态（仅用于未运行时的编辑）
    var editCountdownHours by remember { mutableStateOf(0) }
    var editCountdownMinutes by remember { mutableStateOf(30) }
    var editCountdownSeconds by remember { mutableStateOf(0) }
    
    // CountDown时间编辑状态 - 使用直接编辑方式
    if (showDialog) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false // 允许全宽度，可能改善焦点行为
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
                    // Tab按钮行 - 替换原来的标题
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Timer Tab
                        DialogTabButton(
                            text = stringResource(R.string.timer_label),
                            icon = "⏱️",
                            isSelected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            modifier = Modifier.weight(1f),
                            allowTwoLines = true
                        )
                        
                        // CountDown Tab
                        DialogTabButton(
                            text = stringResource(R.string.countdown_label),
                            icon = "⏰",
                            isSelected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            modifier = Modifier.weight(1f),
                            allowTwoLines = true
                        )
                    }
                    
                    // 根据选中的Tab显示不同内容
                    when (selectedTab) {
                        0 -> {
                            // Timer界面
                            TimerDisplay(
                                hours = hours,
                                minutes = minutes,
                                seconds = seconds
                            )
                            
                            // Timer控制按钮区域
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
                                        text = stringResource(R.string.start_timer),
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
                        }
                        1 -> {
                            // CountDown界面 - 使用直接编辑方式
                            DirectEditCountdownDisplay(
                                hours = if (isCountdownRunning) countdownHours else editCountdownHours,
                                minutes = if (isCountdownRunning) countdownMinutes else editCountdownMinutes,
                                seconds = if (isCountdownRunning) countdownSeconds else editCountdownSeconds,
                                isCountdownRunning = isCountdownRunning,
                                onTimeChange = { h, m, s ->
                                    if (!isCountdownRunning) {
                                        editCountdownHours = h
                                        editCountdownMinutes = m
                                        editCountdownSeconds = s
                                    }
                                }
                            )
                            
                            // CountDown控制按钮区域
                            if (!isCountdownRunning) {
                                // 开始状态
                                Button(
                                    onClick = { 
                                        // 调用HomeViewModel开始CountDown并记录到数据库
                                        val totalMinutes = editCountdownHours * 60 + editCountdownMinutes + if (editCountdownSeconds > 0) 1 else 0
                                        onStartCountdown?.invoke(totalMinutes)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFFF5722)
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
                                // 倒计时状态 - 两个按钮
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // 暂停/继续按钮
                                    Button(
                                        onClick = { 
                                            if (isCountdownPaused) {
                                                onResumeCountdown()
                                            } else {
                                                onPauseCountdown()
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isCountdownPaused) Color(0xFF4CAF50) else Color(0xFFFFC107)
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = if (isCountdownPaused) stringResource(R.string.resume_timer) else stringResource(R.string.pause_timer),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White
                                        )
                                    }
                                    
                                    // 停止按钮
                                    Button(
                                        onClick = { 
                                            onStopCountdown()
                                        },
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
                        
                        // 背景计时按钮
                        if ((selectedTab == 0 && isTimerRunning) || (selectedTab == 1 && isCountdownRunning)) {
                            Button(
                                onClick = if (selectedTab == 0) onBackgroundMode else onCountdownBackgroundMode,
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
 * 计时器数字显示组件
 */
@Composable
private fun TimerDisplay(
    hours: Int,
    minutes: Int,
    seconds: Int
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 小时
        TimeUnit(
            value = hours,
            label = stringResource(R.string.time_unit_hour)
        )
        
        // 分隔符
        Text(
            text = ":",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF5A7DF5)
        )
        
        // 分钟
        TimeUnit(
            value = minutes,
            label = stringResource(R.string.time_unit_minute)
        )
        
        // 分隔符
        Text(
            text = ":",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF5A7DF5)
        )
        
        // 秒
        TimeUnit(
            value = seconds,
            label = stringResource(R.string.time_unit_second)
        )
    }
}

/**
 * 时间单位显示组件
 */
@Composable
private fun TimeUnit(
    value: Int,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 数字背景块
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    color = Color(0xFF5A7DF5),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = String.format("%02d", value),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
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

/**
 * 对话框Tab按钮组件
 */
@Composable
private fun DialogTabButton(
    text: String,
    icon: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    allowTwoLines: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(if (allowTwoLines) 48.dp else 40.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) {
                Color(0xFF4CAF50)
            } else {
                Color(0xFFF5F5F5)
            }
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (isSelected) 2.dp else 0.dp
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        if (allowTwoLines) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = icon,
                    fontSize = 14.sp
                )
                Text(
                    text = text,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else Color(0xFF666666),
                    maxLines = 1
                )
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = icon,
                    fontSize = 14.sp
                )
                Text(
                    text = text,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else Color(0xFF666666)
                )
            }
        }
    }
}

/**
 * 直接编辑的CountDown显示组件（移植自备份项目）
 */
@Composable
private fun DirectEditCountdownDisplay(
    hours: Int,
    minutes: Int,
    seconds: Int,
    isCountdownRunning: Boolean,
    onTimeChange: (hours: Int, minutes: Int, seconds: Int) -> Unit
) {
    // 内部编辑状态
    var editingHours by remember { mutableStateOf(hours) }
    var editingMinutes by remember { mutableStateOf(minutes) }
    var editingSeconds by remember { mutableStateOf(seconds) }
    
    // 当计时器运行时，使用实际值
    if (isCountdownRunning) {
        editingHours = hours
        editingMinutes = minutes
        editingSeconds = seconds
    }
    
    // 当外部值改变时，更新内部编辑状态（仅在不运行时）
    LaunchedEffect(hours, minutes, seconds) {
        if (!isCountdownRunning) {
            editingHours = hours
            editingMinutes = minutes  
            editingSeconds = seconds
        }
    }
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 小时
        DirectEditTimeUnit(
            value = editingHours,
            label = stringResource(R.string.time_unit_hour),
            maxValue = 23,
            isEditable = !isCountdownRunning,
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
        DirectEditTimeUnit(
            value = editingMinutes,
            label = stringResource(R.string.time_unit_minute),
            maxValue = 59,
            isEditable = !isCountdownRunning,
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
        DirectEditTimeUnit(
            value = editingSeconds,
            label = stringResource(R.string.time_unit_second),
            maxValue = 59,
            isEditable = !isCountdownRunning,
            onValueChange = { newValue ->
                editingSeconds = newValue
                onTimeChange(editingHours, editingMinutes, newValue)
            }
        )
    }
}

/**
 * 直接编辑的时间单位组件（移植自备份项目）
 */
@Composable
private fun DirectEditTimeUnit(
    value: Int,
    label: String,
    maxValue: Int,
    isEditable: Boolean,
    onValueChange: (Int) -> Unit
) {
    var textValue by remember { mutableStateOf("%02d".format(value)) }
    var isEditing by remember { mutableStateOf(false) }
    
    // 当值改变时更新显示文本
    LaunchedEffect(value) {
        if (!isEditing) {
            textValue = "%02d".format(value)
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
                    onValueChange = { newValue: String ->

                        // 限制输入为数字，最多两位
                        val filtered = newValue.filter { char -> char.isDigit() }.take(2)
                        textValue = filtered
                        
                        // 如果输入有效，更新值
                        val intValue = filtered.toIntOrNull()
                        if (intValue != null && intValue <= maxValue) {
                            onValueChange(intValue)
                        }
                        
                        // 如果输入了两位数字，结束编辑
                        if (filtered.length == 2) {
                            isEditing = false
                            val finalValue = intValue?.coerceIn(0, maxValue) ?: 0
                            textValue = "%02d".format(finalValue)

                        }
                    },
                    textStyle = TextStyle(
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
                            textValue = "%02d".format(intValue)
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
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (isEditable) Color(0xFF333333) else Color(0xFF999999),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * CountDown时间单位组件 (保留原版本以防回退需要)
 */
@Composable
private fun CountdownTimeUnit(
    value: Int,
    label: String,
    isEditable: Boolean,
    isEditing: Boolean,
    onValueChange: (Int) -> Unit,
    onEditingStateChange: (Boolean) -> Unit
) {
    var textValue by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val textFieldValue = remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue()) }
    var hasFocusedOnce by remember { mutableStateOf(false) } // 跟踪是否已经获得过焦点
    
    // 当开始编辑时，设置初始文本值并选中全部文本
    LaunchedEffect(isEditing) {
        println("⭐ LaunchedEffect triggered: label=$label, isEditing=$isEditing, value=$value")
        if (isEditing) {
            println("⭐ Starting edit mode for $label")
            val initialText = value.toString()
            textValue = initialText
            textFieldValue.value = androidx.compose.ui.text.input.TextFieldValue(
                text = initialText,
                selection = androidx.compose.ui.text.TextRange(0, initialText.length) // 选中所有文本
            )
            println("⭐ Text set to '$initialText', selection: 0-${initialText.length}")
            
            // 尝试多次请求焦点，确保成功
            repeat(3) { attempt ->
                kotlinx.coroutines.delay((50 * (attempt + 1)).toLong()) // 递增延迟
                focusRequester.requestFocus()
                println("⭐ Focus requested for $label (attempt ${attempt + 1})")
            }
            

        } else {
            // 重置焦点跟踪状态
            hasFocusedOnce = false
            println("⭐ Reset hasFocusedOnce for $label")
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
                    color = if (isEditing) Color(0xFFFF7043) else Color(0xFFFF5722), // 编辑时稍微浅一点
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable(enabled = isEditable && !isEditing) {
                    println("⭐ CountdownTimeUnit clicked: label=$label, value=$value, isEditable=$isEditable, isEditing=$isEditing")
                    onEditingStateChange(true)
                    println("⭐ onEditingStateChange(true) called")

                },
            contentAlignment = Alignment.Center
        ) {
            println("⭐ CountdownTimeUnit render: label=$label, isEditing=$isEditing, value=$value")
            if (isEditing) {
                println("⭐ Rendering BasicTextField for $label")
                // 编辑模式 - 使用OutlinedTextField替代BasicTextField进行测试
                androidx.compose.material3.OutlinedTextField(
                    value = textFieldValue.value.text,
                    onValueChange = { newText ->
                        println("⭐ OutlinedTextField onValueChange TRIGGERED: old='${textFieldValue.value.text}', new='$newText' for $label")
                        // 只允许数字输入，最多2位
                        val filtered = newText.filter { it.isDigit() }.take(2)
                        println("⭐ Filtered text: '$filtered' (original: '$newText')")
                        textValue = filtered
                        textFieldValue.value = androidx.compose.ui.text.input.TextFieldValue(
                            text = filtered,
                            selection = androidx.compose.ui.text.TextRange(filtered.length) // 光标在末尾
                        )
                        
                        // 如果用户输入了有效数字，延迟一点完成编辑（给用户时间输入更多）
                        if (filtered.isNotEmpty() && filtered != "0") {
                            val maxValue = if (label.contains("Hour") || label.contains("小时")) 23 else 59
                            val newValue = filtered.toIntOrNull()?.coerceIn(0, maxValue) ?: value
                            println("⭐ Will complete edit with value: $newValue")
                        }
                    },
                    modifier = Modifier
                        .size(64.dp)
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            println("⭐ Focus changed for $label: isFocused=${focusState.isFocused}, isEditing=$isEditing, hasFocusedOnce=$hasFocusedOnce")
                            if (focusState.isFocused) {
                                hasFocusedOnce = true
                                println("⭐ $label gained focus successfully!")
                            } else if (!focusState.isFocused && isEditing && hasFocusedOnce) {
                                // 只有在真正获得过焦点后失去焦点时才完成编辑
                                println("⭐ Lost focus, completing edit for $label")
                                val maxValue = if (label.contains("Hour") || label.contains("小时")) 23 else 59
                                val newValue = textValue.toIntOrNull()?.coerceIn(0, maxValue) ?: value
                                onValueChange(newValue)
                                onEditingStateChange(false)
                            } else if (!focusState.isFocused && isEditing && !hasFocusedOnce) {
                                println("⭐ $label failed to gain focus, still trying...")
                            }
                        },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Done
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onDone = {
                            val maxValue = if (label.contains("Hour") || label.contains("小时")) 23 else 59
                            val newValue = textValue.toIntOrNull()?.coerceIn(0, maxValue) ?: value
                            onValueChange(newValue)
                            onEditingStateChange(false)
                        }
                    ),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = Color.White
                    )
                )
            } else {
                // 显示模式 - 显示数字
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

// TimeEditDialog已移除 - 现在使用原地编辑 