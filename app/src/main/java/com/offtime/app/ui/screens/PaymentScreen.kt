package com.offtime.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.offtime.app.BuildConfig
import com.offtime.app.ui.viewmodel.PaymentViewModel
import com.offtime.app.ui.viewmodel.PaymentMethod
import androidx.compose.ui.res.stringResource
import com.offtime.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    onNavigateBack: () -> Unit = {},
    onPaymentSuccess: () -> Unit = {},
    viewModel: PaymentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadSubscriptionInfo()
    }
    
    // 监听付费成功事件
    LaunchedEffect(uiState.isPaymentSuccess) {
        if (uiState.isPaymentSuccess) {
            onPaymentSuccess()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        // 顶部栏
        TopAppBar(
            title = { Text(stringResource(R.string.payment_title)) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Top
        ) {
            
            // 主要内容区域
            Column {
                // 应用图标和标题
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = stringResource(R.string.payment_title),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = stringResource(R.string.payment_subtitle),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // 试用状态卡片
                uiState.subscriptionInfo?.let { info ->
                    TrialStatusCard(info)
                }
                
                // 删除高级版功能部分，因为本应用本身就是付费应用
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 获取当前Activity上下文
            val context = LocalContext.current
            
            // 底部付费区域
            PaymentSection(
                isLoading = uiState.isLoading,
                selectedPaymentMethod = uiState.selectedPaymentMethod,
                onPaymentMethodSelect = { viewModel.selectPaymentMethod(it) },
                onPayment = { 
                    if (context is android.app.Activity) {
                        viewModel.processPayment(context)
                    }
                }
            )
            
            // 底部间距，确保内容不被底部导航栏遮挡
            Spacer(modifier = Modifier.height(80.dp))
        }
        
        // 错误提示
        uiState.errorMessage?.let { error ->
            LaunchedEffect(error) {
                // 这里可以显示Snackbar或其他错误提示
            }
        }
    }
}

@Composable
fun TrialStatusCard(subscriptionInfo: PaymentViewModel.SubscriptionInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (subscriptionInfo.isTrialExpired) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (subscriptionInfo.isTrialExpired) Icons.Default.ErrorOutline else Icons.Default.Schedule,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = if (subscriptionInfo.isTrialExpired) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.secondary
                }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = if (subscriptionInfo.isTrialExpired) stringResource(R.string.payment_trial_expired) else stringResource(R.string.payment_trial_active),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = if (subscriptionInfo.isTrialExpired) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.secondary
                }
            )
            
            if (!subscriptionInfo.isTrialExpired && subscriptionInfo.remainingTrialDays > 0) {
                Text(
                    text = stringResource(R.string.account_trial_days_remaining, subscriptionInfo.remainingTrialDays),
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else if (subscriptionInfo.isTrialExpired) {
                Text(
                    text = stringResource(R.string.account_trial_expired_description),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}



@Composable
fun PaymentSection(
    isLoading: Boolean,
    selectedPaymentMethod: PaymentMethod,
    onPaymentMethodSelect: (PaymentMethod) -> Unit,
    onPayment: () -> Unit
) {
    Column {
        // 价格显示
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.payment_one_time),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "￥",
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = PaymentViewModel.PREMIUM_PRICE_DISPLAY,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Text(
                    text = stringResource(R.string.payment_lifetime),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 支付方式选择
        Text(
            text = stringResource(R.string.payment_select_method),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // 调试信息
        if (com.offtime.app.BuildConfig.DEBUG) {
            Text(
                text = "${stringResource(R.string.payment_debug_selected_method)} ${getPaymentMethodDisplayName(selectedPaymentMethod)}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(12.dp)
        ) {
            val paymentMethods = mutableListOf<PaymentMethod>()
            
            // 根据构建配置动态添加支付方式
            if (BuildConfig.ENABLE_GOOGLE_PAY) {
                paymentMethods.add(PaymentMethod.GOOGLE_PLAY)
            } else {
                // 非Google Play版本显示支付宝和微信
                paymentMethods.add(PaymentMethod.ALIPAY)
                paymentMethods.add(PaymentMethod.WECHAT)
            }
            
            paymentMethods.forEachIndexed { index, method ->
                PaymentMethodItem(
                    method = method,
                    isSelected = selectedPaymentMethod == method,
                    onSelect = { onPaymentMethodSelect(method) }
                )
                if (index < paymentMethods.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 付费按钮
        Button(
            onClick = onPayment,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = getPaymentIcon(selectedPaymentMethod),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.payment_select_method) + " " + getPaymentMethodDisplayName(selectedPaymentMethod) + " " + 
                            (if (BuildConfig.ENABLE_GOOGLE_PAY) PaymentViewModel.PREMIUM_CURRENCY_SYMBOL else "¥") + 
                            PaymentViewModel.PREMIUM_PRICE_DISPLAY,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        // 说明文字 - 根据版本显示不同的支付条款
        Text(
            text = stringResource(
                if (BuildConfig.ENABLE_GOOGLE_PAY) R.string.payment_terms_google_play 
                else R.string.payment_terms
            ),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )
    }
}

@Composable
fun PaymentMethodItem(
    method: PaymentMethod,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onSelect
            )
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    )
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getPaymentIcon(method),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = getPaymentMethodDisplayName(method),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun getPaymentMethodDisplayName(method: PaymentMethod): String {
    return when (method) {
        PaymentMethod.ALIPAY -> stringResource(R.string.payment_alipay)
        PaymentMethod.WECHAT -> stringResource(R.string.payment_wechat)
        PaymentMethod.GOOGLE_PLAY -> stringResource(R.string.payment_google_play)
        PaymentMethod.OTHER -> "Other"
    }
}

@Composable
fun getPaymentIcon(method: PaymentMethod): androidx.compose.ui.graphics.vector.ImageVector {
    return when (method) {
        PaymentMethod.ALIPAY -> Icons.Default.Payment
        PaymentMethod.WECHAT -> Icons.AutoMirrored.Filled.Chat
        PaymentMethod.GOOGLE_PLAY -> Icons.Default.Store
        PaymentMethod.OTHER -> Icons.Default.CreditCard
    }
}
