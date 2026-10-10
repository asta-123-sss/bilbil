-- =====================================================================
-- 智能广告投放 升级脚本 v3
-- 对已运行中的数据库执行：用户加积分余额 + 新建商家申请表 + 广告表
-- 执行方式（服务器 docker 内 mysql）：
--   docker exec -i bilbil-mysql mysql -uroot -p bilbil < sql/upgrade_v3.sql
-- =====================================================================

USE `bilbil`;

-- 1. 用户表加积分余额（商家投放广告，管理员可充值）
ALTER TABLE `user`
  ADD COLUMN `balance` BIGINT NOT NULL DEFAULT 0 COMMENT '积分余额（商家投放广告，管理员可充值）' AFTER `role`;

-- 2. 商家申请表
CREATE TABLE IF NOT EXISTS `merchant_apply` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '申请ID',
  `user_id`     BIGINT       NOT NULL                COMMENT '申请用户ID',
  `real_name`   VARCHAR(100) NOT NULL                COMMENT '姓名/公司名',
  `contact`     VARCHAR(100) NOT NULL                COMMENT '联系方式',
  `reason`      VARCHAR(500) NOT NULL                COMMENT '申请理由',
  `status`      TINYINT      NOT NULL DEFAULT 0      COMMENT '状态：0待审核 1通过 2拒绝',
  `review_note` VARCHAR(500) DEFAULT NULL            COMMENT '审核备注',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `review_time` DATETIME     DEFAULT NULL            COMMENT '审核时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商家申请表';

-- 3. 广告表
CREATE TABLE IF NOT EXISTS `ad` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '广告ID',
  `user_id`     BIGINT       NOT NULL                COMMENT '商家用户ID',
  `title`       VARCHAR(100) NOT NULL                COMMENT '广告标题',
  `cover_url`   VARCHAR(500) DEFAULT NULL            COMMENT '广告图',
  `target_url`  VARCHAR(500) NOT NULL                COMMENT '落地页链接',
  `category_id` BIGINT       NOT NULL                COMMENT '目标分区（兴趣定向）',
  `cpm`         BIGINT       NOT NULL DEFAULT 10     COMMENT '每千次曝光价格（积分）',
  `budget`      BIGINT       NOT NULL                COMMENT '总预算（积分）',
  `impressions` BIGINT       NOT NULL DEFAULT 0      COMMENT '累计曝光次数',
  `clicks`      BIGINT       NOT NULL DEFAULT 0      COMMENT '累计点击次数',
  `spent`       BIGINT       NOT NULL DEFAULT 0      COMMENT '已消耗积分',
  `status`      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1投放中 2已暂停 3已结束',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_category_status` (`category_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='广告表';
