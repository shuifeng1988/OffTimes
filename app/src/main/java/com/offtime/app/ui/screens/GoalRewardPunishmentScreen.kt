package com.offtime.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.offtime.app.R
import com.offtime.app.data.entity.GoalRewardPunishmentUserEntity
import com.offtime.app.ui.viewmodel.GoalCategoryItem
import com.offtime.app.ui.viewmodel.GoalRewardPunishmentViewModel
import com.offtime.app.utils.CategoryUtils
import com.offtime.app.utils.DateLocalizer
import com.offtime.app.utils.DefaultValueLocalizer
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalRewardPunishmentScreen(
    onNavigateBack: () -> Unit,
    viewModel: GoalRewardPunishmentViewModel = hiltViewModel()
) {
    val goalCategoryItems by viewModel.goalCategoryItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadData()
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部工具栏
        TopAppBar(
            title = { 
                Text(
                    text = stringResource(R.string.goals_rewards_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                ) 
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                }
            }
        )
        
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            goalCategoryItems.isEmpty() -> {
                EmptyGoalState()
            }
            else -> {
                GoalRewardPunishmentContent(
                    goalCategoryItems = goalCategoryItems,
                    viewModel = viewModel,
                    onGoalUpdate = { goal -> viewModel.updateGoal(goal) },
                    onCreateGoal = { catId -> viewModel.createGoalForCategory(catId) },
                    getConditionDescription = { conditionType -> viewModel.getConditionDescription(conditionType) }
                )
            }
        }
    }
}

@Composable
private fun EmptyGoalState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.emoji_target),
            fontSize = 64.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.no_goal_data),
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.please_set_categories_first),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GoalRewardPunishmentContent(
    goalCategoryItems: List<GoalCategoryItem>,
    viewModel: GoalRewardPunishmentViewModel,
    onGoalUpdate: (GoalRewardPunishmentUserEntity) -> Unit,
    onCreateGoal: (Int) -> Unit,
    getConditionDescription: (Int) -> String
) {
    val defaultCategoryId by viewModel.defaultCategoryId.collectAsState()
    val categoryRewardPunishmentEnabled by viewModel.categoryRewardPunishmentEnabled.collectAsState()
    
    LazyColumn(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // 目标设置区域
        items(goalCategoryItems) { item ->
            GoalCategoryCard(
                item = item,
                defaultCategoryId = defaultCategoryId,
                categoryRewardPunishmentEnabled = categoryRewardPunishmentEnabled,
                onGoalUpdate = onGoalUpdate,
                onCreateGoal = onCreateGoal,
                getConditionDescription = getConditionDescription,
                onDefaultCategoryChanged = { viewModel.setDefaultCategory(it) },
                onCategoryRewardPunishmentToggle = { categoryId, enabled -> 
                    viewModel.setCategoryRewardPunishmentEnabled(categoryId, enabled) 
                }
            )
        }
    }
}

@Composable
private fun GoalCategoryCard(
    item: GoalCategoryItem,
    defaultCategoryId: Int,
    categoryRewardPunishmentEnabled: Map<Int, Boolean>,
    onGoalUpdate: (GoalRewardPunishmentUserEntity) -> Unit,
    onCreateGoal: (Int) -> Unit,
    getConditionDescription: (Int) -> String,
    onDefaultCategoryChanged: (Int) -> Unit,
    onCategoryRewardPunishmentToggle: (Int, Boolean) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    val categoryStyle = CategoryUtils.getCategoryStyle(item.category.name)
    val isDefaultCategory = item.category.id == defaultCategoryId
    val isRewardPunishmentEnabled = categoryRewardPunishmentEnabled[item.category.id] ?: true
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 分类标题行，包含默认按钮、开关和编辑按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧：分类名称
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = categoryStyle.emoji,
                        fontSize = 24.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = DateLocalizer.getCategoryName(LocalContext.current, item.category.name),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = categoryStyle.color
                    )
                }
                
                // 右侧：控制按钮组
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 默认分类按钮
                    @OptIn(ExperimentalMaterial3Api::class)
                    FilterChip(
                        selected = isDefaultCategory,
                        onClick = { 
                            if (!isDefaultCategory) {
                                onDefaultCategoryChanged(item.category.id)
                            }
                        },
                        label = {
                            Text(
                                text = stringResource(R.string.default_category),
                                fontSize = 16.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = categoryStyle.color.copy(alpha = 0.2f),
                            selectedLabelColor = categoryStyle.color
                        ),
                        modifier = Modifier.height(32.dp)
                    )
                    
                    // 奖罚显示开关
                    Switch(
                        checked = isRewardPunishmentEnabled,
                        onCheckedChange = { enabled ->
                            onCategoryRewardPunishmentToggle(item.category.id, enabled)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = categoryStyle.color,
                            checkedTrackColor = categoryStyle.color.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.scale(0.8f)
                    )
                    
                    // 编辑按钮
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.action_edit))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (item.goal != null) {
                // 显示目标信息
                GoalInfoDisplay(item.goal, getConditionDescription)
            } else {
                // 没有目标，显示创建按钮
                NoGoalDisplay(onCreateGoal = { onCreateGoal(item.category.id) })
            }
        }
    }
    
    // 编辑对话框
    if (showEditDialog && item.goal != null) {
        GoalEditDialog(
            goal = item.goal,
            onDismiss = { showEditDialog = false },
            onSave = { updatedGoal ->
                onGoalUpdate(updatedGoal)
                showEditDialog = false
            }
        )
    }
}

@Composable
private fun GoalInfoDisplay(
    goal: GoalRewardPunishmentUserEntity,
    getConditionDescription: (Int) -> String
) {
    val context = LocalContext.current
    
    Column {
        // 目标信息
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.target_time),
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${goal.dailyGoalMin} ${DefaultValueLocalizer.localizeTimeUnit(context, goal.goalTimeUnit)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.completion_condition),
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${getConditionDescription(goal.conditionType)} ${stringResource(R.string.target_time_suffix)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = if (goal.conditionType == 0) Color(0xFFFF5722) else Color(0xFF4CAF50)
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))
        
        // 奖励和惩罚
        Text(
            text = "${stringResource(R.string.emoji_reward)} ${stringResource(R.string.completion_reward)}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF4CAF50)
        )
        
        Row(
            modifier = Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.frequency_prefix) + DefaultValueLocalizer.localizeTimeUnit(context, goal.rewardTimeUnit),
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
        Text(
            text = DefaultValueLocalizer.localizeRewardText(context, goal.rewardText),
                fontSize = 16.sp
            )
            
            if (goal.rewardNumber > 0) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)),
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Text(
                        text = goal.rewardNumber.toString(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
                }
                
                if (goal.rewardUnit.isNotBlank()) {
                    Text(
                        text = DefaultValueLocalizer.localizeRewardUnit(context, goal.rewardUnit),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "${stringResource(R.string.emoji_warning)} ${stringResource(R.string.incomplete_punishment)}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFFF5722)
        )
        
        Row(
            modifier = Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.frequency_prefix) + DefaultValueLocalizer.localizeTimeUnit(context, goal.punishTimeUnit),
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
        Text(
            text = DefaultValueLocalizer.localizePunishmentText(context, goal.punishText),
                fontSize = 16.sp
            )
            
            if (goal.punishNumber > 0) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFF5722).copy(alpha = 0.1f)),
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Text(
                        text = goal.punishNumber.toString(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF5722),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
                }
                
                if (goal.punishUnit.isNotBlank()) {
                    Text(
                        text = DefaultValueLocalizer.localizePunishmentUnit(context, goal.punishUnit),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun NoGoalDisplay(onCreateGoal: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.no_goal_set),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onCreateGoal,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.set_goal))
        }
    }
}

@Composable
private fun GoalEditDialog(
    goal: GoalRewardPunishmentUserEntity,
    onDismiss: () -> Unit,
    onSave: (GoalRewardPunishmentUserEntity) -> Unit
) {
    val context = LocalContext.current
    
    /**
     * 获取数据库存储的标准奖励文本（中文版本）
     */
    fun getStandardRewardText(): String {
        return "薯片"
    }
    
    /**
     * 获取数据库存储的标准惩罚文本（中文版本）  
     */
    fun getStandardPunishText(): String {
        return "俯卧撑"
    }
    
    var dailyGoalMin by remember { mutableStateOf(goal.dailyGoalMin.toString()) }
    var goalTimeUnit by remember { mutableStateOf(DefaultValueLocalizer.localizeTimeUnit(context, goal.goalTimeUnit)) }
    var conditionType by remember { mutableStateOf(goal.conditionType) }
    var rewardText by remember { 
        mutableStateOf(
            if (goal.rewardText.isBlank()) {
                DefaultValueLocalizer.localizeRewardText(context, getStandardRewardText())
            } else {
                DefaultValueLocalizer.localizeRewardText(context, goal.rewardText)
            }
        ) 
    }
    var rewardNumber by remember { mutableStateOf(goal.rewardNumber.toString()) }
    var rewardUnit by remember { 
        mutableStateOf(
            if (goal.rewardUnit.isBlank()) {
                DefaultValueLocalizer.localizeRewardUnit(context, "包")
            } else {
                DefaultValueLocalizer.localizeRewardUnit(context, goal.rewardUnit)
            }
        ) 
    }
    var rewardTimeUnit by remember { mutableStateOf(DefaultValueLocalizer.localizeTimeUnit(context, goal.rewardTimeUnit)) }
    var punishText by remember { 
        mutableStateOf(
            if (goal.punishText.isBlank()) {
                DefaultValueLocalizer.localizePunishmentText(context, getStandardPunishText())
            } else {
                DefaultValueLocalizer.localizePunishmentText(context, goal.punishText)
            }
        ) 
    }
    var punishNumber by remember { mutableStateOf(goal.punishNumber.toString()) }
    var punishUnit by remember { 
        mutableStateOf(
            if (goal.punishUnit.isBlank()) {
                DefaultValueLocalizer.localizePunishmentUnit(context, "个")
            } else {
                DefaultValueLocalizer.localizePunishmentUnit(context, goal.punishUnit)
            }
        ) 
    }
    var punishTimeUnit by remember { mutableStateOf(DefaultValueLocalizer.localizeTimeUnit(context, goal.punishTimeUnit)) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_goal)) },
        text = {
            Column {
                // 目标时间设置
                Text(stringResource(R.string.goal_settings), fontWeight = FontWeight.Medium, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                OutlinedTextField(
                    value = dailyGoalMin,
                    onValueChange = { dailyGoalMin = it },
                        label = { 
                            Text(
                                text = stringResource(R.string.quantity), 
                                fontSize = 16.sp,
                                maxLines = 1,
                                softWrap = false
                            ) 
                        },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp),
                        singleLine = true
                    )
                    
                    // 时间单位选择
                    var expanded by remember { mutableStateOf(false) }
                    val timeUnits = listOf(
                        stringResource(R.string.time_unit_hour),
                        stringResource(R.string.time_unit_minute),
                        stringResource(R.string.time_unit_second)
                    )
                    
                    @OptIn(ExperimentalMaterial3Api::class)
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = goalTimeUnit,
                            onValueChange = { },
                            readOnly = true,
                            label = { 
                                Text(
                                    text = stringResource(R.string.unit), 
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    softWrap = false
                                ) 
                            },
                            trailingIcon = { 
                                @OptIn(ExperimentalMaterial3Api::class)
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) 
                            },
                            modifier = @Suppress("DEPRECATION") Modifier.menuAnchor(),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp),
                            singleLine = true
                        )
                        @OptIn(ExperimentalMaterial3Api::class)
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            timeUnits.forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text(unit, fontSize = 16.sp) },
                                    onClick = {
                                        goalTimeUnit = unit
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 完成条件
                Text(stringResource(R.string.completion_condition), fontWeight = FontWeight.Medium, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                
                Row {
                    @OptIn(ExperimentalMaterial3Api::class)
                    FilterChip(
                        selected = conditionType == 0,
                        onClick = { conditionType = 0 },
                        label = { Text(stringResource(R.string.less_than_target), fontSize = 16.sp) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    @OptIn(ExperimentalMaterial3Api::class)
                    FilterChip(
                        selected = conditionType == 1,
                        onClick = { conditionType = 1 },
                        label = { Text(stringResource(R.string.more_than_target), fontSize = 16.sp) }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 奖励设置
                Text(stringResource(R.string.completion_reward), fontWeight = FontWeight.Medium, fontSize = 16.sp, color = Color(0xFF4CAF50))
                Spacer(modifier = Modifier.height(8.dp))
                
                // 第一行：奖励内容
                OutlinedTextField(
                    value = rewardText,
                    onValueChange = { rewardText = it },
                    label = { Text(stringResource(R.string.reward_content), fontSize = 16.sp) },
                    placeholder = { Text(stringResource(R.string.reward_content_placeholder), fontSize = 16.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 第二行：数量、单位、/、频率单位
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // 数量+单位的水平组合
                    Row(
                        modifier = Modifier.weight(1.8f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        OutlinedTextField(
                            value = rewardNumber,
                            onValueChange = { rewardNumber = it },
                            label = { 
                                Text(
                                    text = stringResource(R.string.quantity), 
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    softWrap = false
                                ) 
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp),
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = rewardUnit,
                            onValueChange = { rewardUnit = it },
                            label = { 
                                Text(
                                    text = stringResource(R.string.unit), 
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    softWrap = false
                                ) 
                            },
                            placeholder = { Text(stringResource(R.string.unit_placeholder), fontSize = 16.sp) },
                            modifier = Modifier.weight(1f),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp),
                            singleLine = true
                        )
                    }
                    
                    // "/" 分隔符
                    Text(
                        text = "/",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    
                    // 奖励时间单位选择
                    var rewardTimeExpanded by remember { mutableStateOf(false) }
                    val rewardTimeUnits = listOf(
                        stringResource(R.string.time_unit_day),
                        stringResource(R.string.time_unit_hour),
                        stringResource(R.string.time_unit_minute),
                        stringResource(R.string.time_unit_second)
                    )
                    
                    @OptIn(ExperimentalMaterial3Api::class)
                    ExposedDropdownMenuBox(
                        expanded = rewardTimeExpanded,
                        onExpandedChange = { rewardTimeExpanded = !rewardTimeExpanded },
                        modifier = Modifier.weight(1.2f)
                    ) {
                        OutlinedTextField(
                            value = rewardTimeUnit,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text(stringResource(R.string.frequency_unit), fontSize = 16.sp) },
                            trailingIcon = { 
                                @OptIn(ExperimentalMaterial3Api::class)
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = rewardTimeExpanded) 
                            },
                            modifier = @Suppress("DEPRECATION") Modifier.menuAnchor(),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp)
                        )
                        @OptIn(ExperimentalMaterial3Api::class)
                        ExposedDropdownMenu(
                            expanded = rewardTimeExpanded,
                            onDismissRequest = { rewardTimeExpanded = false }
                        ) {
                            rewardTimeUnits.forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text(unit, fontSize = 16.sp) },
                                    onClick = {
                                        rewardTimeUnit = unit
                                        rewardTimeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 显示组合后的效果预览
                val rewardPreviewText = if (rewardText.isNotBlank() && rewardNumber.isNotBlank() && rewardUnit.isNotBlank()) {
                    val localizedTimeUnit = DefaultValueLocalizer.localizeTimeUnit(context, rewardTimeUnit)
                    val localizedRewardText = DefaultValueLocalizer.localizeRewardText(context, rewardText)
                    val localizedRewardUnit = DefaultValueLocalizer.localizeRewardUnit(context, rewardUnit)
                    stringResource(R.string.preview_reward_template, localizedTimeUnit, localizedRewardText, rewardNumber, localizedRewardUnit)
                } else {
                    val localizedTimeUnit = DefaultValueLocalizer.localizeTimeUnit(context, rewardTimeUnit)
                    stringResource(R.string.preview_reward_placeholder, localizedTimeUnit)
                }
                
                Text(
                    text = rewardPreviewText,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 惩罚设置
                Text(stringResource(R.string.incomplete_punishment), fontWeight = FontWeight.Medium, fontSize = 16.sp, color = Color(0xFFFF5722))
                Spacer(modifier = Modifier.height(8.dp))
                
                // 第一行：惩罚动作
                OutlinedTextField(
                    value = punishText,
                    onValueChange = { punishText = it },
                    label = { Text(stringResource(R.string.punishment_action), fontSize = 16.sp) },
                    placeholder = { Text(stringResource(R.string.punishment_action_placeholder), fontSize = 16.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 第二行：数量、单位、/、频率单位
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // 数量+单位的水平组合
                    Row(
                        modifier = Modifier.weight(1.8f),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        OutlinedTextField(
                            value = punishNumber,
                            onValueChange = { punishNumber = it },
                            label = { 
                                Text(
                                    text = stringResource(R.string.quantity), 
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    softWrap = false
                                ) 
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp),
                            singleLine = true
                        )
                        
                        OutlinedTextField(
                            value = punishUnit,
                            onValueChange = { punishUnit = it },
                            label = { 
                                Text(
                                    text = stringResource(R.string.unit), 
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    softWrap = false
                                ) 
                            },
                            placeholder = { Text(stringResource(R.string.unit_placeholder), fontSize = 16.sp) },
                            modifier = Modifier.weight(1f),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp),
                            singleLine = true
                        )
                    }
                    
                    // "/" 分隔符
                    Text(
                        text = "/",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    
                    // 惩罚时间单位选择
                    var punishTimeExpanded by remember { mutableStateOf(false) }
                    val punishTimeUnits = listOf(
                        stringResource(R.string.time_unit_day),
                        stringResource(R.string.time_unit_hour),
                        stringResource(R.string.time_unit_minute),
                        stringResource(R.string.time_unit_second)
                    )
                    
                    @OptIn(ExperimentalMaterial3Api::class)
                    ExposedDropdownMenuBox(
                        expanded = punishTimeExpanded,
                        onExpandedChange = { punishTimeExpanded = !punishTimeExpanded },
                        modifier = Modifier.weight(1.2f)
                    ) {
                        OutlinedTextField(
                            value = punishTimeUnit,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text(stringResource(R.string.frequency_unit), fontSize = 16.sp) },
                            trailingIcon = { 
                                @OptIn(ExperimentalMaterial3Api::class)
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = punishTimeExpanded) 
                            },
                            modifier = @Suppress("DEPRECATION") Modifier.menuAnchor(),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp)
                        )
                        @OptIn(ExperimentalMaterial3Api::class)
                        ExposedDropdownMenu(
                            expanded = punishTimeExpanded,
                            onDismissRequest = { punishTimeExpanded = false }
                        ) {
                            punishTimeUnits.forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text(unit, fontSize = 16.sp) },
                                    onClick = {
                                        punishTimeUnit = unit
                                        punishTimeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 显示组合后的效果预览
                val previewText = if (punishText.isNotBlank() && punishNumber.isNotBlank() && punishUnit.isNotBlank()) {
                    val localizedTimeUnit = DefaultValueLocalizer.localizeTimeUnit(context, punishTimeUnit)
                    val localizedPunishText = DefaultValueLocalizer.localizePunishmentText(context, punishText)
                    val localizedPunishUnit = DefaultValueLocalizer.localizePunishmentUnit(context, punishUnit)
                    stringResource(R.string.preview_punishment_template, localizedTimeUnit, localizedPunishText, punishNumber, localizedPunishUnit)
                } else {
                    val localizedTimeUnit = DefaultValueLocalizer.localizeTimeUnit(context, punishTimeUnit)
                    stringResource(R.string.preview_punishment_placeholder, localizedTimeUnit)
                }
                
                Text(
                    text = previewText,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val minutes = dailyGoalMin.toIntOrNull() ?: goal.dailyGoalMin
                    val rewardNum = rewardNumber.toIntOrNull() ?: 0
                    val punishNum = punishNumber.toIntOrNull() ?: 0
                    
                    // 保存时将本地化的单位转换回标准版本（中文）存储
                    val updatedGoal = goal.copy(
                        dailyGoalMin = minutes,
                        goalTimeUnit = DefaultValueLocalizer.reverseLocalizeTimeUnit(goalTimeUnit),
                        conditionType = conditionType,
                        rewardText = rewardText,  // 直接保存用户输入的文本
                        rewardNumber = rewardNum,
                        rewardUnit = rewardUnit,  // 直接保存用户输入的单位
                        rewardTimeUnit = DefaultValueLocalizer.reverseLocalizeTimeUnit(rewardTimeUnit),
                        punishText = punishText,  // 直接保存用户输入的文本
                        punishNumber = punishNum,
                        punishUnit = punishUnit,  // 直接保存用户输入的单位
                        punishTimeUnit = DefaultValueLocalizer.reverseLocalizeTimeUnit(punishTimeUnit)
                    )
                    onSave(updatedGoal)
                }
            ) {
                Text(stringResource(R.string.save), fontSize = 16.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), fontSize = 16.sp)
            }
        }
    )
}

 