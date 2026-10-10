-- =====================================================================
-- 仿B站视频平台 (bilbil) 数据库初始化脚本
-- 数据库: bilbil
-- 字符集: utf8mb4 / utf8mb4_general_ci
-- MySQL: 8.0+
-- 说明: 点赞数、收藏数、播放量等计数不直接维护在业务主表，
--       而是通过 Redis 缓存计数 + 定时/异步批量回写数据库实现最终一致。
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `bilbil`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;

USE `bilbil`;

-- ---------------------------------------------------------------------
-- 1. 用户表 user
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username`    VARCHAR(50)  NOT NULL                COMMENT '用户名（登录账号，唯一）',
  `password`    VARCHAR(100) NOT NULL                COMMENT '密码（BCrypt 加密存储）',
  `nickname`    VARCHAR(50)  DEFAULT NULL            COMMENT '昵称',
  `avatar`      VARCHAR(500) DEFAULT NULL            COMMENT '头像 URL',
  `gender`      TINYINT      NOT NULL DEFAULT 0      COMMENT '性别：0未知 1男 2女',
  `birthday`    DATE         DEFAULT NULL            COMMENT '生日',
  `signature`   VARCHAR(200) DEFAULT NULL            COMMENT '个性签名',
  `email`       VARCHAR(100) DEFAULT NULL            COMMENT '邮箱',
  `phone`       VARCHAR(20)  DEFAULT NULL            COMMENT '手机号',
  `status`      TINYINT      NOT NULL DEFAULT 0      COMMENT '账号状态：0正常 1封禁',
  `role`        TINYINT      NOT NULL DEFAULT 0      COMMENT '角色：0普通 1管理员 2商家',
  `balance`     BIGINT       NOT NULL DEFAULT 0      COMMENT '积分余额（商家投放广告，管理员可充值）',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_email` (`email`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ---------------------------------------------------------------------
-- 2. 视频分区表 video_category
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `video_category`;
CREATE TABLE `video_category` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '分区ID',
  `name`        VARCHAR(50)  NOT NULL                COMMENT '分区名称（如：游戏、生活、科技）',
  `description` VARCHAR(200) DEFAULT NULL            COMMENT '分区描述',
  `sort`        INT          NOT NULL DEFAULT 0      COMMENT '排序权重（越小越靠前）',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频分区表';

-- ---------------------------------------------------------------------
-- 3. 视频主表 video
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `video`;
CREATE TABLE `video` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '视频ID',
  `user_id`       BIGINT       NOT NULL                COMMENT '投稿用户ID（UP主）',
  `title`         VARCHAR(100) NOT NULL                COMMENT '视频标题',
  `description`   VARCHAR(1000) DEFAULT NULL           COMMENT '视频简介',
  `category_id`   BIGINT       NOT NULL                COMMENT '所属分区ID',
  `tags`          VARCHAR(500)  DEFAULT NULL           COMMENT '标签（逗号分隔）',
  `cover_url`     VARCHAR(500)  DEFAULT NULL           COMMENT '封面图 URL',
  `video_url`     VARCHAR(500)  DEFAULT NULL           COMMENT '视频访问 URL（Nginx 反向代理地址）',
  `file_id`       VARCHAR(255)  DEFAULT NULL           COMMENT '视频源文件存储ID（FastDFS group/name 或本地文件名）',
  `file_md5`      VARCHAR(64)   DEFAULT NULL           COMMENT '源文件 MD5（用于秒传去重）',
  `file_name`     VARCHAR(255)  DEFAULT NULL           COMMENT '原始文件名',
  `file_size`     BIGINT        DEFAULT 0             COMMENT '文件大小（字节）',
  `duration`      INT           DEFAULT 0             COMMENT '视频时长（秒）',
  `visibility`    TINYINT      NOT NULL DEFAULT 0      COMMENT '可见权限：0公开 1仅粉丝 2私密',
  `status`        TINYINT      NOT NULL DEFAULT 0      COMMENT '视频状态：0待审核 1已发布 2已下架 3转码中',
  `play_count`    BIGINT       NOT NULL DEFAULT 0      COMMENT '播放量（Redis 缓存，异步回写）',
  `like_count`    BIGINT       NOT NULL DEFAULT 0      COMMENT '点赞数（Redis 缓存，异步回写）',
  `collect_count` BIGINT       NOT NULL DEFAULT 0      COMMENT '收藏数（Redis 缓存，异步回写）',
  `coin_count`    BIGINT       NOT NULL DEFAULT 0      COMMENT '投币数（Redis 缓存，异步回写）',
  `comment_count` BIGINT       NOT NULL DEFAULT 0      COMMENT '评论数',
  `danmaku_count` BIGINT       NOT NULL DEFAULT 0      COMMENT '弹幕数',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '投稿时间',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `publish_time`  DATETIME     DEFAULT NULL            COMMENT '发布时间（审核通过）',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_status_create` (`status`, `create_time`),
  KEY `idx_file_md5` (`file_md5`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频主表';

-- ---------------------------------------------------------------------
-- 4. 视频分片表 video_chunk（分块上传/断点续传核心表）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `video_chunk`;
CREATE TABLE `video_chunk` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '分片记录ID',
  `upload_id`   VARCHAR(64) NOT NULL                COMMENT '分片上传会话标识（一次分片上传任务唯一）',
  `video_id`    BIGINT      DEFAULT NULL            COMMENT '合并完成后关联的视频ID（未合并前为NULL）',
  `chunk_index` INT         NOT NULL                COMMENT '分片序号（从0开始）',
  `chunk_size`  BIGINT      NOT NULL DEFAULT 0      COMMENT '分片大小（字节）',
  `chunk_md5`   VARCHAR(64) DEFAULT NULL            COMMENT '分片 MD5（用于校验完整性）',
  `file_id`     VARCHAR(255) NOT NULL               COMMENT '分片文件存储ID（FastDFS 或本地文件名）',
  `status`      TINYINT     NOT NULL DEFAULT 0      COMMENT '状态：0已上传 1已合并 2已清理',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_upload_chunk` (`upload_id`, `chunk_index`),
  KEY `idx_video_id` (`video_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频分片表（分块上传/断点续传）';

-- ---------------------------------------------------------------------
-- 5. 用户关注表 user_follow
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `user_follow`;
CREATE TABLE `user_follow` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT   NOT NULL                COMMENT '关注者用户ID',
  `follow_id`   BIGINT   NOT NULL                COMMENT '被关注者用户ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_follow` (`user_id`, `follow_id`),
  KEY `idx_follow_id` (`follow_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户关注表';

-- ---------------------------------------------------------------------
-- 6. 视频点赞表 video_like
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `video_like`;
CREATE TABLE `video_like` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT   NOT NULL                COMMENT '点赞用户ID',
  `video_id`    BIGINT   NOT NULL                COMMENT '被点赞视频ID',
  `status`      TINYINT  NOT NULL DEFAULT 1      COMMENT '状态：0已取消 1已点赞',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_video` (`user_id`, `video_id`),
  KEY `idx_video_id` (`video_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频点赞表';

-- ---------------------------------------------------------------------
-- 7. 视频收藏表 video_collect
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `video_collect`;
CREATE TABLE `video_collect` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT   NOT NULL                COMMENT '收藏用户ID',
  `video_id`    BIGINT   NOT NULL                COMMENT '被收藏视频ID',
  `status`      TINYINT  NOT NULL DEFAULT 1      COMMENT '状态：0已取消 1已收藏',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_video` (`user_id`, `video_id`),
  KEY `idx_video_id` (`video_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频收藏表';

-- ---------------------------------------------------------------------
-- 8. 评论表 comment（支持子评论/回复）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `comment`;
CREATE TABLE `comment` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `video_id`      BIGINT        NOT NULL                COMMENT '所属视频ID',
  `user_id`       BIGINT        NOT NULL                COMMENT '评论用户ID',
  `parent_id`     BIGINT        NOT NULL DEFAULT 0      COMMENT '父评论ID（0=一级评论）',
  `root_id`       BIGINT        NOT NULL DEFAULT 0      COMMENT '根评论ID（用于回复的回复，指向一级评论）',
  `reply_user_id` BIGINT        DEFAULT NULL            COMMENT '被回复的用户ID（@某人）',
  `content`       VARCHAR(1000) NOT NULL                COMMENT '评论内容',
  `like_count`    INT           NOT NULL DEFAULT 0      COMMENT '评论点赞数',
  `status`        TINYINT       NOT NULL DEFAULT 0      COMMENT '状态：0正常 1已删除',
  `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
  PRIMARY KEY (`id`),
  KEY `idx_video_parent` (`video_id`, `parent_id`),
  KEY `idx_root_id` (`root_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表（支持回复）';

-- ---------------------------------------------------------------------
-- 9. 弹幕表 danmaku
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `danmaku`;
CREATE TABLE `danmaku` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '弹幕ID',
  `video_id`    BIGINT       NOT NULL                COMMENT '所属视频ID',
  `user_id`     BIGINT       NOT NULL                COMMENT '发送弹幕用户ID',
  `content`     VARCHAR(500) NOT NULL                COMMENT '弹幕内容',
  `video_time`  INT          NOT NULL DEFAULT 0      COMMENT '弹幕出现时间点（秒）',
  `color`       VARCHAR(20)  NOT NULL DEFAULT '#FFFFFF' COMMENT '弹幕颜色（十六进制）',
  `mode`        TINYINT      NOT NULL DEFAULT 0      COMMENT '弹幕模式：0滚动 1顶部 2底部',
  `font_size`   TINYINT      NOT NULL DEFAULT 25     COMMENT '字体大小',
  `status`      TINYINT      NOT NULL DEFAULT 0      COMMENT '状态：0正常 1已屏蔽',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (`id`),
  KEY `idx_video_time` (`video_id`, `video_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='弹幕表';

-- ---------------------------------------------------------------------
-- 10. 消息通知表 message_notify
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `message_notify`;
CREATE TABLE `message_notify` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `user_id`     BIGINT       NOT NULL                COMMENT '接收消息的用户ID',
  `sender_id`   BIGINT       NOT NULL                COMMENT '触发消息的用户ID',
  `type`        TINYINT      NOT NULL                COMMENT '消息类型：1点赞 2评论 3关注 4回复 5系统',
  `content`     VARCHAR(500) DEFAULT NULL            COMMENT '消息内容',
  `video_id`    BIGINT       DEFAULT NULL            COMMENT '关联视频ID（可空）',
  `is_read`     TINYINT      NOT NULL DEFAULT 0      COMMENT '是否已读：0未读 1已读',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_read` (`user_id`, `is_read`),
  KEY `idx_user_create` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息通知表';

-- ---------------------------------------------------------------------
-- 11. 视频投币表 video_coin
-- ---------------------------------------------------------------------
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

-- ---------------------------------------------------------------------
-- 12. 观看历史表 watch_history
-- ---------------------------------------------------------------------
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

-- ---------------------------------------------------------------------
-- 13. 私信表 private_message
-- ---------------------------------------------------------------------
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

-- ---------------------------------------------------------------------
-- 14. 商家申请表 merchant_apply
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `merchant_apply`;
CREATE TABLE `merchant_apply` (
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

-- ---------------------------------------------------------------------
-- 15. 广告表 ad
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `ad`;
CREATE TABLE `ad` (
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

-- ---------------------------------------------------------------------
-- 初始化数据：默认视频分区
-- ---------------------------------------------------------------------
INSERT INTO `video_category` (`name`, `description`, `sort`) VALUES
  ('游戏',   '单机、网游、手游、电竞等游戏相关内容', 1),
  ('生活',   '日常记录、美食、Vlog 等生活内容',        2),
  ('科技',   '数码评测、编程、前沿技术分享',           3),
  ('动画',   '番剧、手书、动画短片',                   4),
  ('音乐',   '翻唱、原创、MV',                         5),
  ('知识',   '课程、科普、纪录片',                     6);
