# Kubernetes 部署快速指南

## 🚀 快速开始

### 1. 构建并推送镜像

#### 推送到 Docker Hub（推荐）

```bash
# 1. 登录 Docker Hub（如果没有账号，先到 https://hub.docker.com/ 注册）
docker login

# 2. 构建镜像
docker build -t autoprotocoltrans:latest .

# 3. 打标签（替换 your-username 为您的 Docker Hub 用户名）
docker tag autoprotocoltrans:latest your-username/autoprotocoltrans:latest

# 4. 推送到 Docker Hub
docker push your-username/autoprotocoltrans:latest
```

#### 推送到其他镜像仓库

```bash
# 构建镜像
docker build -t your-registry/autoprotocoltrans:latest .

# 推送到镜像仓库
docker push your-registry/autoprotocoltrans:latest
```

**详细说明请参考：[Docker Hub 推送指南](./docs/DOCKER_HUB.md)**

### 2. 修改镜像地址

编辑 `k8s/app-deployment.yaml`，将 `image: autoprotocoltrans:latest` 改为您的镜像地址。

如果使用 Docker Hub：`your-username/autoprotocoltrans:latest`

### 3. 部署到 Kubernetes

```bash
# 部署所有资源
kubectl apply -f k8s/

# 或者使用 kustomize
kubectl apply -k k8s/
```

### 4. 查看部署状态

```bash
kubectl get all -n autoprotocoltrans
```

### 5. 访问应用

```bash
# 方式1：端口转发（临时）
kubectl port-forward service/autoprotocoltrans-app 8080:80 -n autoprotocoltrans
# 访问 http://localhost:8080

# 方式2：使用 NodePort
# 使用 k8s/app-service-nodeport.yaml，然后通过 <NodeIP>:30080 访问
```

## 📋 配置文件说明

所有 Kubernetes 配置文件位于 `k8s/` 目录：

- **MySQL 相关**：部署 MySQL 数据库
- **应用相关**：部署 Spring Boot 应用
- **可选配置**：Ingress、NodePort 等

## 🔧 重要配置

### 修改数据库密码

编辑 `k8s/mysql-secret.yaml` 和 `k8s/app-secret.yaml`

### 调整副本数

编辑 `k8s/app-deployment.yaml` 中的 `replicas` 字段

### 使用私有镜像仓库

创建 Secret 并在 `app-deployment.yaml` 中配置 `imagePullSecrets`

## 📖 详细文档

更多详细信息请参考：[k8s/README.md](./k8s/README.md)

