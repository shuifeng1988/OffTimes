# Git 使用指南 - OffTimes项目
# Git Usage Guide - OffTimes Project

## 📚 目录 / Table of Contents

1. [基础概念](#基础概念--basic-concepts)
2. [日常备份操作](#日常备份操作--daily-backup-operations)
3. [恢复操作](#恢复操作--restore-operations)
4. [高级操作](#高级操作--advanced-operations)
5. [最佳实践](#最佳实践--best-practices)
6. [常见问题](#常见问题--common-issues)

## 基础概念 / Basic Concepts

### Git的三个区域 / Three Areas in Git
- **工作区 (Working Directory)**: 你正在编辑的文件
- **暂存区 (Staging Area)**: 准备提交的文件
- **仓库 (Repository)**: 已提交的历史记录

### 基本命令 / Basic Commands
```bash
git status          # 查看状态
git add <file>       # 添加文件到暂存区
git add .           # 添加所有文件到暂存区
git commit -m "msg" # 提交更改
git log             # 查看历史记录
```

## 日常备份操作 / Daily Backup Operations

### 1. 检查项目状态 / Check Project Status
```bash
# 查看当前状态
git status

# 查看具体修改内容
git diff
```

### 2. 添加文件到暂存区 / Add Files to Staging
```bash
# 添加单个文件
git add README.md

# 添加所有修改的文件
git add .

# 添加特定类型的文件
git add *.kt        # 所有Kotlin文件
git add app/src/    # 特定目录
```

### 3. 提交更改 / Commit Changes
```bash
# 基本提交
git commit -m "修复登录问题"

# 详细提交信息
git commit -m "feat: 添加Google登录功能

- 实现GoogleLoginManager
- 添加OAuth配置
- 更新UI界面
- 修复相关bug

版本: 1.1.1"
```

### 4. 查看历史 / View History
```bash
# 简洁历史
git log --oneline

# 详细历史
git log --stat

# 图形化历史
git log --graph --oneline
```

## 恢复操作 / Restore Operations

### 1. 撤销工作区修改 / Undo Working Directory Changes
```bash
# 恢复单个文件
git restore README.md

# 恢复所有文件
git restore .

# 恢复特定目录
git restore app/src/
```

### 2. 撤销暂存区修改 / Undo Staging Area Changes
```bash
# 从暂存区移除文件（保留工作区修改）
git restore --staged README.md

# 移除所有暂存的文件
git restore --staged .
```

### 3. 恢复到历史版本 / Restore to Historical Version
```bash
# 查看历史提交
git log --oneline

# 恢复到特定提交（危险操作！）
git reset --hard <commit-hash>

# 示例：恢复到初始提交
git reset --hard 9144f63
```

### 4. 创建恢复点 / Create Restore Points
```bash
# 创建标签作为重要版本标记
git tag v1.1.0
git tag v1.1.0 -m "稳定版本 - 项目清理完成"

# 查看所有标签
git tag

# 恢复到标签版本
git checkout v1.1.0
```

## 高级操作 / Advanced Operations

### 1. 分支管理 / Branch Management
```bash
# 创建新分支
git branch feature-google-login

# 切换分支
git checkout feature-google-login

# 创建并切换分支
git checkout -b feature-payment

# 查看所有分支
git branch

# 合并分支
git checkout master
git merge feature-google-login

# 删除分支
git branch -d feature-google-login
```

### 2. 远程仓库 / Remote Repository
```bash
# 添加远程仓库
git remote add origin https://github.com/username/offtimes.git

# 推送到远程仓库
git push origin master

# 从远程仓库拉取
git pull origin master

# 查看远程仓库
git remote -v
```

### 3. 文件忽略 / File Ignoring
编辑 `.gitignore` 文件：
```gitignore
# 构建文件
/app/build/
/build/
*.apk
*.aab

# 敏感文件
gradle.properties
*.jks
*.keystore

# IDE文件
.idea/
*.iml

# 系统文件
.DS_Store
Thumbs.db
```

### 4. 查看差异 / View Differences
```bash
# 查看工作区与暂存区差异
git diff

# 查看暂存区与最后提交差异
git diff --staged

# 查看两个提交之间差异
git diff HEAD~1 HEAD

# 查看特定文件差异
git diff HEAD~1 README.md
```

## 最佳实践 / Best Practices

### 1. 提交信息规范 / Commit Message Convention
```bash
# 格式：<类型>: <描述>
feat: 添加新功能
fix: 修复bug
docs: 更新文档
style: 代码格式调整
refactor: 代码重构
test: 添加测试
chore: 构建工具或辅助工具的变动

# 示例
git commit -m "feat: 实现Google登录功能"
git commit -m "fix: 修复应用崩溃问题"
git commit -m "docs: 更新README文档"
```

### 2. 定期备份策略 / Regular Backup Strategy
```bash
# 每日备份
git add .
git commit -m "daily: $(date '+%Y-%m-%d') 日常开发备份"

# 功能完成备份
git commit -m "feat: 完成用户登录模块"

# 版本发布备份
git commit -m "release: v1.1.0 正式版本发布"
git tag v1.1.0
```

### 3. 安全操作 / Safe Operations
```bash
# 在危险操作前创建备份分支
git branch backup-$(date '+%Y%m%d')

# 使用软重置而不是硬重置
git reset --soft HEAD~1  # 保留文件修改
git reset --hard HEAD~1  # 删除所有修改（危险！）

# 检查状态后再操作
git status
git diff
# 确认无误后再执行操作
```

## 常见问题 / Common Issues

### 1. 如何撤销最后一次提交？ / How to Undo Last Commit?
```bash
# 撤销提交但保留修改
git reset --soft HEAD~1

# 撤销提交和修改（危险！）
git reset --hard HEAD~1

# 修改最后一次提交信息
git commit --amend -m "新的提交信息"
```

### 2. 如何恢复删除的文件？ / How to Restore Deleted Files?
```bash
# 如果文件还没有提交删除
git restore <deleted-file>

# 如果已经提交了删除
git checkout HEAD~1 -- <deleted-file>
```

### 3. 如何查看某个文件的修改历史？ / How to View File History?
```bash
# 查看文件修改历史
git log --follow -- <filename>

# 查看文件每行的修改者
git blame <filename>

# 查看文件在特定提交的内容
git show <commit-hash>:<filename>
```

### 4. 如何处理合并冲突？ / How to Handle Merge Conflicts?
```bash
# 查看冲突文件
git status

# 手动编辑冲突文件，然后
git add <resolved-file>
git commit -m "resolve: 解决合并冲突"

# 或者取消合并
git merge --abort
```

## 实用脚本 / Useful Scripts

### 1. 快速备份脚本 / Quick Backup Script
```bash
#!/bin/bash
# quick_backup.sh
echo "🔄 快速备份当前项目状态..."
git add .
git commit -m "backup: $(date '+%Y-%m-%d %H:%M:%S') 自动备份"
echo "✅ 备份完成！"
```

### 2. 项目状态检查脚本 / Project Status Check Script
```bash
#!/bin/bash
# check_status.sh
echo "📊 项目状态检查："
echo "===================="
echo "🔍 Git状态："
git status --short
echo ""
echo "📝 最近提交："
git log --oneline -5
echo ""
echo "📁 工作区文件数量："
find . -name "*.kt" -o -name "*.xml" -o -name "*.md" | wc -l
```

### 3. 版本发布脚本 / Release Script
```bash
#!/bin/bash
# release.sh
VERSION=$1
if [ -z "$VERSION" ]; then
    echo "请提供版本号: ./release.sh v1.1.0"
    exit 1
fi

echo "🚀 准备发布版本 $VERSION..."
git add .
git commit -m "release: $VERSION 版本发布"
git tag $VERSION -m "Release $VERSION"
echo "✅ 版本 $VERSION 发布完成！"
```

## 总结 / Summary

Git是一个强大的版本控制工具，掌握以下核心概念：

1. **定期提交** - 养成频繁提交的习惯
2. **清晰的提交信息** - 使用规范的提交信息格式
3. **安全操作** - 在危险操作前创建备份
4. **合理使用分支** - 为不同功能创建独立分支
5. **及时备份** - 定期推送到远程仓库

记住：**Git不仅是备份工具，更是开发历史的记录者！**

---

**创建日期**: 2025年9月15日
**适用项目**: OffTimes v1.1.0+
**维护者**: OffTimes Developer Team
