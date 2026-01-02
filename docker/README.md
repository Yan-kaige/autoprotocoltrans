# Docker 容器化部署指南

## 前置要求

- Docker 20.10+
- Docker Compose 2.0+

## 快速开始

### 1. 构建并启动所有服务

```bash
docker-compose up -d
```

这个命令会：
- 构建应用镜像（包含前端构建）
- 启动 MySQL 数据库容器
- 启动 Spring Boot 应用容器
- 自动创建所有数据库表

### 2. 查看服务状态

```bash
docker-compose ps
```

### 3. 查看日志

查看所有服务日志：
```bash
docker-compose logs -f
```

查看应用日志：
```bash
docker-compose logs -f app
```

查看数据库日志：
```bash
docker-compose logs -f mysql
```

### 4. 访问应用

应用启动后，访问：http://localhost:8080

## 环境变量配置

### 数据库配置

可以通过环境变量修改数据库配置，编辑 `docker-compose.yml`：

```yaml
services:
  app:
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/protovol_trans?...
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: 123456
```

### MySQL 配置

```yaml
services:
  mysql:
    environment:
      MYSQL_ROOT_PASSWORD: 123456
      MYSQL_DATABASE: protovol_trans
      MYSQL_USER: appuser
      MYSQL_PASSWORD: apppass
```

## 常用操作

### 停止服务

```bash
docker-compose down
```

### 停止服务并删除数据卷（⚠️ 会删除数据库数据）

```bash
docker-compose down -v
```

### 重新构建镜像

```bash
docker-compose build --no-cache
```

### 只启动数据库

```bash
docker-compose up -d mysql
```

### 进入容器

进入应用容器：
```bash
docker-compose exec app sh
```

进入数据库容器：
```bash
docker-compose exec mysql bash
```

### 数据库连接

使用 MySQL 客户端连接：
```bash
mysql -h 127.0.0.1 -P 3307 -u root -p123456 protovol_trans
```

## 数据持久化

数据库数据存储在 Docker 卷 `autoprotocoltrans_mysql_data` 中，即使删除容器，数据也会保留。

查看数据卷：
```bash
docker volume ls
```

备份数据库：
```bash
docker-compose exec mysql mysqldump -u root -p123456 protovol_trans > backup.sql
```

恢复数据库：
```bash
docker-compose exec -T mysql mysql -u root -p123456 protovol_trans < backup.sql
```

## 单独构建 Docker 镜像

如果只需要构建应用镜像：

```bash
docker build -t autoprotocoltrans:latest .
```

运行镜像（需要外部 MySQL）：
```bash
docker run -d \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3307/protovol_trans?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=123456 \
  --name autoprotocoltrans-app \
  autoprotocoltrans:latest
```

## 生产环境建议

1. **修改默认密码**：修改 `docker-compose.yml` 中的数据库密码
2. **使用环境变量文件**：创建 `.env` 文件管理敏感信息
3. **配置资源限制**：为容器设置 CPU 和内存限制
4. **启用 HTTPS**：配置反向代理（如 Nginx）启用 HTTPS
5. **日志管理**：配置日志收集和监控
6. **数据备份**：设置定期数据库备份策略

### 示例：使用 .env 文件

创建 `.env` 文件：
```env
MYSQL_ROOT_PASSWORD=your_strong_password
MYSQL_DATABASE=protovol_trans
SPRING_DATASOURCE_PASSWORD=your_strong_password
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

## 故障排查

### 应用无法连接数据库

1. 检查 MySQL 容器是否正常启动：
```bash
docker-compose ps mysql
```

2. 检查数据库连接字符串是否正确

3. 查看应用日志：
```bash
docker-compose logs app
```

### 端口冲突

如果 8080 或 3307 端口被占用，修改 `docker-compose.yml` 中的端口映射：
```yaml
ports:
  - "8081:8080"  # 修改主机端口
```

### 构建失败

1. 检查网络连接（需要下载 Docker 镜像和 npm 包）
2. 清理构建缓存后重新构建：
```bash
docker-compose build --no-cache
```

### 前端构建失败

1. 检查 Node.js 版本（需要 Node 18+）
2. 清理 npm 缓存：
```bash
docker-compose exec app npm cache clean --force
```









