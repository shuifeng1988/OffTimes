package com.offtime.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.offtime.app.data.entity.AppCategoryEntity
import com.offtime.app.utils.CategoryUtils
import com.offtime.app.utils.DateLocalizer

@Composable
fun CategoryTabs(
    categories: List<AppCategoryEntity>,
    selectedCategory: AppCategoryEntity?,
    onCategorySelected: (AppCategoryEntity) -> Unit
) {
    // 获取屏幕配置信息
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    
    // 缩小间距以适应页面宽度
    val horizontalSpacing = when {
        screenWidth < 360.dp -> 4.dp
        screenWidth > 480.dp -> 8.dp
        else -> 6.dp
    }
    
    // 减少内容边距以提供更多空间
    val contentPadding = when {
        screenWidth < 360.dp -> 8.dp
        screenWidth > 480.dp -> 12.dp
        else -> 10.dp
    }
    
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 12.dp), // 减少边距
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing, Alignment.CenterHorizontally), // 居中排列并设置间距
        contentPadding = PaddingValues(horizontal = contentPadding) // 减少内容边距
    ) {
        items(categories) { category ->
            CategoryTab(
                category = category,
                isSelected = selectedCategory?.id == category.id,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

@Composable
fun CategoryTab(
    category: AppCategoryEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // 获取屏幕配置信息
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val context = LocalContext.current
    
    // 缩小字体和尺寸以适应页面宽度
    val emojiSize = when {
        screenWidth < 360.dp -> 14.sp
        screenWidth > 480.dp -> 18.sp
        else -> 16.sp
    }
    
    val textSize = when {
        screenWidth < 360.dp -> 16.sp
        screenWidth > 480.dp -> 16.sp
        else -> 16.sp
    }
    
    val horizontalPadding = when {
        screenWidth < 360.dp -> 12.dp
        screenWidth > 480.dp -> 18.dp
        else -> 15.dp
    }
    
    val verticalPadding = when {
        screenHeight < 700.dp -> 8.dp
        screenHeight > 900.dp -> 12.dp
        else -> 10.dp
    }
    
    val minHeight = when {
        screenHeight < 700.dp -> 36.dp
        screenHeight > 900.dp -> 44.dp
        else -> 40.dp
    }
    
    val categoryStyle = CategoryUtils.getCategoryStyle(category.name)
    val backgroundColor = categoryStyle.color
    val iconEmoji = categoryStyle.emoji
    
    val cardColor = if (isSelected) backgroundColor else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    
    Card(
        modifier = Modifier
            .clickable { onClick() }
            .heightIn(min = minHeight), // 响应式最小高度
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 3.dp
        ),
        shape = RoundedCornerShape(30.dp) // 进一步增加圆角
    ) {
        Row(
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = verticalPadding), // 响应式内边距
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = iconEmoji,
                fontSize = emojiSize // 响应式emoji大小
            )
            Spacer(modifier = Modifier.width(10.dp)) // 增加间距
            Text(
                text = DateLocalizer.getCategoryName(context, category.name),
                color = contentColor,
                fontSize = textSize, // 响应式字体大小
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1 // 确保单行显示
            )
        }
    }
} 