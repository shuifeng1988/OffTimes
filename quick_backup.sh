#!/bin/bash
# OffTimes 快速备份脚本

echo "🔄 快速备份当前项目状态..."
echo "时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo

# 检查是否有修改
if [ -z "$(git status --porcelain)" ]; then
    echo "📝 没有需要备份的修改"
    exit 0
fi

# 显示将要备份的文件
echo "�� 将要备份的文件:"
git status --short

echo
read -p "确认备份这些修改吗？(y/N): " -n 1 -r
echo

if [[ $REPLY =~ ^[Yy]$ ]]; then
    # 添加所有修改
    git add .
    
    # 创建提交
    git commit -m "backup: $(date '+%Y-%m-%d %H:%M:%S') 自动备份"
    
    echo "✅ 备份完成！"
    echo "📊 当前历史记录:"
    git log --oneline -3
else
    echo "❌ 备份已取消"
fi
