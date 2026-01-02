# Docker 容器化部署指南

## 📦 项目容器化说明

本项目已经完全支持 Docker 容器化部署，使用多阶段构建（Multi-stage Build）技术，将前端和后端打包到一个镜像中。

## 🚀 快速开始

### 前置要求

- **Docker** 20.10 或更高版本
- **Docker Compose** 2.0 或更高版本

### 一键启动

```bash
# 构建镜像并启动所有服务（包括 MySQL）
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

启动成功后，访问：**http://localhost:8080**

## 📁 文件说明

### Dockerfile

使用**多阶段构建**，包含三个阶段：

1. **前端构建阶段** (`frontend-builder`)
   - 使用 Node.js 18 Alpine 镜像
   - 安装前端依赖
   - 构建前端项目（输出到 `src/main/resources/static`）

2. **后端构建阶段** (`backend-builder`)
   - 使用 Maven 3.9 + Java 17 镜像
   - 下载 Maven 依赖（利用 Docker 缓存）
   - 复制前端构建产物
   - 打包 Spring Boot 应用为 JAR

3. **运行阶段**
   - 使用 Java 17 JRE Alpine 镜像（体积小）
   - 复制 JAR 文件
   - 配置时区为 Asia/Shanghai
   - 启动应用

### docker-compose.yml

定义了两个服务：

1. **mysql**：MySQL 8.0 数据库
   - 端口映射：3307:3306
   - 自动执行 `db/migration/` 目录下的 SQL 脚本
   - 数据持久化到 Docker 卷

2. **app**：Spring Boot 应用
   - 端口映射：8080:8080
   - 依赖 MySQL 服务
   - 通过环境变量配置数据库连接

## 🔧 环境变量配置

### 数据库配置

可以通过环境变量覆盖默认配置：

```yaml
services:
  app:
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/protovol_trans?...
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: your_password
```

### MySQL 配置

```yaml
services:
  mysql:
    environment:
      MYSQL_ROOT_PASSWORD: your_password
      MYSQL_DATABASE: protovol_trans
      MYSQL_USER: appuser
      MYSQL_PASSWORD: apppass
```

## 📋 常用命令

### 启动和停止

```bash
# 启动所有服务（后台运行）
docker-compose up -d

# 启动并查看日志
docker-compose up

# 停止所有服务
docker-compose down

# 停止并删除数据卷（⚠️ 会删除数据库数据）
docker-compose down -v
```

### 查看日志

```bash
# 查看所有服务日志
docker-compose logs -f

# 查看应用日志
docker-compose logs -f app

# 查看数据库日志
docker-compose logs -f mysql
```

### 重新构建

```bash
# 重新构建镜像（不使用缓存）
docker-compose build --no-cache

# 重新构建并启动
docker-compose up -d --build
```

### 进入容器

```bash
# 进入应用容器
docker-compose exec app sh

# 进入数据库容器
docker-compose exec mysql bash
```

### 数据库操作

```bash
# 连接数据库
docker-compose exec mysql mysql -u root -p123456 protovol_trans

# 备份数据库
docker-compose exec mysql mysqldump -u root -p123456 protovol_trans > backup.sql

# 恢复数据库
docker-compose exec -T mysql mysql -u root -p123456 protovol_trans < backup.sql
```

## 🗄️ 数据库初始化

MySQL 容器启动时会自动执行 `src/main/resources/db/migration/` 目录下的 SQL 脚本：

- `V1__create_mapping_config_table.sql` - 映射配置表
- `V2__create_dictionary_table.sql` - 字典表
- `V3__create_dictionary_item_table.sql` - 字典项表
- `V4__create_custom_function_table.sql` - 自定义函数表

**注意**：SQL 文件按字母顺序执行，建议使用 `V1__`, `V2__` 等前缀控制执行顺序。

## 💾 数据持久化

数据库数据存储在 Docker 卷 `autoprotocoltrans_mysql_data` 中，即使删除容器，数据也会保留。

```bash
# 查看数据卷
docker volume ls

# 删除数据卷（⚠️ 会删除所有数据）
docker volume rm autoprotocoltrans_mysql_data
```

## 🏗️ 单独构建镜像

如果只需要构建应用镜像（不使用 docker-compose）：

```bash
# 构建镜像
docker build -t autoprotocoltrans:latest .

# 运行容器（需要外部 MySQL）
docker run -d \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3307/protovol_trans?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=123456 \
  --name autoprotocoltrans-app \
  autoprotocoltrans:latest
```

## 🔒 生产环境建议

### 1. 使用环境变量文件

创建 `.env` 文件（不要提交到 Git）：

```env
MYSQL_ROOT_PASSWORD=your_strong_password_here
MYSQL_DATABASE=protovol_trans
SPRING_DATASOURCE_PASSWORD=your_strong_password_here
```

在 `docker-compose.yml` 中使用：

```yaml
services:
  mysql:
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
  app:
    environment:
      SPRING_DATASOURCE_PASSWORD: ${SPRING_DATASOURCE_PASSWORD}
```

### 2. 资源限制

为容器设置资源限制：

```yaml
services:
  app:
    deploy:
      resources:
        limits:
          cpus: '1.0'
          memory: 1G
        reservations:
          cpus: '0.5'
          memory: 512M
```

### 3. 健康检查

已配置健康检查，确保服务正常启动：

```yaml
healthcheck:
  test: ["CMD", "wget", "--quiet", "--tries=1", "--spider", "http://localhost:8080/api/v2/config/list"]
  interval: 30s
  timeout: 10s
  retries: 3
```

### 4. 网络配置

使用自定义网络，服务间通过服务名通信：

```yaml
networks:
  app-network:
    driver: bridge
```

## 🐛 故障排查

### 应用无法启动

1. **检查日志**：
   ```bash
   docker-compose logs app
   ```

2. **检查数据库连接**：
   - 确认 MySQL 容器已启动：`docker-compose ps mysql`
   - 检查数据库连接字符串是否正确

### 端口冲突

如果 8080 或 3307 端口被占用，修改 `docker-compose.yml`：

```yaml
services:
  app:
    ports:
      - "8081:8080"  # 修改主机端口
  mysql:
    ports:
      - "3308:3306"  # 修改主机端口
```

### 构建失败

1. **网络问题**：确保能够访问 Docker Hub 和 npm  registry
2. **清理缓存**：`docker-compose build --no-cache`
3. **检查 Dockerfile**：确认路径和命令正确

### 数据库连接失败

1. **等待 MySQL 就绪**：应用配置了 `depends_on`，会等待 MySQL 健康检查通过
2. **检查环境变量**：确认数据库连接信息正确
3. **查看 MySQL 日志**：`docker-compose logs mysql`

## 📊 镜像大小优化

当前镜像使用了以下优化：

- ✅ 多阶段构建（减少最终镜像大小）
- ✅ Alpine Linux 基础镜像（体积小）
- ✅ JRE 而非 JDK（运行环境更小）
- ✅ 前端构建产物直接打包到 JAR 中（单层镜像）

最终镜像大小约 **200-300 MB**。

## 🔄 CI/CD 集成

可以轻松集成到 CI/CD 流程：

```yaml
# GitHub Actions 示例
- name: Build and push Docker image
  run: |
    docker build -t autoprotocoltrans:latest .
    docker tag autoprotocoltrans:latest registry.example.com/autoprotocoltrans:${{ github.sha }}
    docker push registry.example.com/autoprotocoltrans:${{ github.sha }}
```

## 📝 总结

容器化部署的优势：

- ✅ **环境一致性**：开发、测试、生产环境完全一致
- ✅ **快速部署**：一条命令启动整个系统
- ✅ **易于扩展**：可以轻松横向扩展
- ✅ **资源隔离**：应用和数据库相互隔离
- ✅ **数据持久化**：数据库数据安全存储

现在您的项目已经完全支持容器化部署了！🎉









