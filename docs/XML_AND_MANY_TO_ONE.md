# XML 兼容和多对一处理功能说明

## 功能概述

第四阶段实现了两个重要功能：
1. **XML 兼容**：支持 XML 格式的输入和输出
2. **多对一映射**：支持多个源字段映射到一个目标字段

## XML 兼容功能

### 1. MessageConverterUtil 工具类

新增了 `MessageConverterUtil` 工具类，提供统一的 XML/JSON 转换接口：

```java
// XML 字符串 -> Map
Map<String, Object> sourceMap = MessageConverterUtil.parseToMap(xmlString, "XML");

// Map -> XML 字符串
String xmlResult = MessageConverterUtil.mapToString(targetMap, "XML", true);
```

### 2. TransformationEngine 支持

`TransformationEngine` 现在支持：
- 自动检测源数据类型（通过 `sourceProtocol` 配置或自动检测）
- 根据 `targetProtocol` 配置输出 XML 或 JSON
- 如果未指定协议类型，默认与源类型相同

### 3. 配置示例

```json
{
  "sourceProtocol": "XML",
  "targetProtocol": "JSON",
  "prettyPrint": true,
  "rules": [
    {
      "sourcePath": "$.user.name",
      "targetPath": "userName",
      "mappingType": "ONE_TO_ONE",
      "transformType": "DIRECT"
    }
  ]
}
```

### 4. XML 输入示例

```xml
<user>
  <name>张三</name>
  <age>25</age>
  <email>zhangsan@example.com</email>
</user>
```

转换后的 JSON：
```json
{
  "userName": "张三",
  "userAge": 25,
  "userEmail": "zhangsan@example.com"
}
```

## 多对一映射功能

### 1. 前端交互

在画布编辑器中，现在支持：
- **多条连线指向同一个目标节点**：可以连接多个源节点到一个目标节点
- **自动识别多对一场景**：导出配置时自动将多条连线合并为 `MANY_TO_ONE` 规则

### 2. 配置结构

多对一映射的配置格式：

```json
{
  "sourcePath": "$.firstName",           // 主源路径
  "additionalSources": [                  // 额外的源路径
    "$.lastName"
  ],
  "targetPath": "fullName",
  "mappingType": "MANY_TO_ONE",
  "transformType": "GROOVY",
  "transformConfig": {
    "groovyScript": "def parts = input as List; return parts[0] + ' ' + parts[1]"
  }
}
```

### 3. Groovy 脚本中的输入

在多对一映射中，Groovy 脚本的 `input` 变量是一个 `List<Object>`：

```groovy
// 示例1：拼接字符串
def parts = input as List
return parts[0]?.toString() + ' ' + parts[1]?.toString()

// 示例2：使用 collect 和 join
def parts = input as List
return parts.collect { it?.toString() ?: '' }.join(' ')

// 示例3：数值计算
def parts = input as List
return (parts[0] as Integer ?: 0) + (parts[1] as Integer ?: 0)

// 示例4：条件判断
def parts = input as List
if (parts.size() >= 2 && parts[0] && parts[1]) {
    return parts[0] + ' ' + parts[1]
} else {
    return parts[0] ?: ''
}
```

### 4. 使用场景示例

#### 场景1：姓名拼接

**源数据：**
```json
{
  "firstName": "张",
  "lastName": "三"
}
```

**映射配置：**
```json
{
  "sourcePath": "$.firstName",
  "additionalSources": ["$.lastName"],
  "targetPath": "fullName",
  "mappingType": "MANY_TO_ONE",
  "transformType": "GROOVY",
  "transformConfig": {
    "groovyScript": "def parts = input as List; return (parts[0] ?: '') + ' ' + (parts[1] ?: '')"
  }
}
```

**结果：**
```json
{
  "fullName": "张 三"
}
```

#### 场景2：地址组合

**源数据：**
```json
{
  "province": "北京市",
  "city": "北京市",
  "district": "朝阳区",
  "street": "建国路88号"
}
```

**映射配置：**
```json
{
  "sourcePath": "$.province",
  "additionalSources": ["$.city", "$.district", "$.street"],
  "targetPath": "address",
  "mappingType": "MANY_TO_ONE",
  "transformType": "GROOVY",
  "transformConfig": {
    "groovyScript": "def parts = input as List; return parts.collect { it?.toString() ?: '' }.join('')"
  }
}
```

**结果：**
```json
{
  "address": "北京市北京市朝阳区建国路88号"
}
```

## 前端使用说明

### 创建多对一映射

1. **拖拽源字段到画布**：从左侧源数据树拖拽第一个源字段（如 firstName）
   - 自动创建源节点（蓝色）和目标节点（紫色）并连接

2. **连接第二个源字段**：
   - 从左侧源数据树拖拽第二个源字段（如 lastName）
   - 创建第二个源节点（蓝色）
   - **将第二个源节点的右侧端口连接到已存在的目标节点**

3. **配置转换规则**：
   - 点击任意一条连线，打开配置对话框
   - 选择转换类型为 "Groovy脚本"
   - 输入脚本，例如：`def parts = input as List; return parts[0] + ' ' + parts[1]`

4. **查看预览**：右侧预览面板会自动显示转换结果

### 导出配置

点击"导出配置"按钮，系统会：
- 自动检测多对一场景（多个源节点连接到同一个目标节点）
- 生成 `MANY_TO_ONE` 类型的规则
- 将所有源路径合并到 `sourcePath` 和 `additionalSources` 中

## 技术实现细节

### 后端

1. **TransformationEngine.applyManyToOneMapping()**：
   - 读取主源路径和所有额外源路径
   - 将值收集到 `List<Object>` 中
   - 传递给转换策略（通常是 GroovyStrategy）

2. **GroovyStrategy**：
   - 检测 `input` 是否为 `List`
   - 如果是 List，同时提供 `input` 和 `inputs` 变量
   - 支持在脚本中使用 `input[0]`, `input[1]` 等方式访问

### 前端

1. **导出配置逻辑**：
   - 按目标节点 ID 分组所有边
   - 如果目标节点只有一条边，生成 `ONE_TO_ONE` 规则
   - 如果目标节点有多条边，生成 `MANY_TO_ONE` 规则

2. **连线验证**：
   - 允许多条线连向同一个目标节点
   - 只验证源节点类型和目标节点类型

## 注意事项

1. **XML 转换**：
   - XML 会被转换为 Map 结构，然后使用 JsonPath 读取
   - XML 属性可能需要特殊处理（Jackson 默认行为）

2. **多对一映射**：
   - 必须使用 Groovy 脚本或其他支持 List 输入的转换类型
   - 直接赋值（DIRECT）不适用于多对一映射
   - 脚本需要处理 List 类型和 null 值

3. **字段顺序**：
   - `additionalSources` 中的顺序对应 Groovy 脚本中 `input[1]`, `input[2]` 的顺序
   - 主源路径对应 `input[0]`

