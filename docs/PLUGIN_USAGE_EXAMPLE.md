# 银行配置插件调用示例

## 快速开始

### 1. 添加依赖

在您的项目 `pom.xml` 中添加：

```xml
<dependencies>
    <!-- PF4J 插件框架 -->
    <dependency>
        <groupId>org.pf4j</groupId>
        <artifactId>pf4j</artifactId>
        <version>3.10.0</version>
    </dependency>
    
    <!-- Jackson JSON 处理 -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>2.17.2</version>
    </dependency>
    <dependency>
        <groupId>com.fasterxml.jackson.datatype</groupId>
        <artifactId>jackson-datatype-jsr310</artifactId>
        <version>2.17.2</version>
    </dependency>
</dependencies>
```

### 2. 创建插件数据模型

创建与导出插件包中 JSON 结构对应的 Java 类：

```java
package com.example.plugin.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

/**
 * 银行插件数据模型
 */
@Data
public class BankPluginData {
    @JsonProperty("bankInfo")
    private BankInfoData bankInfo;
    
    @JsonProperty("transactionTypes")
    private List<TransactionTypeData> transactionTypes;
    
    @JsonProperty("configs")
    private List<ConfigData> configs; // 兼容字段，通常为空
}

/**
 * 银行信息
 */
@Data
public class BankInfoData {
    private Long id;
    private String name;
    private String code;
    private String description;
    private Boolean enabled;
}

/**
 * 交易类型数据
 */
@Data
public class TransactionTypeData {
    private Long id;
    private String transactionName;
    private String description;
    private Boolean enabled;
    private List<ConfigData> configs;
}

/**
 * 配置数据
 */
@Data
public class ConfigData {
    private Long id;
    private String configType; // REQUEST 或 RESPONSE
    private String version;
    private String name;
    private String description;
    private String configContent; // JSON 格式的配置内容
    private Boolean enabled;
}
```

### 3. 创建插件加载器

```java
package com.example.plugin;

import com.example.plugin.model.BankPluginData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * 银行配置插件加载器
 */
public class BankConfigPluginLoader {
    
    private final PluginManager pluginManager;
    private final ObjectMapper objectMapper;
    private final Map<String, BankPluginData> loadedPlugins;
    
    public BankConfigPluginLoader(String pluginsRoot) {
        this.pluginManager = new DefaultPluginManager(Paths.get(pluginsRoot));
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.loadedPlugins = new HashMap<>();
    }
    
    /**
     * 加载银行配置插件
     * 
     * @param pluginZipPath 插件 ZIP 文件路径
     * @return 插件ID
     * @throws Exception 加载失败时抛出异常
     */
    public String loadPlugin(String pluginZipPath) throws Exception {
        // 加载插件
        Path pluginPath = Paths.get(pluginZipPath);
        String pluginId = pluginManager.loadPlugin(pluginPath);
        
        // 启动插件
        pluginManager.startPlugin(pluginId);
        
        // 读取配置数据
        BankPluginData pluginData = readPluginConfig(pluginId);
        loadedPlugins.put(pluginId, pluginData);
        
        System.out.println("插件加载成功: " + pluginId);
        System.out.println("银行名称: " + pluginData.getBankInfo().getName());
        System.out.println("交易类型数量: " + pluginData.getTransactionTypes().size());
        
        return pluginId;
    }
    
    /**
     * 读取插件配置
     */
    private BankPluginData readPluginConfig(String pluginId) throws IOException {
        PluginWrapper plugin = pluginManager.getPlugin(pluginId);
        if (plugin == null) {
            throw new IllegalArgumentException("插件不存在: " + pluginId);
        }
        
        // 获取插件路径
        Path pluginPath = plugin.getPluginPath();
        
        // 读取 bank-config.json
        Path configFile = pluginPath.resolve("bank-config.json");
        if (!Files.exists(configFile)) {
            throw new IOException("配置文件不存在: " + configFile);
        }
        
        String configJson = Files.readString(configFile);
        return objectMapper.readValue(configJson, BankPluginData.class);
    }
    
    /**
     * 获取已加载的插件数据
     */
    public BankPluginData getPluginData(String pluginId) {
        return loadedPlugins.get(pluginId);
    }
    
    /**
     * 根据银行编码获取插件数据
     */
    public BankPluginData getPluginByBankCode(String bankCode) {
        return loadedPlugins.values().stream()
            .filter(data -> bankCode.equals(data.getBankInfo().getCode()))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * 卸载插件
     */
    public void unloadPlugin(String pluginId) {
        pluginManager.stopPlugin(pluginId);
        pluginManager.unloadPlugin(pluginId);
        loadedPlugins.remove(pluginId);
    }
    
    /**
     * 关闭插件管理器
     */
    public void shutdown() {
        pluginManager.stopPlugins();
        pluginManager.unloadPlugins();
    }
}
```

### 4. 使用示例

#### 示例 1: 基本加载和使用

```java
package com.example;

import com.example.plugin.BankConfigPluginLoader;
import com.example.plugin.model.BankPluginData;
import com.example.plugin.model.TransactionTypeData;
import com.example.plugin.model.ConfigData;

public class BankConfigExample {
    
    public static void main(String[] args) {
        // 创建插件加载器
        BankConfigPluginLoader loader = new BankConfigPluginLoader("plugins");
        
        try {
            // 加载插件
            String pluginId = loader.loadPlugin("plugins/bank-icbc-plugin.zip");
            
            // 获取插件数据
            BankPluginData pluginData = loader.getPluginData(pluginId);
            
            // 使用配置数据
            System.out.println("银行信息:");
            System.out.println("  名称: " + pluginData.getBankInfo().getName());
            System.out.println("  编码: " + pluginData.getBankInfo().getCode());
            
            // 遍历交易类型
            for (TransactionTypeData transactionType : pluginData.getTransactionTypes()) {
                System.out.println("\n交易类型: " + transactionType.getTransactionName());
                
                // 遍历配置
                for (ConfigData config : transactionType.getConfigs()) {
                    System.out.println("  配置类型: " + config.getConfigType());
                    System.out.println("  版本: " + config.getVersion());
                    System.out.println("  配置内容: " + config.getConfigContent());
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            loader.shutdown();
        }
    }
}
```

#### 示例 2: 根据银行编码查找配置

```java
package com.example;

import com.example.plugin.BankConfigPluginLoader;
import com.example.plugin.model.BankPluginData;
import com.example.plugin.model.TransactionTypeData;
import com.example.plugin.model.ConfigData;

public class BankConfigLookupExample {
    
    public static void main(String[] args) {
        BankConfigPluginLoader loader = new BankConfigPluginLoader("plugins");
        
        try {
            // 加载多个银行插件
            loader.loadPlugin("plugins/bank-icbc-plugin.zip");
            loader.loadPlugin("plugins/bank-ccb-plugin.zip");
            loader.loadPlugin("plugins/bank-abc-plugin.zip");
            
            // 根据银行编码查找
            String bankCode = "ICBC";
            BankPluginData icbcData = loader.getPluginByBankCode(bankCode);
            
            if (icbcData != null) {
                System.out.println("找到银行: " + icbcData.getBankInfo().getName());
                
                // 查找特定交易类型的配置
                String transactionName = "今日余额查询";
                TransactionTypeData transactionType = icbcData.getTransactionTypes().stream()
                    .filter(t -> transactionName.equals(t.getTransactionName()))
                    .findFirst()
                    .orElse(null);
                
                if (transactionType != null) {
                    // 获取请求配置
                    ConfigData requestConfig = transactionType.getConfigs().stream()
                        .filter(c -> "REQUEST".equals(c.getConfigType()))
                        .findFirst()
                        .orElse(null);
                    
                    if (requestConfig != null) {
                        System.out.println("请求配置版本: " + requestConfig.getVersion());
                        // 解析 configContent 使用
                        // ObjectMapper mapper = new ObjectMapper();
                        // YourConfigModel config = mapper.readValue(requestConfig.getConfigContent(), YourConfigModel.class);
                    }
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            loader.shutdown();
        }
    }
}
```

#### 示例 3: 在 Spring Boot 中使用

```java
package com.example.config;

import com.example.plugin.BankConfigPluginLoader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PreDestroy;
import java.io.File;
import java.nio.file.Paths;

@Configuration
public class PluginConfig {
    
    @Value("${plugin.root:plugins}")
    private String pluginRoot;
    
    private BankConfigPluginLoader pluginLoader;
    
    @Bean
    public BankConfigPluginLoader bankConfigPluginLoader() {
        pluginLoader = new BankConfigPluginLoader(pluginRoot);
        return pluginLoader;
    }
    
    @PreDestroy
    public void destroy() {
        if (pluginLoader != null) {
            pluginLoader.shutdown();
        }
    }
}
```

```java
package com.example.service;

import com.example.plugin.BankConfigPluginLoader;
import com.example.plugin.model.BankPluginData;
import com.example.plugin.model.TransactionTypeData;
import com.example.plugin.model.ConfigData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BankConfigService {
    
    @Autowired
    private BankConfigPluginLoader pluginLoader;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 获取银行的请求配置
     */
    public String getRequestConfig(String bankCode, String transactionName) {
        BankPluginData pluginData = pluginLoader.getPluginByBankCode(bankCode);
        if (pluginData == null) {
            return null;
        }
        
        TransactionTypeData transactionType = pluginData.getTransactionTypes().stream()
            .filter(t -> transactionName.equals(t.getTransactionName()))
            .findFirst()
            .orElse(null);
        
        if (transactionType == null) {
            return null;
        }
        
        ConfigData requestConfig = transactionType.getConfigs().stream()
            .filter(c -> "REQUEST".equals(c.getConfigType()))
            .findFirst()
            .orElse(null);
        
        return requestConfig != null ? requestConfig.getConfigContent() : null;
    }
    
    /**
     * 获取银行的响应配置
     */
    public String getResponseConfig(String bankCode, String transactionName) {
        BankPluginData pluginData = pluginLoader.getPluginByBankCode(bankCode);
        if (pluginData == null) {
            return null;
        }
        
        TransactionTypeData transactionType = pluginData.getTransactionTypes().stream()
            .filter(t -> transactionName.equals(t.getTransactionName()))
            .findFirst()
            .orElse(null);
        
        if (transactionType == null) {
            return null;
        }
        
        ConfigData responseConfig = transactionType.getConfigs().stream()
            .filter(c -> "RESPONSE".equals(c.getConfigType()))
            .findFirst()
            .orElse(null);
        
        return responseConfig != null ? responseConfig.getConfigContent() : null;
    }
}
```

#### 示例 4: 解析配置内容

`configContent` 字段包含的是 JSON 格式的映射配置，需要根据您的业务模型进行解析：

```java
package com.example.service;

import com.example.plugin.model.ConfigData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

public class ConfigParser {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 解析配置内容为 JSON 节点
     */
    public JsonNode parseConfig(ConfigData configData) throws Exception {
        return objectMapper.readTree(configData.getConfigContent());
    }
    
    /**
     * 解析配置内容为自定义模型
     */
    public <T> T parseConfig(ConfigData configData, Class<T> clazz) throws Exception {
        return objectMapper.readValue(configData.getConfigContent(), clazz);
    }
    
    /**
     * 获取配置中的特定字段
     */
    public String getConfigField(ConfigData configData, String fieldName) throws Exception {
        JsonNode root = objectMapper.readTree(configData.getConfigContent());
        JsonNode field = root.get(fieldName);
        return field != null ? field.asText() : null;
    }
}
```

### 5. 配置文件示例

#### application.yml

```yaml
plugin:
  root: plugins  # 插件根目录
  auto-load: true  # 是否自动加载插件
  plugins:
    - bank-icbc-plugin.zip
    - bank-ccb-plugin.zip
    - bank-abc-plugin.zip
```

### 6. 完整项目结构示例

```
your-project/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── example/
│       │           ├── model/          # 数据模型
│       │           │   ├── BankPluginData.java
│       │           │   ├── BankInfoData.java
│       │           │   ├── TransactionTypeData.java
│       │           │   └── ConfigData.java
│       │           ├── plugin/         # 插件加载器
│       │           │   └── BankConfigPluginLoader.java
│       │           ├── service/       # 业务服务
│       │           │   ├── BankConfigService.java
│       │           │   └── ConfigParser.java
│       │           └── config/         # Spring 配置
│       │               └── PluginConfig.java
│       └── resources/
│           └── application.yml
├── plugins/                            # 插件目录
│   ├── bank-icbc-plugin.zip
│   ├── bank-ccb-plugin.zip
│   └── bank-abc-plugin.zip
└── pom.xml
```

## 常见问题

### Q1: 如何动态加载新插件？

```java
// 监听插件目录变化，自动加载新插件
public void watchPluginDirectory() {
    // 使用 WatchService 或定时任务检查新插件
    // 发现新插件后调用 loader.loadPlugin()
}
```

### Q2: 如何热更新插件？

```java
// 卸载旧插件，加载新插件
loader.unloadPlugin(pluginId);
loader.loadPlugin(newPluginPath);
```

### Q3: 插件加载失败怎么办？

```java
try {
    String pluginId = loader.loadPlugin(pluginPath);
} catch (Exception e) {
    log.error("插件加载失败: " + pluginPath, e);
    // 处理错误，可以记录日志或发送告警
}
```

### Q4: 如何验证插件完整性？

```java
// 检查必要文件是否存在
Path pluginPath = plugin.getPluginPath();
if (!Files.exists(pluginPath.resolve("plugin.properties"))) {
    throw new IOException("插件描述文件缺失");
}
if (!Files.exists(pluginPath.resolve("bank-config.json"))) {
    throw new IOException("配置文件缺失");
}
```

## 注意事项

1. **插件路径**: 确保插件 ZIP 文件路径正确
2. **资源释放**: 使用完毕后记得调用 `shutdown()` 释放资源
3. **异常处理**: 妥善处理插件加载和读取过程中的异常
4. **版本管理**: 注意插件版本，避免加载不兼容的版本
5. **线程安全**: 如果多线程访问，注意同步控制

