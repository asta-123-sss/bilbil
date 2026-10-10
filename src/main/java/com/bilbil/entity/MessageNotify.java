package com.bilbil.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 消息通知实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageNotify implements Serializable {

    /** 消息ID */
    private Long id;
    /** 接收消息的用户ID */
    private Long userId;
    /** 触发消息的用户ID */
    private Long senderId;
    /** 消息类型：1点赞 2评论 3关注 4回复 5系统 */
    private Integer type;
    /** 消息内容 */
    private String content;
    /** 关联视频ID */
    private Long videoId;
    /** 是否已读：0未读 1已读 */
    private Integer isRead;
    /** 创建时间 */
    private LocalDateTime createTime;
}
