# 快速启动指南

## 前提条件

- JDK 17+
- Maven 3.6+
- Node.js 16+ 和 npm

## 启动步骤

### 1. 启动后端服务

在项目根目录执行：

```bash
mvn clean install
mvn spring-boot:run
```

后端将在 http://localhost:8080 启动

### 2. 启动前端开发服务器

打开新的终端窗口，进入frontend目录：

```bash
cd frontend
npm install
npm run dev
```

前端开发服务器将在 http://localhost:3000 启动

### 3. 访问应用

在浏览器中访问 http://localhost:3000

## 使用示例

### 示例1：JSON到JSON的简单转换

1. 点击"新建规则"
2. 填写规则名称：`示例规则1`
3. 选择源协议：`JSON`，目标协议：`JSON`
4. 点击"添加字段映射"
5. 配置：
   - 源字段路径：`name`
   - 目标字段路径：`userName`
   - 映射类型：`1对1`
   - 转换类型：`等于`
6. 在测试区域输入源数据：
```json
{
  "name": "张三",
  "age": 25
}
```
7. 点击"执行转换"，查看结果

### 示例2：使用函数转换

1. 创建规则，选择源字段：`status`
2. 选择转换类型：`函数映射`
3. 选择函数：`upperCase`（转大写）
4. 输入源数据测试转换效果

### 示例3：字典映射

1. 创建规则，选择转换类型：`字典映射`
2. 添加映射项：
   - `male` -> `男`
   - `female` -> `女`
3. 测试转换

## 常见问题

### 端口被占用

如果8080端口被占用，修改 `src/main/resources/application.properties` 中的 `server.port`

### 前端无法连接后端

确保后端服务已启动，并且检查 `frontend/vite.config.js` 中的proxy配置

### 枚举值转换错误

如果遇到枚举值相关的错误，确保前后端都使用枚举的字符串值（如 `JSON`, `ONE_TO_ONE` 等）

