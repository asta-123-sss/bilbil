package com.bilbil.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 视频主表实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Video implements Serializable {

    /** 视频ID */
    private Long id;
    /** 投稿用户ID（UP主） */
    private Long userId;
    /** 视频标题 */
    private String title;
    /** 视频简介 */
    private String description;
    /** 所属分区ID */
    private Long categoryId;
    /** 标签（逗号分隔） */
    private String tags;
    /** 封面图 URL */
    private String coverUrl;
    /** 视频访问 URL */
    private String videoUrl;
    /** 视频源文件存储ID */
    private String fileId;
    /** 源文件 MD5（秒传去重） */
    private String fileMd5;
    /** 原始文件名 */
    private String fileName;
    /** 文件大小（字节） */
    private Long fileSize;
    /** 视频时长（秒） */
    private Integer duration;
    /** 可见权限：0公开 1仅粉丝 2私密 */
    private Integer visibility;
    /** 视频状态：0待审核 1已发布 2已下架 3转码中 */
    private Integer status;
    /** 播放量 */
    private Long playCount;
    /** 点赞数 */
    private Long likeCount;
    /** 收藏数 */
    private Long collectCount;
    /** 投币数 */
    private Long coinCount;
    /** 评论数 */
    private Long commentCount;
    /** 弹幕数 */
    private Long danmakuCount;
    /** 投稿时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
    /** 发布时间（审核通过） */
    private LocalDateTime publishTime;
}
