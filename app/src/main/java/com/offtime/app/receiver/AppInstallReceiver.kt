package com.offtime.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.offtime.app.data.repository.AppRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppInstallReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AppInstallReceiverEntryPoint {
        fun appRepository(): AppRepository
    }

    override fun onReceive(context: Context, intent: Intent) {
        val hiltEntryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            AppInstallReceiverEntryPoint::class.java
        )
        val appRepository = hiltEntryPoint.appRepository()

        val packageName = intent.data?.schemeSpecificPart ?: return
        
        when (intent.action) {
            Intent.ACTION_PACKAGE_ADDED -> {
                // 应用安装
                if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                    // 新安装，不是更新
                    CoroutineScope(Dispatchers.IO).launch {
                        appRepository.updateAppInfo(packageName)
                    }
                }
            }
            Intent.ACTION_PACKAGE_REPLACED -> {
                // 应用更新
                CoroutineScope(Dispatchers.IO).launch {
                    appRepository.updateAppInfo(packageName)
                }
            }
            Intent.ACTION_PACKAGE_REMOVED -> {
                // 应用卸载
                if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                    // 真正卸载，不是更新过程中的临时卸载
                    CoroutineScope(Dispatchers.IO).launch {
                        appRepository.removeUninstalledApp(packageName)
                    }
                }
            }
        }
    }
} 