package com.bilbil.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 视频投币实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoCoin implements Serializable {

    /** 主键 */
    private Long id;
    /** 投币用户ID */
    private Long userId;
    /** 被投币视频ID */
    private Long videoId;
    /** 状态：0已取消 1已投币 */
    private Integer status;
    /** 投币时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
