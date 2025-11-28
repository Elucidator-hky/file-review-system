-- ============================================
-- 文件审查系统 - 初始化测试数据
-- 创建时间：2025-11-28
-- ============================================

SET NAMES utf8mb4;

-- ============================================
-- 1. 插入平台超级管理员租户（租户ID=0，特殊租户）
-- ============================================
INSERT INTO `tenant` (`id`, `tenant_name`, `contact_name`, `contact_phone`, `storage_quota`, `user_quota`, `status`, `create_time`)
VALUES (0, '平台管理', '系统管理员', '00000000000', 0, 999, 1, NOW());

-- ============================================
-- 2. 插入平台超级管理员账号
-- 密码：admin123（BCrypt加密后）
-- ============================================
INSERT INTO `user` (`tenant_id`, `username`, `password`, `real_name`, `phone`, `roles`, `status`, `create_time`)
VALUES (
  0,
  'admin',
  '$2a$10$X5wFuJCKYE7Z8rYvJ3LpNOYKQqXYJ3QZqN8h3Kp2yZxKJy8XqP2YW',  -- 密码: admin123
  '系统管理员',
  '00000000000',
  'PLATFORM_ADMIN',
  1,
  NOW()
);

-- ============================================
-- 3. 插入测试租户
-- ============================================
INSERT INTO `tenant` (`tenant_name`, `contact_name`, `contact_phone`, `storage_quota`, `user_quota`, `status`, `create_time`)
VALUES
('测试企业A', '张经理', '13800138001', 107374182400, 50, 1, NOW()),  -- 100GB
('测试企业B', '李经理', '13800138002', 53687091200, 30, 1, NOW());   -- 50GB

-- ============================================
-- 4. 插入测试用户（测试企业A - tenant_id=1）
-- 所有密码均为：123456
-- ============================================
-- 企业管理员
INSERT INTO `user` (`tenant_id`, `username`, `password`, `real_name`, `phone`, `roles`, `status`)
VALUES (
  1,
  'admin_a',
  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',  -- 密码: 123456
  '企业管理员A',
  '13800138011',
  'TENANT_ADMIN',
  1
);

-- 审查员（单角色）
INSERT INTO `user` (`tenant_id`, `username`, `password`, `real_name`, `phone`, `roles`, `status`)
VALUES (
  1,
  'reviewer_a1',
  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',  -- 密码: 123456
  '审查员A1',
  '13800138012',
  'REVIEWER',
  1
);

-- 审查员（双角色：审查员+普通用户）
INSERT INTO `user` (`tenant_id`, `username`, `password`, `real_name`, `phone`, `roles`, `status`)
VALUES (
  1,
  'reviewer_a2',
  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',  -- 密码: 123456
  '审查员A2（双角色）',
  '13800138013',
  'REVIEWER,USER',
  1
);

-- 普通用户
INSERT INTO `user` (`tenant_id`, `username`, `password`, `real_name`, `phone`, `roles`, `status`)
VALUES (
  1,
  'user_a1',
  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',  -- 密码: 123456
  '普通用户A1',
  '13800138014',
  'USER',
  1
),
(
  1,
  'user_a2',
  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',  -- 密码: 123456
  '普通用户A2',
  '13800138015',
  'USER',
  1
);

-- ============================================
-- 5. 插入测试用户（测试企业B - tenant_id=2）
-- ============================================
-- 企业管理员
INSERT INTO `user` (`tenant_id`, `username`, `password`, `real_name`, `phone`, `roles`, `status`)
VALUES (
  2,
  'admin_b',
  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',  -- 密码: 123456
  '企业管理员B',
  '13800138021',
  'TENANT_ADMIN',
  1
);

-- 审查员
INSERT INTO `user` (`tenant_id`, `username`, `password`, `real_name`, `phone`, `roles`, `status`)
VALUES (
  2,
  'reviewer_b1',
  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',  -- 密码: 123456
  '审查员B1',
  '13800138022',
  'REVIEWER',
  1
);

-- 普通用户
INSERT INTO `user` (`tenant_id`, `username`, `password`, `real_name`, `phone`, `roles`, `status`)
VALUES (
  2,
  'user_b1',
  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',  -- 密码: 123456
  '普通用户B1',
  '13800138023',
  'USER',
  1
);

-- ============================================
-- 6. 更新租户用户数量统计
-- ============================================
UPDATE `tenant` SET `user_count` = 5 WHERE `id` = 1;
UPDATE `tenant` SET `user_count` = 3 WHERE `id` = 2;

-- ============================================
-- 测试账号说明
-- ============================================
-- 平台超级管理员：
--   用户名: admin
--   密码: admin123
--   角色: PLATFORM_ADMIN
--
-- 测试企业A（tenant_id=1）：
--   企业管理员: admin_a / 123456
--   审查员: reviewer_a1 / 123456 （单角色）
--   审查员: reviewer_a2 / 123456 （双角色：审查员+普通用户）
--   普通用户: user_a1 / 123456
--   普通用户: user_a2 / 123456
--
-- 测试企业B（tenant_id=2）：
--   企业管理员: admin_b / 123456
--   审查员: reviewer_b1 / 123456
--   普通用户: user_b1 / 123456
