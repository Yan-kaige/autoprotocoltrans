# 报文转换系统 - 核心架构设计

## 一、系统架构概览

```
┌─────────────────────────────────────────────────────────────┐
│                     前端 (Vue 3 + AntV X6)                    │
│  ┌──────────┐  ┌──────────────┐  ┌──────────┐              │
│  │ 源数据树  │  │  拖拽画布     │  │ 目标数据树 │              │
│  └──────────┘  └──────────────┘  └──────────┘              │
└──────────────────────┬──────────────────────────────────────┘
                       │ JSON配置
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                   后端 (Spring Boot)                          │
│  ┌──────────────────────────────────────────────────────┐   │
│  │          TransformationEngine (转换引擎)              │   │
│  │  ┌─────────────────────────────────────────────┐     │   │
│  │  │      Strategy Pattern (策略模式)             │     │   │
│  │  │  ┌──────┐ ┌─────────┐ ┌──────────┐         │     │   │
│  │  │  │Direct│ │Groovy   │ │Dictionary│ ...     │     │   │
│  │  │  └──────┘ └─────────┘ └──────────┘         │     │   │
│  │  └─────────────────────────────────────────────┘     │   │
│  │                                                       │   │
│  │  JsonPath解析 → 策略执行 → 深度路径赋值               │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

## 二、核心组件设计

### 1. 策略接口 (TransformStrategy)

```java
public interface TransformStrategy {
    /**
     * 执行转换
     * @param sourceValue 源字段值（可能为单个值或List<Object>）
     * @param ruleConfig 规则配置Map
     * @return 转换后的值
     */
    Object transform(Object sourceValue, Map<String, Object> ruleConfig);
    
    /**
     * 获取策略类型
     */
    String getType();
}
```

### 2. JSON配置结构示例

```json
{
  "sourceProtocol": "JSON",
  "targetProtocol": "JSON",
  "rules": [
    {
      "sourcePath": "$.user.name",
      "targetPath": "customer.userName",
      "mappingType": "ONE_TO_ONE",
      "transformType": "DIRECT"
    },
    {
      "sourcePath": "$.user.status",
      "targetPath": "customer.status",
      "mappingType": "ONE_TO_ONE",
      "transformType": "FUNCTION",
      "function": "upperCase"
    },
    {
      "sourcePath": "$.user.firstName",
      "targetPath": "customer.fullName",
      "mappingType": "MANY_TO_ONE",
      "transformType": "GROOVY",
      "groovyScript": "def parts = input as List; return parts[0] + ' ' + parts[1]",
      "additionalSources": ["$.user.lastName"]
    },
    {
      "sourcePath": "$.order.status",
      "targetPath": "order.status",
      "mappingType": "ONE_TO_ONE",
      "transformType": "DICTIONARY",
      "dictionary": {
        "pending": "待处理",
        "completed": "已完成"
      }
    },
    {
      "sourcePath": null,
      "targetPath": "order.createTime",
      "mappingType": "ONE_TO_ONE",
      "transformType": "FIXED",
      "fixedValue": "2024-01-01 00:00:00"
    }
  ]
}
```

## 三、类图设计

```
┌─────────────────────┐
│ TransformStrategy   │ (接口)
├─────────────────────┤
│ + transform()       │
│ + getType()         │
└──────────┬──────────┘
           │
           ├──────────────────┬──────────────────┬─────────────────┐
           │                  │                  │                 │
┌──────────▼──────────┐ ┌─────▼────────┐ ┌──────▼─────────┐ ┌───▼─────────┐
│ DirectStrategy      │ │GroovyStrategy│ │DictionaryStrategy│ │FunctionStrategy│
├─────────────────────┤ ├──────────────┤ ├─────────────────┤ ├─────────────┤
│ + transform()       │ │ + transform()│ │ + transform()   │ │ + transform()│
└─────────────────────┘ └──────────────┘ └─────────────────┘ └─────────────┘
           │
           │
┌──────────▼──────────────────────────────────────────────────────────┐
│                    TransformationEngine                              │
├──────────────────────────────────────────────────────────────────────┤
│ - strategies: Map<String, TransformStrategy>                        │
│ - jsonPath: JsonPath                                                │
│ + transform(sourceJson: String, config: MappingConfig): String     │
│ - parseSource(sourceJson: String): Map<String, Object>             │
│ - applyRule(sourceMap: Map, rule: MappingRule, targetMap: Map)     │
│ - setDeepValue(targetMap: Map, path: String, value: Object)        │
└──────────────────────────────────────────────────────────────────────┘
```

