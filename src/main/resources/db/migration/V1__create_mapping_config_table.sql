-- 创建映射配置表
CREATE TABLE IF NOT EXISTS `mapping_config` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(255) NOT NULL COMMENT '配置名称（唯一）',
    `description` TEXT COMMENT '配置描述',
    `config_content` LONGTEXT NOT NULL COMMENT '配置内容（JSON格式）',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='映射配置表';



