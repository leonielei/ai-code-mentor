# GitHub 上传指南

## 方法 1: 使用 GitHub CLI (推荐)

如果你已经安装了 GitHub CLI，运行以下命令：

```bash
# 创建 GitHub 仓库并推送
gh repo create ai-code-mentor --public --source=. --remote=origin --push
```

## 方法 2: 手动创建仓库

### 步骤 1: 在 GitHub 上创建新仓库

1. 访问 https://github.com/new
2. 填写仓库信息：
   - **Repository name**: `ai-code-mentor` (或你喜欢的名字)
   - **Description**: `AI-powered programming learning platform with Java 25 support`
   - **Visibility**: 选择 Public 或 Private
   - **不要**勾选 "Initialize this repository with a README"（我们已经有了）
3. 点击 "Create repository"

### 步骤 2: 连接本地仓库到 GitHub

在项目目录下运行以下命令（将 `YOUR_USERNAME` 替换为你的 GitHub 用户名）：

```bash
# 添加远程仓库
git remote add origin https://github.com/YOUR_USERNAME/ai-code-mentor.git

# 或者使用 SSH（如果你配置了 SSH key）
# git remote add origin git@github.com:YOUR_USERNAME/ai-code-mentor.git

# 推送代码到 GitHub
git branch -M main
git push -u origin main
```

### 步骤 3: 验证

访问你的 GitHub 仓库页面，应该能看到所有文件已经上传。

## 如果遇到问题

### 如果提示需要认证：

1. **使用 Personal Access Token**:
   - 访问 https://github.com/settings/tokens
   - 生成新的 token (classic)
   - 在推送时使用 token 作为密码

2. **或者配置 SSH key**:
   ```bash
   # 生成 SSH key（如果还没有）
   ssh-keygen -t ed25519 -C "your_email@example.com"
   
   # 将公钥添加到 GitHub
   # 复制 ~/.ssh/id_ed25519.pub 的内容
   # 在 GitHub Settings > SSH and GPG keys 中添加
   ```

### 如果推送被拒绝：

```bash
# 先拉取远程更改（如果有）
git pull origin main --allow-unrelated-histories

# 然后再推送
git push -u origin main
```

