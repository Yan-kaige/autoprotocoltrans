# 银行配置插件导出使用说明

## 功能概述

系统支持将每个银行的配置导出为 PF4J 插件格式的 ZIP 包，其他项目可以直接使用这些插件来加载银行配置。

## 导出功能

### 前端操作

1. 进入"配置管理"页面
2. 在银行列表中找到要导出的银行
3. 点击"导出插件"按钮
4. 系统会自动下载 ZIP 格式的插件包

### 插件包结构

导出的 ZIP 包包含以下文件：

```
bank-code-plugin.zip
├── plugin.properties    # PF4J 插件描述文件
├── bank-config.json     # 银行配置数据（JSON格式）
├── README.md            # 使用说明文档
└── META-INF/
    └── MANIFEST.MF      # JAR 清单文件
```

### plugin.properties 文件格式

```properties
# PF4J Plugin Descriptor
plugin.id=bank-icbc
plugin.version=1.0.0
plugin.description=工商银行 报文转换配置插件
plugin.provider=AutoProtocolTrans
plugin.class=com.kai.plugin.BankConfigPlugin
plugin.requires=*

# Bank Information
bank.id=1
bank.name=工商银行
bank.code=ICBC
bank.enabled=true
```

### bank-config.json 文件格式

```json
{
  "bankInfo": {
    "id": 1,
    "name": "工商银行",
    "code": "ICBC",
    "description": "中国工商银行",
    "enabled": true
  },
  "transactionTypes": [
    {
      "id": 1,
      "transactionName": "今日余额查询",
      "description": null,
      "enabled": true,
      "configs": [
        {
          "id": 1,
          "configType": "REQUEST",
          "version": "v1",
          "name": "今日余额查询-请求",
          "description": null,
          "configContent": "{...}",
          "enabled": true
        },
        {
          "id": 2,
          "configType": "RESPONSE",
          "version": "v1",
          "name": "今日余额查询-响应",
          "description": null,
          "configContent": "{...}",
          "enabled": true
        }
      ]
    }
  ]
}
```

## 在其他项目中使用插件

### 快速开始

详细的调用示例请参考 [PLUGIN_USAGE_EXAMPLE.md](./PLUGIN_USAGE_EXAMPLE.md)

### 1. 添加依赖

在您的项目 `pom.xml` 中添加：

```xml
<dependency>
    <groupId>org.pf4j</groupId>
    <artifactId>pf4j</artifactId>
    <version>3.10.0</version>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.17.2</version>
</dependency>
```

### 2. 最简单的调用方式

```java
import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// 1. 创建插件管理器
PluginManager pluginManager = new DefaultPluginManager(Paths.get("plugins"));

// 2. 加载插件（从 ZIP 文件）
String pluginId = pluginManager.loadPlugin(Paths.get("bank-icbc-plugin.zip"));

// 3. 启动插件
pluginManager.startPlugin(pluginId);

// 4. 读取配置
Path pluginPath = pluginManager.getPlugin(pluginId).getPluginPath();
String configJson = Files.readString(pluginPath.resolve("bank-config.json"));

// 5. 解析配置
ObjectMapper mapper = new ObjectMapper();
BankPluginData data = mapper.readValue(configJson, BankPluginData.class);

// 6. 使用配置
System.out.println("银行: " + data.getBankInfo().getName());
```

### 3. 完整示例代码

请查看 [PLUGIN_USAGE_EXAMPLE.md](./PLUGIN_USAGE_EXAMPLE.md) 获取：
- 完整的数据模型定义
- 插件加载器封装类
- Spring Boot 集成示例
- 配置解析示例
- 常见问题解答

## 插件数据模型

### BankPluginData

```java
public class BankPluginData {
    private BankInfo bankInfo;
    private List<TransactionTypeData> transactionTypes;
}
```

### TransactionTypeData

```java
public class TransactionTypeData {
    private Long id;
    private String transactionName;
    private String description;
    private Boolean enabled;
    private List<ConfigData> configs;
}
```

### ConfigData

```java
public class ConfigData {
    private Long id;
    private String configType;  // REQUEST 或 RESPONSE
    private String version;
    private String name;
    private String description;
    private String configContent;  // JSON 格式的配置内容
    private Boolean enabled;
}
```

## 注意事项

1. **插件版本**：导出的插件版本固定为 1.0.0，如需更新版本，请重新导出
2. **配置内容**：`configContent` 字段包含完整的映射配置 JSON，需要解析后使用
3. **依赖关系**：插件不包含外部依赖，配置数据为纯 JSON 格式
4. **兼容性**：插件包符合 PF4J 3.x 规范，兼容 PF4J 3.0.0 及以上版本

## 故障排查

### 插件加载失败

- 检查 ZIP 文件是否完整
- 确认 `plugin.properties` 文件格式正确
- 检查插件 ID 是否唯一

### 配置读取失败

- 确认 `bank-config.json` 文件存在
- 检查 JSON 格式是否正确
- 确认 Jackson 版本兼容

### 插件启动失败

- 检查插件类路径是否正确
- 确认插件依赖是否满足
- 查看插件日志获取详细错误信息

