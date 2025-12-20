# Groovy脚本使用示例

## 什么是Groovy脚本？

Groovy是一种运行在JVM上的动态脚本语言，语法类似Java但更简洁。在我们的报文转换系统中，Groovy脚本用于实现自定义的字段转换逻辑。

## 脚本中的变量

- `input`: 输入值（可能是单个值或List）
- `inputs`: 当输入是List时的别名

## 常用示例

### 1. 去掉邮箱@后面的部分

```groovy
// 如果input是字符串
if (input == null) return ''
def str = input.toString()
def index = str.indexOf('@')
if (index > 0) {
    return str.substring(0, index)
}
return str
```

**简化版本：**
```groovy
input?.toString()?.split('@')?[0] ?: ''
```

### 2. 字符串转大写
```groovy
input?.toString()?.toUpperCase()
```

### 3. 字符串转小写
```groovy
input?.toString()?.toLowerCase()
```

### 4. 去除空格
```groovy
input?.toString()?.trim()
```

### 5. 字符串拼接（多对1映射）
```groovy
def parts = input as List
if (parts == null || parts.size() < 2) return ''
return (parts[0] ?: '') + ' ' + (parts[1] ?: '')
```

### 6. 字符串替换
```groovy
// 将所有的空格替换为下划线
input?.toString()?.replaceAll(' ', '_')
```

### 7. 字符串截取
```groovy
// 截取前10个字符
def str = input?.toString()
str ? str.substring(0, Math.min(10, str.length())) : ''
```

### 8. 数字格式化
```groovy
// 保留2位小数
if (input instanceof Number) {
    return String.format('%.2f', input)
}
return input
```

### 9. 日期格式化
```groovy
import java.text.SimpleDateFormat
import java.util.Date

def date = input instanceof Date ? input : new Date(input.toString())
def sdf = new SimpleDateFormat('yyyy-MM-dd HH:mm:ss')
return sdf.format(date)
```

### 10. 条件判断
```groovy
def value = input?.toString()?.toInteger()
if (value >= 18) {
    return '成年'
} else {
    return '未成年'
}
```

## 注意事项

1. 使用 `?.` 安全调用操作符避免空指针异常
2. 使用 `?:` Elvis操作符提供默认值
3. 多对1映射时，input是List类型
4. 返回null或空字符串会在转换结果中忽略该字段（取决于配置）

