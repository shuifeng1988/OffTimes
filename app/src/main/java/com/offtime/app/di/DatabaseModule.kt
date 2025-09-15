package com.offtime.app.di

import android.content.Context
import androidx.room.Room
import com.offtime.app.data.dao.AppCategoryDao
import com.offtime.app.data.dao.AppCategoryDefaultDao
import com.offtime.app.data.dao.AppInfoDao
import com.offtime.app.data.dao.AppInfoDefaultDao
import com.offtime.app.data.dao.GoalRewardPunishmentDefaultDao
import com.offtime.app.data.dao.GoalRewardPunishmentUserDao
import com.offtime.app.data.dao.TimerSessionDefaultDao
import com.offtime.app.data.dao.TimerSessionUserDao
import com.offtime.app.data.dao.AppSessionDefaultDao
import com.offtime.app.data.dao.AppSessionUserDao
import com.offtime.app.data.dao.DailyUsageDao
import com.offtime.app.data.dao.SummaryUsageDao
import com.offtime.app.data.dao.RewardPunishmentUserDao
import com.offtime.app.data.dao.RewardPunishmentWeekUserDao
import com.offtime.app.data.dao.RewardPunishmentMonthUserDao
import com.offtime.app.data.dao.AppSettingsDao
import com.offtime.app.data.dao.UserDao
import com.offtime.app.data.dao.BackupSettingsDao
import com.offtime.app.data.database.OffTimeDatabase
import com.offtime.app.utils.FirstLaunchManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }
    
    @Provides
    @Singleton
    fun provideOffTimeDatabase(@ApplicationContext context: Context): OffTimeDatabase {
        return OffTimeDatabase.getDatabase(context)
    }
    
    @Provides
    fun provideAppInfoDao(database: OffTimeDatabase): AppInfoDao {
        return database.appInfoDao()
    }
    
    @Provides
    fun provideAppCategoryDao(database: OffTimeDatabase): AppCategoryDao {
        return database.appCategoryDao()
    }
    
    @Provides
    fun provideAppInfoDefaultDao(database: OffTimeDatabase): AppInfoDefaultDao {
        return database.appInfoDefaultDao()
    }
    
    @Provides
    fun provideAppCategoryDefaultDao(database: OffTimeDatabase): AppCategoryDefaultDao {
        return database.appCategoryDefaultDao()
    }
    
    @Provides
    fun provideGoalRewardPunishmentDefaultDao(database: OffTimeDatabase): GoalRewardPunishmentDefaultDao {
        return database.goalRewardPunishmentDefaultDao()
    }
    
    @Provides
    fun provideGoalRewardPunishmentUserDao(database: OffTimeDatabase): GoalRewardPunishmentUserDao {
        return database.goalRewardPunishmentUserDao()
    }
    
    @Provides
    fun provideTimerSessionDefaultDao(database: OffTimeDatabase): TimerSessionDefaultDao {
        return database.timerSessionDefaultDao()
    }
    
    @Provides
    fun provideTimerSessionUserDao(database: OffTimeDatabase): TimerSessionUserDao {
        return database.timerSessionUserDao()
    }
    
    @Provides
    fun provideAppSessionDefaultDao(database: OffTimeDatabase): AppSessionDefaultDao {
        return database.appSessionDefaultDao()
    }
    
    @Provides
    fun provideAppSessionUserDao(database: OffTimeDatabase): AppSessionUserDao {
        return database.appSessionUserDao()
    }
    
    @Provides
    fun provideDailyUsageDao(database: OffTimeDatabase): DailyUsageDao {
        return database.dailyUsageDao()
    }
    
    @Provides
    fun provideSummaryUsageDao(database: OffTimeDatabase): SummaryUsageDao {
        return database.summaryUsageDao()
    }

    @Provides
    fun provideRewardPunishmentUserDao(database: OffTimeDatabase): RewardPunishmentUserDao {
        return database.rewardPunishmentUserDao()
    }

    @Provides
    fun provideRewardPunishmentWeekUserDao(database: OffTimeDatabase): RewardPunishmentWeekUserDao {
        return database.rewardPunishmentWeekUserDao()
    }

    @Provides
    fun provideRewardPunishmentMonthUserDao(database: OffTimeDatabase): RewardPunishmentMonthUserDao {
        return database.rewardPunishmentMonthUserDao()
    }

    @Provides
    fun provideAppSettingsDao(database: OffTimeDatabase): AppSettingsDao {
        return database.appSettingsDao()
    }
    
    @Provides
    fun provideUserDao(database: OffTimeDatabase): UserDao {
        return database.userDao()
    }
    
    @Provides
    fun provideBackupSettingsDao(database: OffTimeDatabase): BackupSettingsDao {
        return database.backupSettingsDao()
    }
    
    @Provides
    @Singleton
    fun provideFirstLaunchManager(@ApplicationContext context: Context): FirstLaunchManager {
        return FirstLaunchManager(context)
    }
    
    @Provides
    @Singleton
    fun provideUsageDataValidator(
        appSessionUserDao: AppSessionUserDao,
        summaryUsageDao: SummaryUsageDao,
        dailyUsageDao: DailyUsageDao,
        appCategoryDao: AppCategoryDao,
        appInfoDao: AppInfoDao
    ): com.offtime.app.utils.UsageDataValidator {
        return com.offtime.app.utils.UsageDataValidator(
            appSessionUserDao,
            summaryUsageDao,
            dailyUsageDao,
            appCategoryDao,
            appInfoDao
        )
    }
} 