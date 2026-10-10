-- =====================================================================
-- 仿B站视频平台 (bilbil) 第二阶段升级脚本
-- 用于在「已建库」的环境上增量升级，新增：投币 / 观看历史 / 私信 / 后台审核
-- 执行方式：mysql -uroot -p123456 bilbil < sql/upgrade_v2.sql
-- =====================================================================

USE `bilbil`;

-- 1. 用户表新增角色列（0普通 1管理员）
ALTER TABLE `user`
  ADD COLUMN `role` TINYINT NOT NULL DEFAULT 0 COMMENT '角色：0普通 1管理员' AFTER `status`;

-- 2. 视频表新增投币数（Redis 缓存，异步回写）
ALTER TABLE `video`
  ADD COLUMN `coin_count` BIGINT NOT NULL DEFAULT 0 COMMENT '投币数（Redis 缓存，异步回写）' AFTER `collect_count`;

-- 3. 投币关系表（用户-视频 是否已投币，用于 Redis 集合回填与 coined 状态）
DROP TABLE IF EXISTS `video_coin`;
CREATE TABLE `video_coin` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT   NOT NULL                COMMENT '投币用户ID',
  `video_id`    BIGINT   NOT NULL                COMMENT '被投币视频ID',
  `status`      TINYINT  NOT NULL DEFAULT 1      COMMENT '状态：0已取消 1已投币',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '投币时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_video` (`user_id`, `video_id`),
  KEY `idx_video_id` (`video_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频投币表';

-- 4. 观看历史表（同一用户+视频只保留一条，重复观看刷新时间与进度）
DROP TABLE IF EXISTS `watch_history`;
CREATE TABLE `watch_history` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT   NOT NULL                COMMENT '用户ID',
  `video_id`    BIGINT   NOT NULL                COMMENT '视频ID',
  `progress`    INT      NOT NULL DEFAULT 0      COMMENT '上次看到第几秒',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次观看时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近观看时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_video` (`user_id`, `video_id`),
  KEY `idx_user_time` (`user_id`, `update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='观看历史表';

-- 5. 私信表（点对点）
DROP TABLE IF EXISTS `private_message`;
CREATE TABLE `private_message` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '私信ID',
  `from_user_id` BIGINT       NOT NULL                COMMENT '发送者用户ID',
  `to_user_id`   BIGINT       NOT NULL                COMMENT '接收者用户ID',
  `content`      VARCHAR(500) NOT NULL                COMMENT '私信内容',
  `is_read`      TINYINT      NOT NULL DEFAULT 0      COMMENT '是否已读：0未读 1已读',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (`id`),
  KEY `idx_from` (`from_user_id`),
  KEY `idx_to` (`to_user_id`),
  KEY `idx_to_read` (`to_user_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='私信表';

-- 6. 提升某账号为管理员（示例：把 username='admin' 的用户设为管理员）
-- 若无该账号，可先注册一个普通账号，再执行下面这条语句将其提升为管理员。
UPDATE `user` SET `role` = 1 WHERE `username` = 'admin';
