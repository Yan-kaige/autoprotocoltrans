# Kubernetes 部署指南

本文档说明如何在 Kubernetes 集群中部署 autoprotocoltrans 应用。

## 📋 前置要求

- Kubernetes 集群（版本 1.19+）
- kubectl 命令行工具已配置
- 镜像已构建并推送到镜像仓库（或使用本地镜像）

## 🚀 快速部署

### 方式一：使用 kubectl 直接部署

```bash
# 1. 创建命名空间和所有资源
kubectl apply -f k8s/

# 2. 查看部署状态
kubectl get all -n autoprotocoltrans

# 3. 查看 Pod 日志
kubectl logs -f deployment/autoprotocoltrans-app -n autoprotocoltrans
```

### 方式二：使用 kustomize（推荐）

```bash
# 1. 部署所有资源
kubectl apply -k k8s/

# 2. 查看部署状态
kubectl get all -n autoprotocoltrans
```

## 📁 文件说明

### 核心配置文件

- `namespace.yaml` - 创建独立的命名空间
- `mysql-secret.yaml` - MySQL 数据库密码等敏感信息
- `mysql-configmap.yaml` - MySQL 配置
- `mysql-init-scripts-configmap.yaml` - 数据库初始化 SQL 脚本
- `mysql-pvc.yaml` - MySQL 数据持久化存储
- `mysql-deployment.yaml` - MySQL 部署
- `mysql-service.yaml` - MySQL 服务（ClusterIP）
- `app-secret.yaml` - 应用敏感配置（数据库密码等）
- `app-configmap.yaml` - 应用配置
- `app-deployment.yaml` - 应用部署（可配置副本数）
- `app-service.yaml` - 应用服务（ClusterIP）

### 可选配置

- `app-service-nodeport.yaml` - NodePort 类型服务（用于外部直接访问）
- `ingress.yaml` - Ingress 配置（用于域名访问）
- `kustomization.yaml` - Kustomize 配置文件

## 🔧 配置说明

### 1. 修改镜像地址

编辑 `k8s/app-deployment.yaml`：

```yaml
spec:
  template:
    spec:
      containers:
      - name: app
        image: your-registry/autoprotocoltrans:v1.0.0  # 修改为您的镜像地址
        imagePullPolicy: IfNotPresent
```

### 2. 修改数据库密码

编辑 `k8s/mysql-secret.yaml`：

```yaml
stringData:
  root-password: "your_strong_password"  # 修改为强密码
  password: "your_strong_password"
```

同时更新 `k8s/app-secret.yaml`：

```yaml
stringData:
  datasource-password: "your_strong_password"  # 与 MySQL 密码一致
```

### 3. 调整资源限制

编辑 `k8s/app-deployment.yaml`：

```yaml
resources:
  requests:
    memory: "512Mi"
    cpu: "250m"
  limits:
    memory: "1Gi"
    cpu: "1000m"
```

### 4. 调整副本数

编辑 `k8s/app-deployment.yaml`：

```yaml
spec:
  replicas: 3  # 修改为您需要的副本数
```

### 5. 配置存储大小

编辑 `k8s/mysql-pvc.yaml`：

```yaml
spec:
  resources:
    requests:
      storage: 20Gi  # 根据需求调整
```

## 🌐 访问应用

### 方式一：使用 Port Forward（临时访问）

```bash
# 端口转发
kubectl port-forward service/autoprotocoltrans-app 8080:80 -n autoprotocoltrans

# 访问 http://localhost:8080
```

### 方式二：使用 NodePort（外部访问）

1. 使用 NodePort 服务：

```bash
# 删除 ClusterIP 服务，使用 NodePort 服务
kubectl delete -f k8s/app-service.yaml
kubectl apply -f k8s/app-service-nodeport.yaml

# 查看 NodePort
kubectl get svc autoprotocoltrans-app -n autoprotocoltrans

# 通过 <NodeIP>:30080 访问
```

2. 修改 NodePort 端口（可选）：

编辑 `k8s/app-service-nodeport.yaml`：

```yaml
spec:
  ports:
  - nodePort: 30080  # 修改为您需要的端口（30000-32767）
```

### 方式三：使用 Ingress（推荐生产环境）

1. 确保已安装 Ingress Controller（如 Nginx Ingress Controller）

2. 编辑 `k8s/ingress.yaml`，修改域名：

```yaml
spec:
  rules:
  - host: autoprotocoltrans.yourdomain.com  # 修改为您的域名
```

3. 启用 Ingress：

编辑 `k8s/kustomization.yaml`，取消注释：

```yaml
resources:
  # ...
  - ingress.yaml
```

4. 部署：

```bash
kubectl apply -k k8s/
```

5. 配置 DNS 或 hosts 文件指向 Ingress Controller 的 IP

## 🔍 常用命令

### 查看资源状态

```bash
# 查看所有资源
kubectl get all -n autoprotocoltrans

# 查看 Pod 状态
kubectl get pods -n autoprotocoltrans

# 查看服务
kubectl get svc -n autoprotocoltrans

# 查看配置
kubectl get configmap -n autoprotocoltrans
kubectl get secret -n autoprotocoltrans
```

### 查看日志

```bash
# 查看应用日志
kubectl logs -f deployment/autoprotocoltrans-app -n autoprotocoltrans

# 查看 MySQL 日志
kubectl logs -f deployment/mysql -n autoprotocoltrans

# 查看指定 Pod 日志
kubectl logs -f <pod-name> -n autoprotocoltrans
```

### 进入容器

```bash
# 进入应用容器
kubectl exec -it deployment/autoprotocoltrans-app -n autoprotocoltrans -- sh

# 进入 MySQL 容器
kubectl exec -it deployment/mysql -n autoprotocoltrans -- bash
```

### 扩容和缩容

```bash
# 扩容应用副本到 3 个
kubectl scale deployment autoprotocoltrans-app --replicas=3 -n autoprotocoltrans

# 缩容到 1 个
kubectl scale deployment autoprotocoltrans-app --replicas=1 -n autoprotocoltrans
```

### 更新部署

```bash
# 更新镜像
kubectl set image deployment/autoprotocoltrans-app app=your-registry/autoprotocoltrans:v1.1.0 -n autoprotocoltrans

# 查看更新状态
kubectl rollout status deployment/autoprotocoltrans-app -n autoprotocoltrans

# 回滚到上一版本
kubectl rollout undo deployment/autoprotocoltrans-app -n autoprotocoltrans
```

## 🔐 安全建议

### 1. 使用私有镜像仓库

如果使用私有镜像仓库，需要创建 Secret：

```bash
kubectl create secret docker-registry registry-secret \
  --docker-server=your-registry.com \
  --docker-username=your-username \
  --docker-password=your-password \
  --docker-email=your-email@example.com \
  -n autoprotocoltrans
```

然后在 `app-deployment.yaml` 中取消注释：

```yaml
imagePullSecrets:
- name: registry-secret
```

### 2. 使用外部 Secret 管理工具

生产环境建议使用：
- **Sealed Secrets**：加密的 Kubernetes Secrets
- **External Secrets Operator**：从外部 Secret 存储（如 AWS Secrets Manager、HashiCorp Vault）同步

### 3. 使用 RBAC

为应用配置最小权限的 ServiceAccount 和 RoleBinding。

### 4. 网络策略

配置 NetworkPolicy 限制 Pod 之间的网络访问。

## 📊 监控和日志

### 查看资源使用情况

```bash
# 查看 Pod 资源使用
kubectl top pods -n autoprotocoltrans

# 查看节点资源使用
kubectl top nodes
```

### 集成监控系统

可以集成以下监控系统：
- **Prometheus + Grafana**：监控和可视化
- **ELK Stack**：日志收集和分析
- **Jaeger**：分布式追踪

## 🗑️ 删除部署

```bash
# 删除所有资源
kubectl delete -f k8s/

# 或使用 kustomize
kubectl delete -k k8s/

# 删除命名空间（会删除命名空间内的所有资源）
kubectl delete namespace autoprotocoltrans
```

**注意**：删除命名空间会删除所有数据，包括数据库 PVC。如果需要保留数据，先备份 PVC。

## 🐛 故障排查

### Pod 无法启动

```bash
# 查看 Pod 详细信息
kubectl describe pod <pod-name> -n autoprotocoltrans

# 查看事件
kubectl get events -n autoprotocoltrans --sort-by='.lastTimestamp'
```

### 数据库连接失败

1. 检查 MySQL Service 是否正常运行
2. 检查 ConfigMap 中的数据库连接 URL
3. 检查 Secret 中的用户名和密码是否正确
4. 查看应用日志：`kubectl logs -f deployment/autoprotocoltrans-app -n autoprotocoltrans`

### 应用无法访问

1. 检查 Service 是否正确创建：`kubectl get svc -n autoprotocoltrans`
2. 检查 Endpoints：`kubectl get endpoints -n autoprotocoltrans`
3. 检查 Pod 健康检查是否通过
4. 使用 port-forward 测试：`kubectl port-forward service/autoprotocoltrans-app 8080:80 -n autoprotocoltrans`

## 📝 最佳实践

1. **使用 ConfigMap 和 Secret 分离配置和敏感信息**
2. **配置资源限制防止资源耗尽**
3. **使用健康检查和就绪探针确保服务可用性**
4. **配置多个副本实现高可用**
5. **使用持久化存储保存数据库数据**
6. **定期备份数据库 PVC**
7. **使用命名空间隔离不同环境**
8. **配置资源配额限制命名空间资源使用**

## 🔄 持续集成/持续部署 (CI/CD)

可以集成到 CI/CD 流程：

```yaml
# GitHub Actions 示例
- name: Deploy to Kubernetes
  run: |
    kubectl set image deployment/autoprotocoltrans-app \
      app=${{ secrets.REGISTRY }}/autoprotocoltrans:${{ github.sha }} \
      -n autoprotocoltrans
```

## 📚 相关文档

- [Kubernetes 官方文档](https://kubernetes.io/docs/)
- [Kustomize 文档](https://kustomize.io/)
- [Spring Boot on Kubernetes](https://spring.io/guides/gs/spring-boot-kubernetes/)


