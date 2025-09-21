package com.offtime.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import com.offtime.app.R
import com.offtime.app.data.entity.AppCategoryEntity
import com.offtime.app.data.entity.AppInfoEntity
import com.offtime.app.service.AppInfoService
import com.offtime.app.ui.viewmodel.CategoriesViewModel
import com.offtime.app.utils.CategoryUtils
import dagger.hilt.android.EntryPointAccessors
import com.offtime.app.ui.theme.LocalResponsiveDimensions
import com.offtime.app.ui.components.CategoryTabs
import com.offtime.app.utils.DateLocalizer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel = hiltViewModel(),
    onNavigateToExcludedApps: () -> Unit = {}
) {
    val categories by viewModel.categories.collectAsState()
    val filteredApps by viewModel.filteredApps.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadData()
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 顶部工具栏
        TopAppBar(
            title = { 
                Text(
                    text = stringResource(R.string.categories_management_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                if (isEditMode) {
                    IconButton(onClick = { viewModel.toggleEditMode() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_button_desc)
                        )
                    }
                }
            },
            actions = {
                // 排除统计按钮
                IconButton(onClick = onNavigateToExcludedApps) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = stringResource(R.string.excluded_apps_title)
                    )
                }
                
                // 编辑模式按钮
                IconButton(onClick = { viewModel.toggleEditMode() }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.edit_mode),
                        tint = if (isEditMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
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
            categories.isEmpty() -> {
                EmptyStateCard()
            }
            else -> {
                AppCategoryManagement(
                    categories = categories,
                    apps = filteredApps,
                    selectedCategory = selectedCategory,
                    isEditMode = isEditMode,
                    onCategorySelected = { viewModel.selectCategory(it) },
                    onAppCategoryChanged = { app, newCategoryId ->
                        viewModel.updateAppCategory(app.packageName, newCategoryId)
                    },
                    onExcludeApp = { packageName ->
                        viewModel.excludeApp(packageName)
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyStateCard() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.no_data),
            fontSize = 18.sp, // 标题保持18sp
            fontWeight = FontWeight.Bold // 改为Bold加粗
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "请先到首页初始化应用数据",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AppCategoryManagement(
    categories: List<AppCategoryEntity>,
    apps: List<AppInfoEntity>,
    selectedCategory: AppCategoryEntity?,
    isEditMode: Boolean,
    onCategorySelected: (AppCategoryEntity) -> Unit,
    onAppCategoryChanged: (AppInfoEntity, Int) -> Unit,
    onExcludeApp: ((String) -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 分类标签行 - 只在非编辑模式下显示
        if (!isEditMode) {
            CategoryTabs(
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelected = onCategorySelected
            )
        }
        
        // 操作提示
        if (isEditMode) {
            OperationHint()
        } else if (selectedCategory?.name == "总使用") {
            TotalUsageHint()
        }
        
        // 应用列表
        AppsList(
            apps = apps,
            categories = categories,
            onAppCategoryChanged = onAppCategoryChanged,
            categoryName = if (isEditMode) "全部应用" else (selectedCategory?.name ?: "全部"),
            onExcludeApp = onExcludeApp
        )
    }
}

@Composable
private fun OperationHint() {
    val dimensions = LocalResponsiveDimensions
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.operation_hint_title),
                    fontSize = dimensions.chartAxisTitleFontSize,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${stringResource(R.string.operation_hint_edit_mode)}\n${stringResource(R.string.operation_hint_exclude_button)}\n${stringResource(R.string.operation_hint_edit_button)}",
                fontSize = dimensions.chartAxisTitleFontSize,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun TotalUsageHint() {
    val dimensions = LocalResponsiveDimensions
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📱",
                    fontSize = 18.sp // 分类emoji保持18sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.total_usage_hint_title),
                    fontSize = 18.sp, // 分类名称使用18sp
                    fontWeight = FontWeight.Bold, // 改为Bold加粗
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.total_usage_hint_desc),
                fontSize = dimensions.chartAxisTitleFontSize,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun AppsList(
    apps: List<AppInfoEntity>,
    categories: List<AppCategoryEntity>,
    onAppCategoryChanged: (AppInfoEntity, Int) -> Unit,
    categoryName: String,
    onExcludeApp: ((String) -> Unit)? = null
) {
    val dimensions = LocalResponsiveDimensions
    LazyColumn(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            val localizedCategoryName = if (categoryName == "全部应用" || categoryName == "全部") {
                if (categoryName == "全部应用") stringResource(R.string.all_apps) else stringResource(R.string.all_categories)
            } else {
                DateLocalizer.getCategoryNameFull(LocalContext.current, categoryName)
            }
            Text(
                text = "$localizedCategoryName (${apps.size})",
                fontSize = dimensions.chartAxisTitleFontSize,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        items(apps) { app ->
            AppItem(
                app = app,
                categories = categories,
                onCategoryChanged = { newCategoryId ->
                    onAppCategoryChanged(app, newCategoryId)
                },
                onExcludeApp = onExcludeApp
            )
        }
    }
}

@Composable
private fun AppItem(
    app: AppInfoEntity,
    categories: List<AppCategoryEntity>,
    onCategoryChanged: (Int) -> Unit,
    onExcludeApp: ((String) -> Unit)? = null
) {
    val dimensions = LocalResponsiveDimensions
    var showCategoryDialog by remember { mutableStateOf(false) }
    val currentCategory = categories.find { it.id == app.categoryId }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 应用图标
            AppIcon(
                packageName = app.packageName,
                appName = app.appName,
                modifier = Modifier.size(40.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = DateLocalizer.localizeAppName(LocalContext.current, app.appName),
                    fontSize = dimensions.chartAxisTitleFontSize,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(R.string.current_category, currentCategory?.let { DateLocalizer.getCategoryName(LocalContext.current, it.name) } ?: stringResource(R.string.uncategorized)),
                    fontSize = dimensions.chartAxisTitleFontSize,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = app.packageName,
                    fontSize = dimensions.chartAxisTitleFontSize,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 排除按钮（如果应用未被排除且提供了排除函数）
            if (!app.isExcluded && onExcludeApp != null) {
                IconButton(
                    onClick = { onExcludeApp(app.packageName) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = stringResource(R.string.exclude_app),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            IconButton(
                onClick = { showCategoryDialog = true }
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.edit_category)
                )
            }
        }
    }
    
    // 分类选择对话框
    if (showCategoryDialog) {
        CategorySelectionDialog(
            categories = categories,
            currentCategoryId = app.categoryId,
            onCategorySelected = { categoryId ->
                onCategoryChanged(categoryId)
                showCategoryDialog = false
            },
            onDismiss = { showCategoryDialog = false }
        )
    }
}

@Composable
private fun AppIcon(
    packageName: String,
    appName: String,
    modifier: Modifier = Modifier
) {
    val dimensions = LocalResponsiveDimensions
    val context = LocalContext.current
    
    // 尝试获取应用图标
    var appIcon by remember { mutableStateOf<androidx.compose.ui.graphics.painter.Painter?>(null) }
    
    LaunchedEffect(packageName) {
        try {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            val bitmap = drawable.toBitmap(96, 96) // 创建96x96的bitmap
            appIcon = BitmapPainter(bitmap.asImageBitmap())
        } catch (e: Exception) {
            appIcon = null
        }
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (appIcon == null) MaterialTheme.colorScheme.primaryContainer 
                else Color.Transparent
            ),
        contentAlignment = Alignment.Center
    ) {
        if (appIcon != null) {
            Image(
                painter = appIcon!!,
                contentDescription = appName,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // 回退到字母占位符
            Text(
                text = appName.take(1).uppercase(),
                fontSize = dimensions.chartAxisTitleFontSize,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun CategorySelectionDialog(
    categories: List<AppCategoryEntity>,
    currentCategoryId: Int,
    onCategorySelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val dimensions = LocalResponsiveDimensions
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.select_category)) },
        text = {
            Column {
                categories.forEach { category ->
                    val isTotalUsage = category.name == "总使用"
                    val isDisabled = isTotalUsage // 总使用分类不允许用户手动分配
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isDisabled) { 
                                if (!isDisabled) onCategorySelected(category.id) 
                            }
                            .padding(vertical = 8.dp)
                            .background(
                                if (isTotalUsage) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = if (isTotalUsage) 8.dp else 0.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = category.id == currentCategoryId,
                            onClick = { if (!isDisabled) onCategorySelected(category.id) },
                            enabled = !isDisabled
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isTotalUsage) {
                                    Text(
                                        text = "📱",
                                        fontSize = 18.sp // 分类emoji保持18sp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = DateLocalizer.getCategoryName(LocalContext.current, category.name),
                                    fontSize = if (isTotalUsage) 18.sp else dimensions.chartAxisTitleFontSize, // 分类名称根据类型设置字体大小
                                    color = if (isTotalUsage) 
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    else 
                                        MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            if (isTotalUsage) {
                                Text(
                                    text = stringResource(R.string.auto_summary_all_apps),
                                    fontSize = dimensions.chartAxisTitleFontSize,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
} 