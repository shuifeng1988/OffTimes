const express = require('express');
const jwt = require('jsonwebtoken');
const { User } = require('../config/database');

const router = express.Router();

const JWT_SECRET = process.env.JWT_SECRET || 'your-super-secret-jwt-key';

// 认证中间件
const authenticateToken = async (req, res, next) => {
    const authHeader = req.headers.authorization;
    const token = authHeader && authHeader.split(' ')[1];

    if (!token) {
        return res.status(401).json({
            code: 401,
            message: '未提供认证令牌',
            success: false
        });
    }

    try {
        const decoded = jwt.verify(token, JWT_SECRET);
        const user = await User.findByPk(decoded.userId);
        
        if (!user) {
            return res.status(401).json({
                code: 401,
                message: '用户不存在',
                success: false
            });
        }

        req.user = user;
        next();
    } catch (error) {
        return res.status(403).json({
            code: 403,
            message: '认证令牌无效',
            success: false
        });
    }
};

/**
 * GET /api/user/profile
 * 获取用户信息
 */
router.get('/profile', authenticateToken, async (req, res) => {
    try {
        const user = req.user;
        
        res.json({
            code: 200,
            message: '获取用户信息成功',
            success: true,
            data: {
                userId: user.id,
                phoneNumber: user.phone_number,
                nickname: user.nickname,
                avatar: user.avatar,
                registerTime: user.createdAt ? user.createdAt.getTime() : Date.now(),
                lastLoginTime: user.last_login_time ? user.last_login_time.getTime() : Date.now()
            }
        });

    } catch (error) {
        console.error('获取用户信息失败:', error);
        res.status(500).json({
            code: 500,
            message: '服务器内部错误',
            success: false
        });
    }
});

/**
 * PUT /api/user/profile
 * 更新用户信息
 */
router.put('/profile', authenticateToken, async (req, res) => {
    try {
        const { nickname, avatar } = req.body;
        const user = req.user;

        // 验证输入
        if (nickname && nickname.length > 50) {
            return res.status(400).json({
                code: 400,
                message: '昵称长度不能超过50字符',
                success: false
            });
        }

        // 更新用户信息
        await user.update({
            nickname: nickname || user.nickname,
            avatar: avatar || user.avatar
        });

        res.json({
            code: 200,
            message: '更新用户信息成功',
            success: true,
            data: {
                userId: user.id,
                phoneNumber: user.phone_number,
                nickname: user.nickname,
                avatar: user.avatar,
                registerTime: user.createdAt ? user.createdAt.getTime() : Date.now(),
                lastLoginTime: user.last_login_time ? user.last_login_time.getTime() : Date.now()
            }
        });

    } catch (error) {
        console.error('更新用户信息失败:', error);
        res.status(500).json({
            code: 500,
            message: '服务器内部错误',
            success: false
        });
    }
});

module.exports = router; 