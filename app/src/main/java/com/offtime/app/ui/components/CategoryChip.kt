package com.offtime.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.offtime.app.R
import com.offtime.app.data.entity.AppCategoryEntity
import com.offtime.app.utils.CategoryUtils
import com.offtime.app.utils.DateLocalizer
import com.offtime.app.utils.ScalingFactorUtils

@Composable
fun CategoryChip(
    category: AppCategoryEntity,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 使用新的缩放因子系统
    val adaptiveFontSizes = ScalingFactorUtils.getAdaptiveFontSizes()
    val adaptiveSpacing = ScalingFactorUtils.getAdaptiveSpacing()
    val context = LocalContext.current
    
    val categoryStyle = CategoryUtils.getCategoryStyle(category.name)
    val backgroundColor = categoryStyle.color
    val iconEmoji = categoryStyle.emoji
    
    val cardColor = if (isSelected) backgroundColor else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    
    Card(
        modifier = modifier
            .fillMaxWidth() // 填满分配的宽度
            .clickable { onClick() }
            .heightIn(min = ScalingFactorUtils.scaledDp(35.dp)), // 增加20%：29dp * 1.2 = 35dp
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 
                ScalingFactorUtils.scaledDp(4.dp) 
            else 
                ScalingFactorUtils.scaledDp(2.dp)
        ),
        shape = RoundedCornerShape(ScalingFactorUtils.scaledDp(8.dp)) // 减小圆角适配更扁的形状
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth() // 填满Card的宽度
                .padding(
                    horizontal = adaptiveSpacing.small,
                    vertical = adaptiveSpacing.xSmall    // 减小垂直内边距适配更扁的形状
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = iconEmoji,
                fontSize = adaptiveFontSizes.xLarge  // 增大emoji：large -> xLarge
            )
            Spacer(modifier = Modifier.width(adaptiveSpacing.small))
            Text(
                text = DateLocalizer.getCategoryName(context, category.name),
                color = contentColor,
                fontSize = adaptiveFontSizes.large,  // 增大文本：medium -> large
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
} 