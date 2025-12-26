# 多阶段构建 Dockerfile
# 阶段1: 构建前端
FROM node:18-alpine AS frontend-builder

# 设置工作目录为项目根目录，以便构建输出到正确的位置
WORKDIR /app

# 复制前端依赖文件
COPY frontend/package*.json ./frontend/

# 安装前端依赖
RUN cd frontend && npm install --registry=https://registry.npmmirror.com

# 复制前端源代码
COPY frontend/ ./frontend/

# 构建前端（输出到 src/main/resources/static）
RUN cd frontend && npm run build

# 阶段2: 构建后端
FROM maven:3.9-eclipse-temurin-17 AS backend-builder

WORKDIR /app

# 复制 pom.xml 和 mvnw
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# 下载依赖（利用Docker缓存层）
RUN mvn dependency:go-offline -B

# 复制源代码
COPY src ./src

# 从前端构建阶段复制构建产物
COPY --from=frontend-builder /app/src/main/resources/static ./src/main/resources/static

# 构建后端应用（跳过测试，加快构建速度）
RUN mvn clean package -DskipTests

# 阶段3: 运行阶段
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 安装必要的工具（包括 wget 用于健康检查）
RUN apk add --no-cache tzdata wget && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    apk del tzdata

# 从构建阶段复制JAR文件
COPY --from=backend-builder /app/target/autoprotocoltrans-*.jar app.jar

# 暴露端口
EXPOSE 8080

# 设置JVM参数
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

