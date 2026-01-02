-- 重构映射配置表
-- 创建新的配置表结构
CREATE TABLE IF NOT EXISTS `mapping_config_v2` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `transaction_type_id` BIGINT(20) NOT NULL COMMENT '交易类型ID',
    `config_type` VARCHAR(20) NOT NULL COMMENT '配置类型：REQUEST（请求）或 RESPONSE（响应）',
    `version` VARCHAR(50) NOT NULL DEFAULT 'v1' COMMENT '配置版本（如：v1、v2等）',
    `name` VARCHAR(255) NOT NULL COMMENT '配置名称',
    `description` TEXT COMMENT '配置描述',
    `config_content` LONGTEXT NOT NULL COMMENT '配置内容（JSON格式）',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用（1:启用，0:禁用）',
    `is_current` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否为当前版本（1:是，0:否）',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_transaction_type_id` (`transaction_type_id`),
    KEY `idx_config_type` (`config_type`),
    KEY `idx_version` (`version`),
    KEY `idx_is_current` (`is_current`),
    UNIQUE KEY `uk_transaction_type_config_version` (`transaction_type_id`, `config_type`, `version`),
    CONSTRAINT `fk_config_transaction_type` FOREIGN KEY (`transaction_type_id`) REFERENCES `transaction_type` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='映射配置表（新结构）';

