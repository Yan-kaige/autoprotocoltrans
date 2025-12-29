# Docker Hub 镜像推送指南

## 📦 Docker Hub 简介

Docker Hub 是 Docker 官方提供的公共镜像仓库，可以免费使用。推送到 Docker Hub 后，其他人也可以通过 `docker pull` 命令下载您的镜像。

## 🚀 快速开始

### 1. 注册 Docker Hub 账号

1. 访问 [Docker Hub](https://hub.docker.com/)
2. 点击 "Sign Up" 注册账号
3. 验证邮箱

### 2. 登录 Docker Hub

```bash
# 在命令行登录
docker login

# 输入您的 Docker Hub 用户名和密码
# Username: your-username
# Password: your-password
```

### 3. 构建镜像并打标签

```bash
# 构建镜像
docker build -t autoprotocoltrans:latest .

# 为镜像打标签（格式：username/repository:tag）
# 替换 your-username 为您的 Docker Hub 用户名
docker tag autoprotocoltrans:latest your-username/autoprotocoltrans:latest

# 也可以打多个标签（例如版本号）
docker tag autoprotocoltrans:latest your-username/autoprotocoltrans:v1.0.0
```

### 4. 推送镜像

```bash
# 推送 latest 标签
docker push your-username/autoprotocoltrans:latest

# 推送版本标签
docker push your-username/autoprotocoltrans:v1.0.0
```

### 5. 验证推送

访问 `https://hub.docker.com/r/your-username/autoprotocoltrans` 查看您的镜像。

## 📝 完整示例

假设您的 Docker Hub 用户名是 `john`：

```bash
# 1. 登录
docker login

# 2. 构建镜像
docker build -t autoprotocoltrans:latest .

# 3. 打标签
docker tag autoprotocoltrans:latest john/autoprotocoltrans:latest
docker tag autoprotocoltrans:latest john/autoprotocoltrans:v1.0.0

# 4. 推送
docker push john/autoprotocoltrans:latest
docker push john/autoprotocoltrans:v1.0.0
```

## 🔧 在 Kubernetes 中使用

推送到 Docker Hub 后，在 Kubernetes 配置中使用：

编辑 `k8s/app-deployment.yaml`：

```yaml
spec:
  template:
    spec:
      containers:
      - name: app
        image: your-username/autoprotocoltrans:latest  # 修改为您的镜像
        imagePullPolicy: Always  # 或 IfNotPresent
```

## ⚠️ 注意事项

### 1. 镜像命名规则

- **公共仓库**：格式为 `username/repository:tag`
- **官方仓库**：仅限 Docker 官方维护的镜像
- **组织仓库**：如果属于某个组织，格式为 `organization/repository:tag`

### 2. 免费账号限制

- **私有仓库**：免费账号只能创建 1 个私有仓库
- **公共仓库**：无数量限制
- **拉取限制**：免费账号有拉取速率限制（匿名用户：100 pulls/6小时，认证用户：200 pulls/6小时）

### 3. 镜像大小

- 建议镜像大小控制在合理范围内（几百 MB 到几 GB）
- 过大的镜像会影响推送和拉取速度

### 4. 安全性

- **敏感信息**：不要在镜像中包含密码、密钥等敏感信息
- **公共仓库**：推送到 Docker Hub 的镜像默认是**公共的**，任何人都可以拉取
- **私有仓库**：如果需要私有仓库，考虑升级到付费计划或使用其他私有仓库

## 🔒 使用私有仓库（可选）

如果不想公开镜像，可以：

### 方式1：使用 Docker Hub 私有仓库（需付费或免费1个）

```bash
# 创建私有仓库后，推送方式相同
docker tag autoprotocoltrans:latest your-username/autoprotocoltrans:latest
docker push your-username/autoprotocoltrans:latest
```

### 方式2：使用其他私有仓库

- **阿里云容器镜像服务**（国内推荐）
- **腾讯云容器镜像服务**
- **Harbor**（自建）
- **GitHub Container Registry (ghcr.io)**
- **AWS ECR**
- **Google Container Registry**

## 📚 最佳实践

### 1. 使用版本标签

```bash
# 同时推送多个标签
docker tag autoprotocoltrans:latest your-username/autoprotocoltrans:v1.0.0
docker tag autoprotocoltrans:latest your-username/autoprotocoltrans:latest
docker push your-username/autoprotocoltrans:v1.0.0
docker push your-username/autoprotocoltrans:latest
```

### 2. 使用语义化版本

- `v1.0.0` - 主版本.次版本.修订版本
- `latest` - 最新版本
- `v1.0.0-beta` - 预发布版本

### 3. 添加镜像描述

在 Docker Hub 网页上为您的镜像添加描述、使用说明等。

### 4. 使用多阶段构建（已实现）

Dockerfile 已经使用了多阶段构建，可以有效减小最终镜像大小。

## 🔄 自动化推送

可以集成到 CI/CD 流程中，例如 GitHub Actions：

```yaml
name: Build and Push Docker Image

on:
  push:
    tags:
      - 'v*'

jobs:
  build-and-push:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Login to Docker Hub
        uses: docker/login-action@v2
        with:
          username: ${{ secrets.DOCKER_USERNAME }}
          password: ${{ secrets.DOCKER_PASSWORD }}
      
      - name: Build and push
        uses: docker/build-push-action@v4
        with:
          context: .
          push: true
          tags: |
            your-username/autoprotocoltrans:latest
            your-username/autoprotocoltrans:${{ github.ref_name }}
```

## 🐛 常见问题

### 1. 推送失败：unauthorized

- 检查是否已登录：`docker login`
- 检查用户名和密码是否正确

### 2. 推送失败：denied: requested access to the resource is denied

- 检查镜像标签是否正确（格式：username/repository:tag）
- 确保有权限推送到该仓库

### 3. 推送速度慢

- Docker Hub 在国内访问可能较慢
- 考虑使用国内镜像加速器或使用国内镜像仓库

### 4. 镜像太大

- 使用多阶段构建（已实现）
- 使用 Alpine Linux 基础镜像（已使用）
- 清理不必要的文件和缓存

## 📖 相关链接

- [Docker Hub 官网](https://hub.docker.com/)
- [Docker Hub 文档](https://docs.docker.com/docker-hub/)
- [Docker 推送命令文档](https://docs.docker.com/engine/reference/commandline/push/)







