# 报文转换系统

基于Spring Boot + Vue实现的报文转换系统，支持JSON/XML之间的自定义报文转换。

## 功能特性

1. **协议转换**：支持JSON ↔ XML之间的双向转换
2. **字段映射关系**：
   - 1对1映射
   - 1对多映射
   - 多对1映射
3. **转换类型**：
   - 等于（直接复制）
   - 函数映射（预置自定义函数）
   - Java方法映射（配置化）
   - 数据类型转换
   - 忽略字段
   - 字典映射
   - 固定值转换
4. **低代码配置**：拖拽式界面配置字段映射关系
5. **实时预览**：支持转换结果的实时预览和测试

## 技术栈

### 后端
- Spring Boot 3.5.9
- Java 17
- Jackson (JSON/XML处理)
- Lombok

### 前端
- Vue 3
- Element Plus
- Vue Router
- Axios
- VueDraggable (拖拽功能)
- Vite

## 项目结构

```
autoprotocoltrans/
├── src/main/java/com/kai/
│   ├── config/          # 配置类
│   ├── controller/      # 控制器
│   ├── dto/            # 数据传输对象
│   ├── enums/          # 枚举类
│   ├── model/          # 实体模型
│   ├── service/        # 服务层
│   └── util/           # 工具类
├── frontend/           # Vue前端项目
│   ├── src/
│   │   ├── api/        # API接口
│   │   ├── components/ # 组件
│   │   ├── router/     # 路由配置
│   │   ├── views/      # 页面视图
│   │   └── main.js     # 入口文件
│   └── package.json
└── pom.xml

```

## 快速开始

### 后端启动

1. 确保已安装Java 17和Maven
2. 在项目根目录执行：
```bash
mvn clean install
mvn spring-boot:run
```

后端服务将运行在 http://localhost:8080

### 前端启动

1. 确保已安装Node.js (推荐16+)
2. 进入frontend目录：
```bash
cd frontend
npm install
npm run dev
```

前端开发服务器将运行在 http://localhost:3000

### 生产构建

构建前端并复制到静态资源目录：
```bash
cd frontend
npm run build
```

构建后的文件会自动复制到 `src/main/resources/static` 目录，可直接通过Spring Boot访问。

## API接口

### 转换接口（推荐使用）

- `POST /api/v2/transform` - 使用配置执行转换（支持JSON/XML互转）

详细的API使用文档请参考：**[API使用文档](docs/API_USAGE.md)**

**快速示例**：
```bash
curl -X POST http://localhost:8080/api/v2/transform \
  -H "Content-Type: application/json" \
  -d '{
    "sourceData": "{\"user\":{\"name\":\"张三\"}}",
    "mappingConfig": {
      "sourceProtocol": "JSON",
      "targetProtocol": "JSON",
      "prettyPrint": true,
      "rules": [...]
    }
  }'
```

**示例代码**：
- Python: `examples/test-api.py`
- Shell: `examples/test-api.sh`
- 配置示例: `examples/transform-example.json`

### 规则管理（旧版API）
- `GET /api/rules` - 获取所有规则
- `GET /api/rules/{id}` - 根据ID获取规则
- `POST /api/rules` - 创建或更新规则
- `DELETE /api/rules/{id}` - 删除规则
- `POST /api/rules/transform` - 执行转换

### 函数管理
- `GET /api/functions` - 获取所有可用函数

## 使用说明

1. **创建转换规则**
   - 点击"新建规则"按钮
   - 填写规则名称、描述
   - 选择源协议类型和目标协议类型

2. **配置字段映射**
   - 点击"添加字段映射"
   - 配置源字段路径和目标字段路径（支持嵌套路径，如 `user.name`）
   - 选择映射类型（1对1、1对多、多对1）
   - 选择转换类型并配置相应参数

3. **测试转换**
   - 在转换测试区域输入源数据
   - 点击"执行转换"查看结果

## 预置函数

- `upperCase` - 转大写
- `lowerCase` - 转小写
- `trim` - 去除空格
- `currentDate` - 当前日期时间
- `length` - 字符串长度
- `toInt` - 转整数
- `toString` - 转字符串
- `toDouble` - 转浮点数

## 注意事项

1. 当前版本使用内存存储规则，重启后数据会丢失。生产环境建议使用数据库存储。
2. XML转换需要确保XML格式正确。
3. 字段路径支持嵌套访问，使用点号分隔（如：`user.address.city`）。

## 开发计划

- [ ] 数据库持久化
- [ ] 更多预置函数
- [ ] Java方法动态调用支持
- [ ] 规则导入导出
- [ ] 转换日志记录
- [ ] 性能优化

## 许可证

MIT License

