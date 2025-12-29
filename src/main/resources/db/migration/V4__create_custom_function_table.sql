-- 创建自定义函数表
CREATE TABLE IF NOT EXISTS `custom_function` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(255) NOT NULL COMMENT '函数名称（唯一）',
    `code` VARCHAR(255) NOT NULL COMMENT '函数编码（唯一）',
    `description` TEXT COMMENT '函数描述',
    `script` LONGTEXT NOT NULL COMMENT 'Groovy脚本代码',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用（1:启用，0:禁用）',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`),
    UNIQUE KEY `uk_code` (`code`),
    KEY `idx_code` (`code`),
    KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自定义函数表';







