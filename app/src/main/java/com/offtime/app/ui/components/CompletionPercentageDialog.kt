package com.offtime.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.offtime.app.R
import com.offtime.app.utils.LocaleUtils

/**
 * 完成数量选择对话框
 * 
 * @param isVisible 是否显示对话框
 * @param isReward 是否为奖励模式（true为奖励，false为惩罚）
 * @param taskDescription 任务描述（如："Push-ups 30个"）
 * @param targetNumber 目标数量（如30个push-ups）
 * @param currentPercent 当前完成百分比
 * @param onDismiss 关闭对话框回调
 * @param onConfirm 确认选择回调，参数为计算出的百分比(0-100)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompletionPercentageDialog(
    isVisible: Boolean,
    isReward: Boolean,
    taskDescription: String,
    targetNumber: Int,
    currentPercent: Int = 0,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    if (!isVisible) return
    
    val context = LocalContext.current
    val localeUtils = LocaleUtils(context)
    val isEnglish = localeUtils.isEnglishLocale()
    
    // 计算当前完成数量（基于百分比）
    val currentCompletedNumber = if (currentPercent > 0 && targetNumber > 0) {
        (targetNumber * currentPercent / 100).coerceAtMost(targetNumber)
    } else 0
    
    var completedNumberText by remember { mutableStateOf(if (currentCompletedNumber > 0) currentCompletedNumber.toString() else "") }
    var isValidInput by remember { mutableStateOf(true) }
    
    // 验证输入并计算百分比
    val completedNumber = try {
        if (completedNumberText.isBlank()) 0 else completedNumberText.toInt().coerceIn(0, targetNumber)
    } catch (e: NumberFormatException) {
        isValidInput = false
        0
    }
    
    val calculatedPercentage = if (targetNumber > 0 && completedNumber >= 0) {
        (completedNumber * 100 / targetNumber).coerceIn(0, 100)
    } else 0
    
    if (completedNumberText.isNotBlank()) {
        isValidInput = try {
            val value = completedNumberText.toInt()
            value in 0..targetNumber
        } catch (e: NumberFormatException) {
            false
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 标题
                Text(
                    text = if (isReward) {
                        if (isEnglish) "Reward Completion" else "奖励完成"
                    } else {
                        if (isEnglish) "Punishment Completion" else "惩罚完成"
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 任务描述
                Text(
                    text = taskDescription,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // 完成数量说明
                Text(
                    text = if (isEnglish) {
                        "How many did you complete?"
                    } else {
                        "您完成了多少个？"
                    },
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 数量输入框
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(50.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = if (isValidInput) 1.dp else 2.dp,
                                color = if (isValidInput) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.error,
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicTextField(
                            value = completedNumberText,
                            onValueChange = { newValue ->
                                // 只允许数字输入，最多根据目标数量位数决定
                                val maxLength = targetNumber.toString().length + 1
                                if (newValue.length <= maxLength && newValue.all { it.isDigit() }) {
                                    completedNumberText = newValue
                                }
                            },
                            textStyle = TextStyle(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = "/ $targetNumber",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (!isValidInput) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isEnglish) "Please enter 0-$targetNumber" else "请输入0-$targetNumber 之间的数字",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                // 显示计算出的百分比
                if (completedNumberText.isNotBlank() && isValidInput) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isEnglish) "= $calculatedPercentage%" else "= $calculatedPercentage%",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // 操作按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (isEnglish) "Cancel" else "取消",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Button(
                        onClick = { 
                            if (isValidInput) {
                                onConfirm(calculatedPercentage)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = isValidInput,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isReward) Color(0xFFFF8F00) else Color(0xFFD32F2F)
                        )
                    ) {
                        Text(
                            text = if (isEnglish) "Confirm" else "确认",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
