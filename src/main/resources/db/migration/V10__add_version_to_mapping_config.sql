-- 为映射配置表添加版本字段
ALTER TABLE `mapping_config` 
ADD COLUMN `version` VARCHAR(50) NOT NULL DEFAULT 'v1' COMMENT '配置版本（如：v1、v2等）' AFTER `enabled`,
ADD INDEX `idx_version` (`version`),
ADD INDEX `idx_bank_request_version` (`bank_category`, `request_type`, `version`);



