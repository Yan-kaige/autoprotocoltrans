# 容器化部署快速指南

## 🚀 一键部署

```bash
# 1. 克隆项目（如果还没有）
git clone <your-repo-url>
cd autoprotocoltrans

# 2. 启动所有服务
docker-compose up -d

# 3. 等待服务启动（约1-2分钟）
docker-compose logs -f

# 4. 访问应用
# 浏览器打开：http://localhost:8080
```

## 📦 项目结构

容器化部署相关文件：

```
autoprotocoltrans/
├── Dockerfile              # 多阶段构建文件
├── docker-compose.yml      # Docker Compose 配置
├── .dockerignore          # Docker 构建忽略文件
├── DOCKER.md              # 详细部署文档
├── DEPLOY.md              # 本文件（快速指南）
└── src/main/resources/
    └── db/migration/      # 数据库初始化脚本
        ├── V1__create_mapping_config_table.sql
        ├── V2__create_dictionary_table.sql
        ├── V3__create_dictionary_item_table.sql
        └── V4__create_custom_function_table.sql
```

## ⚡ 快速命令

```bash
# 启动
docker-compose up -d

# 停止
docker-compose down

# 查看日志
docker-compose logs -f app

# 重新构建
docker-compose up -d --build

# 查看状态
docker-compose ps
```

## 🔧 配置修改

### 修改端口

编辑 `docker-compose.yml`：

```yaml
services:
  app:
    ports:
      - "8081:8080"  # 修改主机端口
```

### 修改数据库密码

编辑 `docker-compose.yml`：

```yaml
services:
  mysql:
    environment:
      MYSQL_ROOT_PASSWORD: your_password
  app:
    environment:
      SPRING_DATASOURCE_PASSWORD: your_password
```

## 📝 详细说明

更多详细信息请参考：[DOCKER.md](./DOCKER.md)

