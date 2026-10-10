package com.bilbil.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 私信实体（点对点）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrivateMessage implements Serializable {

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
