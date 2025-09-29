package com.offtime.app.ui.viewmodel

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.offtime.app.data.model.AppUpdateInfo
import com.offtime.app.service.AppUpdateService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 应用更新ViewModel
 */
@HiltViewModel
class UpdateViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appUpdateService: AppUpdateService
) : ViewModel() {
    
    companion object {
        private const val TAG = "UpdateViewModel"
    }
    
    // 更新检查状态
    private val _updateCheckState = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
    val updateCheckState: StateFlow<UpdateCheckState> = _updateCheckState.asStateFlow()
    
    // 下载状态
    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()
    
    /**
     * 检查应用更新
     */
    fun checkForUpdate() {
        if (!appUpdateService.isInAppUpdateAllowed()) {
            Log.d(TAG, "当前版本不支持应用内更新")
            _updateCheckState.value = UpdateCheckState.NotSupported
            return
        }
        
        viewModelScope.launch {
            _updateCheckState.value = UpdateCheckState.Checking
            
            try {
                val result = appUpdateService.checkForUpdate()
                
                if (result.isSuccess) {
                    val updateInfo = result.getOrNull()
                    if (updateInfo != null) {
                        Log.d(TAG, "✅ 发现新版本: ${updateInfo.latestVersion}")
                        _updateCheckState.value = UpdateCheckState.UpdateAvailable(updateInfo)
                    } else {
                        Log.d(TAG, "✅ 当前已是最新版本")
                        _updateCheckState.value = UpdateCheckState.NoUpdate
                    }
                } else {
                    val error = result.exceptionOrNull()
                    Log.e(TAG, "❌ 检查更新失败", error)
                    _updateCheckState.value = UpdateCheckState.Error(error?.message ?: "检查更新失败")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ 检查更新异常", e)
                _updateCheckState.value = UpdateCheckState.Error(e.message ?: "检查更新异常")
            }
        }
    }
    
    /**
     * 开始下载更新
     */
    fun startDownload(updateInfo: AppUpdateInfo) {
        viewModelScope.launch {
            try {
                _downloadState.value = DownloadState.Downloading(0)
                
                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                
                val request = DownloadManager.Request(Uri.parse(updateInfo.downloadUrl)).apply {
                    setTitle("OffTimes 更新")
                    setDescription("正在下载 v${updateInfo.latestVersion}")
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "offtimes-${updateInfo.latestVersion}.apk")
                    setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                }
                
                val downloadId = downloadManager.enqueue(request)
                
                Log.d(TAG, "🔽 开始下载更新，下载ID: $downloadId")
                _downloadState.value = DownloadState.DownloadStarted(downloadId)
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ 下载失败", e)
                _downloadState.value = DownloadState.Error(e.message ?: "下载失败")
            }
        }
    }
    
    /**
     * 打开浏览器下载
     */
    fun openBrowserDownload(updateInfo: AppUpdateInfo) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo.downloadUrl)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Log.d(TAG, "🌐 已打开浏览器下载")
        } catch (e: Exception) {
            Log.e(TAG, "❌ 打开浏览器失败", e)
        }
    }
    
    /**
     * 重置状态
     */
    fun resetStates() {
        _updateCheckState.value = UpdateCheckState.Idle
        _downloadState.value = DownloadState.Idle
    }
}

/**
 * 更新检查状态
 */
sealed class UpdateCheckState {
    object Idle : UpdateCheckState()
    object Checking : UpdateCheckState()
    object NoUpdate : UpdateCheckState()
    object NotSupported : UpdateCheckState()
    data class UpdateAvailable(val updateInfo: AppUpdateInfo) : UpdateCheckState()
    data class Error(val message: String) : UpdateCheckState()
}

/**
 * 下载状态
 */
sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progress: Int) : DownloadState()
    data class DownloadStarted(val downloadId: Long) : DownloadState()
    data class DownloadCompleted(val filePath: String) : DownloadState()
    data class Error(val message: String) : DownloadState()
}
