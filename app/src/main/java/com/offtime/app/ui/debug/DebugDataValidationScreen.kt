package com.offtime.app.ui.debug

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offtime.app.R
import com.offtime.app.utils.DataValidationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class DebugDataValidationViewModel @Inject constructor(
    private val dataValidationUtils: DataValidationUtils
) : ViewModel() {
    
    data class UiState(
        val isLoading: Boolean = false,
        val validationReports: List<DataValidationUtils.DataValidationReport> = emptyList(),
        val fixedRecordsCount: Int = 0,
        val statusMessage: String = "",
        val isFixing: Boolean = false
    )
    
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    /**
     * 验证最近N天的数据
     */
    fun validateRecentData(days: Int = 7) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, statusMessage = "Validating data...")
            
            try {
                val reports = mutableListOf<DataValidationUtils.DataValidationReport>()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val calendar = Calendar.getInstance()
                
                for (i in 0 until days) {
                    calendar.time = Date()
                    calendar.add(Calendar.DAY_OF_YEAR, -i)
                    val date = dateFormat.format(calendar.time)
                    
                    val report = dataValidationUtils.validateDataIntegrity(date)
                    reports.add(report)
                }
                
                val totalAnomalous = reports.sumOf { it.anomalousRecords }
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    validationReports = reports,
                    statusMessage = "Validation complete: found $totalAnomalous anomalous records"
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    statusMessage = "Validation failed: ${e.message}"
                )
            }
        }
    }
    
    /**
     * 修复最近N天的异常数据
     */
    fun fixRecentAnomalousData(days: Int = 7) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isFixing = true, statusMessage = "正在修复异常数据...")
            
            try {
                val fixedCount = dataValidationUtils.fixRecentAnomalousData(days)
                
                _uiState.value = _uiState.value.copy(
                    isFixing = false,
                    fixedRecordsCount = fixedCount,
                    statusMessage = "修复完成：共修复 $fixedCount 条异常记录"
                )
                
                // 修复完成后重新验证
                validateRecentData(days)
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isFixing = false,
                    statusMessage = "修复失败：${e.message}"
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugDataValidationScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: DebugDataValidationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        viewModel.validateRecentData(7)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            text = context.getString(R.string.debug_data_validation),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = context.getString(R.string.debug_data_validation_subtitle),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = context.getString(R.string.action_back))
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
                .verticalScroll(rememberScrollState())
        ) {
            // 状态卡片
            StatusCard(
                statusMessage = uiState.statusMessage,
                isLoading = uiState.isLoading,
                isFixing = uiState.isFixing
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 操作按钮
            ActionButtons(
                onValidate = { viewModel.validateRecentData(7) },
                onFix = { viewModel.fixRecentAnomalousData(7) },
                isLoading = uiState.isLoading,
                isFixing = uiState.isFixing
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 验证报告列表
            if (uiState.validationReports.isNotEmpty()) {
                ValidationReportsList(
                    reports = uiState.validationReports
                )
            }
            
            // 说明文字
            Spacer(modifier = Modifier.height(24.dp))
            InfoCard()
        }
    }
}

@Composable
private fun StatusCard(
    statusMessage: String,
    isLoading: Boolean,
    isFixing: Boolean
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    isFixing -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = context.getString(R.string.debug_status),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = statusMessage,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActionButtons(
    onValidate: () -> Unit,
    onFix: () -> Unit,
    isLoading: Boolean,
    isFixing: Boolean
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onValidate,
            enabled = !isLoading && !isFixing,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(context.getString(R.string.debug_check_data))
        }
        
        Button(
            onClick = onFix,
            enabled = !isLoading && !isFixing,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(context.getString(R.string.debug_fix_anomalies))
        }
    }
}

@Composable
private fun ValidationReportsList(
    reports: List<DataValidationUtils.DataValidationReport>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "验证报告",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            reports.forEach { report ->
                ValidationReportItem(report = report)
                if (report != reports.last()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun ValidationReportItem(
    report: DataValidationUtils.DataValidationReport
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = report.date,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "总记录: ${report.totalRecords}条",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (report.maxAnomalousDurationSec > 0) {
                Text(
                    text = "最大异常: ${String.format("%.1f", report.getMaxAnomalousDurationMinutes())}分钟",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (report.anomalousRecords > 0) {
                Text(
                    text = "${report.anomalousRecords}条异常",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Text(
                    text = "正常",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
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
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "功能说明",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = """
                • 检查数据：扫描最近7天的柱状图数据，检测单小时使用时间是否超过60分钟
                • 修复异常：自动将超过60分钟的异常数据修正为60分钟上限
                • 异常原因：通常由跨时段会话分拆算法的累积误差导致
                • 数据安全：修复过程只调整异常数据，不影响正常数据
                """.trimIndent(),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
} 