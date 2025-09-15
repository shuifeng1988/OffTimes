package com.offtime.app.ui.offlinetimer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.offtime.app.data.entity.AppCategoryEntity
import com.offtime.app.service.OfflineTimerService
import com.offtime.app.utils.CategoryUtils
import com.offtime.app.utils.DateLocalizer
import com.offtime.app.ui.components.CountdownTimerDialog
import kotlinx.coroutines.delay
import com.offtime.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineTimerScreen(
    onNavigateBack: () -> Unit,
    viewModel: OfflineTimerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    // Tab状态管理
    var selectedTab by remember { mutableStateOf(0) } // 0: Timer, 1: CountDown
    
    // CountDown相关状态
    var showCountdownDialog by remember { mutableStateOf(false) }
    var isCountdownRunning by remember { mutableStateOf(false) }
    var isCountdownPaused by remember { mutableStateOf(false) }
    var countdownHours by remember { mutableStateOf(0) }
    var countdownMinutes by remember { mutableStateOf(30) }
    var countdownSeconds by remember { mutableStateOf(0) }

    // 监听消息和错误
    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            // 这里可以显示SnackBar或Toast
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            // 这里可以显示错误SnackBar或Toast
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back_button))
                        }
                    }
                )
                
                // Tab按钮行
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Timer Tab
                    TabButton(
                        text = stringResource(R.string.timer_label),
                        icon = "⏱️",
                        isSelected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    
                    // CountDown Tab
                    TabButton(
                        text = stringResource(R.string.countdown_label),
                        icon = "⏰",
                        isSelected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    ) { paddingValues ->
        // 根据选中的Tab显示不同内容
        when (selectedTab) {
            0 -> {
                // Timer界面 - 原有内容
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                ) {
                    when {
                        uiState.isLoading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        
                        uiState.categoriesWithDuration.isEmpty() -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.no_category_data),
                                    textAlign = TextAlign.Center,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        else -> {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(uiState.categoriesWithDuration) { categoryWithDuration ->
                                    TimerCard(
                                        category = categoryWithDuration.category,
                                        todayDurationSec = categoryWithDuration.todayDurationSec,
                                        todaySessionCount = categoryWithDuration.todaySessionCount,
                                        isTimerRunning = viewModel.isTimerRunning(categoryWithDuration.category.id),
                                        timerState = viewModel.getTimerState(categoryWithDuration.category.id),
                                        onStartTimer = { 
                                            viewModel.startTimer(categoryWithDuration.category)
                                            // 启动前台服务
                                            val timerState = viewModel.getTimerState(categoryWithDuration.category.id)
                                            if (timerState != null) {
                                                OfflineTimerService.startTimer(
                                                    context = context,
                                                    catId = categoryWithDuration.category.id,
                                                    categoryName = categoryWithDuration.category.name,
                                                    sessionId = timerState.sessionId
                                                )
                                            }
                                        },
                                        onStopTimer = { 
                                            viewModel.stopTimer(categoryWithDuration.category.id)
                                            // 停止前台服务
                                            OfflineTimerService.stopTimer(context)
                                            
                                            // 触发数据聚合，确保计时数据立即更新到聚合表
                                            try {
                                                val intent = android.content.Intent(context, com.offtime.app.service.DataAggregationService::class.java)
                                                intent.action = com.offtime.app.service.DataAggregationService.ACTION_AGGREGATE_DATA
                                                context.startService(intent)
                                                android.util.Log.d("OfflineTimerScreen", "✅ 停止计时后触发数据聚合")
                                            } catch (e: Exception) {
                                                android.util.Log.e("OfflineTimerScreen", "❌ 触发数据聚合失败", e)
                                            }
                                        },
                                        viewModel = viewModel
                                    )
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                // CountDown界面
                CountdownScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    isCountdownRunning = isCountdownRunning,
                    isCountdownPaused = isCountdownPaused,
                    hours = countdownHours,
                    minutes = countdownMinutes,
                    seconds = countdownSeconds,
                    onStartCountdown = { 
                        isCountdownRunning = true
                        isCountdownPaused = false
                    },
                    onPauseCountdown = { isCountdownPaused = true },
                    onResumeCountdown = { isCountdownPaused = false },
                    onStopCountdown = { 
                        isCountdownRunning = false
                        isCountdownPaused = false
                    },
                    onTimeChange = { h, m, s ->
                        countdownHours = h
                        countdownMinutes = m
                        countdownSeconds = s
                    }
                )
            }
        }
        
        // CountDown Dialog (如果需要的话，保留原有逻辑)
        CountdownTimerDialog(
            showDialog = showCountdownDialog,
            isTimerRunning = isCountdownRunning,
            isTimerPaused = isCountdownPaused,
            hours = countdownHours,
            minutes = countdownMinutes,
            seconds = countdownSeconds,
            initialMinutes = 30,
            onTimerDurationChange = { minutes -> 
                countdownMinutes = minutes
                countdownHours = 0
                countdownSeconds = 0
            },
            onStartTimer = { 
                isCountdownRunning = true
                isCountdownPaused = false
                showCountdownDialog = false
            },
            onPauseTimer = { isCountdownPaused = true },
            onResumeTimer = { isCountdownPaused = false },
            onStopTimer = { 
                isCountdownRunning = false
                isCountdownPaused = false
            },
            onBackgroundMode = { showCountdownDialog = false },
            onTestSound = { /* TODO: 实现测试音效 */ },
            onDismiss = { showCountdownDialog = false }
        )
    }
}

@Composable
fun TimerCard(
    category: AppCategoryEntity,
    todayDurationSec: Int,
    todaySessionCount: Int,
    isTimerRunning: Boolean,
    timerState: TimerState?,
    onStartTimer: () -> Unit,
    onStopTimer: () -> Unit,
    viewModel: OfflineTimerViewModel
) {
    val context = LocalContext.current
    // 计算当前显示的时长（累计时长 + 当前计时时长）
    var displayDuration by remember { mutableStateOf(todayDurationSec) }
    
    // 实时更新计时显示
    LaunchedEffect(isTimerRunning, timerState) {
        if (isTimerRunning && timerState != null) {
            while (isTimerRunning) {
                val currentTime = System.currentTimeMillis()
                val currentSessionDuration = ((currentTime - timerState.startTime) / 1000).toInt()
                displayDuration = todayDurationSec + currentSessionDuration
                viewModel.updateTimerDuration(category.id, currentSessionDuration)
                delay(1000) // 每秒更新一次显示
            }
        } else {
            displayDuration = todayDurationSec
        }
    }

    val categoryColor = CategoryUtils.getCategoryColor(category.name)
    val categoryEmoji = CategoryUtils.getCategoryEmoji(category.name)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(categoryColor.copy(alpha = 0.1f))
                .padding(16.dp)
        ) {
            // 分类标题
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = categoryEmoji,
                        fontSize = 24.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = DateLocalizer.getCategoryNameFull(context, category.name),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                // 状态指示器
                if (isTimerRunning) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.timer_running),
                            color = Color.White,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 今日总计时统计
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = categoryColor.copy(alpha = 0.05f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "\uD83D\uDCCA " + stringResource(R.string.today_total_time),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = categoryColor
                        )
                        Text(
                            text = viewModel.formatDuration(displayDuration),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = categoryColor
                        )
                    }
                    
                    // 计时次数信息
                    if (todaySessionCount > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "\uD83D\uDD22 " + stringResource(R.string.sessions_count) + ":",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${todaySessionCount}" + stringResource(R.string.session_unit),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        if (todaySessionCount > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⏱️ " + stringResource(R.string.average_duration) + ":",
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = viewModel.formatDuration(
                                        if (todaySessionCount > 0) todayDurationSec / todaySessionCount else 0
                                    ),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    if (isTimerRunning) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.completed_label),
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = viewModel.formatDuration(todayDurationSec),
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.current_timer_label),
                                fontSize = 16.sp,
                                color = Color(0xFF4CAF50)
                            )
                            Text(
                                text = viewModel.formatDuration(displayDuration - todayDurationSec),
                                fontSize = 16.sp,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 控制按钮
            Button(
                onClick = if (isTimerRunning) onStopTimer else onStartTimer,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTimerRunning) {
                        MaterialTheme.colorScheme.error
                    } else {
                        categoryColor
                    }
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = if (isTimerRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = if (isTimerRunning) stringResource(R.string.stop_timer) else stringResource(R.string.start_timer),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Tab按钮组件
 */
@Composable
private fun TabButton(
    text: String,
    icon: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) {
                Color(0xFF4CAF50)
            } else {
                Color(0xFFF5F5F5)
            }
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (isSelected) 4.dp else 1.dp
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = icon,
                fontSize = 16.sp
            )
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else Color(0xFF666666)
            )
        }
    }
}

/**
 * CountDown界面组件
 */
@Composable
private fun CountdownScreen(
    modifier: Modifier = Modifier,
    isCountdownRunning: Boolean,
    isCountdownPaused: Boolean,
    hours: Int,
    minutes: Int,
    seconds: Int,
    onStartCountdown: () -> Unit,
    onPauseCountdown: () -> Unit,
    onResumeCountdown: () -> Unit,
    onStopCountdown: () -> Unit,
    onTimeChange: (Int, Int, Int) -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 大时钟显示
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF8F9FA)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 时间显示
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 小时
                    TimeDisplayUnit(
                        value = hours,
                        label = stringResource(R.string.time_unit_hour),
                        isEditable = !isCountdownRunning,
                        onValueChange = { newHours ->
                            onTimeChange(newHours, minutes, seconds)
                        }
                    )
                    
                    Text(
                        text = ":",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF5722)
                    )
                    
                    // 分钟
                    TimeDisplayUnit(
                        value = minutes,
                        label = stringResource(R.string.time_unit_minute),
                        isEditable = !isCountdownRunning,
                        onValueChange = { newMinutes ->
                            onTimeChange(hours, newMinutes, seconds)
                        }
                    )
                    
                    Text(
                        text = ":",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF5722)
                    )
                    
                    // 秒
                    TimeDisplayUnit(
                        value = seconds,
                        label = stringResource(R.string.time_unit_second),
                        isEditable = !isCountdownRunning,
                        onValueChange = { newSeconds ->
                            onTimeChange(hours, minutes, newSeconds)
                        }
                    )
                }
                
                // 控制按钮
                if (!isCountdownRunning) {
                    // 开始按钮
                    Button(
                        onClick = onStartCountdown,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.start_countdown),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                } else {
                    // 运行中的控制按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 暂停/继续按钮
                        Button(
                            onClick = if (isCountdownPaused) onResumeCountdown else onPauseCountdown,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCountdownPaused) Color(0xFF4CAF50) else Color(0xFFFFC107)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (isCountdownPaused) stringResource(R.string.resume_timer) else stringResource(R.string.pause_timer),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        
                        // 停止按钮
                        Button(
                            onClick = onStopCountdown,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF44336)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.stop_timer),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
        
        // 状态提示
        if (isCountdownRunning) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCountdownPaused) Color(0xFFFFF3E0) else Color(0xFFE8F5E8)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (isCountdownPaused) "⏸️ 倒计时已暂停" else "⏰ 倒计时进行中...",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isCountdownPaused) Color(0xFFFF8F00) else Color(0xFF4CAF50)
                )
            }
        }
    }
}

/**
 * 时间显示单位组件（简化版）
 */
@Composable
private fun TimeDisplayUnit(
    value: Int,
    label: String,
    isEditable: Boolean,
    onValueChange: (Int) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 数字显示
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    color = Color(0xFFFF5722),
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable(enabled = isEditable) {
                    // TODO: 实现编辑功能
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = String.format("%02d", value),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        
        // 单位标签
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF666666)
        )
    }
} 