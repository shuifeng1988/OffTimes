package com.offtime.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.offtime.app.R
import com.offtime.app.data.dao.UsageData
import kotlin.math.max

@Composable
fun UsageLineChart(
    data: List<UsageData>,
    @Suppress("UNUSED_PARAMETER") title: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            if (data.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.no_data), color = Color.Gray)
                }
            } else {
                // 图表主体区域
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Y轴标签（左侧）
                    Column(
                        modifier = Modifier
                            .width(60.dp)
                            .height(190.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End
                    ) {
                        // 垂直显示"使用时间" - 增大字体
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("使", fontSize = 16.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text("用", fontSize = 16.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text("时", fontSize = 16.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text("间", fontSize = 16.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        }
                        
                        // Y轴刻度标签
                        val maxUsage = data.maxOfOrNull { it.usageMinutes } ?: 60
                        val maxHours = (maxUsage / 60f).coerceAtLeast(1f)
                        for (i in 5 downTo 0) {
                            val hours = maxHours * i / 5f
                            Text(
                                text = if (hours >= 1f) "${hours.toInt()}h" else "${(hours * 60).toInt()}m",
                                fontSize = 10.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    }
                    
                    // 图表区域
                    Column {
                        Canvas(
                            modifier = Modifier
                                .weight(1f)
                                .height(190.dp)
                                .padding(start = 8.dp)
                        ) {
                            drawUsageLineChart(data, size.width, size.height)
                        }
                        
                        // X轴标签
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp, top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            data.reversed().forEachIndexed { index, item ->
                                if (index == 0 || index == data.size - 1 || index % max(1, data.size / 6) == 0) {
                                    Text(
                                        text = formatPeriodLabel(item.period),
                                        fontSize = 20.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompletionLineChart(
    data: List<UsageData>,
    @Suppress("UNUSED_PARAMETER") title: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            if (data.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.no_data), color = Color.Gray)
                }
            } else {
                // 图表主体区域
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Y轴标签（左侧）
                    Column(
                        modifier = Modifier
                            .width(60.dp)
                            .height(190.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End
                    ) {
                        // 垂直显示"达标率" - 增大字体
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("达", fontSize = 16.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text("标", fontSize = 16.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text("率", fontSize = 16.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        }
                        
                        // Y轴刻度标签
                        for (i in 5 downTo 0) {
                            val percentage = i * 20 // 0%, 20%, 40%, 60%, 80%, 100%
                            Text(
                                text = "${percentage}%",
                                fontSize = 10.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    }
                    
                    // 图表区域
                    Column {
                        Canvas(
                            modifier = Modifier
                                .weight(1f)
                                .height(190.dp)
                                .padding(start = 8.dp)
                        ) {
                            drawCompletionLineChart(data, size.width, size.height)
                        }
                        
                        // X轴标签
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp, top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            data.reversed().forEachIndexed { index, item ->
                                if (index == 0 || index == data.size - 1 || index % max(1, data.size / 6) == 0) {
                                    Text(
                                        text = formatPeriodLabel(item.period),
                                        fontSize = 20.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawUsageLineChart(data: List<UsageData>, width: Float, height: Float) {
    if (data.isEmpty()) return

    val reversedData = data.reversed()
    val maxUsage = reversedData.maxOfOrNull { it.usageMinutes } ?: 60
    val maxValue = (maxUsage / 60f).coerceAtLeast(1f) * 60 // 确保至少1小时的范围
    val stepX = width / (reversedData.size - 1).coerceAtLeast(1)

    // 绘制网格线
    for (i in 0..5) {
        val y = height * (1 - i / 5f)
        drawLine(
            color = Color(0xFFE0E0E0),
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 1.dp.toPx()
        )
    }

    // 绘制垂直网格线
    for (i in 0 until reversedData.size) {
        val x = i * stepX
        drawLine(
            color = Color(0xFFE0E0E0),
            start = Offset(x, 0f),
            end = Offset(x, height),
            strokeWidth = 1.dp.toPx()
        )
    }

    // 绘制折线
    val path = Path()
    val points = mutableListOf<Offset>()

    reversedData.forEachIndexed { index, item ->
        val x = index * stepX
        val y = height * (1 - item.usageMinutes.toFloat() / maxValue)
        points.add(Offset(x, y))

        if (index == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }

    // 绘制折线
    drawPath(
        path = path,
        color = Color(0xFF2196F3),
        style = Stroke(width = 3.dp.toPx())
    )

    // 绘制数据点
    points.forEach { point ->
        drawCircle(
            color = Color(0xFF2196F3),
            radius = 5.dp.toPx(),
            center = point
        )
    }
}

private fun DrawScope.drawCompletionLineChart(data: List<UsageData>, width: Float, height: Float) {
    if (data.isEmpty()) return
    
    val reversedData = data.reversed()
    val stepX = width / (reversedData.size - 1).coerceAtLeast(1)
    val maxValue = 100f // 最大显示100%
    
    // 绘制网格线
    for (i in 0..5) {
        val y = height * (1 - i / 5f)
        drawLine(
            color = Color(0xFFE0E0E0),
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 1.dp.toPx()
        )
    }
    
    // 绘制垂直网格线
    for (i in 0 until reversedData.size) {
        val x = i * stepX
        drawLine(
            color = Color(0xFFE0E0E0),
            start = Offset(x, 0f),
            end = Offset(x, height),
            strokeWidth = 1.dp.toPx()
        )
    }
    
    // 绘制折线
    val path = Path()
    val points = mutableListOf<Offset>()
    
    reversedData.forEachIndexed { index, item ->
        val x = index * stepX
        val y = height * (1 - item.completionRate / maxValue)
        points.add(Offset(x, y))
        
        if (index == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    
    // 绘制折线
    drawPath(
        path = path,
        color = Color(0xFF9C27B0), // 紫色，与附图一致
        style = Stroke(width = 3.dp.toPx())
    )
    
    // 绘制数据点
    points.forEach { point ->
        drawCircle(
            color = Color(0xFF9C27B0),
            radius = 5.dp.toPx(),
            center = point
        )
    }
}

private fun formatPeriodLabel(period: String): String {
    return when {
        // 日期格式：yyyy-MM-dd -> MM-dd
        period.contains("-") && period.length == 10 -> period.substring(5) // MM-dd
        
        // 月份格式：yyyy-MM -> MM，但支持新的"年-月"和"本月"格式
        period.contains("-") && period.length == 7 -> period.substring(5) // MM
        
        // 新的周格式：本周、年-周（如25-20）
        period == "本周" -> "本周"
        period.contains("-") && period.length <= 5 && period.split("-").size == 2 -> {
            val parts = period.split("-")
            if (parts[0].length == 2 && parts[1].length <= 2) {
                period // 已经是"年-周"或"年-月"格式
            } else {
                period
            }
        }
        
        // 新的月格式：本月
        period == "本月" -> "本月"
        
        // 特殊标记
        period == "今天" || period == "today" -> "今天"
        
        // 其他格式直接返回
        else -> period
    }
} 