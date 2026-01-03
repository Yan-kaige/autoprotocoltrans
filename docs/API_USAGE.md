# API 使用文档

## 概述

本文档说明如何使用导出的配置通过 REST API 转换自定义报文。系统支持 JSON 和 XML 格式的输入输出。

## API 接口

### 转换接口

**端点**: `POST /api/v2/transform`

**功能**: 根据配置将源数据转换为目标格式

**请求头**:
```
Content-Type: application/json
```

**请求体**:
```json
{
  "sourceData": "源数据（JSON或XML字符串）",
  "mappingConfig": {
    "sourceProtocol": "JSON",
    "targetProtocol": "JSON",
    "prettyPrint": true,
    "xmlRootElementName": "user",
    "includeXmlDeclaration": true,
    "rules": [
      {
        "sourcePath": "$.user.name",
        "targetPath": "customer.userName",
        "mappingType": "ONE_TO_ONE",
        "transformType": "DIRECT"
      }
    ]
  }
}
```

**响应体**:
```json
{
  "success": true,
  "transformedData": "转换后的数据（JSON或XML字符串）",
  "errorMessage": null
}
```

**错误响应**:
```json
{
  "success": false,
  "transformedData": null,
  "errorMessage": "错误信息"
}
```

## 使用示例

### 1. cURL 命令示例

#### JSON 到 JSON 转换

```bash
curl -X POST http://localhost:8080/api/v2/transform \
  -H "Content-Type: application/json" \
  -d '{
    "sourceData": "{\"user\":{\"name\":\"张三\",\"age\":30}}",
    "mappingConfig": {
      "sourceProtocol": "JSON",
      "targetProtocol": "JSON",
      "prettyPrint": true,
      "rules": [
        {
          "sourcePath": "$.user.name",
          "targetPath": "customer.name",
          "mappingType": "ONE_TO_ONE",
          "transformType": "DIRECT"
        },
        {
          "sourcePath": "$.user.age",
          "targetPath": "customer.age",
          "mappingType": "ONE_TO_ONE",
          "transformType": "DIRECT"
        }
      ]
    }
  }'
```

#### XML 到 JSON 转换

```bash
curl -X POST http://localhost:8080/api/v2/transform \
  -H "Content-Type: application/json" \
  -d '{
    "sourceData": "<user><name>张三</name><age>30</age></user>",
    "mappingConfig": {
      "sourceProtocol": "XML",
      "targetProtocol": "JSON",
      "prettyPrint": true,
      "rules": [
        {
          "sourcePath": "$.name",
          "targetPath": "customer.name",
          "mappingType": "ONE_TO_ONE",
          "transformType": "DIRECT"
        }
      ]
    }
  }'
```

#### JSON 到 XML 转换

```bash
curl -X POST http://localhost:8080/api/v2/transform \
  -H "Content-Type: application/json" \
  -d '{
    "sourceData": "{\"name\":\"张三\",\"age\":30}",
    "mappingConfig": {
      "sourceProtocol": "JSON",
      "targetProtocol": "XML",
      "prettyPrint": true,
      "xmlRootElementName": "user",
      "includeXmlDeclaration": true,
      "rules": [
        {
          "sourcePath": "$.name",
          "targetPath": "name",
          "mappingType": "ONE_TO_ONE",
          "transformType": "DIRECT"
        },
        {
          "sourcePath": "$.age",
          "targetPath": "age",
          "mappingType": "ONE_TO_ONE",
          "transformType": "DIRECT"
        }
      ]
    }
  }'
```

### 2. Java 代码示例

```java
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

public class TransformClient {
    
    private static final String API_URL = "http://localhost:8080/api/v2/transform";
    private static final RestTemplate restTemplate = new RestTemplate();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    public static String transform(String sourceData, Map<String, Object> mappingConfig) {
        try {
            // 构建请求体
            Map<String, Object> request = new HashMap<>();
            request.put("sourceData", sourceData);
            request.put("mappingConfig", mappingConfig);
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // 发送请求
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(API_URL, entity, Map.class);
            
            // 处理响应
            Map<String, Object> responseBody = response.getBody();
            if (Boolean.TRUE.equals(responseBody.get("success"))) {
                return (String) responseBody.get("transformedData");
            } else {
                throw new RuntimeException("转换失败: " + responseBody.get("errorMessage"));
            }
        } catch (Exception e) {
            throw new RuntimeException("调用转换接口失败", e);
        }
    }
    
    // 使用示例
    public static void main(String[] args) {
        // 源数据
        String sourceData = "{\"user\":{\"name\":\"张三\",\"age\":30}}";
        
        // 映射配置（可以从前端导出的配置复制）
        Map<String, Object> mappingConfig = new HashMap<>();
        mappingConfig.put("sourceProtocol", "JSON");
        mappingConfig.put("targetProtocol", "JSON");
        mappingConfig.put("prettyPrint", true);
        
        List<Map<String, Object>> rules = new ArrayList<>();
        
        Map<String, Object> rule1 = new HashMap<>();
        rule1.put("sourcePath", "$.user.name");
        rule1.put("targetPath", "customer.name");
        rule1.put("mappingType", "ONE_TO_ONE");
        rule1.put("transformType", "DIRECT");
        rules.add(rule1);
        
        Map<String, Object> rule2 = new HashMap<>();
        rule2.put("sourcePath", "$.user.age");
        rule2.put("targetPath", "customer.age");
        rule2.put("mappingType", "ONE_TO_ONE");
        rule2.put("transformType", "DIRECT");
        rules.add(rule2);
        
        mappingConfig.put("rules", rules);
        
        // 执行转换
        String result = transform(sourceData, mappingConfig);
        System.out.println("转换结果:\n" + result);
    }
}
```

### 3. Python 代码示例

```python
import requests
import json

API_URL = "http://localhost:8080/api/v2/transform"

def transform(source_data, mapping_config):
    """
    调用转换接口
    
    Args:
        source_data: 源数据（JSON或XML字符串）
        mapping_config: 映射配置字典
    
    Returns:
        转换后的数据字符串
    """
    request_body = {
        "sourceData": source_data,
        "mappingConfig": mapping_config
    }
    
    headers = {
        "Content-Type": "application/json"
    }
    
    response = requests.post(API_URL, json=request_body, headers=headers)
    response.raise_for_status()
    
    result = response.json()
    if result.get("success"):
        return result.get("transformedData")
    else:
        raise Exception(f"转换失败: {result.get('errorMessage')}")


# 使用示例
if __name__ == "__main__":
    # 源数据
    source_data = json.dumps({
        "user": {
            "name": "张三",
            "age": 30
        }
    }, ensure_ascii=False)
    
    # 映射配置（可以从前端导出的配置复制）
    mapping_config = {
        "sourceProtocol": "JSON",
        "targetProtocol": "JSON",
        "prettyPrint": True,
        "rules": [
            {
                "sourcePath": "$.user.name",
                "targetPath": "customer.name",
                "mappingType": "ONE_TO_ONE",
                "transformType": "DIRECT"
            },
            {
                "sourcePath": "$.user.age",
                "targetPath": "customer.age",
                "mappingType": "ONE_TO_ONE",
                "transformType": "DIRECT"
            }
        ]
    }
    
    # 执行转换
    result = transform(source_data, mapping_config)
    print("转换结果:")
    print(result)
```

### 4. JavaScript/Node.js 示例

```javascript
const axios = require('axios');

const API_URL = 'http://localhost:8080/api/v2/transform';

/**
 * 调用转换接口
 * @param {string} sourceData - 源数据（JSON或XML字符串）
 * @param {object} mappingConfig - 映射配置对象
 * @returns {Promise<string>} 转换后的数据字符串
 */
async function transform(sourceData, mappingConfig) {
  try {
    const response = await axios.post(API_URL, {
      sourceData,
      mappingConfig
    }, {
      headers: {
        'Content-Type': 'application/json'
      }
    });
    
    if (response.data.success) {
      return response.data.transformedData;
    } else {
      throw new Error(`转换失败: ${response.data.errorMessage}`);
    }
  } catch (error) {
    throw new Error(`调用转换接口失败: ${error.message}`);
  }
}

// 使用示例
async function main() {
  // 源数据
  const sourceData = JSON.stringify({
    user: {
      name: '张三',
      age: 30
    }
  });
  
  // 映射配置（可以从前端导出的配置复制）
  const mappingConfig = {
    sourceProtocol: 'JSON',
    targetProtocol: 'JSON',
    prettyPrint: true,
    rules: [
      {
        sourcePath: '$.user.name',
        targetPath: 'customer.name',
        mappingType: 'ONE_TO_ONE',
        transformType: 'DIRECT'
      },
      {
        sourcePath: '$.user.age',
        targetPath: 'customer.age',
        mappingType: 'ONE_TO_ONE',
        transformType: 'DIRECT'
      }
    ]
  };
  
  // 执行转换
  const result = await transform(sourceData, mappingConfig);
  console.log('转换结果:');
  console.log(result);
}

main().catch(console.error);
```

## 配置格式说明

### MappingConfig 字段说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sourceProtocol | String | 是 | 源协议类型，可选值：`JSON`、`XML` |
| targetProtocol | String | 是 | 目标协议类型，可选值：`JSON`、`XML` |
| prettyPrint | Boolean | 否 | 是否格式化输出，默认 `false` |
| xmlRootElementName | String | 否 | XML根元素名称（仅当targetProtocol为XML时有效） |
| includeXmlDeclaration | Boolean | 否 | 是否包含XML声明（仅当targetProtocol为XML时有效），默认 `false` |
| rules | Array | 是 | 映射规则列表 |

### MappingRule 字段说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sourcePath | String | 是* | 源字段路径（JsonPath表达式），如 `$.user.name`。对于`FIXED`类型可为null |
| additionalSources | Array<String> | 否 | 额外的源路径（仅用于`MANY_TO_ONE`映射） |
| targetPath | String | 是 | 目标字段路径，支持深度路径，如 `user.address.city` |
| mappingType | String | 是 | 映射类型：`ONE_TO_ONE`、`ONE_TO_MANY`、`MANY_TO_ONE` |
| transformType | String | 是 | 转换类型：`DIRECT`、`FUNCTION`、`GROOVY`、`DICTIONARY`、`FIXED`、`IGNORE` |
| transformConfig | Object | 否 | 转换配置（根据transformType不同而不同） |

### transformConfig 配置说明

#### DIRECT（直接映射）
无需额外配置，或 `transformConfig` 为空。

#### FUNCTION（函数映射）
```json
{
  "transformConfig": {
    "function": "upperCase"
  }
}
```
支持的函数：`upperCase`、`lowerCase`、`trim`、`currentDate`

#### GROOVY（Groovy脚本）
```json
{
  "transformConfig": {
    "groovyScript": "return input.toUpperCase()"
  }
}
```
脚本中可以使用 `input` 变量，对于`MANY_TO_ONE`映射，`input` 是一个 `List`。

#### DICTIONARY（字典映射）
```json
{
  "transformConfig": {
    "dictionary": {
      "pending": "待处理",
      "completed": "已完成",
      "cancelled": "已取消"
    }
  }
}
```

#### FIXED（固定值）
```json
{
  "transformConfig": {
    "fixedValue": "固定值"
  }
}
```

#### ONE_TO_MANY（一对多映射）
```json
{
  "transformConfig": {
    "groovyScript": "def parts = input?.toString()?.split('-') ?: []; return [province: parts[0] ?: '', city: parts[1] ?: '']",
    "subMappings": [
      {
        "sourcePath": "$.province",
        "targetPath": "address.province"
      },
      {
        "sourcePath": "$.city",
        "targetPath": "address.city"
      }
    ]
  }
}
```

## 完整配置示例

查看 `docs/CONFIG_EXAMPLE.json` 文件获取完整的配置示例，包含所有映射类型和转换类型的用法。

## 前端导出配置

1. 在画布编辑器页面配置好映射规则
2. 点击"导出配置"按钮
3. 配置已复制到剪贴板，可以直接粘贴到代码中使用

## 注意事项

1. **JsonPath 路径**：源路径使用 JsonPath 表达式，必须以 `$` 开头，如 `$.user.name`
2. **目标路径**：目标路径不使用 `$` 前缀，使用点号分隔，如 `user.name` 或 `customer.address.city`
3. **XML 根元素**：当目标协议为 XML 时，建议指定 `xmlRootElementName`，避免生成 `<HashMap>` 标签
4. **错误处理**：请始终检查响应中的 `success` 字段，如果为 `false`，查看 `errorMessage` 了解错误原因
5. **数据格式**：源数据必须是有效的 JSON 或 XML 字符串

## 调试接口

### 调试解析接口

**端点**: `POST /api/v2/transform/debug/parse`

**功能**: 查看 XML/JSON 解析后的结构（用于调试）

**请求体**:
```json
{
  "sourceData": "<user><name>张三</name></user>",
  "sourceType": "XML"
}
```

**响应体**:
```json
{
  "success": true,
  "parsedJson": "解析后的JSON字符串",
  "parsedMap": { /* 解析后的Map对象 */ }
}
```

这个接口可以帮助您了解后端是如何解析 XML/JSON 数据的，便于正确配置 `sourcePath`。












