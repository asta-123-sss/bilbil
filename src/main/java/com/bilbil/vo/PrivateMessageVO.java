package com.bilbil.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 私信返回对象
 */
@Data
@Builder
public class PrivateMessageVO implements Serializable {

    /** 私信ID */
    private Long id;
    /** 发送者用户ID */
    private Long fromUserId;
    /** 接收者用户ID */
    private Long toUserId;
    /** 私信内容 */
    private String content;
    /** 是否已读：0未读 1已读 */
    private Integer isRead;
    /** 发送时间 */
    private LocalDateTime createTime;
}
