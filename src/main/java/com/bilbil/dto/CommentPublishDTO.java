package com.bilbil.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 评论发布请求参数
 */
@Data
public class CommentPublishDTO {

    /** 所属视频ID */
    @NotNull(message = "视频ID不能为空")
    private Long videoId;

    /** 评论内容 */
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 1000, message = "评论内容不能超过1000字")
    private String content;

    /** 父评论ID（回复评论时传，一级评论为0） */
    private Long parentId = 0L;

    /** 被回复的用户ID（@某人，回复时传） */
    private Long replyUserId;
}
