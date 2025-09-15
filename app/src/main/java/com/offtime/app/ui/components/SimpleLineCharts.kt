package com.offtime.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offtime.app.R
import com.offtime.app.data.dao.UsageData
import com.offtime.app.data.dao.RewardPunishmentData
import com.offtime.app.utils.DateLocalizer
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import androidx.compose.foundation.background
import com.offtime.app.ui.theme.LocalResponsiveDimensions
import com.offtime.app.utils.UnifiedTextManager

/**
 * 简洁的使用时间折线图
 */
@Composable
fun SimpleUsageLineChart(
    usageData: List<UsageData>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 垂直标题
        VerticalTitle(stringResource(R.string.chart_axis_usage_time))
        
        // 图表区域
        Column(modifier = Modifier.weight(1f)) {
            if (usageData.isEmpty()) {
                EmptyChartPlaceholder()
            } else {
                // 只计算有效数据（>=0）的最大值，排除-1的无数据标记
                val maxUsage = usageData.filter { it.usageMinutes >= 0 }.maxOfOrNull { it.usageMinutes }?.toFloat() ?: 1f
                val maxValueMinutes = max(maxUsage, 60f) // 最小显示1小时
                val maxValueHours = maxValueMinutes / 60f // 转换为小时

                // 数据有效性判断：-1表示无数据记录（程序安装前），>=0表示有数据记录（程序安装后，包括0使用时间）
                val hasDataList = usageData.map { data ->
                    data.usageMinutes >= 0  // -1=无记录（灰色虚线）, >=0=有记录（彩色实线）
                }
                
                val context = LocalContext.current
                SimpleLineChart(
                    data = usageData.map { 
                        if (it.usageMinutes >= 0) it.usageMinutes.toFloat() / 60f else 0f 
                    }, // 转换为小时，-1转为0用于绘制
                    labels = usageData.map { DateLocalizer.formatPeriodLabel(context, it.period) },
                    maxValue = maxValueHours,
                    yAxisUnit = "",
                    lineColor = Color(0xFF2196F3),
                    hasData = hasDataList,
                    chartType = "usage", // 使用时间图表类型
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * 简洁的完成率折线图
 */
@Composable
fun SimpleCompletionLineChart(
    completionData: List<UsageData>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 垂直标题
        VerticalTitle(stringResource(R.string.chart_axis_completion_rate))
        
        // 图表区域
        Column(modifier = Modifier.weight(1f)) {
            if (completionData.isEmpty()) {
                EmptyChartPlaceholder()
            } else {
                val context = LocalContext.current
                SimpleLineChart(
                    data = completionData.map { 
                        if (it.completionRate >= 0f) it.completionRate else 0f 
                    }, // 直接使用百分比数值，-1转为0用于绘制
                    labels = completionData.map { DateLocalizer.formatPeriodLabel(context, it.period) },
                    maxValue = 100f,
                    yAxisUnit = "",
                    lineColor = Color(0xFF9C27B0),
                    hasData = completionData.map { it.completionRate >= 0f }, // -1表示无数据，>=0表示有数据
                    chartType = "completion", // 完成率图表类型
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * 简洁的奖励完成度折线图
 */
@Composable
fun SimpleRewardChart(
    rewardPunishmentData: List<RewardPunishmentData>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 垂直标题
        VerticalTitle(stringResource(R.string.chart_axis_reward_completion))
        
        // 图表区域
        Column(modifier = Modifier.weight(1f)) {
            val context = LocalContext.current
            
            // 生成默认数据或使用现有数据
            val effectiveData = if (rewardPunishmentData.isEmpty()) {
                // 生成15个默认数据点（最近15天）
                generateDefaultRewardPunishmentData()
            } else {
                rewardPunishmentData
            }
            
            SimpleLineChart(
                data = effectiveData.map { 
                    if (it.rewardValue >= 0f) it.rewardValue else 0f 
                }, // 直接使用百分比数值，-1转为0用于绘制
                labels = effectiveData.map { DateLocalizer.formatPeriodLabel(context, it.period) },
                maxValue = 100f,
                yAxisUnit = "",
                lineColor = Color(0xFF4CAF50), // 绿色
                hasData = effectiveData.map { it.rewardValue >= 0f }, // >=0表示有数据（包括0%完成度）
                chartType = "completion", // 使用与完成率相同的图表类型
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * 简洁的惩罚完成度折线图
 */
@Composable
fun SimplePunishmentChart(
    rewardPunishmentData: List<RewardPunishmentData>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 垂直标题
        VerticalTitle(stringResource(R.string.chart_axis_punishment_completion))
        
        // 图表区域
        Column(modifier = Modifier.weight(1f)) {
            val context = LocalContext.current
            
            // 生成默认数据或使用现有数据
            val effectiveData = if (rewardPunishmentData.isEmpty()) {
                // 生成15个默认数据点（最近15天）
                generateDefaultRewardPunishmentData()
            } else {
                rewardPunishmentData
            }
            
            SimpleLineChart(
                data = effectiveData.map { 
                    if (it.punishmentValue >= 0f) it.punishmentValue else 0f 
                }, // 直接使用百分比数值，-1转为0用于绘制
                labels = effectiveData.map { DateLocalizer.formatPeriodLabel(context, it.period) },
                maxValue = 100f,
                yAxisUnit = "",
                lineColor = Color(0xFFFF8F00), // 橙色，与惩罚完成模块颜色一致
                hasData = effectiveData.map { it.punishmentValue >= 0f }, // >=0表示有数据（包括0%完成度）
                chartType = "completion", // 使用与完成率相同的图表类型
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * 垂直标题组件
 */
@Composable
private fun VerticalTitle(title: String) {
    // 获取响应式尺寸参数
    val dimensions = LocalResponsiveDimensions
    
    // 检测当前语言
    val isEnglish = UnifiedTextManager.isEnglish()
    
    Column(
        modifier = Modifier.width(dimensions.chartAxisTitleWidth),  // 使用全局宽度参数
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy((-6).dp)  // 增加负间距使字符更紧密
    ) {
        title.forEachIndexed { index, char ->
            Text(
                text = char.toString(),
                fontSize = dimensions.chartAxisTitleFontSize,  // 使用全局标题字体大小
                fontWeight = if (char == 'H' || char == '%') FontWeight.Normal else FontWeight.Bold, // 单位不加粗
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,  // 使用主题色彩
                fontFamily = if (isEnglish) FontFamily.Monospace else FontFamily.Default, // 英文使用等宽字体更适合垂直排列
                modifier = if (index > 0) Modifier.offset(y = (-3).dp) else Modifier  // 增加向上偏移
            )
        }
    }
}

/**
 * 空图表占位符
 */
@Composable
private fun EmptyChartPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.no_data),
            color = Color.Gray,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * 通用的简洁折线图
 */
@Composable
private fun SimpleLineChart(
    data: List<Float>,
    labels: List<String>,
    maxValue: Float,
    yAxisUnit: String,
    lineColor: Color,
    hasData: List<Boolean> = List(data.size) { true }, // 默认所有数据都有效
    chartType: String,
    modifier: Modifier = Modifier
) {
    // 获取响应式尺寸参数
    val dimensions = LocalResponsiveDimensions
    
    Column(modifier = modifier.fillMaxWidth()) {
        // 图表主体
        Row(modifier = Modifier.weight(1f)) {
            // Y轴标签
            Column(
                modifier = Modifier
                    .wrapContentWidth() // 改为自适应宽度，移除固定宽度约束
                    .fillMaxHeight()
                    .padding(end = dimensions.chartAxisLabelToAxisDistance), // 移除start padding，只保留与轴的距离
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                val steps = 4
                for (i in steps downTo 0) {
                    val value = maxValue * i / steps
                    val formattedText = when (chartType) {
                        "usage" -> {
                            // 使用时间图表：带一位小数的数字表示小时，去掉"h"
                            String.format("%.1f", value)
                        }
                        "completion" -> {
                            // 完成率图表：不带小数的数字表示百分比
                            String.format("%.0f", value)
                        }
                        else -> {
                            // 默认格式
                            String.format("%.1f%s", value, yAxisUnit)
                        }
                    }
                    Text(
                        text = formattedText,
                        fontSize = dimensions.chartAxisLabelFontSize,  // 使用全局字体大小
                        color = Color.Gray,
                        textAlign = TextAlign.End,
                        maxLines = 1,  // 强制单行显示
                        overflow = TextOverflow.Visible,  // 允许溢出显示
                        modifier = Modifier.wrapContentWidth() // 改为自适应宽度
                    )
                }
            }
            
            // 图表区域
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                val width = size.width
                val height = size.height
                
                // 绘制坐标轴（使用全局粗细参数）
                drawLine(
                    color = Color.Black,
                    start = Offset(0f, 0f),
                    end = Offset(0f, height),
                    strokeWidth = dimensions.chartAxisStrokeWidth.toPx()
                )
                drawLine(
                    color = Color.Black,
                    start = Offset(0f, height),
                    end = Offset(width, height),
                    strokeWidth = dimensions.chartAxisStrokeWidth.toPx()
                )
                
                // 绘制水平网格线
                for (i in 1..4) {
                    val y = height * (1 - i / 4f)
                    drawLine(
                        color = Color.LightGray,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = dimensions.chartGridStrokeWidth.toPx()
                    )
                }
                
                // 绘制垂直网格线
                if (data.size > 1) {
                    for (i in 1 until data.size) {
                        val x = width * i / (data.size - 1)
                        drawLine(
                            color = Color.LightGray,
                            start = Offset(x, 0f),
                            end = Offset(x, height),
                            strokeWidth = dimensions.chartGridStrokeWidth.toPx()
                        )
                    }
                }
                
                // 绘制X轴刻度线（与标签位置对应）
                val totalLabels = labels.size
                val selectedIndices = when {
                    totalLabels <= 4 -> (0 until totalLabels).toList()
                    else -> listOf(0, totalLabels / 3, totalLabels * 2 / 3, totalLabels - 1)
                }
                
                selectedIndices.forEach { index ->
                    val x = if (data.size == 1) {
                        width / 2
                    } else {
                        width * index / (data.size - 1)
                    }
                    
                    // 绘制X轴刻度线（向下延伸6dp）
                    drawLine(
                        color = Color.Black,
                        start = Offset(x, height),
                        end = Offset(x, height + 6.dp.toPx()),
                        strokeWidth = 4f
                    )
                }
                
                // 绘制数据线 - 分段绘制有数据和无数据的部分
                if (data.size > 1) {
                    for (i in 0 until data.size - 1) {
                        val x1 = width * i / (data.size - 1)
                        val y1 = height - (height * data[i] / maxValue).coerceIn(0f, height)
                        
                        val x2 = width * (i + 1) / (data.size - 1)
                        val y2 = height - (height * data[i + 1] / maxValue).coerceIn(0f, height)
                        
                        val currentHasData = hasData.getOrElse(i) { true }
                        val nextHasData = hasData.getOrElse(i + 1) { true }
                        
                        when {
                            currentHasData && nextHasData -> {
                                // 两个点都有数据，使用实线
                                drawLine(
                                    color = lineColor,
                                    start = Offset(x1, y1),
                                    end = Offset(x2, y2),
                                    strokeWidth = 2.dp.toPx()
                                )
                            }
                            !currentHasData && !nextHasData -> {
                                // 两个点都没数据，使用虚线
                                drawLine(
                                    color = Color.LightGray,
                                    start = Offset(x1, y1),
                                    end = Offset(x2, y2),
                                    strokeWidth = 2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 5.dp.toPx()), 0f)
                                )
                            }
                            else -> {
                                // 一个有数据一个没数据，使用过渡颜色虚线
                                val transitionColor = if (currentHasData) lineColor else Color.LightGray
                                drawLine(
                                    color = transitionColor,
                                    start = Offset(x1, y1),
                                    end = Offset(x2, y2),
                                    strokeWidth = 2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()), 0f)
                                )
                            }
                        }
                    }
                    
                    // 绘制数据点
                    data.forEachIndexed { index, value ->
                        val x = width * index / (data.size - 1)
                        val y = height - (height * value / maxValue).coerceIn(0f, height)
                        val currentHasData = hasData.getOrElse(index) { true }
                        
                        val finalColor = if (currentHasData) lineColor else Color.LightGray
                        
                        drawCircle(
                            color = finalColor,
                            radius = 3.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }
                }
            }
        }
        
        // X轴标签 - 智能选择标签显示
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 占位空间，对应Y轴标签和垂直标题的宽度
            Spacer(modifier = Modifier.width(dimensions.chartAxisTitleWidth + dimensions.chartAxisLabelToAxisDistance))
            
            // X轴标签区域，与Canvas对齐
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 选择4个关键标签：左边、1/3位置、2/3位置、右边
                val totalLabels = labels.size
                val selectedIndices = when {
                    totalLabels <= 4 -> (0 until totalLabels).toList()
                    else -> listOf(0, totalLabels / 3, totalLabels * 2 / 3, totalLabels - 1)
                }
                
                selectedIndices.forEach { index ->
                    Text(
                        text = labels[index],
                        fontSize = dimensions.chartAxisLabelFontSize,  // 使用全局字体大小
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

/**
 * 生成默认的奖励惩罚数据（15个数据点）
 * 用于在没有实际数据时显示图表
 */
private fun generateDefaultRewardPunishmentData(): List<RewardPunishmentData> {
    val calendar = java.util.Calendar.getInstance()
    val data = mutableListOf<RewardPunishmentData>()
    
    for (i in 14 downTo 0) {
        calendar.time = java.util.Date()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -i)
        
        val displayPeriod = if (i == 0) "今天" else {
            val month = calendar.get(java.util.Calendar.MONTH) + 1
            val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
            "${month}/${day}"
        }
        
        // 使用-1f表示安装前的数据（将显示为灰色虚线）
        data.add(RewardPunishmentData(
            period = displayPeriod,
            rewardValue = -1f,    // -1表示安装前数据
            punishmentValue = -1f // -1表示安装前数据
        ))
    }
    
    return data
} 