package com.offtime.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.offtime.app.navigation.getBottomNavItems

@Composable
fun ModernBottomNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current
    val bottomNavItems = getBottomNavItems(context)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(com.offtime.app.utils.ScalingFactorUtils.uniformScaledDp(70.dp)),  // 减小高度：80dp -> 70dp
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = com.offtime.app.utils.ScalingFactorUtils.uniformScaledDp(8.dp),
        shape = RoundedCornerShape(topStart = com.offtime.app.utils.ScalingFactorUtils.uniformScaledDp(20.dp), topEnd = com.offtime.app.utils.ScalingFactorUtils.uniformScaledDp(20.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = com.offtime.app.utils.ScalingFactorUtils.uniformScaledDp(12.dp), vertical = com.offtime.app.utils.ScalingFactorUtils.uniformScaledDp(6.dp)),  // 减小内边距
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavItems.forEach { item ->
                val isSelected = currentRoute == item.route
                
                ModernNavItem(
                    selectedIcon = item.selectedIcon,
                    unselectedIcon = item.unselectedIcon,
                    label = item.label,
                    isSelected = isSelected,
                    onClick = {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ModernNavItem(
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "scale"
    )
    
    val animatedColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        },
        animationSpec = tween(durationMillis = 200),
        label = "color"
    )
    
    val backgroundAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "background"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = backgroundAlpha),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)  // 减小项目内边距
            .scale(animatedScale),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 图标容器 - 为Pixel 4优化
        Box(
            modifier = Modifier
                .size(20.dp),  // 减小图标容器：28dp -> 20dp
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSelected) selectedIcon else unselectedIcon,
                contentDescription = label,
                tint = animatedColor,
                modifier = Modifier.size(18.dp)  // 减小图标：24dp -> 18dp
            )
        }
        
        Spacer(modifier = Modifier.height(1.dp))
        
        // 标签文字 - 为Pixel 4优化
        Text(
            text = label,
            fontSize = 12.sp,  // 减小字体：14sp -> 12sp
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = animatedColor,
            maxLines = 1
        )
        
        // 选中指示器
        if (isSelected) {
            Spacer(modifier = Modifier.height(1.dp))
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(
                        animatedColor,
                        shape = CircleShape
                    )
            )
        } else {
            Spacer(modifier = Modifier.height(5.dp))
        }
    }
} 