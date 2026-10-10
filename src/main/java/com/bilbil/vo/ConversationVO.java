package com.bilbil.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 私信会话返回对象
 */
@Data
@Builder
public class ConversationVO implements Serializable {

    /** 对方用户ID */
    private Long otherUserId;
    /** 对方昵称 */
    private String otherUserName;
    /** 对方头像 */
    private String otherUserAvatar;
    /** 最后一条消息内容 */
    private String lastContent;
    /** 最后一条消息时间 */
    private LocalDateTime lastTime;
    /** 未读消息数 */
    private Long unreadCount;
}
