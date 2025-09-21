package com.offtime.app.ui.screens.debug

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.offtime.app.R
import com.offtime.app.ui.viewmodel.DataRepairViewModel
import com.offtime.app.utils.UsageDataValidator

/**
 * 数据修复调试界面
 * 
 * 功能：
 * 1. 显示数据一致性检查结果
 * 2. 列出发现的问题（重复记录、可疑会话等）
 * 3. 提供一键修复功能
 * 4. 显示修复前后对比
 * 5. 生成详细的修复报告
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataRepairDebugScreen(
    onNavigateBack: () -> Unit,
    viewModel: DataRepairViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadValidationData()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🔧 ${stringResource(R.string.debug_data_repair_screen_title)}") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.debug_back))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshValidation() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.debug_refresh))
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 概览卡片
            item {
                DataOverviewCard(
                    reports = uiState.validationReports,
                    isLoading = uiState.isLoading
                )
            }
            
            // 快速修复按钮
            item {
                QuickRepairActions(
                    onPerformFullRepair = { viewModel.performFullRepair() },
                    onFixDouYinIssues = { viewModel.fixDouYinIssues() },
                    onRecalculateData = { viewModel.recalculateData() },
                    isRepairing = uiState.isRepairing
                )
            }
            
            // 分类问题列表
            items(uiState.validationReports) { report ->
                CategoryIssueCard(
                    report = report,
                    onFixCategory = { viewModel.fixSpecificCategory(report.categoryId) }
                )
            }
            
            // 修复结果显示
            if (uiState.repairResult != null) {
                item {
                    RepairResultCard(result = uiState.repairResult!!)
                }
            }
            
            // 修复报告
            if (uiState.repairReport.isNotEmpty()) {
                item {
                    RepairReportCard(report = uiState.repairReport)
                }
            }
        }
    }
}

@Composable
private fun DataOverviewCard(
    reports: List<UsageDataValidator.ValidationReport>,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Assessment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "数据质量概览",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    Text("正在检查数据质量...")
                }
            } else {
                val inconsistentCount = reports.count { !it.isConsistent }
                val totalDuplicates = reports.sumOf { it.duplicateSessions.size }
                val totalSuspicious = reports.sumOf { it.screenOffSessions.size }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DataMetricChip(
                        label = "不一致分类",
                        value = inconsistentCount,
                        isError = inconsistentCount > 0
                    )
                    DataMetricChip(
                        label = "重复会话",
                        value = totalDuplicates,
                        isError = totalDuplicates > 0
                    )
                    DataMetricChip(
                        label = "可疑会话",
                        value = totalSuspicious,
                        isError = totalSuspicious > 0
                    )
                }
                
                if (inconsistentCount == 0 && totalDuplicates == 0 && totalSuspicious == 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.Green
                        )
                        Text(
                            "数据质量良好，无需修复",
                            color = Color.Green,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DataMetricChip(
    label: String,
    value: Int,
    isError: Boolean
) {
    Surface(
        color = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun QuickRepairActions(
    onPerformFullRepair: () -> Unit,
    onFixDouYinIssues: () -> Unit,
    onRecalculateData: () -> Unit,
    isRepairing: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "快速修复操作",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPerformFullRepair,
                    enabled = !isRepairing,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isRepairing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.Build, contentDescription = null)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("完整修复")
                }
                
                OutlinedButton(
                    onClick = onFixDouYinIssues,
                    enabled = !isRepairing,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("修复抖音")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedButton(
                onClick = onRecalculateData,
                enabled = !isRepairing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Calculate, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("重新计算汇总数据")
            }
        }
    }
}

@Composable
private fun CategoryIssueCard(
    report: UsageDataValidator.ValidationReport,
    onFixCategory: () -> Unit
) {
    val hasIssues = !report.isConsistent || 
                   report.duplicateSessions.isNotEmpty() || 
                   report.screenOffSessions.isNotEmpty()
    
    if (!hasIssues) return
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "🏷️ ${report.categoryName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                TextButton(onClick = onFixCategory) {
                    Text("修复")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (!report.isConsistent) {
                IssueRow(
                    icon = Icons.Default.Error,
                    title = "数据不一致",
                    description = "饼图: ${report.pieChartTotal/60}分钟，详情: ${report.detailTotal/60}分钟，差异: ${report.timeDifferenceSeconds}秒"
                )
            }
            
            if (report.duplicateSessions.isNotEmpty()) {
                IssueRow(
                    icon = Icons.Default.ContentCopy,
                    title = "重复会话",
                    description = "发现 ${report.duplicateSessions.size} 个重复记录"
                )
            }
            
            if (report.screenOffSessions.isNotEmpty()) {
                IssueRow(
                    icon = Icons.Default.Warning,
                    title = "可疑会话",
                    description = "发现 ${report.screenOffSessions.size} 个可疑的长时间记录"
                )
            }
        }
    }
}

@Composable
private fun IssueRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(16.dp)
        )
        Column {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RepairResultCard(
    result: com.offtime.app.service.DataRepairService.DataRepairResult
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (result.isRepairSuccessful) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    if (result.isRepairSuccessful) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (result.isRepairSuccessful) Color.Green else MaterialTheme.colorScheme.primary
                )
                Text(
                    "修复结果: ${if (result.isRepairSuccessful) "成功" else "部分完成"}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text("修复问题数量: ${result.fixedIssuesCount}")
            Text("系统应用清理: ${if (result.systemAppCleaned) "✅" else "❌"}")
            Text("汇总数据重算: ${if (result.summaryRecalculated) "✅" else "❌"}")
            
            if (!result.errorMessage.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "错误信息: ${result.errorMessage}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun RepairReportCard(
    report: String
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "📋 详细修复报告",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                report,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
    }
} 