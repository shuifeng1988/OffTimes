package com.offtime.app.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import com.offtime.app.ui.viewmodel.StatsViewModel
import com.offtime.app.ui.viewmodel.RewardPunishmentSummaryData
import com.offtime.app.ui.viewmodel.CategoryRewardPunishmentData
import com.offtime.app.data.entity.AppCategoryEntity
import com.offtime.app.utils.DateLocalizer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.offtime.app.R

/**
 * 奖励详情卡片
 * 显示不同时间段各分类的奖励完成情况
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardDetailsCard(
    summaryData: List<RewardPunishmentSummaryData>,
    categories: List<AppCategoryEntity>,
    selectedPeriod: String,
    isLoading: Boolean,
    onPeriodSelected: (String) -> Unit,
    categoryRewardPunishmentData: List<CategoryRewardPunishmentData>,
    dimensions: com.offtime.app.ui.theme.ResponsiveDimensions,
    modifier: Modifier = Modifier
) {
    var showInfoDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<AppCategoryEntity?>(null) }
    val periods = listOf("昨天", "本周", "本月", "总共")
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF0FFEC) // 浅绿色背景，突出奖励主题
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 标题和图标
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎁",
                        fontSize = dimensions.chartAxisTitleFontSize,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = stringResource(R.string.stats_reward_details_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333)
                    )
                }
                
                // 信息按钮
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF4CAF50))
                        .clickable { showInfoDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "i",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            
            // 时间段选择器 - 独立一行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                CompactPeriodSelector(
                    periods = periods,
                    selectedPeriod = selectedPeriod,
                    onPeriodSelected = onPeriodSelected
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 分类按钮网格
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                CategoryRewardGrid(
                    categories = categories,
                    summaryData = summaryData,
                    categoryRewardPunishmentData = categoryRewardPunishmentData,
                    selectedPeriod = selectedPeriod,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 奖励汇总信息区域
                RewardSummarySection(
                    summaryData = summaryData,
                    selectedPeriod = selectedPeriod,
                    selectedCategory = selectedCategory
                )
            }
        }
    }
    
    // 信息对话框
    if (showInfoDialog) {
        InfoDialog(
            title = stringResource(R.string.info_reward_details_title),
            content = stringResource(R.string.info_reward_details_content),
            onDismiss = { showInfoDialog = false }
        )
    }
}

/**
 * 惩罚详情卡片
 * 显示不同时间段各分类的惩罚完成情况
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PunishmentDetailsCard(
    summaryData: List<RewardPunishmentSummaryData>,
    categories: List<AppCategoryEntity>,
    selectedPeriod: String,
    isLoading: Boolean,
    onPeriodSelected: (String) -> Unit,
    categoryRewardPunishmentData: List<CategoryRewardPunishmentData>,
    dimensions: com.offtime.app.ui.theme.ResponsiveDimensions,
    modifier: Modifier = Modifier
) {
    var showInfoDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<AppCategoryEntity?>(null) }
    val periods = listOf("昨天", "本周", "本月", "总共")
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF4E5) // 浅橙色背景，突出惩罚主题
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 标题和图标
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚡",
                        fontSize = dimensions.chartAxisTitleFontSize,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = stringResource(R.string.stats_punishment_details_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333)
                    )
                }
                
                // 信息按钮
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFF8F00))
                        .clickable { showInfoDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "i",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            
            // 时间段选择器 - 独立一行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                CompactPeriodSelector(
                    periods = periods,
                    selectedPeriod = selectedPeriod,
                    onPeriodSelected = onPeriodSelected
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 分类按钮网格
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                CategoryPunishmentGrid(
                    categories = categories,
                    summaryData = summaryData,
                    categoryRewardPunishmentData = categoryRewardPunishmentData,
                    selectedPeriod = selectedPeriod,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 惩罚汇总信息区域
                PunishmentSummarySection(
                    summaryData = summaryData,
                    selectedPeriod = selectedPeriod,
                    selectedCategory = selectedCategory
                )
            }
        }
    }
    
    // 信息对话框
    if (showInfoDialog) {
        InfoDialog(
            title = stringResource(R.string.info_punishment_details_title),
            content = stringResource(R.string.info_punishment_details_content),
            onDismiss = { showInfoDialog = false }
        )
    }
}

/**
 * 简洁的时间段选择器 - 文本样式
 */
@Composable
private fun CompactPeriodSelector(
    periods: List<String>,
    selectedPeriod: String,
    onPeriodSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        periods.forEach { period ->
            CompactPeriodButton(
                text = DateLocalizer.formatPeriodLabel(context, period),
                isSelected = period == selectedPeriod,
                onClick = { onPeriodSelected(period) }
            )
        }
    }
}

/**
 * 简洁的时间段按钮 - 字体大小改为16sp
 */
@Composable
private fun CompactPeriodButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val textColor = if (isSelected) Color(0xFF4CAF50) else Color(0xFF666666)
    
    Text(
        text = text,
        color = textColor,
        fontSize = 16.sp, // 从12sp增加到16sp
        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

/**
 * 分类奖励网格 - 改为横向圆形按钮，添加选中状态
 */
@Composable
private fun CategoryRewardGrid(
    categories: List<AppCategoryEntity>,
    summaryData: List<RewardPunishmentSummaryData>,
    categoryRewardPunishmentData: List<CategoryRewardPunishmentData>,
    selectedPeriod: String,
    selectedCategory: AppCategoryEntity?,
    onCategorySelected: (AppCategoryEntity?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        categories.forEach { category ->
            val categoryData = summaryData.find { 
                it.categoryName == category.name && it.period == selectedPeriod 
            }
            
            CategoryRewardCircularButton(
                category = category,
                rewardData = categoryData,
                categoryRewardPunishmentData = categoryRewardPunishmentData,
                summaryData = summaryData,
                selectedPeriod = selectedPeriod,
                isSelected = selectedCategory?.id == category.id,
                onCategorySelected = onCategorySelected
            )
        }
    }
}

/**
 * 分类惩罚网格 - 改为横向圆形按钮，添加选中状态
 */
@Composable
private fun CategoryPunishmentGrid(
    categories: List<AppCategoryEntity>,
    summaryData: List<RewardPunishmentSummaryData>,
    categoryRewardPunishmentData: List<CategoryRewardPunishmentData>,
    selectedPeriod: String,
    selectedCategory: AppCategoryEntity?,
    onCategorySelected: (AppCategoryEntity?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        categories.forEach { category ->
            val categoryData = summaryData.find { 
                it.categoryName == category.name && it.period == selectedPeriod 
            }
            
            CategoryPunishmentCircularButton(
                category = category,
                punishmentData = categoryData,
                categoryRewardPunishmentData = categoryRewardPunishmentData,
                summaryData = summaryData,
                selectedPeriod = selectedPeriod,
                isSelected = selectedCategory?.id == category.id,
                onCategorySelected = onCategorySelected
            )
        }
    }
}

/**
 * 奖励汇总信息区域 - 支持显示选中的分类
 */
@Composable
private fun RewardSummarySection(
    summaryData: List<RewardPunishmentSummaryData>,
    selectedPeriod: String,
    selectedCategory: AppCategoryEntity?,
    modifier: Modifier = Modifier
) {
    val periodData = if (selectedCategory != null) {
        // 如果选中了分类，只显示该分类的数据
        summaryData.filter { it.period == selectedPeriod && it.categoryName == selectedCategory.name }
    } else {
        // 如果没有选中分类，显示所有分类的数据
        summaryData.filter { it.period == selectedPeriod }
    }
    
    if (periodData.isEmpty()) {
        // 没有数据时显示提示
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF8F9FA))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            val categoryText = if (selectedCategory != null) " ${DateLocalizer.getCategoryName(LocalContext.current, selectedCategory.name)}" else ""
            Text(
                text = stringResource(R.string.stats_no_reward_in_period, categoryText),
                fontSize = 16.sp,
                color = Color(0xFF666666)
            )
        }
        return
    }
    
    // 计算汇总数据
    val totalExpectedRewardCount = periodData.sumOf { it.expectedRewardCount }
    val totalRewardCount = periodData.sumOf { it.rewardCount }
    val rewardCategories = periodData.filter { it.expectedRewardCount > 0 }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题 - 根据是否选中分类显示不同文本
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🎁",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(end = 6.dp)
                )
                val titleText = if (selectedCategory != null) {
                    stringResource(R.string.stats_reward_summary_category, 
                        DateLocalizer.formatPeriodLabel(LocalContext.current, selectedPeriod), 
                        DateLocalizer.getCategoryName(LocalContext.current, selectedCategory.name))
                } else {
                    stringResource(R.string.stats_reward_summary, 
                        DateLocalizer.formatPeriodLabel(LocalContext.current, selectedPeriod))
                }
                Text(
                    text = titleText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (totalExpectedRewardCount > 0) {
                // 奖励次数统计：完成次数/应获得次数
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.reward_completed_count),
                        fontSize = 16.sp,
                        color = Color(0xFF333333)
                    )
                    Text(
                        text = stringResource(R.string.reward_completion_ratio, totalRewardCount, totalExpectedRewardCount),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 各分类详情（如果没有选中特定分类）或选中分类的详情
                if (selectedCategory == null && rewardCategories.size > 1) {
                    // 显示多个分类的详情
                    rewardCategories.forEach { data ->
                        // 从rewardTotal中提取实际完成数量
                        val actualAmount = extractActualAmount(data.rewardTotal)
                        // 计算应完成的奖励内容数量：应获得奖励次数 × 每次奖励基准数量
                        val baseAmountPerReward = if (data.rewardCount > 0 && actualAmount > 0) {
                            actualAmount / data.rewardCount // 每次完成的平均数量
                        } else {
                            1 // 默认每次1个单位（如1包薯片）
                        }
                        val expectedAmount = data.expectedRewardCount * baseAmountPerReward
                        val completionRate = if (expectedAmount > 0) {
                            String.format("%.0f", (actualAmount.toFloat() / expectedAmount.toFloat()) * 100)
                        } else {
                            "0"
                        }
                        val rewardType = DateLocalizer.localizeRewardText(LocalContext.current, data.rewardContent)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = DateLocalizer.getCategoryName(LocalContext.current, data.categoryName),
                                fontSize = 16.sp,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(0.8f),
                                textAlign = TextAlign.Start
                            )
                            Text(
                                text = "$actualAmount/$expectedAmount/${completionRate}%",
                                fontSize = 16.sp,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1.4f),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = rewardType,
                                fontSize = 16.sp,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(0.8f),
                                textAlign = TextAlign.End
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                } else if (rewardCategories.isNotEmpty()) {
                    // 显示单个分类的详细信息
                    val data = rewardCategories.first()
                    // 从rewardTotal中提取实际完成数量
                    val actualAmount = extractActualAmount(data.rewardTotal)
                    // 计算应完成的奖励内容数量：应获得奖励次数 × 每次奖励基准数量
                    // 这里使用实际数量作为基准，如果没有实际完成记录则使用默认值
                    val baseAmountPerReward = if (data.rewardCount > 0 && actualAmount > 0) {
                        actualAmount / data.rewardCount // 每次完成的平均数量
                    } else {
                        1 // 默认每次1个单位（如1包薯片）
                    }
                    val expectedAmount = data.expectedRewardCount * baseAmountPerReward
                    val completionRate = if (expectedAmount > 0) {
                        String.format("%.0f", (actualAmount.toFloat() / expectedAmount.toFloat()) * 100)
                    } else {
                        "0"
                    }
                    val rewardType = DateLocalizer.localizeRewardText(LocalContext.current, data.rewardContent)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = DateLocalizer.getCategoryName(LocalContext.current, data.categoryName),
                            fontSize = 16.sp,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(0.8f),
                            textAlign = TextAlign.Start
                        )
                        Text(
                            text = "$actualAmount/$expectedAmount/${completionRate}%",
                            fontSize = 16.sp,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1.4f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = rewardType,
                            fontSize = 16.sp,
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(0.8f),
                            textAlign = TextAlign.End
                        )
                    }
                }
            } else {
                val categoryText = if (selectedCategory != null) DateLocalizer.getCategoryName(LocalContext.current, selectedCategory.name) else ""
                Text(
                    text = stringResource(R.string.stats_no_reward_in_period, categoryText),
                    fontSize = 16.sp,
                    color = Color(0xFF999999),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * 惩罚汇总信息区域 - 支持显示选中的分类
 */
@Composable
private fun PunishmentSummarySection(
    summaryData: List<RewardPunishmentSummaryData>,
    selectedPeriod: String,
    selectedCategory: AppCategoryEntity?,
    modifier: Modifier = Modifier
) {
    val periodData = if (selectedCategory != null) {
        // 如果选中了分类，只显示该分类的数据
        summaryData.filter { it.period == selectedPeriod && it.categoryName == selectedCategory.name }
    } else {
        // 如果没有选中分类，显示所有分类的数据
        summaryData.filter { it.period == selectedPeriod }
    }
    
    if (periodData.isEmpty()) {
        // 没有数据时显示提示
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF8F9FA))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            val categoryText = if (selectedCategory != null) " ${DateLocalizer.getCategoryName(LocalContext.current, selectedCategory.name)}" else ""
            Text(
                text = stringResource(R.string.stats_no_punishment_in_period, categoryText),
                fontSize = 16.sp,
                color = Color(0xFF666666)
            )
        }
        return
    }
    
    // 计算汇总数据
    val totalExpectedPunishCount = periodData.sumOf { it.expectedPunishCount }
    val totalPunishCount = periodData.sumOf { it.punishCount }
    val punishCategories = periodData.filter { it.expectedPunishCount > 0 }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题 - 根据是否选中分类显示不同文本
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚡",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(end = 6.dp)
                )
                val titleText = if (selectedCategory != null) {
                    stringResource(R.string.stats_punishment_summary_category, 
                        DateLocalizer.formatPeriodLabel(LocalContext.current, selectedPeriod), 
                        DateLocalizer.getCategoryName(LocalContext.current, selectedCategory.name))
                } else {
                    stringResource(R.string.stats_punishment_summary, 
                        DateLocalizer.formatPeriodLabel(LocalContext.current, selectedPeriod))
                }
                Text(
                    text = titleText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF8F00)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (totalExpectedPunishCount > 0) {
                // 惩罚次数统计：完成次数/应获得次数
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.punishment_completed_count),
                        fontSize = 16.sp,
                        color = Color(0xFF333333)
                    )
                    Text(
                        text = stringResource(R.string.punishment_completion_ratio, totalPunishCount, totalExpectedPunishCount),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF8F00)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 各分类详情（如果没有选中特定分类）或选中分类的详情
                if (selectedCategory == null && punishCategories.size > 1) {
                    // 显示多个分类的详情
                    punishCategories.forEach { data ->
                        // 从punishTotal中提取实际完成数量
                        val actualAmount = extractActualAmount(data.punishTotal)
                        // 计算应完成的惩罚内容数量：应获得惩罚次数 × 每次惩罚基准数量
                        val baseAmountPerPunishment = if (data.punishCount > 0 && actualAmount > 0) {
                            actualAmount / data.punishCount // 每次完成的平均数量
                        } else {
                            30 // 默认每次30个单位（如30个俯卧撑）
                        }
                        val expectedAmount = data.expectedPunishCount * baseAmountPerPunishment
                        val completionRate = if (expectedAmount > 0) {
                            String.format("%.0f", (actualAmount.toFloat() / expectedAmount.toFloat()) * 100)
                        } else {
                            "0"
                        }
                        val punishmentType = DateLocalizer.localizePunishmentText(LocalContext.current, data.punishContent)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = DateLocalizer.getCategoryName(LocalContext.current, data.categoryName),
                                fontSize = 16.sp,
                                color = Color(0xFFFF8F00),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(0.8f),
                                textAlign = TextAlign.Start
                            )
                            Text(
                                text = "$actualAmount/$expectedAmount/${completionRate}%",
                                fontSize = 16.sp,
                                color = Color(0xFFFF8F00),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1.4f),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = punishmentType,
                                fontSize = 16.sp,
                                color = Color(0xFFFF8F00),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(0.8f),
                                textAlign = TextAlign.End
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                } else if (punishCategories.isNotEmpty()) {
                    // 显示单个分类的详细信息
                    val data = punishCategories.first()
                    // 从punishTotal中提取实际完成数量
                    val actualAmount = extractActualAmount(data.punishTotal)
                    // 计算应完成的惩罚内容数量：应获得惩罚次数 × 每次惩罚基准数量
                    // 对于惩罚，特别是学习/健身类别，每次未达标应该惩罚的数量更大
                    val baseAmountPerPunishment = if (data.punishCount > 0 && actualAmount > 0) {
                        actualAmount / data.punishCount // 每次完成的平均数量
                    } else {
                        30 // 默认每次30个单位（如30个俯卧撑）
                    }
                    val expectedAmount = data.expectedPunishCount * baseAmountPerPunishment
                    val completionRate = if (expectedAmount > 0) {
                        String.format("%.0f", (actualAmount.toFloat() / expectedAmount.toFloat()) * 100)
                    } else {
                        "0"
                    }
                    val punishmentType = DateLocalizer.localizePunishmentText(LocalContext.current, data.punishContent)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = DateLocalizer.getCategoryName(LocalContext.current, data.categoryName),
                            fontSize = 16.sp,
                            color = Color(0xFFFF8F00),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(0.8f),
                            textAlign = TextAlign.Start
                        )
                        Text(
                            text = "$actualAmount/$expectedAmount/${completionRate}%",
                            fontSize = 16.sp,
                            color = Color(0xFFFF8F00),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1.4f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = punishmentType,
                            fontSize = 16.sp,
                            color = Color(0xFFFF8F00),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(0.8f),
                            textAlign = TextAlign.End
                        )
                    }
                }
            } else {
                val categoryText = if (selectedCategory != null) DateLocalizer.getCategoryName(LocalContext.current, selectedCategory.name) else ""
                Text(
                    text = stringResource(R.string.stats_no_punishment_in_period, categoryText),
                    fontSize = 16.sp,
                    color = Color(0xFF999999),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * 分类奖励圆形按钮
 */
@Composable
private fun CategoryRewardCircularButton(
    category: AppCategoryEntity,
    rewardData: RewardPunishmentSummaryData?,
    categoryRewardPunishmentData: List<CategoryRewardPunishmentData>,
    summaryData: List<RewardPunishmentSummaryData>,
    selectedPeriod: String,
    isSelected: Boolean,
    onCategorySelected: (AppCategoryEntity?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDetails by remember { mutableStateOf(false) }
    val hasReward = rewardData?.rewardCount ?: 0 > 0
    
    // 查找当前分类的奖罚完成度数据
    val rewardPunishmentInfo = categoryRewardPunishmentData.find { it.category.id == category.id }
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(
                    color = when {
                        isSelected -> Color(0xFF4CAF50) // 选中状态：深绿色
                        hasReward -> Color(0xFFE8F5E8) // 有奖励：浅绿色背景
                        else -> Color(0xFFE0E0E0) // 无奖励：稍深灰色背景，表示可点击
                }
            )
            .clickable { 
                    // 允许点击任何分类按钮，无论是否有奖励数据
                    if (isSelected) {
                        onCategorySelected(null) // 取消选中
                    } else {
                        onCategorySelected(category) // 选中当前分类
                }
            },
        contentAlignment = Alignment.Center
    ) {
            // 分类图标 - 选中时图标更突出
        Text(
            text = com.offtime.app.utils.CategoryUtils.getCategoryStyle(category.name).emoji,
                fontSize = if (isSelected) 30.sp else 28.sp, // 选中时图标稍大
                modifier = if (isSelected) {
                    Modifier.graphicsLayer(alpha = 1f) // 选中时图标完全不透明
                } else {
                    Modifier
                }
            )
            
            // 移除图标右上角的数字显示
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // 分类名称
        Text(
            text = DateLocalizer.getCategoryName(LocalContext.current, category.name),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF333333),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(2.dp))
        
        // 奖励完成度信息
        if (rewardPunishmentInfo != null) {
            when {
                rewardPunishmentInfo.rewardCompletionRate == -1f -> {
                    // 开关关闭
                    Text(
                        text = "---",
                        fontSize = 16.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
                rewardPunishmentInfo.rewardCompletionRate == -2f -> {
                    // 奖励内容为空
                    Text(
                        text = "NA",
                        fontSize = 16.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
                else -> {
                    // 显示真实的奖励完成度：已领取次数:目标达成次数
                    val categoryData = summaryData.find { 
                        it.categoryName == category.name && it.period == selectedPeriod 
                    }
                    
                    val rewardDone = categoryData?.rewardCount ?: 0  // 已领取奖励次数
                    val expectedRewards = categoryData?.expectedRewardCount ?: 0  // 应获得奖励次数
                    
                    Text(
                        text = "${rewardDone}:${expectedRewards}",
                        fontSize = 16.sp,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Text(
                text = "---",
                fontSize = 16.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
    
    // 详情对话框
    if (showDetails && rewardData != null) {
        RewardDetailsDialog(
            data = rewardData,
            onDismiss = { showDetails = false }
        )
    }
}

/**
 * 分类惩罚圆形按钮
 */
@Composable
private fun CategoryPunishmentCircularButton(
    category: AppCategoryEntity,
    punishmentData: RewardPunishmentSummaryData?,
    categoryRewardPunishmentData: List<CategoryRewardPunishmentData>,
    summaryData: List<RewardPunishmentSummaryData>,
    selectedPeriod: String,
    isSelected: Boolean,
    onCategorySelected: (AppCategoryEntity?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDetails by remember { mutableStateOf(false) }
    val hasPunishment = punishmentData?.punishCount ?: 0 > 0
    
    // 查找当前分类的奖罚完成度数据
    val rewardPunishmentInfo = categoryRewardPunishmentData.find { it.category.id == category.id }
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(
                    color = when {
                        isSelected -> Color(0xFFFF8F00) // 选中状态：深橙色
                        hasPunishment -> Color(0xFFFFE8D6) // 有惩罚：浅橙色背景
                        else -> Color(0xFFE0E0E0) // 无惩罚：稍深灰色背景，表示可点击
                }
            )
            .clickable { 
                    // 允许点击任何分类按钮，无论是否有惩罚数据
                    if (isSelected) {
                        onCategorySelected(null) // 取消选中
                    } else {
                        onCategorySelected(category) // 选中当前分类
                }
            },
        contentAlignment = Alignment.Center
    ) {
            // 分类图标 - 选中时图标更突出
        Text(
            text = com.offtime.app.utils.CategoryUtils.getCategoryStyle(category.name).emoji,
                fontSize = if (isSelected) 30.sp else 28.sp, // 选中时图标稍大
                modifier = if (isSelected) {
                    Modifier.graphicsLayer(alpha = 1f) // 选中时图标完全不透明
                } else {
                    Modifier
                }
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // 分类名称
        Text(
            text = DateLocalizer.getCategoryName(LocalContext.current, category.name),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF333333),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(2.dp))
        
        // 惩罚完成度信息
        if (rewardPunishmentInfo != null) {
            when {
                rewardPunishmentInfo.punishmentCompletionRate == -1f -> {
                    // 开关关闭
                    Text(
                        text = "---",
                        fontSize = 16.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
                rewardPunishmentInfo.punishmentCompletionRate == -2f -> {
                    // 惩罚内容为空
                    Text(
                        text = "NA",
                        fontSize = 16.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
                else -> {
                    // 显示真实的惩罚完成度：已执行次数:需要执行次数
                    val categoryData = summaryData.find { 
                        it.categoryName == category.name && it.period == selectedPeriod 
                    }
                    
                    val punishmentDone = categoryData?.punishCount ?: 0  // 已执行惩罚次数
                    val expectedPunishments = categoryData?.expectedPunishCount ?: 0  // 应获得惩罚次数
                    
                    Text(
                        text = "${punishmentDone}:${expectedPunishments}",
                        fontSize = 16.sp,
                        color = Color(0xFFFF9800),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Text(
                text = "---",
                fontSize = 16.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
    
    // 详情对话框
    if (showDetails && punishmentData != null) {
        PunishmentDetailsDialog(
            data = punishmentData,
            onDismiss = { showDetails = false }
        )
    }
}

/**
 * 奖励详情对话框
 */
@Composable
private fun RewardDetailsDialog(
    data: RewardPunishmentSummaryData,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🎁", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${DateLocalizer.formatPeriodLabel(LocalContext.current, data.period)} · ${DateLocalizer.getCategoryName(LocalContext.current, data.categoryName)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.expected_rewards_count, data.expectedRewardCount),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.completed_rewards_count, data.rewardCount),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${stringResource(R.string.reward_content)}: ${DateLocalizer.localizeRewardText(LocalContext.current, data.rewardContent)}",
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${stringResource(R.string.total_obtained)}: ${data.rewardTotal}",
                    fontSize = 16.sp,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.dialog_confirm),
                    fontSize = 16.sp
                )
            }
        }
    )
}

/**
 * 惩罚详情对话框
 */
@Composable
private fun PunishmentDetailsDialog(
    data: RewardPunishmentSummaryData,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "⚡", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${DateLocalizer.formatPeriodLabel(LocalContext.current, data.period)} · ${DateLocalizer.getCategoryName(LocalContext.current, data.categoryName)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.expected_punishments_count, data.expectedPunishCount),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.completed_punishments_count, data.punishCount),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = DateLocalizer.localizePunishmentText(LocalContext.current, data.punishContent),
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${stringResource(R.string.total_completed)}: ${data.punishTotal}",
                    fontSize = 16.sp,
                    color = Color(0xFFFF8F00),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.dialog_confirm),
                    fontSize = 16.sp
                )
            }
        }
    )
}

/**
 * 信息对话框
 */
@Composable
private fun InfoDialog(
    title: String,
    content: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = content,
                fontSize = 16.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.dialog_confirm),
                    fontSize = 16.sp
                )
            }
        }
    )
} 



/**
 * 从奖惩总量字符串中提取实际数量
 * 
 * @param totalString 如"30个俯卧撑"或"30 Push-ups"
 * @return 提取的数量，如果提取失败返回0
 */
private fun extractActualAmount(totalString: String): Int {
    return Regex("\\d+").find(totalString)?.value?.toIntOrNull() ?: 0
} 