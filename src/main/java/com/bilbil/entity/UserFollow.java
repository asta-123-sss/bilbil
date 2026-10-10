package com.bilbil.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户关注实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFollow implements Serializable {

    /** 主键 */
    private Long id;
    /** 关注者用户ID */
    private Long userId;
    /** 被关注者用户ID */
    private Long followId;
    /** 关注时间 */
    private LocalDateTime createTime;
}
