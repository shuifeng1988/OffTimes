package com.offtime.app.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.offtime.app.data.dao.*
import com.offtime.app.data.entity.*

/**
 * OffTimes 应用的核心数据库
 * 
 * 数据库架构说明：
 * 1. 基础数据表：存储应用信息和分类信息
 * 2. 使用数据表：记录应用使用统计和会话信息
 * 3. 目标奖罚表：管理用户设定的目标和奖罚机制
 * 4. 汇总统计表：存储按不同时间周期聚合的数据
 * 
 * 数据流转顺序：
 * 系统 UsageStats → app_sessions_user → daily_usage_user → summary_usage_user
 * 
 * 版本历史：
 * - v1-v5: 基础表结构建立
 * - v6-v10: 添加使用统计功能
 * - v11-v15: 完善奖罚机制和数据聚合
 */
@Database(
    entities = [
        // 基础数据实体
        AppInfoEntity::class,                    // 用户设备上的应用信息
        AppCategoryEntity::class,                // 用户自定义的应用分类
        AppInfoDefaultEntity::class,             // 系统预设的应用信息模板
        AppCategoryDefaultEntity::class,         // 系统预设的分类模板
        
        // 目标和奖罚实体
        GoalRewardPunishmentDefaultEntity::class, // 默认的目标奖罚规则
        GoalRewardPunishmentUserEntity::class,    // 用户自定义的目标奖罚规则
        
        // 定时器功能实体
        TimerSessionDefaultEntity::class,         // 默认定时器会话模板
        TimerSessionUserEntity::class,            // 用户的定时器使用记录
        
        // 核心使用统计实体
        AppSessionDefaultEntity::class,           // 默认会话模板（预留）
        AppSessionUserEntity::class,              // 【核心】用户应用使用会话记录
        DailyUsageUserEntity::class,              // 【重要】按日聚合的使用统计
        
        // 多时间维度汇总统计实体
        SummaryUsageUserEntity::class,            // 通用汇总统计（支持日/周/月）
        SummaryUsageWeekUserEntity::class,        // 按周汇总的使用统计
        SummaryUsageMonthUserEntity::class,       // 按月汇总的使用统计
        
        // 奖罚完成度记录实体
        RewardPunishmentUserEntity::class,        // 按日的奖罚完成记录
        RewardPunishmentWeekUserEntity::class,    // 按周的奖罚完成记录
        RewardPunishmentMonthUserEntity::class,   // 按月的奖罚完成记录
        
        // 应用设置实体
        AppSettingsEntity::class,                 // 应用配置和用户偏好设置
        
        // 备份设置实体
        BackupSettingsEntity::class,              // 用户数据备份配置
        
        // 用户账户实体
        UserEntity::class                          // 用户账户信息
    ],
    version = 30,
    exportSchema = false
)
abstract class OffTimeDatabase : RoomDatabase() {
    
    // === 基础数据访问接口 ===
    abstract fun appInfoDao(): AppInfoDao                              // 管理用户设备应用信息
    abstract fun appCategoryDao(): AppCategoryDao                      // 管理用户应用分类
    abstract fun appInfoDefaultDao(): AppInfoDefaultDao                // 管理默认应用信息模板
    abstract fun appCategoryDefaultDao(): AppCategoryDefaultDao        // 管理默认分类模板
    
    // === 目标奖罚数据访问接口 ===
    abstract fun goalRewardPunishmentDefaultDao(): GoalRewardPunishmentDefaultDao  // 默认目标奖罚规则
    abstract fun goalRewardPunishmentUserDao(): GoalRewardPunishmentUserDao        // 用户目标奖罚规则
    
    // === 定时器功能数据访问接口 ===
    abstract fun timerSessionDefaultDao(): TimerSessionDefaultDao      // 默认定时器模板
    abstract fun timerSessionUserDao(): TimerSessionUserDao            // 用户定时器记录
    
    // === 核心使用统计数据访问接口 ===
    abstract fun appSessionDefaultDao(): AppSessionDefaultDao          // 默认会话模板
    abstract fun appSessionUserDao(): AppSessionUserDao                // 【核心】用户使用会话数据
    abstract fun dailyUsageDao(): DailyUsageDao                        // 【重要】日使用统计数据
    abstract fun summaryUsageDao(): SummaryUsageDao                    // 汇总统计数据（多时间维度）
    
    // === 奖罚完成度数据访问接口 ===
    abstract fun rewardPunishmentUserDao(): RewardPunishmentUserDao            // 日奖罚完成度
    abstract fun rewardPunishmentWeekUserDao(): RewardPunishmentWeekUserDao    // 周奖罚完成度
    abstract fun rewardPunishmentMonthUserDao(): RewardPunishmentMonthUserDao  // 月奖罚完成度
    
    // === 设置数据访问接口 ===
    abstract fun appSettingsDao(): AppSettingsDao                      // 应用设置和配置
    abstract fun backupSettingsDao(): BackupSettingsDao               // 备份设置和配置
    
    // === 用户账户数据访问接口 ===
    abstract fun userDao(): UserDao                                     // 用户账户管理
    
    companion object {
        const val DATABASE_NAME = "offtime_database"
        
        @Volatile
        private var INSTANCE: OffTimeDatabase? = null
        
        /**
         * 获取数据库实例（单例模式）
         * 
         * 数据库初始化流程：
         * 1. 创建数据库实例
         * 2. 添加数据库回调（用于初始化默认数据）
         * 3. 添加所有版本迁移脚本
         * 4. 禁用破坏性迁移以保护用户数据
         */
        fun getDatabase(context: Context): OffTimeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OffTimeDatabase::class.java,
                    DATABASE_NAME
                )
                    .addCallback(DatabaseCallback())  // 数据库创建时的回调处理
                    .addMigrations(
                        // 按版本顺序添加所有迁移脚本
                        MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, 
                                    MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_8, MIGRATION_9_10,
            MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25, MIGRATION_25_26, MIGRATION_26_27, MIGRATION_27_28, MIGRATION_28_29, MIGRATION_29_30
                    )
                    // 生产环境：禁用破坏性迁移以保护用户数据
                    // 如果遇到无法迁移的情况，应用会崩溃，提醒开发者添加相应的迁移脚本
                    .build()
                INSTANCE = instance
                instance
            }
        }
        
        /**
         * 数据库版本迁移：从版本1到版本2
         * 
         * 迁移内容：
         * - 创建应用信息默认表 (app_info_defaults)
         * - 创建应用分类默认表 (AppCategory_Defaults)
         * - 插入系统预设的分类数据
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 创建应用信息默认表：存储系统预设的应用信息模板
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS app_info_defaults (
                        packageName TEXT PRIMARY KEY NOT NULL,    -- 应用包名（唯一标识）
                        appName TEXT NOT NULL,                    -- 应用显示名称
                        versionName TEXT NOT NULL,                -- 版本名称
                        versionCode INTEGER NOT NULL,             -- 版本代码
                        isSystemApp INTEGER NOT NULL,             -- 是否为系统应用
                        categoryId INTEGER NOT NULL DEFAULT 1,    -- 所属分类ID
                        firstInstallTime INTEGER NOT NULL,        -- 首次安装时间
                        lastUpdateTime INTEGER NOT NULL,          -- 最后更新时间
                        isEnabled INTEGER NOT NULL DEFAULT 1,     -- 是否启用统计
                        createdAt INTEGER NOT NULL,               -- 记录创建时间
                        updatedAt INTEGER NOT NULL                -- 记录更新时间
                    )
                """)
                
                // 创建应用分类默认表：存储系统预设的分类模板
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS AppCategory_Defaults (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,  -- 分类ID（自增）
                        name TEXT NOT NULL,                             -- 分类名称
                        displayOrder INTEGER NOT NULL,                  -- 显示顺序
                        isDefault INTEGER NOT NULL DEFAULT 0,           -- 是否为默认分类
                        isLocked INTEGER NOT NULL DEFAULT 0,            -- 是否锁定（不可删除）
                        targetType TEXT NOT NULL,                       -- 目标类型（LESS_THAN/MORE_THAN）
                        createdAt INTEGER NOT NULL,                     -- 创建时间
                        updatedAt INTEGER NOT NULL                      -- 更新时间
                    )
                """)
                
                // 插入系统预设的分类数据
                insertDefaultCategoriesToDefaultTable(db)
            }
        }
        
        /**
         * 数据库版本迁移：从版本2到版本3
         * 
         * 迁移内容：
         * - 添加目标奖罚功能
         * - 创建目标奖罚默认表和用户表
         * - 插入默认的目标奖罚数据
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 创建目标奖罚默认表：存储系统预设的目标奖罚规则
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS goals_reward_punishment_defaults (
                        catId INTEGER PRIMARY KEY NOT NULL,       -- 分类ID（关联分类表）
                        dailyGoalMin INTEGER NOT NULL,            -- 日目标时长（分钟）
                        conditionType INTEGER NOT NULL,           -- 条件类型（0=少于目标获奖励，1=超过目标受惩罚）
                        rewardText TEXT NOT NULL,                 -- 奖励内容描述
                        punishText TEXT NOT NULL,                 -- 惩罚内容描述
                        updateTime INTEGER NOT NULL               -- 更新时间
                    )
                """)
                
                // 创建目标奖罚用户表：存储用户自定义的目标奖罚规则
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS goals_reward_punishment_users (
                        catId INTEGER PRIMARY KEY NOT NULL,       -- 分类ID（关联分类表）
                        dailyGoalMin INTEGER NOT NULL,            -- 日目标时长（分钟）
                        conditionType INTEGER NOT NULL,           -- 条件类型
                        rewardText TEXT NOT NULL,                 -- 奖励内容描述
                        punishText TEXT NOT NULL,                 -- 惩罚内容描述
                        updateTime INTEGER NOT NULL               -- 更新时间
                    )
                """)
                
                // 插入默认的目标奖罚数据
                insertDefaultGoalData(db)
            }
        }
        
        /**
         * 数据库版本迁移：从版本3到版本4
         * 
         * 迁移内容：
         * - 添加"总使用"分类
         * - 为总使用分类创建默认目标
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val currentTime = System.currentTimeMillis()
                
                // 在默认表中添加总使用分类
                db.execSQL("""
                    INSERT OR IGNORE INTO AppCategory_Defaults (name, displayOrder, isDefault, isLocked, targetType, createdAt, updatedAt) 
                    VALUES ('总使用', 4, 1, 1, 'LESS_THAN', $currentTime, $currentTime)
                """)
                
                // 在用户表中添加总使用分类（如果不存在）
                db.execSQL("""
                    INSERT OR IGNORE INTO AppCategory_Users (name, displayOrder, isDefault, isLocked, targetType, createdAt, updatedAt) 
                    VALUES ('总使用', 4, 1, 1, 'LESS_THAN', $currentTime, $currentTime)
                """)
                
                // 为总使用分类创建默认目标（找到刚插入的分类ID）
                db.execSQL("""
                    INSERT OR IGNORE INTO goals_reward_punishment_users (catId, dailyGoalMin, conditionType, rewardText, punishText, updateTime)
                    SELECT id, 240, 0, '薯片', '俯卧撑30个', $currentTime
                    FROM AppCategory_Users 
                    WHERE name = '总使用' AND id NOT IN (SELECT catId FROM goals_reward_punishment_users)
                """)
            }
        }
        
        /**
         * 数据库版本迁移：从版本4到版本5
         * 
         * 迁移内容：
         * - 清理重复的总使用分类
         * - 确保总使用分类的唯一性
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 清理重复的总使用分类数据
                // 1. 先删除所有总使用分类的目标数据
                db.execSQL("""
                    DELETE FROM goals_reward_punishment_users 
                    WHERE catId IN (SELECT id FROM AppCategory_Users WHERE name = '总使用')
                """)
                
                // 2. 删除用户表中的所有总使用分类
                db.execSQL("""
                    DELETE FROM AppCategory_Users WHERE name = '总使用'
                """)
                
                // 3. 重新插入唯一的总使用分类
                val currentTime = System.currentTimeMillis()
                db.execSQL("""
                    INSERT INTO AppCategory_Users (name, displayOrder, isDefault, isLocked, targetType, createdAt, updatedAt) 
                    VALUES ('总使用', 4, 1, 1, 'LESS_THAN', $currentTime, $currentTime)
                """)
                
                // 4. 为总使用分类创建目标数据
                db.execSQL("""
                    INSERT INTO goals_reward_punishment_users (catId, dailyGoalMin, conditionType, rewardText, punishText, updateTime)
                    SELECT id, 240, 0, '薯片', '俯卧撑30个', $currentTime
                    FROM AppCategory_Users 
                    WHERE name = '总使用'
                """)
            }
        }

        // 数据库版本迁移：从版本5到版本6（添加线下活动计时表）
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 创建线下活动计时默认表
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS timer_sessions_defaults (
                        catId INTEGER NOT NULL PRIMARY KEY,
                        programName TEXT NOT NULL DEFAULT '线下活动',
                        date TEXT NOT NULL DEFAULT '',
                        startTime INTEGER NOT NULL DEFAULT 0,
                        endTime INTEGER NOT NULL DEFAULT 0,
                        durationSec INTEGER NOT NULL DEFAULT 0,
                        isOffline INTEGER NOT NULL DEFAULT 1,
                        updateTime INTEGER NOT NULL DEFAULT 0
                    )
                """)
                
                // 创建线下活动计时用户表
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS timer_sessions_users (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        catId INTEGER NOT NULL,
                        programName TEXT NOT NULL DEFAULT '线下活动',
                        date TEXT NOT NULL,
                        startTime INTEGER NOT NULL,
                        endTime INTEGER NOT NULL,
                        durationSec INTEGER NOT NULL DEFAULT 0,
                        isOffline INTEGER NOT NULL DEFAULT 1,
                        updateTime INTEGER NOT NULL DEFAULT 0
                    )
                """)
                
                // 为所有现有分类创建默认计时会话模板
                val categories = mutableListOf<Int>()
                val categoryCursor = db.query("SELECT id FROM AppCategory_Users ORDER BY displayOrder")
                while (categoryCursor.moveToNext()) {
                    categories.add(categoryCursor.getInt(0))
                }
                categoryCursor.close()
                
                // 如果用户表为空，也从默认表获取分类
                if (categories.isEmpty()) {
                    val defaultCategoryCursor = db.query("SELECT id FROM AppCategory_Defaults ORDER BY displayOrder")
                    while (defaultCategoryCursor.moveToNext()) {
                        categories.add(defaultCategoryCursor.getInt(0))
                    }
                    defaultCategoryCursor.close()
                }
                
                // 为每个分类插入默认模板
                for (catId in categories) {
                    db.execSQL("""
                        INSERT OR IGNORE INTO timer_sessions_defaults 
                        (catId, programName, date, startTime, endTime, durationSec, isOffline, updateTime) 
                        VALUES ($catId, '线下活动', '', 0, 0, 0, 1, ${System.currentTimeMillis()})
                    """)
                }
            }
        }

        // 数据库版本迁移：从版本6到版本7（添加真实App会话采集表）
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 创建真实App会话默认表
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS app_sessions_defaults (
                        catId INTEGER NOT NULL PRIMARY KEY,
                        pkgName TEXT NOT NULL DEFAULT '',
                        date TEXT NOT NULL DEFAULT '',
                        startTime INTEGER NOT NULL DEFAULT 0,
                        endTime INTEGER NOT NULL DEFAULT 0,
                        durationSec INTEGER NOT NULL DEFAULT 0,
                        isOffline INTEGER NOT NULL DEFAULT 0,
                        updateTime INTEGER NOT NULL DEFAULT 0
                    )
                """)
                
                // 创建真实App会话用户表
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS app_sessions_users (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        catId INTEGER NOT NULL,
                        pkgName TEXT NOT NULL,
                        date TEXT NOT NULL,
                        startTime INTEGER NOT NULL,
                        endTime INTEGER NOT NULL,
                        durationSec INTEGER NOT NULL DEFAULT 0,
                        isOffline INTEGER NOT NULL DEFAULT 0,
                        updateTime INTEGER NOT NULL DEFAULT 0
                    )
                """)
                
                // 为所有现有分类创建默认App会话模板
                val categories = mutableListOf<Int>()
                val categoryCursor = db.query("SELECT id FROM AppCategory_Users ORDER BY displayOrder")
                while (categoryCursor.moveToNext()) {
                    categories.add(categoryCursor.getInt(0))
                }
                categoryCursor.close()
                
                // 如果用户表为空，也从默认表获取分类
                if (categories.isEmpty()) {
                    val defaultCategoryCursor = db.query("SELECT id FROM AppCategory_Defaults ORDER BY displayOrder")
                    while (defaultCategoryCursor.moveToNext()) {
                        categories.add(defaultCategoryCursor.getInt(0))
                    }
                    defaultCategoryCursor.close()
                }
                
                // 为每个分类插入默认App会话模板
                for (catId in categories) {
                    db.execSQL("""
                        INSERT OR IGNORE INTO app_sessions_defaults 
                        (catId, pkgName, date, startTime, endTime, durationSec, isOffline, updateTime) 
                        VALUES ($catId, '', '', 0, 0, 0, 0, ${System.currentTimeMillis()})
                    """)
                }
            }
        }
        
        // 版本7 → 8：新增汇总表
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // daily_usage_user
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS daily_usage_user (
                        id TEXT NOT NULL PRIMARY KEY,
                        date TEXT NOT NULL,
                        catId INTEGER NOT NULL,
                        slotIndex INTEGER NOT NULL,
                        isOffline INTEGER NOT NULL,
                        durationSec INTEGER NOT NULL,
                        updateTime INTEGER NOT NULL
                    )
                """)

                // summary_usage_user
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS summary_usage_user (
                        id TEXT NOT NULL PRIMARY KEY,
                        date TEXT NOT NULL,
                        catId INTEGER NOT NULL,
                        totalSec INTEGER NOT NULL,
                        updateTime INTEGER NOT NULL
                    )
                """)

                // summary_usage_week_user
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS summary_usage_week_user (
                        id TEXT NOT NULL PRIMARY KEY,
                        weekStartDate TEXT NOT NULL,
                        catId INTEGER NOT NULL,
                        avgDailySec INTEGER NOT NULL,
                        updateTime INTEGER NOT NULL
                    )
                """)

                // summary_usage_month_user
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS summary_usage_month_user (
                        id TEXT NOT NULL PRIMARY KEY,
                        month TEXT NOT NULL,
                        catId INTEGER NOT NULL,
                        avgDailySec INTEGER NOT NULL,
                        updateTime INTEGER NOT NULL
                    )
                """)

                // reward_punishment_user
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS reward_punishment_user (
                        id TEXT NOT NULL PRIMARY KEY,
                        date TEXT NOT NULL,
                        catId INTEGER NOT NULL,
                        isGoalMet INTEGER NOT NULL,
                        rewardDone INTEGER NOT NULL,
                        punishDone INTEGER NOT NULL,
                        updateTime INTEGER NOT NULL
                    )
                """)
            }
        }
        
        // 数据库版本迁移：从版本8到版本9（原本添加奖罚完成度表，现已移除）
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 原本创建 goals_reward_punishment_completion 表的代码已移除
                // 该表已被 reward_punishment_user 表替代
                android.util.Log.d("DatabaseMigration", "迁移8→9：跳过已废弃的goals_reward_punishment_completion表创建")
            }
        }
        
        // 版本9 → 8：降级迁移（处理版本回退情况）
        private val MIGRATION_9_8 = object : Migration(9, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                android.util.Log.d("DatabaseMigration", "开始执行数据库降级: 版本9 → 版本8")
                
                // 强制转换app_info_Users表从版本9格式到版本8格式
                try {
                    android.util.Log.d("DatabaseMigration", "检查app_info_Users表schema")
                    
                    // 直接检查是否存在snake_case列
                    var hasOldSchema = false
                    try {
                        db.query("SELECT package_name FROM app_info_Users LIMIT 1").close()
                        hasOldSchema = true
                        android.util.Log.d("DatabaseMigration", "检测到版本9的app_info_Users表 (snake_case列)")
                    } catch (e: Exception) {
                        android.util.Log.d("DatabaseMigration", "未检测到版本9的schema，可能已经是版本8格式")
                    }
                    
                    if (hasOldSchema) {
                        android.util.Log.d("DatabaseMigration", "开始转换app_info_Users表schema")
                        
                        // 1. 重命名现有表
                        db.execSQL("ALTER TABLE app_info_Users RENAME TO app_info_Users_v9")
                        android.util.Log.d("DatabaseMigration", "已重命名旧表为app_info_Users_v9")
                        
                        // 2. 创建版本8格式的新表
                        db.execSQL("""
                            CREATE TABLE app_info_Users (
                                packageName TEXT PRIMARY KEY NOT NULL,
                                appName TEXT NOT NULL,
                                versionName TEXT NOT NULL,
                                versionCode INTEGER NOT NULL,
                                isSystemApp INTEGER NOT NULL,
                                categoryId INTEGER NOT NULL DEFAULT 1,
                                firstInstallTime INTEGER NOT NULL,
                                lastUpdateTime INTEGER NOT NULL,
                                isEnabled INTEGER NOT NULL DEFAULT 1,
                                createdAt INTEGER NOT NULL,
                                updatedAt INTEGER NOT NULL
                            )
                        """)
                        android.util.Log.d("DatabaseMigration", "已创建版本8格式的app_info_Users表")
                        
                        // 3. 转换并插入数据
                        val currentTime = System.currentTimeMillis()
                        db.execSQL("""
                            INSERT INTO app_info_Users (
                                packageName, appName, versionName, versionCode, isSystemApp, 
                                categoryId, firstInstallTime, lastUpdateTime, isEnabled, 
                                createdAt, updatedAt
                            )
                            SELECT 
                                package_name,
                                COALESCE(label, package_name),
                                COALESCE(version_name, 'Unknown'),
                                COALESCE(version_code, 0),
                                COALESCE(is_system, 0),
                                COALESCE(category_id, 1),
                                COALESCE(first_seen, $currentTime),
                                COALESCE(last_updated, $currentTime),
                                COALESCE(is_enabled, 1),
                                $currentTime,
                                $currentTime
                            FROM app_info_Users_v9
                        """)
                        android.util.Log.d("DatabaseMigration", "已转换并插入数据到新表")
                        
                        // 4. 删除旧表
                        db.execSQL("DROP TABLE app_info_Users_v9")
                        android.util.Log.d("DatabaseMigration", "已删除旧版本表")
                        
                        // 5. 验证转换结果
                        val countCursor = db.query("SELECT COUNT(*) FROM app_info_Users")
                        if (countCursor.moveToFirst()) {
                            val count = countCursor.getInt(0)
                            android.util.Log.d("DatabaseMigration", "表转换完成，共转换 $count 条记录")
                        }
                        countCursor.close()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("DatabaseMigration", "转换app_info_Users表时出错: ${e.message}", e)
                    // 如果转换失败，清空表并重新创建
                    try {
                        db.execSQL("DROP TABLE IF EXISTS app_info_Users")
                        db.execSQL("DROP TABLE IF EXISTS app_info_Users_v9")
                        db.execSQL("""
                            CREATE TABLE app_info_Users (
                                packageName TEXT PRIMARY KEY NOT NULL,
                                appName TEXT NOT NULL,
                                versionName TEXT NOT NULL,
                                versionCode INTEGER NOT NULL,
                                isSystemApp INTEGER NOT NULL,
                                categoryId INTEGER NOT NULL DEFAULT 1,
                                firstInstallTime INTEGER NOT NULL,
                                lastUpdateTime INTEGER NOT NULL,
                                isEnabled INTEGER NOT NULL DEFAULT 1,
                                createdAt INTEGER NOT NULL,
                                updatedAt INTEGER NOT NULL
                            )
                        """)
                        android.util.Log.d("DatabaseMigration", "转换失败，已重新创建空的app_info_Users表")
                    } catch (e2: Exception) {
                        android.util.Log.e("DatabaseMigration", "重新创建表也失败: ${e2.message}", e2)
                    }
                }
                
                // 确保所有版本8需要的汇总表都存在
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS daily_usage_user (
                        id TEXT NOT NULL PRIMARY KEY,
                        date TEXT NOT NULL,
                        catId INTEGER NOT NULL,
                        slotIndex INTEGER NOT NULL,
                        isOffline INTEGER NOT NULL,
                        durationSec INTEGER NOT NULL,
                        updateTime INTEGER NOT NULL
                    )
                """)

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS summary_usage_user (
                        id TEXT NOT NULL PRIMARY KEY,
                        date TEXT NOT NULL,
                        catId INTEGER NOT NULL,
                        totalSec INTEGER NOT NULL,
                        updateTime INTEGER NOT NULL
                    )
                """)

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS summary_usage_week_user (
                        id TEXT NOT NULL PRIMARY KEY,
                        weekStartDate TEXT NOT NULL,
                        catId INTEGER NOT NULL,
                        avgDailySec INTEGER NOT NULL,
                        updateTime INTEGER NOT NULL
                    )
                """)

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS summary_usage_month_user (
                        id TEXT NOT NULL PRIMARY KEY,
                        month TEXT NOT NULL,
                        catId INTEGER NOT NULL,
                        avgDailySec INTEGER NOT NULL,
                        updateTime INTEGER NOT NULL
                    )
                """)

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS reward_punishment_user (
                        id TEXT NOT NULL PRIMARY KEY,
                        date TEXT NOT NULL,
                        catId INTEGER NOT NULL,
                        isGoalMet INTEGER NOT NULL,
                        rewardDone INTEGER NOT NULL,
                        punishDone INTEGER NOT NULL,
                        updateTime INTEGER NOT NULL
                    )
                """)
                
                android.util.Log.d("DatabaseMigration", "数据库降级完成: 版本9 → 版本8")
            }
        }
        
        // 数据库版本迁移：从版本9到版本10（添加周和月奖惩记录表）
        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 创建周奖惩记录表
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS reward_punishment_week_user (
                        id TEXT NOT NULL PRIMARY KEY,
                        weekStart TEXT NOT NULL,
                        weekEnd TEXT NOT NULL,
                        catId INTEGER NOT NULL,
                        isGoalMet INTEGER NOT NULL,
                        rewardDone INTEGER NOT NULL,
                        punishDone INTEGER NOT NULL,
                        updateTime INTEGER NOT NULL
                    )
                """)
                
                // 创建月奖惩记录表
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS reward_punishment_month_user (
                        id TEXT NOT NULL PRIMARY KEY,
                        yearMonth TEXT NOT NULL,
                        catId INTEGER NOT NULL,
                        isGoalMet INTEGER NOT NULL,
                        rewardDone INTEGER NOT NULL,
                        punishDone INTEGER NOT NULL,
                        updateTime INTEGER NOT NULL
                    )
                """)
                
                android.util.Log.d("DatabaseMigration", "数据库升级完成: 版本9 → 版本10，已添加周和月奖惩记录表")
            }
        }
        
        // 数据库版本迁移：从版本10到版本11（修改周表和月表结构以支持奖惩次数统计）
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 重建周表，改为统计奖惩次数
                db.execSQL("DROP TABLE IF EXISTS reward_punishment_week_user")
                db.execSQL("""
                    CREATE TABLE reward_punishment_week_user (
                        id TEXT PRIMARY KEY NOT NULL,
                        weekStart TEXT NOT NULL,
                        weekEnd TEXT NOT NULL,
                        catId INTEGER NOT NULL,
                        totalRewardCount INTEGER NOT NULL,
                        totalPunishCount INTEGER NOT NULL,
                        doneRewardCount INTEGER NOT NULL,
                        donePunishCount INTEGER NOT NULL,
                        updateTime INTEGER NOT NULL
                    )
                """)
                
                // 重建月表，改为统计奖惩次数
                db.execSQL("DROP TABLE IF EXISTS reward_punishment_month_user")
                db.execSQL("""
                    CREATE TABLE reward_punishment_month_user (
                        id TEXT PRIMARY KEY NOT NULL,
                        yearMonth TEXT NOT NULL,
                        catId INTEGER NOT NULL,
                        totalRewardCount INTEGER NOT NULL,
                        totalPunishCount INTEGER NOT NULL,
                        doneRewardCount INTEGER NOT NULL,
                        donePunishCount INTEGER NOT NULL,
                        updateTime INTEGER NOT NULL
                    )
                """)
                
                android.util.Log.d("DatabaseMigration", "数据库升级完成: 版本10 → 版本11，已更新周和月表结构为奖惩次数统计")
            }
        }
        
        // 数据库版本迁移：从版本11到版本12（安全的空迁移）
        // 不向用户表添加任何数据，保护用户数据
        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 安全的空迁移 - 不修改用户表数据
                android.util.Log.d("DatabaseMigration", "安全迁移完成：版本11 → 版本12，未修改用户表")
            }
        }
        
        // 数据库版本迁移：从版本12到版本13（删除废弃的goals_reward_punishment_completion表）
        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 删除废弃的goals_reward_punishment_completion表
                // 该表已被reward_punishment_user表替代
                db.execSQL("DROP TABLE IF EXISTS goals_reward_punishment_completion")
                
                android.util.Log.d("DatabaseMigration", "迁移完成：版本12 → 版本13，已删除废弃的goals_reward_punishment_completion表")
            }
        }
        
        // 数据库版本迁移：从版本13到版本14（添加应用排除字段，删除排除统计分类）
        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. 为应用用户表添加isExcluded字段（默认表不需要此字段）
                db.execSQL("ALTER TABLE app_info_Users ADD COLUMN isExcluded INTEGER NOT NULL DEFAULT 0")
                
                // 2. 将"排除统计"分类中的应用标记为排除状态
                db.execSQL("""
                    UPDATE app_info_Users 
                    SET isExcluded = 1 
                    WHERE categoryId IN (
                        SELECT id FROM AppCategory_Users WHERE name = '排除统计'
                    )
                """)
                
                // 3. 将这些应用重新分类到娱乐分类
                db.execSQL("""
                    UPDATE app_info_Users 
                    SET categoryId = (
                        SELECT id FROM AppCategory_Users WHERE name = '娱乐' LIMIT 1
                    )
                    WHERE categoryId IN (
                        SELECT id FROM AppCategory_Users WHERE name = '排除统计'
                    )
                """)
                
                // 4. 删除"排除统计"分类的目标奖罚数据
                db.execSQL("""
                    DELETE FROM goals_reward_punishment_users 
                    WHERE catId IN (
                        SELECT id FROM AppCategory_Users WHERE name = '排除统计'
                    )
                """)
                
                // 5. 删除"排除统计"分类
                db.execSQL("DELETE FROM AppCategory_Users WHERE name = '排除统计'")
                db.execSQL("DELETE FROM AppCategory_Defaults WHERE name = '排除统计'")
                
                android.util.Log.d("DatabaseMigration", "迁移完成：版本13 → 版本14，已添加应用排除字段并删除排除统计分类")
            }
        }
        
        // 数据库版本迁移：从版本14到版本15（添加应用设置表）
        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 创建应用设置表
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS app_settings (
                        id INTEGER PRIMARY KEY NOT NULL,
                        defaultCategoryId INTEGER NOT NULL DEFAULT 1,
                        categoryRewardPunishmentEnabled TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """)
                
                // 插入默认设置记录
                val currentTime = System.currentTimeMillis()
                db.execSQL("""
                    INSERT OR REPLACE INTO app_settings (id, defaultCategoryId, categoryRewardPunishmentEnabled, createdAt, updatedAt)
                    VALUES (1, 1, '', $currentTime, $currentTime)
                """)
                
                android.util.Log.d("DatabaseMigration", "迁移完成：版本14 → 版本15，已添加应用设置表")
            }
        }
        
        // 数据库版本迁移：从版本15到版本16（添加用户账户表）
        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 创建用户账户表
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_accounts (
                        user_id TEXT PRIMARY KEY NOT NULL,
                        phone_number TEXT NOT NULL,
                        password_hash TEXT NOT NULL,
                        nickname TEXT NOT NULL DEFAULT '',
                        avatar TEXT NOT NULL DEFAULT '',
                        is_logged_in INTEGER NOT NULL DEFAULT 0,
                        last_login_time INTEGER NOT NULL DEFAULT 0,
                        register_time INTEGER NOT NULL,
                        server_user_id TEXT NOT NULL DEFAULT '',
                        is_data_sync_enabled INTEGER NOT NULL DEFAULT 0
                    )
                """)
                
                // 为手机号创建唯一索引
                db.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS index_user_accounts_phone_number 
                    ON user_accounts(phone_number)
                """)
                
                android.util.Log.d("DatabaseMigration", "迁移完成：版本15 → 版本16，已添加用户账户表")
            }
        }
        
        // 数据库版本迁移：从版本16到版本17（添加时间单位和结构化奖罚字段）
        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 为目标奖罚用户表添加新字段
                db.execSQL("ALTER TABLE goals_reward_punishment_users ADD COLUMN goalTimeUnit TEXT NOT NULL DEFAULT '分钟'")
                db.execSQL("ALTER TABLE goals_reward_punishment_users ADD COLUMN rewardNumber INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE goals_reward_punishment_users ADD COLUMN rewardUnit TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE goals_reward_punishment_users ADD COLUMN punishNumber INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE goals_reward_punishment_users ADD COLUMN punishUnit TEXT NOT NULL DEFAULT ''")
                
                // 为目标奖罚默认表添加新字段
                db.execSQL("ALTER TABLE goals_reward_punishment_defaults ADD COLUMN goalTimeUnit TEXT NOT NULL DEFAULT '分钟'")
                db.execSQL("ALTER TABLE goals_reward_punishment_defaults ADD COLUMN rewardNumber INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE goals_reward_punishment_defaults ADD COLUMN rewardUnit TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE goals_reward_punishment_defaults ADD COLUMN punishNumber INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE goals_reward_punishment_defaults ADD COLUMN punishUnit TEXT NOT NULL DEFAULT ''")
                
                // 解析现有数据并填充新字段
                migrateRewardPunishmentData(db)
                
                android.util.Log.d("DatabaseMigration", "迁移完成：版本16 → 版本17，已添加时间单位和结构化奖罚字段")
            }
        }
        
        // 数据库版本迁移：从版本17到版本18（添加惩罚时间单位字段）
        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 为目标奖罚用户表添加惩罚时间单位字段
                db.execSQL("ALTER TABLE goals_reward_punishment_users ADD COLUMN punishTimeUnit TEXT NOT NULL DEFAULT '小时'")
                
                // 为目标奖罚默认表添加惩罚时间单位字段
                db.execSQL("ALTER TABLE goals_reward_punishment_defaults ADD COLUMN punishTimeUnit TEXT NOT NULL DEFAULT '小时'")
                
                android.util.Log.d("DatabaseMigration", "迁移完成：版本17 → 版本18，已添加惩罚时间单位字段")
            }
        }
        
        // 数据库版本迁移：从版本18到版本19（添加奖励时间单位字段）
        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 为目标奖罚用户表添加奖励时间单位字段
                db.execSQL("ALTER TABLE goals_reward_punishment_users ADD COLUMN rewardTimeUnit TEXT NOT NULL DEFAULT '天'")
                
                // 为目标奖罚默认表添加奖励时间单位字段
                db.execSQL("ALTER TABLE goals_reward_punishment_defaults ADD COLUMN rewardTimeUnit TEXT NOT NULL DEFAULT '天'")
                
                android.util.Log.d("DatabaseMigration", "迁移完成：版本18 → 版本19，已添加奖励时间单位字段")
            }
        }
        
        // 数据库版本迁移：从版本19到版本20（更新现有数据的默认奖罚值）
        private val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 更新所有现有用户表中的奖罚值为标准化格式
                db.execSQL("""
                    UPDATE goals_reward_punishment_users 
                    SET rewardText = '薯片', 
                        rewardNumber = 1, 
                        rewardUnit = '包', 
                        rewardTimeUnit = '小时',
                        punishText = '俯卧撑',
                        punishNumber = 30,
                        punishUnit = '个',
                        punishTimeUnit = '小时'
                """)
                
                // 更新所有默认表中的奖罚值为标准化格式
                db.execSQL("""
                    UPDATE goals_reward_punishment_defaults 
                    SET rewardText = '薯片', 
                        rewardNumber = 1, 
                        rewardUnit = '包', 
                        rewardTimeUnit = '小时',
                        punishText = '俯卧撑',
                        punishNumber = 30,
                        punishUnit = '个',
                        punishTimeUnit = '小时'
                """)
                
                android.util.Log.d("DatabaseMigration", "迁移完成：版本19 → 版本20，已更新所有现有数据的默认奖罚值为标准化格式")
            }
        }
        
        // 数据库版本迁移：从版本20到版本21（修复组合文本问题）
        private val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                android.util.Log.d("DatabaseMigration", "开始迁移：版本20 → 版本21，修复奖励惩罚文本字段的组合文本问题")
                
                // 修复用户表中可能存在的组合文本问题
                // 检查并修复 rewardText 字段，如果包含数字或单位，则清理为纯动作名
                val userCursor = db.query("SELECT catId, rewardText, punishText FROM goals_reward_punishment_users")
                while (userCursor.moveToNext()) {
                    val catId = userCursor.getInt(0)
                    val rewardText = userCursor.getString(1)
                    val punishText = userCursor.getString(2)
                    
                    // 清理奖励文本：移除数字、单位等，只保留纯动作名
                    val cleanRewardText = cleanActionText(rewardText)
                    val cleanPunishText = cleanActionText(punishText)
                    
                    // 只有当文本确实需要清理时才更新
                    if (cleanRewardText != rewardText || cleanPunishText != punishText) {
                        db.execSQL("""
                            UPDATE goals_reward_punishment_users 
                            SET rewardText = ?, punishText = ?
                            WHERE catId = ?
                        """, arrayOf(cleanRewardText, cleanPunishText, catId))
                        
                        android.util.Log.d("DatabaseMigration", "清理分类$catId: 奖励'$rewardText'->'$cleanRewardText', 惩罚'$punishText'->'$cleanPunishText'")
                    }
                }
                userCursor.close()
                
                // 同样处理默认表
                val defaultCursor = db.query("SELECT catId, rewardText, punishText FROM goals_reward_punishment_defaults")
                while (defaultCursor.moveToNext()) {
                    val catId = defaultCursor.getInt(0)
                    val rewardText = defaultCursor.getString(1)
                    val punishText = defaultCursor.getString(2)
                    
                    val cleanRewardText = cleanActionText(rewardText)
                    val cleanPunishText = cleanActionText(punishText)
                    
                    if (cleanRewardText != rewardText || cleanPunishText != punishText) {
                        db.execSQL("""
                            UPDATE goals_reward_punishment_defaults 
                            SET rewardText = ?, punishText = ?
                            WHERE catId = ?
                        """, arrayOf(cleanRewardText, cleanPunishText, catId))
                        
                        android.util.Log.d("DatabaseMigration", "清理默认分类$catId: 奖励'$rewardText'->'$cleanRewardText', 惩罚'$punishText'->'$cleanPunishText'")
                    }
                }
                defaultCursor.close()
                
                android.util.Log.d("DatabaseMigration", "迁移完成：版本20 → 版本21，已修复所有组合文本问题")
            }
        }
        
        // 清理动作文本，移除数字、单位等，只保留纯动作名称
        private fun cleanActionText(text: String): String {
            // 常见的清理模式：移除数字、单位、频率词汇
            var cleanText = text
                .replace(Regex("每小时|每分钟|每秒|每天"), "") // 移除频率词汇
                .replace(Regex("\\d+"), "") // 移除数字
                .replace(Regex("个|包|杯|组|公里|分钟|小时|秒"), "") // 移除单位
                .replace(Regex("一"), "") // 移除"一"
                .trim()
            
            // 如果清理后为空或过短，则使用默认值
            if (cleanText.isEmpty() || cleanText.length < 2) {
                // 根据原文本判断应该是奖励还是惩罚
                cleanText = if (text.contains("俯卧撑") || text.contains("跑步") || text.contains("仰卧起坐")) {
                    "俯卧撑"
                } else {
                    "薯片"
                }
            }
            
            return cleanText
        }
        
        // 迁移现有奖罚数据的辅助函数
        private fun migrateRewardPunishmentData(db: SupportSQLiteDatabase) {
            // 迁移用户表数据
            val userCursor = db.query("SELECT catId, rewardText, punishText FROM goals_reward_punishment_users")
            while (userCursor.moveToNext()) {
                val catId = userCursor.getInt(0)
                val rewardText = userCursor.getString(1)
                val punishText = userCursor.getString(2)
                
                val (rewardNumber, rewardUnit) = parseRewardPunishmentText(rewardText)
                val (punishNumber, punishUnit) = parseRewardPunishmentText(punishText)
                
                db.execSQL("""
                    UPDATE goals_reward_punishment_users 
                    SET rewardNumber = ?, rewardUnit = ?, punishNumber = ?, punishUnit = ?
                    WHERE catId = ?
                """, arrayOf(rewardNumber, rewardUnit, punishNumber, punishUnit, catId))
            }
            userCursor.close()
            
            // 迁移默认表数据
            val defaultCursor = db.query("SELECT catId, rewardText, punishText FROM goals_reward_punishment_defaults")
            while (defaultCursor.moveToNext()) {
                val catId = defaultCursor.getInt(0)
                val rewardText = defaultCursor.getString(1)
                val punishText = defaultCursor.getString(2)
                
                val (rewardNumber, rewardUnit) = parseRewardPunishmentText(rewardText)
                val (punishNumber, punishUnit) = parseRewardPunishmentText(punishText)
                
                db.execSQL("""
                    UPDATE goals_reward_punishment_defaults 
                    SET rewardNumber = ?, rewardUnit = ?, punishNumber = ?, punishUnit = ?
                    WHERE catId = ?
                """, arrayOf(rewardNumber, rewardUnit, punishNumber, punishUnit, catId))
            }
            defaultCursor.close()
        }
        
        // 解析奖罚文本，提取数字和动作名称
        private fun parseRewardPunishmentText(text: String): Pair<Int, String> {
            // 常见模式匹配
            val patterns = listOf(
                Regex("(\\d+)个(.+)"),           // "30个俯卧撑" -> 30, "俯卧撑"
                Regex("(.+)(\\d+)个"),           // "俯卧撑30个" -> 30, "俯卧撑"
                Regex("每小时(.+)\\s*(\\d+)\\s*个"), // "每小时俯卧撑 30 个" -> 30, "俯卧撑"
                Regex("每小时(.+)"),             // "每小时俯卧撑" -> 0, "俯卧撑"
                Regex("(\\d+)(.+)"),             // "30俯卧撑" -> 30, "俯卧撑"
            )
            
            for (pattern in patterns) {
                val match = pattern.find(text)
                if (match != null) {
                    val groups = match.groupValues
                    when (pattern.pattern) {
                        "(\\d+)个(.+)" -> {
                            return Pair(groups[1].toIntOrNull() ?: 0, groups[2].trim())
                        }
                        "(.+)(\\d+)个" -> {
                            return Pair(groups[2].toIntOrNull() ?: 0, groups[1].trim())
                        }
                        "每小时(.+)\\s*(\\d+)\\s*个" -> {
                            return Pair(groups[2].toIntOrNull() ?: 0, groups[1].trim())
                        }
                        "每小时(.+)" -> {
                            return Pair(30, groups[1].trim()) // 默认30个
                        }
                        "(\\d+)(.+)" -> {
                            return Pair(groups[1].toIntOrNull() ?: 0, groups[2].trim())
                        }
                    }
                }
            }
            
            // 如果没有匹配到，尝试提取纯动作名称
            val cleanText = text.replace(Regex("每小时|每分钟|每秒|\\d+|个|包|杯|组|公里"), "").trim()
            if (cleanText.isNotBlank()) {
                return Pair(30, cleanText) // 默认30个
            }
            
            // 最后的回退
            return Pair(30, "俯卧撑")
        }
        
        // 数据库版本迁移：从版本21到版本22（添加widget显示天数配置）
        private val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                android.util.Log.d("DatabaseMigration", "开始迁移：版本21 → 版本22，添加widget显示天数配置")
                
                // 为app_settings表添加widgetDisplayDays字段
                db.execSQL("""
                    ALTER TABLE app_settings ADD COLUMN widgetDisplayDays INTEGER NOT NULL DEFAULT 30
                """)
                
                android.util.Log.d("DatabaseMigration", "迁移完成：版本21 → 版本22，已添加widget显示天数配置字段")
            }
        }
        
        // 数据库版本迁移：从版本22到版本23（添加用户付费相关字段）
        private val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                android.util.Log.d("DatabaseMigration", "开始迁移：版本22 → 版本23，添加用户付费相关字段")
                
                // 为user_accounts表添加付费相关字段
                db.execSQL("""
                    ALTER TABLE user_accounts ADD COLUMN is_premium INTEGER NOT NULL DEFAULT 0
                """)
                
                db.execSQL("""
                    ALTER TABLE user_accounts ADD COLUMN trial_start_time INTEGER NOT NULL DEFAULT 0
                """)
                
                db.execSQL("""
                    ALTER TABLE user_accounts ADD COLUMN subscription_status TEXT NOT NULL DEFAULT 'TRIAL'
                """)
                
                db.execSQL("""
                    ALTER TABLE user_accounts ADD COLUMN payment_time INTEGER NOT NULL DEFAULT 0
                """)
                
                db.execSQL("""
                    ALTER TABLE user_accounts ADD COLUMN payment_amount INTEGER NOT NULL DEFAULT 0
                """)
                
                // 为现有用户设置试用开始时间为当前时间
                val currentTime = System.currentTimeMillis()
                db.execSQL("""
                    UPDATE user_accounts 
                    SET trial_start_time = $currentTime, subscription_status = 'TRIAL'
                    WHERE trial_start_time = 0
                """)
                
                android.util.Log.d("DatabaseMigration", "迁移完成：版本22 → 版本23，已添加用户付费相关字段")
            }
        }
        
        // 数据库创建回调，用于插入默认数据
        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                android.util.Log.d("DatabaseCallback", "数据库onCreate回调被调用，开始插入默认数据")
                
                // 插入默认分类数据到默认表
                insertDefaultCategoriesToDefaultTable(db)
                
                // 验证默认分类是否插入成功
                val categoryCountCursor = db.query("SELECT COUNT(*) FROM AppCategory_Defaults")
                val categoryCount = if (categoryCountCursor.moveToFirst()) categoryCountCursor.getInt(0) else 0
                categoryCountCursor.close()
                android.util.Log.d("DatabaseCallback", "默认分类插入后数量: $categoryCount")
                
                // 插入默认目标奖罚数据
                insertDefaultGoalData(db)
                
                // 创建并插入默认设置数据
                insertDefaultSettings(db)
                
                android.util.Log.d("DatabaseCallback", "数据库默认数据插入完成")
            }
            
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                android.util.Log.d("DatabaseCallback", "数据库onOpen回调被调用")
                
                // 每次打开数据库时检查并确保默认数据存在
                val categoryCountCursor = db.query("SELECT COUNT(*) FROM AppCategory_Defaults")
                val categoryCount = if (categoryCountCursor.moveToFirst()) categoryCountCursor.getInt(0) else 0
                categoryCountCursor.close()
                
                android.util.Log.d("DatabaseCallback", "数据库打开时默认分类数量: $categoryCount")
                
                if (categoryCount == 0) {
                    android.util.Log.w("DatabaseCallback", "检测到默认分类数据缺失，重新插入")
                    insertDefaultCategoriesToDefaultTable(db)
                    
                    // 重新验证
                    val newCategoryCountCursor = db.query("SELECT COUNT(*) FROM AppCategory_Defaults")
                    val newCategoryCount = if (newCategoryCountCursor.moveToFirst()) newCategoryCountCursor.getInt(0) else 0
                    newCategoryCountCursor.close()
                    android.util.Log.d("DatabaseCallback", "重新插入后默认分类数量: $newCategoryCount")
                    
                    if (newCategoryCount > 0) {
                        // 如果默认数据重新插入成功，也插入目标奖罚数据
                        insertDefaultGoalData(db)
                        insertDefaultSettings(db)
                        android.util.Log.d("DatabaseCallback", "默认数据修复完成")
                    }
                }
            }
        }
        
        // 插入默认分类数据到默认表
        private fun insertDefaultCategoriesToDefaultTable(db: SupportSQLiteDatabase) {
            val currentTime = System.currentTimeMillis()
            
            // 插入5个默认分类到默认表
            db.execSQL("""
                INSERT OR IGNORE INTO AppCategory_Defaults (name, displayOrder, isDefault, isLocked, targetType, createdAt, updatedAt) 
                VALUES ('娱乐', 1, 1, 0, 'LESS_THAN', $currentTime, $currentTime)
            """)
            
            db.execSQL("""
                INSERT OR IGNORE INTO AppCategory_Defaults (name, displayOrder, isDefault, isLocked, targetType, createdAt, updatedAt) 
                VALUES ('学习', 2, 1, 0, 'MORE_THAN', $currentTime, $currentTime)
            """)
            
            db.execSQL("""
                INSERT OR IGNORE INTO AppCategory_Defaults (name, displayOrder, isDefault, isLocked, targetType, createdAt, updatedAt) 
                VALUES ('健身', 3, 1, 0, 'MORE_THAN', $currentTime, $currentTime)
            """)
            
            db.execSQL("""
                INSERT OR IGNORE INTO AppCategory_Defaults (name, displayOrder, isDefault, isLocked, targetType, createdAt, updatedAt) 
                VALUES ('总使用', 4, 1, 1, 'LESS_THAN', $currentTime, $currentTime)
            """)
        }
        
        // 插入默认目标奖罚数据 - 统一使用中文标准版本
        private fun insertDefaultGoalData(db: SupportSQLiteDatabase) {
            val currentTime = System.currentTimeMillis()
            
            // 数据库统一存储中文标准版本，UI层负责本地化显示
            val standardRewardText = "薯片"
            val standardRewardUnit = "包"
            val standardPunishText = "俯卧撑"
            val standardPunishUnit = "个"
            val standardTimeUnitMinute = "分钟"
            val standardTimeUnitHour = "小时"
            
            // 根据规范插入默认目标奖罚数据
            // 娱乐: 120分钟, ≤ 目标算完成 (conditionType = 0)
            db.execSQL("""
                INSERT OR IGNORE INTO goals_reward_punishment_defaults (catId, dailyGoalMin, goalTimeUnit, conditionType, rewardText, rewardNumber, rewardUnit, rewardTimeUnit, punishText, punishNumber, punishUnit, punishTimeUnit, updateTime) 
                VALUES (1, 120, ?, 0, ?, 1, ?, ?, ?, 30, ?, ?, ?)
            """, arrayOf(standardTimeUnitMinute, standardRewardText, standardRewardUnit, standardTimeUnitHour, standardPunishText, standardPunishUnit, standardTimeUnitHour, currentTime))
            
            // 学习: 30分钟, ≥ 目标算完成 (conditionType = 1)
            db.execSQL("""
                INSERT OR IGNORE INTO goals_reward_punishment_defaults (catId, dailyGoalMin, goalTimeUnit, conditionType, rewardText, rewardNumber, rewardUnit, rewardTimeUnit, punishText, punishNumber, punishUnit, punishTimeUnit, updateTime) 
                VALUES (2, 30, ?, 1, ?, 1, ?, ?, ?, 30, ?, ?, ?)
            """, arrayOf(standardTimeUnitMinute, standardRewardText, standardRewardUnit, standardTimeUnitHour, standardPunishText, standardPunishUnit, standardTimeUnitHour, currentTime))
            
            // 健身: 30分钟, ≥ 目标算完成 (conditionType = 1)
            db.execSQL("""
                INSERT OR IGNORE INTO goals_reward_punishment_defaults (catId, dailyGoalMin, goalTimeUnit, conditionType, rewardText, rewardNumber, rewardUnit, rewardTimeUnit, punishText, punishNumber, punishUnit, punishTimeUnit, updateTime) 
                VALUES (3, 30, ?, 1, ?, 1, ?, ?, ?, 30, ?, ?, ?)
            """, arrayOf(standardTimeUnitMinute, standardRewardText, standardRewardUnit, standardTimeUnitHour, standardPunishText, standardPunishUnit, standardTimeUnitHour, currentTime))
            
            // 总使用: 240分钟(4小时), ≤ 目标算完成 (conditionType = 0)
            db.execSQL("""
                INSERT OR IGNORE INTO goals_reward_punishment_defaults (catId, dailyGoalMin, goalTimeUnit, conditionType, rewardText, rewardNumber, rewardUnit, rewardTimeUnit, punishText, punishNumber, punishUnit, punishTimeUnit, updateTime) 
                VALUES (4, 240, ?, 0, ?, 2, ?, ?, ?, 60, ?, ?, ?)
            """, arrayOf(standardTimeUnitMinute, standardRewardText, standardRewardUnit, standardTimeUnitHour, standardPunishText, standardPunishUnit, standardTimeUnitHour, currentTime))
        }
        
        private val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                android.util.Log.d("DatabaseMigration", "开始迁移：版本23 → 版本24，为用户表添加支付宝用户ID字段")
                
                // 为用户表添加支付宝用户ID字段
                db.execSQL("""
                    ALTER TABLE user_accounts ADD COLUMN alipay_user_id TEXT NOT NULL DEFAULT ''
                """)
                
                // 为支付宝用户ID字段创建唯一索引
                db.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS index_user_accounts_alipay_user_id ON user_accounts(alipay_user_id)
                """)
                
                android.util.Log.d("DatabaseMigration", "迁移完成：版本23 → 版本24，已添加支付宝用户ID字段和索引")
            }
        }
        
        private val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                android.util.Log.d("DatabaseMigration", "开始迁移：版本24 → 版本25，添加用户数据备份设置表")
                
                // 创建备份设置表
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS backup_settings (
                        id INTEGER NOT NULL PRIMARY KEY,
                        backupEnabled INTEGER NOT NULL DEFAULT 1,
                        backupTimeHour INTEGER NOT NULL DEFAULT 2,
                        backupTimeMinute INTEGER NOT NULL DEFAULT 0,
                        lastBackupDate TEXT,
                        totalBackupsCount INTEGER NOT NULL DEFAULT 0,
                        lastBackupResult TEXT,
                        lastBackupError TEXT,
                        wifiOnlyBackup INTEGER NOT NULL DEFAULT 1,
                        autoDeleteOldBackups INTEGER NOT NULL DEFAULT 1,
                        maxBackupDays INTEGER NOT NULL DEFAULT 90,
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        updatedAt INTEGER NOT NULL DEFAULT 0
                    )
                """)
                
                // 插入默认备份设置（随机时间避免网络拥堵）
                val randomHour = (1..6).random()
                val randomMinute = (0..59).random()
                val currentTime = System.currentTimeMillis()
                
                db.execSQL("""
                    INSERT OR REPLACE INTO backup_settings (
                        id, backupEnabled, backupTimeHour, backupTimeMinute, 
                        lastBackupDate, totalBackupsCount, lastBackupResult, lastBackupError,
                        wifiOnlyBackup, autoDeleteOldBackups, maxBackupDays,
                        createdAt, updatedAt
                    ) VALUES (
                        1, 1, $randomHour, $randomMinute, 
                        NULL, 0, NULL, NULL,
                        1, 1, 90,
                        $currentTime, $currentTime
                    )
                """)
                
                android.util.Log.d("DatabaseMigration", "迁移完成：版本24 → 版本25，已创建备份设置表，随机备份时间：${randomHour}:${String.format("%02d", randomMinute)}")
            }
        }
        
        private val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(db: SupportSQLiteDatabase) {
                android.util.Log.d("DatabaseMigration", "开始迁移：版本25 → 版本26，统一数据库存储为中文标准版本")
                
                // 数据库统一存储中文版本，UI层负责本地化显示
                val standardRewardText = "薯片"
                val standardRewardUnit = "包"
                val standardPunishText = "俯卧撑"
                val standardPunishUnit = "个"
                val standardTimeUnitMinute = "分钟"
                val standardTimeUnitHour = "小时"
                
                android.util.Log.d("DatabaseMigration", "统一标准: 奖励='$standardRewardText $standardRewardUnit', 惩罚='$standardPunishText $standardPunishUnit'")
                
                // 更新默认表 - 统一为中文标准版本
                db.execSQL("""
                    UPDATE goals_reward_punishment_defaults 
                    SET rewardText = ?, rewardUnit = ?, punishText = ?, punishUnit = ?, goalTimeUnit = ?, rewardTimeUnit = ?, punishTimeUnit = ?
                """, arrayOf(standardRewardText, standardRewardUnit, standardPunishText, standardPunishUnit, standardTimeUnitMinute, standardTimeUnitHour, standardTimeUnitHour))
                
                // 更新用户表 - 将所有英文/中文混合数据统一为中文标准版本
                db.execSQL("""
                    UPDATE goals_reward_punishment_users 
                    SET rewardText = ?, rewardUnit = ?, punishText = ?, punishUnit = ?, goalTimeUnit = ?, rewardTimeUnit = ?, punishTimeUnit = ?
                    WHERE rewardText IN ('薯片', 'Chips') OR punishText IN ('俯卧撑', 'Push-ups') OR rewardUnit IN ('包', 'pack') OR punishUnit IN ('个', '')
                """, arrayOf(standardRewardText, standardRewardUnit, standardPunishText, standardPunishUnit, standardTimeUnitMinute, standardTimeUnitHour, standardTimeUnitHour))
                
                // 验证更新结果
                val cursor = db.query("""
                    SELECT COUNT(*) as count, rewardText, rewardUnit, punishText, punishUnit 
                    FROM goals_reward_punishment_users 
                    GROUP BY rewardText, rewardUnit, punishText, punishUnit
                """)
                
                while (cursor.moveToNext()) {
                    val count = cursor.getInt(0)
                    val rewardText = cursor.getString(1)
                    val rewardUnit = cursor.getString(2)
                    val punishText = cursor.getString(3)
                    val punishUnit = cursor.getString(4)
                    android.util.Log.d("DatabaseMigration", "统一后数据: $count 条记录 - 奖励: '$rewardText $rewardUnit', 惩罚: '$punishText $punishUnit'")
                }
                cursor.close()
                
                android.util.Log.d("DatabaseMigration", "迁移完成：版本25 → 版本26，数据库已统一为中文标准版本，UI层将根据系统语言进行本地化显示")
            }
        }
        
        // 数据库版本迁移：从版本26到版本27（修复user_accounts表缺失字段）
        private val MIGRATION_26_27 = object : Migration(26, 27) {
            override fun migrate(db: SupportSQLiteDatabase) {
                android.util.Log.d("DatabaseMigration", "开始迁移：版本26 → 版本27，修复user_accounts表缺失字段")
                
                // 检查并添加缺失的字段
                try {
                    // 添加email字段（如果不存在）
                    db.execSQL("""
                        ALTER TABLE user_accounts 
                        ADD COLUMN email TEXT NOT NULL DEFAULT ''
                    """)
                    android.util.Log.d("DatabaseMigration", "已添加email字段")
                } catch (e: Exception) {
                    android.util.Log.d("DatabaseMigration", "email字段可能已存在，跳过: ${e.message}")
                }
                
                try {
                    // 添加google_id字段（如果不存在）
                    db.execSQL("""
                        ALTER TABLE user_accounts 
                        ADD COLUMN google_id TEXT NOT NULL DEFAULT ''
                    """)
                    android.util.Log.d("DatabaseMigration", "已添加google_id字段")
                } catch (e: Exception) {
                    android.util.Log.d("DatabaseMigration", "google_id字段可能已存在，跳过: ${e.message}")
                }
                
                try {
                    // 添加subscription_expiry_time字段（如果不存在）
                    db.execSQL("""
                        ALTER TABLE user_accounts 
                        ADD COLUMN subscription_expiry_time INTEGER NOT NULL DEFAULT 0
                    """)
                    android.util.Log.d("DatabaseMigration", "已添加subscription_expiry_time字段")
                } catch (e: Exception) {
                    android.util.Log.d("DatabaseMigration", "subscription_expiry_time字段可能已存在，跳过: ${e.message}")
                }
                
                // 创建缺失的索引
                try {
                    db.execSQL("""
                        CREATE UNIQUE INDEX IF NOT EXISTS index_user_accounts_email 
                        ON user_accounts(email)
                    """)
                    android.util.Log.d("DatabaseMigration", "已创建email索引")
                } catch (e: Exception) {
                    android.util.Log.d("DatabaseMigration", "email索引创建失败: ${e.message}")
                }
                
                try {
                    db.execSQL("""
                        CREATE UNIQUE INDEX IF NOT EXISTS index_user_accounts_google_id 
                        ON user_accounts(google_id)
                    """)
                    android.util.Log.d("DatabaseMigration", "已创建google_id索引")
                } catch (e: Exception) {
                    android.util.Log.d("DatabaseMigration", "google_id索引创建失败: ${e.message}")
                }
                
                android.util.Log.d("DatabaseMigration", "迁移完成：版本26 → 版本27，已修复user_accounts表结构")
            }
        }
        
        // 数据库版本迁移：从版本27到版本28（确保所有字段存在并重建表结构）
        private val MIGRATION_27_28 = object : Migration(27, 28) {
            override fun migrate(db: SupportSQLiteDatabase) {
                android.util.Log.d("DatabaseMigration", "开始迁移：版本27 → 版本28，确保user_accounts表结构完整")
                
                // 重建user_accounts表以确保所有字段都存在
                // 1. 创建新的临时表
                db.execSQL("""
                    CREATE TABLE user_accounts_new (
                        user_id TEXT NOT NULL PRIMARY KEY,
                        phone_number TEXT NOT NULL DEFAULT 'undefined',
                        password_hash TEXT NOT NULL DEFAULT 'undefined',
                        nickname TEXT NOT NULL DEFAULT '',
                        avatar TEXT NOT NULL DEFAULT '',
                        is_logged_in INTEGER NOT NULL DEFAULT 0,
                        last_login_time INTEGER NOT NULL DEFAULT 0,
                        register_time INTEGER NOT NULL DEFAULT 0,
                        server_user_id TEXT NOT NULL DEFAULT '',
                        is_data_sync_enabled INTEGER NOT NULL DEFAULT 0,
                        is_premium INTEGER NOT NULL DEFAULT 0,
                        trial_start_time INTEGER NOT NULL DEFAULT 0,
                        subscription_status TEXT NOT NULL DEFAULT 'TRIAL',
                        payment_time INTEGER NOT NULL DEFAULT 0,
                        payment_amount INTEGER NOT NULL DEFAULT 0,
                        alipay_user_id TEXT NOT NULL DEFAULT '',
                        email TEXT NOT NULL DEFAULT '',
                        google_id TEXT NOT NULL DEFAULT '',
                        subscription_expiry_time INTEGER NOT NULL DEFAULT 0
                    )
                """)
                
                // 2. 复制现有数据到新表（只复制存在的字段）
                db.execSQL("""
                    INSERT INTO user_accounts_new (
                        user_id, phone_number, password_hash, nickname, avatar, 
                        is_logged_in, last_login_time, register_time, server_user_id, 
                        is_data_sync_enabled, is_premium, trial_start_time, 
                        subscription_status, payment_time, payment_amount, alipay_user_id,
                        email, google_id, subscription_expiry_time
                    )
                    SELECT 
                        user_id, 
                        COALESCE(phone_number, 'undefined'),
                        COALESCE(password_hash, 'undefined'),
                        COALESCE(nickname, ''),
                        COALESCE(avatar, ''),
                        COALESCE(is_logged_in, 0),
                        COALESCE(last_login_time, 0),
                        COALESCE(register_time, 0),
                        COALESCE(server_user_id, ''),
                        COALESCE(is_data_sync_enabled, 0),
                        COALESCE(is_premium, 0),
                        COALESCE(trial_start_time, 0),
                        COALESCE(subscription_status, 'TRIAL'),
                        COALESCE(payment_time, 0),
                        COALESCE(payment_amount, 0),
                        COALESCE(alipay_user_id, ''),
                        COALESCE(email, ''),
                        COALESCE(google_id, ''),
                        COALESCE(subscription_expiry_time, 0)
                    FROM user_accounts
                """)
                
                // 3. 删除旧表
                db.execSQL("DROP TABLE user_accounts")
                
                // 4. 重命名新表
                db.execSQL("ALTER TABLE user_accounts_new RENAME TO user_accounts")
                
                // 5. 重新创建所有索引
                db.execSQL("""
                    CREATE UNIQUE INDEX index_user_accounts_phone_number 
                    ON user_accounts(phone_number)
                """)
                
                db.execSQL("""
                    CREATE UNIQUE INDEX index_user_accounts_alipay_user_id 
                    ON user_accounts(alipay_user_id)
                """)
                
                db.execSQL("""
                    CREATE UNIQUE INDEX index_user_accounts_email 
                    ON user_accounts(email)
                """)
                
                db.execSQL("""
                    CREATE UNIQUE INDEX index_user_accounts_google_id 
                    ON user_accounts(google_id)
                """)
                
                // 确保默认分类数据存在
                android.util.Log.d("DatabaseMigration", "检查并插入默认分类数据")
                val categoryCountCursor = db.query("SELECT COUNT(*) FROM AppCategory_Defaults")
                val categoryCount = if (categoryCountCursor.moveToFirst()) categoryCountCursor.getInt(0) else 0
                categoryCountCursor.close()
                
                if (categoryCount == 0) {
                    android.util.Log.d("DatabaseMigration", "默认分类表为空，开始插入默认数据")
                    insertDefaultCategoriesToDefaultTable(db)
                    android.util.Log.d("DatabaseMigration", "默认分类数据插入完成")
                } else {
                    android.util.Log.d("DatabaseMigration", "默认分类表已有 $categoryCount 条数据，跳过插入")
                }
                
                android.util.Log.d("DatabaseMigration", "迁移完成：版本27 → 版本28，user_accounts表结构已完全重建")
            }
        }
        
        // 数据库版本迁移：从版本28到版本29（添加奖罚完成百分比字段）
        private val MIGRATION_28_29 = object : Migration(28, 29) {
            override fun migrate(db: SupportSQLiteDatabase) {
                android.util.Log.d("DatabaseMigration", "开始迁移：版本28 → 版本29，添加奖罚完成百分比字段")
                
                try {
                    // 添加新的百分比字段到reward_punishment_user表
                    db.execSQL("ALTER TABLE reward_punishment_user ADD COLUMN rewardCompletionPercent INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE reward_punishment_user ADD COLUMN punishCompletionPercent INTEGER NOT NULL DEFAULT 0")
                    
                    // 为现有数据设置默认百分比
                    // 如果已完成(rewardDone=1或punishDone=1)，设置为100%，否则为0%
                    db.execSQL("""
                        UPDATE reward_punishment_user 
                        SET rewardCompletionPercent = CASE WHEN rewardDone = 1 THEN 100 ELSE 0 END
                    """)
                    
                    db.execSQL("""
                        UPDATE reward_punishment_user 
                        SET punishCompletionPercent = CASE WHEN punishDone = 1 THEN 100 ELSE 0 END
                    """)
                    
                    android.util.Log.d("DatabaseMigration", "迁移完成：版本28 → 版本29，奖罚完成百分比字段已添加")
                    
                } catch (e: Exception) {
                    android.util.Log.e("DatabaseMigration", "迁移失败：版本28 → 版本29", e)
                    throw e
                }
            }
        }
        
        // 数据库版本迁移：从版本29到版本30（修复总使用分类的目标数据）
        private val MIGRATION_29_30 = object : Migration(29, 30) {
            override fun migrate(db: SupportSQLiteDatabase) {
                android.util.Log.d("DatabaseMigration", "开始迁移：版本29 → 版本30，修复总使用分类的目标数据")
                
                try {
                    val currentTime = System.currentTimeMillis()
                    
                    // 为"总使用"分类添加默认目标数据到defaults表（如果不存在）
                    db.execSQL("""
                        INSERT OR IGNORE INTO goals_reward_punishment_defaults (catId, dailyGoalMin, goalTimeUnit, conditionType, rewardText, rewardNumber, rewardUnit, rewardTimeUnit, punishText, punishNumber, punishUnit, punishTimeUnit, updateTime) 
                        VALUES (4, 240, '分钟', 0, '薯片', 2, '包', '小时', '俯卧撑', 60, '个', '小时', $currentTime)
                    """)
                    
                    // 为没有目标配置的"总使用"分类用户记录添加默认目标数据
                    // 首先检查"总使用"分类是否存在且没有目标配置
                    db.execSQL("""
                        INSERT OR IGNORE INTO goals_reward_punishment_users (catId, dailyGoalMin, goalTimeUnit, conditionType, rewardText, rewardNumber, rewardUnit, rewardTimeUnit, punishText, punishNumber, punishUnit, punishTimeUnit, updateTime)
                        SELECT u.id, 240, '分钟', 0, '薯片', 2, '包', '小时', '俯卧撑', 60, '个', '小时', $currentTime
                        FROM AppCategory_Users u
                        WHERE u.name = '总使用' OR u.name = 'All'
                        AND u.id NOT IN (SELECT catId FROM goals_reward_punishment_users)
                    """)
                    
                    android.util.Log.d("DatabaseMigration", "迁移完成：版本29 → 版本30，总使用分类的目标数据已修复")
                    
                } catch (e: Exception) {
                    android.util.Log.e("DatabaseMigration", "迁移失败：版本29 → 版本30", e)
                    throw e
                }
            }
        }
        
        // 插入默认设置数据
        private fun insertDefaultSettings(db: SupportSQLiteDatabase) {
            val currentTime = System.currentTimeMillis()
            
            // 插入默认设置记录
            db.execSQL("""
                INSERT OR REPLACE INTO app_settings (id, defaultCategoryId, categoryRewardPunishmentEnabled, widgetDisplayDays, createdAt, updatedAt)
                VALUES (1, 1, '', 30, $currentTime, $currentTime)
            """)
        }
    }
} 