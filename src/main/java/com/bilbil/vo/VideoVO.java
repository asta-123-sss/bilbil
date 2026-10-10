package com.bilbil.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 视频信息返回对象
 */
@Data
@Builder
public class VideoVO implements Serializable {

    /** 视频ID */
    private Long id;
    /** 视频标题 */
    private String title;
    /** 视频简介 */
    private String description;
    /** 分区ID */
    private Long categoryId;
    /** 分区名称 */
    private String categoryName;
    /** 标签 */
    private String tags;
    /** 封面图 URL */
    private String coverUrl;
    /** 视频播放 URL */
    private String videoUrl;
    /** 视频时长（秒） */
    private Integer duration;
    /** 可见权限 */
    private Integer visibility;
    /** 视频状态 */
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

    // -------- 作者信息 --------
    /** 作者ID */
    private Long authorId;
    /** 作者昵称 */
    private String authorName;
    /** 作者头像 */
    private String authorAvatar;

    // -------- 当前登录用户的互动状态（可选，登录后返回） --------
    /** 当前用户是否已点赞 */
    private Boolean liked;
    /** 当前用户是否已收藏 */
    private Boolean collected;
    /** 当前用户是否已投币 */
    private Boolean coined;
    /** 当前用户是否已关注作者 */
    private Boolean followed;
}
