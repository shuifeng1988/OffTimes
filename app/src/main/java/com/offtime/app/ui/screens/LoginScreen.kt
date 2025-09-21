package com.offtime.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.offtime.app.BuildConfig
import com.offtime.app.ui.viewmodel.LoginViewModel
import com.offtime.app.ui.viewmodel.LoginType
import androidx.compose.ui.res.stringResource
import com.offtime.app.R

/**
 * 登录注册界面
 * 支持手机号+密码登录、手机号+验证码登录、用户注册
 * 
 * 界面特点：
 * - 自适应屏幕尺寸
 * - 支持多种登录方式
 * - 实时表单验证
 * - 友好的错误提示
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentContext = androidx.compose.ui.platform.LocalContext.current
    
    // 屏幕适配
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val isSmallScreen = screenHeight < 700.dp
    
    val titleFontSize = if (isSmallScreen) 24.sp else 28.sp
    val verticalSpacing = if (isSmallScreen) 16.dp else 20.dp
    val horizontalPadding = 24.dp
    
    // 监听登录成功
    LaunchedEffect(uiState.isLoginSuccess) {
        if (uiState.isLoginSuccess) {
            onLoginSuccess()
        }
    }
    
    // Google登录Activity启动器
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        android.util.Log.d("LoginScreen", "Google登录结果: resultCode=${result.resultCode}")
        // 处理Google登录结果
        viewModel.handleGoogleSignInResult(result.data)
    }
    
    // 监听Google登录Intent启动
    LaunchedEffect(uiState.googleSignInIntent) {
        uiState.googleSignInIntent?.let { intent ->
            try {
                android.util.Log.d("LoginScreen", "启动Google登录Intent")
                googleSignInLauncher.launch(intent)
                // 清除Intent状态，防止重复启动
                viewModel.clearGoogleSignInIntent()
            } catch (e: Exception) {
                android.util.Log.e("LoginScreen", "启动Google登录Intent失败", e)
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontalPadding)
    ) {
        // 顶部导航栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = verticalSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back_button)
                )
            }
            Text(
                text = if (uiState.isRegisterMode) stringResource(R.string.register_title) else stringResource(R.string.login_title),
                fontSize = titleFontSize,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(verticalSpacing))
        
        // 登录方式切换标签 - 根据构建配置显示
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val availableTypes = buildList {
                if (BuildConfig.ENABLE_PASSWORD_LOGIN) add(LoginType.PASSWORD)
                if (BuildConfig.ENABLE_SMS_LOGIN) add(LoginType.SMS_CODE)
                if (BuildConfig.ENABLE_ALIPAY_LOGIN) add(LoginType.ALIPAY)
                if (BuildConfig.ENABLE_GOOGLE_LOGIN) add(LoginType.GOOGLE)
            }
            
            if (availableTypes.isNotEmpty()) {
                TabRow(
                    selectedTabIndex = availableTypes.indexOf(uiState.loginType).takeIf { it >= 0 } ?: 0,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    availableTypes.forEach { loginType ->
                        Tab(
                            selected = uiState.loginType == loginType,
                            onClick = { viewModel.setLoginType(loginType) },
                            text = {
                                Text(
                                    when (loginType) {
                                        LoginType.PASSWORD -> stringResource(R.string.login_password_tab)
                                        LoginType.SMS_CODE -> stringResource(R.string.login_sms_tab)
                                        LoginType.ALIPAY -> stringResource(R.string.login_alipay_quick)
                                        LoginType.GOOGLE -> stringResource(R.string.login_google_quick)
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(verticalSpacing * 1.5f))
        
        // Google Play版本：直接显示Google登录，不显示手机号输入框
        if (BuildConfig.ENABLE_GOOGLE_LOGIN && !BuildConfig.ENABLE_SMS_LOGIN && !BuildConfig.ENABLE_PASSWORD_LOGIN) {
            // 纯Google登录版本 - 简洁界面
            GoogleOnlyLoginContent(
                isLoading = uiState.isLoading,
                onGoogleLogin = {
                    if (currentContext is android.app.Activity) {
                        viewModel.loginWithGoogle(currentContext, forceAccountPicker = false)
                    }
                },
                onSwitchAccount = {
                    if (currentContext is android.app.Activity) {
                        viewModel.loginWithGoogle(currentContext, forceAccountPicker = true)
                    }
                }
            )
        } else {
            // 传统版本 - 显示手机号输入框和多种登录方式
            // 账号/手机号输入框
            OutlinedTextField(
                value = uiState.phoneNumber,
                onValueChange = viewModel::setPhoneNumber,
                label = { 
                    Text(
                        if (uiState.loginType == LoginType.PASSWORD) 
                            stringResource(R.string.login_account) 
                        else 
                            stringResource(R.string.login_phone_number)
                    ) 
                },
                placeholder = { 
                    Text(
                        if (uiState.loginType == LoginType.PASSWORD) 
                            stringResource(R.string.login_account_placeholder) 
                        else 
                            stringResource(R.string.login_phone_number_placeholder)
                    ) 
                },
                leadingIcon = {
                    Icon(
                        if (uiState.loginType == LoginType.PASSWORD) 
                            Icons.Default.AccountCircle 
                        else 
                            Icons.Default.Phone, 
                        contentDescription = null
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (uiState.loginType == LoginType.PASSWORD) 
                        KeyboardType.Text 
                    else 
                        KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                isError = uiState.phoneNumberError != null,
                supportingText = uiState.phoneNumberError?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(verticalSpacing))
            
            // 根据登录类型显示不同的输入框
            when (uiState.loginType) {
            LoginType.PASSWORD -> {
                // 密码输入框
                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = viewModel::setPassword,
                    label = { Text(stringResource(R.string.login_password)) },
                    placeholder = { Text(stringResource(R.string.login_password_placeholder)) },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null)
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { viewModel.togglePasswordVisibility() }
                        ) {
                            Icon(
                                imageVector = if (uiState.passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (uiState.passwordVisible) stringResource(R.string.login_hide_password) else stringResource(R.string.login_show_password)
                            )
                        }
                    },
                    visualTransformation = if (uiState.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (uiState.isRegisterMode) {
                                // 注册模式需要先发送验证码
                            } else {
                                viewModel.loginWithPassword()
                            }
                        }
                    ),
                    singleLine = true,
                    isError = uiState.passwordError != null,
                    supportingText = uiState.passwordError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // 注册模式下显示确认密码
                if (uiState.isRegisterMode) {
                    Spacer(modifier = Modifier.height(verticalSpacing))
                    
                    OutlinedTextField(
                        value = uiState.confirmPassword,
                        onValueChange = viewModel::setConfirmPassword,
                        label = { Text(stringResource(R.string.login_confirm_password)) },
                        placeholder = { Text(stringResource(R.string.login_confirm_password_placeholder)) },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null)
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        singleLine = true,
                        isError = uiState.confirmPasswordError != null,
                        supportingText = uiState.confirmPasswordError?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            LoginType.SMS_CODE -> {
                // 验证码输入框和发送按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    OutlinedTextField(
                        value = uiState.smsCode,
                        onValueChange = viewModel::setSmsCode,
                        label = { Text(stringResource(R.string.login_verification_code)) },
                        placeholder = { Text(stringResource(R.string.login_verification_code_placeholder)) },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null)
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                viewModel.loginWithSmsCode()
                            }
                        ),
                        singleLine = true,
                        isError = uiState.smsCodeError != null,
                        supportingText = uiState.smsCodeError?.let { { Text(it) } },
                        modifier = Modifier.weight(1f)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Button(
                        onClick = viewModel::sendSmsCode,
                        enabled = !uiState.isSendingCode && uiState.countDown == 0,
                        modifier = Modifier
                            .height(56.dp)
                            .widthIn(min = 100.dp)
                    ) {
                        Text(
                            text = when {
                                uiState.isSendingCode -> stringResource(R.string.login_sending_code)
                                uiState.countDown > 0 -> "${uiState.countDown}s"
                                else -> stringResource(R.string.login_send_code)
                            }
                        )
                    }
                }
            }
            
            LoginType.ALIPAY -> {
                // 支付宝登录说明
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                            Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.login_alipay_quick),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                        )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = stringResource(R.string.login_alipay_description),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                        
                        if (BuildConfig.DEBUG) {
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            OutlinedButton(
                                onClick = { viewModel.simulateAlipayLogin() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.login_alipay_debug))
                            }
                            
                            Text(
                                text = stringResource(R.string.login_alipay_debug_note),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
            
            LoginType.GOOGLE -> {
                // Google登录说明
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.login_google_quick),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = stringResource(R.string.login_google_description),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                        
                        if (BuildConfig.DEBUG) {
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            OutlinedButton(
                                onClick = { 
                                    if (currentContext is android.app.Activity) {
                                        viewModel.loginWithGoogle(currentContext)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.login_google_debug))
                            }
                            
                            Text(
                                text = stringResource(R.string.login_google_debug_note),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
            } // 结束 when 语句
        } // 结束 else 语句块
        
        // 注册模式下在密码登录或支付宝登录标签页显示验证码输入框（仅在需要SMS验证码的版本中显示）
        if (uiState.isRegisterMode && uiState.loginType != LoginType.SMS_CODE && BuildConfig.REQUIRE_SMS_VERIFICATION) {
            Spacer(modifier = Modifier.height(verticalSpacing))
            
            // 验证码输入框和发送按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                OutlinedTextField(
                    value = uiState.smsCode,
                    onValueChange = viewModel::setSmsCode,
                    label = { Text("验证码") },
                    placeholder = { Text("请输入验证码") },
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    isError = uiState.smsCodeError != null,
                    supportingText = uiState.smsCodeError?.let { { Text(it) } },
                    modifier = Modifier.weight(1f)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Button(
                    onClick = viewModel::sendSmsCode,
                    enabled = !uiState.isSendingCode && uiState.countDown == 0 && uiState.phoneNumber.isNotEmpty(),
                    modifier = Modifier
                        .height(56.dp)
                        .widthIn(min = 100.dp)
                ) {
                    Text(
                        text = when {
                            uiState.isSendingCode -> stringResource(R.string.login_sending_code)
                            uiState.countDown > 0 -> "${uiState.countDown}s"
                            else -> stringResource(R.string.login_send_code)
                        }
                    )
                }
            }
        }
        
        // 注册模式下的昵称输入（可选）
        if (uiState.isRegisterMode) {
            Spacer(modifier = Modifier.height(verticalSpacing))
            
            OutlinedTextField(
                value = uiState.nickname,
                onValueChange = viewModel::setNickname,
                label = { Text("昵称（可选）") },
                placeholder = { Text("设置您的昵称") },
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null)
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        Spacer(modifier = Modifier.height(verticalSpacing * 2))
        
        
        // 主要操作按钮（Google Play纯登录版本不显示此按钮）
        if (!(BuildConfig.ENABLE_GOOGLE_LOGIN && !BuildConfig.ENABLE_SMS_LOGIN && !BuildConfig.ENABLE_PASSWORD_LOGIN)) {
            Button(
            onClick = {
                if (uiState.isRegisterMode) {
                    viewModel.register()
                } else {
                    when (uiState.loginType) {
                        LoginType.PASSWORD -> viewModel.loginWithPassword()
                        LoginType.SMS_CODE -> viewModel.loginWithSmsCode()
                        LoginType.ALIPAY -> {
                            if (currentContext is android.app.Activity) {
                                viewModel.loginWithAlipay(currentContext)
                            }
                        }
                        LoginType.GOOGLE -> {
                            if (currentContext is android.app.Activity) {
                                viewModel.loginWithGoogle(currentContext)
                            }
                        }
                    }
                }
            },
            enabled = !uiState.isLoading && (uiState.loginType == LoginType.ALIPAY || uiState.loginType == LoginType.GOOGLE || viewModel.isFormValid()),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (uiState.loginType) {
                    LoginType.ALIPAY -> {
                        Icon(
                            imageVector = Icons.Default.Payment,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    LoginType.GOOGLE -> {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    else -> { /* No icon for other types */ }
                }
                
                Text(
                    text = when {
                        uiState.isRegisterMode -> stringResource(R.string.register_button)
                        uiState.loginType == LoginType.ALIPAY -> stringResource(R.string.login_with_alipay)
                        uiState.loginType == LoginType.GOOGLE -> stringResource(R.string.login_with_google)
                        else -> stringResource(R.string.login_button)
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } // 结束 Button
        } // 结束 if 条件判断
        
        Spacer(modifier = Modifier.height(verticalSpacing))
        
        // 登录/注册模式切换（Google Play纯登录版本不显示）
        if (!(BuildConfig.ENABLE_GOOGLE_LOGIN && !BuildConfig.ENABLE_SMS_LOGIN && !BuildConfig.ENABLE_PASSWORD_LOGIN)) {
            Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (uiState.isRegisterMode) stringResource(R.string.has_account) else stringResource(R.string.no_account),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(
                onClick = viewModel::toggleRegisterMode
            ) {
                Text(
                    text = if (uiState.isRegisterMode) stringResource(R.string.login_now) else stringResource(R.string.register_now),
                    fontWeight = FontWeight.Medium
                )
            }
        } // 结束 Row
        } // 结束 if 条件判断
        
        // 错误信息显示
        uiState.errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(verticalSpacing))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        

        
        Spacer(modifier = Modifier.height(verticalSpacing * 2))
    }
}

/**
 * Google Play版本专用的简洁登录界面
 * 只显示Google登录选项，不显示手机号相关输入框
 */
@Composable
private fun GoogleOnlyLoginContent(
    isLoading: Boolean,
    onGoogleLogin: () -> Unit,
    onSwitchAccount: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Google登录说明卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = stringResource(R.string.login_google_welcome),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(R.string.login_google_description),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Google登录按钮
        Button(
            onClick = onGoogleLogin,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(12.dp))
            } else {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            
            Text(
                text = stringResource(R.string.login_with_google),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        // 切换账号选项（如果提供了回调函数）
        onSwitchAccount?.let { switchAccount ->
            Spacer(modifier = Modifier.height(12.dp))
            
            TextButton(
                onClick = switchAccount,
                enabled = !isLoading
            ) {
                Text(
                    text = stringResource(R.string.login_switch_google_account),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 隐私和条款说明
        Text(
            text = stringResource(R.string.login_google_privacy_note),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
} 