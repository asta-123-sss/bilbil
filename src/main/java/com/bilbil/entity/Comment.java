package com.bilbil.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评论实体（支持子评论/回复）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment implements Serializable {

    /** 评论ID */
    private Long id;
    /** 所属视频ID */
    private Long videoId;
    /** 评论用户ID */
    private Long userId;
    /** 父评论ID（0=一级评论） */
    private Long parentId;
    /** 根评论ID（指向一级评论，用于回复的回复） */
    private Long rootId;
    /** 被回复的用户ID（@某人） */
    private Long replyUserId;
    /** 评论内容 */
    private String content;
    /** 评论点赞数 */
    private Integer likeCount;
    /** 状态：0正常 1已删除 */
    private Integer status;
    /** 评论时间 */
    private LocalDateTime createTime;
}
