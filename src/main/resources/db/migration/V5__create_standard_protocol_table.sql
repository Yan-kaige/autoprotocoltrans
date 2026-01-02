-- 创建标准协议表
CREATE TABLE IF NOT EXISTS `standard_protocol` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(255) NOT NULL COMMENT '协议名称（唯一）',
    `code` VARCHAR(255) NOT NULL COMMENT '协议编码（唯一）',
    `description` TEXT COMMENT '协议描述',
    `protocol_type` VARCHAR(20) NOT NULL COMMENT '协议类型（JSON/XML）',
    `data_format` LONGTEXT NOT NULL COMMENT '数据格式模板（JSON或XML字符串）',
    `category` VARCHAR(100) COMMENT '协议分类（如：用户信息、订单信息、支付信息等）',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用（1:启用，0:禁用）',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`),
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_code` (`code`),
    KEY `idx_protocol_type` (`protocol_type`),
    KEY `idx_category` (`category`),
    KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标准协议表';


