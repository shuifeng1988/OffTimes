package com.offtime.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offtime.app.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import com.offtime.app.data.dao.UsageData
import com.offtime.app.data.dao.RewardPunishmentData
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.layout.Layout
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt as mathRoundToInt
import androidx.compose.material3.MaterialTheme
import com.offtime.app.utils.CategoryUtils
import com.offtime.app.ui.theme.LocalResponsiveDimensions
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items

@Deprecated(
    message = "使用PieChartWithCardsRefactored替代。此函数将在下一版本中移除。",
    replaceWith = ReplaceWith(
        "PieChartWithCardsRefactored(realUsageSec, virtualUsageSec, goalSec, yesterdayRewardDone, yesterdayPunishDone, yesterdayHasData, yesterdayGoalMet, categoryName, goalConditionType, rewardText, punishText, onRewardComplete, onPunishmentComplete, onYesterdayInfoClick, categoryUsageData, isRewardPunishmentEnabled, modifier)",
        "com.offtime.app.ui.components.PieChartWithCardsRefactored"
    )
)
@Composable
fun PieChartWithCards(
    realUsageSec: Int,      // 真实APP使用秒数
    virtualUsageSec: Int,   // 虚拟APP使用秒数 
    goalSec: Int,           // 目标秒数
    yesterdayRewardDone: Boolean,
    yesterdayPunishDone: Boolean,
    yesterdayHasData: Boolean,  // 昨日是否有数据
    yesterdayGoalMet: Boolean,  // 昨日目标是否完成（从数据库读取的真实状态）
    categoryName: String = "娱乐",  // 分类名称，用于判断提示逻辑
    goalConditionType: Int = 0, // 目标类型：0=≤目标算完成(娱乐类), 1=≥目标算完成(学习类)
    rewardText: String = "薯片", // 奖励内容
    punishText: String = "俯卧撑30个", // 惩罚内容
    onRewardComplete: () -> Unit = {},
    onPunishmentComplete: () -> Unit = {},
    onYesterdayInfoClick: () -> Unit = {}, // 点击昨日信息按钮的回调
    categoryUsageData: List<com.offtime.app.ui.viewmodel.HomeViewModel.CategoryUsageItem> = emptyList(), // 各分类使用数据
    isRewardPunishmentEnabled: Boolean = true, // 奖罚开关状态
    modifier: Modifier = Modifier
) {
    // 获取响应式尺寸参数
    val dimensions = LocalResponsiveDimensions
    
    // 直接使用从数据库读取的真实目标完成状态，不再进行推断
    val wasYesterdayGoalMet = yesterdayGoalMet
    
    // 添加调试日志
    android.util.Log.d("PieChartWithCards", "昨日状态调试: " +
        "rewardDone=$yesterdayRewardDone, punishDone=$yesterdayPunishDone, " +
        "hasData=$yesterdayHasData, goalMet=$wasYesterdayGoalMet, " +
        "categoryName=$categoryName")
    
    // 计算文字提示内容
    val totalUsageSec = realUsageSec + virtualUsageSec
    val remainingSec = goalSec - totalUsageSec
    
    // 将文字提示和饼图放在同一图层中
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // 饼图 - 背景层，向上移动更多
        if (categoryName == "总使用" && categoryUsageData.isNotEmpty()) {
            // 总使用分类且有多分类数据时，使用多分类饼图
            MultiCategoryPieChart(
                categoryUsageData = categoryUsageData,
                goalSec = goalSec,
                modifier = Modifier
                    .size(200.dp)
                    .offset(y = -20.dp)
            )
        } else {
            // 普通分类或无多分类数据时，使用单分类饼图
            PieChart(
                realUsageSec = realUsageSec, 
                virtualUsageSec = virtualUsageSec, 
                goalSec = goalSec,
                categoryName = categoryName, // 传入分类名称用于颜色选择
                modifier = Modifier
                    .size(200.dp)
                    .offset(y = -20.dp) // 向上移动更多，为文字腾出更多空间
            )
        }
        
        // 昨天奖罚达标提示模块 - 放在饼图上一层的左边
        if (yesterdayHasData && isRewardPunishmentEnabled) {
            // 昨日有数据时，显示正常的奖罚模块
            android.util.Log.d("PieChartWithCards", "昨日状态检查: goalMet=$wasYesterdayGoalMet, rewardDone=$yesterdayRewardDone, punishDone=$yesterdayPunishDone")
            if (wasYesterdayGoalMet && rewardText.isNotBlank()) {
                // 昨日达标，显示奖励模块（无论是否已完成）
                android.util.Log.d("PieChartWithCards", "显示奖励模块")
                YesterdayRewardModule(
                    rewardText = rewardText,
                    isCompleted = yesterdayRewardDone,
                    onRewardComplete = onRewardComplete,
                    onInfoClick = onYesterdayInfoClick,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 16.dp)
                        .offset(y = -20.dp) // 与饼图同一高度
                )
            } else if (!wasYesterdayGoalMet && punishText.isNotBlank()) {
                // 昨日未达标，显示惩罚模块（无论是否已完成）
                android.util.Log.d("PieChartWithCards", "显示惩罚模块")
                YesterdayPunishmentModule(
                    punishText = punishText,
                    isCompleted = yesterdayPunishDone,
                    onPunishmentComplete = onPunishmentComplete,
                    onInfoClick = onYesterdayInfoClick,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 16.dp)
                        .offset(y = -20.dp) // 与饼图同一高度
                )
            }
        }
        
        // 昨日无数据模块 - 放在左边与奖罚模块相同位置
        if (!yesterdayHasData) {
            // 昨日无数据时显示"昨日无数据"
            NoDataModule(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
                    .offset(y = -20.dp) // 与饼图同一高度
            )
        }
        // 删除右边的奖惩完成确认模块
        // 注释掉原来的CompletionConfirmModule逻辑
        
        // 文字提示 - 前景层，放在饼图下方，向下移动一些
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .align(Alignment.BottomCenter)
                .offset(y = -10.dp), // 向下移动，减少向上偏移
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                goalSec <= 0 -> {
                    // 目标时间无效
                    Text(
                        text = stringResource(R.string.home_set_goal_warning),
                        fontSize = dimensions.chartAxisTitleFontSize,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                totalUsageSec <= 0 -> {
                    // 还没有使用时间 - 根据目标类型显示不同提醒
                    val goalHours = goalSec / 3600f
                    val isEntertainmentType = goalConditionType == 0 // 0: ≤目标算完成(娱乐类)
                    
                    if (isEntertainmentType) {
                        // 娱乐类：显示可使用时间
                        Text(
                            text = stringResource(R.string.home_available_time, goalHours),
                            fontSize = dimensions.chartAxisTitleFontSize,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50) // 温和的绿色
                        )
                    } else {
                        // 学习/健身类：提醒需要达成目标
                        Text(
                            text = stringResource(R.string.home_goal_warning, goalHours),
                            fontSize = dimensions.chartAxisTitleFontSize,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9800) // 警告橙色
                        )
                    }
                }
                remainingSec > 0 -> {
                    // 还有剩余时间 - 根据目标类型判断
                    val remainingHours = remainingSec / 3600f
                    val isEntertainmentType = goalConditionType == 0 // 0: ≤目标算完成(娱乐类)
                    
                    if (isEntertainmentType) {
                        // 娱乐类：低于目标值是好的
                        Text(
                            text = "✅ 还可以使用${String.format("%.1f", remainingHours)}h，继续保持！",
                            fontSize = dimensions.chartAxisTitleFontSize,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50) // 温和的绿色
                        )
                    } else {
                        // 学习/工作类：低于目标值需要努力
                        Text(
                            text = "⚠️ 还需使用${String.format("%.1f", remainingHours)}h达成目标",
                            fontSize = dimensions.chartAxisTitleFontSize,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9800) // 警告橙色
                        )
                    }
                }
                else -> {
                    // 已经超出目标 - 根据目标类型判断
                    val overHours = (-remainingSec) / 3600f
                    val isEntertainmentType = goalConditionType == 0 // 0: ≤目标算完成(娱乐类)
                    
                    if (isEntertainmentType) {
                        // 娱乐类：超出目标值需要警告
                        Text(
                            text = "❌ 已超出目标${String.format("%.1f", overHours)}h",
                            fontSize = dimensions.chartAxisTitleFontSize,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF44336) // 警告红色
                        )
                    } else {
                        // 学习/工作类：超出目标值是好的
                        Text(
                            text = "🎉 已完成目标并超出${String.format("%.1f", overHours)}h，太棒了！",
                            fontSize = dimensions.chartAxisTitleFontSize,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50) // 温和的绿色
                        )
                    }
                }
            }
        }
    }
}

/**
 * 昨日奖励模块
 */
@Composable
private fun YesterdayRewardModule(
    rewardText: String,
    isCompleted: Boolean,
    onRewardComplete: () -> Unit,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 解析奖励文本，提取内容和数量
    val (content, quantity) = parseRewardPunishmentText(rewardText)
    
    Card(
        modifier = modifier
            .width(102.dp) // 增加20%：85dp * 1.2 = 102dp
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0) // 温暖的橙色背景
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 第一行：达标状态 - 靠左对齐
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = stringResource(R.string.goal_achieved),
                    fontSize = 19.sp, // 增加20%：16sp * 1.2 = 19.2sp
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF8F00)
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // 右上角信息按钮（减小点击区域避免冲突）
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "查看昨日详细数据",
                    modifier = Modifier
                        .size(20.dp) // 减小尺寸避免与按钮冲突
                        .clickable { 
                            android.util.Log.d("YesterdayRewardModule", "点击信息图标，显示昨日详情")
                            onInfoClick() 
                        },
                    tint = Color(0xFFFF8F00).copy(alpha = 0.7f)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp)) // 增加20%：3dp * 1.2 = 3.6dp
            
            // 第二行：奖励内容
            Text(
                text = content,
                fontSize = 19.sp, // 增加20%：16sp * 1.2 = 19.2sp
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            
            Spacer(modifier = Modifier.height(4.dp)) // 增加20%：3dp * 1.2 = 3.6dp
            
            // 第三行：数量
            Text(
                text = quantity,
                fontSize = 19.sp, // 增加20%：16sp * 1.2 = 19.2sp
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            
            Spacer(modifier = Modifier.height(6.dp)) // 增加20%：5dp * 1.2 = 6dp
            
            // 第四行：单个按钮
            CompactActionButton(
                text = stringResource(R.string.reward_button),
                onClick = onRewardComplete,
                backgroundColor = Color(0xFFFF8F00),
                showCheckIcon = isCompleted,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * 昨日惩罚模块
 */
@Composable
private fun YesterdayPunishmentModule(
    punishText: String,
    isCompleted: Boolean,
    onPunishmentComplete: () -> Unit,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 解析惩罚文本，提取内容和数量
    val (content, quantity) = parseRewardPunishmentText(punishText)
    
    Card(
        modifier = modifier
            .width(102.dp) // 增加20%：85dp * 1.2 = 102dp
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE) // 浅红色背景
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 第一行：未达标状态 - 靠左对齐
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = stringResource(R.string.goal_not_achieved),
                    fontSize = 19.sp, // 增加20%：16sp * 1.2 = 19.2sp
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD32F2F)
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // 右上角信息按钮（减小点击区域避免冲突）
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "查看昨日详细数据",
                    modifier = Modifier
                        .size(20.dp) // 减小尺寸避免与按钮冲突
                        .clickable { 
                            android.util.Log.d("YesterdayPunishmentModule", "点击信息图标，显示昨日详情")
                            onInfoClick() 
                        },
                    tint = Color(0xFFD32F2F).copy(alpha = 0.7f)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp)) // 增加20%：3dp * 1.2 = 3.6dp
            
            // 第二行：惩罚内容
            Text(
                text = content,
                fontSize = 19.sp, // 增加20%：16sp * 1.2 = 19.2sp
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            
            Spacer(modifier = Modifier.height(4.dp)) // 增加20%：3dp * 1.2 = 3.6dp
            
            // 第三行：数量
            Text(
                text = quantity,
                fontSize = 19.sp, // 增加20%：16sp * 1.2 = 19.2sp
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            
            Spacer(modifier = Modifier.height(6.dp)) // 增加20%：5dp * 1.2 = 6dp
            
            // 第四行：单个按钮
            CompactActionButton(
                text = stringResource(R.string.punishment_button),
                onClick = onPunishmentComplete,
                backgroundColor = Color(0xFFD32F2F),
                showCheckIcon = isCompleted,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * 昨日无数据模块
 */
@Composable
private fun NoDataModule(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(96.dp) // 增加20%：80dp * 1.2 = 96dp
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5) // 灰色背景
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "📊",
                fontSize = 24.sp // 增加20%：20.sp * 1.2 = 24sp
            )
            
            Spacer(modifier = Modifier.height(7.dp)) // 增加20%：6dp * 1.2 = 7.2dp
            
            Text(
                text = stringResource(R.string.no_data_yesterday),
                fontSize = 19.sp, // 增加20%：16sp * 1.2 = 19.2sp
                fontWeight = FontWeight.Medium,
                color = Color(0xFF757575),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 完成确认模块
 */
@Composable
private fun CompletionConfirmModule(
    isReward: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(96.dp) // 增加20%：80dp * 1.2 = 96dp
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (isReward) Color(0xFFE8F5E8) else Color(0xFFFFF3E0) // 奖励用绿色，惩罚用橙色背景
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isReward) "✅" else "⚠️", // 奖励用对勾，惩罚用警告图标
                fontSize = 24.sp // 增加20%：20.sp * 1.2 = 24sp
            )
            
            Spacer(modifier = Modifier.height(7.dp)) // 增加20%：6dp * 1.2 = 7.2dp
            
            Text(
                text = if (isReward) "昨日奖励完成" else "昨日惩罚完成",
                fontSize = 19.sp, // 增加20%：16sp * 1.2 = 19.2sp
                fontWeight = FontWeight.Medium,
                color = if (isReward) Color(0xFF2E7D32) else Color(0xFFFF8F00), // 奖励用绿色，惩罚用橙色文字
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 点击完成按钮（原滑动完成按钮）
 */
@Composable
private fun ClickToCompleteButton(
    onComplete: () -> Unit,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = {
            android.util.Log.d("ClickButton", "点击完成按钮")
            onComplete()
        },
        modifier = modifier
            .height(28.dp) // 进一步减小按钮高度：32.dp -> 28.dp
            .fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(14.dp), // 减小圆角：16.dp -> 14.dp
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        // 使用对号图标代替文字
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "完成",
            modifier = Modifier.size(16.dp),
            tint = Color.White
        )
    }
}

@Composable
private fun PieChart(
    realUsageSec: Int,
    virtualUsageSec: Int, 
    goalSec: Int,
    categoryName: String = "娱乐",
    modifier: Modifier = Modifier
) {
    // 获取响应式尺寸参数
    val dimensions = LocalResponsiveDimensions
    
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // 获取分类对应的颜色
        val categoryColor = CategoryUtils.getCategoryColor(categoryName)
        val realColor = categoryColor  // 真实APP使用分类本身的颜色
        val virtualColor = categoryColor.copy(alpha = 0.5f)  // 虚拟APP使用相近的颜色（透明度50%）
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            // 保持和娱乐分类一样的半径大小 - 使用原来的0.8倍半径
            val radius = minOf(size.width, size.height) / 2 * 0.8f // 恢复为0.8f，和娱乐分类一致
            val total = realUsageSec + virtualUsageSec
            
            if (total == 0) {
                // 当没有使用时间时，显示完整的灰色圆圈，使用相同半径
                drawCircle(color = Color.LightGray, radius = radius, center = center)
                return@Canvas
            }
            
            val remaining = (goalSec - total).coerceAtLeast(0)
            var startAngle = -90f
            
            if (remaining > 0) {
                val totalAll = total + remaining
                val realSweep = realUsageSec * 360f / totalAll
                val virtualSweep = virtualUsageSec * 360f / totalAll
                val remainSweep = remaining * 360f / totalAll
                
                // 使用分类对应的颜色绘制真实APP使用部分
                drawArc(color = realColor, startAngle = startAngle, sweepAngle = realSweep, useCenter = true, size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2), topLeft = Offset(center.x - radius, center.y - radius))
                startAngle += realSweep
                
                // 使用相近颜色绘制虚拟APP使用部分
                drawArc(color = virtualColor, startAngle = startAngle, sweepAngle = virtualSweep, useCenter = true, size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2), topLeft = Offset(center.x - radius, center.y - radius))
                startAngle += virtualSweep
                
                drawArc(color = Color.LightGray, startAngle = startAngle, sweepAngle = remainSweep, useCenter = true, size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2), topLeft = Offset(center.x - radius, center.y - radius))
            } else {
                val realSweep = realUsageSec * 360f / total
                val virtualSweep = virtualUsageSec * 360f / total
                
                // 使用分类对应的颜色绘制真实APP使用部分
                drawArc(color = realColor, startAngle = startAngle, sweepAngle = realSweep, useCenter = true, size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2), topLeft = Offset(center.x - radius, center.y - radius))
                // 使用相近颜色绘制虚拟APP使用部分
                drawArc(color = virtualColor, startAngle = startAngle + realSweep, sweepAngle = virtualSweep, useCenter = true, size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2), topLeft = Offset(center.x - radius, center.y - radius))
            }
        }
        
        // 显示使用时间和剩余时间的文字
        val totalUsage = realUsageSec + virtualUsageSec
        val remaining = (goalSec - totalUsage).coerceAtLeast(0)
        
        if (totalUsage > 0) {
            // 计算有颜色部分的中心角度和位置
            val totalAll = if (remaining > 0) totalUsage + remaining else totalUsage
            val usageSweep = totalUsage * 360f / totalAll
            val centerAngle = -90f + usageSweep / 2f // 有颜色部分的中心角度
            
            // 将角度转换为弧度
            val centerAngleRad = Math.toRadians(centerAngle.toDouble())
            
            // 计算标签位置（距离中心约40dp，和娱乐分类保持一致）
            val labelRadius = 40.dp.value
            val offsetX = (kotlin.math.cos(centerAngleRad) * labelRadius).toFloat()
            val offsetY = (kotlin.math.sin(centerAngleRad) * labelRadius).toFloat()
            
            // 已使用时间文字 - 显示在有颜色部分的中心
            val usageHours = totalUsage / 3600f
            Text(
                text = "${String.format("%.1f", usageHours)}h",
                fontSize = dimensions.chartAxisLabelFontSize,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.offset(x = offsetX.dp, y = offsetY.dp)
            )
        }
        
        if (remaining > 0) {
            // 计算灰色部分的中心角度和位置
            val totalAll = totalUsage + remaining
            val usageSweep = totalUsage * 360f / totalAll
            val remainSweep = remaining * 360f / totalAll
            val grayCenterAngle = -90f + usageSweep + remainSweep / 2f // 灰色部分的中心角度
            
            // 将角度转换为弧度
            val grayCenterAngleRad = Math.toRadians(grayCenterAngle.toDouble())
            
            // 计算标签位置（距离中心约50dp，和娱乐分类保持一致）
            val grayLabelRadius = 50.dp.value
            val grayOffsetX = (kotlin.math.cos(grayCenterAngleRad) * grayLabelRadius).toFloat()
            val grayOffsetY = (kotlin.math.sin(grayCenterAngleRad) * grayLabelRadius).toFloat()
            
            // 剩余时间文字 - 显示在灰色部分的中心
            val remainingHours = remaining / 3600f
            Text(
                text = "${String.format("%.1f", remainingHours)}h",
                fontSize = dimensions.chartAxisLabelFontSize,
                color = Color(0xFF666666),
                modifier = Modifier.offset(x = grayOffsetX.dp, y = grayOffsetY.dp)
            )
        }
    }
}

/**
 * 多分类饼图组件（用于总使用分类显示各分类的颜色组成）
 */
@Composable
fun MultiCategoryPieChart(
    categoryUsageData: List<com.offtime.app.ui.viewmodel.HomeViewModel.CategoryUsageItem>,
    goalSec: Int,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = minOf(size.width, size.height) / 2 * 0.8f
            
            // 计算总使用时间
            val totalUsageSeconds = categoryUsageData.sumOf { it.realUsageSec + it.virtualUsageSec }
            
            if (totalUsageSeconds == 0) {
                // 当没有使用时间时，显示完整的灰色圆圈
                drawCircle(color = Color.LightGray, radius = radius, center = center)
                return@Canvas
            }
            
            val remaining = (goalSec - totalUsageSeconds).coerceAtLeast(0)
            val totalAll = totalUsageSeconds + remaining
            var startAngle = -90f
            
            // 绘制各分类的使用时间（分别绘制真实使用和虚拟使用）
            categoryUsageData.forEach { categoryItem ->
                // 绘制真实APP使用部分
                if (categoryItem.realUsageSec > 0) {
                    val realSweepAngle = categoryItem.realUsageSec * 360f / totalAll
                    drawArc(
                        color = categoryItem.color, // 使用分类的标准颜色
                        startAngle = startAngle, 
                        sweepAngle = realSweepAngle, 
                        useCenter = true, 
                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2), 
                        topLeft = Offset(center.x - radius, center.y - radius)
                    )
                    startAngle += realSweepAngle
                }
                
                // 绘制线下活动使用部分
                if (categoryItem.virtualUsageSec > 0) {
                    val virtualSweepAngle = categoryItem.virtualUsageSec * 360f / totalAll
                    drawArc(
                        color = categoryItem.color.copy(alpha = 0.5f), // 使用半透明颜色表示线下活动
                        startAngle = startAngle, 
                        sweepAngle = virtualSweepAngle, 
                        useCenter = true, 
                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2), 
                        topLeft = Offset(center.x - radius, center.y - radius)
                    )
                    startAngle += virtualSweepAngle
                }
            }
            
            // 绘制剩余时间（灰色）
            if (remaining > 0) {
                val remainSweep = remaining * 360f / totalAll
                drawArc(
                    color = Color.LightGray, 
                    startAngle = startAngle, 
                    sweepAngle = remainSweep, 
                    useCenter = true, 
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2), 
                    topLeft = Offset(center.x - radius, center.y - radius)
                )
            }
        }
        
        // 显示使用时间和剩余时间的文字
        val totalUsageSeconds = categoryUsageData.sumOf { it.realUsageSec + it.virtualUsageSec }
        val remaining = (goalSec - totalUsageSeconds).coerceAtLeast(0)
        
        if (totalUsageSeconds > 0) {
            // 计算有颜色部分的中心角度和位置
            val totalAll = if (remaining > 0) totalUsageSeconds + remaining else totalUsageSeconds
            val usageSweep = totalUsageSeconds * 360f / totalAll
            val centerAngle = -90f + usageSweep / 2f // 有颜色部分的中心角度
            
            // 将角度转换为弧度
            val centerAngleRad = Math.toRadians(centerAngle.toDouble())
            
            // 计算标签位置（距离中心约40dp）
            val labelRadius = 40.dp.value
            val offsetX = (kotlin.math.cos(centerAngleRad) * labelRadius).toFloat()
            val offsetY = (kotlin.math.sin(centerAngleRad) * labelRadius).toFloat()
            
            // 已使用时间文字 - 显示在有颜色部分的中心
            val usageHours = totalUsageSeconds / 3600f
            Text(
                text = "${String.format("%.1f", usageHours)}h",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.offset(x = offsetX.dp, y = offsetY.dp)
            )
        }
        
        if (remaining > 0) {
            // 计算灰色部分的中心角度和位置
            val totalAll = totalUsageSeconds + remaining
            val usageSweep = totalUsageSeconds * 360f / totalAll
            val remainSweep = remaining * 360f / totalAll
            val grayCenterAngle = -90f + usageSweep + remainSweep / 2f // 灰色部分的中心角度
            
            // 将角度转换为弧度
            val grayCenterAngleRad = Math.toRadians(grayCenterAngle.toDouble())
            
            // 计算标签位置（距离中心约50dp）
            val grayLabelRadius = 50.dp.value
            val grayOffsetX = (kotlin.math.cos(grayCenterAngleRad) * grayLabelRadius).toFloat()
            val grayOffsetY = (kotlin.math.sin(grayCenterAngleRad) * grayLabelRadius).toFloat()
            
            // 剩余时间文字 - 显示在灰色部分的中心
            val remainingHours = remaining / 3600f
            Text(
                text = "${String.format("%.1f", remainingHours)}h",
                fontSize = 16.sp,
                color = Color(0xFF666666),
                modifier = Modifier.offset(x = grayOffsetX.dp, y = grayOffsetY.dp)
            )
        }
    }
}

/**
 * 24小时使用详情柱状图组件（重构版）
 * 
 * 功能：
 * - 显示每小时的真实使用时间（实心柱子）
 * - 显示每小时的虚拟使用时间（半透明柱子）
 * - 支持多分类显示
 * - 清晰的坐标轴和刻度线
 * - 简洁的代码结构
 * - 支持点击柱子查看该时段应用详情
 * 
 * @param realUsageMinutes 每小时真实使用时间（分钟）
 * @param virtualUsageMinutes 每小时虚拟使用时间（分钟）
 * @param categoryName 分类名称
 * @param categoryHourlyData 多分类小时数据
 * @param onBarClick 点击柱子的回调，参数为小时(0-23)
 * @param modifier Modifier
 */
@Composable
fun HourlyBarChart(
    realUsageMinutes: List<Int>,
    virtualUsageMinutes: List<Int>,
    categoryName: String = "娱乐",
    categoryHourlyData: List<com.offtime.app.ui.viewmodel.HomeViewModel.CategoryHourlyItem> = emptyList(),
    onBarClick: ((hour: Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (categoryName == "总使用" && categoryHourlyData.isNotEmpty()) {
        // 多分类柱状图
        MultiCategoryHourlyBarChart(
            categoryHourlyData = categoryHourlyData,
            onBarClick = onBarClick,
            modifier = modifier
        )
    } else {
        // 单分类柱状图
        SingleCategoryHourlyBarChart(
            realUsageMinutes = realUsageMinutes,
            virtualUsageMinutes = virtualUsageMinutes,
            categoryName = categoryName,
            onBarClick = onBarClick,
            modifier = modifier
        )
    }
}

/**
 * 单分类24小时柱状图
 */
@Composable
private fun SingleCategoryHourlyBarChart(
    realUsageMinutes: List<Int>,
    virtualUsageMinutes: List<Int>,
    categoryName: String,
    onBarClick: ((hour: Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val categoryColor = CategoryUtils.getCategoryColor(categoryName)
    
    Column(modifier = modifier.fillMaxWidth()) {
        // 单位标签
        Text(
            text = "min",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(start = 30.dp, bottom = 4.dp)
        )
        
        // 主图表
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp) // 进一步优化柱状图高度
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        if (onBarClick != null) {
                            // 添加调试日志
                            android.util.Log.d("BarChart", "=== 柱状图点击检测 ===")
                            android.util.Log.d("BarChart", "Canvas尺寸: ${size.width} x ${size.height}")
                            android.util.Log.d("BarChart", "点击位置: ${offset.x}, ${offset.y}")
                            android.util.Log.d("BarChart", "设备类型: ${android.os.Build.MODEL}")
                            
                            // 计算点击位置对应的小时
                            val chartWidth = size.width - 60.dp.toPx()
                            val chartLeft = 50.dp.toPx()
                            val barSpacing = chartWidth / 24f
                            
                            android.util.Log.d("BarChart", "图表区域: 左边距=${chartLeft}, 宽度=${chartWidth}, 柱子间距=${barSpacing}")
                            
                            val x = offset.x
                            if (x >= chartLeft) {
                                val hour = ((x - chartLeft) / barSpacing).toInt()
                                android.util.Log.d("BarChart", "计算得到小时: $hour")
                                
                                if (hour in 0..23) {
                                    // 检查该小时是否有数据
                                    val realMin = realUsageMinutes.getOrNull(hour) ?: 0
                                    val virtualMin = virtualUsageMinutes.getOrNull(hour) ?: 0
                                    val totalMin = realMin + virtualMin
                                    
                                    if (totalMin > 0) {
                                        android.util.Log.d("BarChart", "✅ 点击检测成功: ${hour}点，使用时长: ${totalMin}分钟")
                                        onBarClick(hour)
                                    } else {
                                        android.util.Log.d("BarChart", "❌ 点击检测失败: ${hour}点无使用数据")
                                    }
                                } else {
                                    android.util.Log.d("BarChart", "❌ 点击检测失败: 小时超出范围 $hour")
                                }
                            } else {
                                android.util.Log.d("BarChart", "❌ 点击检测失败: 点击位置在图表外 ${offset.x} < $chartLeft")
                            }
                        }
                    }
                }
        ) {
            val chartWidth = size.width - 60.dp.toPx() // 左侧留Y轴标签空间，右侧留少量边距
            val chartHeight = size.height - 35.dp.toPx() // 减少底部边距，增加图表绘制区域
            val chartLeft = 50.dp.toPx()
            val chartTop = 5.dp.toPx() // 减少顶部边距
            val chartBottom = chartTop + chartHeight
            val chartRight = chartLeft + chartWidth
            
            // 计算数据最大值（每小时的总使用时间）
            val hourlyTotals = (0 until 24).map { hour ->
                (realUsageMinutes.getOrNull(hour) ?: 0) + (virtualUsageMinutes.getOrNull(hour) ?: 0)
            }
            val dataMaxValue = hourlyTotals.maxOrNull() ?: 0
            
            // 调试日志
            android.util.Log.d("HourlyBarChart", "[$categoryName] 每小时数据: ${hourlyTotals.withIndex().filter { it.value > 0 }.map { "${it.index}h:${it.value}min" }}")
            android.util.Log.d("HourlyBarChart", "[$categoryName] 数据最大值: ${dataMaxValue}分钟")
            val maxMinutes = if (dataMaxValue == 0) {
                60
            } else {
                // 智能选择Y轴最大值，确保有适当的留白空间
                val candidateMaxValues = listOf(
                    15, 30, 45, 60, 90, 120, 150, 180, 240, 300, 360, 480, 600, 720, 900, 1200, 1440
                )
                
                // 找到第一个大于等于 (dataMaxValue * 1.2) 的候选值，确保有20%的留白
                val targetValue = (dataMaxValue * 1.2f).toInt()
                candidateMaxValues.find { it >= targetValue } ?: run {
                    // 如果都不够大，则向上取整到小时
                    ((dataMaxValue + 59) / 60) * 60
                }
            }
             
             // 调试日志
             android.util.Log.d("HourlyBarChart", "[$categoryName] Y轴最大值: ${maxMinutes}分钟 (数据最大值: ${dataMaxValue}分钟)")
            
            // 绘制Y轴
            drawLine(
                color = Color.Black,
                start = Offset(chartLeft, chartTop),
                end = Offset(chartLeft, chartBottom),
                strokeWidth = 2.dp.toPx()
            )
            
            // 绘制X轴
            drawLine(
                color = Color.Black,
                start = Offset(chartLeft, chartBottom),
                end = Offset(chartRight, chartBottom),
                strokeWidth = 2.dp.toPx()
            )
            
            // 绘制Y轴刻度和标签
            val ySteps = 5
            for (i in 0..ySteps) {
                val value = (maxMinutes * i / ySteps)
                val y = chartBottom - (chartHeight * i / ySteps)
                
                // 刻度线
                drawLine(
                    color = Color.Black,
                    start = Offset(chartLeft - 5.dp.toPx(), y),
                    end = Offset(chartLeft, y),
                    strokeWidth = 1.dp.toPx()
                )
                
                // 网格线
                if (i > 0) {
                    drawLine(
                        color = Color.LightGray,
                        start = Offset(chartLeft, y),
                        end = Offset(chartRight, y),
                        strokeWidth = 0.5.dp.toPx()
                    )
                }
                
                // Y轴标签
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(
                        value.toString(),
                        chartLeft - 10.dp.toPx(),
                        y + 4.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 16.sp.toPx()
                            textAlign = android.graphics.Paint.Align.RIGHT
                        }
                    )
                }
            }
            
            // 绘制柱状图
            val barWidth = chartWidth / 24f * 0.6f
            val barSpacing = chartWidth / 24f
            
            for (hour in 0 until 24) {
                val x = chartLeft + hour * barSpacing + (barSpacing - barWidth) / 2
                
                // X轴刻度线（每2小时一个）
                if (hour % 2 == 0) {
                    drawLine(
                        color = Color.Black,
                        start = Offset(x + barWidth / 2, chartBottom),
                        end = Offset(x + barWidth / 2, chartBottom + 5.dp.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )
                    
                    // X轴标签
                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawText(
                            hour.toString(),
                            x + barWidth / 2,
                            chartBottom + 20.dp.toPx(), // 增加2dp距离
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.GRAY
                                textSize = 16.sp.toPx()
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                        )
                    }
                    
                    // 垂直网格线
                    if (hour > 0) {
                        drawLine(
                            color = Color.LightGray,
                            start = Offset(x + barWidth / 2, chartTop),
                            end = Offset(x + barWidth / 2, chartBottom),
                            strokeWidth = 0.5.dp.toPx()
                        )
                    }
                }
                
                val realMin = realUsageMinutes.getOrNull(hour) ?: 0
                val virtualMin = virtualUsageMinutes.getOrNull(hour) ?: 0
                val totalMin = realMin + virtualMin
                
                if (totalMin > 0) {
                    // 计算高度
                    val realHeight = (realMin.toFloat() / maxMinutes) * chartHeight
                    val virtualHeight = (virtualMin.toFloat() / maxMinutes) * chartHeight
                    
                    // 真实使用（底部，实心）
                    if (realMin > 0) {
                        drawRect(
                            color = categoryColor,
                            topLeft = Offset(x, chartBottom - realHeight),
                            size = androidx.compose.ui.geometry.Size(barWidth, realHeight)
                        )
                    }
                    
                    // 虚拟使用（顶部，半透明）
                    if (virtualMin > 0) {
                        drawRect(
                            color = categoryColor.copy(alpha = 0.5f),
                            topLeft = Offset(x, chartBottom - realHeight - virtualHeight),
                            size = androidx.compose.ui.geometry.Size(barWidth, virtualHeight)
                        )
                    }
                }
            }
        }
        

        }
}

/**
 * 多分类24小时柱状图
 */
@Composable
private fun MultiCategoryHourlyBarChart(
    categoryHourlyData: List<com.offtime.app.ui.viewmodel.HomeViewModel.CategoryHourlyItem>,
    onBarClick: ((hour: Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // 单位标签
        Text(
            text = "min",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(start = 30.dp, bottom = 4.dp)
        )
        
        // 主图表
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp) // 进一步优化柱状图高度
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        if (onBarClick != null) {
                            // 添加调试日志
                            android.util.Log.d("BarChart", "=== 多分类柱状图点击检测 ===")
                            android.util.Log.d("BarChart", "Canvas尺寸: ${size.width} x ${size.height}")
                            android.util.Log.d("BarChart", "点击位置: ${offset.x}, ${offset.y}")
                            android.util.Log.d("BarChart", "设备类型: ${android.os.Build.MODEL}")
                            
                            // 计算点击位置对应的小时
                            val chartWidth = size.width - 60.dp.toPx()
                            val chartLeft = 50.dp.toPx()
                            val barSpacing = chartWidth / 24f
                            
                            android.util.Log.d("BarChart", "图表区域: 左边距=${chartLeft}, 宽度=${chartWidth}, 柱子间距=${barSpacing}")
                            
                            val x = offset.x
                            if (x >= chartLeft) {
                                val hour = ((x - chartLeft) / barSpacing).toInt()
                                android.util.Log.d("BarChart", "计算得到小时: $hour")
                                
                                if (hour in 0..23) {
                                    // 检查该小时是否有数据
                                    val totalMin = categoryHourlyData.sumOf { category ->
                                        (category.hourlyRealUsage.getOrNull(hour) ?: 0) +
                                        (category.hourlyVirtualUsage.getOrNull(hour) ?: 0)
                                    }
                                    
                                    if (totalMin > 0) {
                                        android.util.Log.d("BarChart", "✅ 多分类点击检测成功: ${hour}点，使用时长: ${totalMin}分钟")
                                        onBarClick(hour)
                                    } else {
                                        android.util.Log.d("BarChart", "❌ 多分类点击检测失败: ${hour}点无使用数据")
                                    }
                                } else {
                                    android.util.Log.d("BarChart", "❌ 多分类点击检测失败: 小时超出范围 $hour")
                                }
                            } else {
                                android.util.Log.d("BarChart", "❌ 多分类点击检测失败: 点击位置在图表外 ${offset.x} < $chartLeft")
                            }
                        }
                    }
                }
        ) {
            val chartWidth = size.width - 60.dp.toPx() // 左侧留Y轴标签空间，右侧留少量边距
            val chartHeight = size.height - 35.dp.toPx() // 减少底部边距，增加图表绘制区域
            val chartLeft = 50.dp.toPx()
            val chartTop = 5.dp.toPx() // 减少顶部边距
            val chartBottom = chartTop + chartHeight
            val chartRight = chartLeft + chartWidth
            
            // 计算每小时各分类的总使用时间
            val hourlyTotals = Array(24) { hour ->
                categoryHourlyData.sumOf { category ->
                    (category.hourlyRealUsage.getOrNull(hour) ?: 0) +
                    (category.hourlyVirtualUsage.getOrNull(hour) ?: 0)
                }
            }
            
            val maxMinutes = hourlyTotals.maxOrNull()?.let { dataMaxValue ->
                if (dataMaxValue == 0) {
                    60
                } else {
                    // 智能选择Y轴最大值，确保有适当的留白空间
                    val candidateMaxValues = listOf(
                        15, 30, 45, 60, 90, 120, 150, 180, 240, 300, 360, 480, 600, 720, 900, 1200, 1440
                    )
                    
                    // 找到第一个大于等于 (dataMaxValue * 1.2) 的候选值，确保有20%的留白
                    val targetValue = (dataMaxValue * 1.2f).toInt()
                    candidateMaxValues.find { it >= targetValue } ?: run {
                        // 如果都不够大，则向上取整到小时
                        ((dataMaxValue + 59) / 60) * 60
                    }
                }
            } ?: 60
            
            // 绘制坐标轴
            drawLine(
                color = Color.Black,
                start = Offset(chartLeft, chartTop),
                end = Offset(chartLeft, chartBottom),
                strokeWidth = 2.dp.toPx()
            )
            
            drawLine(
                color = Color.Black,
                start = Offset(chartLeft, chartBottom),
                end = Offset(chartRight, chartBottom),
                strokeWidth = 2.dp.toPx()
            )
            
            // Y轴刻度和标签
            val ySteps = 5
            for (i in 0..ySteps) {
                val value = (maxMinutes * i / ySteps)
                val y = chartBottom - (chartHeight * i / ySteps)
                
                drawLine(
                    color = Color.Black,
                    start = Offset(chartLeft - 5.dp.toPx(), y),
                    end = Offset(chartLeft, y),
                    strokeWidth = 1.dp.toPx()
                )
                
                if (i > 0) {
                    drawLine(
                        color = Color.LightGray,
                        start = Offset(chartLeft, y),
                        end = Offset(chartRight, y),
                        strokeWidth = 0.5.dp.toPx()
                    )
                }
                
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(
                        value.toString(),
                        chartLeft - 10.dp.toPx(),
                        y + 4.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 16.sp.toPx()
                            textAlign = android.graphics.Paint.Align.RIGHT
                        }
                    )
                }
            }
            
            // 绘制堆叠柱状图
            val barWidth = chartWidth / 24f * 0.6f
            val barSpacing = chartWidth / 24f
            
            for (hour in 0 until 24) {
                val x = chartLeft + hour * barSpacing + (barSpacing - barWidth) / 2
                
                // X轴刻度线和标签
                if (hour % 2 == 0) {
                    drawLine(
                        color = Color.Black,
                        start = Offset(x + barWidth / 2, chartBottom),
                        end = Offset(x + barWidth / 2, chartBottom + 5.dp.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )
                    
                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawText(
                            hour.toString(),
                            x + barWidth / 2,
                            chartBottom + 20.dp.toPx(), // 增加2dp距离
                            android.graphics.Paint().apply {
                                color = android.graphics.Color.GRAY
                                textSize = 16.sp.toPx()
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                        )
                    }
                    
                    if (hour > 0) {
                        drawLine(
                            color = Color.LightGray,
                            start = Offset(x + barWidth / 2, chartTop),
                            end = Offset(x + barWidth / 2, chartBottom),
                            strokeWidth = 0.5.dp.toPx()
                        )
                    }
                }
                
                // 绘制堆叠的分类柱子
                var currentBottom = chartBottom
                categoryHourlyData.forEach { category ->
                    val realMin = category.hourlyRealUsage.getOrNull(hour) ?: 0
                    val virtualMin = category.hourlyVirtualUsage.getOrNull(hour) ?: 0
                    
                    // 真实使用
                    if (realMin > 0) {
                        val realHeight = (realMin.toFloat() / maxMinutes) * chartHeight
                        drawRect(
                            color = category.color,
                            topLeft = Offset(x, currentBottom - realHeight),
                            size = androidx.compose.ui.geometry.Size(barWidth, realHeight)
                        )
                        currentBottom -= realHeight
                    }
                    
                    // 虚拟使用
                    if (virtualMin > 0) {
                        val virtualHeight = (virtualMin.toFloat() / maxMinutes) * chartHeight
                        drawRect(
                            color = category.color.copy(alpha = 0.5f),
                            topLeft = Offset(x, currentBottom - virtualHeight),
                            size = androidx.compose.ui.geometry.Size(barWidth, virtualHeight)
                        )
                        currentBottom -= virtualHeight
                    }
                }
            }
        }
        

    }
}

/**
 * 解析奖罚文本，分离内容和数量
 */
private fun parseRewardPunishmentText(text: String): Pair<String, String> {
    return try {
        // 使用正则表达式匹配：内容 + 数字 + 单位
        val regex = """^([^\d]+)(\d+.*)$""".toRegex()
        val matchResult = regex.find(text)
        
        if (matchResult != null) {
            val content = matchResult.groupValues[1].trim()
            val quantity = matchResult.groupValues[2].trim()
            Pair(content, quantity)
        } else {
            // 如果解析失败，返回原文本和空字符串
            Pair(text, "")
        }
    } catch (e: Exception) {
        // 异常情况下返回原文本
        Pair(text, "")
    }
}

/**
 * 紧凑的操作按钮
 */
@Composable
private fun CompactActionButton(
    text: String,
    onClick: () -> Unit,
    backgroundColor: Color,
    showCheckIcon: Boolean = false,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = {
            android.util.Log.d("CompactActionButton", "点击$text 按钮")
            onClick()
        },
        modifier = modifier
            .height(28.dp), // 从32dp减少到28dp，缩小按钮高度
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(10.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 1.dp,
            pressedElevation = 2.dp
        ),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 3.dp) // 减少内边距
    ) {
        if (showCheckIcon) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "已完成",
                modifier = Modifier.size(16.dp),
                tint = Color.White
            )
        } else {
            Text(
                text = text,
                fontSize = 16.sp, // 从16sp减少到14sp，减少2sp
                fontWeight = FontWeight.Bold
            )
        }
    }
} 