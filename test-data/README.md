# 测试数据说明

本目录包含用于测试报文转换系统的示例数据。

## 文件说明

### 1. 简单数据示例

- **sample-json-source.json**: 简单的JSON用户数据
  - 包含用户基本信息、地址、联系方式等
  - 适用于测试1对1字段映射

- **sample-xml-source.xml**: 简单的XML用户数据
  - 与JSON示例结构类似，但格式为XML
  - 可用于测试JSON↔XML转换

### 2. 复杂数据示例

- **complex-json-source.json**: 复杂的JSON订单数据
  - 包含订单信息、客户信息、商品列表等嵌套结构
  - 适用于测试1对多、多对1映射

- **complex-xml-source.xml**: 复杂的XML订单数据
  - 订单数据结构，字段命名与JSON不同
  - 可用于测试复杂转换规则

## 使用示例

### 示例1：简单用户信息转换（JSON → JSON）

**源数据** (sample-json-source.json):
```json
{
  "name": "张三",
  "age": 25,
  "gender": "male"
}
```

**转换规则**:
- 源字段: `name` → 目标字段: `userName` (等于)
- 源字段: `age` → 目标字段: `userAge` (等于)
- 源字段: `gender` → 目标字段: `userGender` (字典映射: male→男, female→女)

**预期结果**:
```json
{
  "userName": "张三",
  "userAge": 25,
  "userGender": "男"
}
```

### 示例2：用户信息转换（JSON → XML）

**源数据**: sample-json-source.json

**转换规则**:
- 源字段: `name` → 目标字段: `user.name` (等于)
- 源字段: `age` → 目标字段: `user.age` (数据类型转换: int)
- 源字段: `address.city` → 目标字段: `user.city` (等于)

**预期结果**:
```xml
<user>
    <name>张三</name>
    <age>25</age>
    <city>北京市</city>
</user>
```

### 示例3：订单信息转换（复杂映射）

**源数据**: complex-json-source.json

**转换规则**:
- 1对1映射:
  - `orderId` → `order.orderNumber` (等于)
  - `customer.name` → `order.customerName` (等于)
- 1对多映射:
  - `items` → 多个目标字段
    - `items[].productName` → `orderItems[].itemName`
    - `items[].quantity` → `orderItems[].itemQty`
- 多对1映射:
  - `totalAmount`, `discount`, `finalAmount` → `order.summary` (合并对象)

### 示例4：使用函数转换

**转换规则**:
- 源字段: `status` → 目标字段: `status` (函数映射: upperCase)
- 源字段: `createTime` → 目标字段: `createTime` (函数映射: currentDate - 转为当前时间)

### 示例5：字典映射

**转换规则**:
- 源字段: `gender` → 目标字段: `gender`
  - 字典映射:
    - `male` → `男`
    - `female` → `女`
- 源字段: `orderStatus` → 目标字段: `status`
  - 字典映射:
    - `pending` → `待处理`
    - `completed` → `已完成`
    - `cancelled` → `已取消`

## 测试步骤

1. 启动后端服务: `mvn spring-boot:run`
2. 启动前端服务: `cd frontend && npm run dev`
3. 访问 http://localhost:3000
4. 创建新的转换规则
5. 配置字段映射关系
6. 在测试区域粘贴源数据
7. 点击"执行转换"查看结果

## 注意事项

- JSON数据需要是有效的JSON格式
- XML数据需要包含XML声明和有效的XML结构
- 字段路径支持嵌套访问，使用点号分隔（如: `user.address.city`）
- 数组字段需要使用索引或通配符（根据实现而定）

