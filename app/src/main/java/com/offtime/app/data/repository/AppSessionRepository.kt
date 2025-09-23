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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val sessionMutex = Mutex()

    fun getLastTimestamp(): Long {
        return prefs.getLong("lastTs", 0L)
    }

    fun updateLastTimestamp(timestamp: Long) {
        prefs.edit().putLong("lastTs", timestamp).apply()
    }

    suspend fun initializeDefaultTableIfEmpty() = withContext(Dispatchers.IO) {
        val defaults = appSessionDefaultDao.getAllDefaults()
        if (defaults.isEmpty()) {
            val categories = appCategoryDao.getAllCategories().first()
            val defaultEntities = categories.map { category ->
                AppSessionDefaultEntity(catId = category.id)
            }
            appSessionDefaultDao.insertDefaults(defaultEntities)
        }
    }

    suspend fun upsertSessionSmart(
        pkgName: String,
        startTime: Long,
        endTime: Long
    ) = sessionMutex.withLock {
        withContext(Dispatchers.IO) {
            if (shouldFilterSystemApp(pkgName)) {
                android.util.Log.d("AppSessionRepository", "过滤应用会话: $pkgName")
                return@withContext
            }

            val originalDuration = ((endTime - startTime) / 1000).toInt()
            if (originalDuration < 2) {
                android.util.Log.d("AppSessionRepository", "会话过短被过滤: $pkgName, 时长:${originalDuration}秒 < 2秒")
                return@withContext
            }

            // 去抖逻辑挪到服务层(结合UI前台态)。这里不再对OffTimes做固定阈值过滤。

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

            val startDate = dateFormat.format(Date(startTime))
            val endDate = dateFormat.format(Date(endTime))

            if (startDate != endDate) {
                splitCrossDaySession(appInfo, pkgName, startTime, endTime, originalDuration, startDate, endDate)
            } else {
                handleSameDaySession(appInfo, pkgName, startTime, endTime, startDate)
            }
        }
    }

    private suspend fun handleSameDaySession(
        appInfo: AppInfoEntity,
        pkgName: String,
        startTime: Long,
        endTime: Long,
        date: String
    ) {
        val mergeGapMillis = 10 * 1000L
        val recentSession = appSessionUserDao.getRecentSessionByPackage(
            pkgName,
            startTime - mergeGapMillis,
            startTime
        )

        if (recentSession != null) {
            val gap = startTime - recentSession.endTime
            android.util.Log.d("AppSessionRepository", "找到最近会话: $pkgName, 时间间隙: ${gap/1000}秒")
            
            val mergedEndTime = endTime
            val mergedDuration = ((mergedEndTime - recentSession.startTime) / 1000).toInt()
            appSessionUserDao.updateSessionEndTime(recentSession.id, mergedEndTime, mergedDuration)
            android.util.Log.d("AppSessionRepository", "智能合并会话成功: $pkgName, 合并后时长:${mergedDuration}秒")

        } else {
            val duplicateSession = appSessionUserDao.getActiveSessionByPackage(pkgName, date, startTime)
            if (duplicateSession != null) {
                // 重复会话（同一startTime）：应当延长该会话而不是跳过
                val newEndTime = if (endTime > duplicateSession.endTime) endTime else duplicateSession.endTime
                val newDuration = ((newEndTime - duplicateSession.startTime) / 1000).toInt()
                if (newEndTime > duplicateSession.endTime) {
                    appSessionUserDao.updateSessionEndTime(duplicateSession.id, newEndTime, newDuration)
                    android.util.Log.d("AppSessionRepository", "扩展重复会话时长: $pkgName, 新时长:${newDuration}秒")
                } else {
                    android.util.Log.w("AppSessionRepository", "检测到重复会话且新结束时间不更晚，保持不变: $pkgName")
                }
                return
            }
            // 新会话前再尝试一次“重叠/邻近”合并，防止边界重排导致插入
            val mergeTarget = appSessionUserDao.findOverlappingOrMergeableSession(
                pkgName, date, startTime, endTime
            )
            if (mergeTarget != null) {
                val newStart = minOf(mergeTarget.startTime, startTime)
                val newEnd = maxOf(mergeTarget.endTime, endTime)
                val newDuration = ((newEnd - newStart) / 1000).toInt()
                appSessionUserDao.updateSessionTimeRange(mergeTarget.id, newStart, newEnd, newDuration)
                android.util.Log.d("AppSessionRepository", "重叠/邻近合并成功: $pkgName, 新区间=[${newStart}, ${newEnd}] ${newDuration}s")
            } else {
                insertSingleSession(appInfo, pkgName, startTime, endTime, date)
            }
        }
    }

    private suspend fun insertSingleSession(
        appInfo: AppInfoEntity,
        pkgName: String,
        startTime: Long,
        endTime: Long,
        date: String
    ) {
        val duration = ((endTime - startTime) / 1000).toInt()
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
        android.util.Log.d("AppSessionRepository", "新会话已插入: $pkgName, 时长:${duration}秒, 日期:${date}")
    }

    private suspend fun splitCrossDaySession(
        appInfo: AppInfoEntity,
        pkgName: String,
        startTime: Long,
        endTime: Long,
        totalDuration: Int,
        startDate: String,
        endDate: String
    ) {
        android.util.Log.d("AppSessionRepository", "处理跨日期会话: $pkgName, 总时长:${totalDuration}秒")

        // 🔧 修复重复记录问题：先检查是否已存在相同的跨天分割记录
        val existingFirstDay = appSessionUserDao.getSessionsByDate(startDate)
            .filter { it.pkgName == pkgName && it.startTime == startTime }
        val existingSecondDay = appSessionUserDao.getSessionsByDate(endDate)
            .filter { it.pkgName == pkgName && it.endTime == endTime }

        if (existingFirstDay.isNotEmpty() || existingSecondDay.isNotEmpty()) {
            android.util.Log.w("AppSessionRepository", "检测到重复的跨天分割记录，跳过处理: $pkgName")
            return
        }

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = startTime
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val firstDayEndTime = calendar.timeInMillis

        calendar.timeInMillis = endTime
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val secondDayStartTime = calendar.timeInMillis

        val firstDayDuration = ((firstDayEndTime - startTime) / 1000).toInt()
        if (firstDayDuration >= 2) {
            // 🔧 增强重复检测：使用更精确的时间范围检查
            handleCrossDaySession(appInfo, pkgName, startTime, firstDayEndTime, startDate, "first_day")
        }

        val secondDayDuration = ((endTime - secondDayStartTime) / 1000).toInt()
        if (secondDayDuration >= 2) {
            // 🔧 增强重复检测：使用更精确的时间范围检查
            handleCrossDaySession(appInfo, pkgName, secondDayStartTime, endTime, endDate, "second_day")
        }
    }

    /**
     * 🔧 新增：专门处理跨天分割的单个部分，增强重复检测
     */
    private suspend fun handleCrossDaySession(
        appInfo: AppInfoEntity,
        pkgName: String,
        startTime: Long,
        endTime: Long,
        date: String,
        part: String
    ) {
        // 更严格的重复检测：检查时间范围重叠
        val existingSessions = appSessionUserDao.getSessionsByDate(date)
            .filter { session ->
                session.pkgName == pkgName &&
                // 检查时间范围重叠
                (session.startTime <= startTime && session.endTime >= endTime) ||
                (session.startTime >= startTime && session.endTime <= endTime) ||
                (session.startTime <= startTime && session.endTime > startTime) ||
                (session.startTime < endTime && session.endTime >= endTime)
            }

        if (existingSessions.isNotEmpty()) {
            android.util.Log.d("AppSessionRepository", "发现重叠的跨天分割记录($part)，尝试合并: $pkgName")
            // 找到最合适的会话进行合并
            val targetSession = existingSessions.maxByOrNull { it.durationSec }
            if (targetSession != null) {
                val newStart = minOf(targetSession.startTime, startTime)
                val newEnd = maxOf(targetSession.endTime, endTime)
                val newDuration = ((newEnd - newStart) / 1000).toInt()
                appSessionUserDao.updateSessionTimeRange(targetSession.id, newStart, newEnd, newDuration)
                android.util.Log.d("AppSessionRepository", "跨天分割合并成功($part): $pkgName, 新区间=[${newStart}, ${newEnd}] ${newDuration}s")
                return
            }
        }

        // 没有重叠，插入新会话
        insertSingleSession(appInfo, pkgName, startTime, endTime, date)
        android.util.Log.d("AppSessionRepository", "跨天分割插入新会话($part): $pkgName, 时长:${((endTime - startTime) / 1000).toInt()}秒")
    }

    private suspend fun shouldFilterSystemApp(packageName: String): Boolean {
        val systemBlacklist = setOf(
            "android", "com.android.systemui", "com.android.settings", "com.android.vending",
            "com.google.android.gms", "com.google.android.gsf", "com.android.phone",
            "com.miui.home", "com.huawei.android.launcher", "com.oppo.launcher", "com.vivo.launcher"
        )
        if (systemBlacklist.any { packageName.startsWith(it) }) {
            return true
        }
        val existingApp = appInfoDao.getAppByPackageName(packageName)
        return existingApp?.isExcluded == true
    }

    private suspend fun createAppInfoFromPackageName(packageName: String, categoryId: Int): AppInfoEntity? {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val packageInfo = pm.getPackageInfo(packageName, 0)
            AppInfoEntity(
                packageName = packageName,
                appName = appInfo.loadLabel(pm).toString(),
                versionName = packageInfo.versionName ?: "N/A",
                versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) packageInfo.longVersionCode else @Suppress("DEPRECATION") packageInfo.versionCode.toLong(),
                isSystemApp = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0,
                categoryId = categoryId,
                firstInstallTime = packageInfo.firstInstallTime,
                lastUpdateTime = packageInfo.lastUpdateTime,
                isEnabled = appInfo.enabled,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun getCategoryIdByPackage(pkgName: String): Int {
        return appInfoDao.getAppByPackageName(pkgName)?.categoryId 
            ?: appCategoryUtils.getCategoryIdByPackageName(pkgName)
    }

    suspend fun getTodaySessions(): List<AppSessionUserEntity> = withContext(Dispatchers.IO) {
        val today = dateFormat.format(Date())
        appSessionUserDao.getSessionsByDate(today)
    }

    suspend fun getTotalSessionCount(): Int = withContext(Dispatchers.IO) {
        appSessionUserDao.getTotalSessionCount()
    }

    suspend fun getSessionsByPackageName(packageName: String, date: String): List<AppSessionUserEntity> = withContext(Dispatchers.IO) {
        appSessionUserDao.getSessionsByPackageNameDebug(packageName).filter { it.date == date }
    }

    suspend fun deleteSessionById(sessionId: Int) = withContext(Dispatchers.IO) {
        appSessionUserDao.deleteSessionById(sessionId)
    }

    suspend fun updateSessionDuration(sessionId: Int, newDurationSec: Int) = withContext(Dispatchers.IO) {
        val session = appSessionUserDao.getAllSessions().find { it.id == sessionId }
        if (session != null) {
            val newEndTime = session.startTime + (newDurationSec * 1000L)
            appSessionUserDao.updateSessionEndTimeAndDuration(sessionId, newEndTime, newDurationSec)
        }
    }
    
    suspend fun cleanSystemAppSessions() = withContext(Dispatchers.IO) {
        val systemPackages = setOf("com.android.systemui", "com.miui.home", "com.android.launcher")
        systemPackages.forEach { pkg ->
            appSessionUserDao.deleteSessionsByPackageName(pkg)
        }
    }

    suspend fun updateActiveSessionDuration(pkgName: String?, currentStartTime: Long, currentTime: Long) {
        // This function is now simplified and directly handled within upsertSessionSmart's merging logic.
        // This stub is to satisfy legacy calls.
        if (pkgName != null) {
             upsertSessionSmart(pkgName, currentStartTime, currentTime)
        }
    }
    
    suspend fun getCurrentActiveUsageByCategory(@Suppress("UNUSED_PARAMETER") categoryId: Int): Int {
        // This is a deprecated calculation. Returning 0 to avoid incorrect values.
        return 0
    }

    suspend fun cleanOldSessions(daysToKeep: Int = 60) = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -daysToKeep)
        val cutoffDate = dateFormat.format(calendar.time)
        appSessionUserDao.deleteOldSessions(cutoffDate)
        android.util.Log.i("AppSessionRepository", "清理了${cutoffDate}之前的app_sessions_users数据")
    }
    
    suspend fun cleanDuplicateSessions(): Int = withContext(Dispatchers.IO) {
        var deletedCount = 0
        val offTimesPackageName = context.packageName
        val offTimesSessions = appSessionUserDao.getSessionsByPackageNameDebug(offTimesPackageName)
        if (offTimesSessions.isNotEmpty()) {
            val idsToDelete = offTimesSessions.map { it.id }
            appSessionUserDao.deleteSessionsByIds(idsToDelete)
            deletedCount += idsToDelete.size
        }

        val allSessions = appSessionUserDao.getAllSessions()
        val sessionsToKeep = mutableMapOf<Pair<String, Long>, AppSessionUserEntity>()
        val idsToDelete = mutableListOf<Int>()

        for (session in allSessions.sortedBy { it.startTime }) {
            val key = Pair(session.pkgName, session.startTime)
            if (sessionsToKeep.containsKey(key)) {
                val existingSession = sessionsToKeep[key]!!
                if (session.durationSec > existingSession.durationSec) {
                    idsToDelete.add(existingSession.id)
                    sessionsToKeep[key] = session
                } else {
                    idsToDelete.add(session.id)
                }
            } else {
                sessionsToKeep[key] = session
            }
        }

        if (idsToDelete.isNotEmpty()) {
            appSessionUserDao.deleteSessionsByIds(idsToDelete)
            deletedCount += idsToDelete.size
        }
        return@withContext deletedCount
    }
} 