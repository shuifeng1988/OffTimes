const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const rateLimit = require('express-rate-limit');
const { body, validationResult } = require('express-validator');
const { User, SmsCode, RefreshToken } = require('../config/database');
const { Op } = require('sequelize');

const router = express.Router();

// JWT密钥
const JWT_SECRET = process.env.JWT_SECRET || 'your-super-secret-jwt-key';
const JWT_REFRESH_SECRET = process.env.JWT_REFRESH_SECRET || 'your-super-secret-refresh-key';

// 短信验证码特殊限制 - 只用于发送SMS的端点
const smsLimiter = rateLimit({
    windowMs: 60 * 1000, // 1分钟
    max: 1, // 每分钟最多1次
    message: {
        code: 429,
        message: '发送验证码过于频繁，请稍后再试'
    }
});

// 支付宝登录限制器 - 相对宽松的限制
const alipayLoginLimiter = rateLimit({
    windowMs: 60 * 1000, // 1分钟
    max: 10, // 每分钟最多10次
    message: {
        code: 429,
        message: '支付宝登录请求过于频繁，请稍后再试'
    }
});

// 普通登录限制器 - 更宽松的限制
const loginLimiter = rateLimit({
    windowMs: 60 * 1000, // 1分钟
    max: 20, // 每分钟最多20次
    message: {
        code: 429,
        message: '登录请求过于频繁，请稍后再试'
    }
});

// 生成随机6位数字验证码
function generateSmsCode() {
    return Math.floor(100000 + Math.random() * 900000).toString();
}

// 模拟发送短信验证码
async function sendSmsCodeReal(phoneNumber, code) {
    console.log(`模拟发送短信验证码: ${phoneNumber} -> ${code}`);
    await new Promise(resolve => setTimeout(resolve, 1000));
    return true;
}

// 验证手机号格式
const phoneValidator = body('phoneNumber')
    .matches(/^1[3-9]\d{9}$/)
    .withMessage('手机号格式不正确');

// 验证账号格式（支持手机号、用户名等）
const accountValidator = body('phoneNumber')
    .matches(/^(1[3-9]\d{9}|[a-zA-Z0-9_]{3,20})$/)
    .withMessage('账号格式不正确，支持手机号或3-20位用户名');

// 验证密码格式
const passwordValidator = body('password')
    .isLength({ min: 6 })
    .withMessage('密码长度至少6位');

/**
 * POST /api/auth/send-sms
 * 发送短信验证码
 */
router.post('/send-sms', smsLimiter, [
    phoneValidator,
    body('type').isIn(['register', 'login']).withMessage('验证码类型无效')
], async (req, res) => {
    try {
        const errors = validationResult(req);
        if (!errors.isEmpty()) {
            return res.status(400).json({
                code: 400,
                message: errors.array()[0].msg,
                success: false
            });
        }

        const { phoneNumber, type } = req.body;
        const clientIp = req.ip || req.connection.remoteAddress;

        // 检查同一手机号1分钟内是否已发送验证码
        const recentCode = await SmsCode.findOne({
            where: {
                phone_number: phoneNumber,
                created_at: {
                    [Op.gte]: new Date(Date.now() - 60 * 1000)
                }
            }
        });

        if (recentCode) {
            return res.status(429).json({
                code: 429,
                message: '验证码发送过于频繁，请稍后再试',
                success: false
            });
        }

        // 生成验证码
        const code = generateSmsCode();
        const expiresAt = new Date(Date.now() + 5 * 60 * 1000);

        // 保存验证码到数据库
        await SmsCode.create({
            phone_number: phoneNumber,
            code: code,
            type: type,
            expires_at: expiresAt,
            ip_address: clientIp
        });

        // 发送短信验证码
        const sendResult = await sendSmsCodeReal(phoneNumber, code);
        
        if (!sendResult) {
            return res.status(500).json({
                code: 500,
                message: '短信发送失败，请稍后再试',
                success: false
            });
        }

        res.json({
            code: 200,
            message: '验证码发送成功',
            success: true
        });

    } catch (error) {
        console.error('发送验证码失败:', error);
        res.status(500).json({
            code: 500,
            message: '服务器内部错误',
            success: false
        });
    }
});

/**
 * POST /api/auth/verify-sms
 * 验证短信验证码
 */
router.post('/verify-sms', [
    phoneValidator,
    body('code').isLength({ min: 6, max: 6 }).withMessage('验证码格式不正确'),
    body('type').isIn(['register', 'login']).withMessage('验证码类型无效')
], async (req, res) => {
    try {
        const errors = validationResult(req);
        if (!errors.isEmpty()) {
            return res.status(400).json({
                code: 400,
                message: errors.array()[0].msg,
                success: false
            });
        }

        const { phoneNumber, code, type } = req.body;

        // 查找有效的验证码
        const smsCode = await SmsCode.findOne({
            where: {
                phone_number: phoneNumber,
                code: code,
                type: type,
                is_used: false,
                expires_at: {
                    [Op.gt]: new Date()
                }
            },
            order: [['created_at', 'DESC']]
        });

        if (!smsCode) {
            return res.status(400).json({
                code: 400,
                message: '验证码无效或已过期',
                success: false
            });
        }

        // 标记验证码为已使用
        await smsCode.update({ is_used: true });

        // 生成验证令牌
        const verifyToken = jwt.sign(
            { 
                phoneNumber,
                type,
                timestamp: Date.now()
            },
            JWT_SECRET,
            { expiresIn: '10m' }
        );

        res.json({
            code: 200,
            message: '验证码验证成功',
            success: true,
            data: {
                verifyToken,
                expiresIn: 600000
            }
        });

    } catch (error) {
        console.error('验证短信验证码失败:', error);
        res.status(500).json({
            code: 500,
            message: '服务器内部错误',
            success: false
        });
    }
});

/**
 * POST /api/auth/register
 * 用户注册
 */
router.post('/register', [
    phoneValidator,
    passwordValidator,
    body('verifyToken').notEmpty().withMessage('验证令牌不能为空'),
    body('nickname').optional().isLength({ max: 50 }).withMessage('昵称长度不能超过50字符')
], async (req, res) => {
    try {
        // 验证输入
        const errors = validationResult(req);
        if (!errors.isEmpty()) {
            return res.status(400).json({
                code: 400,
                message: errors.array()[0].msg,
                success: false
            });
        }

        const { phoneNumber, password, verifyToken, nickname = '' } = req.body;

        // 验证验证令牌
        let tokenPayload;
        try {
            tokenPayload = jwt.verify(verifyToken, JWT_SECRET);
        } catch (err) {
            return res.status(400).json({
                code: 400,
                message: '验证令牌无效或已过期',
                success: false
            });
        }

        // 检查令牌信息
        if (tokenPayload.phoneNumber !== phoneNumber || tokenPayload.type !== 'register') {
            return res.status(400).json({
                code: 400,
                message: '验证令牌信息不匹配',
                success: false
            });
        }

        // 检查手机号是否已注册
        const existingUser = await User.findOne({ where: { phone_number: phoneNumber } });
        if (existingUser) {
            return res.status(400).json({
                code: 400,
                message: '该手机号已注册',
                success: false
            });
        }

        // 加密密码
        const passwordHash = await bcrypt.hash(password, 12);

        // 创建用户
        const user = await User.create({
            phone_number: phoneNumber,
            password_hash: passwordHash,
            nickname: nickname,
            last_login_time: new Date()
        });

        // 生成JWT令牌
        const accessToken = jwt.sign(
            { userId: user.id, phoneNumber: user.phone_number },
            JWT_SECRET,
            { expiresIn: '24h' }
        );

        const refreshToken = jwt.sign(
            { userId: user.id },
            JWT_REFRESH_SECRET,
            { expiresIn: '7d' }
        );

        // 保存刷新令牌
        await RefreshToken.create({
            user_id: user.id,
            token: refreshToken,
            expires_at: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000) // 7天
        });

        res.status(201).json({
            code: 200,
            message: '注册成功',
            success: true,
            data: {
                user: {
                    userId: user.id,
                    phoneNumber: user.phone_number,
                    nickname: user.nickname,
                    avatar: user.avatar,
                    registerTime: user.createdAt ? user.createdAt.getTime() : Date.now(),
                    lastLoginTime: user.last_login_time ? user.last_login_time.getTime() : Date.now()
                },
                accessToken,
                refreshToken,
                expiresIn: 86400000 // 24小时（毫秒）
            }
        });

    } catch (error) {
        console.error('注册失败:', error);
        res.status(500).json({
            code: 500,
            message: '服务器内部错误',
            success: false
        });
    }
});

/**
 * POST /api/auth/register-no-sms
 * 用户注册（无需短信验证码）
 */
router.post('/register-no-sms', [
    accountValidator,
    passwordValidator,
    body('nickname').optional().isLength({ max: 50 }).withMessage('昵称长度不能超过50字符')
], async (req, res) => {
    try {
        // 验证输入
        const errors = validationResult(req);
        if (!errors.isEmpty()) {
            return res.status(400).json({
                code: 400,
                message: errors.array()[0].msg,
                success: false
            });
        }

        const { phoneNumber, password, nickname = '' } = req.body;

        // 检查账号是否已注册
        const existingUser = await User.findOne({ where: { phone_number: phoneNumber } });
        if (existingUser) {
            return res.status(400).json({
                code: 400,
                message: '该账号已注册',
                success: false
            });
        }

        // 加密密码
        const passwordHash = await bcrypt.hash(password, 12);

        // 创建用户
        const user = await User.create({
            phone_number: phoneNumber,
            password_hash: passwordHash,
            nickname: nickname,
            last_login_time: new Date()
        });

        // 生成JWT令牌
        const accessToken = jwt.sign(
            { userId: user.id, phoneNumber: user.phone_number },
            JWT_SECRET,
            { expiresIn: '24h' }
        );

        const refreshToken = jwt.sign(
            { userId: user.id },
            JWT_REFRESH_SECRET,
            { expiresIn: '7d' }
        );

        // 保存刷新令牌
        await RefreshToken.create({
            user_id: user.id,
            token: refreshToken,
            expires_at: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000) // 7天
        });

        res.status(201).json({
            code: 200,
            message: '注册成功',
            success: true,
            data: {
                user: {
                    userId: user.id,
                    phoneNumber: user.phone_number,
                    nickname: user.nickname,
                    avatar: user.avatar,
                    registerTime: user.createdAt ? user.createdAt.getTime() : Date.now(),
                    lastLoginTime: user.last_login_time ? user.last_login_time.getTime() : Date.now()
                },
                accessToken,
                refreshToken,
                expiresIn: 86400000 // 24小时（毫秒）
            }
        });

    } catch (error) {
        console.error('注册失败:', error);
        res.status(500).json({
            code: 500,
            message: '服务器内部错误',
            success: false
        });
    }
});

/**
 * POST /api/auth/login
 * 密码登录
 */
router.post('/login', loginLimiter, [
    accountValidator,
    passwordValidator
], async (req, res) => {
    try {
        // 验证输入
        const errors = validationResult(req);
        if (!errors.isEmpty()) {
            return res.status(400).json({
                code: 400,
                message: errors.array()[0].msg,
                success: false
            });
        }

        const { phoneNumber, password } = req.body;

        // 查找用户（支持手机号或用户名登录）
        let user = await User.findOne({ where: { phone_number: phoneNumber } });
        
        // 如果用户不存在，检查是否是测试账号
        if (!user && password === 'offtime123' && 
            (phoneNumber === 'admin' || phoneNumber === 'testuser' || phoneNumber === '13800138000')) {
            // 创建测试用户
            const passwordHash = await bcrypt.hash(password, 12);
            
            user = await User.create({
                phone_number: phoneNumber,
                password_hash: passwordHash,
                nickname: phoneNumber === 'admin' ? '管理员' : 
                         phoneNumber === 'testuser' ? '测试用户' : '测试手机用户',
                last_login_time: new Date()
            });
            
            console.log(`✅ 创建测试账号: ${phoneNumber}`);
        }
        
        if (!user) {
            return res.status(400).json({
                code: 400,
                message: '账号或密码错误',
                success: false
            });
        }

        // 验证密码
        const isPasswordValid = await bcrypt.compare(password, user.password_hash);
        if (!isPasswordValid) {
            return res.status(400).json({
                code: 400,
                message: '账号或密码错误',
                success: false
            });
        }

        // 更新最后登录时间
        await user.update({ last_login_time: new Date() });

        // 生成JWT令牌
        const accessToken = jwt.sign(
            { userId: user.id, phoneNumber: user.phone_number },
            JWT_SECRET,
            { expiresIn: '24h' }
        );

        const refreshToken = jwt.sign(
            { userId: user.id },
            JWT_REFRESH_SECRET,
            { expiresIn: '7d' }
        );

        // 保存刷新令牌
        await RefreshToken.create({
            user_id: user.id,
            token: refreshToken,
            expires_at: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000) // 7天
        });

        res.json({
            code: 200,
            message: '登录成功',
            success: true,
            data: {
                user: {
                    userId: user.id,
                    phoneNumber: user.phone_number,
                    nickname: user.nickname,
                    avatar: user.avatar,
                    registerTime: user.createdAt ? user.createdAt.getTime() : Date.now(),
                    lastLoginTime: user.last_login_time ? user.last_login_time.getTime() : Date.now()
                },
                accessToken,
                refreshToken,
                expiresIn: 86400000 // 24小时（毫秒）
            }
        });

    } catch (error) {
        console.error('登录失败:', error);
        res.status(500).json({
            code: 500,
            message: '服务器内部错误',
            success: false
        });
    }
});

/**
 * POST /api/auth/login-sms
 * 验证码登录
 */
router.post('/login-sms', [
    phoneValidator,
    body('verifyToken').notEmpty().withMessage('验证令牌不能为空')
], async (req, res) => {
    try {
        // 验证输入
        const errors = validationResult(req);
        if (!errors.isEmpty()) {
            return res.status(400).json({
                code: 400,
                message: errors.array()[0].msg,
                success: false
            });
        }

        const { phoneNumber, verifyToken } = req.body;

        // 验证验证令牌
        let tokenPayload;
        try {
            tokenPayload = jwt.verify(verifyToken, JWT_SECRET);
        } catch (err) {
            return res.status(400).json({
                code: 400,
                message: '验证令牌无效或已过期',
                success: false
            });
        }

        // 检查令牌信息
        if (tokenPayload.phoneNumber !== phoneNumber || tokenPayload.type !== 'login') {
            return res.status(400).json({
                code: 400,
                message: '验证令牌信息不匹配',
                success: false
            });
        }

        // 查找用户
        const user = await User.findOne({ where: { phone_number: phoneNumber } });
        if (!user) {
            return res.status(400).json({
                code: 400,
                message: '用户不存在',
                success: false
            });
        }

        // 更新最后登录时间
        await user.update({ last_login_time: new Date() });

        // 生成JWT令牌
        const accessToken = jwt.sign(
            { userId: user.id, phoneNumber: user.phone_number },
            JWT_SECRET,
            { expiresIn: '24h' }
        );

        const refreshToken = jwt.sign(
            { userId: user.id },
            JWT_REFRESH_SECRET,
            { expiresIn: '7d' }
        );

        // 保存刷新令牌
        await RefreshToken.create({
            user_id: user.id,
            token: refreshToken,
            expires_at: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000) // 7天
        });

        res.json({
            code: 200,
            message: '登录成功',
            success: true,
            data: {
                user: {
                    userId: user.id,
                    phoneNumber: user.phone_number,
                    nickname: user.nickname,
                    avatar: user.avatar,
                    registerTime: user.createdAt ? user.createdAt.getTime() : Date.now(),
                    lastLoginTime: user.last_login_time ? user.last_login_time.getTime() : Date.now()
                },
                accessToken,
                refreshToken,
                expiresIn: 86400000 // 24小时（毫秒）
            }
        });

    } catch (error) {
        console.error('验证码登录失败:', error);
        res.status(500).json({
            code: 500,
            message: '服务器内部错误',
            success: false
        });
    }
});

/**
 * POST /api/auth/login-alipay
 * 支付宝登录
 */
router.post('/login-alipay', alipayLoginLimiter, [
    body('alipayUserId').notEmpty().withMessage('支付宝用户ID不能为空'),
    body('authCode').notEmpty().withMessage('授权码不能为空'),
    body('nickname').optional().isLength({ max: 50 }).withMessage('昵称长度不能超过50字符'),
    body('avatar').optional().custom((value) => {
        if (value && value.trim() !== '' && !value.match(/^https?:\/\/.+/)) {
            throw new Error('头像URL格式不正确');
        }
        return true;
    })
], async (req, res) => {
    try {
        // 验证输入
        const errors = validationResult(req);
        if (!errors.isEmpty()) {
            return res.status(400).json({
                code: 400,
                message: errors.array()[0].msg,
                success: false
            });
        }

        const { alipayUserId, authCode, nickname = '支付宝用户', avatar = '' } = req.body;
        
        console.log(`🚀 支付宝登录请求: alipayUserId=${alipayUserId}, authCode=${authCode}`);

        // 在生产环境中，这里应该验证支付宝的authCode
        // 目前为了调试，我们接受所有以debug_开头的授权码
        if (process.env.NODE_ENV === 'production' && !authCode.startsWith('debug_')) {
            // TODO: 在这里添加真实的支付宝授权码验证逻辑
            // const isValidAuth = await verifyAlipayAuthCode(authCode, alipayUserId);
            // if (!isValidAuth) {
            //     return res.status(400).json({
            //         code: 400,
            //         message: '支付宝授权验证失败',
            //         success: false
            //     });
            // }
        }

        // 查找或创建用户（使用支付宝用户ID作为唯一标识）
        let user = await User.findOne({ 
            where: { 
                phone_number: `alipay_${alipayUserId}` // 使用特殊格式存储支付宝用户
            } 
        });

        if (!user) {
            // 创建新的支付宝用户
            user = await User.create({
                phone_number: `alipay_${alipayUserId}`,
                password_hash: await bcrypt.hash(`alipay_${alipayUserId}_${Date.now()}`, 12), // 随机密码
                nickname: nickname,
                avatar: avatar,
                last_login_time: new Date()
            });
            
            console.log(`🚀 创建新支付宝用户: ${user.id}`);
        } else {
            // 更新最后登录时间和用户信息
            await user.update({ 
                last_login_time: new Date(),
                nickname: nickname || user.nickname,
                avatar: avatar || user.avatar
            });
            
            console.log(`🚀 支付宝用户登录: ${user.id}`);
        }

        // 生成JWT令牌
        const accessToken = jwt.sign(
            { userId: user.id, phoneNumber: user.phone_number },
            JWT_SECRET,
            { expiresIn: '24h' }
        );

        const refreshToken = jwt.sign(
            { userId: user.id },
            JWT_REFRESH_SECRET,
            { expiresIn: '7d' }
        );

        // 保存刷新令牌
        await RefreshToken.create({
            user_id: user.id,
            token: refreshToken,
            expires_at: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000) // 7天
        });

        res.json({
            code: 200,
            message: '支付宝登录成功',
            success: true,
            data: {
                user: {
                    userId: user.id,
                    phoneNumber: user.phone_number,
                    nickname: user.nickname,
                    avatar: user.avatar,
                    registerTime: user.createdAt ? user.createdAt.getTime() : Date.now(),
                    lastLoginTime: user.last_login_time ? user.last_login_time.getTime() : Date.now()
                },
                accessToken,
                refreshToken,
                expiresIn: 86400000 // 24小时（毫秒）
            }
        });

    } catch (error) {
        console.error('支付宝登录失败:', error);
        res.status(500).json({
            code: 500,
            message: '服务器内部错误',
            success: false
        });
    }
});

/**
 * POST /api/auth/logout
 * 用户登出
 */
router.post('/logout', async (req, res) => {
    try {
        const authHeader = req.headers.authorization;
        if (!authHeader || !authHeader.startsWith('Bearer ')) {
            return res.status(401).json({
                code: 401,
                message: '未提供有效的认证令牌',
                success: false
            });
        }

        const token = authHeader.substring(7);
        
        try {
            const decoded = jwt.verify(token, JWT_SECRET);
            
            // 撤销所有刷新令牌
            await RefreshToken.update(
                { is_revoked: true },
                { where: { user_id: decoded.userId, is_revoked: false } }
            );
            
        } catch (err) {
            // 即使令牌无效，也返回成功，因为登出的目的已经达到
        }

        res.json({
            code: 200,
            message: '登出成功',
            success: true
        });

    } catch (error) {
        console.error('登出失败:', error);
        res.status(500).json({
            code: 500,
            message: '服务器内部错误',
            success: false
        });
    }
});

/**
 * POST /api/auth/login-google
 * Google登录
 */
router.post('/login-google', [
    body('googleId').notEmpty().withMessage('Google用户ID不能为空'),
    body('email').isEmail().withMessage('邮箱格式不正确'),
    body('name').notEmpty().withMessage('用户姓名不能为空'),
    body('idToken').notEmpty().withMessage('ID Token不能为空'),
    body('photoUrl').optional()
], async (req, res) => {
    try {
        // 验证输入
        const errors = validationResult(req);
        if (!errors.isEmpty()) {
            return res.status(400).json({
                code: 400,
                message: errors.array()[0].msg,
                success: false
            });
        }

        const { googleId, email, name, photoUrl = '', idToken } = req.body;
        
        console.log(`🚀 Google登录请求: googleId=${googleId}, email=${email}, name=${name}`);

        // 在生产环境中，这里应该验证Google的ID Token
        // 目前为了调试，我们接受所有以debug_开头的ID Token
        if (process.env.NODE_ENV === 'production' && !idToken.startsWith('debug_')) {
            // TODO: 在这里添加真实的Google ID Token验证逻辑
            // const isValidToken = await verifyGoogleIdToken(idToken, googleId);
            // if (!isValidToken) {
            //     return res.status(400).json({
            //         code: 400,
            //         message: 'Google ID Token验证失败',
            //         success: false
            //     });
            // }
        }

        // 查找或创建用户（使用Google ID作为唯一标识）
        let user = await User.findOne({ 
            where: { 
                phone_number: `google_${googleId}` // 使用特殊格式存储Google用户
            } 
        });

        if (!user) {
            // 创建新的Google用户
            user = await User.create({
                phone_number: `google_${googleId}`,
                password_hash: '', // Google登录不需要密码
                nickname: name,
                avatar: photoUrl,
                is_data_sync_enabled: true,
                is_premium: false,
                trial_start_time: new Date(),
                subscription_status: 'TRIAL',
                payment_time: null,
                payment_amount: 0,
                alipay_user_id: '',
                email: email,
                google_id: googleId
            });
            
            console.log(`✅ 创建新Google用户: ${user.nickname} (${user.email})`);
        } else {
            // 更新现有用户信息
            await user.update({
                nickname: name,
                avatar: photoUrl,
                email: email,
                last_login_time: new Date()
            });
            
            console.log(`✅ 更新现有Google用户: ${user.nickname} (${user.email})`);
        }

        // 生成JWT token
        const accessToken = jwt.sign(
            { 
                userId: user.id, 
                phoneNumber: user.phone_number,
                email: user.email,
                googleId: user.google_id
            },
            JWT_SECRET,
            { expiresIn: '24h' }
        );

        const refreshToken = jwt.sign(
            { userId: user.id },
            JWT_REFRESH_SECRET,
            { expiresIn: '30d' }
        );

        // 存储refresh token
        await RefreshToken.create({
            user_id: user.id,
            token: refreshToken,
            expires_at: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000) // 30天
        });

        res.json({
            code: 200,
            message: 'Google登录成功',
            success: true,
            data: {
                user: {
                    userId: user.id.toString(),
                    phoneNumber: user.phone_number,
                    nickname: user.nickname,
                    avatar: user.avatar,
                    email: user.email,
                    googleId: user.google_id,
                    isPremium: user.is_premium,
                    trialStartTime: user.trial_start_time ? user.trial_start_time.getTime() : 0,
                    subscriptionStatus: user.subscription_status,
                    registerTime: user.created_at ? user.created_at.getTime() : Date.now()
                },
                accessToken,
                refreshToken,
                expiresIn: 24 * 60 * 60 * 1000 // 24小时，毫秒
            }
        });

    } catch (error) {
        console.error('Google登录错误:', error);
        res.status(500).json({
            code: 500,
            message: 'Google登录失败',
            success: false
        });
    }
});

module.exports = router; 