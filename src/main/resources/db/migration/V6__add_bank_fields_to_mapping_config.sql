-- 为映射配置表添加银行相关字段
ALTER TABLE `mapping_config` 
ADD COLUMN `bank_category` VARCHAR(100) COMMENT '银行类别' AFTER `description`,
ADD COLUMN `transaction_name` VARCHAR(255) COMMENT '交易名称' AFTER `bank_category`,
ADD COLUMN `request_type` VARCHAR(100) COMMENT '请求类型（如：今日余额查询、历史余额查询等）' AFTER `transaction_name`,
ADD INDEX `idx_bank_category` (`bank_category`),
ADD INDEX `idx_transaction_name` (`transaction_name`),
ADD INDEX `idx_request_type` (`request_type`);



