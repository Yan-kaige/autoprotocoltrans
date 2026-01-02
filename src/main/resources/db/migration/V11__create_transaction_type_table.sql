-- 创建交易类型表
CREATE TABLE IF NOT EXISTS `transaction_type` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `bank_id` BIGINT(20) NOT NULL COMMENT '银行ID',
    `transaction_name` VARCHAR(255) NOT NULL COMMENT '交易类型名称（如：今日余额查询、历史余额查询等）',
    `description` TEXT COMMENT '交易类型描述',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用（1:启用，0:禁用）',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    `update_time` DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_bank_id` (`bank_id`),
    KEY `idx_transaction_name` (`transaction_name`),
    KEY `idx_enabled` (`enabled`),
    UNIQUE KEY `uk_bank_transaction` (`bank_id`, `transaction_name`),
    CONSTRAINT `fk_transaction_bank` FOREIGN KEY (`bank_id`) REFERENCES `bank_info` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易类型表';

