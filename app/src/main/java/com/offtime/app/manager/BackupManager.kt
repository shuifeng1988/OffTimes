package com.offtime.app.manager

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.offtime.app.data.dao.*
import com.offtime.app.data.entity.*
import com.offtime.app.data.network.UserApiService
import com.offtime.app.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.Result

/**
 * 用户数据备份管理器
 * 
 * 功能包括：
 * 1. 增量数据备份（每天只备份昨天新增的数据）
 * 2. 数据上传到云端服务器
 * 3. 数据恢复和下载
 * 4. 备份设置管理
 * 5. 网络状态检查（WiFi优先）
 */
@Singleton
class BackupManager @Inject constructor(
    private val context: Context,
    private val backupSettingsDao: BackupSettingsDao,
    private val appSessionUserDao: AppSessionUserDao,
    private val timerSessionUserDao: TimerSessionUserDao,
    private val appCategoryDao: AppCategoryDao,
    private val goalRewardPunishmentUserDao: GoalRewardPunishmentUserDao,
    private val userApiService: UserApiService,
    private val userRepository: UserRepository
) {
    
    companion object {
        private const val TAG = "BackupManager"
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        // 支持备份的基础数据表
        private val BACKUP_TABLES = listOf(
            "app_sessions_users",
            "timer_sessions_users",
            "AppCategory_Users",
            "goals_reward_punishment_users"
        )
    }
    
    /**
     * 数据备份结果
     */
    data class BackupResult(
        val success: Boolean,
        val message: String,
        val backupedTables: List<String> = emptyList(),
        val totalRecords: Int = 0,
        val errorDetails: String? = null
    )
    
    /**
     * 数据恢复结果
     */
    data class RestoreResult(
        val success: Boolean,
        val message: String,
        val restoredTables: List<String> = emptyList(),
        val totalRecords: Int = 0,
        val errorDetails: String? = null
    )
    
    /**
     * 执行增量数据备份
     * 只备份昨天新增或修改的数据
     */
    suspend fun performIncrementalBackup(): BackupResult = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "开始执行增量数据备份")
            
            // 1. 检查备份设置
            val settings = backupSettingsDao.getBackupSettings()
            if (settings?.backupEnabled != true) {
                Log.i(TAG, "备份功能已关闭，跳过备份")
                return@withContext BackupResult(false, "备份功能已关闭")
            }
            
            // 2. 检查网络连接
            if (settings.wifiOnlyBackup && !isWifiConnected()) {
                Log.w(TAG, "仅WiFi备份模式，当前非WiFi网络，跳过备份")
                return@withContext BackupResult(false, "等待WiFi网络连接")
            }
            
            if (!isNetworkAvailable()) {
                Log.w(TAG, "网络不可用，跳过备份")
                return@withContext BackupResult(false, "网络连接不可用")
            }
            
            // 3. 检查用户登录状态
            if (!userRepository.isUserLoggedIn()) {
                Log.w(TAG, "用户未登录，跳过备份")
                return@withContext BackupResult(false, "请先登录账户")
            }
            
            // 4. 获取昨天的日期
            val yesterday = getYesterday()
            Log.i(TAG, "准备备份日期: $yesterday")
            
            // 5. 备份各个基础数据表
            val successTables = mutableListOf<String>()
            var totalRecords = 0
            var hasError = false
            var lastError: String? = null
            
            for (tableName in BACKUP_TABLES) {
                try {
                    val backupData = collectTableData(tableName, yesterday)
                    
                    if (backupData.isEmpty()) {
                        Log.d(TAG, "表 $tableName 在 $yesterday 无新增数据，跳过备份")
                        continue
                    }
                    
                    val token = userRepository.getAccessToken() ?: throw Exception("访问令牌不可用")
                    val response = userApiService.uploadBackup(
                        authorization = "Bearer $token",
                        request = com.offtime.app.data.network.BackupUploadRequest(
                            tableName = tableName,
                            backupData = backupData,
                            backupDate = yesterday
                        )
                    )
                    
                    if (response.isSuccessful && response.body()?.code == 200) {
                        successTables.add(tableName)
                        totalRecords += backupData.size
                        Log.i(TAG, "表 $tableName 备份成功: ${backupData.size} 条记录")
                    } else {
                        val errorMsg = response.body()?.message ?: "网络请求失败"
                        Log.e(TAG, "表 $tableName 备份失败: $errorMsg")
                        hasError = true
                        lastError = "表 $tableName: $errorMsg"
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "备份表 $tableName 时出错", e)
                    hasError = true
                    lastError = "表 $tableName: ${e.message}"
                }
            }
            
            // 6. 更新备份结果
            val today = getTodayString()
            val result = when {
                successTables.isEmpty() && hasError -> "FAILED"
                successTables.isNotEmpty() && hasError -> "PARTIAL"
                else -> "SUCCESS"
            }
            
            backupSettingsDao.updateLastBackupInfo(
                date = today,
                result = result,
                error = lastError
            )
            
            val finalResult = BackupResult(
                success = successTables.isNotEmpty(),
                message = when (result) {
                    "SUCCESS" -> "数据备份完成：${successTables.size}个表，共${totalRecords}条记录"
                    "PARTIAL" -> "部分备份完成：${successTables.size}个表成功，共${totalRecords}条记录"
                    else -> "备份失败：$lastError"
                },
                backupedTables = successTables,
                totalRecords = totalRecords,
                errorDetails = lastError
            )
            
            Log.i(TAG, "增量备份完成: ${finalResult.message}")
            return@withContext finalResult
            
        } catch (e: Exception) {
            Log.e(TAG, "执行增量备份失败", e)
            
            // 记录失败信息
            try {
                backupSettingsDao.updateLastBackupInfo(
                    date = getTodayString(),
                    result = "FAILED", 
                    error = e.message
                )
            } catch (ignored: Exception) { }
            
            return@withContext BackupResult(
                success = false,
                message = "备份执行失败",
                errorDetails = e.message
            )
        }
    }
    
    /**
     * 恢复用户数据
     * @param dateFrom 开始日期
     * @param dateTo 结束日期（可选，默认到今天）
     * @param tableName 指定表名（可选，默认恢复所有表）
     */
    suspend fun restoreUserData(
        dateFrom: String,
        dateTo: String? = null,
        tableName: String? = null
    ): RestoreResult = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "开始恢复数据: dateFrom=$dateFrom, dateTo=$dateTo, table=$tableName")
            
            // 1. 检查用户登录状态
            if (!userRepository.isUserLoggedIn()) {
                return@withContext RestoreResult(false, "请先登录账户")
            }
            
            // 2. 检查网络连接
            if (!isNetworkAvailable()) {
                return@withContext RestoreResult(false, "网络连接不可用")
            }
            
            // 3. 从服务器下载备份数据
            val token = userRepository.getAccessToken() ?: throw Exception("访问令牌不可用")
            val response = userApiService.downloadBackup(
                authorization = "Bearer $token",
                tableName = tableName,
                dateFrom = dateFrom,
                dateTo = dateTo
            )
            
            if (!response.isSuccessful || response.body()?.code != 200) {
                val errorMsg = response.body()?.message ?: "网络请求失败"
                Log.e(TAG, "下载备份数据失败: $errorMsg")
                return@withContext RestoreResult(false, "下载失败: $errorMsg")
            }
            
            val backups = response.body()?.data?.backups ?: emptyList()
            if (backups.isEmpty()) {
                return@withContext RestoreResult(false, "未找到符合条件的备份数据")
            }
            
            // 4. 恢复数据到本地数据库
            val restoredTables = mutableListOf<String>()
            var totalRecords = 0
            
            for (backup in backups) {
                try {
                    val count = restoreTableData(backup.tableName, backup.backupData)
                    restoredTables.add(backup.tableName)
                    totalRecords += count
                    Log.i(TAG, "恢复表 ${backup.tableName}: $count 条记录")
                    
                } catch (e: Exception) {
                    Log.e(TAG, "恢复表 ${backup.tableName} 失败", e)
                    // 继续恢复其他表
                }
            }
            
            val result = RestoreResult(
                success = restoredTables.isNotEmpty(),
                message = if (restoredTables.isNotEmpty()) {
                    "数据恢复完成：${restoredTables.size}个表，共${totalRecords}条记录"
                } else {
                    "数据恢复失败"
                },
                restoredTables = restoredTables,
                totalRecords = totalRecords
            )
            
            Log.i(TAG, "数据恢复完成: ${result.message}")
            return@withContext result
            
        } catch (e: Exception) {
            Log.e(TAG, "恢复数据失败", e)
            return@withContext RestoreResult(
                success = false,
                message = "恢复失败: ${e.message}",
                errorDetails = e.message
            )
        }
    }
    
    /**
     * 手动执行备份（用于UI调用）
     */
    suspend fun performManualBackup(): Result<BackupResult> = withContext(Dispatchers.IO) {
        return@withContext try {
            val result = performIncrementalBackup()
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "手动备份失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 从备份恢复数据（用于UI调用）
     */
    suspend fun restoreFromBackup(
        dateFrom: String? = null,
        dateTo: String? = null,
        tableName: String? = null
    ): Result<RestoreResult> = withContext(Dispatchers.IO) {
        return@withContext try {
            val from = dateFrom ?: getYesterday()
            val result = restoreUserData(from, dateTo, tableName)
            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "数据恢复失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 获取备份信息概览
     */
    suspend fun getBackupInfo(): BackupInfo? = withContext(Dispatchers.IO) {
        try {
            if (!userRepository.isUserLoggedIn()) {
                return@withContext null
            }
            
            val token = userRepository.getAccessToken() ?: return@withContext null
            val response = userApiService.getBackupInfo("Bearer $token")
            if (response.isSuccessful && response.body()?.code == 200) {
                val data = response.body()?.data
                return@withContext BackupInfo(
                    backupEnabled = data?.settings?.backupEnabled ?: false,
                    backupTime = data?.settings?.backupTime ?: "02:00",
                    lastBackupDate = data?.settings?.lastBackupDate,
                    totalBackups = data?.settings?.totalBackups ?: 0,
                    totalDataSize = data?.settings?.totalDataSize ?: 0,
                    tableStats = data?.tableStats?.map { stat ->
                        TableStat(
                            tableName = stat.tableName,
                            backupCount = stat.backupCount,
                            latestBackup = stat.latestBackup,
                            totalRecords = stat.totalRecords,
                            totalSize = stat.totalSize
                        )
                    } ?: emptyList()
                )
            }
            
            return@withContext null
            
        } catch (e: Exception) {
            Log.e(TAG, "获取备份信息失败", e)
            return@withContext null
        }
    }
    
    /**
     * 更新备份设置
     */
    suspend fun updateBackupSettings(
        enabled: Boolean? = null,
        hour: Int? = null,
        minute: Int? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // 更新本地设置
            val current = backupSettingsDao.getBackupSettings() 
                ?: BackupSettingsEntity() // 使用默认值
            
            val updated = current.copy(
                backupEnabled = enabled ?: current.backupEnabled,
                backupTimeHour = hour ?: current.backupTimeHour,
                backupTimeMinute = minute ?: current.backupTimeMinute,
                updatedAt = System.currentTimeMillis()
            )
            
            backupSettingsDao.upsertBackupSettings(updated)
            
            // 同步到服务器
            if (userRepository.isUserLoggedIn() && isNetworkAvailable()) {
                try {
                    val token = userRepository.getAccessToken() ?: throw Exception("访问令牌不可用")
                    val response = userApiService.updateBackupSettings(
                        authorization = "Bearer $token",
                        request = com.offtime.app.data.network.BackupSettingsRequest(
                            backupEnabled = updated.backupEnabled,
                            backupTimeHour = updated.backupTimeHour,
                            backupTimeMinute = updated.backupTimeMinute
                        )
                    )
                    
                    if (!response.isSuccessful || response.body()?.code != 200) {
                        val errorMsg = response.body()?.message ?: "网络请求失败"
                        Log.w(TAG, "同步备份设置到服务器失败: $errorMsg")
                        // 不返回失败，本地设置已保存
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "同步备份设置异常", e)
                    // 不返回失败，本地设置已保存
                }
            }
            
            Log.i(TAG, "备份设置更新成功")
            return@withContext true
            
        } catch (e: Exception) {
            Log.e(TAG, "更新备份设置失败", e)
            return@withContext false
        }
    }
    
    /**
     * 收集指定表在指定日期的数据
     */
    private suspend fun collectTableData(tableName: String, date: String): List<Any> {
        return when (tableName) {
            "app_sessions_users" -> {
                appSessionUserDao.getSessionsByDate(date)
            }
            "timer_sessions_users" -> {
                timerSessionUserDao.getSessionsByDate(date)
            }
            "AppCategory_Users" -> {
                // 分类数据按更新时间筛选
                appCategoryDao.getCategoriesUpdatedOnDate(date)
            }
            "goals_reward_punishment_users" -> {
                // 目标奖惩数据按更新时间筛选
                goalRewardPunishmentUserDao.getGoalsUpdatedOnDate(date)
            }
            else -> emptyList()
        }
    }
    
    /**
     * 恢复表数据到本地数据库
     */
    private suspend fun restoreTableData(tableName: String, @Suppress("UNUSED_PARAMETER") data: List<Any>): Int {
        return when (tableName) {
            "app_sessions_users" -> {
                // 这里需要根据实际API响应的数据格式进行转换
                // 为简化，假设服务器返回的是正确的JSON格式
                // 实际实现时需要进行数据类型转换和验证
                0 // 占位返回值
            }
            "timer_sessions_users" -> {
                0 // 占位返回值  
            }
            "AppCategory_Users" -> {
                0 // 占位返回值
            }
            "goals_reward_punishment_users" -> {
                0 // 占位返回值
            }
            else -> 0
        }
    }
    
    /**
     * 检查是否连接到WiFi
     */
    private fun isWifiConnected(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
    
    /**
     * 检查网络是否可用
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    
    /**
     * 获取昨天的日期字符串
     */
    private fun getYesterday(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        return dateFormat.format(calendar.time)
    }
    
    /**
     * 获取今天的日期字符串
     */
    private fun getTodayString(): String {
        return dateFormat.format(Date())
    }
    
    /**
     * 备份信息数据类
     */
    data class BackupInfo(
        val backupEnabled: Boolean,
        val backupTime: String,
        val lastBackupDate: String?,
        val totalBackups: Int,
        val totalDataSize: Long,
        val tableStats: List<TableStat>
    )
    
    data class TableStat(
        val tableName: String,
        val backupCount: Int,
        val latestBackup: String?,
        val totalRecords: Int,
        val totalSize: Long
    )
} 