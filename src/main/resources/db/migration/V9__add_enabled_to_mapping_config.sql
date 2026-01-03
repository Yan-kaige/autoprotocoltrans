-- 为映射配置表添加启用状态字段
ALTER TABLE `mapping_config` 
ADD COLUMN `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用（1:启用，0:禁用）' AFTER `request_type`,
ADD INDEX `idx_enabled` (`enabled`);



