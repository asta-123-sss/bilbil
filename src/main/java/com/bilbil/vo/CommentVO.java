package com.bilbil.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评论返回对象
 */
@Data
@Builder
public class CommentVO implements Serializable {

    /** 评论ID */
    private Long id;
    /** 所属视频ID */
    private Long videoId;
    /** 评论用户ID */
    private Long userId;
    /** 评论用户昵称 */
    private String userName;
    /** 评论用户头像 */
    private String userAvatar;
    /** 评论内容 */
    private String content;
    /** 父评论ID */
    private Long parentId;
    /** 根评论ID */
    private Long rootId;
    /** 被回复用户ID */
    private Long replyUserId;
    /** 被回复用户昵称 */
    private String replyUserName;
    /** 回复数（一级评论返回） */
    private Long replyCount;
    /** 评论时间 */
    private LocalDateTime createTime;
}
