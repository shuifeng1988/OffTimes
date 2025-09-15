package com.offtime.app.utils

import android.util.Log
import com.offtime.app.BuildConfig

/**
 * 日志工具类
 * 
 * 用于统一管理应用中的日志输出，在生产环境中自动禁用调试日志
 * 
 * @author OffTime Team
 * @version 1.0.0
 * @since 2024-12-19
 */
object LogUtils {
    
    private const val TAG_PREFIX = "OffTime_"
    
    /**
     * 是否启用调试日志
     * 在Debug版本中启用，Release版本中禁用
     */
    private val DEBUG = BuildConfig.DEBUG
    
    /**
     * 调试日志
     * @param tag 日志标签
     * @param message 日志信息
     */
    fun d(tag: String, message: String) {
        if (DEBUG) {
            Log.d("$TAG_PREFIX$tag", message)
        }
    }
    
    /**
     * 信息日志（生产环境也会输出，用于关键操作信息）
     * @param tag 日志标签
     * @param message 日志信息
     */
    fun i(tag: String, message: String) {
        Log.i("$TAG_PREFIX$tag", message)
    }
    
    /**
     * 警告日志（总是输出，表示潜在问题）
     * @param tag 日志标签
     * @param message 日志信息
     */
    fun w(tag: String, message: String) {
        Log.w("$TAG_PREFIX$tag", message)
    }
    
    /**
     * 详细调试日志（仅Debug版本输出）
     * @param tag 日志标签
     * @param message 日志信息
     */
    fun v(tag: String, message: String) {
        if (DEBUG) {
            Log.v("$TAG_PREFIX$tag", message)
        }
    }
    
    /**
     * 错误日志（总是输出，即使在Release版本中）
     * @param tag 日志标签
     * @param message 日志信息
     * @param throwable 异常信息（可选）
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e("$TAG_PREFIX$tag", message, throwable)
        } else {
            Log.e("$TAG_PREFIX$tag", message)
        }
    }
} 