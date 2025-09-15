const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const rateLimit = require('express-rate-limit');
require('dotenv').config();

const authRoutes = require('./routes/auth');
const userRoutes = require('./routes/user');
const backupRoutes = require('./routes/backup');
const { initDatabase } = require('./config/database');

const app = express();
const PORT = process.env.PORT || 8080;

// 安全中间件
app.use(helmet());
app.use(cors({
    origin: '*', // 在生产环境中，请指定具体的域名
    methods: ['GET', 'POST', 'PUT', 'DELETE'],
    allowedHeaders: ['Content-Type', 'Authorization']
}));

// 请求频率限制
const limiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15分钟
    max: 100, // 每个IP最多100个请求
    message: {
        code: 429,
        message: '请求过于频繁，请稍后再试'
    }
});
app.use('/api/', limiter);

// 短信验证码限制已移至 routes/auth.js 中的具体端点

// 解析JSON请求体
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true }));

// 健康检查端点
app.get('/health', (req, res) => {
    res.json({
        code: 200,
        message: 'OffTimes服务器运行正常',
        timestamp: new Date().toISOString()
    });
});

// API根路径信息
app.get('/api', (req, res) => {
    res.json({
        code: 200,
        message: 'OffTimes API 服务运行正常',
        success: true,
        data: {
            version: '1.0.0',
            timestamp: new Date().toISOString(),
            endpoints: {
                auth: {
                    login: 'POST /api/auth/login',
                    register: 'POST /api/auth/register',
                    sendSms: 'POST /api/auth/send-sms',
                    verifySms: 'POST /api/auth/verify-sms',
                    loginSms: 'POST /api/auth/login-sms',
                    loginAlipay: 'POST /api/auth/login-alipay',
                    loginGoogle: 'POST /api/auth/login-google'
                },
                user: {
                    profile: 'GET /api/user/profile',
                    updateProfile: 'PUT /api/user/profile'
                },
                backup: {
                    upload: 'POST /api/backup/upload',
                    download: 'GET /api/backup/download',
                    info: 'GET /api/backup/info'
                }
            },
            testAccounts: [
                {
                    account: '13800138000',
                    password: 'offtime123',
                    type: 'phone',
                    note: '测试手机号账号'
                },
                {
                    account: 'admin',
                    password: 'offtime123',
                    type: 'username',
                    note: '管理员账号'
                },
                {
                    account: 'testuser',
                    password: 'offtime123',
                    type: 'username',
                    note: '普通测试用户'
                }
            ]
        }
    });
});

// API路由
app.use('/api/auth', authRoutes);
app.use('/api/user', userRoutes);
app.use('/api/backup', backupRoutes);

// 404处理
app.use('*', (req, res) => {
    res.status(404).json({
        code: 404,
        message: '接口不存在'
    });
});

// 全局错误处理
app.use((err, req, res, next) => {
    console.error('服务器错误:', err);
    res.status(500).json({
        code: 500,
        message: '服务器内部错误',
        error: process.env.NODE_ENV === 'development' ? err.message : '内部错误'
    });
});

// 启动服务器
async function startServer() {
    try {
        // 初始化数据库
        const forceSync = process.env.FORCE_DB_SYNC === 'true';
        await initDatabase(forceSync);
        console.log('数据库连接成功');
        
        app.listen(PORT, '0.0.0.0', () => {
            console.log(`=================================`);
            console.log(`🚀 OffTimes服务器启动成功！`);
            console.log(`📡 端口: ${PORT}`);
            console.log(`🌍 环境: ${process.env.NODE_ENV || 'development'}`);
            console.log(`📅 时间: ${new Date().toLocaleString()}`);
            console.log(`=================================`);
        });
    } catch (error) {
        console.error('服务器启动失败:', error);
        process.exit(1);
    }
}

startServer();

// 优雅关闭
process.on('SIGTERM', () => {
    console.log('收到SIGTERM信号，正在优雅关闭服务器...');
    process.exit(0);
});

process.on('SIGINT', () => {
    console.log('收到SIGINT信号，正在优雅关闭服务器...');
    process.exit(0);
}); 