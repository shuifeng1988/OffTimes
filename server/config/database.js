const { Sequelize } = require('sequelize');

// 数据库配置
const sequelize = new Sequelize({
    dialect: 'sqlite',
    storage: './data/offtime.db', // SQLite数据库文件路径
    logging: console.log, // 开发环境下显示SQL日志
    define: {
        timestamps: true, // 自动添加createdAt和updatedAt字段
        underscored: true, // 使用下划线命名
        freezeTableName: true // 使用单数表名
    },
    pool: {
        max: 5,
        min: 0,
        acquire: 30000,
        idle: 10000
    }
});

// 用户模型
const User = sequelize.define('users', {
    id: {
        type: Sequelize.UUID,
        defaultValue: Sequelize.UUIDV4,
        primaryKey: true
    },
    phone_number: {
        type: Sequelize.STRING(50), // 增加长度以支持alipay_前缀和用户名
        allowNull: false,
        unique: true,
        validate: {
            // 支持普通手机号、支付宝用户ID格式和用户名
            is: /^(1[3-9]\d{9}|alipay_.+|[a-zA-Z0-9_]{3,20})$/
        }
    },
    password_hash: {
        type: Sequelize.STRING(255),
        allowNull: false
    },
    nickname: {
        type: Sequelize.STRING(50),
        defaultValue: ''
    },
    avatar: {
        type: Sequelize.STRING(255),
        defaultValue: ''
    },
    last_login_time: {
        type: Sequelize.DATE,
        defaultValue: Sequelize.NOW
    },
    is_active: {
        type: Sequelize.BOOLEAN,
        defaultValue: true
    }
});

// 验证码模型
const SmsCode = sequelize.define('sms_codes', {
    id: {
        type: Sequelize.UUID,
        defaultValue: Sequelize.UUIDV4,
        primaryKey: true
    },
    phone_number: {
        type: Sequelize.STRING(50), // 增加长度以支持alipay_前缀
        allowNull: false,
        validate: {
            // 支持普通手机号和支付宝用户ID格式
            is: /^(1[3-9]\d{9}|alipay_.+)$/
        }
    },
    code: {
        type: Sequelize.STRING(6),
        allowNull: false
    },
    type: {
        type: Sequelize.ENUM('register', 'login', 'reset'),
        allowNull: false
    },
    expires_at: {
        type: Sequelize.DATE,
        allowNull: false
    },
    is_used: {
        type: Sequelize.BOOLEAN,
        defaultValue: false
    },
    ip_address: {
        type: Sequelize.STRING(45),
        allowNull: true
    }
});

// 刷新令牌模型
const RefreshToken = sequelize.define('refresh_tokens', {
    id: {
        type: Sequelize.UUID,
        defaultValue: Sequelize.UUIDV4,
        primaryKey: true
    },
    user_id: {
        type: Sequelize.UUID,
        allowNull: false,
        references: {
            model: User,
            key: 'id'
        }
    },
    token: {
        type: Sequelize.STRING(500),
        allowNull: false,
        unique: true
    },
    expires_at: {
        type: Sequelize.DATE,
        allowNull: false
    },
    is_revoked: {
        type: Sequelize.BOOLEAN,
        defaultValue: false
    }
});

// 用户数据备份表
const UserDataBackup = sequelize.define('user_data_backups', {
    id: {
        type: Sequelize.UUID,
        defaultValue: Sequelize.UUIDV4,
        primaryKey: true
    },
    user_id: {
        type: Sequelize.UUID,
        allowNull: false,
        references: {
            model: User,
            key: 'id'
        }
    },
    backup_date: {
        type: Sequelize.DATEONLY,
        allowNull: false
    },
    table_name: {
        type: Sequelize.STRING(100),
        allowNull: false
    },
    backup_data: {
        type: Sequelize.TEXT('long'), // 存储JSON格式的备份数据
        allowNull: false
    },
    data_hash: {
        type: Sequelize.STRING(64), // SHA256哈希，用于检查数据完整性
        allowNull: false
    },
    record_count: {
        type: Sequelize.INTEGER,
        defaultValue: 0
    },
    file_size: {
        type: Sequelize.INTEGER,
        defaultValue: 0
    }
}, {
    indexes: [
        {
            unique: true,
            fields: ['user_id', 'backup_date', 'table_name']
        },
        {
            fields: ['user_id', 'table_name']
        },
        {
            fields: ['backup_date']
        }
    ]
});

// 用户备份设置表
const UserBackupSettings = sequelize.define('user_backup_settings', {
    id: {
        type: Sequelize.UUID,
        defaultValue: Sequelize.UUIDV4,
        primaryKey: true
    },
    user_id: {
        type: Sequelize.UUID,
        allowNull: false,
        unique: true,
        references: {
            model: User,
            key: 'id'
        }
    },
    backup_enabled: {
        type: Sequelize.BOOLEAN,
        defaultValue: true
    },
    backup_time_hour: {
        type: Sequelize.INTEGER, // 0-23小时
        defaultValue: 2, // 默认凌晨2点
        validate: {
            min: 0,
            max: 23
        }
    },
    backup_time_minute: {
        type: Sequelize.INTEGER, // 0-59分钟
        defaultValue: 0,
        validate: {
            min: 0,
            max: 59
        }
    },
    last_backup_date: {
        type: Sequelize.DATEONLY,
        allowNull: true
    },
    total_backups: {
        type: Sequelize.INTEGER,
        defaultValue: 0
    },
    total_data_size: {
        type: Sequelize.BIGINT,
        defaultValue: 0
    }
});

// 建立关联关系
User.hasMany(RefreshToken, { foreignKey: 'user_id', onDelete: 'CASCADE' });
RefreshToken.belongsTo(User, { foreignKey: 'user_id' });

User.hasMany(UserDataBackup, { foreignKey: 'user_id', onDelete: 'CASCADE' });
UserDataBackup.belongsTo(User, { foreignKey: 'user_id' });

User.hasOne(UserBackupSettings, { foreignKey: 'user_id', onDelete: 'CASCADE' });
UserBackupSettings.belongsTo(User, { foreignKey: 'user_id' });

// 初始化数据库
async function initDatabase(forceSync = false) {
    try {
        // 测试连接
        await sequelize.authenticate();
        console.log('数据库连接测试成功');
        
        // 同步模型（创建表）
        if (forceSync) {
            console.log('⚠️  强制同步数据库模式（将更新表结构）...');
            await sequelize.sync({ alter: true }); // alter: true 会更新表结构而不删除数据
        } else {
            await sequelize.sync({ force: false }); // force: false 表示不删除现有表
        }
        console.log('数据库表同步完成');
        
        return true;
    } catch (error) {
        console.error('数据库初始化失败:', error);
        throw error;
    }
}

module.exports = {
    sequelize,
    User,
    SmsCode,
    RefreshToken,
    UserDataBackup,
    UserBackupSettings,
    initDatabase
}; 