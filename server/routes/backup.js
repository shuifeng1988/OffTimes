const express = require('express');
const crypto = require('crypto');
const jwt = require('jsonwebtoken');
const { UserDataBackup, UserBackupSettings, User } = require('../config/database');
const router = express.Router();

// JWT验证中间件
const authenticateToken = (req, res, next) => {
    const authHeader = req.headers['authorization'];
    const token = authHeader && authHeader.split(' ')[1];

    if (!token) {
        return res.status(401).json({
            code: 401,
            message: '访问令牌缺失'
        });
    }

    jwt.verify(token, process.env.JWT_SECRET || 'your-secret-key', (err, user) => {
        if (err) {
            return res.status(403).json({
                code: 403,
                message: '访问令牌无效'
            });
        }
        req.user = user;
        next();
    });
};

// 计算数据哈希
const calculateDataHash = (data) => {
    return crypto.createHash('sha256').update(JSON.stringify(data)).digest('hex');
};

// 获取今天的日期字符串
const getTodayString = () => {
    return new Date().toISOString().split('T')[0];
};

/**
 * POST /api/backup/upload
 * 上传用户数据备份（增量备份）
 * 
 * 请求体：
 * {
 *   "table_name": "app_sessions_users",
 *   "backup_data": [...], // 昨天新增的数据
 *   "backup_date": "2024-01-20" // 备份的数据日期（昨天）
 * }
 * 
 * 或驼峰命名格式：
 * {
 *   "tableName": "app_sessions_users",
 *   "backupData": [...],
 *   "backupDate": "2024-01-20"
 * }
 */
router.post('/upload', authenticateToken, async (req, res) => {
    try {
        // 兼容驼峰命名和下划线命名
        const table_name = req.body.table_name || req.body.tableName;
        const backup_data = req.body.backup_data || req.body.backupData;
        const backup_date = req.body.backup_date || req.body.backupDate;
        
        // 修复JWT token用户ID字段
        const userId = req.user.userId || req.user.id;

        if (!table_name || !backup_data || !backup_date) {
            return res.status(400).json({
                code: 400,
                message: '参数不完整：需要table_name、backup_data和backup_date'
            });
        }

        // 验证表名是否为基础数据表
        const allowedTables = [
            'app_sessions_users',
            'timer_sessions_users', 
            'AppCategory_Users',
            'goals_reward_punishment_users'
        ];

        if (!allowedTables.includes(table_name)) {
            return res.status(400).json({
                code: 400,
                message: `不支持的表名：${table_name}`
            });
        }

        // 检查是否为数组且有数据
        if (!Array.isArray(backup_data)) {
            return res.status(400).json({
                code: 400,
                message: 'backup_data必须是数组格式'
            });
        }

        // 如果数据为空，不进行备份
        if (backup_data.length === 0) {
            return res.json({
                code: 200,
                message: '无新增数据，跳过备份',
                data: {
                    table_name,
                    backup_date,
                    record_count: 0,
                    skipped: true
                }
            });
        }

        // 计算数据哈希和大小
        const dataHash = calculateDataHash(backup_data);
        const jsonString = JSON.stringify(backup_data);
        const fileSize = Buffer.byteLength(jsonString, 'utf8');

        // 检查是否已存在相同的备份
        const existingBackup = await UserDataBackup.findOne({
            where: {
                user_id: userId,
                backup_date,
                table_name
            }
        });

        if (existingBackup) {
            // 如果数据哈希相同，说明数据没有变化
            if (existingBackup.data_hash === dataHash) {
                return res.json({
                    code: 200,
                    message: '数据未变化，无需更新备份',
                    data: {
                        table_name,
                        backup_date,
                        record_count: existingBackup.record_count,
                        unchanged: true
                    }
                });
            }

            // 数据有变化，更新备份
            await existingBackup.update({
                backup_data: jsonString,
                data_hash: dataHash,
                record_count: backup_data.length,
                file_size: fileSize
            });

            console.log(`用户 ${userId} 更新了表 ${table_name} 在 ${backup_date} 的备份`);
        } else {
            // 创建新备份
            await UserDataBackup.create({
                user_id: userId,
                backup_date,
                table_name,
                backup_data: jsonString,
                data_hash: dataHash,
                record_count: backup_data.length,
                file_size: fileSize
            });

            console.log(`用户 ${userId} 创建了表 ${table_name} 在 ${backup_date} 的新备份`);
        }

        // 更新用户备份设置
        const today = getTodayString();
        await UserBackupSettings.upsert({
            user_id: userId,
            last_backup_date: today,
            total_backups: await UserDataBackup.count({ where: { user_id: userId } }),
            total_data_size: await UserDataBackup.sum('file_size', { where: { user_id: userId } }) || 0
        });

        res.json({
            code: 200,
            message: '数据备份成功',
            data: {
                table_name,
                backup_date,
                record_count: backup_data.length,
                file_size: fileSize,
                data_hash: dataHash
            }
        });

    } catch (error) {
        console.error('备份上传失败:', error);
        res.status(500).json({
            code: 500,
            message: '备份上传失败',
            error: process.env.NODE_ENV === 'development' ? error.message : '服务器内部错误'
        });
    }
});

/**
 * GET /api/backup/download
 * 下载用户数据备份
 * 
 * 查询参数：
 * - table_name: 表名（可选，不指定则返回所有表）
 * - date_from: 开始日期（可选）
 * - date_to: 结束日期（可选）
 */
router.get('/download', authenticateToken, async (req, res) => {
    try {
        const { table_name, date_from, date_to } = req.query;
        // 修复JWT token用户ID字段
        const userId = req.user.userId || req.user.id;

        // 构建查询条件
        const whereCondition = { user_id: userId };
        
        if (table_name) {
            whereCondition.table_name = table_name;
        }

        if (date_from && date_to) {
            whereCondition.backup_date = {
                [require('sequelize').Op.between]: [date_from, date_to]
            };
        } else if (date_from) {
            whereCondition.backup_date = {
                [require('sequelize').Op.gte]: date_from
            };
        } else if (date_to) {
            whereCondition.backup_date = {
                [require('sequelize').Op.lte]: date_to
            };
        }

        const backups = await UserDataBackup.findAll({
            where: whereCondition,
            order: [['backup_date', 'DESC'], ['table_name', 'ASC']],
            attributes: ['table_name', 'backup_date', 'backup_data', 'record_count', 'data_hash']
        });

        if (backups.length === 0) {
            return res.json({
                code: 200,
                message: '未找到符合条件的备份数据',
                data: {
                    backups: [],
                    total_count: 0
                }
            });
        }

        // 处理备份数据
        const processedBackups = backups.map(backup => ({
            table_name: backup.table_name,
            backup_date: backup.backup_date,
            backup_data: JSON.parse(backup.backup_data),
            record_count: backup.record_count,
            data_hash: backup.data_hash
        }));

        console.log(`用户 ${userId} 下载了 ${backups.length} 个备份文件`);

        res.json({
            code: 200,
            message: '备份数据获取成功',
            data: {
                backups: processedBackups,
                total_count: backups.length,
                query_params: { table_name, date_from, date_to }
            }
        });

    } catch (error) {
        console.error('备份下载失败:', error);
        res.status(500).json({
            code: 500,
            message: '备份下载失败',
            error: process.env.NODE_ENV === 'development' ? error.message : '服务器内部错误'
        });
    }
});

/**
 * GET /api/backup/info
 * 获取用户备份信息概览
 */
router.get('/info', authenticateToken, async (req, res) => {
    try {
        // 修复JWT token用户ID字段
        const userId = req.user.userId || req.user.id;

        // 获取备份设置
        const backupSettings = await UserBackupSettings.findOne({
            where: { user_id: userId }
        });

        // 如果没有设置，创建默认设置
        if (!backupSettings) {
            await UserBackupSettings.create({
                user_id: userId,
                backup_enabled: true,
                backup_time_hour: Math.floor(Math.random() * 6) + 1, // 随机1-6点
                backup_time_minute: Math.floor(Math.random() * 60)
            });
        }

        // 重新获取设置
        const settings = await UserBackupSettings.findOne({
            where: { user_id: userId }
        });

        // 获取备份统计
        const backupStats = await UserDataBackup.findAll({
            where: { user_id: userId },
            attributes: [
                'table_name',
                [require('sequelize').fn('COUNT', require('sequelize').col('id')), 'backup_count'],
                [require('sequelize').fn('MAX', require('sequelize').col('backup_date')), 'latest_backup'],
                [require('sequelize').fn('SUM', require('sequelize').col('record_count')), 'total_records'],
                [require('sequelize').fn('SUM', require('sequelize').col('file_size')), 'total_size']
            ],
            group: ['table_name'],
            raw: true
        });

        res.json({
            code: 200,
            message: '备份信息获取成功',
            data: {
                settings: {
                    backup_enabled: settings.backup_enabled,
                    backup_time: `${String(settings.backup_time_hour).padStart(2, '0')}:${String(settings.backup_time_minute).padStart(2, '0')}`,
                    last_backup_date: settings.last_backup_date,
                    total_backups: settings.total_backups,
                    total_data_size: settings.total_data_size
                },
                table_stats: backupStats.map(stat => ({
                    table_name: stat.table_name,
                    backup_count: parseInt(stat.backup_count),
                    latest_backup: stat.latest_backup,
                    total_records: parseInt(stat.total_records || 0),
                    total_size: parseInt(stat.total_size || 0)
                }))
            }
        });

    } catch (error) {
        console.error('获取备份信息失败:', error);
        res.status(500).json({
            code: 500,
            message: '获取备份信息失败',
            error: process.env.NODE_ENV === 'development' ? error.message : '服务器内部错误'
        });
    }
});

/**
 * PUT /api/backup/settings
 * 更新用户备份设置
 * 
 * 请求体：
 * {
 *   "backup_enabled": true,
 *   "backup_time_hour": 2,
 *   "backup_time_minute": 30
 * }
 */
router.put('/settings', authenticateToken, async (req, res) => {
    try {
        const { backup_enabled, backup_time_hour, backup_time_minute } = req.body;
        // 修复JWT token用户ID字段
        const userId = req.user.userId || req.user.id;

        const updateData = {};

        if (typeof backup_enabled === 'boolean') {
            updateData.backup_enabled = backup_enabled;
        }

        if (typeof backup_time_hour === 'number' && backup_time_hour >= 0 && backup_time_hour <= 23) {
            updateData.backup_time_hour = backup_time_hour;
        }

        if (typeof backup_time_minute === 'number' && backup_time_minute >= 0 && backup_time_minute <= 59) {
            updateData.backup_time_minute = backup_time_minute;
        }

        if (Object.keys(updateData).length === 0) {
            return res.status(400).json({
                code: 400,
                message: '无有效的更新参数'
            });
        }

        await UserBackupSettings.upsert({
            user_id: userId,
            ...updateData
        });

        console.log(`用户 ${userId} 更新了备份设置:`, updateData);

        res.json({
            code: 200,
            message: '备份设置更新成功',
            data: updateData
        });

    } catch (error) {
        console.error('更新备份设置失败:', error);
        res.status(500).json({
            code: 500,
            message: '更新备份设置失败',
            error: process.env.NODE_ENV === 'development' ? error.message : '服务器内部错误'
        });
    }
});

/**
 * DELETE /api/backup/clear
 * 清空用户所有备份数据（谨慎操作）
 */
router.delete('/clear', authenticateToken, async (req, res) => {
    try {
        // 修复JWT token用户ID字段
        const userId = req.user.userId || req.user.id;

        const deletedCount = await UserDataBackup.destroy({
            where: { user_id: userId }
        });

        // 重置备份设置统计
        await UserBackupSettings.update({
            total_backups: 0,
            total_data_size: 0,
            last_backup_date: null
        }, {
            where: { user_id: userId }
        });

        console.log(`用户 ${userId} 清空了所有备份数据，共删除 ${deletedCount} 条记录`);

        res.json({
            code: 200,
            message: '备份数据清空成功',
            data: {
                deleted_count: deletedCount
            }
        });

    } catch (error) {
        console.error('清空备份数据失败:', error);
        res.status(500).json({
            code: 500,
            message: '清空备份数据失败',
            error: process.env.NODE_ENV === 'development' ? error.message : '服务器内部错误'
        });
    }
});

module.exports = router; 