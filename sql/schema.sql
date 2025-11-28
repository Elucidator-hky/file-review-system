-- ============================================
-- 文件审查系统 - 数据库表结构
-- 创建时间：2025-11-28
-- 数据库：file_review_system
-- ============================================

-- 设置字符集
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================
-- 1. 租户表 (tenant)
-- ============================================
DROP TABLE IF EXISTS `tenant`;
CREATE TABLE `tenant` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '租户ID',
  `tenant_name` VARCHAR(100) NOT NULL COMMENT '企业名称',
  `contact_name` VARCHAR(50) NOT NULL COMMENT '联系人姓名',
  `contact_phone` VARCHAR(20) NOT NULL COMMENT '联系电话',
  `storage_quota` BIGINT NOT NULL DEFAULT 107374182400 COMMENT '存储配额（字节，默认100GB）',
  `storage_used` BIGINT NOT NULL DEFAULT 0 COMMENT '已使用存储（字节）',
  `user_quota` INT NOT NULL DEFAULT 50 COMMENT '用户数量配额',
  `user_count` INT NOT NULL DEFAULT 0 COMMENT '已创建用户数量',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-正常，0-停用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_name` (`tenant_name`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户表';

-- ============================================
-- 2. 用户表 (user)
-- ============================================
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(100) NOT NULL COMMENT '密码（BCrypt加密）',
  `real_name` VARCHAR(50) NOT NULL COMMENT '真实姓名',
  `phone` VARCHAR(20) NOT NULL COMMENT '手机号',
  `roles` VARCHAR(100) NOT NULL COMMENT '角色列表，逗号分隔（PLATFORM_ADMIN/TENANT_ADMIN/REVIEWER/USER）',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-正常，0-停用',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_username` (`tenant_id`, `username`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================
-- 3. 审查任务表 (review_task)
-- ============================================
DROP TABLE IF EXISTS `review_task`;
CREATE TABLE `review_task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
  `task_name` VARCHAR(100) NOT NULL COMMENT '任务名称',
  `creator_id` BIGINT NOT NULL COMMENT '创建者ID',
  `reviewer_id` BIGINT NOT NULL COMMENT '审查员ID',
  `current_version` INT NOT NULL DEFAULT 1 COMMENT '当前版本号',
  `current_status` VARCHAR(20) NOT NULL DEFAULT 'REVIEWING' COMMENT '当前状态：REVIEWING-审查中，APPROVED-已通过，REJECTED-已打回',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_task_name` (`tenant_id`, `task_name`),
  KEY `idx_creator_id` (`creator_id`),
  KEY `idx_reviewer_id` (`reviewer_id`),
  KEY `idx_current_status` (`current_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审查任务表';

-- ============================================
-- 4. 审查版本表 (review_version)
-- ============================================
DROP TABLE IF EXISTS `review_version`;
CREATE TABLE `review_version` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '版本ID',
  `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
  `task_id` BIGINT NOT NULL COMMENT '任务ID',
  `version_number` INT NOT NULL COMMENT '版本号（v1、v2、v3...）',
  `submit_desc` TEXT COMMENT '提交说明',
  `status` VARCHAR(20) NOT NULL DEFAULT 'REVIEWING' COMMENT '版本状态：REVIEWING-审查中，APPROVED-已通过，REJECTED-已打回',
  `review_result` VARCHAR(20) DEFAULT NULL COMMENT '审查结果：APPROVED-通过，REJECTED-打回',
  `review_comment` TEXT COMMENT '审查意见',
  `reviewer_id` BIGINT DEFAULT NULL COMMENT '审查人ID',
  `review_time` DATETIME DEFAULT NULL COMMENT '审查时间',
  `files_ready` TINYINT NOT NULL DEFAULT 1 COMMENT '文件是否就绪：0-复制中，1-已就绪（用于异步复制）',
  `file_count` INT NOT NULL DEFAULT 0 COMMENT '文件数量',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_version` (`task_id`, `version_number`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_status` (`status`),
  KEY `idx_reviewer_id` (`reviewer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审查版本表';

-- ============================================
-- 5. 审查文件表 (review_file)
-- ============================================
DROP TABLE IF EXISTS `review_file`;
CREATE TABLE `review_file` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '文件ID',
  `tenant_id` BIGINT NOT NULL COMMENT '租户ID',
  `version_id` BIGINT NOT NULL COMMENT '版本ID',
  `file_name` VARCHAR(255) NOT NULL COMMENT '原始文件名（如：财务报表.pdf）',
  `minio_object_name` VARCHAR(500) NOT NULL COMMENT 'MinIO对象名（格式：tenant_id/md5）',
  `file_md5` VARCHAR(32) NOT NULL COMMENT '文件MD5值',
  `file_size` BIGINT NOT NULL COMMENT '文件大小（字节）',
  `file_type` VARCHAR(100) NOT NULL DEFAULT 'application/pdf' COMMENT '文件类型',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_version_filename` (`version_id`, `file_name`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_file_md5` (`tenant_id`, `file_md5`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审查文件表';

-- ============================================
-- 索引说明
-- ============================================
-- tenant表：
--   - uk_tenant_name: 企业名称全局唯一
--   - idx_status: 按状态筛选租户
--
-- user表：
--   - uk_tenant_username: 同一租户下用户名唯一
--   - idx_tenant_id: 租户数据隔离查询
--   - idx_status: 按状态筛选用户
--
-- review_task表：
--   - uk_tenant_task_name: 同一租户下任务名唯一
--   - idx_creator_id: 查询我的任务
--   - idx_reviewer_id: 查询待审查任务
--   - idx_current_status: 按状态筛选任务
--
-- review_version表：
--   - uk_task_version: 同一任务下版本号唯一
--   - idx_tenant_id: 租户数据隔离
--   - idx_status: 按状态筛选版本
--   - idx_reviewer_id: 查询审查人的审查记录
--
-- review_file表：
--   - uk_version_filename: 同一版本下文件名唯一
--   - idx_tenant_id: 租户数据隔离
--   - idx_file_md5: MD5去重查询（秒传功能）

SET FOREIGN_KEY_CHECKS = 1;
