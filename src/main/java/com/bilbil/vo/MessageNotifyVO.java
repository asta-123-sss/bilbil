package com.bilbil.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 消息通知返回对象
 */
@Data
@Builder
public class MessageNotifyVO implements Serializable {

    /** 消息ID */
    private Long id;
    /** 消息类型：1点赞 2评论 3关注 4回复 5系统 */
    private Integer type;
    /** 消息内容 */
    private String content;
    /** 关联视频ID */
    private Long videoId;
    /** 是否已读：0未读 1已读 */
    private Integer isRead;
    /** 触发用户ID */
    private Long senderId;
    /** 触发用户昵称 */
    private String senderName;
    /** 触发用户头像 */
    private String senderAvatar;
    /** 创建时间 */
    private LocalDateTime createTime;
}
