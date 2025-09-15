package com.offtime.app.data.dao

import androidx.room.*
import com.offtime.app.data.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * 用户数据访问对象 (DAO)
 * 提供用户账户相关的数据库操作方法
 * 
 * 包含以下功能：
 * - 用户注册和登录状态管理
 * - 用户信息查询和更新
 * - 密码验证和修改
 * - 数据同步状态管理
 */
@Dao
interface UserDao {
    
    /**
     * 插入新用户账户
     * @param user 用户实体对象
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)
    
    /**
     * 更新用户信息
     * @param user 用户实体对象
     */
    @Update
    suspend fun updateUser(user: UserEntity)
    
    /**
     * 删除用户账户
     * @param user 用户实体对象
     */
    @Delete
    suspend fun deleteUser(user: UserEntity)
    
    /**
     * 根据手机号查询用户
     * @param phoneNumber 手机号
     * @return 用户实体对象，如果不存在则返回null
     */
    @Query("SELECT * FROM user_accounts WHERE phone_number = :phoneNumber LIMIT 1")
    suspend fun getUserByPhoneNumber(phoneNumber: String): UserEntity?
    
    /**
     * 根据用户ID查询用户
     * @param userId 用户ID
     * @return 用户实体对象，如果不存在则返回null
     */
    @Query("SELECT * FROM user_accounts WHERE user_id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?
    
    /**
     * 根据支付宝用户ID查询用户
     * @param alipayUserId 支付宝用户ID
     * @return 用户实体对象，如果不存在则返回null
     */
    @Query("SELECT * FROM user_accounts WHERE alipay_user_id = :alipayUserId LIMIT 1")
    suspend fun getUserByAlipayUserId(alipayUserId: String): UserEntity?
    
    /**
     * 根据Google用户ID查询用户
     * @param googleId Google用户ID
     * @return 用户实体对象，如果不存在则返回null
     */
    @Query("SELECT * FROM user_accounts WHERE google_id = :googleId LIMIT 1")
    suspend fun getUserByGoogleId(googleId: String): UserEntity?
    
    /**
     * 根据邮箱地址查询用户
     * @param email 邮箱地址
     * @return 用户实体对象，如果不存在则返回null
     */
    @Query("SELECT * FROM user_accounts WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?
    
    /**
     * 获取当前登录用户
     * @return 当前登录的用户实体对象
     */
    @Query("SELECT * FROM user_accounts WHERE is_logged_in = 1 LIMIT 1")
    suspend fun getCurrentUser(): UserEntity?
    
    /**
     * 获取当前登录用户的Flow对象，用于实时监听登录状态变化
     * @return 当前登录用户的Flow
     */
    @Query("SELECT * FROM user_accounts WHERE is_logged_in = 1 LIMIT 1")
    fun getCurrentUserFlow(): Flow<UserEntity?>
    
    /**
     * 更新用户登录状态
     * @param userId 用户ID
     * @param isLoggedIn 登录状态
     * @param lastLoginTime 最后登录时间
     */
    @Query("UPDATE user_accounts SET is_logged_in = :isLoggedIn, last_login_time = :lastLoginTime WHERE user_id = :userId")
    suspend fun updateLoginStatus(userId: String, isLoggedIn: Boolean, lastLoginTime: Long)
    
    /**
     * 登出所有用户（设置所有用户登录状态为false）
     */
    @Query("UPDATE user_accounts SET is_logged_in = 0")
    suspend fun logoutAllUsers()
    
    /**
     * 更新用户密码哈希值
     * @param userId 用户ID
     * @param passwordHash 新的密码哈希值
     */
    @Query("UPDATE user_accounts SET password_hash = :passwordHash WHERE user_id = :userId")
    suspend fun updatePassword(userId: String, passwordHash: String)
    
    /**
     * 更新用户昵称
     * @param userId 用户ID
     * @param nickname 新昵称
     */
    @Query("UPDATE user_accounts SET nickname = :nickname WHERE user_id = :userId")
    suspend fun updateNickname(userId: String, nickname: String)
    
    /**
     * 更新用户头像
     * @param userId 用户ID
     * @param avatar 头像URL
     */
    @Query("UPDATE user_accounts SET avatar = :avatar WHERE user_id = :userId")
    suspend fun updateAvatar(userId: String, avatar: String)
    
    /**
     * 更新数据同步设置
     * @param userId 用户ID
     * @param isEnabled 是否启用数据同步
     */
    @Query("UPDATE user_accounts SET is_data_sync_enabled = :isEnabled WHERE user_id = :userId")
    suspend fun updateDataSyncSetting(userId: String, isEnabled: Boolean)
    
    /**
     * 检查手机号是否已存在
     * @param phoneNumber 手机号
     * @return 存在返回true，不存在返回false
     */
    @Query("SELECT COUNT(*) > 0 FROM user_accounts WHERE phone_number = :phoneNumber")
    suspend fun isPhoneNumberExists(phoneNumber: String): Boolean
} 