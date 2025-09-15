package com.offtime.app.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 声音管理器 - 负责播放定时器结束提醒音
 */
class SoundManager private constructor(private val context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: SoundManager? = null
        
        fun getInstance(context: Context): SoundManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SoundManager(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        private const val TAG = "SoundManager"
    }
    
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var mediaPlayer: MediaPlayer? = null
    
    /**
     * 播放定时器结束提醒音
     * 使用系统音调生成器创建一个友好的提醒音
     */
    fun playCountdownFinishedSound() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 检查是否在静音或震动模式
                when (audioManager.ringerMode) {
                    AudioManager.RINGER_MODE_SILENT -> {
                        Log.d(TAG, "设备处于静音模式，只播放震动")
                        playVibration()
                        return@launch
                    }
                    AudioManager.RINGER_MODE_VIBRATE -> {
                        Log.d(TAG, "设备处于震动模式，播放震动")
                        playVibration()
                        return@launch
                    }
                    AudioManager.RINGER_MODE_NORMAL -> {
                        Log.d(TAG, "设备处于正常模式，播放声音和震动")
                        playSound()
                        playVibration()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "播放提醒音失败", e)
            }
        }
    }
    
    /**
     * 播放悦耳的提醒音
     */
    private suspend fun playSound() {
        withContext(Dispatchers.Main) {
            try {
                // 释放之前的MediaPlayer
                mediaPlayer?.release()
                
                // 创建新的MediaPlayer
                mediaPlayer = MediaPlayer().apply {
                    // 设置音频属性 - 使用通知音频流
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        setAudioStreamType(AudioManager.STREAM_NOTIFICATION)
                    }
                    
                    // 设置数据源为系统通知音
                    try {
                        val notificationUri = android.provider.Settings.System.DEFAULT_NOTIFICATION_URI
                        setDataSource(context, notificationUri)
                        
                        // 异步准备并播放
                        setOnPreparedListener { player ->
                            try {
                                player.start()
                                Log.d(TAG, "定时器结束提醒音播放成功")
                            } catch (e: Exception) {
                                Log.e(TAG, "播放提醒音失败", e)
                                // 如果系统通知音播放失败，使用音调生成器作为备选方案
                                playToneAsBackup()
                            }
                        }
                        
                        setOnCompletionListener { player ->
                            player.release()
                            Log.d(TAG, "提醒音播放完成")
                        }
                        
                        setOnErrorListener { player, what, extra ->
                            Log.e(TAG, "MediaPlayer错误: what=$what, extra=$extra")
                            player.release()
                            // 使用音调生成器作为备选方案
                            playToneAsBackup()
                            true
                        }
                        
                        prepareAsync()
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "设置MediaPlayer数据源失败", e)
                        release()
                        // 使用音调生成器作为备选方案
                        playToneAsBackup()
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "创建MediaPlayer失败", e)
                // 使用音调生成器作为备选方案
                playToneAsBackup()
            }
        }
    }
    
    /**
     * 备选方案：使用音调生成器播放提醒音
     */
    private fun playToneAsBackup() {
        try {
            CoroutineScope(Dispatchers.IO).launch {
                val toneGenerator = ToneGenerator(
                    AudioManager.STREAM_NOTIFICATION,
                    ToneGenerator.MAX_VOLUME / 2 // 使用中等音量
                )
                
                // 播放两个音调组成的提醒音：高音-低音-高音
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 300) // 300ms高音
                kotlinx.coroutines.delay(100)
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP2, 300) // 300ms中音
                kotlinx.coroutines.delay(100)
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 300) // 300ms高音
                
                kotlinx.coroutines.delay(1000) // 等待播放完成
                toneGenerator.release()
                
                Log.d(TAG, "备选音调提醒音播放完成")
            }
        } catch (e: Exception) {
            Log.e(TAG, "播放备选音调失败", e)
        }
    }
    
    /**
     * 播放震动提醒
     */
    private fun playVibration() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            
            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // 创建一个有节奏的震动模式：短-停-短-停-长
                    val vibrationPattern = longArrayOf(0, 200, 100, 200, 100, 500)
                    val vibrationEffect = VibrationEffect.createWaveform(vibrationPattern, -1)
                    vibrator.vibrate(vibrationEffect)
                } else {
                    @Suppress("DEPRECATION")
                    val vibrationPattern = longArrayOf(0, 200, 100, 200, 100, 500)
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(vibrationPattern, -1)
                }
                Log.d(TAG, "震动提醒播放成功")
            } else {
                Log.d(TAG, "设备不支持震动")
            }
        } catch (e: Exception) {
            Log.e(TAG, "播放震动提醒失败", e)
        }
    }
    
    /**
     * 停止所有音频播放
     */
    fun stopAllSounds() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            mediaPlayer = null
            Log.d(TAG, "已停止所有音频播放")
        } catch (e: Exception) {
            Log.e(TAG, "停止音频播放失败", e)
        }
    }
    
    /**
     * 检查是否可以播放声音
     */
    fun canPlaySound(): Boolean {
        return audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL
    }
    
    /**
     * 获取当前音量等级（0-100）
     */
    fun getCurrentVolumeLevel(): Int {
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)
        return if (maxVolume > 0) (currentVolume * 100 / maxVolume) else 0
    }
} 