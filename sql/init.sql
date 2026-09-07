-- =============================================
-- 线上投票小程序 - 初始化数据库脚本
-- 版本: V1.0
-- 说明: 建库 + 6张核心表（依赖顺序: user → vote → question → option → vote_record → ip_limit）
-- =============================================

-- 1. 建库
CREATE DATABASE IF NOT EXISTS vote_system DEFAULT CHARACTER SET utf8mb4;
USE vote_system;

-- 2. 用户表：登录即注册
CREATE TABLE IF NOT EXISTS `user` (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  openid VARCHAR(100) NOT NULL UNIQUE COMMENT '微信OpenID',
  nickname VARCHAR(50) COMMENT '昵称',
  avatar_url VARCHAR(500) COMMENT '头像URL',
  role VARCHAR(20) DEFAULT 'USER' COMMENT '角色:USER/ADMIN',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 3. 投票活动表
CREATE TABLE IF NOT EXISTS `vote` (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  title VARCHAR(200) NOT NULL COMMENT '投票标题',
  description TEXT COMMENT '活动说明',
  start_time DATETIME NOT NULL COMMENT '开始时间',
  end_time DATETIME NOT NULL COMMENT '结束时间',
  status TINYINT DEFAULT 0 COMMENT '状态:0草稿 1发布中 2已结束',
  creator_id BIGINT NOT NULL COMMENT '创建人ID',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX idx_status (status),
  INDEX idx_creator (creator_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投票活动表';

-- 4. 题目表
CREATE TABLE IF NOT EXISTS `question` (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  vote_id BIGINT NOT NULL COMMENT '关联投票ID',
  question_text TEXT NOT NULL COMMENT '题目内容',
  question_type TINYINT NOT NULL COMMENT '类型:1单选 2多选 3填空',
  is_required TINYINT DEFAULT 1 COMMENT '是否必填:0否 1是',
  sort_order INT DEFAULT 0 COMMENT '排序序号',
  INDEX idx_vote_id (vote_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目表';

-- 5. 选项表（仅单选/多选题使用）
CREATE TABLE IF NOT EXISTS `option` (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  question_id BIGINT NOT NULL COMMENT '关联题目ID',
  option_text VARCHAR(500) NOT NULL COMMENT '选项内容',
  sort_order INT DEFAULT 0 COMMENT '排序序号',
  INDEX idx_question_id (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='选项表';

-- 6. 投票记录表：唯一索引 = 防重复投票核心
CREATE TABLE IF NOT EXISTS `vote_record` (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  openid VARCHAR(100) NOT NULL COMMENT '用户OpenID',
  vote_id BIGINT NOT NULL COMMENT '关联投票ID',
  answer_content JSON NOT NULL COMMENT '答案内容:[{question_id, answer}]',
  ip_address VARCHAR(50) COMMENT '操作IP',
  submitted_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  UNIQUE KEY uk_user_vote (openid, vote_id),
  INDEX idx_vote_id (vote_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投票记录表';

-- 7. IP限流记录表
CREATE TABLE IF NOT EXISTS `ip_limit` (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
  ip_address VARCHAR(50) NOT NULL COMMENT 'IP地址',
  window_start DATETIME NOT NULL COMMENT '窗口开始时间',
  submit_count INT DEFAULT 0 COMMENT '窗口内提交次数',
  UNIQUE KEY uk_ip_window (ip_address, window_start)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IP限流记录表';

-- =============================================
-- 测试数据（可选，验证表结构可读性）
-- =============================================
INSERT INTO `user` (openid, nickname, role) VALUES
('test_openid_001', '测试管理员', 'ADMIN'),
('test_openid_002', '测试用户', 'USER');

INSERT INTO `vote` (title, description, start_time, end_time, status, creator_id) VALUES
('2026年度优秀员工评选', '每人限投一次，请认真选择', NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY), 1, 1);
SET @vote_id = LAST_INSERT_ID();

-- 第1题：单选
INSERT INTO `question` (vote_id, question_text, question_type, is_required, sort_order) VALUES
(@vote_id, '你支持哪位候选人当选优秀员工？', 1, 1, 1);
SET @q1 = LAST_INSERT_ID();
INSERT INTO `option` (question_id, option_text, sort_order) VALUES
(@q1, '张三', 1), (@q1, '李四', 2), (@q1, '王五', 3);

-- 第2题：多选
INSERT INTO `question` (vote_id, question_text, question_type, is_required, sort_order) VALUES
(@vote_id, '你认为该候选人具备哪些优秀特质？(可多选)', 2, 1, 2);
SET @q2 = LAST_INSERT_ID();
INSERT INTO `option` (question_id, option_text, sort_order) VALUES
(@q2, '责任心强', 1), (@q2, '专业能力突出', 2), (@q2, '团队协作好', 3);

-- 第3题：填空
INSERT INTO `question` (vote_id, question_text, question_type, is_required, sort_order) VALUES
(@vote_id, '请留下你对评选活动的建议', 3, 0, 3);

-- 一条示例投票记录（用于验证唯一索引）
INSERT INTO `vote_record` (openid, vote_id, answer_content, ip_address) VALUES
('test_openid_002', @vote_id,
 '[{"question_id":1,"answer":[1]},{"question_id":2,"answer":[1,2]},{"question_id":3,"answer":"很好"}]',
 '127.0.0.1');
