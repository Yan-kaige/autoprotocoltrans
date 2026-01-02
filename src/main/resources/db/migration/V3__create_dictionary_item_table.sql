-- 创建字典项表
CREATE TABLE IF NOT EXISTS `dictionary_item` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `dictionary_id` BIGINT(20) NOT NULL COMMENT '字典ID（关联dictionary表）',
    `dict_key` VARCHAR(255) NOT NULL COMMENT '字典键（key）',
    `dict_value` VARCHAR(500) NOT NULL COMMENT '字典值（value）',
    `sort_order` INT(11) DEFAULT 0 COMMENT '排序序号',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_dictionary_id` (`dictionary_id`),
    KEY `idx_dict_key` (`dict_key`),
    CONSTRAINT `fk_dictionary_item_dictionary` FOREIGN KEY (`dictionary_id`) REFERENCES `dictionary` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字典项表';









