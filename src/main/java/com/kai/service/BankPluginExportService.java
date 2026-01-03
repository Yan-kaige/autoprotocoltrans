package com.kai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kai.model.BankInfo;
import com.kai.model.MappingConfigV2;
import com.kai.model.TransactionType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 银行插件导出服务
 * 将银行的配置导出为 PF4J 插件格式的 ZIP 包
 */
@Service
@Slf4j
public class BankPluginExportService {
    
    private final ObjectMapper objectMapper;
    
    public BankPluginExportService() {
        this.objectMapper = new ObjectMapper();
        // 注册 Java 8 时间模块
        this.objectMapper.registerModule(new JavaTimeModule());
        // 禁用将日期写为时间戳
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
    
    @Autowired
    private BankInfoService bankInfoService;
    
    @Autowired
    private TransactionTypeService transactionTypeService;
    
    @Autowired
    private MappingConfigV2Service mappingConfigV2Service;
    
    /**
     * 导出银行配置为 PF4J 插件包
     * 
     * @param bankId 银行ID
     * @return ZIP 文件的字节数组
     * @throws Exception 导出失败时抛出异常
     */
    public byte[] exportBankPlugin(Long bankId) throws Exception {
        // 获取银行信息
        BankInfo bank = bankInfoService.getById(bankId);
        if (bank == null) {
            throw new IllegalArgumentException("银行信息不存在，ID: " + bankId);
        }
        
        // 获取该银行的所有交易类型
        List<TransactionType> transactionTypes = transactionTypeService.getByBankId(bankId);
        
        // 收集所有配置数据（创建简化的银行信息，排除时间字段）
        BankPluginData pluginData = new BankPluginData();
        BankInfoData bankData = new BankInfoData();
        bankData.setId(bank.getId());
        bankData.setName(bank.getName());
        bankData.setCode(bank.getCode());
        bankData.setDescription(bank.getDescription());
        bankData.setEnabled(bank.getEnabled());
        pluginData.setBankInfo(bankData);
        pluginData.setTransactionTypes(new ArrayList<>());
        pluginData.setConfigs(new ArrayList<>());
        
        // 为每个交易类型收集配置
        for (TransactionType transactionType : transactionTypes) {
            TransactionTypeData typeData = new TransactionTypeData();
            typeData.setId(transactionType.getId());
            typeData.setTransactionName(transactionType.getTransactionName());
            typeData.setDescription(transactionType.getDescription());
            typeData.setEnabled(transactionType.getEnabled());
            
            // 获取当前版本的请求和响应配置
            MappingConfigV2 requestConfig = mappingConfigV2Service.getCurrentConfig(
                transactionType.getId(), "REQUEST"
            );
            MappingConfigV2 responseConfig = mappingConfigV2Service.getCurrentConfig(
                transactionType.getId(), "RESPONSE"
            );
            
            if (requestConfig != null) {
                ConfigData requestData = new ConfigData();
                requestData.setId(requestConfig.getId());
                requestData.setConfigType("REQUEST");
                requestData.setVersion(requestConfig.getVersion());
                requestData.setName(requestConfig.getName());
                requestData.setDescription(requestConfig.getDescription());
                requestData.setConfigContent(requestConfig.getConfigContent());
                requestData.setEnabled(requestConfig.getEnabled());
                typeData.getConfigs().add(requestData);
            }
            
            if (responseConfig != null) {
                ConfigData responseData = new ConfigData();
                responseData.setId(responseConfig.getId());
                responseData.setConfigType("RESPONSE");
                responseData.setVersion(responseConfig.getVersion());
                responseData.setName(responseConfig.getName());
                responseData.setDescription(responseConfig.getDescription());
                responseData.setConfigContent(responseConfig.getConfigContent());
                responseData.setEnabled(responseConfig.getEnabled());
                typeData.getConfigs().add(responseData);
            }
            
            pluginData.getTransactionTypes().add(typeData);
        }
        
        // 生成插件包
        return generatePluginZip(bank, pluginData);
    }
    
    /**
     * 生成 PF4J 插件 ZIP 包
     */
    private byte[] generatePluginZip(BankInfo bank, BankPluginData pluginData) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try (ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
            // 1. 生成 plugin.properties（PF4J 插件描述文件）
            String pluginId = "bank-" + bank.getCode().toLowerCase().replaceAll("[^a-z0-9]", "-");
            String pluginVersion = "1.0.0";
            String pluginDescription = bank.getName() + " 报文转换配置插件";
            
            String pluginProperties = generatePluginProperties(
                pluginId, 
                pluginVersion, 
                pluginDescription,
                bank
            );
            addZipEntry(zos, "plugin.properties", pluginProperties);
            
            // 2. 生成 bank-config.json（配置数据文件）
            String configJson = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(pluginData);
            addZipEntry(zos, "bank-config.json", configJson);
            
            // 3. 生成 README.md（使用说明）
            String readme = generateReadme(bank, pluginId, pluginVersion);
            addZipEntry(zos, "README.md", readme);
            
            // 4. 生成 MANIFEST.MF（可选，用于 JAR 包）
            String manifest = generateManifest(pluginId, pluginVersion, bank.getName());
            addZipEntry(zos, "META-INF/MANIFEST.MF", manifest);
        }
        
        return baos.toByteArray();
    }
    
    /**
     * 生成 PF4J plugin.properties 文件内容
     */
    private String generatePluginProperties(String pluginId, String version, 
                                           String description, BankInfo bank) {
        StringBuilder sb = new StringBuilder();
        sb.append("# PF4J Plugin Descriptor\n");
        sb.append("# Generated by AutoProtocolTrans\n");
        sb.append("# Export Time: ").append(LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");
        
        sb.append("plugin.id=").append(pluginId).append("\n");
        sb.append("plugin.version=").append(version).append("\n");
        sb.append("plugin.description=").append(description).append("\n");
        sb.append("plugin.provider=").append("AutoProtocolTrans").append("\n");
        sb.append("plugin.class=").append("com.kai.plugin.BankConfigPlugin").append("\n");
        sb.append("plugin.requires=*").append("\n");
        sb.append("\n");
        sb.append("# Bank Information\n");
        sb.append("bank.id=").append(bank.getId()).append("\n");
        sb.append("bank.name=").append(bank.getName()).append("\n");
        sb.append("bank.code=").append(bank.getCode()).append("\n");
        sb.append("bank.enabled=").append(bank.getEnabled()).append("\n");
        
        return sb.toString();
    }
    
    /**
     * 生成 README.md 文件内容
     */
    private String generateReadme(BankInfo bank, String pluginId, String version) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(bank.getName()).append(" 报文转换配置插件\n\n");
        sb.append("## 插件信息\n\n");
        sb.append("- **插件ID**: ").append(pluginId).append("\n");
        sb.append("- **版本**: ").append(version).append("\n");
        sb.append("- **银行名称**: ").append(bank.getName()).append("\n");
        sb.append("- **银行编码**: ").append(bank.getCode()).append("\n");
        sb.append("- **导出时间**: ").append(LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");
        
        sb.append("## 使用说明\n\n");
        sb.append("### 1. 安装插件\n\n");
        sb.append("将本 ZIP 包解压到 PF4J 插件目录，或直接使用 ZIP 包作为插件。\n\n");
        
        sb.append("### 2. 加载配置\n\n");
        sb.append("在您的项目中使用 PF4J 加载此插件：\n\n");
        sb.append("```java\n");
        sb.append("PluginManager pluginManager = new DefaultPluginManager();\n");
        sb.append("pluginManager.loadPlugin(Paths.get(\"path/to/plugin.zip\"));\n");
        sb.append("pluginManager.startPlugin(\"").append(pluginId).append("\");\n");
        sb.append("```\n\n");
        
        sb.append("### 3. 读取配置\n\n");
        sb.append("从插件中读取 `bank-config.json` 文件获取配置数据。\n\n");
        
        sb.append("## 配置内容\n\n");
        sb.append("本插件包含以下配置：\n\n");
        sb.append("- 银行基本信息\n");
        sb.append("- 交易类型配置\n");
        sb.append("- 请求和响应映射配置\n\n");
        
        sb.append("## 文件结构\n\n");
        sb.append("```\n");
        sb.append("plugin.zip\n");
        sb.append("├── plugin.properties    # PF4J 插件描述文件\n");
        sb.append("├── bank-config.json      # 银行配置数据\n");
        sb.append("├── README.md             # 本文件\n");
        sb.append("└── META-INF/\n");
        sb.append("    └── MANIFEST.MF       # JAR 清单文件\n");
        sb.append("```\n\n");
        
        sb.append("## 注意事项\n\n");
        sb.append("- 请确保您的项目已集成 PF4J 框架\n");
        sb.append("- 配置数据以 JSON 格式存储，可直接解析使用\n");
        sb.append("- 建议在生产环境使用前进行充分测试\n");
        
        return sb.toString();
    }
    
    /**
     * 生成 MANIFEST.MF 文件内容
     */
    private String generateManifest(String pluginId, String version, String bankName) {
        StringBuilder sb = new StringBuilder();
        sb.append("Manifest-Version: 1.0\n");
        sb.append("Plugin-Id: ").append(pluginId).append("\n");
        sb.append("Plugin-Version: ").append(version).append("\n");
        sb.append("Plugin-Description: ").append(bankName).append(" 报文转换配置插件\n");
        sb.append("Created-By: AutoProtocolTrans\n");
        sb.append("Export-Time: ").append(LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        
        return sb.toString();
    }
    
    /**
     * 添加 ZIP 条目
     */
    private void addZipEntry(ZipOutputStream zos, String entryName, String content) throws IOException {
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }
    
    /**
     * 银行插件数据模型
     */
    @Data
    public static class BankPluginData {
        private BankInfoData bankInfo;
        private List<TransactionTypeData> transactionTypes;
        private List<ConfigData> configs; // 兼容旧格式，实际使用 transactionTypes 中的 configs
    }
    
    /**
     * 银行信息数据模型（简化版，不包含时间字段）
     */
    @Data
    public static class BankInfoData {
        private Long id;
        private String name;
        private String code;
        private String description;
        private Boolean enabled;
    }
    
    /**
     * 交易类型数据模型
     */
    @Data
    public static class TransactionTypeData {
        private Long id;
        private String transactionName;
        private String description;
        private Boolean enabled;
        private List<ConfigData> configs = new ArrayList<>();
    }
    
    /**
     * 配置数据模型
     */
    @Data
    public static class ConfigData {
        private Long id;
        private String configType; // REQUEST 或 RESPONSE
        private String version;
        private String name;
        private String description;
        private String configContent; // JSON 格式的配置内容
        private Boolean enabled;
    }
}

