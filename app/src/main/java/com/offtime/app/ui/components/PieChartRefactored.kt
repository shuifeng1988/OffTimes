package com.offtime.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import com.offtime.app.R
import com.offtime.app.utils.CategoryUtils
import com.offtime.app.utils.OptimizedScalingUtils
import com.offtime.app.utils.adaptiveSpacing
import com.offtime.app.utils.adaptiveChartHeight
import com.offtime.app.utils.SpacingLevel
import com.offtime.app.utils.ChartType
import com.offtime.app.ui.viewmodel.HomeViewModel
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.atan2

/**
 * 重构后的饼图容器组件
 * 
 * 功能完全等价于原来的PieChartWithCards，但架构更清晰，维护更简单。
 * 
 * 主要改进：
 * 1. 将18个参数封装为3个数据类
 * 2. 将1421行代码拆分为10个独立组件
 * 3. 使用新的适配系统OptimizedScalingUtils
 * 4. 每个组件职责单一，状态控制清晰
 */

// ============ 数据类定义 ============

/**
 * 使用数据状态
 */
data class UsageState(
    val realUsageSec: Int = 0,
    val virtualUsageSec: Int = 0,
    val goalSec: Int = 7200,
    val categoryId: Int = -1, // 分类ID
    val categoryName: String = "娱乐",
    val goalConditionType: Int = 0, // 0=≤目标算完成(娱乐类), 1=≥目标算完成(学习类)
    val categoryUsageData: List<HomeViewModel.CategoryUsageItem> = emptyList()
) {
    val totalUsageSec: Int get() = realUsageSec + virtualUsageSec
    val remainingSec: Int get() = goalSec - totalUsageSec  // 移除coerceAtLeast(0)，允许负值表示超出
    val isOverGoal: Boolean get() = totalUsageSec > goalSec
    val isEntertainmentType: Boolean get() = goalConditionType == 0
}

/**
 * 昨日奖罚状态
 */
sealed class YesterdayRewardPunishmentState {
    object NoData : YesterdayRewardPunishmentState()
    object NoRewardPunishment : YesterdayRewardPunishmentState() // 奖罚系统关闭
    
    data class PendingReward(
        val rewardText: String,
        val onComplete: () -> Unit,
        val onInfoClick: () -> Unit,
        val isCompleted: Boolean = false,
        val completionPercent: Int = 0 // 新增：完成百分比
    ) : YesterdayRewardPunishmentState()
    
    data class PendingPunishment(
        val punishText: String,
        val onComplete: () -> Unit,
        val onInfoClick: () -> Unit,
        val isCompleted: Boolean = false,
        val completionPercent: Int = 0 // 新增：完成百分比
    ) : YesterdayRewardPunishmentState()
    
    data class RewardCompleted(
        val rewardText: String
    ) : YesterdayRewardPunishmentState()
    
    data class PunishmentCompleted(
        val punishText: String
    ) : YesterdayRewardPunishmentState()
}

/**
 * 图表配置
 */
data class ChartConfig(
    val showMultiCategory: Boolean = false,
    val realUsageMinutes: List<Int> = emptyList(),
    val virtualUsageMinutes: List<Int> = emptyList(),
    val categoryHourlyData: List<HomeViewModel.CategoryHourlyItem> = emptyList()
)

// ============ 主容器组件 ============

/**
 * 重构后的饼图容器组件 - 替代原来的PieChartWithCards
 */
@Composable
fun RefactoredPieChart(
    usageState: UsageState,
    yesterdayState: YesterdayRewardPunishmentState,
    isPunishLoading: Boolean = false,
    @Suppress("UNUSED_PARAMETER") chartConfig: ChartConfig = ChartConfig(),
    onCategoryClick: ((categoryId: Int, categoryName: String) -> Unit)? = null,
    // 线下计时器状态
    isTimerRunning: Boolean = false,
    @Suppress("UNUSED_PARAMETER") isTimerPaused: Boolean = false,
    isTimerInBackground: Boolean = false,
    isCurrentCategoryTiming: Boolean = false,
    timerHours: Int = 0,
    timerMinutes: Int = 0,
    timerSeconds: Int = 0,
    onShowTimerDialog: () -> Unit = {},
    onTimerBackgroundClick: () -> Unit = {},
    // CountDown相关状态
    isCountdownTimerRunning: Boolean = false,
    @Suppress("UNUSED_PARAMETER") isCountdownTimerPaused: Boolean = false,
    isCountdownInBackground: Boolean = false,
    isCurrentCategoryCountdownTiming: Boolean = false,
    countdownHours: Int = 0,
    countdownMinutes: Int = 30,
    countdownSecondsUnit: Int = 0,
    onCountdownBackgroundClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // 智能布局系统 - 避免复杂缩放导致的黑屏问题
    val chartSize = com.offtime.app.utils.ScalingFactorUtils.intelligentScaledDp(180.dp)  // 使用智能缩放因子
    
    // 简化偏移距离计算
    val pieRadius = chartSize * 0.4f  // 饼图实际半径（72dp）
    val moduleOffset = pieRadius - com.offtime.app.utils.ScalingFactorUtils.intelligentScaledDp(20.dp)  // 使用智能缩放
    
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 饼图容器
        Box(
            modifier = Modifier.size(chartSize),
            contentAlignment = Alignment.Center
        ) {
            // 主饼图 - 背景层
            PieChartCore(
                usageState = usageState,
                onCategoryClick = onCategoryClick,
                modifier = Modifier.fillMaxSize()
            )
            
            // 左侧状态模块 - 移到饼图左侧外部，额外左移30dp (原20dp + 新增10dp)
            YesterdayStateModule(
                state = yesterdayState,
                isPunishLoading = isPunishLoading,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = -moduleOffset - com.offtime.app.utils.ScalingFactorUtils.intelligentScaledDp(30.dp))
            )
            
            // 右侧计时器模块 - 在饼图右边，与左边奖罚模块对称
            OfflineTimerModule(
                isTimerRunning = isTimerRunning,
                isTimerPaused = isTimerPaused,
                isTimerInBackground = isTimerInBackground,
                isCurrentCategoryTiming = isCurrentCategoryTiming,
                timerHours = timerHours,
                timerMinutes = timerMinutes,
                timerSeconds = timerSeconds,
                // CountDown相关状态
                isCountdownTimerRunning = isCountdownTimerRunning,
                isCountdownTimerPaused = isCountdownTimerPaused,
                isCountdownInBackground = isCountdownInBackground,
                isCurrentCategoryCountdownTiming = isCurrentCategoryCountdownTiming,
                countdownHours = countdownHours,
                countdownMinutes = countdownMinutes,
                countdownSecondsUnit = countdownSecondsUnit,
                onStartTimer = onShowTimerDialog,
                onBackgroundClick = onTimerBackgroundClick,
                onCountdownBackgroundClick = onCountdownBackgroundClick,
                modifier = Modifier
                    .align(Alignment.CenterEnd)  // 饼图右边，与左边奖罚模块对称
                    .offset(x = moduleOffset + 30.dp)  // 向右偏移，再额外右移30dp
            )
            
            // 移除CountDown按钮 - 现在通过Timer界面的Tab来访问
        }
        
        // 饼图与文字间距 - 使用智能缩放
        Spacer(modifier = Modifier.height(com.offtime.app.utils.ScalingFactorUtils.intelligentScaledDp(8.dp)))
        
        // 底部文字提示
        UsageStatusText(
            usageState = usageState,
            modifier = Modifier.fillMaxWidth()
        )
        
        // 底部文字提示与日周月按钮间距 - 使用智能缩放
        Spacer(modifier = Modifier.height(com.offtime.app.utils.ScalingFactorUtils.intelligentScaledDp(13.dp)))
    }
}

// ============ 核心饼图组件 ============

/**
 * 核心饼图绘制组件
 * 只负责饼图的绘制，不包含其他UI元素
 */
@Composable
private fun PieChartCore(
    usageState: UsageState,
    onCategoryClick: ((categoryId: Int, categoryName: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (usageState.categoryName == "总使用" && usageState.categoryUsageData.isNotEmpty()) {
            // 多分类饼图
            RefactoredMultiCategoryPieChart(
                categoryUsageData = usageState.categoryUsageData,
                goalSec = usageState.goalSec,
                onCategoryClick = onCategoryClick
            )
        } else {
            // 单分类饼图
            SingleCategoryPieChart(
                usageState = usageState,
                onCategoryClick = onCategoryClick
            )
        }
    }
}

/**
 * 单分类饼图
 */
@Composable
private fun SingleCategoryPieChart(
    usageState: UsageState,
    onCategoryClick: ((categoryId: Int, categoryName: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // 获取分类颜色
        val categoryColor = CategoryUtils.getCategoryColor(usageState.categoryName)
        val realColor = categoryColor
        val virtualColor = categoryColor.copy(alpha = 0.5f)
        
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(usageState) {
                    detectTapGestures { offset ->
                        onCategoryClick?.let { callback ->
                            // 添加调试日志
                            android.util.Log.d("PieChart", "=== 单分类饼图点击检测 ===")
                            android.util.Log.d("PieChart", "Canvas尺寸: ${size.width} x ${size.height}")
                            android.util.Log.d("PieChart", "点击位置: ${offset.x}, ${offset.y}")
                            android.util.Log.d("PieChart", "设备类型: ${android.os.Build.MODEL}")
                            
                            // 计算点击位置对应的分类
                            val clickedCategory = calculateClickedSingleCategory(
                                offset = offset,
                                size = androidx.compose.ui.geometry.Size(size.width.toFloat(), size.height.toFloat()),
                                usageState = usageState
                            )
                            clickedCategory?.let { (categoryId, categoryName) ->
                                android.util.Log.d("PieChart", "✅ 点击检测成功: $categoryName (ID: $categoryId)")
                                callback(categoryId, categoryName)
                            } ?: run {
                                android.util.Log.d("PieChart", "❌ 点击检测失败: 无有效分类")
                            }
                        }
                    }
                }
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = minOf(size.width, size.height) / 2 * 0.8f
            
            android.util.Log.d("PieChart", "Canvas绘制: center=$center, radius=$radius")
            
            if (usageState.totalUsageSec == 0) {
                // 无使用时间 - 显示灰色圆圈
                drawCircle(color = Color.LightGray, radius = radius, center = center)
                return@Canvas
            }
            
            drawPieSlices(
                center = center,
                radius = radius,
                realUsageSec = usageState.realUsageSec,
                virtualUsageSec = usageState.virtualUsageSec,
                goalSec = usageState.goalSec,
                realColor = realColor,
                virtualColor = virtualColor
            )
        }
        
        // 使用时间标签
        UsageLabels(usageState = usageState)
    }
}

/**
 * 多分类饼图
 */
@Composable
private fun RefactoredMultiCategoryPieChart(
    categoryUsageData: List<HomeViewModel.CategoryUsageItem>,
    goalSec: Int,
    onCategoryClick: ((categoryId: Int, categoryName: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val labelFontSize = com.offtime.app.utils.ScalingFactorUtils.intelligentScaledSp(14.sp)
    
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(categoryUsageData, goalSec) {
                    detectTapGestures { offset ->
                        onCategoryClick?.let { callback ->
                            // 添加调试日志
                            android.util.Log.d("PieChart", "=== 多分类饼图点击检测 ===")
                            android.util.Log.d("PieChart", "Canvas尺寸: ${size.width} x ${size.height}")
                            android.util.Log.d("PieChart", "点击位置: ${offset.x}, ${offset.y}")
                            android.util.Log.d("PieChart", "设备类型: ${android.os.Build.MODEL}")
                            android.util.Log.d("PieChart", "分类数量: ${categoryUsageData.size}")
                            
                            // 计算点击位置对应的分类
                            val clickedCategory = calculateClickedCategory(
                                offset = offset,
                                size = androidx.compose.ui.geometry.Size(size.width.toFloat(), size.height.toFloat()),
                                categoryUsageData = categoryUsageData,
                                goalSec = goalSec
                            )
                            clickedCategory?.let { (categoryId, categoryName) ->
                                android.util.Log.d("PieChart", "✅ 点击检测成功: $categoryName (ID: $categoryId)")
                                callback(categoryId, categoryName)
                            } ?: run {
                                android.util.Log.d("PieChart", "❌ 点击检测失败: 无有效分类")
                            }
                        }
                    }
                }
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = minOf(size.width, size.height) / 2 * 0.8f
            
            android.util.Log.d("PieChart", "Canvas绘制: center=$center, radius=$radius")
            
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
            
            // 计算标签位置（距离中心约40dp + 智能缩放）
            val labelRadius = com.offtime.app.utils.ScalingFactorUtils.intelligentScaledDp(40.dp).value
            val offsetX = (kotlin.math.cos(centerAngleRad) * labelRadius).toFloat()
            val offsetY = (kotlin.math.sin(centerAngleRad) * labelRadius).toFloat()
            
            // 已使用时间文字 - 显示在有颜色部分的中心
            val usageHours = totalUsageSeconds / 3600f
            Text(
                text = "${String.format("%.1f", usageHours)}h",
                fontSize = labelFontSize,
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
            
            // 计算标签位置（距离中心约50dp + 智能缩放）
            val grayLabelRadius = com.offtime.app.utils.ScalingFactorUtils.intelligentScaledDp(50.dp).value
            val grayOffsetX = (kotlin.math.cos(grayCenterAngleRad) * grayLabelRadius).toFloat()
            val grayOffsetY = (kotlin.math.sin(grayCenterAngleRad) * grayLabelRadius).toFloat()
            
            // 剩余时间文字 - 显示在灰色部分的中心
            val remainingHours = remaining / 3600f
            Text(
                text = "${String.format("%.1f", remainingHours)}h",
                fontSize = labelFontSize,
                color = Color(0xFF666666),
                modifier = Modifier.offset(x = grayOffsetX.dp, y = grayOffsetY.dp)
            )
        }
    }
}

/**
 * 饼图扇形绘制
 */
private fun DrawScope.drawPieSlices(
    center: Offset,
    radius: Float,
    realUsageSec: Int,
    virtualUsageSec: Int,
    goalSec: Int,
    realColor: Color,
    virtualColor: Color
) {
    val totalUsage = realUsageSec + virtualUsageSec
    val remaining = (goalSec - totalUsage).coerceAtLeast(0)
    var startAngle = -90f
    
    if (remaining > 0) {
        // 还有剩余时间
        val totalAll = totalUsage + remaining
        val realSweep = realUsageSec * 360f / totalAll
        val virtualSweep = virtualUsageSec * 360f / totalAll
        val remainSweep = remaining * 360f / totalAll
        
        // 绘制真实使用
        drawArc(
            color = realColor,
            startAngle = startAngle,
            sweepAngle = realSweep,
            useCenter = true,
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            topLeft = Offset(center.x - radius, center.y - radius)
        )
        startAngle += realSweep
        
        // 绘制虚拟使用
        drawArc(
            color = virtualColor,
            startAngle = startAngle,
            sweepAngle = virtualSweep,
            useCenter = true,
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            topLeft = Offset(center.x - radius, center.y - radius)
        )
        startAngle += virtualSweep
        
        // 绘制剩余部分
        drawArc(
            color = Color.LightGray,
            startAngle = startAngle,
            sweepAngle = remainSweep,
            useCenter = true,
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            topLeft = Offset(center.x - radius, center.y - radius)
        )
    } else {
        // 已超出目标
        val realSweep = realUsageSec * 360f / totalUsage
        val virtualSweep = virtualUsageSec * 360f / totalUsage
        
        drawArc(
            color = realColor,
            startAngle = startAngle,
            sweepAngle = realSweep,
            useCenter = true,
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            topLeft = Offset(center.x - radius, center.y - radius)
        )
        
        drawArc(
            color = virtualColor,
            startAngle = startAngle + realSweep,
            sweepAngle = virtualSweep,
            useCenter = true,
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            topLeft = Offset(center.x - radius, center.y - radius)
        )
    }
}

/**
 * 使用时间标签
 */
@Composable
private fun UsageLabels(
    usageState: UsageState,
    modifier: Modifier = Modifier
) {
    val scalingUtils = OptimizedScalingUtils
    val labelFontSize = scalingUtils.adaptiveFontSize(14.sp)
    
    Box(modifier = modifier) {
        // 已使用时间标签
        if (usageState.totalUsageSec > 0) {
            val totalAll = if (usageState.remainingSec > 0) 
                usageState.totalUsageSec + usageState.remainingSec 
                else usageState.totalUsageSec
            val usageSweep = usageState.totalUsageSec * 360f / totalAll
            val centerAngle = -90f + usageSweep / 2f
            
            val centerAngleRad = Math.toRadians(centerAngle.toDouble())
            val labelRadius = com.offtime.app.utils.ScalingFactorUtils.uniformScaledDp(40.dp).value
            val offsetX = (cos(centerAngleRad) * labelRadius).toFloat()
            val offsetY = (sin(centerAngleRad) * labelRadius).toFloat()
            
            val usageHours = usageState.totalUsageSec / 3600f
            Text(
                text = "${String.format("%.1f", usageHours)}h",
                fontSize = labelFontSize,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.offset(x = offsetX.dp, y = offsetY.dp)
            )
        }
        
        // 剩余时间标签
        if (usageState.remainingSec > 0) {
            val totalAll = usageState.totalUsageSec + usageState.remainingSec
            val usageSweep = usageState.totalUsageSec * 360f / totalAll
            val remainSweep = usageState.remainingSec * 360f / totalAll
            val grayCenterAngle = -90f + usageSweep + remainSweep / 2f
            
            val grayCenterAngleRad = Math.toRadians(grayCenterAngle.toDouble())
            val grayLabelRadius = com.offtime.app.utils.ScalingFactorUtils.uniformScaledDp(50.dp).value
            val grayOffsetX = (cos(grayCenterAngleRad) * grayLabelRadius).toFloat()
            val grayOffsetY = (sin(grayCenterAngleRad) * grayLabelRadius).toFloat()
            
            val remainingHours = usageState.remainingSec / 3600f
            Text(
                text = "${String.format("%.1f", remainingHours)}h",
                fontSize = labelFontSize,
                color = Color(0xFF666666),
                modifier = Modifier.offset(x = grayOffsetX.dp, y = grayOffsetY.dp)
            )
        }
    }
}

// ============ 状态模块组件 ============

/**
 * 昨日状态模块 - 左侧（待处理的奖罚和无数据状态）
 */
@Composable
private fun YesterdayStateModule(
    state: YesterdayRewardPunishmentState,
    isPunishLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (isPunishLoading) {
        LoadingPunishmentCard(modifier = modifier)
        return
    }
    when (state) {
        is YesterdayRewardPunishmentState.NoData -> {
            NoDataCard(modifier = modifier)
        }
        is YesterdayRewardPunishmentState.PendingReward -> {
            PendingRewardCard(
                rewardText = state.rewardText,
                onComplete = state.onComplete,
                onInfoClick = state.onInfoClick,
                isCompleted = state.isCompleted,
                completionPercent = state.completionPercent,
                modifier = modifier
            )
        }
        is YesterdayRewardPunishmentState.PendingPunishment -> {
            PendingPunishmentCard(
                punishText = state.punishText,
                onComplete = state.onComplete,
                onInfoClick = state.onInfoClick,
                isCompleted = state.isCompleted,
                completionPercent = state.completionPercent,
                modifier = modifier
            )
        }
        else -> {
            // 其他状态不在左侧显示
        }
    }
}

@Composable
private fun LoadingPunishmentCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .width(75.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = Color(0xFFD32F2F),
                strokeWidth = 2.dp,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.loading),
                fontSize = 14.sp,
                color = Color(0xFFD32F2F),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 线下计时器模块 - 右侧
 */
@Composable
private fun OfflineTimerModule(
    isTimerRunning: Boolean,
    @Suppress("UNUSED_PARAMETER") isTimerPaused: Boolean,
    isTimerInBackground: Boolean,
    isCurrentCategoryTiming: Boolean,
    timerHours: Int,
    timerMinutes: Int,
    timerSeconds: Int,
    // CountDown相关参数
    isCountdownTimerRunning: Boolean = false,
    @Suppress("UNUSED_PARAMETER") isCountdownTimerPaused: Boolean = false,
    isCountdownInBackground: Boolean = false,
    isCurrentCategoryCountdownTiming: Boolean = false,
    countdownHours: Int = 0,
    countdownMinutes: Int = 30,
    countdownSecondsUnit: Int = 0,
    onStartTimer: () -> Unit,
    onBackgroundClick: () -> Unit,
    onCountdownBackgroundClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // 优先显示CountDown背景运行状态
    if (isCurrentCategoryCountdownTiming && isCountdownInBackground && isCountdownTimerRunning) {
        // CountDown背景计时模式 - 显示倒计时时间
        Box(
            modifier = modifier
                .width(80.dp)
                .height(44.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(12.dp),
                    ambientColor = Color(0xFFFF5722).copy(alpha = 0.3f),
                    spotColor = Color(0xFFFF5722).copy(alpha = 0.3f)
                )
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFF7043), // 浅橘红色
                            Color(0xFFFF5722), // 标准橘红色
                            Color(0xFFE64A19)  // 深橘红色
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { onCountdownBackgroundClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = String.format("%02d:%02d:%02d", countdownHours, countdownMinutes, countdownSecondsUnit),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    } else if (isCurrentCategoryTiming && isTimerInBackground && isTimerRunning) {
        // 背景计时模式 - 显示计时时间
        Box(
            modifier = modifier
                .width(80.dp)
                .height(44.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(12.dp),
                    ambientColor = Color(0xFF4CAF50).copy(alpha = 0.3f),
                    spotColor = Color(0xFF4CAF50).copy(alpha = 0.3f)
                )
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF66BB6A), // 浅绿色
                            Color(0xFF4CAF50), // 标准绿色
                            Color(0xFF388E3C)  // 深绿色
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { onBackgroundClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = String.format("%02d:%02d:%02d", timerHours, timerMinutes, timerSeconds),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    } else {
        // 普通模式 - 显示"计时"按钮
        Box(
            modifier = modifier
                .width(80.dp)
                .height(44.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(12.dp),
                    ambientColor = Color(0xFF4CAF50).copy(alpha = 0.3f),
                    spotColor = Color(0xFF4CAF50).copy(alpha = 0.3f)
                )
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF66BB6A), // 浅绿色
                            Color(0xFF4CAF50), // 标准绿色
                            Color(0xFF388E3C)  // 深绿色
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { onStartTimer() },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // 添加计时图标
                Text(
                    text = "⏱️",
                    fontSize = 14.sp
                )
                Text(
                    text = stringResource(R.string.timer_label),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * 定时按钮模块 - 右侧
 */
@Composable
private fun CountdownTimerModule(
    isCountdownTimerRunning: Boolean,
    @Suppress("UNUSED_PARAMETER") isCountdownTimerPaused: Boolean,
    isCountdownInBackground: Boolean,
    isCurrentCategoryCountdownTiming: Boolean,
    countdownHours: Int,
    countdownMinutes: Int,
    countdownSecondsUnit: Int,
    onShowCountdownDialog: () -> Unit,
    onBackgroundClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isCountdownInBackground && isCountdownTimerRunning && isCurrentCategoryCountdownTiming) {
        // 背景计时模式 - 显示倒计时时间
        Box(
            modifier = modifier
                .width(80.dp)
                .height(44.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(12.dp),
                    ambientColor = Color(0xFFFF5722).copy(alpha = 0.3f),
                    spotColor = Color(0xFFFF5722).copy(alpha = 0.3f)
                )
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFF7043), // 浅橘红色
                            Color(0xFFFF5722), // 标准橘红色
                            Color(0xFFE64A19)  // 深橘红色
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { onBackgroundClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = String.format("%02d:%02d:%02d", countdownHours, countdownMinutes, countdownSecondsUnit),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    } else {
        // 普通模式 - 显示"定时"按钮
        Box(
            modifier = modifier
                .width(80.dp)
                .height(44.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(12.dp),
                    ambientColor = Color(0xFFFF5722).copy(alpha = 0.3f),
                    spotColor = Color(0xFFFF5722).copy(alpha = 0.3f)
                )
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFF7043), // 浅橘红色
                            Color(0xFFFF5722), // 标准橘红色
                            Color(0xFFE64A19)  // 深橘红色
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { onShowCountdownDialog() },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // 添加倒计时图标
                Text(
                    text = "⏰",
                    fontSize = 14.sp
                )
                Text(
                    text = stringResource(R.string.countdown_label),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * 无数据卡片
 */
@Composable
private fun NoDataCard(
    modifier: Modifier = Modifier
) {
    val cardWidth = com.offtime.app.utils.ScalingFactorUtils.intelligentScaledDp(60.dp)  // 使用智能缩放
    val cardPadding = com.offtime.app.utils.ScalingFactorUtils.intelligentScaledDp(0.dp)
    val fontSize = com.offtime.app.utils.ScalingFactorUtils.intelligentScaledSp(12.sp)   // 使用智能缩放
    
    Card(
        modifier = modifier
            .width(cardWidth)
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "📊",
                fontSize = 20.sp
            )
            
            Spacer(modifier = Modifier.height(adaptiveSpacing(SpacingLevel.XS)))
            
            Text(
                text = stringResource(R.string.no_data_yesterday),
                fontSize = fontSize,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF757575),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 待领取奖励卡片
 */
@Composable
private fun PendingRewardCard(
    rewardText: String,
    onComplete: () -> Unit,
    onInfoClick: () -> Unit,
    isCompleted: Boolean = false,
    completionPercent: Int = 0, // 新增：完成百分比
    modifier: Modifier = Modifier
) {
    val cardWidth = 75.dp  // 适当增加宽度以容纳14sp按钮文字
    val fontSize = 14.sp   // 保持原来的字体大小
    
    // 解析奖励文本
    val (content, quantity) = parseRewardPunishmentText(rewardText)
    
    Card(
        modifier = modifier
            .width(cardWidth)
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 达标状态和信息按钮 - 居中对齐
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.goal_achieved),
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF8F00)
                )
                
                // 信息按钮 - 与文字同一水平线，大小匹配
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "查看昨日详细数据",
                    modifier = Modifier
                        .size(16.dp) // 调整为与文字大小相符
                        .clickable { onInfoClick() },
                    tint = Color(0xFFFF8F00).copy(alpha = 0.7f)
                )
            }
            
            Spacer(modifier = Modifier.height(adaptiveSpacing(SpacingLevel.SM)))
            
            // 奖励内容
            Text(
                text = content,
                fontSize = fontSize,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            
            Spacer(modifier = Modifier.height(adaptiveSpacing(SpacingLevel.SM)))
            
            // 数量
            Text(
                text = quantity,
                fontSize = fontSize,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            
            Spacer(modifier = Modifier.height(adaptiveSpacing(SpacingLevel.MD)))
            
            // 完成按钮
            CompactActionButton(
                text = stringResource(R.string.reward_button),
                onClick = onComplete,
                backgroundColor = Color(0xFFFF8F00),
                showCheckIcon = isCompleted && completionPercent == 0, // 只有在没有百分比时才显示勾号
                completionPercent = if (completionPercent > 0) completionPercent else null,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * 待执行惩罚卡片
 */
@Composable
private fun PendingPunishmentCard(
    punishText: String,
    onComplete: () -> Unit,
    onInfoClick: () -> Unit,
    isCompleted: Boolean = false,
    completionPercent: Int = 0, // 新增：完成百分比
    modifier: Modifier = Modifier
) {
    val cardWidth = 75.dp  // 适当增加宽度以容纳14sp按钮文字
    val fontSize = 14.sp   // 保持原来的字体大小
    
    // 添加调试日志
    android.util.Log.d("PendingPunishmentCard", "=== 惩罚卡片调试 ===")
    android.util.Log.d("PendingPunishmentCard", "接收到的punishText: '$punishText'")
    
    // 解析惩罚文本
    val (content, quantity) = parseRewardPunishmentText(punishText)
    
    android.util.Log.d("PendingPunishmentCard", "解析结果: content='$content', quantity='$quantity'")
    
    Card(
        modifier = modifier
            .width(cardWidth)
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 未达标状态和信息按钮 - 居中对齐
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.goal_not_achieved),
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD32F2F)
                )
                
                // 信息按钮 - 与文字同一水平线，大小匹配
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "查看昨日详细数据",
                    modifier = Modifier
                        .size(16.dp) // 调整为与文字大小相符
                        .clickable { onInfoClick() },
                    tint = Color(0xFFD32F2F).copy(alpha = 0.7f)
                )
            }
            
            Spacer(modifier = Modifier.height(adaptiveSpacing(SpacingLevel.SM)))
            
            // 惩罚内容
            Text(
                text = content,
                fontSize = fontSize,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            
            Spacer(modifier = Modifier.height(adaptiveSpacing(SpacingLevel.SM)))
            
            // 数量
            Text(
                text = quantity,
                fontSize = fontSize,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            
            Spacer(modifier = Modifier.height(adaptiveSpacing(SpacingLevel.MD)))
            
            // 完成按钮
            CompactActionButton(
                text = stringResource(R.string.punishment_button),
                onClick = onComplete,
                backgroundColor = Color(0xFFD32F2F),
                showCheckIcon = isCompleted && completionPercent == 0, // 只有在没有百分比时才显示勾号
                completionPercent = if (completionPercent > 0) completionPercent else null,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * 已完成卡片
 */
@Composable
private fun CompletedCard(
    isReward: Boolean,
    modifier: Modifier = Modifier
) {
    val cardWidth = com.offtime.app.utils.ScalingFactorUtils.intelligentScaledDp(80.dp)  // 使用智能缩放
    val cardPadding = com.offtime.app.utils.ScalingFactorUtils.intelligentScaledDp(2.dp)  // 使用智能缩放
    val fontSize = com.offtime.app.utils.ScalingFactorUtils.intelligentScaledSp(12.sp)
    
    Card(
        modifier = modifier
            .width(cardWidth)
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (isReward) Color(0xFFE8F5E8) else Color(0xFFFFF3E0)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isReward) "✅" else "⚠️",
                fontSize = 20.sp
            )
            
            Spacer(modifier = Modifier.height(adaptiveSpacing(SpacingLevel.XS)))
            
            Text(
                text = if (isReward) stringResource(R.string.yesterday_reward_completed) else stringResource(R.string.yesterday_punishment_completed),
                fontSize = fontSize,
                fontWeight = FontWeight.Medium,
                color = if (isReward) Color(0xFF2E7D32) else Color(0xFFFF8F00),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ============ 文字提示组件 ============

/**
 * 使用状态文字提示
 */
@Composable
private fun UsageStatusText(
    usageState: UsageState,
    modifier: Modifier = Modifier
) {
    val fontSize = com.offtime.app.utils.ScalingFactorUtils.intelligentScaledSp(16.sp)
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            usageState.goalSec <= 0 -> {
                Text(
                    text = stringResource(R.string.please_set_goal_time),
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            usageState.totalUsageSec <= 0 -> {
                ShowInitialStatusText(usageState, fontSize)
            }
            usageState.remainingSec > 0 -> {
                ShowRemainingTimeText(usageState, fontSize)
            }
            else -> {
                ShowOverGoalText(usageState, fontSize)
            }
        }
    }
}

/**
 * 初始状态文字
 */
@Composable
private fun ShowInitialStatusText(
    usageState: UsageState,
    fontSize: androidx.compose.ui.unit.TextUnit
) {
    val goalHours = usageState.goalSec / 3600f
    
    if (usageState.isEntertainmentType) {
        Text(
            text = stringResource(R.string.today_can_use_hours, String.format("%.1f", goalHours)),
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4CAF50)
        )
    } else {
        Text(
            text = stringResource(R.string.today_need_use_hours, String.format("%.1f", goalHours)),
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFF9800)
        )
    }
}

/**
 * 剩余时间文字
 */
@Composable
private fun ShowRemainingTimeText(
    usageState: UsageState,
    fontSize: androidx.compose.ui.unit.TextUnit
) {
    val remainingHours = usageState.remainingSec / 3600f
    
    if (usageState.isEntertainmentType) {
        Text(
            text = stringResource(R.string.can_still_use_hours, String.format("%.1f", remainingHours)),
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4CAF50)
        )
    } else {
        Text(
            text = stringResource(R.string.still_need_use_hours, String.format("%.1f", remainingHours)),
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFF9800)
        )
    }
}

/**
 * 超出目标文字
 */
@Composable
private fun ShowOverGoalText(
    usageState: UsageState,
    fontSize: androidx.compose.ui.unit.TextUnit
) {
    val overHours = (-usageState.remainingSec) / 3600f
    
    if (usageState.isEntertainmentType) {
        Text(
            text = stringResource(R.string.exceeded_goal_hours, String.format("%.1f", overHours)),
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF44336)
        )
    } else {
        Text(
            text = stringResource(R.string.completed_goal_exceeded, String.format("%.1f", overHours)),
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4CAF50)
        )
    }
}

// ============ 辅助组件和函数 ============

/**
 * 紧凑型操作按钮（支持百分比显示）
 */
@Composable
private fun CompactActionButton(
    text: String,
    onClick: () -> Unit,
    backgroundColor: Color,
    showCheckIcon: Boolean = false,
    completionPercent: Int? = null, // 新增：完成百分比，如果不为null则显示百分比
    modifier: Modifier = Modifier
) {
    val buttonHeight = com.offtime.app.utils.ScalingFactorUtils.intelligentScaledDp(36.dp) // 从40.dp减少10%到36.dp
    
    Button(
        onClick = {
            android.util.Log.d("RefactoredPieChart", "点击$text 按钮")
            onClick()
        },
        modifier = modifier.height(buttonHeight),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(14.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        ),
        contentPadding = PaddingValues(
            horizontal = 8.dp, // 增加水平内边距
            vertical = 4.dp    // 确保垂直居中
        )
    ) {
        when {
            completionPercent != null && completionPercent > 0 -> {
                // 显示完成百分比
                Text(
                    text = "$completionPercent%",
                    fontSize = 16.sp, // 确保字体大小至少16sp
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
            showCheckIcon -> {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "已完成",
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
            }
            else -> {
                Text(
                    text = text,
                    fontSize = 16.sp, // 确保字体大小至少16sp
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 解析奖励惩罚文本，提取内容和数量
 * 支持多种格式：
 * - "薯片1包" -> ("薯片", "1包")
 * - "1 pack Chips" -> ("Chips", "1 pack")  
 * - "俯卧撑30个" -> ("俯卧撑", "30个")
 * - "30 Push-ups" -> ("Push-ups", "30")
 */
private fun parseRewardPunishmentText(text: String): Pair<String, String> {
    android.util.Log.d("ParseRewardPunishment", "解析文本: '$text'")
    
    // 使用统一文本管理器进行解析，确保快速且一致的显示
    val result = com.offtime.app.utils.UnifiedTextManager.parseAndFormatText(text)
    
    android.util.Log.d("ParseRewardPunishment", "解析结果: content='${result.first}', quantity='${result.second}'")
    
    return result
}

// ============ 兼容性包装函数 ============

/**
 * 兼容性包装函数 - 提供与原PieChartWithCards相同的接口
 * 
 * 使用方法：
 * 将原来的 PieChartWithCards(...) 直接替换为 PieChartWithCardsRefactored(...)
 */
@Composable
fun PieChartWithCardsRefactored(
    realUsageSec: Int,
    virtualUsageSec: Int,
    goalSec: Int,
    yesterdayRewardDone: Boolean,
    yesterdayPunishDone: Boolean,
    yesterdayHasData: Boolean,
    yesterdayGoalMet: Boolean,
    categoryId: Int, // 添加真实的categoryId参数
    categoryName: String = "娱乐",
    goalConditionType: Int = 0,
    rewardText: String = "薯片",
    punishText: String = "俯卧撑30个",
    yesterdayRewardPercent: Int = 0, // 新增：昨日奖励完成百分比
    yesterdayPunishPercent: Int = 0, // 新增：昨日惩罚完成百分比
    onRewardComplete: () -> Unit = {},
    onPunishmentComplete: () -> Unit = {},
    onYesterdayInfoClick: () -> Unit = {},
    categoryUsageData: List<HomeViewModel.CategoryUsageItem> = emptyList(),
    isRewardPunishmentEnabled: Boolean = true,
    rewardPunishmentSummary: List<HomeViewModel.RewardPunishmentSummaryData> = emptyList(),
    isPunishLoading: Boolean = false,
    onCategoryClick: ((categoryId: Int, categoryName: String) -> Unit)? = null,
    // 线下计时器状态
    isTimerRunning: Boolean = false,
    @Suppress("UNUSED_PARAMETER") isTimerPaused: Boolean = false,
    isTimerInBackground: Boolean = false,
    isCurrentCategoryTiming: Boolean = false,
    timerHours: Int = 0,
    timerMinutes: Int = 0,
    timerSeconds: Int = 0,
    onShowTimerDialog: () -> Unit = {},
    onTimerBackgroundClick: () -> Unit = {},
    // CountDown相关状态
    isCountdownTimerRunning: Boolean = false,
    @Suppress("UNUSED_PARAMETER") isCountdownTimerPaused: Boolean = false,
    isCountdownInBackground: Boolean = false,
    isCurrentCategoryCountdownTiming: Boolean = false,
    countdownHours: Int = 0,
    countdownMinutes: Int = 30,
    countdownSecondsUnit: Int = 0,
    onCountdownBackgroundClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // 转换为新的数据结构  
    val usageState = UsageState(
        realUsageSec = realUsageSec,
        virtualUsageSec = virtualUsageSec,
        goalSec = goalSec,
        categoryId = categoryId, // 使用传入的真实categoryId
        categoryName = categoryName,
        goalConditionType = goalConditionType,
        categoryUsageData = categoryUsageData
    )
    
    // 从rewardPunishmentSummary中获取昨天的数据
    val yesterdayData = rewardPunishmentSummary.find { it.period == "昨天" && it.categoryName == categoryName }
    
    android.util.Log.d("PieChartWithCardsRefactored", "=== 昨日奖罚状态调试 ===")
    android.util.Log.d("PieChartWithCardsRefactored", "yesterdayHasData: $yesterdayHasData")
    android.util.Log.d("PieChartWithCardsRefactored", "yesterdayGoalMet: $yesterdayGoalMet")
    android.util.Log.d("PieChartWithCardsRefactored", "isRewardPunishmentEnabled: $isRewardPunishmentEnabled")
    android.util.Log.d("PieChartWithCardsRefactored", "rewardText: '$rewardText'")
    android.util.Log.d("PieChartWithCardsRefactored", "punishText: '$punishText'")
    android.util.Log.d("PieChartWithCardsRefactored", "yesterdayData: ${yesterdayData?.punishTotal}")
    
    val yesterdayState = when {
        !yesterdayHasData -> YesterdayRewardPunishmentState.NoData
        !isRewardPunishmentEnabled -> YesterdayRewardPunishmentState.NoRewardPunishment
        yesterdayGoalMet && rewardText.isNotBlank() -> {
            // 显示奖励模块（无论是否已完成）
            YesterdayRewardPunishmentState.PendingReward(
                rewardText = rewardText,
                onComplete = onRewardComplete,
                onInfoClick = onYesterdayInfoClick,
                isCompleted = yesterdayRewardDone,
                completionPercent = yesterdayRewardPercent
            )
        }
        !yesterdayGoalMet && punishText.isNotBlank() -> {
            // 显示惩罚模块（无论是否已完成）
            // 总是使用计算的应惩罚量（punishText），而不是实际完成量（yesterdayData.punishTotal）
            // 这样用户可以看到"应该做30个Push-ups"，而不是"实际完成0个"
            val actualPunishText = punishText
            
            android.util.Log.d("PieChartWithCardsRefactored", "最终使用的惩罚文本: '$actualPunishText'（应惩罚量，非实际完成量）")
            
            YesterdayRewardPunishmentState.PendingPunishment(
                punishText = actualPunishText,
                onComplete = onPunishmentComplete,
                onInfoClick = onYesterdayInfoClick,
                isCompleted = yesterdayPunishDone,
                completionPercent = yesterdayPunishPercent
            )
        }
        else -> YesterdayRewardPunishmentState.NoRewardPunishment
    }
    
    // 使用重构后的组件
    RefactoredPieChart(
        usageState = usageState,
        yesterdayState = yesterdayState,
        isPunishLoading = isPunishLoading,
        onCategoryClick = onCategoryClick,
        // 线下计时器状态和回调
        isTimerRunning = isTimerRunning,
        isTimerPaused = isTimerPaused,
        isTimerInBackground = isTimerInBackground,
        isCurrentCategoryTiming = isCurrentCategoryTiming,
        timerHours = timerHours,
        timerMinutes = timerMinutes,
        timerSeconds = timerSeconds,
        onShowTimerDialog = onShowTimerDialog,
        onTimerBackgroundClick = onTimerBackgroundClick,
        // CountDown相关状态和回调
        isCountdownTimerRunning = isCountdownTimerRunning,
        isCountdownTimerPaused = isCountdownTimerPaused,
        isCountdownInBackground = isCountdownInBackground,
        isCurrentCategoryCountdownTiming = isCurrentCategoryCountdownTiming,
        countdownHours = countdownHours,
        countdownMinutes = countdownMinutes,
        countdownSecondsUnit = countdownSecondsUnit,
        onCountdownBackgroundClick = onCountdownBackgroundClick,
        modifier = modifier
    )
}

// ============ 点击检测辅助函数 ============

/**
 * 计算点击位置对应的分类（多分类饼图专用）
 * @param offset 点击位置
 * @param size Canvas尺寸
 * @param categoryUsageData 分类使用数据
 * @param goalSec 目标时间
 * @return 对应的分类ID和名称，如果点击位置无效则返回null
 */
private fun calculateClickedCategory(
    offset: Offset,
    size: Size,
    categoryUsageData: List<HomeViewModel.CategoryUsageItem>,
    goalSec: Int
): Pair<Int, String>? {
    val center = Offset(size.width / 2, size.height / 2)
    val radius = minOf(size.width, size.height) / 2 * 0.8f
    
    // 检查点击位置是否在饼图区域内
    val distance = sqrt((offset.x - center.x).let { it * it } + (offset.y - center.y).let { it * it })
    if (distance > radius) {
        return null // 点击位置在饼图外部
    }
    
    // 计算总使用时间
    val totalUsageSeconds = categoryUsageData.sumOf { it.realUsageSec + it.virtualUsageSec }
    if (totalUsageSeconds == 0) {
        return null // 没有使用时间，无法确定分类
    }
    
    val remaining = (goalSec - totalUsageSeconds).coerceAtLeast(0)
    val totalAll = totalUsageSeconds + remaining
    
    // 计算点击位置的角度
    val dx = offset.x - center.x
    val dy = offset.y - center.y
    var clickAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    
    // 将角度标准化到[0, 360)范围，并调整起始角度为-90度（顶部）
    clickAngle = (clickAngle + 90f + 360f) % 360f
    
    // 遍历各分类，找出点击位置对应的分类
    var currentStartAngle = 0f
    
    for (categoryItem in categoryUsageData) {
        // 计算该分类的角度范围
        val realSweepAngle = if (categoryItem.realUsageSec > 0) {
            categoryItem.realUsageSec * 360f / totalAll
        } else 0f
        
        val virtualSweepAngle = if (categoryItem.virtualUsageSec > 0) {
            categoryItem.virtualUsageSec * 360f / totalAll
        } else 0f
        
        val totalCategorySweepAngle = realSweepAngle + virtualSweepAngle
        
        // 检查点击角度是否在当前分类的范围内
        if (clickAngle >= currentStartAngle && clickAngle < currentStartAngle + totalCategorySweepAngle) {
            return Pair(categoryItem.categoryId, categoryItem.categoryName)
        }
        
        currentStartAngle += totalCategorySweepAngle
    }
    
    // 检查是否点击在剩余时间（灰色）区域
    if (remaining > 0) {
        val remainSweep = remaining * 360f / totalAll
        if (clickAngle >= currentStartAngle && clickAngle < currentStartAngle + remainSweep) {
            // 点击在剩余时间区域，不触发任何回调
            return null
        }
    }
    
    return null
}

/**
 * 计算点击位置对应的分类（单分类饼图专用）
 * @param offset 点击位置
 * @param size Canvas尺寸
 * @param usageState 使用状态
 * @return 对应的分类ID和名称，如果点击位置无效则返回null
 */
private fun calculateClickedSingleCategory(
    offset: Offset,
    size: Size,
    usageState: UsageState
): Pair<Int, String>? {
    val center = Offset(size.width / 2, size.height / 2)
    val radius = minOf(size.width, size.height) / 2 * 0.8f
    
    // 检查点击位置是否在饼图区域内
    val distance = sqrt((offset.x - center.x).let { it * it } + (offset.y - center.y).let { it * it })
    if (distance > radius) {
        return null // 点击位置在饼图外部
    }
    
    // 检查是否有使用时间
    if (usageState.totalUsageSec == 0) {
        return null // 没有使用时间
    }
    
    // 计算点击位置的角度
    val dx = offset.x - center.x
    val dy = offset.y - center.y
    var clickAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    
    // 将角度标准化到[0, 360)范围，并调整起始角度为-90度（顶部）
    clickAngle = (clickAngle + 90f + 360f) % 360f
    
    val remaining = (usageState.goalSec - usageState.totalUsageSec).coerceAtLeast(0)
    
    if (remaining > 0) {
        // 有剩余时间的情况
        val totalAll = usageState.totalUsageSec + remaining
        val usageSweepAngle = usageState.totalUsageSec * 360f / totalAll
        
        // 检查是否点击在有颜色的使用时间区域
        if (clickAngle < usageSweepAngle) {
            return Pair(usageState.categoryId, usageState.categoryName)
        }
    } else {
        // 已超出目标，整个饼图都是使用时间
        return Pair(usageState.categoryId, usageState.categoryName)
    }
    
    return null
} 