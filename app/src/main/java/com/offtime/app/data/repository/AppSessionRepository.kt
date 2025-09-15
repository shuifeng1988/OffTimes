package com.offtime.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.offtime.app.data.dao.AppSessionDefaultDao
import com.offtime.app.data.entity.AppInfoEntity
import com.offtime.app.data.dao.AppSessionUserDao
import com.offtime.app.data.dao.AppCategoryDao
import com.offtime.app.data.dao.AppInfoDao
import com.offtime.app.data.entity.AppSessionDefaultEntity
import com.offtime.app.data.entity.AppSessionUserEntity
import com.offtime.app.utils.AppCategoryUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSessionRepository @Inject constructor(
    private val appSessionDefaultDao: AppSessionDefaultDao,
    private val appSessionUserDao: AppSessionUserDao,
    private val appCategoryDao: AppCategoryDao,
    private val appInfoDao: AppInfoDao,
    private val appCategoryUtils: AppCategoryUtils,
    private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("usage_prefs", Context.MODE_PRIVATE)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    

    
    // 获取上次采集时间戳
    fun getLastTimestamp(): Long {
        return prefs.getLong("lastTs", 0L)
    }
    
    // 更新上次采集时间戳
    fun updateLastTimestamp(timestamp: Long) {
        prefs.edit().putLong("lastTs", timestamp).apply()
    }
    
    // 初始化默认表（如果为空）
    suspend fun initializeDefaultTableIfEmpty() = withContext(Dispatchers.IO) {
        val defaults = appSessionDefaultDao.getAllDefaults()
        if (defaults.isEmpty()) {
            // 从分类表获取所有分类
            val categories = appCategoryDao.getAllCategories().first()
            val defaultEntities = categories.map { category ->
                AppSessionDefaultEntity(catId = category.id)
            }
            appSessionDefaultDao.insertDefaults(defaultEntities)
        }
    }
    
    // 插入或更新会话（支持跨日期会话自动分割）
    suspend fun upsertSession(
        pkgName: String,
        startTime: Long,
        endTime: Long
    ) = withContext(Dispatchers.IO) {
        
        // 首先检查是否为需要过滤的系统应用或排除统计应用
        if (shouldFilterSystemApp(pkgName)) {
            android.util.Log.d("AppSessionRepository", "过滤应用会话: $pkgName")
            return@withContext
        }
        
        val originalDuration = ((endTime - startTime) / 1000).toInt()
        
        // 应用智能后台过滤
        val adjustedDuration = com.offtime.app.utils.BackgroundAppFilterUtils.adjustUsageDuration(
            packageName = pkgName,
            originalDuration = originalDuration,
            sessionStartTime = startTime,
            sessionEndTime = endTime
        )
        
        // 获取应用的最小有效时长
        val minValidDuration = com.offtime.app.utils.BackgroundAppFilterUtils.getMinimumValidDuration(pkgName)
        
        if (adjustedDuration >= minValidDuration) {
            // 检测是否为可能的后台唤醒模式
            val isBackgroundWakeup = com.offtime.app.utils.BackgroundAppFilterUtils.detectBackgroundWakeupPattern(
                pkgName, adjustedDuration, startTime, endTime
            )
            
            if (isBackgroundWakeup) {
                android.util.Log.w("AppSessionRepository", 
                    "检测到可能的后台唤醒模式: $pkgName, 时长:${adjustedDuration}秒，已跳过记录")
                return@withContext
            }
            
            // 只记录调整后仍然有效的会话
            var appInfo = appInfoDao.getAppByPackageName(pkgName)
            
            // 如果应用信息不存在，尝试自动创建
            if (appInfo == null) {
                android.util.Log.w("AppSessionRepository", "应用信息不存在，尝试自动创建: $pkgName")
                try {
                    // 使用智能分类创建应用信息
                    val categoryId = getCategoryIdByPackage(pkgName)
                    val newAppInfo = createAppInfoFromPackageName(pkgName, categoryId)
                    if (newAppInfo != null) {
                        appInfoDao.insertApp(newAppInfo)
                        appInfo = newAppInfo
                        android.util.Log.d("AppSessionRepository", "自动创建应用信息成功: $pkgName -> categoryId=$categoryId")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AppSessionRepository", "自动创建应用信息失败: $pkgName", e)
                }
            }
            
            if (appInfo != null) {
                val date = dateFormat.format(Date(startTime))
                
                // 使用调整后的时长创建会话
                val adjustedEndTime = startTime + (adjustedDuration * 1000L)
                val sessionEntity = AppSessionUserEntity(
                    id = 0,
                    pkgName = pkgName,
                    catId = appInfo.categoryId,
                    startTime = startTime,
                    endTime = adjustedEndTime,
                    durationSec = adjustedDuration,
                    date = date
                )
                
                appSessionUserDao.insertSession(sessionEntity)
                val startTimeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(startTime)
                val endTimeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(adjustedEndTime)
                
                if (adjustedDuration != originalDuration) {
                    android.util.Log.d("AppSessionRepository", 
                        "智能过滤会话已插入: $pkgName, ${startTimeStr}-${endTimeStr}, 原始时长:${originalDuration}秒 -> 调整后时长:${adjustedDuration}秒")
                } else {
                    android.util.Log.d("AppSessionRepository", 
                        "会话已插入: $pkgName, ${startTimeStr}-${endTimeStr}, 时长:${adjustedDuration}秒")
                }
            }
        } else {
            val filterLevel = com.offtime.app.utils.BackgroundAppFilterUtils.getFilterLevel(pkgName)
            android.util.Log.d("AppSessionRepository", 
                "会话过短被过滤: $pkgName, 原始时长:${originalDuration}秒, 调整后时长:${adjustedDuration}秒, 最小有效时长:${minValidDuration}秒, 过滤级别:${filterLevel}")
        }
    }
    
    /**
     * 智能插入或更新会话（新版本，支持连续使用的智能合并和跨日期会话分割）
     * 
     * 逻辑：
     * 1. 检查是否为跨日期会话，如果是则分割为多个会话
     * 2. 对每个会话分别检查是否存在可以合并的近期会话
     * 3. 如果存在且时间间隔小于5分钟，则更新现有会话的结束时间和时长
     * 4. 否则插入新的会话记录
     */
    suspend fun upsertSessionSmart(
        pkgName: String,
        startTime: Long,
        endTime: Long
    ) = withContext(Dispatchers.IO) {
        
        // 首先检查是否为需要过滤的系统应用或排除统计应用
        if (shouldFilterSystemApp(pkgName)) {
            android.util.Log.d("AppSessionRepository", "过滤应用会话: $pkgName")
            return@withContext
        }
        
        val originalDuration = ((endTime - startTime) / 1000).toInt()
        
        // 先验证会话时长是否合理，过滤异常的超长会话
        val isValidSession = com.offtime.app.utils.BackgroundAppFilterUtils.validateSessionDuration(
            packageName = pkgName,
            durationSeconds = originalDuration,
            sessionStartTime = startTime,
            sessionEndTime = endTime
        )
        
        if (!isValidSession) {
            android.util.Log.w("AppSessionRepository", "过滤异常超长会话: $pkgName, 原始时长:${originalDuration}秒(${originalDuration/3600.0}小时)")
            return@withContext
        }
        
        // 应用智能后台过滤
        val adjustedDuration = com.offtime.app.utils.BackgroundAppFilterUtils.adjustUsageDuration(
            packageName = pkgName,
            originalDuration = originalDuration,
            sessionStartTime = startTime,
            sessionEndTime = endTime
        )
        
        // 获取应用的最小有效时长
        val minValidDuration = com.offtime.app.utils.BackgroundAppFilterUtils.getMinimumValidDuration(pkgName)
        
        if (adjustedDuration >= minValidDuration) {
            // 检测是否为可能的后台唤醒模式
            val isBackgroundWakeup = com.offtime.app.utils.BackgroundAppFilterUtils.detectBackgroundWakeupPattern(
                pkgName, adjustedDuration, startTime, endTime
            )
            
            if (isBackgroundWakeup) {
                android.util.Log.w("AppSessionRepository", 
                    "智能合并检测到可能的后台唤醒模式: $pkgName, 时长:${adjustedDuration}秒，已跳过记录")
                return@withContext
            }
            
            // 记录原始时长调整信息
            if (adjustedDuration != originalDuration) {
                android.util.Log.d("AppSessionRepository", 
                    "智能合并前应用过滤: $pkgName, 原始时长:${originalDuration}秒 -> 调整后时长:${adjustedDuration}秒")
            }
            
            // 使用调整后的时长进行智能合并处理
            val adjustedEndTime = startTime + (adjustedDuration * 1000L)
            
            // 如果应用信息不存在，尝试自动创建
            var appInfo = appInfoDao.getAppByPackageName(pkgName)
            if (appInfo == null) {
                android.util.Log.w("AppSessionRepository", "应用信息不存在，尝试自动创建: $pkgName")
                try {
                    val categoryId = getCategoryIdByPackage(pkgName)
                    val newAppInfo = createAppInfoFromPackageName(pkgName, categoryId)
                    if (newAppInfo != null) {
                        appInfoDao.insertApp(newAppInfo)
                        appInfo = newAppInfo
                        android.util.Log.d("AppSessionRepository", "自动创建应用信息成功: $pkgName -> categoryId=$categoryId")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AppSessionRepository", "自动创建应用信息失败: $pkgName", e)
                    return@withContext
                }
            }
            
            if (appInfo == null) {
                android.util.Log.e("AppSessionRepository", "无法获取或创建应用信息，会话数据丢失: $pkgName")
                return@withContext
            }
            
            // 检查是否为跨日期会话，如果是则分割为多个会话
            val startDate = dateFormat.format(Date(startTime))
            val endDate = dateFormat.format(Date(adjustedEndTime))
            
            if (startDate != endDate) {
                // 跨日期会话，分割处理
                splitCrossDaySession(appInfo, pkgName, startTime, adjustedEndTime, adjustedDuration, startDate, endDate)
                return@withContext
            }
            
            // 同一天的会话，进行智能合并处理
            val date = startDate
            
                        // 根据应用类型定义不同的合并间隙阈值
            val mergeGapMillis = when {
                // Chrome和其他浏览器使用更长的合并间隙（60秒）
                pkgName.contains("chrome", ignoreCase = true) || 
                pkgName.contains("browser", ignoreCase = true) -> 60 * 1000L
                // 视频和音乐应用也使用较长间隙（30秒）
                pkgName.contains("music", ignoreCase = true) ||
                pkgName.contains("video", ignoreCase = true) ||
                pkgName.contains("youtube", ignoreCase = true) -> 30 * 1000L
                // 其他应用保持原来的10秒
                else -> 10 * 1000L
            }
            
            // 查找在合并间隙内的最近会话
            val recentSession = appSessionUserDao.getRecentSessionByPackage(
                pkgName, 
                startTime - mergeGapMillis, // 最早结束时间
                startTime                   // 当前会话开始时间
            )
            
            // 使用动态间隙阈值作为合并条件
            if (recentSession != null) {
                val gap = startTime - recentSession.endTime
                val isChrome = pkgName.contains("chrome", ignoreCase = true) || pkgName.contains("browser", ignoreCase = true)
                val logPrefix = if (isChrome) "🔍" else "📱"
                
                android.util.Log.d("AppSessionRepository", "$logPrefix 找到最近会话: $pkgName, 时间间隙: ${gap/1000}秒, 合并阈值: ${mergeGapMillis/1000}秒")
                
                if (gap <= mergeGapMillis) {
                    // 存在可以合并的会话，更新现有会话
                    val mergedEndTime = adjustedEndTime
                    val mergedDuration = ((mergedEndTime - recentSession.startTime) / 1000).toInt()
                    
                    appSessionUserDao.updateSessionEndTime(recentSession.id, mergedEndTime, mergedDuration)
                    
                    val recentStartStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(recentSession.startTime)
                    val mergedEndStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(mergedEndTime)
                    
                    android.util.Log.d("AppSessionRepository", 
                        "✅ $logPrefix 智能合并会话成功: $pkgName, ${recentStartStr}-${mergedEndStr}, 间隙:${gap/1000}秒(≤${mergeGapMillis/1000}s), 合并后时长:${mergedDuration}秒")
                    return@withContext
                }
            }
            
            // 检查是否存在重复会话（相同开始时间）
            val duplicateSession = appSessionUserDao.getActiveSessionByPackage(pkgName, date, startTime)
            if (duplicateSession != null) {
                android.util.Log.w("AppSessionRepository", "⚠️ 检测到重复会话，跳过插入: $pkgName, 开始时间: ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(startTime)}")
                return@withContext
            }
            
            // 没有找到可合并的会话且无重复，插入新记录
            insertSingleSession(appInfo, pkgName, startTime, adjustedEndTime, date, adjustedDuration, originalDuration)
        } else {
            val filterLevel = com.offtime.app.utils.BackgroundAppFilterUtils.getFilterLevel(pkgName)
            android.util.Log.d("AppSessionRepository", 
                "智能合并会话过短被过滤: $pkgName, 原始时长:${originalDuration}秒, 调整后时长:${adjustedDuration}秒, 最小有效时长:${minValidDuration}秒, 过滤级别:${filterLevel}")
        }
    }
    
    /**
     * 处理单日内会话的智能合并逻辑
     */
    private suspend fun processSmartSessionForSingleDay(
        appInfo: com.offtime.app.data.entity.AppInfoEntity,
        pkgName: String,
        startTime: Long,
        endTime: Long,
        date: String
    ) {
        val duration = ((endTime - startTime) / 1000).toInt()
        
        // 查找可以合并或重叠的会话
        val overlappingSession = appSessionUserDao.findOverlappingOrMergeableSession(
            pkgName = pkgName,
            date = date,
            newStartTime = startTime,
            newEndTime = endTime
        )
        
        if (overlappingSession != null) {
            // 找到重叠或可合并的会话，计算合并后的时间范围
            val mergedStartTime = minOf(overlappingSession.startTime, startTime)
            val mergedEndTime = maxOf(overlappingSession.endTime, endTime)
            val newDuration = ((mergedEndTime - mergedStartTime) / 1000).toInt()
            
            // 检查合并后是否仍为跨日期会话
            val mergedStartDate = dateFormat.format(Date(mergedStartTime))
            val mergedEndDate = dateFormat.format(Date(mergedEndTime))
            
            if (mergedStartDate == mergedEndDate) {
                // 合并后仍在同一天，正常合并
                appSessionUserDao.updateSessionTimeRange(
                    sessionId = overlappingSession.id,
                    newStartTime = mergedStartTime,
                    newEndTime = mergedEndTime,
                    newDurationSec = newDuration
                )
                
                val startTimeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(mergedStartTime)
                val endTimeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(mergedEndTime)
                val oldStartTimeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(overlappingSession.startTime)
                val oldEndTimeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(overlappingSession.endTime)
                android.util.Log.d("AppSessionRepository", 
                    "智能合并重叠会话: $pkgName")
                android.util.Log.d("AppSessionRepository", 
                    "  原会话: ${oldStartTimeStr}-${oldEndTimeStr} (${overlappingSession.durationSec}秒)")
                android.util.Log.d("AppSessionRepository", 
                    "  新会话: ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(startTime)}-${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(endTime)} (${duration}秒)")
                android.util.Log.d("AppSessionRepository", 
                    "  合并后: ${startTimeStr}-${endTimeStr} (${newDuration}秒)")
            } else {
                // 合并后跨日期，不合并，直接插入新会话
                android.util.Log.d("AppSessionRepository", "合并后跨日期，不进行合并: $pkgName")
                insertSingleSession(appInfo, pkgName, startTime, endTime, date)
            }
        } else {
            // 没有找到可合并的会话，插入新记录
            insertSingleSession(appInfo, pkgName, startTime, endTime, date)
        }
    }
    
    /**
     * 插入单个会话记录（支持智能过滤后的时长）
     */
    private suspend fun insertSingleSession(
        appInfo: com.offtime.app.data.entity.AppInfoEntity,
        pkgName: String,
        startTime: Long,
        endTime: Long,
        date: String,
        adjustedDuration: Int? = null,
        originalDuration: Int? = null
    ) {
        val duration = adjustedDuration ?: ((endTime - startTime) / 1000).toInt()
        val sessionEntity = AppSessionUserEntity(
            id = 0,
            pkgName = pkgName,
            catId = appInfo.categoryId,
            startTime = startTime,
            endTime = endTime,
            durationSec = duration,
            date = date
        )
        
        appSessionUserDao.insertSession(sessionEntity)
        val startTimeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(startTime)
        val endTimeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(endTime)
        
        if (originalDuration != null && adjustedDuration != null && adjustedDuration != originalDuration) {
            android.util.Log.d("AppSessionRepository", 
                "智能过滤会话已插入: $pkgName, ${startTimeStr}-${endTimeStr}, 原始时长:${originalDuration}秒 -> 调整后时长:${adjustedDuration}秒, 日期:${date}")
        } else {
            android.util.Log.d("AppSessionRepository", 
                "新会话已插入: $pkgName, ${startTimeStr}-${endTimeStr}, 时长:${duration}秒, 日期:${date}")
        }
    }
    
    /**
     * 处理跨日期会话的分割
     */
    private suspend fun splitCrossDaySession(
        appInfo: com.offtime.app.data.entity.AppInfoEntity,
        pkgName: String,
        startTime: Long,
        endTime: Long,
        totalDuration: Int,
        startDate: String,
        endDate: String
    ) {
        android.util.Log.d("AppSessionRepository", "处理跨日期会话: $pkgName, 开始=${startDate}, 结束=${endDate}, 总时长:${totalDuration}秒")
        
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = startTime
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
        calendar.set(java.util.Calendar.MINUTE, 59)
        calendar.set(java.util.Calendar.SECOND, 59)
        calendar.set(java.util.Calendar.MILLISECOND, 999)
        val firstDayEndTime = calendar.timeInMillis
        
        // 第二天开始时间
        calendar.timeInMillis = endTime
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val secondDayStartTime = calendar.timeInMillis
        
        // 按实际时间比例分配时长
        val totalTimeSpan = endTime - startTime
        val firstDayTimeSpan = firstDayEndTime - startTime + 1000 // +1秒包含23:59:59
        @Suppress("UNUSED_VARIABLE")
        val secondDayTimeSpan = endTime - secondDayStartTime
        
        // 第一个会话：开始时间到第一天结束
        val firstDayDuration = ((firstDayTimeSpan.toDouble() / totalTimeSpan.toDouble()) * totalDuration).toInt()
        val minValidDuration = com.offtime.app.utils.BackgroundAppFilterUtils.getMinimumValidDuration(pkgName)
        
        if (firstDayDuration >= minValidDuration) {
            val firstSessionEntity = AppSessionUserEntity(
                id = 0,
                pkgName = pkgName,
                catId = appInfo.categoryId,
                startTime = startTime,
                endTime = firstDayEndTime,
                durationSec = firstDayDuration,
                date = startDate
            )
            appSessionUserDao.insertSession(firstSessionEntity)
            android.util.Log.d("AppSessionRepository", "跨日期会话第一部分已插入: $pkgName, 日期=${startDate}, 时长=${firstDayDuration}秒")
        }
        
        // 第二个会话：第二天开始到结束时间
        val secondDayDuration = totalDuration - firstDayDuration
        if (secondDayDuration >= minValidDuration) {
            val secondSessionEntity = AppSessionUserEntity(
                id = 0,
                pkgName = pkgName,
                catId = appInfo.categoryId,
                startTime = secondDayStartTime,
                endTime = endTime,
                durationSec = secondDayDuration,
                date = endDate
            )
            appSessionUserDao.insertSession(secondSessionEntity)
            android.util.Log.d("AppSessionRepository", "跨日期会话第二部分已插入: $pkgName, 日期=${endDate}, 时长=${secondDayDuration}秒")
        }
    }
    
    /**
     * 实时更新当前活跃应用的使用时长
     * 用于每分钟更新正在使用的应用时长，无需等待应用结束
     * 
     * @param pkgName 应用包名，如果为null则更新所有活跃应用
     * @param currentStartTime 当前会话开始时间
     * @param currentTime 当前时间
     */
    suspend fun updateActiveSessionDuration(
        pkgName: String?,
        currentStartTime: Long,
        currentTime: Long
    ) = withContext(Dispatchers.IO) {
        
        // 如果pkgName为null，通过回调获取所有活跃应用并更新
        if (pkgName == null) {
            updateAllActiveApplications(currentTime)
            return@withContext
        }
        
        // 首先检查是否为需要过滤的系统应用
        if (shouldFilterSystemApp(pkgName)) {
            return@withContext
        }
        
        val date = dateFormat.format(Date(currentStartTime))
        val originalDuration = ((currentTime - currentStartTime) / 1000).toInt()
        
        // 应用智能后台过滤到实时使用时长
        val adjustedDuration = com.offtime.app.utils.BackgroundAppFilterUtils.adjustUsageDuration(
            packageName = pkgName,
            originalDuration = originalDuration,
            sessionStartTime = currentStartTime,
            sessionEndTime = currentTime
        )
        
        // 获取应用的最小有效时长（实时更新使用更低的阈值以保证响应性）
        val minValidDuration = com.offtime.app.utils.BackgroundAppFilterUtils.getMinimumValidDuration(pkgName)
        // 对于OffTimes自身，实时更新不设阈值，确保所有使用都被记录
        val realtimeThreshold = if (pkgName.contains("offtime")) 0 else minValidDuration
        
        // 只有调整后的使用时长超过阈值才开始记录
        if (adjustedDuration >= realtimeThreshold) {
            // 检测是否为可能的后台唤醒模式
            val isBackgroundWakeup = com.offtime.app.utils.BackgroundAppFilterUtils.detectBackgroundWakeupPattern(
                pkgName, adjustedDuration, currentStartTime, currentTime
            )
            
            if (isBackgroundWakeup) {
                android.util.Log.w("AppSessionRepository", 
                    "实时更新检测到可能的后台唤醒模式: $pkgName, 时长:${adjustedDuration}秒，已跳过更新")
                return@withContext
            }
            
            var appInfo = appInfoDao.getAppByPackageName(pkgName)
            
            // 如果应用信息不存在，尝试自动创建
            if (appInfo == null) {
                try {
                    val categoryId = getCategoryIdByPackage(pkgName)
                    val newAppInfo = createAppInfoFromPackageName(pkgName, categoryId)
                    if (newAppInfo != null) {
                        appInfoDao.insertApp(newAppInfo)
                        appInfo = newAppInfo
                        android.util.Log.d("AppSessionRepository", "实时更新时自动创建应用信息: $pkgName -> categoryId=$categoryId")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AppSessionRepository", "实时更新时创建应用信息失败: $pkgName", e)
                }
            }
            
            if (appInfo != null) {
                // 查找或创建当前会话
                val existingSession = appSessionUserDao.getActiveSessionByPackage(pkgName, date, currentStartTime)
                
                if (existingSession != null) {
                    // 更新现有会话的结束时间和时长（使用调整后的时长）
                    val adjustedEndTime = currentStartTime + (adjustedDuration * 1000L)
                    appSessionUserDao.updateSessionEndTime(existingSession.id, adjustedEndTime, adjustedDuration)
                    
                    if (adjustedDuration != originalDuration) {
                        android.util.Log.d("AppSessionRepository", 
                            "实时更新会话(已过滤): $pkgName, 原始时长:${originalDuration}秒 -> 调整后时长:${adjustedDuration}秒")
                    } else {
                        android.util.Log.d("AppSessionRepository", 
                            "实时更新会话: $pkgName, 时长:${adjustedDuration}秒")
                    }
                } else {
                    // 创建新的会话记录（使用调整后的时长）
                    val adjustedEndTime = currentStartTime + (adjustedDuration * 1000L)
                    val sessionEntity = AppSessionUserEntity(
                        id = 0,
                        pkgName = pkgName,
                        catId = appInfo.categoryId,
                        startTime = currentStartTime,
                        endTime = adjustedEndTime,
                        durationSec = adjustedDuration,
                        date = date
                    )
                    
                    appSessionUserDao.insertSession(sessionEntity)
                    
                    if (adjustedDuration != originalDuration) {
                        android.util.Log.d("AppSessionRepository", 
                            "实时创建新会话(已过滤): $pkgName, 原始时长:${originalDuration}秒 -> 调整后时长:${adjustedDuration}秒")
                    } else {
                        android.util.Log.d("AppSessionRepository", 
                            "实时创建新会话: $pkgName, 时长:${adjustedDuration}秒")
                    }
                }
            }
        } else {
            val filterLevel = com.offtime.app.utils.BackgroundAppFilterUtils.getFilterLevel(pkgName)
            android.util.Log.v("AppSessionRepository", 
                "实时更新时长不足被跳过: $pkgName, 原始时长:${originalDuration}秒, 调整后时长:${adjustedDuration}秒, 阈值:${realtimeThreshold}秒, 过滤级别:${filterLevel}")
        }
    }
    
    /**
     * 检查是否应该过滤这个应用的使用时间记录
     * 主要过滤系统启动器、核心系统组件和"排除统计"分类的应用
     */
    private suspend fun shouldFilterSystemApp(packageName: String): Boolean {
        // 系统应用黑名单：需要过滤的系统核心组件和启动器
        val systemBlacklist = setOf(
            "android",
            "com.android.systemui",
            "com.android.settings",
            "com.android.vending",
            "com.google.android.gms",
            "com.google.android.gsf",
            "com.android.phone",
            "com.android.contacts",
            "com.android.dialer",
            "com.android.mms",
            "com.android.bluetooth",
            "com.android.nfc",
            "com.android.server.telecom",
            "com.android.shell",
            "com.android.sharedstoragebackup",
            "com.android.externalstorage",
            "com.android.providers.media",
            "com.android.providers.downloads",
            "com.android.wallpaper.livepicker",
            "com.android.inputmethod.latin",
            "com.android.cellbroadcastreceiver",
            "com.android.emergency",
            "com.miui.securitycenter",     // MIUI安全中心
            "com.miui.securityadd",        // MIUI安全组件
            "com.xiaomi.finddevice",       // 查找设备
            // 小米/MIUI系统应用
            "com.miui.powerkeeper",        // MIUI电量和性能
            "com.miui.battery",            // MIUI电池管理
            "com.miui.cleanmaster",        // MIUI清理大师
            "com.miui.antispam",           // MIUI骚扰拦截
            "com.miui.personalassistant",  // MIUI智能助理
            "com.miui.voiceassist",        // 小爱同学
            "com.xiaomi.aiasst.service",   // 小爱助手服务
            "com.xiaomi.market",           // 小米应用商店
            "com.xiaomi.gamecenter",       // 小米游戏中心
            "com.xiaomi.payment",          // 小米支付
            "com.xiaomi.smarthome",        // 米家
            "com.xiaomi.xmsf",             // 小米服务框架
            // 华为/荣耀系统应用
            "com.huawei.appmarket",        // 华为应用市场
            "com.huawei.gamecenter",       // 华为游戏中心
            "com.huawei.health",           // 华为健康
            "com.huawei.hiassistant",      // 华为智慧助手
            "com.huawei.hicare",           // 华为服务
            "com.huawei.hicloud",          // 华为云空间
            "com.huawei.hivoice",          // 华为语音助手
            "com.huawei.securitymgr",      // 华为手机管家
            "com.huawei.systemmanager",    // 华为系统管理
            "com.huawei.wallet",           // 华为钱包
            // 三星系统应用
            "com.samsung.android.bixby.service", // Bixby服务
            "com.samsung.android.health",  // 三星健康
            "com.samsung.android.samsungpass", // 三星通行证
            "com.samsung.android.spay",    // 三星支付
            "com.samsung.android.wellbeing", // 三星数字健康
            "com.samsung.knox.securefolder", // 三星安全文件夹
            // vivo系统应用
            "com.vivo.appstore",           // vivo应用商店
            "com.vivo.gamecenter",         // vivo游戏中心
            "com.vivo.health",             // vivo健康
            "com.vivo.jovi",               // Jovi智能助手
            "com.vivo.pushservice",        // vivo推送服务
            "com.vivo.wallet",             // vivo钱包
            "com.bbk.account",             // BBK账户
            "com.bbk.cloud",               // BBK云服务
            // OPPO系统应用
            "com.oppo.battery",            // OPPO电池管理
            "com.oppo.powermanager",       // OPPO电源管理
            "com.oppo.breeno",             // 小布助手
            "com.oppo.breeno.service",     // 小布助手服务
            "com.oppo.breeno.speech",      // 小布语音
            "com.oppo.breeno.assistant",   // 小布助手主程序
            "com.oppo.safecenter",         // OPPO手机管家
            "com.oppo.usercenter",         // OPPO用户中心
            "com.oppo.ota",                // OPPO系统更新
            "com.oppo.oppopush",           // OPPO推送服务
            "com.oppo.statistics.rom",     // OPPO统计服务
            "com.oppo.secscanservice",     // OPPO安全扫描
            "com.oppo.securityguard",      // OPPO安全卫士
            "com.oppo.sysoptimizer",       // OPPO系统优化
            "com.oppo.usagestats",         // OPPO使用统计
            "com.oppo.wellbeing",          // OPPO数字健康
            // 启动器应用（系统桌面）- 不应被统计使用时间
            "com.google.android.apps.nexuslauncher", // Pixel Launcher
            "com.android.launcher",        // 原生Android Launcher
            "com.android.launcher3",       // Android Launcher3
            "com.miui.home",              // MIUI桌面
            "com.huawei.android.launcher", // 华为桌面
            "com.oppo.launcher",          // OPPO桌面
            "com.vivo.launcher",          // vivo桌面
            "com.oneplus.launcher",       // OnePlus桌面
            "com.sec.android.app.launcher", // 三星桌面
            "com.sonymobile.home"         // 索尼桌面
        )
        
        // 如果在黑名单中，过滤掉
        val isSystemBlacklisted = systemBlacklist.any { packageName.startsWith(it) }
        
        if (isSystemBlacklisted) {
            android.util.Log.d("AppSessionRepository", "系统应用过滤: $packageName")
            return true
        }
        
        // 检查应用是否被排除
        try {
            val existingApp = appInfoDao.getAppByPackageName(packageName)
            if (existingApp?.isExcluded == true) {
                android.util.Log.d("AppSessionRepository", "排除应用过滤: $packageName")
                return true
            }
        } catch (e: Exception) {
            android.util.Log.e("AppSessionRepository", "检查应用排除状态失败: ${e.message}", e)
        }
        
        return false
    }
    
    /**
     * 更新所有活跃应用的使用时长
     * 这个方法将委托给UsageStatsCollectorService来处理
     */
    private suspend fun updateAllActiveApplications(currentTime: Long) {
        // 此方法的实现被移到UnifiedUpdateService中，直接调用UsageStatsCollectorService的方法
        // 避免在Repository层直接依赖Service层
        android.util.Log.d("AppSessionRepository", "批量更新活跃应用的功能已转移到UnifiedUpdateService, 时间戳: $currentTime")
    }
    
    /**
     * 从包名创建应用信息
     */
    private suspend fun createAppInfoFromPackageName(packageName: String, categoryId: Int): AppInfoEntity? {
        return try {
            val packageManager = context.packageManager
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            
            val appName = appInfo.loadLabel(packageManager).toString()
            val versionName = packageInfo.versionName ?: "Unknown"
            val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            
            val isSystemApp = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            
            AppInfoEntity(
                packageName = packageName,
                appName = appName,
                versionName = versionName,
                versionCode = versionCode,
                isSystemApp = isSystemApp,
                categoryId = categoryId,
                firstInstallTime = packageInfo.firstInstallTime,
                lastUpdateTime = packageInfo.lastUpdateTime,
                isEnabled = appInfo.enabled,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            android.util.Log.e("AppSessionRepository", "创建应用信息失败: $packageName", e)
            null
        }
    }

    // 获取包名对应的分类ID
    private suspend fun getCategoryIdByPackage(pkgName: String): Int {
        return try {
            // 优先从app_info_users表查询用户的实际分类设置
            val existingApp = appInfoDao.getAppByPackageName(pkgName)
            
            if (existingApp != null) {
                // 如果应用已存在于app_info_users表，使用用户设置的分类
                android.util.Log.d("AppSessionRepository", 
                    "使用用户设置的分类: $pkgName -> categoryId=${existingApp.categoryId}")
                existingApp.categoryId
            } else {
                // 如果应用不存在，使用智能分类
                val smartCategoryId = appCategoryUtils.getCategoryIdByPackageName(pkgName)
                android.util.Log.d("AppSessionRepository", 
                    "使用智能分类: $pkgName -> categoryId=$smartCategoryId")
                smartCategoryId
            }
        } catch (e: Exception) {
            android.util.Log.e("AppSessionRepository", "获取应用分类失败: ${e.message}", e)
            // 回退到智能分类
            appCategoryUtils.getCategoryIdByPackageName(pkgName)
        }
    }
    
    // 获取今日某分类的总时长
    suspend fun getTodayDurationByCategory(catId: Int): Int = withContext(Dispatchers.IO) {
        val today = dateFormat.format(Date())
        appSessionUserDao.getTotalDurationByCatIdAndDate(catId, today) ?: 0
    }
    
    // 获取今日所有会话（聚合后的智能会话）
    suspend fun getTodaySessions(): List<AppSessionUserEntity> = withContext(Dispatchers.IO) {
        val today = dateFormat.format(Date())
        val rawSessions = appSessionUserDao.getSessionsByDate(today)
        
        // 对同一应用的相近会话进行聚合
        aggregateNearSessions(rawSessions)
    }
    
    // 聚合相近的会话（解决短暂切换问题）
    private fun aggregateNearSessions(sessions: List<AppSessionUserEntity>): List<AppSessionUserEntity> {
        if (sessions.isEmpty()) return sessions
        
        val grouped = sessions.groupBy { it.pkgName }
        val result = mutableListOf<AppSessionUserEntity>()
        
        grouped.forEach { (_, pkgSessions) ->
            val sortedSessions = pkgSessions.sortedBy { it.startTime }
            var currentGroup = mutableListOf<AppSessionUserEntity>()
            
            for (session in sortedSessions) {
                if (currentGroup.isEmpty()) {
                    currentGroup.add(session)
                } else {
                    val lastSession = currentGroup.last()
                    // 如果与上一个会话间隔小于2分钟，则合并
                    if (session.startTime - lastSession.endTime <= 120_000) {
                        currentGroup.add(session)
                    } else {
                        // 输出当前组的聚合结果
                        if (currentGroup.isNotEmpty()) {
                            result.add(mergeSessionGroup(currentGroup))
                        }
                        currentGroup = mutableListOf(session)
                    }
                }
            }
            
            // 处理最后一组
            if (currentGroup.isNotEmpty()) {
                result.add(mergeSessionGroup(currentGroup))
            }
        }
        
        return result.sortedByDescending { it.updateTime }
    }
    
    // 合并会话组
    private fun mergeSessionGroup(sessions: List<AppSessionUserEntity>): AppSessionUserEntity {
        if (sessions.size == 1) return sessions.first()
        
        val first = sessions.first()
        val last = sessions.last()
        val totalDuration = sessions.sumOf { it.durationSec }
        
        return first.copy(
            startTime = first.startTime,
            endTime = last.endTime,
            durationSec = totalDuration,
            updateTime = last.updateTime
        )
    }
    
    // 清理过期数据 - 原始会话数据保留至少60天
    suspend fun cleanOldSessions(daysToKeep: Int = 60) = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -daysToKeep)
        val cutoffDate = dateFormat.format(calendar.time)
        appSessionUserDao.deleteOldSessions(cutoffDate)
        android.util.Log.i("AppSessionRepository", "清理了${cutoffDate}之前的app_sessions_users数据")
    }
    
    /**
     * 清理系统应用的历史会话记录
     * 删除已录入的启动器等系统应用数据
     */
    suspend fun cleanSystemAppSessions() = withContext(Dispatchers.IO) {
        try {
            android.util.Log.i("AppSessionRepository", "开始清理系统应用会话记录...")
            
            // 系统应用黑名单（与shouldFilterSystemApp保持一致）
            val systemBlacklist = setOf(
                "android",
                "com.android.systemui",
                "com.android.settings",
                "com.android.vending", 
                "com.google.android.gms",
                "com.google.android.gsf",
                "com.android.phone",
                "com.android.contacts",
                "com.android.dialer",
                "com.android.mms",
                "com.android.bluetooth",
                "com.android.nfc",
                "com.android.server.telecom",
                "com.android.shell",
                "com.android.sharedstoragebackup",
                "com.android.externalstorage",
                "com.android.providers.media",
                "com.android.providers.downloads",
                "com.android.wallpaper.livepicker",
                "com.android.inputmethod.latin",
                "com.android.cellbroadcastreceiver",
                "com.android.emergency",
                "com.miui.securitycenter",
                "com.miui.securityadd",
                "com.xiaomi.finddevice",
                // 启动器应用（系统桌面）
                "com.google.android.apps.nexuslauncher",
                "com.android.launcher",
                "com.android.launcher3",
                "com.miui.home",
                "com.huawei.android.launcher",
                "com.oppo.launcher",
                "com.vivo.launcher",
                "com.oneplus.launcher",
                "com.sec.android.app.launcher",
                "com.sonymobile.home"
            )
            
            // 获取所有会话记录
            val allSessions = appSessionUserDao.getAllSessions()
            var deletedCount = 0
            
            for (session in allSessions) {
                val shouldDelete = systemBlacklist.any { session.pkgName.startsWith(it) }
                if (shouldDelete) {
                    // 删除这个会话记录
                    appSessionUserDao.deleteSessionById(session.id)
                    android.util.Log.d("AppSessionRepository", "删除系统应用会话: ${session.pkgName} (${session.durationSec}秒)")
                    deletedCount++
                }
            }
            
            android.util.Log.i("AppSessionRepository", "系统应用会话清理完成，共删除 $deletedCount 条记录")
            
        } catch (e: Exception) {
            android.util.Log.e("AppSessionRepository", "清理系统应用会话失败: ${e.message}", e)
        }
    }
    
    /**
     * 同步历史会话的分类ID与app_info_users表保持一致
     * 当用户修改应用分类后，调用此方法更新历史会话记录
     */
    suspend fun syncHistorySessionCategories(packageName: String? = null) = withContext(Dispatchers.IO) {
        try {
            val allSessions = if (packageName != null) {
                appSessionUserDao.getSessionsByPackageNameDebug(packageName)
            } else {
                appSessionUserDao.getAllSessions()
            }
            
            var updatedCount = 0
            for (session in allSessions) {
                val currentCategoryId = getCategoryIdByPackage(session.pkgName)
                if (session.catId != currentCategoryId) {
                    val updatedSession = session.copy(
                        catId = currentCategoryId,
                        updateTime = System.currentTimeMillis()
                    )
                    appSessionUserDao.updateSession(updatedSession)
                    updatedCount++
                    android.util.Log.d("AppSessionRepository", 
                        "同步会话分类: ${session.pkgName} ID=${session.id} catId ${session.catId} -> $currentCategoryId")
                }
            }
            
            android.util.Log.i("AppSessionRepository", "历史会话分类同步完成，更新了 $updatedCount 条记录")
        } catch (e: Exception) {
            android.util.Log.e("AppSessionRepository", "同步历史会话分类失败: ${e.message}", e)
        }
    }
    
    /**
     * 根据包名和日期获取会话记录（用于数据修复）
     */
    suspend fun getSessionsByPackageName(packageName: String, date: String): List<AppSessionUserEntity> = withContext(Dispatchers.IO) {
        try {
            val allSessions = appSessionUserDao.getSessionsByPackageNameDebug(packageName)
            return@withContext allSessions.filter { it.date == date }
        } catch (e: Exception) {
            android.util.Log.e("AppSessionRepository", "获取指定应用会话失败: $packageName", e)
            return@withContext emptyList()
        }
    }
    
    /**
     * 根据会话ID删除记录（用于数据修复）
     */
    suspend fun deleteSessionById(sessionId: Int) = withContext(Dispatchers.IO) {
        try {
            appSessionUserDao.deleteSessionById(sessionId)
            android.util.Log.d("AppSessionRepository", "删除会话记录: sessionId=$sessionId")
        } catch (e: Exception) {
            android.util.Log.e("AppSessionRepository", "删除会话记录失败: sessionId=$sessionId", e)
        }
    }
    
    /**
     * 更新会话时长（用于数据修复）
     */
    suspend fun updateSessionDuration(sessionId: Int, newDurationSec: Int) = withContext(Dispatchers.IO) {
        try {
            // 获取原会话记录
            val allSessions = appSessionUserDao.getAllSessions()
            val session = allSessions.find { it.id == sessionId }
            
            if (session != null) {
                val newEndTime = session.startTime + (newDurationSec * 1000L)
                appSessionUserDao.updateSessionEndTimeAndDuration(sessionId, newEndTime, newDurationSec)
                android.util.Log.d("AppSessionRepository", "更新会话时长: sessionId=$sessionId, ${session.durationSec}s -> ${newDurationSec}s")
            } else {
                android.util.Log.w("AppSessionRepository", "未找到要更新的会话: sessionId=$sessionId")
            }
        } catch (e: Exception) {
            android.util.Log.e("AppSessionRepository", "更新会话时长失败: sessionId=$sessionId", e)
        }
    }
    
    /**
     * 获取总会话数量（用于数据统计）
     */
    suspend fun getTotalSessionCount(): Int = withContext(Dispatchers.IO) {
        try {
            return@withContext appSessionUserDao.getTotalSessionCount()
        } catch (e: Exception) {
            android.util.Log.e("AppSessionRepository", "获取总会话数量失败", e)
            return@withContext 0
        }
    }
    
    /**
     * 获取当前活跃会话的使用时间（按分类统计）
     * 用于实时显示正在进行中的应用使用时间
     */
    suspend fun getCurrentActiveUsageByCategory(categoryId: Int): Int = withContext(Dispatchers.IO) {
        return@withContext try {
            val currentTime = System.currentTimeMillis()
            val today = dateFormat.format(Date(currentTime))
            
            // 获取今日所有会话（包括正在进行的）
            val todaySessions = appSessionUserDao.getSessionsByDate(today)
            
            // 获取所有应用信息以检查分类
            val allApps = appInfoDao.getAllAppsList()
            val appCategoryMap = allApps.associate { it.packageName to it.categoryId }
            
            var totalActiveUsage = 0
            
            // 计算指定分类的活跃使用时间
            todaySessions.forEach { session ->
                val sessionCategoryId = appCategoryMap[session.pkgName]
                
                // 只统计指定分类的会话，或者总使用分类（categoryId为总使用分类的ID）
                if (sessionCategoryId == categoryId || isTotal(categoryId)) {
                    val sessionEndTime = session.endTime
                    val sessionStartTime = session.startTime
                    
                    // 检查会话是否可能正在进行中（结束时间接近开始时间或为当前时间附近）
                    val timeSinceEnd = currentTime - sessionEndTime
                    val sessionDuration = session.durationSec
                    
                    // 如果会话的结束时间在最近5分钟内，且会话时长较长，可能是正在进行的会话
                    if (timeSinceEnd <= 5 * 60 * 1000L && sessionDuration >= 60) {
                        // 计算从会话开始到现在的实际时间
                        val realDuration = ((currentTime - sessionStartTime) / 1000).toInt()
                        val additionalTime = realDuration - sessionDuration
                        
                        if (additionalTime > 0 && additionalTime <= 24 * 60 * 60) { // 额外时间不超过24小时
                            totalActiveUsage += additionalTime
                            android.util.Log.d("AppSessionRepository", 
                                "检测到活跃会话: ${session.pkgName}, 额外时间: ${additionalTime}s")
                        }
                    }
                }
            }
            
            android.util.Log.d("AppSessionRepository", 
                "分类${categoryId}当前活跃使用时间: ${totalActiveUsage}s")
            
            totalActiveUsage
            
        } catch (e: Exception) {
            android.util.Log.e("AppSessionRepository", "获取当前活跃使用时间失败", e)
            0
        }
    }
    
    /**
     * 检查是否为总使用分类
     */
    private suspend fun isTotal(categoryId: Int): Boolean {
        return try {
            // 使用AppCategoryDao获取分类信息
            val category = context.let { ctx ->
                val database = com.offtime.app.data.database.OffTimeDatabase.getDatabase(ctx)
                database.appCategoryDao().getCategoryById(categoryId)
            }
            category?.name == "总使用"
        } catch (e: Exception) {
            false
        }
    }
} 