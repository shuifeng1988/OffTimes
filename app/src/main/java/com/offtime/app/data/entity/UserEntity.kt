package com.offtime.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 用户账户实体类
 * 用于存储用户的基本账户信息，包括手机号、密码等
 * 
 * @property userId 用户唯一标识ID
 * @property phoneNumber 用户手机号，用作登录账号
 * @property passwordHash 用户密码哈希值，不存储明文密码
 * @property nickname 用户昵称，可选设置
 * @property avatar 用户头像URL，可选设置
 * @property isLoggedIn 当前登录状态
 * @property lastLoginTime 最后登录时间戳
 * @property registerTime 注册时间戳
 * @property serverUserId 服务器端用户ID，用于数据同步
 * @property isDataSyncEnabled 是否启用数据同步功能
 * @property isPremium 是否为付费用户
 * @property trialStartTime 试用开始时间戳
 * @property subscriptionStatus 订阅状态：TRIAL(试用中), EXPIRED(试用过期), PREMIUM(已付费)
 * @property paymentTime 付费时间戳，0表示未付费
 * @property paymentAmount 付费金额（分），0表示未付费
 */
@Entity(
    tableName = "user_accounts",
    indices = [
        Index(value = ["phone_number"], unique = true),
        Index(value = ["alipay_user_id"], unique = true),
        Index(value = ["google_id"], unique = true),
        Index(value = ["email"], unique = true)
    ]
)
data class UserEntity(
    @PrimaryKey
    @ColumnInfo(name = "user_id")
    val userId: String,
    
    @ColumnInfo(name = "phone_number")
    val phoneNumber: String,
    
    @ColumnInfo(name = "password_hash")
    val passwordHash: String,
    
    @ColumnInfo(name = "nickname", defaultValue = "")
    val nickname: String,
    
    @ColumnInfo(name = "avatar", defaultValue = "")
    val avatar: String,
    
    @ColumnInfo(name = "is_logged_in", defaultValue = "0")
    val isLoggedIn: Boolean,
    
    @ColumnInfo(name = "last_login_time", defaultValue = "0")
    val lastLoginTime: Long,
    
    @ColumnInfo(name = "register_time")
    val registerTime: Long,
    
    @ColumnInfo(name = "server_user_id", defaultValue = "")
    val serverUserId: String,
    
    @ColumnInfo(name = "is_data_sync_enabled", defaultValue = "0")
    val isDataSyncEnabled: Boolean,
    
    @ColumnInfo(name = "is_premium", defaultValue = "0")
    val isPremium: Boolean,
    
    @ColumnInfo(name = "trial_start_time", defaultValue = "0")
    val trialStartTime: Long,
    
    @ColumnInfo(name = "subscription_status", defaultValue = "TRIAL")
    val subscriptionStatus: String, // TRIAL, EXPIRED, PREMIUM
    
    @ColumnInfo(name = "payment_time", defaultValue = "0")
    val paymentTime: Long,
    
    @ColumnInfo(name = "payment_amount", defaultValue = "0")
    val paymentAmount: Int, // 付费金额（分）
    
    @ColumnInfo(name = "alipay_user_id", defaultValue = "")
    val alipayUserId: String, // 支付宝用户ID
    
    @ColumnInfo(name = "email", defaultValue = "")
    val email: String = "", // 邮箱地址
    
    @ColumnInfo(name = "google_id", defaultValue = "")
    val googleId: String = "", // Google用户ID
    
    @ColumnInfo(name = "subscription_expiry_time", defaultValue = "0")
    val subscriptionExpiryTime: Long = 0L // 订阅过期时间
) {
    companion object {
        const val STATUS_TRIAL = "TRIAL"
        const val STATUS_EXPIRED = "EXPIRED"
        const val STATUS_PREMIUM = "PREMIUM"
        
        const val TRIAL_DAYS = 7 // 试用天数
        const val PREMIUM_PRICE_YUAN = 9.9 // 付费价格（元）
        const val PREMIUM_PRICE_CENTS = 990 // 付费价格（分）
    }
    
    /**
     * 检查试用期是否过期
     */
    fun isTrialExpired(): Boolean {
        if (isPremium) return false
        if (trialStartTime == 0L) return false
        
        val trialEndTime = trialStartTime + (TRIAL_DAYS * 24 * 60 * 60 * 1000L)
        return System.currentTimeMillis() > trialEndTime
    }
    
    /**
     * 获取剩余试用天数
     */
    fun getRemainingTrialDays(): Int {
        if (isPremium) return -1
        if (trialStartTime == 0L) return TRIAL_DAYS
        
        val trialEndTime = trialStartTime + (TRIAL_DAYS * 24 * 60 * 60 * 1000L)
        val remainingTime = trialEndTime - System.currentTimeMillis()
        
        return if (remainingTime > 0) {
            (remainingTime / (24 * 60 * 60 * 1000L)).toInt() + 1
        } else {
            0
        }
    }
    
    /**
     * 检查用户是否可以使用应用
     */
    fun canUseApp(): Boolean {
        return isPremium || !isTrialExpired()
    }
} 