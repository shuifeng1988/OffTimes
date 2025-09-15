package com.offtime.app.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * 语言变化监听器
 * 监听系统语言变化并通知界面更新
 */
object LanguageChangeListener {
    
    private val _languageChangeEvent = MutableStateFlow(System.currentTimeMillis())
    val languageChangeEvent: StateFlow<Long> = _languageChangeEvent.asStateFlow()
    
    private var receiver: BroadcastReceiver? = null
    private var isRegistered = false
    
    /**
     * 注册语言变化监听器
     */
    fun register(context: Context) {
        if (isRegistered) return
        
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_LOCALE_CHANGED) {
                    android.util.Log.d("LanguageChangeListener", "检测到系统语言变化")
                    onLanguageChanged()
                }
            }
        }
        
        val filter = IntentFilter(Intent.ACTION_LOCALE_CHANGED)
        context.registerReceiver(receiver, filter)
        isRegistered = true
        
        android.util.Log.d("LanguageChangeListener", "语言变化监听器已注册")
    }
    
    /**
     * 取消注册语言变化监听器
     */
    fun unregister(context: Context) {
        if (!isRegistered || receiver == null) return
        
        try {
            context.unregisterReceiver(receiver)
            isRegistered = false
            android.util.Log.d("LanguageChangeListener", "语言变化监听器已取消注册")
        } catch (e: IllegalArgumentException) {
            android.util.Log.w("LanguageChangeListener", "取消注册失败，可能已经取消过了: ${e.message}")
        }
    }
    
    /**
     * 手动触发语言变化事件（用于测试或强制刷新）
     */
    fun triggerLanguageChange() {
        onLanguageChanged()
    }
    
    /**
     * 处理语言变化事件
     */
    private fun onLanguageChanged() {
        // 清除统一文本管理器的语言缓存
        UnifiedTextManager.clearLanguageCache()
        
        // 发送语言变化事件
        _languageChangeEvent.value = System.currentTimeMillis()
        
        android.util.Log.d("LanguageChangeListener", "语言变化事件已触发，当前语言: ${Locale.getDefault().language}")
    }
} 